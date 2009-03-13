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
package org.apache.jackrabbit.jcr2spi.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import java.util.List;
import java.util.Arrays;

/**
 * <code>AbstractLockTest</code>...
 */
public abstract class AbstractLockTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(AbstractLockTest.class);

    Node lockedNode;
    Node childNode;
    Lock lock;

    Session otherSession;

    abstract boolean isSessionScoped();

    protected void setUp() throws Exception {
        super.setUp();

        otherSession = helper.getSuperuserSession();

        lockedNode = testRootNode.addNode(nodeName1, testNodeType);
        lockedNode.addMixin(mixLockable);
        childNode = lockedNode.addNode(nodeName2, testNodeType);
        testRootNode.save();

        lock = lockedNode.lock(false, isSessionScoped());
    }

    protected void tearDown() throws Exception {
        // make sure all locks are removed
        try {
            lockedNode.unlock();
        } catch (RepositoryException e) {
            // ignore
        }
        if (otherSession.isLive()) {
            otherSession.logout();
            otherSession = null;
        }
        lockedNode = null;
        childNode = null;
        lock = null;
        super.tearDown();
    }

    public void testParentChildLock() throws Exception {
        childNode.addMixin(mixLockable);
        testRootNode.save();

        // lock child node
        try {
            childNode.lock(false, isSessionScoped());
            // unlock parent node
            lockedNode.unlock();
            // child node must still hold lock
            assertTrue("child node must still hold lock", childNode.isLocked() && childNode.holdsLock());
        } finally {
            childNode.unlock();
        }
    }

    public void testParentChildLock2() throws Exception {
        childNode.addMixin(mixLockable);
        testRootNode.save();
        try {
            Lock l = childNode.lock(false, isSessionScoped());
            assertTrue("child node must still hold lock", l.getNode().isSame(childNode));
        } finally {
            childNode.unlock();
        }
    }

    /**
     * Test Lock.isDeep()
     */
    public void testNotIsDeep() throws RepositoryException {
        assertFalse("Lock.isDeep() must be false if the lock has not been set as not deep", lock.isDeep());
    }

    /**
     * Test Lock.isSessionScoped()
     */
    public void testIsSessionScoped() throws RepositoryException {
        if (isSessionScoped()) {
            assertTrue("Lock.isSessionScoped() must be true.", lock.isSessionScoped());
        } else {
            assertFalse("Lock.isSessionScoped() must be false. ", lock.isSessionScoped());
        }
    }

    public void testLockIsLive() throws RepositoryException {
        // assert: lock must be alive
        assertTrue("lock must be alive", lock.isLive());
    }

    public void testRefresh() throws RepositoryException {
        // assert: refresh must succeed
        lock.refresh();
    }

    public void testUnlock() throws RepositoryException {
        // unlock node
        lockedNode.unlock();
        // assert: lock must not be alive
        assertFalse("lock must not be alive", lock.isLive());
    }

    public void testRefreshNotLive() throws Exception {
        // unlock node
        lockedNode.unlock();
        // refresh
        try {
            lock.refresh();
            fail("Refresh on a lock that is not alive must fail");
        } catch (LockException e) {
            // success
        }
    }

    /**
     * Tests if a locked, checked-in node can be unlocked
     */
    public void testCheckedInUnlock() throws Exception {
        if (!isSupported(Repository.OPTION_VERSIONING_SUPPORTED)) {
            throw new NotExecutableException("Repository does not support versioning.");
        }

        lockedNode.addMixin(mixVersionable);
        lockedNode.save();

        // lock and check-in
        lockedNode.checkin();

        // do the unlock
        lockedNode.unlock();
        assertFalse("Could not unlock a locked, checked-in node", lockedNode.holdsLock());
    }

    public void testReorder() throws Exception {
        testRootNode.addNode(nodeName2);
        testRootNode.addNode(nodeName3);
        testRootNode.save();

        // move last node in front of first
        testRootNode.orderBefore(lockedNode.getName(), nodeName3);
        testRootNode.save();

        assertTrue("Node must remain locked upon reordering", testRootNode.getNode(lockedNode.getName()).isLocked());
    }

    public void testReorderSNS() throws Exception {
        // create 2 additional nodes with same name
        testRootNode.addNode(nodeName1);
        testRootNode.addNode(nodeName1);
        testRootNode.save();

        // assert: first node locked
        assertTrue("First child node locked", testRootNode.getNode(nodeName1 + "[1]").isLocked());

        // move first node to last
        testRootNode.orderBefore(nodeName1 + "[1]", null);
        testRootNode.save();

        // assert: third node locked
        assertTrue("Third child node locked", testRootNode.getNode(nodeName1 + "[3]").isLocked());
    }

    /**
     * Tests if move preserves lock state (JIRA issue JCR-207). A node that has
     * been locked must still appear locked when it has been moved or renamed,
     * regardless whether the changes have already been made persistent.
     */
    public void testMoveLocked() throws Exception {

        Session session = testRootNode.getSession();

        childNode.addMixin(mixLockable);
        childNode.save();

        try {
            // lock child node
            childNode.lock(false, isSessionScoped());

            // assert: child node locked
            assertTrue("Child node locked", childNode.isLocked());

            // move child node up
            String newPath = testRootNode.getPath() + "/" + childNode.getName();
            session.move(childNode.getPath(), newPath);

            // assert: child node locked, before save
            assertTrue("Child node locked before save", childNode.isLocked());
            session.save();

            // assert: child node locked, after save
            assertTrue("Child node locked after save", childNode.isLocked());

        } finally {
            session.refresh(false);
            childNode.unlock();
        }
    }

    /**
     * Tests if unlocking the first of two locked same-name sibling nodes does
     * not unlock the second (JIRA issue JCR-284).
     */
    public void testUnlockSameNameSibling() throws RepositoryException {
        Session session = testRootNode.getSession();

        // create two same-name sibling nodes
        Node lockedNode2 = testRootNode.addNode(nodeName1);
        lockedNode2.addMixin("mix:lockable");
        session.save();

        // lock both nodes
        lockedNode2.lock(false, isSessionScoped());

        try {
            // assert: both nodes are locked
            assertTrue("First node locked: ", lockedNode.isLocked());
            assertTrue("Second node locked: ", lockedNode2.isLocked());
        } catch (RepositoryException e) {
            // mk sure all locks are release again
            lockedNode.unlock();
            lockedNode2.unlock();
            throw new RepositoryException(e);
        }

        try {
            // unlock first sibling
            lockedNode.unlock();

            // assert: first node unlocked, second node still locked
            assertFalse("First node unlocked: ", lockedNode.isLocked());
            assertTrue("Second node locked: ", lockedNode2.isLocked());

        } finally {
            // mk sure all locks are release again
            lockedNode2.unlock();
        }
    }

    /**
     * A locked node must also be locked if accessed by some other session.
     */
    public void testLockVisibility() throws RepositoryException {
        Node ln2 = (Node) otherSession.getItem(lockedNode.getPath());
        assertTrue("Locked node must also be locked for another session", ln2.isLocked());
        assertTrue("Locked node must also be locked for another session", ln2.holdsLock());
    }

    /**
     * If a locked nodes is unlocked again, any Lock instance retrieved by
     * another session must change the lock-status. Similarly, the previously
     * locked node must not be marked locked any more.
     */
    public void testUnlockByOtherSession() throws RepositoryException {
        Node ln2 = (Node) otherSession.getItem(lockedNode.getPath());
        Lock l2 = ln2.getLock();

        lockedNode.unlock();

        assertFalse("Lock must be informed if Node is unlocked.", l2.isLive());
    }

    /**
     * If a locked nodes is unlocked again, any Lock instance retrieved by
     * another session must change the lock-status. Similarly, the previously
     * locked node must not be marked locked any more.
     */
    public void testUnlockByOtherSession2() throws RepositoryException {
        Node ln2 = (Node) otherSession.getItem(lockedNode.getPath());

        lockedNode.unlock();

        assertFalse("Node is not locked any more", ln2.isLocked());
        assertFalse("Node is not locked any more", ln2.holdsLock());
        try {
            ln2.getLock();
            fail("Node is not locked any more");
        } catch (LockException e) {
            // OK
        }
    }

    public void testRemoveLockedNode() throws RepositoryException {
        Node n = (Node) otherSession.getItem(lockedNode.getPath());

        // since removing a node is a modification of the non-locked parent
        // the removal must succeed.
        n.remove();
        otherSession.save();
    }

    public void testRemoveMixLockableFromLockedNode() throws RepositoryException {
        try {
            lockedNode.removeMixin(mixLockable);
            lockedNode.save();

            // the mixin got removed -> the lock should implicitely be released
            // as well in order not to have inconsistencies
            String msg = "Lock should have been released.";
            assertFalse(msg, lock.isLive());
            assertFalse(msg, lockedNode.isLocked());
            if (!isSessionScoped()) {
                List tokens = Arrays.asList(superuser.getLockTokens());
                assertFalse(msg, tokens.contains(lock.getLockToken()));
            }

            assertFalse(msg, lockedNode.hasProperty(jcrLockOwner));
            assertFalse(msg, lockedNode.hasProperty(jcrlockIsDeep));

        } catch (ConstraintViolationException e) {
            // cannot remove the mixin -> ok
            // consequently the node must still be locked, the lock still live...
            String msg = "Lock must still be live.";
            assertTrue(msg, lock.isLive());
            assertTrue(msg, lockedNode.isLocked());
            if (!isSessionScoped()) {
                List tokens = Arrays.asList(superuser.getLockTokens());
                assertTrue(tokens.contains(lock.getLockToken()));
            }
            assertTrue(msg, lockedNode.hasProperty(jcrLockOwner));
            assertTrue(msg, lockedNode.hasProperty(jcrlockIsDeep));
        } finally {
            // ev. re-add the mixin in order to be able to unlock the node
            if (lockedNode.isLocked() && !lockedNode.isNodeType(mixLockable)) {
                lockedNode.addMixin(mixLockable);
                lockedNode.save();
            }
        }
    }
}
