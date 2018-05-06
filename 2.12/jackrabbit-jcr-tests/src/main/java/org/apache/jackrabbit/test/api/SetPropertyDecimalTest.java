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

import javax.jcr.Node;
import javax.jcr.Value;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * <code>SetPropertyDecmimalTest</code> tests the <code>Node.setProperty(String,
 * BigDecimal)</code> method
 *
 */
public class SetPropertyDecimalTest extends AbstractJCRTest {

    private Node testNode;

    private BigDecimal b1 = new BigDecimal("-123545678901234567890123545678901234567890.123545678901234567890");
    private BigDecimal b2 = new BigDecimal("123545678901234567890123545678901234567890.123545678901234567890");

    protected void setUp() throws Exception {
        super.setUp();
        testNode = testRootNode.addNode(nodeName1, testNodeType);
        // abort test if the repository does not allow setting
        // decimal properties on this node
        ensureCanSetProperty(testNode, propertyName1, testNode.getSession().getValueFactory().createValue(new BigDecimal(0)));
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Tests if adding a property with <code>Node.setProperty(String,
     * BigDecimal)</code> works with <code>Session.save()</code>
     */
    public void testNewDecimalPropertySession() throws Exception {
        testNode.setProperty(propertyName1, b1);
        superuser.save();
        assertEquals("Setting property with Node.setProperty(String, double) and Session.save() not working",
                b1,
                testNode.getProperty(propertyName1).getDecimal());
    }

    /**
     * Tests if modifying a property with <code>Node.setProperty(String,
     * BigDecimal)</code> works with <code>Session.save()</code>
     */
    public void testModifyDecimalPropertySession() throws Exception {
        testNode.setProperty(propertyName1, b1);
        superuser.save();
        testNode.setProperty(propertyName1, b2);
        superuser.save();
        assertEquals("Modifying property with Node.setProperty(String, double) and Session.save() not working",
                b2,
                testNode.getProperty(propertyName1).getDecimal());
    }

    /**
     * Tests if removing a <code>BigDecimal</code> property with
     * <code>Node.setProperty(String, null)</code> works with
     * <code>Session.save()</code>
     */
    public void testRemoveDecimalPropertySession() throws Exception {
        testNode.setProperty(propertyName1, b1);
        testRootNode.getSession().save();
        testNode.setProperty(propertyName1, (Value) null);
        testRootNode.getSession().save();
        assertFalse("Removing decimal property with Node.setProperty(String, null) and Session.save() not working",
                testNode.hasProperty(propertyName1));
    }
}
