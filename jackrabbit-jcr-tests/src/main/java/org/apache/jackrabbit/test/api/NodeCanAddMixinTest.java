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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * <code>NodeCanAddMixinTest</code> contains the test cases for the method
 * <code>Node.canAddMixin(String)</code>.
 *
 * @test
 * @sources NodeCanAddMixinTest.java
 * @executeClass org.apache.jackrabbit.test.api.NodeCanAddMixinTest
 * @keywords level2
 */
public class NodeCanAddMixinTest extends AbstractJCRTest {

    /**
     * Tests if <code>Node.canAddMixin(String mixinName)</code> throws a
     * <code>LockException</code> if <code>Node</code> is locked
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

        // remove first slash of path to get rel path to root
        String pathRelToRoot = node.getPath().substring(1);

        // access node through another session to lock it
        Session session2 = helper.getSuperuserSession();
        try {
            Node node2 = session2.getRootNode().getNode(pathRelToRoot);
            node2.lock(true, true);

            node.refresh(false);
            assertFalse("Node.canAddMixin(String mixinName) must return false " +
                    "if the node is locked.",
                    node.canAddMixin(mixinName));

            node2.unlock();
        } finally {
            session2.logout();
        }
    }

    /**
     * Tests if <code>Node.canAddMixin(String mixinName)</code> throws a
     * <code>VersionException</code> if <code>Node</code> is checked-in
     */
    public void testCheckedIn()
            throws ConstraintViolationException, NotExecutableException, RepositoryException {

        Session session = testRootNode.getSession();

        if (!isSupported(Repository.OPTION_VERSIONING_SUPPORTED)) {
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

        node.checkin();

        assertFalse("Node.canAddMixin(String mixinName) must return false " +
                "if the node is checked-in.",
                node.canAddMixin(mixinName));
    }

    /**
     * Tests if <code>Node.canAddMixin(String mixinName)</code> throws a
     * <code>NoSuchNodeTypeException</code> if <code>mixinName</code> is not the
     * name of an existing mixin node type
     */
    public void testNonExisting() throws RepositoryException {
        Session session = testRootNode.getSession();
        String nonExistingMixinName = NodeMixinUtil.getNonExistingMixinName(session);

        Node node = testRootNode.addNode(nodeName1, testNodeType);

        try {
            node.canAddMixin(nonExistingMixinName);
            fail("Node.canAddMixin(String mixinName) must throw a " +
                    "NoSuchNodeTypeException if mixinName is an unknown mixin type");
        } catch (NoSuchNodeTypeException e) {
            // success
        }
    }

}