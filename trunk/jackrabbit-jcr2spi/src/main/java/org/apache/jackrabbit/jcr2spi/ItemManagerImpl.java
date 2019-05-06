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

import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateCreationListener;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.jcr2spi.util.LogUtil;
import org.apache.jackrabbit.jcr2spi.version.VersionHistoryImpl;
import org.apache.jackrabbit.jcr2spi.version.VersionImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import java.util.Iterator;

/**
 * <code>ItemManagerImpl</code> implements the <code>ItemManager</code> interface.
 */
public class ItemManagerImpl implements ItemManager, ItemStateCreationListener {

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
    private final ItemCache itemCache;

    /**
     * Creates a new per-session instance <code>ItemManagerImpl</code> instance.
     *
     * @param hierMgr HierarchyManager associated with the new instance
     * @param session the session associated with the new instance
     * @param cache the ItemCache to be used.
     */
    ItemManagerImpl(HierarchyManager hierMgr, SessionImpl session, ItemCache cache) {
        this.hierMgr = hierMgr;
        this.session = session;
        itemCache = cache;

        // start listening to creation of ItemStates upon batch-reading in the
        // workspace item state factory.
        Workspace wsp = session.getWorkspace();
        if (wsp instanceof WorkspaceImpl) {
            ((WorkspaceImpl) wsp).getItemStateFactory().addCreationListener(this);
        }
    }

    //--------------------------------------------------------< ItemManager >---
    /**
     * @see ItemManager#dispose()
     */
    public void dispose() {
        // stop listening
        Workspace wsp = session.getWorkspace();
        if (wsp instanceof WorkspaceImpl) {
            ((WorkspaceImpl) wsp).getItemStateFactory().removeCreationListener(this);
        }
        // ... and clear the cache.
        itemCache.clear();
    }

    /**
     * @see ItemManager#nodeExists(Path)
     */
    public boolean nodeExists(Path path) throws RepositoryException {
        try {
            // session-sanity & permissions are checked upon itemExists(ItemState)
            NodeState nodeState = hierMgr.getNodeState(path);
            return itemExists(nodeState);
        } catch (PathNotFoundException pnfe) {
            return false;
        } catch (ItemNotFoundException infe) {
            return false;
        }
    }

    /**
     * @see ItemManager#propertyExists(Path)
     */
    public boolean propertyExists(Path path) throws RepositoryException {
        try {
            // session-sanity & permissions are checked upon itemExists(ItemState)
            PropertyState propState = hierMgr.getPropertyState(path);
            return itemExists(propState);
        } catch (PathNotFoundException pnfe) {
            return false;
        } catch (ItemNotFoundException infe) {
            return false;
        }
    }

    /**
     * @see ItemManager#itemExists(HierarchyEntry)
     */
    public boolean itemExists(HierarchyEntry hierarchyEntry) throws RepositoryException {
        try {
            // session-sanity & permissions are checked upon itemExists(ItemState)
            ItemState state = hierarchyEntry.getItemState();
            return itemExists(state);
        } catch (ItemNotFoundException e) {
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
     * @see ItemManager#getNode(Path)
     */
    public synchronized Node getNode(Path path) throws PathNotFoundException, RepositoryException {
        NodeEntry nodeEntry = hierMgr.getNodeEntry(path);
        try {
            return (Node) getItem(nodeEntry);
        } catch (ItemNotFoundException infe) {
            throw new PathNotFoundException(LogUtil.safeGetJCRPath(path, session.getPathResolver()));
        }
    }

    /**
     * @see ItemManager#getProperty(Path)
     */
    public synchronized Property getProperty(Path path) throws PathNotFoundException, RepositoryException {
        PropertyEntry propertyEntry = hierMgr.getPropertyEntry(path);
        try {
            return (Property) getItem(propertyEntry);
        } catch (ItemNotFoundException infe) {
            throw new PathNotFoundException(LogUtil.safeGetJCRPath(path, session.getPathResolver()));
        }
    }

    /**
     * @see ItemManager#getItem(HierarchyEntry)
     */
    public Item getItem(HierarchyEntry hierarchyEntry) throws ItemNotFoundException, RepositoryException {
        session.checkIsAlive();
        ItemState state = hierarchyEntry.getItemState();
        if (!state.isValid()) {
            throw new ItemNotFoundException(LogUtil.safeGetJCRPath(state, session.getPathResolver()));
        }

        // first try to access item from cache
        Item item = itemCache.getItem(state);
        // not yet in cache, need to create instance
        if (item == null) {
            // create instance of item
            if (hierarchyEntry.denotesNode()) {
                item = createNodeInstance((NodeState) state);
            } else {
                item = createPropertyInstance((PropertyState) state);
            }
        }
        return item;
    }

    /**
     * @see ItemManager#hasChildNodes(NodeEntry)
     */
    public synchronized boolean hasChildNodes(NodeEntry parentEntry)
            throws ItemNotFoundException, RepositoryException {
        // check sanity of session
        session.checkIsAlive();

        Iterator<NodeEntry> iter = parentEntry.getNodeEntries();
        while (iter.hasNext()) {
            try {
                // check read access by accessing the nodeState (implicit validation check)
                NodeEntry entry = iter.next();
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
            throws ItemNotFoundException, RepositoryException {
        // check sanity of session
        session.checkIsAlive();

        Iterator<NodeEntry> it = parentEntry.getNodeEntries();
        return new LazyItemIterator(this, it);
    }

    /**
     * @see ItemManager#hasChildProperties(NodeEntry)
     */
    public synchronized boolean hasChildProperties(NodeEntry parentEntry)
            throws ItemNotFoundException, RepositoryException {
        // check sanity of session
        session.checkIsAlive();

        Iterator<PropertyEntry> iter = parentEntry.getPropertyEntries();
        while (iter.hasNext()) {
            try {
                PropertyEntry entry = iter.next();
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
            throws ItemNotFoundException, RepositoryException {
        // check sanity of session
        session.checkIsAlive();

        Iterator<PropertyEntry> propEntries = parentEntry.getPropertyEntries();
        return new LazyItemIterator(this, propEntries);
    }

    //-------------------------------------------------------------< Object >---

    /**
     * Returns the the state of this instance in a human readable format.
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ItemManager (" + super.toString() + ")\n");
        builder.append("Items in cache:\n");
        builder.append(itemCache);
        return builder.toString();
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
        ItemLifeCycleListener[] listeners = new ItemLifeCycleListener[]{itemCache};

        // check special nodes
        Name ntName = state.getNodeTypeName();
        if (NameConstants.NT_VERSION.equals(ntName)) {
            // version
            return new VersionImpl(session, state, listeners);
        } else if (NameConstants.NT_VERSIONHISTORY.equals(ntName)) {
            // version-history
            return new VersionHistoryImpl(session, state, listeners);
        } else {
            // create common node object
            return new NodeImpl(session, state, listeners);
        }
    }

    /**
     * @param state
     * @return a new <code>Property</code> instance.
     */
    private PropertyImpl createPropertyInstance(PropertyState state) {
        // we want to be informed on life cycle changes of the new property object
        // in order to maintain item cache consistency
        ItemLifeCycleListener[] listeners = new ItemLifeCycleListener[]{itemCache};
        // create property object
        PropertyImpl prop = new PropertyImpl(session, state, listeners);
        return prop;
    }

    //------------------------------------------< ItemStateCreationListener >---
    /**
     *
     * @param state
     */
    public void created(ItemState state) {
        if (state.isNode()) {
            try {
                createNodeInstance((NodeState) state);
            } catch (RepositoryException e) {
                // log warning and ignore
                log.warn("Unable to create Node instance: " + e.getMessage());
            }
        } else {
            createPropertyInstance((PropertyState) state);
        }
    }

    public void statusChanged(ItemState state, int previousStatus) {
        // stop listening if an state reached Status.REMOVED.
        if (Status.REMOVED == state.getStatus()) {
            state.removeListener(this);
        }
        // otherwise: nothing to do -> Item is listening to status changes and
        // forces cleanup of cache entries through it's own status changes.
    }
}
