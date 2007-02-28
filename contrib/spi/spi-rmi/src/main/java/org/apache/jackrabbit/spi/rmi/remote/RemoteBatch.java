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
package org.apache.jackrabbit.spi.rmi.remote;

import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.name.QName;

import javax.jcr.RepositoryException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * <code>RemoteBatch</code> is the remote version of the SPI
 * {@link org.apache.jackrabbit.spi.Batch} interface.
 */
public interface RemoteBatch extends Remote {

    /**
     * @see org.apache.jackrabbit.spi.Batch#addNode(org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.name.QName, org.apache.jackrabbit.name.QName, String)
     */
    public void addNode(NodeId parentId,
                        QName nodeName,
                        QName nodetypeName,
                        String uuid)
            throws RepositoryException, RemoteException;

    /**
     * @see org.apache.jackrabbit.spi.Batch#addProperty(org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.name.QName, org.apache.jackrabbit.spi.QValue)
     */
    public void addProperty(NodeId parentId, QName propertyName, QValue value)
            throws RepositoryException, RemoteException;

    /**
     * @see org.apache.jackrabbit.spi.Batch#addProperty(org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.name.QName, org.apache.jackrabbit.spi.QValue[])
     */
    public void addProperty(NodeId parentId,
                            QName propertyName,
                            QValue[] values)
            throws RepositoryException, RemoteException;

    /**
     * @see org.apache.jackrabbit.spi.Batch#setValue(org.apache.jackrabbit.spi.PropertyId, org.apache.jackrabbit.spi.QValue)
     */
    public void setValue(PropertyId propertyId, QValue value)
            throws RepositoryException, RemoteException;

    /**
     * @see org.apache.jackrabbit.spi.Batch#setValue(org.apache.jackrabbit.spi.PropertyId, org.apache.jackrabbit.spi.QValue[])
     */
    public void setValue(PropertyId propertyId, QValue[] values)
            throws RepositoryException, RemoteException;

    /**
     * @see org.apache.jackrabbit.spi.Batch#remove(org.apache.jackrabbit.spi.ItemId)
     */
    public void remove(ItemId itemId)
            throws RepositoryException, RemoteException;

    /**
     * @see org.apache.jackrabbit.spi.Batch#reorderNodes(org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.spi.NodeId)
     */
    public void reorderNodes(NodeId parentId,
                             NodeId srcNodeId,
                             NodeId beforeNodeId)
            throws RepositoryException, RemoteException;

    /**
     * @see org.apache.jackrabbit.spi.Batch#setMixins(org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.name.QName[])
     */
    public void setMixins(NodeId nodeId, QName[] mixinNodeTypeIds)
            throws RepositoryException, RemoteException;

    /**
     * @see org.apache.jackrabbit.spi.Batch#move(org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.spi.NodeId, org.apache.jackrabbit.name.QName)
     */
    public void move(NodeId srcNodeId,
                     NodeId destParentNodeId,
                     QName destName)
            throws RepositoryException, RemoteException;
}
