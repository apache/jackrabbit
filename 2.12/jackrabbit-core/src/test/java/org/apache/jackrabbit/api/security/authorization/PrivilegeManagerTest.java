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
package org.apache.jackrabbit.api.security.authorization;

import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>PrivilegeManagerTest</code>...
 */
public class PrivilegeManagerTest extends AbstractJCRTest {

    private NameResolver resolver;
    protected PrivilegeManager privilegeMgr;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        resolver = (SessionImpl) superuser;
        privilegeMgr = ((JackrabbitWorkspace) superuser.getWorkspace()).getPrivilegeManager();
    }

    protected void assertSamePrivilegeName(String expected, String present) throws NamespaceException, IllegalNameException {
        assertEquals("Privilege names are not the same", resolver.getQName(expected), resolver.getQName(present));
    }

    public void testRegisteredPrivileges() throws RepositoryException {
        Privilege[] ps = privilegeMgr.getRegisteredPrivileges();

        List<Privilege> l = new ArrayList<Privilege>(Arrays.asList(ps));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_READ)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_ADD_CHILD_NODES)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_REMOVE_CHILD_NODES)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_MODIFY_PROPERTIES)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_REMOVE_NODE)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_READ_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_MODIFY_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_WRITE)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_ALL)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_LIFECYCLE_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_LOCK_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_NODE_TYPE_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_RETENTION_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_VERSION_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(PrivilegeRegistry.REP_WRITE)));
        // including repo-level operation privileges
        assertTrue(l.remove(privilegeMgr.getPrivilege(NameConstants.JCR_NAMESPACE_MANAGEMENT.toString())));
        assertTrue(l.remove(privilegeMgr.getPrivilege(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT.toString())));
        assertTrue(l.remove(privilegeMgr.getPrivilege(NameConstants.JCR_WORKSPACE_MANAGEMENT.toString())));
        assertTrue(l.remove(privilegeMgr.getPrivilege(PrivilegeRegistry.REP_PRIVILEGE_MANAGEMENT)));

        assertTrue(l.isEmpty());
    }

    public void testAllPrivilege() throws RepositoryException {
        Privilege p = privilegeMgr.getPrivilege(Privilege.JCR_ALL);
        assertSamePrivilegeName(p.getName(), Privilege.JCR_ALL);
        assertTrue(p.isAggregate());
        assertFalse(p.isAbstract());

        List<Privilege> l = new ArrayList<Privilege>(Arrays.asList(p.getAggregatePrivileges()));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_READ)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_ADD_CHILD_NODES)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_REMOVE_CHILD_NODES)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_MODIFY_PROPERTIES)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_REMOVE_NODE)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_READ_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_MODIFY_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_LIFECYCLE_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_LOCK_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_NODE_TYPE_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_RETENTION_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_VERSION_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_WRITE)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(PrivilegeRegistry.REP_WRITE)));
        // including repo-level operation privileges
        assertTrue(l.remove(privilegeMgr.getPrivilege(NameConstants.JCR_NAMESPACE_MANAGEMENT.toString())));
        assertTrue(l.remove(privilegeMgr.getPrivilege(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT.toString())));
        assertTrue(l.remove(privilegeMgr.getPrivilege(NameConstants.JCR_WORKSPACE_MANAGEMENT.toString())));
        assertTrue(l.remove(privilegeMgr.getPrivilege(PrivilegeRegistry.REP_PRIVILEGE_MANAGEMENT)));
        
        assertTrue(l.isEmpty());

        l = new ArrayList<Privilege>(Arrays.asList(p.getDeclaredAggregatePrivileges()));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_READ)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_WRITE)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(PrivilegeRegistry.REP_WRITE)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_READ_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_MODIFY_ACCESS_CONTROL)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_LIFECYCLE_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_LOCK_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_RETENTION_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_VERSION_MANAGEMENT)));
        assertTrue(l.remove(privilegeMgr.getPrivilege(Privilege.JCR_NODE_TYPE_MANAGEMENT)));
        // including repo-level operation privileges
        assertTrue(l.remove(privilegeMgr.getPrivilege(NameConstants.JCR_NAMESPACE_MANAGEMENT.toString())));
        assertTrue(l.remove(privilegeMgr.getPrivilege(NameConstants.JCR_NODE_TYPE_DEFINITION_MANAGEMENT.toString())));
        assertTrue(l.remove(privilegeMgr.getPrivilege(NameConstants.JCR_WORKSPACE_MANAGEMENT.toString())));
        assertTrue(l.remove(privilegeMgr.getPrivilege(PrivilegeRegistry.REP_PRIVILEGE_MANAGEMENT)));
        
        assertTrue(l.isEmpty());
    }

    public void testGetPrivilegeFromName() throws AccessControlException, RepositoryException {
        Privilege p = privilegeMgr.getPrivilege(Privilege.JCR_READ);

        assertTrue(p != null);
        assertSamePrivilegeName(Privilege.JCR_READ, p.getName());
        assertFalse(p.isAggregate());

        p = privilegeMgr.getPrivilege(Privilege.JCR_WRITE);

        assertTrue(p != null);
        assertSamePrivilegeName(p.getName(), Privilege.JCR_WRITE);
        assertTrue(p.isAggregate());
    }

    public void testGetPrivilegesFromInvalidName() throws RepositoryException {
        try {
            privilegeMgr.getPrivilege("unknown");
            fail("invalid privilege name");
        } catch (AccessControlException e) {
            // OK
        }
    }

    public void testGetPrivilegesFromEmptyNames() {
        try {
            privilegeMgr.getPrivilege("");
            fail("invalid privilege name array");
        } catch (AccessControlException e) {
            // OK
        } catch (RepositoryException e) {
            // OK
        }
    }

    public void testGetPrivilegesFromNullNames() {
        try {
            privilegeMgr.getPrivilege(null);
            fail("invalid privilege name (null)");
        } catch (Exception e) {
            // OK
        }
    }

    public void testRegisterPrivilegeWithIllegalName() throws RepositoryException {
        Map<String, String[]> illegal = new HashMap<String, String[]>();
        illegal.put("invalid:privilegeName", new String[0]);
        illegal.put("jcr:newPrivilege", new String[] {"invalid:privilegeName"});
        illegal.put(".e:privilegeName", new String[0]);
        illegal.put("jcr:newPrivilege", new String[] {".e:privilegeName"});

        for (String illegalName : illegal.keySet()) {
            try {
                privilegeMgr.registerPrivilege(illegalName, true, illegal.get(illegalName));
                fail("Illegal name -> Exception expected");
            } catch (NamespaceException e) {
                // success
            } catch (IllegalNameException e) {
                // success
            }
        }
    }
}