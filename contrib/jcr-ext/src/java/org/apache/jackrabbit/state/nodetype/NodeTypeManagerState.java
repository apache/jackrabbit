/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.HashSet;
import java.util.Set;

/**
 * Node type manager state. Instances of this class are used to hold
 * and manage the internal state of node type managers.
 */
public class NodeTypeManagerState {

    /** Available node type states. */
    private final Set types;

    /** Creates an empty node type manager state instance. */
    public NodeTypeManagerState() {
        types = new HashSet();
    }

    /**
     * Returns all available node type states.
     *
     * @return node type states
     */
    public NodeTypeState[] getNodeTypeStates() {
        return (NodeTypeState[]) types.toArray(new NodeTypeState[types.size()]);
    }

    /**
     * Adds a node type to the node type manager state.
     *
     * @param state node type state
     */
    public void addNodeTypeState(NodeTypeState state) {
        types.add(state);
    }

}
