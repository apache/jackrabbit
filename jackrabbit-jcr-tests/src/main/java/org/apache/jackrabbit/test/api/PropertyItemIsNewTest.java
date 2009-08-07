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

import javax.jcr.Item;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Node;

/**
 * Test cases for {@link Item#isNew()} on a property.
 * <p/>
 * Configuration requirements:<br/>
 * The node at {@link #testRoot} must allow a child node of type
 * {@link #testNodeType} with name {@link #nodeName1}. The node type must
 * support a non-mandatory string property with name {@link #propertyName1}.
 *
 * @test
 * @sources PropertyItemIsNewTest.java
 * @executeClass org.apache.jackrabbit.test.api.PropertyItemIsNewTest
 * @keywords level2
 */
public class PropertyItemIsNewTest extends AbstractJCRTest {

    protected Node testNode;

    protected void setUp() throws Exception {
        super.setUp();
        // build persistent node
        try {
            testNode = testRootNode.addNode(nodeName1, testNodeType);
            testRootNode.save();
        } catch (RepositoryException e) {
            fail("Failed to create test node." + e.getMessage());
        }
    }

    protected void tearDown() throws Exception {
        testNode = null;
        super.tearDown();
    }

    /**
     * Test if Item.isNew() returns true direct after a new PropertyItem is set
     * (before node is saved (transient)).
     *
     * @see javax.jcr.Item#isNew()
     */
    public void testTransientPropertyItemIsNew () throws RepositoryException {
        Property testProperty = testNode.setProperty(propertyName1, "test");
        Item testPropertyItem = superuser.getItem(testProperty.getPath());
        // check testPropertyItem.isNew() before save
        assertTrue("Item.isNew() must return true directly after a new Property is set (before current node is saved)", testPropertyItem.isNew());
    }

    /**
     * Test if Item.isNew() returns false after a new PropertyItem is set and
     * the node is saved (persistent).
     * This is equivalent to the test if Item.isNew() returns false for an
     * already exixting and not modified PropertyItem.
     *
     * @see javax.jcr.Item#isNew()
     */
    public void testPersistentPropertyItemIsNew () throws RepositoryException {
        Property testProperty = testNode.setProperty(propertyName1, "test");
        testNode.save();
        Item testPropertyItem = superuser.getItem(testProperty.getPath());
        // check testPropertyItem.isNew() after save
        assertFalse("Item.isNew() must return false after a new PropertyItem is set and the current Node is saved", testPropertyItem.isNew());
    }

}