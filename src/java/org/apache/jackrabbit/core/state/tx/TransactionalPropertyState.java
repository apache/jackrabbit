/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.state.tx;

import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PersistentPropertyState;

/**
 * Represents a property state delivered by the transactional subsystem. This
 * state will be backed up by a persistent property state and return live values
 * as long as no modifier method is invoked.
 */
class TransactionalPropertyState extends PersistentPropertyState {

    /**
     * Transaction we are participating in
     */
    private TransactionImpl tx;

    /**
     * Create a new instance of this class. Used when this state is backed
     * up by an existing persistent property state.
     *
     * @param state existing property state
     * @param tx    transaction
     */
    public TransactionalPropertyState(PersistentPropertyState state,
                                      Transaction tx) {

        super(state, STATUS_EXISTING, null);

        this.tx = (TransactionImpl) tx;
    }

    /**
     * Create a new instance of this class. Used when this state is not backed
     * up by an existing persistent property state.
     *
     * @param name       qualified name
     * @param parentUUID parent UUID
     * @param tx         transaction
     */
    public TransactionalPropertyState(QName name, String parentUUID,
                                      Transaction tx) {

        super(name, parentUUID, null);

        this.tx = (TransactionImpl) tx;
    }

    /**
     * @see org.apache.jackrabbit.core.state.ItemStateListener#stateDestroyed
     */
    public synchronized void stateDestroyed(ItemState destroyed) {
        switch (status) {
            case STATUS_NEW:
            case STATUS_EXISTING_REMOVED:
            case STATUS_STALE_DESTROYED:
            case STATUS_STALE_MODIFIED:
            case STATUS_UNDEFINED:
                break;
            case STATUS_EXISTING:
            case STATUS_EXISTING_MODIFIED:
                status = STATUS_STALE_DESTROYED;
                break;
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.ItemStateListener#stateModified
     */
    public synchronized void stateModified(ItemState modified) {
        switch (status) {
            case STATUS_NEW:
            case STATUS_EXISTING_MODIFIED:
            case STATUS_EXISTING_REMOVED:
            case STATUS_STALE_DESTROYED:
            case STATUS_STALE_MODIFIED:
            case STATUS_UNDEFINED:
                break;
            case STATUS_EXISTING:
                copy(modified);
                break;
        }
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistableItemState#store
     *      <p/>
     *      A store on an item disconnects the transactional state from the
     *      actual persistent state, i.e. changes to the underlying persistent
     *      state are no longer reflected in this state.
     */
    public synchronized void store() throws ItemStateException {
        try {
            tx.enlist(this, status);
        } catch (TransactionException e) {
            throw new ItemStateException("Unable to store item.", e);
        }

        if (status == STATUS_NEW) {
            notifyStateCreated();
        } else {
            notifyStateUpdated();
        }
        status = STATUS_EXISTING;
    }

    /**
     * @see org.apache.jackrabbit.core.state.PersistableItemState#destroy
     *      <p/>
     *      A store on an item disconnects the transactional state from the
     *      actual persistent state, i.e. changes to the underlying persistent
     *      state are no longer reflected in this state.
     */
    public synchronized void destroy() throws ItemStateException {
        try {
            tx.enlist(this, STATUS_EXISTING_REMOVED);
        } catch (TransactionException e) {
            throw new ItemStateException("Unable to destroy item.", e);
        }
        notifyStateDestroyed();

        status = STATUS_UNDEFINED;
    }
}
