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

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;

/**
 * <code>PropertyTest</code> contains all test cases for the
 * <code>javax.jcr.Property</code> that are related to writing, modifing or
 * deleting properties (level 2 of the specification).
 *
 * @tck.config nodetype name of a node type. The node at <code>testroot</code>
 * must allow child nodes with this node.
 * @tck.config nodename1 name of a child node at <code>testroot</code>.
 * @tck.config propertyname1 name of a string property in
 * <code>nodetype</code>.
 * 
 * @test
 * @sources PropertyTest.java
 * @executeClass org.apache.jackrabbit.test.api.PropertyTest
 * @keywords level2
 */
public class PropertyTest extends AbstractJCRTest {

    /**
     * Tests if <code>Item.isSame(Item otherItem)</code> will return true when
     * two <code>Property</code> objects representing the same actual repository
     * item have been retrieved through two different sessions and one has been
     * modified.
     */
    public void testIsSameMustNotCompareStates()
            throws RepositoryException {

        // create a node, add a property and save it
        Node testNode1 = testRootNode.addNode(nodeName1, testNodeType);
        Property prop1 = testNode1.setProperty(propertyName1, "value1");
        testRootNode.save();

        // accuire the same property through a different session
        Session session = helper.getSuperuserSession();
        try {
            Property prop2 = (Property) session.getItem(prop1.getPath());

            // change the value of prop2
            prop2.setValue("value2");

            assertTrue("Two references of same property must return true for " +
                    "property1.isSame(property2)", prop1.isSame(prop2));
        } finally {
            session.logout();
        }
    }
}