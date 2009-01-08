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

import org.apache.jackrabbit.api.jsr283.lock.Lock;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>SessionLockManager</code> implements the
 * {@link org.apache.jackrabbit.api.jsr283.lock.LockManager}. In contrast
 * to the internal {@link LockManager} interface that is created once
 * for each <code>WorkspaceInfo</code>, the JSR 283 <code>LockManager</code>
 * is associated with a single <code>Session</code> and its
 * <code>Workspace</code>.
 *
 * @see org.apache.jackrabbit.api.jsr283.Workspace#getLockManager()
 */
public class SessionLockManager implements org.apache.jackrabbit.api.jsr283.lock.LockManager {

    private static Logger log = LoggerFactory.getLogger(SessionLockManager.class);

    private final SessionImpl session;
    private final LockManager systemLockMgr;
    private final Set lockTokens = new HashSet();

    public SessionLockManager(SessionImpl session, LockManager systemLockMgr) throws RepositoryException {
        this.session = session;
        this.systemLockMgr = systemLockMgr;
    }

    /**
     * @see org.apache.jackrabbit.api.jsr283.lock.LockManager#getLockTokens()
     */
    public String[] getLockTokens() throws RepositoryException {
        synchronized (lockTokens) {
            String[] result = new String[lockTokens.size()];
            lockTokens.toArray(result);
            return result;
        }
    }

    /**
     * @see org.apache.jackrabbit.api.jsr283.lock.LockManager#addLockToken(String)
     */
    public void addLockToken(String lockToken) throws LockException, RepositoryException {
        // TODO
        throw new UnsupportedRepositoryOperationException("Not yet implemented");
    }

    /**
     * @see org.apache.jackrabbit.api.jsr283.lock.LockManager#removeLockToken(String)
     */
    public void removeLockToken(String lockToken) throws LockException, RepositoryException {
        // TODO
        throw new UnsupportedRepositoryOperationException("Not yet implemented");
    }

    /**
     * @see org.apache.jackrabbit.api.jsr283.lock.LockManager#isLocked(String)
     */
    public boolean isLocked(String absPath) throws RepositoryException {
        NodeImpl node = (NodeImpl) session.getNode(absPath);
        /*
         NOTE: with JSR 283 a transient node below a deep lock will report
         islocked = true. therefore, the shortcut for NEW nodes that was
         present with NodeImpl.isLocked before cannot be applied any more.
         */
        return systemLockMgr.isLocked(node);
    }

    /**
     * @see org.apache.jackrabbit.api.jsr283.lock.LockManager#getLock(String)
     */
    public Lock getLock(String absPath) throws
            UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, RepositoryException {
        NodeImpl node = (NodeImpl) session.getNode(absPath);
        return (Lock) systemLockMgr.getLock(node);
    }

    /**
     * @see org.apache.jackrabbit.api.jsr283.lock.LockManager#holdsLock(String)
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
     * @see org.apache.jackrabbit.api.jsr283.lock.LockManager#lock(String, boolean, boolean, long, String)
     */
    public Lock lock(String absPath, boolean isDeep, boolean isSessionScoped,
                     long timeoutHint, String ownerInfo) throws RepositoryException {
        NodeImpl node = (NodeImpl) session.getNode(absPath);

        if (session.hasPendingChanges(node)) {
            String msg = "Unable to lock node. Node has pending changes: " + this;
            log.debug(msg);
            throw new InvalidItemStateException(msg);
        }
        checkLockable(node);
        session.getAccessManager().checkPermission(session.getQPath(node.getPath()), Permission.LOCK_MNGMT);

        synchronized (systemLockMgr) {
            return (Lock) systemLockMgr.lock(node, isDeep, isSessionScoped, timeoutHint, ownerInfo);
        }
    }

    /**
     * @see org.apache.jackrabbit.api.jsr283.lock.LockManager#unlock(String)
     */
    public void unlock(String absPath) throws
            UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, InvalidItemStateException,
            RepositoryException {

        NodeImpl node = (NodeImpl) session.getNode(absPath);
        if (session.hasPendingChanges(node)) {
            String msg = "Unable to unlock node. Node has pending changes: " + this;
            log.debug(msg);
            throw new InvalidItemStateException(msg);
        }
        checkLockable(node);
        session.getAccessManager().checkPermission(session.getQPath(node.getPath()), Permission.LOCK_MNGMT);

        synchronized (systemLockMgr) {
            systemLockMgr.unlock(node);
        }
    }

    //--------------------------------------------------------------------------
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