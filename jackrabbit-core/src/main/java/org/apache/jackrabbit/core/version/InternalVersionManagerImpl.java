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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.jcr.InvalidItemStateException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.ActivityViolationException;
import javax.jcr.version.VersionException;

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.cluster.UpdateEventChannel;
import org.apache.jackrabbit.core.cluster.UpdateEventListener;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.NodeIdFactory;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.observation.DelegatingObservationDispatcher;
import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ISMLocking;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class implements a VersionManager.
 */
public class InternalVersionManagerImpl extends InternalVersionManagerBase
        implements ItemStateListener, UpdateEventListener {

    /**
     * the default logger
     */
    private static Logger log = LoggerFactory.getLogger(InternalVersionManagerImpl.class);

    /**
     * The path of the jcr:system node: /jcr:system
     */
    private static final Path SYSTEM_PATH;

    static {
        PathFactory factory = PathFactoryImpl.getInstance();
        SYSTEM_PATH = factory.getRootPath().resolve(
                factory.createElement(NameConstants.JCR_SYSTEM));
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
     * the dynamic event state collection factory
     */
    private final DynamicESCFactory escFactory;

    /**
     * Persistent root node of the version histories.
     */
    private final NodeStateEx historyRoot;

    /**
     * Persistent root node of the activities.
     */
    private final NodeStateEx activitiesRoot;

    /**
     * Map of returned items. this is kept for invalidating
     */
    private final Map<ItemId, InternalVersionItem> versionItems =
            new ReferenceMap<>(ReferenceStrength.HARD, ReferenceStrength.WEAK);

    /**
     * Creates a new internal version manager
     *
     * @param pMgr underlying persistence manager
     * @param fs workspace file system
     * @param ntReg node type registry
     * @param obsMgr observation manager
     * @param systemId node id of the version storage parent (i.e. jcr:system)
     * @param historiesId node id of the version storage (i.e. jcr:versionStorage)
     * @param activitiesId node id of the activities storage (i.e. jcr:activities)
     * @param cacheFactory item state cache factory
     * @param ismLocking workspace item state locking
     * @throws RepositoryException if an error occurs
     */
    public InternalVersionManagerImpl(PersistenceManager pMgr, FileSystem fs,
                              NodeTypeRegistry ntReg,
                              DelegatingObservationDispatcher obsMgr,
                              NodeId systemId,
                              NodeId historiesId,
                              NodeId activitiesId,
                              ItemStateCacheFactory cacheFactory,
                              ISMLocking ismLocking,
                              NodeIdFactory nodeIdFactory) throws RepositoryException {
        super(ntReg, historiesId, activitiesId, nodeIdFactory);
        try {
            this.pMgr = pMgr;
            this.fs = fs;
            this.escFactory = new DynamicESCFactory(obsMgr);

            // need to store the version storage root directly into the persistence manager
            if (!pMgr.exists(historiesId)) {
                NodeState root = pMgr.createNew(historiesId);
                root.setParentId(systemId);
                root.setNodeTypeName(NameConstants.REP_VERSIONSTORAGE);
                PropertyState pt = pMgr.createNew(new PropertyId(historiesId, NameConstants.JCR_PRIMARYTYPE));
                pt.setMultiValued(false);
                pt.setType(PropertyType.NAME);
                pt.setValues(new InternalValue[]{InternalValue.create(NameConstants.REP_VERSIONSTORAGE)});
                root.addPropertyName(pt.getName());
                ChangeLog cl = new ChangeLog();
                cl.added(root);
                cl.added(pt);
                pMgr.store(cl);
            }

            // check for jcr:activities
            if (!pMgr.exists(activitiesId)) {
                NodeState root = pMgr.createNew(activitiesId);
                root.setParentId(systemId);
                root.setNodeTypeName(NameConstants.REP_ACTIVITIES);
                PropertyState pt = pMgr.createNew(new PropertyId(activitiesId, NameConstants.JCR_PRIMARYTYPE));
                pt.setMultiValued(false);
                pt.setType(PropertyType.NAME);
                pt.setValues(new InternalValue[]{InternalValue.create(NameConstants.REP_ACTIVITIES)});
                root.addPropertyName(pt.getName());
                ChangeLog cl = new ChangeLog();
                cl.added(root);
                cl.added(pt);
                pMgr.store(cl);
            }

            sharedStateMgr = createItemStateManager(pMgr, systemId, ntReg, cacheFactory, ismLocking, nodeIdFactory);

            stateMgr = LocalItemStateManager.createInstance(sharedStateMgr, escFactory, cacheFactory);
            stateMgr.addListener(this);

            NodeState nodeState = (NodeState) stateMgr.getItemState(historiesId);
            historyRoot = new NodeStateEx(stateMgr, ntReg, nodeState, NameConstants.JCR_VERSIONSTORAGE);

            nodeState = (NodeState) stateMgr.getItemState(activitiesId);
            activitiesRoot =  new NodeStateEx(stateMgr, ntReg, nodeState, NameConstants.JCR_ACTIVITIES);

            // create the virtual item state provider
            versProvider = new VersionItemStateProvider(historiesId, activitiesId, sharedStateMgr);

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
     * Return the persistence manager.
     *
     * @return the persistence manager
     */
    public PersistenceManager getPersistenceManager() {
        return pMgr;
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
     * <p>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    @Override
    protected VersionHistoryInfo createVersionHistory(Session session,
                  final NodeState node, final NodeId copiedFrom)
            throws RepositoryException {
        NodeStateEx state = (NodeStateEx)
                escFactory.doSourced((SessionImpl) session, new SourcedTarget() {
            public Object run() throws RepositoryException {
                return internalCreateVersionHistory(node, copiedFrom);
            }
        });

        if (state == null) {
            throw new InvalidItemStateException(
                    "History already exists for node " + node.getNodeId());
        }
        Name root = NameConstants.JCR_ROOTVERSION;
        return new VersionHistoryInfo(
                state.getNodeId(),
                state.getState().getChildNodeEntry(root, 1).getId());
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    public NodeId createActivity(Session session, final String title) throws RepositoryException {
        NodeStateEx state = (NodeStateEx)
                escFactory.doSourced((SessionImpl) session, new SourcedTarget() {
            public Object run() throws RepositoryException {
                return internalCreateActivity(title);
            }
        });
        return state.getNodeId();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    public void removeActivity(Session session, final NodeId nodeId)
            throws RepositoryException {
        escFactory.doSourced((SessionImpl) session, new SourcedTarget() {
            public Object run() throws RepositoryException {
                InternalActivityImpl act = (InternalActivityImpl) getItem(nodeId);
                internalRemoveActivity(act);
                return null;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasItem(NodeId id) {
        VersioningLock.ReadLock lock = acquireReadLock();
        try {
            return stateMgr.hasItemState(id);
        } finally {
            lock.release();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected InternalVersionItem getItem(NodeId id)
            throws RepositoryException {

        if (id.equals(historiesId)) {
            return null;
        }
        if (id.equals(activitiesId)) {
            return null;
        }
        VersioningLock.ReadLock lock = acquireReadLock();
        try {
            synchronized (versionItems) {
                InternalVersionItem item = versionItems.get(id);
                if (item == null) {
                    item = createInternalVersionItem(id);
                    if (item != null) {
                        versionItems.put(id, item);
                    } else {
                        return null;
                    }
                }
                return item;
            }
        } finally {
            lock.release();
        }
    }
    /**
     * {@inheritDoc}
     *
     * this method currently does no modifications to the persistence and just
     * checks if the checkout is valid in respect to a possible activity set on
     * the session
     */
    public NodeId canCheckout(NodeStateEx state, NodeId activityId) throws RepositoryException {
        NodeId baseId = state.getPropertyValue(NameConstants.JCR_BASEVERSION).getNodeId();
        if (activityId != null) {
            // If there exists another workspace with node N' where N' also has version
            // history H, N' is checked out and the jcr:activity property of N'
            // references A, then the checkout fails with an
            // ActivityViolationException indicating which workspace currently has
            // the checkout.

            // we're currently leverage the fact, that only references to "real"
            // workspaces are recorded. so check all references if their sources
            // exist in 'this' workspace
            if (stateMgr.hasNodeReferences(activityId)) {
                try {
                    NodeReferences refs = stateMgr.getNodeReferences(activityId);
                    for (PropertyId id: refs.getReferences()) {
                        if (!state.hasNode(id.getParentId())) {
                            throw new ActivityViolationException("Unable to checkout. " +
                                    "Activity is already used for the same node in " +
                                    "another workspace.");
                        }
                    }
                } catch (ItemStateException e) {
                    throw new RepositoryException("Error during checkout.", e);
                }
            }

            // If there is a version in H that is not an eventual predecessor of N but
            // whose jcr:activity references A, then the checkout fails with an
            // ActivityViolationException
            InternalActivityImpl a = (InternalActivityImpl) getItem(activityId);
            NodeId historyId = state.getPropertyValue(NameConstants.JCR_VERSIONHISTORY).getNodeId();
            InternalVersionHistory history = (InternalVersionHistory) getItem(historyId);
            InternalVersion version = a.getLatestVersion(history);
            if (version != null) {
                InternalVersion baseVersion = (InternalVersion) getItem(baseId);
                while (baseVersion != null && !baseVersion.getId().equals(version.getId())) {
                    baseVersion = baseVersion.getLinearPredecessor();
                }
                if (baseVersion == null) {
                    throw new ActivityViolationException("Unable to checkout. " +
                            "Activity is used by another version on a different branch: " + version.getName());
                }
            }
        }
        return baseId;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    public InternalVersion checkin(
            Session session, final NodeStateEx node, final Calendar created)
            throws RepositoryException {
        return (InternalVersion) escFactory.doSourced(
                (SessionImpl) session,
                new SourcedTarget() {
                    public Object run() throws RepositoryException {
                        return checkin(node, created);
                    }
                });
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    public void removeVersion(Session session,
                              final InternalVersionHistory history,
                              final Name name)
            throws VersionException, RepositoryException {

        if (!history.hasVersion(name)) {
            throw new VersionException("Version with name " + name.toString()
                    + " does not exist in this VersionHistory");
        }

        escFactory.doSourced((SessionImpl) session, new SourcedTarget() {
            public Object run() throws RepositoryException {
                internalRemoveVersion((InternalVersionHistoryImpl) history, name);
                return null;
            }
        });
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    public void removeVersionHistory(Session session, final InternalVersionHistory history) throws RepositoryException {
        escFactory.doSourced((SessionImpl) session, new SourcedTarget() {
            public Object run() throws RepositoryException {
                internalRemoveVersionHistory((InternalVersionHistoryImpl) history);
                return null;
            }
        });
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    public InternalVersion setVersionLabel(Session session,
                                           final InternalVersionHistory history,
                                           final Name version, final Name label,
                                           final boolean move)
            throws RepositoryException {

        return (InternalVersion)
                escFactory.doSourced((SessionImpl) session, new SourcedTarget() {
            public Object run() throws RepositoryException {
                return setVersionLabel((InternalVersionHistoryImpl) history, version, label, move);
            }
        });
    }

    /**
     * Invoked by some external source to indicate that some items in the
     * versions tree were updated. Version histories are reloaded if possible.
     * Matching items are removed from the cache.
     *
     * @param items items updated
     */
    public void itemsUpdated(Collection<InternalVersionItem> items) {
        VersioningLock.ReadLock lock = acquireReadLock();
        try {
            synchronized (versionItems) {
                for (InternalVersionItem item : items) {
                    InternalVersionItem cached = versionItems.remove(item.getId());
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
            lock.release();
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
    @Override
    protected void itemDiscarded(InternalVersionItem item) {
        // evict removed item from cache
        VersioningLock.ReadLock lock = acquireReadLock();
        try {
            versionItems.remove(item.getId());
        } finally {
            lock.release();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasItemReferences(NodeId id)
            throws RepositoryException {
        return stateMgr.hasNodeReferences(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeStateEx getNodeStateEx(NodeId parentNodeId)
            throws RepositoryException {
        try {
            NodeState state = (NodeState) stateMgr.getItemState(parentNodeId);
            return new NodeStateEx(stateMgr, ntReg, state, null);
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * returns the version history root node
     *
     * @return the version history root node
     */
    @Override
    protected NodeStateEx getHistoryRoot() {
        return historyRoot;
    }

    /**
     * returns the activities root node
     *
     * @return the activities root node
     */
    @Override
    protected NodeStateEx getActivitiesRoot() {
        return activitiesRoot;
    }

    /**
     * Returns the shared item state manager.
     * @return the shared item state manager.
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
     * @param ismLocking    the ISM locking implementation
     * @return item state manager
     * @throws ItemStateException if an error occurs
     */
    protected VersionItemStateManager createItemStateManager(PersistenceManager pMgr,
                                                             NodeId rootId,
                                                             NodeTypeRegistry ntReg,
                                                             ItemStateCacheFactory cacheFactory,
                                                             ISMLocking ismLocking,
                                                             NodeIdFactory nodeIdFactory)
            throws ItemStateException {
        return new VersionItemStateManager(pMgr, rootId, ntReg, cacheFactory, ismLocking, nodeIdFactory);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Not used.
     */
    public void stateCreated(ItemState created) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * Not used.
     */
    public void stateModified(ItemState modified) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * Remove item from cache on removal.
     */
    public void stateDestroyed(ItemState destroyed) {
        // evict removed item from cache
        VersioningLock.ReadLock lock = acquireReadLock();
        try {
            versionItems.remove(destroyed.getId());
        } finally {
            lock.release();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Not used.
     */
    public void stateDiscarded(ItemState discarded) {
    }

    //--------------------------------------------------< UpdateEventListener >

    /**
     * {@inheritDoc}
     */
    public void externalUpdate(ChangeLog changes, List<EventState> events,
                               long timestamp, String userData)
            throws RepositoryException {
        EventStateCollection esc = getEscFactory().createEventStateCollection(null);
        esc.addAll(events);
        esc.setTimestamp(timestamp);
        esc.setUserData(userData);

        sharedStateMgr.externalUpdate(changes, esc);

        Collection<InternalVersionItem> items =
            new ArrayList<InternalVersionItem>();
        synchronized (versionItems) {
            for (Map.Entry<ItemId, InternalVersionItem> entry : versionItems
                    .entrySet()) {
                if (changes.has(entry.getKey())) {
                    items.add(entry.getValue());
                }
            }
        }
        itemsUpdated(items);
    }

    //--------------------------------------------------------< inner classes >

    public static final class DynamicESCFactory implements EventStateCollectionFactory {

        /**
         * the observation manager
         */
        private final DelegatingObservationDispatcher obsMgr;

        /**
         * The event source of the current thread.
         */
        private final ThreadLocal<SessionImpl> source =
            new ThreadLocal<SessionImpl>();

        /**
         * Creates a new event state collection factory
         * @param obsMgr dispatcher
         */
        public DynamicESCFactory(DelegatingObservationDispatcher obsMgr) {
            this.obsMgr = obsMgr;
        }

        /**
         * {@inheritDoc}
         * <p>
         * This object uses one instance of a <code>LocalItemStateManager</code>
         * to update data on behalf of many sessions. In order to maintain the
         * association between update operation and session who actually invoked
         * the update, an internal event source is used.
         */
        public EventStateCollection createEventStateCollection()
                throws RepositoryException {
            SessionImpl session = source.get();
            if (session != null) {
                return createEventStateCollection(session);
            } else {
                throw new RepositoryException("Unknown event source.");
            }
        }

        /**
         * {@inheritDoc}
         * <p>
         * This object uses one instance of a <code>LocalItemStateManager</code>
         * to update data on behalf of many sessions. In order to maintain the
         * association between update operation and session who actually invoked
         * the update, an internal event source is used.
         */
        public EventStateCollection createEventStateCollection(SessionImpl source) {
            return obsMgr.createEventStateCollection(source, SYSTEM_PATH);
        }

        /**
         * Executes the given runnable using the given event source.
         *
         * @param eventSource event source
         * @param runnable the runnable to execute
         * @return the return value of the executed runnable
         * @throws RepositoryException if an error occurs
         */
        public Object doSourced(SessionImpl eventSource, SourcedTarget runnable)
                throws RepositoryException {
            source.set(eventSource);
            try {
                return runnable.run();
            } finally {
                source.remove();
            }
        }

    }

    private abstract class SourcedTarget {
        public abstract Object run() throws RepositoryException;
    }
}
