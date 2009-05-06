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

import java.util.NoSuchElementException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlPolicyIterator;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>AccessControlPolicyIteratorTest</code>...
 */
public class AccessControlPolicyIteratorTest extends AbstractAccessControlTest {

    private String path;

    protected void setUp() throws Exception {
        super.setUp();

        // policy-option is cover the by the 'OPTION_ACCESS_CONTROL_SUPPORTED' -> see super-class

        Node n = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();
        path = n.getPath();
    }

    public void testGetSize() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanReadAc(path);

        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);
        // get size must succeed. its value however is indefined.
        long size = it.getSize();
        assertTrue("Size must be -1 or any value >= 0", size == -1 || size >= 0);
    }

    public void testGetInitialPosition() throws RepositoryException, AccessDeniedException, NotExecutableException {
        checkCanReadAc(path);

        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);
        // the initial position of the policy iterator must be 0.
        assertTrue("Initial position of AccessControlPolicyIterator must be 0.", it.getPosition() == 0);
    }

    public void testGetPosition() throws NotExecutableException, RepositoryException {
        checkCanReadAc(path);
        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);

        long position = 0;
        while (it.hasNext()) {
            assertEquals("Position must be adjusted during iteration.", position, it.getPosition());
            it.nextAccessControlPolicy();
            assertEquals("Position must be adjusted after calling next.", ++position, it.getPosition());
        }
    }

    public void testSkip() throws NotExecutableException, RepositoryException {
        checkCanReadAc(path);
        AccessControlPolicyIterator it = acMgr.getApplicablePolicies(path);

        long size = it.getSize();
        if (size > -1) {
            it.skip(size);
            assertFalse("After skipping all elements 'hasNext()' must return false", it.hasNext());

            try {
                it.nextAccessControlPolicy();
                fail("After skipping all 'nextAccessControlPolicy()' must fail.");
            } catch (NoSuchElementException e) {
                // success
            }
        } else {
            throw new NotExecutableException();
        }
    }
}