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
     * Test lock token functionality
     */
    public void testAddRemoveLockToken() throws Exception {
        // create new node
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.addMixin(mixReferenceable);
        n.addMixin(mixLockable);
        testRootNode.save();

        // lock node and get lock token
        Lock lock = n.lock(false, true);

        // assert: session must get a non-null lock token
        assertNotNull("session must get a non-null lock token",
                lock.getLockToken());

        // assert: session must hold lock token
        assertTrue("session must hold lock token",
                containsLockToken(superuser, lock.getLockToken()));

        // remove lock token
        String lockToken = lock.getLockToken();
        superuser.removeLockToken(lockToken);

        // assert: session must get a null lock token
        assertNull("session must get a null lock token",
                lock.getLockToken());

        // assert: session must still hold lock token
        assertFalse("session must not hold lock token",
                containsLockToken(superuser, lockToken));

        // assert: session unable to modify node
        try {
            n.addNode(nodeName2, testNodeType);
            fail("session unable to modify node");
        } catch (LockException e) {
            // expected
        }

        // add lock token
        superuser.addLockToken(lockToken);

        // assert: session must get a non-null lock token
        assertNotNull("session must get a non-null lock token",
                lock.getLockToken());

        // assert: session must hold lock token
        assertTrue("session must hold lock token",
                containsLockToken(superuser, lock.getLockToken()));

        // assert: session able to modify node
        n.addNode(nodeName2, testNodeType);
    }

    /**
     * Test session scope: other session may not access nodes that are
     * locked.
     */
    public void testNodeLocked() throws Exception {
        // create new node and lock it
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.addMixin(mixReferenceable);
        n1.addMixin(mixLockable);
        testRootNode.save();

        // lock node
        Lock lock = n1.lock(false, true);

        // assert: isLive must return true
        assertTrue("Lock must be live", lock.isLive());

        // create new session
        Session otherSuperuser = helper.getSuperuserSession();

        // get same node
        Node n2 = otherSuperuser.getNodeByUUID(n1.getUUID());

        // assert: lock token must be null for other session
        assertNull("Lock token must be null for other session",
                n2.getLock().getLockToken());

        // assert: modifying same node in other session must fail
        try {
            n2.addNode(nodeName2, testNodeType);
            fail("modifying same node in other session must fail");
        } catch (LockException e) {
            // expected
        }

        // logout
        otherSuperuser.logout();
    }

    /**
     * Test to get the lock holding node of a node
     */
    public void testGetNode() throws Exception {
        // create new node with a sub node and lock it
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.addMixin(mixLockable);
        Node n1Sub = n1.addNode(nodeName1, testNodeType);
        n1Sub.addMixin(mixLockable);
        testRootNode.save();

        // lock node
        n1.lock(true, true);

        assertEquals("getNode() must return the lock holder",
                n1.getPath(),
                n1.getLock().getNode().getPath());

        assertEquals("getNode() must return the lock holder",
                n1.getPath(),
                n1Sub.getLock().getNode().getPath());

        n1.unlock();
    }

    /**
     * Test if getLockOwner() returns the same value as returned by
     * Session.getUserId at the time that the lock was placed
     */
    public void testGetLockOwnerProperty() throws Exception {
        // create new node and lock it
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.addMixin(mixLockable);
        testRootNode.save();

        // lock node
        Lock lock = n1.lock(false, true);

        if (n1.getSession().getUserId() == null) {
            assertFalse("jcr:lockOwner must not exist if Session.getUserId() returns null",
                    n1.hasProperty(jcrLockOwner));
        } else {
            assertEquals("getLockOwner() must return the same value as stored " +
                    "in property " + jcrLockOwner + " of the lock holding " +
                    "node",
                    n1.getProperty(jcrLockOwner).getString(),
                    lock.getLockOwner());
        }
        n1.unlock();
    }

    /**
     * Test if getLockOwner() returns the same value as returned by
     * Session.getUserId at the time that the lock was placed
     */
    public void testGetLockOwner() throws Exception {
        // create new node and lock it
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.addMixin(mixLockable);
        testRootNode.save();

        // lock node
        Lock lock = n1.lock(false, true);

        assertEquals("getLockOwner() must return the same value as returned " +
                "by Session.getUserId at the time that the lock was placed",
                testRootNode.getSession().getUserId(),
                lock.getLockOwner());

        n1.unlock();
    }

    /**
     * Test if a shallow lock does not lock the child nodes of the locked node.
     */
    public void testShallowLock() throws Exception {
        // create new nodes
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.addMixin(mixReferenceable);
        n1.addMixin(mixLockable);
        Node n2 = n1.addNode(nodeName2, testNodeType);
        testRootNode.save();

        // lock parent node
        n1.lock(false, true);

        assertFalse("Shallow lock must not lock the child nodes of a node.",
                n2.isLocked());
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

        // assert: unable to deep lock parent node
        try {
            n1.lock(true, true);
            fail("unable to deep lock parent node");
        } catch (LockException e) {
            // expected
        }
    }

    /**
     * Test locks are released when session logs out
     */
    public void testLogout() throws Exception {
        // add node
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.addMixin(mixReferenceable);
        n1.addMixin(mixLockable);
        testRootNode.save();

        // create new session
        Session otherSuperuser = helper.getSuperuserSession();

        // get node created above
        Node n2 = otherSuperuser.getNodeByUUID(n1.getUUID());

        // lock node
        Lock lock = n2.lock(false, true);

        // assert: lock must be alive
        assertTrue("lock must be alive", lock.isLive());

        // assert: node must be locked
        assertTrue("node must be locked", n1.isLocked());

        // log out
        otherSuperuser.logout();

        // assert: lock must not be alive
        assertFalse("lock must not be alive", lock.isLive());

        // assert: node must not be locked
        assertFalse("node must not be locked", n1.isLocked());
    }

    /**
     * Test locks may be transferred to other session
     */
    public void testLockTransfer() throws Exception {
        // add node
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.addMixin(mixReferenceable);
        n1.addMixin(mixLockable);
        testRootNode.save();

        // create new session
        Session otherSuperuser = helper.getSuperuserSession();

        // get node created above
        Node n2 = otherSuperuser.getNodeByUUID(n1.getUUID());

        // lock node
        Lock lock = n2.lock(false, true);

        // assert: user must get non-null token
        assertNotNull("user must get non-null token", lock.getLockToken());

        // transfer to standard session
        String lockToken = lock.getLockToken();
        otherSuperuser.removeLockToken(lockToken);
        superuser.addLockToken(lockToken);

        // assert: user must get null token
        assertNull("user must get null token", lock.getLockToken());

        // assert: user must get non-null token
        assertNotNull("user must get non-null token",
                n1.getLock().getLockToken());

        // log out
        otherSuperuser.logout();
    }

    /**
     * Test open-scoped locks
     */
    public void testOpenScopedLocks() throws Exception {
        // add node
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.addMixin(mixReferenceable);
        n1.addMixin(mixLockable);
        testRootNode.save();

        // create new session
        Session otherSuperuser = helper.getSuperuserSession();

        // get node created above
        Node n2 = otherSuperuser.getNodeByUUID(n1.getUUID());

        // lock node
        Lock lock = n2.lock(false, false);

        // transfer to standard session
        String lockToken = lock.getLockToken();
        otherSuperuser.removeLockToken(lockToken);
        superuser.addLockToken(lockToken);

        // log out
        otherSuperuser.logout();

        // assert: node still locked
        assertTrue(n1.isLocked());
    }

    /**
     * Test refresh
     */
    public void testRefresh() throws Exception {
        // create new node
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.addMixin(mixReferenceable);
        n.addMixin(mixLockable);
        testRootNode.save();

        // lock node and get lock token
        Lock lock = n.lock(false, true);

        // assert: lock must be alive
        assertTrue("lock must be alive", lock.isLive());

        // assert: refresh must fail, since lock is still alive
        try {
            lock.refresh();
            fail("refresh must fail, since lock is still alive");
        } catch (LockException e) {
            // expected
        }

        // unlock node
        n.unlock();

        // assert: lock must not be alive
        assertFalse("lock must not be alive", lock.isLive());

        // refresh
        lock.refresh();

        // assert: lock must again be alive
        assertTrue("lock must again be alive", lock.isLive());
    }

    /**
     * Return a flag indicating whether the indicated session contains
     * a specific lock token
     */
    private boolean containsLockToken(Session session, String lockToken) {
        String[] lt = session.getLockTokens();
        for (int i = 0; i < lt.length; i++) {
            if (lt[i].equals(lockToken)) {
                return true;
            }
        }
        return false;
    }
}

