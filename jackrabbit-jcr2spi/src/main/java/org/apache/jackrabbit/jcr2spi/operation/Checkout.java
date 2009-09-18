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
import org.apache.jackrabbit.jcr2spi.version.VersionManager;
import org.apache.jackrabbit.spi.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import java.util.Iterator;

/**
 * <code>Checkout</code>...
 */
public class Checkout extends AbstractOperation {

    private static Logger log = LoggerFactory.getLogger(Checkout.class);

    private final NodeState nodeState;
    private final NodeId activityId;
    private final boolean supportsActivity;
    private final VersionManager mgr;

    private Checkout(NodeState nodeState, VersionManager mgr) {
        this.nodeState = nodeState;
        this.mgr = mgr;
        supportsActivity = false;
        activityId = null;
        // NOTE: affected-states only needed for transient modifications
    }

    private Checkout(NodeState nodeState, NodeId activityId, VersionManager mgr) {
        this.nodeState = nodeState;
        this.activityId = activityId;
        this.mgr = mgr;
        supportsActivity = true;
        // NOTE: affected-states only needed for transient modifications
    }

    //----------------------------------------------------------< Operation >---
    public void accept(OperationVisitor visitor) throws RepositoryException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException {
        assert status == STATUS_PENDING;
        visitor.visit(this);
    }

    /**
     * Invalidate the target <code>NodeState</code>.
     *
     * @see Operation#persisted()
     */
    public void persisted() {
        assert status == STATUS_PENDING;
        status = STATUS_PERSISTED;
        try {
            NodeEntry vhe = mgr.getVersionHistoryEntry(nodeState);
            if (vhe != null) {
                vhe.invalidate(true);
            }
        } catch (RepositoryException e) {
            log.warn("Failed to access Version history entry -> skip invalidation.", e);
        }
        // non-recursive invalidation (but including all properties)
        NodeEntry nodeEntry = (NodeEntry) nodeState.getHierarchyEntry();
        Iterator<PropertyEntry> entries = nodeEntry.getPropertyEntries();
        while (entries.hasNext()) {
            PropertyEntry pe = entries.next();
            pe.invalidate(false);
        }
        nodeEntry.invalidate(false);
    }

    //----------------------------------------< Access Operation Parameters >---
    /**
     *
     * @return
     */
    public NodeId getNodeId() throws RepositoryException {
        return nodeState.getNodeEntry().getWorkspaceId();
    }

    /**
     * The id of the current activity present on the editing session or <code>null</code>.
     *
     * @return id of the current activity present on the editing session or <code>null</code>.
     */
    public NodeId getActivityId() {
        return activityId;
    }

    /**
     * Returns <code>true</code>, if activities are supported, 
     * <code>false</code> otherwise.
     *
     * @return  <code>true</code>, if activities are supported,
     * <code>false</code> otherwise.
     */
    public boolean supportsActivity() {
        return supportsActivity;
    }

    //------------------------------------------------------------< Factory >---
    public static Operation create(NodeState nodeState, VersionManager mgr) {
        return new Checkout(nodeState, mgr);
    }

    public static Operation create(NodeState nodeState, NodeId activityId, VersionManager mgr) {
        return new Checkout(nodeState, activityId, mgr);
    }
}