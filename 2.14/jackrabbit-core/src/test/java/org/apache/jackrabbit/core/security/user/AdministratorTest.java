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

import java.util.Properties;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.AbstractUserTest;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>AdministratorTest</code>...
 */
public class AdministratorTest extends AbstractUserTest {

    private String adminId;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (!(userMgr instanceof UserManagerImpl)) {
            throw new NotExecutableException();
        }
        adminId = superuser.getUserID();
        if (!((UserManagerImpl) userMgr).isAdminId(adminId)) {
            throw new NotExecutableException();
        }
    }

    public void testGetPrincipal() throws RepositoryException {
        Authorizable admin = userMgr.getAuthorizable(adminId);
        assertNotNull(admin);
        assertFalse(admin.isGroup());
        assertTrue(admin.getPrincipal() instanceof AdminPrincipal);
    }

    public void testRemoveSelf() throws RepositoryException, NotExecutableException {
        Authorizable admin = userMgr.getAuthorizable(adminId);
        if (admin == null) {
            throw new NotExecutableException();
        }
        try {
            admin.remove();
            fail("The Administrator should not be allowed to remove the own authorizable.");
        } catch (RepositoryException e) {
            // success
        }
    }

    /**
     * Test if the administrator is recreated upon login if the corresponding
     * node gets removed.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testRemoveAdminNode() throws RepositoryException, NotExecutableException {
        Authorizable admin = userMgr.getAuthorizable(adminId);

        if (admin == null || !(admin instanceof AuthorizableImpl)) {
            throw new NotExecutableException();
        }

        // access the node corresponding to the admin user and remove it
        NodeImpl adminNode = ((AuthorizableImpl) admin).getNode();
        Session s = adminNode.getSession();
        adminNode.remove();
        // use session obtained from the node as usermgr may point to a dedicated
        // system workspace different from the superusers workspace.
        s.save();

        // after removing the node the admin user doesn't exist any more
        assertNull(userMgr.getAuthorizable(adminId));

        // login must succeed as system user mgr recreates the admin user
        Session s2 = getHelper().getSuperuserSession();
        try {
            admin = userMgr.getAuthorizable(adminId);
            assertNotNull(admin);
            assertNotNull(getUserManager(s2).getAuthorizable(adminId));
        } finally {
            s2.logout();
        }
    }

    /**
     * Test for collisions that would prevent from recreate the admin user.
     * - an intermediate rep:AuthorizableFolder node with the same name
     */
    public void testAdminNodeCollidingWithAuthorizableFolder() throws RepositoryException, NotExecutableException {
        Authorizable admin = userMgr.getAuthorizable(adminId);

        if (admin == null || !(admin instanceof AuthorizableImpl)) {
            throw new NotExecutableException();
        }

        // access the node corresponding to the admin user and remove it
        NodeImpl adminNode = ((AuthorizableImpl) admin).getNode();
        String adminPath = adminNode.getPath();
        String adminNodeName = adminNode.getName();
        Node parentNode = adminNode.getParent();

        Session s = adminNode.getSession();
        adminNode.remove();
        // use session obtained from the node as usermgr may point to a dedicated
        // system workspace different from the superusers workspace.
        s.save();

        Session s2 = null;
        String collidingPath = null;
        try {
            // now create a colliding node:
            Node n = parentNode.addNode(adminNodeName, "rep:AuthorizableFolder");
            collidingPath = n.getPath();
            s.save();

            // force recreation of admin user.
            s2 = getHelper().getSuperuserSession();

            admin = userMgr.getAuthorizable(adminId);
            assertNotNull(admin);
            assertEquals(adminNodeName, ((AuthorizableImpl) admin).getNode().getName());
            assertFalse(adminPath.equals(((AuthorizableImpl) admin).getNode().getPath()));

        } finally {
            if (s2 != null) {
                s2.logout();
            }
            // remove the extra folder and the admin user (created underneath) again.
            if (collidingPath != null) {
                s.getNode(collidingPath).remove();
                s.save();
            }
        }
    }

    /**
     * Test for collisions that would prevent from recreate the admin user.
     * - a colliding node somewhere else with the same jcr:uuid.
     *
     * Test if creation of the administrator user forces the removal of some
     * other node in the repository that by change happens to have the same
     * jcr:uuid and thus inhibits the creation of the admininstrator user.
     */
    public void testAdminNodeCollidingWithRandomNode() throws RepositoryException, NotExecutableException {
        Authorizable admin = userMgr.getAuthorizable(adminId);

        if (admin == null || !(admin instanceof AuthorizableImpl)) {
            throw new NotExecutableException();
        }

        // access the node corresponding to the admin user and remove it
        NodeImpl adminNode = ((AuthorizableImpl) admin).getNode();
        NodeId nid = adminNode.getNodeId();

        Session s = adminNode.getSession();
        adminNode.remove();
        // use session obtained from the node as usermgr may point to a dedicated
        // system workspace different from the superusers workspace.
        s.save();

        Session s2 = null;
        String collidingPath = null;
        try {
            // create a colliding node outside of the user tree
            NameResolver nr = (SessionImpl) s;
            // NOTE: testRootNode will not be present if users are stored in a distinct wsp.
            //       therefore use root node as start...
            NodeImpl tr = (NodeImpl) s.getRootNode();
            Node n = tr.addNode(nr.getQName("tmpNode"), nr.getQName(testNodeType), nid);
            collidingPath = n.getPath();
            s.save();

            // force recreation of admin user.
            s2 = getHelper().getSuperuserSession();

            admin = userMgr.getAuthorizable(adminId);
            assertNotNull(admin);
            // the colliding node must have been removed.
            assertFalse(s2.nodeExists(collidingPath));

        } finally {
            if (s2 != null) {
                s2.logout();
            }
            if (collidingPath != null && s.nodeExists(collidingPath)) {
                s.getNode(collidingPath).remove();
                s.save();
            }
        }
    }

    /**
     * Reconfiguration of the user-root-path will result in node collision
     * upon initialization of the built-in repository users. Test if the
     * UserManagerImpl in this case removes the colliding admin-user node.
     */
    public void testChangeUserRootPath() throws RepositoryException, NotExecutableException {
        Authorizable admin = userMgr.getAuthorizable(adminId);

        if (admin == null || !(admin instanceof AuthorizableImpl)) {
            throw new NotExecutableException();
        }

        // access the node corresponding to the admin user and remove it
        NodeImpl adminNode = ((AuthorizableImpl) admin).getNode();

        Session s = adminNode.getSession();
        adminNode.remove();
        // use session obtained from the node as usermgr may point to a dedicated
        // system workspace different from the superusers workspace.
        s.save();

        Session s2 = null;
        String collidingPath = null;
        try {
            // create a colliding user node outside of the user tree
            Properties props = new Properties();
            props.setProperty("usersPath", "/testPath");
            UserManager um = new UserManagerImpl((SessionImpl) s, adminId, props);
            User collidingUser = um.createUser(adminId, adminId);
            collidingPath = ((AuthorizableImpl) collidingUser).getNode().getPath();
            s.save();

            // force recreation of admin user.
            s2 = getHelper().getSuperuserSession();

            admin = userMgr.getAuthorizable(adminId);
            assertNotNull(admin);
            // the colliding node must have been removed.
            assertFalse(s2.nodeExists(collidingPath));

        } finally {
            if (s2 != null) {
                s2.logout();
            }
            if (collidingPath != null && s.nodeExists(collidingPath)) {
                s.getNode(collidingPath).remove();
                s.save();
            }
        }
    }
}
