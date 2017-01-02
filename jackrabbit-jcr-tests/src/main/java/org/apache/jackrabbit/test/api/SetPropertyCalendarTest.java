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

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * <code>SetPropertyCalendarTest</code> tests the <code>Node.setProperty(String,
 * Calendar)</code> method
 *
 */
public class SetPropertyCalendarTest extends AbstractJCRTest {

    private Node testNode;

    private Calendar c1 = new GregorianCalendar(2005, 1, 10, 14, 8, 56);
    private Calendar c2 = new GregorianCalendar(1945, 1, 6, 16, 20, 0);

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
     * Calendar)</code> works with <code>Session.save()</code>
     */
    public void testNewCalendarPropertySession() throws Exception {
        testNode.setProperty(propertyName1, c1);
        superuser.save();
        assertEquals("Setting property with Node.setProperty(String, Calendar) and Session.save() not working",
                vf.createValue(c1),
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
                vf.createValue(c2),
                testNode.getProperty(propertyName1).getValue());
    }

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * Calendar)</code> works with <code>parentNode.save()</code>
     */
    public void testNewCalendarPropertyParent() throws Exception {
        testNode.setProperty(propertyName1, c1);
        testRootNode.getSession().save();
        assertEquals("Setting property with Node.setProperty(String, Calendar) and parentNode.save() not working",
                vf.createValue(c1),
                testNode.getProperty(propertyName1).getValue());
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * Calendar)</code> works with <code>parentNode.save()</code>
     */
    public void testModifyCalendarPropertyParent() throws Exception {
        testNode.setProperty(propertyName1, c1);
        testRootNode.getSession().save();
        testNode.setProperty(propertyName1, c2);
        testRootNode.getSession().save();
        assertEquals("Modifying property with Node.setProperty(String, Calendar) and parentNode.save() not working",
                vf.createValue(c2),
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
        testRootNode.getSession().save();
        testNode.setProperty(propertyName1, (Calendar) null);
        testRootNode.getSession().save();
        assertFalse("Removing property with Node.setProperty(String, (Calendar)null) and parentNode.save() not working",
                testNode.hasProperty(propertyName1));
    }

}
