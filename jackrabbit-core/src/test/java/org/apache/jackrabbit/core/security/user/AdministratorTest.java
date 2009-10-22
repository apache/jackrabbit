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

import org.apache.jackrabbit.api.security.user.AbstractUserTest;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

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

    public void testMemberOfAdministrators() throws RepositoryException {
        Authorizable admins = userMgr.getAuthorizable(SecurityConstants.ADMINISTRATORS_NAME);
        if (admins != null && admins.isGroup()) {
            assertTrue(((Group) admins).isMember(userMgr.getAuthorizable(adminId)));
        }
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

        // login must succeed as system user mgr recreateds the admin user
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
     * 
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testCollidingAdminNode() throws RepositoryException, NotExecutableException {
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
        try {
            // no create a colliding node:
            parentNode.addNode(adminNodeName, "rep:AuthorizableFolder");
            s.save();

            // force recreation of admin user.
            s2 = getHelper().getSuperuserSession();

            admin = userMgr.getAuthorizable(adminId);
            assertNotNull(admin);
            assertEquals(adminNodeName, ((AuthorizableImpl) admin).getNode().getName());
            assertFalse(adminPath.equals(((AuthorizableImpl) admin).getNode().getPath()));

        } finally {
            if (s2 == null) {
                // something went wrong -> remove the folder again.
                parentNode.remove();
                s.save();
                // force recreation of admin user.
                s2 = getHelper().getSuperuserSession();
            }
            if (s2 != null) {
                s2.logout();
            }
        }
    }
}
