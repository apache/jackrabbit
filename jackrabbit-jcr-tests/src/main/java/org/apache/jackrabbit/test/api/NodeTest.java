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
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.Value;
import javax.jcr.PropertyType;
import javax.jcr.lock.LockException;

/**
 * <code>NodeTest</code> contains all test cases for the
 * <code>javax.jcr.Node</code> that are related to writing, modifing or deleting
 * nodes (level 2 of the specification).
 *
 * @test
 * @sources NodeTest.java
 * @executeClass org.apache.jackrabbit.test.api.NodeTest
 * @keywords level2
 */
public class NodeTest extends AbstractJCRTest {

    private Session superuserW2;

    /**
     * to be able to test the update(String) and getCorrespondingNodePath(String)
     * methods we need an addtional workspace
     */
    public void setUp() throws Exception {
        super.setUp();

        // login to second workspace
        superuserW2 = helper.getSuperuserSession(workspaceName);
    }

    /**
     * remove all nodes in second workspace and log out
     */
    public void tearDown() throws Exception {
        try {
            cleanUpTestRoot(superuserW2);
        } catch (RepositoryException e) {
            log.println("Exception in tearDown: " + e.toString());
        } finally {
            // log out
            superuserW2.logout();
            superuserW2 = null;
        }

        super.tearDown();
    }


    /**
     * Calls {@link javax.jcr.Node#getCorrespondingNodePath(String )} with a non
     * existing workspace. <br/><br/> This should throw an {@link
     * javax.jcr.NoSuchWorkspaceException }.
     */
    public void testGetCorrespondingNodePathNoSuchWorkspaceException() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create testNode in default workspace
        Node defaultTestNode = defaultRootNode.addNode(nodeName1, testNodeType);

        // save changes
        superuser.save();

        try {
            defaultTestNode.getCorrespondingNodePath(getNonExistingWorkspaceName(superuser));
            fail("Calling Node.getCorrespondingNodePath(workspace) with invalid workspace should throw NoSuchWorkspaceException");
        } catch (NoSuchWorkspaceException e) {
            // ok, works as expected
        }
    }


    /**
     * Calls {@link javax.jcr.Node#getCorrespondingNodePath(String)} on  a node
     * that has no corresponding node in second workspace
     */
    public void testGetCorrespondingNodePathItemNotFoundException() throws RepositoryException, NotExecutableException {
      
        // make sure the repository supports multiple workspaces
        super.ensureMultipleWorkspacesSupported();
      
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create testNode in default workspace
        Node defaultTestNode = defaultRootNode.addNode(nodeName1, testNodeType);

        // save changes
        superuser.save();

        try {
            // call the update method on test node in default workspace
            defaultTestNode.getCorrespondingNodePath(workspaceName);
            fail("Calling Node.getCorrespondingNodePath() on node that has no correspondend node should throw ItemNotFoundException");
        } catch (ItemNotFoundException e) {
            // ok, works as expected
        }
    }

    /**
     * Creates a node with same path in both workspaces to check if {@link
     * javax.jcr.Node#getCorrespondingNodePath(String)} works properly.
     */
    public void testGetCorrespondingNodePath() throws RepositoryException, NotExecutableException {
      
        // make sure the repository supports multiple workspaces
        super.ensureMultipleWorkspacesSupported();

        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create test node in default workspace
        Node defaultTestNode = defaultRootNode.addNode(nodeName1, testNodeType);

        // save changes
        superuser.save();

        // get the root node in the second workspace
        Node rootNodeW2 = (Node) superuserW2.getItem(testRootNode.getPath());

        // create test node in second workspace
        rootNodeW2.addNode(nodeName1, testNodeType);

        // save changes
        superuserW2.save();

        // call the update method on test node in default workspace
        defaultTestNode.getCorrespondingNodePath(workspaceName);

        // ok, works as expected
    }

    /**
     * Tries calling {@link javax.jcr.Node#update(String)} after node has
     * changed in first workspace but not been saved yet. <br/><br/> This should
     * throw and {@link javax.jcr.InvalidItemStateException}. <br/><br/>
     * Prerequisites: <ul> <li><code>javax.jcr.tck.propertyname1</code> name of
     * a String property that can be modified in <code>javax.jcr.tck.nodetype</code>
     * for testing</li> </ul>
     */
    public void testUpdateInvalidItemStateException() throws RepositoryException, NotExecutableException {

        // make sure the repository supports multiple workspaces
        super.ensureMultipleWorkspacesSupported();

        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create a test node in default workspace
        Node testNode = defaultRootNode.addNode(nodeName1, testNodeType);

        // save changes
        superuser.save();

        // get the root node in the second workspace
        Node rootNodeW2 = (Node) superuserW2.getItem(testRootNode.getPath());

        // create test node in second workspace
        rootNodeW2.addNode(nodeName1);

        // save changes
        superuserW2.save();

        // modify the node
        testNode.setProperty(propertyName1, "test");

        try {
            // try calling update
            testNode.update(workspaceName);
            fail("Calling Node.update() on modified node should throw InvalidItemStateException");
        } catch (InvalidItemStateException e) {
            // ok , works as expected
        }
    }

    /**
     * Tries to use {@link javax.jcr.Node#update(String)} with an invalid
     * workspace. <br/><br/> This should throw an {@link
     * javax.jcr.NoSuchWorkspaceException}.
     */
    public void testUpdateNoSuchWorkspaceException() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create a test node in default workspace
        Node testNode = defaultRootNode.addNode(nodeName1, testNodeType);

        // save changes
        superuser.save();

        try {
            testNode.update(getNonExistingWorkspaceName(superuser));
            fail("Calling Node.update() on a non existing workspace should throw NoSuchWorkspaceException");
        } catch (NoSuchWorkspaceException e) {
            // ok, works as expected
        }
    }

    /**
     * Calls {@link javax.jcr.Node#update(String)} for a node that only exists
     * in current workspace. <br><br> In that case nothing should happen.
     * <br/><br/>Prerequisites: <ul> <li><code>javax.jcr.tck.propertyname1</code>
     * name of a String property that can be modified in
     * <code>javax.jcr.tck.nodetype</code> for testing</li> </ul>
     */
    public void testUpdateNoClone() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create a test node in default workspace
        Node testNode = defaultRootNode.addNode(nodeName1, testNodeType);

        // modify the node
        testNode.setProperty(propertyName1, "test");

        superuser.save();

        // call the update method on test node in default workspace
        testNode.update(workspaceName);

        // check if property is still there
        assertTrue("Node got property removed after Node.update() eventhough node has no clone", testNode.hasProperty(propertyName1));
        // check if node did not get childs suddenly
        assertFalse("Node has children assigned after Node.update() eventhough node has no clone", testNode.hasNodes());
    }


    /**
     * Checks if {@link javax.jcr.Node#update(String)} works properly by
     * creating the same node in two workspaces one with a child node the other
     * with a property set. <br/><br/> Calling <code>update()</code> on the node
     * with properties, should remove the properties and add the child node.
     * <br/><br/>Prerequisites: <ul> <li><code>javax.jcr.tck.nodetype</code>
     * must allow children of same nodetype. <li><code>javax.jcr.tck.propertyname1</code>
     * name of a String property that can be modified in
     * <code>javax.jcr.tck.nodetype</code> for testing</li> </ul>
     */
    public void testUpdate() throws RepositoryException, NotExecutableException {

        // make sure the repository supports multiple workspaces
        super.ensureMultipleWorkspacesSupported();

        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create test node in default workspace
        Node defaultTestNode = defaultRootNode.addNode(nodeName1, testNodeType);

        defaultTestNode.setProperty(propertyName1, "test");

        // save changes
        superuser.save();

        // get the root node in the second workspace
        Node rootNodeW2 = (Node) superuserW2.getItem(testRootNode.getPath());

        // create test node in second workspace
        Node testNodeW2 = rootNodeW2.addNode(nodeName1, testNodeType);

        // add a child node
        testNodeW2.addNode(nodeName2, testNodeType);

        // save changes
        superuserW2.save();

        // call the update method on test node in default workspace
        defaultTestNode.update(workspaceName);

        // ok first check if node has no longer propertis
        assertFalse("Node updated with Node.update() should have property removed", defaultTestNode.hasProperty(propertyName1));
        // ok check if the child has been added
        assertTrue("Node updated with Node.update() should have received childrens", defaultTestNode.hasNode(nodeName2));
    }

    /**
     * Tries to add a node using {@link javax.jcr.Node#addNode(String)} where
     * node type can not be determined by parent (<code>nt:base</code> is used
     * as parent nodetype). <br/><br/> This should throw a {@link
     * javax.jcr.nodetype.ConstraintViolationException}.
     */
    public void testAddNodeConstraintViolationExceptionUndefinedNodeType() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());
        String nodetype = testNodeTypeNoChildren == null ? ntBase : testNodeTypeNoChildren;
        Node defaultTestNode = defaultRootNode.addNode(nodeName1, nodetype);

        try {
            defaultTestNode.addNode(nodeName2);
            fail("Adding a node with node.addNode(node) where nodetype can not be determined from parent should" +
                    " throw ConstraintViolationException");
        } catch (ConstraintViolationException e) {
            // ok, works as expected
        }
    }

    /**
     * Tries to add a node using {@link javax.jcr.Node#addNode(String)} as a
     * child of a property.<br/> <br/> This should throw an {@link
     * javax.jcr.nodetype.ConstraintViolationException}.
     * <br/><br/>Prerequisites: <ul> <li><code>javax.jcr.tck.propertyname1</code>
     * name of a String property that can be set in <code>javax.jcr.tck.nodetype</code>
     * for testing</li> </ul>
     */
    public void testAddNodeConstraintViolationExceptionProperty() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // add a node
        Node defaultTestNode = defaultRootNode.addNode(nodeName1, testNodeType);

        // set a property
        defaultTestNode.setProperty(propertyName1, "test");

        try {
            // try to add a node as a child of a property
            defaultTestNode.addNode(propertyName1 + "/" + nodeName2);
            fail("Adding a node as a child of a property should throw ConstraintViolationException");
        } catch (ConstraintViolationException e) {
            // ok, works as expected
        }
    }

    /**
     * Tries to create a node using {@link javax.jcr.Node#addNode(String,
     * String)}  at a location where there is already a node with same name and
     * the parent does not allow same name siblings. <br/><br/> This should
     * throw an {@link javax.jcr.ItemExistsException }. <br/><br> Prerequisites:
     * <ul> <li><code>javax.jcr.tck.NodeTest.testAddNodeItemExistsException.nodetype<code>
     * node type that does not allow same name siblings and allows to add child
     * nodes of the same type.</li> </ul>
     */
    public void testAddNodeItemExistsException() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // add a node
        Node defaultTestNode = defaultRootNode.addNode(nodeName2, testNodeType);
        // add a child
        defaultTestNode.addNode(nodeName3, testNodeType);

        // save the new node
        defaultRootNode.save();

        try {
            // try to add a node with same name again
            defaultTestNode.addNode(nodeName3, testNodeType);
            defaultRootNode.save();
            fail("Adding a node to a location where same name siblings are not allowed, but a node with same name" +
                    " already exists should throw ItemExistsException ");
        } catch (ItemExistsException e) {
            //ok, works as expected
        }
    }

    /**
     * Tries to add a node using {@link javax.jcr.Node#addNode(String)} to a non
     * existing destination node. <br/><br/> This should throw an {@link
     * javax.jcr.PathNotFoundException}.
     */
    public void testAddNodePathNotFoundException() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        try {
            // use invalid parent path
            defaultRootNode.addNode(nodeName1 + "/" + nodeName2);
            fail("Creating a node at a non existent destination should throw PathNotFoundException");
        } catch (PathNotFoundException e) {
            // ok, works as expected
        }
    }

    /**
     * Adds a new node using {@link javax.jcr.Node#addNode(String)} with an
     * index for the new name. <br/><br/> This should throw an {@link
     * RepositoryException}.
     */
    public void testAddNodeRepositoryExceptionRelPathIndex() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        try {
            // use invalid relPath
            defaultRootNode.addNode(nodeName1 + "[1]", testNodeType);
            fail("Creating a node with index as postfix for new name should throw RepositoryException");
        } catch (RepositoryException e) {
            // ok, works as expected
        }
    }

    /**
     * Creates a new node using {@link Node#addNode(String)}, then tries to call
     * {@link javax.jcr.Node#save()} on the newly node. <br/><br/> This should
     * throw an {@link RepositoryException}.
     */
    public void testAddNodeRepositoryExceptionSaveOnNewNode() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // add a node
        Node testNode = defaultRootNode.addNode(nodeName1, testNodeType);

        try {
            // try to call save on newly created node
            testNode.save();
            fail("Calling Node.save() on a newly created node should throw RepositoryException");
        } catch (RepositoryException e) {
            // ok, works as expected.
        }
    }

    /**
     * Creates a new node using {@link Node#addNode(String)} , saves using
     * {@link javax.jcr.Node#save()} on parent node. Uses a second session to
     * verify if the node has been safed.
     */
    public void testAddNodeParentSave() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // add a node
        Node testNode = defaultRootNode.addNode(nodeName1, testNodeType);

        // save new nodes
        defaultRootNode.save();

        // use a different session to verify if the node is there
        Session session = helper.getReadOnlySession();
        try {
            testNode = (Node) session.getItem(testNode.getPath());
        } finally {
            session.logout();
        }
    }

    /**
     * Creates a new node using {@link Node#addNode(String)} , saves using
     * {@link javax.jcr.Session#save()}. Uses a second session to verify if the
     * node has been safed.
     */
    public void testAddNodeSessionSave() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // add a node
        Node testNode = defaultRootNode.addNode(nodeName1, testNodeType);

        // save new nodes
        superuser.save();

        // use a different session to verify if the node is there
        Session session = helper.getReadOnlySession();
        try {
            testNode = (Node) session.getItem(testNode.getPath());
        } finally {
            session.logout();
        }
    }

    /**
     * Creates a node with a mandatory child node using {@link
     * Node#addNode(String, String)}, saves on parent node then tries to delete
     * the mandatory child node. <br/><br/> This should throw a {@link
     * ConstraintViolationException}. <br/><br/>Prerequisites: <ul>
     * <li><code>javax.jcr.tck.NodeTest.testRemoveMandatoryNode.nodetype2</code>
     * a node type that has a mandatory child node</li> <li><code>javax.jcr.tck.NodeTest.testRemoveMandatoryNode.nodetype3</code>
     * nodetype of the mandatory child node</li> <li><code>javax.jcr.tck.NodeTest.testRemoveMandatoryNode.nodename3</code>
     * name of the mandatory child node</li> </ul>
     */
    public void testRemoveMandatoryNode() throws RepositoryException {

        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create the node with the mandatory child node definition
        Node defaultTestNode = defaultRootNode.addNode(nodeName2, getProperty("nodetype2"));

        // add the mandatory child node
        Node defaultTestNodeChild = defaultTestNode.addNode(nodeName3, getProperty("nodetype3"));

        // save changes
        defaultRootNode.save();

        try {
            // try to remove the mandatory node
            defaultTestNodeChild.remove();

            defaultTestNode.save();
            fail("Removing a mandatory node should throw a ConstraintViolationException");
        } catch (ConstraintViolationException e) {
            // ok, works as expected
        }
    }

    /**
     * Removes a node using {@link javax.jcr.Node#remove()} with session 1,
     * afterwards it tries the same with session 2. <br/><br/> This should throw
     * an {@link InvalidItemStateException}.
     */
    public void testRemoveInvalidItemStateException() throws RepositoryException {

        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create the node
        Node defaultTestNode = defaultRootNode.addNode(nodeName1, testNodeType);

        // save the nodes
        superuser.save();

        // get the node with session 2
        Session testSession = helper.getReadWriteSession();
        try {
            Node defaultTestNodeSession2 = (Node) testSession.getItem(defaultTestNode.getPath());

            // remove node with session 1
            defaultTestNode.remove();
            superuser.save();

            // try to remove already deleted node with session 2
            try {
                defaultTestNodeSession2.remove();
                testSession.save();
                fail("Removing a node already deleted by other session should throw an InvalidItemStateException!");
            } catch (InvalidItemStateException e) {
                //ok, works as expected
            }
        } finally {
            testSession.logout();
        }
    }

    /**
     * Removes a node using {@link javax.jcr.Node#remove()}, then saves with
     * parent's nodes {@link javax.jcr.Node#save()} method.
     */
    public void testRemoveNodeParentSave() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create the node
        Node defaultTestNode = defaultRootNode.addNode(nodeName1, testNodeType);

        // save the nodes
        defaultRootNode.save();

        // remove them
        defaultTestNode.remove();

        defaultRootNode.save();

        // check if the node has been properly removed
        try {
            defaultRootNode.getNode(nodeName1);
            fail("Permanently removed node should no longer be adressable using Parent Node's getNode() method");
        } catch (PathNotFoundException e) {
            // ok , works as expected
        }
    }


    /**
     * Removes a node using {@link javax.jcr.Node#remove()}, then saves using
     * {@link javax.jcr.Session#save()} method.
     */
    public void testRemoveNodeSessionSave() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create the node
        Node defaultTestNode = defaultRootNode.addNode(nodeName1, testNodeType);

        // save the nodes
        superuser.save();

        // remove them
        defaultTestNode.remove();

        superuser.save();

        // check if the node has been properly removed
        try {
            superuser.getItem(defaultRootNode.getPath() + "/" + nodeName1);
            fail("Permanently removed node should no longer be adressable using Session.getItem()");
        } catch (PathNotFoundException e) {
            // ok, works as expected
        }
    }

    /**
     * Tests if <code>Node.remove()</code> does not throw a
     * <code>LockException</code> if <code>Node</code> is locked.
     * <p/>
     * The test creates a node <code>nodeName1</code> of type
     * <code>testNodeType</code> under <code>testRoot</code> and locks the node
     * with the superuser session. Then the test removes
     * <code>nodeName1</code>.
     */
    public void testRemoveNodeLockedItself()
            throws LockException, NotExecutableException, RepositoryException {

        Session session = testRootNode.getSession();

        if (!isSupported(Repository.OPTION_LOCKING_SUPPORTED)) {
            throw new NotExecutableException("Locking is not supported.");
        }

        // create a node that is lockable
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        // or try to make it lockable if it is not
        if (!node.isNodeType(mixLockable)) {
            if (node.canAddMixin(mixLockable)) {
                node.addMixin(mixLockable);
            } else {
                throw new NotExecutableException("Node " + nodeName1 + " is not lockable and does not " +
                        "allow to add mix:lockable");
            }
        }
        testRootNode.save();

        // remove first slash of path to get rel path to root
        String pathRelToRoot = node.getPath().substring(1);

        // access node through another session to lock it
        Session session2 = helper.getSuperuserSession();
        try {
            Node node2 = session2.getRootNode().getNode(pathRelToRoot);
            node2.lock(true, true);

            // test fails if a LockException is thrown when removing the node
            // (remove must be possible since the parent is not locked)
            node.remove();
        } finally {
            session2.logout();
        }
    }

    /**
     * Tests if <code>Node.remove()</code> throws a <code>LockException</code>
     * if the parent node of <code>Node</code> is locked.
     * <p/>
     * The test creates a node <code>nodeName1</code> of type
     * <code>testNodeType</code> under <code>testRoot</code>, adds a child node
     * <code>nodeName2</code> and locks it with the superuser session. Then the
     * test tries to remove the <code>nodeName2</code>.
     */
    public void testRemoveNodeParentLocked()
            throws LockException, NotExecutableException, RepositoryException {

        Session session = testRootNode.getSession();

        if (!isSupported(Repository.OPTION_LOCKING_SUPPORTED)) {
            throw new NotExecutableException("Locking is not supported.");
        }

        // create a node that is lockable
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        // or try to make it lockable if it is not
        if (!node.isNodeType(mixLockable)) {
            if (node.canAddMixin(mixLockable)) {
                node.addMixin(mixLockable);
            } else {
                throw new NotExecutableException("Node " + nodeName1 + " is not lockable and does not " +
                        "allow to add mix:lockable");
            }
        }
        // create a child node
        Node subNode = node.addNode(nodeName2, testNodeType);
        testRootNode.save();

        // lock the node
        // remove first slash of path to get rel path to root
        String pathRelToRoot = node.getPath().substring(1);
        // access node through another session to lock it
        Session session2 = helper.getSuperuserSession();
        try {
            Node node2 = session2.getRootNode().getNode(pathRelToRoot);
            node2.lock(true, true);

            try {
                subNode.remove();
                session.save();
                fail("Removal of a Node must throw a LockException upon remove() " +
                     "or upon save() if the parent of the node is locked");
            } catch (LockException e) {
                // success
            }

            // unlock to remove node at tearDown()
            node2.unlock();
        } finally {
            session2.logout();
        }
    }

    /**
     * Tests object identity, meaning two nodes objects accuired through the
     * same session must have the same properties and states. <br/><br/>
     * Prerequisites: <ul> <li><code>javax.jcr.tck.nodetype</code> must allow
     * children of same node type</li> <li><code>javax.jcr.tck.propertyname1</code>
     * name of a String property that can be set in <code>javax.jcr.tck.nodetype</code>
     * for testing</li> </ul>
     */
    public void testNodeIdentity() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create a node
        Node testNode1 = defaultRootNode.addNode(nodeName1, testNodeType);
        // add a child node
        testNode1.addNode(nodeName1, testNodeType);
        // add a property
        testNode1.setProperty(propertyName1, "test");

        // save the new node
        defaultRootNode.save();

        // acquire the same node with session 2
        Node testNode2 = (Node) superuser.getItem(testNode1.getPath());

        // check if they have the same property
        assertTrue("Two references of same node have different properties", testNode1.getProperty(propertyName1).isSame(testNode2.getProperty(propertyName1)));
        // check if they have the same child
        assertTrue("Two references of same node have different children", testNode1.getNode(nodeName1).isSame(testNode2.getNode(nodeName1)));
        // check state methods
        assertEquals("Two references of same node have different State for Node.isCheckedOut()", testNode1.isCheckedOut(), testNode2.isCheckedOut());
        assertEquals("Two references of same node have different State for Node.isLocked()", testNode1.isLocked(), testNode2.isLocked());
        assertEquals("Two references of same node have different State for Node.isModified()", testNode1.isModified(), testNode2.isModified());
        assertEquals("Two references of same node have different State for Node.isNew()", testNode1.isNew(), testNode2.isNew());
        assertEquals("Two references of same node have different State for Node.isNode()", testNode1.isNode(), testNode2.isNode());
        assertEquals("Two references of same node have different State for Node.isNodeType()", testNode1.isNodeType(testNodeType), testNode2.isNodeType(testNodeType));
        assertTrue("Two references of same node should return true for Node1.isSame(Node2)", testNode1.isSame(testNode2));
        assertEquals("Two references of same node have different Definitions", testNode1.getDefinition().getName(), testNode2.getDefinition().getName());
    }

    /**
     * Tests if <code>Item.isSame(Item otherItem)</code> will return true when
     * two <code>Node</code> objects representing the same actual repository
     * item have been retrieved through two different sessions and one has been
     * modified.
     */
    public void testIsSameMustNotCompareStates()
            throws RepositoryException {

        // create a node and save it
        Node testNode1 = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();

        // accuire the same node with a different session
        Session session = helper.getReadOnlySession();
        try {
            Node testNode2 = (Node) session.getItem(testNode1.getPath());

            // add a property and do not save it so property is different in testNode2
            testNode1.setProperty(propertyName1, "value1");

            assertTrue("Two references of same node should return true for Node1.isSame(Node2)",
                    testNode1.isSame(testNode2));
        } finally {
            session.logout();
        }
    }

    /**
     * Checks if {@link Node#isModified()} works correcty for unmodified and
     * modified nodes.
     */
    public void testIsModified() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create a node
        Node testNode = defaultRootNode.addNode(nodeName1, testNodeType);

        defaultRootNode.save();

        assertFalse("Unmodified node should return false on Node.isModified()", testNode.isModified());

        // check if modified properties are recognised
        testNode.setProperty(propertyName1, "test");

        assertTrue("Modified node should return true on Node.isModified()", testNode.isModified());

        defaultRootNode.save();

        // check if modified child nodes are recognised
        testNode.addNode(nodeName2, testNodeType);

        assertTrue("Modified node should return true on Node.isModified()", testNode.isModified());

    }

    /**
     * Checks if {@link Node#isNew()} works correctly for new and existing,
     * unmodified nodes.
     */
    public void testIsNew() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create a node
        Node testNode = defaultRootNode.addNode(nodeName1, testNodeType);

        assertTrue("Newly created node should return true on newNode.isNew()", testNode.isNew());

        defaultRootNode.save();

        assertFalse("Unmodified, exisiting node should return false on newNode.isNew()", testNode.isNew());

    }

    /**
     * Tries to call {@link Node#refresh(boolean)}  on a deleted node.
     * <br/><br/> This should throw an {@link InvalidItemStateException}.
     */
    public void testRefreshInvalidItemStateException() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create a node
        Node testNode = defaultRootNode.addNode(nodeName1, testNodeType);

        // save the new node
        defaultRootNode.save();

        // remove the node
        defaultRootNode.remove();

        try {
            testNode.refresh(true);
            fail("Calling Node.refresh() on deleted node should throw InvalidItemStateException!");
        } catch (InvalidItemStateException e) {
            // ok, works as expected
        }
    }

    /**
     * Checks if {@link javax.jcr.Node#refresh(boolean refresh)} works properly
     * with <code>refresh</code> set to <code>false</code>.<br/> <br/>
     * Procedure: <ul> <li>Creates two nodes with session 1</li> <li>Modifies
     * node 1 with session 1 by adding a child node</li> <li>Get node 2 with
     * session 2</li> <li>Modifies node 2 with session 2 by adding a child
     * node</li> <li>saves session 2 changes using {@link
     * javax.jcr.Node#save()}</li> <li>calls <code>Node.refresh(false)</code>
     * on root node in session 1</li> </ul> Session 1 changes should be cleared
     * and session 2 changes should now be visible to session 1.
     * <br/><br/>Prerequisites: <ul> <li><code>javax.jcr.tck.nodetype</code>
     * must accept children of same nodetype</li> </ul>
     */
    public void testRefreshBooleanFalse() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create a node
        Node testNode1Session1 = defaultRootNode.addNode(nodeName1, testNodeType);
        // create a second node
        Node testNode2Session1 = defaultRootNode.addNode(nodeName2, testNodeType);

        // save the new nodes
        defaultRootNode.save();

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
            defaultRootNode.refresh(false);

            // check if session 1 flag has been cleared
            assertFalse("Session should have no pending changes recorded after Node.refresh(false)!", superuser.hasPendingChanges());

            // check if added child node for node 1 by session 1 has been removed
            assertFalse("Node Modifications have not been flushed after Node.refresh(false)", testNode1Session1.hasNodes());

            // check if added child node for node 2 by session 2 has become visible in session 1
            assertTrue("Node modified by a different session has not been updated after Node.refresh(false)", testNode2Session1.hasNodes());
        } finally {
            session2.logout();
        }
    }

    /**
     * Checks if {@link javax.jcr.Node#refresh(boolean refresh)} works properly
     * with <code>refresh</code> set to <code>true</code>.<br/> <br/>
     * Procedure: <ul> <li>Creates two nodes with session 1</li> <li>Modifies
     * node 1 with session 1 by adding a child node</li> <li>Get node 2 with
     * session 2</li> <li>Modifies node 2 with session 2 by adding a child
     * node</li> <li>saves session 2 changes using {@link
     * javax.jcr.Node#save()}</li> <li>calls <code>Node.refresh(true)</code> on
     * root node in session 1</li> </ul> Session 1 changes and session 2
     * changes now be visible to session 1. <br/><br/>Prerequisites: <ul>
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
        defaultRootNode.save();

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
            defaultRootNode.refresh(true);

            // check if session 1 flag has been cleared
            assertTrue("Session should still have pending changes recorded after Node.refresh(true)!", superuser.hasPendingChanges());

            // check if added child node for node 1 by session 1 is still there
            assertTrue("Node Modifications are lost after Node.refresh(true)", testNode1Session1.hasNodes());

            // check if added child node for node 2 by session 2 has become visible in session 1
            assertTrue("Node modified by a different session has not been updated after Node.refresh(true)", testNode2Session1.hasNodes());
        } finally {
            session2.logout();
        }
    }

    /**
     * Tries to save a node using {@link javax.jcr.Node#save()} that was already
     * deleted by an other session.<br/> <br/> Procedure: <ul> <li>Creates a new
     * node with session 1, saves it, adds a child node.</li> <li>Access new
     * node with session 2,deletes the node, saves it.</li> <li>Session 1 tries
     * to save modifications using <code>Node.save()</code> on root node .</li>
     * </ul> This should throw an {@link javax.jcr.InvalidItemStateException}.
     * <br/><br/>Prerequisites: <ul> <li><code>javax.jcr.tck.nodetype</code>
     * must accept children of same nodetype</li> </ul>
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
                defaultRootNode.save();
                fail("Saving a modified Node using Node.save() already deleted by an other session should throw InvalidItemStateException");
            } catch (InvalidItemStateException e) {
                // ok, works as expected
            }
        } finally {
            testSession.logout();
        }
    }

    /**
     * Tries to create and save a node using {@link javax.jcr.Node#save()} with
     * an mandatory property that is not set on saving time.
     * <p/>
     * Prerequisites: <ul> <li><code>javax.jcr.tck.Node.testSaveContstraintViolationException.nodetype2</code>
     * must reference a nodetype that has at least one property that is
     * mandatory but not autocreated</li> </ul>
     */
    public void testSaveContstraintViolationException() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create a node with at least one mandatory, not autocreated property
        defaultRootNode.addNode(nodeName1, this.getProperty("nodetype2"));

        // save changes
        try {
            superuser.save();
            fail("Trying to use parent Node.save() with a node that has a mandatory property not set, should throw ConstraintViolationException");
        } catch (ConstraintViolationException e) {
            // ok
        }
    }

    /**
     * Creates a new node, saves it uses second session to verify if node has
     * been added.
     */
    public void testNodeSave() throws RepositoryException {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create a node
        Node testNode = defaultRootNode.addNode(nodeName1, testNodeType);

        // save new node
        defaultRootNode.save();

        // get the new node with a different session
        Session testSession = helper.getReadOnlySession();
        try {
            testSession.getItem(testNode.getPath());
        } finally {
            testSession.logout();
        }
    }

    /**
     * Tests if a {@link javax.jcr.RepositoryException} is thrown when calling
     * <code>Node.save()</code> on a newly added node
     */
    public void testSaveOnNewNodeRepositoryException() throws Exception {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        // create a node
        Node newNode = defaultRootNode.addNode(nodeName1, testNodeType);

        try {
            newNode.save();
            fail("Calling Node.save() on a newly added node should throw a RepositoryException");
        } catch (RepositoryException success) {
            // ok
        }
    }

    /**
     * Tests if the primary node type is properly stored in jcr:primaryType
     */
    public void testPrimaryType() throws Exception {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        Node testNode = defaultRootNode.addNode(nodeName1, testNodeType);
        assertEquals("The primary node type is not properly stored in jcr:primaryType",testNodeType,testNode.getProperty(jcrPrimaryType).getString());
    }

    /**
     * Tests if jcr:primaryType is protected
     */
    public void testPrimaryTypeProtected() throws Exception {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        Node testNode = defaultRootNode.addNode(nodeName1, testNodeType);
        try {
            testNode.setProperty(jcrPrimaryType,ntBase);
            fail("Manually setting jcr:primaryType should throw a ConstraintViolationException");
        }
        catch (ConstraintViolationException success) {
            // ok
        }
    }

    /**
     * Tests if jcr:mixinTypes is protected
     */
    public void testMixinTypesProtected() throws Exception {
        // get default workspace test root node using superuser session
        Node defaultRootNode = (Node) superuser.getItem(testRootNode.getPath());

        Node testNode = defaultRootNode.addNode(nodeName1, testNodeType);

        Value mixinName = superuser.getValueFactory().createValue(mixLockable, PropertyType.NAME);
        try {
            testNode.setProperty(jcrMixinTypes, new Value[]{mixinName});
            fail("Manually setting jcr:mixinTypes should throw a ConstraintViolationException");
        }
        catch (ConstraintViolationException success) {
            // ok
        }
    }
}
