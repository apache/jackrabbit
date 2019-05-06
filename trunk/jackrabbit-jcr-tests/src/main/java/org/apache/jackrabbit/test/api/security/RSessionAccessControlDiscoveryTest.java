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
package org.apache.jackrabbit.test.api.security;

import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.test.NotExecutableException;

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

        readOnlySession = getHelper().getReadOnlySession();
        testAcMgr = getAccessControlManager(readOnlySession);
        testPath = testRootNode.getPath();
    }

    protected void tearDown() throws Exception {
        if (readOnlySession != null) {
            readOnlySession.logout();
        }
        super.tearDown();
    }

    public void testGetSupportedPrivileges() throws RepositoryException {
        Privilege[] privileges = testAcMgr.getSupportedPrivileges(testPath);
        assertNotNull("getSupportedPrivileges must return a non-null value even for read-only session.", privileges);
        assertTrue("getSupportedPrivileges must return a non-empty array even for read-only session.", privileges.length > 0);
    }

    public void testGetPrivileges() throws RepositoryException {
        List<Privilege> privs = Arrays.asList(testAcMgr.getPrivileges(testPath));
        Privilege readPrivilege = testAcMgr.privilegeFromName(Privilege.JCR_READ);
        assertTrue("A read-only session must have READ access to the test node.",
                privs.contains(readPrivilege));
    }

    public void testHasPrivileges() throws RepositoryException, NotExecutableException {
        Privilege priv = testAcMgr.privilegeFromName(Privilege.JCR_READ);
        assertTrue("Read-only session must have READ privilege on test node.",
                testAcMgr.hasPrivileges(testPath, new Privilege[] {priv}));
    }

    public void testNotHasPrivileges() throws RepositoryException, NotExecutableException {
        Privilege all = testAcMgr.privilegeFromName(Privilege.JCR_ALL);
        assertFalse("Read-only session must not have ALL privilege",
                testAcMgr.hasPrivileges(testPath, new Privilege[] {all}));
    }
}
