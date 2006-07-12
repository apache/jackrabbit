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
import org.apache.jackrabbit.jcr2spi.util.Dumpable;
import org.apache.jackrabbit.jcr2spi.version.VersionHistoryImpl;
import org.apache.jackrabbit.jcr2spi.version.VersionImpl;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.PropertyId;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * There's one <code>ItemManagerImpl</code> instance per <code>Session</code>
 * instance. It is the factory for <code>Node</code> and <code>Property</code>
 * instances.
 * <p/>
 * The <code>ItemManagerImpl</code>'s responsabilities are:
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
 * one <code>ItemManagerImpl</code> instance per started global transaction.
 */
public class ItemManagerImpl implements Dumpable, ItemManager {

    private static Logger log = LoggerFactory.getLogger(ItemManagerImpl.class);

    private final SessionImpl session;

    private final ItemStateManager itemStateMgr;
    private final HierarchyManager hierMgr;

    /**
     * A cache for item instances created by this <code>ItemManagerImpl</code>
     */
    // TODO: TO-BE-FIXED. Usage of SPI-Id required refactoring of the cache
    private IdKeyMap itemCache;

    /**
     * Creates a new per-session instance <code>ItemManagerImpl</code> instance.
     *
     * @param itemStateMgr the item state itemStateManager associated with
     * the new instance
     * @param session the session associated with the new instance
     */
    ItemManagerImpl(ItemStateManager itemStateMgr, HierarchyManager hierMgr,
                    SessionImpl session) {
        this.itemStateMgr = itemStateMgr;
        this.hierMgr = hierMgr;
        this.session = session;
        // setup item cache with weak references to items
        itemCache = new DefaultIdKeyMap(); // TODO, JR: new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
    }

    private NodeDefinition getDefinition(NodeState state)
            throws RepositoryException {
        NodeId parentId = state.getParentId();
        NodeState parentState = null;
        if (parentId != null) {
            NodeImpl parent = (NodeImpl) getItem(parentId);
            parentState = (NodeState) parent.getItemState();
        }
        NodeDefinition def = session.getItemDefinitionManager().getNodeDefinition(state, parentState);
        return def;
    }

    private PropertyDefinition getDefinition(PropertyState state)
        throws RepositoryException {
        // fallback: try finding applicable definition
        NodeId parentId = state.getParentId();
        NodeImpl parent = (NodeImpl) getItem(parentId);
        NodeState parentState = (NodeState) parent.getItemState();
        PropertyDefinition def = session.getItemDefinitionManager().getPropertyDefinition(state, parentState);
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
        if (!session.getAccessManager().canRead(id)) {
            // clear cache
            ItemImpl item = retrieveItem(id);
            if (item != null) {
                evictItem(id);
            }
            throw new AccessDeniedException("cannot read item " + id);
        }

        try {
            return itemStateMgr.getItemState(id);
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

    //-------------------------------------------------< item factory methods >
    private ItemImpl createItemInstance(ItemId id)
            throws ItemNotFoundException, RepositoryException {
        // create instance of item using its state object
        ItemImpl item;
        ItemState state;
        try {
            state = itemStateMgr.getItemState(id);
        } catch (NoSuchItemStateException nsise) {
            throw new ItemNotFoundException(id.toString());
        } catch (ItemStateException ise) {
            String msg = "failed to retrieve item state of item " + id;
            log.error(msg, ise);
            throw new RepositoryException(msg, ise);
        }

        if (state.isNode()) {
            item = createNodeInstance((NodeState) state);
        } else {
            item = createPropertyInstance((PropertyState) state);
        }
        return item;
    }

    private NodeImpl createNodeInstance(NodeState state, NodeDefinition def)
            throws RepositoryException {
        // DIFF JR: no need to build NodeId from state
        // we want to be informed on life cycle changes of the new node object
        // in order to maintain item cache consistency
        ItemLifeCycleListener[] listeners = new ItemLifeCycleListener[]{this};

        // check special nodes
        if (state.getNodeTypeName().equals(QName.NT_VERSION)) {
            // version
            return new VersionImpl(this, session, state, def, listeners);
        } else if (state.getNodeTypeName().equals(QName.NT_VERSIONHISTORY)) {
            // version-history
            return new VersionHistoryImpl(this, session, state, def, listeners);
        } else {
            // create common node object
            return new NodeImpl(this, session, state, def, listeners);
        }

    }

    private NodeImpl createNodeInstance(NodeState state) throws RepositoryException {
        // 1. get definition of the specified node
        NodeDefinition def = getDefinition(state);
        // 2. create instance
        return createNodeInstance(state, def);
    }

    private PropertyImpl createPropertyInstance(PropertyState state,
                                                PropertyDefinition def) {
        // we want to be informed on life cycle changes of the new property object
        // in order to maintain item cache consistency
        ItemLifeCycleListener[] listeners = new ItemLifeCycleListener[]{this};
        // create property object
        PropertyImpl prop = new PropertyImpl(this, session, state, def, listeners);
        return prop;
    }

    private PropertyImpl createPropertyInstance(PropertyState state)
            throws RepositoryException {
        // 1. get definition for the specified property
        PropertyDefinition def = getDefinition(state);
        // 2. create instance
        return createPropertyInstance(state, def);
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
        return (ItemImpl) itemCache.get(id);
    }

    /**
     * Puts the reference of an item in the cache with
     * the item's path as the key.
     *
     * @param item the item to cache
     */
    private void cacheItem(ItemImpl item) {
        ItemId id = item.getId();
        if (itemCache.containsKey(id)) {
            log.warn("overwriting cached item " + id);
        }
        if (log.isDebugEnabled()) {
            log.debug("caching item " + id);
        }
        itemCache.put(id, item);
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
        itemCache.remove(id);
    }

    //--------------------------------------------------------< ItemManager >---
    /**
     * @inheritdoc
     */
    public void dispose() {
        itemCache.clear();
    }

    /**
     * @inheritDoc
     */
    public boolean itemExists(Path path) {
        try {
            // check sanity of session
            session.checkIsAlive();

            ItemId id = hierMgr.getItemId(path);

            // check if state exists for the given item
            if (!itemStateMgr.hasItemState(id)) {
                return false;
            }

            // check privileges
            if (!session.getAccessManager().canRead(id)) {
                // clear cache
                evictItem(id);
                // item exists but the session has not been granted read access
                return false;
            }
            return true;
        } catch (PathNotFoundException pnfe) {
            return false;
        } catch (ItemNotFoundException infe) {
            return false;
        } catch (RepositoryException re) {
            return false;
        }
    }

    /**
     * @inheritDoc
     */
    public boolean itemExists(ItemId id) {
        try {
            // check sanity of session
            session.checkIsAlive();

            // check if state exists for the given item
            if (!itemStateMgr.hasItemState(id)) {
                return false;
            }

            // check privileges
            if (!session.getAccessManager().canRead(id)) {
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
     * @inheritDoc
     */
    public synchronized ItemImpl getItem(org.apache.jackrabbit.name.Path path)
            throws PathNotFoundException, AccessDeniedException, RepositoryException {
        ItemId id = hierMgr.getItemId(path);
        try {
            return getItem(id);
        } catch (ItemNotFoundException infe) {
            throw new PathNotFoundException(hierMgr.safeGetJCRPath(path));
        }
    }

    /**
     * @inheritDoc
     */
    public synchronized ItemImpl getItem(ItemId id)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.checkIsAlive();

        // check privileges
        if (!session.getAccessManager().canRead(id)) {
            // clear cache
            evictItem(id);
            throw new AccessDeniedException("cannot read item " + id);
        }

        // check cache
        ItemImpl item = retrieveItem(id);
        if (item == null) {
            // create instance of item
            item = createItemInstance(id);
        }
        return item;
    }

    /**
     * @inheritDoc
     */
    public synchronized boolean hasChildNodes(NodeId parentId)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.checkIsAlive();

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
            NodeId id = entry.getId();
            // check read access
            if (session.getAccessManager().canRead(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @inheritDoc
     */
    public synchronized NodeIterator getChildNodes(NodeId parentId)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.checkIsAlive();

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
            NodeId id = entry.getId();
            // check read access
            if (session.getAccessManager().canRead(id)) {
                childIds.add(id);
            }
        }

        return new LazyItemIterator(this, childIds);
    }

    /**
     * @inheritDoc
     */
    public synchronized boolean hasChildProperties(NodeId parentId)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.checkIsAlive();

        ItemState state = getItemState(parentId);
        if (!state.isNode()) {
            String msg = "can't list child properties of property " + parentId;
            log.debug(msg);
            throw new RepositoryException(msg);
        }
        NodeState nodeState = (NodeState) state;
        Iterator iter = nodeState.getPropertyNames().iterator();

        while (iter.hasNext()) {
            QName propName = (QName) iter.next();

            PropertyId id = nodeState.getPropertyId(propName);
            // check read access
            if (session.getAccessManager().canRead(id)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @inheritDoc
     */
    public synchronized PropertyIterator getChildProperties(NodeId parentId)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.checkIsAlive();

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
            QName propName = (QName) iter.next();
            PropertyId id = nodeState.getPropertyId(propName);
            // check read access
            if (session.getAccessManager().canRead(id)) {
                childIds.add(id);
            }
        }

        return new LazyItemIterator(this, childIds);
    }

    //------------------------------------------------< ItemLifeCycleListener >
    /**
     * {@inheritDoc}
     */
    public void itemCreated(ItemId id, ItemImpl item) {
        if (log.isDebugEnabled()) {
            log.debug("created item " + id);
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
    public void dump(PrintStream ps) {
        ps.println("ItemManagerImpl (" + this + ")");
        ps.println();
        ps.println("Items in cache:");
        ps.println();
        Iterator iter = itemCache.keySet().iterator();
        while (iter.hasNext()) {
            ItemId id = (ItemId) iter.next();
            ItemImpl item = (ItemImpl) itemCache.get(id);
            if (item.isNode()) {
                ps.print("Node: ");
            } else {
                ps.print("Property: ");
            }
            if (item.getItemState().isTransient()) {
                ps.print("transient ");
            } else {
                ps.print("          ");
            }
            ps.println(id + "\t" + hierMgr.safeGetJCRPath(id) + " (" + item + ")");
        }
    }
}
