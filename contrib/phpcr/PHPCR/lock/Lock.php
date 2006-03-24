<?php

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


require_once 'PHPCR/lock/LockException.php';
require_once 'PHPCR/RepositoryException.php';


/**
 * Represents a lock placed on an item.
 * <p/>
 * <b>Level 2 only</b>
 * <p/>
 * A lock is associated with an item and a user (not a ticket)
 *
 * @author Markus Nix <mnix@mayflower.de>
 * @package phpcr
 * @subpackage lock
 */
interface Lock
{
    /**
     * Returns the user ID of the user who owns this lock. This is the value of the
     * <code>jcr:lockOwner</code> property of the lock-holding node. It is also the
     * value returned by <code>Session.getUserId</code> at the time that the lock was
     * placed. The lock owner's identity is only provided for informational purposes.
     * It does not govern who can perform an unlock or make changes to the locked nodes;
     * that depends entirely upon who the token holder is.
     * @return a user ID.
     */
    public function getLockOwner();

    /**
     * Returns <code>true</code> if this is a deep lock; <code>false</code> otherwise.
     *
     * @return a boolean
     */
    public function isDeep();

    /**
     * Returns the lock holding node. Note that <code>N.getLock().getNode()</code>
     * (where <code>N</code> is a locked node) will only return <code>N</code>
     * if <code>N</code> is the lock holder. If <code>N</code> is in the subtree
     * of the lock holder, <code>H</code>, then this call will return <code>H</code>.
     *
     * @return an <code>Node</code>.
     */
    public function getNode();

    /**
     * May return the lock token for this lock.
     * <p/>
     * If this <code>Session</code> holds the lock token for this lock, then this method will
     * return that lock token. If this <code>Session</code> does not hold the applicable lock
     * token then this method will return null.
     *
     * @return a <code>String</code>.
     */
    public function getLockToken();

    /**
     * Returns true if this <code>Lock</code> object represents a lock that is currently in effect.
     * If this lock has been unlocked either explicitly or due to an implementation-specific limitation
     * (like a timeout) then it returns <code>false</code>. Note that this method is intended for
     * those cases where one is holding a <code>Lock</code> Java object and wants to find out
     * whether the lock (the JCR-level entity that is attached to the lockable node) that this
     * object originally represented still exists. For example, a timeout or explicit
     * <code>unlock</code> will remove a lock from a node but the <code>Lock</code>
     * Java object corresponding to that lock may still exist, and in that case its
     * <code>isLive</code> method will return <code>false</code>.
     * @return a <code>boolean</code>.
     */
    public function isLive();

    /**
     * Returns <code>true</code> if this is a session-scoped lock. Returns
     * <code>false</code> if this is an open-scoped lock.
     *
     * @return a <code>boolean</code>
     */
    public function isSessionScoped();

    /**
     * Refreshes (brings back to life) a previously unlocked <code>Lock</code> object
     * (one for which <code>isLive</code> returns <code>false</code>). If this lock
     * is still live (<code>isLive</code> returns <code>true</code>) or if this <code>Session</code>
     * does not hold the correct lock token for this lock, then a <code>LockException</code>
     * is thrown. A <code>RepositoryException</code> is thrown if another error occurs.
     *
     * The implementation may either revive an existing lock or issue a new lock
     * (removing this one from the session and substituting the new one).
     * This is an implementation-specific issue.
     * @throws LockException if this lock is still live (<code>isLive</code> returns <code>true</code>)
     * or if this <code>Session</code> does not hold the correct lock token for this lock.
     * @throws RepositoryException if another error occurs.
     */
    public function refresh();
}

?>