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

import javax.jcr.Node;
import javax.jcr.ValueFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * <code>SetPropertyCalendarTest</code> tests the <code>Node.setProperty(String,
 * Calendar)</code> method
 *
 * @test
 * @sources SetPropertyCalendarTest.java
 * @executeClass org.apache.jackrabbit.test.api.SetPropertyCalendarTest
 * @keywords level2
 */
public class SetPropertyCalendarTest extends AbstractJCRTest {

    private Node testNode;

    private ValueFactory vFactory;

    private Calendar c1 = new GregorianCalendar(2005, 1, 10, 14, 8, 56);
    private Calendar c2 = new GregorianCalendar(1945, 1, 6, 16, 20, 0);

    protected void setUp() throws Exception {
        super.setUp();
        testNode = testRootNode.addNode(nodeName1, testNodeType);
        vFactory = superuser.getValueFactory();
    }

    protected void tearDown() throws Exception {
        testNode = null;
        vFactory = null;
        super.tearDown();
    }

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * Calendar)</code> works with <code>Session.save()</code>
     */
    public void testNewCalendarPropertySession() throws Exception {
        testNode.setProperty(propertyName1, c1);
        superuser.save();
        assertEquals("Setting property with Node.setProperty(String, Calendar) and Session.save() not working",
                vFactory.createValue(c1),
                testNode.getProperty(propertyName1).getValue());
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * Calendar)</code> works with <code>Session.save()</code>
     */
    public void testModifyCalendarPropertySession() throws Exception {
        testNode.setProperty(propertyName1, c1);
        superuser.save();
        testNode.setProperty(propertyName1, c2);
        superuser.save();
        assertEquals("Modifying property with Node.setProperty(String, Calendar) and Session.save() not working",
                vFactory.createValue(c2),
                testNode.getProperty(propertyName1).getValue());
    }

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * Calendar)</code> works with <code>parentNode.save()</code>
     */
    public void testNewCalendarPropertyParent() throws Exception {
        testNode.setProperty(propertyName1, c1);
        testRootNode.save();
        assertEquals("Setting property with Node.setProperty(String, Calendar) and parentNode.save() not working",
                vFactory.createValue(c1),
                testNode.getProperty(propertyName1).getValue());
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * Calendar)</code> works with <code>parentNode.save()</code>
     */
    public void testModifyCalendarPropertyParent() throws Exception {
        testNode.setProperty(propertyName1, c1);
        testRootNode.save();
        testNode.setProperty(propertyName1, c2);
        testRootNode.save();
        assertEquals("Modifying property with Node.setProperty(String, Calendar) and parentNode.save() not working",
                vFactory.createValue(c2),
                testNode.getProperty(propertyName1).getValue());
    }

    /**
     * Tests if removing a <code>Calendar</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>Session.save()</code>
     */
    public void testRemoveCalendarPropertySession() throws Exception {
        testNode.setProperty(propertyName1, c1);
        superuser.save();
        testNode.setProperty(propertyName1, (Calendar) null);
        superuser.save();
        assertFalse("Removing property with Node.setProperty(String, (Calendar)null) and Session.save() not working",
                testNode.hasProperty(propertyName1));
    }

    /**
     * Tests if removing a <code>Calendar</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>parentNode.save()</code>
     */
    public void testRemoveCalendarPropertyParent() throws Exception {
        testNode.setProperty(propertyName1, c1);
        testRootNode.save();
        testNode.setProperty(propertyName1, (Calendar) null);
        testRootNode.save();
        assertFalse("Removing property with Node.setProperty(String, (Calendar)null) and parentNode.save() not working",
                testNode.hasProperty(propertyName1));
    }

}