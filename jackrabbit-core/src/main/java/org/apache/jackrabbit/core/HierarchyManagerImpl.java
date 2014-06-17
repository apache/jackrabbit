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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>HierarchyManagerImpl</code> ...
 */
public class HierarchyManagerImpl implements HierarchyManager {

    private static Logger log = LoggerFactory.getLogger(HierarchyManagerImpl.class);

    /**
     * The parent name returned for orphaned or root nodes.
     * TODO: Is it proper to use an invalid Name for this.
     */
    private static final Name EMPTY_NAME = NameFactoryImpl.getInstance().create("", "");

    protected final NodeId rootNodeId;
    protected final ItemStateManager provider;

    /**
     * Flags describing what items to return in {@link #resolvePath(Path, int)}.
     */
    static final int RETURN_NODE = 1;
    static final int RETURN_PROPERTY = 2;
    static final int RETURN_ANY = (RETURN_NODE | RETURN_PROPERTY);

    public HierarchyManagerImpl(NodeId rootNodeId,
                                ItemStateManager provider) {
        this.rootNodeId = rootNodeId;
        this.provider = provider;
    }

    public NodeId getRootNodeId() {
        return rootNodeId;
    }

    //-------------------------------------------------------< implementation >

    /**
     * Internal implementation that iteratively resolves a path into an item.
     *
     * @param elements path elements
     * @param next index of next item in <code>elements</code> to inspect
     * @param id id of item at path <code>elements[0]</code>..<code>elements[next - 1]</code>
     * @param typesAllowed one of <code>RETURN_ANY</code>, <code>RETURN_NODE</code>
     *                     or <code>RETURN_PROPERTY</code>
     * @return id or <code>null</code>
     * @throws ItemStateException if an intermediate item state is not found
     * @throws MalformedPathException if building an intermediate path fails
     */
    protected ItemId resolvePath(Path.Element[] elements, int next,
                                 ItemId id, int typesAllowed)
            throws ItemStateException, MalformedPathException {

        PathBuilder builder = new PathBuilder();
        for (int i = 0; i < next; i++) {
            builder.addLast(elements[i]);
        }
        for (int i = next; i < elements.length; i++) {
            Path.Element elem = elements[i];
            NodeId parentId = (NodeId) id;
            id = null;

            Name name = elem.getName();
            int index = elem.getIndex();
            if (index == 0) {
                index = 1;
            }
            int typeExpected = typesAllowed;
            if (i < elements.length - 1) {
                // intermediate items must always be nodes
                typeExpected = RETURN_NODE;
            }
            NodeState parentState = (NodeState) getItemState(parentId);
            if ((typeExpected & RETURN_NODE) != 0) {
                ChildNodeEntry nodeEntry =
                        getChildNodeEntry(parentState, name, index);
                if (nodeEntry != null) {
                    id = nodeEntry.getId();
                }
            }
            if (id == null && (typeExpected & RETURN_PROPERTY) != 0) {
                if (parentState.hasPropertyName(name) && (index <= 1)) {
                    // property
                    id = new PropertyId(parentState.getNodeId(), name);
                }
            }
            if (id == null) {
                break;
            }
            builder.addLast(elements[i]);
            pathResolved(id, builder);
        }
        return id;
    }

    //---------------------------------------------------------< overridables >
    /**
     * Return an item state, given its item id.
     * <p>
     * Low-level hook provided for specialized derived classes.
     *
     * @param id item id
     * @return item state
     * @throws NoSuchItemStateException if the item does not exist
     * @throws ItemStateException       if an error occurs
     * @see ZombieHierarchyManager#getItemState(ItemId)
     */
    protected ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {
        return provider.getItemState(id);
    }

    /**
     * Determines whether an item state for a given item id exists.
     * <p>
     * Low-level hook provided for specialized derived classes.
     *
     * @param id item id
     * @return <code>true</code> if an item state exists, otherwise
     *         <code>false</code>
     * @see ZombieHierarchyManager#hasItemState(ItemId)
     */
    protected boolean hasItemState(ItemId id) {
        return provider.hasItemState(id);
    }

    /**
     * Returns the <code>parentUUID</code> of the given item.
     * <p>
     * Low-level hook provided for specialized derived classes.
     *
     * @param state item state
     * @return <code>parentUUID</code> of the given item
     * @see ZombieHierarchyManager#getParentId(ItemState)
     */
    protected NodeId getParentId(ItemState state) {
        return state.getParentId();
    }

    /**
     * Return all parents of a node. A shareable node has possibly more than
     * one parent.
     *
     * @param state item state
     * @param useOverlayed whether to use overlayed state for shareable nodes
     * @return set of parent <code>NodeId</code>s. If state has no parent,
     *         array has length <code>0</code>.
     */
    protected Set<NodeId> getParentIds(ItemState state, boolean useOverlayed) {
        if (state.isNode()) {
            // if this is a node, quickly check whether it is shareable and
            // whether it contains more than one parent
            NodeState ns = (NodeState) state;
            if (ns.isShareable() && useOverlayed && ns.hasOverlayedState()) {
                ns = (NodeState) ns.getOverlayedState();
            }
            Set<NodeId> s = ns.getSharedSet();
            if (s.size() > 1) {
                return s;
            }
        }
        NodeId parentId = getParentId(state);
        if (parentId != null) {
            LinkedHashSet<NodeId> s = new LinkedHashSet<NodeId>();
            s.add(parentId);
            return s;
        }
        return Collections.emptySet();
    }

    /**
     * Returns the <code>ChildNodeEntry</code> of <code>parent</code> with the
     * specified <code>uuid</code> or <code>null</code> if there's no such entry.
     * <p>
     * Low-level hook provided for specialized derived classes.
     *
     * @param parent node state
     * @param id   id of child node entry
     * @return the <code>ChildNodeEntry</code> of <code>parent</code> with
     *         the specified <code>uuid</code> or <code>null</code> if there's
     *         no such entry.
     * @see ZombieHierarchyManager#getChildNodeEntry(NodeState, NodeId)
     */
    protected ChildNodeEntry getChildNodeEntry(NodeState parent,
                                                         NodeId id) {
        return parent.getChildNodeEntry(id);
    }

    /**
     * Returns the <code>ChildNodeEntry</code> of <code>parent</code> with the
     * specified <code>name</code> and <code>index</code> or <code>null</code>
     * if there's no such entry.
     * <p>
     * Low-level hook provided for specialized derived classes.
     *
     * @param parent node state
     * @param name   name of child node entry
     * @param index  index of child node entry
     * @return the <code>ChildNodeEntry</code> of <code>parent</code> with
     *         the specified <code>name</code> and <code>index</code> or
     *         <code>null</code> if there's no such entry.
     * @see ZombieHierarchyManager#getChildNodeEntry(NodeState, Name, int)
     */
    protected ChildNodeEntry getChildNodeEntry(NodeState parent,
                                                         Name name,
                                                         int index) {
        return parent.getChildNodeEntry(name, index);
    }

    /**
     * Adds the path element of an item id to the path currently being built.
     * Recursively invoked method that may be overridden by some subclass to
     * either return cached responses or add response to cache. On exit,
     * <code>builder</code> contains the path of <code>state</code>.
     *
     * @param builder  builder currently being used
     * @param state    item to find path of
     * @param detector path cycle detector
     */
    protected void buildPath(
            PathBuilder builder, ItemState state, CycleDetector detector)
            throws ItemStateException, RepositoryException {

        // shortcut
        if (state.getId().equals(rootNodeId)) {
            builder.addRoot();
            return;
        }

        NodeId parentId = getParentId(state);
        if (parentId == null) {
            String msg = "failed to build path of " + state.getId()
                    + ": orphaned item";
            log.debug(msg);
            throw new ItemNotFoundException(msg);
        } else if (detector.checkCycle(parentId)) {
            throw new InvalidItemStateException(
                    "Path cycle detected: " + parentId);
        }

        NodeState parent = (NodeState) getItemState(parentId);
        // recursively build path of parent
        buildPath(builder, parent, detector);

        if (state.isNode()) {
            NodeState nodeState = (NodeState) state;
            NodeId id = nodeState.getNodeId();
            ChildNodeEntry entry = getChildNodeEntry(parent, id);
            if (entry == null) {
                String msg = "failed to build path of " + state.getId() + ": "
                        + parent.getNodeId() + " has no child entry for "
                        + id;
                log.debug(msg);
                throw new ItemNotFoundException(msg);
            }
            // add to path
            if (entry.getIndex() == 1) {
                builder.addLast(entry.getName());
            } else {
                builder.addLast(entry.getName(), entry.getIndex());
            }
        } else {
            PropertyState propState = (PropertyState) state;
            Name name = propState.getName();
            // add to path
            builder.addLast(name);
        }
    }

    /**
     * Internal implementation of {@link #resolvePath(Path)} that will either
     * resolve to a node or a property. Should be overridden by a subclass
     * that can resolve an intermediate path into an <code>ItemId</code>. This
     * subclass can then invoke {@link #resolvePath(org.apache.jackrabbit.spi.Path.Element[], int, ItemId, int)}
     * with a value of <code>next</code> greater than <code>1</code>.
     *
     * @param path path to resolve
     * @param typesAllowed one of <code>RETURN_ANY</code>, <code>RETURN_NODE</code>
     *                     or <code>RETURN_PROPERTY</code>
     * @return id or <code>null</code>
     * @throws RepositoryException if an error occurs
     */
    protected ItemId resolvePath(Path path, int typesAllowed)
            throws RepositoryException {

        Path.Element[] elements = path.getElements();
        ItemId id = rootNodeId;

        try {
            return resolvePath(elements, 1, id, typesAllowed);
        } catch (ItemStateException e) {
            String msg = "failed to retrieve state of intermediary node";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * Called by {@link #resolvePath(org.apache.jackrabbit.spi.Path.Element[], int, ItemId, int)}.
     * May be overridden by some subclass to process/cache intermediate state.
     *
     * @param id      id of resolved item
     * @param builder path builder containing path resolved
     * @throws MalformedPathException if the path contained in <code>builder</code>
     *                                is malformed
     */
    protected void pathResolved(ItemId id, PathBuilder builder)
            throws MalformedPathException {

        // do nothing
    }

    //-----------------------------------------------------< HierarchyManager >

    /**
     * {@inheritDoc}
     */
    public final ItemId resolvePath(Path path) throws RepositoryException {
        // shortcut
        if (path.denotesRoot()) {
            return rootNodeId;
        }
        if (!path.isCanonical()) {
            String msg = "path is not canonical";
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        return resolvePath(path, RETURN_ANY);
    }

    /**
     * {@inheritDoc}
     */
    public NodeId resolveNodePath(Path path) throws RepositoryException {
        return (NodeId) resolvePath(path, RETURN_NODE);
    }

    /**
     * {@inheritDoc}
     */
    public PropertyId resolvePropertyPath(Path path) throws RepositoryException {
        return (PropertyId) resolvePath(path, RETURN_PROPERTY);
    }

    /**
     * {@inheritDoc}
     */
    public Path getPath(ItemId id)
            throws ItemNotFoundException, RepositoryException {
        // shortcut
        if (id.equals(rootNodeId)) {
            return PathFactoryImpl.getInstance().getRootPath();
        }

        PathBuilder builder = new PathBuilder();

        try {
            buildPath(builder, getItemState(id), new CycleDetector());
            return builder.getPath();
        } catch (NoSuchItemStateException nsise) {
            String msg = "failed to build path of " + id;
            log.debug(msg);
            throw new ItemNotFoundException(msg, nsise);
        } catch (ItemStateException ise) {
            String msg = "failed to build path of " + id;
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        } catch (MalformedPathException mpe) {
            String msg = "failed to build path of " + id;
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Name getName(ItemId itemId)
            throws ItemNotFoundException, RepositoryException {
        if (itemId.denotesNode()) {
            NodeId nodeId = (NodeId) itemId;
            try {
                NodeState nodeState = (NodeState) getItemState(nodeId);
                NodeId parentId = getParentId(nodeState);
                if (parentId == null) {
                    // this is the root or an orphaned node
                    // FIXME
                    return EMPTY_NAME;
                }
                return getName(nodeId, parentId);
            } catch (NoSuchItemStateException nsis) {
                String msg = "failed to resolve name of " + nodeId;
                log.debug(msg);
                throw new ItemNotFoundException(nodeId.toString());
            } catch (ItemStateException ise) {
                String msg = "failed to resolve name of " + nodeId;
                log.debug(msg);
                throw new RepositoryException(msg, ise);
            }
        } else {
            return ((PropertyId) itemId).getName();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Name getName(NodeId id, NodeId parentId)
            throws ItemNotFoundException, RepositoryException {

        NodeState parentState;

        try {
            parentState = (NodeState) getItemState(parentId);
        } catch (NoSuchItemStateException nsis) {
            String msg = "failed to resolve name of " + id;
            log.debug(msg);
            throw new ItemNotFoundException(id.toString());
        } catch (ItemStateException ise) {
            String msg = "failed to resolve name of " + id;
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }

        ChildNodeEntry entry =
                getChildNodeEntry(parentState, id);
        if (entry == null) {
            String msg = "failed to resolve name of " + id;
            log.debug(msg);
            throw new ItemNotFoundException(msg);
        }
        return entry.getName();
    }

    /**
     * {@inheritDoc}
     */
    public int getDepth(ItemId id)
            throws ItemNotFoundException, RepositoryException {
        // shortcut
        if (id.equals(rootNodeId)) {
            return 0;
        }
        try {
            ItemState state = getItemState(id);
            NodeId parentId = getParentId(state);
            int depth = 0;
            while (parentId != null) {
                depth++;
                state = getItemState(parentId);
                parentId = getParentId(state);
            }
            return depth;
        } catch (NoSuchItemStateException nsise) {
            String msg = "failed to determine depth of " + id;
            log.debug(msg);
            throw new ItemNotFoundException(msg, nsise);
        } catch (ItemStateException ise) {
            String msg = "failed to determine depth of " + id;
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getRelativeDepth(NodeId ancestorId, ItemId descendantId)
            throws ItemNotFoundException, RepositoryException {
        if (ancestorId.equals(descendantId)) {
            return 0;
        }
        int depth = 1;
        try {
            ItemState state = getItemState(descendantId);
            NodeId parentId = getParentId(state);
            while (parentId != null) {
                if (parentId.equals(ancestorId)) {
                    return depth;
                }
                depth++;
                state = getItemState(parentId);
                parentId = getParentId(state);
            }
            // not an ancestor
            return -1;
        } catch (NoSuchItemStateException nsise) {
            String msg = "failed to determine depth of " + descendantId
                    + " relative to " + ancestorId;
            log.debug(msg);
            throw new ItemNotFoundException(msg, nsise);
        } catch (ItemStateException ise) {
            String msg = "failed to determine depth of " + descendantId
                    + " relative to " + ancestorId;
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAncestor(NodeId nodeId, ItemId itemId)
            throws ItemNotFoundException, RepositoryException {
        if (nodeId.equals(itemId)) {
            // can't be ancestor of self
            return false;
        }
        try {
            ItemState state = getItemState(itemId);
            NodeId parentId = getParentId(state);
            while (parentId != null) {
                if (parentId.equals(nodeId)) {
                    return true;
                }
                state = getItemState(parentId);
                parentId = getParentId(state);
            }
            // not an ancestor
            return false;
        } catch (NoSuchItemStateException nsise) {
            String msg = "failed to determine degree of relationship of "
                    + nodeId + " and " + itemId;
            log.debug(msg);
            throw new ItemNotFoundException(msg, nsise);
        } catch (ItemStateException ise) {
            String msg = "failed to determine degree of relationship of "
                    + nodeId + " and " + itemId;
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isShareAncestor(NodeId ancestor, NodeId descendant)
            throws ItemNotFoundException, RepositoryException {
        if (ancestor.equals(descendant)) {
            // can't be ancestor of self
            return false;
        }
        try {
            ItemState state = getItemState(descendant);
            Set<NodeId> parentIds = getParentIds(state, false);
            while (parentIds.size() > 0) {
                if (parentIds.contains(ancestor)) {
                    return true;
                }
                Set<NodeId> grandparentIds = new LinkedHashSet<NodeId>();
                for (NodeId parentId : parentIds) {
                    grandparentIds.addAll(getParentIds(getItemState(parentId), false));
                }
                parentIds = grandparentIds;
            }
            // not an ancestor
            return false;
        } catch (NoSuchItemStateException nsise) {
            String msg = "failed to determine degree of relationship of "
                    + ancestor + " and " + descendant;
            log.debug(msg);
            throw new ItemNotFoundException(msg, nsise);
        } catch (ItemStateException ise) {
            String msg = "failed to determine degree of relationship of "
                    + ancestor + " and " + descendant;
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getShareRelativeDepth(NodeId ancestor, ItemId descendant)
            throws ItemNotFoundException, RepositoryException {

        if (ancestor.equals(descendant)) {
            return 0;
        }
        int depth = 1;
        try {
            ItemState state = getItemState(descendant);
            Set<NodeId> parentIds = getParentIds(state, true);
            while (parentIds.size() > 0) {
                if (parentIds.contains(ancestor)) {
                    return depth;
                }
                depth++;
                Set<NodeId> grandparentIds = new LinkedHashSet<NodeId>();
                for (NodeId parentId : parentIds) {
                    state = getItemState(parentId);
                    grandparentIds.addAll(getParentIds(state, true));
                }
                parentIds = grandparentIds;
            }
            // not an ancestor
            return -1;
        } catch (NoSuchItemStateException nsise) {
            String msg = "failed to determine degree of relationship of "
                    + ancestor + " and " + descendant;
            log.debug(msg);
            throw new ItemNotFoundException(msg, nsise);
        } catch (ItemStateException ise) {
            String msg = "failed to determine degree of relationship of "
                    + ancestor + " and " + descendant;
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    /**
     * Utility class used to detect path cycles with as little overhead
     * as possible. The {@link #checkCycle(ItemId)} method is called for
     * each path element as the
     * {@link HierarchyManagerImpl#buildPath(PathBuilder, ItemState, CycleDetector)}
     * method walks up the hierarchy. At first, during the first fifteen
     * path elements, the detector does nothing in order to avoid
     * introducing any unnecessary overhead to normal paths that seldom
     * are deeper than that. After that initial threshold all item
     * identifiers along the path are tracked, and a cycle is reported
     * if an identifier is encountered that already occurred along the
     * same path.
     */
    protected static class CycleDetector {

        private int count = 0;

        private Set<ItemId> ids;

        boolean checkCycle(ItemId id) throws InvalidItemStateException {
            if (count++ >= 15) {
                if (ids == null) {
                    ids = new HashSet<ItemId>();
                } else {
                    return !ids.add(id);
                }
            }
            return false;
        }

    }

}
