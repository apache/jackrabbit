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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;

import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.iterators.FilterIterator;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
import org.apache.jackrabbit.data.core.InternalXAResource;
import org.apache.jackrabbit.data.core.TransactionContext;
import org.apache.jackrabbit.data.core.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension to <code>LocalItemStateManager</code> that remembers changes on
 * multiple save() requests and commits them only when an associated transaction
 * is itself committed.
 */
public class XAItemStateManager extends LocalItemStateManager implements InternalXAResource {

    /**
     * The logger instance.
     */
    private static Logger log = LoggerFactory.getLogger(XAItemStateManager.class);

    /**
     * Default change log attribute name.
     */
    private static final String DEFAULT_ATTRIBUTE_NAME = "ChangeLog";

    /**
     * This map holds the ChangeLog on a per thread basis while this state
     * manager is in one of the {@link #prepare}, {@link #commit}, {@link
     * #rollback} methods.
     */
    private final Map<Thread, ChangeLog> commitLogs = Collections.synchronizedMap(new IdentityHashMap<>());

    /**
     * Current instance-local change log.
     */
    private transient ChangeLog txLog;

    /**
     * Current update operation.
     */
    private transient SharedItemStateManager.Update update;

    /**
     * Change log attribute name.
     */
    private final String attributeName;

    /**
     * Optional virtual item state provider.
     */
    private VirtualItemStateProvider virtualProvider;

    /**
     * Creates a new instance of this class with a custom attribute name.
     *
     * @param sharedStateMgr shared state manager
     * @param factory        event state collection factory
     * @param attributeName  the attribute name, if {@code null} then a default name is used
     */
    protected XAItemStateManager(SharedItemStateManager sharedStateMgr,
                              EventStateCollectionFactory factory,
                              String attributeName,
                              ItemStateCacheFactory cacheFactory) {
        super(sharedStateMgr, factory, cacheFactory);
        if (attributeName != null) {
            this.attributeName = attributeName;
        } else {
            this.attributeName = DEFAULT_ATTRIBUTE_NAME;
        }
    }

    /**
     * Creates a new {@code XAItemStateManager} instance and registers it as an {@link ItemStateListener}
     * with the given {@link SharedItemStateManager}. 
     * 
     * @param sharedStateMgr the {@link SharedItemStateManager}
     * @param factory the {@link EventStateCollectionFactory}
     * @param attributeName the attribute name, if {@code null} then a default name is used
     * @param cacheFactory the {@link ItemStateCacheFactory}
     * @return a new {@code XAItemStateManager} instance
     */
    public static XAItemStateManager createInstance(SharedItemStateManager sharedStateMgr,
            EventStateCollectionFactory factory, String attributeName, ItemStateCacheFactory cacheFactory) {
        XAItemStateManager mgr = new XAItemStateManager(sharedStateMgr, factory, attributeName, cacheFactory);
        sharedStateMgr.addListener(mgr);
        return mgr;
    }

    /**
     * Set optional virtual item state provider.
     */
    public void setVirtualProvider(VirtualItemStateProvider virtualProvider) {
        this.virtualProvider = virtualProvider;
    }

    /**
     * {@inheritDoc}
     */
    public void associate(TransactionContext tx) {
        ChangeLog txLog = null;
        if (tx != null) {
            txLog = (ChangeLog) tx.getAttribute(attributeName);
            if (txLog == null) {
                txLog = new ChangeLog();
                tx.setAttribute(attributeName, txLog);
            }
        }
        this.txLog = txLog;
    }

    /**
     * {@inheritDoc}
     */
    public void beforeOperation(TransactionContext tx) {
        ChangeLog txLog = (ChangeLog) tx.getAttribute(attributeName);
        if (txLog != null) {
            commitLogs.put(Thread.currentThread(), txLog);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void prepare(TransactionContext tx) throws TransactionException {
        ChangeLog txLog = (ChangeLog) tx.getAttribute(attributeName);
        if (txLog != null && txLog.hasUpdates()) {
            try {
                if (virtualProvider != null) {
                    updateVirtualReferences(txLog);
                }
                update = sharedStateMgr.beginUpdate(txLog, factory, virtualProvider);
            } catch (ReferentialIntegrityException rie) {
                txLog.undo(sharedStateMgr);
                throw new TransactionException("Unable to prepare transaction.", rie);
            } catch (ItemStateException ise) {
                txLog.undo(sharedStateMgr);
                throw new TransactionException("Unable to prepare transaction.", ise);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void commit(TransactionContext tx) throws TransactionException {
        ChangeLog txLog = (ChangeLog) tx.getAttribute(attributeName);
        if (txLog != null && txLog.hasUpdates()) {
            try {
                update.end();
            } catch (ItemStateException ise) {
                txLog.undo(sharedStateMgr);
                throw new TransactionException("Unable to commit transaction.", ise);
            }
            txLog.reset();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void rollback(TransactionContext tx) {
        ChangeLog txLog = (ChangeLog) tx.getAttribute(attributeName);
        if (txLog != null && txLog.hasUpdates()) {
            if (update != null) {
                update.cancel();
            }
            txLog.undo(sharedStateMgr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void afterOperation(TransactionContext tx) {
        commitLogs.remove(Thread.currentThread());
    }

    /**
     * Returns the current change log. First tries thread-local change log,
     * then instance-local change log. Returns <code>null</code> if no
     * change log was found.
     */
    public ChangeLog getChangeLog() {
        ChangeLog changeLog = commitLogs.get(Thread.currentThread());
        if (changeLog == null) {
            changeLog = txLog;
        }
        return changeLog;
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    protected ChangeLog getChanges() {
        throw new UnsupportedOperationException("getChanges");
    }

    //-----------------------------------------------------< ItemStateManager >
    /**
     * {@inheritDoc}
     * <p>
     * If this state manager is committing changes, this method first checks
     * the commitLog ThreadLocal. Else if associated to a transaction check
     * the transactional change log. Fallback is always the call to the base
     * class.
     */
    public ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {

        if (virtualProvider != null && virtualProvider.hasItemState(id)) {
            return virtualProvider.getItemState(id);
        }
        // 1) check local changes
        ChangeLog changeLog = super.getChanges();
        ItemState state = changeLog.get(id);
        if (state != null) {
            return state;
        }
        // 2) check tx log
        changeLog = getChangeLog();
        if (changeLog != null) {
            state = changeLog.get(id);
            if (state != null) {
                return state;
            }
        }
        // 3) fallback to base class
        return super.getItemState(id);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If this state manager is committing changes, this method first checks
     * the commitLog ThreadLocal. Else if associated to a transaction check
     * the transactional change log. Fallback is always the call to the base
     * class.
     */
    public boolean hasItemState(ItemId id) {
        if (virtualProvider != null && virtualProvider.hasItemState(id)) {
            return true;
        }
        // 1) check local changes
        ChangeLog changeLog = super.getChanges();
        try {
            ItemState state = changeLog.get(id);
            if (state != null) {
                return true;
            }
        } catch (NoSuchItemStateException e) {
            // marked removed in local ism
            return false;
        }
        // if we get here, then there is no item state with
        // the given id known to the local ism
        // 2) check tx log
        changeLog = getChangeLog();
        if (changeLog != null) {
            try {
                ItemState state = changeLog.get(id);
                if (state != null) {
                    return true;
                }
            } catch (NoSuchItemStateException e) {
                // marked removed in tx log
                return false;
            }
        }
        // 3) fallback to shared ism
        return sharedStateMgr.hasItemState(id);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If this state manager is committing changes, this method first
     * checks the commitLog ThreadLocal. Else if associated to a transaction
     * check the transactional change log. Fallback is always the call to
     * the base class.
     */
    public NodeReferences getNodeReferences(NodeId id)
            throws NoSuchItemStateException, ItemStateException {

        if (virtualProvider != null && virtualProvider.hasNodeReferences(id)) {
            return virtualProvider.getNodeReferences(id);
        }
        return getReferences(id);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If this state manager is committing changes, this method first
     * checks the commitLog ThreadLocal. Else if associated to a transaction
     * check the transactional change log. Fallback is always the call to
     * the base class.
     */
    public boolean hasNodeReferences(NodeId id) {
        if (virtualProvider != null && virtualProvider.hasNodeReferences(id)) {
            return true;
        }
        try {
            return getReferences(id).hasReferences();
        } catch (ItemStateException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * If associated with a transaction, simply merge the changes given to
     * the ones already known (removing items that were first added and
     * then again deleted).
     */
    protected void update(ChangeLog changeLog)
            throws ReferentialIntegrityException, StaleItemStateException,
            ItemStateException {
        if (txLog != null) {
            txLog.merge(changeLog);
        } else {
            super.update(changeLog);
        }
    }

    //-------------------------------------------------------< implementation >

    /**
     * Returns the node references for the given <code>id</code>.
     *
     * @param id the node references id.
     * @return the node references for the given <code>id</code>.
     * @throws ItemStateException if an error occurs while reading from the
     *                            underlying shared item state manager.
     */
    private NodeReferences getReferences(NodeId id)
            throws ItemStateException {
        NodeReferences refs;
        try {
            refs = super.getNodeReferences(id);
        } catch (NoSuchItemStateException e) {
            refs = new NodeReferences(id);
        }
        // apply changes from change log
        ChangeLog changes = getChangeLog();
        if (changes != null) {
            // check removed reference properties
            for (PropertyState prop : filterReferenceProperties(changes.deletedStates())) {
                InternalValue[] values = prop.getValues();
                for (int i = 0; i < values.length; i++) {
                    if (values[i].getNodeId().equals(id)) {
                        refs.removeReference(prop.getPropertyId());
                        break;
                    }
                }
            }
            // check added reference properties
            for (PropertyState prop : filterReferenceProperties(changes.addedStates())) {
                InternalValue[] values = prop.getValues();
                for (int i = 0; i < values.length; i++) {
                    if (values[i].getNodeId().equals(id)) {
                        refs.addReference(prop.getPropertyId());
                        break;
                    }
                }
            }
            // check modified properties
            for (ItemState state : changes.modifiedStates()) {
                if (state.isNode()) {
                    continue;
                }
                try {
                    PropertyState old = (PropertyState) sharedStateMgr.getItemState(state.getId());
                    if (old.getType() == PropertyType.REFERENCE) {
                        // remove if one of the old values references the node
                        InternalValue[] values = old.getValues();
                        for (int i = 0; i < values.length; i++) {
                            if (values[i].getNodeId().equals(id)) {
                                refs.removeReference(old.getPropertyId());
                                break;
                            }
                        }
                    }
                } catch (NoSuchItemStateException e) {
                    // property is stale
                }

                PropertyState prop = (PropertyState) state;
                if (prop.getType() == PropertyType.REFERENCE) {
                    // add if modified value references node
                    InternalValue[] values = prop.getValues();
                    for (int i = 0; i < values.length; i++) {
                        if (values[i].getNodeId().equals(id)) {
                            refs.addReference(prop.getPropertyId());
                            break;
                        }
                    }
                }
            }
        }
        return refs;
    }

    /**
     * Takes an iterator over {@link ItemState}s and returns a new iterator that
     * filters out all but REFERENCE {@link PropertyState}s.
     *
     * @param itemStates item state source iterator.
     * @return iterator over reference property states.
     */
    private Iterable<PropertyState> filterReferenceProperties(
            final Iterable<ItemState> itemStates) {
        return new Iterable<PropertyState>() {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public Iterator<PropertyState> iterator() {
                return (Iterator<PropertyState>) new FilterIterator(
                        itemStates.iterator(), new Predicate<ItemState>() {
                    public boolean evaluate(ItemState state) {
                        if (!state.isNode()) {
                            PropertyState prop = (PropertyState) state;
                            return prop.getType() == PropertyType.REFERENCE;
                        }
                        return false;
                    }
                });
            }
        };
    }

    /**
     * Determine all node references whose targets only exist in the view of
     * this transaction and store the modified view back to the virtual provider.
     * @param changes change log
     * @throws ItemStateException if an error occurs
     */
    private void updateVirtualReferences(ChangeLog changes) throws ItemStateException {
        ChangeLog references = new ChangeLog();

        for (ItemState state : changes.addedStates()) {
            if (!state.isNode()) {
                PropertyState prop = (PropertyState) state;
                if (prop.getType() == PropertyType.REFERENCE) {
                    InternalValue[] vals = prop.getValues();
                    for (int i = 0; vals != null && i < vals.length; i++) {
                        addVirtualReference(
                                references, prop.getPropertyId(),
                                vals[i].getNodeId());
                    }
                }
            }
        }
        for (ItemState state : changes.modifiedStates()) {
            if (!state.isNode()) {
                PropertyState newProp = (PropertyState) state;
                PropertyState oldProp =
                        (PropertyState) getItemState(state.getId());
                if (oldProp.getType() == PropertyType.REFERENCE) {
                    InternalValue[] vals = oldProp.getValues();
                    for (int i = 0; vals != null && i < vals.length; i++) {
                        removeVirtualReference(
                                references, oldProp.getPropertyId(),
                                vals[i].getNodeId());
                    }
                }
                if (newProp.getType() == PropertyType.REFERENCE) {
                    InternalValue[] vals = newProp.getValues();
                    for (int i = 0; vals != null && i < vals.length; i++) {
                        addVirtualReference(
                                references, newProp.getPropertyId(),
                                vals[i].getNodeId());
                    }
                }
            }
        }
        for (ItemState state : changes.deletedStates()) {
            if (!state.isNode()) {
                PropertyState prop = (PropertyState) state;
                if (prop.getType() == PropertyType.REFERENCE) {
                    InternalValue[] vals = prop.getValues();
                    for (int i = 0; vals != null && i < vals.length; i++) {
                        removeVirtualReference(
                                references, prop.getPropertyId(),
                                vals[i].getNodeId());
                    }
                }
            }
        }

        virtualProvider.setNodeReferences(references);
    }

    /**
     * Add a virtual reference from some reference property to a virtual node.
     * Ignored if <code>refsId.getTargetId()</code> does not denote a
     * virtual node.
     * @param sourceId property id
     * @param targetId target node id
     */
    private void addVirtualReference(
            ChangeLog references, PropertyId sourceId, NodeId targetId)
            throws NoSuchItemStateException, ItemStateException {

        NodeReferences refs = references.getReferencesTo(targetId);
        if (refs == null) {
            refs = virtualProvider.getNodeReferences(targetId);
        }
        if (refs == null && virtualProvider.hasItemState(targetId)) {
            refs = new NodeReferences(targetId);
        }
        if (refs != null) {
            refs.addReference(sourceId);
            references.modified(refs);
        }
    }

    /**
     * Remove a virtual reference from some reference property to a virtual node.
     * Ignored if <code>refsId.getTargetId()</code> does not denote a
     * virtual node.
     * @param sourceId property id
     * @param targetId target node id
     */
    private void removeVirtualReference(
            ChangeLog references, PropertyId sourceId, NodeId targetId)
            throws NoSuchItemStateException, ItemStateException {

        NodeReferences refs = references.getReferencesTo(targetId);
        if (refs == null) {
            refs = virtualProvider.getNodeReferences(targetId);
        }
        if (refs == null && virtualProvider.hasItemState(targetId)) {
            refs = new NodeReferences(targetId);
        }
        if (refs != null) {
            refs.removeReference(sourceId);
            references.modified(refs);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Check whether the shared state modified is contained in our transactional
     * log: in that case, update its state as well, as it might get reused
     * in a subsequent transaction (see JCR-1554).
     */
    public void stateModified(ItemState modified) {
        ChangeLog changeLog = (ChangeLog) commitLogs.get(Thread.currentThread());
        if (changeLog != null) {
            ItemState local;
            if (modified.getContainer() != this) {
                // shared state was modified
                try {
                    local = changeLog.get(modified.getId());
                    if (local != null && local.isConnected()) {
                        local.pull();
                    }
                } catch (NoSuchItemStateException e) {
                    log.warn("Modified state marked for deletion: " + modified.getId());
                }
            }
        }
        super.stateModified(modified);
    }
}
