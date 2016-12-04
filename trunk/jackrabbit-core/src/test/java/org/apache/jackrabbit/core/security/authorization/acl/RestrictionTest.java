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
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.core.security.authorization.AbstractEvaluationTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.value.StringValue;

/**
 * <code>ReadTest</code>...
 */
public class RestrictionTest extends AbstractEvaluationTest {

    private String path_root;
    private String path_a;
    private String path_b;
    private String path_c;
    private String path_d;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create some nodes below the test root in order to apply ac-stuff
        Node a = testRootNode.addNode("a", testNodeType);
        Node b = a.addNode("b", testNodeType);
        Node c = b.addNode("c", testNodeType);
        Node d = c.addNode("d", testNodeType);
        superuser.save();

        path_root = testRootNode.getPath();
        path_a = a.getPath();
        path_b = b.getPath();
        path_c = c.getPath();
        path_d = d.getPath();
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

    private void addEntry(String path, boolean grant, String restriction, String... privilegeNames) throws Exception {
        if (restriction.length() > 0) {
            Map<String, Value> rs = new HashMap<String, Value>();
            rs.put("rep:glob", new StringValue(restriction));
            modifyPrivileges(path, testUser.getPrincipal(), AccessControlUtils.privilegesFromNames(acMgr, privilegeNames), grant, rs);
        } else {
            modifyPrivileges(path, testUser.getPrincipal(), AccessControlUtils.privilegesFromNames(acMgr, privilegeNames), grant, Collections.<String, Value>emptyMap());
        }
    }

    /**
     * Tests if the restriction are active at the proper place
     */
    public void testHasPermissionWithRestrictions() throws Exception {
        // create permissions
        // allow rep:write      /testroot
        // deny  jcr:removeNode /testroot/a  glob=*/c
        // allow jcr:removeNode /testroot/a  glob=*/b
        // allow jcr:removeNode /testroot/a  glob=*/c/*

        addEntry(path_root, true, "", Privilege.JCR_READ, Privilege.JCR_WRITE);
        addEntry(path_a, false, "*/c", Privilege.JCR_REMOVE_NODE);
        addEntry(path_a, true, "*/b", Privilege.JCR_REMOVE_NODE);
        addEntry(path_a, true, "*/c/*", Privilege.JCR_REMOVE_NODE);

        Session testSession = getTestSession();
        try {
            AccessControlManager acMgr = getAccessControlManager(testSession);

            assertFalse("user should not have remove node on /a/b/c",
                    acMgr.hasPrivileges(path_c, AccessControlUtils.privilegesFromNames(acMgr, Privilege.JCR_REMOVE_NODE)));
            assertTrue("user should have remove node on /a/b",
                    acMgr.hasPrivileges(path_b, AccessControlUtils.privilegesFromNames(acMgr, Privilege.JCR_REMOVE_NODE)));
            assertTrue("user should have remove node on /a/b/c/d",
                    acMgr.hasPrivileges(path_d, AccessControlUtils.privilegesFromNames(acMgr, Privilege.JCR_REMOVE_NODE)));

            // should be able to remove /a/b/c/d
            testSession.getNode(path_d).remove();
            testSession.save();

            try {
                testSession.getNode(path_c).remove();
                testSession.save();
                fail("removing node on /a/b/c should fail");
            } catch (RepositoryException e) {
                // all ok
            }
        } finally {
            testSession.logout();
        }
    }

}