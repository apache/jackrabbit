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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.api.XASession;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.lock.XALockManager;
import org.apache.jackrabbit.core.security.authentication.AuthContext;
import org.apache.jackrabbit.core.state.XAItemStateManager;
import org.apache.jackrabbit.core.version.InternalVersionManager;
import org.apache.jackrabbit.core.version.InternalXAVersionManager;
import org.apache.jackrabbit.data.core.InternalXAResource;
import org.apache.jackrabbit.data.core.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.security.auth.Subject;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Session extension that provides XA support.
 */
@SuppressWarnings("deprecation")
public class XASessionImpl extends SessionImpl
        implements XASession, XAResource {

    /**
     * Logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(XASessionImpl.class);

    /**
     * Global transactions
     */
    private static final Map<Xid, TransactionContext> txGlobal =
        Collections.synchronizedMap(new HashMap<Xid, TransactionContext>());

    /**
     * System property specifying the default Transaction Timeout
     */
    public static final String SYSTEM_PROPERTY_DEFAULT_TRANSACTION_TIMEOUT = "org.apache.jackrabbit.core.defaultTransactionTimeout";

    /**
     * Default transaction timeout, in seconds.
     * Either it is specified by the System Property {@link XASessionImpl#SYSTEM_PROPERTY_DEFAULT_TRANSACTION_TIMEOUT} or
     * it is per default 5 seconds if it is not set by the TransactionManager at runtime
     */
    private static final int DEFAULT_TX_TIMEOUT = Integer.parseInt(System.getProperty(SYSTEM_PROPERTY_DEFAULT_TRANSACTION_TIMEOUT, "5"));

    /**
     * Currently associated transaction
     */
    private TransactionContext tx;

    /**
     * Transaction timeout, in seconds
     */
    private int txTimeout;

    /**
     * List of transactional resources.
     */
    private InternalXAResource[] txResources;

    /**
     * Create a new instance of this class.
     *
     * @param repositoryContext repository context
     * @param loginContext login context containing authenticated subject
     * @param wspConfig    workspace configuration
     * @throws AccessDeniedException if the subject of the given login context
     *                               is not granted access to the specified
     *                               workspace
     * @throws RepositoryException   if another error occurs
     */
    protected XASessionImpl(
            RepositoryContext repositoryContext, AuthContext loginContext,
            WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {
        super(repositoryContext, loginContext, wspConfig);
        init();
    }

    /**
     * Create a new instance of this class.
     *
     * @param repositoryContext repository context
     * @param subject   authenticated subject
     * @param wspConfig workspace configuration
     * @throws AccessDeniedException if the given subject is not granted access
     *                               to the specified workspace
     * @throws RepositoryException   if another error occurs
     */
    protected XASessionImpl(
            RepositoryContext repositoryContext, Subject subject,
            WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {
        super(repositoryContext, subject, wspConfig);
        init();
    }

    /**
     * Initialize this object.
     */
    private void init() throws RepositoryException {
        WorkspaceImpl workspace = context.getWorkspace();
        XAItemStateManager stateMgr =
            (XAItemStateManager) workspace.getItemStateManager();
        XALockManager lockMgr =
            (XALockManager) workspace.getInternalLockManager();
        InternalXAVersionManager versionMgr =
            (InternalXAVersionManager) getInternalVersionManager();

        /**
         * Create array that contains all resources that participate in this
         * transactions. Some resources depend on each other, therefore you
         * should only change the sequence if you know what you are doing!
         *
         * There are two artificial resources on the version manager (begin and
         * end), which handle locking of the version manager. The begin resource
         * acquires the write lock on the version manager in its prepare method,
         * while the end resource releases the write lock in either commit or
         * rollback. Please note that the write lock is only acquired if there
         * is something to commit by the version manager.
         * For further information see JCR-335 and JCR-962.
         */
        txResources = new InternalXAResource[] {
            versionMgr.getXAResourceBegin(),
            versionMgr, stateMgr, lockMgr,
            versionMgr.getXAResourceEnd()
        };
        stateMgr.setVirtualProvider(versionMgr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected InternalVersionManager createVersionManager()
            throws RepositoryException {
        return new InternalXAVersionManager(
                repositoryContext.getInternalVersionManager(),
                repositoryContext.getNodeTypeRegistry(),
                this,
                repositoryContext.getItemStateCacheFactory());
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
    public int getTransactionTimeout() {
        return txTimeout == 0 ? DEFAULT_TX_TIMEOUT : txTimeout;
    }

    /**
     * {@inheritDoc}
     */
    public boolean setTransactionTimeout(int seconds) {
        txTimeout = seconds;
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
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
     * <p>
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
        TransactionContext tx = txGlobal.get(xid);
        if (flags == TMNOFLAGS) {
            if (tx != null) {
                throw new XAException(XAException.XAER_DUPID);
            }
            tx = createTransaction(xid);
        } else if (flags == TMJOIN) {
            if (tx == null) {
                throw new XAException(XAException.XAER_NOTA);
            }
        } else if (flags == TMRESUME) {
            if (tx == null) {
                throw new XAException(XAException.XAER_NOTA);
            }
            if (!tx.isSuspended()) {
                log.error("Unable to resume: transaction not suspended.");
                throw new XAException(XAException.XAER_PROTO);
            }
            tx.setSuspended(false);
        } else {
            throw new XAException(XAException.XAER_INVAL);
        }

        associate(tx);
    }

    /**
     * Create a new transaction context.
     * @param xid xid of global transaction.
     * @return transaction context
     */
    private TransactionContext createTransaction(Xid xid) {
        TransactionContext tx = new TransactionContext(xid, txResources);
        txGlobal.put(xid, tx);
        return tx;
    }

    /**
     * {@inheritDoc}
     * <p>
     * If <code>TMSUCCESS</code> is specified, we disassociate this session
     * from the transaction specified.
     * If <code>TMFAIL</code> is specified, we disassociate this session from
     * the transaction specified and mark the transaction rollback only.
     * If <code>TMSUSPEND</code> is specified, we disassociate this session
     * from the transaction specified.
     * All other flags generate an <code>XAException</code> of type
     * <code>XAER_INVAL</code>
     * <p>
     * It is legal for a transaction association to be suspended and then
     * ended (either with <code>TMSUCCESS</code> or <code>TMFAIL</code>)
     * without having been resumed again.
     */
    public void end(Xid xid, int flags) throws XAException {
        TransactionContext tx = txGlobal.get(xid);
        if (tx == null) {
            throw new XAException(XAException.XAER_NOTA);
        }
        if (flags == TMSUSPEND) {
            if (!isAssociated()) {
                log.error("Resource not associated with a transaction.");
                throw new XAException(XAException.XAER_PROTO);
            }
            associate(null);
            tx.setSuspended(true);
        } else if (flags == TMFAIL || flags == TMSUCCESS) {
            if (!tx.isSuspended()) {
                if (!isAssociated()) {
                    log.error("Resource not associated with a transaction.");
                    throw new XAException(XAException.XAER_PROTO);
                }
                associate(null);
            } else {
                tx.setSuspended(false);
            }
        } else {
            throw new XAException(XAException.XAER_INVAL);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int prepare(Xid xid) throws XAException {
        TransactionContext tx = txGlobal.get(xid);
        if (tx == null) {
            throw new XAException(XAException.XAER_NOTA);
        }
        tx.prepare();
        return XA_OK;
    }

    /**
     * {@inheritDoc}
     */
    public void commit(Xid xid, boolean onePhase) throws XAException {
        TransactionContext tx = txGlobal.get(xid);
        if (tx == null) {
            throw new XAException(XAException.XAER_NOTA);
        }
        try {
        	if (onePhase) {
        		tx.prepare();
        	}
        	tx.commit();
        } finally {
        	txGlobal.remove(xid);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void rollback(Xid xid) throws XAException {
        TransactionContext tx = txGlobal.get(xid);
        if (tx == null) {
            throw new XAException(XAException.XAER_NOTA);
        }
        try {
        	tx.rollback();
        } finally {
        	txGlobal.remove(xid);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * No recovery support yet.
     */
    public Xid[] recover(int flags) throws XAException {
        return new Xid[0];
    }

    /**
     * {@inheritDoc}
     * <p>
     * No recovery support yet.
     */
    public void forget(Xid xid) throws XAException {
    }

    /**
     * Associate this session with a global transaction. Internally, set
     * the transaction containing all transaction-local objects to be
     * used when performing item retrieval and store.
     */
    public synchronized void associate(TransactionContext tx) {
        this.tx = tx;
        for (InternalXAResource txResource : txResources) {
            txResource.associate(tx);
        }
    }

    /**
     * Return a flag indicating whether this resource is associated
     * with a transaction.
     *
     * @return <code>true</code> if this resource is associated
     *         with a transaction; otherwise <code>false</code>
     */
    private boolean isAssociated() {
        return tx != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void logout() {
        super.logout();
        // dispose the caches
        try {
            ((InternalXAVersionManager) versionMgr).close();
        } catch (Exception e) {
            log.warn("error while closing InternalXAVersionManager", e);
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
}
