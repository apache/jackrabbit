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

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.spi.Path;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

/**
 * Defines the functionality needed for locking and unlocking nodes.
 */
public interface LockManager {

    /**
     * Lock a node. Checks whether the node is not locked and then
     * returns a lock object for this node.
     * @param node node
     * @param isDeep whether the lock applies to this node only
     * @param isSessionScoped whether the lock is session scoped
     * @return lock object
     * @throws LockException if this node already is locked, or some descendant
     *         node is locked and <code>isDeep</code> is <code>true</code>
     * @see javax.jcr.Node#lock
     */
    Lock lock(NodeImpl node, boolean isDeep, boolean isSessionScoped)
            throws LockException, RepositoryException;

    /**
     * Lock a node. Checks whether the node is not locked and then
     * returns a lock object for this node.
     *
     * @param node Node to create the lock for.
     * @param isDeep whether the lock applies to this node only
     * @param isSessionScoped whether the lock is session scoped
     * @param timeoutHint Desired lock timeout in seconds.
     * @param ownerInfo Optional string acting as information about the owner.
     * @return the lock.
     * @throws LockException if this node already is locked, or some descendant
     *         node is locked and <code>isDeep</code> is <code>true</code>
     * @see javax.jcr.lock.LockManager#lock(String, boolean, boolean, long, String)
     * @throws RepositoryException
     */
    Lock lock(NodeImpl node, boolean isDeep, boolean isSessionScoped, long timeoutHint, String ownerInfo)
            throws LockException, RepositoryException;

    /**
     * Returns the Lock object that applies to a node. This may be either a lock
     * on this node itself or a deep lock on a node above this node.
     * @param node node
     * @return lock object
     * @throws LockException if this node is not locked
     * @see javax.jcr.Node#getLock
     */
    Lock getLock(NodeImpl node) throws LockException, RepositoryException;

    /**
     * Returns all locks owned by the specified session.
     * @param session session
     * @return an array of lock objects
     * @throws RepositoryException if an error occurs
     * @see SessionImpl#getLocks
     */
    Lock[] getLocks(SessionImpl session) throws RepositoryException;

    /**
     * Removes the lock on a node given by its path.
     * @param node node
     * @throws LockException if this node is not locked or the session
     *         does not have the correct lock token
     * @see javax.jcr.Node#unlock
     */
    void unlock(NodeImpl node) throws LockException, RepositoryException;

    /**
     * Returns <code>true</code> if the node given holds a lock;
     * otherwise returns <code>false</code>.
     * @param node node
     * @return <code>true</code> if the node given holds a lock;
     *         otherwise returns <code>false</code>
     * @see javax.jcr.Node#holdsLock
     * @throws javax.jcr.RepositoryException  If an exception occurs.
     */
    boolean holdsLock(NodeImpl node) throws RepositoryException;

    /**
     * Returns <code>true</code> if this node is locked either as a result
     * of a lock held by this node or by a deep lock on a node above this
     * node; otherwise returns <code>false</code>
     * @param node node
     * @return <code>true</code> if this node is locked either as a result
     * of a lock held by this node or by a deep lock on a node above this
     * node; otherwise returns <code>false</code>
     * @see javax.jcr.Node#isLocked
     * @throws javax.jcr.RepositoryException If an exception occurs.
     */
    boolean isLocked(NodeImpl node) throws RepositoryException;

    /**
     * Check whether the node given is locked by somebody else than the
     * current session. Access is allowed if the node is not locked or
     * if the session itself holds the lock to this node, i.e. the session
     * contains the lock token for the lock.
     * @param node node to check
     * @throws LockException if write access to the specified node is not allowed
     * @throws RepositoryException if some other error occurs
     */
    void checkLock(NodeImpl node)
            throws LockException, RepositoryException;

    /**
     * Check whether the path given is locked by somebody else than the
     * session described. Access is allowed if the node is not locked or
     * if the session itself holds the lock to this node, i.e. the session
     * contains the lock token for the lock.
     * @param path path to check
     * @param session session
     * @throws LockException if write access to the specified path is not allowed
     * @throws RepositoryException if some other error occurs
     */
    void checkLock(Path path, Session session)
            throws LockException, RepositoryException;

    /**
     * Check whether a session is allowed to unlock a node.
     *
     * @throws LockException if unlocking is denied
     * @throws RepositoryException if some other error occurs
     */
    void checkUnlock(Session session, NodeImpl node) throws LockException,
            RepositoryException;

    /**
     * Invoked by a session to inform that a lock token has been added.
     *
     * @param session session that has a added lock token
     * @param lt added lock token
     * @throws LockException
     * @throws RepositoryException
     */
    void addLockToken(SessionImpl session, String lt) throws LockException, RepositoryException;

    /**
     * Invoked by a session to inform that a lock token has been removed.
     *
     * @param session session that has a removed lock token
     * @param lt removed lock token
     * @throws LockException
     * @throws RepositoryException
     */
    void removeLockToken(SessionImpl session, String lt) throws LockException, RepositoryException;
}
