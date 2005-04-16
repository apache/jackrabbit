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
package org.apache.jackrabbit.lite;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.base.BaseSession;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.xml.DocumentViewExportVisitor;
import org.apache.jackrabbit.xml.SystemViewExportVisitor;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Lightweight implementation of the JCR Session interface.
 * <p>
 * The session attributes are managed as a private hash map
 * that subclasses can modify using the protected
 * {@link #addAttribute(String, Object) addAttribute} method.
 * <p>
 * The session namespace mappings are managed using two property
 * mappings (one for prefixes, the other for URIs). If a namespace
 * mapping is not registered in the session, then it is looked up
 * using the namespace registry given by
 * <code>getWorkspace().getNamespaceRegistry()</code>.
 */
public class LiteSession extends BaseSession {

    /** The content repository associated with this session. */
    private final Repository repository;

    /** The user id associated with this session. */
    private final String userId;

    /** Session attributes. */
    private final Map attributes;

    /** The namespace prefix to URI mappings of this session. */
    private final Properties prefixToURI;

    /** The namespace URI to prefix mappings of this session. */
    private final Properties uriToPrefix;

    /**
     * Initializes a session. The constructor is protected to signify
     * that this class needs to be subclassed to be of any real use.
     *
     * @param repository the content repository associated with this session
     * @param userId the user id associated with this session
     */
    protected LiteSession(Repository repository, String userId) {
        this.repository = repository;
        this.userId = userId;
        this.attributes = new HashMap();
        this.prefixToURI = new Properties();
        this.uriToPrefix = new Properties();
    }

    /**
     * Returns the content repository associated with this session.
     *
     * @return content repository
     */
    public Repository getRepository() {
        return repository;
    }

    /**
     * Returns the user id associated with this session.
     *
     * @return user id
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Adds a session attribute.
     *
     * @param name attribute name
     * @param value attribute value
     */
    protected void addAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    /**
     * Returns the value of the identified session attribute.
     *
     * @param name attribute name
     * @return attribute value
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Returns the names of available session attributes.
     *
     * @return session attribute names
     */
    public String[] getAttributeNames() {
        return (String[]) attributes.keySet().toArray(new String[0]);
    }

    public Item getItem(String absPath) throws PathNotFoundException,
            RepositoryException {
        Path path = Path.parseJCRPath(this, absPath);
        return path.walk(getRootNode());
    }

    /**
     * Returns the prefix that is mapped to the given namespace URI.
     * Implemented by first looking up the local session namespaces and
     * then the workspace namespace registry given by
     * <code>getWorkspace().getNamespaceRegistry()</code>.
     *
     * @param uri namespace URI
     * @return namespace prefix
     * @throws NamespaceException if the namespace is not registred
     * @throws RepositoryException on repository errors
     */
    public String getNamespacePrefix(String uri) throws NamespaceException,
            RepositoryException {
        String prefix = uriToPrefix.getProperty(uri);
        if (prefix != null) {
            return prefix;
        } else {
            return getWorkspace().getNamespaceRegistry().getPrefix(uri);
        }
    }

    /**
     * Returns the URI that is mapped to the given namespace prefix.
     * Implemented by first looking up the local session namespaces and
     * then the workspace namespace registry given by
     * <code>getWorkspace().getNamespaceRegistry()</code>.
     *
     * @param prefix namespace prefix
     * @return namespace URI
     * @throws NamespaceException if the namespace is not registred
     * @throws RepositoryException on repository errors
     */
    public String getNamespaceURI(String prefix) throws NamespaceException,
            RepositoryException {
        String uri = uriToPrefix.getProperty(prefix);
        if (uri != null) {
            return uri;
        } else {
            return getWorkspace().getNamespaceRegistry().getURI(prefix);
        }
    }

    /**
     * Returns all namespace prefixes of this session. Implemented by
     * combining the namespace prefixes returned by
     * <code>getWorkspace().getNamespaceRegistry().getPrefixes()</code>
     * with the local session namespace prefixes.
     *
     * @return namespace prefixes
     * @throws RepositoryException on repository errors
     */
    public String[] getNamespacePrefixes() throws RepositoryException {
        Set prefixes = new HashSet();
        prefixes.addAll(Arrays.asList(
                getWorkspace().getNamespaceRegistry().getPrefixes()));
        prefixes.addAll(prefixToURI.keySet());
        return (String[]) prefixes.toArray(new String[0]);
    }

    /**
     * Registers the given namespace with a new prefix during this session.
     * The given namespace URI must already be registered in the workspace
     * and no other namespace must already be registered for the same
     * prefix.
     *
     * @param prefix namespace prefix
     * @param uri namespace URI
     * @throws NamespaceException if the URI is not registered in workspace,
     *                            if the prefix is already registered to
     *                            another namespace URI, or if the prefix
     *                            is reserved
     * @throws RepositoryException on repository errors
     */
    public void setNamespacePrefix(String prefix, String uri)
            throws NamespaceException, RepositoryException {
        if (prefix.length() >= 3
                && prefix.substring(0, 3).equalsIgnoreCase("xml")) {
            throw new NamespaceException(
                    "Unable to register the reserved prefix " + prefix);
        } else {
            NamespaceRegistry registry = getWorkspace().getNamespaceRegistry();
            String registeredPrefix = registry.getPrefix(uri);
            if (!registeredPrefix.equals(prefix)) {
                try {
                    registry.getURI(prefix);
                } catch (NamespaceException e) {
                    // Prefix not already registered, register for session
                    prefixToURI.setProperty(prefix, uri);
                    uriToPrefix.setProperty(uri, prefix);
                    return;
                }
                throw new NamespaceException(
                        "Prefix " + prefix + " is already registered");
            }
        }
    }

    /**
     * Serialized the identified subtree using the document view XML format.
     * Implemented using the
     * {@link DocumentViewExportVisitor DocumentViewExportVisitor} class.
     * {@inheritDoc}
     */
    public void exportDocView(String absPath, ContentHandler contentHandler,
            boolean skipBinary, boolean noRecurse)
            throws InvalidSerializedDataException, PathNotFoundException,
            SAXException, RepositoryException {
        Item item = getItem(absPath);
        if (item.isNode()) {
            item.accept(new DocumentViewExportVisitor(
                    contentHandler, skipBinary, noRecurse));
        } else {
            throw new PathNotFoundException("Invalid node path " + absPath);
        }
    }

    /**
     * Serialized the identified subtree using the system view XML format.
     * Implemented using the
     * {@link SystemViewExportVisitor SystemViewExportVisitor} class.
     * {@inheritDoc}
     */
    public void exportSysView(String absPath, ContentHandler contentHandler,
            boolean skipBinary, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        Item item = getItem(absPath);
        if (item.isNode()) {
            item.accept(new SystemViewExportVisitor(
                    contentHandler, skipBinary, noRecurse));
        } else {
            throw new PathNotFoundException("Invalid node path " + absPath);
        }
    }

    public ContentHandler getImportContentHandler(String parentAbsPath)
            throws PathNotFoundException, ConstraintViolationException,
            VersionException, LockException, RepositoryException {
        // TODO Auto-generated method stub
        return super.getImportContentHandler(parentAbsPath);
    }
}
