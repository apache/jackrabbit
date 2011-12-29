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

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;

/** <code>DeepLockTest</code>... */
public class DeepLockTest extends AbstractLockTest {

    protected boolean isSessionScoped() {
        return true;
    }

    protected boolean isDeep() {
        return true;
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

    public void testParentChildDeepLock()
            throws RepositoryException, NotExecutableException {
        ensureMixinType(childNode, mixLockable);
        testRootNode.getSession().save();

        // try to lock child node
        try {
            childNode.lock(false, false);
            fail("child node is already locked by deep lock on parent.");
        } catch (LockException e) {
            // ok
        }

        try {
            lockMgr.lock(childNode.getPath(), false, false, getTimeoutHint(), getLockOwner());
            fail("child node is already locked by deep lock on parent.");
        } catch (LockException e) {
            // ok
        }
    }

    public void testDeepLockAboveLockedChild() throws RepositoryException, NotExecutableException {
        Node parent = lockedNode.getParent();
        if (!parent.isNodeType(mixLockable)) {
            try {
                ensureMixinType(parent, mixLockable);
                parent.save();
            } catch (RepositoryException e) {
                throw new NotExecutableException();
            }
        }

        try {
            parent.lock(true, true);
            fail("Creating a deep lock on a parent of a locked node must fail.");
        } catch (LockException e) {
            // expected
        }
        try {
            lockMgr.lock(parent.getPath(), true, true, getTimeoutHint(), getLockOwner());
            fail("Creating a deep lock on a parent of a locked node must fail.");
        } catch (LockException e) {
            // expected
        }
    }

    public void testShallowLockAboveLockedChild() throws RepositoryException, NotExecutableException {
        Node parent = lockedNode.getParent();
        ensureMixinType(parent, mixLockable);
        parent.save();

        try {
            // creating a shallow lock on the parent must succeed.
            parent.lock(false, true);
        } finally {
            parent.unlock();
        }
        try {
            // creating a shallow lock on the parent must succeed.
            lockMgr.lock(parent.getPath(), false, true, getTimeoutHint(), getLockOwner());
        } finally {
            parent.unlock();
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