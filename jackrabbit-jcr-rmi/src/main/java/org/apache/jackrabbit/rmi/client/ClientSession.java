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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.security.AccessControlException;

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.jackrabbit.rmi.remote.RemoteSession;
import org.apache.jackrabbit.rmi.value.SerialValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteSession RemoteSession}
 * interface. This class makes a remote session locally available using
 * the JCR {@link javax.jcr.Session Session} interface.
 *
 * @see javax.jcr.Session
 * @see org.apache.jackrabbit.rmi.remote.RemoteSession
 */
public class ClientSession extends ClientObject implements Session {

    /**
     * Logger instance.
     */
    private static final Logger log =
        LoggerFactory.getLogger(ClientSession.class);

    /** The current repository. */
    private final Repository repository;

    /**
     * Flag indicating whether the session is to be considered live of not.
     * This flag is initially set to <code>true</code> and reset to
     * <code>false</code> by the {@link #logout()} method. The {@link #isLive()}
     * method first checks this flag before asking the remote session.
     */
    private boolean live = true;

    /** The adapted remote session. */
    protected final RemoteSession remote;

    /**
     * The adapted workspace of this session. This field is set on the first
     * call to the {@link #getWorkspace()} method assuming, that a workspace
     * instance is not changing during the lifetime of a session, that is,
     * each call to the server-side <code>Session.getWorkspace()</code> allways
     * returns the same object.
     */
    private Workspace workspace;

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
    public String getUserID() {
        try {
            return remote.getUserID();
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
        if (workspace == null) {
            try {
                workspace =
                    getFactory().getWorkspace(this, remote.getWorkspace());
            } catch (RemoteException ex) {
                throw new RemoteRuntimeException(ex);
            }
        }

        return workspace;
    }

    /** {@inheritDoc} */
    public Session impersonate(Credentials credentials)
            throws RepositoryException {
        try {
            RemoteSession session = remote.impersonate(credentials);
            return getFactory().getSession(repository, session);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Node getRootNode() throws RepositoryException {
        try {
            return getNode(this, remote.getRootNode());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Node getNodeByIdentifier(String id) throws RepositoryException {
        try {
            return getNode(this, remote.getNodeByIdentifier(id));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Node getNodeByUUID(String uuid) throws RepositoryException {
        try {
            return getNode(this, remote.getNodeByUUID(uuid));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Item getItem(String path) throws RepositoryException {
        try {
            return getItem(this, remote.getItem(path));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Node getNode(String path) throws RepositoryException {
        try {
            return getNode(this, remote.getNode(path));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Property getProperty(String path) throws RepositoryException {
        try {
            return (Property) getItem(this, remote.getProperty(path));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean itemExists(String path) throws RepositoryException {
        try {
            return remote.itemExists(path);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean nodeExists(String path) throws RepositoryException {
        try {
            return remote.nodeExists(path);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean propertyExists(String path) throws RepositoryException {
        try {
            return remote.propertyExists(path);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public void removeItem(String path) throws RepositoryException {
        try {
            remote.removeItem(path);
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
    public void save() throws RepositoryException {
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

    /**
     * Returns the {@link SerialValueFactory#getInstance()}.
     *
     * {@inheritDoc}
     */
    public ValueFactory getValueFactory() {
        return SerialValueFactory.getInstance();
    }

    /** {@inheritDoc} */
    public void checkPermission(String path, String actions)
            throws AccessControlException, RepositoryException {
        if (!hasPermission(path, actions)) {
            throw new AccessControlException(
                    "No permission for " + actions + " on " + path);
        }
    }

    /** {@inheritDoc} */
    public boolean hasPermission(String path, String actions)
            throws RepositoryException {
        try {
            return remote.hasPermission(path, actions);
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public void importXML(String path, InputStream xml, int mode)
            throws IOException, RepositoryException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] bytes = new byte[4096];
            for (int n = xml.read(bytes); n != -1; n = xml.read(bytes)) {
                buffer.write(bytes, 0, n);
            }
            remote.importXML(path, buffer.toByteArray(), mode);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        } finally {
            // JCR-2903
            try { xml.close(); } catch (IOException ignore) {}
        }
    }

    /** {@inheritDoc} */
    public ContentHandler getImportContentHandler(
            final String path, final int mode) throws RepositoryException {
        getItem(path); // Check that the path exists
        try {
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ContentHandler handler =
                SerializingContentHandler.getSerializer(buffer);
            return new DefaultContentHandler(handler) {
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
    public void setNamespacePrefix(String prefix, String uri)
            throws RepositoryException {
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
    public String getNamespaceURI(String prefix) throws RepositoryException {
        try {
            return remote.getNamespaceURI(prefix);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getNamespacePrefix(String uri) throws RepositoryException {
        try {
            return remote.getNamespacePrefix(uri);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void logout() {

        // ignore if we are not alive any more.
        if (!isLive()) {
            return;
        }

        try {
            remote.logout();
        } catch (RemoteException ex) {
            // JCRRMI-3: Just log a warning, the connection is dead anyway
            log.warn("Remote logout failed", ex);
        } finally {
            // mark "dead"
            live = false;
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
    public void exportSystemView(
            String path, ContentHandler handler,
            boolean binaryAsLink, boolean noRecurse)
            throws SAXException, RepositoryException {
        try {
            byte[] xml = remote.exportSystemView(path, binaryAsLink, noRecurse);

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
    public void exportSystemView(
            String path, OutputStream output,
            boolean binaryAsLink, boolean noRecurse)
            throws IOException, RepositoryException {
        try {
            byte[] xml = remote.exportSystemView(path, binaryAsLink, noRecurse);
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
    public void exportDocumentView(
            String path, ContentHandler handler,
            boolean binaryAsLink, boolean noRecurse)
            throws SAXException, RepositoryException {
        try {
            byte[] xml = remote.exportDocumentView(path, binaryAsLink, noRecurse);

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
    public void exportDocumentView(
            String path, OutputStream output,
            boolean binaryAsLink, boolean noRecurse)
            throws IOException, RepositoryException {
        try {
            byte[] xml = remote.exportDocumentView(path, binaryAsLink, noRecurse);
            output.write(xml);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLive() {
        try {
            return live && remote.isLive();
        } catch (RemoteException e) {
            log.warn("Failed to test remote session state", e);
            return false;
        }
    }

    public AccessControlManager getAccessControlManager()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        try {
            return getFactory().getAccessControlManager(
                remote.getAccessControlManager());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public RetentionManager getRetentionManager()
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCR-3206");
    }

    public boolean hasCapability(
            String methodName, Object target, Object[] arguments)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCR-3206");
    }

}
