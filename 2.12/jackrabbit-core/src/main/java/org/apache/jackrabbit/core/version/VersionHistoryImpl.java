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

import org.apache.jackrabbit.core.AbstractNodeData;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.commons.iterator.FrozenNodeIteratorAdapter;

import javax.jcr.version.VersionHistory;
import javax.jcr.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.AccessDeniedException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionIterator;

/**
 * Base implementation of the {@link javax.jcr.version.VersionHistory} interface.
 */
public class VersionHistoryImpl extends NodeImpl implements VersionHistory {

    /**
     * Logger instance.
     */
    private static Logger log = LoggerFactory.getLogger(VersionHistoryImpl.class);

    /**
     * Create a new instance of this class.
     * @param itemMgr item manager
     * @param sessionContext component context of the associated session
     * @param data node data
     */
    public VersionHistoryImpl(
            ItemManager itemMgr, SessionContext sessionContext,
            AbstractNodeData data) {
        super(itemMgr, sessionContext, data);
    }

    /**
     * Returns the internal version history. Subclass responsibility.
     *
     * @return internal version history
     * @throws RepositoryException if the internal version history is not available
     */
    protected InternalVersionHistory getInternalVersionHistory()
            throws RepositoryException {
        SessionImpl session = sessionContext.getSessionImpl();
        InternalVersionHistory history =
                session.getInternalVersionManager().getVersionHistory((NodeId) id);
        if (history == null) {
            throw new InvalidItemStateException(id + ": the item does not exist anymore");
        }
        return history;
    }

    /**
     * @see javax.jcr.version.VersionHistory#getRootVersion()
     */
    public javax.jcr.version.Version getRootVersion() throws RepositoryException {
        SessionImpl session = sessionContext.getSessionImpl();
        return (Version) session.getNodeById(
                getInternalVersionHistory().getRootVersion().getId());
    }

    /**
     * @see javax.jcr.version.VersionHistory#getAllVersions()
     */
    public VersionIterator getAllVersions() throws RepositoryException {
        return new VersionIteratorImpl(
                getSession(), getInternalVersionHistory().getRootVersion());
    }

    /**
     * @see VersionHistory#getAllFrozenNodes()
     */
    public NodeIterator getAllFrozenNodes() throws RepositoryException {
        return new FrozenNodeIteratorAdapter(getAllVersions());
    }

    /**
     * @see VersionHistory#getAllLinearVersions()
     */
    @SuppressWarnings("deprecation")
    public VersionIterator getAllLinearVersions() throws RepositoryException {
        // get base version. this can certainly be optimized
        SessionImpl session = sessionContext.getSessionImpl();
        InternalVersionHistory vh = getInternalVersionHistory();
        Node vn = session.getNodeById(vh.getVersionableId());
        InternalVersion base = ((VersionImpl) vn.getBaseVersion()).getInternalVersion();

        return new VersionIteratorImpl(getSession(), vh.getRootVersion(), base);
    }

    /**
     * @see VersionHistory#getAllLinearFrozenNodes()
     */
    public NodeIterator getAllLinearFrozenNodes() throws RepositoryException {
        return new FrozenNodeIteratorAdapter(getAllLinearVersions());
    }

    /**
     * @see javax.jcr.version.VersionHistory#getVersion(String)
     */
    public javax.jcr.version.Version getVersion(String versionName)
            throws VersionException, RepositoryException {
        try {
            Name name = sessionContext.getQName(versionName);
            InternalVersion v = getInternalVersionHistory().getVersion(name);
            if (v == null) {
                throw new VersionException("No version with name '" + versionName + "' exists in this version history.");
            }
            return (Version) sessionContext.getSessionImpl().getNodeById(v.getId());
        } catch (NameException e) {
            throw new VersionException(e);
        }
    }

    /**
     * @see javax.jcr.version.VersionHistory#getVersionByLabel(String)
     */
    public javax.jcr.version.Version getVersionByLabel(String label) throws RepositoryException {
        try {
            Name qLabel = sessionContext.getQName(label);
            InternalVersion v =
                getInternalVersionHistory().getVersionByLabel(qLabel);
            if (v == null) {
                throw new VersionException("No version with label '" + label + "' exists in this version history.");
            }
            return (Version) sessionContext.getSessionImpl().getNodeById(v.getId());
        } catch (NameException e) {
            throw new VersionException(e);
        }
    }

    /**
     * @see javax.jcr.version.VersionHistory#addVersionLabel(String, String, boolean)
     */
    public void addVersionLabel(String versionName, String label, boolean move)
            throws VersionException, RepositoryException {
        try {
            // check permissions
            checkVersionManagementPermission();
            sessionContext.getSessionImpl().getInternalVersionManager().setVersionLabel(
                    getSession(), getInternalVersionHistory(),
                    sessionContext.getQName(versionName),
                    sessionContext.getQName(label), move);
        } catch (NameException e) {
            throw new VersionException(e);
        }
    }

    /**
     * @see javax.jcr.version.VersionHistory#removeVersionLabel(String)
     */
    public void removeVersionLabel(String label) throws RepositoryException {
        try {
            // check permissions
            checkVersionManagementPermission();
            InternalVersion existing = sessionContext.getSessionImpl().getInternalVersionManager().setVersionLabel(
                    getSession(), getInternalVersionHistory(),
                    null, sessionContext.getQName(label), true);
            if (existing == null) {
                throw new VersionException("No version with label '" + label + "' exists in this version history.");
            }
        } catch (NameException e) {
            throw new VersionException(e);
        }
    }


    /**
     * @see javax.jcr.version.VersionHistory#getVersionLabels
     */
    public String[] getVersionLabels() throws RepositoryException {
        Name[] labels = getInternalVersionHistory().getVersionLabels();
        String[] ret = new String[labels.length];
        for (int i = 0; i < labels.length; i++) {
            ret[i] = sessionContext.getJCRName(labels[i]);
        }
        return ret;
    }

    /**
     * @see javax.jcr.version.VersionHistory#getVersionLabels(javax.jcr.version.Version)
     */
    public String[] getVersionLabels(javax.jcr.version.Version version)
            throws VersionException, RepositoryException {
        checkOwnVersion(version);
        Name[] labels = ((VersionImpl) version).getInternalVersion().getLabels();
        String[] ret = new String[labels.length];
        for (int i = 0; i < labels.length; i++) {
            ret[i] = sessionContext.getJCRName(labels[i]);
        }
        return ret;
    }

    /**
     * @see javax.jcr.version.VersionHistory#hasVersionLabel(String)
     */
    public boolean hasVersionLabel(String label) throws RepositoryException {
        try {
            Name qLabel = sessionContext.getQName(label);
            return getInternalVersionHistory().getVersionByLabel(qLabel) != null;
        } catch (NameException e) {
            throw new IllegalArgumentException("Unable to resolve label: " + e);
        }
    }

    /**
     * @see javax.jcr.version.VersionHistory#hasVersionLabel(javax.jcr.version.Version, String)
     */
    public boolean hasVersionLabel(javax.jcr.version.Version version, String label)
            throws VersionException, RepositoryException {
        checkOwnVersion(version);
        try {
            Name qLabel = sessionContext.getQName(label);
            return ((VersionImpl) version).getInternalVersion().hasLabel(qLabel);
        } catch (NameException e) {
            throw new VersionException(e);
        }
    }

    /**
     * @see javax.jcr.version.VersionHistory#removeVersion(String)
     */
    public void removeVersion(String versionName)
            throws UnsupportedRepositoryOperationException, VersionException,
            RepositoryException {
        try {
            // check permissions
            checkVersionManagementPermission();
            sessionContext.getSessionImpl().getInternalVersionManager().removeVersion(
                    getSession(),
                    getInternalVersionHistory(),
                    sessionContext.getQName(versionName));
        } catch (NameException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Removes this VersionHistory from storage.
     *
     * @throws RepositoryException if an error occurs.
     */
    public void removeVersionHistory() throws RepositoryException {
        checkVersionManagementPermission();
        InternalVersionManager internalVersionManager =
                sessionContext.getSessionImpl().getInternalVersionManager();
        internalVersionManager.removeVersionHistory(
                getSession(),
                getInternalVersionHistory());
    }

    /**
     * @see javax.jcr.Item#isSame(javax.jcr.Item)
     */
    @Override
    public boolean isSame(Item otherItem) {
        if (otherItem instanceof VersionHistoryImpl) {
            // since all version histories live in the same workspace, we can compare the uuids
            try {
                InternalVersionHistory other = ((VersionHistoryImpl) otherItem).getInternalVersionHistory();
                return other.getId().equals(getInternalVersionHistory().getId());
            } catch (RepositoryException e) {
                log.warn("Unable to retrieve internal version history objects: " + e.getMessage());
                log.debug("Stack dump:", e);
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String getVersionableUUID() throws RepositoryException {
        return getVersionableIdentifier();
    }

    /**
     * {@inheritDoc}
     */
    public String getVersionableIdentifier() throws RepositoryException {
        return getInternalVersionHistory().getVersionableId().toString();
    }

    /**
     * Checks if the current session has version management permission
     *
     * @throws AccessDeniedException if version management is not allowed
     * @throws RepositoryException if an error occurs
     */
    private void checkVersionManagementPermission() throws RepositoryException {
        try {
            sessionContext.getAccessManager().checkPermission(getPrimaryPath(), Permission.VERSION_MNGMT);
        } catch (ItemNotFoundException e) {
            // ignore.
        }
    }

    /**
     * Checks if the given version belongs to this history
     *
     * @param version the version
     * @throws javax.jcr.version.VersionException if the specified version is
     *         not part of this version history
     * @throws javax.jcr.RepositoryException if a repository error occurs
     */
    private void checkOwnVersion(Version version)
            throws VersionException, RepositoryException {
        if (!version.getParent().isSame(this)) {
            throw new VersionException("Specified version not contained in this history.");
        }
    }

    //--------------------------------------< Overwrite "protected" methods >---

    /**
     * Always throws a {@link javax.jcr.nodetype.ConstraintViolationException} since this node
     * is protected.
     *
     * @throws javax.jcr.nodetype.ConstraintViolationException
     */
    @Override
    public void update(String srcWorkspaceName) throws ConstraintViolationException {
        String msg = "update operation not allowed: " + this;
        log.debug(msg);
        throw new ConstraintViolationException(msg);
    }

    /**
     * Always throws a {@link javax.jcr.nodetype.ConstraintViolationException} since this node
     * is protected.
     *
     * @throws javax.jcr.nodetype.ConstraintViolationException
     */
    @Override
    public NodeIterator merge(String srcWorkspace, boolean bestEffort)
            throws ConstraintViolationException {
        String msg = "merge operation not allowed: " + this;
        log.debug(msg);
        throw new ConstraintViolationException(msg);
    }

    //--------------------------------------------------------------< Object >

    /**
     * Return a string representation of this version history node
     * for diagnostic purposes.
     *
     * @return "version history node /path/to/item"
     */
    public String toString() {
        return "version history " + super.toString();
    }

}
