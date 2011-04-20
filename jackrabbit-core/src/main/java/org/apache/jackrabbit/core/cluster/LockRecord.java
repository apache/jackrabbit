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

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.Record;

/**
 * Cluster record representing a lock or unlock operation.
 */
public class LockRecord extends ClusterRecord {

    /**
     * Identifier: LOCK.
     */
    static final char IDENTIFIER = 'L';

    /**
     * Node id.
     */
    private NodeId nodeId;

    /**
     * Flag indicating whether this is a lock or an unlock.
     */
    private boolean isLock;

    /**
     * Flag indicating whether the lock is deep.
     */
    private boolean isDeep;

    /**
     * Lock owner.
     */
    private String lockOwner;

    /**
     * Create a new instance of this class. Used when a lock operation should
     * be serialized.
     *
     * @param nodeId node id
     * @param isDeep flag indicating whether the lock is deep
     * @param lockOwner the name of the lock owner.
     * @param record journal record
     * @param workspace workspace
     */
    public LockRecord(NodeId nodeId, boolean isDeep, String lockOwner,
                      Record record, String workspace) {
        super(record, workspace);

        this.nodeId = nodeId;
        this.isLock = true;
        this.isDeep = isDeep;
        this.lockOwner = lockOwner;
    }

    /**
     * Create a new instance of this class. Used when an unlock operation should
     * be serialized.
     *
     * @param nodeId node id
     * @param record journal record
     * @param workspace workspace
     */
    public LockRecord(NodeId nodeId, Record record, String workspace) {
        super(record, workspace);

        this.nodeId = nodeId;
        this.isLock = false;
    }

    /**
     * Create a new instance of this class. Used when a deserializing either a
     * lock or an unlock operation.
     *
     * @param record journal record
     * @param workspace workspace
     */
    LockRecord(Record record, String workspace) {
        super(record, workspace);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doRead() throws JournalException {
        nodeId = record.readNodeId();
        isLock = record.readBoolean();
        if (isLock) {
            isDeep = record.readBoolean();
            lockOwner = record.readString();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doWrite() throws JournalException {
        record.writeChar(IDENTIFIER);
        record.writeNodeId(nodeId);
        record.writeBoolean(isLock);
        if (isLock) {
            record.writeBoolean(isDeep);
            record.writeString(lockOwner);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(ClusterRecordProcessor processor) {
        processor.process(this);
    }

    /**
     * Return the node id.
     *
     * @return node id
     */
    public NodeId getNodeId() {
        return nodeId;
    }

    /**
     * Return a flag indicating whether this is a lock or an unlock operation.
     *
     * @return <code>true</code> if this is a lock operation;
     *         <code>false</code> if this is an unlock operation
     */
    public boolean isLock() {
        return isLock;
    }

    /**
     * Return a flag indicating whether the lock is deep.
     *
     * @return <code>true</code> if the lock is deep;
     *         <code>false</code> otherwise
     */
    public boolean isDeep() {
        return isDeep;
    }

    /**
     * Return the user id associated with the lock operation.
     *
     * @return user id
     * @deprecated User {@link #getOwner()} instead.
     */
    public String getUserId() {
        return lockOwner;
    }

    /**
     * Return the lock owner associated with the lock operation.
     *
     * @return lock owner associated with the lock operation.
     */
    public String getOwner() {
        return lockOwner;
    }
}
