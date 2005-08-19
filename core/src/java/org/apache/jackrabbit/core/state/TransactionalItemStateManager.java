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
import org.apache.jackrabbit.core.WorkspaceImpl;
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
     * ThreadLocal that holds the ChangeLog while this item state manager
     * is in commit().
     */
    private static ThreadLocal commitLog = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return new CommitLog();
        }
    };

    /**
     * Current transactional change log
     */
    private transient ChangeLog txLog;

    /**
     * Creates a new <code>LocalItemStateManager</code> instance.
     *
     * @param sharedStateMgr shared state manager
     */
    public TransactionalItemStateManager(SharedItemStateManager sharedStateMgr, WorkspaceImpl wspImpl) {
        super(sharedStateMgr, wspImpl);
    }

    /**
     * Set transaction context.
     *
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
     * Prepare a transaction
     *
     * @param tx transaction context
     * @throws TransactionException if an error occurs
     */
    public void prepare(TransactionContext tx) throws TransactionException {
        ChangeLog changeLog = (ChangeLog) tx.getAttribute(ATTRIBUTE_CHANGE_LOG);
        if (changeLog != null) {
            try {
                sharedStateMgr.checkTargetsExist(changeLog);
            } catch (ItemStateException e) {
                log.error(e);
                changeLog.undo(sharedStateMgr);
                throw new TransactionException("Unable to prepare transaction.", e);
            }
        }
    }

    /**
     * Commit changes made within a transaction
     *
     * @param tx transaction context
     * @throws TransactionException if an error occurs
     */
    public void commit(TransactionContext tx) throws TransactionException {
        ChangeLog changeLog = (ChangeLog) tx.getAttribute(ATTRIBUTE_CHANGE_LOG);
        if (changeLog != null) {
            try {
                // set changeLog in ThreadLocal
                ((CommitLog) commitLog.get()).setChanges(changeLog);
                super.update(changeLog);
            } catch (ItemStateException e) {
                log.error(e);
                changeLog.undo(sharedStateMgr);
                throw new TransactionException("Unable to commit transaction.", e);
            } finally {
                ((CommitLog) commitLog.get()).setChanges(null);
            }
            changeLog.reset();
            tx.notifyCommitted();
        }
    }

    /**
     * Rollback changes made within a transaction
     *
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
     * {@inheritDoc}
     * <p/>
     * If this state manager is committing changes, this method first checks
     * the commitLog ThreadLocal. Else if associated to a transaction check
     * the transactional change log. Fallback is always the call to the base
     * class.
     */
    public ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {

        ChangeLog changeLog = ((CommitLog) commitLog.get()).getChanges();
        if (changeLog != null) {
            // check items in commit log
            ItemState state = changeLog.get(id);
            if (state != null) {
                return state;
            }
        } else if (txLog != null) {
            // check items in change log
            ItemState state = txLog.get(id);
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
        ChangeLog changeLog = ((CommitLog) commitLog.get()).getChanges();
        if (changeLog != null) {
            // check items in commit log
            try {
                ItemState state = changeLog.get(id);
                if (state != null) {
                    return true;
                }
            } catch (NoSuchItemStateException e) {
                return false;
            }
        } else if (txLog != null) {
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
     * {@inheritDoc}
     * <p/>
     * If this state manager is committing changes, this method first
     * checks the commitLog ThreadLocal. Else if associated to a transaction
     * check the transactional change log. Fallback is always the call to
     * the base class.
     */
    public NodeReferences getNodeReferences(NodeReferencesId id)
            throws NoSuchItemStateException, ItemStateException {

        ChangeLog changeLog = ((CommitLog) commitLog.get()).getChanges();
        if (changeLog != null) {
            // check commit log
            NodeReferences refs = changeLog.get(id);
            if (refs != null) {
                return refs;
            }
        } else if (txLog != null) {
            // check change log
            NodeReferences refs = txLog.get(id);
            if (refs != null) {
                return refs;
            }
        }
        return super.getNodeReferences(id);
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
        ChangeLog changeLog = ((CommitLog) commitLog.get()).getChanges();
        if (changeLog != null) {
            // check commit log
            if (changeLog.get(id) != null) {
                return true;
            }
        } else if (txLog != null) {
            // check change log
            if (txLog.get(id) != null) {
                return true;
            }
        }
        return super.hasNodeReferences(id);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * If associated to a transaction, simply merge the changes given to
     * the ones already known (removing items that were first added and
     * then again deleted).
     */
    protected void update(ChangeLog changeLog)
            throws StaleItemStateException, ItemStateException {
        if (txLog != null) {
            txLog.merge(changeLog);
        } else {
            super.update(changeLog);
        }
    }

    //--------------------------< inner classes >-------------------------------

    /**
     * Helper class that serves as a container for a ChangeLog in a ThreadLocal.
     * The <code>CommitLog</code> is associated with a <code>ChangeLog</code>
     * while the <code>TransactionalItemStateManager</code> is in the commit
     * method.
     */
    private static class CommitLog {

        /**
         * The changes that are about to be committed
         */
        private ChangeLog changes;

        /**
         * Sets changes that are about to be committed.
         *
         * @param changes that are about to be committed, or <code>null</code>
         *                if changes have been committed and the commit log should be reset.
         */
        private void setChanges(ChangeLog changes) {
            this.changes = changes;
        }

        /**
         * The changes that are about to be committed, or <code>null</code> if
         * the <code>TransactionalItemStateManager</code> is currently not
         * committing any changes.
         *
         * @return the changes about to be committed.
         */
        private ChangeLog getChanges() {
            return changes;
        }
    }
}
