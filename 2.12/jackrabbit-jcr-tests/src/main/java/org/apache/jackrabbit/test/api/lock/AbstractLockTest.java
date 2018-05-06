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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.RepositoryStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** <code>AbstractLockTest</code>... */
public abstract class AbstractLockTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(AbstractLockTest.class);

    protected LockManager lockMgr;
    protected Node lockedNode;
    protected Node childNode;
    protected Lock lock;

    protected void setUp() throws Exception {
        // check for lock support before creating the session in the super.setup
        checkSupportedOption(Repository.OPTION_LOCKING_SUPPORTED);
        
        super.setUp();
        
        lockedNode = testRootNode.addNode(nodeName1, testNodeType);
        ensureMixinType(lockedNode, mixLockable);
        childNode = lockedNode.addNode(nodeName2, testNodeType);
        testRootNode.getSession().save();

        lockMgr = getLockManager(testRootNode.getSession());
        lock = lockMgr.lock(lockedNode.getPath(), isDeep(), isSessionScoped(), getTimeoutHint(), getLockOwner());
    }

    protected void tearDown() throws Exception {
        // release the lock created during setup
        if (lockMgr != null && lockedNode != null && lockMgr.isLocked(lockedNode.getPath())) {
            try {
                lockMgr.unlock(lockedNode.getPath());
            } catch (RepositoryException e) {
                // ignore
            }
        }
        super.tearDown();
    }

    protected abstract boolean isSessionScoped();
    protected abstract boolean isDeep();

    protected void assertLockable(Node n)
            throws RepositoryException, NotExecutableException {
        ensureMixinType(n, mixLockable);
        n.getSession().save();
    }

    protected long getTimeoutHint() throws RepositoryException {
        String timoutStr = getProperty(RepositoryStub.PROP_LOCK_TIMEOUT);
        long hint = Long.MAX_VALUE;
        if (timoutStr != null) {
            try {
                hint = Long.parseLong(timoutStr);
            } catch (NumberFormatException e) {
                log.warn(e.getMessage());
            }
        }
        return hint;
    }

    protected String getLockOwner() throws RepositoryException {
        String ownerStr = getProperty(RepositoryStub.PROP_LOCK_OWNER);
        if (ownerStr == null) {
            ownerStr = superuser.getUserID();
        }
        return ownerStr;
    }

    protected static LockManager getLockManager(Session session) throws RepositoryException {
        return session.getWorkspace().getLockManager();
    }

    /**
     * Test {@link javax.jcr.lock.Lock#isDeep()}.
     */
    public void testIsDeep() {
        assertEquals("Lock.isDeep must be consistent with lock call.", isDeep(), lock.isDeep());
    }

    /**
     * Test {@link javax.jcr.lock.Lock#isLive()}.
     */
    public void testIsLive() throws RepositoryException {
        assertTrue("Lock.isLive must be true.", lock.isLive());
    }

    /**
     * Test {@link javax.jcr.lock.Lock#refresh()} on a released lock.
     */
    public void testRefresh() throws RepositoryException {
        // refresh must succeed
        lock.refresh();
    }

    // TODO: test if timeout gets reset upon Lock.refresh()
    
    /**
     * Test {@link javax.jcr.lock.Lock#refresh()} on a released lock.
     *
     * @throws Exception
     */
    public void testRefreshNotLive() throws Exception {
        // release the lock
        lockMgr.unlock(lockedNode.getPath());
        // refresh
        try {
            lock.refresh();
            fail("Refresh on a lock that is not alive must fail");
        } catch (LockException e) {
            // success
        }
    }

    /**
     * Test {@link javax.jcr.lock.Lock#getNode()}.
     *
     * @throws RepositoryException If an exception occurs.
     */
    public void testLockHoldingNode() throws RepositoryException {
        assertTrue("Lock.getNode() must be lockholding node.", lock.getNode().isSame(lockedNode));
    }

    /**
     * Test {@link LockManager#isLocked(String)} and {@link javax.jcr.Node#isLocked()}.
     *
     * @throws RepositoryException If an exception occurs.
     */
    public void testNodeIsLocked() throws RepositoryException {
        assertTrue("Node must be locked after lock creation.", lockedNode.isLocked());
        assertTrue("Node must be locked after lock creation.", lockMgr.isLocked(lockedNode.getPath()));
    }

    /**
     * Test {@link LockManager#holdsLock(String)} and {@link javax.jcr.Node#holdsLock()}. 
     *
     * @throws RepositoryException If an exception occurs.
     */
    public void testNodeHoldsLocked() throws RepositoryException {
        assertTrue("Node must hold lock after lock creation.", lockedNode.holdsLock());
        assertTrue("Node must hold lock after lock creation.", lockMgr.holdsLock(lockedNode.getPath()));
    }


    /**
     * A locked node must also be locked if accessed by some other session.
     */
    public void testLockVisibility() throws RepositoryException {
        Session otherSession = getHelper().getReadWriteSession();
        try {
            Node ln = (Node) otherSession.getItem(lockedNode.getPath());
            assertTrue("Locked node must also be locked for another session", ln.isLocked());
            assertTrue("Locked node must also be locked for another session", ln.holdsLock());
            assertTrue("Locked node must also be locked for another session", getLockManager(otherSession).holdsLock(ln.getPath()));
        } finally {
            otherSession.logout();
        }
    }

    /**
     * Test {@link javax.jcr.lock.Lock#isSessionScoped()}
     */
    public void testIsSessionScoped() {
        assertEquals("Lock.isSessionScoped must be consistent with lock call.", isSessionScoped(), lock.isSessionScoped());
    }

    /**
     * Test {@link javax.jcr.lock.Lock#isLockOwningSession()}
     *
     * @throws RepositoryException If an exception occurs.
     */
    public void testIsLockOwningSession() throws RepositoryException {
        assertTrue("Session must be lock owner", lock.isLockOwningSession());
        assertTrue("Session must be lock owner", lockedNode.getLock().isLockOwningSession());
        assertTrue("Session must be lock owner", lockMgr.getLock(lockedNode.getPath()).isLockOwningSession());

        Session otherSession = getHelper().getReadOnlySession();
        try {
            Lock lck = otherSession.getNode(lockedNode.getPath()).getLock();
            assertFalse("Session must not be lock owner", lck.isLockOwningSession());

            Lock lck2 = getLockManager(otherSession).getLock(lockedNode.getPath());
            assertFalse("Session must not be lock owner", lck2.isLockOwningSession());
        } finally {
            otherSession.logout();
        }

        Session otherAdmin = getHelper().getSuperuserSession();
        try {
            Lock lck = otherAdmin.getNode(lockedNode.getPath()).getLock();
            assertFalse("Other Session for the same userID must not be lock owner", lck.isLockOwningSession());

            Lock lck2 = getLockManager(otherAdmin).getLock(lockedNode.getPath());
            assertFalse("Other Session for the same userID must not be lock owner", lck2.isLockOwningSession());

        } finally {
            otherAdmin.logout();
        }
    }

    /**
     * Test {@link javax.jcr.lock.Lock#getSecondsRemaining()} 
     */
    public void testGetSecondsRemaining() throws RepositoryException {
        if (lock.isLive()) {
            assertTrue("Seconds remaining must be a positive long.", lock.getSecondsRemaining() > 0);            
        } else {
            assertTrue("Seconds remaining must be a negative long.", lock.getSecondsRemaining() < 0);
        }
    }

    /**
     * Test {@link javax.jcr.lock.Lock#getSecondsRemaining()}
     */
    public void testGetSecondsRemainingAfterUnlock() throws RepositoryException {
        lockMgr.unlock(lockedNode.getPath());
        assertTrue("Lock has been released: seconds remaining must be a negative long.", lock.getSecondsRemaining() < 0);
    }

    /**
     * Test expiration of the lock
     */
    public synchronized void testLockExpiration()
            throws RepositoryException, NotExecutableException {
        lockedNode.unlock();

        long hint = 1;
        lock = lockMgr.lock(
                lockedNode.getPath(), isDeep(), isSessionScoped(), hint, null);

        // only test if timeout hint was respected.
        long remaining = lock.getSecondsRemaining();
        if (remaining <= hint) {
            if (remaining > 0) {
                try {
                    wait(remaining * 4000); // wait four time as long to be safe
                } catch (InterruptedException ignore) {
                }
            }
            long secs = lock.getSecondsRemaining();
            assertTrue(
                    "A released lock must return a negative number of seconds, was: " + secs,
                    secs < 0);
            String message = "If the timeout hint is respected the lock"
                + " must be automatically released.";
            assertFalse(message, lock.isLive());
            assertFalse(message, lockedNode.isLocked());
            assertFalse(message, lockMgr.isLocked(lockedNode.getPath()));
            assertFalse(message, lockedNode.hasProperty(Property.JCR_LOCK_IS_DEEP));
            assertFalse(message, lockedNode.hasProperty(Property.JCR_LOCK_OWNER));
        } else {
            throw new NotExecutableException("timeout hint was ignored.");
        }
    }

    /**
     * Test expiration of the lock
     */
    public synchronized void testOwnerHint()
            throws RepositoryException, NotExecutableException {
        lockedNode.unlock();

        lock = lockMgr.lock(lockedNode.getPath(), isDeep(), isSessionScoped(), Long.MAX_VALUE, "test");

        String owner = lock.getLockOwner();
        if (!"test".equals(lock.getLockOwner())) {
            throw new NotExecutableException();
        } else {
            assertTrue(lockedNode.hasProperty(Property.JCR_LOCK_OWNER));
            assertEquals("test", lockedNode.getProperty(Property.JCR_LOCK_OWNER).getString());
        }
    }

    /**
     * Test if Lock is properly released.
     * 
     * @throws RepositoryException
     */
    public void testUnlock() throws RepositoryException {
        // release the lock
        lockMgr.unlock(lockedNode.getPath());
        
        // assert: lock must not be alive
        assertFalse("lock must not be alive", lock.isLive());
    }

    /**
     * Test {@link LockManager#unlock(String)} for a session that is not
     * lock owner.
     * 
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testUnlockByOtherSession() throws RepositoryException, NotExecutableException {
        Session otherSession = getHelper().getReadWriteSession();
        try {
            getLockManager(otherSession).unlock(lockedNode.getPath());
            fail("Another session must not be allowed to unlock.");
        } catch (LockException e) {
            // success
            // make sure the node is still locked and the lock properties are
            // still present.
            assertTrue(lockMgr.isLocked(lockedNode.getPath()));
            assertTrue(lockedNode.hasProperty(jcrlockIsDeep));
            assertTrue(lockedNode.hasProperty(jcrLockOwner));
        } finally {
            otherSession.logout();
        }
    }

    public void testIsLockedChild() throws RepositoryException {
        assertEquals("Child node must be locked according to isDeep flag.", isDeep(), childNode.isLocked());
        assertEquals("Child node must be locked according to isDeep flag.", isDeep(), lockMgr.isLocked(childNode.getPath()));
    }

    public void testIsLockedNewChild() throws RepositoryException {
        Node newChild = lockedNode.addNode(nodeName3, testNodeType);
        assertEquals("New child node must be locked according to isDeep flag.", isDeep(),
                newChild.isLocked());
        assertEquals("New child node must be locked according to isDeep flag.", isDeep(),
                lockMgr.isLocked(newChild.getPath()));
    }

    public void testHoldsLockChild() throws RepositoryException {
        assertFalse("Child node below a locked node must never be lock holder",
                childNode.holdsLock());
        assertFalse("Child node below a locked node must never be lock holder",
                lockMgr.holdsLock(childNode.getPath()));
    }

    public void testHoldsLockNewChild() throws RepositoryException {
        Node newChild = lockedNode.addNode(nodeName3, testNodeType);
        assertFalse("Child node below a locked node must never be lock holder",
                newChild.holdsLock());
        assertFalse("Child node below a locked node must never be lock holder",
                lockMgr.holdsLock(newChild.getPath()));
    }

    public void testGetLockOnChild() throws RepositoryException {
        if (isDeep()) {
            // get lock must succeed even if child is not lockable.
            Lock lock = childNode.getLock();
            assertNotNull(lock);
            assertTrue("Lock.getNode() must return the lock holding node", lockedNode.isSame(lock.getNode()));

            Lock lock2 = lockMgr.getLock(childNode.getPath());
            assertNotNull(lock2);
            assertTrue("Lock.getNode() must return the lock holding node", lockedNode.isSame(lock2.getNode()));
        } else {
            try {
                childNode.getLock();
                fail("Node.getLock() must throw if node is not locked.");
            } catch (LockException e) {
                // success
            }
            try {
                lockMgr.getLock(childNode.getPath());
                fail("LockManager.getLock(String) must throw if node is not locked.");
            } catch (LockException e) {
                // success
            }
        }
    }

    public void testGetLockOnNewChild() throws RepositoryException {
        Node newChild = lockedNode.addNode(nodeName3, testNodeType);
        if (isDeep()) {
            // get lock must succeed even if child is not lockable.
            Lock lock = newChild.getLock();
            assertNotNull(lock);
            assertTrue("Lock.getNode() must return the lock holding node", lockedNode.isSame(lock.getNode()));

            Lock lock2 = lockMgr.getLock(newChild.getPath());
            assertNotNull(lock2);
            assertTrue("Lock.getNode() must return the lock holding node", lockedNode.isSame(lock2.getNode()));
        } else {
            try {
                newChild.getLock();
                fail("Node.getLock() must throw if node is not locked.");
            } catch (LockException e) {
                // success
            }
            try {
                lockMgr.getLock(newChild.getPath());
                fail("LockManager.getLock(String) must throw if node is not locked.");
            } catch (LockException e) {
                // success
            }
        }
    }

    public void testRemoveMixLockableFromLockedNode() throws RepositoryException,
            NotExecutableException {
        try {
            lockedNode.removeMixin(mixLockable);
            lockedNode.save();

            // the mixin got removed -> the lock should implicitly be released
            // as well in order not to have inconsistencies
            String msg = "Lock should have been released.";
            assertFalse(msg, lock.isLive());
            assertFalse(msg, lockedNode.isLocked());
            assertFalse(msg, lockMgr.isLocked(lockedNode.getPath()));

            assertFalse(msg, lockedNode.hasProperty(jcrLockOwner));
            assertFalse(msg, lockedNode.hasProperty(jcrlockIsDeep));

        } catch (ConstraintViolationException e) {
            // cannot remove the mixin -> ok
            // consequently the node must still be locked, the lock still live...
            String msg = "Lock must still be live.";
            assertTrue(msg, lock.isLive());
            assertTrue(msg, lockedNode.isLocked());
            assertTrue(msg, lockMgr.isLocked(lockedNode.getPath()));

            assertTrue(msg, lockedNode.hasProperty(jcrLockOwner));
            assertTrue(msg, lockedNode.hasProperty(jcrlockIsDeep));
        } finally {
            // ev. re-add the mixin in order to be able to unlock the node
            if (lockedNode.isLocked()) {
                ensureMixinType(lockedNode, mixLockable);
                lockedNode.save();
            }
        }
    }
}