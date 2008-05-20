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
package org.apache.jackrabbit.api.jsr283.security;

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.List;

/**
 * <code>RSessionAccessControlDiscoveryTest</code>: A read-only session must
 * be able to call 'hasPrivilege' and 'getPrivileges' and 'getSupportedPrivileges'
 * without access denied exception
 */
public class RSessionAccessControlDiscoveryTest extends AbstractAccessControlTest {

    private Session readOnlySession;
    private AccessControlManager testAcMgr;
    private String testPath;

    protected void setUp() throws Exception {
        super.setUp();

        readOnlySession = helper.getReadOnlySession();
        testAcMgr = getAccessControlManager(readOnlySession);
        testPath = testRootNode.getPath();
    }

    protected void tearDown() throws Exception {
        if (readOnlySession != null) {
            readOnlySession.logout();
        }
        super.tearDown();
    }

    private Privilege getPrivilege(String name) throws RepositoryException, NotExecutableException {
        Privilege[] privileges = acMgr.getSupportedPrivileges(testPath);
        for (int i = 0; i < privileges.length; i++) {
            if (name.equals(privileges[i].getName())) {
                return privileges[i];
            }
        }
        throw new NotExecutableException();
    }

    public void testGetSupportedPrivileges() throws RepositoryException {
        Privilege[] privileges = testAcMgr.getSupportedPrivileges(testPath);
        assertNotNull("getSupportedPrivileges must return a non-null value even for read-only session.", privileges);
        assertTrue("getSupportedPrivileges must return a non-empty array even for read-only session.", privileges.length > 0);
    }

    public void testGetPrivileges() throws RepositoryException {
        Privilege[] privs = testAcMgr.getPrivileges(testPath);
        List names = new ArrayList(privs.length);
        for (int i = 0; i < privs.length; i++) {
            names.add(privs[i].getName());
        }
        assertTrue("A read-only session must have READ access to the test node.",
                names.contains(Privilege.READ));
    }

    public void testHasPrivileges() throws RepositoryException, NotExecutableException {
        Privilege priv = getPrivilege(Privilege.READ);
        assertTrue("Read-only session must have READ privilege on test node.",
                testAcMgr.hasPrivileges(testPath, new Privilege[] {priv}));
    }

    public void testNotHasPrivileges() throws RepositoryException, NotExecutableException {
        Privilege all = getPrivilege(Privilege.ALL);
        assertFalse("Read-only session must not have ALL privilege",
                testAcMgr.hasPrivileges(testPath, new Privilege[] {all}));
    }
}
