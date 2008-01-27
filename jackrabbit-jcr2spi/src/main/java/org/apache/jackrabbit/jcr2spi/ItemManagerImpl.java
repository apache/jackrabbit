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

import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.util.Dumpable;
import org.apache.jackrabbit.jcr2spi.util.LogUtil;
import org.apache.jackrabbit.jcr2spi.version.VersionHistoryImpl;
import org.apache.jackrabbit.jcr2spi.version.VersionImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
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
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;

/**
 * <code>ItemManagerImpl</code> implements the <code>ItemManager</code> interface.
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
            // session-sanity & permissions are checked upon itemExists(ItemState)
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
     * @see ItemManager#itemExists(HierarchyEntry)
     */
    public boolean itemExists(HierarchyEntry hierarchyEntry) {
        try {
            // session-sanity & permissions are checked upon itemExists(ItemState)
            ItemState state = hierarchyEntry.getItemState();
            return itemExists(state);
        } catch (ItemNotFoundException e) {
            return false;
        } catch (RepositoryException e) {
            return false;
        }
    }

    /**
     *
     * @param itemState
     * @return
     */
    private boolean itemExists(ItemState itemState) {
        try {
            // check sanity of session
            session.checkIsAlive();
            // return true, if ItemState is valid. Access rights are granted,
            // otherwise the state would not have been retrieved.
            return itemState.isValid();
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
        HierarchyEntry itemEntry = hierMgr.getHierarchyEntry(path);
        try {
            return getItem(itemEntry);
        } catch (ItemNotFoundException infe) {
            throw new PathNotFoundException(LogUtil.safeGetJCRPath(path, session.getPathResolver()));
        }
    }

    /**
     * @see ItemManager#getItem(HierarchyEntry)
     */
    public Item getItem(HierarchyEntry hierarchyEntry) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        ItemState itemState = hierarchyEntry.getItemState();
        return getItem(itemState);
    }

    /**
     *
     * @param itemState
     * @return
     * @throws ItemNotFoundException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    private Item getItem(ItemState itemState) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        session.checkIsAlive();
        if (!itemState.isValid()) {
            throw new ItemNotFoundException();
        }

        // first try to access item from cache
        Item item = retrieveItem(itemState);
        // not yet in cache, need to create instance
        if (item == null) {
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
     * @see ItemManager#hasChildNodes(NodeEntry)
     */
    public synchronized boolean hasChildNodes(NodeEntry parentEntry)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.checkIsAlive();

        Iterator iter = parentEntry.getNodeEntries();
        while (iter.hasNext()) {
            try {
                // check read access by accessing the nodeState (implicit validation check)
                NodeEntry entry = (NodeEntry) iter.next();
                entry.getNodeState();
                return true;
            } catch (ItemNotFoundException e) {
                // should not occur. ignore
                log.debug("Failed to access node state.", e);
            }
        }
        return false;
    }

    /**
     * @see ItemManager#getChildNodes(NodeEntry)
     */
    public synchronized NodeIterator getChildNodes(NodeEntry parentEntry)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.checkIsAlive();

        Iterator it = parentEntry.getNodeEntries();
        return new LazyItemIterator(this, it);
    }

    /**
     * @see ItemManager#hasChildProperties(NodeEntry)
     */
    public synchronized boolean hasChildProperties(NodeEntry parentEntry)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.checkIsAlive();

        Iterator iter = parentEntry.getPropertyEntries();
        while (iter.hasNext()) {
            try {
                PropertyEntry entry = (PropertyEntry) iter.next();
                // check read access by accessing the propState (also implicit validation).
                entry.getPropertyState();
                return true;
            } catch (ItemNotFoundException e) {
                // should not occur. ignore
                log.debug("Failed to access node state.", e);
            }
        }
        return false;
    }

    /**
     * @see ItemManager#getChildProperties(NodeEntry)
     */
    public synchronized PropertyIterator getChildProperties(NodeEntry parentEntry)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        // check sanity of session
        session.checkIsAlive();

        Iterator propEntries = parentEntry.getPropertyEntries();
        return new LazyItemIterator(this, propEntries);
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
            ps.println(state + "\t" + LogUtil.safeGetJCRPath(state, session.getPathResolver()) + " (" + item + ")");
        }
    }

    //----------------------------------------------------< private methods >---
    /**
     * @param state
     * @return a new <code>Node</code> instance.
     * @throws RepositoryException
     */
    private NodeImpl createNodeInstance(NodeState state) throws RepositoryException {
        // we want to be informed on life cycle changes of the new node object
        // in order to maintain item cache consistency
        ItemLifeCycleListener[] listeners = new ItemLifeCycleListener[]{this};

        // check special nodes
        Name ntName = state.getNodeTypeName();
        if (NameConstants.NT_VERSION.equals(ntName)) {
            // version
            return new VersionImpl(this, session, state, listeners);
        } else if (NameConstants.NT_VERSIONHISTORY.equals(ntName)) {
            // version-history
            return new VersionHistoryImpl(this, session, state, listeners);
        } else {
            // create common node object
            return new NodeImpl(this, session, state, listeners);
        }
    }

    /**
     * @param state
     * @return a new <code>Property</code> instance.
     */
    private PropertyImpl createPropertyInstance(PropertyState state) {
        // we want to be informed on life cycle changes of the new property object
        // in order to maintain item cache consistency
        ItemLifeCycleListener[] listeners = new ItemLifeCycleListener[]{this};
        // create property object
        PropertyImpl prop = new PropertyImpl(this, session, state, listeners);
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
