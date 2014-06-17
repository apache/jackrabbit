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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.core.id.NodeId;

/**
 * Extends the <code>ItemStateListener</code> allowing a client to be
 * additionally informed about changes on a <code>NodeState</code>.
 */
public interface NodeStateListener extends ItemStateListener {

    /**
     * Called when a child node has been added
     *
     * @param state node state that changed
     * @param name  name of node that was added
     * @param index index of new node
     * @param id    id of new node
     */
    void nodeAdded(NodeState state,
                   Name name, int index, NodeId id);

    /**
     * Called when a node has been modified, typically as a result of removal
     * or addition of a child node.
     * <p>
     * Please note, that this method is not called if
     * {@link #stateModified(ItemState)} was called.
     *
     * @param state node state that changed
     */
    void nodeModified(NodeState state);

    /**
     * Called when the children nodes were replaced by other nodes, typically
     * as result of a reorder operation.
     *
     * @param state node state that changed
     */
    void nodesReplaced(NodeState state);

    /**
     * Called when a child node has been removed
     *
     * @param state node state that changed
     * @param name  name of node that was removed
     * @param index index of removed node
     * @param id    id of removed node
     */
    void nodeRemoved(NodeState state, Name name, int index, NodeId id);
}
