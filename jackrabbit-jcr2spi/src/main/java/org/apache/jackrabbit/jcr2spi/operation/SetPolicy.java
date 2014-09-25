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
import java.util.Iterator;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.operation.AddNode.SetPolicyAddNode;
import org.apache.jackrabbit.jcr2spi.operation.AddProperty.SetPolicyAddProperty;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;

public class SetPolicy extends TransientOperation {
 
    /**
     * List of operations added by this SetPolicy operation.
     */
    private static List<Operation> operations = new ArrayList<Operation>();
        
    private final NodeState aclNodeState;

    private SetPolicy(NodeState aclNodeState) throws RepositoryException {
        super(ItemStateValidator.CHECK_NONE);
        this.aclNodeState = aclNodeState;
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
        for (Operation op : operations) {
            op.undo();
        }
    }

    //--------------------------------------< Access Operation Parameters >--
    public NodeId getParentId() throws RepositoryException {
        return aclNodeState.getParent().getNodeId();
    }
    
    public NodeState getParentState() throws RepositoryException {
        return aclNodeState.getParent();
    }
    
    public Name getNodeName() {
        return aclNodeState.getName();
    }
    
    public Name getNodeTypeName() {
        return aclNodeState.getNodeTypeName();
    }
    
    public String getUuid() {
        return aclNodeState.getUniqueID();
    }
    
    public Iterator<NodeEntry> getEntryNodes() throws RepositoryException {
        return ((NodeEntry) aclNodeState.getHierarchyEntry()).getNodeEntries();
    }
    //--------------------------------------------------------< factory >---
    public static Operation create(NodeState aclNodeState) throws RepositoryException {
        SetPolicy sp = new SetPolicy(aclNodeState);
        return sp;
    }
    /**
     * Static factory method that creates an AddNode operation for this SetPolicy.
     * @param parentState
     * @param nodeName
     * @param nodeTypeName
     * @param uuid
     * @return
     * @throws RepositoryException
     */
    public static Operation createAddNode(NodeState parentState, Name nodeName, Name nodeTypeName, String uuid) throws RepositoryException {
        Operation addNode = SetPolicyAddNode.create(parentState, nodeName, nodeTypeName, uuid);        
        operations.add(addNode);
        return addNode;
    }
    /**
     * Static factory method that creates an AddProperty operation for this SetPolicy.
     * @param parentState
     * @param propName
     * @param propertyType
     * @param values
     * @param definition
     * @return
     * @throws RepositoryException
     */
    public static Operation createAddProperty(NodeState parentState, Name propName, 
                                              int propertyType, QValue[] values, 
                                              QPropertyDefinition definition) throws RepositoryException {
        Operation addProperty = SetPolicyAddProperty.create(parentState, propName, propertyType, values, definition);
        operations.add(addProperty);
        return addProperty;
    }
}


