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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import java.util.List;
import java.util.Arrays;

/** <code>AbstractLockTest</code>... */
public abstract class AbstractLockTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(AbstractLockTest.class);

    protected Node lockedNode;
    protected Node childNode;
    protected Lock lock;

    protected abstract boolean isSessionScoped();
    protected abstract boolean isDeep();

    protected void setUp() throws Exception {
        super.setUp();

        lockedNode = testRootNode.addNode(nodeName1, testNodeType);
        lockedNode.addMixin(mixLockable);
        childNode = lockedNode.addNode(nodeName2, testNodeType);
        testRootNode.save();

        // TODO: remove cast
        // TODO: replace by LockManager#lock call
        lock = (Lock) lockedNode.lock(isDeep(), isSessionScoped());
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

    /**
     *
     */
    public void testIsDeep() {
        assertEquals("Lock.isDeep must be consistent with lock call.", isDeep(), lock.isDeep());
    }

    /**
     *
     */
    public void testIsSessionScoped() {
        assertEquals("Lock.isSessionScoped must be consistent with lock call.", isSessionScoped(), lock.isSessionScoped());
    }

    /**
     *
     * @throws RepositoryException
     */
    public void testIsLockOwningSession() throws RepositoryException {
        assertTrue("Session must be lock owner", lock.isLockOwningSession());
        assertTrue("Session must be lock owner", ((Lock) lockedNode.getLock()).isLockOwningSession());

        Session otherSession = helper.getReadOnlySession();
        try {
            Lock lck = (Lock) ((Node) otherSession.getItem(lockedNode.getPath())).getLock();
            assertFalse("Session must not be lock owner", lck.isLockOwningSession());
        } finally {
            otherSession.logout();
        }

        Session otherAdmin = helper.getSuperuserSession();
        try {
            Lock lck = (Lock) ((Node) otherAdmin.getItem(lockedNode.getPath())).getLock();
            assertFalse("Other Session for the same userID must not be lock owner", lck.isLockOwningSession());
        } finally {
            otherAdmin.logout();
        }
    }

    /**
     *
     */
    public void testGetSecondsRemaining() {
        assertTrue("Seconds remaining must be a positive long or 0.", lock.getSecondsRemaining() >= 0);
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
            List tokens = Arrays.asList(superuser.getLockTokens());
            assertFalse(msg, tokens.contains(lock.getLockToken()));

            assertFalse(msg, lockedNode.hasProperty(jcrLockOwner));
            assertFalse(msg, lockedNode.hasProperty(jcrlockIsDeep));

        } catch (ConstraintViolationException e) {
            // cannot remove the mixin -> ok
            // consequently the node must still be locked, the lock still live...
            String msg = "Lock must still be live.";
            assertTrue(msg, lock.isLive());
            assertTrue(msg, lockedNode.isLocked());
            List tokens = Arrays.asList(superuser.getLockTokens());
            assertTrue(tokens.contains(lock.getLockToken()));
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