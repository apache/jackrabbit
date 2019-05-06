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
import javax.jcr.Property;
import javax.jcr.InvalidItemStateException;
import javax.jcr.version.VersionException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.NodeType;
import javax.transaction.UserTransaction;
import javax.transaction.RollbackException;
import java.util.StringTokenizer;

/**
 * <code>XATest</code> contains the test cases for the methods
 * inside {@link XASessionImpl}.
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

        otherSuperuser = getHelper().getSuperuserSession();

        // clean testroot on second workspace
        Session s2 = getHelper().getSuperuserSession(workspaceName);
        try {
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
        } finally {
            s2.logout();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void tearDown() throws Exception {
        if (otherSuperuser != null) {
            otherSuperuser.logout();
            otherSuperuser = null;
        }
        super.tearDown();
    }

    /**
     * @see junit.framework.TestCase#runTest()
     *
     * Make sure that tested repository supports transactions
     */
    protected void runTest() throws Throwable {
        if (isSupported(Repository.OPTION_TRANSACTIONS_SUPPORTED)) {
            super.runTest();
        }
    }

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/JCR-2796">JCR-2796</a>.
     */
    public void testRestore() throws Exception {
        Session session = getHelper().getSuperuserSession();
        try {
            VersionManager vm = session.getWorkspace().getVersionManager();

            // make sure that 'testNode' does not exist at the beginning
            // of the test
            while (session.nodeExists("/testNode")) {
                session.getNode("/testNode").remove();
                session.save();
            }

            // 1) create 'testNode' that has a child and a grandchild
            Node node = session.getRootNode().addNode("testNode");
            node.addMixin(NodeType.MIX_VERSIONABLE);
            node.addNode("child").addNode("grandchild");
            session.save();

            // 2) check in 'testNode' and give a version-label
            Version version = vm.checkin(node.getPath());
            vm.getVersionHistory(node.getPath()).addVersionLabel(
                    version.getName(), "testLabel", false);

            // 3) do restore by label
            UserTransaction utx = new UserTransactionImpl(session);
            utx.begin();
            vm.restoreByLabel(node.getPath(), "testLabel", true);
            utx.commit();

            // 4) try to get the grandchild (fails if the restoring has
            // been done within a transaction)
            assertTrue(node.hasNode("child/grandchild"));
        } finally {
            session.logout();
        }
    }

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/JCR-2712">JCR-2712</a>.
     */
    public void testVersioningRollbackWithoutPrepare() throws Exception {
        Session session = getHelper().getSuperuserSession();
        try {
            if (session.getRootNode().hasNode("testNode")) {
                session.getRootNode().getNode("testNode").remove();
                session.save();
            }

            UserTransaction utx;
            for (int i = 0; i < 50; i++) {
                utx = new UserTransactionImpl(session);
                utx.begin();
                session.getRootNode().addNode("testNode").addMixin(
                        NodeType.MIX_VERSIONABLE);
                session.save();

                utx.rollback();
            }
        } finally {
            session.logout();
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
        Session otherSuperuser = getHelper().getSuperuserSession();

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
        Session otherSuperuser = getHelper().getSuperuserSession();
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
        Session otherSuperuser = getHelper().getSuperuserSession();
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
        Session otherSuperuser = getHelper().getSuperuserSession();
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
        Session otherSuperuser = getHelper().getSuperuserSession();
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
        Session otherSuperuser = getHelper().getSuperuserSession();
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
        Session otherSuperuser = getHelper().getSuperuserSession();
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

    /**
     * Checks if getReferences() reflects an added reference property that has
     * been saved but not yet committed.
     * <p>
     * Spec say:
     * <p>
     * <i>Some level 2 implementations may only return properties that have been
     * saved (in a transactional setting this includes both those properties
     * that have been saved but not yet committed, as well as properties that
     * have been committed). Other level 2 implementations may additionally
     * return properties that have been added within the current Session but are
     * not yet saved.</i>
     * <p>
     * Jackrabbit does not support the latter, but at least has to support the
     * first.
     */
    public void testGetReferencesAddedRef() throws Exception {
        // create one referenceable node
        Node target = testRootNode.addNode(nodeName1);
        target.addMixin(mixReferenceable);
        // second node, which will later reference the target node
        Node n = testRootNode.addNode(nodeName2);
        testRootNode.save();

        UserTransactionImpl tx = new UserTransactionImpl(superuser);
        tx.begin();
        try {
            // create reference
            n.setProperty(propertyName1, target);
            testRootNode.save();
            assertTrue("Node.getReferences() must reflect references that have " +
                    "been saved but not yet committed", target.getReferences().hasNext());
        } finally {
            tx.rollback();
        }
    }

    /**
     * Checks if getReferences() reflects a removed reference property that has
     * been saved but not yet committed.
     */
    public void testGetReferencesRemovedRef() throws Exception {
        // create one referenceable node
        Node target = testRootNode.addNode(nodeName1);
        target.addMixin(mixReferenceable);
        // second node, which reference the target node
        Node n = testRootNode.addNode(nodeName2);
        // create reference
        n.setProperty(propertyName1, target);
        testRootNode.save();

        UserTransactionImpl tx = new UserTransactionImpl(superuser);
        tx.begin();
        try {
            n.getProperty(propertyName1).remove();
            testRootNode.save();
            assertTrue("Node.getReferences() must reflect references that have " +
                    "been saved but not yet committed", !target.getReferences().hasNext());
        } finally {
            tx.rollback();
        }
    }

    /**
     * Checks if getReferences() reflects a modified reference property that has
     * been saved but not yet committed.
     */
    public void testGetReferencesModifiedRef() throws Exception {
        // create two referenceable node
        Node target1 = testRootNode.addNode(nodeName1);
        target1.addMixin(mixReferenceable);
        // second node, which reference the target1 node
        Node target2 = testRootNode.addNode(nodeName2);
        target2.addMixin(mixReferenceable);
        Node n = testRootNode.addNode(nodeName3);
        // create reference
        n.setProperty(propertyName1, target1);
        testRootNode.save();

        UserTransactionImpl tx = new UserTransactionImpl(superuser);
        tx.begin();
        try {
            // change reference
            n.setProperty(propertyName1, target2);
            testRootNode.save();
            assertTrue("Node.getReferences() must reflect references that have " +
                    "been saved but not yet committed", !target1.getReferences().hasNext());
            assertTrue("Node.getReferences() must reflect references that have " +
                    "been saved but not yet committed", target2.getReferences().hasNext());
        } finally {
            tx.rollback();
        }
    }

    /**
     * Checks if getReferences() reflects a modified reference property that has
     * been saved but not yet committed. The old value is a reference, while
     * the new value is not.
     */
    public void testGetReferencesModifiedRefOldValueReferenceable() throws Exception {
        // create one referenceable node
        Node target = testRootNode.addNode(nodeName1);
        target.addMixin(mixReferenceable);
        Node n = testRootNode.addNode(nodeName2);
        // create reference
        n.setProperty(propertyName1, target);
        testRootNode.save();

        UserTransactionImpl tx = new UserTransactionImpl(superuser);
        tx.begin();
        try {
            // change reference to a string value
            n.setProperty(propertyName1, "foo");
            testRootNode.save();
            assertTrue("Node.getReferences() must reflect references that have " +
                    "been saved but not yet committed", !target.getReferences().hasNext());
        } finally {
            tx.rollback();
        }
    }

    /**
     * Checks if getReferences() reflects a modified reference property that has
     * been saved but not yet committed. The new value is a reference, while
     * the old value wasn't.
     */
    public void testGetReferencesModifiedRefNewValueReferenceable() throws Exception {
        // create one referenceable node
        Node target = testRootNode.addNode(nodeName1);
        target.addMixin(mixReferenceable);
        Node n = testRootNode.addNode(nodeName2);
        // create string property
        n.setProperty(propertyName1, "foo");
        testRootNode.save();

        UserTransactionImpl tx = new UserTransactionImpl(superuser);
        tx.begin();
        try {
            // change string into a reference
            n.setProperty(propertyName1, target);
            testRootNode.save();
            assertTrue("Node.getReferences() must reflect references that have " +
                    "been saved but not yet committed", target.getReferences().hasNext());
        } finally {
            tx.rollback();
        }
    }

    //--------------------------------------------------------------< locking >

    /**
     * Test locking a node in one session. Verify that node is not locked
     * in other session until commit.
     * @throws Exception
     */
    public void testLockCommit() throws Exception {
        Session other = getHelper().getSuperuserSession();
        try {
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
        } finally {
            // logout
            other.logout();
        }
    }

    /**
     * Test locking and unlocking behavior in transaction
     * @throws Exception
     */
    public void testLockUnlockCommit() throws Exception {
        Session other = getHelper().getSuperuserSession();
        try {
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

            n.unlock();
            // commit in first session
            utx.commit();

            // verify node is locked in both sessions
            assertFalse("Node locked in session 1", n.isLocked());
            assertFalse("Node locked in session 2", nOther.isLocked());
        } finally {
            // logout
            other.logout();
        }
    }
    
    /**
     * Test locking and unlocking behavior in transaction
     * (see JCR-2356)
     * @throws Exception
     */
    public void testCreateLockUnlockInDifferentTransactions() throws Exception {
        // create new node and lock it
        UserTransaction utx = new UserTransactionImpl(superuser);
        utx.begin();

        // add node that is both lockable and referenceable, save
        Node rootNode = superuser.getRootNode(); 
        Node n = rootNode.addNode(nodeName1);
        n.addMixin(mixLockable);
        n.addMixin(mixReferenceable);
        rootNode.save();
        
        String uuid = n.getUUID();
        
        // commit
        utx.commit();
        
        // start new Transaction and try to add lock token
        utx = new UserTransactionImpl(superuser);
        utx.begin();
        
        n = superuser.getNodeByUUID(uuid);
        // lock this new node
        Lock lock = n.lock(true, false);
        
        // verify node is locked
        assertTrue("Node not locked", n.isLocked());
        
        String lockToken = lock.getLockToken();
        // assert: session must get a non-null lock token
        assertNotNull("session must get a non-null lock token", lockToken);
        // assert: session must hold lock token
        assertTrue("session must hold lock token", containsLockToken(superuser, lockToken));

        n.save();

        superuser.removeLockToken(lockToken);

        String nlt = lock.getLockToken();
        assertTrue("freshly obtained lock token must either be null or the same as the one returned earlier",
                nlt == null || nlt.equals(lockToken));

        assertFalse("session must not hold lock token", containsLockToken(superuser, lockToken));
        
        // commit
        utx.commit();

        nlt = lock.getLockToken();
        assertTrue("freshly obtained lock token must either be null or the same as the one returned earlier",
                nlt == null || nlt.equals(lockToken));

        assertFalse("session must not hold lock token", containsLockToken(superuser, lockToken));

        // start new Transaction and try to unlock
        utx = new UserTransactionImpl(superuser);
        utx.begin();

        n = superuser.getNodeByUUID(uuid);

        // verify node is locked
        assertTrue("Node not locked", n.isLocked());
        // assert: session must not hold lock token
        assertFalse("session must not hold lock token", containsLockToken(superuser, lockToken));
        
        superuser.addLockToken(lockToken);
        
        // assert: session must not hold lock token
        assertTrue("session must hold lock token", containsLockToken(superuser, lockToken));

        n.unlock();
        
        // commit
        utx.commit();
    }

    /**
     * Test locking a node in one session. Verify that node is not locked
     * in session after rollback.
     * @throws Exception
     */
    public void testLockRollback() throws Exception {
        Session other = getHelper().getSuperuserSession();
        try {
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
            assertFalse("Node not locked in session 2", nOther.hasProperty(jcrLockOwner));

            // rollback in first session
            utx.rollback();

            // verify node is not locked in either session
            assertFalse("Node not locked in session 1", n.isLocked());
            assertFalse("Node not locked in session 2", nOther.isLocked());
            assertFalse("Node not locked in session 2", nOther.hasProperty(jcrlockIsDeep));
        } finally {
            // logout
            other.logout();
        }
    }

    /**
     * Test locking a node inside a transaction that has been locked in another
     * session, which leads to a failure when committing.
     * @throws Exception
     */
    public void testLockTwice() throws Exception {
        Session other = getHelper().getSuperuserSession();
        try {
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
            assertTrue("Node locked in session 2", nOther.hasProperty(jcrLockOwner));

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
            assertTrue("Node locked in session 2", nOther.hasProperty(jcrlockIsDeep));

        } finally {
            // logout
            other.logout();
        }
    }

    /**
     * Test locking a new node inside a transaction.
     * @throws Exception
     */
    public void testLockNewNode() throws Exception {
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
        assertTrue("Node locked in transaction", n.isLocked());

        // commit
        utx.commit();

        // Check if it is locked in other session
        Session other = getHelper().getSuperuserSession();
        Node nOther = other.getNodeByUUID(n.getUUID());
        assertTrue(nOther.isLocked());

        // Check if it is also locked in other transaction
        Session other2 = getHelper().getSuperuserSession();
        // start new Transaction and try to add locktoken
        utx = new UserTransactionImpl(other2);
        utx.begin();

        Node nOther2 = other2.getNodeByUUID(n.getUUID());
        assertTrue(nOther2.isLocked());

        utx.commit();

        other.logout();
        other2.logout();

    }

    /**
     * Test add and remove lock tokens in a transaction
     * @throws Exception
     */
    public void testAddRemoveLockToken() throws Exception {
        // create new node and lock it
        UserTransaction utx = new UserTransactionImpl(superuser);
        utx.begin();

        // add node that is both lockable and referenceable, save
        Node rootNode = superuser.getRootNode();
        Node n = rootNode.addNode(nodeName1);
        n.addMixin(mixLockable);
        n.addMixin(mixReferenceable);
        rootNode.save();

        String uuid = n.getUUID();

        // lock this new node
        Lock lock = n.lock(true, false);
        String lockToken = lock.getLockToken();

        // assert: session must get a non-null lock token
        assertNotNull("session must get a non-null lock token", lockToken);

        // assert: session must hold lock token
        assertTrue("session must hold lock token", containsLockToken(superuser, lockToken));

        superuser.removeLockToken(lockToken);

        String nlt = lock.getLockToken();
        assertTrue("freshly obtained lock token must either be null or the same as the one returned earlier",
                nlt == null || nlt.equals(lockToken));

        // commit
        utx.commit();

        // refresh Lock Info
        lock = n.getLock();

        nlt = lock.getLockToken();
        assertTrue("freshly obtained lock token must either be null or the same as the one returned earlier",
                nlt == null || nlt.equals(lockToken));

        Session other = getHelper().getSuperuserSession();
        try {
            // start new Transaction and try to add lock token
            utx = new UserTransactionImpl(other);
            utx.begin();

            Node otherNode = other.getNodeByUUID(uuid);
            assertTrue("Node not locked", otherNode.isLocked());
            try {
                otherNode.setProperty(propertyName1, "foo");
                fail("Lock exception should be thrown");
            } catch (LockException e) {
                // expected
            }

            // add lock token
            other.addLockToken(lockToken);

            // refresh Lock Info
            lock = otherNode.getLock();

            // assert: session must hold lock token
            assertTrue("session must hold lock token", containsLockToken(other, lock.getLockToken()));

            otherNode.unlock();

            assertFalse("Node is locked", otherNode.isLocked());

            otherNode.setProperty(propertyName1, "foo");
            other.save();
            utx.commit();
        } finally {
            other.logout();
        }
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

    /**
     * Test correct behaviour of lock related properties within transaction.
     *
     * @throws Exception
     */
    public void testLockProperties() throws Exception {
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixLockable);
        n.addMixin(mixReferenceable);
        testRootNode.save();

        // get user transaction object, start and lock node
        UserTransaction utx = new UserTransactionImpl(superuser);
        utx.begin();
        Lock lock = n.lock(false, true);

        // verify that the lock properties have been created and are neither
        // NEW nor MODIFIED.
        assertTrue(n.hasProperty(jcrLockOwner));
        Property lockOwner = n.getProperty(jcrLockOwner);
        assertFalse(lockOwner.isNew());
        assertFalse(lockOwner.isModified());

        assertTrue(n.hasProperty(jcrlockIsDeep));
        Property lockIsDeep = n.getProperty(jcrlockIsDeep);
        assertFalse(lockIsDeep.isNew());
        assertFalse(lockIsDeep.isModified());

        // rollback
        utx.rollback();

        // verify that the lock properties have been removed again.
        assertFalse(n.hasProperty(jcrLockOwner));
        try {
            lockOwner.getPath();
            fail("jcr:lockIsDeep property must have been invalidated.");
        } catch (InvalidItemStateException e) {
            // success
        }
        assertFalse(n.hasProperty(jcrlockIsDeep));
        try {
            lockIsDeep.getPath();
            fail("jcr:lockIsDeep property must have been invalidated.");
        } catch (InvalidItemStateException e) {
            // success
        }
    }

    /**
     * Test correct behaviour of lock related properties within transaction.
     *
     * @throws Exception
     */
    public void testLockProperties2() throws Exception {
        // add node that is both lockable and referenceable, save
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixLockable);
        n.addMixin(mixReferenceable);
        testRootNode.save();

        Lock lock = n.lock(false, true);
        try {
            // get user transaction object, start
            UserTransaction utx = new UserTransactionImpl(superuser);
            utx.begin();

            // verify that the lock properties are present
            assertTrue(n.hasProperty(jcrLockOwner));
            assertTrue(n.hasProperty(jcrlockIsDeep));

            // unlock
            n.unlock();

            // verify that the lock properties have been removed.
            assertFalse(n.hasProperty(jcrLockOwner));
            assertFalse(n.hasProperty(jcrlockIsDeep));

            // rollback
            utx.rollback();

            // verify lock is live again -> properties must be present
            assertTrue(n.hasProperty(jcrLockOwner));
            assertTrue(n.hasProperty(jcrlockIsDeep));
        } finally {
            n.unlock();
        }
    }

    /**
     * Test visibility of lock properties by another session.
     *
     * @throws Exception
     */
    public void testLockProperties3() throws Exception {
        // add node that is both lockable and referenceable, save
        Node n = testRootNode.addNode(nodeName1);
        n.addMixin(mixLockable);
        n.addMixin(mixReferenceable);
        testRootNode.save();

        Lock lock = n.lock(false, true);

        // get user transaction object, start
        UserTransaction utx = new UserTransactionImpl(superuser);
        utx.begin();

        // unlock
        n.unlock();

        Node n2 = (Node) otherSuperuser.getItem(n.getPath());
        assertTrue(n2.isLocked());
        assertTrue(n2.hasProperty(jcrLockOwner));
        assertTrue(n2.hasProperty(jcrlockIsDeep));
        Lock lock2 = n2.getLock();

        // complete transaction
        utx.commit();

        // unlock must now be visible to other session
        n2.refresh(false);
        assertFalse(lock2.isLive());
        assertFalse(n2.isLocked());
        assertFalse(n2.hasProperty(jcrLockOwner));
        assertFalse(n2.hasProperty(jcrlockIsDeep));
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
        Session s2 = getHelper().getSuperuserSession(workspaceName);

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

    /**
     * Tests two different Threads for prepare and commit in a Transaction
     */
    public void testDistributedThreadAccess() throws Exception {
        // get user transaction object
        UserTransaction utx = new UserTransactionImpl(superuser, true);
        //utx.setTransactionTimeout(50);
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

        // commit
        utx.commit();

        // assertion: node exists in this session
        try {
            superuser.getNodeByUUID(n.getUUID());
        } catch (ItemNotFoundException e) {
            fail("Committed node not visible in this session");
        }
    }
    
    /**
     * Tests two different Sessions in one Transaction
     * (see JCR-769)
     */
    public void testTwoSessionsInOneTransaction() throws Exception {
        Session otherSuperuser = getHelper().getSuperuserSession();
        
        // get user transaction object
        UserTransactionImpl utx = new UserTransactionImpl(superuser, true);
        utx.enlistXAResource(otherSuperuser);
        
        // start transaction
        utx.begin();

        Node rootNode = superuser.getRootNode();
        // add node and save
        Node n = rootNode.addNode(nodeName1, testNodeType);
        n.addMixin(mixReferenceable);
        rootNode.save();

        // assertion: node exists in this session
        try {
            superuser.getNodeByUUID(n.getUUID());
        } catch (ItemNotFoundException e) {
            fail("New node not visible after save()");
        }

        // assertion: node does exist in other session
        try {
            otherSuperuser.getNodeByUUID(n.getUUID());
            fail("Uncommitted node visible for other session");
        } catch (ItemNotFoundException e) {
            /* expected */
        }

        // add node with other session and save
        rootNode = otherSuperuser.getRootNode();
        Node n1 = rootNode.addNode(nodeName2, testNodeType);
        n1.addMixin(mixReferenceable);
        rootNode.save();

        // assertion: node exists in this session
        try {
            otherSuperuser.getNodeByUUID(n1.getUUID());
        } catch (ItemNotFoundException e) {
            fail("New node not visible after save()");
        }

        // assertion: node does exist in other session
        try {
            superuser.getNodeByUUID(n1.getUUID());
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
            fail("Committed node not visible in the other session");
        }

        // assertion: node1 exists in this session
        try {
            superuser.getNodeByUUID(n1.getUUID());
        } catch (ItemNotFoundException e) {
            fail("Committed node not visible in this session");
        }

        // assertion: node1 also exists in other session
        try {
            otherSuperuser.getNodeByUUID(n1.getUUID());
        } catch (ItemNotFoundException e) {
            fail("Committed node not visible in this session");
        }

        // logout
        superuser.logout();
        otherSuperuser.logout();
    }
    
    /**
     * Test add lock token and remove node in in a transaction
     * (see JCR-2332) 
     * @throws Exception
     */
    public void testAddLockTokenRemoveNode() throws Exception {
        // create new node and lock it
        UserTransaction utx = new UserTransactionImpl(superuser);
        utx.begin();

        // add node that is both lockable and referenceable, save
        Node rootNode = superuser.getRootNode(); 
        Node n = rootNode.addNode(nodeName1);
        n.addMixin(mixLockable);
        n.addMixin(mixReferenceable);
        rootNode.save();

        String uuid = n.getUUID();
        
        // lock this new node
        Lock lock = n.lock(true, false);
        String lockToken = lock.getLockToken();
        
        // assert: session must get a non-null lock token
        assertNotNull("session must get a non-null lock token", lockToken);

        // assert: session must hold lock token
        assertTrue("session must hold lock token", containsLockToken(superuser, lockToken));

        superuser.removeLockToken(lockToken);

        String nlt = lock.getLockToken();
        assertTrue("freshly obtained lock token must either be null or the same as the one returned earlier",
                nlt == null || nlt.equals(lockToken));

        // commit
        utx.commit();
        
        // refresh Lock Info
        lock = n.getLock();

        nlt = lock.getLockToken();
        assertTrue("freshly obtained lock token must either be null or the same as the one returned earlier",
                nlt == null || nlt.equals(lockToken));

        Session other = getHelper().getSuperuserSession();
        // start new Transaction and try to add lock token unlock the node and then remove it
        utx = new UserTransactionImpl(other);
        utx.begin();
        
        Node otherNode = other.getNodeByUUID(uuid); 
        assertTrue("Node not locked", otherNode.isLocked());
        // add lock token
        other.addLockToken(lockToken);
      
        // refresh Lock Info
        lock = otherNode.getLock();

        // assert: session must hold lock token
        assertTrue("session must hold lock token", containsLockToken(other, lock.getLockToken()));        
        
        otherNode.unlock();
        
        assertFalse("Node is locked", otherNode.isLocked());
        
        otherNode.remove();
        other.save();
        utx.commit();
    }

    /**
     * Tests if it is possible to add-lock a node and unlock-remove it with
     * a shared session in different transactions
     * (see JCR-2341)  
     * @throws Exception
     */
    public void testAddLockTokenRemoveNode2() throws Exception {
        // create new node and lock it
        UserTransaction utx = new UserTransactionImpl(superuser);
        utx.begin();

        // add node that is both lockable and referenceable, save
        Node rootNode = superuser.getRootNode(); 
        Node n = rootNode.addNode(nodeName1);
        n.addMixin(mixLockable);
        n.addMixin(mixReferenceable);
        rootNode.save();

        String uuid = n.getUUID();
        
        // lock this new node
        Lock lock = n.lock(true, false);
        String lockToken = lock.getLockToken();
        
        // commit
        utx.commit();
        
        
        // refresh Lock Info
        lock = n.getLock();

        // start new Transaction and try to add lock token unlock the node and then remove it
        utx = new UserTransactionImpl(superuser);
        utx.begin();
        
        Node otherNode = superuser.getNodeByUUID(uuid); 
        assertTrue("Node not locked", otherNode.isLocked());
        // add lock token
        superuser.addLockToken(lockToken);
      
        // refresh Lock Info
        lock = otherNode.getLock();

        // assert: session must hold lock token
        assertTrue("session must hold lock token", containsLockToken(superuser, lockToken));        
        
        otherNode.unlock();
        
        assertFalse("Node is locked", otherNode.isLocked());
        
        otherNode.remove();
        superuser.save();
        utx.commit();
    }
    
    /**
     * Test setting the same property multiple times. Exposes an issue where
     * the same property instance got reused in subsequent transactions
     * (see JCR-1554).
     *
     * @throws Exception if an error occurs
     */
    public void testSetProperty() throws Exception {
        final String testNodePath = testPath + "/" + Math.random();

        Session session = getHelper().getSuperuserSession();
        try {
            // Add node
            doTransactional(new Operation() {
                public void invoke(Session session) throws Exception {
                    session.getRootNode().addNode(testNodePath);
                    session.save();
                }
            }, session);

            for (int i = 1; i <= 3; i++) {
                // Set property "name" to value "value"
                doTransactional(new Operation() {
                    public void invoke(Session session) throws Exception {
                        Node n = (Node) session.getItem("/" + testNodePath);
                        n.setProperty("name", "value");
                        session.save();
                    }
                }, session);
            }
        } finally {
            session.logout();
        }
    }

    /**
     * Test deleting a subnode after creation. Exposes an issue where
     * the same node instance got reused in subsequent transactions
     * (see JCR-1554).
     *
     * @throws Exception if an error occurs
     */
    public void testDeleteNode() throws Exception {
        final String testNodePath = testPath + "/" + Math.random();

        Session session = getHelper().getSuperuserSession();
        try {
            for (int i = 1; i <= 3; i++) {
                // Add parent node
                doTransactional(new Operation() {
                    public void invoke(Session session) throws Exception {
                        session.getRootNode().addNode(testNodePath);
                        session.save();
                    }
                }, session);

                // Add child node
                doTransactional(new Operation() {
                    public void invoke(Session session) throws Exception {
                        session.getRootNode().addNode(testNodePath + "/subnode");
                        session.save();
                    }
                }, session);

                // Remove parent node
                doTransactional(new Operation() {
                    public void invoke(Session session) throws Exception {
                        session.getRootNode().getNode(testNodePath).remove();
                        session.save();
                    }
                }, session);
            }
        } finally {
            session.logout();
        }
    }

    /**
     * Operation to invoke on a session scope.
     */
    interface Operation {

        /**
         * Invoke the operation.
         * @param session session to use inside operation
         * @throws Exception if an error occurs
         */
        void invoke(Session session) throws Exception;
    }

    /**
     * Wrap a session-scoped operation with a transaction.
     *
     * @param op operation to invoke
     * @param session session to use for the transaction
     * @throws Exception if an error occurs
     */
    private void doTransactional(Operation op, Session session) throws Exception {
        UserTransaction utx = new UserTransactionImpl(session);
        utx.begin();

        op.invoke(session);

        utx.commit();
    }

    /**
     * Return a flag indicating whether the indicated session contains
     * a specific lock token
     */
    private boolean containsLockToken(Session session, String lockToken) {
        String[] lt = session.getLockTokens();
        for (int i = 0; i < lt.length; i++) {
            if (lt[i].equals(lockToken)) {
                return true;
            }
        }
        return false;
    }
}
