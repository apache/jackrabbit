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
package org.apache.jackrabbit.rmi.client;

import java.rmi.RemoteException;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.apache.jackrabbit.rmi.remote.RemoteVersionHistory;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteVersionHistory RemoteVersionHistory}
 * interface. This class makes a remote version history locally available using
 * the JCR {@link javax.jcr.version.VersionHistory VersionHistory} interface.
 *
 * @see javax.jcr.version.VersionHistory
 * @see org.apache.jackrabbit.rmi.remote.RemoteVersionHistory
 */
public class ClientVersionHistory extends ClientNode implements VersionHistory {

    /** The adapted remote version history. */
    private RemoteVersionHistory remote;

    /**
     * Creates a local adapter for the given remote version history.
     *
     * @param session current session
     * @param remote  remote version history
     * @param factory local adapter factory
     */
    public ClientVersionHistory(Session session, RemoteVersionHistory remote,
        LocalAdapterFactory factory) {
        super(session, remote, factory);
        this.remote = remote;
    }

    /** {@inheritDoc} */
    public Version getRootVersion() throws RepositoryException {
        try {
            return getFactory().getVersion(getSession(), remote.getRootVersion());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public VersionIterator getAllVersions() throws RepositoryException {
        try {
            return getFactory().getVersionIterator(
                    getSession(), remote.getAllVersions());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Version getVersion(String versionName) throws VersionException,
        RepositoryException {
        try {
            return getFactory().getVersion(getSession(), remote.getVersion(versionName));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Version getVersionByLabel(String label) throws RepositoryException {
        try {
            return getFactory().getVersion(getSession(), remote.getVersionByLabel(label));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void addVersionLabel(String versionName, String label,
            boolean moveLabel) throws VersionException, RepositoryException {
        try {
            remote.addVersionLabel(versionName, label, moveLabel);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void removeVersionLabel(String label)
            throws VersionException, RepositoryException {
        try {
            remote.removeVersionLabel(label);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasVersionLabel(String label) throws RepositoryException {
        try {
            return remote.hasVersionLabel(label);
        } catch (RemoteException ex) {
            // grok the exception and assume label is missing
            return false;
        }
    }

    /** {@inheritDoc} */
    public boolean hasVersionLabel(Version version, String label)
            throws VersionException, RepositoryException {
        try {
            String versionIdentifier = version.getIdentifier();
            return remote.hasVersionLabel(versionIdentifier, label);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String[] getVersionLabels() throws RepositoryException {
        try {
            return remote.getVersionLabels();
        } catch (RemoteException ex) {
            // grok the exception and return an empty array
            return new String[0];
        }
    }

    /** {@inheritDoc} */
    public String[] getVersionLabels(Version version)
            throws VersionException, RepositoryException {
        try {
            String versionIdentifier = version.getIdentifier();
            return remote.getVersionLabels(versionIdentifier);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void removeVersion(String versionName)
            throws UnsupportedRepositoryOperationException, VersionException,
            RepositoryException {
        try {
            remote.removeVersion(versionName);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc}
     * @deprecated As of JCR 2.0, {@link #getVersionableIdentifier} should be
     *             used instead.
     */
    public String getVersionableUUID() throws RepositoryException {
        try {
            return remote.getVersionableUUID();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeIterator getAllFrozenNodes() throws RepositoryException {
        try {
            return getFactory().getNodeIterator(getSession(), remote.getAllFrozenNodes());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeIterator getAllLinearFrozenNodes() throws RepositoryException {
        try {
            return getFactory().getNodeIterator(getSession(), remote.getAllLinearFrozenNodes());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public VersionIterator getAllLinearVersions() throws RepositoryException {
        try {
            return getFactory().getVersionIterator(getSession(), remote.getAllLinearVersions());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getVersionableIdentifier() throws RepositoryException {
        try {
            return remote.getVersionableIdentifier();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }
}
