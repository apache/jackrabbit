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
package org.apache.jackrabbit.api.jsr283.lock;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;

/** <code>DeepLockTest</code>... */
public class DeepLockTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(DeepLockTest.class);

    protected Node lockedNode;
    protected Node childNode;
    protected Lock lock;

    protected void setUp() throws Exception {
        super.setUp();

        lockedNode = testRootNode.addNode(nodeName1, testNodeType);
        lockedNode.addMixin(mixLockable);
        childNode = lockedNode.addNode(nodeName2, testNodeType);
        testRootNode.save();

        // TODO: remove cast
        // TODO: replace by LockManager#lock call
        lock = (Lock) lockedNode.lock(true, true);
    }

    protected void tearDown() throws Exception {
        // make sure all locks are removed
        try {
            lockedNode.unlock();
        } catch (RepositoryException e) {
            // ignore
        }
        super.tearDown();
    }

    // TODO: replace locking calls by LockManager#...

    public void testNewBelowDeepLock() throws RepositoryException {
        Node newNode = lockedNode.addNode(nodeName3);
        assertTrue(newNode.isLocked());
    }

    public void testLockHoldingNode() throws RepositoryException {
        assertTrue("Lock.getNode() must be lockholding node.", lock.getNode().isSame(lockedNode));
    }

    public void testLockIsDeep() throws RepositoryException {
        assertTrue("Lock.isDeep() if lock has been created deeply.", lock.isDeep());
    }

    public void testNodeIsLocked() throws RepositoryException {
        assertTrue("Creating a deep lock must create a lock on the lock-holding node", lockedNode.isLocked());
        assertTrue("Creating a deep lock must create a lock on the lock-holding node", lockedNode.holdsLock());
    }

    public void testIsLockedChild() throws RepositoryException {
        assertTrue("Child node below deep lock must be locked", childNode.isLocked());
    }

    public void testIsLockedNewChild() throws RepositoryException {
        Node newChild = lockedNode.addNode(nodeName3, testNodeType);
        assertTrue("Child node below deep lock must be locked even if it is NEW",
                newChild.isLocked());
    }

    public void testNotHoldsLockChild() throws RepositoryException {
        assertFalse("Child node below deep lock must not be lock holder",
                childNode.holdsLock());
    }

    public void testGetLockOnChild() throws RepositoryException {
        // get lock must succeed even if child is not lockable.
        javax.jcr.lock.Lock lock = childNode.getLock();
        assertNotNull(lock);
        assertTrue("Lock.getNode() must return the lock holding node",
                lockedNode.isSame(lock.getNode()));
    }

    public void testGetLockOnNewChild() throws RepositoryException {
        // get lock must succeed even if child is not lockable.
        Node newChild = lockedNode.addNode(nodeName3, testNodeType);
        javax.jcr.lock.Lock lock = newChild.getLock();
        assertNotNull(lock);
        assertTrue("Lock.getNode() must return the lock holding node",
                lockedNode.isSame(lock.getNode()));
    }

    public void testGetNodeOnLockObtainedFromChild() throws RepositoryException {
        javax.jcr.lock.Lock lock = childNode.getLock();
        assertTrue("Lock.getNode() must return the lock holding node even if lock is obtained from child node.", lock.getNode().isSame(lockedNode));
    }

    public void testGetNodeOnLockObtainedFromNewChild() throws RepositoryException {
        Node newChild = lockedNode.addNode(nodeName3, testNodeType);
        javax.jcr.lock.Lock lock = newChild.getLock();
        assertTrue("Lock.getNode() must return the lock holding node even if lock is obtained from child node.", lock.getNode().isSame(lockedNode));
    }

    public void testParentChildDeepLock() throws RepositoryException {
        childNode.addMixin(mixLockable);
        testRootNode.save();

        // try to lock child node
        try {
            childNode.lock(false, false);
            fail("child node is already locked by deep lock on parent.");
        } catch (LockException e) {
            // ok
        }
    }

    public void testDeepLockAboveLockedChild() throws RepositoryException, NotExecutableException {
        try {
            Node parent = lockedNode.getParent();
            if (!parent.isNodeType(mixLockable)) {
                try {
                    parent.addMixin(mixLockable);
                    parent.save();
                } catch (RepositoryException e) {
                    throw new NotExecutableException();
                }
            }

            parent.lock(true, true);
            fail("Creating a deep lock on a parent of a locked node must fail.");
        } catch (LockException e) {
            // expected
        }
    }

    public void testShallowLockAboveLockedChild() throws RepositoryException, NotExecutableException {
        Node parent = lockedNode.getParent();
        try {
            if (!parent.isNodeType(mixLockable)) {
                try {
                    parent.addMixin(mixLockable);
                    parent.save();
                } catch (RepositoryException e) {
                    throw new NotExecutableException();
                }
            }

            // creating a shallow lock on the parent must succeed.
            parent.lock(false, true);
        } finally {
            parent.unlock();
        }
    }

    public void testRemoveLockedChild() throws RepositoryException {
        Session otherSession = helper.getReadWriteSession();
        try {
            Node child = (Node) otherSession.getItem(childNode.getPath());
            child.remove();
            otherSession.save();
            fail("A node below a deeply locked node cannot be removed by another Session.");
        } catch (LockException e) {
            // success
        } finally {
            otherSession.logout();
        }
    }
}