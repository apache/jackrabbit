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
package org.apache.jackrabbit.state.nodetype;

/**
 * Node type manager state. Instances of this class are used to hold
 * and manage the internal state of node type managers.
 */
public class NodeTypeManagerState {

    /** Available node type states. */
    private NodeTypeState[] nodeTypeStates;

    /**
     * Returns all available node type states.
     *
     * @return node type states
     */
    public NodeTypeState[] getNodeTypeStates() {
        return nodeTypeStates;
    }

    /**
     * Sets the node type manager state.
     *
     * @param nodeTypeStates node type states
     */
    public void setNodeTypeStates(NodeTypeState[] nodeTypeStates) {
        this.nodeTypeStates = nodeTypeStates;
    }

}
