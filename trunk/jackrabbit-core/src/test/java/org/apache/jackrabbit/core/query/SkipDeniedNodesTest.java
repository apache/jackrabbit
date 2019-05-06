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
package org.apache.jackrabbit.core.query;

import java.security.Principal;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.api.security.AbstractAccessControlTest;

/**
 * <code>SkipDeniedNodesTest</code> checks if nodes are correctly skipped
 * when a session does not have access to it.
 */
public class SkipDeniedNodesTest extends AbstractAccessControlTest {

    private Session anonymous;

    private Node n1;
    private Node n2;
    private Node n3;
    private Node n4;

    protected void setUp() throws Exception {
        super.setUp();
        anonymous = getHelper().getReadOnlySession();

        n1 = testRootNode.addNode(nodeName1);
        n1.setProperty(propertyName1, "a");
        n2 = testRootNode.addNode(nodeName2);
        n2.setProperty(propertyName1, "b");
        n3 = testRootNode.addNode(nodeName3);
        n3.setProperty(propertyName1, "c");
        n4 = testRootNode.addNode(nodeName4);
        n4.setProperty(propertyName1, "d");
        superuser.save();

        JackrabbitAccessControlList acl = getACL(n2.getPath());
        acl.addEntry(getPrincipal(anonymous), privilegesFromName(Privilege.JCR_READ) , false);

        acMgr.setPolicy(n2.getPath(), acl);
        superuser.save();
    }
    
    protected void tearDown() throws Exception {
        anonymous.logout();
        super.tearDown();
    }

    public void testSkipNodes() throws RepositoryException {
        Node testNode = (Node) anonymous.getItem(testRoot);
        checkSequence(testNode.getNodes(),
                new String[]{n1.getPath(), n3.getPath(), n4.getPath()});

        QueryManager qm = anonymous.getWorkspace().getQueryManager();
        String ntName = n1.getPrimaryNodeType().getName();
        String stmt = testPath + "/element(*, " + ntName + ") order by @" + propertyName1;
        QueryImpl query = (QueryImpl) qm.createQuery(stmt, Query.XPATH);
        query.setOffset(0);
        query.setLimit(2);
        checkSequence(query.execute().getNodes(), new String[]{n1.getPath(), n3.getPath()});

        query = (QueryImpl) qm.createQuery(stmt, Query.XPATH);
        query.setOffset(2);
        query.setLimit(2);
        checkSequence(query.execute().getNodes(), new String[]{n4.getPath()});
    }

    private void checkSequence(NodeIterator nodes, String[] paths)
            throws RepositoryException {
        for (int i = 0; i < paths.length; i++) {
            assertTrue("No more nodes, expected: " + paths[i], nodes.hasNext());
            assertEquals("Wrong sequence", paths[i], nodes.nextNode().getPath());
        }
        assertFalse("No more nodes expected", nodes.hasNext());
    }

    private static Principal getPrincipal(Session session) throws NotExecutableException {
        UserManager userMgr;
        if (!(session instanceof JackrabbitSession)) {
            throw new NotExecutableException();
        }
        try {
            userMgr = ((JackrabbitSession) session).getUserManager();
            User user = (User) userMgr.getAuthorizable(session.getUserID());
            if (user == null) {
                throw new NotExecutableException("cannot get user for userID : " + session.getUserID());
            }
            return user.getPrincipal();
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }
    }

    private JackrabbitAccessControlList getACL(String path) throws RepositoryException, AccessDeniedException, NotExecutableException {
        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);
        while (it.hasNext()) {
            AccessControlPolicy acp = it.nextAccessControlPolicy();
            if (acp instanceof JackrabbitAccessControlList) {
                return (JackrabbitAccessControlList) acp;
            }
        }
        throw new NotExecutableException("No JackrabbitAccessControlList found at " + path + " .");
    }
}
