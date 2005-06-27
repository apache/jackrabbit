/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import org.apache.log4j.Logger;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * <code>HierarchyManagerImpl</code> ...
 */
public class HierarchyManagerImpl implements HierarchyManager {

    private static Logger log = Logger.getLogger(HierarchyManagerImpl.class);

    protected final NodeId rootNodeId;
    protected final ItemStateManager provider;
    // used for outputting user-friendly paths and names
    protected final NamespaceResolver nsResolver;

    public HierarchyManagerImpl(String rootNodeUUID,
                                ItemStateManager provider,
                                NamespaceResolver nsResolver) {
        rootNodeId = new NodeId(rootNodeUUID);
        this.provider = provider;
        this.nsResolver = nsResolver;
    }

    public NodeId getRootNodeId() {
        return rootNodeId;
    }

    public NamespaceResolver getNamespaceResolver() {
        return nsResolver;
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
            return path.toJCRPath(nsResolver);
        } catch (NoPrefixDeclaredException npde) {
            log.error("failed to convert " + path.toString() + " to JCR path.");
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

    //---------------------------------------------------------< overridables >
    /**
     * @param id
     * @return
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    protected ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {
        return provider.getItemState(id);
    }

    /**
     * @param id
     * @return
     */
    protected boolean hasItemState(ItemId id) {
        return provider.hasItemState(id);
    }

    /**
     * Resolve a path into an item id. Recursively invoked method that may be
     * overridden by some subclass to either return cached responses or add
     * response to cache.
     *
     * @param path full path of item to resolve
     * @param id   item id
     * @param next next path element index to resolve
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
     */
    protected ItemId resolvePath(Path path, ItemState state, int next)
            throws PathNotFoundException, ItemStateException {

        Path.PathElement[] elements = path.getElements();
        if (elements.length == next) {
            return state.getId();
        }
        Path.PathElement elem = elements[next];

        QName name = elem.getName();
        int index = elem.getIndex();
        if (index == 0) {
            index = 1;
        }

        NodeState parentState = (NodeState) state;
        ItemId childId;

        if (parentState.hasChildNodeEntry(name, index)) {
            // child node
            NodeState.ChildNodeEntry nodeEntry =
                    parentState.getChildNodeEntry(name, index);
            childId = new NodeId(nodeEntry.getUUID());

        } else if (parentState.hasPropertyName(name)) {
            // property
            if (index > 1) {
                // properties can't have same name siblings
                throw new PathNotFoundException(safeGetJCRPath(path));

            } else if (next < elements.length - 1) {
                // property is not the last element in the path
                throw new PathNotFoundException(safeGetJCRPath(path));
            }

            childId = new PropertyId(parentState.getUUID(), name);

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

        String parentUUID = state.getParentUUID();
        if (parentUUID == null) {
            builder.addRoot();
            return;
        }

        NodeState parent = (NodeState) getItemState(new NodeId(parentUUID));
        // recursively build path of parent
        buildPath(builder, parent);

        if (state.isNode()) {
            NodeState nodeState = (NodeState) state;
            String uuid = nodeState.getUUID();
            NodeState.ChildNodeEntry entry = parent.getChildNodeEntry(uuid);
            if (entry == null) {
                String msg = "failed to build path of " + state.getId() + ": "
                        + parent.getUUID() + " has no child entry for "
                        + uuid;
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
            QName name = propState.getName();
            // add to path
            builder.addLast(name);
        }
    }

    //-----------------------------------------------------< HierarchyManager >
    /**
     * {@inheritDoc}
     */
    public synchronized ItemId resolvePath(Path path)
            throws PathNotFoundException, RepositoryException {
        // shortcut
        if (path.denotesRoot()) {
            return rootNodeId;
        }

        if (!path.isCanonical()) {
            String msg = "path is not canonical";
            log.debug(msg);
            throw new RepositoryException(msg);
        }

        return resolvePath(path, rootNodeId, 1);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Path getPath(ItemId id)
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
        } catch (MalformedPathException mpe) {
            String msg = "failed to build path of " + id;
            log.debug(msg);
            throw new RepositoryException(msg, mpe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public QName getName(ItemId itemId)
            throws ItemNotFoundException, RepositoryException {
        if (itemId.denotesNode()) {
            NodeId nodeId = (NodeId) itemId;
            NodeState parentState;
            if (!hasItemState(nodeId)) {
                String msg = "failed to resolve name of " + nodeId;
                log.debug(msg);
                throw new ItemNotFoundException(nodeId.toString());
            }
            try {
                NodeState nodeState = (NodeState) getItemState(nodeId);
                String parentUUID = nodeState.getParentUUID();
                if (parentUUID == null) {
                    // this is the root or an orphaned node
                    // FIXME
                    return new QName(Constants.NS_DEFAULT_URI, "");
                }
                parentState = (NodeState) getItemState(new NodeId(parentUUID));
            } catch (ItemStateException ise) {
                String msg = "failed to resolve name of " + nodeId;
                log.debug(msg);
                throw new RepositoryException(msg, ise);
            }

            NodeState.ChildNodeEntry entry =
                    parentState.getChildNodeEntry(nodeId.getUUID());
            if (entry == null) {
                String msg = "failed to resolve name of " + nodeId;
                log.debug(msg);
                throw new RepositoryException(msg);
            }
            return entry.getName();
        } else {
            PropertyId propId = (PropertyId) itemId;
            return propId.getName();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getDepth(ItemId id)
            throws ItemNotFoundException, RepositoryException {
        try {
            ItemState state = getItemState(id);
            String parentUUID = state.getParentUUID();
            if (parentUUID != null) {
                return getDepth(new NodeId(parentUUID)) + 1;
            }
            return 0;
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
    public boolean isAncestor(NodeId nodeId, ItemId itemId)
            throws ItemNotFoundException, RepositoryException {
        try {
            ItemState state = getItemState(itemId);
            String parentUUID = state.getParentUUID();
            if (parentUUID != null) {
                if (parentUUID.equals(nodeId.getUUID())) {
                    return true;
                }
                return isAncestor(nodeId, new NodeId(parentUUID));
            }
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

