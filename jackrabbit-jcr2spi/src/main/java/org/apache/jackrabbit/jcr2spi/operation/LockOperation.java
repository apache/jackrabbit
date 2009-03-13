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
package org.apache.jackrabbit.jcr2spi.operation;

import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.spi.LockInfo;
import org.apache.jackrabbit.spi.NodeId;

import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * <code>LockOperation</code>...
 */
public class LockOperation extends AbstractOperation {

    private final NodeState nodeState;
    private final boolean isDeep;
    private final boolean isSessionScoped;
    private final long timeoutHint;
    private final String ownerHint;

    private LockInfo lockInfo = null;

    private LockOperation(NodeState nodeState, boolean isDeep, boolean isSessionScoped,
                          long timeoutHint, String ownerHint) {
        this.nodeState = nodeState;
        this.isDeep = isDeep;
        this.isSessionScoped = isSessionScoped;
        this.timeoutHint = timeoutHint;
        this.ownerHint = ownerHint;

        // NOTE: affected-states only needed for transient modifications
    }

    //----------------------------------------------------------< Operation >---
    /**
     * @see Operation#accept(OperationVisitor)
     */
    public void accept(OperationVisitor visitor) throws RepositoryException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException {
        assert status == STATUS_PENDING;
        visitor.visit(this);
    }

    /**
     * Invalidates the <code>NodeState</code> that has been locked.
     *
     * @see Operation#persisted()
     */
    public void persisted() {
        assert status == STATUS_PENDING;
        status = STATUS_PERSISTED;
        // non-recursive invalidation
        nodeState.getHierarchyEntry().invalidate(false);
    }

    //----------------------------------------< Access Operation Parameters >---
    public NodeId getNodeId() throws RepositoryException {
        return nodeState.getNodeId();
    }

    public boolean isDeep() {
        return isDeep;
    }

    public boolean isSessionScoped() {
        return isSessionScoped;
    }

    public long getTimeoutHint() {
        return timeoutHint;
    }

    public String getOwnerHint() {
        return ownerHint;
    }

    public void setLockInfo(LockInfo lockInfo) {
        if (lockInfo == null) {
            throw new IllegalArgumentException("IdIterator must not be null.");
        }
        if (this.lockInfo != null) {
            throw new IllegalStateException("Merge operation has already been executed -> FailedIds already set.");
        }
        this.lockInfo = lockInfo;
    }

    public LockInfo getLockInfo() {
        if (lockInfo == null) {
            throw new IllegalStateException("Merge operation has not been executed yet.");
        }
        return lockInfo;
    }
    //------------------------------------------------------------< Factory >---
    /**
     *
     * @param nodeState
     * @param isDeep
     * @return
     */
    public static LockOperation create(NodeState nodeState, boolean isDeep, boolean isSessionScoped) {
        return create(nodeState, isDeep, isSessionScoped, Long.MAX_VALUE, null);
    }

    public static LockOperation create(NodeState nodeState, boolean isDeep, boolean isSessionScoped, long timeoutHint, String ownerHint) {
        LockOperation lck = new LockOperation(nodeState, isDeep, isSessionScoped, timeoutHint, ownerHint);
        return lck;
    }
}