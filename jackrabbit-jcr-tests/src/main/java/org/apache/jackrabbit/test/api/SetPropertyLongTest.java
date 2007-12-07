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
 * <code>SetPropertyLongTest</code> tests the <code>Node.setProperty(String,
 * long)</code> method
 *
 * @test
 * @sources SetPropertyLongTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetPropertyLongTest
 * @keywords level2
 */
public class SetPropertyLongTest extends AbstractJCRTest {

    private Node testNode;

    private long l1 = 2147483650L;
    private long l2 = -2147483800L;

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
     * long)</code> works with <code>Session.save()</code>
     */
    public void testNewLongPropertySession() throws Exception {
        testNode.setProperty(propertyName1, l1);
        superuser.save();
        assertEquals("Setting property with Node.setProperty(String, long) and Session.save() not working",
                new Long(l1),
                new Long(testNode.getProperty(propertyName1).getLong()));
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * long)</code> works with <code>Session.save()</code>
     */
    public void testModifyLongPropertySession() throws Exception {
        testNode.setProperty(propertyName1, l1);
        superuser.save();
        testNode.setProperty(propertyName1, l2);
        superuser.save();
        assertEquals("Modifying property with Node.setProperty(String, long) and Session.save() not working",
                new Long(l2),
                new Long(testNode.getProperty(propertyName1).getLong()));
    }

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * long)</code> works with <code>parentNode.save()</code>
     */
    public void testNewLongPropertyParent() throws Exception {
        testNode.setProperty(propertyName1, l1);
        testRootNode.save();
        assertEquals("Setting property with Node.setProperty(String, long) and parentNode.save() not working",
                new Long(l1),
                new Long(testNode.getProperty(propertyName1).getLong()));
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * long)</code> works with <code>parentNode.save()</code>
     */
    public void testModifyLongPropertyParent() throws Exception {
        testNode.setProperty(propertyName1, l1);
        testRootNode.save();
        testNode.setProperty(propertyName1, l2);
        testRootNode.save();
        assertEquals("Modifying property with Node.setProperty(String, long) and parentNode.save() not working",
                new Long(l2),
                new Long(testNode.getProperty(propertyName1).getLong()));
    }

    /**
     * Tests if removing a <code>long</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>Session.save()</code>
     */
    public void testRemoveLongPropertySession() throws Exception {
        testNode.setProperty(propertyName1, l1);
        superuser.save();
        testNode.setProperty(propertyName1, (Value) null);
        superuser.save();
        assertFalse("Removing long property with Node.setProperty(String, null) and Session.save() not working",
                testNode.hasProperty(propertyName1));
    }

    /**
     * Tests if removing a <code>long</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>parentNode.save()</code>
     */
    public void testRemoveLongPropertyParent() throws Exception {
        testNode.setProperty(propertyName1, l1);
        testRootNode.save();
        testNode.setProperty(propertyName1, (Value) null);
        testRootNode.save();
        assertFalse("Removing long property with Node.setProperty(String, null) and parentNode.save() not working",
                testNode.hasProperty(propertyName1));
    }

}