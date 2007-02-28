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
package org.apache.jackrabbit.spi.rmi.server;

import org.apache.jackrabbit.spi.rmi.remote.RemoteBatch;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.name.QName;

import javax.jcr.RepositoryException;
import java.rmi.RemoteException;

/**
 * <code>ServerBatch</code> implements the server side of a remote batch.
 */
class ServerBatch extends ServerObject implements RemoteBatch {

    /**
     * The wrapped SPI batch.
     */
    private final Batch batch;

    ServerBatch(Batch batch) throws RemoteException {
        this.batch = batch;
    }

    /**
     * @return the wrapped SPI batch.
     */
    Batch getBatch() {
        return batch;
    }

    /**
     * {@inheritDoc}
     */
    public void addNode(NodeId parentId,
                        QName nodeName,
                        QName nodetypeName,
                        String uuid) throws RepositoryException, RemoteException {
        try {
            batch.addNode(parentId, nodeName, nodetypeName, uuid);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addProperty(NodeId parentId, QName propertyName, QValue value)
            throws RepositoryException, RemoteException {
        try {
            batch.addProperty(parentId, propertyName, value);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addProperty(NodeId parentId,
                            QName propertyName,
                            QValue[] values) throws RepositoryException, RemoteException {
        try {
            batch.addProperty(parentId, propertyName, values);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(PropertyId propertyId, QValue value)
            throws RepositoryException, RemoteException {
        try {
            batch.setValue(propertyId, value);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(PropertyId propertyId, QValue[] values)
            throws RepositoryException, RemoteException {
        try {
            batch.setValue(propertyId, values);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void remove(ItemId itemId) throws RepositoryException, RemoteException {
        try {
            batch.remove(itemId);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void reorderNodes(NodeId parentId,
                             NodeId srcNodeId,
                             NodeId beforeNodeId) throws RepositoryException, RemoteException {
        try {
            batch.reorderNodes(parentId, srcNodeId, beforeNodeId);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setMixins(NodeId nodeId, QName[] mixinNodeTypeIds)
            throws RepositoryException, RemoteException {
        try {
            batch.setMixins(nodeId, mixinNodeTypeIds);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void move(NodeId srcNodeId,
                     NodeId destParentNodeId,
                     QName destName) throws RepositoryException, RemoteException {
        try {
            batch.move(srcNodeId, destParentNodeId, destName);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }
}
