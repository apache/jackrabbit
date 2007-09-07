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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.Repository;
import javax.jcr.Node;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.NodeIterator;
import javax.jcr.version.VersionException;
import javax.jcr.version.Version;
import javax.jcr.lock.Lock;
import javax.transaction.UserTransaction;
import javax.transaction.RollbackException;
import java.util.StringTokenizer;

/**
 * <code>XATest</code> contains the test cases for the methods
 * inside {@link org.apache.jackrabbit.api.XASession}.
 */
public class XATest extends AbstractJCRTest {

    /**
     * Other superuser.
     */
    private Session otherSuperuser;

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        super.setUp();

        otherSuperuser = helper.getSuperuserSession();

        // clean testroot on second workspace
        Session s2 = helper.getSuperuserSession(workspaceName);
        Node root = s2.getRootNode();
        if (root.hasNode(testPath)) {
            // clean test root
            Node testRootNode = root.getNode(testPath);
            for (NodeIterator children = testRootNode.getNodes(); children.hasNext();) {
                children.nextNode().remove();
            }
        } else {
            // create nodes to testPath
            StringTokenizer names = new StringTokenizer(testPath, "/");
            Node currentNode = root;
            while (names.hasMoreTokens()) {
                String name = names.nextToken();
                if (currentNode.hasNode(name)) {
                    currentNode = currentNode.getNode(name);
                } else {
                    currentNode = currentNode.addNode(name, testNodeType);
                }
            }
        }
        root.save();

    }

    /**
     * {@inheritDoc}
     */
    protected void tearDown() throws Exception {
        if (otherSuperuser != null) {
            otherSuperuser.logout();
        }
        super.tearDown();
    }

    /**
     * @see junit.framework#runTest
     *
     * Make sure that tested repository supports transactions
     */
    protected void runTest() throws Throwable {
        if (isSupported(Repository.OPTION_TRANSACTIONS_SUPPORTED)) {
            super.runTest();
        }
    }

    /**
     * Add a node inside a transaction and commit changes. Make sure
     * node exists for other sessions only after commit.
     * @throws Exception
     */
    public void testAddNodeCommit() throws Exception {
        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser);

        // start transaction
        utx.begin();

        // add node and save
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.addMixin(mixReferenceable);
        testRootNode.save();

        // assertion: node exists in this session
        try {
            superuser.getNodeByUUID(n.getUUID());
        } catch (ItemNotFoundException e) {
            fail("New node not visible after save()");
        }

        // assertion: node does not exist in other session
        Session otherSuperuser = helper.getSuperuserSession();

        try {
            otherSuperuser.getNodeByUUID(n.getUUID());
            fail("Uncommitted node visible for other session");
        } catch (ItemNotFoundException e) {
            /* expected */
        }

        // commit
        utx.commit();

        // assertion: node exists in this session
        try {
            superuser.getNodeByUUID(n.getUUID());
        } catch (ItemNotFoundException e) {
            fail("Committed node not visible in this session");
        }

        // assertion: node also exists in other session
        try {
            otherSuperuser.getNodeByUUID(n.getUUID());
        } catch (ItemNotFoundException e) {
            fail("Committed node not visible in other session");
        }

        // logout
        otherSuperuser.logout();
    }

    /**
     * Set a property inside a transaction and commit changes. Make sure
     * property exists for other sessions only after commit.
     * @throws Exception
     */
    public void testSetPropertyCommit() throws Exception {
        // prerequisite: non-existing property
        if (testRootNode.hasProperty(propertyName1)) {
            testRootNode.getProperty(propertyName1).remove();
            testRootNode.save();
        }

        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser);

        // start transaction
        utx.begin();

        // set property and save
        testRootNode.setProperty(propertyName1, "0");
        testRootNode.save();

        // assertion: property exists in this session
        assertTrue(testRootNode.hasProperty(propertyName1));

        // assertion: property does not exist in other session
        Session otherSuperuser = helper.getSuperuserSession();
        Node otherRootNode = otherSuperuser.getRootNode().getNode(testPath);
        assertFalse(otherRootNode.hasProperty(propertyName1));

        // commit
        utx.commit();

        // assertion: property exists in this session
        assertTrue(testRootNode.hasProperty(propertyName1));

        // assertion: property also exists in other session
        assertTrue(otherRootNode.hasProperty(propertyName1));

        // logout
        otherSuperuser.logout();
    }

    /**
     * @throws Exception
     */
    public void testAddAndSetProperty() throws Exception {
        // prerequisite: non-existing property
        if (testRootNode.hasProperty(propertyName1)) {
            testRootNode.getProperty(propertyName1).remove();
            testRootNode.save();
        }

        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser);

        // start transaction
        utx.begin();

        // 'add' property and save
        testRootNode.setProperty(propertyName1, "0");
        testRootNode.save();

        // 'modify' property and save
        testRootNode.setProperty(propertyName1, "1");
        testRootNode.save();

        // commit
        utx.commit();

        // check property value
        Session otherSuperuser = helper.getSuperuserSession();
        Node n = (Node) otherSuperuser.getItem(testRootNode.getPath());
        assertEquals(n.getProperty(propertyName1).getString(), "1");
        otherSuperuser.logout();
    }

    /**
     * @throws Exception
     */
    public void testPropertyIsNew() throws Exception {
        // prerequisite: non-existing property
        if (testRootNode.hasProperty(propertyName1)) {
            testRootNode.getProperty(propertyName1).remove();
            testRootNode.save();
        }

        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser);

        // start transaction
        utx.begin();

        // 'add' property and save
        testRootNode.setProperty(propertyName1, "0");

        assertTrue("New property must be new.", testRootNode.getProperty(propertyName1).isNew());

        testRootNode.save();

        assertFalse("Saved property must not be new.", testRootNode.getProperty(propertyName1).isNew());

        // commit
        utx.commit();
    }

    /**
     * @throws Exception
     */
    public void testNewNodeIsLocked() throws Exception {
        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser);

        // start transaction
        utx.begin();

        // add node and save
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();

        assertFalse("New node must not be locked.", n.isLocked());

        // commit
        utx.commit();
    }

    /**
     * @throws Exception
     */
    public void testPropertyIsModified() throws Exception {
        // prerequisite: existing property
        testRootNode.setProperty(propertyName1, "0");
        testRootNode.save();

        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser);

        // start transaction
        utx.begin();

        // 'add' property and save
        testRootNode.setProperty(propertyName1, "1");

        assertTrue("Unsaved property must be modified.", testRootNode.getProperty(propertyName1).isModified());

        testRootNode.save();

        assertFalse("Saved property must not be modified.", testRootNode.getProperty(propertyName1).isModified());

        // commit
        utx.commit();
    }

    /**
     * @throws Exception
     */
    public void testDeleteAndAddProperty() throws Exception {
        // prerequisite: existing property
        testRootNode.setProperty(propertyName1, "0");
        testRootNode.save();

        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser);

        // start transaction
        utx.begin();

        // 'delete' property and save
        testRootNode.getProperty(propertyName1).remove();
        testRootNode.save();

        // 'add' property and save
        testRootNode.setProperty(propertyName1, "1");
        testRootNode.save();

        // commit
        utx.commit();

        // check property value
        Session otherSuperuser = helper.getSuperuserSession();
        Node n = (Node) otherSuperuser.getItem(testRootNode.getPath());
        assertEquals(n.getProperty(propertyName1).getString(), "1");
        otherSuperuser.logout();
    }

    /**
     * @throws Exception
     */
    public void testModifyAndDeleteProperty() throws Exception {
        // prerequisite: existing property
        testRootNode.setProperty(propertyName1, "0");
        testRootNode.save();

        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser);

        // start transaction
        utx.begin();

        // 'modify' property and save
        testRootNode.setProperty(propertyName1, "1");
        testRootNode.save();

        // 'delete' property and save
        testRootNode.getProperty(propertyName1).remove();
        testRootNode.save();

        // commit
        utx.commit();

        // check property value
        Session otherSuperuser = helper.getSuperuserSession();
        Node n = (Node) otherSuperuser.getItem(testRootNode.getPath());
        assertFalse("Property must be deleted.", n.hasProperty(propertyName1));
        otherSuperuser.logout();
    }

    /**
     * @throws Exception
     */
    public void testAddAndDeleteProperty() throws Exception {
        // prerequisite: non-existing property
        if (testRootNode.hasProperty(propertyName1)) {
            testRootNode.getProperty(propertyName1).remove();
            testRootNode.save();
        }

        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser);

        // start transaction
        utx.begin();

        // 'add' property and save
        testRootNode.setProperty(propertyName1, "1");
        testRootNode.save();

        // 'delete' property and save
        testRootNode.getProperty(propertyName1).remove();
        testRootNode.save();

        // commit
        utx.commit();

        // check property value
        Session otherSuperuser = helper.getSuperuserSession();
        Node n = (Node) otherSuperuser.getItem(testRootNode.getPath());
        assertFalse("Property must be deleted.", n.hasProperty(propertyName1));
        otherSuperuser.logout();
    }

    /**
     * Add a node inside a transaction and rollback changes.
     * @throws Exception
     */
    public void testAddNodeRollback() throws Exception {
        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser);

        // start transaction
        utx.begin();

        // add node and save
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.addMixin(mixReferenceable);
        testRootNode.save();

        // assertion: node exists in this session
        String uuid = n.getUUID();

        try {
            superuser.getNodeByUUID(uuid);
        } catch (ItemNotFoundException e) {
            fail("New node not visible after save()");
        }

        // rollback
        utx.rollback();

        // assertion: node does not exist in this session
        try {
            superuser.getNodeByUUID(uuid);
            fail("Node still visible after rollback()");
        } catch (ItemNotFoundException e) {
            /* expected */
        }
    }

    /**
     * Set a property inside a transaction and rollback changes.
     * @throws Exception
     */
    public void testSetPropertyRollback() throws Exception {
        // prerequisite: non-existing property
        if (testRootNode.hasProperty(propertyName1)) {
            testRootNode.getProperty(propertyName1).remove();
            testRootNode.save();
        }

        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser);

        // start transaction
        utx.begin();

        // set property and save
        testRootNode.setProperty(propertyName1, "0");
        testRootNode.save();

        // assertion: property exists in this session
        assertTrue(testRootNode.hasProperty(propertyName1));

        // rollback
        utx.rollback();

        // assertion: property does not exist in this session
        assertFalse(testRootNode.hasProperty(propertyName1));
    }

    /**
     * Remove a node inside a transaction and rollback changes. Check
     * that the node reference may again be used after having rolled
     * back changes.
     * @throws Exception
     */
    public void testRemoveNodeRollback() throws Exception {
        // prerequisite: existing node
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.addMixin(mixReferenceable);
        testRootNode.save();

        String uuid = n1.getUUID();

        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser);

        // start transaction
        utx.begin();

        // remove node and save
        Node n2 = superuser.getNodeByUUID(uuid);
        n2.remove();
        testRootNode.save();

        // assertion: node no longer exists
        try {
            superuser.getNodeByUUID(uuid);
            fail("Removed node still exists after save()");
        } catch (ItemNotFoundException e) {
            /* expected */
        }

        // rollback
        utx.rollback();

        // assertion: node exists again
        try {
            superuser.getNodeByUUID(uuid);
        } catch (ItemNotFoundException e) {
            fail("Removed node not visible after rollback()");
        }
    }

    /**
     * Remove a property inside a transaction and rollback changes.
     * Check that the property reference may again be used after
     * having rolled back changes.
     * @throws Exception
     */
    public void testRemovePropertyRollback() throws Exception {
        // prerequisite: existing property
        if (!testRootNode.hasProperty(propertyName1)) {
            testRootNode.setProperty(propertyName1, "0");
            testRootNode.save();
        }

        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser);

        // start transaction
        utx.begin();

        // remove property and save
        testRootNode.getProperty(propertyName1).remove();
        testRootNode.save();

        // assertion: property no longer exists
        assertFalse(testRootNode.hasProperty(propertyName1));

        // rollback
        utx.rollback();

        // assertion: property exists and reference valid
        assertTrue(testRootNode.hasProperty(propertyName1));
    }

    /**
     * Add reference to some node in one session while removing
     * the node in another.
     * @throws Exception
     */
    public void testAddReference() throws Exception {
        // add two nodes, second one referenceable
        Node n1 = testRootNode.addNode(nodeName1);
        Node n2 = testRootNode.addNode(nodeName2);
        n2.addMixin(mixReferenceable);
        testRootNode.save();

        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser);

        // start transaction
        utx.begin();

        // add reference and save
        n1.setProperty(propertyName1, n2);
        testRootNode.save();

        // remove referenced node in other session
        Session otherSuperuser = helper.getSuperuserSession();
        Node otherRootNode = otherSuperuser.getRootNode().getNode(testPath);
        otherSuperuser.getNodeByUUID(n2.getUUID()).remove();
        otherRootNode.save();

        // assertion: commit must fail since integrity violated
        try {
            utx.commit();
            fail("Commit succeeds with violated integrity");
        } catch (RollbackException e) {
            /* expected */
        }

        // logout
        otherSuperuser.logout();
    }

    //--------------------------------------------------------------< locking >

    /**
     * Test locking a node in one session. Verify that node is not locked
     * in other session until commit.
     * @throws Exception
     */
    public void testLockCommit() throws Exception {
        Session other = helper.getSuperuserSession();

        // add node that is both lockable and referenceable, save
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixLockable);
        n.addMixin(mixReferenceable);
        testRootNode.save();

        // reference node in second session
        Node nOther = other.getNodeByUUID(n.getUUID());

        // verify node is not locked in either session
        assertFalse("Node not locked in session 1", n.isLocked());
        assertFalse("Node not locked in session 2", nOther.isLocked());

        // get user transaction object, start and lock node
        UserTransaction utx = new UserTransactionImpl(superuser);
        utx.begin();
        n.lock(false, true);

        // verify node is locked in first session only
        assertTrue("Node locked in session 1", n.isLocked());
        assertFalse("Node not locked in session 2", nOther.isLocked());

        // commit in first session
        utx.commit();

        // verify node is locked in both sessions
        assertTrue("Node locked in session 1", n.isLocked());
        assertTrue("Node locked in session 2", nOther.isLocked());

        // logout
        other.logout();
    }

    /**
     * Test locking a node in one session. Verify that node is not locked
     * in session after rollback.
     * @throws Exception
     */
    public void testLockRollback() throws Exception {
        Session other = helper.getSuperuserSession();

        // add node that is both lockable and referenceable, save
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixLockable);
        n.addMixin(mixReferenceable);
        testRootNode.save();

        // reference node in second session
        Node nOther = other.getNodeByUUID(n.getUUID());

        // verify node is not locked in either session
        assertFalse("Node not locked in session 1", n.isLocked());
        assertFalse("Node not locked in session 2", nOther.isLocked());

        // get user transaction object, start and lock node
        UserTransaction utx = new UserTransactionImpl(superuser);
        utx.begin();
        n.lock(false, true);

        // verify node is locked in first session only
        assertTrue("Node locked in session 1", n.isLocked());
        assertFalse("Node not locked in session 2", nOther.isLocked());

        // rollback in first session
        utx.rollback();

        // verify node is not locked in either session
        assertFalse("Node not locked in session 1", n.isLocked());
        assertFalse("Node not locked in session 2", nOther.isLocked());

        // logout
        other.logout();
    }

    /**
     * Test locking a node inside a transaction that has been locked in another
     * session, which leads to a failure when committing.
     * @throws Exception
     */
    public void testLockTwice() throws Exception {
        Session other = helper.getSuperuserSession();

        // add node that is both lockable and referenceable, save
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixLockable);
        n.addMixin(mixReferenceable);
        testRootNode.save();

        // reference node in second session
        Node nOther = other.getNodeByUUID(n.getUUID());

        // verify node is not locked in either session
        assertFalse("Node not locked in session 1", n.isLocked());
        assertFalse("Node not locked in session 2", nOther.isLocked());

        // get user transaction object, start and lock node
        UserTransaction utx = new UserTransactionImpl(superuser);
        utx.begin();
        n.lock(false, true);

        // lock node in non-transactional session, too
        nOther.lock(false, true);

        // verify node is locked in both sessions
        assertTrue("Node locked in session 1", n.isLocked());
        assertTrue("Node locked in session 2", nOther.isLocked());

        // assertion: commit must fail since node has already been locked
        try {
            utx.commit();
            fail("Commit succeeds with double locking");
        } catch (RollbackException e) {
            /* expected */
        }

        // verify node is locked in both sessions
        assertTrue("Node locked in session 1", n.isLocked());
        assertTrue("Node locked in session 2", nOther.isLocked());

        // logout
        other.logout();
    }

    /**
     * Test locking a new node inside a transaction.
     * @throws Exception
     */
    public void xxxtestLockNewNode() throws Exception {
        // get user transaction object, start
        UserTransaction utx = new UserTransactionImpl(superuser);
        utx.begin();

        // add node that is both lockable and referenceable, save
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixLockable);
        n.addMixin(mixReferenceable);
        testRootNode.save();

        // lock this new node
        n.lock(false, true);

        // commit
        utx.commit();
    }

    /**
     * Test locking/unlocking a node inside a transaction which should be a
     * no-op.
     * @throws Exception
     */
    public void testLockUnlock() throws Exception {
        // add node that is both lockable and referenceable, save
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixLockable);
        n.addMixin(mixReferenceable);
        testRootNode.save();

        // verify node is not locked in this session
        assertFalse("Node not locked", n.isLocked());

        // get user transaction object, start and lock node
        UserTransaction utx = new UserTransactionImpl(superuser);
        utx.begin();
        n.lock(false, true);

        // verify node is locked
        assertTrue("Node locked", n.isLocked());

        // unlock node
        n.unlock();

        // commit
        utx.commit();

        // verify node is not locked
        assertFalse("Node not locked", n.isLocked());
    }

    /**
     * Test correct behaviour of {@link javax.jcr.lock.Lock} inside a
     * transaction.
     * @throws Exception
     */
    public void testLockBehaviour() throws Exception {
        // add node that is both lockable and referenceable, save
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixLockable);
        n.addMixin(mixReferenceable);
        testRootNode.save();

        // get user transaction object, start and lock node
        UserTransaction utx = new UserTransactionImpl(superuser);
        utx.begin();
        Lock lock = n.lock(false, true);

        // verify lock is live
        assertTrue("Lock live", lock.isLive());

        // rollback
        utx.rollback();

        // verify lock is not live anymore
        assertFalse("Lock not live", lock.isLive());
    }

    /**
     * Test correct behaviour of {@link javax.jcr.lock.Lock} inside a
     * transaction.
     * @throws Exception
     */
    public void testLockBehaviour2() throws Exception {
        // add node that is both lockable and referenceable, save
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixLockable);
        n.addMixin(mixReferenceable);
        testRootNode.save();

        Lock lock = n.lock(false, true);

        // get user transaction object, start
        UserTransaction utx = new UserTransactionImpl(superuser);
        utx.begin();

        // verify lock is live
        assertTrue("Lock live", lock.isLive());

        // unlock
        n.unlock();

        // verify lock is no longer live
        assertFalse("Lock not live", lock.isLive());

        // rollback
        utx.rollback();

        // verify lock is live again
        assertTrue("Lock live", lock.isLive());
    }

    //-----------------------------------------------------------< versioning >

    /**
     * Checkin inside tx should not be visible to other users.
     */
    public void testCheckin() throws Exception {
        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser);

        // add node and save
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.addMixin(mixVersionable);
        testRootNode.save();

        // reference node in other session
        Node nOther = otherSuperuser.getNodeByUUID(n.getUUID());

        // start transaction
        utx.begin();

        // checkin node
        n.checkin();

        // assert: base versions must differ
        if (n.getBaseVersion().getName().equals(nOther.getBaseVersion().getName())) {
            fail("Base versions must differ");
        }

        // assert: version must not be visible to other session
        try {
            nOther.getVersionHistory().getVersion(n.getBaseVersion().getName());
            fail("Version must not be visible to other session.");
        } catch (VersionException e) {
            // expected.
        }

        // commit
        utx.commit();

        // assert: base versions must be equal
        assertEquals("Base versions must be equal",
                n.getBaseVersion().getName(), nOther.getBaseVersion().getName());
    }

    /**
     * Checkin from two sessions simultaneously should throw when committing.
     * @throws Exception
     */
    public void testConflictingCheckin() throws Exception {
        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser);

        // add node and save
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.addMixin(mixVersionable);
        testRootNode.save();

        // reference node in other session
        Node nOther = otherSuperuser.getNodeByUUID(n.getUUID());

        // start transaction
        utx.begin();

        // checkin node inside tx
        n.checkin();

        // checkin node outside tx
        nOther.checkin();

        // commit
        try {
            utx.commit();
            fail("Commit failing with modified version history.");
        } catch (RollbackException e) {
            // expected
        }
    }

    /**
     * Test removed version gets invalid for other users on commit.
     */
    public void testRemoveVersion() throws Exception {
        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser);

        // add node and save
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.addMixin(mixVersionable);
        testRootNode.save();

        // reference node in other session
        Node nOther = otherSuperuser.getNodeByUUID(n.getUUID());

        // create two versions, reference first version in other session
        n.checkin();
        Version vOther = nOther.getBaseVersion();
        n.checkout();
        n.checkin();

        // start transaction
        utx.begin();

        // remove version and commit
        n.getVersionHistory().removeVersion(vOther.getName());

        // commit
        utx.commit();

        // assert: version has become invalid
        try {
            vOther.getPredecessors();
            fail("Removed version still operational.");
        } catch (RepositoryException e) {
            // expected
        }
    }

    /**
     * Tests a couple of checkin/restore/remove operations on different
     * workspaces and different transactions.
     *
     * @throws Exception
     */
    public void testXAVersionsThoroughly() throws Exception {
        Session s1 = superuser;
        Session s2 = helper.getSuperuserSession(workspaceName);

        // add node and save
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.addMixin(mixVersionable);
        testRootNode.save();

        if (!s2.itemExists(testRootNode.getPath())) {
            s2.getRootNode().addNode(testRootNode.getName());
            s2.save();
        }
        s2.getWorkspace().clone(s1.getWorkspace().getName(), n1.getPath(), n1.getPath(), true);
        Node n2 = (Node) s2.getItem(n1.getPath());

        //log.println("---------------------------------------");
        String phase="init";

        Version v1_1 = n1.getBaseVersion();
        Version v2_1 = n2.getBaseVersion();

        check(v1_1, phase, "jcr:rootVersion", 0);
        check(v2_1, phase, "jcr:rootVersion", 0);

        //log.println("--------checkout/checkin n1 (uncommitted)----------");
        phase="checkin N1 uncomitted.";

        UserTransaction tx = new UserTransactionImpl(s1);
        tx.begin();

        n1.checkout();
        n1.checkin();

        Version v1_2 = n1.getBaseVersion();

        check(v1_1, phase, "jcr:rootVersion", 1);
        check(v2_1, phase, "jcr:rootVersion", 0);
        check(v1_2, phase, "1.0", 0);

        //log.println("--------checkout/checkin n1 (comitted)----------");
        phase="checkin N1 committed.";

        tx.commit();

        check(v1_1, phase, "jcr:rootVersion", 1);
        check(v2_1, phase, "jcr:rootVersion", 1);
        check(v1_2, phase, "1.0", 0);

        //log.println("--------restore n2 (uncommitted) ----------");
        phase="restore N2 uncommitted.";

        tx = new UserTransactionImpl(s2);
        tx.begin();

        n2.restore("1.0", false);
        Version v2_2 = n2.getBaseVersion();

        check(v1_1, phase, "jcr:rootVersion", 1);
        check(v2_1, phase, "jcr:rootVersion", 1);
        check(v1_2, phase, "1.0", 0);
        check(v2_2, phase, "1.0", 0);

        //log.println("--------restore n2 (comitted) ----------");
        phase="restore N2 committed.";

        tx.commit();

        check(v1_1, phase, "jcr:rootVersion", 1);
        check(v2_1, phase, "jcr:rootVersion", 1);
        check(v1_2, phase, "1.0", 0);
        check(v2_2, phase, "1.0", 0);

        //log.println("--------checkout/checkin n2 (uncommitted) ----------");
        phase="checkin N2 uncommitted.";

        tx = new UserTransactionImpl(s2);
        tx.begin();

        n2.checkout();
        n2.checkin();

        Version v2_3 = n2.getBaseVersion();

        check(v1_1, phase, "jcr:rootVersion", 1);
        check(v2_1, phase, "jcr:rootVersion", 1);
        check(v1_2, phase, "1.0", 0);
        check(v2_2, phase, "1.0", 1);
        check(v2_3, phase, "1.1", 0);

        //log.println("--------checkout/checkin n2 (committed) ----------");
        phase="checkin N2 committed.";

        tx.commit();

        check(v1_1, phase, "jcr:rootVersion", 1);
        check(v2_1, phase, "jcr:rootVersion", 1);
        check(v1_2, phase, "1.0", 1);
        check(v2_2, phase, "1.0", 1);
        check(v2_3, phase, "1.1", 0);

        //log.println("--------checkout/checkin n1 (uncommitted) ----------");
        phase="checkin N1 uncommitted.";

        tx = new UserTransactionImpl(s1);
        tx.begin();

        n1.checkout();
        n1.checkin();

        Version v1_3 = n1.getBaseVersion();

        check(v1_1, phase, "jcr:rootVersion", 1);
        check(v2_1, phase, "jcr:rootVersion", 1);
        check(v1_2, phase, "1.0", 2);
        check(v2_2, phase, "1.0", 1);
        check(v2_3, phase, "1.1", 0);
        check(v1_3, phase, "1.0.0", 0);

        //log.println("--------checkout/checkin n1 (committed) ----------");
        phase="checkin N1 committed.";

        tx.commit();

        check(v1_1, phase, "jcr:rootVersion", 1);
        check(v2_1, phase, "jcr:rootVersion", 1);
        check(v1_2, phase, "1.0", 2);
        check(v2_2, phase, "1.0", 2);
        check(v2_3, phase, "1.1", 0);
        check(v1_3, phase, "1.0.0", 0);

        //log.println("--------remove n1-1.0 (uncommitted) ----------");
        phase="remove N1 1.0 uncommitted.";

        tx = new UserTransactionImpl(s1);
        tx.begin();

        n1.getVersionHistory().removeVersion("1.0");

        check(v1_1, phase, "jcr:rootVersion", 2);
        check(v2_1, phase, "jcr:rootVersion", 1);
        check(v1_2, phase, "1.0", -1);
        check(v2_2, phase, "1.0", 2);
        check(v2_3, phase, "1.1", 0);
        check(v1_3, phase, "1.0.0", 0);

        //log.println("--------remove n1-1.0  (committed) ----------");
        phase="remove N1 1.0 committed.";

        tx.commit();

        check(v1_1, phase, "jcr:rootVersion", 2);
        check(v2_1, phase, "jcr:rootVersion", 2);
        check(v1_2, phase, "1.0", -1);
        check(v2_2, phase, "1.0", -1);
        check(v2_3, phase, "1.1", 0);
        check(v1_3, phase, "1.0.0", 0);

        //s1.logout();
        s2.logout();

    }

    /**
     * helper method for {@link #testXAVersionsThoroughly()}
     */
    private void check(Version v, String phase, String name, int numSucc) {
        String vName;
        int vSucc = -1;
        try {
            vName = v.getName();
            //vSucc = v.getProperty("jcr:successors").getValues().length;
            vSucc = v.getSuccessors().length;
        } catch (RepositoryException e) {
            // node is invalid after remove
            vName = name;
        }
        assertEquals(phase + " Version Name", name, vName);
        assertEquals(phase + " Num Successors", numSucc, vSucc);
    }



    /**
     * Test new version label becomes available to other sessions on commit.
     */
    public void testSetVersionLabel() throws Exception {
        final String versionLabel = "myVersion";

        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser);

        // add node and save
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.addMixin(mixVersionable);
        testRootNode.save();

        // reference node in other session
        Node nOther = otherSuperuser.getNodeByUUID(n.getUUID());

        // create another version
        Version v = n.checkin();

        // start transaction
        utx.begin();

        // add new version label
        n.getVersionHistory().addVersionLabel(v.getName(), versionLabel, false);

        // assert: version label unknown in other session
        try {
            nOther.getVersionHistory().getVersionByLabel(versionLabel);
            fail("Version label visible outside tx.");
        } catch (VersionException e) {
            // expected
        }

        // commit
        utx.commit();

        // assert: version label known in other session
        nOther.getVersionHistory().getVersionByLabel(versionLabel);
    }
}
