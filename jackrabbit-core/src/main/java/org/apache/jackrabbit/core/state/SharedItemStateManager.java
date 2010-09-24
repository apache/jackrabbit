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
package org.apache.jackrabbit.core.state;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.cluster.UpdateEventChannel;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.persistence.CachingPersistenceManager;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.util.Dumpable;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared <code>ItemStateManager</code> (SISM). Caches objects returned from a
 * <code>PersistenceManager</code>. Objects returned by this item state
 * manager are shared among all sessions.
 * <p/>
 * A shared item state manager operates on a <code>PersistenceManager</code>
 * (PM) that is used to load and store the item states. Additionally, a SISM can
 * have <code>VirtualItemStateProvider</code>s (VISP) that are used to provide
 * additional, non-persistent, read-only states. Examples of VISP are the
 * content representation of the NodeTypes (/jcr:system/jcr:nodeTypes) and the
 * version store (/jcr:system/jcr:versionStore). those 2 VISP are added to the
 * SISM during initialization of a workspace. i.e. they are 'mounted' to all
 * workspaces. we assume, that VISP cannot be added dynamically, neither during
 * runtime nor by configuration.
 * <p/>
 * The states from the VISP are read-only. by the exception for node references.
 * remember that the referrers are stored in a {@link NodeReferences} state,
 * having the ID of the target state.
 * <br/>
 * there are 5 types of referential relations to be distinguished:
 * <ol>
 * <li> normal --> normal (references from 'normal' states to 'normal' states)
 *      this is the normal case and will be handled by the SISM.
 *
 * <li> normal --> virtual (references from 'normal' states to 'virtual' states)
 *      those references should be handled by the VISP rather by the SISM.
 *
 * <li> virtual --> normal (references from 'virtual' states to 'normal' states)
 *      such references are not supported. eg. references of versioned nodes do
 *      not impose any constraints on the referenced nodes.
 *
 * <li> virtual --> virtual (references from 'virtual' states to 'virtual'
 *      states of the same VISP).
 *      intra-virtual references are handled by the item state manager of the VISP.
 *
 * <li> virtual --> virtual' (references from 'virtual' states to 'virtual'
 *      states of different VISP).
 *      those do currently not occur and are therefore not supported.
 * </ol>
 * <p/>
 * if VISP are not dynamic, there is not risk that NV-type references can dangle
 * (since a VISP cannot be 'unmounted', leaving eventual references dangling).
 * although multi-workspace-referrers are not explicitly supported, the
 * architecture of <code>NodeReferences</code> support multiple referrers with
 * the same PropertyId. So the number of references can be tracked (an example
 * of multi-workspace-referrers is a version referenced by the jcr:baseVersion
 * of several (corresponding) nodes in multiple workspaces).
 * <br/>
 * As mentioned, VN-type references should not impose any constraints on the
 * referrers (e.g. a normal node referenced by a versioned reference property).
 * In case of the version store, the VN-type references are not stored at
 * all, but reinforced as NN-type references in the normal states in case of a
 * checkout operation.
 * <br/>
 * VV-type references should be handled by the respective VISP. they look as
 * NN-type references in the scope if the VISP anyway...so no special treatment
 * should be necessary.
 * <br/>
 * VV'-type references are currently not possible, since the version store and
 * virtual node type representation don't allow such references.
 */
public class SharedItemStateManager
        implements ItemStateManager, ItemStateListener, Dumpable {

    /**
     * Logger instance
     */
    private static Logger log = LoggerFactory.getLogger(SharedItemStateManager.class);

    /**
     * Flag for enabling hierarchy validation.
     * @see <a href="https://issues.apache.org/jira/browse/JCR-2598">JCR-2598</a>
     */
    private static final boolean VALIDATE_HIERARCHY =
        Boolean.getBoolean("org.apache.jackrabbit.core.state.validatehierarchy");

    /**
     * cache of weak references to ItemState objects issued by this
     * ItemStateManager
     */
    private final ItemStateCache cache;

    /**
     * Persistence Manager used for loading and storing items
     */
    private final PersistenceManager persistMgr;

    /**
     * node type registry used for identifying referenceable nodes
     */
    private final NodeTypeRegistry ntReg;

    /**
     * Flag indicating whether this item state manager uses node references to
     * verify integrity of its reference properties.
     */
    private final boolean usesReferences;

    /**
     * Flag indicating whether this item state manager is checking referential
     * integrity when storing modifications. The default is to to check
     * for referential integrity.
     * Should be changed very carefully by experienced developers only.
     *
     * @see "https://issues.apache.org/jira/browse/JCR-954"
     */
    private boolean checkReferences = true;

    /**
     * id of root node
     */
    private final NodeId rootNodeId;

    /**
     * Virtual item state providers
     */
    private VirtualItemStateProvider[] virtualProviders =
            new VirtualItemStateProvider[0];

    /**
     * State change dispatcher.
     */
    private final transient StateChangeDispatcher dispatcher = new StateChangeDispatcher();

    /**
     * The locking strategy.
     */
    private ISMLocking ismLocking;

    /**
     * Update event channel.
     */
    private UpdateEventChannel eventChannel;

    /**
     * Creates a new <code>SharedItemStateManager</code> instance.
     *
     * @param persistMgr
     * @param rootNodeId
     * @param ntReg
     */
    public SharedItemStateManager(PersistenceManager persistMgr,
                                  NodeId rootNodeId,
                                  NodeTypeRegistry ntReg,
                                  boolean usesReferences,
                                  ItemStateCacheFactory cacheFactory,
                                  ISMLocking locking)
            throws ItemStateException {
        cache = new ItemStateReferenceCache(cacheFactory);
        this.persistMgr = persistMgr;
        this.ntReg = ntReg;
        this.usesReferences = usesReferences;
        this.rootNodeId = rootNodeId;
        this.ismLocking = locking;
        // create root node state if it doesn't yet exist
        if (!hasNonVirtualItemState(rootNodeId)) {
            createRootNodeState(rootNodeId, ntReg);
        }
        ensureActivitiesNode();
    }

    /**
     * Enables or disables the referential integrity checking, this
     * should be used very carefully by experienced developers only.
     *
     * @see "https://issues.apache.org/jira/browse/JCR-954"
     * @param checkReferences whether to do referential integrity checks
     */
    public void setCheckReferences(boolean checkReferences) {
        this.checkReferences = checkReferences;
    }

    /**
     * Set an update event channel
     *
     * @param eventChannel update event channel
     */
    public void setEventChannel(UpdateEventChannel eventChannel) {
        this.eventChannel = eventChannel;
    }

    /**
     * Sets a new locking strategy.
     *
     * @param ismLocking the locking strategy for this item state manager.
     */
    public void setISMLocking(ISMLocking ismLocking) {
        if (ismLocking == null) {
            throw new NullPointerException();
        }
        this.ismLocking = ismLocking;
    }

    //-----------------------------------------------------< ItemStateManager >
    /**
     * {@inheritDoc}
     */
    public ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {
        // check the virtual root ids (needed for overlay)
        for (int i = 0; i < virtualProviders.length; i++) {
            if (virtualProviders[i].isVirtualRoot(id)) {
                return virtualProviders[i].getItemState(id);
            }
        }

        ISMLocking.ReadLock readLock = acquireReadLock(id);
        try {
            // check internal first
            if (hasNonVirtualItemState(id)) {
                return getNonVirtualItemState(id);
            }
        } finally {
            readLock.release();
        }

        // check if there is a virtual state for the specified item
        for (int i = 0; i < virtualProviders.length; i++) {
            if (virtualProviders[i].hasItemState(id)) {
                return virtualProviders[i].getItemState(id);
            }
        }

        throw new NoSuchItemStateException(id.toString());
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasItemState(ItemId id) {
        // check the virtual root ids (needed for overlay)
        for (int i = 0; i < virtualProviders.length; i++) {
            if (virtualProviders[i].isVirtualRoot(id)) {
                return true;
            }
        }

        ISMLocking.ReadLock readLock;
        try {
            readLock = acquireReadLock(id);
        } catch (ItemStateException e) {
            return false;
        }

        try {
            if (cache.isCached(id)) {
                return true;
            }

            // check if this manager has the item state
            if (hasNonVirtualItemState(id)) {
                return true;
            }
        } finally {
            readLock.release();
        }

        // otherwise check virtual ones
        for (int i = 0; i < virtualProviders.length; i++) {
            if (virtualProviders[i].hasItemState(id)) {
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public NodeReferences getNodeReferences(NodeId id)
            throws NoSuchItemStateException, ItemStateException {
        ISMLocking.ReadLock readLock = acquireReadLock(id);
        try {
            // check persistence manager
            try {
                return persistMgr.loadReferencesTo(id);
            } catch (NoSuchItemStateException e) {
                // ignore
            }
        } finally {
            readLock.release();
        }

        // check virtual providers
        for (int i = 0; i < virtualProviders.length; i++) {
            try {
                return virtualProviders[i].getNodeReferences(id);
            } catch (NoSuchItemStateException e) {
                // ignore
            }
        }

        // throw
        throw new NoSuchItemStateException(id.toString());
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNodeReferences(NodeId id) {
        ISMLocking.ReadLock readLock;
        try {
            readLock = acquireReadLock(id);
        } catch (ItemStateException e) {
            return false;
        }
        try {
            // check persistence manager
            try {
                if (persistMgr.existsReferencesTo(id)) {
                    return true;
                }
            } catch (ItemStateException e) {
                // ignore
            }
        } finally {
            readLock.release();
        }

        // check virtual providers
        for (int i = 0; i < virtualProviders.length; i++) {
            if (virtualProviders[i].hasNodeReferences(id)) {
                return true;
            }
        }

        return false;
    }

    //----------------------------------------------------< ItemStateListener >

    /**
     * {@inheritDoc}
     * <p/>
     * Notifications are received for items that this manager created itself or items that are
     * managed by one of the virtual providers.
     */
    public void stateCreated(ItemState created) {
        if (created.getContainer() == this) {
            // shared state was created
            cache.cache(created);
        }
        dispatcher.notifyStateCreated(created);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Notifications are received for items that this manager created itself or items that are
     * managed by one of the virtual providers.
     */
    public void stateModified(ItemState modified) {
        dispatcher.notifyStateModified(modified);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Notifications are received for items that this manager created itself or items that are
     * managed by one of the virtual providers.
     */
    public void stateDestroyed(ItemState destroyed) {
        if (destroyed.getContainer() == this) {
            // shared state was destroyed
            cache.evict(destroyed.getId());
        }
        dispatcher.notifyStateDestroyed(destroyed);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Notifications are received for items that this manager created itself or items that are
     * managed by one of the virtual providers.
     */
    public void stateDiscarded(ItemState discarded) {
        if (discarded.getContainer() == this) {
            // shared state was discarded
            cache.evict(discarded.getId());
        }
        dispatcher.notifyStateDiscarded(discarded);
    }

    //-------------------------------------------------------------< Dumpable >
    /**
     * {@inheritDoc}
     */
    public void dump(PrintStream ps) {
        ps.println("SharedItemStateManager (" + this + ")");
        if (cache instanceof Dumpable) {
            ps.println();
            ps.print("[referenceCache] ");
            ((Dumpable) cache).dump(ps);
        }
    }

    //-------------------------------------------------< misc. public methods >
    /**
     * Disposes this <code>SharedItemStateManager</code> and frees resources.
     */
    public void dispose() {
        // remove virtual item state providers (see JCR-2023)
        for (int i = 0; i < virtualProviders.length; i++) {
            virtualProviders[i].removeListener(this);
        }
        virtualProviders = new VirtualItemStateProvider[0];

        // clear cache
        cache.evictAll();
    }

    /**
     * Adds a new virtual item state provider.<p/>
     * NOTE: This method is not synchronized, because it is called right after
     * creation only by the same thread and therefore concurrency issues
     * do not occur. Should this ever change, the synchronization status
     * has to be re-examined.
     *
     * @param prov
     */
    public void addVirtualItemStateProvider(VirtualItemStateProvider prov) {
        VirtualItemStateProvider[] provs =
                new VirtualItemStateProvider[virtualProviders.length + 1];
        System.arraycopy(virtualProviders, 0, provs, 0, virtualProviders.length);
        provs[virtualProviders.length] = prov;
        virtualProviders = provs;

        prov.addListener(this);
    }

    /**
     * Object representing a single update operation.
     */
    class Update implements org.apache.jackrabbit.core.cluster.Update {

        /**
         * Local change log.
         */
        private final ChangeLog local;

        /**
         * Event state collection factory.
         */
        private final EventStateCollectionFactory factory;

        /**
         * Virtual provider containing references to be left out when updating
         * references.
         */
        private final VirtualItemStateProvider virtualProvider;

        /**
         * Shared change log.
         */
        private ChangeLog shared;

        /**
         * Virtual node references.
         */
        private ChangeLog[] virtualNodeReferences;

        /**
         * Events to dispatch.
         */
        private EventStateCollection events;

        /**
         * The write lock we currently hold or <code>null</code> if none is
         * hold.
         */
        private ISMLocking.WriteLock writeLock;

        /**
         * Map of attributes stored for this update operation.
         */
        private HashMap<String, Object> attributes;

        /**
         * Timestamp when this update was created.
         */
        private long timestamp = System.currentTimeMillis();

        /**
         * Create a new instance of this class.
         */
        public Update(ChangeLog local, EventStateCollectionFactory factory,
                      VirtualItemStateProvider virtualProvider) {
            this.local = local;
            this.factory = factory;
            this.virtualProvider = virtualProvider;
        }

        /**
         * Begin update operation. Prepares everything upto the point where
         * the persistence manager's <code>store</code> method may be invoked.
         * If this method succeeds, a write lock will have been acquired on the
         * item state manager and either {@link #end()} or {@link #cancel()} has
         * to be called in order to release it.
         */
        public void begin() throws ItemStateException, ReferentialIntegrityException {
            shared = new ChangeLog();

            virtualNodeReferences = new ChangeLog[virtualProviders.length];

            /* let listener know about change */
            if (eventChannel != null) {
                eventChannel.updateCreated(this);
            }

            try {
                writeLock = acquireWriteLock(local);
            } finally {
                if (writeLock == null && eventChannel != null) {
                    eventChannel.updateCancelled(this);
                }
            }

            boolean succeeded = false;

            try {
                if (usesReferences) {
                    // Update node references based on modifications in change
                    // log (added/modified/removed REFERENCE properties)
                    updateReferences();
                }

                // If enabled, check whether reference targets
                // exist/were not removed
                if (checkReferences) {
                    checkReferentialIntegrity();
                }

                /**
                 * prepare the events. this needs to be after the referential
                 * integrity check, since another transaction could have modified
                 * the states.
                 */
                try {
                    events = factory.createEventStateCollection();
                } catch (RepositoryException e) {
                    String msg = "Unable to create event state collection.";
                    log.error(msg);
                    throw new ItemStateException(msg, e);
                }

                /**
                 * Reconnect all items contained in the change log to their
                 * respective shared item and add the shared items to a
                 * new change log.
                 */
                for (ItemState state : local.modifiedStates()) {
                    state.connect(getItemState(state.getId()));
                    if (state.isStale()) {
                        boolean merged = false;
                        if (state.isNode()) {
                            NodeStateMerger.MergeContext context =
                                    new NodeStateMerger.MergeContext() {
                                        public boolean isAdded(ItemId id) {
                                            try {
                                                ItemState is = local.get(id);
                                                return is != null
                                                        && is.getStatus() == ItemState.STATUS_NEW;
                                            } catch (NoSuchItemStateException e) {
                                                return false;
                                            }
                                        }

                                        public boolean isDeleted(ItemId id) {
                                            return local.deleted(id);
                                        }

                                        public boolean isModified(ItemId id) {
                                            return local.isModified(id);
                                        }

                                        public boolean allowsSameNameSiblings(NodeId id) {
                                            try {
                                                NodeState ns = getNodeState(id);
                                                NodeState parent = getNodeState(ns.getParentId());
                                                Name name = parent.getChildNodeEntry(id).getName();
                                                EffectiveNodeType ent = ntReg.getEffectiveNodeType(
                                                        parent.getNodeTypeName(),
                                                        parent.getMixinTypeNames());
                                                QNodeDefinition def = ent.getApplicableChildNodeDef(name, ns.getNodeTypeName(), ntReg);
                                                return def != null ? def.allowsSameNameSiblings() : false;
                                            } catch (Exception e) {
                                                log.warn("Unable to get node definition", e);
                                                return false;
                                            }
                                        }

                                        protected NodeState getNodeState(NodeId id)
                                                throws ItemStateException {
                                            if (local.has(id)) {
                                                return (NodeState) local.get(id);
                                            } else {
                                                return (NodeState) getItemState(id);
                                            }
                                        }
                                    };

                            merged = NodeStateMerger.merge((NodeState) state, context);
                        }
                        if (!merged) {
                            String msg = state.getId() + " has been modified externally";
                            log.debug(msg);
                            throw new StaleItemStateException(msg);
                        }
                        // merge succeeded, fall through
                    }

                    // update modification count (will be persisted as well)
                    state.getOverlayedState().touch();

                    shared.modified(state.getOverlayedState());
                }
                for (ItemState state : local.deletedStates()) {
                    state.connect(getItemState(state.getId()));
                    if (state.isStale()) {
                        String msg = state.getId() + " has been modified externally";
                        log.debug(msg);
                        throw new StaleItemStateException(msg);
                    }
                    shared.deleted(state.getOverlayedState());
                }
                for (ItemState state : local.addedStates()) {
                    state.connect(createInstance(state));
                    shared.added(state.getOverlayedState());
                }

                // filter out virtual node references for later processing
                // (see comment above)
                for (NodeReferences refs : local.modifiedRefs()) {
                    boolean virtual = false;
                    NodeId id = refs.getTargetId();
                    for (int i = 0; i < virtualProviders.length; i++) {
                        if (virtualProviders[i].hasItemState(id)) {
                            ChangeLog virtualRefs = virtualNodeReferences[i];
                            if (virtualRefs == null) {
                                virtualRefs = new ChangeLog();
                                virtualNodeReferences[i] = virtualRefs;
                            }
                            virtualRefs.modified(refs);
                            virtual = true;
                            break;
                        }
                    }
                    if (!virtual) {
                        // if target of node reference does not lie in a virtual
                        // space, add to modified set of normal provider.
                        shared.modified(refs);
                    }
                }

                checkAddedChildNodes();

                /* create event states */
                events.createEventStates(rootNodeId, local, SharedItemStateManager.this);

                /* let listener know about change */
                if (eventChannel != null) {
                    eventChannel.updatePrepared(this);
                }

                if (VALIDATE_HIERARCHY) {
                    log.info("Validating change-set hierarchy");
                    try {
                        validateHierarchy(local);
                    } catch (ItemStateException e) {
                        throw e;
                    } catch (RepositoryException e) {
                        throw new ItemStateException("Invalid hierarchy", e);
                    }
                }

                /* Push all changes from the local items to the shared items */
                local.push();

                succeeded = true;

            } finally {
                if (!succeeded) {
                    cancel();
                }
            }
        }

        /**
         * End update operation. This will store the changes to the associated
         * <code>PersistenceManager</code>. At the end of this operation, an
         * eventual read or write lock on the item state manager will have
         * been released.
         * @throws ItemStateException if some error occurs
         */
        public void end() throws ItemStateException {
            boolean succeeded = false;

            try {
                /* Store items in the underlying persistence manager */
                long t0 = System.currentTimeMillis();
                persistMgr.store(shared);
                succeeded = true;
                if (log.isDebugEnabled()) {
                    long t1 = System.currentTimeMillis();
                    log.debug("persisting change log " + shared + " took " + (t1 - t0) + "ms");
                }
            } finally {
                if (!succeeded) {
                    cancel();
                }
            }

            ISMLocking.ReadLock readLock = null;
            try {
                // downgrade to read lock
                readLock = writeLock.downgrade();
                writeLock = null;

                // Let the shared item listeners know about the change
                // JCR-2171: This must happen after downgrading the lock!
                shared.persisted();

                /* notify virtual providers about node references */
                for (int i = 0; i < virtualNodeReferences.length; i++) {
                    ChangeLog virtualRefs = virtualNodeReferences[i];
                    if (virtualRefs != null) {
                        virtualProviders[i].setNodeReferences(virtualRefs);
                    }
                }

                /* dispatch the events */
                events.dispatch();

                /* let listener know about finished operation */
                if (eventChannel != null) {
                    String path = events.getSession().getUserID() + "@" + events.getCommonPath();
                    eventChannel.updateCommitted(this, path);
                }
            } finally {
                if (writeLock != null) {
                    // exception occurred before downgrading lock
                    writeLock.release();
                    writeLock = null;
                } else if (readLock != null) {
                    readLock.release();
                }
            }
        }

        /**
         * Cancel update operation. At the end of this operation, the write lock
         * on the item state manager will have been released.
         */
        public void cancel() {
            try {
                /* let listener know about canceled operation */
                if (eventChannel != null) {
                    eventChannel.updateCancelled(this);
                }

                local.disconnect();

                for (ItemState state : shared.modifiedStates()) {
                    try {
                        state.copy(loadItemState(state.getId()), false);
                    } catch (ItemStateException e) {
                        state.discard();
                    }
                }
                for (ItemState state : shared.deletedStates()) {
                    try {
                        state.copy(loadItemState(state.getId()), false);
                    } catch (ItemStateException e) {
                        state.discard();
                    }
                }
                for (ItemState state : shared.addedStates()) {
                    state.discard();
                }
            } finally {
                if (writeLock != null) {
                    writeLock.release();
                    writeLock = null;
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void setAttribute(String name, Object value) {
            if (attributes == null) {
                attributes = new HashMap<String, Object>();
            }
            attributes.put(name, value);
        }

        /**
         * {@inheritDoc}
         */
        public Object getAttribute(String name) {
            if (attributes != null) {
                return attributes.get(name);
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public ChangeLog getChanges() {
            return local;
        }

        /**
         * {@inheritDoc}
         */
        public List<EventState> getEvents() {
            return events.getEvents();
        }

        /**
         * {@inheritDoc}
         */
        public long getTimestamp() {
            return timestamp;
        }

        public String getUserData() {
            return events.getUserData();
        }

        /**
         * Updates the target node references collections based on the
         * modifications in the change log (i.e. added/removed/modified
         * <code>REFERENCE</code> properties).
         * <p>
         * <b>Important node:</b> For consistency reasons this method must
         * only be called <i>once</i> per change log and the change log
         * should not be modified anymore afterwards.
         *
         * @throws ItemStateException if an error occurs
         */
        private void updateReferences() throws ItemStateException {
            // process added REFERENCE properties
            for (ItemState state : local.addedStates()) {
                if (!state.isNode()) {
                    // add new references to the target
                    addReferences((PropertyState) state);
                }
            }

            // process modified REFERENCE properties
            for (ItemState state : local.modifiedStates()) {
                if (!state.isNode()) {
                    // remove old references from the target
                    removeReferences(getItemState(state.getId()));
                    // add new references to the target
                    addReferences((PropertyState) state);
                }
            }

            // process removed REFERENCE properties
            for (ItemState state : local.deletedStates()) {
                removeReferences(state);
            }
        }

        private void addReferences(PropertyState property) throws NoSuchItemStateException,
                ItemStateException {
            if (property.getType() == PropertyType.REFERENCE) {
                InternalValue[] values = property.getValues();
                for (int i = 0; values != null && i < values.length; i++) {
                    addReference(property.getPropertyId(), values[i].getNodeId());
                }
            }
        }

        private void addReference(PropertyId id, NodeId target)
                throws ItemStateException {
            if (virtualProvider == null
                    || !virtualProvider.hasNodeReferences(target)) {
                // get or create the references instance
                NodeReferences refs = local.getReferencesTo(target);
                if (refs == null) {
                    if (hasNodeReferences(target)) {
                        refs = getNodeReferences(target);
                    } else {
                        refs = new NodeReferences(target);
                    }
                }
                // add reference
                refs.addReference(id);
                // update change log
                local.modified(refs);
            }
        }

        private void removeReferences(ItemState state)
                throws NoSuchItemStateException, ItemStateException {
            if (!state.isNode()) {
                PropertyState property = (PropertyState) state;
                if (property.getType() == PropertyType.REFERENCE) {
                    InternalValue[] values = property.getValues();
                    for (int i = 0; values != null && i < values.length; i++) {
                        removeReference(
                                property.getPropertyId(), values[i].getNodeId());
                    }
                }
            }
        }

        private void removeReference(PropertyId id, NodeId target)
                throws ItemStateException {
            if (virtualProvider == null
                    || !virtualProvider.hasNodeReferences(target)) {
                // either get node references from change log or load from
                // persistence manager
                NodeReferences refs = local.getReferencesTo(target);
                if (refs == null && hasNodeReferences(target)) {
                    refs = getNodeReferences(target);
                }
                if (refs != null) {
                    // remove reference
                    refs.removeReference(id);
                    // update change log
                    local.modified(refs);
                }
            }
        }

        /**
         * Verify the added child nodes of the added or modified states exist.
         * If they don't exist, most likely the problem is that the same session
         * is used concurrently.
         */
        private void checkAddedChildNodes() throws ItemStateException {
            for (ItemState state : local.addedStates()) {
                checkAddedChildNode(state);
            }
            for (ItemState state : local.modifiedStates()) {
                checkAddedChildNode(state);
            }
        }

        private void checkAddedChildNode(ItemState state) throws ItemStateException {
            if (state.isNode()) {
                NodeState node = (NodeState) state;
                for (ChildNodeEntry child : node.getAddedChildNodeEntries()) {
                    NodeId id = child.getId();
                    if (local.get(id) == null &&
                            !id.equals(RepositoryImpl.VERSION_STORAGE_NODE_ID) &&
                            !id.equals(RepositoryImpl.ACTIVITIES_NODE_ID) &&
                            !id.equals(RepositoryImpl.NODETYPES_NODE_ID) &&
                            !cache.isCached(id) &&
                            !persistMgr.exists(id)) {
                        String msg = "Trying to add a non-existing child node: " + id;
                        log.debug(msg);
                        throw new ItemStateException(msg);
                    }
                }
            }
        }

        /**
         * Verifies that
         * <ul>
         * <li>no referenceable nodes are deleted if they are still being referenced</li>
         * <li>targets of modified node references exist</li>
         * </ul>
         *
         * @throws ReferentialIntegrityException if a new or modified REFERENCE
         *                                       property refers to a non-existent
         *                                       target or if a removed node is still
         *                                       being referenced
         * @throws ItemStateException            if another error occurs
         */
        private void checkReferentialIntegrity()
                throws ReferentialIntegrityException, ItemStateException {

            // check whether removed referenceable nodes are still being referenced
            for (ItemState state : local.deletedStates()) {
                if (state.isNode()) {
                    NodeState node = (NodeState) state;
                    if (isReferenceable(node)) {
                        NodeId targetId = node.getNodeId();
                        // either get node references from change log or
                        // load from persistence manager
                        NodeReferences refs = local.getReferencesTo(targetId);
                        if (refs == null) {
                            if (!hasNodeReferences(targetId)) {
                                continue;
                            }
                            refs = getNodeReferences(targetId);
                        }
                        // in some versioning operations (such as restore) a node
                        // may actually be deleted and then again added with the
                        // same UUID, i.e. the node is still referenceable.
                        if (refs.hasReferences() && !local.has(targetId)) {
                            String msg = node.getNodeId()
                                    + ": the node cannot be removed because it is still being referenced.";
                            log.debug(msg);
                            throw new ReferentialIntegrityException(msg);
                        }
                    }
                }
            }

            // check whether targets of modified node references exist
            for (NodeReferences refs : local.modifiedRefs()) {
                // no need to check existence of target if there are no references
                if (refs.hasReferences()) {
                    // please note:
                    // virtual providers are indirectly checked via 'hasItemState()'
                    NodeId id = refs.getTargetId();
                    if (!local.has(id) && !hasItemState(id)) {
                        String msg = "Target node " + id
                                + " of REFERENCE property does not exist";
                        log.debug(msg);
                        throw new ReferentialIntegrityException(msg);
                    }
                }
            }
        }

        /**
         * Determines whether the specified node is <i>referenceable</i>, i.e.
         * whether the mixin type <code>mix:referenceable</code> is either
         * directly assigned or indirectly inherited.
         *
         * @param state node state to check
         * @return true if the specified node is <i>referenceable</i>, false otherwise.
         * @throws ItemStateException if an error occurs
         */
        private boolean isReferenceable(NodeState state) throws ItemStateException {
            // shortcut: check some well known built-in types first
            Name primary = state.getNodeTypeName();
            Set<Name> mixins = state.getMixinTypeNames();
            if (mixins.contains(NameConstants.MIX_REFERENCEABLE)
                    || mixins.contains(NameConstants.MIX_VERSIONABLE)
                    || primary.equals(NameConstants.NT_RESOURCE)) {
                return true;
            }

            // build effective node type
            try {
                EffectiveNodeType type = ntReg.getEffectiveNodeType(primary, mixins);
                return type.includesNodeType(NameConstants.MIX_REFERENCEABLE);
            } catch (NodeTypeConflictException ntce) {
                String msg = "internal error: failed to build effective node type for node "
                        + state.getNodeId();
                log.debug(msg);
                throw new ItemStateException(msg, ntce);
            } catch (NoSuchNodeTypeException nsnte) {
                String msg = "internal error: failed to build effective node type for node "
                        + state.getNodeId();
                log.debug(msg);
                throw new ItemStateException(msg, nsnte);
            }
        }

    }
    
    /**
     * Validates the hierarchy consistency of the changes in the changelog.
     * 
     * @param changeLog
     *            The local changelog the should be validated
     * @throws ItemStateException
     *             If the hierarchy changes are inconsistent.
     * @throws RepositoryException
     *             If the consistency could not be validated
     * 
     */
    private void validateHierarchy(ChangeLog changeLog) throws ItemStateException, RepositoryException {

        // Check the deleted node states
        validateDeleted(changeLog);

        // Check the added node states
        validateAdded(changeLog);

        // Check the modified node states
        validateModified(changeLog);
    }

    /**
     * Checks the parents and children of all deleted node states in the changelog.
     * 
     * @param changeLog
     *            The local changelog the should be validated
     * @throws ItemStateException
     *             If the hierarchy changes are inconsistent.
     */
    private void validateDeleted(ChangeLog changeLog) throws ItemStateException {

        // Check each deleted nodestate
        for (ItemState removedState : changeLog.deletedStates()) {
            if (removedState instanceof NodeState) {

                // Get the next state
                NodeState removedNodeState = (NodeState) removedState;
                NodeId id = removedNodeState.getNodeId();

                // Get and check the corresponding overlayed state
                NodeState overlayedState = (NodeState) removedState.getOverlayedState();
                if (overlayedState == null) {
                    String message = "Unable to load persistent state for removed node " + id;
                    overlayedState = (NodeState) SharedItemStateManager.this.getItemState(id);
                    if (overlayedState == null) {
                        log.error(message);
                        throw new ItemStateException(message);
                    }
                }

                // Check whether an version of this node has been restored
                boolean addedAndRemoved = changeLog.has(removedNodeState.getId());
                if (!addedAndRemoved) {

                    // Check the old parent
                    NodeId oldParentId = overlayedState.getParentId();
                    if (changeLog.deleted(oldParentId)) {
                        // parent has been deleted aswell
                    } else if (changeLog.isModified(oldParentId)) {
                        // the modified state will be check later on
                    } else {
                        String message = "Node with id " + id
                                + " has been removed, but the parent node isn't part of the changelog " + oldParentId;
                        log.error(message);
                        throw new ItemStateException(message);
                    }

                    // Get the original list of child ids
                    for (ChildNodeEntry entry : overlayedState.getChildNodeEntries()) {

                        // Check the next child
                        NodeId childId = entry.getId();

                        if (changeLog.deleted(childId)) {
                            // child has been deleted aswell
                        } else if (changeLog.isModified(childId)) {

                            // the modified state will be check later on
                        } else {
                            String message = "Node with id " + id
                                    + " has been removed, but the old child node isn't part of the changelog "
                                    + childId;
                            log.error(message);
                            throw new ItemStateException(message);
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks the parents and children of all added node states in the changelog.
     * 
     * @param changeLog
     *            The local changelog the should be validated
     * @throws ItemStateException
     *             If the hierarchy changes are inconsistent.
     */
    private void validateAdded(ChangeLog changeLog) throws ItemStateException {

        // Check each added node
        for (ItemState state : changeLog.addedStates()) {
            if (state instanceof NodeState) {

                // Get the next added node
                NodeState addedNodeState = (NodeState) state;
                NodeId id = addedNodeState.getNodeId();

                // Check the parent
                NodeId parentId = addedNodeState.getParentId();
                if (changeLog.has(parentId)) { // Added or modified
                    // the modified state will be check later on
                    checkParent(changeLog, addedNodeState, parentId);
                } else {
                    String message = "Node with id " + id
                            + " has been added, but the parent node isn't part of the changelog " + parentId;
                    log.error(message);
                    throw new ItemStateException(message);
                }

                // Check the children
                for (ChildNodeEntry entry : addedNodeState.getChildNodeEntries()) {

                    // Get the next child
                    NodeId childId = entry.getId();

                    if (changeLog.has(childId)) {
                        NodeState childState = (NodeState) changeLog.get(childId);
                        checkParent(changeLog, childState, id);
                        // the child state will be check later on

                    } else {
                        String message = "Node with id " + id
                                + " has been added, but the child node isn't part of the changelog " + childId;
                        log.error(message);
                        throw new ItemStateException(message);
                    }
                }
            }
        }
    }

    /**
     * Checks the parents and children of all modified node states in the changelog.
     * 
     * @param changeLog
     *            The local changelog the should be validated
     * @throws ItemStateException
     *             If the hierarchy changes are inconsistent.
     */
    private void validateModified(ChangeLog changeLog) throws ItemStateException, RepositoryException {

        // Check all modified nodes
        for (ItemState state : changeLog.modifiedStates()) {
            if (state instanceof NodeState) {

                // Check the next node
                NodeState modifiedNodeState = (NodeState) state;
                NodeId id = modifiedNodeState.getNodeId();

                // Check whether to overlayed state is present for determining diffs
                NodeState overlayedState = (NodeState) modifiedNodeState.getOverlayedState();
                if (overlayedState == null) {
                    String message = "Unable to load persistent state for modified node " + id;
                    log.error(message);
                    throw new ItemStateException(message);
                }

                // Check the parent
                NodeId parentId = modifiedNodeState.getParentId();
                NodeId oldParentId = overlayedState.getParentId();

                // The parent should not be deleted
                if (parentId != null && changeLog.deleted(parentId)) {
                    String message = "Parent of node with id " + id + " has been deleted";
                    log.error(message);
                    throw new ItemStateException(message);
                }

                if (parentId != null && changeLog.has(parentId)) {
                    checkParent(changeLog, modifiedNodeState, parentId);
                }

                // Check whether this node is the root node
                if (parentId == null && oldParentId == null) {
                    // The root node can be ignored

                } else if (!parentId.equals(oldParentId)) {

                    // This node has been moved, check whether the parent has been modified aswell
                    if (changeLog.has(parentId)) {
                        checkParent(changeLog, modifiedNodeState, parentId);
                    } else if (!isShareable(modifiedNodeState)) {
                        String message = "New parent of node " + id + " is not present in the changelog " + id;
                        log.error(message);
                        throw new ItemStateException(message);
                    }

                    // The old parent must be modified or deleted
                    if (!changeLog.isModified(oldParentId) && !changeLog.deleted(oldParentId)) {
                        String message = "Node with id " + id
                                + " has been move, but the original parent is not part of the changelog: "
                                + oldParentId;
                        log.error(message);
                        throw new ItemStateException(message);
                    }
                }

                // Check all assigned children
                for (ChildNodeEntry entry : modifiedNodeState.getChildNodeEntries()) {

                    NodeId childId = entry.getId();

                    // Check whether this node has a deleted childid
                    if (changeLog.deleted(childId) && !changeLog.has(childId)) { // Versionable
                        String message = "Node with id " + id + " has a deleted childid: " + childId;
                        log.error(message);
                        throw new ItemStateException(message);
                    }

                    if (changeLog.has(childId)) {
                        NodeState childState = (NodeState) changeLog.get(childId);
                        checkParent(changeLog, childState, id);
                    }
                }

                // Check all children the have been added
                for (ChildNodeEntry entry : modifiedNodeState.getAddedChildNodeEntries()) {
                    NodeId childId = entry.getId();
                    if (!changeLog.has(childId)) {
                        String message = "ChildId " + childId + " has been added to parent " + id
                                + ", but is not present in the changelog";
                        log.error(message);
                        throw new ItemStateException(message);
                    }
                }

                // Check all children the have been moved or removed
                for (ChildNodeEntry entry : modifiedNodeState.getRemovedChildNodeEntries()) {
                    NodeId childId = entry.getId();
                    if (!changeLog.isModified(childId) && !changeLog.deleted(childId)) {
                        String message = "Child node entry with id " + childId
                                + " has been removed, but is not present in the changelog";
                        log.error(message);
                        throw new ItemStateException(message);
                    }
                }
            }
        }
    }

    /**
     * Check the consistency of a parent/child relationship.
     * 
     * @param changeLog
     *            The changelog to check
     * @param childState
     *            The id of the node for which the parent/child relationship should be validated.
     * @param expectedParent
     *            The expected parent id of the child node.
     * @throws ItemStateException
     *             If a inconsistency has been detected.
     */
    void checkParent(ChangeLog changeLog, NodeState childState, NodeId expectedParent) throws ItemStateException {

        // Check whether the the changelog contains an entry for the parent aswell.
        NodeId parentId = childState.getParentId();
        if (!parentId.equals(expectedParent)) {
            Set sharedSet = childState.getSharedSet();
            if (sharedSet.contains(expectedParent)) {
                return;
            }
            String message = "Child node has another parent id " + parentId + ", expected " + expectedParent;
            log.error(message);
            throw new ItemStateException(message);
        }

        if (!changeLog.has(parentId)) {
            String message = "Parent not part of changelog";
            log.error(message);
            throw new ItemStateException(message);
        }

        // Get the parent from the changelog
        NodeState parent = (NodeState) changeLog.get(parentId);

        // Get and check the child node entry from the parent
        NodeId childId = childState.getNodeId();
        ChildNodeEntry childNodeEntry = parent.getChildNodeEntry(childId);
        if (childNodeEntry == null) {
            String message = "Child not present in parent";
            log.error(message);
            throw new ItemStateException(message);
        }
    }

    /**
     * Determines whether the specified node is <i>shareable</i>, i.e. whether the mixin type <code>mix:shareable</code>
     * is either directly assigned or indirectly inherited.
     * 
     * @param state
     *            node state to check
     * @return true if the specified node is <i>shareable</i>, false otherwise.
     * @throws RepositoryException
     *             if an error occurs
     */
    private boolean isShareable(NodeState state) throws RepositoryException {
        // shortcut: check some wellknown built-in types first
        Name primary = state.getNodeTypeName();
        Set mixins = state.getMixinTypeNames();
        if (mixins.contains(NameConstants.MIX_SHAREABLE)) {
            return true;
        }

        try {
            EffectiveNodeType type = ntReg.getEffectiveNodeType(primary, mixins);
            return type.includesNodeType(NameConstants.MIX_SHAREABLE);
        } catch (NodeTypeConflictException ntce) {
            String msg = "internal error: failed to build effective node type for node " + state.getNodeId();
            log.debug(msg);
            throw new RepositoryException(msg, ntce);
        }
    }
    
    /**
     * Begin update operation. This will return an object that can itself be
     * ended/canceled.
     */
    public Update beginUpdate(ChangeLog local, EventStateCollectionFactory factory,
                              VirtualItemStateProvider virtualProvider)
            throws ReferentialIntegrityException, StaleItemStateException,
                   ItemStateException {

        Update update = new Update(local, factory, virtualProvider);
        update.begin();
        return update;
    }

    /**
     * Store modifications registered in a <code>ChangeLog</code>. The items
     * contained in the <tt>ChangeLog</tt> are not states returned by this
     * item state manager but rather must be reconnected to items provided
     * by this state manager.<p/>
     * After successfully storing the states the observation manager is informed
     * about the changes, if an observation manager is passed to this method.<p/>
     * NOTE: This method is not synchronized, because all methods it invokes
     * on instance members (such as {@link PersistenceManager#store} are
     * considered to be thread-safe. Should this ever change, the
     * synchronization status has to be re-examined.
     *
     * @param local   change log containing local items
     * @param factory event state collection factory
     * @throws ReferentialIntegrityException if a new or modified REFERENCE
     *                                       property refers to a non-existent
     *                                       target or if a removed node is still
     *                                       being referenced
     * @throws StaleItemStateException       if at least one of the affected item
     *                                       states has become stale
     * @throws ItemStateException            if another error occurs
     */
    public void update(ChangeLog local, EventStateCollectionFactory factory)
            throws ReferentialIntegrityException, StaleItemStateException,
                   ItemStateException {

        beginUpdate(local, factory, null).end();
    }

    /**
     * Handle an external update.
     *
     * @param external external change containing only node and property ids.
     * @param events events to deliver
     */
    public void externalUpdate(ChangeLog external, EventStateCollection events) {
        boolean holdingWriteLock = false;

        ISMLocking.WriteLock wLock = null;
        try {
            wLock = acquireWriteLock(external);
            holdingWriteLock = true;

            doExternalUpdate(external);
        } catch (ItemStateException e) {
            String msg = "Unable to acquire write lock.";
            log.error(msg);
        }

        ISMLocking.ReadLock rLock = null;
        try {
            if (wLock != null) {
                rLock = wLock.downgrade();
                holdingWriteLock = false;
                events.dispatch();
            }
        } finally {
            if (holdingWriteLock) {
                if (wLock != null) {
                    wLock.release();
                }
            } else {
                if (rLock != null) {
                    rLock.release();
                }
            }
        }

    }

    /**
     * Perform the external update. While executing this method, the
     * <code>writeLock</code> on this manager is held.
     *
     * @param external external change containing only node and property ids.
     */
    protected void doExternalUpdate(ChangeLog external) {
        // workaround to flush cache of persistence manager
        if (persistMgr instanceof CachingPersistenceManager) {
            ((CachingPersistenceManager) persistMgr).onExternalUpdate(external);
        }

        ChangeLog shared = new ChangeLog();

        // Build a copy of the external change log, consisting of shared
        // states we have in our cache. Inform listeners about this
        // change.
        for (ItemState state : external.modifiedStates()) {
            state = cache.retrieve(state.getId());
            if (state != null) {
                try {
                    ItemState currentState = loadItemState(state.getId());
                    state.copy(currentState, true);
                    shared.modified(state);
                } catch (NoSuchItemStateException e) {
                    // This is likely to happen because a subsequent delete
                    // of this very state has not yet been transmitted.
                    String msg = "Unable to retrieve state: " + state.getId() + ", ignored.";
                    log.info(msg);
                    state.discard();
                } catch (ItemStateException e) {
                    String msg = "Unable to retrieve state: " + state.getId();
                    log.warn(msg);
                    state.discard();
                }
            }
        }
        for (ItemState state : external.deletedStates()) {
            state = cache.retrieve(state.getId());
            if (state != null) {
                shared.deleted(state);
            }
        }
        shared.persisted();
    }

    /**
     * Add an <code>ItemStateListener</code>
     * @param listener the new listener to be informed on modifications
     */
    public void addListener(ItemStateListener listener) {
        dispatcher.addListener(listener);
    }

    /**
     * Remove an <code>ItemStateListener</code>
     * @param listener an existing listener
     */
    public void removeListener(ItemStateListener listener) {
        dispatcher.removeListener(listener);
    }

    //-------------------------------------------------------< implementation >

    /**
     * Create a new node state instance
     *
     * @param id         uuid
     * @param nodeTypeName node type name
     * @param parentId   parent UUID
     * @return new node state instance
     */
    private NodeState createInstance(NodeId id, Name nodeTypeName,
                                     NodeId parentId) {

        NodeState state = persistMgr.createNew(id);
        state.setNodeTypeName(nodeTypeName);
        state.setParentId(parentId);
        state.setStatus(ItemState.STATUS_NEW);
        state.setContainer(this);

        return state;
    }

    /**
     * Create root node state
     *
     * @param rootNodeId root node id
     * @param ntReg        node type registry
     * @return root node state
     * @throws ItemStateException if an error occurs
     */
    private NodeState createRootNodeState(NodeId rootNodeId,
                                          NodeTypeRegistry ntReg)
            throws ItemStateException {

        NodeState rootState = createInstance(rootNodeId, NameConstants.REP_ROOT, null);
        NodeState jcrSystemState = createInstance(RepositoryImpl.SYSTEM_ROOT_NODE_ID, NameConstants.REP_SYSTEM, rootNodeId);

        // FIXME need to manually setup root node by creating mandatory jcr:primaryType property
        // @todo delegate setup of root node to NodeTypeInstanceHandler

        // create jcr:primaryType property on root node state
        rootState.addPropertyName(NameConstants.JCR_PRIMARYTYPE);

        PropertyState prop = createInstance(NameConstants.JCR_PRIMARYTYPE, rootNodeId);
        prop.setValues(new InternalValue[]{InternalValue.create(NameConstants.REP_ROOT)});
        prop.setType(PropertyType.NAME);
        prop.setMultiValued(false);

        // create jcr:primaryType property on jcr:system node state
        jcrSystemState.addPropertyName(NameConstants.JCR_PRIMARYTYPE);

        PropertyState primaryTypeProp = createInstance(NameConstants.JCR_PRIMARYTYPE, jcrSystemState.getNodeId());
        primaryTypeProp.setValues(new InternalValue[]{InternalValue.create(NameConstants.REP_SYSTEM)});
        primaryTypeProp.setType(PropertyType.NAME);
        primaryTypeProp.setMultiValued(false);

        // add child node entry for jcr:system node
        rootState.addChildNodeEntry(NameConstants.JCR_SYSTEM, RepositoryImpl.SYSTEM_ROOT_NODE_ID);

        // add child node entry for virtual jcr:versionStorage
        jcrSystemState.addChildNodeEntry(NameConstants.JCR_VERSIONSTORAGE, RepositoryImpl.VERSION_STORAGE_NODE_ID);

        // add child node entry for virtual jcr:activities
        jcrSystemState.addChildNodeEntry(NameConstants.JCR_ACTIVITIES, RepositoryImpl.ACTIVITIES_NODE_ID);

        // add child node entry for virtual jcr:nodeTypes
        jcrSystemState.addChildNodeEntry(NameConstants.JCR_NODETYPES, RepositoryImpl.NODETYPES_NODE_ID);


        ChangeLog changeLog = new ChangeLog();
        changeLog.added(rootState);
        changeLog.added(prop);
        changeLog.added(jcrSystemState);
        changeLog.added(primaryTypeProp);

        persistMgr.store(changeLog);
        changeLog.persisted();

        return rootState;
    }

    /**
     * Makes sure child node entry for mandatory jcr:activities exist.
     * Repositories upgraded from 1.x do not have it.
     * <p/>
     * This method assumes that the jcr:system node already exists.
     *
     * @throws ItemStateException if an error occurs while reading or writing to
     *                            the persistence manager.
     */
    private void ensureActivitiesNode() throws ItemStateException {
        NodeState jcrSystemState = (NodeState) getNonVirtualItemState(RepositoryImpl.SYSTEM_ROOT_NODE_ID);
        if (!jcrSystemState.hasChildNodeEntry(RepositoryImpl.ACTIVITIES_NODE_ID)) {
            jcrSystemState.addChildNodeEntry(NameConstants.JCR_ACTIVITIES, RepositoryImpl.ACTIVITIES_NODE_ID);

            ChangeLog changeLog = new ChangeLog();
            changeLog.modified(jcrSystemState);

            persistMgr.store(changeLog);
            changeLog.persisted();
        }
    }

    /**
     * Returns the item state for the given id without considering virtual
     * item state providers.
     */
    private ItemState getNonVirtualItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {
        ItemState state = cache.retrieve(id);
        if (state == null) {
            synchronized (this) {
                // Use a double check to ensure that the cache entry is
                // not created twice. We don't synchronize the entire
                // method to allow the first cache retrieval to proceed
                // even when another thread is loading a new item state.
                state = cache.retrieve(id);
                if (state == null) {
                    // not found in cache, load from persistent storage
                    state = loadItemState(id);
                    state.setStatus(ItemState.STATUS_EXISTING);
                    // put it in cache
                    cache.cache(state);
                    // set parent container
                    state.setContainer(this);
                }
            }
        }
        return state;
    }

    /**
     * Checks if this item state manager has the given item state without
     * considering the virtual item state managers.
     */
    protected boolean hasNonVirtualItemState(ItemId id) {
        if (cache.isCached(id)) {
            return true;
        }

        try {
            if (id.denotesNode()) {
                return persistMgr.exists((NodeId) id);
            } else {
                return persistMgr.exists((PropertyId) id);
            }
        } catch (ItemStateException ise) {
            return false;
        }
    }

    /**
     * Create a new item state instance
     *
     * @param other other state associated with new instance
     * @return new node state instance
     */
    private ItemState createInstance(ItemState other) {
        if (other.isNode()) {
            NodeState ns = (NodeState) other;
            return createInstance(ns.getNodeId(), ns.getNodeTypeName(), ns.getParentId());
        } else {
            PropertyState ps = (PropertyState) other;
            return createInstance(ps.getName(), ps.getParentId());
        }
    }

    /**
     * Create a new property state instance
     *
     * @param propName   property name
     * @param parentId parent Id
     * @return new property state instance
     */
    private PropertyState createInstance(Name propName, NodeId parentId) {
        PropertyState state = persistMgr.createNew(new PropertyId(parentId, propName));
        state.setStatus(ItemState.STATUS_NEW);
        state.setContainer(this);

        return state;
    }

    /**
     * Load item state from persistent storage.
     *
     * @param id item id
     * @return item state
     */
    private ItemState loadItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {

        ItemState state;
        if (id.denotesNode()) {
            state = persistMgr.load((NodeId) id);
        } else {
            state = persistMgr.load((PropertyId) id);
        }
        return state;
    }

    /**
     * Acquires the read lock on this item state manager.
     *
     * @param id the id of the item for which to acquire a read lock.
     * @throws ItemStateException if the read lock cannot be acquired.
     */
    private ISMLocking.ReadLock acquireReadLock(ItemId id) throws ItemStateException {
        try {
            return ismLocking.acquireReadLock(id);
        } catch (InterruptedException e) {
            throw new ItemStateException("Interrupted while acquiring read lock");
        }
    }

    /**
     * Acquires the write lock on this item state manager.
     *
     * @param changeLog the change log for which to acquire a write lock.
     * @throws ItemStateException if the write lock cannot be acquired.
     */
    private ISMLocking.WriteLock acquireWriteLock(ChangeLog changeLog) throws ItemStateException {
        try {
            return ismLocking.acquireWriteLock(changeLog);
        } catch (InterruptedException e) {
            throw new ItemStateException("Interrupted while acquiring write lock");
        }
    }
}
