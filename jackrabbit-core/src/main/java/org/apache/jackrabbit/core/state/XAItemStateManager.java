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

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.TransactionException;
import org.apache.jackrabbit.core.TransactionContext;
import org.apache.jackrabbit.core.InternalXAResource;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.commons.collections.Predicate;

import javax.jcr.ReferentialIntegrityException;
import javax.jcr.PropertyType;
import java.util.Iterator;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.Collections;

/**
 * Extension to <code>LocalItemStateManager</code> that remembers changes on
 * multiple save() requests and commits them only when an associated transaction
 * is itself committed.
 */
public class XAItemStateManager extends LocalItemStateManager implements InternalXAResource {

    /**
     * Default change log attribute name.
     */
    private static final String DEFAULT_ATTRIBUTE_NAME = "ChangeLog";

    /**
     * This map holds the ChangeLog on a per thread basis while this state
     * manager is in one of the {@link #prepare}, {@link #commit}, {@link
     * #rollback} methods.
     */
    private final Map commitLogs = Collections.synchronizedMap(new IdentityHashMap());

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
     * Creates a new instance of this class.
     *
     * @param sharedStateMgr shared state manager
     * @param factory        event state collection factory
     */
    public XAItemStateManager(SharedItemStateManager sharedStateMgr,
                              EventStateCollectionFactory factory, ItemStateCacheFactory cacheFactory) {
        this(sharedStateMgr, factory, DEFAULT_ATTRIBUTE_NAME, cacheFactory);
    }

    /**
     * Creates a new instance of this class with a custom attribute name.
     *
     * @param sharedStateMgr shared state manager
     * @param factory        event state collection factory
     * @param attributeName  attribute name
     */
    public XAItemStateManager(SharedItemStateManager sharedStateMgr,
                              EventStateCollectionFactory factory,
                              String attributeName,
                              ItemStateCacheFactory cacheFactory) {
        super(sharedStateMgr, factory, cacheFactory);

        this.attributeName = attributeName;
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
        if (txLog != null) {
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
        if (txLog != null) {
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
        if (txLog != null) {
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
        ChangeLog changeLog = (ChangeLog) commitLogs.get(Thread.currentThread());
        if (changeLog == null) {
            changeLog = txLog;
        }
        return changeLog;
    }

    //-----------------------------------------------------< ItemStateManager >
    /**
     * {@inheritDoc}
     * <p/>
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
        ChangeLog changeLog = getChangeLog();
        if (changeLog != null) {
            ItemState state = changeLog.get(id);
            if (state != null) {
                return state;
            }
        }
        return super.getItemState(id);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * If this state manager is committing changes, this method first checks
     * the commitLog ThreadLocal. Else if associated to a transaction check
     * the transactional change log. Fallback is always the call to the base
     * class.
     */
    public boolean hasItemState(ItemId id) {
        if (virtualProvider != null && virtualProvider.hasItemState(id)) {
            return true;
        }
        ChangeLog changeLog = getChangeLog();
        if (changeLog != null) {
            try {
                ItemState state = changeLog.get(id);
                if (state != null) {
                    return true;
                }
            } catch (NoSuchItemStateException e) {
                return false;
            }
        }
        return super.hasItemState(id);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * If this state manager is committing changes, this method first
     * checks the commitLog ThreadLocal. Else if associated to a transaction
     * check the transactional change log. Fallback is always the call to
     * the base class.
     */
    public NodeReferences getNodeReferences(NodeReferencesId id)
            throws NoSuchItemStateException, ItemStateException {

        if (virtualProvider != null && virtualProvider.hasNodeReferences(id)) {
            return virtualProvider.getNodeReferences(id);
        }
        return getReferences(id);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * If this state manager is committing changes, this method first
     * checks the commitLog ThreadLocal. Else if associated to a transaction
     * check the transactional change log. Fallback is always the call to
     * the base class.
     */
    public boolean hasNodeReferences(NodeReferencesId id) {
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
     * <p/>
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
    private NodeReferences getReferences(NodeReferencesId id)
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
            UUID uuid = id.getTargetId().getUUID();
            // check removed reference properties
            for (Iterator it = filterReferenceProperties(changes.deletedStates());
                 it.hasNext(); ) {
                PropertyState prop = (PropertyState) it.next();
                InternalValue[] values = prop.getValues();
                for (int i = 0; i < values.length; i++) {
                    if (values[i].getUUID().equals(uuid)) {
                        refs.removeReference(prop.getPropertyId());
                        break;
                    }
                }
            }
            // check added reference properties
            for (Iterator it = filterReferenceProperties(changes.addedStates());
                 it.hasNext(); ) {
                PropertyState prop = (PropertyState) it.next();
                InternalValue[] values = prop.getValues();
                for (int i = 0; i < values.length; i++) {
                    if (values[i].getUUID().equals(uuid)) {
                        refs.addReference(prop.getPropertyId());
                        break;
                    }
                }
            }
            // check modified properties
            for (Iterator it = changes.modifiedStates(); it.hasNext(); ) {
                ItemState state = (ItemState) it.next();
                if (state.isNode()) {
                    continue;
                }
                try {
                    PropertyState old = (PropertyState) sharedStateMgr.getItemState(state.getId());
                    if (old.getType() == PropertyType.REFERENCE) {
                        // remove if one of the old values references the node
                        InternalValue[] values = old.getValues();
                        for (int i = 0; i < values.length; i++) {
                            if (values[i].getUUID().equals(uuid)) {
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
                        if (values[i].getUUID().equals(uuid)) {
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
    private Iterator filterReferenceProperties(Iterator itemStates) {
        return new FilterIterator(itemStates, new Predicate() {
            public boolean evaluate(Object object) {
                ItemState state = (ItemState) object;
                if (!state.isNode()) {
                    PropertyState prop = (PropertyState) state;
                    return prop.getType() == PropertyType.REFERENCE;
                }
                return false;
            }
        });
    }

    /**
     * Determine all node references whose targets only exist in the view of
     * this transaction and store the modified view back to the virtual provider.
     * @param changes change log
     * @throws ItemStateException if an error occurs
     */
    private void updateVirtualReferences(ChangeLog changes) throws ItemStateException {
        for (Iterator iter = changes.addedStates(); iter.hasNext();) {
            ItemState state = (ItemState) iter.next();
            if (!state.isNode()) {
                PropertyState prop = (PropertyState) state;
                if (prop.getType() == PropertyType.REFERENCE) {
                    InternalValue[] vals = prop.getValues();
                    for (int i = 0; vals != null && i < vals.length; i++) {
                        UUID uuid = vals[i].getUUID();
                        NodeReferencesId refsId = new NodeReferencesId(uuid);
                        addVirtualReference(prop.getPropertyId(), refsId);
                    }
                }
            }
        }
        for (Iterator iter = changes.modifiedStates(); iter.hasNext();) {
            ItemState state = (ItemState) iter.next();
            if (!state.isNode()) {
                PropertyState newProp = (PropertyState) state;
                PropertyState oldProp =
                        (PropertyState) getItemState(state.getId());
                if (oldProp.getType() == PropertyType.REFERENCE) {
                    InternalValue[] vals = oldProp.getValues();
                    for (int i = 0; vals != null && i < vals.length; i++) {
                        UUID uuid = vals[i].getUUID();
                        NodeReferencesId refsId = new NodeReferencesId(uuid);
                        removeVirtualReference(oldProp.getPropertyId(), refsId);
                    }
                }
                if (newProp.getType() == PropertyType.REFERENCE) {
                    InternalValue[] vals = newProp.getValues();
                    for (int i = 0; vals != null && i < vals.length; i++) {
                        UUID uuid = vals[i].getUUID();
                        NodeReferencesId refsId = new NodeReferencesId(uuid);
                        addVirtualReference(newProp.getPropertyId(), refsId);
                    }
                }
            }
        }
        for (Iterator iter = changes.deletedStates(); iter.hasNext();) {
            ItemState state = (ItemState) iter.next();
            if (!state.isNode()) {
                PropertyState prop = (PropertyState) state;
                if (prop.getType() == PropertyType.REFERENCE) {
                    InternalValue[] vals = prop.getValues();
                    for (int i = 0; vals != null && i < vals.length; i++) {
                        UUID uuid = vals[i].getUUID();
                        NodeReferencesId refsId = new NodeReferencesId(uuid);
                        removeVirtualReference(prop.getPropertyId(), refsId);
                    }
                }
            }
        }
    }

    /**
     * Add a virtual reference from some reference property to a virtual node.
     * Ignored if <code>refsId.getTargetId()</code> does not denote a
     * virtual node.
     * @param sourceId property id
     * @param refsId node references id
     */
    private void addVirtualReference(PropertyId sourceId,
                                     NodeReferencesId refsId)
            throws NoSuchItemStateException, ItemStateException {

        NodeReferences refs = virtualProvider.getNodeReferences(refsId);
        if (refs == null && virtualProvider.hasItemState(refsId.getTargetId())) {
            refs = new NodeReferences(refsId);
        }
        if (refs != null) {
            refs.addReference(sourceId);
            virtualProvider.setNodeReferences(refs);
        }
    }

    /**
     * Remove a virtual reference from some reference property to a virtual node.
     * Ignored if <code>refsId.getTargetId()</code> does not denote a
     * virtual node.
     * @param sourceId property id
     * @param refsId node references id
     */
    private void removeVirtualReference(PropertyId sourceId,
                                        NodeReferencesId refsId)
            throws NoSuchItemStateException, ItemStateException {

        NodeReferences refs = virtualProvider.getNodeReferences(refsId);
        if (refs == null && virtualProvider.hasItemState(refsId.getTargetId())) {
            refs = new NodeReferences(refsId);
        }
        if (refs != null) {
            refs.removeReference(sourceId);
            virtualProvider.setNodeReferences(refs);
        }
    }
}
