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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.rmi.remote.RemoteItem;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteProperty;
import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;
import org.apache.jackrabbit.rmi.remote.security.RemoteAccessControlManager;

/**
 * Remote adapter for the JCR {@link javax.jcr.Session Session} interface.
 * This class makes a local session available as an RMI service using the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteSession RemoteSession}
 * interface.
 *
 * @see javax.jcr.Session
 * @see org.apache.jackrabbit.rmi.remote.RemoteSession
 */
public class ServerSession extends ServerObject implements RemoteSession {

    /** The adapted local session. */
    private Session session;

    /**
     * The server workspace for this session. This field is assigned on demand
     * by the first call to {@link #getWorkspace()}. The assumption is that
     * there is only one workspace instance per session and that each call to
     * the <code>Session.getWorkspace()</code> method of a single session will
     * allways return the same object.
     */
    private RemoteWorkspace remoteWorkspace;

    /**
     * Creates a remote adapter for the given local session.
     *
     * @param session local session
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerSession(Session session, RemoteAdapterFactory factory)
            throws RemoteException {
        super(factory);
        this.session = session;
    }

    /** {@inheritDoc} */
    public String getUserID() throws RemoteException {
        return session.getUserID();
    }

    /** {@inheritDoc} */
    public Object getAttribute(String name) throws RemoteException {
        return session.getAttribute(name);
    }

    /** {@inheritDoc} */
    public String[] getAttributeNames() throws RemoteException {
        return session.getAttributeNames();
    }

    /** {@inheritDoc} */
    public RemoteSession impersonate(Credentials credentials)
            throws RepositoryException, RemoteException {
        try {
            Session newSession = session.impersonate(credentials);
            return getFactory().getRemoteSession(newSession);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteWorkspace getWorkspace() throws RemoteException {
        if (remoteWorkspace == null) {
            remoteWorkspace =
                getFactory().getRemoteWorkspace(session.getWorkspace());
        }

        return remoteWorkspace;
    }

    /** {@inheritDoc} */
    public boolean hasPermission(String path, String actions)
            throws RepositoryException, RemoteException {
        return session.hasPermission(path, actions);
    }

    /** {@inheritDoc} */
    public String getNamespacePrefix(String uri)
             throws RepositoryException, RemoteException {
        try {
            return session.getNamespacePrefix(uri);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String[] getNamespacePrefixes()
            throws RepositoryException, RemoteException {
        try {
            return session.getNamespacePrefixes();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getNamespaceURI(String prefix)
            throws RepositoryException, RemoteException {
        try {
            return session.getNamespaceURI(prefix);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void setNamespacePrefix(String prefix, String uri)
            throws RepositoryException, RemoteException {
        try {
            session.setNamespacePrefix(prefix, uri);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean itemExists(String path) throws RepositoryException, RemoteException {
        return session.itemExists(path);
    }

    /** {@inheritDoc} */
    public boolean nodeExists(String path) throws RepositoryException, RemoteException {
        return session.nodeExists(path);
    }

    /** {@inheritDoc} */
    public boolean propertyExists(String path) throws RepositoryException, RemoteException {
        return session.propertyExists(path);
    }

    /** {@inheritDoc} */
    public RemoteNode getNodeByIdentifier(String id)
            throws RepositoryException, RemoteException {
        try {
            return getRemoteNode(session.getNodeByIdentifier(id));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("deprecation")
    public RemoteNode getNodeByUUID(String uuid)
            throws RepositoryException, RemoteException {
        try {
            return getRemoteNode(session.getNodeByUUID(uuid));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteNode getRootNode()
            throws RepositoryException, RemoteException {
        try {
            return getRemoteNode(session.getRootNode());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteItem getItem(String path)
            throws RepositoryException, RemoteException {
        try {
            return getRemoteItem(session.getItem(path));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteNode getNode(String path)
            throws RepositoryException, RemoteException {
        try {
            return getRemoteNode(session.getNode(path));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteProperty getProperty(String path)
            throws RepositoryException, RemoteException {
        try {
            return (RemoteProperty) getRemoteItem(session.getProperty(path));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasPendingChanges()
            throws RepositoryException, RemoteException {
        try {
            return session.hasPendingChanges();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void removeItem(String path)
            throws RepositoryException, RemoteException {
        try {
            session.removeItem(path);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void move(String from, String to)
            throws RepositoryException, RemoteException {
        try {
            session.move(from, to);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void save() throws RepositoryException, RemoteException {
        try {
            session.save();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void refresh(boolean keepChanges)
            throws RepositoryException, RemoteException {
        try {
            session.refresh(keepChanges);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void logout() throws RemoteException {
        session.logout();
    }

    /** {@inheritDoc} */
    public boolean isLive() throws RemoteException {
        return session.isLive();
    }

    /** {@inheritDoc} */
    public void importXML(String path, byte[] xml, int mode)
            throws IOException, RepositoryException, RemoteException {
        try {
            session.importXML(path, new ByteArrayInputStream(xml), mode);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void addLockToken(String token) throws RemoteException {
        session.addLockToken(token);
    }

    /** {@inheritDoc} */
    public String[] getLockTokens() throws RemoteException {
        return session.getLockTokens();
    }

    /** {@inheritDoc} */
    public void removeLockToken(String token) throws RemoteException {
        session.removeLockToken(token);
    }

    /** {@inheritDoc} */
    public byte[] exportDocumentView(
            String path, boolean binaryAsLink, boolean noRecurse)
            throws IOException, RepositoryException, RemoteException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            session.exportDocumentView(path, buffer, binaryAsLink, noRecurse);
            return buffer.toByteArray();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public byte[] exportSystemView(
            String path, boolean binaryAsLink, boolean noRecurse)
            throws IOException, RepositoryException, RemoteException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            session.exportSystemView(path, buffer, binaryAsLink, noRecurse);
            return buffer.toByteArray();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteAccessControlManager getAccessControlManager()
            throws UnsupportedRepositoryOperationException,
            RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteAccessControlManager(
                session.getAccessControlManager());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
}
