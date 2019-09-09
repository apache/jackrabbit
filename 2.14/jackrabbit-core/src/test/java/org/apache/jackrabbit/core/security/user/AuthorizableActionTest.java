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
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.core.security.user.action.AbstractAuthorizableAction;
import org.apache.jackrabbit.core.security.user.action.AccessControlAction;
import org.apache.jackrabbit.core.security.user.action.AuthorizableAction;
import org.apache.jackrabbit.core.security.user.action.ClearMembershipAction;
import org.apache.jackrabbit.core.security.user.action.PasswordValidationAction;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>AuthorizableActionTest</code>...
 */
public class AuthorizableActionTest extends AbstractUserTest {

    private UserManagerImpl impl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        impl = (UserManagerImpl) userMgr;
    }

    @Override
    protected void tearDown() throws Exception {
        // reset the actions
        setActions(null);

        super.tearDown();
    }

    private void setActions(AuthorizableAction action) {
        AuthorizableAction[] actions = (action == null) ?
                new AuthorizableAction[0] :
                new AuthorizableAction[] {action};
        impl.setAuthorizableActions(actions);
    }

    public void testAccessControlAction() throws Exception {
        AccessControlAction action = new AccessControlAction();
        action.setUserPrivilegeNames("jcr:all");
        action.setGroupPrivilegeNames("jcr:read");

        User u = null;
        Group gr = null;
        try {
            setActions(action);

            String uid = getTestPrincipal().getName();
            u = impl.createUser(uid, buildPassword(uid));
            save(superuser);
            assertAcAction(u, impl);

            String grId = getTestPrincipal().getName();
            gr = impl.createGroup(grId);
            save(superuser);
            assertAcAction(gr, impl);
        } catch (UnsupportedRepositoryOperationException e) {
            throw new NotExecutableException(e.getMessage());
        } finally {
            if (u != null) {
                u.remove();
            }
            if (gr != null) {
                gr.remove();
            }
            save(superuser);
        }
    }

    private static void assertAcAction(Authorizable a, UserManagerImpl umgr) throws RepositoryException, NotExecutableException {
        Session s = umgr.getSession();
        AccessControlManager acMgr = s.getAccessControlManager();
        boolean hasACL = false;
        AccessControlPolicyIterator it = acMgr.getApplicablePolicies("/");
        while (it.hasNext()) {
            if (it.nextAccessControlPolicy() instanceof AccessControlList) {
                hasACL = true;
                break;
            }
        }

        if (!hasACL) {
            for (AccessControlPolicy p : acMgr.getPolicies("/")) {
                if (p instanceof AccessControlList) {
                    hasACL = true;
                    break;
                }
            }
        }

        if (!hasACL) {
            throw new NotExecutableException("No ACLs in workspace containing users.");
        }

        String path = a.getPath();
        assertEquals(1, acMgr.getPolicies(path).length);
        assertTrue(acMgr.getPolicies(path)[0] instanceof AccessControlList);
    }

    public void testClearMembershipAction() throws Exception {
        User u = null;
        Group gr = null;
        try {
            setActions(new ClearMembershipAction());

            String uid = getTestPrincipal().getName();
            u = impl.createUser(uid, buildPassword(uid));

            String grId = getTestPrincipal().getName();
            gr = impl.createGroup(grId);
            gr.addMember(u);

            save(superuser);

            assertTrue(gr.isMember(u));

            u.remove();
            u = null;

            assertFalse(gr.isMember(u));
        } finally {
            if (u != null) {
                u.remove();
            }
            if (gr != null) {
                gr.remove();
            }
            save(superuser);
        }
    }

    public void testPasswordAction() throws Exception {
        User u = null;

        try {
            TestAction action = new TestAction();
            setActions(action);

            String uid = getTestPrincipal().getName();
            u = impl.createUser(uid, buildPassword(uid));

            u.changePassword("pw1");
            assertEquals(1, action.called);

            u.changePassword("pw2", "pw1");
            assertEquals(2, action.called);
        } finally {
            if (u != null) {
                u.remove();
            }
            save(superuser);
        }
    }

    public void testPasswordValidationAction() throws Exception {
        User u = null;

        try {
            String uid = getTestPrincipal().getName();
            u = impl.createUser(uid, buildPassword(uid));

            PasswordValidationAction pwAction = new PasswordValidationAction();
            pwAction.setConstraint("^.*(?=.{8,})(?=.*[a-z])(?=.*[A-Z]).*");
            setActions(pwAction);

            List<String> invalid = new ArrayList<String>();
            invalid.add("pw1");
            invalid.add("only6C");
            invalid.add("12345678");
            invalid.add("WITHOUTLOWERCASE");
            invalid.add("withoutuppercase");

            for (String pw : invalid) {
                try {
                    u.changePassword(pw);
                    fail("should throw constraint violation");
                } catch (ConstraintViolationException e) {
                    // success
                }
            }

            List<String> valid = new ArrayList<String>();
            valid.add("abCDefGH");
            valid.add("Abbbbbbbbbbbb");
            valid.add("cDDDDDDDDDDDDDDDDD");
            valid.add("gH%%%%%%%%%%%%%%%%^^");
            valid.add("&)(*&^%23qW");

            for (String pw : valid) {
                u.changePassword(pw);
            }

        } finally {
            if (u != null) {
                u.remove();
            }
            save(superuser);
        }
    }

    public void testPasswordValidationActionIgnoresHashedPwStringOnCreate() throws Exception {
        User u = null;

        try {
            PasswordValidationAction pwAction = new PasswordValidationAction();
            pwAction.setConstraint("^.*(?=.{8,})(?=.*[a-z])(?=.*[A-Z]).*");
            setActions(pwAction);

            String uid = getTestPrincipal().getName();
            String hashed = PasswordUtility.buildPasswordHash("DWkej32H");
            u = impl.createUser(uid, hashed);

        } finally {
            if (u != null) {
                u.remove();
            }
            save(superuser);
        }
    }

    public void testPasswordValidationActionOnChange() throws Exception {
        User u = null;

        try {
            String uid = getTestPrincipal().getName();
            u = impl.createUser(uid, buildPassword(uid));

            PasswordValidationAction pwAction = new PasswordValidationAction();
            pwAction.setConstraint("abc");
            setActions(pwAction);

            String hashed = PasswordUtility.buildPasswordHash("abc");
            u.changePassword(hashed);

            fail("Password change must always enforce password validation.");

        } catch (ConstraintViolationException e) {
            // success
        } finally {
            if (u != null) {
                u.remove();
            }
            save(superuser);
        }
    }

    //--------------------------------------------------------------------------
    private class TestAction extends AbstractAuthorizableAction {

        private int called = 0;

        @Override
        public void onPasswordChange(User user, String newPassword, Session session) throws RepositoryException {
            called++;
        }
    }
}