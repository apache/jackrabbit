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
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;

/**
 * <code>ReorderNodes</code>...
 */
public class ReorderNodes extends TransientOperation {

    private final NodeId parentId;
    private final NodeId insertId;
    private final NodeId beforeId;

    private final NodeState parentState;
    private final NodeState insert;
    private final NodeState before;

    private ReorderNodes(NodeState parentState, NodeState insert, NodeState before)
            throws RepositoryException {
        super(NO_OPTIONS);
        this.parentState = parentState;
        this.insert = insert;
        this.before = before;

        this.parentId = parentState.getNodeId();
        this.insertId = insert.getNodeId();
        this.beforeId = (before == null) ? null : before.getNodeId();

        addAffectedItemState(parentState);
    }

    //----------------------------------------------------------< Operation >---
    /**
     *
     * @param visitor
     */
    public void accept(OperationVisitor visitor) throws ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
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
        insert.getHierarchyEntry().complete(this);
    }

    /**
     * @see Operation#undo()
     */
    @Override
    public void undo() throws RepositoryException {
        assert status == STATUS_PENDING;
        status = STATUS_UNDO;
        insert.getHierarchyEntry().complete(this);
    }

    //----------------------------------------< Access Operation Parameters >---

    public NodeId getParentId() {
        return parentId;
    }

    public NodeId getInsertId() {
        return insertId;
    }

    public NodeId getBeforeId() {
        return beforeId;
    }

    public NodeState getParentState() {
        return parentState;
    }

    public NodeState getInsertNode() {
        return insert;
    }

    public NodeState getBeforeNode() {
        return before;
    }

    //------------------------------------------------------------< Factory >---

    public static Operation create(
            NodeState parentState, Path srcPath, Path beforePath)
            throws ItemNotFoundException, RepositoryException {
        // make sure the parent hierarchy entry has its child entries loaded
        assertChildNodeEntries(parentState);

        NodeState insert = parentState.getChildNodeState(
                srcPath.getName(), srcPath.getNormalizedIndex());
        NodeState before = null;
        if (beforePath != null) {
            before = parentState.getChildNodeState(
                    beforePath.getName(), beforePath.getNormalizedIndex());
        }
        return new ReorderNodes(parentState, insert, before);
    }
}