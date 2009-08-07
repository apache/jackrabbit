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
package org.apache.jackrabbit.core.lock;

import org.apache.jackrabbit.core.InternalXAResource;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.TransactionContext;
import org.apache.jackrabbit.core.TransactionException;
import org.apache.jackrabbit.spi.Path;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

/**
 * Session-local lock manager that implements the semantical changes inside
 * transactions. This manager validates lock/unlock operations inside its
 * view of the locking space.
 */
public class XALockManager implements LockManager, InternalXAResource {

    /**
     * Attribute name for XA Environment.
     */
    private static final String XA_ENV_ATTRIBUTE_NAME = "XALockManager.XAEnv";

    /**
     * Global lock manager.
     */
    private final LockManagerImpl lockMgr;

    /**
     * Current XA environment.
     */
    private XAEnvironment xaEnv;

    /**
     * Create a new instance of this class.
     * @param lockMgr lockMgr global lock manager
     */
    public XALockManager(LockManagerImpl lockMgr) {
        this.lockMgr = lockMgr;
    }

    //----------------------------------------------------------< LockManager >

    /**
     * {@inheritDoc}
     */
    public Lock lock(NodeImpl node, boolean isDeep, boolean isSessionScoped)
            throws LockException, RepositoryException {
        return lock(node, isDeep, isSessionScoped, AbstractLockInfo.TIMEOUT_INFINITE, null);
    }

    /**
     * @see LockManager#lock(NodeImpl, boolean, boolean, long, String)
     */
    public Lock lock(NodeImpl node, boolean isDeep, boolean isSessionScoped, long timoutHint, String ownerInfo)
            throws LockException, RepositoryException {
        AbstractLockInfo info;
        if (isInXA()) {
            info = xaEnv.lock(node, isDeep, isSessionScoped, timoutHint, ownerInfo);
        } else {
            info = lockMgr.internalLock(node, isDeep, isSessionScoped, timoutHint, ownerInfo);
        }
        lockMgr.writeLockProperties(node, info.lockOwner, info.deep);
        return new XALock(this, info, node);
    }

    /**
     * {@inheritDoc}
     */
    public Lock getLock(NodeImpl node) throws LockException, RepositoryException {
        AbstractLockInfo info;
        if (isInXA()) {
            info = xaEnv.getLockInfo(node);
        } else {
            info = lockMgr.getLockInfo(node.getNodeId());
        }
        if (info == null) {
            throw new LockException("Node not locked: " + node);
        }
        SessionImpl session = (SessionImpl) node.getSession();
        NodeImpl holder = (NodeImpl) session.getItemManager().getItem(info.getId());
        return new XALock(this, info, holder);
    }

    /**
     * {@inheritDoc}
     */
    public Lock[] getLocks(SessionImpl session) throws RepositoryException {
        AbstractLockInfo[] infos;
        if (isInXA()) {
            infos = xaEnv.getLockInfos(session);
        } else {
            infos = lockMgr.getLockInfos(session);
        }

        XALock[] locks = new XALock[infos.length];

        for (int i = 0; i < infos.length; i++) {
            AbstractLockInfo info = infos[i];
            NodeImpl holder = (NodeImpl) session.getItemManager().getItem(info.getId());
            locks[i] = new XALock(this, info, holder);
        }
        return locks;
    }

    /**
     * {@inheritDoc}
     */
    public void unlock(NodeImpl node) throws LockException, RepositoryException {
        lockMgr.removeLockProperties(node);
        if (isInXA()) {
            xaEnv.unlock(node);
        } else {
            lockMgr.internalUnlock(node);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean holdsLock(NodeImpl node) throws RepositoryException {
        AbstractLockInfo info;
        if (isInXA()) {
            info = xaEnv.getLockInfo(node);
        } else {
            info = lockMgr.getLockInfo(node.getNodeId());
        }
        return info != null && info.getId().equals(node.getId());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLockHolder(Session session, NodeImpl node)
            throws RepositoryException {
        AbstractLockInfo info;
        if (isInXA()) {
            info = xaEnv.getLockInfo(node);
        } else {
            info = lockMgr.getLockInfo(node.getNodeId());
        }
        return info != null && info.getId().equals(node.getId())
                && info.getLockHolder() == session;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLocked(NodeImpl node) throws RepositoryException {
        AbstractLockInfo info;
        if (isInXA()) {
            info = xaEnv.getLockInfo(node);
        } else {
            info = lockMgr.getLockInfo(node.getNodeId());
        }
        return info != null;
    }

    /**
     * {@inheritDoc}
     */
    public void checkLock(NodeImpl node) throws LockException, RepositoryException {
        AbstractLockInfo info;
        if (isInXA()) {
            info = xaEnv.getLockInfo(node);
        } else {
            info = lockMgr.getLockInfo(node.getNodeId());
        }
        if (info != null && info.getLockHolder() != node.getSession()) {
            throw new LockException("Node locked.");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkLock(Path path, Session session)
            throws LockException, RepositoryException {

        SessionImpl sessionImpl = (SessionImpl) session;
        checkLock(sessionImpl.getItemManager().getNode(path));
    }

    /**
     * {@inheritDoc}
     */
    public void lockTokenAdded(SessionImpl session, String lt) throws RepositoryException {
        if (isInXA()) {
            xaEnv.addLockToken(session, lt);
        } else {
            lockMgr.lockTokenAdded(session, lt);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void lockTokenRemoved(SessionImpl session, String lt) throws RepositoryException {
        if (isInXA()) {
            xaEnv.removeLockToken(session, lt);
        } else {
            lockMgr.lockTokenRemoved(session, lt);
        }
    }

    //-----------------------------------------------------------< transaction >

    /**
     * {@inheritDoc}
     */
    public void associate(TransactionContext tx) {
        XAEnvironment xaEnv = null;
        if (tx != null) {
            xaEnv = (XAEnvironment) tx.getAttribute(XA_ENV_ATTRIBUTE_NAME);
            if (xaEnv == null) {
                xaEnv = new XAEnvironment(lockMgr);
                tx.setAttribute(XA_ENV_ATTRIBUTE_NAME, xaEnv);
            }
        }
        this.xaEnv = xaEnv;
    }

    /**
     * {@inheritDoc}
     */
    public void beforeOperation(TransactionContext tx) {
    }

    /**
     * {@inheritDoc}
     */
    public void prepare(TransactionContext tx) throws TransactionException {
        XAEnvironment xaEnv = (XAEnvironment) tx.getAttribute(XA_ENV_ATTRIBUTE_NAME);
        if (xaEnv != null) {
            xaEnv.prepare();
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This will finish the update and unlock the shared lock manager.
     */
    public void commit(TransactionContext tx) {
        XAEnvironment xaEnv = (XAEnvironment) tx.getAttribute(XA_ENV_ATTRIBUTE_NAME);
        if (xaEnv != null) {
            xaEnv.commit();
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This will undo all updates and unlock the shared lock manager.
     */
    public void rollback(TransactionContext tx) {
        XAEnvironment xaEnv = (XAEnvironment) tx.getAttribute(XA_ENV_ATTRIBUTE_NAME);
        if (xaEnv != null) {
            xaEnv.rollback();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void afterOperation(TransactionContext tx) {
    }

    /**
     * Return a flag indicating whether a lock info belongs to a different
     * XA environment.
     */
    public boolean differentXAEnv(AbstractLockInfo info) {
        if (isInXA()) {
            return xaEnv.differentXAEnv(info);
        } else {
            return info instanceof XAEnvironment.LockInfo;
        }
    }

    /**
     * Return a flag indicating whether this version manager is currently
     * associated with an XA transaction.
     */
    private boolean isInXA() {
        return xaEnv != null;
    }
}
