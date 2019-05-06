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
package org.apache.jackrabbit.spi.commons.logging;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.Tree;

/**
 * Log wrapper for a {@link Batch}.
 */
public class BatchLogger extends AbstractLogger implements Batch {
    private final Batch batch;

    /**
     * Create a new instance for the given <code>batch</code> which uses
     * <code>writer</code> for persisting log messages.
     * @param batch
     * @param writer
     */
    public BatchLogger(Batch batch, LogWriter writer) {
        super(writer);
        this.batch = batch;
    }

    /**
     * @return  the wrapped Batch
     */
    public Batch getBatch() {
        return batch;
    }

    // -----------------------------------------------------< Batch >---

    public void addNode(final NodeId parentId, final Name nodeName, final Name nodetypeName, final String uuid)
            throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                batch.addNode(parentId, nodeName, nodetypeName, uuid);
                return null;
            }}, "addNode(NodeId, Name, Name, String)", new Object[]{parentId, nodeName, nodetypeName, uuid});
    }

    public void addProperty(final NodeId parentId, final Name propertyName, final QValue value)
            throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                batch.addProperty(parentId, propertyName, value);
                return null;
            }}, "addProperty(NodeId, Name, QValue)", new Object[]{parentId, propertyName, value});
    }

    public void addProperty(final NodeId parentId, final Name propertyName, final QValue[] values)
            throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                batch.addProperty(parentId, propertyName, values);
                return null;
            }}, "addProperty(NodeId, Name, QValue[])", new Object[]{parentId, propertyName, values});
    }

    public void setValue(final PropertyId propertyId, final QValue value) throws RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                batch.setValue(propertyId, value);
                return null;
            }}, "setValue(PropertyId, QValue)", new Object[]{propertyId, value});
    }

    public void setValue(final PropertyId propertyId, final QValue[] values) throws RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                batch.setValue(propertyId, values);
                return null;
            }}, "setValue(PropertyId, QValue[])", new Object[]{propertyId, values});
    }

    public void remove(final ItemId itemId) throws RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                batch.remove(itemId);
                return null;
            }}, "remove(ItemId)", new Object[]{itemId});
    }

    public void reorderNodes(final NodeId parentId, final NodeId srcNodeId, final NodeId beforeNodeId)
            throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                batch.reorderNodes(parentId, srcNodeId, beforeNodeId);
                return null;
            }}, "reorderNodes(NodeId, NodeId, NodeId)", new Object[]{parentId, srcNodeId, beforeNodeId});
    }

    public void setMixins(final NodeId nodeId, final Name[] mixinNodeTypeNames) throws RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                batch.setMixins(nodeId, mixinNodeTypeNames);
                return null;
            }}, "setMixins(NodeId, Name[])", new Object[]{nodeId, mixinNodeTypeNames});
    }

    public void setPrimaryType(final NodeId nodeId, final Name primaryNodeTypeName) throws RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                batch.setPrimaryType(nodeId, primaryNodeTypeName);
                return null;
            }}, "setPrimaryType(NodeId, Name)", new Object[]{nodeId, primaryNodeTypeName});
    }

    public void move(final NodeId srcNodeId, final NodeId destParentNodeId, final Name destName)
            throws RepositoryException {

        execute(new Callable() {
            public Object call() throws RepositoryException {
                batch.move(srcNodeId, destParentNodeId, destName);
                return null;
            }}, "move(NodeId, NodeId, Name)", new Object[]{srcNodeId, destParentNodeId, destName});
    }

    @Override
    public void  setTree(final NodeId parentId, final Tree contentTree)
            throws RepositoryException {
        execute(new Callable() {
            public Object call() throws RepositoryException {
                batch.setTree(parentId, contentTree);
                return null;
            }}, "setTree(NodeId, Tree)", new Object[]{parentId, contentTree});
    }
}
