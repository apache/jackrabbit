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
package org.apache.jackrabbit.core.version;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.cluster.UpdateEventChannel;
import org.apache.jackrabbit.core.cluster.UpdateEventListener;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.observation.DelegatingObservationDispatcher;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.name.NameConstants;
import org.apache.jackrabbit.name.PathBuilder;
import org.apache.jackrabbit.conversion.MalformedPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

/**
 * This Class implements a VersionManager.
 */
public class VersionManagerImpl extends AbstractVersionManager implements ItemStateListener, UpdateEventListener {

    /**
     * the default logger
     */
    private static Logger log = LoggerFactory.getLogger(VersionManager.class);

    /**
     * The path to the version storage: /jcr:system/jcr:versionStorage
     */
    private static final Path VERSION_STORAGE_PATH;

    static {
        try {
            PathBuilder builder = new PathBuilder();
            builder.addRoot();
            builder.addLast(NameConstants.JCR_SYSTEM);
            builder.addLast(NameConstants.JCR_VERSIONSTORAGE);
            VERSION_STORAGE_PATH = builder.getPath();
        } catch (MalformedPathException e) {
            // will not happen. path is always valid
            throw new InternalError("Cannot initialize path");
        }
    }

    /**
     * The persistence manager for the versions
     */
    private final PersistenceManager pMgr;

    /**
     * The file system for this version manager
     */
    private final FileSystem fs;

    /**
     * the version state manager for the version storage
     */
    private VersionItemStateManager sharedStateMgr;

    /**
     * the virtual item state provider that exposes the version storage
     */
    private final VersionItemStateProvider versProvider;

    /**
     * the node type manager
     */
    private NodeTypeRegistry ntReg;

    /**
     * the dynamic event state collection factory
     */
    private final DynamicESCFactory escFactory;

    /**
     * Map of returned items. this is kept for invalidating
     */
    private final ReferenceMap versionItems = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);

    /**
     * Creates a new version manager
     *
     */
    public VersionManagerImpl(PersistenceManager pMgr, FileSystem fs,
                              NodeTypeRegistry ntReg,
                              DelegatingObservationDispatcher obsMgr, NodeId rootId,
                              NodeId rootParentId,
                              ItemStateCacheFactory cacheFactory) throws RepositoryException {
        try {
            this.pMgr = pMgr;
            this.fs = fs;
            this.ntReg = ntReg;
            this.escFactory = new DynamicESCFactory(obsMgr);

            // need to store the version storage root directly into the persistence manager
            if (!pMgr.exists(rootId)) {
                NodeState root = pMgr.createNew(rootId);
                root.setParentId(rootParentId);
                root.setDefinitionId(ntReg.getEffectiveNodeType(NameConstants.REP_SYSTEM).getApplicableChildNodeDef(
                        NameConstants.JCR_VERSIONSTORAGE, NameConstants.REP_VERSIONSTORAGE, ntReg).getId());
                root.setNodeTypeName(NameConstants.REP_VERSIONSTORAGE);
                PropertyState pt = pMgr.createNew(new PropertyId(rootId, NameConstants.JCR_PRIMARYTYPE));
                pt.setDefinitionId(ntReg.getEffectiveNodeType(NameConstants.REP_SYSTEM).getApplicablePropertyDef(
                        NameConstants.JCR_PRIMARYTYPE, PropertyType.NAME, false).getId());
                pt.setMultiValued(false);
                pt.setType(PropertyType.NAME);
                pt.setValues(new InternalValue[]{InternalValue.create(NameConstants.REP_VERSIONSTORAGE)});
                root.addPropertyName(pt.getName());
                ChangeLog cl = new ChangeLog();
                cl.added(root);
                cl.added(pt);
                pMgr.store(cl);
            }
            sharedStateMgr = createItemStateManager(pMgr, rootId, ntReg, cacheFactory);

            stateMgr = new LocalItemStateManager(sharedStateMgr, escFactory, cacheFactory);
            stateMgr.addListener(this);

            NodeState nodeState = (NodeState) stateMgr.getItemState(rootId);
            historyRoot = new NodeStateEx(stateMgr, ntReg, nodeState, NameConstants.JCR_VERSIONSTORAGE);

            // create the virtual item state provider
            versProvider = new VersionItemStateProvider(
                    getHistoryRootId(), sharedStateMgr);

        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public VirtualItemStateProvider getVirtualItemStateProvider() {
        return versProvider;
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws Exception {
        pMgr.close();
        fs.close();
    }

    /**
     * Returns the event state collection factory.
     * @return the event state collection factory.
     */
    public DynamicESCFactory getEscFactory() {
        return escFactory;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    public VersionHistory createVersionHistory(Session session, final NodeState node)
            throws RepositoryException {
        InternalVersionHistory history = (InternalVersionHistory)
                escFactory.doSourced((SessionImpl) session, new SourcedTarget(){
            public Object run() throws RepositoryException {
                return createVersionHistory(node);
            }
        });

        if (history == null) {
            throw new VersionException("History already exists for node " + node.getNodeId());
        }
        return (VersionHistory) ((SessionImpl) session).getNodeById(history.getId());
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasItem(NodeId id) {
        acquireReadLock();
        try {
            return stateMgr.hasItemState(id);
        } finally {
            releaseReadLock();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected InternalVersionItem getItem(NodeId id)
            throws RepositoryException {

        if (id.equals(getHistoryRootId())) {
            return null;
        }
        acquireReadLock();
        try {
            synchronized (versionItems) {
                InternalVersionItem item = (InternalVersionItem) versionItems.get(id);
                if (item == null) {
                    if (stateMgr.hasItemState(id)) {
                        NodeState state = (NodeState) stateMgr.getItemState(id);
                        NodeStateEx pNode = new NodeStateEx(stateMgr, ntReg, state, null);
                        NodeId parentId = pNode.getParentId();
                        InternalVersionItem parent = getItem(parentId);
                        Name ntName = state.getNodeTypeName();
                        if (ntName.equals(NameConstants.NT_FROZENNODE)) {
                            item = new InternalFrozenNodeImpl(this, pNode, parent);
                        } else if (ntName.equals(NameConstants.NT_VERSIONEDCHILD)) {
                            item = new InternalFrozenVHImpl(this, pNode, parent);
                        } else if (ntName.equals(NameConstants.NT_VERSION)) {
                            item = ((InternalVersionHistory) parent).getVersion(id);
                        } else if (ntName.equals(NameConstants.NT_VERSIONHISTORY)) {
                            item = new InternalVersionHistoryImpl(this, pNode);
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                    versionItems.put(id, item);
                }
                return item;
            }
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        } finally {
            releaseReadLock();
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    public Version checkin(final NodeImpl node) throws RepositoryException {
        InternalVersion version = (InternalVersion)
                escFactory.doSourced((SessionImpl) node.getSession(), new SourcedTarget(){
            public Object run() throws RepositoryException {
                String histUUID = node.getProperty(NameConstants.JCR_VERSIONHISTORY).getString();
                return checkin((InternalVersionHistoryImpl)
                        getVersionHistory(NodeId.valueOf(histUUID)), node);
            }
        });

        return (VersionImpl)
                ((SessionImpl) node.getSession()).getNodeById(version.getId());
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    public void removeVersion(VersionHistory history, final Name name)
            throws VersionException, RepositoryException {

        final VersionHistoryImpl historyImpl = (VersionHistoryImpl) history;
        if (!historyImpl.hasNode(name)) {
            throw new VersionException("Version with name " + name.toString()
                    + " does not exist in this VersionHistory");
        }

        escFactory.doSourced((SessionImpl) history.getSession(), new SourcedTarget(){
            public Object run() throws RepositoryException {
                InternalVersionHistoryImpl vh = (InternalVersionHistoryImpl)
                        historyImpl.getInternalVersionHistory();
                removeVersion(vh, name);
                return null;
            }
        });
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    public Version setVersionLabel(final VersionHistory history,
                                   final Name version, final Name label,
                                   final boolean move)
            throws RepositoryException {

        InternalVersion v = (InternalVersion)
                escFactory.doSourced((SessionImpl) history.getSession(), new SourcedTarget(){
            public Object run() throws RepositoryException {
                InternalVersionHistoryImpl vh = (InternalVersionHistoryImpl)
                        ((VersionHistoryImpl) history).getInternalVersionHistory();
                return setVersionLabel(vh, version, label, move);
            }
        });

        if (v == null) {
            return null;
        } else {
            return (Version)
                    ((SessionImpl) history.getSession()).getNodeByUUID(v.getId().getUUID());
        }
    }

    /**
     * Invoked by some external source to indicate that some items in the
     * versions tree were updated. Version histories are reloaded if possible.
     * Matching items are removed from the cache.
     *
     * @param items items updated
     */
    public void itemsUpdated(Collection items) {
        acquireReadLock();
        try {
            synchronized (versionItems) {
                Iterator iter = items.iterator();
                while (iter.hasNext()) {
                    InternalVersionItem item = (InternalVersionItem) iter.next();
                    InternalVersionItem cached = (InternalVersionItem) versionItems.remove(item.getId());
                    if (cached != null) {
                        if (cached instanceof InternalVersionHistoryImpl) {
                            InternalVersionHistoryImpl vh = (InternalVersionHistoryImpl) cached;
                            try {
                                vh.reload();
                                versionItems.put(vh.getId(), vh);
                            } catch (RepositoryException e) {
                                log.warn("Unable to update version history: " + e.toString());
                            }
                        }
                    }
                }
            }
        } finally {
            releaseReadLock();
        }
    }

    /**
     * Set an event channel to inform about updates.
     *
     * @param eventChannel event channel
     */
    public void setEventChannel(UpdateEventChannel eventChannel) {
        sharedStateMgr.setEventChannel(eventChannel);
        eventChannel.setListener(this);
    }

    /**
     * {@inheritDoc}
     */
    protected void itemDiscarded(InternalVersionItem item) {
        // evict removed item from cache
        acquireReadLock();
        try {
            versionItems.remove(item.getId());
        } finally {
            releaseReadLock();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected List getItemReferences(InternalVersionItem item) {
        try {
            NodeReferences refs = stateMgr.getNodeReferences(
                    new NodeReferencesId(item.getId()));
            return refs.getReferences();
        } catch (ItemStateException e) {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * returns the id of the version history root node
     *
     * @return the id of the version history root node
     */
    NodeId getHistoryRootId() {
        return historyRoot.getState().getNodeId();
    }

    /**
     * Return the shared item state manager.
     */
    protected SharedItemStateManager getSharedStateMgr() {
        return sharedStateMgr;
    }

    /**
     * Creates a <code>VersionItemStateManager</code> or derivative.
     *
     * @param pMgr          persistence manager
     * @param rootId        root node id
     * @param ntReg         node type registry
     * @param cacheFactory  cache factory
     * @return item state manager
     * @throws ItemStateException if an error occurs
     */
    protected VersionItemStateManager createItemStateManager(PersistenceManager pMgr,
                                                             NodeId rootId,
                                                             NodeTypeRegistry ntReg,
                                                             ItemStateCacheFactory cacheFactory)
            throws ItemStateException {
        return new VersionItemStateManager(pMgr, rootId, ntReg, cacheFactory);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Not used.
     */
    public void stateCreated(ItemState created) {}

    /**
     * {@inheritDoc}
     * <p/>
     * Not used.
     */
    public void stateModified(ItemState modified) {}

    /**
     * {@inheritDoc}
     * <p/>
     * Remove item from cache on removal.
     */
    public void stateDestroyed(ItemState destroyed) {
        // evict removed item from cache
        acquireReadLock();
        try {
            versionItems.remove(destroyed.getId());
        } finally {
            releaseReadLock();
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Not used.
     */
    public void stateDiscarded(ItemState discarded) {}

    //--------------------------------------------------< UpdateEventListener >

    /**
     * {@inheritDoc}
     */
    public void externalUpdate(ChangeLog changes, List events) throws RepositoryException {
        EventStateCollection esc = getEscFactory().createEventStateCollection(null);
        esc.addAll(events);

        sharedStateMgr.externalUpdate(changes, esc);
    }

    //--------------------------------------------------------< inner classes >

    public static final class DynamicESCFactory implements EventStateCollectionFactory {

        /**
         * the observation manager
         */
        private DelegatingObservationDispatcher obsMgr;

        /**
         * the current event source
         */
        private SessionImpl source;


        /**
         * Creates a new event state collection factory
         * @param obsMgr
         */
        public DynamicESCFactory(DelegatingObservationDispatcher obsMgr) {
            this.obsMgr = obsMgr;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * This object uses one instance of a <code>LocalItemStateManager</code>
         * to update data on behalf of many sessions. In order to maintain the
         * association between update operation and session who actually invoked
         * the update, an internal event source is used.
         */
        public synchronized EventStateCollection createEventStateCollection()
                throws RepositoryException {
            if (source == null) {
                throw new RepositoryException("Unknown event source.");
            }
            return createEventStateCollection(source);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * This object uses one instance of a <code>LocalItemStateManager</code>
         * to update data on behalf of many sessions. In order to maintain the
         * association between update operation and session who actually invoked
         * the update, an internal event source is used.
         */
        public EventStateCollection createEventStateCollection(SessionImpl source) {
            return obsMgr.createEventStateCollection(source, VERSION_STORAGE_PATH);
        }

        /**
         * Executes the given runnable using the given event source.
         *
         * @param eventSource
         * @param runnable
         * @throws RepositoryException
         */
        public synchronized Object doSourced(SessionImpl eventSource, SourcedTarget runnable)
                throws RepositoryException {
            this.source = eventSource;
            try {
                return runnable.run();
            } finally {
                this.source = null;
            }
        }
    }

    private abstract class SourcedTarget {
        public abstract Object run() throws RepositoryException;
    }
}
