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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.AbstractUserTest;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
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

    public void testRemoveAdminNode() throws RepositoryException, NotExecutableException {
        Authorizable admin = userMgr.getAuthorizable(adminId);

        if (admin == null || !(admin instanceof AuthorizableImpl)) {
            throw new NotExecutableException();
        }

        Session s = null;
        try {
            NodeImpl adminNode = ((AuthorizableImpl) admin).getNode();
            s = adminNode.getSession();
            adminNode.remove();
            // use session obtained from the node as usermgr may point to a dedicated
            // system workspace different from the superusers workspace.
            s.save();
            fail();
        } catch (RepositoryException e) {
            // success
        } finally {
            if (s != null) {
                s.refresh(false);
            }
        }
    }

    public void testSessionRemoveItem()  throws RepositoryException, NotExecutableException {
        Authorizable admin = userMgr.getAuthorizable(adminId);

        if (admin == null || !(admin instanceof AuthorizableImpl)) {
            throw new NotExecutableException();
        }

        Session s = null;
        try {
            NodeImpl parent = (NodeImpl) ((AuthorizableImpl) admin).getNode().getParent();
            s = parent.getSession();
            s.removeItem(parent.getPath());
            s.save();
            fail();
        } catch (RepositoryException e) {
            // success
        } finally {
            if (s != null) {
                s.refresh(false);
            }
        }
    }

    public void testSessionMoveAdminNode()  throws RepositoryException, NotExecutableException {
        Authorizable admin = userMgr.getAuthorizable(adminId);

        if (admin == null || !(admin instanceof AuthorizableImpl)) {
            throw new NotExecutableException();
        }

        Session s = null;
        try {
            NodeImpl adminNode = ((AuthorizableImpl) admin).getNode();
            s = adminNode.getSession();
            s.move(adminNode.getPath(), "/somewhereelse");
            // use session obtained from the node as usermgr may point to a dedicated
            // system workspace different from the superusers workspace.
            s.save();
            fail();
        } catch (RepositoryException e) {
            // success
        }  finally {
            if (s != null) {
                s.refresh(false);
            }
        }
    }

    public void testSessionMoveParentNode()  throws RepositoryException, NotExecutableException {
        Authorizable admin = userMgr.getAuthorizable(adminId);

        if (admin == null || !(admin instanceof AuthorizableImpl)) {
            throw new NotExecutableException();
        }

        Session s = null;
        try {
            NodeImpl parent = (NodeImpl) ((AuthorizableImpl) admin).getNode().getParent();
            s = parent.getSession();
            s.move(parent.getPath(), "/somewhereelse");
            // use session obtained from the node as usermgr may point to a dedicated
            // system workspace different from the superusers workspace.
            s.save();
            fail();
        } catch (RepositoryException e) {
            // success
        } finally {
            if (s != null) {
                s.refresh(false);
            }
        }
    }

    public void testWorkspaceMoveAdminNode()  throws RepositoryException, NotExecutableException {
        Authorizable admin = userMgr.getAuthorizable(adminId);

        if (admin == null || !(admin instanceof AuthorizableImpl)) {
            throw new NotExecutableException();
        }

        // access the node corresponding to the admin user and remove it
        try {
            NodeImpl adminNode = ((AuthorizableImpl) admin).getNode();
            Session s = adminNode.getSession();
            s.getWorkspace().move(adminNode.getPath(), "/somewhereelse");
            fail();
        } catch (RepositoryException e) {
            // success
        }
    }
}
