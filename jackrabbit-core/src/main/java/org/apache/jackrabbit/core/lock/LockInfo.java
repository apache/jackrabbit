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
import org.apache.jackrabbit.core.id.NodeId;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;

/**
 * Internal lock information.
 */
public abstract class LockInfo {

    /**
     * Constant for the undefined or infinite timeout.
     */
    static final long TIMEOUT_INFINITE = Long.MAX_VALUE;

    /**
     * Constant for the expired timeout.
     */
    static final long TIMEOUT_EXPIRED = -1;

    /**
     * Lock holder node id. Used also as the lock token.
     */
    private final NodeId id;

    /**
     * Flag indicating whether lock is session scoped
     */
    private final boolean sessionScoped;

    /**
     * Flag indicating whether lock is deep
     */
    private final boolean deep;

    /**
     * Lock owner, determined on creation time
     */
    private final String lockOwner;

    /**
     * Flag indicating whether this lock is live
     */
    private boolean live;

    /**
     * Session currently holding lock
     */
    private SessionImpl lockHolder;

    /**
     * Create a new instance of this class.
     *
     * @param id            lock holder node id
     * @param sessionScoped whether lock token is session scoped
     * @param deep          whether lock is deep
     * @param lockOwner     owner of lock
     * @param timeoutHint   the timeoutHint
     */
    protected LockInfo(
            NodeId id, boolean sessionScoped, boolean deep,
            String lockOwner, long timeoutHint) {
        this.id = id;
        this.sessionScoped = sessionScoped;
        this.deep = deep;
        this.lockOwner = lockOwner;
        // TODO: TOBEFIXED for 2.0 respect and deal with timeout hint.
    }

    /**
     * Return the lock token associated with this lock.
     *
     * @return lock token
     */
    public String getLockToken() {
        String uuid = id.toString();
        return uuid + "-" + getLockTokenCheckDigit(uuid);
    }

    /**
     * Return the ID of the lock holding node
     * @return the id
     */
    public NodeId getId() {
        return id;
    }

    /**
     * Return the lock owner.
     * 
     * @return lock owner
     */
    public String getLockOwner() {
        return lockOwner;
    }

    /**
     * Return a flag indicating whether the lock is deep.
     * 
     * @return <code>true</code> if the lock is deep;
     *         <code>false</code> otherwise
     */
    public boolean isDeep() {
        return deep;
    }

    /**
     * Return a flag indicating whether the session given is lock holder. 
     *
     * @param session session to compare with
     */
    public boolean isLockHolder(Session session) {
        return lockHolder == session;
    }

    /**
     * Return the session currently holding the lock
     *
     * @return session currently holding the lock
     */
    public SessionImpl getLockHolder() {
        return lockHolder;
    }

    /**
     * Set the session currently holding the lock
     *
     * @param lockHolder session currently holding the lock
     */
    public void setLockHolder(SessionImpl lockHolder) {
        this.lockHolder = lockHolder;
    }

    /**
     * Return a flag indicating whether the lock is live
     *
     * @return <code>true</code> if the lock is live; otherwise <code>false</code>
     */
    public boolean isLive() {
        return live;
    }

    /**
     * Set the live flag
     *
     * @param live live flag
     */
    public void setLive(boolean live) {
        this.live = live;
    }

    /**
     * Return a flag indicating whether the lock information may still change.
     *
     * @return <code>true</code> if the lock is still alive.
     */
    public boolean mayChange() {
        return live;
    }

    /**
     * Return a flag indicating whether the lock is session-scoped
     *
     * @return <code>true</code> if the lock is session-scoped;
     *         otherwise <code>false</code>
     */
    public boolean isSessionScoped() {
        return sessionScoped;
    }

    /**
     * Return the number of seconds remaining until the lock expires.
     *
     * @return number of seconds remaining until the lock expires.
     */
    public long getSecondsRemaining() {
        // TODO: TOBEFIXED for 2.0
        // TODO  - add support for timeout specified by the API user -> LockManager#lock
        return isLive() ? TIMEOUT_INFINITE : TIMEOUT_EXPIRED;
    }

    /**
     * Utility method that throws a {@link LockException} with the
     * "failure node path" set to the path of the node that holds this lock.
     * The given session is used to resolve the path of the lock holder node.
     *
     * @param message exception message
     * @param session session that the user was using for the failing operation
     * @throws LockException always thrown, unless another error occurs
     * @throws RepositoryException if the path of this lock can not be resolved
     */
    public void throwLockException(String message, SessionImpl session)
            throws LockException, RepositoryException {
        String path;
        try {
            path = session.getJCRPath(
                    session.getHierarchyManager().getPath(id));
        } catch (RepositoryException ignored) {
            path = null;
        }
        if (path != null) {
            throw new LockException(
                    message + " (lock held by node " + path + ")", null, path);
        } else {
            throw new LockException(message);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append('(');
        if (deep) {
            buffer.append("deep ");
        }
        if (sessionScoped) {
            buffer.append("session ");
        }
        buffer.append("holder:");
        if (lockHolder != null) {
            buffer.append(lockHolder.getUserID());
        } else {
            buffer.append("none");
        }
        buffer.append("owner:").append(lockOwner);
        buffer.append(')');
        return buffer.toString();
    }

    /**
     * Parse a lock token string representation and return the lock
     * holder node id.
     *
     * @param token string representation of lock token
     * @return lock holder node id
     * @throws IllegalArgumentException if some field is illegal
     */
    public static NodeId parseLockToken(String token)
            throws IllegalArgumentException {
        int sep = token.lastIndexOf('-');
        if (sep == -1 || sep == token.length() - 1) {
            throw new IllegalArgumentException("Separator not found.");
        }
        String uuid = token.substring(0, sep);
        if (getLockTokenCheckDigit(uuid) != token.charAt(token.length() - 1)) {
            throw new IllegalArgumentException("Bad check digit.");
        }
        return NodeId.valueOf(uuid);
    }

    /**
     * Return the check digit for a lock token, given by its UUID
     * @param uuid uuid
     * @return check digit
     */
    private static char getLockTokenCheckDigit(String uuid) {
        int result = 0;

        int multiplier = 36;
        for (int i = 0; i < uuid.length(); i++) {
            char c = uuid.charAt(i);
            if (c >= '0' && c <= '9') {
                int num = c - '0';
                result += multiplier * num;
                multiplier--;
            } else if (c >= 'A' && c <= 'F') {
                int num = c - 'A' + 10;
                result += multiplier * num;
                multiplier--;
            } else if (c >= 'a' && c <= 'f') {
                int num = c - 'a' + 10;
                result += multiplier * num;
                multiplier--;
            }
        }

        int rem = result % 37;
        if (rem != 0) {
            rem = 37 - rem;
        }
        if (rem >= 0 && rem <= 9) {
            return (char) ('0' + rem);
        } else if (rem >= 10 && rem <= 35) {
            return (char) ('A' + rem - 10);
        } else {
            return '+';
        }
    }

}
