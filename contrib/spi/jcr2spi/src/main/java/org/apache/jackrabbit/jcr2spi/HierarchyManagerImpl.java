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
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.ItemStateManager;
import org.apache.jackrabbit.jcr2spi.state.NoSuchItemStateException;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.state.ChildNodeEntry;
import org.apache.jackrabbit.jcr2spi.util.LogUtil;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.MalformedPathException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * <code>HierarchyManagerImpl</code> ...
 */
public class HierarchyManagerImpl implements HierarchyManager {

    private static Logger log = LoggerFactory.getLogger(HierarchyManagerImpl.class);

    // TODO: TO-BE-FIXED. With SPI_ItemId rootId must not be stored separately
    protected final NodeId rootNodeId;
    protected final ItemStateManager itemStateManager;
    // used for outputting user-friendly paths and names
    protected final NamespaceResolver nsResolver;

    public HierarchyManagerImpl(NodeId rootNodeId,
                                ItemStateManager itemStateManager,
                                NamespaceResolver nsResolver) {
        this.rootNodeId = rootNodeId;
        this.itemStateManager = itemStateManager;
        this.nsResolver = nsResolver;
    }

    public NodeId getRootNodeId() {
        return rootNodeId;
    }

    //---------------------------------------------------------< overridables >
    /**
     * Return an item state, given its item id.
     * <p/>
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
        return itemStateManager.getItemState(id);
    }

    /**
     * Determines whether an item state for a given item id exists.
     * <p/>
     * Low-level hook provided for specialized derived classes.
     *
     * @param id item id
     * @return <code>true</code> if an item state exists, otherwise
     *         <code>false</code>
     * @see ZombieHierarchyManager#hasItemState(ItemId)
     */
    protected boolean hasItemState(ItemId id) {
        return itemStateManager.hasItemState(id);
    }

    /**
     * Returns the <code>parentUUID</code> of the given item.
     * <p/>
     * Low-level hook provided for specialized derived classes.
     *
     * @param state item state
     * @return parent <code>NodeId</code> of the given item state
     * @see ZombieHierarchyManager#getParentId(ItemState)
     */
    protected NodeId getParentId(ItemState state) {
        return state.getParent().getNodeId();
    }

    /**
     *
     * @param state
     * @return
     */
    protected NodeState getParentState(ItemState state) {
        return state.getParent();
    }

    /**
     * Returns the <code>ChildNodeEntry</code> of <code>parent</code> with the
     * specified <code>uuid</code> or <code>null</code> if there's no such entry.
     * <p/>
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
     * <p/>
     * Low-level hook provided for specialized derived classes.
     *
     * @param parent node state
     * @param name   name of child node entry
     * @param index  index of child node entry
     * @return the <code>ChildNodeEntry</code> of <code>parent</code> with
     *         the specified <code>name</code> and <code>index</code> or
     *         <code>null</code> if there's no such entry.
     * @see ZombieHierarchyManager#getChildNodeEntry(NodeState, QName, int)
     */
    protected ChildNodeEntry getChildNodeEntry(NodeState parent,
                                               QName name,
                                               int index) {
        return parent.getChildNodeEntry(name, index);
    }

    /**
     * Resolve a path into an item state. Recursively invoked method that may be
     * overridden by some subclass to either return cached responses or add
     * response to cache.
     *
     * @param path  full path of item to resolve
     * @param state intermediate state
     * @param next  next path element index to resolve
     * @return the state of the item denoted by <code>path</code>
     */
    protected ItemState resolvePath(Path path, ItemState state, int next)
        throws PathNotFoundException, RepositoryException {

        Path.PathElement[] elements = path.getElements();
        if (elements.length == next) {
            return state;
        }
        Path.PathElement elem = elements[next];

        QName name = elem.getName();
        int index = elem.getNormalizedIndex();

        NodeState parentState = (NodeState) state;
        ItemState childState;

        if (parentState.hasChildNodeEntry(name, index)) {
            // child node
            ChildNodeEntry nodeEntry = getChildNodeEntry(parentState, name, index);
            try {
                childState = nodeEntry.getNodeState();
            } catch (ItemStateException e) {
                // should never occur
                throw new RepositoryException(e);
            }
        } else if (parentState.hasPropertyName(name)) {
            // property
            if (index > Path.INDEX_DEFAULT) {
                // properties can't have same name siblings
                throw new PathNotFoundException(LogUtil.safeGetJCRPath(path, nsResolver));
            } else if (next < elements.length - 1) {
                // property is not the last element in the path
                throw new PathNotFoundException(LogUtil.safeGetJCRPath(path, nsResolver));
            }
            try {
                childState = parentState.getPropertyState(name);
            } catch (ItemStateException e) {
                // should never occur
                throw new RepositoryException(e);
            }
        } else {
            // no such item
            throw new PathNotFoundException(LogUtil.safeGetJCRPath(path, nsResolver));
        }
        return resolvePath(path, childState, next + 1);
    }

    /**
     * Adds the path element of an item id to the path currently being built.
     * Recursively invoked method that may be overridden by some subclass to
     * either return cached responses or add response to cache. On exit,
     * <code>builder</code> contains the path of <code>state</code>.
     *
     * @param builder builder currently being used
     * @param state   item to find path of
     */
    protected void buildPath(Path.PathBuilder builder, ItemState state)
            throws ItemStateException, RepositoryException {

        // shortcut
        if (state.getId().equals(rootNodeId)) {
            builder.addRoot();
            return;
        }

        NodeState parentState = state.getParent();
        if (parentState == null) {
            String msg = "failed to build path of " + state.getId()
                    + ": orphaned item";
            log.debug(msg);
            throw new ItemNotFoundException(msg);
        }

        // recursively build path of parent
        buildPath(builder, parentState);

        if (state.isNode()) {
            NodeState nodeState = (NodeState) state;
            NodeId id = nodeState.getNodeId();
            ChildNodeEntry entry = getChildNodeEntry(parentState, id);
            if (entry == null) {
                String msg = "failed to build path of " + state.getId() + ": "
                        + parentState.getNodeId() + " has no child entry for "
                        + id;
                log.debug(msg);
                throw new ItemNotFoundException(msg);
            }
            // add to path
            if (entry.getIndex() == org.apache.jackrabbit.name.Path.INDEX_DEFAULT) {
                builder.addLast(entry.getName());
            } else {
                builder.addLast(entry.getName(), entry.getIndex());
            }
        } else {
            PropertyState propState = (PropertyState) state;
            QName name = propState.getQName();
            // add to path
            builder.addLast(name);
        }
    }

    //---------------------------------------------------< HierarchyManager >---
    /**
     * @see HierarchyManager#getItemState(Path)
     */
    public ItemState getItemState(Path qPath) throws PathNotFoundException, RepositoryException {
        try {
            ItemState rootState = itemStateManager.getItemState(rootNodeId);
            // shortcut
            if (qPath.denotesRoot()) {
                return rootState;
            }

            if (!qPath.isCanonical()) {
                String msg = "path is not canonical";
                log.debug(msg);
                throw new RepositoryException(msg);
            }

            return resolvePath(qPath, rootState, 1);
        } catch (ItemStateException e) {
            // should never occur
            throw new RepositoryException(e);
        }
    }

    /**
     * @see HierarchyManager#getQPath(ItemState)
     */
    public Path getQPath(ItemState itemState) throws ItemNotFoundException, RepositoryException {

        Path.PathBuilder builder = new Path.PathBuilder();
        try {
            buildPath(builder, itemState);
            return builder.getPath();
        } catch (NoSuchItemStateException e) {
            String msg = "failed to build path of " + itemState.getId();
            log.debug(msg);
            throw new ItemNotFoundException(msg, e);
        } catch (ItemStateException e) {
            String msg = "failed to build path of " + itemState.getId();
            log.debug(msg);
            throw new RepositoryException(msg, e);
        } catch (MalformedPathException e) {
            String msg = "failed to build path of " + itemState.getId();
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * @see HierarchyManager#getQName(ItemState)
     */
    public QName getQName(ItemState itemState)
            throws ItemNotFoundException, RepositoryException {
        if (itemState.isNode()) {
            NodeState parentState = itemState.getParent();
            if (parentState == null) {
                // shortcut. the given state represents the root or an orphaned node
                return QName.ROOT;
            }

            NodeId nodeId = ((NodeState)itemState).getNodeId();
            ChildNodeEntry entry = getChildNodeEntry(parentState, nodeId);
            if (entry == null) {
                String msg = "failed to resolve name of " + nodeId;
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            return entry.getName();
        } else {
            return ((PropertyState)itemState).getQName();
        }
    }

    /**
     * @see HierarchyManager#getDepth(ItemState)
     */
    public int getDepth(ItemState itemState) throws ItemNotFoundException, RepositoryException {
        int depth = Path.ROOT_DEPTH;
        NodeState parentState = getParentState(itemState);
        while (parentState != null) {
            depth++;
            itemState = parentState;
            parentState = getParentState(itemState);
        }
        return depth;
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
}

