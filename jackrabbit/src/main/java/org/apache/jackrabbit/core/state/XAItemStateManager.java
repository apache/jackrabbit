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
import org.apache.jackrabbit.core.TransactionException;
import org.apache.jackrabbit.core.TransactionContext;
import org.apache.jackrabbit.core.InternalXAResource;
import org.apache.log4j.Logger;

import javax.jcr.ReferentialIntegrityException;

/**
 * Extension to <code>LocalItemStateManager</code> that remembers changes on
 * multiple save() requests and commits them only when an associated transaction
 * is itself committed.
 */
public class XAItemStateManager extends LocalItemStateManager implements InternalXAResource {

    /**
     * Logger instance.
     */
    private static Logger log = Logger.getLogger(XAItemStateManager.class);

    /**
     * Default change log attribute name.
     */
    private static final String DEFAULT_ATTRIBUTE_NAME = "ChangeLog";

    /**
     * ThreadLocal that holds the ChangeLog while this state manager is in one
     * of the {@link #prepare}, {@link #commit}, {@link #rollback}
     * methods.
     */
    private ThreadLocal commitLog = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return new CommitLog();
        }
    };

    /**
     * Current instance-local change log.
     */
    private transient ChangeLog txLog;

    /**
     * Change log attribute name.
     */
    private final String attributeName;

    /**
     * Creates a new instance of this class.
     * @param sharedStateMgr shared state manager
     * @param wspImpl workspace
     */
    public XAItemStateManager(SharedItemStateManager sharedStateMgr,
                              WorkspaceImpl wspImpl) {
        this(sharedStateMgr, wspImpl, DEFAULT_ATTRIBUTE_NAME);
    }

    /**
     * Creates a new instance of this class with a custom attribute name.
     * @param sharedStateMgr shared state manager
     * @param wspImpl workspace
     * @param attributeName attribute name
     */
    public XAItemStateManager(SharedItemStateManager sharedStateMgr,
                              WorkspaceImpl wspImpl, String attributeName) {
        super(sharedStateMgr, wspImpl);

        this.attributeName = attributeName;
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
    public void prepare(TransactionContext tx) throws TransactionException {
        ChangeLog txLog = (ChangeLog) tx.getAttribute(attributeName);
        if (txLog != null) {
            try {
                ((CommitLog) commitLog.get()).setChanges(txLog);
                sharedStateMgr.checkReferentialIntegrity(txLog);
            } catch (ReferentialIntegrityException rie) {
                log.error(rie);
                txLog.undo(sharedStateMgr);
                throw new TransactionException("Unable to prepare transaction.", rie);
            } catch (ItemStateException ise) {
                log.error(ise);
                txLog.undo(sharedStateMgr);
                throw new TransactionException("Unable to prepare transaction.", ise);
            } finally {
                ((CommitLog) commitLog.get()).setChanges(null);
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
                ((CommitLog) commitLog.get()).setChanges(txLog);
                super.update(txLog);
            } catch (ReferentialIntegrityException rie) {
                log.error(rie);
                txLog.undo(sharedStateMgr);
                throw new TransactionException("Unable to commit transaction.", rie);
            } catch (ItemStateException ise) {
                log.error(ise);
                txLog.undo(sharedStateMgr);
                throw new TransactionException("Unable to commit transaction.", ise);
            } finally {
                ((CommitLog) commitLog.get()).setChanges(null);
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
            try {
                ((CommitLog) commitLog.get()).setChanges(txLog);
                txLog.undo(sharedStateMgr);
            } finally {
                ((CommitLog) commitLog.get()).setChanges(null);
            }
        }
    }

    /**
     * Returns the current change log. First tries thread-local change log,
     * then instance-local change log. Returns <code>null</code> if no
     * change log was found.
     */
    public ChangeLog getChangeLog() {
        ChangeLog changeLog = ((CommitLog) commitLog.get()).getChanges();
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

        ChangeLog changeLog = getChangeLog();
        if (changeLog != null) {
            NodeReferences refs = changeLog.get(id);
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
        ChangeLog changeLog = getChangeLog();
        if (changeLog != null) {
            if (changeLog.get(id) != null) {
                return true;
            }
        }
        return super.hasNodeReferences(id);
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
