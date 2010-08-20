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
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.util.Text;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.util.Iterator;

/** <code>NodeResolverTest</code>... */
public abstract class NodeResolverTest extends AbstractJCRTest {

    NodeResolver nodeResolver;
    UserManager umgr;
    String usersPath = UserConstants.USERS_PATH;
    String groupsPath = UserConstants.GROUPS_PATH;
    String authorizablesPath = UserConstants.AUTHORIZABLES_PATH;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        nodeResolver = createNodeResolver(superuser);
        if (!(superuser instanceof JackrabbitSession)) {
            throw new NotExecutableException();
        }

        umgr = ((JackrabbitSession) superuser).getUserManager();
        if (umgr instanceof UserManagerImpl) {
            UserManagerImpl uImpl = (UserManagerImpl) umgr;
            usersPath = uImpl.getUsersPath();
            groupsPath = uImpl.getGroupsPath();

            authorizablesPath = usersPath;
            while (!Text.isDescendant(authorizablesPath, groupsPath)) {
                authorizablesPath = Text.getRelativeParent(authorizablesPath, 1);
            }
        }
    }

    protected UserImpl getCurrentUser() throws NotExecutableException, RepositoryException {
        String uid = superuser.getUserID();
        if (uid != null) {
            Authorizable auth = umgr.getAuthorizable(uid);
            if (auth != null && auth instanceof UserImpl) {
                return (UserImpl) auth;
            }
        }
        // unable to retrieve current user
        throw new NotExecutableException();
    }

    protected void save() throws RepositoryException {
        if (!umgr.isAutoSave() && superuser.hasPendingChanges()) {
            superuser.save();
        }
    }

    protected abstract NodeResolver createNodeResolver(SessionImpl session) throws RepositoryException, NotExecutableException;

    protected NodeResolver createNodeResolver(Session session) throws NotExecutableException, RepositoryException {
        if (!(session instanceof SessionImpl)) {
            throw new NotExecutableException();
        }

        NodeResolver resolver = createNodeResolver((SessionImpl) session);
        UserManager umr = ((SessionImpl) session).getUserManager();
        if (umr instanceof UserManagerImpl) {
            UserManagerImpl uImpl = (UserManagerImpl) umr;
            resolver.setSearchRoots(uImpl.getUsersPath(), uImpl.getGroupsPath());
        }
        return resolver;
    }

    public void testFindNode() throws NotExecutableException, RepositoryException {
        UserImpl currentUser = getCurrentUser();

        NodeResolver nr = createNodeResolver(currentUser.getNode().getSession());

        Node result = nr.findNode(currentUser.getNode().getQName(), UserConstants.NT_REP_USER);
        assertNotNull(result);
        assertTrue(currentUser.getNode().isSame(result));

        result = nr.findNode(currentUser.getNode().getQName(), UserConstants.NT_REP_AUTHORIZABLE);
        assertNotNull(result);
        assertTrue(currentUser.getNode().isSame(result));

        result = nr.findNode(currentUser.getNode().getQName(), UserConstants.NT_REP_GROUP);
        assertNull(result);

        Iterator<Group> it = currentUser.memberOf();
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
        UserImpl currentUser = getCurrentUser();

        NodeResolver nr = createNodeResolver(currentUser.getNode().getSession());

        Node result = nr.findNode(UserConstants.P_PRINCIPAL_NAME, currentUser.getPrincipal().getName(), UserConstants.NT_REP_USER);
        assertNotNull(result);
        assertTrue(currentUser.getNode().isSame(result));

        Iterator<Group> it = currentUser.memberOf();
        while (it.hasNext()) {
            GroupImpl gr = (GroupImpl) it.next();

            result = nr.findNode(UserConstants.P_PRINCIPAL_NAME, gr.getPrincipal().getName(), UserConstants.NT_REP_GROUP);
            assertNotNull(result);
            assertTrue(gr.getNode().isSame(result));

            result = nr.findNode(UserConstants.P_PRINCIPAL_NAME, gr.getPrincipal().getName(), UserConstants.NT_REP_AUTHORIZABLE_FOLDER);
            assertNull(result);
        }
    }

    public void testFindNodeByMultiValueProp() throws NotExecutableException, RepositoryException {
        UserImpl currentUser = getCurrentUser();

        Value[] vs = new Value[] {
                superuser.getValueFactory().createValue("blub"),
                superuser.getValueFactory().createValue("blib")
        };
        currentUser.setProperty(propertyName1, vs);
        save();

        NodeResolver nr = createNodeResolver(currentUser.getNode().getSession());

        Node result = nr.findNode(((SessionImpl) superuser).getQName(propertyName1),
                "blib", UserConstants.NT_REP_USER);
        assertNotNull(result);
        assertTrue(currentUser.getNode().isSame(result));

        currentUser.removeProperty(propertyName1);
        save();
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

        UserImpl currentUser = getCurrentUser();
        currentUser.setProperty(propertyName1, vs);

        int expResultSize = 1;
        Iterator<Group> it = currentUser.memberOf();
        while (it.hasNext()) {
            GroupImpl gr = (GroupImpl) it.next();
            gr.setProperty(propertyName1, vs);
            expResultSize++;
        }
        save();

        Name propName = ((SessionImpl) superuser).getQName(propertyName1);

        try {
            NodeResolver nr = createNodeResolver(currentUser.getNode().getSession());

            NodeIterator result = nr.findNodes(propName, "blub", UserConstants.NT_REP_USER, false);
            assertTrue("expected result", result.hasNext());
            assertEquals(currentUser.getNode().getPath(), result.nextNode().getPath());
            assertFalse("expected no more results", result.hasNext());

            result = nr.findNodes(propName, "blub", UserConstants.NT_REP_AUTHORIZABLE, false);
            assertEquals(expResultSize, getSize(result));

        } finally {
            currentUser.removeProperty(propertyName1);
            it = currentUser.memberOf();
            while (it.hasNext()) {
                GroupImpl gr = (GroupImpl) it.next();
                gr.removeProperty(propertyName1);
            }
            save();
        }
    }

    /**
     * 
     * @throws NotExecutableException
     * @throws RepositoryException
     */
    public void testFindNodesByRelPathProperties() throws NotExecutableException, RepositoryException {
        Value[] vs = new Value[] {
                superuser.getValueFactory().createValue("blub"),
                superuser.getValueFactory().createValue("blib")
        };

        String relPath = "relPath/" + propertyName1;
        String relPath2 = "another/" + propertyName1;
        String relPath3 = "relPath/relPath/" + propertyName1;
        UserImpl currentUser = getCurrentUser();
        currentUser.setProperty(relPath, vs);
        currentUser.setProperty(relPath2, vs);
        currentUser.setProperty(relPath3, vs);        
        save();

        Path relQPath = ((SessionImpl) superuser).getQPath(relPath);
        Path relQName = ((SessionImpl) superuser).getQPath(propertyName1);

        try {
            NodeResolver nr = createNodeResolver(currentUser.getNode().getSession());
            // 1) findNodes(QPath.....
            // relPath : "prop1" -> should find the currentuserNode
            NodeIterator result = nr.findNodes(relQName, "blub", UserManager.SEARCH_TYPE_USER, false, Long.MAX_VALUE);
            assertTrue("expected result", result.hasNext());
            Node n = result.nextNode();
            assertEquals(currentUser.getNode().getPath(), n.getPath());
            assertFalse("expected no more results", result.hasNext());

            // relPath : "relPath/prop1" -> should find the currentuserNode
            result = nr.findNodes(relQPath, "blub", UserManager.SEARCH_TYPE_USER, false, Long.MAX_VALUE);
            assertTrue("expected result", result.hasNext());
            assertEquals(currentUser.getNode().getPath(), result.nextNode().getPath());
            assertFalse("expected no more results", result.hasNext());

            // 2) findNodes(Name.......)
            // search by Name -> should not find deep property
            Name propName = ((SessionImpl) superuser).getQName(propertyName1);            
            result = nr.findNodes(propName, "blub", UserConstants.NT_REP_USER, false);
            assertFalse("should not find result", result.hasNext());

        } finally {
            currentUser.removeProperty(relPath);
            currentUser.removeProperty(relPath2);
            currentUser.removeProperty(relPath3);            
            save();
        }
    }


    public void testFindNodesWithNonExistingSearchRoot() throws NotExecutableException, RepositoryException {
        String searchRoot = nodeResolver.getSearchRoot(UserConstants.NT_REP_AUTHORIZABLE);
        if (superuser.nodeExists(searchRoot)) {
            throw new NotExecutableException();
        }

        NodeIterator result = nodeResolver.findNodes(UserConstants.P_PRINCIPAL_NAME, "anyValue", UserConstants.NT_REP_AUTHORIZABLE, true);
        assertNotNull(result);
        assertFalse(result.hasNext());
    }

    public void testGetSearchRoot() {
        String searchRoot = nodeResolver.getSearchRoot(UserConstants.NT_REP_AUTHORIZABLE);
        assertNotNull(searchRoot);
        assertEquals(authorizablesPath, searchRoot);

        searchRoot = nodeResolver.getSearchRoot(UserConstants.NT_REP_GROUP);
        assertNotNull(searchRoot);
        assertEquals(groupsPath, searchRoot);

        searchRoot = nodeResolver.getSearchRoot(UserConstants.NT_REP_USER);
        assertNotNull(searchRoot);
        assertEquals(usersPath, searchRoot);
    }

    public void testGetSearchRootDefault() {
        String searchRoot = nodeResolver.getSearchRoot(UserConstants.NT_REP_AUTHORIZABLE_FOLDER);
        assertNotNull(searchRoot);
        assertEquals(authorizablesPath, searchRoot);

        searchRoot = nodeResolver.getSearchRoot(NameConstants.NT_UNSTRUCTURED);
        assertNotNull(searchRoot);
        assertEquals(authorizablesPath, searchRoot);
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