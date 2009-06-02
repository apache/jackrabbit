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
package org.apache.jackrabbit.jcr2spi.lock;

import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

import org.apache.jackrabbit.jcr2spi.state.NodeState;

/**
 * Defines the functionality needed for locking and unlocking nodes.
 */
public interface LockStateManager {

    /**
     * Lock a node. Checks whether the node is not locked and then
     * returns a lock object for this node.
     *
     * @param nodeState
     * @param isDeep whether the lock applies to this node only
     * @param isSessionScoped whether the lock is session scoped
     * @return lock object
     * @throws LockException if this node already is locked, or some descendant
     *         node is locked and <code>isDeep</code> is <code>true</code>
     * @see javax.jcr.Node#lock
     */
    Lock lock(NodeState nodeState, boolean isDeep, boolean isSessionScoped)
        throws LockException, RepositoryException;

    /**
     * Lock a node. Checks whether the node is not locked and then
     * returns a lock object for this node.
     *
     * @param nodeState
     * @param isDeep whether the lock applies to this node only
     * @param isSessionScoped whether the lock is session scoped
     * @param timeoutHint optional timeout hint.
     * @param ownerHint optional String defining the lock owner info to be
     * displayed.
     * @return lock object
     * @throws LockException if this node already is locked, or some descendant
     *         node is locked and <code>isDeep</code> is <code>true</code>
     * @see javax.jcr.Node#lock
     */
    Lock lock(NodeState nodeState, boolean isDeep, boolean isSessionScoped, long timeoutHint, String ownerHint)
        throws LockException, RepositoryException;

    /**
     * Removes the lock on a node.
     *
     * @param nodeState
     * @throws LockException if this node is not locked or the session does not
     * have the correct lock token
     * @see javax.jcr.Node#unlock
     */
    void unlock(NodeState nodeState) throws LockException, RepositoryException;


    /**
     * Returns the Lock object that applies to a node. This may be either a lock
     * on this node itself or a deep lock on a node above this node.
     *
     * @param nodeState
     * @return lock object
     * @throws LockException if this node is not locked
     * @see javax.jcr.Node#getLock
     */
    Lock getLock(NodeState nodeState) throws LockException, RepositoryException;

    /**
     * Returns <code>true</code> if this node is locked either as a result
     * of a lock held by this node or by a deep lock on a node above this
     * node; otherwise returns <code>false</code>.
     *
     * @param nodeState
     * @return <code>true</code> if this node is locked either as a result
     * of a lock held by this node or by a deep lock on a node above this
     * node; otherwise returns <code>false</code>
     * @throws RepositoryException If an error occurs.
     * @see javax.jcr.Node#isLocked
     */
    boolean isLocked(NodeState nodeState) throws RepositoryException;

    /**
     * Check whether the given node state is locked by somebody else than the
     * current session. Access is allowed if the node is not locked or
     * if the session itself holds the lock to this node, i.e. the session
     * contains the lock token for the lock. If the node is not locked at
     * all this method returns silently.
     *
     * @param nodeState
     * @throws LockException if write access to the specified node is not allowed
     * @throws RepositoryException if some other error occurs
     */
    void checkLock(NodeState nodeState) throws LockException, RepositoryException;

    /**
     *
     * @return The lock tokens associated with the <code>Session</code> this
     * lock manager has been created for.
     */
    public String[] getLockTokens() throws RepositoryException;

    /**
     * Invoked by a session to inform that a lock token has been added.
     *
     * @param lt added lock token
     */
    void addLockToken(String lt) throws LockException, RepositoryException;

    /**
     * Invoked by a session to inform that a lock token has been removed.
     *
     * @param lt removed lock token
     */
    void removeLockToken(String lt) throws LockException, RepositoryException;
}
