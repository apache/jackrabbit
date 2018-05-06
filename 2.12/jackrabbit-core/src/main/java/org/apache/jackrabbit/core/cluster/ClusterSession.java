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
package org.apache.jackrabbit.core.cluster;

import java.io.InputStream;
import java.io.OutputStream;

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

import org.xml.sax.ContentHandler;

/**
 * Represents the session that has made some changes on another node in the
 * cluster. The only method currently implemented is {@link #getUserID()}.
 */
class ClusterSession implements Session {

    /**
     * User id to represent.
     */
    private final String userId;

    /**
     * Create a new instance of this class.
     *
     * @param userId user id
     */
    public ClusterSession(String userId) {
        this.userId = userId;
    }

    /**
     * Returns <code>true</code> if the given <code>userId</code> is the same as
     * the {@link #userId} of this session.
     *
     * @param userId the user id or <code>null</code>.
     * @return <code>true</code> if they are the same; <code>false</code>
     *         otherwise.
     */
    boolean isUserId(String userId) {
        if (userId == null) {
            return this.userId == null;
        } else {
            return userId.equals(this.userId);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getUserID() {
        return userId;
    }

    /**
     * {@inheritDoc}
     */
    public Repository getRepository() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Object getAttribute(String s) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getAttributeNames() {
        return new String[0];
    }

    /**
     * {@inheritDoc}
     */
    public Workspace getWorkspace() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Session impersonate(Credentials credentials) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Node getRootNode() throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Node getNodeByUUID(String s) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Item getItem(String s) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean itemExists(String s) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void move(String s, String s1) throws UnsupportedRepositoryOperationException {

        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void save() throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void refresh(boolean b) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasPendingChanges() throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void checkPermission(String s, String s1) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public ContentHandler getImportContentHandler(String s, int i) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void importXML(String s, InputStream inputStream, int i) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void exportSystemView(String s, ContentHandler contentHandler, boolean b, boolean b1)
            throws UnsupportedRepositoryOperationException {

        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void exportSystemView(String s, OutputStream outputStream, boolean b, boolean b1)
            throws UnsupportedRepositoryOperationException {

        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void exportDocumentView(String s, ContentHandler contentHandler, boolean b, boolean b1)
            throws UnsupportedRepositoryOperationException {

        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void exportDocumentView(String s, OutputStream outputStream, boolean b, boolean b1)
            throws UnsupportedRepositoryOperationException {

        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void setNamespacePrefix(String s, String s1) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getNamespacePrefixes() throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespaceURI(String s) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespacePrefix(String s) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void logout() {
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLive() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void addLockToken(String s) {
    }

    /**
     * {@inheritDoc}
     */
    public String[] getLockTokens() {
        return new String[0];
    }

    /**
     * {@inheritDoc}
     */
    public void removeLockToken(String s) {
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj instanceof ClusterSession) {
            ClusterSession other = (ClusterSession) obj;
            return isUserId(other.userId);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return userId.hashCode();
    }

    public AccessControlManager getAccessControlManager()
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public Node getNode(String path) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public Node getNodeByIdentifier(String identifier)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public Property getProperty(String arg0) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public RetentionManager getRetentionManager()
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public boolean hasCapability(String arg0, Object arg1, Object[] arg2)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public boolean hasPermission(String arg0, String arg1)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public boolean nodeExists(String path) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public boolean propertyExists(String path) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    public void removeItem(String path) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

}
