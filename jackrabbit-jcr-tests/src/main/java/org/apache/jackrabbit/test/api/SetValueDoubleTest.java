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
 * node type {@link #testNodeType} must define a single value double property
 * with name {@link #propertyName1}.
 *
 * @test
 * @sources SetValueDoubleTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetValueDoubleTest
 * @keywords level2
 */
public class SetValueDoubleTest extends AbstractJCRTest {

    /**
     * The double value
     */
    private Value value;

    /**
     * The node with the double property
     */
    private Node node;

    /**
     * The double property
     */
    private Property property1;

    protected void setUp() throws Exception {
        super.setUp();

        // initialize some double value
        value = superuser.getValueFactory().createValue(93845.94d);

        // create a new node under the testRootNode
        node = testRootNode.addNode(nodeName1, testNodeType);

        // abort test if the repository does not allow setting
        // double properties on this node
        ensureCanSetProperty(node, propertyName1, node.getSession().getValueFactory().createValue(0.0d));

        // create a new single-value property and save it
        property1 = node.setProperty(propertyName1, 0.0d);
        superuser.save();
    }

    protected void tearDown() throws Exception {
        value = null;
        node = null;
        property1 = null;
        super.tearDown();
    }

    /**
     * Test the persistence of a property modified with an double parameter and
     * saved from the Session
     */
    public void testDoubleSession() throws RepositoryException {
        property1.setValue(value);
        superuser.save();
        assertEquals("Double node property not saved", value.getDouble(), property1.getValue().getDouble(), 0);
    }

    /**
     * Test the persistence of a property modified with an double parameter and
     * saved from the parent Node
     */
    public void testDoubleParent() throws RepositoryException {
        property1.setValue(value.getDouble());
        node.save();
        assertEquals("Double node property not saved", value.getDouble(), property1.getValue().getDouble(), 0);
    }

    /**
     * Test the deletion of a property by assigning it a null value, saved from
     * the Session
     */
    public void testRemoveDoubleSession() throws RepositoryException {
        property1.setValue((Value) null);
        superuser.save();

        try {
            node.getProperty(propertyName1);
            fail("The property should not exist anymore, as a null Double has been assigned");
        } catch (PathNotFoundException e) {
            //success : the property has been deleted by assigning it a null value
        }
    }

    /**
     * Test the deletion of a property by assigning it a null value, saved from
     * the parent Node
     */
    public void testRemoveDoubleParent() throws RepositoryException {
        property1.setValue((Value) null);
        node.save();

        try {
            node.getProperty(propertyName1);
            fail("The property should not exist anymore, as a null Double has been assigned");
        } catch (PathNotFoundException e) {
            //success : the property has been deleted by assigning it a null value
        }
    }
}
