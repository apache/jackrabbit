/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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

import javax.jcr.RepositoryException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.nodetype.NodeDefinition;

/**
 * Implementation of a {@link javax.jcr.version.VersionHistory} that works in an
 * XA environment.
 */
public class XAVersionHistory extends AbstractVersionHistory {

    /**
     * Internal version history. Gets fetched again from the version manager if
     * needed.
     */
    private InternalVersionHistory history;

    /**
     * XA Version manager.
     */
    private final XAVersionManager vMgr;

    /**
     * Create a new instance of this class.
     * @param itemMgr item manager
     * @param session session
     * @param id node id
     * @param state node state
     * @param definition node definition
     * @param listeners life cycle listeners
     * @param history internal version history
     */
    public XAVersionHistory(ItemManager itemMgr, SessionImpl session, NodeId id,
                            NodeState state, NodeDefinition definition,
                            ItemLifeCycleListener[] listeners,
                            InternalVersionHistory history) {
        super(itemMgr, session, id, state, definition, listeners);

        this.history = history;
        this.vMgr = (XAVersionManager) session.getVersionManager();
    }

    /**
     * {@inheritDoc}
     */
    protected InternalVersionHistory getInternalVersionHistory()
            throws RepositoryException {

        ensureUpToDate();
        sanityCheck();
        return history;
    }

    /**
     * {@inheritDoc}
     */
    protected void sanityCheck() throws RepositoryException {
        super.sanityCheck();

        if (history == null) {
            throw new InvalidItemStateException(id + ": the item does not exist anymore");
        }
    }

    /**
     * Ensure the internal version is up-to-date.
     */
    private synchronized void ensureUpToDate() throws RepositoryException {
        if (history != null) {
            if (vMgr.differentXAEnv(((InternalVersionHistoryImpl) history))) {
                history = vMgr.getVersionHistory(history.getId());
            }
        }
    }
}
