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
package org.apache.jackrabbit.jcr2spi.version;

import java.util.Iterator;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.jcr2spi.WorkspaceManager;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.operation.AddLabel;
import org.apache.jackrabbit.jcr2spi.operation.Checkin;
import org.apache.jackrabbit.jcr2spi.operation.Checkout;
import org.apache.jackrabbit.jcr2spi.operation.Checkpoint;
import org.apache.jackrabbit.jcr2spi.operation.CreateActivity;
import org.apache.jackrabbit.jcr2spi.operation.CreateConfiguration;
import org.apache.jackrabbit.jcr2spi.operation.Merge;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.operation.RemoveActivity;
import org.apache.jackrabbit.jcr2spi.operation.RemoveLabel;
import org.apache.jackrabbit.jcr2spi.operation.RemoveVersion;
import org.apache.jackrabbit.jcr2spi.operation.ResolveMergeConflict;
import org.apache.jackrabbit.jcr2spi.operation.Restore;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * <code>VersionManagerImpl</code>...
 */
public class VersionManagerImpl implements VersionManager {

    private final WorkspaceManager workspaceManager;

    public VersionManagerImpl(WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    public NodeEntry checkin(NodeState nodeState) throws RepositoryException {
        Checkin ci = Checkin.create(nodeState, this);
        workspaceManager.execute(ci);
        return workspaceManager.getHierarchyManager().getNodeEntry(ci.getNewVersionId());
    }

    public void checkout(NodeState nodeState) throws RepositoryException {
        Operation co = Checkout.create(nodeState, this);
        workspaceManager.execute(co);
    }

    public void checkout(NodeState nodeState, NodeId activityId) throws RepositoryException {
        Operation co = Checkout.create(nodeState, activityId, this);
        workspaceManager.execute(co);
    }

    public NodeEntry checkpoint(NodeState nodeState) throws RepositoryException {
        Checkpoint cp = Checkpoint.create(nodeState, this);
        workspaceManager.execute(cp);
        return workspaceManager.getHierarchyManager().getNodeEntry(cp.getNewVersionId());
    }

    public NodeEntry checkpoint(NodeState nodeState, NodeId activityId) throws RepositoryException {
        Checkpoint cp = Checkpoint.create(nodeState, activityId, this);
        workspaceManager.execute(cp);
        return workspaceManager.getHierarchyManager().getNodeEntry(cp.getNewVersionId());
    }

    /**
     * Search nearest ancestor that is versionable. If no versionable ancestor
     * can be found, <code>true</code> is returned.
     *
     * @param nodeState
     * @return
     * @throws RepositoryException
     */
    public boolean isCheckedOut(NodeState nodeState) throws RepositoryException {
        // shortcut: if state is new, its ancestor must be checkout
        if (nodeState.getStatus() == Status.NEW) {
            return true;
        }

        NodeEntry nodeEntry = nodeState.getNodeEntry();
        try {
            // NOTE: since the hierarchy might not be completely loaded or some
            // entry might even not be accessible, the check may not detect
            // a checked-in parent. ok, as long as the 'server' finds out upon
            // save or upon executing the workspace operation.
            while (!nodeEntry.hasPropertyEntry(NameConstants.JCR_ISCHECKEDOUT)) {
                NodeEntry parent = nodeEntry.getParent();
                if (parent == null) {
                    // reached root state without finding a jcr:isCheckedOut property
                    return true;
                }
                nodeEntry = parent;
            }
            PropertyState propState = nodeEntry.getPropertyEntry(NameConstants.JCR_ISCHECKEDOUT).getPropertyState();
            Boolean b = Boolean.valueOf(propState.getValue().getString());
            return b.booleanValue();
        } catch (ItemNotFoundException e) {
            // error while accessing jcr:isCheckedOut property state.
            // -> assume that checkedOut status is ok. see above for general
            // notes about the capabilities of the jcr2spi implementation.
        }
        return true;
    }

    public void checkIsCheckedOut(NodeState nodeState) throws VersionException, RepositoryException {
        if (!isCheckedOut(nodeState)) {
            throw new VersionException(nodeState + " is checked-in");
        }
    }

    public void removeVersion(NodeState versionHistoryState, NodeState versionState) throws RepositoryException {
        Operation op = RemoveVersion.create(versionState, versionHistoryState, this);
        workspaceManager.execute(op);
    }

    public void addVersionLabel(NodeState versionHistoryState, NodeState versionState, Name qLabel, boolean moveLabel) throws RepositoryException {
        Operation op = AddLabel.create(versionHistoryState, versionState, qLabel, moveLabel);
        workspaceManager.execute(op);
    }

    public void removeVersionLabel(NodeState versionHistoryState, NodeState versionState, Name qLabel) throws RepositoryException {
        Operation op = RemoveLabel.create(versionHistoryState, versionState, qLabel);
        workspaceManager.execute(op);
    }

    public void restore(NodeState nodeState, Path relativePath, NodeState versionState, boolean removeExisting) throws RepositoryException {
        Operation op = Restore.create(nodeState, relativePath, versionState, removeExisting);
        workspaceManager.execute(op);
    }

    public void restore(NodeState[] versionStates, boolean removeExisting) throws RepositoryException {
        Operation op = Restore.create(versionStates, removeExisting);
        workspaceManager.execute(op);
    }

    public Iterator<NodeId> merge(NodeState nodeState, String workspaceName, boolean bestEffort) throws RepositoryException {
        return merge(nodeState, workspaceName, bestEffort, false);
    }

    public Iterator<NodeId> merge(NodeState nodeState, String workspaceName, boolean bestEffort, boolean isShallow) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        Merge op = Merge.create(nodeState, workspaceName, bestEffort, isShallow, this);
        workspaceManager.execute(op);
        return op.getFailedIds();
    }

    public void resolveMergeConflict(NodeState nodeState, NodeState versionState,
                                     boolean done) throws RepositoryException {
        NodeId vId = versionState.getNodeId();

        PropertyState mergeFailedState = nodeState.getPropertyState(NameConstants.JCR_MERGEFAILED);
        QValue[] vs = mergeFailedState.getValues();

        NodeId[] mergeFailedIds = new NodeId[vs.length - 1];
        for (int i = 0, j = 0; i < vs.length; i++) {
            NodeId id = workspaceManager.getIdFactory().createNodeId(vs[i].getString());
            if (!id.equals(vId)) {
                mergeFailedIds[j] = id;
                j++;
            }
            // else: the version id is being solved by this call and not
            // part of 'jcr:mergefailed' any more
        }

        PropertyState predecessorState = nodeState.getPropertyState(NameConstants.JCR_PREDECESSORS);
        vs = predecessorState.getValues();

        int noOfPredecessors = (done) ? vs.length + 1 : vs.length;
        NodeId[] predecessorIds = new NodeId[noOfPredecessors];

        int i = 0;
        while (i < vs.length) {
            predecessorIds[i] = workspaceManager.getIdFactory().createNodeId(vs[i].getString());
            i++;
        }
        if (done) {
            predecessorIds[i] = vId;
        }
        Operation op = ResolveMergeConflict.create(nodeState, mergeFailedIds, predecessorIds, done);
        workspaceManager.execute(op);
    }

    public NodeEntry createConfiguration(NodeState nodeState) throws UnsupportedRepositoryOperationException, RepositoryException {
        CreateConfiguration op = CreateConfiguration.create(nodeState, this);
        workspaceManager.execute(op);
        return workspaceManager.getHierarchyManager().getNodeEntry(op.getNewConfigurationId());
    }

    public NodeEntry createActivity(String title) throws UnsupportedRepositoryOperationException, RepositoryException {
        CreateActivity op = CreateActivity.create(title, this);
        workspaceManager.execute(op);
        return workspaceManager.getHierarchyManager().getNodeEntry(op.getNewActivityId());
    }

    public void removeActivity(NodeState activityState) throws UnsupportedRepositoryOperationException, RepositoryException {
        Operation op = RemoveActivity.create(activityState, workspaceManager.getHierarchyManager());
        workspaceManager.execute(op);
    }

    public Iterator<NodeId> mergeActivity(NodeState activityState) throws UnsupportedRepositoryOperationException, RepositoryException {
        Merge op = Merge.create(activityState, null, false, false, this);
        workspaceManager.execute(op);
        return op.getFailedIds();
    }

    public NodeEntry getVersionableNodeEntry(NodeState versionState) throws RepositoryException {
        NodeState ns = versionState.getChildNodeState(NameConstants.JCR_FROZENNODE, Path.INDEX_DEFAULT);
        PropertyState ps = ns.getPropertyState(NameConstants.JCR_FROZENUUID);
        String uniqueID = ps.getValue().getString();

        NodeId versionableId = workspaceManager.getIdFactory().createNodeId(uniqueID);
        return workspaceManager.getHierarchyManager().getNodeEntry(versionableId);
    }

    public NodeEntry getVersionHistoryEntry(NodeState versionableState) throws RepositoryException {
        PropertyState ps = versionableState.getPropertyState(NameConstants.JCR_VERSIONHISTORY);
        String uniqueID = ps.getValue().getString();
        NodeId vhId = workspaceManager.getIdFactory().createNodeId(uniqueID);
        return workspaceManager.getHierarchyManager().getNodeEntry(vhId);
    }
}