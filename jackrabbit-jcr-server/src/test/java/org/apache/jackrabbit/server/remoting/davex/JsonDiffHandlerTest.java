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
import org.xml.sax.ContentHandler;

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>JsonDiffHandlerTest</code>...
 */
public class JsonDiffHandlerTest extends TestCase {

    public void testGetItemPath() throws Exception {
        Map<String, String> m = new HashMap<String, String>();
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
        for (String targetPath : m.keySet()) {
            String expItemPath = m.get(targetPath);
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

        public Session impersonate(Credentials credentials) {
            return null;
        }

        public Node getRootNode() {
            return null;
        }

        public Node getNodeByUUID(String uuid) {
            return null;
        }

        public Item getItem(String absPath) {
            return null;
        }

        public boolean itemExists(String absPath) {
            return false;
        }

        public void move(String srcAbsPath, String destAbsPath) {
        }

        public void save() {
        }

        public void refresh(boolean keepChanges) {
        }

        public boolean hasPendingChanges() {
            return false;
        }

        public ValueFactory getValueFactory() {
            return null;
        }

        public void checkPermission(String absPath, String actions) throws AccessControlException {
        }

        public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) {
            return null;
        }

        public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) {
        }

        public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) {
        }

        public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) {
        }

        public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) {
        }

        public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) {
        }

        public void setNamespacePrefix(String prefix, String uri) {
        }

        public String[] getNamespacePrefixes() {
            return new String[0];
        }

        public String getNamespaceURI(String prefix) {
            return null;
        }

        public String getNamespacePrefix(String uri) {
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

        public AccessControlManager getAccessControlManager() {
            // TODO Auto-generated method stub
            return null;
        }

        public Node getNode(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        public Node getNodeByIdentifier(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        public Property getProperty(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        public RetentionManager getRetentionManager() {
            // TODO Auto-generated method stub
            return null;
        }

        public boolean hasCapability(String arg0, Object arg1, Object[] arg2) {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean hasPermission(String arg0, String arg1) {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean nodeExists(String arg0) {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean propertyExists(String arg0) {
            // TODO Auto-generated method stub
            return false;
        }

        public void removeItem(String arg0) {
            // TODO Auto-generated method stub
            
        }
    }
}
