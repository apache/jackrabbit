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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.security.AuthContext;
import org.apache.jackrabbit.core.lock.LockManager;
import org.apache.jackrabbit.core.lock.TxLockManager;
import org.apache.jackrabbit.core.lock.SharedLockManager;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.TransactionalItemStateManager;
import org.apache.log4j.Logger;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.security.auth.Subject;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.HashMap;
import java.util.Map;

/**
 * Session extension that provides XA support.
 */
public class XASessionImpl extends SessionImpl
        implements XASession, XAResource {

    /**
     * Logger instance
     */
    private static final Logger log = Logger.getLogger(XASessionImpl.class);

    /**
     * Global transactions
     */
    private static final Map txGlobal = new HashMap();

    /**
     * Known attribute name.
     */
    private static final String ATTRIBUTE_CHANGE_LOG = "ChangeLog";

    /**
     * Known attribute name.
     */
    private static final String ATTRIBUTE_LOCK_MANAGER = "LockManager";

    /**
     * Currently associated transaction
     */
    private TransactionContext tx;

    /**
     * Transaction timeout, in seconds
     */
    private int txTimeout;

    /**
     * Create a new instance of this class.
     *
     * @param rep          repository
     * @param loginContext login context containing authenticated subject
     * @param wspConfig    workspace configuration
     * @throws AccessDeniedException if the subject of the given login context
     *                               is not granted access to the specified
     *                               workspace
     * @throws RepositoryException   if another error occurs
     */
    protected XASessionImpl(RepositoryImpl rep, AuthContext loginContext,
                            WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {

        super(rep, loginContext, wspConfig);
    }

    /**
     * Create a new instance of this class.
     *
     * @param rep       repository
     * @param subject   authenticated subject
     * @param wspConfig workspace configuration
     * @throws AccessDeniedException if the given subject is not granted access
     *                               to the specified workspace
     * @throws RepositoryException   if another error occurs
     */
    protected XASessionImpl(RepositoryImpl rep, Subject subject,
                            WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {

        super(rep, subject, wspConfig);
    }

    //-------------------------------------------------------------< XASession >
    /**
     * {@inheritDoc}
     */
    public XAResource getXAResource() {
        return this;
    }

    //------------------------------------------------------------< XAResource >
    /**
     * {@inheritDoc}
     */
    public int getTransactionTimeout() throws XAException {
        return txTimeout;
    }

    /**
     * {@inheritDoc}
     */
    public boolean setTransactionTimeout(int seconds) throws XAException {
        txTimeout = seconds;
        return true;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Two resources belong to the same resource manager if both connections
     * (i.e. sessions) have the same credentials.
     */
    public boolean isSameRM(XAResource xares) throws XAException {
        if (xares instanceof XASessionImpl) {
            XASessionImpl xases = (XASessionImpl) xares;
            return stringsEqual(userId, xases.userId);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * If <code>TMNOFLAGS</code> is specified, we create a new transaction
     * context and associate it with this resource.
     * If <code>TMJOIN</code> is specified, this resource should use the
     * same transaction context as another, already known transaction.
     * If <code>TMRESUME</code> is specified, we should resume work on
     * a transaction context that was suspended earlier.
     * All other flags generate an <code>XAException</code> of type
     * <code>XAER_INVAL</code>
     */
    public void start(Xid xid, int flags) throws XAException {
        if (isAssociated()) {
            log.error("Resource already associated with a transaction.");
            throw new XAException(XAException.XAER_PROTO);
        }

        TransactionContext tx;
        if (flags == TMNOFLAGS) {
            tx = (TransactionContext) txGlobal.get(xid);
            if (tx != null) {
                throw new XAException(XAException.XAER_DUPID);
            }
            tx = createTransaction(xid);
        } else if (flags == TMJOIN) {
            tx = (TransactionContext) txGlobal.get(xid);
            if (tx == null) {
                throw new XAException(XAException.XAER_NOTA);
            }
        } else if (flags == TMRESUME) {
            tx = (TransactionContext) txGlobal.get(xid);
            if (tx == null) {
                throw new XAException(XAException.XAER_NOTA);
            }
        } else {
            throw new XAException(XAException.XAER_INVAL);
        }

        associate(tx);
    }

    /**
     * Create a global transaction.
     *
     * @param xid xid of global transaction.
     * @return transaction
     */
    private TransactionContext createTransaction(Xid xid) {
        TransactionContext tx = new TransactionContext();
        txGlobal.put(xid, tx);
        return tx;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * If <code>TMSUCCESS</code> is specified, we disassociate this session
     * from the transaction specified.
     * If <code>TMFAIL</code> is specified, we disassociate this session from
     * the transaction specified and mark the transaction rollback only.
     * If <code>TMSUSPEND</code> is specified, we disassociate this session
     * from the transaction specified.
     * All other flags generate an <code>XAException</code> of type
     * <code>XAER_INVAL</code>
     */
    public void end(Xid xid, int flags) throws XAException {
        if (!isAssociated()) {
            log.error("Resource not associated with a transaction.");
            throw new XAException(XAException.XAER_PROTO);
        }

        TransactionContext tx = (TransactionContext) txGlobal.get(xid);
        if (tx == null) {
            throw new XAException(XAException.XAER_NOTA);
        }
        if (flags == TMSUCCESS) {
            disassociate();
        } else if (flags == TMFAIL) {
            disassociate();
        } else if (flags == TMSUSPEND) {
            disassociate();
        } else {
            throw new XAException(XAException.XAER_INVAL);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int prepare(Xid xid) throws XAException {
        TransactionContext tx = (TransactionContext) txGlobal.get(xid);
        if (tx == null) {
            throw new XAException(XAException.XAER_NOTA);
        }

        TransactionalItemStateManager stateMgr = wsp.getItemStateManager();
        stateMgr.setChangeLog(getChangeLog(tx), true);

        try {
            // 1. Prepare state manager
            try {
                stateMgr.prepare();
            } catch (TransactionException e) {
                throw new ExtendedXAException(XAException.XA_RBOTHER, e);
            }

            // 2. Prepare lock manager
            try {
                TxLockManager lockMgr = getTxLockManager(tx);
                if (lockMgr != null) {
                    lockMgr.prepare();
                }
            } catch (TransactionException e) {
                stateMgr.rollback();
                throw new ExtendedXAException(XAException.XA_RBOTHER, e);
            }
            return XA_OK;

        } finally {
            stateMgr.setChangeLog(null, true);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void rollback(Xid xid) throws XAException {
        TransactionContext tx = (TransactionContext) txGlobal.get(xid);
        if (tx == null) {
            throw new XAException(XAException.XAER_NOTA);
        }

        TransactionalItemStateManager stateMgr = wsp.getItemStateManager();
        stateMgr.setChangeLog(getChangeLog(tx), true);

        try {
            // 1. Rollback changes on lock manager
            TxLockManager lockMgr = getTxLockManager(tx);
            if (lockMgr != null) {
                lockMgr.rollback();
            }

            // 2. Rollback changes on state manager
            stateMgr.rollback();

        } finally {
            stateMgr.setChangeLog(null, true);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void commit(Xid xid, boolean onePhase) throws XAException {
        TransactionContext tx = (TransactionContext) txGlobal.get(xid);
        if (tx == null) {
            throw new XAException(XAException.XAER_NOTA);
        }

        TransactionalItemStateManager stateMgr = wsp.getItemStateManager();
        stateMgr.setChangeLog(getChangeLog(tx), true);

        TxLockManager lockMgr = getTxLockManager(tx);

        try {
            // 1. Commit changes on state manager
            try {
                stateMgr.commit();
            } catch (TransactionException e) {
                if (lockMgr != null) {
                    lockMgr.rollback();
                }
                throw new ExtendedXAException(XAException.XA_RBOTHER, e);
            }

            // 2. Commit changes on lock manager
            if (lockMgr != null) {
                lockMgr.commit();
            }
        } finally {
            stateMgr.setChangeLog(null, true);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * No recovery support yet.
     */
    public Xid[] recover(int flags) throws XAException {
        return new Xid[0];
    }

    /**
     * {@inheritDoc}
     * <p/>
     * No recovery support yet.
     */
    public void forget(Xid xid) throws XAException {
    }

    /**
     * Associate this session with a global transaction. Internally, set
     * the transaction containing all transaction-local objects to be
     * used when performing item retrieval and store.
     */
    void associate(TransactionContext tx) {
        this.tx = tx;

        ChangeLog txLog = getChangeLog(tx);
        if (txLog == null) {
            txLog = new ChangeLog();
            tx.setAttribute(ATTRIBUTE_CHANGE_LOG, txLog);
        }
        wsp.getItemStateManager().setChangeLog(txLog, false);
    }

    /**
     * Return a flag indicating whether this resource is associated
     * with a transaction.
     *
     * @return <code>true</code> if this resource is associated
     *         with a transaction; otherwise <code>false</code>
     */
    boolean isAssociated() {
        return tx != null;
    }

    /**
     * Disassociate this session from a global transaction. Internally,
     * clear the transaction object.
     */
    void disassociate() {
        tx = null;

        wsp.getItemStateManager().setChangeLog(null, false);
    }

    /**
     * Compare two strings for equality. If both are <code>null</code>, this
     * is also considered to be equal.
     */
    private static boolean stringsEqual(String s1, String s2) {
        if (s1 == null) {
            return s2 == null;
        } else {
            return s1.equals(s2);
        }
    }

    /**
     * Internal XAException derived class that allows passing a base exception
     * in its constructor.
     */
    static class ExtendedXAException extends XAException {

        /**
         * Create an XAException with a given error code and a root cause.
         * @param errcode The error code identifying the exception.
         * @param cause The cause (which is saved for later retrieval by the
         *              {@link #getCause()} method).  (A <tt>null</tt> value is
         *              permitted, and indicates that the cause is nonexistent
         *              or unknown.)
         */
        public ExtendedXAException(int errcode, Throwable cause) {
            super(errcode);

            if (cause != null) {
                initCause(cause);
            }
        }
    }

    //-------------------------------------------------------< locking support >

    /**
     * Return the lock manager for this session. In a transactional environment,
     * this is a session-local object that records locking/unlocking operations
     * until final commit.
     *
     * @return lock manager for this session
     * @throws javax.jcr.RepositoryException if an error occurs
     */
    public LockManager getLockManager() throws RepositoryException {
        if (tx != null) {
            TxLockManager lockMgr = (TxLockManager) tx.getAttribute(ATTRIBUTE_LOCK_MANAGER);
            if (lockMgr == null) {
                lockMgr = new TxLockManager(
                        (SharedLockManager) super.getLockManager());
                tx.setAttribute(ATTRIBUTE_LOCK_MANAGER, lockMgr);
            }
            return lockMgr;
        }
        return super.getLockManager();
    }

    /**
     * Return the transactional change log for this session.
     *
     * @param tx transactional context
     * @return change log for this session, may be <code>null</code>
     */
    private static ChangeLog getChangeLog(TransactionContext tx) {
        return (ChangeLog) tx.getAttribute(ATTRIBUTE_CHANGE_LOG);
    }

    /**
     * Return the transactional lock manager for this session. Returns
     * <code>null</code> if no lock manager has been used yet.
     *
     * @return lock manager for this session
     */
    private static TxLockManager getTxLockManager(TransactionContext tx) {
        return (TxLockManager) tx.getAttribute(ATTRIBUTE_LOCK_MANAGER);
    }
}
