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

import org.apache.jackrabbit.core.security.TestPrincipal;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * <code>AccessControlEntryTest</code>...
 */
public class RSessionAccessControlEntryTest extends AbstractAccessControlTest {

    private static Logger log = LoggerFactory.getLogger(RSessionAccessControlEntryTest.class);

    private String path;
    private Session readOnlySession;
    private AccessControlManager testAcMgr;

    protected void setUp() throws Exception {
        super.setUp();

        // TODO: test if options is supporte
        //checkSupportedOption(superuser, Repository.OPTION_ACCESS_CONTROL_ENTRY_SUPPORTED);

        Node n = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();
        path = n.getPath();

        readOnlySession = helper.getReadOnlySession();
        testAcMgr = getAccessControlManager(readOnlySession);
    }

    protected void tearDown() throws Exception {
        if (readOnlySession != null) {
            readOnlySession.logout();
        }
        super.tearDown();
    }
    
    public void testGetAccessControlEntries() throws RepositoryException, AccessDeniedException, NotExecutableException {
        try {
            testAcMgr.getAccessControlEntries(path);
            fail("read only session may not read AC content.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testGetEffectiveAccessControlEntries() throws NotExecutableException, RepositoryException {
        try {
            testAcMgr.getEffectiveAccessControlEntries(path);
            fail("read only session may not read AC content.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testAddAccessControlEntry() throws NotExecutableException, RepositoryException {
        Privilege[] privs = testAcMgr.getSupportedPrivileges(path);
        try {
            testAcMgr.addAccessControlEntry(path, new TestPrincipal("principal"), privs);
            fail("read only session may not add an AC entry.");
        } catch (AccessDeniedException e) {
            // success
        }
    }
}
