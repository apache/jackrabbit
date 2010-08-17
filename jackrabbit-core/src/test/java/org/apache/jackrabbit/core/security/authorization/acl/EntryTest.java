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
package org.apache.jackrabbit.core.security.authorization.acl;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.authorization.AbstractEntryTest;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import java.security.Principal;

/**
 * <code>EntryTest</code>...
 */
public class EntryTest extends AbstractEntryTest {

    private ACLTemplate acl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        SessionImpl s = (SessionImpl) superuser;

        acl = new ACLTemplate(testPath, s.getPrincipalManager(), new PrivilegeRegistry(s), s.getValueFactory());
    }

    @Override
    protected JackrabbitAccessControlEntry createEntry(Principal principal, Privilege[] privileges, boolean isAllow)
            throws RepositoryException {
        return acl.createEntry(principal, privileges, isAllow);
    }

    public void testIsLocal() throws NotExecutableException, RepositoryException {
        ACLTemplate.Entry entry = (ACLTemplate.Entry) createEntry(new String[] {Privilege.JCR_READ}, true);

        // false since acl has been created from path only -> no id
        assertFalse(entry.isLocal(((NodeImpl) testRootNode).getNodeId()));
        // false since internal id is null -> will never match.
        assertFalse(entry.isLocal(new NodeId()));
    }

    public void testIsLocal2()  throws NotExecutableException, RepositoryException {
        String path = testRootNode.getPath();
        AccessControlPolicy[] acls = acMgr.getPolicies(path);
        if (acls.length == 0) {
            AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);
            if (!it.hasNext()) {
                throw new NotExecutableException();
            }
            acMgr.setPolicy(path, it.nextAccessControlPolicy());
            acls = acMgr.getPolicies(path);
        }

        assertTrue(acls[0] instanceof ACLTemplate);

        ACLTemplate acl = (ACLTemplate) acls[0];
        assertEquals(path, acl.getPath());       

        ACLTemplate.Entry entry = acl.createEntry(testPrincipal, new Privilege[] {acMgr.privilegeFromName(Privilege.JCR_READ)}, true);

        // node is must be present + must match to testrootnodes id.
        assertTrue(entry.isLocal(((NodeImpl) testRootNode).getNodeId()));
        // but not to a random id.
        assertFalse(entry.isLocal(new NodeId()));
    }
}