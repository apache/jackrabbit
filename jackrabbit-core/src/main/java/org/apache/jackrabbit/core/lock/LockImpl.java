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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

/**
 * Implementation of a <code>Lock</code> that gets returned to clients asking
 * for a lock.
 */
class LockImpl implements org.apache.jackrabbit.api.jsr283.lock.Lock {

    /**
     * Lock info containing latest information
     */
    protected final AbstractLockInfo info;

    /**
     * Node holding lock
     */
    protected final Node node;

    /**
     * Create a new instance of this class.
     *
     * @param info lock information
     * @param node node holding lock
     */
    public LockImpl(AbstractLockInfo info, Node node) {
        this.info = info;
        this.node = node;
    }

    //-----------------------------------------------------------------< Lock >

    /**
     * {@inheritDoc}
     */
    public String getLockOwner() {
        // TODO:  TOBEFIXED for 2.0
        // TODO   - respect ownerInfo supplied by the client -> see LockManager#lock
        return info.lockOwner;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDeep() {
        return info.deep;
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
        // TODO: TOBEFIXED for 2.0
        // TODO  - token must not be exposed for session-scoped locks (-> adjust tests and derived projects first)
        // TODO  - openScoped tokens *may* be exposed even if session is not lock holder
        /*
        if (info.isSessionScoped()) {
            return null;
        }
        */
        try {
            return info.getLockToken(node.getSession());
        } catch (RepositoryException e) {
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
            throw new LockException("Lock is not live any more.");
        }
        if (getLockToken() == null) {
            throw new LockException("Session does not hold lock.");
        }
        // TODO: TOBEFIXED for 2.0
        // TODO  - add refresh if timeout is supported -> see #getSecondsRemaining
        // since a lock has no expiration date no other action is required
    }

    //--------------------------------------------------< new JSR 283 methods >

    /**
     * Always returns {@link Long#MAX_VALUE}.
     *
     * @return Always returns {@link Long#MAX_VALUE}.
     * @see org.apache.jackrabbit.api.jsr283.lock.Lock#getSecondsRemaining()
     */
    public long getSecondsRemaining() {
        // TODO: TOBEFIXED for 2.0
        // TODO  - add support for timeout specified by the API user -> LockManager#lock
        return Long.MAX_VALUE;
    }

    /**
     * @see org.apache.jackrabbit.api.jsr283.lock.Lock#isLockOwningSession()
     */
    public boolean isLockOwningSession() {
        try {
            return node.getSession().equals(info.getLockHolder());
        } catch (RepositoryException e) {
            return false;
        }
    }
}
