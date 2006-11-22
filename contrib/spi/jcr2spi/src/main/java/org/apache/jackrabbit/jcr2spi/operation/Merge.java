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

import org.apache.jackrabbit.jcr2spi.observation.InternalEventListener;
import org.apache.jackrabbit.jcr2spi.state.NodeState;

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

    private final NodeState nodeState;
    private final String srcWorkspaceName;
    private final boolean bestEffort;
    private final InternalEventListener eventListener;

    private Merge(NodeState nodeState, String srcWorkspaceName, boolean bestEffort, InternalEventListener eventListener) {
        this.nodeState = nodeState;
        this.srcWorkspaceName = srcWorkspaceName;
        this.bestEffort = bestEffort;
        this.eventListener = eventListener;

        this.addAffectedItemState(nodeState);
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
     * @see Operation#persisted()
     */
    public void persisted() {
        nodeState.invalidate(true);
        // TODO: invalidate the corresponding part of the version storage
    }

    //----------------------------------------< Access Operation Parameters >---
    public NodeState getNodeState() {
        return nodeState;
    }

    public String getSourceWorkspaceName() {
        return srcWorkspaceName;
    }

    public boolean bestEffort() {
        return bestEffort;
    }

    public InternalEventListener getEventListener() {
        return eventListener;
    }

    //------------------------------------------------------------< Factory >---
    /**
     *
     * @param nodeState
     * @param srcWorkspaceName
     * @return
     */
    public static Operation create(NodeState nodeState, String srcWorkspaceName, boolean bestEffort, InternalEventListener eventListener) {
        return new Merge(nodeState, srcWorkspaceName, bestEffort, eventListener);
    }
}