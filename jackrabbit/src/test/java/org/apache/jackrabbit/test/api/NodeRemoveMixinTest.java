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

import javax.jcr.version.VersionException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;

/**
 * <code>NodeRemoveMixinTest</code> contains the test cases for the method
 * <code>Node.removeMixin(String)</code>.
 *
 * @test
 * @sources NodeRemoveMixinTest.java
 * @executeClass org.apache.jackrabbit.test.api.NodeRemoveMixinTest
 * @keywords level2
 */
public class NodeRemoveMixinTest extends AbstractJCRTest {

    /**
     * Tests if <code>Node.removeMixin(String mixinName)</code> removes the
     * requested mixin properly
     */
    public void testRemoveSuccessfully()
            throws NotExecutableException, RepositoryException {

        Session session = testRootNode.getSession();
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        String mixinName = NodeMixinUtil.getAddableMixinName(session, node);

        if (mixinName == null) {
            throw new NotExecutableException("No testable mixin node type found");
        }

        node.addMixin(mixinName);
        testRootNode.save();

        try {
            node.removeMixin(mixinName);
        } catch (ConstraintViolationException e) {
            /**
             * In some implementations it may not be possible to remove mixin node
             * types (short of removing the node itself). In these cases this
             * method will throw a ConstraintViolationException.
             */
            throw new NotExecutableException("Repository does not support remove of mixin.");
        }

        // test if mixin is removed from property jcr:mixinTypes immediately
        // it is implementation-specific, if the property is removed completely
        // or set to an empty array when removing the last mixin type
        try {
            Property mixinProps = node.getProperty(jcrMixinTypes);

            // getValues() returns an empty array
            assertTrue("Node.removeMixin(String mixinName) did not remove mixin from " +
                    "property " + jcrMixinTypes + ".",
                    mixinProps.getValues().length == 0);
        } catch (PathNotFoundException e) {
            // success (property jcr:mixinTypes has been completely removed)
        }

        // it is implementation-specific if a removed mixin isn't available
        // before or after save therefore save before further tests
        testRootNode.save();

        // test if removed mixin isn't available anymore by node.getMixinNodeTypes()
        assertTrue("removeMixin(String mixinName) did not remove mixin.",
                node.getMixinNodeTypes().length == 0);
    }

    /**
     * Tests if <code>Node.removeMixin(String mixinName)</code> throws a
     * NoSuchNodeTypeException <code>Node</code> does not have assigned the
     * requested mixin
     */
    public void testNotAssigned()
            throws NotExecutableException, RepositoryException {

        Session session = testRootNode.getSession();
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        String mixinName = NodeMixinUtil.getAddableMixinName(session, node);

        if (mixinName == null) {
            throw new NotExecutableException("No testable mixin node type found");
        }

        node.addMixin(mixinName);
        testRootNode.save();

        String notAssignedMixin = NodeMixinUtil.getAddableMixinName(session, node);
        if (notAssignedMixin == null) {
            throw new NotExecutableException("No testable mixin node type found");
        }

        try {
            node.removeMixin(notAssignedMixin);
            fail("Node.removeMixin(String mixinName) must throw a " +
                    "NoSuchNodeTypeException if Node does not have the " +
                    "specified mixin.");
        } catch (NoSuchNodeTypeException e) {
            // success
        }
    }

    /**
     * Tests if <code>Node.removeMixin(String mixinName)</code> throws a
     * <code>LockException</code> if <code>Node</code> is locked.
     * <p/>
     * The test creates a node <code>nodeName1</code> of type
     * <code>testNodeType</code> under <code>testRoot</code>, adds a mixin and
     * then locks the node with the superuser session. Then the test tries to
     * remove the before added mixin readWrite <code>Session</code>.
     */
    public void testLocked()
            throws ConstraintViolationException, NotExecutableException, RepositoryException {

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

        String mixinName = NodeMixinUtil.getAddableMixinName(session, node);
        if (mixinName == null) {
            throw new NotExecutableException("No testable mixin node type found");
        }

        node.addMixin(mixinName);
        testRootNode.save();

        // remove first slash of path to get rel path to root
        String pathRelToRoot = node.getPath().substring(1);

        // access node through another session to lock it
        Session session2 = helper.getSuperuserSession();
        try {
            Node node2 = session2.getRootNode().getNode(pathRelToRoot);
            node2.lock(true, true);

            try {
                // remove mixin on locked node must throw either directly upon
                // removeMixin or upon save.
                node.removeMixin(mixinName);
                node.save();
                fail("Node.removeMixin(String mixinName) must throw a " +
                        "LockException if the node is locked.");
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
     * Tests if <code>Node.removeMixin(String mixinName)</code> throws a
     * <code>VersionException</code> if <code>Node</code> is checked-in
     * <p/>
     * The test creates a node <code>nodeName1</code> of type
     * <code>testNodeType</code> under <code>testRoot</code>, adds a mixin and
     * then checks it in. Then the test tries to remove the added.
     */
    public void testCheckedIn()
            throws ConstraintViolationException, NotExecutableException, RepositoryException {

        Session session = testRootNode.getSession();

        if (!isSupported(Repository.OPTION_LOCKING_SUPPORTED)) {
            throw new NotExecutableException("Versioning is not supported.");
        }

        // create a node that is versionable
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        // or try to make it versionable if it is not
        if (!node.isNodeType(mixVersionable)) {
            if (node.canAddMixin(mixVersionable)) {
                node.addMixin(mixVersionable);
            } else {
                throw new NotExecutableException("Node " + nodeName1 + " is not versionable and does not " +
                        "allow to add mix:versionable");
            }
        }
        testRootNode.save();

        String mixinName = NodeMixinUtil.getAddableMixinName(session, node);
        if (mixinName == null) {
            throw new NotExecutableException("No testable mixin node type found");
        }

        node.addMixin(mixinName);
        testRootNode.save();
        node.checkin();

        try {
            node.removeMixin(mixinName);
            fail("Node.removeMixin(String mixinName) must throw a " +
                    "VersionException if the node is checked-in.");
        } catch (VersionException e) {
            // success
        }
    }
}
