/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.test.api.lock;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeType;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.lock.Lock;

/**
 * <code>LockTest</code> contains the test cases for the methods
 * inside ...
 *
 * @test
 * @sources XATest.java
 * @executeClass org.apache.jackrabbit.test.api.xa.XATest
 * @keywords level2
 */
public class LockTest extends AbstractJCRTest {

    /**
     * @see junit.framework#runTest
     *
     * Make sure that tested repository supports locking
     */
    protected void runTest() throws Throwable {
        Repository rep = helper.getRepository();
        if (rep.getDescriptor(Repository.OPTION_LOCKING_SUPPORTED) != null) {
            super.runTest();
        }
    }

    /**
     * Test session scope: other session may not access nodes that are
     * locked.
     * @throws Exception
     */
    public void testSessionScope() throws Exception {
        // create new node and lock it
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.addMixin(mixReferenceable);
        n.addMixin(mixLockable);
        testRootNode.save();

        // remember uuid
        String uuid = n.getUUID();

        // lock node
        Lock lock = n.lock(false, true);

        // assertion: isLive must return true
        assertTrue("Lock must be live", lock.isLive());

        // assertion: lock token must not be null for holding session
        String lockToken = n.getLock().getLockToken();
        assertNotNull("Lock token must not be null for holding session", lockToken);

        // create new session
        Session otherSuperuser = helper.getSuperuserSession();

        // get same node
        n = otherSuperuser.getNodeByUUID(uuid);

        // assertion: lock token must be null for other session
        assertNull("Lock token must be null for other session",
                n.getLock().getLockToken());

        // assertion: modifying same node in other session must fail
        try {
            n.addNode(nodeName2);
            fail("Modified node locked by other session.");
        } catch (LockException e) {
            // expected
        }

        // logout
        otherSuperuser.logout();

        // unlock node
        superuser.getNodeByUUID(uuid).unlock();

        // assertion: isLive must return false
        assertFalse("Lock must be dead", lock.isLive());
    }
}

