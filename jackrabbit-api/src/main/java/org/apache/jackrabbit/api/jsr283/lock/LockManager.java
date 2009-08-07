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
package org.apache.jackrabbit.api.jsr283.lock;

import javax.jcr.lock.LockException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;

/**
 * This interface holds extensions made in JCR 2.0 while work
 * is in progress implementing JCR 2.0. It encapsulates methods for the
 * management of locks.
 *
 * @since JCR 2.0
 */

public interface LockManager{

    /**
     * Adds the specified lock token to the current <code>Session</code>.
     * Holding a lock token makes the current <code>Session</code> the owner
     * of the lock specified by that particular lock token.
     * <p>
     * A <code>LockException</code> is thrown if the specified lock token is
     * already held by another <code>Session</code> and the implementation
     * does not support simultaneous ownership of open-scoped locks.
     * @param lockToken a lock token (a string).
     * @throws LockException if the specified lock token is already held by
     *                       another <code>Session</code> and the implementation
     *                       does not support simultaneous ownership of open-scoped
     *                       locks.
     * @throws RepositoryException if another error occurs.
     */
    public void addLockToken(String lockToken) throws LockException, RepositoryException;

    /**
     * Returns the <code>Lock</code> object that applies to the node at the
     * specified <code>absPath</code>. This may be either a lock on that node
     * itself or a deep lock on a node above that node.
     * <p>
     * If the node is not locked (no lock applies to this node), a
     * <code>LockException</code> is thrown.
     * <p>
     * If the current session does not have sufficient privileges to get the
     * lock, an <code>AccessDeniedException</code> is thrown.
     * <p>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if
     * this implementation does not support locking.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath absolute path of node for which to obtain the lock
     * @return The applicable <code>Lock</code> object.
     * @throws UnsupportedRepositoryOperationException if this implementation does not support locking.
     * @throws LockException if no lock applies to this node.
     * @throws AccessDeniedException if the current session does not have permission to get the lock.
     * @throws RepositoryException if another error occurs.
     */
    public Lock getLock(String absPath) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException;

    /**
     * Returns an array containing all lock tokens currently held by the
     * current <code>Session</code>. Note that any such tokens will represent
     * open-scoped locks, since session-scoped locks do not have tokens.
     *
     * @return an array of lock tokens (strings)
     * @throws RepositoryException if an error occurs.
     */
    public String[] getLockTokens() throws RepositoryException;

    /**
     * Returns <code>true</code> if the node at <code>absPath</code> holds a
     * lock; otherwise returns <code>false</code>. To <i>hold</i> a lock means
     * that this node has actually had a lock placed on it specifically, as
     * opposed to just having a lock <i>apply</i> to it due to a deep lock held
     * by a node above.
     *
     * @param absPath absolute path of node
     * @return a <code>boolean</code>.
     * @throws RepositoryException if an error occurs.
     */
    public boolean holdsLock(String absPath) throws RepositoryException;

    /**
     * <p>
     * Places a lock on the node at <code>absPath</code>.
     * If successful, the node is said to <i>hold</i> the lock.
     * </p>
     * <p>
     * If <code>isDeep</code> is <code>true</code> then the lock
     * applies to the specified node and all its descendant nodes;
     * if <code>false</code>, the lock applies only to the specified node.
     * On a successful lock, the <code>jcr:isDeep</code> property of the
     * locked node is set to this value.
     * </p>
     * <p>
     * If <code>isSessionScoped</code> is <code>true</code> then this lock
     * will expire upon the expiration of the current session (either
     * through an automatic or explicit <code>Session.logout</code>);
     * if false, this lock does not expire until it is explicitly unlocked,
     * it times out, or it is automatically unlocked due to a
     * implementation-specific limitation.
     * </p>
     * <p>
     * The timeout parameter specifies the number of seconds until the lock times out
     * (if it is not refreshed with <code>Lock.refresh</code> in the meantime).
     * An implementation may use this information as a hint or ignore it altogether.
     * Clients can discover the actual timeout by inspecting the returned <code>Lock</code>
     * object.
     * </p>
     * <p>
     * The <code>ownerInfo</code> parameter can be used to pass a string holding
     * owner information relevant to the client. An implementation may either use
     * or ignore this parameter. If it uses the parameter it must set the
     * <code>jcr:lockOwner</code> property of the locked node to this value and
     * return this value on <code>Lock.getLockOwner</code>. If it ignores this
     * parameter the <code>jcr:lockOwner</code> property (and the value returned
     * by <code>Lock.getLockOwner</code>) is set to either the value returned by
     * <code>Session.getUserID</code> of the owning session or an
     * implementation-specific string identifying the owner.
     * </p>
     * <p>
     * The method returns a <code>Lock</code> object representing the new lock.
     * If the lock is open-scoped the returned lock will include a lock token.
     * The lock token is also automatically added to the set of lock tokens held
     * by the current session.
     * </p>
     * <p>
     * The addition or change of the properties <code>jcr:isDeep</code>
     * and <code>jcr:lockOwner</code> are persisted immediately; there is no
     * need to call <code>save</code>.
     * </p>
     * <p>
     * It is possible to lock a node even if it is checked-in.
     * </p>
     * <p>
     * If this node is not of mixin node type <code>mix:lockable</code> then an
     * <code>LockException</code> is thrown.
     * <p>
     * If this node is already locked (either because it holds a lock or a
     * lock above it applies to it), a <code>LockException</code> is thrown.
     * <p>
     * If <code>isDeep</code> is <code>true</code> and a descendant node of
     * this node already holds a lock, then a <code>LockException</code> is
     * thrown.
     * <p>
     * If this node does not have a persistent state (has never been saved
     * or otherwise persisted), a <code>LockException</code> is thrown.
     * <p>
     * If the current session does not have sufficient privileges to place the
     * lock, an <code>AccessDeniedException</code> is thrown.
     * <p>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if
     * this implementation does not support locking.
     * <p>
     * An InvalidItemStateException is thrown if this node has pending unsaved
     * changes.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath absolute path of node to be locked
     * @param isDeep if <code>true</code> this lock will apply to this node and all its descendants; if
     * <code>false</code>, it applies only to this node.
     * @param isSessionScoped if <code>true</code>, this lock expires with the current session; if <code>false</code> it
     * expires when explicitly or automatically unlocked for some other reason.
     * @param timeoutHint desired lock timeout in seconds (servers are free to
     * ignore this value); specify {@link Long#MAX_VALUE} for no timeout.
     * @param ownerInfo a string containing owner information
     * supplied by the client; servers are free to ignore this value.
     * @return A <code>Lock</code> object containing a lock token.
     * @throws UnsupportedRepositoryOperationException if this implementation does not support locking.
     * @throws LockException if this node is not <code>mix:lockable</code> or this node is already locked or
     * <code>isDeep</code> is <code>true</code> and a descendant node of this node already holds a lock.
     * @throws AccessDeniedException if this session does not have permission to lock this node.
     * @throws InvalidItemStateException if this node has pending unsaved changes.
     * @throws RepositoryException if another error occurs.
     */
    public Lock lock(String absPath, boolean isDeep, boolean isSessionScoped,
                     long timeoutHint, String ownerInfo) throws RepositoryException;

    /**
     * Returns <code>true</code> if the node at <code>absPath</code> is locked
     * either as a result of a lock held by that node or by a deep
     * lock on a node above that node; otherwise returns <code>false</code>.
     *
     * @param absPath absolute path of node
     * @return a <code>boolean</code>.
     * @throws RepositoryException if an error occurs.
     */
    public boolean isLocked(String absPath) throws RepositoryException;

    /**
     * Removes the specified lock token from this <code>Session</code>.
     * <p>
     * A <code>LockException</code> is thrown if the current <code>Session</code>
     * does not hold the specified lock token.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param lockToken a lock token (a string)
     * @throws LockException if the current <code>Session</code> does not hold
     *                       the specified lock token.
     * @throws RepositoryException if another error occurs.
     */
    public void removeLockToken(String lockToken) throws LockException, RepositoryException;

    /**
     * Removes the lock on the node at <code>absPath</code>. Also removes
     * the properties <code>jcr:lockOwner</code> and <code>jcr:lockIsDeep</code>
     * from that node. As well, the corresponding lock token is removed from
     * the set of lock tokens held by the current <code>Session</code>.
     * <p>
     * If the node does not currently hold a lock or holds a lock for which
     * this <code>Session</code> is not the owner, then a
     * <code>LockException</code> is thrown. Note however that the system
     * may give permission to a non-owning session to unlock a lock. Typically
     * such "lock-superuser" capability is intended to facilitate
     * administrational clean-up of orphaned open-scoped locks.
     * <p>
     * Note that it is possible to unlock a node even if it is checked-in (the
     * lock-related properties will be changed despite the checked-in status).
     * <p>
     * If the current session does not have sufficient privileges to remove the
     * lock, an <code>AccessDeniedException</code> is thrown.
     * <p>
     * An <code>InvalidItemStateException</code> is thrown if this node has
     * pending unsaved changes.
     * <p>
     * An <code>UnsupportedRepositoryOperationException</code> is thrown if
     * this implementation does not support locking.
     * <p>
     * A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * @param absPath absolute path of node to be unlocked
     * @throws UnsupportedRepositoryOperationException if this implementation does not support locking.
     * @throws LockException if this node does not currently hold a lock or holds a lock for which this Session does not have the correct lock token
     * @throws AccessDeniedException if the current session does not have permission to unlock this node.
     * @throws InvalidItemStateException if this node has pending unsaved changes.
     * @throws RepositoryException if another error occurs.
     */
    public void unlock(String absPath) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException;
}

