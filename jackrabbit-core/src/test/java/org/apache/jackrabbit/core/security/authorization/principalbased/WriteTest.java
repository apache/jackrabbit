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

import org.apache.jackrabbit.core.security.authorization.AbstractWriteTest;
import org.apache.jackrabbit.core.security.authorization.JackrabbitAccessControlList;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.Session;
import javax.jcr.Node;
import java.security.Principal;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * <code>EvaluationTest</code>...
 */
public class WriteTest extends AbstractWriteTest {

    private static Logger log = LoggerFactory.getLogger(WriteTest.class);

    private List toClear = new ArrayList();

    protected void setUp() throws Exception {
        super.setUp();

        // simple test to check if proper provider is present:
        getPolicy(acMgr, path, getTestUser().getPrincipal());
    }

    protected void clearACInfo() {
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
    }

    protected JackrabbitAccessControlList getPolicy(AccessControlManager acM, String path, Principal principal) throws RepositoryException, AccessDeniedException, NotExecutableException {
        if (acM instanceof JackrabbitAccessControlManager) {
            AccessControlPolicy[] policies = ((JackrabbitAccessControlManager) acM).getApplicablePolicies(principal);
            for (int i = 0; i < policies.length; i++) {
                if (policies[i] instanceof ACLTemplate) {
                    ACLTemplate acl = (ACLTemplate) policies[i];
                    toClear.add(acl.getPath());
                    return acl;
                }
            }
        }
        throw new NotExecutableException();
    }

    protected Map getRestrictions(Session s, String path) throws RepositoryException, NotExecutableException {
        if (s instanceof SessionImpl) {
            Map restr = new HashMap();
            restr.put(((SessionImpl) s).getJCRName(ACLTemplate.P_NODE_PATH), path);
            return restr;
        } else {
            throw new NotExecutableException();
        }
    }


    public void testAutocreatedProperties() throws RepositoryException, NotExecutableException {
        givePrivileges(path, testUser.getPrincipal(), privilegesFromName(PrivilegeRegistry.REP_WRITE), getRestrictions(superuser, path));

        // testuser is not allowed to READ the protected property jcr:created.
        Map restr = getRestrictions(superuser, path);
        restr.put(ACLTemplate.P_GLOB, GlobPattern.create("/afolder/jcr:created"));
        withdrawPrivileges(path, testUser.getPrincipal(), privilegesFromName(Privilege.JCR_READ), restr);

        // still: adding a nt:folder node should be possible
        Node n = getTestSession().getNode(path);
        Node folder = n.addNode("afolder", "nt:folder");

        assertFalse(folder.hasProperty("jcr:created"));
    }
    // TODO: add specific tests with other restrictions
}
