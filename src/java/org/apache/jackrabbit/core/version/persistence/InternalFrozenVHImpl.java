/*
 * Copyright 2004 The Apache Software Foundation.
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

import org.apache.jackrabbit.core.version.*;
import org.apache.jackrabbit.core.QName;

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
     * @see org.apache.jackrabbit.core.version.InternalFrozenVersionHistory#getVersionHistoryId()
     */
    public String getVersionHistoryId() {
        return (String) node.getPropertyValue(VersionManager.PROPNAME_VERSION_HISTORY).internalValue();
    }

    /**
     * @see org.apache.jackrabbit.core.version.InternalFrozenVersionHistory#getVersionHistory()
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
     * @see org.apache.jackrabbit.core.version.InternalFrozenVersionHistory#getBaseVersionId()
     */
    public String getBaseVersionId() {
        return (String) node.getPropertyValue(VersionManager.PROPNAME_BASE_VERSION).internalValue();
    }

    /**
     * @see org.apache.jackrabbit.core.version.InternalFrozenVersionHistory#getBaseVesion()
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
