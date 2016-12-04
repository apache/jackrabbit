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
package org.apache.jackrabbit.webdav.jcr.transaction;

import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.WebdavResponse;
import org.apache.jackrabbit.webdav.jcr.JcrDavException;
import org.apache.jackrabbit.webdav.jcr.JcrDavSession;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.transaction.TransactionConstants;
import org.apache.jackrabbit.webdav.transaction.TransactionInfo;
import org.apache.jackrabbit.webdav.transaction.TransactionResource;
import org.apache.jackrabbit.webdav.transaction.TxActiveLock;
import org.apache.jackrabbit.webdav.transaction.TxLockManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.HashMap;
import java.util.Iterator;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * <code>TxLockManagerImpl</code> manages locks with locktype
 * '{@link TransactionConstants#TRANSACTION dcr:transaction}'.
 * <p>
 */
 //todo: removing all expired locks
 //todo: 'local' and 'global' are not accurate terms in the given context > replace
 /*todo: the usage of the 'global' transaction is not according to the JTA specification,
   which explicitly requires any transaction present on a servlet to be completed before
   the service method returns. Starting/completing transactions on the session object,
   which is possible with the jackrabbit implementation is a hack.*/
 /*todo: review of this transaction part is therefore required. Is there a use-case
   for those 'global' transactions at all...*/
public class TxLockManagerImpl implements TxLockManager {

    private static Logger log = LoggerFactory.getLogger(TxLockManagerImpl.class);

    private final TransactionMap map = new TransactionMap();

    private final Map<TransactionListener, TransactionListener> listeners = new IdentityHashMap<TransactionListener, TransactionListener>();

    /**
     * Create a new lock.
     *
     * @param lockInfo as present in the request body.
     * @param resource
     * @return the lock
     * @throws DavException             if the lock could not be obtained.
     * @throws IllegalArgumentException if the resource is <code>null</code> or
     *                                  does not implement {@link TransactionResource} interface.
     * @see LockManager#createLock(org.apache.jackrabbit.webdav.lock.LockInfo, org.apache.jackrabbit.webdav.DavResource)
     */
    public ActiveLock createLock(LockInfo lockInfo, DavResource resource)
            throws DavException {
        if (resource == null || !(resource instanceof TransactionResource)) {
            throw new IllegalArgumentException("Invalid resource");
        }
        return createLock(lockInfo, (TransactionResource) resource);
    }

    /**
     * Create a new lock.
     *
     * @param lockInfo
     * @param resource
     * @return the lock
     * @throws DavException if the request lock has the wrong lock type or if
     *                      the lock could not be obtained for any reason.
     */
    private synchronized ActiveLock createLock(LockInfo lockInfo, TransactionResource resource)
            throws DavException {
        if (!lockInfo.isDeep() || !TransactionConstants.TRANSACTION.equals(lockInfo.getType())) {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
        }

        ActiveLock existing = getLock(lockInfo.getType(), lockInfo.getScope(), resource);
        if (existing != null) {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
        // TODO: check for locks on member resources is required as well for lock is always deep!

        Transaction tx = createTransaction(resource.getLocator(), lockInfo);
        tx.start(resource);

        // keep references to this lock
        addReferences(tx, getMap(resource), resource);

        return tx.getLock();
    }

    /**
     * Build the transaction object associated by the lock.
     *
     * @param locator
     * @param lockInfo
     * @return
     */
    private Transaction createTransaction(DavResourceLocator locator, LockInfo lockInfo) {
        if (TransactionConstants.GLOBAL.equals(lockInfo.getScope())) {
            return new GlobalTransaction(locator, new TxActiveLock(lockInfo));
        } else {
            return new LocalTransaction(locator, new TxActiveLock(lockInfo));
        }
    }

    /**
     * Refresh the lock identified by the given lock token.
     *
     * @param lockInfo
     * @param lockToken
     * @param resource
     * @return the lock
     * @throws DavException
     * @throws IllegalArgumentException if the resource is <code>null</code> or
     *                                  does not implement {@link TransactionResource} interface.
     * @see LockManager#refreshLock(org.apache.jackrabbit.webdav.lock.LockInfo, String, org.apache.jackrabbit.webdav.DavResource)
     */
    public ActiveLock refreshLock(LockInfo lockInfo, String lockToken,
                                  DavResource resource) throws DavException {
        if (resource == null || !(resource instanceof TransactionResource)) {
            throw new IllegalArgumentException("Invalid resource");
        }
        return refreshLock(lockInfo, lockToken, (TransactionResource) resource);
    }

    /**
     * Reset the timeout of the lock identified by the given lock token.
     *
     * @param lockInfo
     * @param lockToken
     * @param resource
     * @return
     * @throws DavException if the lock did not exist or is expired.
     */
    private synchronized ActiveLock refreshLock(LockInfo lockInfo, String lockToken,
                                                TransactionResource resource) throws DavException {

        TransactionMap responsibleMap = getMap(resource);
        Transaction tx = responsibleMap.get(lockToken);
        if (tx == null) {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "No valid transaction lock found for resource '" + resource.getResourcePath() + "'");
        } else if (tx.getLock().isExpired()) {
            removeExpired(tx, responsibleMap, resource);
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "Transaction lock for resource '" + resource.getResourcePath() + "' was already expired.");
        } else {
            tx.getLock().setTimeout(lockInfo.getTimeout());
        }
        return tx.getLock();
    }

    /**
     * Throws UnsupportedOperationException.
     *
     * @param lockToken
     * @param resource
     * @throws DavException
     * @see LockManager#releaseLock(String, org.apache.jackrabbit.webdav.DavResource)
     */
    public void releaseLock(String lockToken, DavResource resource)
            throws DavException {
        throw new UnsupportedOperationException("A transaction lock can only be release with a TransactionInfo object and a lock token.");
    }

    /**
     * Release the lock identified by the given lock token.
     *
     * @param lockInfo
     * @param lockToken
     * @param resource
     * @throws DavException
     */
    public synchronized void releaseLock(TransactionInfo lockInfo, String lockToken,
                                         TransactionResource resource) throws DavException {
        if (resource == null) {
            throw new IllegalArgumentException("Resource must not be null.");
        }
        TransactionMap responsibleMap = getMap(resource);
        Transaction tx = responsibleMap.get(lockToken);

        if (tx == null) {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "No transaction lock found for resource '" + resource.getResourcePath() + "'");
        } else if (tx.getLock().isExpired()) {
            removeExpired(tx, responsibleMap, resource);
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "Transaction lock for resource '" + resource.getResourcePath() + "' was already expired.");
        } else {
            if (lockInfo.isCommit()) {
                TransactionListener[] txListeners;
                synchronized (listeners) {
                    txListeners = listeners.values().toArray(new TransactionListener[listeners.values().size()]);
                }
                for (TransactionListener txListener : txListeners) {
                    txListener.beforeCommit(resource, lockToken);
                }
                DavException ex = null;
                try {
                    tx.commit(resource);
                } catch (DavException e) {
                    ex = e;
                }
                for (TransactionListener txListener : txListeners) {
                    txListener.afterCommit(resource, lockToken, ex == null);
                }
                if (ex != null) {
                    throw ex;
                }
            } else {
                tx.rollback(resource);
            }
            removeReferences(tx, responsibleMap, resource);
        }
    }

    /**
     * Always returns null
     *
     * @param type
     * @param scope
     * @param resource
     * @return null
     * @see #getLock(Type, Scope, TransactionResource)
     * @see LockManager#getLock(org.apache.jackrabbit.webdav.lock.Type, org.apache.jackrabbit.webdav.lock.Scope, org.apache.jackrabbit.webdav.DavResource)
     */
    public ActiveLock getLock(Type type, Scope scope, DavResource resource) {
        return null;
    }

    /**
     * Returns true if the given lock token belongs to a lock that applies to
     * the given resource, false otherwise. The token may either be retrieved
     * from the {@link DavConstants#HEADER_LOCK_TOKEN Lock-Token header} or
     * from the {@link TransactionConstants#HEADER_TRANSACTIONID TransactionId header}.
     *
     * @param token
     * @param resource
     * @return
     * @see LockManager#hasLock(String token, DavResource resource)
     */
    public boolean hasLock(String token, DavResource resource) {
        return getLock(token, null, resource) != null;
    }

    /**
     * Return the lock applied to the given resource or <code>null</code>
     *
     * @param type
     * @param scope
     * @param resource
     * @return lock applied to the given resource or <code>null</code>
     * @see LockManager#getLock(Type, Scope, DavResource)
     *      todo: is it correct to return one that specific lock, the current session is token-holder of?
     */
    public ActiveLock getLock(Type type, Scope scope, TransactionResource resource) {
        ActiveLock lock = null;
        if (TransactionConstants.TRANSACTION.equals(type)) {
            String[] sessionTokens = resource.getSession().getLockTokens();
            int i = 0;
            while (lock == null && i < sessionTokens.length) {
                String lockToken = sessionTokens[i];
                lock = getLock(lockToken, scope, resource);
                i++;
            }
        }
        return lock;
    }

    //-----------------------------< listener support >-------------------------

    /**
     * Adds a transaction listener to this <code>TxLockManager</code>.
     * @param listener the listener to add.
     */
    public void addTransactionListener(TransactionListener listener) {
        synchronized (listeners) {
            listeners.put(listener, listener);
        }
    }

    /**
     * Removes a transaction listener from this <code>TxLockManager</code>.
     * @param listener the listener to remove.
     */
    public void removeTransactionListener(TransactionListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * @param lockToken
     * @param resource
     * @return
     */
    private ActiveLock getLock(String lockToken, Scope scope, DavResource resource) {
        if (!(resource instanceof TransactionResource)) {
            log.warn("TransactionResource expected");
            return null;
        }

        ActiveLock lock = null;
        Transaction tx = null;
        TransactionMap m = map;
        // check if main-map contains that txId
        if (m.containsKey(lockToken)) {
            tx = m.get(lockToken);
        } else {
            // look through all the nested tx-maps (i.e. global txs) for the given txId
            Iterator<Transaction> it = m.values().iterator();
            while (it.hasNext() && tx == null) {
                Transaction txMap = it.next();
                if (!txMap.isLocal()) {
                    m = (TransactionMap) txMap;
                    if (m.containsKey(lockToken)) {
                        tx = ((TransactionMap) txMap).get(lockToken);
                    }
                }
            }
        }

        if (tx != null) {
            if (tx.getLock().isExpired()) {
                removeExpired(tx, m, (TransactionResource) resource);
            } else if (tx.appliesToResource(resource) && (scope == null || tx.getLock().getScope().equals(scope))) {
                lock = tx.getLock();
            }
        }
        return lock;
    }

    /**
     * Return the map that may contain a transaction lock for the given resource.
     * In case the resource provides a transactionId, the map must be a
     * repository transaction that is identified by the given id and which in
     * turn can act as map.
     *
     * @param resource
     * @return responsible map.
     * @throws DavException if no map could be retrieved.
     */
    private TransactionMap getMap(TransactionResource resource)
            throws DavException {

        String txKey = resource.getTransactionId();
        if (txKey == null) {
            return map;
        } else {
            if (!map.containsKey(txKey)) {
                throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "Transaction map '" + map + " does not contain a transaction with TransactionId '" + txKey + "'.");
            }
            Transaction tx = map.get(txKey);
            if (tx.isLocal()) {
                throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "TransactionId '" + txKey + "' points to a local transaction, that cannot act as transaction map");
            } else if (tx.getLock() != null && tx.getLock().isExpired()) {
                removeExpired(tx, map, resource);
                throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "Attempt to retrieve an expired global transaction.");
            }
            // tx is a global transaction that acts as map as well.
            return (TransactionMap) tx;
        }
    }

    /**
     * Rollbacks the specified transaction and releases the lock. This includes
     * the removal of all references.
     *
     * @param tx
     * @param responsibleMap
     * @param resource
     */
    private static void removeExpired(Transaction tx, TransactionMap responsibleMap,
                                      TransactionResource resource) {
        log.debug("Removing expired transaction lock " + tx);
        try {
            tx.rollback(resource);
            removeReferences(tx, responsibleMap, resource);
        } catch (DavException e) {
            log.error("Error while removing expired transaction lock: " + e.getMessage());
        }
    }

    /**
     * Create the required references to the new transaction specified by tx.
     *
     * @param tx
     * @param responsibleMap
     * @param resource
     * @throws DavException
     */
    private static void addReferences(Transaction tx, TransactionMap responsibleMap,
                                      TransactionResource resource) {
        log.debug("Adding transactionId '" + tx.getId() + "' as session lock token.");
        resource.getSession().addLockToken(tx.getId());

        responsibleMap.put(tx.getId(), tx);
        resource.getSession().addReference(tx.getId());
    }

    /**
     * Remove all references to the specified transaction.
     *
     * @param tx
     * @param responsibleMap
     * @param resource
     */
    private static void removeReferences(Transaction tx, TransactionMap responsibleMap,
                                         TransactionResource resource) {
        log.debug("Removing transactionId '" + tx.getId() + "' from session lock tokens.");
        resource.getSession().removeLockToken(tx.getId());

        responsibleMap.remove(tx.getId());
        resource.getSession().removeReference(tx.getId());
    }

    /**
     * @param resource
     * @return JCR session
     */
    private static Session getRepositorySession(TransactionResource resource) throws DavException {
        return JcrDavSession.getRepositorySession(resource.getSession());
    }

    //------------------------------------------< inner classes, interfaces >---
    /**
     * Internal <code>Transaction</code> interface
     */
    private interface Transaction {

        TxActiveLock getLock();

        /**
         * @return the id of this transaction.
         */
        String getId();

        /**
         * @return path of the lock holding resource
         */
        String getResourcePath();

        /**
         * @param resource
         * @return true if the lock defined by this transaction applies to the
         *         given resource, either due to the resource holding that lock or due
         *         to a deep lock hold by any ancestor resource.
         */
        boolean appliesToResource(DavResource resource);

        /**
         * @return true if this transaction is used to allow for transient changes
         *         on the underlying repository, that may be persisted with the final
         *         UNLOCK request only.
         */
        boolean isLocal();

        /**
         * Start this transaction.
         *
         * @param resource
         * @throws DavException if an error occurs.
         */
        void start(TransactionResource resource) throws DavException;

        /**
         * Commit this transaction
         *
         * @param resource
         * @throws DavException if an error occurs.
         */
        void commit(TransactionResource resource) throws DavException;

        /**
         * Rollback this transaction.
         *
         * @param resource
         * @throws DavException if an error occurs.
         */
        void rollback(TransactionResource resource) throws DavException;
    }

    /**
     * Abstract transaction covering functionally to both implementations.
     */
    private abstract static class AbstractTransaction extends TransactionMap implements Transaction {

        private final DavResourceLocator locator;
        private final TxActiveLock lock;

        private AbstractTransaction(DavResourceLocator locator, TxActiveLock lock) {
            this.locator = locator;
            this.lock = lock;
        }

        //----------------------------------------------------< Transaction >---
        /**
         * @see #getLock()
         */
        public TxActiveLock getLock() {
            return lock;
        }

        /**
         * @see #getId()
         */
        public String getId() {
            return lock.getToken();
        }

        /**
         * @see #getResourcePath()
         */
        public String getResourcePath() {
            return locator.getResourcePath();
        }

        /**
         * @see #appliesToResource(DavResource)
         */
        public boolean appliesToResource(DavResource resource) {
            if (locator.isSameWorkspace(resource.getLocator())) {
                String lockResourcePath = getResourcePath();
                String resPath = resource.getResourcePath();

                while (!"".equals(resPath)) {
                    if (lockResourcePath.equals(resPath)) {
                        return true;
                    }
                    resPath = Text.getRelativeParent(resPath, 1);
                }
            }
            return false;
        }
    }

    /**
     * Local transaction
     */
    private final static class LocalTransaction extends AbstractTransaction {

        private LocalTransaction(DavResourceLocator locator, TxActiveLock lock) {
            super(locator, lock);
        }

        //----------------------------------------------------< Transaction >---        
        /**
         * @see org.apache.jackrabbit.webdav.jcr.transaction.TxLockManagerImpl.Transaction#isLocal() 
         */
        public boolean isLocal() {
            return true;
        }

        /**
         * @see org.apache.jackrabbit.webdav.jcr.transaction.TxLockManagerImpl.Transaction#start(TransactionResource)
         */
        public void start(TransactionResource resource) throws DavException {
            try {
                // make sure, the given resource represents an existing repository item
                if (!getRepositorySession(resource).itemExists(resource.getLocator().getRepositoryPath())) {
                    throw new DavException(DavServletResponse.SC_CONFLICT, "Unable to start local transaction: no repository item present at " + getResourcePath());
                }
            } catch (RepositoryException e) {
                log.error("Unexpected error: " + e.getMessage());
                throw new JcrDavException(e);
            }
        }

        /**
         * @see org.apache.jackrabbit.webdav.jcr.transaction.TxLockManagerImpl.Transaction#commit(TransactionResource) 
         */
        public void commit(TransactionResource resource) throws DavException {
            try {
                getItem(resource).save();
            } catch (RepositoryException e) {
                throw new JcrDavException(e);
            }
        }

        /**
         * @see org.apache.jackrabbit.webdav.jcr.transaction.TxLockManagerImpl.Transaction#rollback(TransactionResource)
         */
        public void rollback(TransactionResource resource) throws DavException {
            try {
                getItem(resource).refresh(false);
            } catch (RepositoryException e) {
                throw new JcrDavException(e);
            }
        }

        //-------------------------------------------------< TransactionMap >---
        /**
         * Always throws <code>DavException</code>.
         * 
         * @see TransactionMap#putTransaction(String, org.apache.jackrabbit.webdav.jcr.transaction.TxLockManagerImpl.Transaction) 
         */
        @Override
        public Transaction putTransaction(String key, Transaction value) throws DavException {
            throw new DavException(WebdavResponse.SC_PRECONDITION_FAILED, "Attempt to nest a new transaction into a local one.");
        }

        //--------------------------------------------------------< private >---
        /**
         * Retrieve the repository item from the given transaction resource.
         *
         * @param resource
         * @return
         * @throws PathNotFoundException
         * @throws RepositoryException
         * @throws DavException
         */
        private Item getItem(TransactionResource resource) throws PathNotFoundException, RepositoryException, DavException {
            String itemPath = resource.getLocator().getRepositoryPath();
            return getRepositorySession(resource).getItem(itemPath);
        }
    }

    /**
     * Global transaction
     */
    private static class GlobalTransaction extends AbstractTransaction {

        private Xid xid;

        private GlobalTransaction(DavResourceLocator locator, TxActiveLock lock) {
            super(locator, lock);
            xid = new XidImpl(lock.getToken());
        }

        //----------------------------------------------------< Transaction >---
        /**
         * @see org.apache.jackrabbit.webdav.jcr.transaction.TxLockManagerImpl.Transaction#isLocal()
         */
        public boolean isLocal() {
            return false;
        }

        /**
         * @see org.apache.jackrabbit.webdav.jcr.transaction.TxLockManagerImpl.Transaction#start(TransactionResource)
         */
        public void start(TransactionResource resource) throws DavException {
            XAResource xaRes = getXAResource(resource);
            try {
                xaRes.setTransactionTimeout((int) getLock().getTimeout() / 1000);
                xaRes.start(xid, XAResource.TMNOFLAGS);
            } catch (XAException e) {
                throw new DavException(DavServletResponse.SC_FORBIDDEN, e.getMessage());
            }
        }

        /**
         * @see org.apache.jackrabbit.webdav.jcr.transaction.TxLockManagerImpl.Transaction#commit(TransactionResource)
         */
        public void commit(TransactionResource resource) throws DavException {
            XAResource xaRes = getXAResource(resource);
            try {
                xaRes.commit(xid, false);
                removeLocalTxReferences(resource);
            } catch (XAException e) {
                throw new DavException(DavServletResponse.SC_FORBIDDEN, e.getMessage());
            }
        }

        /**
         * @see org.apache.jackrabbit.webdav.jcr.transaction.TxLockManagerImpl.Transaction#rollback(TransactionResource)
         */
        public void rollback(TransactionResource resource) throws DavException {
            XAResource xaRes = getXAResource(resource);
            try {
                xaRes.rollback(xid);
                removeLocalTxReferences(resource);
            } catch (XAException e) {
                throw new DavException(DavServletResponse.SC_FORBIDDEN, e.getMessage());
            }
        }

        //-------------------------------------------------< TransactionMap >---
        @Override
        public Transaction putTransaction(String key, Transaction value) throws DavException {
            if (!(value instanceof LocalTransaction)) {
                throw new DavException(WebdavResponse.SC_PRECONDITION_FAILED, "Attempt to nest global transaction into a global one.");
            }
            return super.put(key, value);
        }

        //--------------------------------------------------------< private >---
        private XAResource getXAResource(TransactionResource resource) throws DavException {
            /*
            // commented, since server should be jackrabbit independent
            Session session = resource.getSession().getRepositorySession();
            if (session instanceof XASession) {
            return ((XASession)session).getXAResource();
            } else {
            throw new DavException(DavServletResponse.SC_FORBIDDEN);
            }
            */
            throw new DavException(DavServletResponse.SC_FORBIDDEN);
        }

        private void removeLocalTxReferences(TransactionResource resource) {
            for (Object o : values()) {
                Transaction tx = (Transaction) o;
                removeReferences(tx, this, resource);
            }
        }
    }

    /**
     *
     */
    private static class TransactionMap extends HashMap<String, Transaction> {

        public Transaction get(String key) {
            Transaction tx = null;
            if (containsKey(key)) {
                tx = super.get(key);
            }
            return tx;
        }

        public Transaction putTransaction(String key, Transaction value) throws DavException {
            // any global and local transactions allowed.
            return super.put(key, value);
        }
    }

    /**
     * Private class implementing Xid interface.
     */
    private static class XidImpl implements Xid {

        private final String id;

        /**
         * Create a new Xid
         *
         * @param id
         */
        private XidImpl(String id) {
            this.id = id;
        }

        /**
         * @return 1
         * @see javax.transaction.xa.Xid#getFormatId()
         */
        public int getFormatId() {
            // todo: define reasonable format id
            return 1;
        }

        /**
         * @return an empty byte array.
         * @see javax.transaction.xa.Xid#getBranchQualifier()
         */
        public byte[] getBranchQualifier() {
            return new byte[0];
        }

        /**
         * @return id as byte array
         * @see javax.transaction.xa.Xid#getGlobalTransactionId()
         */
        public byte[] getGlobalTransactionId() {
            return id.getBytes();
        }
    }
}
