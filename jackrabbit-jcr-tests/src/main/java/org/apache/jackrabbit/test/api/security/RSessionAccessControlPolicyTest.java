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

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>AccessControlPolicyTest</code>...
 */
public class RSessionAccessControlPolicyTest extends AbstractAccessControlTest {

    private String path;
    private Session readOnlySession;
    private AccessControlManager testAcMgr;

    protected void setUp() throws Exception {
        super.setUp();

        // policy-option is cover the by the 'OPTION_ACCESS_CONTROL_SUPPORTED' -> see super-class
        
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();
        path = n.getPath();

        readOnlySession = getHelper().getReadOnlySession();
        testAcMgr = getAccessControlManager(readOnlySession);
    }

    protected void tearDown() throws Exception {
        if (readOnlySession != null) {
            readOnlySession.logout();
        }
        super.tearDown();
    }

    public void testGetPolicy() throws RepositoryException, AccessDeniedException, NotExecutableException {
        try {
            testAcMgr.getPolicies(path);
            fail("read only session may not read AC content.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testGetEffectivePolicy() throws RepositoryException, AccessDeniedException, NotExecutableException {
        try {
            testAcMgr.getEffectivePolicies(path);
            fail("read only session may not read AC content.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testGetApplicablePolicies() throws RepositoryException, AccessDeniedException, NotExecutableException {
        try {
            testAcMgr.getApplicablePolicies(path);
            fail("read only session may not read AC content.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testSetPolicy() throws RepositoryException, AccessDeniedException, NotExecutableException {
        // retrieve valid policy using superuser session:
        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);
        if (!it.hasNext()) {
            throw new NotExecutableException();
        }

        try {
            testAcMgr.setPolicy(path, it.nextAccessControlPolicy());
            fail("read only session may not modify AC content.");
        } catch (AccessControlException e) {
            // success.
        }
    }

    public void testSetInvalidPolicy() throws RepositoryException, AccessDeniedException, NotExecutableException {
        try {
            testAcMgr.setPolicy(path, new AccessControlPolicy() {
                public String getName() throws RepositoryException {
                    return getClass().getName();
                }
                public String getDescription() throws RepositoryException {
                    return "";
                }
            });
            fail("Invalid policy may not be set by a READ-only session.");
        } catch (AccessControlException e) {
            // success.
        }
    }
}
