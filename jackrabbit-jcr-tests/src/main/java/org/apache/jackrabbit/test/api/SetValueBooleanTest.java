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
 * node type {@link #testNodeType} must define a single value boolean property
 * with name {@link #propertyName1}.
 *
 * @test
 * @sources SetValueBooleanTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetValueBooleanTest
 * @keywords level2
 */
public class SetValueBooleanTest extends AbstractJCRTest {

    /**
     * The boolean value
     */
    private Value value;

    /**
     * The node with the boolean property
     */
    private Node node;

    /**
     * The boolean property
     */
    private Property property1;

    protected void setUp() throws Exception {
        super.setUp();

        // initialize some boolean value
        value = superuser.getValueFactory().createValue(true);

        // create a new node under the testRootNode
        node = testRootNode.addNode(nodeName1, testNodeType);

        // create a new single-value property and save it
        property1 = node.setProperty(propertyName1, false);
        superuser.save();
    }

    protected void tearDown() throws Exception {
        value = null;
        node = null;
        property1 = null;
        super.tearDown();
    }

    /**
     * Test the persistence of a property modified with an BooleanValue
     * parameter and saved from the Session
     */
    public void testBooleanSession() throws RepositoryException {
        property1.setValue(value);
        superuser.save();
        assertEquals("Boolean property not saved", value.getBoolean(), property1.getValue().getBoolean());
    }

    /**
     * Test the persistence of a property modified with an boolean parameter and
     * saved from the parent Node
     */
    public void testBooleanParent() throws RepositoryException {
        property1.setValue(value.getBoolean());
        node.save();
        assertEquals("Boolean property not saved", value.getBoolean(), property1.getValue().getBoolean());
    }

    /**
     * Test the deletion of a property by assigning it a null value, saved from
     * the Session
     */
    public void testRemoveBooleanSession() throws RepositoryException {
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
    public void testRemoveBooleanParent() throws RepositoryException {
        property1.setValue((Value) null);
        node.save();

        try {
            node.getProperty(propertyName1);
            fail("The property should not exist anymore, as a null Value has been assigned");
        } catch (PathNotFoundException e) {
            //success : the property has been deleted by assigning it a null value
        }
    }
}
