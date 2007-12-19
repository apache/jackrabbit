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

import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

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
     * Path resolver for outputting user-friendly paths in error messages.
     */
    protected final PathResolver resolver;

    public HierarchyManagerImpl(NodeId rootNodeId,
                                ItemStateManager provider,
                                PathResolver resolver) {
        this.rootNodeId = rootNodeId;
        this.provider = provider;
        this.resolver = resolver;
    }

    public NodeId getRootNodeId() {
        return rootNodeId;
    }

    //-------------------------------------------------< misc. helper methods >

    /**
     * Failsafe conversion of internal <code>Path</code> to JCR path for use in
     * error messages etc.
     *
     * @param path path to convert
     * @return JCR path
     */
    public String safeGetJCRPath(Path path) {
        try {
            return resolver.getJCRPath(path);
        } catch (NamespaceException e) {
            log.error("failed to convert {} to a JCR path", path);
            // return string representation of internal path as a fallback
            return path.toString();
        }
    }

    /**
     * Failsafe translation of internal <code>ItemId</code> to JCR path for use
     * in error messages etc.
     *
     * @param id id to translate
     * @return JCR path
     */
    public String safeGetJCRPath(ItemId id) {
        try {
            return safeGetJCRPath(getPath(id));
        } catch (RepositoryException re) {
            log.error(id + ": failed to determine path to");
            // return string representation if id as a fallback
            return id.toString();
        }
    }

    //-------------------------------------------------------< implementation >
    /**
     * Recursively invoked method that resolves a path into an item id.
     *
     * @param path  full path of item to resolve
     * @param state intermediate state
     * @param next  next path element index to resolve
     * @param denotesNode flag indicating whether <code>path</code> refers to a
     *                    node (<code>true</code>) or a property (<code>false</code>)
     * @return the id of the item denoted by <code>path</code> or
     *         <code>null</code> if no item exists at <code>path</code>.
     * @throws ItemStateException if an error occured
     */
    private ItemId resolvePath(Path path, ItemState state, int next,
                                 boolean denotesNode)
            throws ItemStateException {

        // allow subclasses to process intermediate state
        beforeResolvePath(path, state, next);

        Path.Element[] elements = path.getElements();
        if (elements.length == next) {
            return state.getId();
        }
        Path.Element elem = elements[next];

        Name name = elem.getName();
        int index = elem.getIndex();
        if (index == 0) {
            index = 1;
        }

        NodeState parentState = (NodeState) state;
        if (next == elements.length - 1) {
            // last path element
            if (denotesNode) {
                if (parentState.hasChildNodeEntry(name, index)) {
                    // child node
                    NodeState.ChildNodeEntry nodeEntry =
                            getChildNodeEntry(parentState, name, index);
                    return nodeEntry.getId();
                }
            } else {
                if (parentState.hasPropertyName(name) && (index <= 1)) {
                    // property
                    return new PropertyId(parentState.getNodeId(), name);
                }
            }
            // no such itemn
            return null;
        }

        // intermediate path element
        ItemId childId;
        if (parentState.hasChildNodeEntry(name, index)) {
            // child node
            NodeState.ChildNodeEntry nodeEntry =
                    getChildNodeEntry(parentState, name, index);
            childId = nodeEntry.getId();
            // recurse
            return resolvePath(path, getItemState(childId), next + 1, denotesNode);
        }
        // no such item
        return null;
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
        return provider.getItemState(id);
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
        return provider.hasItemState(id);
    }

    /**
     * Returns the <code>parentUUID</code> of the given item.
     * <p/>
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
    protected NodeState.ChildNodeEntry getChildNodeEntry(NodeState parent,
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
     * @see ZombieHierarchyManager#getChildNodeEntry(NodeState, Name, int)
     */
    protected NodeState.ChildNodeEntry getChildNodeEntry(NodeState parent,
                                                         Name name,
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
     * @param denotesNode flag indicating whether <code>path</code> refers to a
     *                    node (<code>true</code>) or a property (<code>false</code>)
     * @return the id of the item denoted by <code>path</code>
     * @throws RepositoryException if an error occured
     */
    protected ItemId resolvePath(Path path, ItemId id, int next,
                                 boolean denotesNode)
            throws RepositoryException {

        try {
            return resolvePath(path, getItemState(id), next, denotesNode);
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
     * Called by recursively invoked method {@link #resolvePath(Path, ItemState, int, boolean)};
     * May be overridden by some subclass to process/cache intermediate state.
     *
     * @param path  full path of item to resolve
     * @param state intermediate state
     * @param next  next path element index to resolve
     */
    protected void beforeResolvePath(Path path, ItemState state, int next) {
        // do nothing
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
    protected void buildPath(PathBuilder builder, ItemState state)
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
        }

        NodeState parent = (NodeState) getItemState(parentId);
        // recursively build path of parent
        buildPath(builder, parent);

        if (state.isNode()) {
            NodeState nodeState = (NodeState) state;
            NodeId id = nodeState.getNodeId();
            NodeState.ChildNodeEntry entry = getChildNodeEntry(parent, id);
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

    //-----------------------------------------------------< HierarchyManager >
    /**
     * {@inheritDoc}
     */
    public ItemId resolvePath(Path path) throws RepositoryException {
        // shortcut
        if (path.denotesRoot()) {
            return rootNodeId;
        }

        if (!path.isCanonical()) {
            String msg = "path is not canonical";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        // first try to resolve node path, then property path
        ItemId id = resolvePath(path, rootNodeId, 1, true);
        return (id != null) ? id : resolvePath(path, rootNodeId, 1, false);
    }

    /**
     * {@inheritDoc}
     */
    public NodeId resolveNodePath(Path path) throws RepositoryException {
        return (NodeId) resolvePath(path, rootNodeId, 1, true);
    }

    /**
     * {@inheritDoc}
     */
    public PropertyId resolvePropertyPath(Path path) throws RepositoryException {
        return (PropertyId) resolvePath(path, rootNodeId, 1, false);
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
            NodeState parentState;
            try {
                NodeState nodeState = (NodeState) getItemState(nodeId);
                NodeId parentId = getParentId(nodeState);
                if (parentId == null) {
                    // this is the root or an orphaned node
                    // FIXME
                    return EMPTY_NAME;
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

            NodeState.ChildNodeEntry entry =
                    getChildNodeEntry(parentState, nodeId);
            if (entry == null) {
                String msg = "failed to resolve name of " + nodeId;
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            return entry.getName();
        } else {
            return ((PropertyId) itemId).getName();
        }
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
}

