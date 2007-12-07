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

import javax.jcr.Value;
import javax.jcr.Node;

/**
 * <code>SetPropertyBooleanTest</code> tests the <code>Node.setProperty(String,
 * boolean)</code> method
 *
 * @test
 * @sources SetPropertyBooleanTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetPropertyBooleanTest
 * @keywords level2
 */
public class SetPropertyBooleanTest extends AbstractJCRTest {

    private Node testNode;

    protected void setUp() throws Exception {
        super.setUp();
        testNode = testRootNode.addNode(nodeName1, testNodeType);
    }

    protected void tearDown() throws Exception {
        testNode = null;
        super.tearDown();
    }

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * boolean)</code> works with <code>Session.save()</code>
     */
    public void testNewBooleanPropertySession() throws Exception {
        testNode.setProperty(propertyName1, true);
        superuser.save();
        assertEquals("Setting property with Node.setProperty(String, boolean) and Session.save() not working",
                true,
                testNode.getProperty(propertyName1).getBoolean());
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * boolean)</code> works with <code>Session.save()</code>
     */
    public void testModifyBooleanPropertySession() throws Exception {
        testNode.setProperty(propertyName1, true);
        superuser.save();
        testNode.setProperty(propertyName1, false);
        superuser.save();
        assertEquals("Modifying property with Node.setProperty(String, boolean) and Session.save() not working",
                false,
                testNode.getProperty(propertyName1).getBoolean());
    }

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * boolean)</code> works with <code>parentNode.save()</code>
     */
    public void testNewBooleanPropertyParent() throws Exception {
        testNode.setProperty(propertyName1, true);
        testRootNode.save();
        assertEquals("Setting property with Node.setProperty(String, boolean) and parentNode.save() not working",
                true,
                testNode.getProperty(propertyName1).getBoolean());
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * boolean)</code> works with <code>parentNode.save()</code>
     */
    public void testModifyBooleanPropertyParent() throws Exception {
        testNode.setProperty(propertyName1, true);
        testRootNode.save();
        testNode.setProperty(propertyName1, false);
        testRootNode.save();
        assertEquals("Modifying property with Node.setProperty(String, boolean) and parentNode.save() not working",
                false,
                testNode.getProperty(propertyName1).getBoolean());
    }

    /**
     * Tests if removing a <code>boolean</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>Session.save()</code>
     */
    public void testRemoveBooleanPropertySession() throws Exception {
        testNode.setProperty(propertyName1, true);
        superuser.save();
        testNode.setProperty(propertyName1, (Value) null);
        superuser.save();
        assertFalse("Removing boolean property with Node.setProperty(String, null) and Session.save() not working",
                testNode.hasProperty(propertyName1));
    }

    /**
     * Tests if removing a <code>boolean</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>parentNode.save()</code>
     */
    public void testRemoveBooleanPropertyParent() throws Exception {
        testNode.setProperty(propertyName1, true);
        testRootNode.save();
        testNode.setProperty(propertyName1, (Value) null);
        testRootNode.save();
        assertFalse("Removing boolean property with Node.setProperty(String, null) and parentNode.save() not working",
                testNode.hasProperty(propertyName1));
    }

}