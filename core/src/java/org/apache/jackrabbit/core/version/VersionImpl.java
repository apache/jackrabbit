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
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.state.NodeState;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import java.util.Calendar;

/**
 * This Class implements a Version that extends the node interface
 */
public class VersionImpl extends NodeImpl implements Version {

    /**
     * the internal version
     */
    protected final InternalVersion version;

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
     * {@inheritDoc}
     */
    public Calendar getCreated() throws RepositoryException {
        return version.getCreated();
    }

    /**
     * {@inheritDoc}
     */
    public Version[] getSuccessors() throws RepositoryException {
        // need to wrap it around proper node
        InternalVersion[] suc = version.getSuccessors();
        Version[] ret = new Version[suc.length];
        for (int i = 0; i < suc.length; i++) {
            ret[i] = (Version) session.getNodeByUUID(suc[i].getId());
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    public Version[] getPredecessors() throws RepositoryException {
        // need to wrap it around proper node
        InternalVersion[] pred = version.getPredecessors();
        Version[] ret = new Version[pred.length];
        for (int i = 0; i < pred.length; i++) {
            ret[i] = (Version) session.getNodeByUUID(pred[i].getId());
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        return version.getId();
    }

    /**
     * {@inheritDoc}
     */
    public VersionHistory getContainingHistory() throws RepositoryException {
        return (VersionHistory) getParent();
    }

    /**
     * Returns the internal version
     *
     * @return
     */
    public InternalVersion getInternalVersion() {
        return version;
    }

    /**
     * Returns the frozen node of this version
     *
     * @return
     * @throws RepositoryException
     */
    public InternalFrozenNode getFrozenNode() throws RepositoryException {
        return version.getFrozenNode();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSame(Item otherItem) {
        if (otherItem instanceof VersionImpl) {
            // since all versions live in the same workspace, we can compare the uuids
            return ((VersionImpl) otherItem).version.getId().equals(version.getId());
        } else {
            return false;
        }
    }
}
