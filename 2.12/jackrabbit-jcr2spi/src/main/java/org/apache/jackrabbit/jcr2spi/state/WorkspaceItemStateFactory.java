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
package org.apache.jackrabbit.jcr2spi.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.ItemInfo;
import org.apache.jackrabbit.spi.ItemInfoCache;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.NodeInfo;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.PropertyInfo;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.ItemInfoCache.Entry;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>WorkspaceItemStateFactory</code>...
 */
public class WorkspaceItemStateFactory extends AbstractItemStateFactory {
    private static Logger log = LoggerFactory.getLogger(WorkspaceItemStateFactory.class);

    private final RepositoryService service;
    private final SessionInfo sessionInfo;
    private final ItemDefinitionProvider definitionProvider;

    private final ItemInfoCache cache;

    public WorkspaceItemStateFactory(RepositoryService service, SessionInfo sessionInfo,
                                     ItemDefinitionProvider definitionProvider, ItemInfoCache cache) {

        this.service = service;
        this.sessionInfo = sessionInfo;
        this.definitionProvider = definitionProvider;
        this.cache = cache;
    }

    public NodeState createRootState(NodeEntry entry) throws ItemNotFoundException, RepositoryException {
        IdFactory idFactory = service.getIdFactory();
        PathFactory pf = service.getPathFactory();

        return createNodeState(idFactory.createNodeId((String) null, pf.getRootPath()), entry);
    }

    /**
     * Creates the node with information retrieved from the <code>RepositoryService</code>.
     */
    public NodeState createNodeState(NodeId nodeId, NodeEntry entry) throws ItemNotFoundException,
            RepositoryException {

        try {
            Entry<NodeInfo> cached = cache.getNodeInfo(nodeId);
            ItemInfo info;
            if (isUpToDate(cached, entry)) {
                info = cached.info;
            } else {
                // otherwise retrieve item info from service and cache the whole batch
                Iterator<? extends ItemInfo> infos = service.getItemInfos(sessionInfo, nodeId);
                info = first(infos, cache, entry.getGeneration());
                if (info == null || !info.denotesNode()) {
                    throw new ItemNotFoundException("NodeId: " + nodeId);
                }
            }

            assertMatchingPath(info, entry);
            return createNodeState((NodeInfo) info, entry);
        }
        catch (PathNotFoundException e) {
            throw new ItemNotFoundException(e);
        }
    }

    /**
     * Creates the node with information retrieved from the <code>RepositoryService</code>.
     * Intermediate entries are created as needed.
     */
    public NodeState createDeepNodeState(NodeId nodeId, NodeEntry anyParent) throws ItemNotFoundException,
            RepositoryException {

        try {
            // Get item info from cache
            Iterator<? extends ItemInfo> infos = null;
            Entry<NodeInfo> cached = cache.getNodeInfo(nodeId);
            ItemInfo info;
            if (cached == null) {
                // or from service if not in cache
                infos = service.getItemInfos(sessionInfo, nodeId);
                info = first(infos, null, 0);
                if (info == null || !info.denotesNode()) {
                    throw new ItemNotFoundException("NodeId: " + nodeId);
                }
            } else {
                info = cached.info;
            }

            // Build the hierarchy entry for the item info
            HierarchyEntry entry = createHierarchyEntries(info, anyParent);
            if (entry == null || !entry.denotesNode()) {
                throw new ItemNotFoundException(
                        "HierarchyEntry does not belong to any existing ItemInfo. No ItemState was created.");
            } else {
                // Now we can check whether the item info from the cache is up to date
                long generation = entry.getGeneration();
                if (isOutdated(cached, entry)) {
                    // if not, retrieve the item info from the service and put the whole batch into the cache
                    infos = service.getItemInfos(sessionInfo, nodeId);
                    info = first(infos, cache, generation);
                } else if (infos != null) {
                    // Otherwise put the whole batch retrieved from the service earlier into the cache
                    cache.put(info, generation);
                    first(infos, cache, generation);
                }

                assertMatchingPath(info, entry);
                return createNodeState((NodeInfo) info, (NodeEntry) entry);
            }
        }
        catch (PathNotFoundException e) {
            throw new ItemNotFoundException(e);
        }
    }

    /**
     * Creates the PropertyState with information retrieved from the <code>RepositoryService</code>.
     */
    public PropertyState createPropertyState(PropertyId propertyId, PropertyEntry entry)
            throws ItemNotFoundException, RepositoryException {

        try {
            // Get item info from cache and use it if up to date
            Entry<PropertyInfo> cached = cache.getPropertyInfo(propertyId);
            ItemInfo info;
            if (isUpToDate(cached, entry)) {
                info = cached.info;
            } else {
                // otherwise retrieve item info from service and cache the whole batch
                Iterator<? extends ItemInfo> infos = service.getItemInfos(sessionInfo, propertyId);
                info = first(infos, cache, entry.getGeneration());
                if (info == null || info.denotesNode()) {
                    throw new ItemNotFoundException("PropertyId: " + propertyId);
                }
            }

            assertMatchingPath(info, entry);
            return createPropertyState((PropertyInfo) info, entry);
        }
        catch (PathNotFoundException e) {
            throw new ItemNotFoundException(e);
        }
    }

    /**
     * Creates the PropertyState with information retrieved from the <code>RepositoryService</code>.
     * Intermediate entries are created as needed.
     */
    public PropertyState createDeepPropertyState(PropertyId propertyId, NodeEntry anyParent)
            throws RepositoryException {

        try {
            // Get item info from cache
            Iterator<? extends ItemInfo> infos = null;
            Entry<PropertyInfo> cached = cache.getPropertyInfo(propertyId);
            ItemInfo info;
            if (cached == null) {
                // or from service if not in cache
                infos = service.getItemInfos(sessionInfo, propertyId);
                info = first(infos, null, 0);
                if (info == null || info.denotesNode()) {
                    throw new ItemNotFoundException("PropertyId: " + propertyId);
                }
            } else {
                info = cached.info;
            }

            // Build the hierarchy entry for the item info
            HierarchyEntry entry = createHierarchyEntries(info, anyParent);
            if (entry == null || entry.denotesNode()) {
                throw new ItemNotFoundException(
                        "HierarchyEntry does not belong to any existing ItemInfo. No ItemState was created.");
            } else {
                long generation = entry.getGeneration();
                if (isOutdated(cached, entry)) {
                    // if not, retrieve the item info from the service and put the whole batch into the cache
                    infos = service.getItemInfos(sessionInfo, propertyId);
                    info = first(infos, cache, generation);
                } else if (infos != null) {
                    // Otherwise put the whole batch retrieved from the service earlier into the cache
                    cache.put(info, generation);
                    first(infos, cache, generation);
                }

                assertMatchingPath(info, entry);
                return createPropertyState((PropertyInfo) info, (PropertyEntry) entry);
            }

        } catch (PathNotFoundException e) {
            throw new ItemNotFoundException(e);
        }
    }

    public Iterator<ChildInfo> getChildNodeInfos(NodeId nodeId) throws ItemNotFoundException,
            RepositoryException {

        return service.getChildInfos(sessionInfo, nodeId);
    }

    public Iterator<PropertyId> getNodeReferences(NodeState nodeState, Name propertyName, boolean weak) {
        NodeEntry entry = nodeState.getNodeEntry();

        // Shortcut
        if (entry.getUniqueID() == null || !entry.hasPropertyEntry(NameConstants.JCR_UUID)) {
            // for sure not referenceable
            Set<PropertyId> t = Collections.emptySet();
            return t.iterator();
        }

        // Has a unique ID and is potentially mix:referenceable. Try to retrieve references
        try {
            return service.getReferences(sessionInfo, entry.getWorkspaceId(), propertyName, weak);
        } catch (RepositoryException e) {
            log.debug("Unable to determine references to {}", nodeState);
            Set<PropertyId> t = Collections.emptySet();
            return t.iterator();
        }
    }

    //------------------------------------------------------------< private >---

    /**
     * Returns the first item in the iterator if it exists. Otherwise returns <code>null</code>.
     * If <code>cache</code> is not <code>null</code>, caches all items by the given
     * <code>generation</code>.
     */
    private static ItemInfo first(Iterator<? extends ItemInfo> infos, ItemInfoCache cache, long generation) {
        ItemInfo first = null;
        if (infos.hasNext()) {
            first = infos.next();
            if (cache != null) {
                cache.put(first, generation);
            }
        }

        if (cache != null) {
            while (infos.hasNext()) {
                cache.put(infos.next(), generation);
            }
        }

        return first;
    }

    /**
     * Create the node state with the information from <code>info</code>.
     *
     * @param info the <code>NodeInfo</code> to use to create the <code>NodeState</code>.
     * @param entry  the hierarchy entry for of this state
     * @return the new <code>NodeState</code>.
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    private NodeState createNodeState(NodeInfo info, NodeEntry entry) throws ItemNotFoundException,
            RepositoryException {

        // Make sure the entry has the correct ItemId
        // this may not be the case, if the hierarchy has not been completely
        // resolved yet -> if uniqueID is present, set it on this entry or on
        // the appropriate parent entry
        String uniqueID = info.getId().getUniqueID();
        Path path = info.getId().getPath();
        if (path == null) {
            entry.setUniqueID(uniqueID);
        } else if (uniqueID != null) {
            // uniqueID that applies to a parent NodeEntry -> get parentEntry
            NodeEntry parent = getAncestor(entry, path.getLength());
            parent.setUniqueID(uniqueID);
        }

        int previousStatus = entry.getStatus();
        if (Status.isTransient(previousStatus) || Status.isStale(previousStatus)) {
            log.debug("Node has pending changes; omit resetting the state.");
            return entry.getNodeState();
        }

        // update NodeEntry from the information present in the NodeInfo (prop entries)
        List<Name> propNames = new ArrayList<Name>();
        for (Iterator<PropertyId> it = info.getPropertyIds(); it.hasNext(); ) {
            PropertyId pId = it.next();
            Name propertyName = pId.getName();
            propNames.add(propertyName);
        }
        try {
            entry.setPropertyEntries(propNames);
        } catch (ItemExistsException e) {
            // should not get here
            log.error("Internal error", e);
        }

        // unless the child-info are omitted by the SPI impl -> make sure
        // the child entries the node entry are initialized or updated.
        Iterator<ChildInfo> childInfos = info.getChildInfos();
        if (childInfos != null) {
            entry.setNodeEntries(childInfos);
        }

        // now build or update the nodestate itself
        NodeState tmp = new NodeState(entry, info, this, definitionProvider);
        entry.setItemState(tmp);

        NodeState nState = entry.getNodeState();
        if (previousStatus == Status._UNDEFINED_) {
            // tmp state was used as resolution for the given entry i.e. the
            // entry was not available before. otherwise the 2 states were
            // merged. see HierarchyEntryImpl#setItemState
            notifyCreated(nState);
        } else {
            notifyUpdated(nState, previousStatus);
        }
        return nState;
    }

    /**
     * Create the property state with the information from <code>info</code>.
     *
     * @param info the <code>PropertyInfo</code> to use to create the <code>PropertyState</code>.
     * @param entry  the hierarchy entry for of this state
     * @return the new <code>PropertyState</code>.
     * @throws RepositoryException
     */
    private PropertyState createPropertyState(PropertyInfo info, PropertyEntry entry)
            throws RepositoryException {

        // make sure uuid part of id is correct
        String uniqueID = info.getId().getUniqueID();
        if (uniqueID != null) {
            // uniqueID always applies to a parent NodeEntry -> get parentEntry
            NodeEntry parent = getAncestor(entry, info.getId().getPath().getLength());
            parent.setUniqueID(uniqueID);
        }

        int previousStatus = entry.getStatus();
        if (Status.isTransient(previousStatus) || Status.isStale(previousStatus)) {
            log.debug("Property has pending changes; omit resetting the state.");
            return entry.getPropertyState();
        }

        // now build or update the nodestate itself
        PropertyState tmp = new PropertyState(entry, info, this, definitionProvider);
        entry.setItemState(tmp);

        PropertyState pState = entry.getPropertyState();
        if (previousStatus == Status._UNDEFINED_) {
            // tmp state was used as resolution for the given entry i.e. the
            // entry was not available before. otherwise the 2 states were
            // merged. see HierarchyEntryImpl#setItemState
            notifyCreated(pState);
        } else {
            notifyUpdated(pState, previousStatus);
        }
        return pState;
    }

    /**
     * Create missing hierarchy entries on the path from <code>anyParent</code> to the path
     * of the <code>itemInfo</code>.
     *
     * @param info
     * @param anyParent
     * @return the hierarchy entry for <code>info</code>
     * @throws RepositoryException
     */
    private HierarchyEntry createHierarchyEntries(ItemInfo info, NodeEntry anyParent)
            throws RepositoryException {

        // Calculate relative path of missing entries
        Path anyParentPath = anyParent.getWorkspacePath();
        Path relPath = anyParentPath.computeRelativePath(info.getPath());
        Path.Element[] missingElems = relPath.getElements();

        NodeEntry entry = anyParent;
        int last = missingElems.length - 1;
        for (int i = 0; i <= last; i++) {
            if (missingElems[i].denotesParent()) {
                // Walk up the hierarchy for 'negative' paths
                // until the smallest common root is found
                entry = entry.getParent();
            } else if (missingElems[i].denotesName()) {
                // Add missing elements starting from the smallest common root
                Name name = missingElems[i].getName();
                int index = missingElems[i].getNormalizedIndex();

                if (i == last && !info.denotesNode()) {
                    return entry.getOrAddPropertyEntry(name);
                } else {
                    entry = createNodeEntry(entry, name, index);
                }
            }
        }
        return entry;
    }

    private NodeEntry createNodeEntry(NodeEntry parentEntry, Name name, int index) throws RepositoryException {
        Entry<NodeInfo> cached = cache.getNodeInfo(parentEntry.getWorkspaceId());
        if (isUpToDate(cached, parentEntry)) {
            Iterator<ChildInfo> childInfos = cached.info.getChildInfos();
            if (childInfos != null) {
                parentEntry.setNodeEntries(childInfos);
            }
        }

        return parentEntry.getOrAddNodeEntry(name, index, null);
    }

    /**
     * Returns true if <code>cache</code> is not <code>null</code> and
     * the cached entry is up to date.
     * @param cacheEntry
     * @param entry
     * @return
     * @throws RepositoryException
     */
    private static boolean isUpToDate(Entry<?> cacheEntry, HierarchyEntry entry) throws RepositoryException {
        return cacheEntry != null &&
            cacheEntry.generation >= entry.getGeneration() &&
            isMatchingPath(cacheEntry.info, entry);
    }

    /**
     * Returns true if <code>cache</code> is not <code>null</code> and
     * the cached entry is not up to date.
     * @param cacheEntry
     * @param entry
     * @return
     * @throws RepositoryException
     */
    private static boolean isOutdated(Entry<?> cacheEntry, HierarchyEntry entry) throws RepositoryException {
        return cacheEntry != null &&
            (cacheEntry.generation < entry.getGeneration() ||
            !isMatchingPath(cacheEntry.info, entry));
    }

    private static boolean isMatchingPath(ItemInfo info, HierarchyEntry entry) throws RepositoryException {
        Path infoPath = info.getPath();
        Path wspPath = entry.getWorkspacePath();
        return infoPath.equals(wspPath);
    }

    /**
     * Validation check: Path of the given ItemInfo must match to the Path of
     * the HierarchyEntry. This is required for Items that are identified by
     * a uniqueID that may move within the hierarchy upon restore or clone.
     *
     * @param info
     * @param entry
     * @throws RepositoryException
     */
    private static void assertMatchingPath(ItemInfo info, HierarchyEntry entry) throws RepositoryException {
        if (!isMatchingPath(info, entry)) {
            // TODO: handle external move of nodes (parents) identified by uniqueID
            throw new ItemNotFoundException("HierarchyEntry " + entry.getWorkspacePath() + " does not match ItemInfo " + info.getPath());
        }
    }

    /**
     * @param entry
     * @param degree
     * @return the ancestor entry at the specified degree.
     */
    private static NodeEntry getAncestor(HierarchyEntry entry, int degree) {
        NodeEntry parent = entry.getParent();
        degree--;
        while (parent != null && degree > 0) {
            parent = parent.getParent();
            degree--;
        }
        if (degree != 0) {
            log.error("Parent of degree {} does not exist.", degree);
            throw new IllegalArgumentException();
        }
        return parent;
    }
}
