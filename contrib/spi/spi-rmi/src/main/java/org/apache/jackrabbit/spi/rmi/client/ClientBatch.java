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
import org.apache.jackrabbit.spi.rmi.remote.RemoteBatch;
import org.apache.jackrabbit.name.QName;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ItemExistsException;
import javax.jcr.AccessDeniedException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import java.rmi.RemoteException;

/**
 * <code>ClientBatch</code> implements a SPI {@link Batch} which wraps a remote
 * batch.
 */
class ClientBatch implements Batch {

    /**
     * The remote batch.
     */
    private final RemoteBatch remoteBatch;

    ClientBatch(RemoteBatch remoteBatch) {
        this.remoteBatch = remoteBatch;
    }

    /**
     * @return the wrapped remote batch.
     */
    RemoteBatch getRemoteBatch() {
        return remoteBatch;
    }

    /**
     * {@inheritDoc}
     */
    public void addNode(NodeId parentId,
                        QName nodeName,
                        QName nodetypeName,
                        String uuid) throws RepositoryException {
        try {
            remoteBatch.addNode(parentId, nodeName, nodetypeName, uuid);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addProperty(NodeId parentId, QName propertyName, QValue value)
            throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        try {
            remoteBatch.addProperty(parentId, propertyName, value);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addProperty(NodeId parentId,
                            QName propertyName,
                            QValue[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, PathNotFoundException, ItemExistsException, AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        try {
            remoteBatch.addProperty(parentId, propertyName, values);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(PropertyId propertyId, QValue value)
            throws RepositoryException {
        try {
            remoteBatch.setValue(propertyId, value);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(PropertyId propertyId, QValue[] values)
            throws RepositoryException {
        try {
            remoteBatch.setValue(propertyId, values);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void remove(ItemId itemId) throws RepositoryException {
        try {
            remoteBatch.remove(itemId);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void reorderNodes(NodeId parentId,
                             NodeId srcNodeId,
                             NodeId beforeNodeId) throws RepositoryException {
        try {
            remoteBatch.reorderNodes(parentId, srcNodeId, beforeNodeId);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setMixins(NodeId nodeId, QName[] mixinNodeTypeIds)
            throws RepositoryException {
        try {
            remoteBatch.setMixins(nodeId, mixinNodeTypeIds);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void move(NodeId srcNodeId,
                     NodeId destParentNodeId,
                     QName destName) throws RepositoryException {
        try {
            remoteBatch.move(srcNodeId, destParentNodeId, destName);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }
}
