/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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

import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PersistentNodeState;

/**
 * Represents a node state delivered by the transactional subsystem. This
 * state will be backed up by a persistent node state and return live values
 * as long as no modifier method is invoked.
 */
class TransactionalNodeState extends PersistentNodeState {

    /**
     * Transaction we are participating in
     */
    private TransactionImpl tx;

    /**
     * Create a new instance of this class. Used when this state is backed
     * up by an existing persistent node state.
     *
     * @param state existing node state
     * @param tx    transaction
     */
    public TransactionalNodeState(PersistentNodeState state,
                                  Transaction tx) {

        super(state, STATUS_EXISTING, null);

        this.tx = (TransactionImpl) tx;
    }

    /**
     * Create a new instance of this class. Used when this state is not backed
     * up by an existing persistent node state.
     *
     * @param uuid UUID of this node
     * @param tx   transaction
     */
    public TransactionalNodeState(String uuid, Transaction tx) {
        super(uuid, null);

        this.tx = (TransactionImpl) tx;
    }

    /**
     * @see org.apache.jackrabbit.core.state.ItemStateListener#stateDestroyed
     *      <p/>
     *      Invoked when the underlying persistent node state has been destroyed.
     *      When such a thing happens, we change our status to
     *      {@link #STATUS_STALE_DESTROYED}.
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
     *      <p/>
     *      Invoked when the underlying persistent node state has been modified
     *      (i.e. made persistent by some other session). If we still have a
     *      clean copy of the persistent node state, we update our internal copy
     *      with the actual values.
     */
    public synchronized void stateModified(ItemState updated) {
        switch (status) {
            case STATUS_NEW:
            case STATUS_EXISTING_MODIFIED:
            case STATUS_EXISTING_REMOVED:
            case STATUS_STALE_DESTROYED:
            case STATUS_STALE_MODIFIED:
            case STATUS_UNDEFINED:
                break;
            case STATUS_EXISTING:
                copy(updated);
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
