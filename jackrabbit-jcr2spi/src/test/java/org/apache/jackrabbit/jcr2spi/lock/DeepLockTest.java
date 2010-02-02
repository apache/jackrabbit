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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>DeepLockTest</code>...
 */
public class DeepLockTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(DeepLockTest.class);

    private final boolean isSessionScoped = false;
    private final boolean isDeep = true;

    private Node lockedNode;
    private Node childNode;
    private Lock lock;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        lockedNode = testRootNode.addNode(nodeName1, testNodeType);
        lockedNode.addMixin(mixLockable);
        childNode = lockedNode.addNode(nodeName2, testNodeType);
        testRootNode.save();

        lock = lockedNode.lock(isDeep, isSessionScoped);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            lockedNode.unlock();
        } catch (RepositoryException e) {
            log.warn(e.getMessage());
        }
        lockedNode = null;
        childNode = null;
        lock = null;
        super.tearDown();
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
        assertTrue("Child node below deep lock must be locked even if its is NEW", newChild.isLocked());
    }

    public void testNotHoldsLockChild() throws RepositoryException {
        assertFalse("Child node below deep lock must not be lock holder", childNode.holdsLock());
    }

    public void testGetLockOnChild() throws RepositoryException {
        // get lock must succeed even if child is not lockable.
        childNode.getLock();
    }

    public void testGetLockOnNewChild() throws RepositoryException {
        // get lock must succeed even if child is not lockable.
        Node newChild = lockedNode.addNode(nodeName3, testNodeType);
        newChild.getLock();
    }

    public void testGetNodeOnLockObtainedFromChild() throws RepositoryException {
        Lock lock = childNode.getLock();
        assertTrue("Lock.getNode() must return the lock holding node even if lock is obtained from child node.", lock.getNode().isSame(lockedNode));
    }

    public void testGetNodeOnLockObtainedFromNewChild() throws RepositoryException {
        Node newChild = lockedNode.addNode(nodeName3, testNodeType);
        Lock lock = newChild.getLock();
        assertTrue("Lock.getNode() must return the lock holding node even if lock is obtained from child node.", lock.getNode().isSame(lockedNode));
    }

    public void testParentChildDeepLock() throws RepositoryException {
        childNode.addMixin(mixLockable);
        testRootNode.save();

        // try to lock child node
        try {
            childNode.lock(false, isSessionScoped);
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

            parent.lock(true, isSessionScoped);
            fail("Creating a deep lock on a parent of a locked node must fail.");
        } catch (LockException e) {
            // expected
        }
    }

    public void testRemoveLockedChild() throws RepositoryException {
        Session otherSession = getHelper().getReadWriteSession();
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