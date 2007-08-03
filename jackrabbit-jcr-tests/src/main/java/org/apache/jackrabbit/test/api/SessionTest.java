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
package org.apache.jackrabbit.test.api;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Value;
import javax.jcr.Repository;
import javax.jcr.lock.LockException;

/**
 * <code>SessionTest</code> contains all test cases for the
 * <code>javax.jcr.Session</code> class that are level 2 (modifing repository
 * content).
 *
 * @test
 * @sources SessionTest.java
 * @executeClass org.apache.jackrabbit.test.api.SessionTest
 * @keywords level2
 */
public class SessionTest extends AbstractJCRTest {

    /**
     * Tries to move a node using <code>{@link javax.jcr.Session#move(String src, String dest)}
     * </code> to a location where a node already exists with
     * same name.<br/> <br/> Prerequisites:
     * <ul>
     * <li><code>javax.jcr.tck.SessionTest.testMoveItemExistsException.nodetype2</code>
     * must contain name of a nodetype that does not allow same name sibling
     * child nodes.</li>
     * <li><code>javax.jcr.tck.SessionTest.testMoveItemExistsException.nodetype3</code>
     * must contain name of a valid nodetype that can be added as a child of
     * <code>nodetype2</code></li>
     * </ul> This should throw an <code>{@link javax.jcr.ItemExistsException}</code>.
     */
    public void testMoveItemExistsException() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create parent node
        Node srcParentNode = defaultRootNode.addNode(nodeName1, testNodeType);
        // create node to move
        Node moveNode = srcParentNode.addNode(nodeName2, getProperty("nodetype3"));

        // create a second node that will serve as new parent, must use a nodetype that does not allow
        // same name siblings
        Node destParentNode = defaultRootNode.addNode(nodeName3, getProperty("nodetype2"));
        // add a valid child
        Node destNode = destParentNode.addNode(nodeName2, getProperty("nodetype3"));

        // save the new nodes
        superuser.save();

        try {
            // move the node
            superuser.move(moveNode.getPath(), destNode.getPath());
            fail("Moving a node using Session.move() to a location where a node with same name already exists must throw ItemExistsException");
        } catch (ItemExistsException e) {
            // ok, works as expected
        }
    }

    /**
     * Calls <code>{@link javax.jcr.Session#move(String src, String dest)}</code>
     * with invalid destination path.<br/> <br/> Should throw
     * <code{@link javax.jcr.PathNotFoundException}</code>.
     */
    public void testMovePathNotFoundExceptionDestInvalid() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create parent node
        Node srcParentNode = defaultRootNode.addNode(nodeName1, testNodeType);
        // create node to move
        Node moveNode = srcParentNode.addNode(nodeName2, testNodeType);

        // save the new nodes
        superuser.save();

        // move the node
        try {
            superuser.move(moveNode.getPath(), defaultRootNode.getPath() + "/" + nodeName2 + "/" + nodeName1);
            fail("Invalid destination path during Session.move() must throw PathNotFoundException");
        } catch (PathNotFoundException e) {
            // ok, works as expected
        }
    }

    /**
     * Calls <code>{@link javax.jcr.Session#move(String src, String dest)} with
     * invalid source path.<br/> <br/> Should throw an <code>{@link
     * javax.jcr.PathNotFoundException}.
     */
    public void testMovePathNotFoundExceptionSrcInvalid() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create a node that will serve as new parent
        Node destParentNode = defaultRootNode.addNode(nodeName3, testNodeType);

        // save the new nodes
        superuser.save();

        // move the node
        try {
            superuser.move(defaultRootNode.getPath() + "/" + nodeName1, destParentNode.getPath() + "/" + nodeName2);
            fail("Invalid source path during Session.move() must throw PathNotFoundException");
        } catch (PathNotFoundException e) {
            // ok. works as expected
        }
    }

    /**
     * Calls <code>{@link javax.jcr.Session#move(String src, String dest)}
     * </code> with a destination path that has an index postfixed.<br/>
     * <br/> This should throw an <code>{@link javax.jcr.RepositoryException}</code>.
     */
    public void testMoveRepositoryException() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create parent node
        Node srcParentNode = defaultRootNode.addNode(nodeName1, testNodeType);
        // create node to be moved
        Node moveNode = srcParentNode.addNode(nodeName2, testNodeType);

        // create a node that will serve as new parent
        Node destParentNode = defaultRootNode.addNode(nodeName3, testNodeType);

        // save the new nodes
        superuser.save();

        // move the node
        try {
            superuser.move(moveNode.getPath(), destParentNode.getPath() + "/" + nodeName2 + "[1]");
            fail("If destination path of Session.move() contains an index as postfix it must throw RepositoryException");
        } catch (RepositoryException e) {
            // ok works as expected
        }
    }

    /**
     * Moves a node using <code>{@link javax.jcr.Session#move(String src, String dest)}
     * </code>, afterwards it tries to only save the old parent node.<br>
     * <br> This should throw <code>{@link javax.jcr.nodetype.ConstraintViolationException}</code>.
     * <br/><br/>Prerequisites: <ul> <li><code>javax.jcr.tck.nodetype</code>
     * must accept children of same nodetype</li> </ul>
     */
    public void testMoveConstraintViolationExceptionSrc() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create parent node
        Node srcParentNode = defaultRootNode.addNode(nodeName1, testNodeType);
        // create node to be moved
        Node moveNode = srcParentNode.addNode(nodeName2, testNodeType);

        // create a node that will serve as new parent
        Node destParentNode = defaultRootNode.addNode(nodeName3, testNodeType);

        // save the new nodes
        superuser.save();

        // move the node
        superuser.move(moveNode.getPath(), destParentNode.getPath() + "/" + nodeName2);

        // save only old parent node
        try {
            srcParentNode.save();
            fail("Saving only the source parent node after a Session.move() operation must throw ConstraintViolationException");
        } catch (ConstraintViolationException e) {
            // ok both work as expected
        }
    }

    /**
     * Moves a node using <code>{@link javax.jcr.Session#move(String src, String dest)}
     * </code>, afterwards it tries to only save the destination parent
     * node.<br> <br> This should throw <code>{@link javax.jcr.nodetype.ConstraintViolationException}</code>.
     * <br/><br/>Prerequisites: <ul> <li><code>javax.jcr.tck.nodetype</code>
     * must accept children of same nodetype</li> </ul>
     */
    public void testMoveConstraintViolationExceptionDest() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create parent node
        Node srcParentNode = defaultRootNode.addNode(nodeName1, testNodeType);
        // create node to be moved
        Node moveNode = srcParentNode.addNode(nodeName2, testNodeType);

        // create a node that will serve as new parent
        Node destParentNode = defaultRootNode.addNode(nodeName3, testNodeType);

        // save the new nodes
        superuser.save();

        // move the node
        superuser.move(moveNode.getPath(), destParentNode.getPath() + "/" + nodeName2);

        // save only moved node
        try {
            destParentNode.save();
            fail("Saving only moved node after a Session.move() operation should throw ContstraintViolationException");
        } catch (ConstraintViolationException e) {
            // ok try to save the source
        }
    }

    /**
     * Calls <code>{@link javax.jcr.Session#move(String src, String dest)} where
     * the parent node of src is locked.<br/> <br/> Should throw a <code>{@link
     * LockException} immediately or on save.
     */
    public void testMoveLockException()
        throws NotExecutableException, RepositoryException {

        Session session = superuser;

        if (!isSupported(Repository.OPTION_LOCKING_SUPPORTED)) {
            throw new NotExecutableException("Locking is not supported.");
        }

        // create a node that is lockable
        Node lockableNode = testRootNode.addNode(nodeName1, testNodeType);
        // or try to make it lockable if it is not
        if (!lockableNode.isNodeType(mixLockable)) {
            if (lockableNode.canAddMixin(mixLockable)) {
                lockableNode.addMixin(mixLockable);
            } else {
                throw new NotExecutableException("Node " + nodeName1 + " is not lockable and does not " +
                        "allow to add mix:lockable");
            }
        }

        // add a sub node (the one that is tried to move later on)
        Node srcNode = lockableNode.addNode(nodeName1, testNodeType);

        testRootNode.save();

        // remove first slash of path to get rel path to root
        String pathRelToRoot = lockableNode.getPath().substring(1);

        // access node through another session to lock it
        Session session2 = helper.getSuperuserSession();
        try {
            Node node2 = session2.getRootNode().getNode(pathRelToRoot);
            node2.lock(true, true);

            try {
                String destPath = testRoot + "/" + nodeName2;
                session.move(srcNode.getPath(), destPath);
                testRootNode.save();
                fail("A LockException is thrown either immediately or on save  if a lock prevents the move.");
            } catch (LockException e){
                // success
            }

        } finally {
            session2.logout();
        }
    }

    /**
     * Checks if <code>{@link javax.jcr.Session#move(String src, String dest)}
     * </code> works properly. To verify if node has been moved properly
     * it uses a second session to retrieve the moved node.
     * <br/><br/>Prerequisites: <ul> <li><code>javax.jcr.tck.nodetype</code>
     * must accept children of same nodetype</li> </ul>
     */
    public void testMoveNode() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create parent node
        Node srcParentNode = defaultRootNode.addNode(nodeName1, testNodeType);
        // create node to be moved
        Node moveNode = srcParentNode.addNode(nodeName2, testNodeType);

        // create a node that will serve as new parent
        Node destParentNode = defaultRootNode.addNode(nodeName3, testNodeType);

        // save the new nodes
        superuser.save();

        //move the nodes
        superuser.move(moveNode.getPath(), destParentNode.getPath() + "/" + nodeName2);

        superuser.save();

        // get moved tree root node with session 2
        Session testSession = helper.getReadWriteSession();
        try {
            testSession.getItem(destParentNode.getPath() + "/" + nodeName2);
            // node found
        } finally {
            testSession.logout();
        }
    }

    /**
     * Checks if a newly created node gets properly saved using <code{@link
     * javax.jcr.Session#save()}</code>.<br/> <br/> It creates a new node, saves
     * it using <code>session.save()</code> then uses a different session to
     * verify if the node has been properly saved.
     */
    public void testSaveNewNode() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create a node
        Node newNode = defaultRootNode.addNode(nodeName1, testNodeType);

        // save changes
        superuser.save();

        // use a different session to verify if the node is there
        Session s = helper.getReadOnlySession();
        try {
            s.getItem(newNode.getPath());
            // throws PathNotFoundException if item was not saved
        } finally {
            s.logout();
        }
    }

    /**
     * Checks if a modified node gets properly saved using <code{@link
     * javax.jcr.Session#save()}</code>.<br/> <br/> It creates a new node, saves
     * it using <code>session.save()</code>, modifies the node by adding a child
     * node, saves again and finally verifies with a different session if
     * changes have been stored properly.<br/> <br/> Prerequisites: <ul>
     * <li><code>javax.jcr.tck.nodetype</code> must accept children of same
     * nodetype</li> </ul>
     */
    public void testSaveModifiedNode() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create a node
        Node newNode = defaultRootNode.addNode(nodeName1, testNodeType);

        // save new node
        superuser.save();

        // and add a child node
        newNode.addNode(nodeName2, testNodeType);

        // save the changes
        superuser.save();

        // check if the child node was created properly

        // get a reference with a second session to the modified node
        Session s = helper.getReadOnlySession();
        try {
            Node newNodeSession2 = (Node) s.getItem(newNode.getPath());
            // check if child is there
            assertTrue("Modifications on  a node are not save after Session.save()", newNodeSession2.hasNode(nodeName2));
        } finally {
            s.logout();
        }
    }

    /**
     * Tries to create and save a node using {@link javax.jcr.Session#save()}
     * with an mandatory property that is not set on saving time.<br/> <br/>
     * Prerequisites: <ul> <li><code>javax.jcr.tck.SessionTest.testSaveContstraintViolationException.nodetype2</code>
     * must reference a nodetype that has one at least one property that is
     * mandatory but not autocreated</li> </ul>
     */
    public void testSaveContstraintViolationException() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create a node with at least one mandatory, not autocreated property
        defaultRootNode.addNode(nodeName1, getProperty("nodetype2"));

        // save changes
        try {
            superuser.save();
            fail("Trying to use Session.save() with a node that has a mandatory property not set, should throw ConstraintViolationException");
        } catch (ConstraintViolationException e) {
            // ok
        }
    }

    /**
     * Tries to save a node using {@link javax.jcr.Session#save()} that was
     * already deleted by an other session.<br/> <br/> Procedure: <ul>
     * <li>Creates a new node with session 1, saves it, adds a child node.</li>
     * <li>Access new node with session 2,deletes the node, saves it.</li>
     * <li>session 1 tries to save modifications .</li> </ul> This should throw
     * an {@link javax.jcr.InvalidItemStateException}. <br/><br/>Prerequisites:
     * <ul> <li><code>javax.jcr.tck.nodetype</code> must accept children of same
     * nodetype</li> </ul>
     */
    public void testSaveInvalidStateException() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create a node
        Node nodeSession1 = defaultRootNode.addNode(nodeName1, testNodeType);

        // save new node
        superuser.save();

        // make a modification
        nodeSession1.addNode(nodeName2, testNodeType);

        // get the new node with a different session
        Session testSession = helper.getReadWriteSession();
        try {
            Node nodeSession2 = (Node) testSession.getItem(nodeSession1.getPath());

            // delete the node with the new session
            nodeSession2.remove();

            // make node removal persistent
            testSession.save();

            // save changes made wit superuser session
            try {
                superuser.save();
                fail("Saving a modified Node using Session.save() already deleted by an other session should throw InvalidItemStateException");
            } catch (InvalidItemStateException e) {
                // ok, works as expected
            }
        } finally {
            testSession.logout();
        }
    }

    /**
     * Checks if {@link javax.jcr.Session#refresh(boolean refresh)} works
     * properly with <code>refresh</code> set to <code>false</code>.<br/> <br/>
     * Procedure: <ul> <li>Creates two nodes with session 1</li> <li>Modifies
     * node 1 with session 1 by adding a child node</li> <li>Get node 2 with
     * session 2</li> <li>Modifies node 2 with session 2 by adding a child
     * node</li> <li>saves session 2 changes using {@link
     * javax.jcr.Session#save()}</li> <li>calls <code>Session.refresh(false)</code>
     * on session 1</li> </ul> Session 1 changes should be cleared and session 2
     * changes should now be visible to session 1. <br/><br/>Prerequisites: <ul>
     * <li><code>javax.jcr.tck.nodetype</code> must accept children of same
     * nodetype</li> </ul>
     */
    public void testRefreshBooleanFalse() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create a node
        Node testNode1Session1 = defaultRootNode.addNode(nodeName1, testNodeType);
        // create a second node
        Node testNode2Session1 = defaultRootNode.addNode(nodeName2, testNodeType);

        // save the new nodes
        superuser.save();

        // add child node to test node 1 using session 1
        testNode1Session1.addNode(nodeName2, testNodeType);

        // get session 2
        Session session2 = helper.getReadWriteSession();

        try {
            // get the second node
            Node testNode2Session2 = (Node) session2.getItem(testNode2Session1.getPath());

            // adds a child node
            testNode2Session2.addNode(nodeName3, testNodeType);

            // save the changes
            session2.save();

            // call refresh on session 1
            superuser.refresh(false);

            // check if session 1 flag has been cleared
            assertFalse("Session should have no pending changes recorded after Session.refresh(false)!", superuser.hasPendingChanges());

            // check if added child node for node 1 by session 1 has been removed
            assertFalse("Node Modifications have not been flushed after session.refresh(false)", testNode1Session1.hasNodes());

            // check if added child node for node 2 by session 2 has become visible in session 1
            assertTrue("Node modified by a different session has not been updated after Session.refresh(false)", testNode2Session1.hasNodes());
        } finally {
            session2.logout();
        }
    }

    /**
     * Checks if {@link javax.jcr.Session#refresh(boolean refresh)} works
     * properly with <code>refresh</code> set to <code>true</code>.<br/> <br/>
     * Procedure: <ul> <li>Creates two nodes with session 1</li> <li>Modifies
     * node 1 with session 1 by adding a child node</li> <li>Get node 2 with
     * session 2</li> <li>Modifies node 2 with session 2 by adding a child
     * node</li> <li>saves session 2 changes using {@link
     * javax.jcr.Session#save()}</li> <li>calls <code>Session.refresh(true)</code>
     * on session 1</li> </ul> Session 1 changes and session 2 changes now be
     * visible to session 1. <br/><br/>Prerequisites: <ul>
     * <li><code>javax.jcr.tck.nodetype</code> must accept children of same
     * nodetype</li> </ul>
     */
    public void testRefreshBooleanTrue() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create a node
        Node testNode1Session1 = defaultRootNode.addNode(nodeName1, testNodeType);
        // create a second node
        Node testNode2Session1 = defaultRootNode.addNode(nodeName2, testNodeType);

        // save the new nodes
        superuser.save();

        // add child node to test node 1 using session 1
        testNode1Session1.addNode(nodeName2, testNodeType);

        // get session 2
        Session session2 = helper.getReadWriteSession();

        try {
            // get the second node
            Node testNode2Session2 = (Node) session2.getItem(testNode2Session1.getPath());

            // adds a child node
            testNode2Session2.addNode(nodeName3, testNodeType);

            // save the changes
            session2.save();

            // call refresh on session 1
            superuser.refresh(true);

            // check if session 1 flag has been cleared
            assertTrue("Session should still have pending changes recorded after Session.refresh(true)!", superuser.hasPendingChanges());

            // check if added child node for node 1 by session 1 is still there
            assertTrue("Node Modifications are lost after session.refresh(true)", testNode1Session1.hasNodes());

            // check if added child node for node 2 by session 2 has become visible in session 1
            assertTrue("Node modified by a different session has not been updated after Session.refresh(true)", testNode2Session1.hasNodes());
        } finally {
            session2.logout();
        }
    }

    /**
     * Checks if {@link javax.jcr.Session#hasPendingChanges()}  works
     * properly.<br/> <br/> Procedure:<br/> <ul> <li>Gets a session, checks
     * inital flag setting</li> <li>Adds a node, checks flag</li> <li>Saves on
     * session, checks flag</li> <li>Adds a property, checks flag</li> <li>Saves
     * on session, checks flag</li> <li>Adds a child node, checks flag</li>
     * <li>Saves on session, checks flag</li> <li>Removes child node, checks
     * flag</li> <li>Saves on session, checks flag</li> <li>Removes property,
     * checks flag</li> <li>Saves on session, checks flag</li> </ul>
     * Prerequisites: <ul> <li><code>javax.jcr.tck.nodetype</code> must accept
     * children of same nodetype</li> <li><code>javax.jcr.tck.propertyname1</code>
     * must be the name of a String property that can be added to a node of type
     * set in <code>javax.jcr.tck.nodetype</code> </ul>
     */
    public void testHasPendingChanges() throws RepositoryException {

        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // initial check if flag is set correctly
        assertFalse("Session should have no pending changes recorded!", superuser.hasPendingChanges());

        // test with adding a node
        Node testNode1 = defaultRootNode.addNode(nodeName1, testNodeType);
        assertTrue("Session should have pending changes recorded after new node was added!", superuser.hasPendingChanges());

        // save the node
        superuser.save();
        // pending changes should have been cleared
        assertFalse("Session should have no pending changes recorded after new node was added and saved!", superuser.hasPendingChanges());

        // adds a property
        testNode1.setProperty(propertyName1, "test");
        assertTrue("Session should have pending changes recorded after a property was added!", superuser.hasPendingChanges());

        // save the new prop
        superuser.save();
        // pending changes should have been cleared
        assertFalse("Session should have no pending changes recorded after added property hase been saved!", superuser.hasPendingChanges());

        // add child node
        Node testChildNode = testNode1.addNode(nodeName1, testNodeType);

        assertTrue("Session should have pending changes recorded after child node has been added!", superuser.hasPendingChanges());

        // save the new child nodes
        superuser.save();

        // pending changes should have been cleared
        assertFalse("Session should have no pending changes recorded after new child node has been added and saved!", superuser.hasPendingChanges());


        // remove the child node
        testChildNode.remove();
        assertTrue("Session should have pending changes recorded after child node has been removed", superuser.hasPendingChanges());

        // save the change
        superuser.save();

        // pending changes should have been cleared
        assertFalse("Session should have no pending changes recorded after child node has been removed and saved!", superuser.hasPendingChanges());

        // remove the property
        testNode1.setProperty(propertyName1, (Value) null);
        assertTrue("Session should have pending changes recorded after property has been removed", superuser.hasPendingChanges());

        // save the change
        superuser.save();

        // pending changes should have been cleared
        assertFalse("Session should have no pending changes recorded after property has been removed and saved!", superuser.hasPendingChanges());

    }
}
