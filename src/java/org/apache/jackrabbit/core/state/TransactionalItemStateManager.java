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
public class TransactionalItemStateManager extends LocalItemStateManager {

    /**
     * Logger instance
     */
    private static Logger log = Logger.getLogger(TransactionalItemStateManager.class);

    /**
     * Known attribute name
     */
    private static final String ATTRIBUTE_CHANGE_LOG = "ChangeLog";

    /**
     * Current transactional change log
     */
    private transient ChangeLog txLog;

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
        txLog = null;

        if (tx != null) {
            txLog = (ChangeLog) tx.getAttribute(ATTRIBUTE_CHANGE_LOG);
            if (txLog == null) {
                txLog = new ChangeLog();
                tx.setAttribute(ATTRIBUTE_CHANGE_LOG, txLog);
            }
        }
    }

    /**
     * Commit changes made within a transaction
     * @param tx transaction context
     * @throws TransactionException if an error occurs
     */
    public void commit(TransactionContext tx) throws TransactionException {
        ChangeLog changeLog = (ChangeLog) tx.getAttribute(ATTRIBUTE_CHANGE_LOG);
        if (changeLog != null) {
            try {
                super.update(changeLog);
            } catch (ItemStateException e) {
                changeLog.undo(sharedStateMgr);
                throw new TransactionException("Unable to end update.", e);
            }
            changeLog.reset();
            tx.notifyCommitted();
        }
    }

    /**
     * Rollback changes made within a transaction
     * @param tx transaction context
     */
    public void rollback(TransactionContext tx) {
        ChangeLog changeLog = (ChangeLog) tx.getAttribute(ATTRIBUTE_CHANGE_LOG);
        if (changeLog != null) {
            changeLog.undo(sharedStateMgr);
        }
        tx.notifyRolledBack();
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

        if (txLog != null) {
            // check items in change log
            ItemState state = txLog.get(id);
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
        if (txLog != null) {
            // check items in change log
            try {
                ItemState state = txLog.get(id);
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

        if (txLog != null) {
            // check change log
            NodeReferences refs = txLog.get(id);
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
        if (txLog != null) {
            txLog.merge(changeLog);
        } else {
            super.update(changeLog);
        }
    }
}
