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

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.lock.Lock;

/**
 * <code>LockTest</code> contains the test cases for the lock support in
 * the JCR specification.
 * <p/>
 * Configuration requirements:<br/>
 * The node at {@link #testRoot} must allow child nodes of type
 * {@link #testNodeType} with name {@link #nodeName1}. The {@link #testNodeType}
 * must allow child nodes of the same node type. If {@link #testNodeType} is not
 * mix:referenceable and mix:lockable the two mixin types are added to the node
 * instance created with {@link #testNodeType}.
 *
 * @test
 * @sources LockTest.java
 * @executeClass org.apache.jackrabbit.test.api.lock.LockTest
 * @keywords locking
 */
public class LockTest extends AbstractJCRTest {

    /**
     * Test session scope: other session may not access nodes that are
     * locked.
     */
    public void testSessionScope() throws Exception {
        // create new node and lock it
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        if (!n.isNodeType(mixReferenceable)) {
            n.addMixin(mixReferenceable);
        }
        if (!n.isNodeType(mixLockable)) {
            n.addMixin(mixLockable);
        }
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

    /**
     * Test parent/child lock
     */
    public void testParentChildLock() throws Exception {
        // create new nodes
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.addMixin(mixReferenceable);
        n1.addMixin(mixLockable);
        Node n2 = n1.addNode(nodeName2, testNodeType);
        n2.addMixin(mixReferenceable);
        n2.addMixin(mixLockable);
        testRootNode.save();

        // lock parent node
        n1.lock(false, true);

        // lock child node
        n2.lock(false, true);

        // unlock parent node
        n1.unlock();

        // child node must still hold lock
        assertTrue("child node must still hold lock", n2.holdsLock());
    }

    /**
     * Test parent/child lock
     */
    public void testParentChildDeepLock() throws Exception {
        // create new nodes
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.addMixin(mixReferenceable);
        n1.addMixin(mixLockable);
        Node n2 = n1.addNode(nodeName2, testNodeType);
        n2.addMixin(mixReferenceable);
        n2.addMixin(mixLockable);
        testRootNode.save();

        // lock child node
        n2.lock(false, true);

        // lock parent node
        n1.lock(true, false);

        // unlock child node
        n2.unlock();

        // parent node must still hold lock
        assertTrue("parent node must still hold lock", n1.holdsLock());
    }
}

