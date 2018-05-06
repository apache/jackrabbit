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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.security.JackrabbitSecurityManager;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.util.Text;

import javax.jcr.Item;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import java.security.Principal;
import java.util.Arrays;

/**
 * <code>SecurityManagerTest</code>...
 */
public class UserPerWorkspaceSecurityManagerTest extends AbstractJCRTest {

    private JackrabbitSecurityManager secMgr;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        RepositoryImpl repo = (RepositoryImpl) superuser.getRepository();
        secMgr = repo.getRepositoryContext().getSecurityManager();
        if (!(secMgr instanceof UserPerWorkspaceSecurityManager)) {
            throw new NotExecutableException();
        }
    }

    private String getAlternativeWorkspaceName() throws RepositoryException {
        String altWsp = null;
        String[] wsps = superuser.getWorkspace().getAccessibleWorkspaceNames();
        if (wsps.length == 1) {
            superuser.getWorkspace().createWorkspace("tmp");
            altWsp = "tmp";
        } else {
            for (String name : wsps) {
                if (!name.equals(superuser.getWorkspace().getName())) {
                    altWsp = name;
                    break;
                }
            }
        }
        return altWsp;
    }

    public void testSystemUserCreation() throws Exception {
        String altWsp = getAlternativeWorkspaceName();
        if (altWsp == null) {
            throw new NotExecutableException();
        }       

        // system users must be automatically be created -> login with admin
        // must therefore succeed.
        Session s = getHelper().getSuperuserSession(altWsp);
        try {
            // admin/anonymous must be present
            String adminId = ((UserPerWorkspaceSecurityManager) secMgr).adminId;
            assertEquals(adminId, s.getUserID());

            UserManager umgr = ((JackrabbitSession) s).getUserManager();
            assertNotNull(umgr.getAuthorizable(adminId));
            assertNotNull(umgr.getAuthorizable(((UserPerWorkspaceSecurityManager) secMgr).anonymousId));

        } finally {
            s.logout();
        }
    }

    public void testSystemUsersAreSaved() throws Exception {
        String adminId = ((UserPerWorkspaceSecurityManager) secMgr).adminId;
        UserManager umgr = ((JackrabbitSession) superuser).getUserManager();
        Principal p = umgr.getAuthorizable(adminId).getPrincipal();

        if (p instanceof ItemBasedPrincipal) {
            Item item = superuser.getItem(((ItemBasedPrincipal) p).getPath());
            assertFalse(item.isNew());
            assertFalse(item.isModified());
        }
    }

    public void testUsersArePerWorkspace() throws Exception {
        String altWsp = getAlternativeWorkspaceName();
        if (altWsp == null) {
            throw new NotExecutableException();
        }

        Session s = getHelper().getSuperuserSession(altWsp);
        User u = null;
        try {
            // other users created in the default workspace...
            u = ((JackrabbitSession) superuser).getUserManager().createUser("testUser", "testUser");
            superuser.save();

            // ... must not be present in the alternate-workspace
            UserManager umgr = ((JackrabbitSession) s).getUserManager();
            assertNull(umgr.getAuthorizable("testUser"));

            try {
                Session us = getHelper().getRepository().login(new SimpleCredentials("testUser", "testUser".toCharArray()), altWsp);
                us.logout();
                fail("testUser must not be able to login to a workspace without this user.");
            } catch (LoginException e) {
                // success
            }

        } finally {
            s.logout();
            if (u != null) {
                u.remove();
                superuser.save();
            }
        }
    }

    public void testAccessibleWorkspaceNames() throws Exception {
        String altWsp = getAlternativeWorkspaceName();
        if (altWsp == null) {
            throw new NotExecutableException();
        }

        Session s = getHelper().getSuperuserSession(altWsp);
        User u = null;
        Session us = null;
        try {
            // other users created in the default workspace...
            u = ((JackrabbitSession) superuser).getUserManager().createUser("testUser", "testUser");
            superuser.save();

            us = getHelper().getRepository().login(new SimpleCredentials("testUser", "testUser".toCharArray()));
            String[] wspNames = us.getWorkspace().getAccessibleWorkspaceNames();
            assertFalse(Arrays.asList(wspNames).contains(altWsp));
            
        } finally {
            s.logout();
            if (us != null) {
                us.logout();
            }
            if (u != null) {
                u.remove();
                superuser.save();
            }
        }

    }

    public void testCloneUser() throws Exception {
        String altWsp = getAlternativeWorkspaceName();
        if (altWsp == null) {
            throw new NotExecutableException();
        }

        UserManager uMgr = ((JackrabbitSession) superuser).getUserManager();

        Session s = getHelper().getSuperuserSession(altWsp);
        User u = null;
        try {
            // other users created in the default workspace...
            u = uMgr.createUser("testUser", "testUser");
            superuser.save();

            String userPath = null;
            if (u.getPrincipal() instanceof ItemBasedPrincipal) {
                userPath = ((ItemBasedPrincipal) u.getPrincipal()).getPath();
                assertTrue(superuser.nodeExists(userPath));
            } else {
                throw new NotExecutableException();
            }

            // ... must not be present in the alternate-workspace
            UserManager umgr = ((JackrabbitSession) s).getUserManager();
            assertNull(umgr.getAuthorizable("testUser"));
            assertFalse(s.nodeExists(userPath));

            String clonePath = userPath;
            String parentPath = Text.getRelativeParent(clonePath, 1);
            while (!s.nodeExists(parentPath)) {
                clonePath = parentPath;
                parentPath = Text.getRelativeParent(parentPath, 1);
            }

            // clone the user into the second workspace
            s.getWorkspace().clone(superuser.getWorkspace().getName(), clonePath, clonePath, true);

            // ... now the user must be visible
            assertNotNull(umgr.getAuthorizable("testUser"));
            if (userPath != null) {
                assertTrue(s.nodeExists(userPath));                
            }
            // ... and able to login to that workspace
            Session us = getHelper().getRepository().login(new SimpleCredentials("testUser", "testUser".toCharArray()), altWsp);
            us.logout();

        } finally {
            // remove the test user in the second workspace
            Authorizable dest = ((JackrabbitSession) s).getUserManager().getAuthorizable("testUser");
            if (dest != null) {
                dest.remove();
                s.save();
            }
            // logout the session
            s.logout();
            if (u != null) {
                // remove as well in the first workspace
                u.remove();
                superuser.save();
            }
        }
    }

    public void testUpdateUser() throws NotExecutableException, RepositoryException {
        // create the same use in 2 different workspace must make the 'corresponding'
        // and updating must succeed
        String altWsp = getAlternativeWorkspaceName();
        if (altWsp == null) {
            throw new NotExecutableException();
        }

        UserManager uMgr = ((JackrabbitSession) superuser).getUserManager();

        Session s = getHelper().getSuperuserSession(altWsp);
        User u = null;
        try {
            // other users created in the default workspace...
            u = uMgr.createUser("testUser", "testUser");
            superuser.save();

            String userPath = null;
            if (u.getPrincipal() instanceof ItemBasedPrincipal) {
                userPath = ((ItemBasedPrincipal) u.getPrincipal()).getPath();
                assertTrue(superuser.nodeExists(userPath));
            } else {
                throw new NotExecutableException();
            }

            // ... must not be present in the alternate-workspace
            UserManager umgr = ((JackrabbitSession) s).getUserManager();
            assertNull(umgr.getAuthorizable("testUser"));
            assertFalse(s.nodeExists(userPath));

            User u2 = umgr.createUser("testUser", "testUser");
            s.save();
            assertTrue(s.nodeExists(userPath));

            Value value = superuser.getValueFactory().createValue("anyValue");
            u.setProperty(propertyName1, value);
            superuser.save();

            // no automatic sync.
            assertFalse(u2.hasProperty(propertyName1));

            // update nodes
            Node n2 = s.getNode(userPath);
            n2.update(superuser.getWorkspace().getName());

            // now the value must be visible
            assertTrue(u2.hasProperty(propertyName1));
            assertEquals(value.getString(), u2.getProperty(propertyName1)[0].getString());
            
        } finally {
            // remove the test user in the destination workspace
            Authorizable dest = ((JackrabbitSession) s).getUserManager().getAuthorizable("testUser");
            if (dest != null) {
                dest.remove();
                s.save();
            }
            // logout the session to the destination workspace
            s.logout();
            if (u != null) {
                // and remove it in the default workspace as well
                u.remove();
                superuser.save();
            }
        }
    }

    public void testNewUsersCanLogin() throws Exception {
        Session s = null;
        User u = null;
        try {
            // other users created in the default workspace...
            u = ((JackrabbitSession) superuser).getUserManager().createUser("testUser", "testUser");
            superuser.save();

            // the new user must be able to login to the repo
            s = getHelper().getRepository().login(new SimpleCredentials("testUser", "testUser".toCharArray()));

        } finally {
            if (s != null) {
                s.logout();
            }
            if (u != null) {
                u.remove();
                superuser.save();
            }
        }
    }

    public void testTransientUserCannotLogin() throws RepositoryException, UnsupportedRepositoryOperationException {
        Session s = null;
        String uid = "testUser";
        UserManager umgr = ((JackrabbitSession) superuser).getUserManager();
        umgr.autoSave(false);
        try {
            // other users created in the default workspace...
            umgr.createUser(uid, uid);
            // the new user must be able to login to the repo
            s = getHelper().getRepository().login(new SimpleCredentials(uid, uid.toCharArray()));

            fail("Non-saved user node -> must not be able to login.");

        } catch (LoginException e) {
            // success
        } finally {
            if (s != null) {
                s.logout();
            }
            superuser.refresh(false);
            Authorizable a = ((JackrabbitSession) superuser).getUserManager().getAuthorizable(uid);
            if (a != null) {
                a.remove();
                superuser.save();
            }
            umgr.autoSave(true);
        }
    }
}