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
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>NodeAddMixinTest</code> contains the test cases for the method
 * <code>Node.AddMixin(String)</code>.
 *
 */
public class NodeAddMixinTest extends AbstractJCRTest {

    /**
     * Tests if <code>Node.addMixin(String mixinName)</code> adds the requested
     * mixin and stores it in property <code>jcr:mixinTypes</code>
     */
    public void testAddSuccessfully()
            throws NotExecutableException, RepositoryException {

        Session session = testRootNode.getSession();
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        String mixinName = NodeMixinUtil.getAddableMixinName(session, node);

        if (mixinName == null) {
            throw new NotExecutableException("No testable mixin node type found");
        }

        node.addMixin(mixinName);

        // test if mixin is written to property jcr:mixinTypes immediately
        Value mixinValues[] = node.getProperty(jcrMixinTypes).getValues();
        boolean found = false;
        for (int i = 0; i < mixinValues.length; i++) {
            found |= mixinName.equals(mixinValues[i].getString());
        }
        if (! found) {
            fail("Mixin type must be added to property " + jcrMixinTypes + " immediately.");
        }

        // it is implementation-specific if a added mixin is available
        // before or after save therefore save before further tests
        testRootNode.getSession().save();

        // test if added mixin is available by node.getMixinNodeTypes()
        NodeType mixins[] = node.getMixinNodeTypes();
        found = false;
        for (int i = 0; i < mixins.length; i++) {
            found |= mixinName.equals(mixins[i].getName());
        }
        if (! found) {
            fail("Mixin '" + mixinName+ "' type not added.");
        }
    }

    /**
     * Tests if <code>Node.addMixin(String mixinName)</code> throws a
     * <code>NoSuchNodeTypeException</code> if <code>mixinName</code> is not the
     * name of an existing mixin node type
     */
    public void testAddNonExisting() throws RepositoryException {
        Session session = testRootNode.getSession();
        String nonExistingMixinName = NodeMixinUtil.getNonExistingMixinName(session);

        Node node = testRootNode.addNode(nodeName1, testNodeType);

        try {
            node.addMixin(nonExistingMixinName);
            fail("Node.addMixin(String mixinName) must throw a " +
                    "NoSuchNodeTypeException if mixinName is an unknown mixin type");
        } catch (NoSuchNodeTypeException e) {
            // success
        }
    }

    /**
     * Test if adding the same mixin twice works as expected.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     * @since JCR 2.0
     */
    public void testAddMixinTwice() throws RepositoryException, NotExecutableException {
        Session session = testRootNode.getSession();
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        String mixinName = NodeMixinUtil.getAddableMixinName(session, node);

        if (mixinName == null) {
            throw new NotExecutableException("No testable mixin node type found");
        }

        node.addMixin(mixinName);
        // adding again must succeed
        node.addMixin(mixinName);

        session.save();

        node.addMixin(mixinName);
        assertFalse(node.isModified());
    }

    /**
     * Test if adding an inherited mixin type has no effect.
     *
     * @throws RepositoryException
     * @since JCR 2.0
     */
    public void testAddInheritedMixin() throws RepositoryException, NotExecutableException {
        Session session = testRootNode.getSession();
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        session.save();

        String inheritedMixin = null;

        NodeType nt = node.getPrimaryNodeType();
        NodeType[] superTypes = nt.getSupertypes();
        for (int i = 0; i < superTypes.length && inheritedMixin == null; i++) {
            if (superTypes[i].isMixin()) {
                inheritedMixin = superTypes[i].getName();
            }
        }

        if (inheritedMixin != null) {
            node.addMixin(inheritedMixin);
            assertFalse(node.isModified());
        } else {
            throw new NotExecutableException("Primary node type does not have a mixin supertype");
        }
    }

    /**
     * Tests if <code>Node.addMixin(String mixinName)</code> throws a
     * <code>LockException</code> if <code>Node</code> is locked
     * <p>
     * The test creates a node <code>nodeName1</code> of type
     * <code>testNodeType</code> under <code>testRoot</code> and locks the node
     * with the superuser session. Then the test tries to add a mixin to
     * <code>nodeName1</code>  with the readWrite <code>Session</code>.
     */
    public void testLocked()
            throws NotExecutableException, RepositoryException {

        Session session = testRootNode.getSession();

        if (!isSupported(Repository.OPTION_LOCKING_SUPPORTED)) {
            throw new NotExecutableException("Locking is not supported.");
        }

        // create a node that is lockable
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        // or try to make it lockable if it is not
        ensureMixinType(node, mixLockable);
        testRootNode.getSession().save();

        String mixinName = NodeMixinUtil.getAddableMixinName(session, node);
        if (mixinName == null) {
            throw new NotExecutableException("No testable mixin node type found");
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
                node.addMixin(mixinName);
                node.save();
                fail("Node.addMixin(String mixinName) must throw a LockException " +
                        "if the node is locked.");
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
     * Tests if <code>Node.addMixin(String mixinName)</code> throws a
     * <code>VersionException</code> if <code>Node</code> is checked-in.
     * <p>
     * The test creates a node <code>nodeName1</code> of type
     * <code>testNodeType</code> under <code>testRoot</code> and checks it in.
     * Then the test tries to add a mixin to <code>nodeName1</code>.
     */
    public void testCheckedIn()
            throws NotExecutableException, RepositoryException {

        Session session = testRootNode.getSession();

        if (!isSupported(Repository.OPTION_VERSIONING_SUPPORTED)) {
            throw new NotExecutableException("Versioning is not supported.");
        }

        // create a node that is versionable
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        // or try to make it versionable if it is not
        ensureMixinType(node, mixVersionable);
        testRootNode.getSession().save();

        String mixinName = NodeMixinUtil.getAddableMixinName(session, node);
        if (mixinName == null || node.isNodeType(mixinName)) {
            throw new NotExecutableException("No testable mixin node type found");
        }

        node.checkin();

        try {
            node.addMixin(mixinName);
            fail("Node.addMixin(String mixinName) must throw a VersionException " +
                    "if the node is checked-in.");
        } catch (VersionException e) {
            // success
        }
    }

    /**
     * Tests if adding mix:referenceable automatically populates the jcr:uuid
     * value.
     */
    public void testAddMixinReferencable()
            throws NotExecutableException, RepositoryException {

        // check if repository supports references
        checkMixReferenceable();

        // get session an create default node
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        ensureMixinType(node, mixReferenceable);
        // implementation specific: mixin may take effect only upon save
        testRootNode.getSession().save();

        // check that it did
        assertTrue(node.isNodeType(mixReferenceable));

        // test if jcr:uuid is not null, empty or throws a exception
        // (format of value is not defined so we can only test if not empty)
        try {
            String uuid = node.getProperty(jcrUUID).getValue().getString();
            // default value is null so check for null
            assertNotNull("Acessing jcr:uuid after assginment of mix:referencable returned null", uuid);
            // check if it was not set to an empty string
            assertTrue("Acessing jcr:uuid after assginment of mix:referencable returned an empty String!", uuid.length() > 0);
        } catch (ValueFormatException e) {
            // trying to access the uuid caused an exception
            fail("Acessing jcr:uuid after assginment of mix:referencable caused an ValueFormatException!");
        }
    }


    /**
     * Checks if the repository supports the mixin mix:Referenceable otherwise a
     * {@link NotExecutableException} is thrown.
     *
     * @throws NotExecutableException if the repository does not support the
     *                                mixin mix:referenceable.
     */
    private void checkMixReferenceable() throws RepositoryException, NotExecutableException {
        try {
            superuser.getWorkspace().getNodeTypeManager().getNodeType(mixReferenceable);
        } catch (NoSuchNodeTypeException e) {
            throw new NotExecutableException("Repository does not support mix:referenceable");
        }
    }
}
