/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.NodeStateListener;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a <code>HierarchyManager</code> that caches paths of
 * items.
 */
public class CachingHierarchyManager extends HierarchyManagerImpl
        implements NodeStateListener {

    /**
     * Default upper limit of cached states
     */
    public static final int DEFAULT_UPPER_LIMIT = 10000;

    private static final int MAX_UPPER_LIMIT =
            Integer.getInteger("org.apache.jackrabbit.core.CachingHierarchyManager.cacheSize", DEFAULT_UPPER_LIMIT);

    private static final int CACHE_STATISTICS_LOG_INTERVAL_MILLIS =
            Integer.getInteger("org.apache.jackrabbit.core.CachingHierarchyManager.logInterval", 60000);

    /**
     * Logger instance
     */
    private static Logger log = LoggerFactory.getLogger(CachingHierarchyManager.class);

    /**
     * Mapping of paths to children in the path map
     */
    private final PathMap<LRUEntry> pathCache = new PathMap<LRUEntry>();

    /**
     * Mapping of item ids to <code>LRUEntry</code> in the path map
     */
    private final ReferenceMap<NodeId, LRUEntry> idCache = new ReferenceMap<>(ReferenceStrength.HARD, ReferenceStrength.HARD);

    /**
     * Cache monitor object
     */
    private final Object cacheMonitor = new Object();

    /**
     * Upper limit
     */
    private final int upperLimit;

    /**
     * Object collecting and logging statistics about the idCache
     */
    private final CacheStatistics idCacheStatistics;

    /**
     * Head of LRU
     */
    private LRUEntry head;

    /**
     * Tail of LRU
     */
    private LRUEntry tail;

    /**
     * Flag indicating whether consistency checking is enabled.
     */
    private boolean consistencyCheckEnabled;

    /**
     * Log interval for item state exceptions.
     */
    private static final int ITEM_STATE_EXCEPTION_LOG_INTERVAL_MILLIS = 60 * 1000;

    /**
     * Last time-stamp item state exception was logged with a stacktrace. 
     */
    private long itemStateExceptionLogTimestamp = 0;

    /**
     * Create a new instance of this class.
     *
     * @param rootNodeId   root node id
     * @param provider     item state manager
     */
    public CachingHierarchyManager(NodeId rootNodeId,
                                   ItemStateManager provider) {
        super(rootNodeId, provider);
        upperLimit = MAX_UPPER_LIMIT;
        idCacheStatistics = new CacheStatistics();
        if (log.isTraceEnabled()) {
            log.trace("CachingHierarchyManager initialized. Max cache size = {}", upperLimit, new Exception());
        } else {
            log.debug("CachingHierarchyManager initialized. Max cache size = {}", upperLimit);
        }
    }

    /**
     * Enable or disable consistency checks in this instance.
     *
     * @param enable <code>true</code> to enable consistency checks;
     *               <code>false</code> to disable
     */
    public void enableConsistencyChecks(boolean enable) {
        this.consistencyCheckEnabled = enable;
    }

    //-------------------------------------------------< base class overrides >

    /**
     * {@inheritDoc}
     */
    protected ItemId resolvePath(Path path, int typesAllowed)
            throws RepositoryException {

        Path pathToNode = path;
        if ((typesAllowed & RETURN_NODE) == 0) {
            // if we must not return a node, pass parent path
            // (since we only cache nodes)
            pathToNode = path.getAncestor(1);
        }

        PathMap.Element<LRUEntry> element = map(pathToNode);
        if (element == null) {
            // not even intermediate match: call base class
            return super.resolvePath(path, typesAllowed);
        }

        LRUEntry entry = element.get();
        if (element.hasPath(path)) {
            // exact match: return answer
            synchronized (cacheMonitor) {
                entry.touch();
            }
            return entry.getId();
        }
        Path.Element[] elements = path.getElements();
        try {
            return resolvePath(elements, element.getDepth() + 1, entry.getId(), typesAllowed);
        } catch (ItemStateException e) {
            String msg = "failed to retrieve state of intermediary node for entry: " 
                    + entry.getId() + ", path: " + path.getString();
            logItemStateException(msg, e);
            log.debug(msg);
            // probably stale cache entry -> evict
            evictAll(entry.getId(), true);
        }
        // JCR-3617: fall back to super class in case of ItemStateException
        return super.resolvePath(path, typesAllowed);
    }

    /**
     * {@inheritDoc}
     */
    protected void pathResolved(ItemId id, PathBuilder builder)
            throws MalformedPathException {

        if (id.denotesNode()) {
            cache((NodeId) id, builder.getPath());
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden method tries to find a mapping for the intermediate item
     * <code>state</code> and add its path elements to the builder currently
     * being used. If no mapping is found, the item is cached instead after
     * the base implementation has been invoked.
     */
    protected void buildPath(
            PathBuilder builder, ItemState state, CycleDetector detector)
            throws ItemStateException, RepositoryException {

        if (state.isNode()) {
            PathMap.Element<LRUEntry> element = get(state.getId());
            if (element != null) {
                try {
                    Path.Element[] elements = element.getPath().getElements();
                    for (int i = elements.length - 1; i >= 0; i--) {
                        builder.addFirst(elements[i]);
                    }
                    return;
                } catch (MalformedPathException mpe) {
                    String msg = "Failed to build path of " + state.getId();
                    log.debug(msg);
                    throw new RepositoryException(msg, mpe);
                }
            }
        }

        super.buildPath(builder, state, detector);

        if (state.isNode()) {
            try {
                cache(((NodeState) state).getNodeId(), builder.getPath());
            } catch (MalformedPathException mpe) {
                log.warn("Failed to build path of " + state.getId());
            }
        }
    }

    //-----------------------------------------------------< HierarchyManager >

    /**
     * {@inheritDoc}
     * <p>
     * Overridden method simply checks whether we have an item matching the id
     * and returns its path, otherwise calls base implementation.
     */
    public Path getPath(ItemId id)
            throws ItemNotFoundException, RepositoryException {

        if (id.denotesNode()) {
            PathMap.Element<LRUEntry> element = get(id);
            if (element != null) {
                try {
                    return element.getPath();
                } catch (MalformedPathException mpe) {
                    String msg = "Failed to build path of " + id;
                    log.debug(msg);
                    throw new RepositoryException(msg, mpe);
                }
            }
        }
        return super.getPath(id);
    }

    /**
     * {@inheritDoc}
     */
    public Name getName(ItemId id)
            throws ItemNotFoundException, RepositoryException {

        if (id.denotesNode()) {
            PathMap.Element<LRUEntry> element = get(id);
            if (element != null) {
                return element.getName();
            }
        }
        return super.getName(id);
    }

    /**
     * {@inheritDoc}
     */
    public int getDepth(ItemId id)
            throws ItemNotFoundException, RepositoryException {

        if (id.denotesNode()) {
            PathMap.Element<LRUEntry> element = get(id);
            if (element != null) {
                return element.getDepth();
            }
        }
        return super.getDepth(id);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAncestor(NodeId nodeId, ItemId itemId)
            throws ItemNotFoundException, RepositoryException {

        if (itemId.denotesNode()) {
            PathMap.Element<LRUEntry> element = get(nodeId);
            if (element != null) {
                PathMap.Element<LRUEntry> child = get(itemId);
                if (child != null) {
                    return element.isAncestorOf(child);
                }
            }
        }
        return super.isAncestor(nodeId, itemId);
    }

    //----------------------------------------------------< ItemStateListener >

    /**
     * {@inheritDoc}
     */
    public void stateCreated(ItemState created) {
    }

    /**
     * {@inheritDoc}
     */
    public void stateModified(ItemState modified) {
        if (modified.isNode()) {
            nodeModified((NodeState) modified);
        }
    }

    /**
     * {@inheritDoc}
     *
     * If path information is cached for <code>modified</code>, this iterates
     * over all child nodes in the path map, evicting the ones that do not
     * (longer) exist in the underlying <code>NodeState</code>.
     */
    public void nodeModified(NodeState modified) {
        synchronized (cacheMonitor) {
            for (PathMap.Element<LRUEntry> element
                    : getCachedPaths(modified.getNodeId())) {
                for (PathMap.Element<LRUEntry> child : element.getChildren()) {
                    ChildNodeEntry cne = modified.getChildNodeEntry(
                            child.getName(), child.getNormalizedIndex());
                    if (cne == null) {
                        // Item does not exist, remove
                        evict(child, true);
                    } else {
                        LRUEntry childEntry = child.get();
                        if (childEntry != null
                                && !cne.getId().equals(childEntry.getId())) {
                            // Different child item, remove
                            evict(child, true);
                        }
                    }
                }
            }
            checkConsistency();
        }
    }

    private List<PathMap.Element<LRUEntry>> getCachedPaths(NodeId id) {
        // JCR-2720: Handle the root path as a special case
        if (rootNodeId.equals(id)) {
            return Collections.singletonList(pathCache.map(
                    PathFactoryImpl.getInstance().getRootPath(), true));
        }

        LRUEntry entry = idCache.get(id);
        if (entry != null) {
            return Arrays.asList(entry.getElements());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stateDestroyed(ItemState destroyed) {
        evictAll(destroyed.getId(), true);
    }

    /**
     * {@inheritDoc}
     */
    public void stateDiscarded(ItemState discarded) {
        if (discarded.isTransient() && !discarded.hasOverlayedState()
                && discarded.getStatus() == ItemState.STATUS_NEW) {
            // a new node has been discarded -> remove from cache
            evictAll(discarded.getId(), true);
        } else if (provider.hasItemState(discarded.getId())) {
            evictAll(discarded.getId(), false);
        } else {
            evictAll(discarded.getId(), true);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void nodeAdded(NodeState state, Name name, int index, NodeId id) {
        synchronized (cacheMonitor) {
            if (idCache.containsKey(state.getNodeId())) {
                // Optimization: ignore notifications for nodes that are not in the cache
                try {
                    Path path = PathFactoryImpl.getInstance().create(getPath(state.getNodeId()), name, index, true);
                    nodeAdded(state, path, id);
                    checkConsistency();
                } catch (PathNotFoundException e) {
                    log.warn("Unable to get path of node " + state.getNodeId()
                            + ", event ignored.");
                } catch (MalformedPathException e) {
                    log.warn("Unable to create path of " + id, e);
                } catch (ItemNotFoundException e) {
                    log.warn("Unable to find item " + state.getNodeId(), e);
                } catch (ItemStateException e) {
                    log.warn("Unable to find item " + id, e);
                } catch (RepositoryException e) {
                    log.warn("Unable to get path of " + state.getNodeId(), e);
                }
            } else if (state.getParentId() == null && idCache.containsKey(id)) {
                // A top level node was added
                evictAll(id, true);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Iterate over all cached children of this state and verify each
     * child's position.
     */
    public void nodesReplaced(NodeState state) {
        synchronized (cacheMonitor) {
            LRUEntry entry = idCache.get(state.getNodeId());
            if (entry == null) {
                return;
            }
            for (PathMap.Element<LRUEntry> parent : entry.getElements()) {
                HashMap<Path.Element, PathMap.Element<LRUEntry>> newChildrenOrder =
                    new HashMap<Path.Element, PathMap.Element<LRUEntry>>();
                boolean orderChanged = false;

                for (PathMap.Element<LRUEntry> child : parent.getChildren()) {
                    LRUEntry childEntry = child.get();
                    if (childEntry == null) {
                        // Child has no associated UUID information: we're
                        // therefore unable to determine if this child's
                        // position is still accurate and have to assume
                        // the worst and remove it.
                        evict(child, false);
                    } else {
                        NodeId childId = childEntry.getId();
                        ChildNodeEntry cne = state.getChildNodeEntry(childId);
                        if (cne == null) {
                            // Child no longer in parent node, so remove it
                            evict(child, false);
                        } else {
                            // Put all children into map of new children order
                            // - regardless whether their position changed or
                            // not - as we might need to reorder them later on.
                            Path.Element newNameIndex =
                                PathFactoryImpl.getInstance().createElement(
                                        cne.getName(), cne.getIndex());
                            newChildrenOrder.put(newNameIndex, child);

                            if (!newNameIndex.equals(child.getPathElement())) {
                                orderChanged = true;
                            }
                        }
                    }
                }

                if (orderChanged) {
                    /* If at least one child changed its position, reorder */
                    parent.setChildren(newChildrenOrder);
                }
            }
            checkConsistency();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void nodeRemoved(NodeState state, Name name, int index, NodeId id) {
        synchronized (cacheMonitor) {
            if (idCache.containsKey(state.getNodeId())) {
                // Optimization: ignore notifications for nodes that are not in the cache
                try {
                    Path path = PathFactoryImpl.getInstance().create(getPath(state.getNodeId()), name, index, true);
                    nodeRemoved(state, path, id);
                    checkConsistency();
                } catch (PathNotFoundException e) {
                    log.warn("Unable to get path of node " + state.getNodeId()
                            + ", event ignored.");
                } catch (MalformedPathException e) {
                    log.warn("Unable to create path of " + id, e);
                } catch (ItemStateException e) {
                    log.warn("Unable to find item " + id, e);
                } catch (ItemNotFoundException e) {
                    log.warn("Unable to get path of " + state.getNodeId(), e);
                } catch (RepositoryException e) {
                    log.warn("Unable to get path of " + state.getNodeId(), e);
                }
            } else if (state.getParentId() == null && idCache.containsKey(id)) {
                // A top level node was removed
                evictAll(id, true);
            }
        }
    }

    //------------------------------------------------------< private methods >

    /**
     * Return the first cached path that is mapped to given id.
     *
     * @param id node id
     * @return cached element, <code>null</code> if not found
     */
    private PathMap.Element<LRUEntry> get(ItemId id) {
        synchronized (cacheMonitor) {
            LRUEntry entry = idCache.get(id);
            if (entry != null) {
                entry.touch();
                return entry.getElements()[0];
            }
            return null;
        }
    }

    /**
     * Return the nearest cached element in the path map, given a path.
     * The returned element is guaranteed to have an associated object that
     * is not <code>null</code>.
     *
     * @param path path
     * @return cached element, <code>null</code> if not found
     */
    private PathMap.Element<LRUEntry> map(Path path) {
        synchronized (cacheMonitor) {
            PathMap.Element<LRUEntry> element = pathCache.map(path, false);
            while (element != null) {
                LRUEntry entry = element.get();
                if (entry != null) {
                    entry.touch();
                    return element;
                }
                element = element.getParent();
            }
            return null;
        }
    }

    /**
     * Cache an item in the hierarchy given its id and path.
     *
     * @param id   node id
     * @param path path to item
     */
    private void cache(NodeId id, Path path) {
        synchronized (cacheMonitor) {
            if (isCached(id, path)) {
                return;
            }
            if (idCache.size() >= upperLimit) {

                idCacheStatistics.log();

                /**
                 * Remove least recently used item. Scans the LRU list from
                 * head to tail and removes the first item that has no children.
                 */
                LRUEntry entry = head;
                while (entry != null) {
                    PathMap.Element<LRUEntry>[] elements = entry.getElements();
                    int childrenCount = 0;
                    for (int i = 0; i < elements.length; i++) {
                        childrenCount += elements[i].getChildrenCount();
                    }
                    if (childrenCount == 0) {
                        evictAll(entry.getId(), false);
                        return;
                    }
                    entry = entry.getNext();
                }
            }
            PathMap.Element<LRUEntry> element = pathCache.put(path);
            if (element.get() != null) {
                if (!id.equals((element.get()).getId())) {
                    log.debug("overwriting PathMap.Element");
                }
            }
            LRUEntry entry = idCache.get(id);
            if (entry == null) {
                entry = new LRUEntry(id, element);
                idCache.put(id, entry);
            } else {
                entry.addElement(element);
            }
            element.set(entry);

            checkConsistency();
        }
    }

    /**
     * Return a flag indicating whether a certain node and/or path is cached.
     * If <code>path</code> is <code>null</code>, check whether the item is
     * cached at all. If <code>path</code> is <b>not</b> <code>null</code>,
     * check whether the item is cached with that path.
     *
     * @param id item id
     * @param path path, may be <code>null</code>
     * @return <code>true</code> if the item is already cached;
     *         <code>false</code> otherwise
     */
    boolean isCached(NodeId id, Path path) {
        synchronized (cacheMonitor) {
            LRUEntry entry = idCache.get(id);
            if (entry == null) {
                return false;
            }
            if (path == null) {
                return true;
            }
            PathMap.Element<LRUEntry>[] elements = entry.getElements();
            for (int i = 0; i < elements.length; i++) {
                if (elements[i].hasPath(path)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Return a flag indicating whether a certain path is cached.
     *
     * @param path item path
     * @return <code>true</code> if the item is already cached;
     *         <code>false</code> otherwise
     */
    boolean isCached(Path path) {
        synchronized (cacheMonitor) {
            PathMap.Element<LRUEntry> element = pathCache.map(path, true);
            if (element != null) {
                return element.get() != null;
            }
            return false;
        }
    }

    /**
     * Remove all path mapping for a given item id. Removes the associated
     * <code>LRUEntry</code> and the <code>PathMap.Element</code> with it.
     * Indexes of same name sibling elements are shifted!
     *
     * @param id item id
     */
    private void evictAll(ItemId id, boolean shift) {
        synchronized (cacheMonitor) {
            LRUEntry entry = idCache.get(id);
            if (entry != null) {
                PathMap.Element<LRUEntry>[] elements = entry.getElements();
                for (int i = 0; i < elements.length; i++) {
                    evict(elements[i], shift);
                }
            }
            checkConsistency();
        }
    }

    /**
     * Evict path map element from cache. This will traverse all children
     * of this element and remove the objects associated with them.
     * Index of same name sibling items are shifted!
     *
     * @param element path map element
     */
    private void evict(PathMap.Element<LRUEntry> element, boolean shift) {
        // assert: synchronized (cacheMonitor)
        element.traverse(new PathMap.ElementVisitor<LRUEntry>() {
            public void elementVisited(PathMap.Element<LRUEntry> element) {
                LRUEntry entry = element.get();
                if (entry.removeElement(element) == 0) {
                    idCache.remove(entry.getId());
                    entry.remove();
                }
            }
        }, false);
        element.remove(shift);
    }

    /**
     * Invoked when a notification about a child node addition has been received.
     *
     * @param state node state where child was added
     * @param path  path to child node
     * @param id    child node id
     *
     * @throws PathNotFoundException if the path was not found
     * @throws RepositoryException If the path's direct ancestor cannot be determined.
     * @throws ItemStateException If the id cannot be resolved to a NodeState.
     */
    private void nodeAdded(NodeState state, Path path, NodeId id)
            throws RepositoryException, ItemStateException {

        // assert: synchronized (cacheMonitor)
        PathMap.Element<LRUEntry> element = null;

        LRUEntry entry = idCache.get(id);
        if (entry != null) {
            // child node already cached: this can have the following
            // reasons:
            //    1) node was moved, cached path is outdated
            //    2) node was cloned, cached path is still valid
            NodeState child = null;
            if (hasItemState(id)) {
                child = (NodeState) getItemState(id);
            }
            if (child == null || !child.isShareable()) {
                PathMap.Element<LRUEntry>[] elements = entry.getElements();
                element = elements[0];
                for (int i = 0; i < elements.length; i++) {
                    elements[i].remove();
                }
            }
        }
        PathMap.Element<LRUEntry> parent = pathCache.map(path.getAncestor(1), true);
        if (parent != null) {
            parent.insert(path.getNameElement());
        }
        if (element != null) {
            // store remembered element at new position
            pathCache.put(path, element);
        }
    }

    /**
     * Invoked when a notification about a child node removal has been received.
     *
     * @param state node state
     * @param path  node path
     * @param id    node id
     *
     * @throws PathNotFoundException if the path was not found.
     * @throws RepositoryException If the path's direct ancestor cannot be determined.
     * @throws ItemStateException If the id cannot be resolved to a NodeState.
     */
    private void nodeRemoved(NodeState state, Path path, NodeId id)
            throws RepositoryException, ItemStateException {

        // assert: synchronized (cacheMonitor)
        PathMap.Element<LRUEntry> parent =
            pathCache.map(path.getAncestor(1), true);
        if (parent == null) {
            return;
        }
        PathMap.Element<LRUEntry> element =
            parent.getDescendant(path.getLastElement(), true);
        if (element != null) {
            // with SNS, this might evict a child that is NOT the one
            // having <code>id</code>, check first whether item has
            // the id passed as argument
            LRUEntry entry = element.get();
            if (entry != null && !entry.getId().equals(id)) {
                return;
            }
            // if item is shareable, remove this path only, otherwise
            // every path this item has been mapped to
            NodeState child = null;
            if (hasItemState(id)) {
                child = (NodeState) getItemState(id);
            }
            if (child == null || !child.isShareable()) {
                evictAll(id, true);
            } else {
                evict(element, true);
            }
        } else {
            // element itself is not cached, but removal might cause SNS
            // index shifting
            parent.remove(path.getNameElement());
        }
    }

    /**
     * Dump contents of path map and elements included to a string.
     */
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        synchronized (cacheMonitor) {
            pathCache.traverse(new PathMap.ElementVisitor<LRUEntry>() {
                public void elementVisited(PathMap.Element<LRUEntry> element) {
                    for (int i = 0; i < element.getDepth(); i++) {
                        builder.append("--");
                    }
                    builder.append(element.getName());
                    int index = element.getIndex();
                    if (index != 0 && index != 1) {
                        builder.append('[');
                        builder.append(index);
                        builder.append(']');
                    }
                    builder.append("  ");
                    builder.append(element.get());
                    builder.append("\n");
                }
            }, true);
        }
        return builder.toString();
    }

    /**
     * Check consistency.
     */
    private void checkConsistency() throws IllegalStateException {
        // assert: synchronized (cacheMonitor)
        if (!consistencyCheckEnabled) {
            return;
        }

        int elementsInCache = 0;

        Iterator<LRUEntry> iter = idCache.values().iterator();
        while (iter.hasNext()) {
            LRUEntry entry = iter.next();
            elementsInCache += entry.getElements().length;
        }

        class PathMapElementCounter implements PathMap.ElementVisitor<LRUEntry> {
            int count;
            public void elementVisited(PathMap.Element<LRUEntry> element) {
                LRUEntry mappedEntry = element.get();
                LRUEntry cachedEntry = idCache.get(mappedEntry.getId());
                if (cachedEntry == null) {
                    String msg = "Path element (" + element +
                        " ) cached in path map, associated id (" +
                        mappedEntry.getId() + ") isn't.";
                    throw new IllegalStateException(msg);
                }
                if (cachedEntry != mappedEntry) {
                    String msg = "LRUEntry associated with element (" + element +
                        " ) in path map is not equal to cached LRUEntry (" +
                        cachedEntry.getId() + ").";
                    throw new IllegalStateException(msg);
                }
                PathMap.Element<LRUEntry>[] elements = cachedEntry.getElements();
                for (int i = 0; i < elements.length; i++) {
                    if (elements[i] == element) {
                        count++;
                        return;
                    }
                }
                String msg = "Element (" + element +
                    ") cached in path map, but not in associated LRUEntry (" +
                    cachedEntry.getId() + ").";
                throw new IllegalStateException(msg);
            }
        }

        PathMapElementCounter counter = new PathMapElementCounter();
        pathCache.traverse(counter, false);
        if (counter.count != elementsInCache) {
            String msg = "PathMap element and cached element count don't match (" +
                counter.count + " != " + elementsInCache + ")";
            throw new IllegalStateException(msg);
        }
    }

    /**
     * Helper method to log item state exception with stack trace every so often.
     * 
     * @param logMessage log message
     * @param e item state exception
     */
    private void logItemStateException(String logMessage, ItemStateException e) {
        long now = System.currentTimeMillis();
        if ((now - itemStateExceptionLogTimestamp) >= ITEM_STATE_EXCEPTION_LOG_INTERVAL_MILLIS) {
            itemStateExceptionLogTimestamp = now;
            log.debug(logMessage, e);
        } else {
            log.debug(logMessage);
        }
    }

    /**
     * Entry in the LRU list
     */
    private class LRUEntry {

        /**
         * Previous entry
         */
        private LRUEntry previous;

        /**
         * Next entry
         */
        private LRUEntry next;

        /**
         * Node id
         */
        private final NodeId id;

        /**
         * Elements in path map
         */
        private PathMap.Element<LRUEntry>[] elements;

        /**
         * Create a new instance of this class
         *
         * @param id node id
         * @param element the path map element for this entry
         */
        @SuppressWarnings("unchecked")
        public LRUEntry(NodeId id, PathMap.Element<LRUEntry> element) {
            this.id = id;
            this.elements = new PathMap.Element[] { element };

            append();
        }

        /**
         * Append entry to end of LRU list
         */
        public void append() {
            if (tail == null) {
                head = this;
                tail = this;
            } else {
                previous = tail;
                tail.next = this;
                tail = this;
            }
        }

        /**
         * Remove entry from LRU list
         */
        public void remove() {
            if (previous != null) {
                previous.next = next;
            }
            if (next != null) {
                next.previous = previous;
            }
            if (head == this) {
                head = next;
            }
            if (tail == this) {
                tail = previous;
            }
            previous = null;
            next = null;
        }

        /**
         * Touch entry. Removes it from its current position in the LRU list
         * and moves it to the end.
         */
        public void touch() {
            remove();
            append();
        }

        /**
         * Return next LRU entry
         *
         * @return next LRU entry
         */
        public LRUEntry getNext() {
            return next;
        }

        /**
         * Return node ID
         *
         * @return node ID
         */
        public NodeId getId() {
            return id;
        }

        /**
         * Return elements in path map that are mapped to <code>id</code>. If
         * this entry is a shareable node or one of its descendant, it can
         * be reached by more than one path.
         *
         * @return element in path map
         */
        public PathMap.Element<LRUEntry>[] getElements() {
            return elements;
        }

        /**
         * Add a mapping to some element.
         */
        @SuppressWarnings("unchecked")
        public void addElement(PathMap.Element<LRUEntry> element) {
            PathMap.Element<LRUEntry>[] tmp =
                new PathMap.Element[elements.length + 1];
            System.arraycopy(elements, 0, tmp, 0, elements.length);
            tmp[elements.length] = element;
            elements = tmp;
        }

        /**
         * Remove a mapping to some element from this entry.
         *
         * @return number of mappings left
         */
        @SuppressWarnings("unchecked")
        public int removeElement(PathMap.Element<LRUEntry> element) {
            boolean found = false;
            for (int i = 0; i < elements.length; i++) {
                if (found) {
                    elements[i - 1] = elements[i];
                } else if (elements[i] == element) {
                    found = true;
                }
            }
            if (found) {
                PathMap.Element<LRUEntry>[] tmp =
                    new PathMap.Element[elements.length - 1];
                System.arraycopy(elements, 0, tmp, 0, tmp.length);
                elements = tmp;
            }
            return elements.length;
        }

        /**
         * {@inheritDoc}
         */
        public String toString() {
            return id.toString();
        }
    }

    private final class CacheStatistics {

        private final String id;

        private final ReferenceMap<NodeId, LRUEntry> cache;

        private long timeStamp = 0;

        public CacheStatistics() {
            this.id = cacheMonitor.toString();
            this.cache = idCache;
        }

        public void log() {
            if (log.isDebugEnabled()) {
                long now = System.currentTimeMillis();
                final String msg = "Cache id = {};size = {};max = {}";
                if (log.isTraceEnabled()) {
                    log.trace(msg, new Object[]{id, this.cache.size(), upperLimit}, new Exception());
                } else if (now > timeStamp + CACHE_STATISTICS_LOG_INTERVAL_MILLIS) {
                    timeStamp = now;
                    log.debug(msg, new Object[]{id, this.cache.size(), upperLimit}, new Exception());
                }
            }
        }
    }

}
