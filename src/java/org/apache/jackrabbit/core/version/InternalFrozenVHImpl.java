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

import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.QName;

import javax.jcr.RepositoryException;
import javax.jcr.version.VersionException;

/**
 * Implements a <code>InternalFrozenVersionHistory</code>
 */
public class InternalFrozenVHImpl extends InternalFreezeImpl
        implements InternalFrozenVersionHistory {

    /**
     * the underlying persistence node
     */
    private NodeStateEx node;

    /**
     * Creates a new frozen version history.
     *
     * @param node
     */
    public InternalFrozenVHImpl(VersionManagerImpl vMgr, NodeStateEx node,
                                InternalVersionItem parent) {
        super(vMgr, parent);
        this.node = node;
    }


    /**
     * {@inheritDoc}
     */
    public QName getName() {
        return node.getName();
    }

    /**
     * {@inheritDoc}
     */
    public String getId() {
        return node.getUUID();
    }

    /**
     * {@inheritDoc}
     */
    public String getVersionHistoryId() {
        return (String) node.getPropertyValue(Constants.JCR_VERSIONHISTORY).internalValue();
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersionHistory getVersionHistory()
            throws VersionException {
        try {
            return getVersionManager().getVersionHistory(getVersionHistoryId());
        } catch (RepositoryException e) {
            throw new VersionException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getBaseVersionId() {
        return (String) node.getPropertyValue(Constants.JCR_BASEVERSION).internalValue();
    }

    /**
     * {@inheritDoc}
     */
    public InternalVersion getBaseVesion()
            throws VersionException {
        try {
            InternalVersionHistory history = getVersionManager().getVersionHistory(getVersionHistoryId());
            return history.getVersion(getBaseVersionId());
        } catch (RepositoryException e) {
            throw new VersionException(e);
        }
    }
}
