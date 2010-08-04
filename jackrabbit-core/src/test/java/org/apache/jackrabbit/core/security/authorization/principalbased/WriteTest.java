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
package org.apache.jackrabbit.core.security.authorization.principalbased;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlPolicy;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AbstractWriteTest;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.TestPrincipal;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.util.Text;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.Map;

/**
 * <code>EvaluationTest</code>...
 */
public class WriteTest extends AbstractWriteTest {

    protected boolean isExecutable() {
        return EvaluationUtil.isExecutable((SessionImpl) superuser, acMgr);
    }

    protected JackrabbitAccessControlList getPolicy(AccessControlManager acM, String path, Principal principal) throws RepositoryException, AccessDeniedException, NotExecutableException {
        return EvaluationUtil.getPolicy(acM, path, principal);
    }

    protected Map<String, Value> getRestrictions(Session s, String path) throws RepositoryException, NotExecutableException {
        return EvaluationUtil.getRestrictions(s, path);
    }


    public void testAutocreatedProperties() throws RepositoryException, NotExecutableException {
        givePrivileges(path, testUser.getPrincipal(), privilegesFromName(PrivilegeRegistry.REP_WRITE), getRestrictions(superuser, path));

        // test user is not allowed to READ the protected property jcr:created.
        Map<String, Value> restr = getRestrictions(superuser, path);
        restr.put(((SessionImpl) superuser).getJCRName(ACLTemplate.P_GLOB), superuser.getValueFactory().createValue("/afolder/jcr:created"));
        withdrawPrivileges(path, testUser.getPrincipal(), privilegesFromName(Privilege.JCR_READ), restr);

        // still: adding a nt:folder node should be possible
        Node n = getTestSession().getNode(path);
        Node folder = n.addNode("afolder", "nt:folder");

        assertFalse(folder.hasProperty("jcr:created"));
    }

    public void testEditor() throws NotExecutableException, RepositoryException {
        UserManager uMgr = getUserManager(superuser);        
        User u = null;
        try {
            u = uMgr.createUser("t", "t");
            if (!uMgr.isAutoSave()) {
                superuser.save();
            }

            Principal p = u.getPrincipal();

            JackrabbitAccessControlManager acMgr = (JackrabbitAccessControlManager) getAccessControlManager(superuser);
            JackrabbitAccessControlPolicy[] acls = acMgr.getApplicablePolicies(p);

            assertEquals(1, acls.length);
            assertTrue(acls[0] instanceof ACLTemplate);

            // access again
            acls = acMgr.getApplicablePolicies(p);

            assertEquals(1, acls.length);            
            assertEquals(1, acMgr.getApplicablePolicies(acls[0].getPath()).getSize());

            assertEquals(0, acMgr.getPolicies(p).length);
            assertEquals(0, acMgr.getPolicies(acls[0].getPath()).length);

            acMgr.setPolicy(acls[0].getPath(), acls[0]);

            assertEquals(0, acMgr.getApplicablePolicies(p).length);
            assertEquals(1, acMgr.getPolicies(p).length);
            assertEquals(1, acMgr.getPolicies(acls[0].getPath()).length);
        } finally {
            superuser.refresh(false);
            if (u != null) {
                u.remove();
                if (!uMgr.isAutoSave()) {
                    superuser.save();
                }
            }
        }
    }

    public void testEditor2() throws NotExecutableException, RepositoryException {
        UserManager uMgr = getUserManager(superuser);
        User u = null;
        User u2 = null;
        try {
            u = uMgr.createUser("t", "t");
            u2 = uMgr.createUser("tt", "tt", new TestPrincipal("tt"), "t/tt");
            if (!uMgr.isAutoSave()) {
                superuser.save();
            }

            Principal p = u.getPrincipal();
            Principal p2 = u2.getPrincipal();

            if (p instanceof ItemBasedPrincipal && p2 instanceof ItemBasedPrincipal &&
                    Text.isDescendant(((ItemBasedPrincipal) p).getPath(), ((ItemBasedPrincipal) p2).getPath())) {

                JackrabbitAccessControlManager acMgr = (JackrabbitAccessControlManager) getAccessControlManager(superuser);

                JackrabbitAccessControlPolicy[] acls = acMgr.getApplicablePolicies(p2);
                acMgr.setPolicy(acls[0].getPath(), acls[0]);

                acls = acMgr.getApplicablePolicies(p);
                String path = acls[0].getPath();

                Node n = superuser.getNode(path);
                assertEquals("rep:PrincipalAccessControl", n.getPrimaryNodeType().getName());
            } else {
                throw new NotExecutableException();
            }
        } finally {
            superuser.refresh(false);
            if (u2 != null) u2.remove();
            if (u != null) u.remove();
            if (!uMgr.isAutoSave()) {
                superuser.save();
            }
        }

    }
    // TODO: add specific tests with other restrictions
}
