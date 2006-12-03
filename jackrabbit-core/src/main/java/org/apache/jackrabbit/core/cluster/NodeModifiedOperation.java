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
package org.apache.jackrabbit.core.cluster;

import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ChangeLog;

/**
 * Describes a journal operation for a node modification.
 */
public class NodeModifiedOperation extends NodeOperation {

    /**
     * Creates a new instance of this class.
     */
    NodeModifiedOperation() {
        super(ItemOperation.MODIFIED);
    }

    /**
     * Create a node operation for a modified node. Only modified/modifiable members must be remembered.
     *
     * @param state node state
     * @return node operation
     */
    public static NodeOperation create(NodeState state) {
        NodeOperation operation = new NodeModifiedOperation();
        operation.setId(state.getNodeId());
        //todo set other members
        return operation;
    }

    /**
     * {@inheritDoc}
     */
    public void apply(ChangeLog changeLog) {
        NodeState state = new NodeState(getId(), null, null, NodeState.STATUS_NEW, false);
        state.setStatus(NodeState.STATUS_EXISTING_MODIFIED);
        changeLog.modified(state);
    }
}