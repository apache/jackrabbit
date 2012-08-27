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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.rmi.remote.RemoteNamespaceRegistry;
import org.apache.jackrabbit.rmi.remote.RemoteNodeTypeManager;
import org.apache.jackrabbit.rmi.remote.RemoteQueryManager;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Local adapter for the JCR-RMI {@link RemoteWorkspace RemoteWorkspace}
 * interface. This class makes a remote workspace locally available using
 * the JCR {@link Workspace Workspace} interface.
 *
 * @see javax.jcr.Workspace
 * @see org.apache.jackrabbit.rmi.remote.RemoteWorkspace
 */
public class ClientWorkspace extends ClientObject implements Workspace {

    /** The current session. */
    private Session session;

    /** The adapted remote workspace. */
    private RemoteWorkspace remote;

    /**
     * The adapted observation manager of this workspace. This field is set on
     * the first call to the {@link #getObservationManager()()} method assuming,
     * that the observation manager instance is not changing during the lifetime
     * of a workspace instance, that is, each call to the server-side
     * <code>Workspace.getObservationManager()</code> allways returns the same
     * object.
     */
    private ObservationManager observationManager;

    private LockManager lockManager;

    private VersionManager versionManager;

    /**
     * Creates a client adapter for the given remote workspace.
     *
     * @param session current session
     * @param remote remote workspace
     * @param factory local adapter factory
     */
    public ClientWorkspace(
            Session session, RemoteWorkspace remote,
            LocalAdapterFactory factory) {
        super(factory);
        this.session = session;
        this.remote = remote;
    }

    /**
     * Returns the current session without contacting the remote workspace.
     *
     * {@inheritDoc}
     */
    public Session getSession() {
        return session;
    }

    /** {@inheritDoc} */
    public String getName() {
        try {
            return remote.getName();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public void copy(String from, String to) throws RepositoryException {
        try {
            remote.copy(from, to);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void copy(String workspace, String from, String to)
            throws RepositoryException {
        try {
            remote.copy(workspace, from, to);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void move(String from, String to) throws RepositoryException {
        try {
            remote.move(from, to);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public QueryManager getQueryManager() throws RepositoryException {
        try {
            RemoteQueryManager manager = remote.getQueryManager();
            return getFactory().getQueryManager(session, manager);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        try {
            RemoteNamespaceRegistry registry = remote.getNamespaceRegistry();
            return getFactory().getNamespaceRegistry(registry);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        try {
            RemoteNodeTypeManager manager = remote.getNodeTypeManager();
            return getFactory().getNodeTypeManager(manager);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public ObservationManager getObservationManager()
            throws RepositoryException {
        if (observationManager == null) {
            try {
                observationManager =
                    getFactory().
                        getObservationManager(this, remote.getObservationManager());
            } catch (RemoteException ex) {
                throw new RemoteRepositoryException(ex);
            }
        }

        return observationManager;
    }

    /** {@inheritDoc} */
    public void clone(
            String workspace, String src, String dst, boolean removeExisting)
            throws RepositoryException {
        try {
            remote.clone(workspace, src, dst, removeExisting);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        try {
            return remote.getAccessibleWorkspaceNames();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public ContentHandler getImportContentHandler(
            final String path, final int mode) throws RepositoryException {
        getSession().getItem(path); // Check that the path exists
        try {
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            return new DefaultContentHandler(
                    SerializingContentHandler.getSerializer(buffer)) {
                public void endDocument() throws SAXException {
                    super.endDocument();
                    try {
                        remote.importXML(path, buffer.toByteArray(), mode);
                    } catch (Exception e) {
                        throw new SAXException("XML import failed", e);
                    }
                }
            };
        } catch (SAXException e) {
            throw new RepositoryException("XML serialization failed", e);
        }
    }

    /** {@inheritDoc} */
    public void importXML(String path, InputStream xml, int uuidBehaviour)
            throws IOException, RepositoryException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] bytes = new byte[4096];
            for (int n = xml.read(bytes); n != -1; n = xml.read(bytes)) {
                buffer.write(bytes, 0, n);
            }
            remote.importXML(path, buffer.toByteArray(), uuidBehaviour);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        } finally {
            // JCR-2903
            try { xml.close(); } catch (IOException ignore) {}
        }
    }

    /** {@inheritDoc} */
    public void restore(Version[] versions, boolean removeExisting)
            throws RepositoryException {
        getVersionManager().restore(versions, removeExisting);
    }

    public void createWorkspace(String name) throws RepositoryException {
        try {
            remote.createWorkspace(name, null);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public void createWorkspace(String name, String srcWorkspace)
            throws RepositoryException {
        try {
            remote.createWorkspace(name, srcWorkspace);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public void deleteWorkspace(String name) throws RepositoryException {
        try {
            remote.deleteWorkspace(name);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public LockManager getLockManager() throws RepositoryException {
        if (lockManager == null) {
            try {
                lockManager = getFactory().getLockManager(
                        session, remote.getLockManager());
            } catch (RemoteException ex) {
                throw new RemoteRepositoryException(ex);
            }
        }

        return lockManager;
    }

    public VersionManager getVersionManager() throws RepositoryException {
        if (versionManager == null) {
            try {
                versionManager = getFactory().getVersionManager(
                        session, remote.getVersionManager());
            } catch (RemoteException ex) {
                throw new RemoteRepositoryException(ex);
            }
        }

        return versionManager;
    }

}
