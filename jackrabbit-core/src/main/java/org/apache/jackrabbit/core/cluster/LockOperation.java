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
package org.apache.jackrabbit.core.cluster;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.Record;

/**
 * Lock operation.
 */
class LockOperation extends AbstractClusterOperation {

    /**
     * Node that was locked.
     */
    private NodeId nodeId;

    /**
     * Flag indicating whether this is a deep lock.
     */
    private boolean deep;

    /**
     * Lock owner
     */
    private String owner;

    /**
     * Flag indicating whether this is a lock. 
     */
    private boolean isLock;

    /**
     * Create an instance of this class. This operation will represent
     * a lock.
     */
    protected LockOperation(ClusterNode clusterNode, String workspace,
                            Record record, NodeId nodeId, boolean deep,
                            String owner) {

        super(clusterNode, workspace, record);

        this.nodeId = nodeId;
        this.deep = deep;
        this.owner = owner;
        isLock = true;
    }

    /**
     * Create an instance of this class. Used to represent an unlock
     * operation.
     *
     * @param clusterNode cluster node
     * @param workspace workspace where operation takes place.
     * @param nodeId unlocked node's id
     */
    protected LockOperation(ClusterNode clusterNode, String workspace,
                            Record record, NodeId nodeId) {

        super(clusterNode, workspace, record);

        this.nodeId = nodeId;
        isLock = false;
    }

    /**
     * Return a flag indicating whether this is a lock.
     *
     * @return <code>true</code> if this is a lock;
     *         <code>false</code> if this is an unlock
     */
    public boolean isLock() {
        return isLock;
    }

    /**
     * Return the target of the lock operation.
     *
     * @return node id
     */
    public NodeId getNodeId() {
        return nodeId;
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
     * Return the lock's owner.
     *
     * @return lock's owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * {@inheritDoc}
     */
    public void write() throws JournalException {
        record.writeChar('L');
        record.writeNodeId(nodeId);
        record.writeBoolean(isLock);
        if (isLock) {
            record.writeBoolean(deep);
            record.writeString(owner);
        }
    }
}
