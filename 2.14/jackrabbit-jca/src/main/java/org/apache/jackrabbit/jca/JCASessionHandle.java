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
package org.apache.jackrabbit.jca;

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
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.version.VersionException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * This class implements the JCA implementation of session.
 */
public final class JCASessionHandle implements Session, XAResource {

    /**
     * Managed connection.
     */
    private JCAManagedConnection mc;

    /**
     * Construct a new session.
     */
    public JCASessionHandle(JCAManagedConnection mc) {
        this.mc = mc;
    }

    /**
     * Return the managed connection.
     */
    public JCAManagedConnection getManagedConnection() {
        return mc;
    }

    /**
     * Set the managed connection.
     */
    public void setManagedConnection(JCAManagedConnection mc) {
        this.mc = mc;
    }

    /**
     * Return the session.
     */
    private Session getSession() {
        return mc.getSession(this);
    }

    /**
     * Return the repository.
     */
    public Repository getRepository() {
        return getSession().getRepository();
    }

    /**
     * Return the user id.
     */
    public String getUserID() {
        return getSession().getUserID();
    }

    /**
     * Return the attribute.
     */
    public Object getAttribute(String name) {
        return getSession().getAttribute(name);
    }

    /**
     * Return the attribute names.
     */
    public String[] getAttributeNames() {
        return getSession().getAttributeNames();
    }

    /**
     * Return the workspace.
     */
    public Workspace getWorkspace() {
        return getSession().getWorkspace();
    }

    /**
     * Impersonate another user.
     */
    public Session impersonate(Credentials cred)
            throws LoginException, RepositoryException {
        throw new RepositoryException("impersonate(..) not supported in managed environment");
    }

    /**
     * Return the root node.
     */
    public Node getRootNode()
            throws RepositoryException {
        return getSession().getRootNode();
    }

    /**
     * Return node by UUID.
     */
    @SuppressWarnings("deprecation")
    public Node getNodeByUUID(String uuid)
            throws ItemNotFoundException, RepositoryException {
        return getSession().getNodeByUUID(uuid);
    }

    /**
     * Return the item.
     */
    public Item getItem(String arg0)
            throws PathNotFoundException, RepositoryException {
        return getSession().getItem(arg0);
    }

    /**
     * Return true if item exists.
     */
    public boolean itemExists(String arg0)
            throws RepositoryException {
        return getSession().itemExists(arg0);
    }

    /**
     * Move the item.
     */
    public void move(String arg0, String arg1)
            throws ItemExistsException, PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, RepositoryException {
        getSession().move(arg0, arg1);
    }

    /**
     * Save the session.
     */
    public void save()
            throws AccessDeniedException, ItemExistsException,
            ConstraintViolationException, InvalidItemStateException, VersionException,
            LockException, NoSuchNodeTypeException, RepositoryException {
        getSession().save();
    }

    /**
     * Refresh the session.
     */
    public void refresh(boolean arg0)
            throws RepositoryException {
        getSession().refresh(arg0);
    }

    /**
     * Return true if it has pending changes.
     */
    public boolean hasPendingChanges()
            throws RepositoryException {
        return getSession().hasPendingChanges();
    }

    /**
     * Return the value factory.
     */
    public ValueFactory getValueFactory()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        return getSession().getValueFactory();
    }

    /**
     * Check permission.
     */
    public void checkPermission(String arg0, String arg1)
            throws AccessControlException, RepositoryException {
        getSession().checkPermission(arg0, arg1);
    }

    /**
     * Return the import content handler.
     */
    public ContentHandler getImportContentHandler(String arg0, int arg1)
            throws PathNotFoundException, ConstraintViolationException, VersionException,
            LockException, RepositoryException {
        return getSession().getImportContentHandler(arg0, arg1);
    }

    /**
     * Import XML content.
     */
    public void importXML(String arg0, InputStream arg1, int arg2)
            throws IOException, PathNotFoundException, ItemExistsException,
            ConstraintViolationException, VersionException, InvalidSerializedDataException,
            LockException, RepositoryException {
        getSession().importXML(arg0, arg1, arg2);
    }

    /**
     * Export system view.
     */
    public void exportSystemView(String arg0, ContentHandler arg1, boolean arg2, boolean arg3)
            throws PathNotFoundException, SAXException, RepositoryException {
        getSession().exportSystemView(arg0, arg1, arg2, arg3);
    }

    /**
     * Export system view.
     */
    public void exportSystemView(String arg0, OutputStream arg1, boolean arg2, boolean arg3)
            throws IOException, PathNotFoundException, RepositoryException {
        getSession().exportSystemView(arg0, arg1, arg2, arg3);
    }

    /**
     * Export document view.
     */
    public void exportDocumentView(String arg0, ContentHandler arg1, boolean arg2, boolean arg3)
            throws PathNotFoundException, SAXException, RepositoryException {
        getSession().exportDocumentView(arg0, arg1, arg2, arg3);
    }

    /**
     * Export document view.
     */
    public void exportDocumentView(String arg0, OutputStream arg1, boolean arg2, boolean arg3)
            throws IOException, PathNotFoundException, RepositoryException {
        getSession().exportDocumentView(arg0, arg1, arg2, arg3);
    }

    /**
     * Set namespace prefix.
     */
    public void setNamespacePrefix(String arg0, String arg1)
            throws NamespaceException, RepositoryException {
        getSession().setNamespacePrefix(arg0, arg1);
    }

    /**
     * Return namespace prefixes.
     */
    public String[] getNamespacePrefixes()
            throws RepositoryException {
        return getSession().getNamespacePrefixes();
    }

    /**
     * Return namespace URI.
     */
    public String getNamespaceURI(String arg0)
            throws NamespaceException, RepositoryException {
        return getSession().getNamespaceURI(arg0);
    }

    /**
     * Return namespace prefix.
     */
    public String getNamespacePrefix(String arg0)
            throws NamespaceException, RepositoryException {
        return getSession().getNamespacePrefix(arg0);
    }

    /**
     * Logout the session.
     */
    public void logout() {
        mc.closeHandle(this);
    }

    /**
     * Return true if session is live.
     */
    public boolean isLive() {
        return getSession().isLive();
    }

    /**
     * Add lock token.
     */
    @SuppressWarnings("deprecation")
    public void addLockToken(String arg0) {
        getSession().addLockToken(arg0);
    }

    /**
     * Return the lock tokens.
     */
    @SuppressWarnings("deprecation")
    public String[] getLockTokens() {
        return getSession().getLockTokens();
    }

    /**
     * Remove lock token.
     */
    @SuppressWarnings("deprecation")
    public void removeLockToken(String arg0) {
        getSession().removeLockToken(arg0);
    }

    public AccessControlManager getAccessControlManager()
            throws RepositoryException {
        return getSession().getAccessControlManager();
    }

    public Node getNode(String arg0) throws RepositoryException {
        return getSession().getNode(arg0);
    }

    public Node getNodeByIdentifier(String arg0) throws RepositoryException {
        return getSession().getNodeByIdentifier(arg0);
    }

    public Property getProperty(String arg0) throws RepositoryException {
        return getSession().getProperty(arg0);
    }

    public RetentionManager getRetentionManager()
            throws RepositoryException {
        return getSession().getRetentionManager();
    }

    public boolean hasCapability(String arg0, Object arg1, Object[] arg2)
            throws RepositoryException {
        return getSession().hasCapability(arg0, arg1, arg2);
    }

    public boolean hasPermission(String arg0, String arg1)
            throws RepositoryException {
        return getSession().hasPermission(arg0, arg1);
    }

    public boolean nodeExists(String path) throws RepositoryException {
        return getSession().nodeExists(path);
    }

    public boolean propertyExists(String path) throws RepositoryException {
        return getSession().propertyExists(path);
    }

    public void removeItem(String path) throws RepositoryException {
        getSession().removeItem(path);
    }

    //--------------------------------------------------------< XAResource >--

    private XAResource getXAResource() throws XAException {
        Session session = getSession();
        if (session instanceof XAResource) {
            return (XAResource) session;
        } else {
            throw new XAException(
                    "XA transactions are not supported with " + session);
        }
    }

    public void start(Xid xid, int flags) throws XAException {
        getXAResource().start(xid, flags);
    }

    public void end(Xid xid, int flags) throws XAException {
        getXAResource().end(xid, flags);
    }

    public int prepare(Xid xid) throws XAException {
        return getXAResource().prepare(xid);
    }

    public void rollback(Xid xid) throws XAException {
        getXAResource().rollback(xid);
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        getXAResource().commit(xid, onePhase);
    }

    public void forget(Xid xid) throws XAException {
        getXAResource().forget(xid);
    }

    public Xid[] recover(int flag) throws XAException {
        return getXAResource().recover(flag);
    }

    public boolean isSameRM(XAResource xares) throws XAException {
        if (xares instanceof JCASessionHandle) {
            xares = ((JCASessionHandle) xares).getXAResource();
        }
        return getXAResource().isSameRM(xares);
    }

    public int getTransactionTimeout() throws XAException {
        return getXAResource().getTransactionTimeout();
    }

    public boolean setTransactionTimeout(int seconds) throws XAException {
        return getXAResource().setTransactionTimeout(seconds);
    }

}
