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
package org.apache.jackrabbit.rmi.server;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.rmi.remote.RemoteVersion;
import org.apache.jackrabbit.rmi.remote.RemoteVersionHistory;

/**
 * Remote adapter for the JCR {@link javax.jcr.version.VersionHistory VersionHistory}
 * interface. This class makes a local version history available as an RMI
 * service using the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteVersionHistory RemoteVersionHistory}
 * interface.
 *
 * @see javax.jcr.version.VersionHistory
 * @see org.apache.jackrabbit.rmi.remote.RemoteVersionHistory
 */
public class ServerVersionHistory extends ServerNode
        implements RemoteVersionHistory {

    /** The adapted local version history. */
    private VersionHistory versionHistory;

    /**
     * Creates a remote adapter for the given local version history.
     *
     * @param versionHistory local version history
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerVersionHistory(VersionHistory versionHistory,
            RemoteAdapterFactory factory) throws RemoteException {
        super(versionHistory, factory);
        this.versionHistory = versionHistory;
    }

    /** {@inheritDoc} */
    public String getVersionableIdentifier() throws RepositoryException,
    		RemoteException {
        try {
            return versionHistory.getVersionableIdentifier();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
    
    /** {@inheritDoc} */
    public RemoteVersion getRootVersion()
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteVersion(versionHistory.getRootVersion());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteIterator getAllLinearVersions() throws RepositoryException,
    		RemoteException {
        try {
            return getFactory().getRemoteVersionIterator(
                    versionHistory.getAllLinearVersions());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
    
    /** {@inheritDoc} */
    public RemoteIterator getAllVersions()
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteVersionIterator(
                    versionHistory.getAllVersions());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteIterator getAllLinearFrozenNodes() throws RepositoryException,
    		RemoteException {
        try {
            return getFactory().getRemoteNodeIterator(
                    versionHistory.getAllLinearFrozenNodes());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
    
    /** {@inheritDoc} */
    public RemoteIterator getAllFrozenNodes() throws RepositoryException,
    		RemoteException {
        try {
            return getFactory().getRemoteNodeIterator(
                    versionHistory.getAllFrozenNodes());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
    
    /** {@inheritDoc} */
    public RemoteVersion getVersion(String versionName)
        throws RepositoryException, RemoteException {
        try {
            Version version = versionHistory.getVersion(versionName);
            return getFactory().getRemoteVersion(version);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteVersion getVersionByLabel(String label)
            throws RepositoryException, RemoteException {
        try {
            Version version = versionHistory.getVersionByLabel(label);
            return getFactory().getRemoteVersion(version);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void addVersionLabel(String versionName, String label,
            boolean moveLabel) throws RepositoryException, RemoteException {
        try {
            versionHistory.addVersionLabel(versionName, label, moveLabel);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void removeVersionLabel(String label) throws RepositoryException,
        RemoteException {
        try {
            versionHistory.removeVersionLabel(label);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasVersionLabel(String label) throws RepositoryException, RemoteException {
        return versionHistory.hasVersionLabel(label);
    }

    /** {@inheritDoc} */
    public boolean hasVersionLabel(String versionUUID, String label)
        throws RepositoryException, RemoteException {
        try {
            Version version = getVersionByUUID(versionUUID);
            return versionHistory.hasVersionLabel(version, label);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String[] getVersionLabels() throws RepositoryException, RemoteException {
        return versionHistory.getVersionLabels();
    }

    /** {@inheritDoc} */
    public String[] getVersionLabels(String versionUUID)
            throws RepositoryException, RemoteException {
        try {
            Version version = getVersionByUUID(versionUUID);
            return versionHistory.getVersionLabels(version);
        } catch (ClassCastException cce) {
            // we do not expect this here as nodes should be returned correctly
            throw getRepositoryException(new RepositoryException(cce));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void removeVersion(String versionName)
            throws RepositoryException, RemoteException {
        try {
            versionHistory.removeVersion(versionName);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getVersionableUUID() throws RepositoryException, RemoteException {
        return versionHistory.getVersionableUUID();
    }
}
