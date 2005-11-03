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
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.security.AuthContext;
import org.apache.jackrabbit.core.state.TransactionContext;
import org.apache.jackrabbit.core.state.TransactionException;
import org.apache.jackrabbit.core.state.TransactionListener;
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
        return XA_OK;
    }

    /**
     * {@inheritDoc}
     */
    public void rollback(Xid xid) throws XAException {
        TransactionContext tx = (TransactionContext) txGlobal.get(xid);
        if (tx == null) {
            throw new XAException(XAException.XAER_NOTA);
        }
        wsp.getItemStateManager().rollback(tx);
    }

    /**
     * {@inheritDoc}
     */
    public void commit(Xid xid, boolean onePhase) throws XAException {
        TransactionContext tx = (TransactionContext) txGlobal.get(xid);
        if (tx == null) {
            throw new XAException(XAException.XAER_NOTA);
        }

        try {
            wsp.getItemStateManager().commit(tx);
        } catch (TransactionException e) {
            throw new ExtendedXAException(XAException.XA_RBOTHER, e);
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

        wsp.getItemStateManager().setTransactionContext(tx);
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

        wsp.getItemStateManager().setTransactionContext(null);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * If we are currently associated with a transaction, the dispatch operation
     * will be postponed until commit.
     */
    protected void dispatch(EventStateCollection events) {
        if (tx != null) {
            tx.addListener(new EventDispatcher(events));
            return;
        }
        super.dispatch(events);
    }

    /**
     * Internal {@link TransactionListener} implementation that will dispatch
     * events only when a transaction has actually been committed.
     */
    static class EventDispatcher implements TransactionListener {

        /**
         * Events to dispatch if transaction is committed
         */
        private final EventStateCollection events;

        /**
         * Create a new instance of this class.
         *
         * @param events events to dispatch on commit
         */
        public EventDispatcher(EventStateCollection events) {
            this.events = events;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * Dispatch events.
         */
        public void transactionCommitted(TransactionContext tx) {
            events.dispatch();
        }

        /**
         * {@inheritDoc}
         * <p/>
         * Nothing to do.
         */
        public void transactionRolledBack(TransactionContext tx) {
        }
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
}
