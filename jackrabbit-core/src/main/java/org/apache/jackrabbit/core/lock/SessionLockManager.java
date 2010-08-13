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

import java.util.HashSet;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

import org.apache.jackrabbit.core.ItemValidator;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>SessionLockManager</code> implements the
 * {@link javax.jcr.lock.LockManager}. In contrast
 * to the internal {@link LockManager} interface that is created once
 * for each <code>WorkspaceInfo</code>, the JSR 283 <code>LockManager</code>
 * is associated with a single <code>Session</code> and its
 * <code>Workspace</code>.
 *
 * @see javax.jcr.Workspace#getLockManager()
 */
public class SessionLockManager implements javax.jcr.lock.LockManager {

    private static Logger log = LoggerFactory.getLogger(SessionLockManager.class);

    /**
     * Component context of the current session
     */
    private final SessionContext context;

    /**
     * Current session
     */
    private final SessionImpl session;

    private final LockManager systemLockMgr;

    private final Set<String> lockTokens = new HashSet<String>();

    /**
     * Creates a lock manager.
     *
     * @param context component context of the current session
     * @param systemLockMgr internal lock manager
     */
    public SessionLockManager(
            SessionContext context, LockManager systemLockMgr) {
        this.context = context;
        this.session = context.getSessionImpl();
        this.systemLockMgr = systemLockMgr;
    }

    /**
     * @see javax.jcr.lock.LockManager#getLockTokens()
     */
    public String[] getLockTokens() throws RepositoryException {
        synchronized (lockTokens) {
            String[] result = new String[lockTokens.size()];
            lockTokens.toArray(result);
            return result;
        }
    }

    /**
     * @see javax.jcr.lock.LockManager#addLockToken(String)
     */
    public void addLockToken(String lockToken) throws LockException, RepositoryException {
        if (!lockTokens.contains(lockToken)) {
            systemLockMgr.addLockToken(session, lockToken);
        } else {
            log.debug("Lock token already present with session -> no effect.");
        }
    }

    /**
     * @see javax.jcr.lock.LockManager#removeLockToken(String)
     */
    public void removeLockToken(String lockToken) throws LockException, RepositoryException {
        if (lockTokens.contains(lockToken)) {
            systemLockMgr.removeLockToken(session, lockToken);
        } else {
            throw new LockException("Lock token " + lockToken + " not present with session.");
        }
    }

    /**
     * @see javax.jcr.lock.LockManager#isLocked(String)
     */
    public boolean isLocked(String absPath) throws RepositoryException {
        NodeImpl node = (NodeImpl) session.getNode(absPath);
        /*
         NOTE: with JSR 283 a transient node below a deep lock will report
         islocked = true. therefore, the shortcut for NEW nodes that was
         present with NodeImpl.isLocked before cannot be applied any more.
         */
        if (node.isNew()) {
            while (node.isNew()) {
                node = (NodeImpl) node.getParent();
            }
            return systemLockMgr.isLocked(node) && systemLockMgr.getLock(node).isDeep();
        } else {
            return systemLockMgr.isLocked(node);
        }
    }

    /**
     * @see javax.jcr.lock.LockManager#getLock(String)
     */
    public Lock getLock(String absPath) throws
            UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, RepositoryException {
        NodeImpl node = (NodeImpl) session.getNode(absPath);
        if (node.isNew()) {
            while (node.isNew()) {
                node = (NodeImpl) node.getParent();
            }
            Lock l = systemLockMgr.getLock(node);
            if (l.isDeep()) {
                return l;
            } else {
                throw new LockException("Node not locked: " + node);
            }
        } else {
            return systemLockMgr.getLock(node);
        }
    }

    /**
     * @see javax.jcr.lock.LockManager#holdsLock(String)
     */
    public boolean holdsLock(String absPath) throws RepositoryException {
        NodeImpl node = (NodeImpl) session.getNode(absPath);
        /* Shortcut:
           New nodes never hold or not-lockable nodes never hold a lock. */
        if (node.isNew() || !node.isNodeType(NameConstants.MIX_LOCKABLE)) {
            return false;
        } else {
            return systemLockMgr.holdsLock(node);
        }
    }

    /**
     * @see javax.jcr.lock.LockManager#lock(String, boolean, boolean, long, String)
     */
    public Lock lock(String absPath, boolean isDeep, boolean isSessionScoped,
                     long timeoutHint, String ownerInfo) throws RepositoryException {
        NodeImpl node = (NodeImpl) session.getNode(absPath);
        
        int options = ItemValidator.CHECK_HOLD | ItemValidator.CHECK_PENDING_CHANGES_ON_NODE;
        context.getItemValidator().checkModify(node, options, Permission.LOCK_MNGMT);
        checkLockable(node);

        synchronized (systemLockMgr) {
            return systemLockMgr.lock(node, isDeep, isSessionScoped, timeoutHint, ownerInfo);
        }
    }

    /**
     * @see javax.jcr.lock.LockManager#unlock(String)
     */
    public void unlock(String absPath) throws
            UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, InvalidItemStateException,
            RepositoryException {

        NodeImpl node = (NodeImpl) session.getNode(absPath);
        int options = ItemValidator.CHECK_HOLD | ItemValidator.CHECK_PENDING_CHANGES_ON_NODE;
        context.getItemValidator().checkModify(node, options, Permission.LOCK_MNGMT);
        checkLockable(node);

        synchronized (systemLockMgr) {
            // basic checks if unlock can be called on the node.
            if (!systemLockMgr.holdsLock(node)) {
                throw new LockException("Node not locked: " + node);
            }
            systemLockMgr.checkUnlock(session, node);
            systemLockMgr.unlock(node);
        }
    }

    //--------------------------------------------------------------------------
    /**
     *
     * @param lockToken
     * @return <code>true</code> if the token was successfully added to the set.
     */
    boolean lockTokenAdded(String lockToken) {
        synchronized (lockTokens) {
            return lockTokens.add(lockToken);
        }
    }

    /**
     * 
     * @param lockToken
     * @return <code>true</code> if the token was successfully removed from the set.
     */
    boolean lockTokenRemoved(String lockToken) {
        synchronized (lockTokens) {
            return lockTokens.remove(lockToken);
        }
    }

    /**
     * Checks if the given node is lockable, i.e. has 'mix:lockable'.
     *
     * @param node
     * @throws LockException       if this node is not lockable
     * @throws RepositoryException if another error occurs
     */
    private static void checkLockable(NodeImpl node) throws LockException, RepositoryException {
        if (!node.isNodeType(NameConstants.MIX_LOCKABLE)) {
            String msg = "Unable to perform a locking operation on a non-lockable node: " + node.safeGetJCRPath();
            log.debug(msg);
            throw new LockException(msg);
        }
    }
}