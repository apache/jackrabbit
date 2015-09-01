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
package org.apache.jackrabbit.core.security.authorization;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.core.security.TestPrincipal;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.api.security.AbstractAccessControlTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** <code>AbstractEvaluationTest</code>... */
public abstract class AbstractEvaluationTest extends AbstractAccessControlTest {

    private static Logger log = LoggerFactory.getLogger(AbstractEvaluationTest.class);

    private String uid;
    protected User testUser;
    protected Credentials creds;

    protected Group testGroup;    
    
    private Session testSession;
    private AccessControlManager testAccessControlManager;
    private Node trn;
    private Set<String> toClear = new HashSet<String>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (!isExecutable()) {
            superuser.logout();
            throw new NotExecutableException();
        }

        try {
            UserManager uMgr = getUserManager(superuser);
            // create the testUser
            uid = "testUser" + UUID.randomUUID();
            creds = new SimpleCredentials(uid, uid.toCharArray());

            testUser = uMgr.createUser(uid, uid);
            if (!uMgr.isAutoSave()) {
                superuser.save();
            }
        } catch (Exception e) {
            superuser.logout();
            throw e;
        }
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            for (String path : toClear) {
                try {
                    AccessControlPolicy[] policies = acMgr.getPolicies(path);
                    for (AccessControlPolicy policy : policies) {
                        acMgr.removePolicy(path, policy);
                    }
                    superuser.save();
                } catch (RepositoryException e) {
                    // log error and ignore
                    log.debug(e.getMessage());
                }
            }

            if (testSession != null && testSession.isLive()) {
                testSession.logout();
            }
            if (testGroup != null && testUser != null) {
                if (testGroup.isDeclaredMember(testUser)) {
                    testGroup.removeMember(testUser);
                }
                testGroup.remove();
            }
            if (uid != null) {
                Authorizable a = getUserManager(superuser).getAuthorizable(uid);
                if (a != null) {
                    a.remove();
                }
            }
            if (!getUserManager(superuser).isAutoSave() && superuser.hasPendingChanges()) {
                superuser.save();
            }
        } finally {
            super.tearDown();
        }
    }

    protected static UserManager getUserManager(Session session) throws
            NotExecutableException {
        if (!(session instanceof JackrabbitSession)) {
            throw new NotExecutableException();
        }
        try {
            return ((JackrabbitSession) session).getUserManager();
        } catch (RepositoryException e) {
            throw new NotExecutableException();
        }
    }

    protected Session getTestSession() throws RepositoryException {
        if (testSession == null) {
            testSession = getHelper().getRepository().login(creds);
        }
        return testSession;
    }

    protected AccessControlManager getTestACManager() throws NotExecutableException, RepositoryException {
        if (testAccessControlManager == null) {
            testAccessControlManager = getAccessControlManager(getTestSession());
        }
        return testAccessControlManager;
    }
    
    protected Group getTestGroup() throws RepositoryException, NotExecutableException {
        if (testGroup == null) {
            // create the testGroup
            Principal principal = new TestPrincipal("testGroup" + UUID.randomUUID());
            UserManager umgr = getUserManager(superuser);
            testGroup = umgr.createGroup(principal);
            testGroup.addMember(testUser);
            if (!umgr.isAutoSave() && superuser.hasPendingChanges()) {
                superuser.save();
            }
        }
        return testGroup;
    }
    
    protected Node getTestNode() throws RepositoryException {
        if (trn == null) {
            trn = (Node) getTestSession().getItem(testRootNode.getPath());
        }
        return trn;
    }

    protected abstract boolean isExecutable();

    protected void checkReadOnly(String path) throws RepositoryException, NotExecutableException {
        Privilege[] privs = getTestACManager().getPrivileges(path);
        assertTrue(privs.length == 1);
        assertEquals(privilegesFromName(Privilege.JCR_READ)[0], privs[0]);
    }

    protected abstract JackrabbitAccessControlList getPolicy(AccessControlManager acMgr, String path, Principal princ) throws RepositoryException, NotExecutableException;
    protected abstract Map<String, Value> getRestrictions(Session session, String path) throws RepositoryException, NotExecutableException;

    protected JackrabbitAccessControlList modifyPrivileges(String path, String privilege, boolean isAllow) throws NotExecutableException, RepositoryException {
        return modifyPrivileges(path, testUser.getPrincipal(), privilegesFromName(privilege), isAllow, getRestrictions(superuser, path));
    }

    protected JackrabbitAccessControlList modifyPrivileges(String path, Principal principal, Privilege[] privileges, boolean isAllow, Map<String, Value> restrictions) throws NotExecutableException, RepositoryException {
        JackrabbitAccessControlList tmpl = getPolicy(acMgr, path, principal);
        tmpl.addEntry(principal, privileges, isAllow, restrictions);
        
        acMgr.setPolicy(tmpl.getPath(), tmpl);
        superuser.save();

        // remember for clean up during tearDown
        toClear.add(tmpl.getPath());
        return tmpl;
    }

    protected JackrabbitAccessControlList givePrivileges(String nPath, Privilege[] privileges,
                                                         Map<String, Value> restrictions)
            throws NotExecutableException, RepositoryException {
        return modifyPrivileges(nPath, testUser.getPrincipal(), privileges, true, restrictions);
    }

    protected JackrabbitAccessControlList givePrivileges(String nPath, Principal principal,
                                                         Privilege[] privileges, Map<String, Value> restrictions)
            throws NotExecutableException, RepositoryException {
        return modifyPrivileges(nPath, principal, privileges, true, restrictions);
    }

    protected JackrabbitAccessControlList withdrawPrivileges(String nPath, Privilege[] privileges, Map<String, Value> restrictions)
            throws NotExecutableException, RepositoryException {
        return modifyPrivileges(nPath, testUser.getPrincipal(), privileges, false, restrictions);
    }

    protected JackrabbitAccessControlList withdrawPrivileges(String nPath, Principal principal, Privilege[] privileges, Map<String, Value> restrictions)
            throws NotExecutableException, RepositoryException {
        return modifyPrivileges(nPath, principal, privileges, false, restrictions);
    }
}