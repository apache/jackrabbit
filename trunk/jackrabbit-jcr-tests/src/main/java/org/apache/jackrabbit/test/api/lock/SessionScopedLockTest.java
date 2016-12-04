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

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;

/** <code>SessionScopedLockTest</code>... */
public class SessionScopedLockTest extends AbstractLockTest {

    protected boolean isSessionScoped() {
        return true;
    }

    protected boolean isDeep() {
        return false;
    }

    /**
     * {@link javax.jcr.lock.Lock#getLockToken()} must
     * always return <code>null</code> for session scoped locks.
     */
    public void testGetLockToken() {
        assertNull("A session scoped lock may never expose the token.", lock.getLockToken());
    }

    /**
     * Test locks are released when session logs out
     */
    public void testImplicitUnlock() throws RepositoryException,
            NotExecutableException {
        Session other = getHelper().getReadWriteSession();
        try {
            Node testNode = (Node) other.getItem(testRootNode.getPath());
            Node lockedNode = testNode.addNode(nodeName1, testNodeType);
            other.save();

            assertLockable(lockedNode);

            Lock lock = getLockManager(other).lock(lockedNode.getPath(), isDeep(), isSessionScoped(), getTimeoutHint(), getLockOwner());
            other.logout();

            assertFalse(lock.isLive());
        } finally {
            if (other.isLive()) {
                other.logout();
            }
        }
    }

    /**
     * Test locks are released when session logs out
     */
    public void testImplicitUnlock2() throws RepositoryException,
            NotExecutableException {
        Session other = getHelper().getReadWriteSession();
        try {
            Node testNode = (Node) other.getItem(testRootNode.getPath());
            Node lockedNode = testNode.addNode(nodeName1, testNodeType);
            other.save();

            assertLockable(lockedNode);

            LockManager lMgr = getLockManager(other);
            Lock lock = lMgr.lock(lockedNode.getPath(), isDeep(), isSessionScoped(), getTimeoutHint(), getLockOwner());

            // access the locked noded added by another session
            testRootNode.refresh(false);
            Node n = (Node) superuser.getItem(lockedNode.getPath());

            // remove lock implicit by logout lock-holding session
            other.logout();

            // check if superuser session is properly informed about the unlock
            assertFalse(n.isLocked());
            assertFalse(n.holdsLock());
            try {
                n.getLock();
                fail("Upon logout of the session a session-scoped lock must be gone.");
            } catch (LockException e) {
                // ok
            }
        } finally {
            if (other.isLive()) {
                other.logout();
            }
        }
    }
}