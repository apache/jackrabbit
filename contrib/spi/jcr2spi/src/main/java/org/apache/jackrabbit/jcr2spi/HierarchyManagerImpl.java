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
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.name.PathFormat;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Item;

/**
 * <code>HierarchyManagerImpl</code> ...
 */
public class HierarchyManagerImpl implements HierarchyManager {

    private static Logger log = LoggerFactory.getLogger(HierarchyManagerImpl.class);

    // DIFF JR: QName.ROOT replaces the EMPTY_NAME QName defined in JR....

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

    public NamespaceResolver getNamespaceResolver() {
        return nsResolver;
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
     * Resolve a path into an item id. Recursively invoked method that may be
     * overridden by some subclass to either return cached responses or add
     * response to cache.
     *
     * @param path full path of item to resolve
     * @param id   intermediate item id
     * @param next next path element index to resolve
     * @return the id of the item denoted by <code>path</code>
     */
    protected ItemId resolvePath(Path path, ItemId id, int next)
            throws RepositoryException {

        try {
            return resolvePath(path, getItemState(id), next);
        } catch (NoSuchItemStateException e) {
            String msg = "failed to retrieve state of intermediary node";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        } catch (ItemStateException e) {
            String msg = "failed to retrieve state of intermediary node";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * Resolve a path into an item id. Recursively invoked method that may be
     * overridden by some subclass to either return cached responses or add
     * response to cache.
     *
     * @param path  full path of item to resolve
     * @param state intermediate state
     * @param next  next path element index to resolve
     * @return the id of the item denoted by <code>path</code>
     */
    protected ItemId resolvePath(Path path, ItemState state, int next)
            throws PathNotFoundException, ItemStateException {

        Path.PathElement[] elements = path.getElements();
        if (elements.length == next) {
            return state.getId();
        }
        Path.PathElement elem = elements[next];

        QName name = elem.getName();
        int index = elem.getNormalizedIndex();

        NodeState parentState = (NodeState) state;
        ItemId childId;

        if (parentState.hasChildNodeEntry(name, index)) {
            // child node
            ChildNodeEntry nodeEntry = getChildNodeEntry(parentState, name, index);
            childId = nodeEntry.getId();
        } else if (parentState.hasPropertyName(name)) {
            // property
            if (index > org.apache.jackrabbit.name.Path.INDEX_DEFAULT) {
                // properties can't have same name siblings
                throw new PathNotFoundException(safeGetJCRPath(path));
            } else if (next < elements.length - 1) {
                // property is not the last element in the path
                throw new PathNotFoundException(safeGetJCRPath(path));
            }
            childId = parentState.getPropertyState(name).getId();
        } else {
            // no such item
            throw new PathNotFoundException(safeGetJCRPath(path));
        }
        return resolvePath(path, getItemState(childId), next + 1);
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

    //-----------------------------------------------------< HierarchyManager >
    /**
     * {@inheritDoc}
     */
    public ItemId getItemId(Item item) throws PathNotFoundException, RepositoryException {
        if (item instanceof ItemImpl) {
            return ((ItemImpl)item).getId();
        } else {
            try {
                return getItemId(PathFormat.parse(item.getPath(), nsResolver));
            } catch (MalformedPathException e) {
                // should not occur.
                throw new RepositoryException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public ItemId getItemId(Path qPath)
            throws PathNotFoundException, RepositoryException {
        // shortcut
        if (qPath.denotesRoot()) {
            return rootNodeId;
        }

        if (!qPath.isCanonical()) {
            String msg = "path is not canonical";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        return resolvePath(qPath, rootNodeId, 1);
    }

    /**
     * {@inheritDoc}
     */
    public Path getQPath(ItemId id)
            throws ItemNotFoundException, RepositoryException {
        // shortcut
        if (id.equals(rootNodeId)) {
            return Path.ROOT;
        }

        Path.PathBuilder builder = new Path.PathBuilder();

        try {
            buildPath(builder, getItemState(id));
            return builder.getPath();
        } catch (NoSuchItemStateException nsise) {
            String msg = "failed to build path of " + id;
            log.debug(msg);
            throw new ItemNotFoundException(msg, nsise);
        } catch (ItemStateException ise) {
            String msg = "failed to build path of " + id;
            log.debug(msg);
            throw new RepositoryException(msg, ise);
        } catch (MalformedPathException e) {
            String msg = "failed to build path of " + id;
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public QName getQName(ItemId itemId)
            throws ItemNotFoundException, RepositoryException {
        if (itemId.denotesNode()) {
            NodeId nodeId = (NodeId) itemId;
            NodeState parentState;
            try {
                NodeState nodeState = (NodeState) getItemState(nodeId);
                NodeId parentId= getParentId(nodeState);
                if (parentId == null) {
                    // this is the root or an orphaned node
                    return QName.ROOT;
                }
                parentState = (NodeState) getItemState(parentId);
            } catch (NoSuchItemStateException nsis) {
                String msg = "failed to resolve name of " + nodeId;
                log.debug(msg);
                throw new ItemNotFoundException(nodeId.toString());
            } catch (ItemStateException ise) {
                String msg = "failed to resolve name of " + nodeId;
                log.debug(msg);
                throw new RepositoryException(msg, ise);
            }

            ChildNodeEntry entry =
                    getChildNodeEntry(parentState, nodeId);
            if (entry == null) {
                String msg = "failed to resolve name of " + nodeId;
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            return entry.getName();
        } else {
            return ((PropertyId) itemId).getQName();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getDepth(ItemId id)
            throws ItemNotFoundException, RepositoryException {
        int depth = org.apache.jackrabbit.name.Path.ROOT_DEPTH;
        // shortcut
        if (id.equals(rootNodeId)) {
            return depth;
        }
        try {
            ItemState state = getItemState(id);
            NodeId parentId = getParentId(state);
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
     * @see HierarchyManager#safeGetJCRPath(ItemId)
     */
    public String safeGetJCRPath(ItemId itemId) {
        try {
            return safeGetJCRPath(getQPath(itemId));
        } catch (RepositoryException e) {
            log.error("failed to convert " + itemId + " to JCR path.");
            return itemId.toString();
        }
    }

    /**
     * @see HierarchyManager#safeGetJCRPath(Path)
     */
    public String safeGetJCRPath(Path qPath) {
        try {
            return PathFormat.format(qPath, nsResolver);
        } catch (NoPrefixDeclaredException npde) {
            log.error("failed to convert " + qPath + " to JCR path.");
            // return string representation of internal path as a fallback
            return qPath.toString();
        }
    }
}

