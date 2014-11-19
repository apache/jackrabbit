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


import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.JUnitTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.RepositoryHelper;
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
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * <code>JsonDiffHandlerTest</code>...
 */
public class JsonDiffHandlerTest extends JUnitTest {

    private static final String JSOP_POLICY_TREE = "+test : { "
            + "\"jcr:primaryType\" : \"nt:unstructured\","
            + "\"jcr:mixinTypes\" : [\"rep:AccessControllable\",\"mix:versionable\"],"
            + "\"jcr:uuid\" : \"0a0ca2e9-ab98-4433-a12b-d57283765207\","
            + "\"jcr:baseVersion\" : \"35d0d137-a3a4-4af3-8cdd-ce565ea6bdc9\","
            + "\"jcr:isCheckedOut\" : \"true\","
            + "\"jcr:predecessors\" : \"35d0d137-a3a4-4af3-8cdd-ce565ea6bdc9\","
            + "\"jcr:versionHistory\" : \"428c9ef2-78e5-4f1c-95d3-16b4ce72d815\","
            + "\"rep:policy\" : {" + "\"jcr:primaryType\" : \"rep:ACL\","
            + "\"allow\" : {" + "\"jcr:primaryType\" : \"rep:GrantACE\","
            + "\"rep:principalName\" : \"everyone\","
            + "\"rep:privileges\" : [\"jcr:write\"]" + "}" + "}" + "}";
    private static final String[] ADD_NODES = {
        "+node1 : {"
        +"\"jcr:primaryType\" : \"nt:file\","
        + "\"jcr:mixinTypes\" : [\"rep:AccessControllable\"],"
        +"\"jcr:uuid\" : \"0a0ca2e9-ab98-4433-a12b-d57283765207\","
        +"\"rep:policy\" : {"
        +"\"jcr:primaryType\" : \"rep:ACL\","
        +"\"deny0\" : {"
        +"\"jcr:primaryType\" : \"rep:DenyACE\","
        +"\"rep:principalName\" : \"everyone\","
        +"\"rep:privileges\" : [\"jcr:read\"]"
        +"}"+"}"+"}",
        "+node2 : {"
        +"\"jcr:primaryType\" : \"nt:unstructured\","
        + "\"jcr:mixinTypes\" : [\"rep:AccessControllable\"],"
        +"\"rep:policy\" : {"
        +"\"jcr:primaryType\" : \"rep:ACL\","
        +"\"allow\" : {"
        +"\"jcr:primaryType\" : \"rep:GrantACE\","
        +"\"rep:principalName\" : \"everyone\","
        +"\"rep:privileges\" : [\"jcr:read\"]"
        +"},"
        +"\"deny\" : {"
        +"\"jcr:primaryType\" : \"rep:DenyACE\","
        +"\"rep:principalName\" : \"everyone\","
        +"\"rep:privileges\" : [\"jcr:write\"]"
        +"}"
        +"}"+"}"
        
    };
    private SessionImpl sImpl;
    String testRoot = "/testroot";
    private NodeImpl target;
    RepositoryHelper helper;
    private AccessControlManager acMgr;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        sImpl = (SessionImpl) helper.getSuperuserSession();
        acMgr = sImpl.getAccessControlManager();
        target = (NodeImpl) sImpl.getRootNode();
    }
    
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

    /*
      Single DIFF string with multiple addNode operations.
     */    
    public void testMultipleAddNodeOperations() throws Exception {

        for(int i = 0; i<ADD_NODES.length; i++) {
            JsonDiffHandler h = new JsonDiffHandler(sImpl, target.getPath(), null);
            new DiffParser(h).parse(ADD_NODES[i]);
        }

        Node node1 = target.getNode("node1");
        Node node2 = target.getNode("node2");

        String aclPath = node1.getPath();
        AccessControlPolicy[] policy1 = acMgr.getPolicies(aclPath);
        assertEquals(policy1.length, 1);
        AccessControlEntry[] entries = ((JackrabbitAccessControlList)policy1[0]).getAccessControlEntries();
        assertEquals(entries.length, 1);

        aclPath = node2.getPath();
        AccessControlPolicy[] policy2 = acMgr.getPolicies(aclPath);
        assertEquals(policy2.length, 1);
        AccessControlEntry[] entries2 = ((JackrabbitAccessControlList)policy2[0]).getAccessControlEntries();
        assertEquals(entries2.length, 2);
    }

    /*
      Test the SetProperty DIFF operation.
     */
    public void testSetProtectedProperty() throws Exception{
        String diff = ADD_NODES[0];
        JsonDiffHandler h = new JsonDiffHandler(sImpl, target.getPath(), null);
        
    }
    
    /*
       Test adding 'rep:policy' policy node as a child node of /testroot/test.
     */
/*    public void testRepPolicyNodeImport() throws Exception {
        try {

            JsonDiffHandler handler = new JsonDiffHandler(sImpl,
                    target.getPath(), null);
            new DiffParser(handler).parse(JSOP_POLICY_TREE);

            assertTrue(target.hasNode("test"));
            Node test = target.getNode("test");
            assertTrue(test.hasNode("rep:policy"));
            assertTrue(test.getNode("rep:policy").getDefinition().isProtected());

            assertTrue(test.getNode("rep:policy").getPrimaryNodeType()
                    .getName().equals("rep:ACL"));

            String path = target.getNode("test").getPath(); // testroot/test

            // retrieves the rep:ACL policy defined at the rep:policy node
            AccessControlPolicy[] policies = acMgr.getPolicies(path);
            assertEquals(1, policies.length);
            assertTrue(policies[0] instanceof JackrabbitAccessControlList);

            AccessControlEntry[] entries = ((JackrabbitAccessControlList) policies[0])
                    .getAccessControlEntries();
            assertEquals(1, entries.length);

            AccessControlEntry entry = entries[0];
            assertEquals("everyone", entry.getPrincipal().getName());
            assertEquals(1, entry.getPrivileges().length);
            assertEquals(acMgr.privilegeFromName(Privilege.JCR_WRITE),
                    entry.getPrivileges()[0]);

            if (entry instanceof JackrabbitAccessControlEntry) {
                assertTrue(((JackrabbitAccessControlEntry) entry).isAllow());
            }

        } finally {
            superuser.refresh(false);
        }
    }*/
    
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
