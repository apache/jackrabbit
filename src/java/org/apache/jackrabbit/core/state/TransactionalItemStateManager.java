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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.ItemId;
import org.apache.log4j.Logger;

/**
 * Extension to <code>LocalItemStateManager</code> that remembers changes on
 * multiple save() requests and commits them only when an associated transaction
 * is itself committed.
 */
public class TransactionalItemStateManager extends LocalItemStateManager
        implements TransactionListener {

    /**
     * Logger instance
     */
    private static Logger log = Logger.getLogger(TransactionalItemStateManager.class);

    /**
     * Known attribute name
     */
    private static final String ATTRIBUTE_CHANGE_LOG = "ChangeLog";

    /**
     * Currently associated transaction
     */
    private transient TransactionContext tx;

    /**
     * Current change log
     */
    private transient ChangeLog changeLog;

    /**
     * Creates a new <code>LocalItemStateManager</code> instance.
     *
     * @param sharedStateMgr shared state manager
     */
    public TransactionalItemStateManager(SharedItemStateManager sharedStateMgr) {
        super(sharedStateMgr);
    }

    /**
     * Set transaction context.
     * @param tx transaction context.
     */
    public void setTransactionContext(TransactionContext tx) {
        if (tx != null) {
            changeLog = (ChangeLog) tx.getAttribute(ATTRIBUTE_CHANGE_LOG);
            if (changeLog == null) {
                changeLog = new ChangeLog();
                tx.setAttribute(ATTRIBUTE_CHANGE_LOG, changeLog);
                tx.addListener(this);
            }
        }
        this.tx = tx;
    }

    //-----------------------------------------------------< ItemStateManager >

    /**
     * @see ItemStateManager#getItemState(org.apache.jackrabbit.core.ItemId)
     *
     * If associated to a transaction, check our transactional
     * change log first.
     */
    public ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {

        if (tx != null) {
            // check items in change log
            ItemState state = changeLog.get(id);
            if (state != null) {
                return state;
            }
        }
        return super.getItemState(id);
    }

    /**
     * @see ItemStateManager#hasItemState(org.apache.jackrabbit.core.ItemId)
     *
     * If associated to a transaction, check our transactional
     * change log first.
     */
    public boolean hasItemState(ItemId id) {
        if (tx != null) {
            // check items in change log
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
     * @see ItemStateManager#getNodeReferences
     *
     * If associated to a transaction, check our transactional
     * change log first.
     */
    public NodeReferences getNodeReferences(NodeReferencesId id)
            throws NoSuchItemStateException, ItemStateException {

        if (tx != null) {
            // check change log
            NodeReferences refs = changeLog.get(id);
            if (refs != null) {
                return refs;
            }
        }
        return super.getNodeReferences(id);
    }

    /**
     * @see LocalItemStateManager#update
     *
     * If associated to a transaction, simply merge the changes given to
     * the ones already known (removing items that were first added and
     * then again deleted).
     */
    protected void update(ChangeLog changeLog) throws ItemStateException {
        if (tx != null) {
            this.changeLog.merge(changeLog);
        } else {
            super.update(changeLog);
        }
    }


    //--------------------------------------------------< TransactionListener >

    /**
     * @see TransactionListener#transactionCommitted
     */
    public void transactionCommitted(TransactionContext tx)
            throws TransactionException {

        ChangeLog changeLog = (ChangeLog) tx.getAttribute(ATTRIBUTE_CHANGE_LOG);
        if (changeLog != null) {
            try {
                super.update(changeLog);
                changeLog.reset();
            } catch (ItemStateException e) {
                throw new TransactionException("Unable to end update.", e);
            }
        }
    }

    /**
     * @see TransactionListener#transactionRolledBack
     */
    public void transactionRolledBack(TransactionContext tx) {
        ChangeLog changeLog = (ChangeLog) tx.getAttribute(ATTRIBUTE_CHANGE_LOG);
        if (changeLog != null) {
            changeLog.undo(sharedStateMgr);
        }
    }
}
