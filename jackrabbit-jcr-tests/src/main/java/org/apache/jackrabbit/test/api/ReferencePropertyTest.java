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

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.PropertyIterator;
import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * Tests a reference property. If the workspace does not contain a node with
 * a reference property a {@link org.apache.jackrabbit.test.NotExecutableException}
 * is thrown.
 *
 * @test
 * @sources ReferencePropertyTest.java
 * @executeClass org.apache.jackrabbit.test.api.ReferencePropertyTest
 * @keywords level1
 */
public class ReferencePropertyTest extends AbstractPropertyTest {

    /** The target of the reference */
    private Node referencedNode;

    /**
     * Sets up the fixture for the test.
     */
    protected void setUp() throws Exception {
        super.setUp();
        referencedNode = prop.getNode();
    }

    protected void tearDown() throws Exception {
        referencedNode = null;
        super.tearDown();
    }

    /**
     * Returns {@link javax.jcr.PropertyType#REFERENCE}.
     * @return {@link javax.jcr.PropertyType#REFERENCE}.
     */
    protected int getPropertyType() {
        return PropertyType.REFERENCE;
    }

    /**
     * Returns {@link Boolean#FALSE}.
     * @return {@link Boolean#FALSE}.
     */
    protected Boolean getPropertyIsMultivalued() {
        return Boolean.FALSE;
    }

    /**
     * Tests if the referenced node is of nodeType mix:referenceable.
     */
    public void testNodeType() throws RepositoryException {
        assertTrue("Property " + prop.getName() + " refers to a node " +
                "which is not of NodeType mix:referenceable.",
                referencedNode.isNodeType(mixReferenceable));
    }

    /**
     * Tests if the referenced node has this property in its referers list in
     * case the property is not transient. Also tests in theis case that the
     * node retrieved by property.getNode() is the same as the one retrieved by
     * session.getNodeByUUID() .
     */
    public void testPropValue() throws RepositoryException {
        Node referenced = session.getNodeByUUID(prop.getString());
        PropertyIterator referers = referenced.getReferences();
        if (!prop.isNew()) {
            boolean found = false;
            while (referers.hasNext()) {
                Property propp = referers.nextProperty();
                if (propp.isSame(prop)) {
                    found = true;
                }
            }
            assertTrue("Referencing property of node " + referenced.getName() +
                    " not found.", found);
            assertTrue("Referenced node retrieved with getNode is different " +
                    "from the node retrieved with getNodeByUUID",
                    referenced.isSame(referencedNode));
        } else {
            log.println("Reference property " + prop.getName() + " is in transient state.");
        }
    }

    /**
     * Tests failure of conversion from Reference type to Boolean type.
     */
    public void testGetBoolean() throws RepositoryException {
        try {
            Value val = PropertyUtil.getValue(prop);
            val.getBoolean();
            fail("Conversion from a Reference value to a Boolean value " +
                    "should throw a ValueFormatException.");
        } catch (ValueFormatException vfe) {
            //ok
        }
    }

    /**
     * Tests failure of conversion from Reference type to Date type.
     */
    public void testGetDate() throws RepositoryException {
        try {
            Value val = PropertyUtil.getValue(prop);
            val.getDate();
            fail("Conversion from a Reference value to a Date value " +
                    "should throw a ValueFormatException.");
        } catch (ValueFormatException vfe) {
            //ok
        }
    }

    /**
     * Tests failure from Reference type to Double type.
     */
    public void testGetDouble() throws RepositoryException {
        try {
            Value val = PropertyUtil.getValue(prop);
            val.getDouble();
            fail("Conversion from a Reference value to a Double value " +
                    "should throw a ValueFormatException.");
        } catch (ValueFormatException vfe) {
            //ok
        }
    }

    /**
     * Tests failure of conversion from Reference type to Long type.
     */
    public void testGetLong() throws RepositoryException {
        try {
            Value val = PropertyUtil.getValue(prop);
            val.getLong();
            fail("Conversion from a Reference value to a Long value " +
                    "should throw a ValueFormatException.");
        } catch (ValueFormatException vfe) {
            //ok
        }
    }

    /**
     * Tests conversion from Reference type to String type.
     */
    public void testGetString() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        // format of reference value is implementation specifc. but at least
        // must not throw
        val.getString();
    }

    /**
     * Tests if Value.getType() returns the same as Property.getType() and also
     * tests that prop.getDefinition().getRequiredType() returns the same type
     * in case it is not of Undefined type.
     */
    public void testGetType() throws RepositoryException {
        assertTrue("Value.getType() returns wrong type.", PropertyUtil.checkGetType(prop, PropertyType.REFERENCE));
    }

    /**
     * Tests equals method of Reference value.
     */
    public void testEquals() throws RepositoryException {
        Property prop2 = referencedNode.getProperty(jcrUUID);
        assertTrue("Incorrect equals method of Reference value.",
                PropertyUtil.equalValues(prop2.getValue(), prop.getValue()));
    }
}
