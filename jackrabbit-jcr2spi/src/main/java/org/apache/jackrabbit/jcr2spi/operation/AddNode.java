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
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>AddNode</code>...
 */
public class AddNode extends TransientOperation {

    private static Logger log = LoggerFactory.getLogger(AddNode.class);

    private final NodeId parentId;
    private final NodeState parentState;
    private final Name nodeName;
    private final Name nodeTypeName;
    private final String uuid;

    private final List<ItemState> addedStates = new ArrayList<ItemState>();

    private AddNode(NodeState parentState, Name nodeName, Name nodeTypeName, String uuid)
            throws RepositoryException {
        this(parentState, nodeName, nodeTypeName, uuid, DEFAULT_OPTIONS);
    }

    AddNode(NodeState parentState, Name nodeName, Name nodeTypeName,
            String uuid, int options) throws RepositoryException {
        super(options);
        this.parentId = parentState.getNodeId();
        this.parentState = parentState;
        this.nodeName = nodeName;
        this.nodeTypeName = nodeTypeName;
        this.uuid = uuid;

        addAffectedItemState(parentState);
    }

    //----------------------------------------------------------< Operation >---
    /**
     *
     * @param visitor
     */
    public void accept(OperationVisitor visitor) throws LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        assert status == STATUS_PENDING;
        visitor.visit(this);
    }

    /**
     * Throws UnsupportedOperationException
     *
     * @see Operation#persisted()
     */
    public void persisted() throws RepositoryException {
        assert status == STATUS_PENDING;
        status = STATUS_PERSISTED;
        parentState.getHierarchyEntry().complete(this);
    }

    /**
     * @see Operation#undo()
     */
    @Override
    public void undo() throws RepositoryException {
        assert status == STATUS_PENDING;
        status = STATUS_UNDO;
        parentState.getHierarchyEntry().complete(this);
    }

    //----------------------------------------< Access Operation Parameters >---
    public NodeId getParentId() {
        return parentId;
    }

    public NodeState getParentState() {
        return parentState;
    }

    public Name getNodeName() {
        return nodeName;
    }

    public Name getNodeTypeName() {
        return nodeTypeName;
    }

    public String getUuid() {
        return uuid;
    }

    public void addedState(List<ItemState> newStates) {
        addedStates.addAll(newStates);
    }

    public List<ItemState> getAddedStates() {
        return addedStates;
    }

    //------------------------------------------------------------< Factory >---
    /**
     *
     * @param parentState
     * @param nodeName
     * @param nodeTypeName
     * @param uuid
     * @return a new <code>AddNode</code> operation.
     */
    public static Operation create(NodeState parentState, Name nodeName,
                                   Name nodeTypeName, String uuid) throws RepositoryException {
        // make sure the parent hierarchy entry has its child entries loaded
        // in order to be able to detect conflicts.
        assertChildNodeEntries(parentState);

        AddNode an = new AddNode(parentState, nodeName, nodeTypeName, uuid);
        return an;
    }
}