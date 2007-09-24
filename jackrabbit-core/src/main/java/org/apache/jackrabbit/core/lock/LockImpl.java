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
class LockImpl implements Lock {

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
        // since a lock has no expiration date no other action is required
    }

    //--------------------------------------------------< new JSR 283 methods >
    /**
     * Returns <code>true</code> if the current session is the owner of this
     * lock, either because it is session-scoped and bound to this session or
     * open-scoped and this session currently holds the token for this lock.
     * Returns <code>false</code> otherwise.
     *
     * @return a <code>boolean</code>.
     * @since JCR 2.0
     */
    public boolean isLockOwningSession() {
        try {
            return info.getLockHolder().equals(node.getSession());
        } catch (RepositoryException e) {
            return false;
        }
    }
}
