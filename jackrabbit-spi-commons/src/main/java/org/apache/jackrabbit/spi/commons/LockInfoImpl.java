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
package org.apache.jackrabbit.spi.commons;

import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.NodeId;

import java.io.Serializable;

/**
 * <code>LockInfoImpl</code> implements a serializable <code>LockInfo</code>
 * based on another lock info.
 */
public class LockInfoImpl implements LockInfo, Serializable {

    /**
     * The lock token for this lock info.
     */
    private final String lockToken;

    /**
     * The owner of the lock.
     */
    private final String lockOwner;

    /**
     * The isDeep flag.
     */
    private final boolean isDeep;

    /**
     * The isSessionScoped flag.
     */
    private final boolean isSessionScoped;

    /**
     * Number of seconds until the lock time outs.
     */
    private final long secondsRemaining;

    /**
     * Flag indicating if the session is lock owner or not.
     */
    private final boolean isLockOwner;

    /**
     * The <code>NodeId</code> of the locked node.
     */
    private final NodeId nodeId;

    /**
     * Creates a new lock info for the given <code>lock</code> info.
     *
     * @param lockToken the lock token
     * @param lockOwner the lock owner
     * @param isDeep whether this lock is deep or not
     * @param isSessionScoped whether this lock is session scoped or not
     * @param nodeId the node id of the locked node.
     * @deprecated Use {@link #LockInfoImpl(String, String, boolean, boolean, long, boolean, NodeId)} instaed.
     */
    public LockInfoImpl(String lockToken, String lockOwner, boolean isDeep,
                        boolean isSessionScoped, NodeId nodeId) {
        this(lockToken, lockOwner, isDeep, isSessionScoped, Long.MAX_VALUE, lockToken != null, nodeId);
    }

    /**
     * Creates a new lock info for the given <code>lock</code> info.
     *
     * @param lockToken the lock token
     * @param lockOwner the lock owner
     * @param isDeep whether this lock is deep or not
     * @param isSessionScoped whether this lock is session scoped or not
     * @param secondsRemaining Number of seconds until the lock timeout is reached.
     * @param isLockOwner <code>true</code> if the calling session is lock
     * owner; <code>false</code> otherwise.
     * @param nodeId the node id of the locked node.
     * @since JCR 2.0
     */
    public LockInfoImpl(String lockToken, String lockOwner, boolean isDeep,
                        boolean isSessionScoped, long secondsRemaining,
                        boolean isLockOwner, NodeId nodeId) {
        this.lockToken = lockToken;
        this.lockOwner = lockOwner;
        this.isDeep = isDeep;
        this.isSessionScoped = isSessionScoped;
        this.secondsRemaining = secondsRemaining;
        this.isLockOwner = isLockOwner;
        this.nodeId = nodeId;
    }

    /**
     * {@inheritDoc}
     */
    public String getLockToken() {
        return lockToken;
    }

    /**
     * {@inheritDoc}
     */
    public String getOwner() {
        return lockOwner;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDeep() {
        return isDeep;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSessionScoped() {
        return isSessionScoped;
    }

    /**
     * {@inheritDoc}
     */
    public long getSecondsRemaining() {
        return secondsRemaining;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLockOwner() {
        return isLockOwner;
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getNodeId() {
        return nodeId;
    }
}