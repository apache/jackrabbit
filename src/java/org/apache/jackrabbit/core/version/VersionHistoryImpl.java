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

import org.apache.jackrabbit.core.IllegalNameException;
import org.apache.jackrabbit.core.ItemLifeCycleListener;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.NoPrefixDeclaredException;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.UnknownPrefixException;
import org.apache.jackrabbit.core.state.NodeState;

import javax.jcr.Item;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

/**
 * This Class implements a version history that extends a node.
 */
public class VersionHistoryImpl extends NodeImpl implements VersionHistory {

    /**
     * the internal version history
     */
    private final InternalVersionHistory history;

    /**
     * creates a new version history node.
     *
     * @param itemMgr
     * @param session
     * @param id
     * @param state
     * @param definition
     * @param listeners
     * @param history
     * @throws RepositoryException
     */
    public VersionHistoryImpl(ItemManager itemMgr, SessionImpl session, NodeId id,
                              NodeState state, NodeDefinition definition,
                              ItemLifeCycleListener[] listeners,
                              InternalVersionHistory history) throws RepositoryException {
        super(itemMgr, session, id, state, definition, listeners);
        this.history = history;
    }

    /**
     * @see VersionHistory#getRootVersion()
     */
    public Version getRootVersion() throws RepositoryException {
        return (Version) session.getNodeByUUID(history.getRootVersion().getId());
    }

    /**
     * @see VersionHistory#getAllVersions()
     */
    public VersionIterator getAllVersions() throws RepositoryException {
        return new VersionIteratorImpl(session, history.getRootVersion());
    }

    /**
     * @see VersionHistory#getVersion(String)
     */
    public Version getVersion(String versionName)
            throws VersionException, RepositoryException {
        try {
            QName name = QName.fromJCRName(versionName, session.getNamespaceResolver());
            InternalVersion v = history.getVersion(name);
            if (v == null) {
                throw new VersionException("No version with name '" + versionName + "' exists in this version history.");
            }
            return (Version) session.getNodeByUUID(v.getId());
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
        try {
            QName qLabel = QName.fromJCRName(label, session.getNamespaceResolver());
            InternalVersion v = history.getVersionByLabel(qLabel);
            if (v == null) {
                throw new VersionException("No version with label '" + label + "' exists in this version history.");
            }
            return (Version) session.getNodeByUUID(v.getId());
        } catch (IllegalNameException e) {
            throw new RepositoryException(e);
        } catch (UnknownPrefixException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see VersionHistory#addVersionLabel(String, String, boolean)
     */
    public void addVersionLabel(String versionName, String label, boolean move)
            throws VersionException, RepositoryException {
        try {
            session.getVersionManager().setVersionLabel(this,
                    QName.fromJCRName(versionName, session.getNamespaceResolver()),
                    QName.fromJCRName(label, session.getNamespaceResolver()),
                    move);
        } catch (IllegalNameException e) {
            throw new RepositoryException(e);
        } catch (UnknownPrefixException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see VersionHistory#removeVersionLabel(String)
     */
    public void removeVersionLabel(String label) throws RepositoryException {
        try {
            Version existing = session.getVersionManager().setVersionLabel(this,
                    null,
                    QName.fromJCRName(label, session.getNamespaceResolver()),
                    true);
            if (existing == null) {
                throw new VersionException("No version with label '" + label + "' exists in this version history.");
            }
        } catch (IllegalNameException e) {
            throw new RepositoryException(e);
        } catch (UnknownPrefixException e) {
            throw new RepositoryException(e);
        }
    }


    /**
     * @see VersionHistory#getVersionLabels
     */
    public String[] getVersionLabels() {
        try {
            QName[] labels = history.getVersionLabels();
            String[] ret = new String[labels.length];
            for (int i = 0; i < labels.length; i++) {
                ret[i] = labels[i].toJCRName(session.getNamespaceResolver());
            }
            return ret;
        } catch (NoPrefixDeclaredException e) {
            throw new IllegalArgumentException("Unable to resolve label name: " + e.toString());
        }
    }

    /**
     * @see VersionHistory#getVersionLabels(Version)
     */
    public String[] getVersionLabels(Version version)
            throws VersionException, RepositoryException {
        checkOwnVersion(version);
        try {
            QName[] labels = ((VersionImpl) version).getInternalVersion().getLabels();
            String[] ret = new String[labels.length];
            for (int i = 0; i < labels.length; i++) {
                ret[i] = labels[i].toJCRName(session.getNamespaceResolver());
            }
            return ret;
        } catch (NoPrefixDeclaredException e) {
            throw new IllegalArgumentException("Unable to resolve label name: " + e.toString());
        }
    }

    /**
     * @see VersionHistory#hasVersionLabel(String)
     */
    public boolean hasVersionLabel(String label) {
        try {
            QName qLabel = QName.fromJCRName(label, session.getNamespaceResolver());
            return history.getVersionByLabel(qLabel) != null;
        } catch (IllegalNameException e) {
            throw new IllegalArgumentException("Unable to resolve label: " + e);
        } catch (UnknownPrefixException e) {
            throw new IllegalArgumentException("Unable to resolve label: " + e);
        }
    }

    /**
     * @see VersionHistory#hasVersionLabel(Version, String)
     */
    public boolean hasVersionLabel(Version version, String label)
            throws VersionException, RepositoryException {
        checkOwnVersion(version);
        try {
            QName qLabel = QName.fromJCRName(label, session.getNamespaceResolver());
            return ((VersionImpl) version).getInternalVersion().hasLabel(qLabel);
        } catch (IllegalNameException e) {
            throw new RepositoryException(e);
        } catch (UnknownPrefixException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see VersionHistory#removeVersion(String)
     */
    public void removeVersion(String versionName)
            throws UnsupportedRepositoryOperationException, VersionException,
            RepositoryException {
        try {
            session.getVersionManager().removeVersion(this,
                    QName.fromJCRName(versionName, session.getNamespaceResolver()));
        } catch (IllegalNameException e) {
            throw new RepositoryException(e);
        } catch (UnknownPrefixException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * @see javax.jcr.Node#getUUID()
     */
    public String getUUID()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return history.getId();
    }

    /**
     * @see javax.jcr.Item#isSame(javax.jcr.Item)
     */
    public boolean isSame(Item otherItem) {
        if (otherItem instanceof VersionHistoryImpl) {
            // since all version histories live in the same workspace, we can compare the uuids
            return ((VersionHistoryImpl) otherItem).history.getId().equals(history.getId());
        } else {
            return false;
        }
    }

    /**
     * Returns the UUID of the node that was versioned.
     *
     * @return
     */
    public String getVersionableUUID() throws RepositoryException {
        return history.getVersionableUUID();
    }

    /**
     * Checks if the given version belongs to this history
     *
     * @param version
     * @throws VersionException
     * @throws RepositoryException
     */
    private void checkOwnVersion(Version version)
            throws VersionException, RepositoryException {
        if (!version.getParent().isSame(this)) {
            throw new VersionException("Specified version not contained in this history.");
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * In addition to the normal behaviour, this method also filters out the
     * references that do not exist in this workspace.
     */
    public PropertyIterator getReferences() throws RepositoryException {
        return getReferences(true);
    }


    /**
     * Returns the internal version history
     *
     * @return
     */
    public InternalVersionHistory getInternalVersionHistory() {
        return history;
    }
}
