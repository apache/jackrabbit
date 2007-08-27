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
package org.apache.jackrabbit.test.api.lock;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>LockTest</code> contains the test cases for the lock support in
 * the JCR specification.
 *
 * @test
 * @sources LockTest.java
 * @executeClass org.apache.jackrabbit.test.api.lock.LockTest
 * @keywords locking
 *
 * @tck.config testroot must allow child nodes of type <code>nodetype</code>
 * @tck.config nodetype nodetype which is lockable or allows to add mix:lockable.
 * The node must also allow child nodes with the same node type as itself.
 * @tck.config nodename1 name of a lockable child node of type <code>nodetype</code>.
 */
public class LockTest extends AbstractJCRTest {

    /**
     * Test lock token functionality
     */
    public void testAddRemoveLockToken() throws Exception {
        // create new node
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        if (needsMixin(n, mixLockable)) {
            n.addMixin(mixLockable);
        }
        testRootNode.save();

        // lock node and get lock token
        Lock lock = n.lock(false, false);
        String lockToken = lock.getLockToken();
        try {
            // assert: session must get a non-null lock token
            assertNotNull("session must get a non-null lock token", lockToken);

            // assert: session must hold lock token
            assertTrue("session must hold lock token", containsLockToken(superuser, lockToken));

            // remove lock token
            superuser.removeLockToken(lockToken);
            // assert: session must get a null lock token
            assertNull("session must get a null lock token", lock.getLockToken());

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
        } finally {
            // make sure lock token is added even if test fail
            superuser.addLockToken(lockToken);
        }
    }

    /**
     * Test session scope: other session may not access nodes that are
     * locked.
     */
    public void testNodeLocked() throws Exception {
        // create new node and lock it
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        if (needsMixin(n1, mixLockable)) {
            n1.addMixin(mixLockable);
        }
        testRootNode.save();

        // lock node
        Lock lock = n1.lock(false, true);

        // assert: isLive must return true
        assertTrue("Lock must be live", lock.isLive());

        // create new session
        Session otherSuperuser = helper.getSuperuserSession();

        try {
            // get same node
            Node n2 = (Node) otherSuperuser.getItem(n1.getPath());

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
        } finally {
            otherSuperuser.logout();
        }
    }

    /**
     * Test to get the lock holding node of a node
     */
    public void testGetNode() throws Exception {
        // create new node with a sub node and lock it
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        if (needsMixin(n1, mixLockable)) {
            n1.addMixin(mixLockable);
        }
        Node n1Sub = n1.addNode(nodeName1, testNodeType);
        if (needsMixin(n1Sub, mixLockable)) {
            n1Sub.addMixin(mixLockable);
        }
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
        if (needsMixin(n1, mixLockable)) {
            n1.addMixin(mixLockable);
        }
        testRootNode.save();

        // lock node
        Lock lock = n1.lock(false, true);

        if (n1.getSession().getUserID() == null) {
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
        if (needsMixin(n1, mixLockable)) {
            n1.addMixin(mixLockable);
        }
        testRootNode.save();

        // lock node
        Lock lock = n1.lock(false, true);

        assertEquals("getLockOwner() must return the same value as returned " +
                "by Session.getUserId at the time that the lock was placed",
                testRootNode.getSession().getUserID(),
                lock.getLockOwner());

        n1.unlock();
    }

    /**
     * Test if a shallow lock does not lock the child nodes of the locked node.
     */
    public void testShallowLock() throws Exception {
        // create new nodes
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        if (needsMixin(n1, mixLockable)) {
            n1.addMixin(mixLockable);
        }
        Node n2 = n1.addNode(nodeName2, testNodeType);
        testRootNode.save();

        // lock parent node
        n1.lock(false, true);

        assertFalse("Shallow lock must not lock the child nodes of a node.",
                n2.isLocked());
    }

    /**
     * Test if it is possible to lock and unlock a checked-in node.
     */
    public void testCheckedIn()
            throws NotExecutableException, RepositoryException {

        if (!isSupported(Repository.OPTION_LOCKING_SUPPORTED)) {
            throw new NotExecutableException("Versioning is not supported.");
        }

        // create a node that is lockable and versionable
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        if (needsMixin(node, mixLockable)) {
            node.addMixin(mixLockable);
        }
        // try to make it versionable if it is not
        if (!node.isNodeType(mixVersionable)) {
            if (node.canAddMixin(mixVersionable)) {
                node.addMixin(mixVersionable);
            } else {
                throw new NotExecutableException("Node " + nodeName1 + " is " +
                        "not versionable and does not allow to add " +
                        "mix:versionable");
            }
        }
        testRootNode.save();

        node.checkin();

        try {
            node.lock(false, false);
        }
        catch (RepositoryException ex) {
            // repository may not allow shallow locks on this resource
            // retry with a deep lock
            node.lock(true, false);
        }
        
        assertTrue("Locking of a checked-in node failed.",
                node.isLocked());

        node.unlock();
        assertFalse("Unlocking of a checked-in node failed.",
                node.isLocked());
    }

    /**
     * Test parent/child lock
     */
    public void testParentChildLock() throws Exception {
        // create new nodes
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        if (needsMixin(n1, mixLockable)) {
            n1.addMixin(mixLockable);
        }
        Node n2 = n1.addNode(nodeName2, testNodeType);
        if (needsMixin(n2, mixLockable)) {
            n2.addMixin(mixLockable);
        }
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
        if (needsMixin(n1, mixLockable)) {
            n1.addMixin(mixLockable);
        }
        Node n2 = n1.addNode(nodeName2, testNodeType);
        if (needsMixin(n2, mixLockable)) {
            n2.addMixin(mixLockable);
        }
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
     * Test Lock.isDeep()
     */
    public void testIsDeep() throws RepositoryException {
        // create two lockable nodes
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        if (needsMixin(n1, mixLockable)) {
            n1.addMixin(mixLockable);
        }
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        if (needsMixin(n2, mixLockable)) {
            n2.addMixin(mixLockable);
        }
        testRootNode.save();

        // lock node 1 "undeeply"
        Lock lock1 = n1.lock(false, true);
        assertFalse("Lock.isDeep() must be false if the lock has not been set " +
                "as not deep",
                lock1.isDeep());

        // lock node 2 "deeply"
        Lock lock2 = n2.lock(true, true);
        assertTrue("Lock.isDeep() must be true if the lock has been set " +
                "as deep",
                lock2.isDeep());
    }

    /**
     * Test Lock.isSessionScoped()
     */
    public void testIsSessionScoped() throws RepositoryException {
        // create two lockable nodes
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        if (needsMixin(n1, mixLockable)) {
            n1.addMixin(mixLockable);
        }
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        if (needsMixin(n2, mixLockable)) {
            n2.addMixin(mixLockable);
        }
        testRootNode.save();

        // lock node 1 session-scoped
        Lock lock1 = n1.lock(false, true);
        assertTrue("Lock.isSessionScoped() must be true if the lock " +
                "is session-scoped",
                lock1.isSessionScoped());

        // lock node 2 open-scoped
        Lock lock2 = n2.lock(false, false);
        assertFalse("Lock.isSessionScoped() must be false if the lock " +
                "is open-scoped",
                lock2.isSessionScoped());

        n2.unlock();
    }

    /**
     * Test locks are released when session logs out
     */
    public void testLogout() throws Exception {
        // add node
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        if (needsMixin(n1, mixLockable)) {
            n1.addMixin(mixLockable);
        }
        testRootNode.save();

        // create new session
        Session otherSuperuser = helper.getSuperuserSession();

        Lock lock;
        try {
            // get node created above
            Node n2 = (Node) otherSuperuser.getItem(n1.getPath());

            // lock node
            lock = n2.lock(false, true);

            // assert: lock must be alive
            assertTrue("lock must be alive", lock.isLive());

            // assert: node must be locked
            assertTrue("node must be locked", n1.isLocked());
        } finally {
            // log out
            otherSuperuser.logout();
        }


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
        if (needsMixin(n1, mixLockable)) {
            n1.addMixin(mixLockable);
        }
        testRootNode.save();

        // create new session
        Session otherSuperuser = helper.getSuperuserSession();

        try {
            // get node created above
            Node n2 = (Node) otherSuperuser.getItem(n1.getPath());

            // lock node
            Lock lock = n2.lock(false, false);

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
        } finally {
            // log out
            otherSuperuser.logout();
            // unlock again
            n1.unlock();
        }
    }

    /**
     * Test open-scoped locks
     */
    public void testOpenScopedLocks() throws Exception {
        // add node
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        if (needsMixin(n1, mixLockable)) {
            n1.addMixin(mixLockable);
        }
        testRootNode.save();

        // create new session
        Session otherSuperuser = helper.getSuperuserSession();

        try {
            // get node created above
            Node n2 = (Node) otherSuperuser.getItem(n1.getPath());

            // lock node
            Lock lock = n2.lock(false, false);

            // transfer to standard session
            String lockToken = lock.getLockToken();
            otherSuperuser.removeLockToken(lockToken);
            superuser.addLockToken(lockToken);
        } finally {
            // log out
            otherSuperuser.logout();
        }

        // assert: node still locked
        assertTrue(n1.isLocked());
    }

    /**
     * Test refresh
     */
    public void testRefresh() throws Exception {
        // create new node
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        if (needsMixin(n, mixLockable)) {
            n.addMixin(mixLockable);
        }
        testRootNode.save();

        // lock node and get lock token
        Lock lock = n.lock(false, true);

        // assert: lock must be alive
        assertTrue("lock must be alive", lock.isLive());

        // assert: refresh must succeed
        lock.refresh();

        // unlock node
        n.unlock();

        // assert: lock must not be alive
        assertFalse("lock must not be alive", lock.isLive());
    }

    /**
     * Test refresh
     */
    public void testRefreshNotLive() throws Exception {
        // create new node
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        if (needsMixin(n, mixLockable)) {
            n.addMixin(mixLockable);
        }
        testRootNode.save();

        // lock node and get lock token
        Lock lock = n.lock(false, true);

        // assert: lock must be alive
        assertTrue("lock must be alive", lock.isLive());

        // unlock node
        n.unlock();

        // assert: lock must not be alive
        assertFalse("lock must not be alive", lock.isLive());

        // refresh
        try {
            lock.refresh();
            fail("Refresh on a lock that is not alive must fail");
        } catch (LockException e) {
            // success
        }
    }

    /**
     * Test getLock
     */
    public void testGetLock() throws Exception {
        // create new nodes
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        if (needsMixin(n1, mixLockable)) {
            n1.addMixin(mixLockable);
        }
        Node n2 = n1.addNode(nodeName2, testNodeType);
        if (needsMixin(n2, mixLockable)) {
            n2.addMixin(mixLockable);
        }
        testRootNode.save();

        // deep lock parent node
        n1.lock(true, true);

        // get lock on child node
        Lock lock = n2.getLock();

        // lock holding node must be parent
        assertTrue("lock holding node must be parent", lock.getNode().isSame(n1));
    }

    /**
     * Tests if a locked, checked-in node can be unlocked
     */
    public void testCheckedInUnlock() throws Exception {
        if (!isSupported(Repository.OPTION_VERSIONING_SUPPORTED)) {
            throw new NotExecutableException("Repository does not support versioning.");
        }

        // set up versionable and lockable node
        Node testNode = testRootNode.addNode(nodeName1, testNodeType);
        if (needsMixin(testNode, mixLockable)) {
            testNode.addMixin(mixLockable);
        }
        testNode.addMixin(mixVersionable);
        testRootNode.save();

        // lock and check-in
        try {
            testNode.lock(false, false);
        }
        catch (RepositoryException ex) {
            // repository may not allow shallow locks on this resource
            // retry with a deep lock
            testNode.lock(true, false);
        }
        testNode.save();
        testNode.checkin();

        // do the unlock
        testNode.unlock();
        assertFalse("Could not unlock a locked, checked-in node", testNode.holdsLock());
    }

    /**
     * Tests if locks are maintained when child nodes are reordered
     */
    public void testReorder() throws Exception {
      
        Node testNode = setUpSameNameSiblings();

        // lock last node (3)
        testNode.lock(false, true);

        // assert: last node locked
        assertTrue("Third child node locked",
                testRootNode.getNode(nodeName1 + "[3]").isLocked());

        // move last node in front of first
        testRootNode.orderBefore(nodeName1 + "[3]", nodeName1 + "[1]");
        testRootNode.save();

        // assert: first node locked
        assertTrue("First child node locked",
                testRootNode.getNode(nodeName1 + "[1]").isLocked());
    }

    /**
     * Tests if locks are maintained when child nodes are reordered
     */
    public void testReorder2() throws Exception {
        
        setUpSameNameSiblings();

        // lock first node (1)
        testRootNode.getNode(nodeName1 + "[1]").lock(false, true);

        // assert: first node locked
        assertTrue("First child node locked",
                testRootNode.getNode(nodeName1 + "[1]").isLocked());

        // move first node to last
        testRootNode.orderBefore(nodeName1 + "[1]", null);
        testRootNode.save();

        // assert: third node locked
        assertTrue("Third child node locked",
                testRootNode.getNode(nodeName1 + "[3]").isLocked());
    }

    /**
     * Tests if move preserves lock state (JIRA issue JCR-207). A node that has
     * been locked must still appear locked when it has been moved or renamed,
     * regardless whether the changes have already been made persistent.
     */
    public void testMoveLocked() throws Exception {
        Session session = testRootNode.getSession();

        // create two nodes, parent and child
        Node testNode1 = testRootNode.addNode(nodeName1, testNodeType);
        if (needsMixin(testNode1, mixLockable)) {
            testNode1.addMixin(mixLockable);
        }
        Node testNode2 = testNode1.addNode(nodeName2, testNodeType);
        if (needsMixin(testNode2, mixLockable)) {
            testNode2.addMixin(mixLockable);
        }
        testRootNode.save();

        // lock child node
        testNode2.lock(false, true);

        // assert: child node locked
        assertTrue("Child node locked", testNode2.isLocked());

        // move child node up
        String newPath = testRootNode.getPath() + "/" + testNode2.getName();
        session.move(testNode2.getPath(), newPath);

        // assert: child node locked, before save
        assertTrue("Child node locked before save", testNode2.isLocked());
        session.save();

        // assert: child node locked, after save
        assertTrue("Child node locked after save", testNode2.isLocked());
    }

    /**
     * Tests if unlocking the first of two locked same-name sibling nodes does
     * not unlock the second (JIRA issue JCR-284).
     */
    public void testUnlockSameNameSibling() throws RepositoryException, NotExecutableException {
        Session session = testRootNode.getSession();

        Node n1, n2;
        
        try {
            // create two same-name sibling nodes
            n1 = testRootNode.addNode(nodeName1, testNodeType);
            n2 = testRootNode.addNode(nodeName1, testNodeType);
            session.save();
        }
        catch (ItemExistsException ex) {
            throw new NotExecutableException("Node does not seem to allow same name siblings");
        }

        n1.addMixin("mix:lockable");
        n2.addMixin("mix:lockable");
        session.save();
        
        // lock both nodes
        n1.lock(true, true);
        n2.lock(true, true);

        // assert: both nodes are locked
        assertTrue("First node locked: ", n1.isLocked());
        assertTrue("Second node locked: ", n2.isLocked());

        // unlock first sibling
        n1.unlock();

        // assert: first node unlocked, second node still locked
        assertFalse("First node unlocked: ", n1.isLocked());
        assertTrue("Second node locked: ", n2.isLocked());
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
    
    /**
     * Create three child nodes with identical names
     */
    private Node setUpSameNameSiblings() throws RepositoryException, NotExecutableException {
        // create three lockable nodes with same name
        try {
            Node testNode = testRootNode.addNode(nodeName1, testNodeType);
            if (needsMixin(testNode, mixLockable)) {
                testNode.addMixin(mixLockable);
            }
            testNode = testRootNode.addNode(nodeName1, testNodeType);
            if (needsMixin(testNode, mixLockable)) {
                testNode.addMixin(mixLockable);
            }
            testNode = testRootNode.addNode(nodeName1, testNodeType);
            if (needsMixin(testNode, mixLockable)) {
                testNode.addMixin(mixLockable);
            }
            testRootNode.save();
            return testNode;
        }
        catch (ItemExistsException ex) {
            // repository does not seem to support same name siblings on this node type
            throw new NotExecutableException("Node type " + testNodeType + " does not support same-name-siblings");
        }
    }
}


