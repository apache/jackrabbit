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
package org.apache.jackrabbit.rmi.remote;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.rmi.remote.security.RemoteAccessControlManager;

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
 * to the client. RMI errors are signaled with RemoteExceptions.
 *
 * @see javax.jcr.Session
 * @see org.apache.jackrabbit.rmi.client.ClientSession
 * @see org.apache.jackrabbit.rmi.server.ServerSession
 */
public interface RemoteSession extends Remote {

    /**
     * Remote version of the
     * {@link javax.jcr.Session#getUserID() Session.getUserID()} method.
     *
     * @return user id
     * @throws RemoteException on RMI errors
     * @see javax.jcr.Session#getUserID()
     */
    String getUserID() throws RemoteException;

    /**
     * Returns the named attribute. Note that only serializable
     * attribute values can be transmitted over the network and that
     * the client should have (or be able to fetch) the object class
     * to access the returned value. Failures to meet these conditions
     * are signalled with RemoteExceptions.
     *
     * @param name attribute name
     * @return attribute value
     * @throws RemoteException on RMI errors
     * @see javax.jcr.Session#getAttribute(java.lang.String)
     */
    Object getAttribute(String name) throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#getAttributeNames() Session.getAttributeNames()}
     * method.
     *
     * @return attribute names
     * @throws RemoteException on RMI errors
     */
    String[] getAttributeNames() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#getWorkspace() Session.getWorkspace()} method.
     *
     * @return workspace
     * @see javax.jcr.Session#getWorkspace()
     * @throws RemoteException on RMI errors
     */
    RemoteWorkspace getWorkspace() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#impersonate(Credentials) Session.impersonate(Credentials)}
     * method.
     *
     * @param credentials credentials for the new session
     * @return new session
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteSession impersonate(Credentials credentials)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#getNodeByIdentifier(String) Session.getNodeByIdentifier(String)}
     * method.
     *
     * @param id node identifier
     * @return node
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteNode getNodeByIdentifier(String id)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#getNodeByUUID(String) Session.getNodeByUUID(String)}
     * method.
     *
     * @param uuid node uuid
     * @return node
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteNode getNodeByUUID(String uuid)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#getItem(String) Session.getItem(String)}
     * method.
     *
     * @param path item path
     * @return item
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteItem getItem(String path) throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#getNode(String) Session.getNode(String)}
     * method.
     *
     * @param path node path
     * @return node
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteNode getNode(String path) throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#getProperty(String) Session.getProperty(String)}
     * method.
     *
     * @param path property path
     * @return property
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteProperty getProperty(String path) throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#itemExists(String) Session.itemExists(String)}
     * method.
     *
     * @param path item path
     * @return <code>true</code> if the item exists,
     *         <code>false</code> otherwise
     * @throws RepositoryException on repository exception
     * @throws RemoteException on RMI errors
     */
    boolean itemExists(String path) throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#nodeExists(String) Session.nodeExists(String)}
     * method.
     *
     * @param path node path
     * @return <code>true</code> if the node exists,
     *         <code>false</code> otherwise
     * @throws RepositoryException on repository exception
     * @throws RemoteException on RMI errors
     */
    boolean nodeExists(String path) throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#propertyExists(String) Session.propertyExists(String)}
     * method.
     *
     * @param path property path
     * @return <code>true</code> if the property exists,
     *         <code>false</code> otherwise
     * @throws RepositoryException on repository exception
     * @throws RemoteException on RMI errors
     */
    boolean propertyExists(String path) throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#removeItem(String) Session.removeItem(String)}
     * method.
     *
     * @param path item path
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void removeItem(String path) throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#move(String,String) Session.move(String,String)}
     * method.
     *
     * @param from source path
     * @param to destination path
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void move(String from, String to)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#save() Session.save()} method.
     *
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void save() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#refresh(boolean) Session.refresh(boolean)}
     * method.
     *
     * @param keepChanges flag to keep transient changes
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void refresh(boolean keepChanges)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#logout() Session.logout()}
     * method.
     *
     * @throws RemoteException on RMI errors
     */
    void logout() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#isLive() Session.isLive()}
     * method.
     *
     * @return <code>true</code> if the session is live,
     *         <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean isLive() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#getRootNode() Session.getRootNode()} method.
     *
     * @return root node
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteNode getRootNode() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#hasPendingChanges() Session.hasPendingChanges()}
     * method.
     *
     * @return <code>true</code> if the session has pending changes,
     *         <code>false</code> otherwise
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    boolean hasPendingChanges() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#hasPermission(String,String) Session.hasPermission(String,String)}
     * method.
     *
     * @param path item path
     * @param actions actions
     * @return <code>true</code> if permission is granted,
     *         <code>false</code> otherwise
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    boolean hasPermission(String path, String actions)
            throws RepositoryException, RemoteException;

    /**
     * Imports the system or document view XML data into a subtree of
     * the identified node. Note that the entire XML stream is transferred
     * as a single byte array over the network. This may cause problems with
     * large XML streams. The remote server will wrap the XML data into
     * a {@link java.io.ByteArrayInputStream ByteArrayInputStream} and feed
     * it to the normal importXML method.
     *
     * @param path node path
     * @param xml imported XML document
     * @param uuidBehaviour UUID handling mode
     * @throws IOException on IO errors
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     * @see javax.jcr.Session#importXML(java.lang.String, java.io.InputStream, int)
     */
    void importXML(String path, byte[] xml, int uuidBehaviour)
            throws IOException, RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#setNamespacePrefix(String,String) Session.setNamespacePrefix(String,String)}
     * method.
     *
     * @param prefix namespace prefix
     * @param uri namespace uri
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void setNamespacePrefix(String prefix, String uri)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#getNamespacePrefixes() Session.getNamespacePrefixes()}
     * method.
     *
     * @return namespace prefixes
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    String[] getNamespacePrefixes() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#getNamespaceURI(String) Session.getNamespaceURI(String)}
     * method.
     *
     * @param prefix namespace prefix
     * @return namespace uri
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    String getNamespaceURI(String prefix)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#getNamespacePrefix(String) Session.getNamespacePrefix(String)}
     * method.
     *
     * @param uri namespace uri
     * @return namespace prefix
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    String getNamespacePrefix(String uri)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#addLockToken(String) Session.addLockToken(String)}
     * method.
     *
     * @param name lock token
     * @throws RemoteException on RMI errors
     */
    void addLockToken(String name) throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#getLockTokens() Session.getLockTokens()}
     * method.
     *
     * @return lock tokens
     * @throws RemoteException on RMI errors
     */
    String[] getLockTokens() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Session#removeLockToken(String) Session.removeLockToken(String)}
     * method.
     *
     * @param name lock token
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
     * @param path node path
     * @param skipBinary binary skip flag
     * @param noRecurse no recursion flag
     * @return exported XML document
     * @throws IOException on IO errors
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     * @see javax.jcr.Session#exportSystemView
     */
    byte[] exportSystemView(String path, boolean skipBinary, boolean noRecurse)
            throws IOException, RepositoryException, RemoteException;

    /**
     * Exports the identified repository subtree as a document view XML
     * stream. Note that the entire XML stream is transferred as a
     * single byte array over the network. This may cause problems with
     * large exports. The remote server uses a
     * {@link java.io.ByteArrayOutputStream ByteArrayOutputStream} to capture
     * the XML data written by the normal exportDocView method.
     *
     * @param path node path
     * @param skipBinary skip binary flag
     * @param noRecurse no recursion flag
     * @return exported XML document
     * @throws IOException on IO errors
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     * @see javax.jcr.Session#exportDocumentView
     */
    byte[] exportDocumentView(String path, boolean skipBinary, boolean noRecurse)
        throws IOException, RepositoryException, RemoteException;

    /**
     * Remote version of the {@link javax.jcr.Session#getAccessControlManager()
     * Session.getAccessControlManager()} method.
     *
     * @throws UnsupportedRepositoryOperationException if the remote session
     *             does not support this method
     * @throws RepositoryException if an error occurred getting the access
     *             control manager
     * @throws RemoteException on RMI errors
     */
    RemoteAccessControlManager getAccessControlManager()
            throws UnsupportedRepositoryOperationException,
            RepositoryException, RemoteException;
}
