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
package org.apache.jackrabbit.server.remoting.davex;

import junit.framework.TestCase;

import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Repository;
import javax.jcr.Workspace;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Item;
import javax.jcr.PathNotFoundException;
import javax.jcr.ItemExistsException;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ValueFactory;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.NamespaceException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.version.VersionException;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.security.AccessControlException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * <code>JsonDiffHandlerTest</code>...
 */
public class JsonDiffHandlerTest extends TestCase {

    public void testGetItemPath() throws Exception {
        Map m = new HashMap();
        m.put("abc", "/reqPath/abc");
        m.put("abc/def/ghi", "/reqPath/abc/def/ghi");
        m.put("/abc", "/abc");
        m.put("/abc/def/ghi", "/abc/def/ghi");
        m.put(".", "/reqPath");
        m.put("./abc", "/reqPath/abc");
        m.put("abc/./def", "/reqPath/abc/def");
        m.put("/abc/./def", "/abc/def");
        m.put("..", "/");
        m.put("../abc/def", "/abc/def");
        m.put("abc/../def/.", "/reqPath/def");
        m.put("abc/../def/..", "/reqPath");
        m.put("abc/../def/..", "/reqPath");
        m.put("abc/../def/..", "/reqPath");
        m.put("abc/../def/..", "/reqPath");
        m.put("abc/../def/..", "/reqPath");
        m.put("abc/../def/..", "/reqPath");
        m.put("./././././", "/reqPath");
        m.put("/./././././", "/");
        m.put("/./abc/def/../ghi", "/abc/ghi");
        
        JsonDiffHandler handler = new JsonDiffHandler(new DummySession(), "/reqPath", null);
        for (Iterator it = m.keySet().iterator(); it.hasNext();) {
            String targetPath = it.next().toString();
            String expItemPath = m.get(targetPath).toString();
            assertEquals(expItemPath, handler.getItemPath(targetPath));
        }
    }

    private final class DummySession implements Session {

        public Repository getRepository() {
            return null;
        }

        public String getUserID() {
            return null;
        }

        public Object getAttribute(String name) {
            return null;
        }

        public String[] getAttributeNames() {
            return new String[0];
        }

        public Workspace getWorkspace() {
            return null;
        }

        public Session impersonate(Credentials credentials) throws LoginException, RepositoryException {
            return null;
        }

        public Node getRootNode() throws RepositoryException {
            return null;
        }

        public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException {
            return null;
        }

        public Item getItem(String absPath) throws PathNotFoundException, RepositoryException {
            return null;
        }

        public boolean itemExists(String absPath) throws RepositoryException {
            return false;
        }

        public void move(String srcAbsPath, String destAbsPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        }

        public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        }

        public void refresh(boolean keepChanges) throws RepositoryException {
        }

        public boolean hasPendingChanges() throws RepositoryException {
            return false;
        }

        public ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException {
            return null;
        }

        public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException {
        }

        public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, RepositoryException {
            return null;
        }

        public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException, RepositoryException {
        }

        public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
        }

        public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException {
        }

        public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
        }

        public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException {
        }

        public void setNamespacePrefix(String prefix, String uri) throws NamespaceException, RepositoryException {
        }

        public String[] getNamespacePrefixes() throws RepositoryException {
            return new String[0];
        }

        public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException {
            return null;
        }

        public String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException {
            return null;
        }

        public void logout() {
        }

        public boolean isLive() {
            return false;
        }

        public void addLockToken(String lt) {
        }

        public String[] getLockTokens() {
            return new String[0];
        }

        public void removeLockToken(String lt) {
        }

        public AccessControlManager getAccessControlManager()
                throws UnsupportedRepositoryOperationException,
                RepositoryException {
            // TODO Auto-generated method stub
            return null;
        }

        public Node getNode(String arg0) throws PathNotFoundException,
                RepositoryException {
            // TODO Auto-generated method stub
            return null;
        }

        public Node getNodeByIdentifier(String arg0)
                throws ItemNotFoundException, RepositoryException {
            // TODO Auto-generated method stub
            return null;
        }

        public Property getProperty(String arg0) throws PathNotFoundException,
                RepositoryException {
            // TODO Auto-generated method stub
            return null;
        }

        public RetentionManager getRetentionManager()
                throws UnsupportedRepositoryOperationException,
                RepositoryException {
            // TODO Auto-generated method stub
            return null;
        }

        public boolean hasCapability(String arg0, Object arg1, Object[] arg2)
                throws RepositoryException {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean hasPermission(String arg0, String arg1)
                throws RepositoryException {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean nodeExists(String arg0) throws RepositoryException {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean propertyExists(String arg0) throws RepositoryException {
            // TODO Auto-generated method stub
            return false;
        }

        public void removeItem(String arg0) throws VersionException,
                LockException, ConstraintViolationException,
                PathNotFoundException, RepositoryException {
            // TODO Auto-generated method stub
            
        }
    }
}