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
package org.apache.jackrabbit.spi.commons.batch;


import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.Tree;

/**
 * This {@link ChangeLog} implementation simply keeps back all calls to its {@link Batch} methods as
 * a list of {@link #operations} (with item of type {@link Operation}). When {@link #apply(Batch)
 * applied} to a batch, all operations in the list are {@link Operation#apply(Batch) applied} to that
 * batch.
 */
public class ChangeLogImpl extends AbstractChangeLog<Operation> {

    public void addNode(NodeId parentId, Name nodeName, Name nodetypeName, String uuid)
            throws RepositoryException {

        addOperation(Operations.addNode(parentId, nodeName, nodetypeName, uuid));
    }

    public void addProperty(NodeId parentId, Name propertyName, QValue value) throws RepositoryException {
        addOperation(Operations.addProperty(parentId, propertyName, value));
    }

    public void addProperty(NodeId parentId, Name propertyName, QValue[] values)
            throws RepositoryException {

        addOperation(Operations.addProperty(parentId, propertyName, values));
    }

    public void move(NodeId srcNodeId, NodeId destParentNodeId, Name destName) throws RepositoryException {
        addOperation(Operations.move(srcNodeId, destParentNodeId, destName));
    }

    public void remove(ItemId itemId) throws RepositoryException {
        addOperation(Operations.remove(itemId));
    }

    public void reorderNodes(NodeId parentId, NodeId srcNodeId, NodeId beforeNodeId)
            throws RepositoryException {

        addOperation(Operations.reorderNodes(parentId, srcNodeId, beforeNodeId));
    }

    public void setMixins(NodeId nodeId, Name[] mixinNodeTypeNames) throws RepositoryException {
        addOperation(Operations.setMixins(nodeId, mixinNodeTypeNames));
    }

    public void setPrimaryType(NodeId nodeId, Name primaryNodeTypeName) throws RepositoryException {
        addOperation(Operations.setPrimaryType(nodeId, primaryNodeTypeName));
    }

    public void setValue(PropertyId propertyId, QValue value) throws RepositoryException {
        addOperation(Operations.setValue(propertyId, value));
    }

    public void setValue(PropertyId propertyId, QValue[] values) throws RepositoryException {
        addOperation(Operations.setValue(propertyId, values));
    }

    @Override
    public void setTree(NodeId parentId, Tree contentTree) throws RepositoryException {
        addOperation(Operations.setTree(parentId, contentTree));
    }
}

