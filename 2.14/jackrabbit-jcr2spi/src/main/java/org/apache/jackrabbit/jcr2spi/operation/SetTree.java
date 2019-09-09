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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.UpdatableItemStateManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;

public class SetTree extends TransientOperation {
 
    /**
     * List of operations added to this SetTree operation.
     */
    private final List<Operation> operations = new ArrayList<Operation>();
        
    private final NodeState treeState;

    private SetTree(NodeState treeState) throws RepositoryException {
        super(ItemStateValidator.CHECK_NONE);
        this.treeState = treeState;
    }

    private SetTree(UpdatableItemStateManager itemStateMgr, NodeState parentState, Name nodeName, Name nodeTypeName, String uuid) throws RepositoryException {
        super(ItemStateValidator.CHECK_NONE);
        Operation addNode = InternalAddNode.create(parentState, nodeName, nodeTypeName, uuid);
        operations.add(addNode);
        
        itemStateMgr.execute(addNode);
        treeState = (NodeState) ((AddNode) addNode).getAddedStates().get(0);
    }

    //-----------------------------------------------------------------< Operation >---
    /**
     * @param visitor
     */
    public void accept(OperationVisitor visitor) throws ValueFormatException, LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        assert status == STATUS_PENDING;
        visitor.visit(this);
    }

    /**
     * Persisting a SetPolicy operation involves persisting each individual operation added
     * by this policy. The concerned operation will assert the status and set it accordingly.
     *
     * @see Operation#persisted()
     */
    @Override
    public void persisted() throws RepositoryException {  
        assert status == STATUS_PENDING;
        status = STATUS_PERSISTED;
        for (Operation op : operations) {
            op.persisted();
        }
    }

    /**
     * Undoing a SetPolicy operation involves undoing all operations added by the SetPolicy.
     * @see Operation#undo()
     */
    @Override
    public void undo() throws RepositoryException {
        assert status == STATUS_PENDING;
        status = STATUS_PERSISTED;
        for (Operation op : operations) {
            op.undo();
        }
    }

    public NodeId getParentId() throws RepositoryException {
        return treeState.getParent().getNodeId();
    }
    
    public NodeState getParentState() throws RepositoryException {
        return treeState.getParent();
    }

    public NodeState getTreeState() throws RepositoryException {
        return treeState;
    }

    /**
     * Add a child node operation to this {@code setTree} instance.
     *
     * @param parentState
     * @param nodeName
     * @param nodeTypeName
     * @param uuid
     * @return
     * @throws RepositoryException
     */
    public Operation addChildNode(NodeState parentState, Name nodeName, Name nodeTypeName, String uuid) throws RepositoryException {
        Operation addNode = InternalAddNode.create(parentState, nodeName, nodeTypeName, uuid);
        operations.add(addNode);
        return addNode;
    }
    /**
     * Add a child property operation to this {@code setTree} instance.
     *
     * @param parentState
     * @param propName
     * @param propertyType
     * @param values
     * @param definition
     * @return
     * @throws RepositoryException
     */
    public Operation addChildProperty(NodeState parentState, Name propName,
                                      int propertyType, QValue[] values,
                                      QPropertyDefinition definition) throws RepositoryException {
        Operation addProperty = new InternalAddProperty(parentState, propName, propertyType, values, definition);
        operations.add(addProperty);
        return addProperty;
    }

    //------------------------------------------------------------< factory >---

    public static SetTree create(NodeState treeState) throws RepositoryException {
        SetTree operation = new SetTree(treeState);
        return operation;
    }

    public static SetTree create(UpdatableItemStateManager itemStateMgr, NodeState parent, Name nodeName, Name nodeTypeName, String uuid) throws RepositoryException {
        return new SetTree(itemStateMgr, parent, nodeName, nodeTypeName, uuid);
    }

    //--------------------------------------------------------------------------

    /**
     * Inner class for adding a protected node.
     */
    private static final class InternalAddNode extends AddNode implements IgnoreOperation {
        /**
         * Options that must not be violated for a successful set policy operation.
         */
        private final static int ADD_NODE_OPTIONS =  ItemStateValidator.CHECK_ACCESS |
                ItemStateValidator.CHECK_LOCK |
                ItemStateValidator.CHECK_COLLISION |
                ItemStateValidator.CHECK_VERSIONING;

        private InternalAddNode(NodeState parentState, Name nodeName, Name nodeTypeName, String uuid) throws RepositoryException {
            super(parentState, nodeName, nodeTypeName, uuid, ADD_NODE_OPTIONS);
        }

        public static Operation create(NodeState parentState, Name nodeName, Name nodeTypeName, String uuid) throws RepositoryException {
            assertChildNodeEntries(parentState);
            InternalAddNode an = new InternalAddNode(parentState, nodeName, nodeTypeName, uuid);
            return an;
        }
    }

    /**
     * Inner class for adding a protected property.
     */
    private static final class InternalAddProperty extends AddProperty implements IgnoreOperation {
        private final static int ADD_PROPERTY_OPTIONS =  ItemStateValidator.CHECK_ACCESS |
                ItemStateValidator.CHECK_LOCK |
                ItemStateValidator.CHECK_COLLISION |
                ItemStateValidator.CHECK_VERSIONING;

        private InternalAddProperty(NodeState parentState, Name propName, int propertyType, QValue[] values, QPropertyDefinition definition) throws RepositoryException {
            super(parentState, propName, propertyType, values, definition, ADD_PROPERTY_OPTIONS);
        }
    }
}


