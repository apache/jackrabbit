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
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.core.WorkspaceImpl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.lock.LockException;

/** <code>LockManagerTest</code>... */
public class LockManagerTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(LockManagerTest.class);

    protected LockManager lockMgr;
    protected Node testNode;
    protected String testPath;

    protected void setUp() throws Exception {
        super.setUp();

        testNode = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        testPath = testNode.getPath();

        // TODO: rm cast and adjust call as soon as 283 is released
        lockMgr = ((WorkspaceImpl) superuser.getWorkspace()).get283LockManager();
    }

    protected void tearDown() throws Exception  {
        if (lockMgr != null && lockMgr.holdsLock(testPath)) {
            lockMgr.unlock(testPath);
        }
        super.tearDown();
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

    public void testLockWithPendingChanges() throws RepositoryException {
        // transient modification
        testNode.addNode(nodeName2);
        try {
            lockMgr.lock(testPath, true, true, Long.MAX_VALUE, superuser.getUserID());
            fail("Attempt to lock a node with transient modifications must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            // success
        }
    }

    public void testNullOwnerHint() throws RepositoryException {
        testNode.addMixin(mixLockable);
        testRootNode.save();

        Lock l = lockMgr.lock(testPath, true, true, Long.MAX_VALUE, null);
        assertNotNull(l.getLockOwner());
    }
}