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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.ItemLifeCycleListener;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.state.NodeState;

import javax.jcr.nodetype.NodeDefinition;

/**
 * This Class implements a Version that extends the node interface
 */
public class VersionImpl extends AbstractVersion {

    /**
     * the internal version
     */
    private final InternalVersion version;

    /**
     * creates a new version node
     *
     * @param itemMgr
     * @param session
     * @param id
     * @param state
     * @param definition
     * @param listeners
     * @param version
     */
    public VersionImpl(ItemManager itemMgr, SessionImpl session, NodeId id,
                       NodeState state, NodeDefinition definition,
                       ItemLifeCycleListener[] listeners,
                       InternalVersion version) {
        super(itemMgr, session, id, state, definition, listeners);
        this.version = version;
    }

    /**
     * Returns the internal version
     *
     * @return the internal version
     */
    protected InternalVersion getInternalVersion() {
        return version;
    }
}
