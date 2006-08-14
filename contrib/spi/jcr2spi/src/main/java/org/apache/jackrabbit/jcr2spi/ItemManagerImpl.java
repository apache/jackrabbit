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
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.state.ChildNodeEntry;
import org.apache.jackrabbit.jcr2spi.state.ChildPropertyEntry;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.util.Dumpable;
import org.apache.jackrabbit.jcr2spi.util.LogUtil;
import org.apache.jackrabbit.jcr2spi.version.VersionHistoryImpl;
import org.apache.jackrabbit.jcr2spi.version.VersionImpl;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.commons.collections.map.ReferenceMap;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Item;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;

/**
 * There's one <code>ItemManagerImpl</code> instance per <code>Session</code>
 * instance. It is the factory for <code>Node</code> and <code>Property</code>
 * instances.
 * <p/>
 * The <code>ItemManagerImpl</code>'s responsabilities are:
 * <ul>
 * <li>providing access to <code>Item</code> instances by <code>ItemState</code>
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

    private final HierarchyManager hierMgr;

    /**
     * A cache for item instances created by this <code>ItemManagerImpl</code>.
     * 
     * The <code>ItemState</code>s act as keys for the map. In contrast to
     * o.a.j.core the item state are copied to transient space for reading and
     * will therefor not change upon transient modifications.
     */
    private Map itemCache;

    /**
     * Creates a new per-session instance <code>ItemManagerImpl</code> instance.
     *
     * @param hierMgr HierarchyManager associated with the new instance
     * @param session the session associated with the new instance
     */
    ItemManagerImpl(HierarchyManager hierMgr, SessionImpl session) {
        this.hierMgr = hierMgr;
        this.session = session;
        /* Setup item cache with weak keys (ItemState) and weak values (Item).*/
        itemCache = new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK);
    }

    //--------------------------------------------------------< ItemManager >---
    /**
     * @see ItemManager#dispose()
     */
    public void dispose() {
        itemCache.clear();
    }

    /**
     * @see ItemManager#itemExists(Path)
     */
    public boolean itemExists(Path path) {
        try {
            // check sanity of session
            session.checkIsAlive();
            // permissions are checked upon itemExists(ItemState)

            ItemState itemState = hierMgr.getItemState(path);
            return itemExists(itemState);
        } catch (PathNotFoundException pnfe) {
            return false;
        } catch (ItemNotFoundException infe) {
            return false;
        } catch (RepositoryException re) {
            return false;
        }
    }

    /**
     * @see ItemManager#itemExists(ItemState)
     */
    public boolean itemExists(ItemState itemState) {
        try {
            // check sanity of session
            session.checkIsAlive();
            // check privileges
            checkAccess(itemState, true);

            // always return true if access rights are granted, existence
            // of the state has been asserted before
            return true;
        } catch (ItemNotFoundException infe) {
            return false;
        } catch (RepositoryException re) {
            return false;
        }
    }

    /**
     * @see ItemManager#getItem(Path)
     */
    public synchronized Item getItem(Path path)
            throws PathNotFoundException, AccessDeniedException, RepositoryException {
        ItemState itemState = hierMgr.getItemState(path);
        try {
            return getItem(itemState);
        } catch (ItemNotFoundException infe) {
            throw new PathNotFoundException(LogUtil.safeGetJCRPath(path, session.getNamespaceResolver()));
        }
    }

    /**
     * @see ItemManager#getItem(ItemState)
     */
    public Item getItem(ItemState itemState) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // first try to access item from cache
        Item item = retrieveItem(itemState);
        // not yet in cache, need to create instance
        if (item == null) {
            // check privileges
            checkAccess(itemState, false);
            // create instance of item
            if (itemState.isNode()) {
                item = createNodeInstance((NodeState) itemState);
            } else {
                item = createPropertyInstance((PropertyState) itemState);
            }
        }
        return item;
    }

    /**
     * @see ItemManager#hasChildNodes(NodeState)
     */
    public synchronized boolean hasChildNodes(NodeState parentState)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.checkIsAlive();
        checkAccess(parentState, true);

        Iterator iter = parentState.getChildNodeEntries().iterator();
        while (iter.hasNext()) {
            try {
                // check read access
                ChildNodeEntry entry = (ChildNodeEntry) iter.next();
                if (session.getAccessManager().canRead(entry.getNodeState())) {
                    return true;
                }
            } catch (ItemStateException e) {
                // should not occur. ignore
                log.debug("Failed to access node state.", e);
            }
        }
        return false;
    }

    /**
     * @see ItemManager#getChildNodes(NodeState)
     */
    public synchronized NodeIterator getChildNodes(NodeState parentState)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.checkIsAlive();
        checkAccess(parentState, true);

        return new LazyItemIterator(this, parentState.getChildNodeEntries());
    }

    /**
     * @see ItemManager#hasChildProperties(NodeState)
     */
    public synchronized boolean hasChildProperties(NodeState parentState)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.checkIsAlive();
        checkAccess(parentState, true);

        Iterator iter = parentState.getPropertyEntries().iterator();
        while (iter.hasNext()) {
            try {
                ChildPropertyEntry entry = (ChildPropertyEntry) iter.next();
                // check read access
                if (session.getAccessManager().canRead(entry.getPropertyState())) {
                    return true;
                }
            } catch (ItemStateException e) {
                // should not occur. ignore
                log.debug("Failed to access node state.", e);
            }
        }
        return false;
    }

    /**
     * @see ItemManager#getChildProperties(NodeState)
     */
    public synchronized PropertyIterator getChildProperties(NodeState parentState)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.checkIsAlive();
        checkAccess(parentState, true);

        return new LazyItemIterator(this, parentState.getPropertyEntries());
    }

    //----------------------------------------------< ItemLifeCycleListener >---
    /**
     * @see ItemLifeCycleListener#itemCreated(Item)
     */
    public void itemCreated(Item item) {
        if (!(item instanceof ItemImpl)) {
            String msg = "Incompatible Item object: " + ItemImpl.class.getName() + " expected.";
            throw new IllegalArgumentException(msg);
        }
        if (log.isDebugEnabled()) {
            log.debug("created item " + item);
        }
        // add instance to cache
        cacheItem(((ItemImpl)item).getItemState(), item);
    }

    /**
     * @see ItemLifeCycleListener#itemInvalidated(Item)
     */
    public void itemInvalidated(Item item) {
        if (!(item instanceof ItemImpl)) {
            String msg = "Incompatible Item object: " + ItemImpl.class.getName() + " expected.";
            throw new IllegalArgumentException(msg);
        }
        if (log.isDebugEnabled()) {
            log.debug("invalidated item " + item);
        }
        // remove instance from cache
        evictItem(((ItemImpl)item).getItemState());
    }

    /**
     * @see ItemLifeCycleListener#itemDestroyed(Item)
     */
    public void itemDestroyed(Item item) {
        if (!(item instanceof ItemImpl)) {
            String msg = "Incompatible Item object: " + ItemImpl.class.getName() + " expected.";
            throw new IllegalArgumentException(msg);
        }
        if (log.isDebugEnabled()) {
            log.debug("destroyed item " + item);
        }
        // we're no longer interested in this item
        ((ItemImpl)item).removeLifeCycleListener(this);
        // remove instance from cache
        evictItem(((ItemImpl)item).getItemState());
    }

    //-----------------------------------------------------------< Dumpable >---
    /**
     * @see Dumpable#dump(PrintStream)
     */
    public void dump(PrintStream ps) {
        ps.println("ItemManager (" + this + ")");
        ps.println();
        ps.println("Items in cache:");
        ps.println();
        Iterator iter = itemCache.keySet().iterator();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            Item item = (Item) itemCache.get(state);
            if (item.isNode()) {
                ps.print("Node: ");
            } else {
                ps.print("Property: ");
            }
            if (item.isNew()) {
                ps.print("new ");
            } else if (item.isModified()) {
                ps.print("modified ");
            } else {
                ps.print("- ");
            }
            ps.println(state.getId() + "\t" + LogUtil.safeGetJCRPath(state, session.getNamespaceResolver(), hierMgr) + " (" + item + ")");
        }
    }

    //----------------------------------------------------< private methods >---
    /**
     *
     * @param state
     * @param removeFromCache
     * @throws RepositoryException
     */
    private void checkAccess(ItemState state, boolean removeFromCache) throws RepositoryException {
        // check privileges
        if (!session.getAccessManager().canRead(state)) {
            if (removeFromCache) {
                // clear cache
                Item item = retrieveItem(state);
                if (item != null) {
                    evictItem(state);
                }
            }
            throw new AccessDeniedException("cannot read item " + state.getId());
        }
    }

    /**
     *
     * @param state
     * @return
     * @throws RepositoryException
     */
    private NodeImpl createNodeInstance(NodeState state) throws RepositoryException {
        // 1. get definition of the specified node
        QNodeDefinition qnd = state.getDefinition(session.getNodeTypeRegistry());
        NodeDefinition def = session.getNodeTypeManager().getNodeDefinition(qnd);

        // 2. create instance
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

    /**
     * 
     * @param state
     * @return
     * @throws RepositoryException
     */
    private PropertyImpl createPropertyInstance(PropertyState state)
            throws RepositoryException {
        // 1. get definition for the specified property
        QPropertyDefinition qpd = state.getDefinition(session.getNodeTypeRegistry());
        PropertyDefinition def = session.getNodeTypeManager().getPropertyDefinition(qpd);

        // 2. create instance
                // we want to be informed on life cycle changes of the new property object
        // in order to maintain item cache consistency
        ItemLifeCycleListener[] listeners = new ItemLifeCycleListener[]{this};
        // create property object
        PropertyImpl prop = new PropertyImpl(this, session, state, def, listeners);
        return prop;
    }

    //-------------------------------------------------< item cache methods >---
    /**
     * Puts the reference of an item in the cache with
     * the item's path as the key.
     *
     * @param item the item to cache
     */
    private void cacheItem(ItemState state, Item item) {
        if (itemCache.containsKey(state)) {
            log.warn("overwriting cached item " + state);
        }
        if (log.isDebugEnabled()) {
            log.debug("caching item " + state);
        }
        itemCache.put(state, item);
    }

    /**
     * Returns an item reference from the cache.
     *
     * @param state State of the item that should be retrieved.
     * @return the item reference stored in the corresponding cache entry
     *         or <code>null</code> if there's no corresponding cache entry.
     */
    private Item retrieveItem(ItemState state) {
        return (Item) itemCache.get(state);
    }

    /**
     * Removes a cache entry for a specific item.
     *
     * @param itemState state of the item to remove from the cache
     */
    private void evictItem(ItemState itemState) {
        if (log.isDebugEnabled()) {
            log.debug("removing item " + itemState + " from cache");
        }
        itemCache.remove(itemState);
    }
}
