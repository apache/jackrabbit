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

import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.log4j.Logger;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.xa.XASession;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

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
     * Known attribute name
     */
    private static final String ATTRIBUTE_ITEM_STATE_MANAGER = "ItemStateManager";

    /**
     * Known attribute name
     */
    private static final String ATTRIBUTE_HIERARCHY_MANAGER = "HierarchyManager";

    /**
     * Known attribute name
     */
    private static final String ATTRIBUTE_ITEM_MANAGER = "ItemManager";

    /**
     * Known attribute name
     */
    private static final String ATTRIBUTE_ACCESS_MANAGER = "AccessManager";

    /**
     * Transaction manager
     */
    private final TransactionManager txManager;

    /**
     * Transaction timeout, in seconds
     */
    private int txTimeout;

    /**
     * Currently associated transaction
     */
    private Transaction tx;

    /**
     * Create a new instance of this class.
     *
     * @param rep         repository
     * @param credentials credentials
     * @param wspConfig   workspace configuration
     * @param txManager   transaction manager
     * @throws RepositoryException if an error occurs
     */
    public XASessionImpl(RepositoryImpl rep, Credentials credentials,
                         WorkspaceConfig wspConfig,
                         TransactionManager txManager)
            throws RepositoryException {

        super(rep, credentials, wspConfig);

        this.txManager = txManager;
    }

    //-------------------------------------------------------------< XASession >

    /**
     * @see XASession#getXAResource
     */
    public XAResource getXAResource() {
        return this;
    }

    //------------------------------------------------------------< XAResource >

    /**
     * @see XAResource#getTransactionTimeout
     */
    public int getTransactionTimeout() throws XAException {
        return txTimeout;
    }

    /**
     * @see XAResource#setTransactionTimeout
     */
    public boolean setTransactionTimeout(int seconds) throws XAException {
        txTimeout = seconds;
        return true;
    }

    /**
     * @see XAResource#isSameRM
     *      <p/>
     *      Two resources belong to the same resource manager if both connections
     *      (i.e. sessions) have the same credentials.
     */
    public boolean isSameRM(XAResource xares) throws XAException {
        if (xares instanceof XASessionImpl) {
            XASessionImpl xases = (XASessionImpl) xares;
            return stringsEqual(userId, xases.userId);
        }
        return false;
    }

    /**
     * @see XAResource#start
     *      <p/>
     *      If <code>TMNOFLAGS</code> is specified, we create a new transaction
     *      context and associate it with this resource.
     *      If <code>TMJOIN</code> is specified, this resource should use the
     *      same transaction context as another, already known transaction.
     *      If <code>TMRESUME</code> is specified, we should resume work on
     *      a transaction context that was suspended earlier.
     *      All other flags generate an <code>XAException</code> of type
     *      <code>XAER_INVAL</code>
     */
    public void start(Xid xid, int flags) throws XAException {
        if (isAssociated()) {
            log.error("Resource already associated with a transaction.");
            throw new XAException(XAException.XAER_PROTO);
        }

        Transaction tx = null;
        if (flags == TMNOFLAGS) {
            tx = txManager.getTransaction(xid);
            if (tx != null) {
                throw new XAException(XAException.XAER_DUPID);
            }
            try {
                tx = beginTransaction(xid);
            } catch (TransactionException e) {
                log.error("Unable to begin transaction.", e);
                throw new XAException(XAException.XAER_RMERR);
            }
        } else if (flags == TMJOIN) {
            tx = txManager.getTransaction(xid);
            if (tx == null) {
                throw new XAException(XAException.XAER_NOTA);
            }
        } else if (flags == TMRESUME) {
            tx = txManager.getTransaction(xid);
            if (tx == null) {
                throw new XAException(XAException.XAER_NOTA);
            }
        } else {
            throw new XAException(XAException.XAER_INVAL);
        }

        associate(tx);
    }

    /**
     * Begin a global transaction. Rebuilds all transaction-local
     * objects and stores them as attributes of the newly created transaction.
     *
     * @param xid xid of global transaction.
     * @return transaction
     */
    private Transaction beginTransaction(Xid xid) throws TransactionException {
        TransactionalStore store = new DefaultTransactionalStore(wsp.getPersistentStateManager());

        Transaction tx = txManager.beginTransaction(xid, store);

        SessionItemStateManager itemStateMgr =
                createSessionItemStateManager(new TransactionalItemStateManager(tx, store));
        tx.setAttribute(ATTRIBUTE_ITEM_STATE_MANAGER, itemStateMgr);

        HierarchyManager hierMgr = itemStateMgr.getHierarchyMgr();
        tx.setAttribute(ATTRIBUTE_HIERARCHY_MANAGER, hierMgr);

        ItemManager itemMgr = createItemManager(itemStateMgr, hierMgr);
        tx.setAttribute(ATTRIBUTE_ITEM_MANAGER, itemMgr);

        AccessManagerImpl accessMgr = createAccessManager(null, hierMgr);
        tx.setAttribute(ATTRIBUTE_ACCESS_MANAGER, accessMgr);

        return tx;
    }

    /**
     * @see XAResource#end
     *      <p/>
     *      If <code>TMSUCCESS</code> is specified, we disassociate this session
     *      from the transaction specified.
     *      If <code>TMFAIL</code> is specified, we disassociate this session from
     *      the transaction specified and mark the transaction rollback only.
     *      If <code>TMSUSPEND</code> is specified, we disassociate this session
     *      from the transaction specified.
     *      All other flags generate an <code>XAException</code> of type
     *      <code>XAER_INVAL</code>
     */
    public void end(Xid xid, int flags) throws XAException {
        if (!isAssociated()) {
            log.error("Resource not associated with a transaction.");
            throw new XAException(XAException.XAER_PROTO);
        }

        Transaction tx = txManager.getTransaction(xid);
        if (tx == null) {
            throw new XAException(XAException.XAER_NOTA);
        }
        if (flags == TMSUCCESS) {
            disassociate();
        } else if (flags == TMFAIL) {
            disassociate();
            try {
                tx.setRollbackOnly();
            } catch (TransactionException e) {
                log.error("Unable to set rollback only.", e);
                throw new XAException(XAException.XAER_RMERR);
            }
        } else if (flags == TMSUSPEND) {
            disassociate();
        } else {
            throw new XAException(XAException.XAER_INVAL);
        }
    }

    /**
     * @see XAResource#prepare
     */
    public synchronized int prepare(Xid xid) throws XAException {
        Transaction tx = txManager.getTransaction(xid);
        if (tx == null) {
            throw new XAException(XAException.XAER_NOTA);
        }
        try {
            tx.prepare();
        } catch (TransactionException e) {
            log.error("Unable to prepare transaction.", e);
            throw new XAException(XAException.XAER_RMERR);
        }
        return XA_OK;
    }

    /**
     * @see XAResource#rollback
     */
    public void rollback(Xid xid) throws XAException {
        Transaction tx = txManager.getTransaction(xid);
        if (tx == null) {
            throw new XAException(XAException.XAER_NOTA);
        }
        try {
            tx.rollback();
        } catch (TransactionException e) {
            log.error("Unable to rollback transaction.", e);
            throw new XAException(XAException.XAER_RMERR);
        }
    }

    /**
     * @see XAResource#commit
     */
    public void commit(Xid xid, boolean onePhase) throws XAException {
        Transaction tx = txManager.getTransaction(xid);
        if (tx == null) {
            throw new XAException(XAException.XAER_NOTA);
        }

        if (onePhase) {
            try {
                tx.prepare();
            } catch (TransactionException e) {
                log.error("Unable to prepare one phase transaction.", e);
                throw new XAException(XAException.XAER_RMERR);
            }
        }

        try {
            tx.commit();
        } catch (TransactionException e) {
            log.error("Unable to commit transaction.", e);
            throw new XAException(XAException.XAER_RMERR);
        }
    }

    /**
     * @see XAResource#recover
     *      <p/>
     *      No recovery support yet.
     */
    public Xid[] recover(int flags) throws XAException {
        return new Xid[0];
    }

    /**
     * @see XAResource#recover
     *      <p/>
     *      No recovery support yet.
     */
    public void forget(Xid xid) throws XAException {
    }

    /**
     * Associate this session with a global transaction. Internally, set
     * the transaction containing all transaction-local objects to be
     * used when performing item retrieval and store.
     */
    void associate(Transaction tx) {
        this.tx = tx;
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
    }

    /**
     * @see SessionImpl#getItemManager
     *      <p/>
     *      Has to be overridden to provide the session item state manager
     *      based on items contained in the current transaction
     */
    protected SessionItemStateManager getItemStateManager() {
        if (tx != null) {
            return (SessionItemStateManager) tx.getAttribute(ATTRIBUTE_ITEM_STATE_MANAGER);
        }
        return super.getItemStateManager();
    }

    /**
     * @see SessionImpl#getItemManager
     *      <p/>
     *      Has to be overridden to provide the hierarchy manager
     *      based on items contained in the current transaction
     */
    protected HierarchyManager getHierarchyManager() {
        if (tx != null) {
            return (HierarchyManager) tx.getAttribute(ATTRIBUTE_HIERARCHY_MANAGER);
        }
        return super.getHierarchyManager();
    }

    /**
     * @see SessionImpl#getItemManager
     *      <p/>
     *      Has to be overridden to provide the item manager based on items
     *      contained in the current transaction
     */
    protected ItemManager getItemManager() {
        if (tx != null) {
            return (ItemManager) tx.getAttribute(ATTRIBUTE_ITEM_MANAGER);
        }
        return super.getItemManager();
    }

    /**
     * @see SessionImpl#getAccessManager
     *      <p/>
     *      Has to be overridden to provide the access manager based on items
     *      contained in the current transaction
     */
    protected AccessManagerImpl getAccessManager() {
        if (tx != null) {
            return (AccessManagerImpl) tx.getAttribute(ATTRIBUTE_ACCESS_MANAGER);
        }
        return super.getAccessManager();
    }


    /**
     * @see SessionImpl#dispatch
     *      <p/>
     *      If we are currently associated with a transaction, the dispatch operation
     *      will be postponed until commit.
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
         * @see TransactionListener#transactionCommitted
         *      <p/>
         *      Dispatch events.
         */
        public void transactionCommitted(Transaction tx) {
            events.dispatch();
        }

        /**
         * @see TransactionListener#transactionCommitted
         *      <p/>
         *      Nothing to do.
         */
        public void transactionRolledBack(Transaction tx) {
        }
    }

    /**
     * Compare two strings for equality. If both are <code>null</code>, this
     * is also considered to be equal.
     */
    private static boolean stringsEqual(String s1, String s2) {
        return s1 == null ? s2 == null : s1.equals(s2);
    }
}
