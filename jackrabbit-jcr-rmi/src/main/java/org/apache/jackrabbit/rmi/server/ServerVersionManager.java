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
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteVersion;
import org.apache.jackrabbit.rmi.remote.RemoteVersionHistory;
import org.apache.jackrabbit.rmi.remote.RemoteVersionManager;

public class ServerVersionManager extends ServerObject
        implements RemoteVersionManager {

    private final VersionManager manager;

    public ServerVersionManager(
            VersionManager manager, RemoteAdapterFactory factory)
            throws RemoteException {
        super(factory);
        this.manager = manager;
    }

    public RemoteVersion checkin(String absPath)
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteVersion(manager.checkin(absPath));
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public void checkout(String absPath) throws RepositoryException {
        try {
            manager.checkout(absPath);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public RemoteVersion checkpoint(String absPath)
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteVersion(manager.checkpoint(absPath));
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public RemoteNode createActivity(String title)
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteNode(manager.createActivity(title));
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public RemoteNode createConfiguration(String absPath)
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteNode(
                    manager.createConfiguration(absPath));
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public RemoteNode getActivity()
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteNode(manager.getActivity());
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public RemoteVersion getBaseVersion(String absPath)
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteVersion(
                    manager.getBaseVersion(absPath));
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public RemoteVersionHistory getVersionHistory(String absPath)
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteVersionHistory(
                    manager.getVersionHistory(absPath));
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public boolean isCheckedOut(String absPath)
            throws RepositoryException {
        try {
            return manager.isCheckedOut(absPath);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public RemoteIterator merge(
            String absPath, String srcWorkspace, boolean bestEffort)
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteNodeIterator(
                    manager.merge(absPath, srcWorkspace, bestEffort));
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public RemoteIterator merge(
            String absPath, String srcWorkspace, boolean bestEffort,
            boolean isShallow) throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteNodeIterator(
                    manager.merge(absPath, srcWorkspace, bestEffort, isShallow));
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public void restore(
            String absPath, String versionName, boolean removeExisting)
            throws RepositoryException {
        try {
            manager.restore(absPath, versionName, removeExisting);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public void restoreByLabel(
            String absPath, String versionLabel, boolean removeExisting)
            throws RepositoryException {
        try {
            manager.restoreByLabel(absPath, versionLabel, removeExisting);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

}
