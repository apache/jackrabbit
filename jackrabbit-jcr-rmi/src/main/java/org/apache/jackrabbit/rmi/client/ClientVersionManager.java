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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.rmi.remote.RemoteVersionManager;

public class ClientVersionManager extends ClientObject
        implements VersionManager {

    /** The current session. */
    private Session session;

    private RemoteVersionManager remote;

    public ClientVersionManager(
            Session session, RemoteVersionManager remote,
            LocalAdapterFactory factory) {
        super(factory);
        this.session = session;
        this.remote = remote;
    }

    public void cancelMerge(String absPath, Version version)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCRRMI-26");
    }

    public Version checkin(String absPath) throws RepositoryException {
        try {
            return getFactory().getVersion(session, remote.checkin(absPath));
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    public void checkout(String absPath) throws RepositoryException {
        try {
            remote.checkout(absPath);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    public Version checkpoint(String absPath) throws RepositoryException {
        try {
            return getFactory().getVersion(session, remote.checkpoint(absPath));
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    public Node createActivity(String title)
            throws RepositoryException {
        try {
            return getFactory().getNode(session, remote.createActivity(title));
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    public Node createConfiguration(String absPath)
            throws RepositoryException {
        try {
            return getFactory().getNode(
                    session, remote.createConfiguration(absPath));
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    public void doneMerge(String absPath, Version version)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCRRMI-26");
    }

    public Node getActivity() throws RepositoryException {
        try {
            return getFactory().getNode(session, remote.getActivity());
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    public Version getBaseVersion(String absPath) throws RepositoryException {
        try {
            return getFactory().getVersion(
                    session, remote.getBaseVersion(absPath));
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    public VersionHistory getVersionHistory(String absPath)
            throws RepositoryException {
        try {
            return getFactory().getVersionHistory(
                    session, remote.getVersionHistory(absPath));
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    public boolean isCheckedOut(String absPath) throws RepositoryException {
        try {
            return remote.isCheckedOut(absPath);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    public NodeIterator merge(Node activityNode) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCRRMI-26");
    }

    public NodeIterator merge(
            String absPath, String srcWorkspace, boolean bestEffort)
            throws RepositoryException {
        try {
            return getFactory().getNodeIterator(
                    session, remote.merge(absPath, srcWorkspace, bestEffort));
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    public NodeIterator merge(
            String absPath, String srcWorkspace, boolean bestEffort,
            boolean isShallow) throws RepositoryException {
        try {
            return getFactory().getNodeIterator(session, remote.merge(
                    absPath, srcWorkspace, bestEffort, isShallow));
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    public void removeActivity(Node activityNode) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCRRMI-26");
    }

    public void restore(Version[] versions, boolean removeExisting)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCRRMI-26");
    }

    public void restore(Version version, boolean removeExisting)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCRRMI-26");
    }

    public void restore(
            String absPath, String versionName, boolean removeExisting)
            throws RepositoryException {
        try {
            remote.restore(absPath, versionName, removeExisting);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    public void restore(String absPath, Version version, boolean removeExisting)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCRRMI-26");
    }

    public void restoreByLabel(
            String absPath, String versionLabel, boolean removeExisting)
            throws RepositoryException {
        try {
            remote.restoreByLabel(absPath, versionLabel, removeExisting);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    public Node setActivity(Node activity) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCRRMI-26");
    }

}
