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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.ItemLifeCycleListener;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.state.NodeState;

import javax.jcr.nodetype.NodeDefinition;

/**
 * This Class implements a version history that extends a node.
 */
public class VersionHistoryImpl extends AbstractVersionHistory {

    /**
     * the internal version history
     */
    private final InternalVersionHistory history;

    /**
     * creates a new version history node.
     *
     * @param itemMgr
     * @param session
     * @param id
     * @param state
     * @param definition
     * @param listeners
     * @param history
     */
    public VersionHistoryImpl(ItemManager itemMgr, SessionImpl session, NodeId id,
                              NodeState state, NodeDefinition definition,
                              ItemLifeCycleListener[] listeners,
                              InternalVersionHistory history) {
        super(itemMgr, session, id, state, definition, listeners);
        this.history = history;
    }

    /**
     * {@inheritDoc}
     */
    protected InternalVersionHistory getInternalVersionHistory() {
        return history;
    }
}
