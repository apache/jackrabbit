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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.lock.Lock;

/**
 * <code>SessionScopedLockTest</code>...
 */
public class SessionScopedLockTest extends AbstractLockTest {

    private static Logger log = LoggerFactory.getLogger(SessionScopedLockTest.class);

    boolean isSessionScoped() {
        return true;
    }

    /**
     * Test locks are released when session logs out
     */
    public void testLockNotLiveAfterLogout() throws RepositoryException {
        Node testRoot2 = (Node) otherSession.getItem(testRootNode.getPath());

        Node lockedNode2 = testRoot2.addNode(nodeName2, testNodeType);
        lockedNode2.addMixin(mixLockable);
        testRoot2.save();

        Lock lock2 = lockedNode2.lock(false, isSessionScoped());
        otherSession.logout();

        assertFalse(lock2.isLive());
    }

    /**
     * Test locks are released when session logs out
     */
    public void testNotLockedAfterLogout() throws RepositoryException {
        Node testRoot2 = (Node) otherSession.getItem(testRootNode.getPath());

        Node lockedNode2 = testRoot2.addNode(nodeName2, testNodeType);
        lockedNode2.addMixin(mixLockable);
        testRoot2.save();

        // force reloading of the testroot in order to be aware of the
        // locked noded added by another session
        testRootNode.refresh(false);
        Node n2 = (Node) superuser.getItem(lockedNode2.getPath());

        // remove lock implicit by logout lock-holding session
        otherSession.logout();

        assertFalse(n2.isLocked());
        assertFalse(n2.holdsLock());
        try {
            n2.getLock();
            fail("Upon logout of the session a session-scoped lock must be gone.");
        } catch (LockException e) {
            // ok
        }
    }
}