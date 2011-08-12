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

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;

/**
 * <code>SetPrimaryNodeType</code>...
 */
public class SetPrimaryType extends TransientOperation {

    private final static int SET_PRIMARY_TYPE_OPTIONS =
            ItemStateValidator.CHECK_VERSIONING
            | ItemStateValidator.CHECK_LOCK;

    private final NodeId nodeId;
    private final NodeState nodeState;
    private final Name primaryTypeName;

    private SetPrimaryType(NodeState nodeState, Name primaryTypeName) throws RepositoryException {
        this(nodeState, primaryTypeName, SET_PRIMARY_TYPE_OPTIONS);
    }
    private SetPrimaryType(NodeState nodeState, Name primaryTypeName, int options) throws RepositoryException {
        super(options);
        this.nodeState = nodeState;
        this.nodeId = nodeState.getNodeId();
        this.primaryTypeName = primaryTypeName;

        // remember node state as affected state
        addAffectedItemState(nodeState);
    }

    //----------------------------------------------------------< Operation >---
    /**
     *
     * @param visitor
     * @see Operation#accept(OperationVisitor)
     */
    public void accept(OperationVisitor visitor) throws AccessDeniedException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        assert status == STATUS_PENDING;
        visitor.visit(this);
    }

    /**
     * @see Operation#persisted()
     */
    public void persisted() throws RepositoryException {
        assert status == STATUS_PENDING;
        status = STATUS_PERSISTED;
        nodeState.getHierarchyEntry().complete(this);
    }

    /**
     * @see Operation#undo()
     */
    @Override
    public void undo() throws RepositoryException {
        assert status == STATUS_PENDING;
        status = STATUS_UNDO;
        nodeState.getHierarchyEntry().complete(this);
    }

    //----------------------------------------< Access Operation Parameters >---
    public NodeState getNodeState() {
        return nodeState;
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    public Name getPrimaryTypeName() {
        return primaryTypeName;
    }

    //------------------------------------------------------------< Factory >---

    public static Operation create(NodeState nodeState, Name primaryTypeName)
            throws RepositoryException {
        if (nodeState == null || primaryTypeName == null) {
            throw new IllegalArgumentException();
        }
        SetPrimaryType op = new SetPrimaryType(nodeState, primaryTypeName);
        return op;
    }
}