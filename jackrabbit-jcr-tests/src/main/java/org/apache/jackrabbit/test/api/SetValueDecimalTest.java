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

import java.math.BigDecimal;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;

/**
 * Tests the various {@link Property#setValue(Value)} methods.
 * <p>
 * Configuration requirements:
 * <p>
 * The node at {@link #testRoot} must allow a
 * child node of type {@link #testNodeType} with name {@link #nodeName1}. The
 * node type {@link #testNodeType} must define a single value decimal property
 * with name {@link #propertyName1}.
 *
 */
public class SetValueDecimalTest extends AbstractJCRTest {

    /**
     * The decimal value
     */
    private Value value;

    /**
     * The node with the decimal property
     */
    private Node node;

    /**
     * The decimal property
     */
    private Property property1;

    protected void setUp() throws Exception {
        super.setUp();

        // initialize some decimal value
        value = superuser.getValueFactory().createValue(new BigDecimal("457841848484454646544884.484984949849498489771174"));

        // create a new node under the testRootNode
        node = testRootNode.addNode(nodeName1, testNodeType);

        // abort test if the repository does not allow setting
        // decimal properties on this node
        ensureCanSetProperty(node, propertyName1, node.getSession().getValueFactory().createValue(new BigDecimal(0)));

        // create a new single-value property and save it
        property1 = node.setProperty(propertyName1, new BigDecimal(0));
        superuser.save();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test the persistence of a property modified with an decimal parameter and
     * saved from the Session
     */
    public void testDoubleValueSession() throws RepositoryException {
        property1.setValue(value);
        superuser.save();
        assertEquals("Decimal node property not saved", value.getDecimal(), property1.getValue().getDecimal());
    }

    /**
     * Test the persistence of a property modified with an decimal parameter and
     * saved from the Session
     */
    public void testDoubleSession() throws RepositoryException {
        property1.setValue(value.getDecimal());
        superuser.save();
        assertEquals("Decimal node property not saved", value.getDecimal(), property1.getValue().getDecimal());
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
            fail("The property should not exist anymore, as a null Decimal has been assigned");
        } catch (PathNotFoundException e) {
            //success : the property has been deleted by assigning it a null value
        }
    }

}
