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

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.NodeDefinitionImpl;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.nodetype.PropertyDefinitionImpl;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.util.Dumpable;
import org.apache.jackrabbit.core.version.VersionHistoryImpl;
import org.apache.jackrabbit.core.version.VersionImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * There's one <code>ItemManager</code> instance per <code>Session</code>
 * instance. It is the factory for <code>Node</code> and <code>Property</code>
 * instances.
 * <p/>
 * The <code>ItemManager</code>'s responsibilities are:
 * <ul>
 * <li>providing access to <code>Item</code> instances by <code>ItemId</code>
 * whereas <code>Node</code> and <code>Item</code> are only providing relative access.
 * <li>returning the instance of an existing <code>Node</code> or <code>Property</code>,
 * given its absolute path.
 * <li>creating the per-session instance of a <code>Node</code>
 * or <code>Property</code> that doesn't exist yet and needs to be created first.
 * <li>guaranteeing that there aren't multiple instances representing the same
 * <code>Node</code> or <code>Property</code> associated with the same
 * <code>Session</code> instance.
 * <li>maintaining a cache of the item instances it created.
 * <li>respecting access rights of associated <code>Session</code> in all methods.
 * </ul>
 * <p/>
 * If the parent <code>Session</code> is an <code>XASession</code>, there is
 * one <code>ItemManager</code> instance per started global transaction.
 */
public class ItemManager implements Dumpable, ItemStateListener {

    private static Logger log = LoggerFactory.getLogger(ItemManager.class);

    private final NodeDefinition rootNodeDef;
    private final NodeId rootNodeId;

    protected final SessionImpl session;

    private final ItemStateManager itemStateProvider;
    private final HierarchyManager hierMgr;

    /**
     * A cache for item instances created by this <code>ItemManager</code>
     */
    private final Map itemCache;

    /**
     * Shareable node cache.
     */
    private final ShareableNodesCache shareableNodesCache;

    /**
     * Creates a new per-session instance <code>ItemManager</code> instance.
     *
     * @param itemStateProvider the item state provider associated with
     *                          the new instance
     * @param hierMgr           the hierarchy manager
     * @param session           the session associated with the new instance
     * @param rootNodeDef       the definition of the root node
     * @param rootNodeId        the id of the root node
     */
    protected ItemManager(SessionItemStateManager itemStateProvider, HierarchyManager hierMgr,
                          SessionImpl session, NodeDefinition rootNodeDef,
                          NodeId rootNodeId) {
        this.itemStateProvider = itemStateProvider;
        this.hierMgr = hierMgr;
        this.session = session;
        this.rootNodeDef = rootNodeDef;
        this.rootNodeId = rootNodeId;

        // setup item cache with weak references to items
        itemCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
        itemStateProvider.addListener(this);

        // setup shareable nodes cache
        shareableNodesCache = new ShareableNodesCache();
    }

    /**
     * Disposes this <code>ItemManager</code> and frees resources.
     */
    void dispose() {
        synchronized (itemCache) {
            itemCache.clear();
        }
        shareableNodesCache.clear();
    }

    private NodeDefinition getDefinition(NodeState state)
            throws RepositoryException {
        NodeDefId defId = state.getDefinitionId();
        NodeDefinitionImpl def = session.getNodeTypeManager().getNodeDefinition(defId);
        if (def == null) {
            /**
             * todo need proper way of handling inconsistent/corrupt definition
             * e.g. 'flag' items that refer to non-existent definitions
             */
            log.warn("node at " + safeGetJCRPath(state.getNodeId())
                    + " has invalid definitionId (" + defId + ")");

            // fallback: try finding applicable definition
            NodeImpl parent = (NodeImpl) getItem(state.getParentId());
            NodeState parentState = (NodeState) parent.getItemState();
            NodeState.ChildNodeEntry cne = parentState.getChildNodeEntry(state.getNodeId());
            def = parent.getApplicableChildNodeDefinition(cne.getName(), state.getNodeTypeName());
            state.setDefinitionId(def.unwrap().getId());
        }
        return def;
    }

    private PropertyDefinition getDefinition(PropertyState state)
            throws RepositoryException {
        PropDefId defId = state.getDefinitionId();
        PropertyDefinitionImpl def = session.getNodeTypeManager().getPropertyDefinition(defId);
        if (def == null) {
            /**
             * todo need proper way of handling inconsistent/corrupt definition
             * e.g. 'flag' items that refer to non-existent definitions
             */
            log.warn("property at " + safeGetJCRPath(state.getPropertyId())
                    + " has invalid definitionId (" + defId + ")");

            // fallback: try finding applicable definition
            NodeImpl parent = (NodeImpl) getItem(state.getParentId());
            def = parent.getApplicablePropertyDefinition(
                    state.getName(), state.getType(), state.isMultiValued(), true);
            state.setDefinitionId(def.unwrap().getId());
        }
        return def;
    }

    /**
     * Retrieves state of item with given <code>id</code>. If the specified item
     * doesn't exist an <code>ItemNotFoundException</code> will be thrown.
     * If the item exists but the current session is not granted read access an
     * <code>AccessDeniedException</code> will be thrown.
     *
     * @param id id of item to be retrieved
     * @return state state of said item
     * @throws ItemNotFoundException if no item with given <code>id</code> exists
     * @throws AccessDeniedException if the current session is not allowed to
     *                               read the said item
     * @throws RepositoryException   if another error occurs
     */
    private ItemState getItemState(ItemId id)
            throws ItemNotFoundException, AccessDeniedException,
            RepositoryException {
        // check privileges
        if (!canRead(id)) {
            // clear cache
            evictItems(id);
            throw new AccessDeniedException("cannot read item " + id);
        }

        try {
            return itemStateProvider.getItemState(id);
        } catch (NoSuchItemStateException nsise) {
            String msg = "no such item: " + id;
            log.debug(msg);
            throw new ItemNotFoundException(msg);
        } catch (ItemStateException ise) {
            String msg = "failed to retrieve item state of " + id;
            log.error(msg);
            throw new RepositoryException(msg, ise);
        }
    }

    private boolean canRead(ItemId id) throws RepositoryException {
        return session.getAccessManager().isGranted(id, AccessManager.READ);
    }

    //--------------------------------------------------< item access methods >
    /**
     * Checks whether an item exists at the specified path.
     *
     * @deprecated As of JSR 283, a <code>Path</code> doesn't anymore uniquely
     * identify an <code>Item</code>, therefore {@link #nodeExists(Path)} and
     * {@link #propertyExists(Path)} should be used instead.
     *
     * @param path path to the item to be checked
     * @return true if the specified item exists
     */
    public boolean itemExists(Path path) {
        try {
            // check sanity of session
            session.sanityCheck();

            ItemId id = hierMgr.resolvePath(path);
            return (id != null) && itemExists(id);
        } catch (RepositoryException re) {
            return false;
        }
    }

    /**
     * Checks whether a node exists at the specified path.
     *
     * @param path path to the node to be checked
     * @return true if a node exists at the specified path
     */
    public boolean nodeExists(Path path) {
        try {
            // check sanity of session
            session.sanityCheck();

            NodeId id = hierMgr.resolveNodePath(path);
            return (id != null) && itemExists(id);
        } catch (RepositoryException re) {
            return false;
        }
    }

    /**
     * Checks whether a property exists at the specified path.
     *
     * @param path path to the property to be checked
     * @return true if a property exists at the specified path
     */
    public boolean propertyExists(Path path) {
        try {
            // check sanity of session
            session.sanityCheck();

            PropertyId id = hierMgr.resolvePropertyPath(path);
            return (id != null) && itemExists(id);
        } catch (RepositoryException re) {
            return false;
        }
    }

    /**
     * Checks if the item with the given id exists.
     *
     * @param id id of the item to be checked
     * @return true if the specified item exists
     */
    public boolean itemExists(ItemId id) {
        try {
            // check sanity of session
            session.sanityCheck();

            // check if state exists for the given item
            if (!itemStateProvider.hasItemState(id)) {
                return false;
            }

            // check privileges
            if (!canRead(id)) {
                // clear cache
                evictItems(id);
                // item exists but the session has not been granted read access
                return false;
            }
            return true;
        } catch (ItemNotFoundException infe) {
            return false;
        } catch (RepositoryException re) {
            return false;
        }
    }

    /**
     * @return
     * @throws RepositoryException
     */
    NodeImpl getRootNode() throws RepositoryException {
        return (NodeImpl) getItem(rootNodeId);
    }

    /**
     * Returns the node at the specified absolute path in the workspace.
     * If no such node exists, then it returns the property at the specified path.
     * If no such property exists a <code>PathNotFoundException</code> is thrown.
     *
     * @deprecated As of JSR 283, a <code>Path</code> doesn't anymore uniquely
     * identify an <code>Item</code>, therefore {@link #getNode(Path)} and
     * {@link #getProperty(Path)} should be used instead.
     * @param path
     * @return
     * @throws PathNotFoundException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    public ItemImpl getItem(Path path)
            throws PathNotFoundException, AccessDeniedException, RepositoryException {
        ItemId id = hierMgr.resolvePath(path);
        if (id == null) {
            throw new PathNotFoundException(safeGetJCRPath(path));
        }
        try {
            return getItem(id);
        } catch (ItemNotFoundException infe) {
            throw new PathNotFoundException(safeGetJCRPath(path));
        }
    }

    /**
     * @param path
     * @return
     * @throws PathNotFoundException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    public NodeImpl getNode(Path path)
            throws PathNotFoundException, AccessDeniedException, RepositoryException {
        NodeId id = hierMgr.resolveNodePath(path);
        if (id == null) {
            throw new PathNotFoundException(safeGetJCRPath(path));
        }
        try {
            return (NodeImpl) getItem(id);
        } catch (ItemNotFoundException infe) {
            throw new PathNotFoundException(safeGetJCRPath(path));
        }
    }

    /**
     * @param path
     * @return
     * @throws PathNotFoundException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    public PropertyImpl getProperty(Path path)
            throws PathNotFoundException, AccessDeniedException, RepositoryException {
        PropertyId id = hierMgr.resolvePropertyPath(path);
        if (id == null) {
            throw new PathNotFoundException(safeGetJCRPath(path));
        }
        try {
            return (PropertyImpl) getItem(id);
        } catch (ItemNotFoundException infe) {
            throw new PathNotFoundException(safeGetJCRPath(path));
        }
    }

    /**
     * @param id
     * @return
     * @throws RepositoryException
     */
    public synchronized ItemImpl getItem(ItemId id)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.sanityCheck();

        ItemData data = retrieveItem(id);
        if (data == null) {
            // not yet in cache, need to create instance:
            // check privileges
            if (!canRead(id)) {
                throw new AccessDeniedException("cannot read item " + id);
            }
            // create instance of item data
            data = createItemData(id);
            cacheItem(data);
        }
        return createItemInstance(data);
    }

    /**
     * Returns a node with a given id and parent id. If the indicated node is
     * shareable, there might be multiple nodes associated with the same id,
     * but there'is only one node with the given parent id.
     *
     * @param id node id
     * @param parentId parent node id
     * @return node
     * @throws RepositoryException if an error occurs
     */
    public synchronized NodeImpl getNode(NodeId id, NodeId parentId)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {

        if (parentId == null) {
            return (NodeImpl) getItem(id);
        }
        AbstractNodeData data = (AbstractNodeData) retrieveItem(id, parentId);
        if (data == null) {
            data = (AbstractNodeData) createItemData(id);
            cacheItem(data);
        }
        if (!data.getParentId().equals(parentId)) {
            // verify that parent actually appears in the shared set
            if (!data.getNodeState().containsShare(parentId)) {
                String msg = "Node with id '" + id
                        + "' does not have shared parent with id: " + parentId;
                throw new ItemNotFoundException(msg);
            }
            data = new NodeDataRef(data, parentId);
            cacheItem(data);
        }
        return createNodeInstance(data);
    }

    /**
     * Returns the item instance for the given item id.
     *
     * @param state the item state
     * @param checkAccess whether to check access
     * @return the item instance for the given item <code>state</code>.
     * @throws RepositoryException
     */
    synchronized ItemImpl getItem(ItemId id, boolean isNew)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.sanityCheck();

        // check cache
        ItemData data = retrieveItem(id);
        if (data == null) {
            // not yet in cache, need to create instance:
            // only check privileges if state is not new
            if (!isNew && !canRead(id)) {
                throw new AccessDeniedException("cannot read item " + id);
            }
            // create instance of item
            data = createItemData(id);
            cacheItem(data);
        }
        return createItemInstance(data);
    }

    /**
     * Create an item instance from an item state. This method creates a
     * new <code>ItemData</code> instance without looking at the cache and
     * returns a new item instance.
     *
     * @param state item state
     * @return item instance
     * @throws RepositoryException if an error occurs
     */
    synchronized ItemImpl createItemInstance(ItemState state)
            throws RepositoryException {

        ItemData data = createItemData(state);
        cacheItem(data);
        return createItemInstance(data);
    }

    /**
     * @param parentId
     * @return
     * @throws ItemNotFoundException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    synchronized boolean hasChildNodes(NodeId parentId)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.sanityCheck();

        ItemState state = getItemState(parentId);
        if (!state.isNode()) {
            String msg = "can't list child nodes of property " + parentId;
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        NodeState nodeState = (NodeState) state;
        Iterator iter = nodeState.getChildNodeEntries().iterator();

        while (iter.hasNext()) {
            NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
            // check read access
            if (canRead(entry.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param parentId
     * @return
     * @throws ItemNotFoundException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    synchronized NodeIterator getChildNodes(NodeId parentId)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.sanityCheck();

        ItemState state = getItemState(parentId);
        if (!state.isNode()) {
            String msg = "can't list child nodes of property " + parentId;
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        NodeState nodeState = (NodeState) state;
        ArrayList childIds = new ArrayList();
        Iterator iter = nodeState.getChildNodeEntries().iterator();

        while (iter.hasNext()) {
            NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
            // check read access
            if (canRead(entry.getId())) {
                childIds.add(entry.getId());
            }
        }

        return new LazyItemIterator(this, childIds, parentId);
    }

    /**
     * @param parentId
     * @return
     * @throws ItemNotFoundException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    synchronized boolean hasChildProperties(NodeId parentId)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.sanityCheck();

        ItemState state = getItemState(parentId);
        if (!state.isNode()) {
            String msg = "can't list child properties of property " + parentId;
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        NodeState nodeState = (NodeState) state;
        Iterator iter = nodeState.getPropertyNames().iterator();

        while (iter.hasNext()) {
            Name propName = (Name) iter.next();
            // check read access
            if (canRead(new PropertyId(parentId, propName))) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param parentId
     * @return
     * @throws ItemNotFoundException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    synchronized PropertyIterator getChildProperties(NodeId parentId)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.sanityCheck();

        ItemState state = getItemState(parentId);
        if (!state.isNode()) {
            String msg = "can't list child properties of property " + parentId;
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        NodeState nodeState = (NodeState) state;
        ArrayList childIds = new ArrayList();
        Iterator iter = nodeState.getPropertyNames().iterator();

        while (iter.hasNext()) {
            Name propName = (Name) iter.next();
            PropertyId id = new PropertyId(parentId, propName);
            // check read access
            if (canRead(id)) {
                childIds.add(id);
            }
        }

        return new LazyItemIterator(this, childIds);
    }

    //-------------------------------------------------< item factory methods >

    private ItemData createItemData(ItemId id)
            throws ItemNotFoundException, RepositoryException {

        ItemState state;
        try {
            state = itemStateProvider.getItemState(id);
        } catch (NoSuchItemStateException nsise) {
            throw new ItemNotFoundException(id.toString());
        } catch (ItemStateException ise) {
            String msg = "failed to retrieve item state of item " + id;
            log.error(msg, ise);
            throw new RepositoryException(msg, ise);
        }
        return createItemData(state);
    }

    private ItemData createItemData(ItemState state) throws RepositoryException {
        ItemId id = state.getId();
        if (id.equals(rootNodeId)) {
            // special handling required for root node
            return new NodeData((NodeState) state, rootNodeDef);
        } else if (state.isNode()) {
            NodeState nodeState = (NodeState) state;
            return new NodeData(nodeState, getDefinition(nodeState));
        } else {
            PropertyState propertyState = (PropertyState) state;
            return new PropertyData(propertyState, getDefinition(propertyState));
        }
    }

    private ItemImpl createItemInstance(ItemData data) {
        if (data.isNode()) {
            return createNodeInstance((AbstractNodeData) data);
        } else {
            return createPropertyInstance((PropertyData) data);
        }
    }

    private NodeImpl createNodeInstance(AbstractNodeData data) {
        // check special nodes
        final NodeState state = data.getNodeState();
        if (state.getNodeTypeName().equals(NameConstants.NT_VERSION)) {
            return new VersionImpl(this, session, data);
        } else if (state.getNodeTypeName().equals(NameConstants.NT_VERSIONHISTORY)) {
            return new VersionHistoryImpl(this, session, data);
        } else {
            // create node object
            return new NodeImpl(this, session, data);
        }
    }

    private PropertyImpl createPropertyInstance(PropertyData data) {
        // check special nodes
        return new PropertyImpl(this, session, data);
    }

    //---------------------------------------------------< item cache methods >

    /**
     * Returns an item reference from the cache.
     *
     * @param id id of the item that should be retrieved.
     * @return the item reference stored in the corresponding cache entry
     *         or <code>null</code> if there's no corresponding cache entry.
     */
    private ItemData retrieveItem(ItemId id) {
        synchronized (itemCache) {
            ItemData data = (ItemData) itemCache.get(id);
            if (data == null && id.denotesNode()) {
                data = shareableNodesCache.retrieveFirst((NodeId) id);
            }
            return data;
        }
    }

    /**
     * Return a node from the cache.
     *
     * @param id id of the node that should be retrieved.
     * @param parentId parent id of the node that should be retrieved
     * @return reference stored in the corresponding cache entry
     *         or <code>null</code> if there's no corresponding cache entry.
     */
    private AbstractNodeData retrieveItem(NodeId id, NodeId parentId) {
        synchronized (itemCache) {
            AbstractNodeData data = shareableNodesCache.retrieve(id, parentId);
            if (data == null) {
                data = (AbstractNodeData) itemCache.get(id);
            }
            return data;
        }
    }

    /**
     * Puts the reference of an item in the cache with
     * the item's path as the key.
     *
     * @param item the item to cache
     */
    private void cacheItem(ItemData data) {
        synchronized (itemCache) {
            if (data.isNode()) {
                AbstractNodeData nd = (AbstractNodeData) data;
                if (nd.getPrimaryParentId() != null) {
                    shareableNodesCache.cache(nd);
                    return;
                }
            }
            ItemId id = data.getId();
            if (itemCache.containsKey(id)) {
                log.warn("overwriting cached item " + id);
            }
            if (log.isDebugEnabled()) {
                log.debug("caching item " + id);
            }
            itemCache.put(id, data);
        }
    }

    /**
     * Removes all cache entries with the given item id. If the item is
     * shareable, there might be more than one cache entry for this item.
     *
     * @param id id of the items to remove from the cache
     * @return <code>true</code> if the item was contained in this cache,
     *         <code>false</code> otherwise.
     */
    private void evictItems(ItemId id) {
        if (log.isDebugEnabled()) {
            log.debug("removing items " + id + " from cache");
        }
        synchronized (itemCache) {
            itemCache.remove(id);
            if (id.denotesNode()) {
                shareableNodesCache.evictAll((NodeId) id);
            }
        }
    }

    /**
     * Removes a cache entry for a specific item.
     *
     * @param id id of the item to remove from the cache
     */
    private void evictItem(ItemData data) {
        if (log.isDebugEnabled()) {
            log.debug("removing item " + data.getId() + " from cache");
        }
        synchronized (itemCache) {
            if (data.isNode()) {
                shareableNodesCache.evict((AbstractNodeData) data);
            }
            ItemData cached = (ItemData) itemCache.get(data.getId());
            if (cached == data) {
                itemCache.remove(data.getId());
            }
        }
    }


    //-------------------------------------------------< misc. helper methods >
    /**
     * Failsafe conversion of internal <code>Path</code> to JCR path for use in
     * error messages etc.
     *
     * @param path path to convert
     * @return JCR path
     */
    String safeGetJCRPath(Path path) {
        try {
            return session.getJCRPath(path);
        } catch (NamespaceException e) {
            log.error("failed to convert " + path.toString() + " to JCR path.");
            // return string representation of internal path as a fallback
            return path.toString();
        }
    }

    /**
     * Failsafe translation of internal <code>ItemId</code> to JCR path for use in
     * error messages etc.
     *
     * @param id path to convert
     * @return JCR path
     */
    String safeGetJCRPath(ItemId id) {
        try {
            return safeGetJCRPath(hierMgr.getPath(id));
        } catch (RepositoryException re) {
            log.error(id + ": failed to determine path to");
            // return string representation if id as a fallback
            return id.toString();
        }
    }

    //------------------------------------------------< ItemLifeCycleListener >

    /**
     * {@inheritDoc}
     */
    public void itemInvalidated(ItemId id, ItemData data) {
        if (log.isDebugEnabled()) {
            log.debug("invalidated item " + id);
        }
        evictItem(data);
    }

    /**
     * {@inheritDoc}
     */
    public void itemDestroyed(ItemId id, ItemData data) {
        if (log.isDebugEnabled()) {
            log.debug("destroyed item " + id);
        }
        synchronized (itemCache) {
            // remove instance from cache
            evictItems(id);
        }
    }

    //-------------------------------------------------------------< Dumpable >
    /**
     * {@inheritDoc}
     */
    public synchronized void dump(PrintStream ps) {
        ps.println("ItemManager (" + this + ")");
        ps.println();
        ps.println("Items in cache:");
        ps.println();
        synchronized (itemCache) {
            Iterator iter = itemCache.keySet().iterator();
            while (iter.hasNext()) {
                ItemId id = (ItemId) iter.next();
                ItemImpl item = (ItemImpl) itemCache.get(id);
                if (item.isNode()) {
                    ps.print("Node: ");
                } else {
                    ps.print("Property: ");
                }
                if (item.isTransient()) {
                    ps.print("transient ");
                } else {
                    ps.print("          ");
                }
                ps.println(id + "\t" + item.safeGetJCRPath() + " (" + item + ")");
            }
        }
    }

    //----------------------------------------------------< ItemStateListener >

    /**
     * {@inheritDoc}
     */
    public void stateCreated(ItemState created) {
        ItemData data = retrieveItem(created.getId());
        if (data != null) {
            data.setStatus(ItemImpl.STATUS_NORMAL);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stateModified(ItemState modified) {
        ItemData data = retrieveItem(modified.getId());
        if (data != null && data.getState() == modified) {
            data.setStatus(ItemImpl.STATUS_MODIFIED);
            /*
            if (modified.isNode()) {
                NodeState state = (NodeState) modified;
                if (state.isShareable()) {
                    //evictItem(modified.getId());
                    NodeData nodeData = (NodeData) data;
                    NodeData shareSibling = new NodeData(nodeData, state.getParentId());
                    shareableNodesCache.cache(shareSibling);
                }
            }
            */
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stateDestroyed(ItemState destroyed) {
        ItemData data = retrieveItem(destroyed.getId());
        if (data != null && data.getState() == destroyed) {
            itemDestroyed(destroyed.getId(), data);

            data.setStatus(ItemImpl.STATUS_DESTROYED);
            data.setState(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stateDiscarded(ItemState discarded) {
        ItemData data = retrieveItem(discarded.getId());
        if (data != null && data.getState() == discarded) {
            if (discarded.isTransient()) {
                switch (discarded.getStatus()) {
                /**
                 * persistent item that has been transiently removed
                 */
                case ItemState.STATUS_EXISTING_REMOVED:
                case ItemState.STATUS_EXISTING_MODIFIED:
                case ItemState.STATUS_STALE_MODIFIED:
                    ItemState persistentState = discarded.getOverlayedState();
                    /**
                     * the state is a transient wrapper for the underlying
                     * persistent state, therefore restore the persistent state
                     * and resurrect this item instance if necessary
                     */
                    SessionItemStateManager stateMgr = session.getItemStateManager();
                    stateMgr.disconnectTransientItemState(discarded);
                    data.setState(persistentState);
                    return;

                    /**
                     * persistent item that has been transiently modified or
                     * removed and the underlying persistent state has been
                     * externally destroyed since the transient
                     * modification/removal.
                     */
                case ItemState.STATUS_STALE_DESTROYED:
                    /**
                     * first notify the listeners that this instance has been
                     * permanently invalidated
                     */
                    itemDestroyed(discarded.getId(), data);
                    // now set state of this instance to 'destroyed'
                    data.setStatus(ItemImpl.STATUS_DESTROYED);
                    data.setState(null);
                    return;

                    /**
                     * new item that has been transiently added
                     */
                case ItemState.STATUS_NEW:
                    /**
                     * first notify the listeners that this instance has been
                     * permanently invalidated
                     */
                    itemDestroyed(discarded.getId(), data);
                    // now set state of this instance to 'destroyed'
                    // finally dispose state
                    data.setStatus(ItemImpl.STATUS_DESTROYED);
                    data.setState(null);
                    return;
                }
            }

            /**
             * first notify the listeners that this instance has been
             * invalidated
             */
            itemInvalidated(discarded.getId(), data);
            // now render this instance 'invalid'
            data.setStatus(ItemImpl.STATUS_INVALIDATED);
        }
    }

    /**
     * Cache of shareable nodes. For performance reasons, methods are not
     * synchronized and thread-safety must be guaranteed by caller.
     */
    class ShareableNodesCache {

        /**
         * This cache is based on a reference map, that maps an item id to a map,
         * which again maps a (hard-ref) parent id to a (weak-ref) shareable node.
         */
        private final ReferenceMap cache;

        /**
         * Create a new instance of this class.
         */
        public ShareableNodesCache() {
            cache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.HARD);
        }

        /**
         * Clear cache.
         *
         * @see ReferenceMap#clear()
         */
        public void clear() {
            cache.clear();
        }

        /**
         * Return the first available node that maps to the given id.
         *
         * @param id node id
         * @return node or <code>null</code>
         */
        public AbstractNodeData retrieveFirst(NodeId id) {
            ReferenceMap map = (ReferenceMap) cache.get(id);
            if (map != null) {
                Iterator iter = map.values().iterator();
                try {
                    while (iter.hasNext()) {
                        AbstractNodeData data = (AbstractNodeData) iter.next();
                        if (data != null) {
                            return data;
                        }
                    }
                } finally {
                    iter = null;
                }
            }
            return null;
        }

        /**
         * Return the node with the given id and parent id.
         *
         * @param id node id
         * @param parentId parent id
         * @return node or <code>null</code>
         */
        public AbstractNodeData retrieve(NodeId id, NodeId parentId) {
            ReferenceMap map = (ReferenceMap) cache.get(id);
            if (map != null) {
                return (AbstractNodeData) map.get(parentId);
            }
            return null;
        }

        /**
         * Cache some node.
         *
         * @param node node to cache
         */
        public void cache(AbstractNodeData data) {
            NodeId id = data.getNodeState().getNodeId();
            ReferenceMap map = (ReferenceMap) cache.get(id);
            if (map == null) {
                map = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
                cache.put(id, map);
            }
            Object old = map.put(data.getPrimaryParentId(), data);
            if (old != null) {
                log.warn("overwriting cached item: " + old);
            }
        }

        /**
         * Evict some node from the cache.
         *
         * @param node node to evict
         */
        public void evict(AbstractNodeData data) {
            ReferenceMap map = (ReferenceMap) cache.get(data.getId());
            if (map != null) {
                map.remove(data.getPrimaryParentId());
            }
        }

        /**
         * Evict all nodes with a given node id from the cache.
         *
         * @param id node id to evict
         */
        public synchronized void evictAll(NodeId id) {
            cache.remove(id);
        }
    }
}
