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

import org.apache.jackrabbit.core.*;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

/**
 * This Class implements a version history that extends a node.
 */
public class VersionHistoryImpl extends NodeWrapper implements VersionHistory {

    /**
     * the internal version history
     */
    private final InternalVersionHistory history;

    /**
     * creates a new version history implementation for the given session and
     * internal version history
     *
     * @param session
     * @param history
     * @throws RepositoryException
     */
    protected VersionHistoryImpl(Session session, InternalVersionHistory history)
            throws RepositoryException {
        super((NodeImpl) session.getNodeByUUID(history.getUUID()));
        this.history = history;
    }

    /**
     * @see VersionHistory#getRootVersion()
     */
    public Version getRootVersion() throws RepositoryException {
        return new VersionImpl(unwrap().getSession(), history.getRootVersion());
    }

    /**
     * @see VersionHistory#getAllVersions()
     */
    public VersionIterator getAllVersions() throws RepositoryException {
        return new VersionIteratorImpl(unwrap().getSession(), history.getRootVersion());
    }

    /**
     * @see VersionHistory#getVersion(String)
     */
    public Version getVersion(String versionName) throws RepositoryException {
        try {
            QName name = QName.fromJCRName(versionName, ((SessionImpl) unwrap().getSession()).getNamespaceResolver());
            InternalVersion v = history.getVersion(name);
            return v == null ? null : new VersionImpl(unwrap().getSession(), v);
        } catch (IllegalNameException e) {
            throw new RepositoryException(e);
        } catch (UnknownPrefixException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see VersionHistory#getVersionByLabel(String)
     */
    public Version getVersionByLabel(String label) throws RepositoryException {
        InternalVersion v = history.getVersionByLabel(label);
        return v == null ? null : new VersionImpl(unwrap().getSession(), v);
    }

    /**
     * @see VersionHistory#addVersionLabel(Version, String, boolean)
     */
    public void addVersionLabel(Version version, String label, boolean move) throws RepositoryException {
        history.addVersionLabel(((VersionImpl) version).version, label, move);
    }

    /**
     * @see VersionHistory#removeVersionLabel(String)
     */
    public void removeVersionLabel(String label) throws RepositoryException {
        history.removeVersionLabel(label);
    }

    /**
     * @see javax.jcr.Node#getUUID()
     */
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        return history.getUUID();
    }

    /**
     * @see javax.jcr.Item#isSame(javax.jcr.Item)
     */
    public boolean isSame(Item otherItem) {
        if (otherItem instanceof VersionHistoryImpl) {
            // since all version histories live in the same workspace, we can compare the uuids
            return ((VersionHistoryImpl) otherItem).history.getUUID().equals(history.getUUID());
        } else {
            return false;
        }
    }
}
