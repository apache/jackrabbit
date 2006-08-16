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

import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.NoSuchItemStateException;
import org.apache.jackrabbit.name.Path;

import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.AccessDeniedException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionException;

/**
 * <code>ReorderNodes</code>...
 */
public class ReorderNodes extends AbstractOperation {

    private final NodeState parentState;
    private final NodeState insert;
    private final NodeState before;

    private ReorderNodes(NodeState parentState, NodeState insert, NodeState before) {
        this.parentState = parentState;
        this.insert = insert;
        this.before = before;
        addAffectedItemState(parentState);
        addAffectedItemState(insert);
        if (before != null) {
            addAffectedItemState(before);
        }
    }

    //----------------------------------------------------------< Operation >---
    /**
     *
     * @param visitor
     */
    public void accept(OperationVisitor visitor) throws ConstraintViolationException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        visitor.visit(this);
    }

    //----------------------------------------< Access Operation Parameters >---
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

    public static Operation create(NodeState parentState, Path.PathElement srcName,
                                   Path.PathElement beforeName) throws NoSuchItemStateException, ItemStateException {
        NodeState insert = parentState.getChildNodeEntry(srcName.getName(), srcName.getNormalizedIndex()).getNodeState();
        NodeState before = (beforeName == null) ? null : parentState.getChildNodeEntry(beforeName.getName(), beforeName.getNormalizedIndex()).getNodeState();
        Operation op = new ReorderNodes(parentState, insert, before);
        return op;
    }
}