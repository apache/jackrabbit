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

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import javax.jcr.RepositoryException;
import javax.jcr.version.VersionException;

/**
 * Implements a <code>InternalFrozenVersionHistory</code>
 */
class InternalFrozenVHImpl extends InternalFreezeImpl
        implements InternalFrozenVersionHistory {

    /**
     * Creates a new frozen version history.
     *
     * @param node
     */
    public InternalFrozenVHImpl(InternalVersionManagerBase vMgr, NodeStateEx node,
                                InternalVersionItem parent) {
        super(vMgr, node, parent);
    }


    /**
     * {@inheritDoc}
     */
    public Name getName() {
        return node.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeId getId() {
        return node.getNodeId();
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getVersionHistoryId() {
        return node.getPropertyValue(NameConstants.JCR_CHILDVERSIONHISTORY).getNodeId();
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersionHistory getVersionHistory()
            throws VersionException {
        try {
            return vMgr.getVersionHistory(getVersionHistoryId());
        } catch (RepositoryException e) {
            throw new VersionException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getBaseVersionId() {
        return node.getPropertyValue(NameConstants.JCR_BASEVERSION).getNodeId();
    }

    /**
     * @deprecate Use {@link #getBaseVersion()} instead.
     */
    public InternalVersion getBaseVesion()
            throws VersionException {
        return getBaseVersion();
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion getBaseVersion()
            throws VersionException {
        try {
            InternalVersionHistory history = vMgr.getVersionHistory(getVersionHistoryId());
            return history.getVersion(getBaseVersionId());
        } catch (RepositoryException e) {
            throw new VersionException(e);
        }
    }
}
