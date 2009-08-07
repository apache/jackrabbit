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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QValue;

/**
 * This {@link ChangeLog} implementation simply keeps back all calls to its {@link Batch} methods as
 * a list of {@link #operations} (with item of type {@link Operation}). When {@link #apply(Batch)
 * applied} to a batch, all operations in the list are {@link Operation#apply(Batch) applied} to that
 * batch.
 */
public class ChangeLogImpl implements ChangeLog {

    /**
     * {@link Operation}s kept in this change log.
     */
    protected final List operations = new LinkedList();

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

    public void setValue(PropertyId propertyId, QValue value) throws RepositoryException {
        addOperation(Operations.setValue(propertyId, value));
    }

    public void setValue(PropertyId propertyId, QValue[] values) throws RepositoryException {
        addOperation(Operations.setValue(propertyId, values));
    }

    public Batch apply(Batch batch) throws RepositoryException {
        if (batch == null) {
            throw new IllegalArgumentException("Batch must not be null");
        }
        for (Iterator it = operations.iterator(); it.hasNext(); ) {
            Operation op = (Operation) it.next();
            op.apply(batch);
        }
        return batch;
    }

    /**
     * This method is called when an operation is added to the list of {@link #operations}
     * kept by this change log.
     * @param op  {@link Operation} to add
     * @throws RepositoryException
     */
    protected void addOperation(Operation op) throws RepositoryException {
        operations.add(op);
    }

    // -----------------------------------------------------< Object >---

    public String toString() {
        StringBuffer b = new StringBuffer();
        for (Iterator it = operations.iterator(); it.hasNext(); ) {
            b.append(it.next());
            if (it.hasNext()) {
                b.append(", ");
            }
        }
        return b.toString();
    }

    public boolean equals(Object other) {
        if (null == other) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (other instanceof ChangeLogImpl) {
            return equals((ChangeLogImpl) other);
        }
        return false;
    }

    public boolean equals(ChangeLogImpl other) {
        return operations.equals(other.operations);
    }

    public int hashCode() {
        throw new IllegalArgumentException("Not hashable");
    }

}

