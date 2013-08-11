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

import java.util.Iterator;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.version.VersionManager;
import org.apache.jackrabbit.spi.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>Merge</code>...
 */
public class Merge extends AbstractOperation {

    private static Logger log = LoggerFactory.getLogger(Merge.class);

    private final NodeState nodeState;
    private final String srcWorkspaceName;
    private final boolean bestEffort;
    private final boolean isShallow;
    private final VersionManager mgr;

    private Iterator<NodeId> failedIds = null;

    private Merge(NodeState nodeState, String srcWorkspaceName, boolean bestEffort, boolean isShallow, VersionManager mgr) {
        this.nodeState = nodeState;
        this.srcWorkspaceName = srcWorkspaceName;
        this.bestEffort = bestEffort;
        this.isShallow = isShallow;
        this.mgr = mgr;

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
     * Invalidates the target nodestate and all descendants.
     *
     * @see Operation#persisted()
     */
    public void persisted() {
        assert status == STATUS_PENDING;
        status = STATUS_PERSISTED;
        if (isActivityMerge()) {
            // TODO be more specific about what needs to be invalidated
            // look for the root entry and invalidate the complete tree
            HierarchyEntry entry = nodeState.getNodeEntry();
            while (entry.getParent() != null) {
                entry = entry.getParent();
            }
            entry.invalidate(true);
        } else {
            try {
                NodeEntry vhe = mgr.getVersionHistoryEntry(nodeState);
                if (vhe != null) {
                    vhe.invalidate(true);
                }
            } catch (RepositoryException e) {
                log.warn("Error while retrieving VersionHistory entry: {}", e.getMessage());
            }
            nodeState.getHierarchyEntry().invalidate(true);
        }
    }

    //----------------------------------------< Access Operation Parameters >---
    public NodeId getNodeId() throws RepositoryException {
        return nodeState.getNodeEntry().getWorkspaceId();
    }

    public String getSourceWorkspaceName() {
        return srcWorkspaceName;
    }

    public boolean bestEffort() {
        return bestEffort;
    }

    public boolean isShallow() {
        return isShallow;
    }

    public boolean isActivityMerge() {
        return srcWorkspaceName == null;
    }

    public void setFailedIds(Iterator<NodeId> failedIds) {
        if (failedIds == null) {
            throw new IllegalArgumentException("IdIterator must not be null.");
        }
        if (this.failedIds != null) {
            throw new IllegalStateException("Merge operation has already been executed -> FailedIds already set.");
        }
        this.failedIds = failedIds;
    }

    public Iterator<NodeId> getFailedIds() {
        if (failedIds == null) {
            throw new IllegalStateException("Merge operation has not been executed yet.");
        }
        return failedIds;
    }
    //------------------------------------------------------------< Factory >---
    /**
     *
     * @param nodeState
     * @param srcWorkspaceName
     * @return
     */
    public static Merge create(NodeState nodeState, String srcWorkspaceName, boolean bestEffort, boolean isShallow, VersionManager mgr) {
        return new Merge(nodeState, srcWorkspaceName, bestEffort, isShallow, mgr);
    }
}
