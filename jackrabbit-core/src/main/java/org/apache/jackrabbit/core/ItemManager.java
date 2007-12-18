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
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.util.Dumpable;
import org.apache.jackrabbit.core.version.VersionHistoryImpl;
import org.apache.jackrabbit.core.version.VersionImpl;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Name;
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
 * The <code>ItemManager</code>'s responsabilities are:
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
public class ItemManager implements ItemLifeCycleListener, Dumpable, ItemStateListener {

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
    }

    /**
     * Disposes this <code>ItemManager</code> and frees resources.
     */
    void dispose() {
        synchronized (itemCache) {
            itemCache.clear();
        }
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
        if (!session.getAccessManager().isGranted(id, AccessManager.READ)) {
            // clear cache
            ItemImpl item = retrieveItem(id);
            if (item != null) {
                evictItem(id);
            }
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

    //--------------------------------------------------< item access methods >
    /**
     * Checks if the item with the given path exists.
     *
     * @param path path to the item to be checked
     * @return true if the specified item exists
     */
    public boolean itemExists(Path path) {
        try {
            // check sanity of session
            session.sanityCheck();

            ItemId id = hierMgr.resolvePath(path);
            if (id == null) {
                return false;
            }

            // check if state exists for the given item
            if (!itemStateProvider.hasItemState(id)) {
                return false;
            }

            // check privileges
            if (!session.getAccessManager().isGranted(id, AccessManager.READ)) {
                // clear cache
                evictItem(id);
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
            if (!session.getAccessManager().isGranted(id, AccessManager.READ)) {
                // clear cache
                evictItem(id);
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
     * @param id
     * @return
     * @throws RepositoryException
     */
    public synchronized ItemImpl getItem(ItemId id)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.sanityCheck();

        // check cache
        ItemImpl item = retrieveItem(id);
        if (item == null) {
            // not yet in cache, need to create instance:
            // check privileges
            if (!session.getAccessManager().isGranted(id, AccessManager.READ)) {
                throw new AccessDeniedException("cannot read item " + id);
            }
            // create instance of item
            item = createItemInstance(id);
        }
        return item;
    }

    /**
     * Returns the item instance for the given item state.
     * @param state the item state
     * @return the item instance for the given item <code>state</code>.
     * @throws RepositoryException
     */
    public synchronized ItemImpl getItem(ItemState state)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.sanityCheck();

        ItemId id = state.getId();
        // check cache
        ItemImpl item = retrieveItem(id);
        if (item == null) {
            // not yet in cache, need to create instance:
            // only check privileges if state is not new
            if (state.getStatus() != ItemState.STATUS_NEW
                    && !session.getAccessManager().isGranted(id, AccessManager.READ)) {
                throw new AccessDeniedException("cannot read item " + id);
            }
            // create instance of item
            item = createItemInstance(id);
        }
        return item;
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
            if (session.getAccessManager().isGranted(entry.getId(), AccessManager.READ)) {
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
            if (session.getAccessManager().isGranted(entry.getId(), AccessManager.READ)) {
                childIds.add(entry.getId());
            }
        }

        return new LazyItemIterator(this, childIds);
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
            if (session.getAccessManager().isGranted(new PropertyId(parentId, propName), AccessManager.READ)) {
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
            if (session.getAccessManager().isGranted(id, AccessManager.READ)) {
                childIds.add(id);
            }
        }

        return new LazyItemIterator(this, childIds);
    }

    //-------------------------------------------------< item factory methods >
    private ItemImpl createItemInstance(ItemId id)
            throws ItemNotFoundException, RepositoryException {
        // create instance of item using its state object
        ItemImpl item;
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

        if (id.equals(rootNodeId)) {
            // special handling required for root node
            item = createNodeInstance((NodeState) state, rootNodeDef);
        } else if (state.isNode()) {
            item = createNodeInstance((NodeState) state);
        } else {
            item = createPropertyInstance((PropertyState) state);
        }
        return item;
    }

    NodeImpl createNodeInstance(NodeState state, NodeDefinition def)
            throws RepositoryException {
        NodeId id = state.getNodeId();
        // we want to be informed on life cycle changes of the new node object
        // in order to maintain item cache consistency
        ItemLifeCycleListener[] listeners = new ItemLifeCycleListener[]{this};

        // check special nodes
        if (state.getNodeTypeName().equals(NameConstants.NT_VERSION)) {
            return createVersionInstance(id, state, def, listeners);

        } else if (state.getNodeTypeName().equals(NameConstants.NT_VERSIONHISTORY)) {
            return createVersionHistoryInstance(id, state, def, listeners);

        } else {
            // create node object
            return new NodeImpl(this, session, id, state, def, listeners);
        }
    }

    NodeImpl createNodeInstance(NodeState state) throws RepositoryException {
        // 1. get definition of the specified node
        NodeDefinition def = getDefinition(state);
        // 2. create instance
        return createNodeInstance(state, def);
    }

    PropertyImpl createPropertyInstance(PropertyState state,
                                        PropertyDefinition def) {
        // we want to be informed on life cycle changes of the new property object
        // in order to maintain item cache consistency
        ItemLifeCycleListener[] listeners = new ItemLifeCycleListener[]{this};
        // create property object
        return new PropertyImpl(
                this, session, state.getPropertyId(), state, def, listeners);
    }

    PropertyImpl createPropertyInstance(PropertyState state)
            throws RepositoryException {
        // 1. get definition for the specified property
        PropertyDefinition def = getDefinition(state);
        // 2. create instance
        return createPropertyInstance(state, def);
    }

    /**
     * Create a version instance.
     * @param id node id
     * @param state node state
     * @param def node definition
     * @param listeners listeners
     * @return version instance
     * @throws RepositoryException if an error occurs
     */
    protected VersionImpl createVersionInstance(
            NodeId id, NodeState state, NodeDefinition def,
            ItemLifeCycleListener[] listeners) throws RepositoryException {

        return new VersionImpl(this, session, id, state, def, listeners);
    }

    /**
     * Create a version history instance.
     * @param id node id
     * @param state node state
     * @param def node definition
     * @param listeners listeners
     * @return version instance
     * @throws RepositoryException if an error occurs
     */
    protected VersionHistoryImpl createVersionHistoryInstance(
            NodeId id, NodeState state, NodeDefinition def,
            ItemLifeCycleListener[] listeners) throws RepositoryException {

        return new VersionHistoryImpl(this, session, id, state, def, listeners);
    }

    //---------------------------------------------------< item cache methods >

    /**
     * Returns an item reference from the cache.
     *
     * @param id id of the item that should be retrieved.
     * @return the item reference stored in the corresponding cache entry
     *         or <code>null</code> if there's no corresponding cache entry.
     */
    private ItemImpl retrieveItem(ItemId id) {
        synchronized (itemCache) {
            return (ItemImpl) itemCache.get(id);
        }
    }

    /**
     * Puts the reference of an item in the cache with
     * the item's path as the key.
     *
     * @param item the item to cache
     */
    private void cacheItem(ItemImpl item) {
        synchronized (itemCache) {
            ItemId id = item.getId();
            if (itemCache.containsKey(id)) {
                log.warn("overwriting cached item " + id);
            }
            if (log.isDebugEnabled()) {
                log.debug("caching item " + id);
            }
            itemCache.put(id, item);
        }
    }

    /**
     * Removes a cache entry for a specific item.
     *
     * @param id id of the item to remove from the cache
     */
    private void evictItem(ItemId id) {
        if (log.isDebugEnabled()) {
            log.debug("removing item " + id + " from cache");
        }
        synchronized (itemCache) {
            itemCache.remove(id);
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
    public void itemCreated(ItemImpl item) {
        if (log.isDebugEnabled()) {
            log.debug("created item " + item.getId());
        }
        // add instance to cache
        cacheItem(item);
    }

    /**
     * {@inheritDoc}
     */
    public void itemInvalidated(ItemId id, ItemImpl item) {
        if (log.isDebugEnabled()) {
            log.debug("invalidated item " + id);
        }
        // remove instance from cache
        evictItem(id);
    }

    /**
     * {@inheritDoc}
     */
    public void itemDestroyed(ItemId id, ItemImpl item) {
        if (log.isDebugEnabled()) {
            log.debug("destroyed item " + id);
        }
        // we're no longer interested in this item
        item.removeLifeCycleListener(this);
        // remove instance from cache
        evictItem(id);
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
        ItemImpl item = retrieveItem(created.getId());
        if (item != null) {
            item.stateCreated(created);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stateModified(ItemState modified) {
        ItemImpl item = retrieveItem(modified.getId());
        if (item != null) {
            item.stateModified(modified);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stateDestroyed(ItemState destroyed) {
        ItemImpl item = retrieveItem(destroyed.getId());
        if (item != null) {
            item.stateDestroyed(destroyed);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stateDiscarded(ItemState discarded) {
        ItemImpl item = retrieveItem(discarded.getId());
        if (item != null) {
            item.stateDiscarded(discarded);
        }
    }
}
