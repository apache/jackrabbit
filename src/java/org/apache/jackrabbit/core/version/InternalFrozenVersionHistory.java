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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.QName;

import javax.jcr.RepositoryException;

/**
 * This Class represents a frozen versionable child node, that was created
 * during a {@link javax.jcr.Node#checkin()} with a OPV==Version node.
 */
public class InternalFrozenVersionHistory extends InternalFreeze {

    /**
     * the underlaying persistence node
     */
    private PersistentNode node;

    /**
     * Creates a new frozen version history.
     *
     * @param node
     */
    protected InternalFrozenVersionHistory(InternalFreeze parent, PersistentNode node) {
        super(parent);
        this.node = node;
    }

    /**
     * Returns the name of this frozen version history
     *
     * @return
     */
    public QName getName() {
        return node.getName();
    }

    /**
     * Returns the version history that was versioned with this node.
     *
     * @return
     * @throws RepositoryException
     */
    public String getVersionHistoryId()
            throws RepositoryException {
        return (String) node.getPropertyValue(VersionManager.PROPNAME_VERSION_HISTORY).internalValue();
    }

    /**
     * Returns the version history that was versioned with this node.
     *
     * @return
     * @throws RepositoryException
     */
    public InternalVersionHistory getVersionHistory()
            throws RepositoryException {
        return getVersionManager().getVersionHistory(getVersionHistoryId());
    }

    public String getBaseVersionId() throws RepositoryException {
        return (String) node.getPropertyValue(VersionManager.PROPNAME_BASE_VERSION).internalValue();
    }

    public InternalVersion getBaseVesion()
            throws RepositoryException {
        return getVersionManager().getVersion(getVersionHistoryId(), getBaseVersionId());
    }
}
