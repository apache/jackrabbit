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
package org.apache.jackrabbit.commons;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.apache.jackrabbit.commons.xml.DocumentViewExporter;
import org.apache.jackrabbit.commons.xml.Exporter;
import org.apache.jackrabbit.commons.xml.ParsingContentHandler;
import org.apache.jackrabbit.commons.xml.SystemViewExporter;
import org.apache.jackrabbit.commons.xml.ToXmlContentHandler;
import org.apache.jackrabbit.util.XMLChar;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Abstract base class for implementing the JCR {@link Session} interface.
 */
public abstract class AbstractSession implements Session {

    /**
     * Local namespace mappings. Prefixes as keys and namespace URIs as values.
     * <p>
     * This map is only accessed from synchronized methods (see
     * <a href="https://issues.apache.org/jira/browse/JCR-1793">JCR-1793</a>).
     */
    private final Map<String, String> namespaces =
        new HashMap<String, String>();

    /**
     * Clears the local namespace mappings. Subclasses that for example
     * want to participate in a session pools should remember to call
     * <code>super.logout()</code> when overriding this method to avoid
     * namespace mappings to be carried over to a new session.
     */
    public void logout() {
        synchronized (namespaces) {
            namespaces.clear();
        }
    }

    //------------------------------------------------< Namespace handling >--

    /**
     * Returns the namespace prefix mapped to the given URI. The mapping is
     * added to the set of session-local namespace mappings unless it already
     * exists there.
     * <p>
     * This behaviour is based on JSR 283 (JCR 2.0), but remains backwards
     * compatible with JCR 1.0.
     *
     * @param uri namespace URI
     * @return namespace prefix
     * @throws NamespaceException if the namespace is not found
     * @throws RepositoryException if a repository error occurs
     */
    public String getNamespacePrefix(String uri)
            throws NamespaceException, RepositoryException {
        synchronized (namespaces) {
            for (Map.Entry<String, String> entry : namespaces.entrySet()) {
                if (entry.getValue().equals(uri)) {
                    return entry.getKey();
                }
            }

            // The following throws an exception if the URI is not found, that's OK
            String prefix = getWorkspace().getNamespaceRegistry().getPrefix(uri);

            // Generate a new prefix if the global mapping is already taken
            String base = prefix;
            for (int i = 2; namespaces.containsKey(prefix); i++) {
                prefix = base + i;
            }

            namespaces.put(prefix, uri);
            return prefix;
        }
    }

    /**
     * Returns the namespace URI mapped to the given prefix. The mapping is
     * added to the set of session-local namespace mappings unless it already
     * exists there.
     * <p>
     * This behaviour is based on JSR 283 (JCR 2.0), but remains backwards
     * compatible with JCR 1.0.
     *
     * @param prefix namespace prefix
     * @return namespace URI
     * @throws NamespaceException if the namespace is not found
     * @throws RepositoryException if a repository error occurs
     */
    public String getNamespaceURI(String prefix)
            throws NamespaceException, RepositoryException {
        synchronized (namespaces) {
            String uri = namespaces.get(prefix);

            if (uri == null) {
                // Not in local mappings, try the global ones
                uri = getWorkspace().getNamespaceRegistry().getURI(prefix);
                if (namespaces.containsValue(uri)) {
                    // The global URI is locally mapped to some other prefix,
                    // so there are no mappings for this prefix
                    throw new NamespaceException("Namespace not found: " + prefix);
                }
                // Add the mapping to the local set, we already know that
                // the prefix is not taken
                namespaces.put(prefix, uri);
            }

            return uri;
        }
    }

    /**
     * Returns the prefixes of all known namespace mappings. All global
     * mappings not already included in the local set of namespace mappings
     * are added there.
     * <p>
     * This behaviour is based on JSR 283 (JCR 2.0), but remains backwards
     * compatible with JCR 1.0.
     *
     * @return namespace prefixes
     * @throws RepositoryException if a repository error occurs
     */
    public String[] getNamespacePrefixes()
            throws RepositoryException {
        for (String uri : getWorkspace().getNamespaceRegistry().getURIs()) {
            getNamespacePrefix(uri);
        }

        synchronized (namespaces) {
            return namespaces.keySet().toArray(new String[namespaces.size()]);
        }
    }

    /**
     * Modifies the session local namespace mappings to contain the given
     * prefix to URI mapping.
     * <p>
     * This behaviour is based on JSR 283 (JCR 2.0), but remains backwards
     * compatible with JCR 1.0.
     *
     * @param prefix namespace prefix
     * @param uri namespace URI
     * @throws NamespaceException if the mapping is illegal
     * @throws RepositoryException if a repository error occurs
     */
    public void setNamespacePrefix(String prefix, String uri)
            throws NamespaceException, RepositoryException {
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix must not be null");
        } else if (uri == null) {
            throw new IllegalArgumentException("Namespace must not be null");
        } else if (prefix.length() == 0) {
            throw new NamespaceException(
                    "Empty prefix is reserved and can not be remapped");
        } else if (uri.length() == 0) {
            throw new NamespaceException(
                    "Default namespace is reserved and can not be remapped");
        } else if (prefix.toLowerCase().startsWith("xml")) {
            throw new NamespaceException(
                    "XML prefixes are reserved: " + prefix);
        } else if (!XMLChar.isValidNCName(prefix)) {
            throw new NamespaceException(
                    "Prefix is not a valid XML NCName: " + prefix);
        }

        synchronized (namespaces) {
            // Remove existing mapping for the given prefix
            namespaces.remove(prefix);

            // Remove existing mapping(s) for the given URI
            Set<String> prefixes = new HashSet<String>();
            for (Map.Entry<String, String> entry : namespaces.entrySet()) {
                if (entry.getValue().equals(uri)) {
                    prefixes.add(entry.getKey());
                }
            }
            namespaces.keySet().removeAll(prefixes);

            // Add the new mapping
            namespaces.put(prefix, uri);
        }
    }

    //---------------------------------------------< XML export and import >--

    /**
     * Generates a document view export using a {@link DocumentViewExporter}
     * instance.
     *
     * @param path of the node to be exported
     * @param handler handler for the SAX events of the export
     * @param skipBinary whether binary values should be skipped
     * @param noRecurse whether to export just the identified node
     * @throws PathNotFoundException if a node at the given path does not exist
     * @throws SAXException if the SAX event handler failed
     * @throws RepositoryException if another error occurs
     */
    public void exportDocumentView(
            String path, ContentHandler handler,
            boolean skipBinary, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        export(path, new DocumentViewExporter(
                this, handler, !noRecurse, !skipBinary));
    }

    /**
     * Generates a system view export using a {@link SystemViewExporter}
     * instance.
     *
     * @param path of the node to be exported
     * @param handler handler for the SAX events of the export
     * @param skipBinary whether binary values should be skipped
     * @param noRecurse whether to export just the identified node
     * @throws PathNotFoundException if a node at the given path does not exist
     * @throws SAXException if the SAX event handler failed
     * @throws RepositoryException if another error occurs
     */
    public void exportSystemView(
            String path, ContentHandler handler,
            boolean skipBinary, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        export(path, new SystemViewExporter(
                this, handler, !noRecurse, !skipBinary));
    }

    /**
     * Calls {@link Session#exportDocumentView(String, ContentHandler, boolean, boolean)}
     * with the given arguments and a {@link ContentHandler} that serializes
     * SAX events to the given output stream.
     *
     * @param absPath passed through
     * @param out output stream to which the SAX events are serialized
     * @param skipBinary passed through
     * @param noRecurse passed through
     * @throws IOException if the SAX serialization failed
     * @throws RepositoryException if another error occurs
     */
    public void exportDocumentView(
            String absPath, OutputStream out,
            boolean skipBinary, boolean noRecurse)
            throws IOException, RepositoryException {
        try {
            ContentHandler handler = new ToXmlContentHandler(out);
            exportDocumentView(absPath, handler, skipBinary, noRecurse);
        } catch (SAXException e) {
            Exception exception = e.getException();
            if (exception instanceof RepositoryException) {
                throw (RepositoryException) exception;
            } else if (exception instanceof IOException) {
                throw (IOException) exception;
            } else {
                throw new RepositoryException(
                        "Error serializing document view XML", e);
            }
        }
    }

    /**
     * Calls {@link Session#exportSystemView(String, ContentHandler, boolean, boolean)}
     * with the given arguments and a {@link ContentHandler} that serializes
     * SAX events to the given output stream.
     *
     * @param absPath passed through
     * @param out output stream to which the SAX events are serialized
     * @param skipBinary passed through
     * @param noRecurse passed through
     * @throws IOException if the SAX serialization failed
     * @throws RepositoryException if another error occurs
     */
    public void exportSystemView(
            String absPath, OutputStream out,
            boolean skipBinary, boolean noRecurse)
            throws IOException, RepositoryException {
        try {
            ContentHandler handler = new ToXmlContentHandler(out);
            exportSystemView(absPath, handler, skipBinary, noRecurse);
        } catch (SAXException e) {
            Exception exception = e.getException();
            if (exception instanceof RepositoryException) {
                throw (RepositoryException) exception;
            } else if (exception instanceof IOException) {
                throw (IOException) exception;
            } else {
                throw new RepositoryException(
                        "Error serializing system view XML", e);
            }
        }
    }

    /**
     * Parses the given input stream as an XML document and processes the
     * SAX events using the {@link ContentHandler} returned by
     * {@link Session#getImportContentHandler(String, int)}.
     *
     * @param parentAbsPath passed through
     * @param in input stream to be parsed as XML and imported
     * @param uuidBehavior passed through
     * @throws IOException if an I/O error occurs
     * @throws InvalidSerializedDataException if an XML parsing error occurs
     * @throws RepositoryException if a repository error occurs
     */
    public void importXML(
            String parentAbsPath, InputStream in, int uuidBehavior)
            throws IOException, InvalidSerializedDataException,
            RepositoryException {
        try {
            ContentHandler handler =
                getImportContentHandler(parentAbsPath, uuidBehavior);
            new ParsingContentHandler(handler).parse(in);
        } catch (SAXException e) {
            Throwable exception = e.getException();
            if (exception instanceof RepositoryException) {
                throw (RepositoryException) exception;
            } else if (exception instanceof IOException) {
                throw (IOException) exception;
            } else {
                throw new InvalidSerializedDataException("XML parse error", e);
            }
        } finally {
            // JCR-2903
            if (in != null) {
                try { in.close(); } catch (IOException ignore) {}
            }
        }
    }

    //-----------------------------------------------------< Item handling >--

    private String toRelativePath(String absPath) throws PathNotFoundException {
        if (absPath.startsWith("/") && absPath.length() > 1) {
            return absPath.substring(1);
        } else {
            throw new PathNotFoundException("Not an absolute path: " + absPath);
        }
    }

    /**
     * Returns the node or property at the given path.
     * <p>
     * The default implementation:
     * <ul>
     * <li>Returns the root node if the given path is "/"
     * <li>Delegates to {@link Session#getNodeByIdentifier(String)} for identifier
     * paths
     * <li>Throws a {@link PathNotFoundException} if the given path does not
     * start with a slash.
     * <li>Calls {@link Node#getNode(String)} on the root node with the part of
     * the given path after the first slash
     * <li>Calls {@link Node#getProperty(String)} similarly in case the above
     * call fails with a {@link PathNotFoundException}
     * </ul>
     * 
     * @see Session#getItem(String)
     * @param absPath
     *            absolute path
     * @return the node or property with the given path
     * @throws PathNotFoundException
     *             if the given path is invalid or not found
     * @throws RepositoryException
     *             if another error occurs
     */
    public Item getItem(String absPath) throws PathNotFoundException, RepositoryException {
        Node root = getRootNode();
        if (absPath.equals("/")) {
            return root;
        } else if (absPath.startsWith("[") && absPath.endsWith("]")) {
            return getNodeByIdentifier(absPath.substring(1, absPath.length() - 1));
        } else {
            String relPath = toRelativePath(absPath);
            if (root.hasNode(relPath)) {
                return root.getNode(relPath);
            } else {
                return root.getProperty(relPath);
            }
        }
    }

    /**
     * Calls {@link #getItem(String)} with the given path and returns
     * <code>true</code> if the call succeeds. Returns <code>false</code>
     * if a {@link PathNotFoundException} was thrown. Other exceptions are
     * passed through.
     *
     * @see Session#itemExists(String)
     * @param absPath absolute path
     * @return <code>true</code> if an item exists at the given path,
     *         <code>false</code> otherwise
     * @throws RepositoryException if an error occurs
     */
    public boolean itemExists(String absPath) throws RepositoryException {
        if (absPath.equals("/")) {
            return true;
        } else {
            Node root = getRootNode();
            String relPath = toRelativePath(absPath);
            return root.hasNode(relPath) || root.hasProperty(relPath);
        }
    }

    /**
     * Removes the identified item. Implemented by calling
     * {@link Item#remove()} on the item removed by {@link #getItem(String)}.
     *
     * @see Session#removeItem(String)
     * @param absPath An absolute path of the item to be removed
     * @throws RepositoryException if the item can not be removed
     */
    public void removeItem(String absPath) throws RepositoryException {
        getItem(absPath).remove();
    }

    /**
     * Returns the node with the given absolute path.
     *
     * @see Session#getNode(String)
     * @param absPath absolute path
     * @return node at the given path
     * @throws RepositoryException if the node can not be accessed
     */
    public Node getNode(String absPath) throws RepositoryException {
        Node root = getRootNode();
        if (absPath.equals("/")) {
            return root;
        } else {
            return root.getNode(toRelativePath(absPath));
        }
    }

    /**
     * Checks whether a node with the given absolute path exists.
     *
     * @see Session#nodeExists(String)
     * @param absPath absolute path
     * @return <code>true</code> if a node with the given path exists,
     *         <code>false</code> otherwise
     * @throws RepositoryException if the path is invalid
     */
    public boolean nodeExists(String absPath) throws RepositoryException {
        if (absPath.equals("/")) {
            return true;
        } else {
            return getRootNode().hasNode(toRelativePath(absPath));
        }
    }

    /**
     * Returns the property with the given absolute path.
     *
     * @see Session#getProperty(String)
     * @param absPath absolute path
     * @return node at the given path
     * @throws RepositoryException if the property can not be accessed
     */
    public Property getProperty(String absPath) throws RepositoryException {
        if (absPath.equals("/")) {
            throw new RepositoryException("The root node is not a property");
        } else {
            return getRootNode().getProperty(toRelativePath(absPath));
        }
    }

    /**
     * Checks whether a property with the given absolute path exists.
     *
     * @see Session#propertyExists(String)
     * @param absPath absolute path
     * @return <code>true</code> if a property with the given path exists,
     *         <code>false</code> otherwise
     * @throws RepositoryException if the path is invalid
     */
    public boolean propertyExists(String absPath) throws RepositoryException {
        if (absPath.equals("/")) {
            return false;
        } else {
            return getRootNode().hasProperty(toRelativePath(absPath));
        }
    }

    //--------------------------------------------------< Session handling >--

    /**
     * Logs in the same workspace with the given credentials.
     * <p>
     * The default implementation:
     * <ul>
     * <li>Retrieves the {@link Repository} instance using
     *     {@link Session#getRepository()}
     * <li>Retrieves the current workspace using {@link Session#getWorkspace()}
     * <li>Retrieves the name of the current workspace using
     *     {@link Workspace#getName()}
     * <li>Calls {@link Repository#login(Credentials, String)} on the
     *     retrieved repository with the given credentials and the retrieved
     *     workspace name.
     * </ul>
     *
     * @param credentials login credentials
     * @return logged in session
     * @throws RepositoryException if an error occurs
     */
    public Session impersonate(Credentials credentials)
            throws RepositoryException {
        return getRepository().login(credentials, getWorkspace().getName());
    }

    //-------------------------------------------------------------< private >

    /**
     * Exports content at the given path using the given exporter.
     *
     * @param path of the node to be exported
     * @param exporter document or system view exporter
     * @throws SAXException if the SAX event handler failed
     * @throws RepositoryException if another error occurs
     */
    private synchronized void export(String path, Exporter exporter)
            throws PathNotFoundException, SAXException, RepositoryException {
        Item item = getItem(path);
        if (item.isNode()) {
            exporter.export((Node) item);
        } else {
            throw new PathNotFoundException(
                    "XML export is not defined for properties: " + path);
        }
    }

}
