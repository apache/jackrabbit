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
package org.apache.jackrabbit.core.version.persistence;

import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.version.InternalFrozenVersionHistory;
import org.apache.jackrabbit.core.version.InternalVersion;
import org.apache.jackrabbit.core.version.InternalVersionHistory;
import org.apache.jackrabbit.core.version.InternalVersionItem;
import org.apache.jackrabbit.core.version.PersistentVersionManager;

import javax.jcr.RepositoryException;
import javax.jcr.version.VersionException;

/**
 *
 */
class InternalFrozenVHImpl extends InternalFreezeImpl implements InternalFrozenVersionHistory {

    /**
     * the underlaying persistence node
     */
    private PersistentNode node;

    private final String id;

    /**
     * Creates a new frozen version history.
     *
     * @param node
     */
    protected InternalFrozenVHImpl(PersistentVersionManager vMgr, PersistentNode node, String id, InternalVersionItem parent) {
        super(vMgr, parent);
        this.node = node;
        this.id = id;
    }

    /**
     * Returns the name of this frozen version history
     *
     * @return
     */
    public QName getName() {
        return node.getName();
    }

    protected String getPersistentId() {
        return node.getUUID();
    }

    public String getId() {
        return id;
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
            return getVersionManager().getVersion(getVersionHistoryId(), getBaseVersionId());
        } catch (RepositoryException e) {
            throw new VersionException(e);
        }
    }
}
