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

import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;

/**
 * Tests the various {@link Property#setValue(Value)} methods.
 * <p/>
 * Configuration requirements:<br/> The node at {@link #testRoot} must allow a
 * child node of type {@link #testNodeType} with name {@link #nodeName1}. The
 * node type {@link #testNodeType} must define a single value reference property
 * with name {@link #propertyName1}. The node type {@link #testNodeType} must
 * be referenceable or allow to add a mix:referenceable, otherwise a
 * {@link NotExecutableException} is thrown.
 *
 * @test
 * @sources SetValueReferenceTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetValueReferenceTest
 * @keywords level2
 */
public class SetValueReferenceTest extends AbstractJCRTest {

    /**
     * The reference value
     */
    private Value value;

    /**
     * The node with the reference property
     */
    private Node node;

    /**
     * The reference property
     */
    private Property property1;

    /**
     * @throws NotExecutableException if the node is not referenceable and does
     *                                not allow to add a mix:referenceable.
     */
    protected void setUp() throws Exception {
        super.setUp();

        // create a new node under the testRootNode
        node = testRootNode.addNode(nodeName1, testNodeType);
        ensureReferenceable(node);

        // initialize some reference value
        value = superuser.getValueFactory().createValue(node);

        // create a new single-value property and save it
        property1 = node.setProperty(propertyName1, value);
        superuser.save();
    }

    protected void tearDown() throws Exception {
        value = null;
        node = null;
        property1 = null;
        super.tearDown();
    }

    /**
     * Test the persistence of a property modified with an Node parameter and
     * saved from the Session Requires a Node value (node)
     */
    public void testNodeSession() throws RepositoryException, NotExecutableException {
        property1.setValue(node);
        superuser.save();
        assertTrue("Reference property not saved", node.isSame(property1.getNode()));
    }

    /**
     * Test the persistence of a property modified with an Node parameter and
     * saved from the parent Node Requires a Node value (node)
     */
    public void testNodeParent() throws RepositoryException {
        property1.setValue(node);
        node.save();
        assertTrue("Reference property not saved", node.isSame(property1.getNode()));
    }

    /**
     * Test the deletion of a property by assigning it a null value, saved from
     * the Session
     */
    public void testRemoveNodeSession() throws RepositoryException {
        property1.setValue((Value) null);
        superuser.save();

        try {
            node.getProperty(propertyName1);
            fail("The property should not exist anymore, as a null Value has been assigned");
        } catch (PathNotFoundException e) {
            //success : the property has been deleted by assigning it a null value
        }
    }

    /**
     * Test the deletion of a property by assigning it a null value, saved from
     * the parent Node
     */
    public void testRemoveNodeParent() throws RepositoryException {
        property1.setValue((Node) null);
        node.save();

        try {
            node.getProperty(propertyName1);
            fail("The property should not exist anymore, as a null Value has been assigned");
        } catch (PathNotFoundException e) {
            //success : the property has been deleted by assigning it a null value
        }
    }

    //-------------------< internal >-------------------------------------------

    /**
     * Ensures that node <code>n</code> is referenceable. If the node is not of
     * type mix:referenceable this method tries to add the mixin
     * mix:referenceable. If the node does not allow to add the mixin, or the
     * repository does not support referenceable nodes a {@link
     * NotExecutableException} is thrown.
     *
     * @param n the node to check.
     * @throws NotExecutableException if the node cannot be made referenceable
     *                                or if the repository does not support
     *                                referenceable nodes.
     */
    private void ensureReferenceable(Node n) throws RepositoryException, NotExecutableException {
        if (n.isNodeType(mixReferenceable)) {
            return;
        }
        if (n.canAddMixin(mixReferenceable)) {
            n.addMixin(mixReferenceable);
            // some implementations may require a save after addMixin()
            n.getSession().save();
        } else {
            throw new NotExecutableException("Node is not referenceable: " + n.getPath());
        }
    }
}
