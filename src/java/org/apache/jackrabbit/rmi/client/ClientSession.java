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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.security.AccessControlException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.xml.SessionImportContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteSession RemoteSession}
 * inteface. This class makes a remote session locally available using
 * the JCR {@link javax.jcr.Session Session} interface.
 * 
 * @author Jukka Zitting
 * @see javax.jcr.Session
 * @see org.apache.jackrabbit.rmi.remote.RemoteSession
 */
public class ClientSession extends ClientObject implements Session {

    /** The current repository. */
    private Repository repository;
    
    /** The adapted remote session. */
    private RemoteSession remote;
    
    /**
     * Creates a client adapter for the given remote session.
     * 
     * @param repository current repository
     * @param remote remote repository
     * @param factory local adapter factory
     */
    public ClientSession(Repository repository, RemoteSession remote,
            LocalAdapterFactory factory) {
        super(factory);
        this.repository = repository;
        this.remote = remote;
    }
    
    /**
     * Returns the current repository without contacting the remote session.
     *  
     * {@inheritDoc}
     */
    public Repository getRepository() {
        return repository;
    }

    /** {@inheritDoc} */
    public String getUserId() {
        try {
            return remote.getUserId();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public Object getAttribute(String name) {
        try {
            return remote.getAttribute(name);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public String[] getAttributeNames() {
        try {
            return remote.getAttributeNames();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public Workspace getWorkspace() {
        try {
            return factory.getWorkspace(this, remote.getWorkspace());
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public Session impersonate(Credentials credentials) throws
            LoginException, RepositoryException {
        try {
            RemoteSession session = remote.impersonate(credentials);
            return factory.getSession(repository, session);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Node getRootNode() throws RepositoryException {
        try {
            return factory.getNode(this, remote.getRootNode());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Node getNodeByUUID(String uuid) throws ItemNotFoundException,
            RepositoryException {
        try {
            return factory.getNode(this, remote.getNodeByUUID(uuid));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Item getItem(String path) throws PathNotFoundException,
            RepositoryException {
        try {
            return factory.getItem(this, remote.getItem(path));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean itemExists(String path) {
        try {
            return remote.itemExists(path);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public void move(String from, String to) throws ItemExistsException,
            PathNotFoundException, ConstraintViolationException,
            RepositoryException {
        try {
            remote.move(from, to);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void save() throws AccessDeniedException, LockException,
            ConstraintViolationException, InvalidItemStateException,
            RepositoryException {
        try {
            remote.save();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void refresh(boolean keepChanges) throws RepositoryException {
        try {
            remote.refresh(keepChanges);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean hasPendingChanges() throws RepositoryException {
        try {
            return remote.hasPendingChanges();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void checkPermission(String path, String actions)
            throws AccessControlException {
        try {
            remote.checkPermission(path, actions);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public void importXML(String path, InputStream xml) throws IOException,
            PathNotFoundException, ItemExistsException,
            ConstraintViolationException, InvalidSerializedDataException,
            RepositoryException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] bytes = new byte[4096];
            for (int n = xml.read(bytes); n != -1; n = xml.read(bytes)) {
                buffer.write(bytes, 0, n);
            }
            remote.importXML(path, buffer.toByteArray());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }
    
    /** {@inheritDoc} */
    public ContentHandler getImportContentHandler(String path) throws
            PathNotFoundException, RepositoryException {
        return new SessionImportContentHandler(this, path);
    }

    /** {@inheritDoc} */
    public void setNamespacePrefix(String prefix, String uri) throws
            NamespaceException, RepositoryException {
        try {
            remote.setNamespacePrefix(prefix, uri);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public String[] getNamespacePrefixes() throws RepositoryException {
        try {
            return remote.getNamespacePrefixes();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getNamespaceURI(String prefix) throws NamespaceException,
            RepositoryException {
        try {
            return remote.getNamespaceURI(prefix);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getNamespacePrefix(String uri) throws NamespaceException,
            RepositoryException {
        try {
            return remote.getNamespacePrefix(uri);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void logout() {
        try {
            remote.logout();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public void addLockToken(String name) {
        try {
            remote.addLockToken(name);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }
    
    /** {@inheritDoc} */
    public String[] getLockTokens() {
        try {
            return remote.getLockTokens();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }
    
    /** {@inheritDoc} */
    public void removeLockToken(String name) {
        try {
            remote.removeLockToken(name);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }
    
    /**
     * Exports the XML system view of the specified repository location
     * to the given XML content handler. This method first requests the
     * raw XML data from the remote session, and then uses an identity
     * transformation to feed the data to the given XML content handler.
     * Possible IO and transformer exceptions are thrown as SAXExceptions. 
     * 
     * {@inheritDoc}
     */
    public void exportSysView(String path, ContentHandler handler,
            boolean binaryAsLink, boolean noRecurse) throws
            PathNotFoundException, SAXException, RepositoryException {
        try {
            byte[] xml = remote.exportSysView(path, binaryAsLink, noRecurse);

            Source source = new StreamSource(new ByteArrayInputStream(xml));
            Result result = new SAXResult(handler);

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, result);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        } catch (IOException ex) {
            throw new SAXException(ex);
        } catch (TransformerConfigurationException ex) {
            throw new SAXException(ex);
        } catch (TransformerException ex) {
            throw new SAXException(ex);
        }
    }

    /**
     * Exports the XML system view of the specified repository location
     * to the given output stream. This method first requests the
     * raw XML data from the remote session, and then writes the data to
     * the output stream.
     * 
     * {@inheritDoc}
     */
    public void exportSysView(String path, OutputStream output,
            boolean binaryAsLink, boolean noRecurse) throws
            PathNotFoundException, IOException, RepositoryException {
        try {
            byte[] xml = remote.exportSysView(path, binaryAsLink, noRecurse);
            output.write(xml);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /**
     * Exports the XML document view of the specified repository location
     * to the given XML content handler. This method first requests the
     * raw XML data from the remote session, and then uses an identity
     * transformation to feed the data to the given XML content handler.
     * Possible IO and transformer exceptions are thrown as SAXExceptions. 
     * 
     * {@inheritDoc}
     */
    public void exportDocView(String path, ContentHandler handler,
            boolean binaryAsLink, boolean noRecurse) throws
            InvalidSerializedDataException, PathNotFoundException,
            SAXException, RepositoryException {
        try {
            byte[] xml = remote.exportDocView(path, binaryAsLink, noRecurse);

            Source source = new StreamSource(new ByteArrayInputStream(xml));
            Result result = new SAXResult(handler);

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, result);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        } catch (IOException ex) {
            throw new SAXException(ex);
        } catch (TransformerConfigurationException ex) {
            throw new SAXException(ex);
        } catch (TransformerException ex) {
            throw new SAXException(ex);
        }
    }

    /**
     * Exports the XML document view of the specified repository location
     * to the given output stream. This method first requests the
     * raw XML data from the remote session, and then writes the data to
     * the output stream.
     * 
     * {@inheritDoc}
     */
    public void exportDocView(String path, OutputStream output,
            boolean binaryAsLink, boolean noRecurse) throws
            InvalidSerializedDataException, PathNotFoundException,
            IOException, RepositoryException {
        try {
            byte[] xml = remote.exportDocView(path, binaryAsLink, noRecurse);
            output.write(xml);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }
}
