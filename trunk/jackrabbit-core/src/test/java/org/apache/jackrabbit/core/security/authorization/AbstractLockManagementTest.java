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
package org.apache.jackrabbit.core.security.authorization;

import javax.jcr.security.Privilege;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockManager;

/** <code>AbstractVersionAccessTest</code>... */
public abstract class AbstractLockManagementTest extends AbstractEvaluationTest {

    private Node createLockableNode(Node parent) throws RepositoryException, NotExecutableException {
        Node n = parent.addNode(nodeName1);
        if (!n.isNodeType(mixLockable)) {
            if (n.canAddMixin(mixLockable)) {
                n.addMixin(mixLockable);
            } else {
                throw new NotExecutableException();
            }
            parent.save();
        }
        return n;
    }

    private Node createLockedNode(Node parent) throws RepositoryException, NotExecutableException {
        Node n = createLockableNode(parent);
        // create a deep, session scoped lock
        n.lock(true, true);
        return n;
    }

    public void testReadLockContent() throws RepositoryException, NotExecutableException {
        Node n = createLockedNode(testRootNode);
        Node childN = n.addNode(nodeName2);
        modifyPrivileges(n.getPath(), Privilege.JCR_READ, false);
        modifyPrivileges(childN.getPath(), Privilege.JCR_READ, true);

        Node childN2 = (Node) getTestSession().getItem(childN.getPath());
        try {
            childN2.getLock();
            fail("TestUser doesn't have permission to read the jcr:lockIsDeep property.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testLock2() throws RepositoryException, NotExecutableException {
        Node n = createLockableNode(testRootNode);

        modifyPrivileges(n.getPath(), PrivilegeRegistry.REP_WRITE, false);
        modifyPrivileges(n.getPath(), Privilege.JCR_LOCK_MANAGEMENT, true);

        Node n2 = getTestNode().getNode(nodeName1);

        // all lock operations must succeed
        Lock l = n2.lock(true, true);
        l.refresh();
        n2.unlock();
    }

    public void testLock3() throws RepositoryException, NotExecutableException {
        Node n = createLockableNode(testRootNode);

        Node trn = getTestNode();
        modifyPrivileges(trn.getPath(), Privilege.JCR_READ, true);
        modifyPrivileges(trn.getPath(), PrivilegeRegistry.REP_WRITE, true);
        modifyPrivileges(trn.getPath(), Privilege.JCR_LOCK_MANAGEMENT, true);

        Node n2 = trn.getNode(n.getName());
        n2.lock(true, true);
        Lock l = n2.getLock();

        // withdraw lock-mgmt -> must not be able to refresh the lock or
        // unlock the node
        modifyPrivileges(trn.getPath(), Privilege.JCR_LOCK_MANAGEMENT, false);

        try {
            l.refresh();
            fail("TestUser doesn't have permission to refresh the lock.");
        } catch (AccessDeniedException e) {
            // success
        }
        try {
            n2.unlock();
            fail("TestUser doesn't have permission to unlock the node.");
        } catch (AccessDeniedException e) {
            // success
        }

        // make sure the lock can be removed upon session.logout.
        modifyPrivileges(trn.getPath(), Privilege.JCR_LOCK_MANAGEMENT, true);
    }

    public void testLock4() throws RepositoryException, NotExecutableException {
        Node n = createLockableNode(testRootNode);

        Node trn = getTestNode();
        modifyPrivileges(trn.getPath(), Privilege.JCR_READ, true);
        modifyPrivileges(trn.getPath(), PrivilegeRegistry.REP_WRITE, true);
        modifyPrivileges(trn.getPath(), Privilege.JCR_LOCK_MANAGEMENT, true);

        Node n2 = trn.getNode(n.getName());
        n2.lock(true, true);
        Lock l = n2.getLock();
        String lt = l.getLockToken();

        // withdraw lock-mgmt -> logout of session must still remove the lock
        modifyPrivileges(trn.getPath(), Privilege.JCR_LOCK_MANAGEMENT, false);

        getTestSession().logout();
        boolean isLocked = n.isLocked();
        assertFalse(isLocked);
    }

    public void testLockBreaking() throws RepositoryException, NotExecutableException {
        String locktoken = null;
        LockManager sulm = superuser.getWorkspace().getLockManager();
        String lockedpath = null;

        try {
            Node trn = getTestNode();
            modifyPrivileges(trn.getPath(), Privilege.JCR_READ, true);
            modifyPrivileges(trn.getPath(), PrivilegeRegistry.REP_WRITE, true);
            modifyPrivileges(trn.getPath(), Privilege.JCR_LOCK_MANAGEMENT, true);

            Session lockingSession = trn.getSession();

            assertFalse("super user and test user should have different user ids: " + lockingSession.getUserID() + " vs " + superuser.getUserID(),
                    lockingSession.getUserID().equals(superuser.getUserID()));

            trn.addNode("locktest", "nt:unstructured");
            trn.addMixin("mix:lockable");
            lockingSession.save();

            // let the "other" user lock the node
            LockManager oulm = lockingSession.getWorkspace().getLockManager();
            Lock l = oulm.lock(trn.getPath(), true, false, Long.MAX_VALUE, null);
            lockedpath = trn.getPath();
            locktoken = l.getLockToken();
            lockingSession.logout();

            // transfer the lock token to the super user and try the unlock

            Node lockednode = superuser.getNode(lockedpath);
            assertTrue(lockednode.isLocked());
            Lock sl = sulm.getLock(lockedpath);
            assertNotNull(sl.getLockToken());
            sulm.addLockToken(sl.getLockToken());
            sulm.unlock(lockedpath);
            locktoken = null;
        }
        finally {
            if (locktoken != null && lockedpath != null) {
                sulm.addLockToken(locktoken);
                sulm.unlock(lockedpath);
            }
        }
    }
}
