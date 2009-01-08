/*
 * $Id$
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.jackrabbit.core.security.authorization;

import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;

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
}
