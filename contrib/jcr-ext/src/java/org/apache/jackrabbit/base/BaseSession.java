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
package org.apache.jackrabbit.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * General base class for implementing the JCR Session interface.
 */
public class BaseSession implements Session {

    /**
     * The default constructor is protected to signify that this
     * class needs to be subclassed to be of any real use.
     */
    protected BaseSession() {
    }

    /** Unsupported operation. {@inheritDoc} */
    public Repository getRepository() {
        throw new UnsupportedOperationException();
    }

    /** Unsupported operation. {@inheritDoc} */
    public String getUserId() {
        throw new UnsupportedOperationException();
    }

    /** Unsupported operation. {@inheritDoc} */
    public Object getAttribute(String name) {
        throw new UnsupportedOperationException();
    }

    /** Unsupported operation. {@inheritDoc} */
    public String[] getAttributeNames() {
        throw new UnsupportedOperationException();
    }

    /** Unsupported operation. {@inheritDoc} */
    public Workspace getWorkspace() {
        throw new UnsupportedOperationException();
    }

    /**
     * Calls
     * <code>getRepository().login(credentials, getWorkspace().getName())</code>.
     * {@inheritDoc}
     */
    public Session impersonate(Credentials credentials) throws LoginException,
            RepositoryException {
        return getRepository().login(credentials, getWorkspace().getName());
    }

    /** Unsupported repository operation. {@inheritDoc} */
    public Node getRootNode() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Unsupported repository operation. {@inheritDoc} */
    public Node getNodeByUUID(String uuid) throws ItemNotFoundException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** {@inheritDoc} */
    public Item getItem(String absPath) throws PathNotFoundException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Calls <code>getItem(absPath)</code> and returns <code>true</code>
     * if a {@link PathNotFoundException PathNotFoundException} is not thrown.
     * Throws a {@link RuntimeException RuntimeException} if a general
     * {@link RepositoryException RepositoryException} is thrown by getItem().
     * {@inheritDoc}
     */
    public boolean itemExists(String absPath) {
        try {
            getItem(absPath);
            return true;
        } catch (PathNotFoundException e) {
            return false;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    /** Unsupported repository operation.{@inheritDoc} */
    public void move(String srcAbsPath, String destAbsPath)
            throws ItemExistsException, PathNotFoundException,
            VersionException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Unsupported repository operation. {@inheritDoc} */
    public void save() throws AccessDeniedException,
            ConstraintViolationException, InvalidItemStateException,
            VersionException, LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Unsupported repository operation. {@inheritDoc} */
    public void refresh(boolean keepChanges) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Unsupported repository operation. {@inheritDoc} */
    public boolean hasPendingChanges() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Unsupported operation. {@inheritDoc} */
    public void checkPermission(String absPath, String actions)
            throws AccessControlException {
        throw new UnsupportedOperationException();
    }

    /** Unsupported repository operation. {@inheritDoc} */
    public ContentHandler getImportContentHandler(String parentAbsPath)
            throws PathNotFoundException, ConstraintViolationException,
            VersionException, LockException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Calls
     * <code>transformer.transform(new StreamSource(in), new SAXResult(handler))</code>
     * with an identity {@link Transformer Transformer} and a
     * {@link ContentHandler ContentHandler} instance created by calling
     * <code>getImportContentHandler(parentAbsPath)</code>. Possible
     * {@see TransformerException TransformerExceptions} and
     * {@see TransformerConfigurationException TransformerConfigurationExceptions}
     * are converted to {@link IOException IOExceptions}.
     * {@inheritDoc}
     */
    public void importXML(String parentAbsPath, InputStream in)
            throws IOException, PathNotFoundException, ItemExistsException,
            ConstraintViolationException, VersionException,
            InvalidSerializedDataException, LockException, RepositoryException {
        try {
            ContentHandler handler = getImportContentHandler(parentAbsPath);

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(new StreamSource(in), new SAXResult(handler));
        } catch (TransformerConfigurationException e) {
            throw new IOException(
                    "Unable to deserialize a SAX stream: " + e.getMessage());
        } catch (TransformerException e) {
            throw new IOException(
                    "Unable to deserialize a SAX stream: " + e.getMessage());
        }
    }

    /** Unsupported repository operation. {@inheritDoc} */
    public void exportSysView(String absPath, ContentHandler contentHandler,
            boolean skipBinary, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Calls
     * <code>exportSysView(absPath, handler, binaryAsLink, noRecurse)</code>
     * with a content handler instance <code>handler</code> created on
     * top fo the given output stream using the Xerces
     * {@link XMLSerializer XMLSerializer} class. Possible
     * {@link SAXException SAXExceptions} are converted to
     * {@link IOException IOExceptions}.
     * {@inheritDoc}
     */
    public void exportSysView(String absPath, OutputStream out,
            boolean skipBinary, boolean noRecurse) throws IOException,
            PathNotFoundException, RepositoryException {
        try {
            XMLSerializer serializer =
                new XMLSerializer(out, new OutputFormat());
            exportDocView(absPath, serializer.asContentHandler(),
                    skipBinary, noRecurse);
        } catch (SAXException e) {
            throw new IOException(
                    "Unable to serialize a system view SAX stream: "
                    + e.getMessage());
        }
    }

    /** Unsupported repository operation. {@inheritDoc} */
    public void exportDocView(String absPath, ContentHandler contentHandler,
            boolean skipBinary, boolean noRecurse)
            throws InvalidSerializedDataException, PathNotFoundException,
            SAXException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Calls
     * <code>exportDocView(absPath, handler, binaryAsLink, noRecurse)</code>
     * with a content handler instance <code>handler</code> created on
     * top fo the given output stream using the Xerces
     * {@link XMLSerializer XMLSerializer} class. Possible
     * {@link SAXException SAXExceptions} are converted to
     * {@link IOException IOExceptions}.
     * {@inheritDoc}
     */
    public void exportDocView(String absPath, OutputStream out,
            boolean skipBinary, boolean noRecurse)
            throws InvalidSerializedDataException, IOException,
            PathNotFoundException, RepositoryException {
        try {
            XMLSerializer serializer =
                new XMLSerializer(out, new OutputFormat("xml", "UTF-8", true));
            exportDocView(absPath, serializer.asContentHandler(),
                    skipBinary, noRecurse);
        } catch (SAXException e) {
            throw new IOException(
                    "Unable to serialize a document view SAX stream: "
                    + e.getMessage());
        }
    }

    /** Unsupported repository operation. {@inheritDoc} */
    public void setNamespacePrefix(String prefix, String uri)
            throws NamespaceException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Unsupported repository operation. {@inheritDoc} */
    public String[] getNamespacePrefixes() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /** Unsupported repository operation. {@inheritDoc} */
    public String getNamespaceURI(String prefix) throws NamespaceException,
            RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Implemented by iterating over the prefix array returned by
     * <code>getNamespacePrefixes()</code> and calling
     * <code>getNamespaceURI(prefix)</code> repeatedly until a match
     * is found for the given namespace URI.
     * {@inheritDoc}
     */
    public String getNamespacePrefix(String uri) throws NamespaceException,
            RepositoryException {
        String[] prefixes = getNamespacePrefixes();
        for (int i = 0; i < prefixes.length; i++) {
            if (uri.equals(getNamespaceURI(prefixes[i]))) {
                return prefixes[i];
            }
        }
        throw new NamespaceException("Unknown namespace URI " + uri);
    }

    /** Does nothing. {@inheritDoc} */
    public void logout() {
    }

    /** Unsupported operation. {@inheritDoc} */
    public void addLockToken(String lt) {
        throw new UnsupportedOperationException();
    }

    /** Unsupported operation. {@inheritDoc} */
    public String[] getLockTokens() {
        throw new UnsupportedOperationException();
    }

    /** Unsupported operation. {@inheritDoc} */
    public void removeLockToken(String lt) {
        throw new UnsupportedOperationException();
    }

}
