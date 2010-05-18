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

import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.AbstractUserTest;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.CompiledPermissions;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.core.security.user.UserAccessControlProvider;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * <code>UserAccessControlProviderTest</code>...
 */
public class UserAccessControlProviderTest extends AbstractUserTest {

    private Session s;
    private AccessControlProvider provider;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        RepositoryImpl repo = (RepositoryImpl) superuser.getRepository();
        String wspName = repo.getConfig().getSecurityConfig().getSecurityManagerConfig().getWorkspaceName();

        s = getHelper().getSuperuserSession(wspName);
        provider = new UserAccessControlProvider();
        provider.init(s, Collections.emptyMap());
    }

    @Override
    protected void cleanUp() throws Exception {
        if (provider != null) {
            provider.close();
        }
        if (s != null) {
            s.logout();
        }
        super.cleanUp();
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/JCR-2630">JCR-2630</a>
     */
    public void testNoNodeForPrincipal() throws RepositoryException {
        final Principal testPrincipal = getTestPrincipal();
        String path = "/home/users/t/" + testPrincipal.getName();
        while (s.nodeExists(path)) {
            path += "_";
        }
        final String principalPath = path;

        List<Set<Principal>> principalSets = new ArrayList<Set<Principal>>();
        principalSets.add(Collections.<Principal>singleton(testPrincipal));
        principalSets.add(Collections.<Principal>singleton(new ItemBasedPrincipal() {
            public String getPath() {
                return principalPath;
            }
            public String getName() {
                return testPrincipal.getName();
            }
        }));

        Path rootPath = ((SessionImpl) s).getQPath("/");
        for (Set<Principal> principals : principalSets) {
            CompiledPermissions cp = provider.compilePermissions(principals);

            assertFalse(cp.canReadAll());
            assertFalse(cp.grants(rootPath, Permission.READ));
            assertEquals(PrivilegeRegistry.NO_PRIVILEGE, cp.getPrivileges(rootPath));
            assertSame(CompiledPermissions.NO_PERMISSION, cp);
        }
    }

    public void testNodeRemovedForPrincipal() throws RepositoryException, NotExecutableException {
        Principal testPrincipal = getTestPrincipal();
        final User u = getUserManager(superuser).createUser(testPrincipal.getName(), "pw");
        save(superuser);

        Path rootPath = ((SessionImpl) s).getQPath("/");
        CompiledPermissions cp = null;
        try {
            Set<Principal> principals = Collections.singleton(u.getPrincipal());
            cp = provider.compilePermissions(principals);

            assertTrue(cp.canReadAll());
            assertTrue(cp.grants(rootPath, Permission.READ));
            assertNotSame(CompiledPermissions.NO_PERMISSION, cp);
        } finally {

            // remove the user to assert that the path doesn't point to an
            // existing node any more -> userNode cannot be resolved any more -> permissions denied.
            u.remove();
            save(superuser);

            if (cp != null) {
                assertFalse(cp.canReadAll());
                assertFalse(cp.grants(rootPath, Permission.READ));
                assertEquals(PrivilegeRegistry.NO_PRIVILEGE, cp.getPrivileges(rootPath));
            }
        }
    }
}