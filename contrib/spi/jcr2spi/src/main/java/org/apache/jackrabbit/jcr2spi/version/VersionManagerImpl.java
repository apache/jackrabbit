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

import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.UpdatableItemStateManager;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.observation.InternalEventListener;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.operation.Checkout;
import org.apache.jackrabbit.jcr2spi.operation.Checkin;
import org.apache.jackrabbit.jcr2spi.operation.Restore;
import org.apache.jackrabbit.jcr2spi.operation.ResolveMergeConflict;
import org.apache.jackrabbit.jcr2spi.operation.Merge;
import org.apache.jackrabbit.jcr2spi.operation.Remove;
import org.apache.jackrabbit.jcr2spi.operation.AddLabel;
import org.apache.jackrabbit.jcr2spi.operation.RemoveLabel;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.EventIterator;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.NodeId;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>VersionManagerImpl</code>...
 */
public class VersionManagerImpl implements VersionManager {

    private static Logger log = LoggerFactory.getLogger(VersionManagerImpl.class);

    private final UpdatableItemStateManager stateManager;

    public VersionManagerImpl(UpdatableItemStateManager stateManager) {
        this.stateManager = stateManager;
    }

    public void checkin(NodeState nodeState) throws RepositoryException {
        Operation ci = Checkin.create(nodeState);
        stateManager.execute(ci);
    }

    public void checkout(NodeState nodeState) throws RepositoryException {
        Operation co = Checkout.create(nodeState);
        stateManager.execute(co);
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
        try {
            /**
             * FIXME should not only rely on existence of jcr:isCheckedOut property
             * but also verify that node.isNodeType("mix:versionable")==true;
             * this would have a negative impact on performance though...
             */
            while (!nodeState.hasPropertyName(QName.JCR_ISCHECKEDOUT)) {
                NodeState parentState = nodeState.getParent();
                if (parentState == null) {
                    // reached root state without finding a jcr:isCheckedOut property
                    return true;
                }
                nodeState = parentState;
            }
            PropertyState propState = nodeState.getPropertyState(QName.JCR_ISCHECKEDOUT);
            Boolean b = Boolean.valueOf(propState.getValue().getString());
            return b.booleanValue();
        } catch (ItemStateException e) {
            // should not occur
            throw new RepositoryException(e);
        }
    }

    public void removeVersion(NodeState versionHistoryState, NodeState versionState) throws RepositoryException {
        Operation op = Remove.create(versionState);
        stateManager.execute(op);
    }

    public void addVersionLabel(NodeState versionHistoryState, NodeState versionState, QName qLabel, boolean moveLabel) throws RepositoryException {
        Operation op = AddLabel.create(versionHistoryState, versionState, qLabel, moveLabel);
        stateManager.execute(op);
    }

    public void removeVersionLabel(NodeState versionHistoryState, NodeState versionState, QName qLabel) throws RepositoryException {
        Operation op = RemoveLabel.create(versionHistoryState, versionState, qLabel);
        stateManager.execute(op);
    }

    public void restore(NodeId nodeId, NodeId versionId, boolean removeExisting) throws RepositoryException {
        Operation op = Restore.create(nodeId, versionId, removeExisting);
        stateManager.execute(op);
    }

    public void restore(NodeId[] versionIds, boolean removeExisting) throws RepositoryException {
        Operation op = Restore.create(versionIds, removeExisting);
        stateManager.execute(op);
    }

    public Collection merge(NodeState nodeState, String workspaceName, boolean bestEffort) throws RepositoryException {
        // TODO find better solution to build the mergeFailed-collection
        final List failedIds = new ArrayList();
        InternalEventListener mergeFailedCollector = new InternalEventListener() {
            public void onEvent(EventIterator events, boolean isLocal) {
                if (isLocal) {
                    while (events.hasNext()) {
                        Event ev = events.nextEvent();
                        if (ev.getType() == Event.PROPERTY_ADDED && QName.JCR_MERGEFAILED.equals(ev.getQPath().getNameElement().getName())) {
                            failedIds.add(ev.getParentId());
                        }
                    }
                }
            }
        };

        Operation op = Merge.create(nodeState, workspaceName, bestEffort, mergeFailedCollector);
        stateManager.execute(op);
        return failedIds;
    }

    public void resolveMergeConflict(NodeState nodeState, NodeState versionState, boolean done) throws RepositoryException {
        Operation op = ResolveMergeConflict.create(nodeState, versionState, done);
        stateManager.execute(op);
    }
}