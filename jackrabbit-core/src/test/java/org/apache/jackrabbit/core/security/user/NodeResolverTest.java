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
package org.apache.jackrabbit.core.security.user;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.util.Iterator;

/** <code>NodeResolverTest</code>... */
public abstract class NodeResolverTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(NodeResolverTest.class);

    NodeResolver nodeResolver;

    protected void setUp() throws Exception {
        super.setUp();

        nodeResolver = createNodeResolver(superuser);
    }

    private static UserImpl getCurrentUser(Session session) throws NotExecutableException, RepositoryException {
        if (!(session instanceof JackrabbitSession)) {
            throw new NotExecutableException();
        }
        try {
            UserManager uMgr = ((JackrabbitSession) session).getUserManager();
            String uid = session.getUserID();
            if (uid != null) {
                Authorizable auth = uMgr.getAuthorizable(session.getUserID());
                if (auth != null && auth instanceof UserImpl) {
                    return (UserImpl) auth;
                }
            }
        } catch (RepositoryException e) {
            // ignore
        }
        // unable to retrieve current user
        throw new NotExecutableException();
    }

    protected abstract NodeResolver createNodeResolver(Session session) throws RepositoryException, NotExecutableException;

    public void testFindNode() throws NotExecutableException, RepositoryException {
        UserImpl currentUser = getCurrentUser(superuser);

        NodeResolver nr = createNodeResolver(currentUser.getNode().getSession());

        Node result = nr.findNode(currentUser.getNode().getQName(), UserConstants.NT_REP_USER);
        assertNotNull(result);
        assertTrue(currentUser.getNode().isSame(result));

        result = nr.findNode(currentUser.getNode().getQName(), UserConstants.NT_REP_AUTHORIZABLE);
        assertNotNull(result);
        assertTrue(currentUser.getNode().isSame(result));

        result = nr.findNode(currentUser.getNode().getQName(), UserConstants.NT_REP_GROUP);
        assertNull(result);

        Iterator it = currentUser.memberOf();
        while (it.hasNext()) {
            GroupImpl gr = (GroupImpl) it.next();

            result = nr.findNode(gr.getNode().getQName(), UserConstants.NT_REP_GROUP);
            assertNotNull(result);
            assertTrue(gr.getNode().isSame(result));

            result = nr.findNode(gr.getNode().getQName(), UserConstants.NT_REP_AUTHORIZABLE);
            assertNotNull(result);
            assertTrue(gr.getNode().isSame(result));

            result = nr.findNode(gr.getNode().getQName(), UserConstants.NT_REP_USER);
            assertNull(result);
        }
    }

    public void testFindNodeByPrincipalName() throws NotExecutableException, RepositoryException {
        UserImpl currentUser = getCurrentUser(superuser);

        NodeResolver nr = createNodeResolver(currentUser.getNode().getSession());

        Node result = nr.findNode(UserConstants.P_PRINCIPAL_NAME, currentUser.getPrincipal().getName(), UserConstants.NT_REP_USER);
        assertNotNull(result);
        assertTrue(currentUser.getNode().isSame(result));

        Iterator it = currentUser.memberOf();
        while (it.hasNext()) {
            GroupImpl gr = (GroupImpl) it.next();

            result = nr.findNode(UserConstants.P_PRINCIPAL_NAME, gr.getPrincipal().getName(), UserConstants.NT_REP_GROUP);
            assertNotNull(result);
            assertTrue(gr.getNode().isSame(result));

            result = nr.findNode(UserConstants.P_PRINCIPAL_NAME, gr.getPrincipal().getName(), UserConstants.NT_REP_AUTHORIZABLE_FOLDER);
            assertNull(result);
        }
    }

    public void testFindNodeByUserID() throws NotExecutableException, RepositoryException {
        UserImpl currentUser = getCurrentUser(superuser);

        NodeResolver nr = createNodeResolver(currentUser.getNode().getSession());

        Node result = nr.findNode(UserConstants.P_USERID, currentUser.getID(), UserConstants.NT_REP_USER);
        assertNotNull(result);
        assertTrue(currentUser.getNode().isSame(result));
    }

    public void testFindNodeByMultiValueProp() throws NotExecutableException, RepositoryException {
        UserImpl currentUser = getCurrentUser(superuser);

        Value[] vs = new Value[] {
                superuser.getValueFactory().createValue("blub"),
                superuser.getValueFactory().createValue("blib")
        };
        currentUser.setProperty(propertyName1, vs);

        NodeResolver nr = createNodeResolver(currentUser.getNode().getSession());

        Node result = nr.findNode(((SessionImpl) superuser).getQName(propertyName1),
                "blib", UserConstants.NT_REP_USER);
        assertNotNull(result);
        assertTrue(currentUser.getNode().isSame(result));

        currentUser.removeProperty(propertyName1);
    }

    public void testFindNodeWithNonExistingSearchRoot() throws NotExecutableException, RepositoryException {
        String searchRoot = nodeResolver.getSearchRoot(UserConstants.NT_REP_AUTHORIZABLE_FOLDER);
        SessionImpl sImpl = (SessionImpl) superuser;

        if (sImpl.nodeExists(searchRoot)) {
            throw new NotExecutableException();
        }
        Node result = nodeResolver.findNode(sImpl.getQName(UserConstants.GROUP_ADMIN_GROUP_NAME), UserConstants.NT_REP_AUTHORIZABLE);
        assertNull(result);
    }

    public void testFindNodes() throws NotExecutableException, RepositoryException {
        Value[] vs = new Value[] {
                superuser.getValueFactory().createValue("blub"),
                superuser.getValueFactory().createValue("blib")
        };

        UserImpl currentUser = getCurrentUser(superuser);
        currentUser.setProperty(propertyName1, vs);

        Iterator it = currentUser.memberOf();
        while (it.hasNext()) {
            GroupImpl gr = (GroupImpl) it.next();
            gr.setProperty(propertyName1, vs);
        }

        Name propName = ((SessionImpl) superuser).getQName(propertyName1);

        try {
            NodeResolver nr = createNodeResolver(currentUser.getNode().getSession());

            NodeIterator result = nr.findNodes(propName, "blub", UserConstants.NT_REP_USER, false);
            assertTrue(result.getSize() == 1);
            assertTrue(result.nextNode().isSame(currentUser.getNode()));

            result = nr.findNodes(propName, "blub", UserConstants.NT_REP_AUTHORIZABLE, false);
            assertTrue(result.getSize() > 1);

        } finally {
            currentUser.removeProperty(propertyName1);
            it = currentUser.memberOf();
            while (it.hasNext()) {
                GroupImpl gr = (GroupImpl) it.next();
                gr.removeProperty(propertyName1);
            }
        }
    }

    public void testFindNodesWithNonExistingSearchRoot() throws NotExecutableException, RepositoryException {
        String searchRoot = nodeResolver.getSearchRoot(UserConstants.NT_REP_AUTHORIZABLE);
        if (((SessionImpl) superuser).nodeExists(searchRoot)) {
            throw new NotExecutableException();
        }

        NodeIterator result = nodeResolver.findNodes(UserConstants.P_REFEREES, "anyValue", UserConstants.NT_REP_AUTHORIZABLE, true);
        assertNotNull(result);
        assertFalse(result.hasNext());
    }

    public void testGetSearchRoot() {
        String searchRoot = nodeResolver.getSearchRoot(UserConstants.NT_REP_AUTHORIZABLE);
        assertNotNull(searchRoot);
        assertEquals(UserConstants.AUTHORIZABLES_PATH, searchRoot);

        searchRoot = nodeResolver.getSearchRoot(UserConstants.NT_REP_GROUP);
        assertNotNull(searchRoot);
        assertEquals(UserConstants.GROUPS_PATH, searchRoot);

        searchRoot = nodeResolver.getSearchRoot(UserConstants.NT_REP_USER);
        assertNotNull(searchRoot);
        assertEquals(UserConstants.USERS_PATH, searchRoot);
    }

    public void testGetSearchRootDefault() {
        String searchRoot = nodeResolver.getSearchRoot(UserConstants.NT_REP_AUTHORIZABLE_FOLDER);
        assertNotNull(searchRoot);
        assertEquals(UserConstants.AUTHORIZABLES_PATH, searchRoot);

        searchRoot = nodeResolver.getSearchRoot(NameConstants.NT_UNSTRUCTURED);
        assertNotNull(searchRoot);
        assertEquals(UserConstants.AUTHORIZABLES_PATH, searchRoot);
    }

    public void testGetNamePathResolver() {
        assertNotNull(nodeResolver.getNamePathResolver());
    }

    public void testGetSession() {
        assertNotNull(nodeResolver.getSession());
    }

    public void testFindNodeEscape() throws RepositoryException {
        Name n = NameFactoryImpl.getInstance().create("",
                "someone" + "@apache.org");
        nodeResolver.findNode(n, UserConstants.NT_REP_USER);
    }
}