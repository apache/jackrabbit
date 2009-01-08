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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.api.jsr283.security.AbstractAccessControlTest;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.core.SessionImpl;

import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.jcr.RepositoryException;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.security.Principal;

/** <code>AbstractEvaluationTest</code>... */
public abstract class AbstractEvaluationTest extends AbstractAccessControlTest {

    private static Logger log = LoggerFactory.getLogger(AbstractEvaluationTest.class);

    protected User testUser;
    protected Credentials creds;
    
    private SessionImpl testSession;
    private AccessControlManager testAccessControlManager;
    private Node trn;
    private Set toClear = new HashSet();

    protected void setUp() throws Exception {
        super.setUp();
        UserManager uMgr = getUserManager(superuser);

        // create the testUser
        String uid = "testUser" + UUID.randomUUID();
        creds = new SimpleCredentials(uid, uid.toCharArray());

        testUser = uMgr.createUser(uid, uid);
    }

    protected void tearDown() throws Exception {
        for (Iterator it = toClear.iterator(); it.hasNext();) {
            String path = it.next().toString();
            try {
                AccessControlPolicy[] policies = acMgr.getPolicies(path);
                for (int i = 0; i < policies.length; i++) {
                    acMgr.removePolicy(path, policies[i]);
                    superuser.save();
                }
            } catch (RepositoryException e) {
                // log error and ignore
                log.error(e.getMessage());
            }
        }

        if (testSession != null && testSession.isLive()) {
            testSession.logout();
        }
        if (testUser != null) {
            testUser.remove();
        }
        super.tearDown();
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

    protected SessionImpl getTestSession() throws RepositoryException {
        if (testSession == null) {
            // TODO: remove cast once 283 is released.
            testSession = (SessionImpl) helper.getRepository().login(creds);
        }
        return testSession;
    }

    protected AccessControlManager getTestACManager() throws NotExecutableException, RepositoryException {
        if (testAccessControlManager == null) {
            testAccessControlManager = getAccessControlManager(getTestSession());
        }
        return testAccessControlManager;
    }
    
    protected Node getTestNode() throws RepositoryException {
        if (trn == null) {
            trn = (Node) getTestSession().getItem(testRootNode.getPath());
        }
        return trn;
    }

    
    protected void checkReadOnly(String path) throws RepositoryException, NotExecutableException {
        Privilege[] privs = getTestACManager().getPrivileges(path);
        assertTrue(privs.length == 1);
        assertEquals(privilegesFromName(Privilege.JCR_READ)[0], privs[0]);
    }

    protected abstract JackrabbitAccessControlList getPolicy(AccessControlManager acMgr, String path, Principal princ) throws RepositoryException, NotExecutableException;
    protected abstract Map getRestrictions(Session session, String path) throws RepositoryException, NotExecutableException;

    protected JackrabbitAccessControlList modifyPrivileges(String path, String privilege, boolean isAllow) throws NotExecutableException, RepositoryException {
        JackrabbitAccessControlList tmpl = getPolicy(acMgr, path, testUser.getPrincipal());
        tmpl.addEntry(testUser.getPrincipal(), privilegesFromName(privilege), isAllow, getRestrictions(superuser, path));
        acMgr.setPolicy(tmpl.getPath(), tmpl);
        superuser.save();

        // remember for clean up during teardown
        toClear.add(tmpl.getPath());
        return tmpl;
    }
}