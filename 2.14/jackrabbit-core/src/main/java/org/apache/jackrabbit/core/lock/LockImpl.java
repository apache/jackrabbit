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

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.security.authorization.Permission;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;

/**
 * Implementation of a <code>Lock</code> that gets returned to clients asking
 * for a lock.
 */
class LockImpl implements javax.jcr.lock.Lock {

    /**
     * Lock info containing latest information
     */
    protected final LockInfo info;

    /**
     * Node holding lock
     */
    protected final NodeImpl node;

    /**
     * Create a new instance of this class.
     *
     * @param info lock information
     * @param node node holding lock
     */
    public LockImpl(LockInfo info, NodeImpl node) {
        this.info = info;
        this.node = node;
    }

    //-----------------------------------------------------------------< Lock >

    /**
     * {@inheritDoc}
     */
    public String getLockOwner() {
        return info.getLockOwner();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDeep() {
        return info.isDeep();
    }

    /**
     * {@inheritDoc}
     */
    public Node getNode() {
        return node;
    }

    /**
     * {@inheritDoc}
     */
    public String getLockToken() {
        if (!info.isSessionScoped() && (info.isLockHolder(node.getSession()) || isAdminUser(node.getSession()))) {
            return info.getLockToken();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLive() throws RepositoryException {
        return info.isLive();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSessionScoped() {
        return info.isSessionScoped();
    }

    /**
     * {@inheritDoc}
     * @throws LockException if this <code>Session</code> is not the lock holder
     * or if this <code>Lock</code> is not alive.
     */
    public void refresh() throws LockException, RepositoryException {
        if (!isLive()) {
            info.throwLockException(
                    "Lock is not live any more",
                    (SessionImpl) node.getSession());
        } else if (!isLockOwningSession()) {
            info.throwLockException(
                    "Session does not hold lock.",
                    (SessionImpl) node.getSession());
        } else {
            // make sure the current session has sufficient privileges to refresh
            // the lock.
            SessionImpl session = (SessionImpl) node.getSession();
            session.getAccessManager().checkPermission(
                    node.getPrimaryPath(), Permission.LOCK_MNGMT);

            // Update the lock timeout
            info.updateTimeoutTime();
        }
    }

    //--------------------------------------------------< new JSR 283 methods >

    /** {@inheritDoc} */
    public long getSecondsRemaining() {
        if (!info.isLive()) {
            return -1;
        }

        // TODO JCR-1590: Disabled until locks get unlocked when they timeout
        long timeout = info.getTimeoutTime();
        if (timeout == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        long remainingSeconds = (timeout - System.currentTimeMillis()) / 1000;

        return Math.max(remainingSeconds, 1); // must always be positive
    }

    /**
     * @see javax.jcr.lock.Lock#isLockOwningSession()
     */
    public boolean isLockOwningSession() {
        return info.isLockHolder(node.getSession());
    }

    /**
     * Check whether a session belongs to an administrative user.
     */
    private boolean isAdminUser(Session session) {
        if (session instanceof SessionImpl) {
            return ((SessionImpl) session).isAdmin();
        } else {
            // fallback. use hardcoded default admin ID
            return "admin".equals(session.getUserID());
        }
    }
}
