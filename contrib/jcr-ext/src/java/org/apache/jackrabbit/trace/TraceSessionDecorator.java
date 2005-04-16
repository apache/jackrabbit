/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.trace;

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
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.decorator.DecoratorFactory;
import org.apache.jackrabbit.decorator.SessionDecorator;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * TODO
 */
public class TraceSessionDecorator extends SessionDecorator implements Session {

    private TraceLogger logger;
    
    public TraceSessionDecorator(DecoratorFactory factory,
            Repository repository, Session session, TraceLogger logger) {
        super(factory, repository, session);
        this.logger = logger;
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public void addLockToken(String lt) {
        logger.trace("addLockToken", lt);
        super.addLockToken(lt);
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public void checkPermission(String absPath, String actions)
            throws AccessControlException {
        logger.trace("checkPermissions", absPath, actions);
        super.checkPermission(absPath, actions);
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public void exportDocView(String absPath, ContentHandler contentHandler,
            boolean binaryAsLink, boolean noRecurse)
            throws InvalidSerializedDataException, PathNotFoundException,
            SAXException, RepositoryException {
        logger.trace("exportDocView", absPath, contentHandler, binaryAsLink, noRecurse);
        super.exportDocView(absPath, contentHandler, binaryAsLink, noRecurse);
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public void exportDocView(String absPath, OutputStream out,
            boolean binaryAsLink, boolean noRecurse)
            throws InvalidSerializedDataException, IOException,
            PathNotFoundException, RepositoryException {
        logger.trace("exportDocView", absPath, out, binaryAsLink, noRecurse);
        super.exportDocView(absPath, out, binaryAsLink, noRecurse);
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public void exportSysView(String absPath, ContentHandler contentHandler,
            boolean binaryAsLink, boolean noRecurse)
            throws PathNotFoundException, SAXException, RepositoryException {
        logger.trace("exportSysView", absPath, contentHandler, binaryAsLink, noRecurse);
        super.exportSysView(absPath, contentHandler, binaryAsLink, noRecurse);
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public void exportSysView(String absPath, OutputStream out,
            boolean binaryAsLink, boolean noRecurse) throws IOException,
            PathNotFoundException, RepositoryException {
        logger.trace("exportSysView", absPath, out, binaryAsLink, noRecurse);
        super.exportSysView(absPath, out, binaryAsLink, noRecurse);
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public Object getAttribute(String name) {
        logger.trace("getAttribute", name);
        return super.getAttribute(name);
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public String[] getAttributeNames() {
        logger.trace("getAttributeNames");
        return super.getAttributeNames();
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public ContentHandler getImportContentHandler(String parentAbsPath)
            throws PathNotFoundException, ConstraintViolationException,
            VersionException, LockException, RepositoryException {
        logger.trace("getImportContentHandler", parentAbsPath);
        return super.getImportContentHandler(parentAbsPath);
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public Item getItem(String absPath) throws PathNotFoundException,
            RepositoryException {
        logger.trace("getItem", absPath);
        return super.getItem(absPath);
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public String[] getLockTokens() {
        logger.trace("getLockTokens");
        return super.getLockTokens();
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public String getNamespacePrefix(String uri) throws NamespaceException,
            RepositoryException {
        logger.trace("getNamespacePrefix", uri);
        return super.getNamespacePrefix(uri);
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public String[] getNamespacePrefixes() throws RepositoryException {
        logger.trace("getNamespacePrefixes");
        return super.getNamespacePrefixes();
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public String getNamespaceURI(String prefix) throws NamespaceException,
            RepositoryException {
        logger.trace("getNamespaceURI", prefix);
        return super.getNamespaceURI(prefix);
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public Node getNodeByUUID(String uuid) throws ItemNotFoundException,
            RepositoryException {
        logger.trace("getNodeByUUID", uuid);
        return super.getNodeByUUID(uuid);
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public Repository getRepository() {
        logger.trace("getRepository");
        return super.getRepository();
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public Node getRootNode() throws RepositoryException {
        logger.trace("getRootNode");
        return super.getRootNode();
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public String getUserId() {
        logger.trace("getUserId");
        return super.getUserId();
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public Workspace getWorkspace() {
        logger.trace("getWorkspace");
        return super.getWorkspace();
    }

    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public boolean hasPendingChanges() throws RepositoryException {
        logger.trace("hasPendingChanges");
        return super.hasPendingChanges();
    }

    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public Session impersonate(Credentials credentials) throws LoginException,
            RepositoryException {
        logger.trace("impersonate", credentials);
        return super.impersonate(credentials);
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public void importXML(String parentAbsPath, InputStream in)
            throws IOException, PathNotFoundException, ItemExistsException,
            ConstraintViolationException, VersionException,
            InvalidSerializedDataException, LockException, RepositoryException {
        logger.trace("importXML", parentAbsPath, in);
        super.importXML(parentAbsPath, in);
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public boolean itemExists(String path) {
        logger.trace("itemExists", path);
        return super.itemExists(path);
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public void logout() {
        logger.trace("logout");
        super.logout();
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public void move(String srcAbsPath, String destAbsPath)
            throws ItemExistsException, PathNotFoundException,
            VersionException, RepositoryException {
        logger.trace("move", srcAbsPath, destAbsPath);
        super.move(srcAbsPath, destAbsPath);
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public void refresh(boolean keepChanges) throws RepositoryException {
        logger.trace("refresh", keepChanges);
        super.refresh(keepChanges);
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public void removeLockToken(String lt) {
        logger.trace("removeLockToken", lt);
        super.removeLockToken(lt);
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public void save() throws AccessDeniedException,
            ConstraintViolationException, InvalidItemStateException,
            VersionException, LockException, RepositoryException {
        logger.trace("save");
        super.save();
    }
    
    /**
     * Logs the method call and forwards it to the underlying session.
     * {@inheritDoc}
     */
    public void setNamespacePrefix(String prefix, String uri)
            throws NamespaceException, RepositoryException {
        logger.trace("setNamespacePrefix", prefix, uri);
        super.setNamespacePrefix(prefix, uri);
    }
    
}
