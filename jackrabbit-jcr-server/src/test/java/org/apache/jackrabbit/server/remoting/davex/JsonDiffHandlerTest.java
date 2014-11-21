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

        @Override
        public Repository getRepository() {
            return null;
        }

        @Override
        public String getUserID() {
            return null;
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }

        @Override
        public String[] getAttributeNames() {
            return new String[0];
        }

        @Override
        public Workspace getWorkspace() {
            return null;
        }

        @Override
        public Session impersonate(Credentials credentials) {
            return null;
        }

        @Override
        public Node getRootNode() {
            return null;
        }

        @Override
        public Node getNodeByUUID(String uuid) {
            return null;
        }

        @Override
        public Item getItem(String absPath) {
            return null;
        }

        @Override
        public boolean itemExists(String absPath) {
            return false;
        }

        @Override
        public void move(String srcAbsPath, String destAbsPath) {
        }

        @Override
        public void save() {
        }

        @Override
        public void refresh(boolean keepChanges) {
        }

        @Override
        public boolean hasPendingChanges() {
            return false;
        }

        @Override
        public ValueFactory getValueFactory() {
            return null;
        }

        @Override
        public void checkPermission(String absPath, String actions) throws AccessControlException {
        }

        @Override
        public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) {
            return null;
        }

        @Override
        public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) {
        }

        @Override
        public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) {
        }

        @Override
        public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) {
        }

        @Override
        public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) {
        }

        @Override
        public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) {
        }

        @Override
        public void setNamespacePrefix(String prefix, String uri) {
        }

        @Override
        public String[] getNamespacePrefixes() {
            return new String[0];
        }

        @Override
        public String getNamespaceURI(String prefix) {
            return null;
        }

        @Override
        public String getNamespacePrefix(String uri) {
            return null;
        }

        @Override
        public void logout() {
        }

        @Override
        public boolean isLive() {
            return false;
        }

        @Override
        public void addLockToken(String lt) {
        }

        @Override
        public String[] getLockTokens() {
            return new String[0];
        }

        @Override
        public void removeLockToken(String lt) {
        }

        @Override
        public AccessControlManager getAccessControlManager() {
            return null;
        }

        @Override
        public Node getNode(String arg0) {
            return null;
        }

        @Override
        public Node getNodeByIdentifier(String arg0) {
            return null;
        }

        @Override
        public Property getProperty(String arg0) {
            return null;
        }

        @Override
        public RetentionManager getRetentionManager() {
            return null;
        }

        @Override
        public boolean hasCapability(String arg0, Object arg1, Object[] arg2) {
            return false;
        }

        @Override
        public boolean hasPermission(String arg0, String arg1) {
            return false;
        }

        @Override
        public boolean nodeExists(String arg0) {
            return false;
        }

        @Override
        public boolean propertyExists(String arg0) {
            return false;
        }

        @Override
        public void removeItem(String arg0) {

        }
    }
}
