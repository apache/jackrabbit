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

import java.util.Arrays;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.RepositoryStub;

/** <code>LockManagerTest</code>... */
public class LockManagerTest extends AbstractJCRTest {

    protected LockManager lockMgr;
    protected Node testNode;
    protected String testPath;

    protected boolean openScopedLockMultiple;

    protected void setUp() throws Exception {
        super.setUp();

        // check for lock support
        if (Boolean.FALSE.toString().equals(superuser.getRepository().getDescriptor(Repository.OPTION_LOCKING_SUPPORTED))) {
            throw new NotExecutableException();
        }

        testNode = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.getSession().save();
        testPath = testNode.getPath();
        openScopedLockMultiple = Boolean.TRUE.toString()
                .equals(getProperty(RepositoryStub.PROP_OPEN_SCOPED_LOCK_MULTIPLE, Boolean.FALSE.toString()));

        lockMgr = getLockManager(superuser);
    }

    protected void tearDown() throws Exception  {
        if (lockMgr != null && lockMgr.holdsLock(testPath)) {
            lockMgr.unlock(testPath);
        }
        super.tearDown();
    }

    private void assertLockable(Node n) throws RepositoryException,
            NotExecutableException {
        ensureMixinType(n, mixLockable);
        n.getSession().save();
    }

   private static LockManager getLockManager(Session session) throws RepositoryException {
        return session.getWorkspace().getLockManager();
    }

    private static boolean containsLockToken(LockManager lMgr, String token) throws RepositoryException {
        return containsLockToken(lMgr.getLockTokens(), token);
    }

    private static boolean containsLockToken(String[] tokens, String token) throws RepositoryException {
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals(token)) {
                return true;
            }
        }
        return false;
    }

    public void testLockNonLockable() throws NotExecutableException, RepositoryException {
        if (testNode.isNodeType(mixLockable)) {
            throw new NotExecutableException();
        }
        try {
            lockMgr.lock(testPath, true, true, Long.MAX_VALUE, superuser.getUserID());
            fail("Attempt to lock a non-lockable node must throw LockException.");
        } catch (LockException e) {
            // success
        }
    }

    public void testLockWithPendingChanges() throws RepositoryException,
            NotExecutableException {
        assertLockable(testNode);

        // transient modification
        testNode.addNode(nodeName2);
        try {
            lockMgr.lock(testPath, true, true, Long.MAX_VALUE, superuser.getUserID());
            fail("Attempt to lock a node with transient modifications must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            // success
        }
    }

    public void testNullOwnerHint() throws RepositoryException,
            NotExecutableException {
        assertLockable(testNode);

        Lock l = lockMgr.lock(testPath, true, true, Long.MAX_VALUE, null);
        assertNotNull(l.getLockOwner());
    }

    public void testGetLockTokens() throws RepositoryException,
            NotExecutableException {
        assertLockable(testNode);

        boolean sessionScoped = false;
        Lock l = lockMgr.lock(testPath, true, sessionScoped, Long.MAX_VALUE, null);
        String ltoken = l.getLockToken();
        assertTrue("Creating open scoped lock must add token to the lock manager.",
                containsLockToken(lockMgr, ltoken));
        assertTrue("Creating open scoped lock must add token to the lock manager.",
                containsLockToken(superuser.getLockTokens(), ltoken));
    }

    public void testGetLockTokensAfterUnlock() throws RepositoryException,
            NotExecutableException {
        assertLockable(testNode);

        boolean sessionScoped = false;
        Lock l = lockMgr.lock(testPath, true, sessionScoped, Long.MAX_VALUE, null);
        String ltoken = l.getLockToken();

        lockMgr.unlock(testPath);
        assertFalse("Removing an open scoped lock must remove the token from the lock manager.",
                containsLockToken(lockMgr, ltoken));
        assertFalse("Removing an open scoped lock must remove the token from the lock manager.",
                containsLockToken(superuser.getLockTokens(), ltoken));
    }

    public void testGetLockTokensSessionScoped() throws RepositoryException,
            NotExecutableException {
        assertLockable(testNode);

        List<String> tokensBefore = Arrays.asList(lockMgr.getLockTokens());

        boolean sessionScoped = true;
        Lock l = lockMgr.lock(testPath, true, sessionScoped, Long.MAX_VALUE, null);

        assertEquals("Creating a session scoped lock must not change the lock tokens.",
                tokensBefore, Arrays.asList(lockMgr.getLockTokens()));
        assertEquals("Creating a session scoped lock must not change the lock tokens.",
                tokensBefore, Arrays.asList(superuser.getLockTokens()));
    }

    public void testAddLockToken() throws RepositoryException,
            NotExecutableException {
        assertLockable(testNode);

        boolean sessionScoped = false;
        Lock l = lockMgr.lock(testPath, true, sessionScoped, Long.MAX_VALUE, null);
        String ltoken = l.getLockToken();

        // adding lock token should have no effect.
        lockMgr.addLockToken(ltoken);
    }

    public void testAddInvalidLockToken() throws RepositoryException {
        try {
            lockMgr.addLockToken("any-token");
            fail("Adding an invalid token must fail.");
        } catch (LockException e) {
            // success
        }
    }

    public void testAddLockTokenToAnotherSession() throws RepositoryException,
            NotExecutableException {
        assertLockable(testNode);

        boolean sessionScoped = false;
        Lock l = lockMgr.lock(testPath, true, sessionScoped, Long.MAX_VALUE, null);
        String ltoken = l.getLockToken();

        Session other = getHelper().getReadWriteSession();
        try {
            LockManager otherLockMgr = getLockManager(other);
            assertFalse(containsLockToken(otherLockMgr, ltoken));

            try {
                otherLockMgr.addLockToken(ltoken);
                if (!openScopedLockMultiple) {
                    fail("Adding token to another session must fail (see config property "
                            + RepositoryStub.PROP_OPEN_SCOPED_LOCK_MULTIPLE + ".");
                }
            } catch (LockException e) {
                if (openScopedLockMultiple) {
                    fail("Adding token to another session must not fail (see config property "
                            + RepositoryStub.PROP_OPEN_SCOPED_LOCK_MULTIPLE + ".");
                }
            }
        } finally {
            other.logout();
        }
    }

    public void testRemoveLockToken() throws Exception {
        assertLockable(testNode);

        boolean sessionScoped = false;
        Lock l = lockMgr.lock(testPath, true, sessionScoped, Long.MAX_VALUE, null);
        String ltoken = l.getLockToken();

        try {
            // remove lock token
            lockMgr.removeLockToken(ltoken);

            assertFalse(containsLockToken(lockMgr, ltoken));
            assertFalse(containsLockToken(superuser.getLockTokens(), ltoken));
        } finally {
            // make sure lock token is added even if test fail
            lockMgr.addLockToken(ltoken);
            assertTrue(containsLockToken(lockMgr, ltoken));
            assertNotNull("Token must be exposed again", l.getLockToken());
            assertEquals("The lock must get the same token again.", ltoken, l.getLockToken());
        }
    }

    public void testRemoveLockToken2() throws Exception {
        assertLockable(testNode);

        boolean sessionScoped = false;
        Lock l = lockMgr.lock(testPath, true, sessionScoped, Long.MAX_VALUE, null);
        String ltoken = l.getLockToken();

        try {
            lockMgr.removeLockToken(ltoken);

            String nlt = l.getLockToken();
            assertTrue("freshly obtained lock token must either be null or the same as the one returned earlier",
                    nlt == null || nlt.equals(ltoken));
        } finally {
            // make sure lock token is added even if test fail
            lockMgr.addLockToken(ltoken);
        }
    }

    public void testRemoveLockToken3() throws Exception {
        assertLockable(testNode);

        boolean sessionScoped = false;
        Lock l = lockMgr.lock(testPath, true, sessionScoped, Long.MAX_VALUE, null);
        String ltoken = l.getLockToken();

        try {
            lockMgr.removeLockToken(ltoken);

            // without holding the token session must not be allowed to modify
            // the locked node.
            try {
                testNode.addNode(nodeName2, testNodeType);
                fail("Session must not be allowed to modify node");
            } catch (LockException e) {
                // expected
            }
        } finally {
            // make sure lock token is added even if test fail
            lockMgr.addLockToken(ltoken);
        }
    }

    public void testRemoveLockTokenTwice() throws Exception {
        assertLockable(testNode);

        boolean sessionScoped = false;
        Lock l = lockMgr.lock(testPath, true, sessionScoped, Long.MAX_VALUE, null);
        String ltoken = l.getLockToken();

        lockMgr.removeLockToken(ltoken);
        try {
            // remove token a second time
            lockMgr.removeLockToken(ltoken);
            fail("Removing a lock token twice must fail.");
        } catch (LockException e) {
            // success
        } finally {
            // make sure lock token is added even if test fail
            lockMgr.addLockToken(ltoken);
        }
    }

    public void testAddLockTokenAgain() throws Exception {
        assertLockable(testNode);

        boolean sessionScoped = false;
        Lock l = lockMgr.lock(testPath, true, sessionScoped, Long.MAX_VALUE, null);
        String ltoken = l.getLockToken();

        try {
            // remove lock token
            lockMgr.removeLockToken(ltoken);
        } finally {
            // make sure lock token is added even if test fail
            lockMgr.addLockToken(ltoken);
            assertTrue(containsLockToken(lockMgr, ltoken));
            assertNotNull("Token must be exposed again", l.getLockToken());
            assertEquals("The lock must get the same token again.", ltoken, l.getLockToken());
        }
    }

    public void testLockTransfer() throws Exception {        
        assertLockable(testNode);

        boolean sessionScoped = false;
        Lock l = lockMgr.lock(testPath, true, sessionScoped, Long.MAX_VALUE, null);
        String ltoken = l.getLockToken();

        Session other = getHelper().getReadWriteSession();
        LockManager otherLockMgr = getLockManager(other);
        try {
            lockMgr.removeLockToken(ltoken);
            otherLockMgr.addLockToken(ltoken);

            assertTrue("The new holding manager must contain the token.", containsLockToken(otherLockMgr, ltoken));

            Lock otherL = otherLockMgr.getLock(testPath);
            assertNotNull("Token must be exposed to new lock holder.", otherL.getLockToken());
            assertEquals("Token must be the same again.", ltoken, otherL.getLockToken());

        } finally {
            otherLockMgr.removeLockToken(ltoken);
            lockMgr.addLockToken(ltoken);
            other.logout();
        }
    }

    public void testLockTransfer2() throws Exception {
        assertLockable(testNode);

        boolean sessionScoped = false;
        Lock l = lockMgr.lock(testPath, true, sessionScoped, Long.MAX_VALUE, null);
        String ltoken = l.getLockToken();

        Session other = getHelper().getReadWriteSession();
        LockManager otherLockMgr = getLockManager(other);
        try {
            lockMgr.removeLockToken(ltoken);
            otherLockMgr.addLockToken(ltoken);

            lockMgr.addLockToken(ltoken);
            if (!openScopedLockMultiple) {
                fail("Adding token to another session must fail (see config property "
                        + RepositoryStub.PROP_OPEN_SCOPED_LOCK_MULTIPLE + ".");
            }
        } catch (LockException e) {
            if (openScopedLockMultiple) {
                fail("Adding token to another session must not fail (see config property "
                        + RepositoryStub.PROP_OPEN_SCOPED_LOCK_MULTIPLE + ".");
            }
        } finally {
            otherLockMgr.removeLockToken(ltoken);
            lockMgr.addLockToken(ltoken);
            other.logout();
        }
    }

    public void testLockTransfer3() throws Exception {
        assertLockable(testNode);

        boolean sessionScoped = false;
        Lock l = lockMgr.lock(testPath, true, sessionScoped, Long.MAX_VALUE, null);
        String ltoken = l.getLockToken();

        Session other = getHelper().getReadWriteSession();
        LockManager otherLockMgr = getLockManager(other);
        try {
            lockMgr.removeLockToken(ltoken);
            otherLockMgr.addLockToken(ltoken);

            lockMgr.removeLockToken(ltoken);
            fail("Removing a token that has been transfered to another manager must fail.");
        } catch (LockException e) {
            // success
        } finally {
            otherLockMgr.removeLockToken(ltoken);
            lockMgr.addLockToken(ltoken);
            other.logout();
        }
    }
}