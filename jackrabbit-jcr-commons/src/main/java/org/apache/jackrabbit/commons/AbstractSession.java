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

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Abstract base class for implementing the JCR {@link Session} interface.
 */
public abstract class AbstractSession implements Session {

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
            ContentHandler handler = getExportContentHandler(out);
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
            ContentHandler handler = getExportContentHandler(out);
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
     * @throws RepositoryException if another error occurs
     */
    public void importXML(
            String parentAbsPath, InputStream in, int uuidBehavior)
            throws IOException, RepositoryException {
        ContentHandler handler =
            getImportContentHandler(parentAbsPath, uuidBehavior);
        new DefaultContentHandler(handler).parse(in);
    }

    /**
     * Returns the node or property at the given path.
     * <p>
     * The default implementation:
     * <ul>
     * <li>Throws a {@link PathNotFoundException} if the given path
     *     does not start with a slash.
     * <li>Returns the root node if the given path is "/"
     * <li>Calls {@link Node#getNode(String)} on the root node with the
     *     part of the given path after the first slash
     * <li>Calls {@link Node#getProperty(String)} similarly in case the
     *     above call fails with a {@link PathNotFoundException}
     * </ul>
     *
     * @param absPath absolute path
     * @return the node or property with the given path
     * @throws PathNotFoundException if the given path is invalid or not found
     * @throws RepositoryException if another error occurs
     */
    public Item getItem(String absPath)
            throws PathNotFoundException, RepositoryException {
        if (!absPath.startsWith("/")) {
            throw new PathNotFoundException("Not an absolute path: " + absPath);
        }

        Node root = getRootNode();
        String relPath = absPath.substring(1);
        if (relPath.length() == 0) {
            return root;
        }

        try {
            return root.getNode(relPath);
        } catch (PathNotFoundException e) {
            return root.getProperty(relPath);
        }
    }

    /**
     * Calls {@link #getItem(String)} with the given path and returns
     * <code>true</code> if the call succeeds. Returns <code>false</code>
     * if a {@link PathNotFoundException} was thrown. Other exceptions are
     * passed through.
     *
     * @param absPath absolute path
     * @return <code>true</code> if an item exists at the given path,
     *         <code>false</code> otherwise
     * @throws RepositoryException if an error occurs
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
     * Creates a {@link ContentHandler} instance that serializes the
     * received SAX events to the given output stream.
     *
     * @param stream output stream to which the SAX events are serialized
     * @return SAX content handler
     * @throws RepositoryException if an error occurs
     */
    private ContentHandler getExportContentHandler(OutputStream stream)
            throws RepositoryException {
        try {
            SAXTransformerFactory stf = (SAXTransformerFactory)
                SAXTransformerFactory.newInstance();
            TransformerHandler handler = stf.newTransformerHandler();

            Transformer transformer = handler.getTransformer();
            transformer.setParameter(OutputKeys.METHOD, "xml");
            transformer.setParameter(OutputKeys.ENCODING, "UTF-8");
            transformer.setParameter(OutputKeys.INDENT, "no");

            handler.setResult(new StreamResult(stream));
            return handler;
        } catch (TransformerFactoryConfigurationError e) {
            throw new RepositoryException(
                    "SAX transformer implementation not available", e);
        } catch (TransformerException e) {
            throw new RepositoryException(
                    "Error creating an XML export content handler", e);
        }
    }

}
