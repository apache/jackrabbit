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
package org.apache.jackrabbit.api.security;

import org.apache.jackrabbit.test.api.security.AbstractAccessControlTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;

import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.security.Privilege;
import java.util.Collections;
import java.util.Set;
import java.security.Principal;

/**
 * <code>JackrabbitAccessControlManagerTest</code>...
 */
public class JackrabbitAccessControlManagerTest extends AbstractAccessControlTest {

    Set<Principal> principals;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (!(acMgr instanceof JackrabbitAccessControlManager)) {
            throw new NotExecutableException();
        }

        Principal principal = EveryonePrincipal.getInstance();
        principals = Collections.singleton(principal);
    }

    public void testHasPrivilegeThrowsAccessDenied() throws RepositoryException {
        Session readOnly = getHelper().getReadOnlySession();
        JackrabbitAccessControlManager jacMgr = (JackrabbitAccessControlManager) readOnly.getAccessControlManager();
        try {
            jacMgr.hasPrivileges(testRoot, principals, new Privilege[] {jacMgr.privilegeFromName(Privilege.JCR_READ)});
            fail("ReadOnly session isn't allowed to determine the privileges of other principals.");
        } catch (AccessDeniedException e) {
            // success
        } finally {
            readOnly.logout();
        }
    }

    public void testGetPrivilegesThrowsAccessDenied() throws RepositoryException {
        Session readOnly = getHelper().getReadOnlySession();
        JackrabbitAccessControlManager jacMgr = (JackrabbitAccessControlManager) readOnly.getAccessControlManager();
        try {
            jacMgr.getPrivileges(testRoot, principals);
            fail("ReadOnly session isn't allowed to determine the privileges of other principals.");
        } catch (AccessDeniedException e) {
            // success
        } finally {
            readOnly.logout();
        }
    }

    public void testHasPrivilegesWithInvalidPath() throws RepositoryException {
        JackrabbitAccessControlManager jacMgr = (JackrabbitAccessControlManager) acMgr;
        String invalidPath = testRoot;
        while (superuser.nodeExists(invalidPath)) {
            invalidPath += "_";
        }
        
        try {
            jacMgr.hasPrivileges(invalidPath, principals, new Privilege[] {jacMgr.privilegeFromName(Privilege.JCR_READ)});
            fail("Invalid path must be detected");
        } catch (PathNotFoundException e) {
            // success
        }
    }

    public void testGetPrivilegesWithInvalidPath() throws RepositoryException {
        JackrabbitAccessControlManager jacMgr = (JackrabbitAccessControlManager) acMgr;
        String invalidPath = testRoot;
        while (superuser.nodeExists(invalidPath)) {
            invalidPath += "_";
        }

        try {
            jacMgr.getPrivileges(invalidPath, principals);
            fail("Invalid path must be detected");
        } catch (PathNotFoundException e) {
            // success
        }
    }

    // TODO add more tests
}