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

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;

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
    protected Session session;
    
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
            throws LoginException, RepositoryException, RemoteException {
        return factory.getRemoteSession(session.impersonate(credentials));
    }

    /** {@inheritDoc} */
    public RemoteWorkspace getWorkspace() throws RemoteException {
        return factory.getRemoteWorkspace(session.getWorkspace());
    }

    /** {@inheritDoc} */
    public void checkPermission(String path, String actions)
            throws AccessControlException, RemoteException {
        session.checkPermission(path, actions);
    }
    
    /** {@inheritDoc} */
    public String getNamespacePrefix(String uri) throws NamespaceException,
            RepositoryException, RemoteException {
        return session.getNamespacePrefix(uri);
    }
    
    /** {@inheritDoc} */
    public String[] getNamespacePrefixes() throws RepositoryException,
            RemoteException {
        return session.getNamespacePrefixes();
    }
    
    /** {@inheritDoc} */
    public String getNamespaceURI(String prefix) throws NamespaceException,
            RepositoryException, RemoteException {
        return session.getNamespaceURI(prefix);
    }

    /** {@inheritDoc} */
    public void setNamespacePrefix(String prefix, String uri)
            throws NamespaceException, RepositoryException, RemoteException {
        session.setNamespacePrefix(prefix, uri);
    }

    /** {@inheritDoc} */
    public boolean itemExists(String path) throws RemoteException {
        return session.itemExists(path);
    }
    
    /** {@inheritDoc} */
    public RemoteNode getNodeByUUID(String uuid) throws ItemNotFoundException,
            RepositoryException, RemoteException {
        return factory.getRemoteNode(session.getNodeByUUID(uuid));
    }

    /** {@inheritDoc} */
    public RemoteNode getRootNode() throws RepositoryException,
            RemoteException {
        return factory.getRemoteNode(session.getRootNode());
    }

    /** {@inheritDoc} */
    public RemoteItem getItem(String path) throws PathNotFoundException,
            RepositoryException, RemoteException {
        return getRemoteItem(session.getItem(path));
    }
    
    /** {@inheritDoc} */
    public boolean hasPendingChanges() throws RepositoryException,
            RemoteException {
        return session.hasPendingChanges();
    }
    
    /** {@inheritDoc} */
    public void move(String from, String to) throws ItemExistsException,
            PathNotFoundException, ConstraintViolationException,
            RepositoryException, RemoteException {
        session.move(from, to);
    }
    
    /** {@inheritDoc} */
    public void save() throws AccessDeniedException, LockException,
            ConstraintViolationException, InvalidItemStateException,
            RepositoryException, RemoteException {
        session.save();
    }
    
    /** {@inheritDoc} */
    public void refresh(boolean keepChanges) throws RepositoryException,
            RemoteException {
        session.refresh(keepChanges);
    }
    
    /** {@inheritDoc} */
    public void logout() throws RemoteException {
        session.logout();
    }

    /** {@inheritDoc} */
    public void importXML(String path, byte[] xml) throws IOException,
            PathNotFoundException, ItemExistsException,
            ConstraintViolationException, InvalidSerializedDataException,
            RepositoryException, RemoteException {
        session.importXML(path, new ByteArrayInputStream(xml));
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
    public byte[] exportDocView(String path, boolean binaryAsLink,
            boolean noRecurse) throws InvalidSerializedDataException,
            PathNotFoundException, IOException, RepositoryException,
            RemoteException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        session.exportDocView(path, buffer, binaryAsLink, noRecurse);
        return buffer.toByteArray();
    }
    
    /** {@inheritDoc} */
    public byte[] exportSysView(String path, boolean binaryAsLink,
            boolean noRecurse) throws PathNotFoundException, IOException,
            RepositoryException, RemoteException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        session.exportSysView(path, buffer, binaryAsLink, noRecurse);
        return buffer.toByteArray();
    }
}
