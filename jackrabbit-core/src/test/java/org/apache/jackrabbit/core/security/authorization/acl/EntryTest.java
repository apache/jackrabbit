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
package org.apache.jackrabbit.core.security.authorization.acl;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.authorization.AbstractEvaluationTest;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>EntryTest</code>...
 */
public class EntryTest extends AbstractEvaluationTest {

    private String testPath;
    private JackrabbitAccessControlList acl;

    protected void setUp() throws Exception {
        super.setUp();
        testPath = testRootNode.getPath();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            acMgr.removePolicy(testPath, acl);
            superuser.save();
        } finally {
            super.tearDown();
        }
    }

    @Override
    protected boolean isExecutable() {
        return EvaluationUtil.isExecutable(acMgr);
    }

    @Override
    protected JackrabbitAccessControlList getPolicy(AccessControlManager acM, String path, Principal principal) throws RepositoryException, AccessDeniedException, NotExecutableException {
        return EvaluationUtil.getPolicy(acM, path, principal);
    }

    @Override
    protected Map<String, Value> getRestrictions(Session s, String path) {
        return Collections.emptyMap();
    }

    public void testIsLocal() throws NotExecutableException, RepositoryException {
        acl = getPolicy(acMgr, testPath, testUser.getPrincipal());
        modifyPrivileges(testPath, Privilege.JCR_READ, true);

        NodeImpl aclNode = (NodeImpl) superuser.getNode(acl.getPath() + "/rep:policy");
        List<Entry> entries = Entry.readEntries(aclNode, testRootNode.getPath());
        assertTrue(!entries.isEmpty());
        assertEquals(1, entries.size());

        Entry entry = entries.iterator().next();
        // false since acl has been created from path only -> no id
        assertTrue(entry.isLocal(((NodeImpl) testRootNode).getNodeId()));
        // false since internal id is null -> will never match.
        assertFalse(entry.isLocal(NodeId.randomId()));
    }

    public void testRestrictions() throws RepositoryException, NotExecutableException {
        // test if restrictions with expanded name are properly resolved
        Map<String, Value> restrictions = new HashMap<String,Value>();
        restrictions.put(ACLTemplate.P_GLOB.toString(), superuser.getValueFactory().createValue("*/test"));

        acl = getPolicy(acMgr, testPath, testUser.getPrincipal());
        acl.addEntry(testUser.getPrincipal(), new Privilege[] {acMgr.privilegeFromName(Privilege.JCR_ALL)}, true, restrictions);
        acMgr.setPolicy(testPath, acl);
        superuser.save();

        Map<String, Boolean> toMatch = new HashMap<String, Boolean>();
        toMatch.put(acl.getPath(), false);
        toMatch.put(acl.getPath() + "test", false);

        toMatch.put(acl.getPath() + "/test", true);
        toMatch.put(acl.getPath() + "/something/test", true);
        toMatch.put(acl.getPath() + "de/test", true);

        NodeImpl aclNode = (NodeImpl) superuser.getNode(acl.getPath() + "/rep:policy");
        List<Entry> entries = Entry.readEntries(aclNode, testRootNode.getPath());
        assertTrue(!entries.isEmpty());
        assertEquals(1, entries.size());

        Entry entry = entries.iterator().next();
        for (String str : toMatch.keySet()) {
            assertEquals("Path to match : " + str, toMatch.get(str).booleanValue(), entry.matches(str));
        }
    }
}