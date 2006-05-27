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
package org.apache.jackrabbit.decorator;

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
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 */
public class SessionDecorator implements Session {

    protected final DecoratorFactory factory;

    protected final Repository repository;

    protected final Session session;

    /**
     * @param factory
     * @param repository the repository (decorator) that was used to create the
     *                   session decorator.
     * @param session
     */
    public SessionDecorator(
            DecoratorFactory factory, Repository repository, Session session) {
        this.factory = factory;
        this.repository = repository;
        this.session = session;
    }

    /** {@inheritDoc} */
    public Repository getRepository() {
        return repository;
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public String getUserID() {
        return session.getUserID();
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public Object getAttribute(String name) {
        return session.getAttribute(name);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public String[] getAttributeNames() {
        return session.getAttributeNames();
    }

    /**
     * Forwards the method call to the underlying session. The returned
     * workspace is wrapped into a workspace decorator using the decorator
     * factory.
     *
     * @return decorated workspace
     */
    public Workspace getWorkspace() {
        return factory.getWorkspaceDecorator(this, session.getWorkspace());
    }

    /**
     * Forwards the method call to the underlying session. The returned
     * session is wrapped into a session decorator using the decorator factory.
     *
     * @return decorated session
     */
    public Session impersonate(Credentials credentials) throws LoginException,
            RepositoryException {
        Session newSession = session.impersonate(credentials);
        return factory.getSessionDecorator(repository, newSession);
    }

    /**
     * Forwards the method call to the underlying session. The returned
     * node is wrapped into a node decorator using the decorator factory.
     *
     * @return decorated node
     */
    public Node getRootNode() throws RepositoryException {
        Node root = session.getRootNode();
        return factory.getNodeDecorator(this, root);
    }

    /**
     * Forwards the method call to the underlying session. The returned
     * node is wrapped into a node decorator using the decorator factory.
     *
     * @return decorated node
     */
    public Node getNodeByUUID(String uuid) throws ItemNotFoundException,
            RepositoryException {
        Node node = session.getNodeByUUID(uuid);
        return factory.getNodeDecorator(this, node);
    }

    /**
     * Forwards the method call to the underlying session. The returned
     * item is wrapped into a node, property, or item decorator using
     * the decorator factory. The decorator type depends on the type
     * of the underlying item.
     *
     * @return decorated item, property, or node
     */
    public Item getItem(String absPath) throws PathNotFoundException,
            RepositoryException {
        Item item = session.getItem(absPath);
        return factory.getItemDecorator(this, item);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public boolean itemExists(String path) throws RepositoryException {
        return session.itemExists(path);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void move(String srcAbsPath, String destAbsPath) throws
            ItemExistsException, PathNotFoundException, VersionException,
            RepositoryException {
        session.move(srcAbsPath, destAbsPath);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void save() throws AccessDeniedException,
            ConstraintViolationException, InvalidItemStateException,
            VersionException, LockException, RepositoryException {
        session.save();
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void refresh(boolean keepChanges) throws RepositoryException {
        session.refresh(keepChanges);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public boolean hasPendingChanges() throws RepositoryException {
        return session.hasPendingChanges();
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void checkPermission(String absPath, String actions)
            throws AccessControlException, RepositoryException {
        session.checkPermission(absPath, actions);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public ContentHandler getImportContentHandler(
            String parentAbsPath, int uuidBehaviour)
            throws PathNotFoundException, ConstraintViolationException,
            VersionException, LockException, RepositoryException {
        return session.getImportContentHandler(parentAbsPath, uuidBehaviour);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void importXML(
            String parentAbsPath, InputStream in, int uuidBehaviour)
            throws IOException, PathNotFoundException, ItemExistsException,
            ConstraintViolationException, VersionException,
            InvalidSerializedDataException, LockException, RepositoryException {
        session.importXML(parentAbsPath, in, uuidBehaviour);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void exportSystemView(
            String absPath, ContentHandler contentHandler,
            boolean binaryAsLink, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        session.exportSystemView(
                absPath, contentHandler, binaryAsLink, noRecurse);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void exportSystemView(
            String absPath, OutputStream out,
            boolean binaryAsLink, boolean noRecurse)
            throws IOException, PathNotFoundException, RepositoryException {
        session.exportSystemView(absPath, out, binaryAsLink, noRecurse);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void exportDocumentView(
            String absPath, ContentHandler contentHandler,
            boolean binaryAsLink, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        session.exportDocumentView(
                absPath, contentHandler, binaryAsLink, noRecurse);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void exportDocumentView(
            String absPath, OutputStream out,
            boolean binaryAsLink, boolean noRecurse) 
            throws IOException, PathNotFoundException, RepositoryException {
        session.exportDocumentView(absPath, out, binaryAsLink, noRecurse);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void setNamespacePrefix(String prefix, String uri)
            throws NamespaceException, RepositoryException {
        session.setNamespacePrefix(prefix, uri);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public String[] getNamespacePrefixes() throws RepositoryException {
        return session.getNamespacePrefixes();
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public String getNamespaceURI(String prefix) throws NamespaceException,
            RepositoryException {
        return session.getNamespaceURI(prefix);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public String getNamespacePrefix(String uri) throws NamespaceException,
            RepositoryException {
        return session.getNamespacePrefix(uri);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void logout() {
        session.logout();
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void addLockToken(String lt) {
        session.addLockToken(lt);
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public String[] getLockTokens() {
        return session.getLockTokens();
    }

    /**
     * Forwards the method call to the underlying session.
     */
    public void removeLockToken(String lt) {
        session.removeLockToken(lt);
    }

    public ValueFactory getValueFactory()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return factory.getValueFactoryDecorator(this, session.getValueFactory());
    }

    public boolean isLive() {
        return session.isLive();
    }
}
