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
package org.apache.jackrabbit.rmi.remote;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.AccessControlException;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;

/**
 * Remote version of the JCR {@link javax.jcr.Session Session} interface.
 * Used by the
 * {@link org.apache.jackrabbit.rmi.server.ServerSession ServerSession}
 * and
 * {@link org.apache.jackrabbit.rmi.client.ClientSession ClientSession}
 * adapters to provide transparent RMI access to remote sessions.
 * <p>
 * Most of the methods in this interface are documented only with a reference
 * to a corresponding Session method. In these cases the remote object
 * will simply forward the method call to the underlying Session instance.
 * Complex return values like workspaces and other objects are returned
 * as remote references to the corresponding remote interface. Simple
 * return values and possible exceptions are simply copied over the network
 * to the client. RMI errors are signalled with RemoteExceptions.
 *
 * @author Jukka Zitting
 * @see javax.jcr.Session
 * @see org.apache.jackrabbit.rmi.client.ClientSession
 * @see org.apache.jackrabbit.rmi.server.ServerSession
 */
public interface RemoteSession extends Remote {

    /**
     * @see javax.jcr.Session#getUserId()
     * @throws RemoteException on RMI errors
     */
    String getUserId() throws RemoteException;

    /**
     * Returns the named attribute. Note that only serializable
     * attribute values can be transmitted over the network and that
     * the client should have (or be able to fetch) the object class
     * to access the returned value. Failures to meet these conditions
     * are signalled with RemoteExceptions.
     *
     * @see javax.jcr.Session#getAttribute(java.lang.String)
     * @throws RemoteException on RMI errors
     */
    Object getAttribute(String name) throws RemoteException;

    /**
     * @see javax.jcr.Session#getAttributeNames()
     * @throws RemoteException on RMI errors
     */
    String[] getAttributeNames() throws RemoteException;

    /**
     * @see javax.jcr.Session#getWorkspace()
     * @throws RemoteException on RMI errors
     */
    RemoteWorkspace getWorkspace() throws RemoteException;

    /**
     * @see javax.jcr.Session#impersonate(javax.jcr.Credentials)
     * @throws RemoteException on RMI errors
     */
    RemoteSession impersonate(Credentials credentials)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Session#getNodeByUUID(java.lang.String)
     * @throws RemoteException on RMI errors
     */
    RemoteNode getNodeByUUID(String uuid)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Session#getItem(java.lang.String)
     * @throws RemoteException on RMI errors
     */
    RemoteItem getItem(String path) throws RepositoryException, RemoteException;
    
    /**
     * @see javax.jcr.Session#itemExists(java.lang.String)
     * @throws RemoteException on RMI errors
     */
    boolean itemExists(String path) throws RemoteException;

    /**
     * @see javax.jcr.Session#move(java.lang.String, java.lang.String)
     * @throws RemoteException on RMI errors
     */
    void move(String from, String to)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Session#save()
     * @throws RemoteException on RMI errors
     */
    void save() throws RepositoryException, RemoteException;
    
    /**
     * @see javax.jcr.Session#refresh(boolean)
     * @throws RemoteException on RMI errors
     */
    void refresh(boolean keepChanges)
            throws RepositoryException, RemoteException;
    
    /**
     * @see javax.jcr.Session#logout()
     * @throws RemoteException on RMI errors
     */
    void logout() throws RemoteException;

    /**
     * @see javax.jcr.Session#getRootNode()
     * @throws RemoteException on RMI errors
     */
    RemoteNode getRootNode() throws RepositoryException, RemoteException;
    
    /**
     * @see javax.jcr.Session#hasPendingChanges()
     * @throws RemoteException on RMI errors
     */
    boolean hasPendingChanges() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Session#checkPermission(java.lang.String, java.lang.String)
     * @throws RemoteException on RMI errors
     */
    void checkPermission(String path, String actions)
            throws AccessControlException, RemoteException;

    /**
     * Imports the system or document view XML data into a subtree of
     * the identified node. Note that the entire XML stream is transferred
     * as a single byte array over the network. This may cause problems with
     * large XML streams. The remote server will wrap the XML data into
     * a {@link java.io.ByteArrayInputStream ByteArrayInputStream} and feed
     * it to the normal importXML method.
     * 
     * @see javax.jcr.Session#importXML(java.lang.String, java.io.InputStream)
     * @throws RemoteException on RMI errors
     */
    void importXML(String path, byte[] xml)
            throws IOException, RepositoryException, RemoteException;
    
    /**
     * @see javax.jcr.Session#setNamespacePrefix(java.lang.String, java.lang.String)
     * @throws RemoteException on RMI errors
     */
    void setNamespacePrefix(String prefix, String uri)
            throws RepositoryException, RemoteException;
    
    /**
     * @see javax.jcr.Session#getNamespacePrefixes()
     * @throws RemoteException on RMI errors
     */
    String[] getNamespacePrefixes() throws RepositoryException, RemoteException;
    
    /**
     * @see javax.jcr.Session#getNamespaceURI(java.lang.String)
     * @throws RemoteException on RMI errors
     */
    String getNamespaceURI(String prefix)
            throws RepositoryException, RemoteException;
    
    /**
     * @see javax.jcr.Session#getNamespacePrefix(java.lang.String)
     * @throws RemoteException on RMI errors
     */
    String getNamespacePrefix(String uri)
            throws RepositoryException, RemoteException;
    
    /**
     * @see javax.jcr.Session#addLockToken(java.lang.String)
     * @throws RemoteException on RMI errors
     */
    void addLockToken(String name) throws RemoteException;
    
    /**
     * @see javax.jcr.Session#getLockTokens()
     * @throws RemoteException on RMI errors
     */
    String[] getLockTokens() throws RemoteException;
    
    /**
     * @see javax.jcr.Session#removeLockToken(java.lang.String)
     * @throws RemoteException on RMI errors
     */
    void removeLockToken(String name) throws RemoteException;

    /**
     * Exports the identified repository subtree as a system view XML
     * stream. Note that the entire XML stream is transferred as a
     * single byte array over the network. This may cause problems with
     * large exports. The remote server uses a
     * {@link java.io.ByteArrayOutputStream ByteArrayOutputStream} to capture
     * the XML data written by the normal exportSysView method.
     *
     * @see javax.jcr.Workspace#exportSysView(java.lang.String, java.io.OutputStream, boolean, boolean)
     * @throws RemoteException on RMI errors
     */
    byte[] exportSysView(String path, boolean binaryAsLink, boolean noRecurse)
            throws IOException, RepositoryException, RemoteException;

    /**
     * Exports the identified repository subtree as a document view XML
     * stream. Note that the entire XML stream is transferred as a
     * single byte array over the network. This may cause problems with
     * large exports. The remote server uses a
     * {@link java.io.ByteArrayOutputStream ByteArrayOutputStream} to capture
     * the XML data written by the normal exportDocView method.
     *
     * @see javax.jcr.Workspace#exportDocView(java.lang.String, java.io.OutputStream, boolean, boolean)
     * @throws RemoteException on RMI errors
     */
    byte[] exportDocView(String path, boolean binaryAsLink, boolean noRecurse)
        throws IOException, RepositoryException, RemoteException;

}
