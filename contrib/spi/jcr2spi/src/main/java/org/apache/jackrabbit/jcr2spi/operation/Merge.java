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
import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.jcr2spi.version.VersionManager;
import org.apache.jackrabbit.spi.IdIterator;
import org.apache.jackrabbit.spi.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * <code>Merge</code>...
 */
public class Merge extends AbstractOperation {

    private static Logger log = LoggerFactory.getLogger(Merge.class);

    private final NodeState nodeState;
    private final String srcWorkspaceName;
    private final boolean bestEffort;
    private final VersionManager mgr;

    private IdIterator failedIds = null;

    private Merge(NodeState nodeState, String srcWorkspaceName, boolean bestEffort, VersionManager mgr) {
        this.nodeState = nodeState;
        this.srcWorkspaceName = srcWorkspaceName;
        this.bestEffort = bestEffort;
        this.mgr = mgr;

        // NOTE: affected-states only needed for transient modifications
    }

    //----------------------------------------------------------< Operation >---
    /**
     * @see Operation#accept(OperationVisitor)
     */
    public void accept(OperationVisitor visitor) throws RepositoryException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException {
        visitor.visit(this);
    }

    /**
     * Invalidates the target nodestate and all descendants.
     *
     * @see Operation#persisted(CacheBehaviour)
     * @param cacheBehaviour
     */
    public void persisted(CacheBehaviour cacheBehaviour) {
        if (cacheBehaviour == CacheBehaviour.INVALIDATE) {
            try {
                mgr.getVersionHistoryNodeState(nodeState).invalidate(true);
            } catch (RepositoryException e) {
                log.warn("Error while retrieving VersionHistory state:", e.getMessage());
            }
            nodeState.getHierarchyEntry().invalidate(true);
        }
    }

    //----------------------------------------< Access Operation Parameters >---
    public NodeId getNodeId() {
        return nodeState.getNodeId();
    }

    public String getSourceWorkspaceName() {
        return srcWorkspaceName;
    }

    public boolean bestEffort() {
        return bestEffort;
    }

    public void setFailedIds(IdIterator failedIds) {
        if (failedIds == null) {
            throw new IllegalArgumentException("IdIterator must not be null.");
        }
        if (this.failedIds != null) {
            throw new IllegalStateException("Merge operation has already been executed -> FailedIds already set.");
        }
        this.failedIds = failedIds;
    }

    public IdIterator getFailedIds() {
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
    public static Merge create(NodeState nodeState, String srcWorkspaceName, boolean bestEffort, VersionManager mgr) {
        return new Merge(nodeState, srcWorkspaceName, bestEffort, mgr);
    }
}