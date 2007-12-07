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
 * Test cases for {@link Item#isModified()} on a property.
 * <p/>
 * Configuration requirements:<br/>
 * The node at {@link #testRoot} must allow a child node of type
 * {@link #testNodeType} with name {@link #nodeName1}. The node type must
 * support a non-mandatory string property with name {@link #propertyName1}.
 *
 * @test
 * @sources PropertyItemIsModifiedTest.java
 * @executeClass org.apache.jackrabbit.test.api.PropertyItemIsModifiedTest
 * @keywords level2
 */
public class PropertyItemIsModifiedTest extends AbstractJCRTest {

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
     * Test if Item.isModified() returns false after a new PropertyItem is set
     * (before node is saved (transient). That means the PropertyItem don't exists
     * persistent).
     *
     * @see javax.jcr.Item#isModified()
     */
    public void testTransientNewPropertyItemIsModified () throws RepositoryException {
        Property testProperty = testNode.setProperty(propertyName1, "test");
        Item testPropertyItem = superuser.getItem(testProperty.getPath());
        // check testPropertyItem.isModified() for a new PropertyItem before save
        assertFalse("Item.isModified() must return false directly after a new PropertyItem is set (before current node is saved)", testPropertyItem.isModified());
    }

    /**
     * Test if Item.isModified() returns false after a new PropertyItem is set
     * and saved (persistent). That means the PropertyItem exists persistently but
     * isn't modified after save.
     * This is equivalent to the test if Item.isModified() returns false for an
     * already exixting and not modified PropertyItem.
     *
     * @see javax.jcr.Item#isModified()
     */
    public void testPersistentNewPropertyItemIsModified () throws RepositoryException {
        Property testProperty = testNode.setProperty(propertyName1, "test");
        testNode.save();
        Item testPropertyItem = superuser.getItem(testProperty.getPath());
        // check testPropertyItem.isModified() for a new PropertyItem after save
        assertFalse("Item.isModified() must return false after a new PropertyItem is set and current node is saved", testPropertyItem.isModified());
    }

    /**
     * Test if Item.isModified() returns true for an already existing and modified
     * PropertyItem (modifications aren't saved (transient)).
     *
     * @see javax.jcr.Item#isModified()
     */
    public void testTransientPropertyItemIsModified () throws RepositoryException {
        Property testProperty = testNode.setProperty(propertyName1, "test1");
        testNode.save();
        testProperty.setValue("test2");
        Item testPropertyItem = superuser.getItem(testProperty.getPath());
        // check testPropertyItem.isModified() before save
        assertTrue("Item.isModified() must return true directly after an existing Property is modified (before current node is saved)", testPropertyItem.isModified());
    }

    /**
     * Test if Item.isModified() returns false for an already exixting and modified
     * PropertyItem after the current node is saved (persistent).
     *
     * @see javax.jcr.Item#isModified()
     */
    public void testPersistentPropertyItemIsModified () throws RepositoryException {
        Property testProperty = testNode.setProperty(propertyName1, "test1");
        testNode.save();
        testProperty.setValue("test2");
        testNode.save();
        Item testPropertyItem = superuser.getItem(testProperty.getPath());
        // check testPropertyItem.isModified() after save
        assertFalse("Item.isModified() must return false after an existing Property is modified and the current Node is saved", testPropertyItem.isModified());
    }

}