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
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.NodeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>OpenScopedLockTest</code>...
 */
public class OpenScopedLockTest extends AbstractLockTest {

    private static Logger log = LoggerFactory.getLogger(OpenScopedLockTest.class);

    @Override
    boolean isSessionScoped() {
        return false;
    }

    public void testLogoutHasNoEffect() throws Exception {
        // create a second session session. since logout of the 'superuser'
        // will cause all inherited tear-down to fail
        Node testRoot2 = (Node) otherSession.getItem(testRootNode.getPath());

        Node lockedNode2 = testRoot2.addNode(nodeName2, testNodeType);
        lockedNode2.addMixin(mixLockable);
        testRoot2.save();

        Lock lock2 = lockedNode2.lock(false, isSessionScoped());

        // force reloading of the testroot in order to be aware of the
        // locked node added by another session
        testRootNode.refresh(false);
        Node n2 = (Node) superuser.getItem(lockedNode2.getPath());
        try {
            String lockToken = lock2.getLockToken();
            otherSession.removeLockToken(lockToken);
            superuser.addLockToken(lockToken);
            otherSession.logout();

            assertTrue("After logout a open-scoped node must still be locked.", lock2.isLive());
            assertTrue("After logout a open-scoped node must still be locked.", n2.isLocked());
        } finally {
            n2.unlock();
        }
    }

    /**
     * Test if the lock token has been automatically added to the set of lock
     * tokens present with the Session that created the new Lock.
     *
     * @throws RepositoryException
     */
    public void testLockTokenPresentWithSession() throws RepositoryException {
        String token = lock.getLockToken();
        String[] allTokens = lockedNode.getSession().getLockTokens();
        for (int i = 0; i < allTokens.length; i++) {
            if (allTokens[i].equals(token)) {
                // lock token is present with the session that applied the lock
                // OK
                return;
            }
        }

        // lock token not present within tokens returned by Session.getLockTokens.
        fail("Upon successful call to Node.lock, the lock token must automatically be added to the set of tokens held by the Session.");
    }

    public void testTokenTransfer() throws Exception {
        String lockToken = lock.getLockToken();
        try {
            superuser.removeLockToken(lockToken);

            String nlt = lock.getLockToken();
            assertTrue("freshly obtained lock token must either be null or the same as the one returned earlier",
                    nlt == null || nlt.equals(lockToken));
        } finally {
            // move lock token back in order to have lock removed properly
            superuser.addLockToken(lockToken);
        }
    }

    public void testRefreshAfterTokenTransfer() throws Exception {
        String lockToken = lock.getLockToken();
        try {
            superuser.removeLockToken(lockToken);
            lock.refresh();
            fail("After transfering lock token the original lock object cannot be refresh by session, that does hold lock any more.");
        } catch (LockException e) {
            // oK
        } finally {
            // move lock token back in order to have lock removed properly
            superuser.addLockToken(lockToken);
        }
    }

    public void testRefreshAfterTokenTransfer2() throws Exception {
        String lockToken = lock.getLockToken();

        Node n2 = (Node) otherSession.getItem(lockedNode.getPath());
        try {
            superuser.removeLockToken(lockToken);
            otherSession.addLockToken(lockToken);

            n2.getLock().refresh();
        } finally {
            // move lock token back in order to have lock removed properly
            otherSession.removeLockToken(lockToken);
            superuser.addLockToken(lockToken);
        }
    }

    public void testLockHolderAfterTokenTransfer() throws Exception {
        String lockToken = lock.getLockToken();
        Node n2 = (Node) otherSession.getItem(lockedNode.getPath());
        try {
            superuser.removeLockToken(lockToken);
            otherSession.addLockToken(lockToken);

            assertTrue("After lockToken transfer, the new lockHolder must get a non-null token", n2.getLock().getLockToken() != null);
            assertTrue("After lockToken transfer, the new lockHolder must get the same token.", n2.getLock().getLockToken().equals(lockToken));
        } finally {
            // move lock token back in order to have lock removed properly
            otherSession.removeLockToken(lockToken);
            superuser.addLockToken(lockToken);
        }
    }

    public void testUnlockAfterTokenTransfer() throws Exception {
        String lockToken = lock.getLockToken();
        try {
            superuser.removeLockToken(lockToken);
            lockedNode.unlock();
            fail("After transfering lock token the original lock object cannot be unlocked by session, that does hold lock any more.");
        } catch (LockException e) {
            // oK
        } finally {
            // move lock token back in order to have lock removed properly
            superuser.addLockToken(lockToken);
        }
    }

    public void testUnlockAfterTokenTransfer2() throws Exception {
        String lockToken = lock.getLockToken();
        try {
            superuser.removeLockToken(lockToken);
            otherSession.addLockToken(lockToken);

            // otherSession is now lockHolder -> unlock must succeed.
            Node n2 = (Node) otherSession.getItem(lockedNode.getPath());
            n2.unlock();
        } catch (RepositoryException e) {
            // only in case of failure:
            // move lock token back in order to have lock removed properly
            // if test succeeds, moving back tokens is not necessary.
            otherSession.removeLockToken(lockToken);
            superuser.addLockToken(lockToken);

            // and rethrow
            throw e;
        }
    }

    /**
     * Test if a Lock created by one session gets properly invalidated
     * if the lock token has been transfered to another session, which
     * unlocks the Node.
     */
    public void testUnlockAfterTokenTransfer3() throws Exception {
        String lockToken = lock.getLockToken();
        try {
            superuser.removeLockToken(lockToken);
            otherSession.addLockToken(lockToken);

            // otherSession is now lockHolder -> unlock must succeed.
            Node n2 = (Node) otherSession.getItem(lockedNode.getPath());
            n2.unlock();

            assertFalse("Lock has been release by another session.", lockedNode.holdsLock());

            assertFalse("Lock has been release by another session.", lock.isLive());
            assertFalse("Lock has been release by another session.", lock.getNode().isLocked());
            try {
                lockedNode.getLock();
                fail("Lock has been release by another session.");
            } catch (LockException e) {
                // ok
            }
        } catch (RepositoryException e) {
            // only in case of failure:
            // move lock token back in order to have lock removed properly
            // if test succeeds, moving back tokens is not necessary.
            otherSession.removeLockToken(lockToken);
            superuser.addLockToken(lockToken);

            // and rethrow
            throw e;
        }
    }

    public void testIsLockedWhileAnotherLockIsPresent() throws Exception {

        Session s = lockedNode.getSession();
        LockManager lm = s.getWorkspace().getLockManager();

        String l2token = null;
        String l2path = null;

        String path = lockedNode.getPath();
        String lockToken = lock.getLockToken();
        assertTrue(lm.isLocked(path));
        assertTrue(lm.holdsLock(path));
        lm.removeLockToken(lockToken);

        Session anotherSession = null;
        try {
            // check lock is seen by new session
            anotherSession = getHelper().getSuperuserSession();
            LockManager anotherLockManager = anotherSession.getWorkspace().getLockManager();
            assertTrue(anotherLockManager.isLocked(path));
            assertTrue(anotherLockManager.holdsLock(path));

            // create a second lock
            Node l2node = anotherSession.getNode(path).getParent().addNode("second-lock");
            l2node.addMixin(NodeType.MIX_LOCKABLE);
            anotherSession.save();
            l2path = l2node.getPath();

            Lock l2 = anotherLockManager.lock(l2path, false, false, Long.MAX_VALUE, "foobar");
            l2token = l2.getLockToken();
            assertNotNull(l2token);
            anotherSession.save();

            anotherSession.refresh(false);
            assertTrue(anotherLockManager.isLocked(path));
            assertTrue(anotherLockManager.holdsLock(path));

            // try to unlock the lock obtained from the other session
            anotherLockManager.addLockToken(lockToken);
            anotherLockManager.unlock(path);
            anotherSession.save();

            // unlock "my" lock
            anotherLockManager.unlock(l2path);
            anotherSession.save();
            l2path = null;
        }
        finally {
            if (anotherSession != null) {
                anotherSession.logout();
            }
            if (l2path != null && l2token != null) {
                superuser.refresh(false);
                LockManager sulm = superuser.getWorkspace().getLockManager();
                sulm.addLockToken(l2token);
                sulm.unlock(l2path);
            }
        }
    }
}