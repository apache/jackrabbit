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

import java.util.ArrayList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Tests for {@code JsonDiffHandler} that trigger the import mode.
 */
public class JsonDiffHandlerImportTest extends AbstractJCRTest {

    private static final String JSOP_POLICY_TREE = "+rep:policy : {"
            + "\"jcr:primaryType\" : \"rep:ACL\","
            + "\"allow\" : {" + "\"jcr:primaryType\" : \"rep:GrantACE\","
            + "\"rep:principalName\" : \"everyone\","
            + "\"rep:privileges\" : [\"jcr:write\"]" + "}" + "}";

    private static final List<String> ADD_NODES = new ArrayList<String>();
    static {
        ADD_NODES.add(
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
                        +"}"+"}"+"}");
        ADD_NODES.add(
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
                        +"}"+"}");

    }

    private AccessControlManager acMgr;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        acMgr = superuser.getAccessControlManager();
    }

    private static void assertPolicy(AccessControlManager acMgr, Node targetNode, int noACEs) throws RepositoryException {
        AccessControlPolicy[] policies = acMgr.getPolicies(targetNode.getPath());
        assertEquals(policies.length, 1);

        AccessControlPolicy acl = policies[0];
        assertTrue(acl instanceof JackrabbitAccessControlList);
        AccessControlEntry[] entries = ((JackrabbitAccessControlList) acl).getAccessControlEntries();
        assertEquals(noACEs, entries.length);
    }

    /**
     * Test two subsequent DIFF strings with policies, thus multiple addNode operations.
     */
    public void testMultipleAddNodeOperations() throws Exception {
        for(String jsonString : ADD_NODES) {
            JsonDiffHandler h = new JsonDiffHandler(superuser, testRoot, null);
            new DiffParser(h).parse(jsonString);
        }

        assertPolicy(acMgr, testRootNode.getNode("node1"), 1);
        assertPolicy(acMgr, testRootNode.getNode("node2"), 2);
    }

    /**
     * Test adding 'rep:policy' policy node as a child node of /testroot without
     * intermediate node.
     */
    public void testAllPolicyNode() throws Exception {
        try {
            testRootNode.addMixin("rep:AccessControllable");

            JsonDiffHandler handler = new JsonDiffHandler(superuser, testRoot, null);
            new DiffParser(handler).parse(JSOP_POLICY_TREE);

            assertTrue(testRootNode.hasNode("rep:policy"));
            assertTrue(testRootNode.getNode("rep:policy").getDefinition().isProtected());

            assertTrue(testRootNode.getNode("rep:policy").getPrimaryNodeType()
                    .getName().equals("rep:ACL"));

            assertPolicy(acMgr, testRootNode, 1);

            AccessControlEntry entry = ((AccessControlList) acMgr.getPolicies(testRoot)[0]).getAccessControlEntries()[0];
            assertEquals(EveryonePrincipal.NAME, entry.getPrincipal().getName());
            assertEquals(1, entry.getPrivileges().length);
            assertEquals(acMgr.privilegeFromName(Privilege.JCR_WRITE), entry.getPrivileges()[0]);

            if (entry instanceof JackrabbitAccessControlEntry) {
                assertTrue(((JackrabbitAccessControlEntry) entry).isAllow());
            }

        } finally {
            superuser.refresh(false);
        }
    }

    /**
     * Test adding 'rep:policy' policy node as a child node of /testroot without
     * intermediate node.
     */
    public void testUpdatePolicyNode() throws Exception {
        try {
            AccessControlUtils.addAccessControlEntry(superuser, testRoot, EveryonePrincipal.getInstance(), new String[] {Privilege.JCR_READ}, false);

            JsonDiffHandler handler = new JsonDiffHandler(superuser, testRoot, null);
            new DiffParser(handler).parse(JSOP_POLICY_TREE);

            assertTrue(testRootNode.hasNode("rep:policy"));
            assertTrue(testRootNode.getNode("rep:policy").getDefinition().isProtected());

            assertTrue(testRootNode.getNode("rep:policy").getPrimaryNodeType()
                    .getName().equals("rep:ACL"));

            assertPolicy(acMgr, testRootNode, 1);

            AccessControlEntry entry = ((AccessControlList) acMgr.getPolicies(testRoot)[0]).getAccessControlEntries()[0];
            assertEquals(EveryonePrincipal.NAME, entry.getPrincipal().getName());
            assertEquals(1, entry.getPrivileges().length);
            assertEquals(acMgr.privilegeFromName(Privilege.JCR_WRITE), entry.getPrivileges()[0]);

            if (entry instanceof JackrabbitAccessControlEntry) {
                assertTrue(((JackrabbitAccessControlEntry) entry).isAllow());
            }

        } finally {
            superuser.refresh(false);
        }
    }
}