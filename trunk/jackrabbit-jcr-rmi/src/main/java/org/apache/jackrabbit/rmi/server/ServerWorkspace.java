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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.rmi.RemoteException;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.rmi.remote.RemoteLockManager;
import org.apache.jackrabbit.rmi.remote.RemoteNamespaceRegistry;
import org.apache.jackrabbit.rmi.remote.RemoteNodeTypeManager;
import org.apache.jackrabbit.rmi.remote.RemoteObservationManager;
import org.apache.jackrabbit.rmi.remote.RemoteQueryManager;
import org.apache.jackrabbit.rmi.remote.RemoteVersionManager;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;

/**
 * Remote adapter for the JCR {@link Workspace Workspace} interface.
 * This class makes a local workspace available as an RMI service using the
 * {@link RemoteWorkspace RemoteWorkspace} interface.
 *
 * @see Workspace
 * @see RemoteWorkspace
 */
public class ServerWorkspace extends ServerObject implements RemoteWorkspace {

    /** The adapted local workspace. */
    private Workspace workspace;

    /**
     * The remote observation manager for this workspace. This field is assigned
     * on demand by the first call to {@link #getObservationManager()}. The
     * assumption is that there is only one observation manager instance per
     * workspace and that each call to the
     * <code>Workspace.getObservationManager()</code> method of a single
     * workspace will allways return the same object.
     */
    private RemoteObservationManager remoteObservationManager;

    private RemoteLockManager remoteLockManager;

    private RemoteVersionManager remoteVersionManager;

    /**
     * Creates a remote adapter for the given local workspace.
     *
     * @param workspace local workspace
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerWorkspace(Workspace workspace, RemoteAdapterFactory factory)
            throws RemoteException {
        super(factory);
        this.workspace = workspace;
    }

    /** {@inheritDoc} */
    public String getName() throws RemoteException {
        return workspace.getName();
    }

    /** {@inheritDoc} */
    public void copy(String from, String to)
            throws RepositoryException, RemoteException {
        try {
            workspace.copy(from, to);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void copy(String workspace, String from, String to)
            throws RepositoryException, RemoteException {
        try {
            this.workspace.copy(workspace, from, to);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void clone(
            String workspace, String from, String to, boolean removeExisting)
            throws RepositoryException, RemoteException {
        try {
            this.workspace.clone(workspace, from, to, removeExisting);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void move(String from, String to)
            throws RepositoryException, RemoteException {
        try {
            workspace.move(from, to);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteNodeTypeManager getNodeTypeManager()
            throws RepositoryException, RemoteException {
        try {
            NodeTypeManager manager = workspace.getNodeTypeManager();
            return getFactory().getRemoteNodeTypeManager(manager);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteNamespaceRegistry getNamespaceRegistry()
            throws RepositoryException, RemoteException {
        try {
            NamespaceRegistry registry = workspace.getNamespaceRegistry();
            return getFactory().getRemoteNamespaceRegistry(registry);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteQueryManager getQueryManager()
            throws RepositoryException, RemoteException {
        try {
            QueryManager queryManager = workspace.getQueryManager();
            return getFactory().getRemoteQueryManager(
                    workspace.getSession(), queryManager);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteObservationManager getObservationManager()
            throws RepositoryException, RemoteException {
        try {
            if (remoteObservationManager == null) {
                ObservationManager observationManager =
                    workspace.getObservationManager();
                remoteObservationManager =
                    getFactory().getRemoteObservationManager(observationManager);
            }
            return remoteObservationManager;
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String[] getAccessibleWorkspaceNames()
            throws RepositoryException, RemoteException {
        try {
            return workspace.getAccessibleWorkspaceNames();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void importXML(String path, byte[] xml, int uuidBehaviour)
            throws IOException, RepositoryException, RemoteException {
        try {
            workspace.importXML(
                    path, new ByteArrayInputStream(xml), uuidBehaviour);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    public void createWorkspace(String name, String source)
            throws RepositoryException, RemoteException {
        if (source != null) {
            workspace.createWorkspace(name, source);
        } else {
            workspace.createWorkspace(name);
        }
    }

    public void deleteWorkspace(String name)
            throws RepositoryException, RemoteException {
        workspace.deleteWorkspace(name);
    }

    /** {@inheritDoc} */
    public RemoteLockManager getLockManager()
            throws RepositoryException, RemoteException {
        try {
            if (remoteLockManager == null) {
                LockManager lockManager = workspace.getLockManager();
                remoteLockManager =
                    getFactory().getRemoteLockManager(lockManager);
            }
            return remoteLockManager;
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    public RemoteVersionManager getVersionManager()
            throws RepositoryException, RemoteException {
        try {
            if (remoteVersionManager == null) {
                VersionManager versionManager = workspace.getVersionManager();
                remoteVersionManager =
                    getFactory().getRemoteVersionManager(workspace.getSession(), versionManager);
            }
            return remoteVersionManager;
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

}
