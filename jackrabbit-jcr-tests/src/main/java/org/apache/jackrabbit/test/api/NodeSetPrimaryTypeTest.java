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

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>SetPrimaryType</code>...
 */
public class NodeSetPrimaryTypeTest extends AbstractJCRTest {

    // TODO: test if node definition is properly reset
    // TODO: test if child items are properly reset upon changing definition
    // TODO: test if conflicts are properly detected

    /**
     * Tests a successful call to <code>Node.setPrimaryType(String)</code>
     */
    public void testSetPrimaryType() throws RepositoryException {
        Session session = testRootNode.getSession();
        Session otherSession = null;

        String nonExistingMixinName = NodeMixinUtil.getNonExistingMixinName(session);

        Node node = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();

        // TODO improve. retrieve settable node type name from config.
        NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
        NodeTypeIterator nts = manager.getPrimaryNodeTypes();
        while (nts.hasNext()) {
            NodeType nt = nts.nextNodeType();
            String ntName = nt.getName();
            if (!nt.isAbstract()
                    && !ntFrozenNode.equals(ntName)
                    && !ntActivity.equals(ntName)) {
                try {
                    node.setPrimaryType(ntName);
                    // property value must be adjusted immediately
                    assertEquals("The value of the jcr:primaryType property must change upon setPrimaryType.", ntName, node.getProperty(jcrPrimaryType).getString());

                    // save changes -> reflected upon Node.getPrimaryNodeType and Property.getValue
                    superuser.save();

                    assertEquals("Node.getPrimaryNodeType must reflect the changes made.", ntName, node.getPrimaryNodeType().getName());
                    assertEquals("The value of the jcr:primaryType property must change upon setPrimaryType.", ntName, node.getProperty(jcrPrimaryType).getString());

                    otherSession = getHelper().getReadOnlySession();
                    assertEquals("Node.getPrimaryNodeType must reflect the changes made.", ntName, otherSession.getNode(node.getPath()).getPrimaryNodeType().getName());
                    assertEquals("The value of the jcr:primaryType property must change upon setPrimaryType.", ntName, otherSession.getNode(node.getPath()).getProperty(jcrPrimaryType).getString());

                    // was successful
                    return;

                } catch (ConstraintViolationException e) {
                    // may happen as long as arbitrary primary types are used for testing -> ignore
                } finally {
                    if (otherSession != null) {
                        otherSession.logout();
                    }
                    // revert any unsaved changes.
                    session.refresh(false);
                }
            }
        }
    }

    /**
     * Passing the current primary type to {@link Node#setPrimaryType(String)}
     * must always succeed.
     *
     * @throws RepositoryException
     */
    public void testSetCurrentType() throws RepositoryException {
        Session session = testRootNode.getSession();

        Node node = testRootNode.addNode(nodeName1, testNodeType);
        superuser.save();

        node.setPrimaryType(testNodeType);
        superuser.save();
    }

    /**
     * Passing the current primary type to {@link Node#setPrimaryType(String)}
     * to a new node must always succeed.
     *
     * @throws RepositoryException
     */
    public void testSetCurrentTypeOnNew() throws RepositoryException {
        Session session = testRootNode.getSession();

        Node node = testRootNode.addNode(nodeName1, testNodeType);
        node.setPrimaryType(testNodeType);
        superuser.save();
    }

    /**
     * Tests if <code>Node.setPrimaryType(String)</code> throws a
     * <code>NoSuchNodeTypeException</code> if the
     * name of an existing node type is passed.
     */
    public void testAddNonExisting() throws RepositoryException {
        Session session = testRootNode.getSession();

        NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
        String nonExistingMixinName = "abc";
        while (manager.hasNodeType(nonExistingMixinName)) {
            nonExistingMixinName += "_";
        }
        Node node = testRootNode.addNode(nodeName1, testNodeType);

        try {
            node.setPrimaryType(nonExistingMixinName);
            // ev. only detected upon save
            superuser.save();
            fail("Node.setPrimaryType(String) must throw a NoSuchNodeTypeException if no nodetype exists with the given name.");
        } catch (NoSuchNodeTypeException e) {
            // success
        }
    }

    /**
     * Tests if <code>Node.setPrimaryType(String)</code> throws a
     * <code>ConstraintViolationException</code> if the
     * name of a mixin type is passed
     */
    public void testSetMixinAsPrimaryType() throws RepositoryException {
        Session session = testRootNode.getSession();

        NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
        NodeTypeIterator nts = manager.getMixinNodeTypes();
        while (nts.hasNext()) {
            try {
                Node node = testRootNode.addNode(nodeName1, testNodeType);
                node.setPrimaryType(nts.nextNodeType().getName());
                fail("Node.setPrimaryType(String) must throw ConstraintViolationException if the specified node type name refers to a mixin.");
            } catch (ConstraintViolationException e) {
                // success
            } finally {
                // reset the changes.
                session.refresh(false);
            }
        }
    }

    /**
     * Tests if <code>Node.setPrimaryType(String)</code> throws a
     * <code>ConstraintViolationException</code> if the
     * name of a mixin type is passed
     */
    public void testSetAbstractAsPrimaryType() throws RepositoryException {
        Session session = testRootNode.getSession();

        NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
        NodeTypeIterator nts = manager.getPrimaryNodeTypes();
        while (nts.hasNext()) {
            NodeType nt = nts.nextNodeType();
            if (nt.isAbstract()) {
                try {
                    Node node = testRootNode.addNode(nodeName1, testNodeType);
                    node.setPrimaryType(nt.getName());
                    fail("Node.setPrimaryType(String) must throw ConstraintViolationException if the specified node type name refers to an abstract node type.");
                } catch (ConstraintViolationException e) {
                    // success
                } finally {
                    // reset the changes.
                    session.refresh(false);
                }
            }
        }
    }

    /**
     * Tests if <code>Node.setPrimaryType(String)</code> throws a
     * <code>LockException</code> if <code>Node</code> is locked.
     */
    public void testLocked() throws NotExecutableException, RepositoryException {

        Session session = testRootNode.getSession();

        if (!isSupported(Repository.OPTION_LOCKING_SUPPORTED)) {
            throw new NotExecutableException("Locking is not supported.");
        }

        // create a node that is lockable
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        // or try to make it lockable if it is not
        ensureMixinType(node, mixLockable);
        testRootNode.getSession().save();

        String primaryTypeName = getPrimaryTypeName(session, node);
        if (primaryTypeName == null) {
            throw new NotExecutableException("No testable node type found");
        }

        // remove first slash of path to get rel path to root
        String pathRelToRoot = node.getPath().substring(1);

        // access node through another session to lock it
        Session session2 = getHelper().getSuperuserSession();
        try {
            Node node2 = session2.getRootNode().getNode(pathRelToRoot);
            node2.lock(true, true);

            try {
                // implementation specific: either throw LockException upon
                // addMixin or upon save.
                node.setPrimaryType(primaryTypeName);
                node.save();
                fail("Node.setPrimaryType(String) must throw a LockException if the node is locked.");
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
     * Tests if <code>Node.setPrimaryType(String)</code> throws a
     * <code>VersionException</code> if <code>Node</code> is checked-in.
     */
    public void testCheckedIn() throws NotExecutableException, RepositoryException {

        Session session = testRootNode.getSession();

        if (!isSupported(Repository.OPTION_VERSIONING_SUPPORTED)) {
            throw new NotExecutableException("Versioning is not supported.");
        }

        // create a node that is versionable
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        // or try to make it versionable if it is not
        ensureMixinType(node, mixVersionable);
        superuser.save();

        String primaryTypeName = getPrimaryTypeName(session, node);
        if (primaryTypeName == null) {
            throw new NotExecutableException("No testable node type found");
        }

        node.checkin();

        try {
            node.setPrimaryType(primaryTypeName);
            fail("Node.setPrimaryType(String) must throw a VersionException if the node is checked-in.");
        } catch (VersionException e) {
            // success
        }
    }

    private static String getPrimaryTypeName(Session session, Node node)
            throws RepositoryException {

        NodeTypeManager manager = session.getWorkspace().getNodeTypeManager();
        NodeTypeIterator nts = manager.getPrimaryNodeTypes();

        while (nts.hasNext()) {
            String name = nts.nextNodeType().getName();
            if (!name.equals(node.getPrimaryNodeType().getName())) {
                return name;
            }
        }
        return null;
    }
}