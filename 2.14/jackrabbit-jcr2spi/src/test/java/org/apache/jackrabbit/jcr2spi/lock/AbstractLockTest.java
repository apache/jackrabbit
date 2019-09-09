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

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        otherSession = getHelper().getSuperuserSession();

        lockedNode = testRootNode.addNode(nodeName1, testNodeType);
        lockedNode.addMixin(mixLockable);
        childNode = lockedNode.addNode(nodeName2, testNodeType);
        testRootNode.save();

        lock = lockedNode.lock(false, isSessionScoped());
    }

    @Override
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
            // make sure all locks are release again
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
            // make sure all locks are release again
            lockedNode2.unlock();
        }
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
}
