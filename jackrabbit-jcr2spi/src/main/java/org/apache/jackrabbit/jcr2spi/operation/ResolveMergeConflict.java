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

import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.PropertyEntry;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.spi.NodeId;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import java.util.Iterator;

/**
 * <code>ResolveMergeConflict</code>...
 */
public class ResolveMergeConflict extends AbstractOperation {

    private final NodeState nodeState;
    private final NodeId[] mergeFailedIds;
    private final NodeId[] predecessorIds;
    private final boolean resolveDone;

    private ResolveMergeConflict(NodeState nodeState, NodeId[] mergeFailedIds, NodeId[] predecessorIds, boolean resolveDone) {
        this.nodeState = nodeState;
        this.mergeFailedIds = mergeFailedIds;
        this.predecessorIds = predecessorIds;
        this.resolveDone = resolveDone;


        // NOTE: affected-states only needed for transient modifications
    }

    //----------------------------------------------------------< Operation >---
    /**
     * @see Operation#accept(OperationVisitor)
     */
    public void accept(OperationVisitor visitor) throws RepositoryException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException {
        assert status == STATUS_PENDING;
        visitor.visit(this);
    }

    /**
     * Invalidates the <code>NodeState</code> that had a merge conflict pending
     * and all its child properties.
     *
     * @see Operation#persisted()
     */
    public void persisted() {
        assert status == STATUS_PENDING;
        status = STATUS_PERSISTED;
        // non-recursive invalidation BUT including all properties
        Iterator<PropertyEntry> propEntries = ((NodeEntry) nodeState.getHierarchyEntry()).getPropertyEntries();
        while (propEntries.hasNext()) {
            PropertyEntry pe = propEntries.next();
            pe.invalidate(false);
        }
        nodeState.getHierarchyEntry().invalidate(false);
    }
    //----------------------------------------< Access Operation Parameters >---
    public NodeId getNodeId() throws RepositoryException {
        return nodeState.getNodeEntry().getWorkspaceId();
    }

    public NodeId[] getMergeFailedIds() {
        return mergeFailedIds;
    }

    public NodeId[] getPredecessorIds() {
        return predecessorIds;
    }

    public boolean resolveDone() {
        return resolveDone;
    }

    //------------------------------------------------------------< Factory >---
    /**
     *
     * @param nodeState
     * @param mergeFailedIds
     * @param predecessorIds
     * @param resolveDone
     */
    public static Operation create(NodeState nodeState, NodeId[] mergeFailedIds, NodeId[] predecessorIds, boolean resolveDone) {
        ResolveMergeConflict up = new ResolveMergeConflict(nodeState, mergeFailedIds, predecessorIds, resolveDone);
        return up;
    }
}