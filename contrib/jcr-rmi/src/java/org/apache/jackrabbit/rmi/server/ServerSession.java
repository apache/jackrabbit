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
package org.apache.jackrabbit.rmi.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.AccessControlException;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.rmi.remote.RemoteItem;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.remote.RemoteWorkspace;

/**
 * Remote adapter for the JCR {@link javax.jcr.Session Session} interface.
 * This class makes a local session available as an RMI service using the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteSession RemoteSession}
 * interface.
 *
 * @author Jukka Zitting
 * @see javax.jcr.Session
 * @see org.apache.jackrabbit.rmi.remote.RemoteSession
 */
public class ServerSession extends ServerObject implements RemoteSession {

    /** The adapted local session. */
    private Session session;

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
    public String getUserId() throws RemoteException {
        return session.getUserId();
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
        return getFactory().getRemoteWorkspace(session.getWorkspace());
    }

    /** {@inheritDoc} */
    public void checkPermission(String path, String actions)
            throws AccessControlException, RemoteException {
        session.checkPermission(path, actions);
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
    public boolean itemExists(String path) throws RemoteException {
        return session.itemExists(path);
    }

    /** {@inheritDoc} */
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
    public boolean hasPendingChanges()
            throws RepositoryException, RemoteException {
        try {
            return session.hasPendingChanges();
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
    public void importXML(String path, byte[] xml)
            throws IOException, RepositoryException, RemoteException {
        try {
            session.importXML(path, new ByteArrayInputStream(xml));
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
    public byte[] exportDocView(
            String path, boolean binaryAsLink, boolean noRecurse)
            throws IOException, RepositoryException, RemoteException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            session.exportDocView(path, buffer, binaryAsLink, noRecurse);
            return buffer.toByteArray();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public byte[] exportSysView(
            String path, boolean binaryAsLink, boolean noRecurse)
            throws IOException, RepositoryException, RemoteException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            session.exportSysView(path, buffer, binaryAsLink, noRecurse);
            return buffer.toByteArray();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

}
