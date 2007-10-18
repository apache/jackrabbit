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
package org.apache.jackrabbit.spi.rmi.client;

import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.SerializableBatch;
import org.apache.jackrabbit.spi.rmi.remote.RemoteSessionInfo;

/**
 * <code>ClientBatch</code> implements a SPI {@link Batch} which wraps a
 * serializable batch.
 */
class ClientBatch implements Batch {

    /**
     * The remote session info.
     */
    private final RemoteSessionInfo remoteSession;

    /**
     * The underlying serializable batch.
     */
    private final SerializableBatch batch;

    ClientBatch(RemoteSessionInfo remoteSession, ItemId itemId) {
        this.remoteSession = remoteSession;
        this.batch = new SerializableBatch(itemId);
    }

    /**
     * @return the wrapped remote batch.
     */
    public SerializableBatch getSerializableBatch() {
        return batch;
    }

    /**
     * @return the remote session info associated with this batch.
     */
    public RemoteSessionInfo getRemoteSessionInfo() {
        return remoteSession;
    }

    /**
     * {@inheritDoc}
     */
    public void addNode(NodeId parentId,
                        Name nodeName,
                        Name nodetypeName,
                        String uuid) {
        batch.addNode(parentId, nodeName, nodetypeName, uuid);
    }

    /**
     * {@inheritDoc}
     */
    public void addProperty(NodeId parentId, Name propertyName, QValue value) {
        batch.addProperty(parentId, propertyName, value);
    }

    /**
     * {@inheritDoc}
     */
    public void addProperty(NodeId parentId,
                            Name propertyName,
                            QValue[] values) {
        batch.addProperty(parentId, propertyName, values);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(PropertyId propertyId, QValue value) {
        batch.setValue(propertyId, value);
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(PropertyId propertyId, QValue[] values) {
        batch.setValue(propertyId, values);
    }

    /**
     * {@inheritDoc}
     */
    public void remove(ItemId itemId) {
        batch.remove(itemId);
    }

    /**
     * {@inheritDoc}
     */
    public void reorderNodes(NodeId parentId,
                             NodeId srcNodeId,
                             NodeId beforeNodeId) {
        batch.reorderNodes(parentId, srcNodeId, beforeNodeId);
    }

    /**
     * {@inheritDoc}
     */
    public void setMixins(NodeId nodeId, Name[] mixinNodeTypeIds) {
        batch.setMixins(nodeId, mixinNodeTypeIds);
    }

    /**
     * {@inheritDoc}
     */
    public void move(NodeId srcNodeId,
                     NodeId destParentNodeId,
                     Name destName) {
        batch.move(srcNodeId, destParentNodeId, destName);
    }
}
