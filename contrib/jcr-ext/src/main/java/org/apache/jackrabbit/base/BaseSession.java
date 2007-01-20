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
package org.apache.jackrabbit.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.regex.Pattern;

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.xml.DocumentViewExportVisitor;
import org.apache.jackrabbit.xml.SystemViewExportVisitor;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Session base class. The dummy session implemented by this class
 * attempts to act like a read-only anonymous session. Subclasses must
 * override at least the {@link #getRepository() getRepository()},
 * {@link #getWorkspace() getWorkspace()},
 * {@link #getRootNode() getRootNode()}, and
 * {@link #getValueFactory() getValueFactory()} methods to make this class
 * somewhat useful. See the method javadocs for full details of which
 * methods to override for each required feature.
 */
public class BaseSession implements Session {

    /** The pattern used to match UUID strings. */
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}");

    /**
     * Unsupported operation. Subclasses should override this method to
     * return the repository to which this session is attached.
     * Overiding this method is required for the default implementation of the
     * {@link #impersonate(Credentials) impersonate(Credentials)} method.
     *
     * @return nothing, throws a {@link UnsupportedOperationException}
     * @see Session#getRepository()
     */
    public Repository getRepository() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns <code>null</code> to indicate that this session is associated
     * with the "anonymous" user. Subclasses should override this method to
     * return the actual user identity associated with the session.
     *
     * @return always <code>null</code>
     * @see Session#getUserID()
     */
    public String getUserID() {
        return null;
    }

    /**
     * Returns <code>null</code> to indicate that the named attribute does
     * not exist. Subclasses should override this method to return the
     * available attribute values.
     *
     * @param name attribute name
     * @return always <code>null</code>
     * @see Session#getAttribute(String)
     */
    public Object getAttribute(String name) {
        return null;
    }

    /**
     * Returns an empty string array to indicate that no attributes are
     * available. Subclasses should override this method to return the
     * available attribute names.
     *
     * @return empty array
     * @see Session#getAttributeNames()
     */
    public String[] getAttributeNames() {
        return new String[0];
    }

    /**
     * Unsupported operation. Subclasses should override this method to
     * return the workspace attached to this session. Overiding this method
     * is required for the default implementations of the
     * {@link #impersonate(Credentials) impersonate(Credentials)} and
     * {@link #getNodeByUUID(String) getNodeByUUID(String)} methods.
     *
     * @return nothing, throws a {@link UnsupportedOperationException}
     * @see Session#getWorkspace()
     */
    public Workspace getWorkspace() {
        throw new UnsupportedOperationException();
    }

    /**
     * Implemented by calling the
     * {@link Repository#login(Credentials, String) login(Credentials, String)}
     * method of the {@link Repository} instance returned by the
     * {@link #getRepository() getRepository()} method. The method is invoked
     * with the given login credentials and the workspace name returned by
     * the {@link Workspace#getName() getName()} method of the
     * {@link Workspace} instance returned by the
     * {@link #getWorkspace() getWorkspace()} method.
     * <p>
     * There should normally be little need for subclasses to override this
     * method unless the underlying repository implementation suggests a
     * more straightforward implementation.
     *
     * @param credentials login credentials
     * @return impersonated session
     * @see Session#impersonate(Credentials)
     */
    public Session impersonate(Credentials credentials)
            throws RepositoryException {
        return getRepository().login(credentials, getWorkspace().getName());
    }

    /**
     * Unsupported operation. Subclasses should override this method to
     * return the root node in the workspace attached to this session.
     * Overriding this method is required for the default implementation of
     * the {@link #getItem(String) getItem(String)} method.
     *
     * @return nothing, throws a {@link UnsupportedRepositoryOperationException}
     * @see Session#getRootNode()
     */
    public Node getRootNode() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Implemented by making the XPath query <code>//*[@jcr:uuid='...']</code>
     * and returning the node that matches the query. Subclasses may want to
     * override this method if an UUID index or another more efficient
     * lookup mechanism can be used directly.
     *
     * @return the identified node
     * @throws ItemNotFoundException if the identified node does not exist
     * @see Session#getNodeByUUID(String)
     */
    public Node getNodeByUUID(String uuid) throws RepositoryException {
        if (UUID_PATTERN.matcher(uuid).matches()) {
            String xpath;
            String jcr = getNamespacePrefix(QName.NS_JCR_URI);
            if (jcr.length() > 0) {
                xpath = "//*[@" + jcr + ":uuid='" + uuid + "']";
            } else {
                xpath = "//*[@uuid='" + uuid + "']";
            }
            Query query =
                getWorkspace().getQueryManager().createQuery(Query.XPATH, xpath);
            QueryResult result = query.execute();
            NodeIterator nodes = result.getNodes();
            if (nodes.hasNext()) {
                return nodes.nextNode();
            }
        }
        throw new ItemNotFoundException(uuid);
    }

    /**
     * Implemented by invoking the {@link Node#getNode(String) getNode(String)}
     * (or {@link Node#getProperty(String) getProperty(String)} if getNode
     * fails) method on the root node returned by the
     * {@link #getRootNode() getRootNode()} method. The path given to the
     * getNode or getProperty method is the given path without the leading "/".
     * If the given path is "/" then the root node is returned directly.
     * <p>
     * Subclasses should not normally need to override this method as long as
     * the referenced methods have been implemented. For performance reasons
     * it might make sense to override this method because this implementation
     * can cause the item path to be traversed twice.
     *
     * @param absPath absolute item path
     * @return identified item
     * @see Session#getItem(String)
     */
    public Item getItem(String absPath) throws PathNotFoundException,
            RepositoryException {
        if (absPath == null || !absPath.startsWith("/")) {
            throw new PathNotFoundException("Invalid item path: " + absPath);
        }

        Node node = getRootNode();
        if (absPath.equals("/")) {
            return node;
        } else {
            String relPath = absPath.substring(1);
            try {
                return node.getNode(relPath);
            } catch (PathNotFoundException e) {
                return node.getProperty(relPath);
            }
        }
    }

    /**
     * Implemented by trying to retrieve the identified item using the
     * {@link #getItem(String) getItem(String)} method. Subclasses may
     * want to override this method for performance as there is no real
     * need for instantiating the identified item.
     *
     * @param absPath absolute item path
     * @return <code>true</code> if the identified item exists,
     *         <code>false</code> otherwise
     * @see Session#itemExists(String)
     */
    public boolean itemExists(String absPath) throws RepositoryException {
        try {
            getItem(absPath);
            return true;
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    /**
     * Unsupported operation. Subclasses should override this method to
     * make it possible to move and rename items.
     *
     * @param srcAbsPath source item path
     * @param destAbsPath destination item path 
     * @see Session#move(String, String)
     */
    public void move(String srcAbsPath, String destAbsPath)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Unsupported operation. Subclasses should override this method to
     * allow modifications to the content repository.
     *
     * @see Session#save()
     */
    public void save() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Does nothing. Subclasses should override this method to correctly
     * manage the transient state and underlying repository changes. 
     *
     * @param keepChanges whether to keep transient changes (ignored)
     * @see Session#refresh(boolean)
     */
    public void refresh(boolean keepChanges) throws RepositoryException {
    }

    /**
     * Returns <code>false</code> to indicate that there are no pending
     * changes. Subclasses should override this method to correctly manage
     * the transient state.
     *
     * @return always <code>false</code>
     * @see Session#hasPendingChanges()
     */
    public boolean hasPendingChanges() throws RepositoryException {
        return false;
    }

    /**
     * Throws an {@link AccessControlException} for anything else than
     * <code>read</code> actions to indicate that only read access is
     * permitted. Subclasses should override this method to correctly
     * report access controls settings.
     *
     * @param absPath item path
     * @param actions action strings
     * @throws AccessControlException if other than <code>read</code> actions
     *                                are requested
     * @see Session#checkPermission(String, String)
     */
    public void checkPermission(String absPath, String actions)
            throws AccessControlException {
        String[] parts = actions.split(",");
        for (int i = 0; i < parts.length; i++) {
            if (!"read".equals(parts[i])) {
                throw new AccessControlException(
                        "No " + actions + " permission for " + absPath);
            }
        }
    }

    /**
     * Unsupported operation. Subclasses should override this method to
     * support XML imports. Overriding this method is required for the
     * default implementation of the
     * {@link #importXML(String, InputStream, int) importXML(String, InputStream, int)}
     * method.
     *
     * @param parentAbsPath path of the parent node
     * @param uuidBehaviour UUID behaviour flag
     * @return nothing, throws an {@link UnsupportedRepositoryOperationException}
     * @see Session#getImportContentHandler(String, int)
     */
    public ContentHandler getImportContentHandler(
            String parentAbsPath, int uuidBehaviour)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Parses the XML input stream and feeds the SAX events to the content
     * handler returned by the
     * {@link #getImportContentHandler(String, int) getImportContentHandler(String, int)}
     * method.
     *
     * @param parentAbsPath path of the parent node
     * @param in XML input stream
     * @param uuidBehaviour UUID behaviour flag
     * @see Session#importXML(String, InputStream, int)
     */
    public void importXML(
            String parentAbsPath, InputStream in, int uuidBehaviour)
            throws IOException, RepositoryException {
        try {
            ContentHandler handler =
                getImportContentHandler(parentAbsPath, uuidBehaviour);

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(new StreamSource(in), new SAXResult(handler));
        } catch (TransformerConfigurationException e) {
            throw new IOException(
                    "Unable to configure a SAX transformer: " + e.getMessage());
        } catch (TransformerException e) {
            throw new IOException(
                    "Unable to deserialize a SAX stream: " + e.getMessage());
        }
    }

    /**
     * Creates a {@link SystemViewExportVisitor} instance and passes it
     * to the node identified by the given path. The export visitor traverses
     * the entire content tree and generates the system view SAX events for
     * the given content handler. Subclasses may override this method for
     * better performance or custom processing, but the default implementation
     * should be good enough for general use.
     * 
     * @param absPath node path
     * @param contentHandler SAX content handler
     * @param skipBinary binary property skip flag
     * @param noRecurse subtree recursion flag
     * @see Session#exportSystemView(String, ContentHandler, boolean, boolean)
     */
    public void exportSystemView(
            String absPath, ContentHandler contentHandler,
            boolean skipBinary, boolean noRecurse)
            throws SAXException, RepositoryException {
        Item item = getItem(absPath);
        if (item.isNode()) {
            ItemVisitor visitor = new SystemViewExportVisitor(
                    contentHandler, skipBinary, noRecurse); 
            item.accept(visitor);
        } else {
            throw new PathNotFoundException("Invalid node path: " + absPath);
        }
    }

    /**
     * Creates a SAX serializer for the given output stream and passes it to the
     * {@link #exportSystemView(String, ContentHandler, boolean, boolean) exportSystemView(String, ContentHandler, boolean, boolean)}
     * method along with the other parameters.
     *
     * @param absPath node path
     * @param out XML output stream
     * @param skipBinary binary property skip flag
     * @param noRecurse subtree recursion flag
     * @see Session#exportSystemView(String, OutputStream, boolean, boolean)
     */
    public void exportSystemView(
            String absPath, OutputStream out,
            boolean skipBinary, boolean noRecurse)
            throws IOException, RepositoryException {
        try {
            SAXTransformerFactory factory = (SAXTransformerFactory)
                SAXTransformerFactory.newInstance();
            TransformerHandler handler = factory.newTransformerHandler();
            handler.setResult(new StreamResult(out));
            exportSystemView(absPath, handler, skipBinary, noRecurse);
        } catch (TransformerConfigurationException e) {
            throw new IOException(
                    "Unable to configure a SAX transformer: " + e.getMessage());
        } catch (SAXException e) {
            throw new IOException(
                    "Unable to serialize a SAX stream: " + e.getMessage());
        }
    }

    /**
     * Creates a {@link DocumentViewExportVisitor} instance and passes it
     * to the node identified by the given path. The export visitor traverses
     * the entire content tree and generates the document view SAX events for
     * the given content handler. Subclasses may override this method for
     * better performance or custom processing, but the default implementation
     * should be good enough for general use.
     * 
     * @param absPath node path
     * @param contentHandler SAX content handler
     * @param skipBinary binary property skip flag
     * @param noRecurse subtree recursion flag
     * @see Session#exportDocumentView(String, ContentHandler, boolean, boolean)
     */
    public void exportDocumentView(
            String absPath, ContentHandler contentHandler,
            boolean skipBinary, boolean noRecurse)
            throws SAXException, RepositoryException {
        Item item = getItem(absPath);
        if (item.isNode()) {
            ItemVisitor visitor = new DocumentViewExportVisitor(
                    contentHandler, skipBinary, noRecurse); 
            item.accept(visitor);
        } else {
            throw new PathNotFoundException("Invalid node path: " + absPath);
        }
    }

    /**
     * Creates a SAX serializer for the given output stream and passes it to the
     * {@link #exportDocumentView(String, ContentHandler, boolean, boolean) exportDocumentView(String, ContentHandler, boolean, boolean)}
     * method along with the other parameters.
     *
     * @param absPath node path
     * @param out XML output stream
     * @param skipBinary binary property skip flag
     * @param noRecurse subtree recursion flag
     * @see Session#exportDocumentView(String, OutputStream, boolean, boolean)
     */
    public void exportDocumentView(
            String absPath, OutputStream out,
            boolean skipBinary, boolean noRecurse)
            throws IOException, RepositoryException {
        try {
            SAXTransformerFactory factory = (SAXTransformerFactory)
                SAXTransformerFactory.newInstance();
            TransformerHandler handler = factory.newTransformerHandler();
            handler.setResult(new StreamResult(out));
            exportDocumentView(absPath, handler, skipBinary, noRecurse);
        } catch (TransformerConfigurationException e) {
            throw new IOException(
                    "Unable to configure a SAX transformer: " + e.getMessage());
        } catch (SAXException e) {
            throw new IOException(
                    "Unable to serialize a SAX stream: " + e.getMessage());
        }
    }

    /**
     * Unsupported operation. Subclasses should override this method to
     * support namespace remapping.
     *
     * @param prefix namespace prefix
     * @param uri namespace uri
     * @see Session#setNamespacePrefix(String, String)
     */
    public void setNamespacePrefix(String prefix, String uri)
            throws NamespaceException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Returns the namespace prefixes registered in the
     * {@link javax.jcr.NamespaceRegistry} associated with the workspace of
     * this session. Subclasses should override this method to support
     * namespace remapping.
     *
     * @return namespace prefixes
     * @see Session#getNamespacePrefixes()
     */
    public String[] getNamespacePrefixes() throws RepositoryException {
        return getWorkspace().getNamespaceRegistry().getPrefixes();
    }

    /**
     * Returns the namespace URI registered for the given prefix in the
     * {@link javax.jcr.NamespaceRegistry} associated with the workspace of
     * this session. Subclasses should override this method to support
     * namespace remapping.
     *
     * @param prefix namespace prefix
     * @return namespace URI
     * @see Session#getNamespaceURI(String)
     */
    public String getNamespaceURI(String prefix) throws RepositoryException {
        return getWorkspace().getNamespaceRegistry().getURI(prefix);
    }

    /**
     * Returns the namespace prefix registered for the given URI in the
     * {@link javax.jcr.NamespaceRegistry} associated with the workspace of
     * this session. Subclasses should override this method to support
     * namespace remapping.
     *
     * @param uri namespace URI
     * @return namespace prefix
     * @see Session#getNamespacePrefix(String)
     */
    public String getNamespacePrefix(String uri) throws RepositoryException {
        return getWorkspace().getNamespaceRegistry().getPrefix(uri);
    }

    /**
     * Does nothing. Subclasses should override this method to actually
     * close this session.
     *
     * @see Session#logout()
     */
    public void logout() {
    }

    /**
     * Unsupported operation. Subclasses should override this method to support
     * lock token management.
     *
     * @param lock token
     * @see Session#addLockToken(String)
     */
    public void addLockToken(String lt) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an empty string array to indicate that no lock tokens are held
     * by this session. Subclasses should override this method to return the
     * actual lock tokens held by this session.
     *
     * @return empty array
     * @see Session#getLockTokens()
     */
    public String[] getLockTokens() {
        return new String[0];
    }

    /**
     * Unsupported operation. Subclasses should override this method to support
     * lock token management.
     *
     * @param lock token
     * @see Session#removeLockToken(String)
     */
    public void removeLockToken(String lt) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation. Subclasses should override this method to
     * allow the creation of new {@link javax.jcr.Value Value} instances.
     *
     * @param lock token
     * @see Session#removeLockToken(String)
     */
    public ValueFactory getValueFactory()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Returns <code>true</code> to indicate that the session has not been
     * closed. Subclasses should override this method to correctly report
     * the state of the session.
     *
     * @return always <code>true</code>
     * @see Session#isLive()
     */
    public boolean isLive() {
        return true;
    }

}
