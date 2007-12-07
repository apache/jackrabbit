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
 * <code>SetPropertyDoubleTest</code> tests the <code>Node.setProperty(String,
 * double)</code> method
 *
 * @test
 * @sources SetPropertyDoubleTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetPropertyDoubleTest
 * @keywords level2
 */
public class SetPropertyDoubleTest extends AbstractJCRTest {

    private Node testNode;

    private double d1 = 1.23e20;
    private double d2 = 1.24e21;

    protected void setUp() throws Exception {
        super.setUp();
        testNode = testRootNode.addNode(nodeName1, testNodeType);

        // abort test if the repository does not allow setting
        // double properties on this node
        ensureCanSetProperty(testNode, propertyName1, testNode.getSession().getValueFactory().createValue(0.0d));
    }

    protected void tearDown() throws Exception {
        testNode = null;
        super.tearDown();
    }

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * double)</code> works with <code>Session.save()</code>
     */
    public void testNewDoublePropertySession() throws Exception {
        testNode.setProperty(propertyName1, d1);
        superuser.save();
        assertEquals("Setting property with Node.setProperty(String, double) and Session.save() not working",
                new Double(d1),
                new Double(testNode.getProperty(propertyName1).getDouble()));
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * double)</code> works with <code>Session.save()</code>
     */
    public void testModifyDoublePropertySession() throws Exception {
        testNode.setProperty(propertyName1, d1);
        superuser.save();
        testNode.setProperty(propertyName1, d2);
        superuser.save();
        assertEquals("Modifying property with Node.setProperty(String, double) and Session.save() not working",
                new Double(d2),
                new Double(testNode.getProperty(propertyName1).getDouble()));
    }

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * double)</code> works with <code>parentNode.save()</code>
     */
    public void testNewDoublePropertyParent() throws Exception {
        testNode.setProperty(propertyName1, d1);
        testRootNode.save();
        assertEquals("Setting property with Node.setProperty(String, double) and parentNode.save() not working",
                new Double(d1),
                new Double(testNode.getProperty(propertyName1).getDouble()));
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * double)</code> works with <code>parentNode.save()</code>
     */
    public void testModifyDoublePropertyParent() throws Exception {
        testNode.setProperty(propertyName1, d1);
        testRootNode.save();
        testNode.setProperty(propertyName1, d2);
        testRootNode.save();
        assertEquals("Modifying property with Node.setProperty(String, double) and parentNode.save() not working",
                new Double(d2),
                new Double(testNode.getProperty(propertyName1).getDouble()));
    }

    /**
     * Tests if removing a <code>double</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>Session.save()</code>
     */
    public void testRemoveDoublePropertySession() throws Exception {
        testNode.setProperty(propertyName1, d1);
        superuser.save();
        testNode.setProperty(propertyName1, (Value) null);
        superuser.save();
        assertFalse("Removing double property with Node.setProperty(String, null) and Session.save() not working",
                testNode.hasProperty(propertyName1));
    }

    /**
     * Tests if removing a <code>double</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>parentNode.save()</code>
     */
    public void testRemoveDoublePropertyParent() throws Exception {
        testNode.setProperty(propertyName1, d1);
        testRootNode.save();
        testNode.setProperty(propertyName1, (Value) null);
        testRootNode.save();
        assertFalse("Removing double property with Node.setProperty(String, null) and parentNode.save() not working",
                testNode.hasProperty(propertyName1));
    }

}