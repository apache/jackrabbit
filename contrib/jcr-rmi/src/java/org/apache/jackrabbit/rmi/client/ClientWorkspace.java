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
package org.apache.jackrabbit.rmi.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;
import org.apache.jackrabbit.rmi.xml.WorkspaceImportContentHandler;
import org.xml.sax.ContentHandler;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteWorkspace RemoteWorkspace}
 * inteface. This class makes a remote workspace locally available using
 * the JCR {@link javax.jcr.Workspace Workspace} interface.
 *
 * @author Jukka Zitting
 * @author Philipp Koch
 * @see javax.jcr.Workspace
 * @see org.apache.jackrabbit.rmi.remote.RemoteWorkspace
 */
public class ClientWorkspace extends ClientObject implements Workspace {

    /** The current session. */
    private Session session;

    /** The adapted remote workspace. */
    private RemoteWorkspace remote;

    /**
     * Creates a client adapter for the given remote workspace.
     *
     * @param session current session
     * @param remote remote workspace
     * @param factory local adapter factory
     */
    public ClientWorkspace(Session session, RemoteWorkspace remote,
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
    public void copy(String from, String to) throws
            ConstraintViolationException, AccessDeniedException,
            PathNotFoundException, ItemExistsException, RepositoryException {
        try {
            remote.copy(from, to);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void copy(String from, String to, String workspace) throws
            NoSuchWorkspaceException, ConstraintViolationException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            RepositoryException {
        try {
            remote.copy(from, to, workspace);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void move(String from, String to) throws
            ConstraintViolationException, AccessDeniedException,
            PathNotFoundException, ItemExistsException, RepositoryException {
        try {
            remote.move(from, to);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public QueryManager getQueryManager() throws RepositoryException {
        try {
            return factory.getQueryManager(session, remote.getQueryManager());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        try {
            return factory.getNamespaceRegistry(remote.getNamespaceRegistry());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        try {
            return factory.getNodeTypeManager(remote.getNodeTypeManager());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public ObservationManager getObservationManager()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public void clone(String workspace, String src, String dst,
            boolean removeExisting) throws NoSuchWorkspaceException,
            ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException,
            LockException, RepositoryException {
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
    public ContentHandler getImportContentHandler(String path,
            int uuidBehaviour) throws PathNotFoundException,
            ConstraintViolationException, VersionException, LockException,
            RepositoryException {
        return new WorkspaceImportContentHandler(this, path, uuidBehaviour);
    }

    /** {@inheritDoc} */
    public void importXML(String path, InputStream xml, int uuidBehaviour)
            throws IOException, PathNotFoundException, ItemExistsException,
            ConstraintViolationException, InvalidSerializedDataException,
            LockException, RepositoryException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] bytes = new byte[4096];
            for (int n = xml.read(bytes); n != -1; n = xml.read(bytes)) {
                buffer.write(bytes, 0, n);
            }
            remote.importXML(path, buffer.toByteArray(), uuidBehaviour);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void restore(Version[] versions, boolean removeExisting) throws
            ItemExistsException, UnsupportedRepositoryOperationException,
            VersionException, LockException, InvalidItemStateException,
            RepositoryException {
        // TODO Auto-generated method stub
    }

}
