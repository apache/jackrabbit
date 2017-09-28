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

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.PropertyType;
import javax.jcr.Node;
import javax.jcr.Property;

/**
 * Tests a path property. If the workspace does not contain a node with a path
 * property a {@link org.apache.jackrabbit.test.NotExecutableException} is
 * thrown.
 *
 */
public class PathPropertyTest extends AbstractPropertyTest {

    /**
     * Returns {@link javax.jcr.PropertyType#PATH}.
     *
     * @return {@link javax.jcr.PropertyType#PATH}.
     */
    protected int getPropertyType() {
        return PropertyType.PATH;
    }

    /**
     * Returns "does not matter" (<code>null</code>).
     * @return <code>null</code>.
     */
    protected Boolean getPropertyIsMultivalued() {
        return null;
    }

    /**
     * Tests conversion from Path type to String type and if the resulting
     * string has correct format.
     */
    public void testGetString() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        assertTrue("Not a valid Path property: " + prop.getName(),
                PropertyUtil.checkPathFormat(val.getString(), session));
    }

    /**
     * Tests failure of conversion from Path type to Boolean type.
     */
    public void testGetBoolean() throws RepositoryException {
        try {
            Value val = PropertyUtil.getValue(prop);
            val.getBoolean();
            fail("Conversion from a Path value to a Boolean value " +
                    "should throw a ValueFormatException.");
        } catch (ValueFormatException vfe) {
            //ok
        }
    }

    /**
     * Tests failure of conversion from Path type to Date type.
     */
    public void testGetDate() throws RepositoryException {
        try {
            Value val = PropertyUtil.getValue(prop);
            val.getDate();
            fail("Conversion from a Path value to a Date value " +
                    "should throw a ValueFormatException.");
        } catch (ValueFormatException vfe) {
            //ok
        }
    }

    /**
     * Tests failure from Path type to Double type.
     */
    public void testGetDouble() throws RepositoryException {
        try {
            Value val = PropertyUtil.getValue(prop);
            val.getDouble();
            fail("Conversion from a Path value to a Double value " +
                    "should throw a ValueFormatException.");
        } catch (ValueFormatException vfe) {
            //ok
        }
    }

    /**
     * Tests failure of conversion from Path type to Long type.
     */
    public void testGetLong() throws RepositoryException {
        try {
            Value val = PropertyUtil.getValue(prop);
            val.getLong();
            fail("Conversion from a Path value to a Long value " +
                    "should throw a ValueFormatException.");
        } catch (ValueFormatException vfe) {
            //ok
        }
    }

    /**
     * Tests if Value.getType() returns the same as Property.getType() and also
     * tests that prop.getDefinition().getRequiredType() returns the same type
     * in case it is not of Undefined type.
     */
    public void testGetType() throws RepositoryException {
        assertTrue("Value.getType() returns wrong type.",
                PropertyUtil.checkGetType(prop, PropertyType.PATH));
    }

    /**
     * Since JCR 2.0 a path property can be dereferenced if it points to a
     * Node.
     * TODO: create several tests out of this one
     */
    public void testGetNode() throws RepositoryException {
        if (!multiple) {
            String nodePath = prop.getParent().getPath();
            String propName = prop.getName();

            // absolute nodes path
            prop.getParent().setProperty(propName, nodePath, PropertyType.PATH);
            String value = prop.getString();
            Node n = prop.getNode();
            assertEquals("The path of the dereferenced property must be equal to the value", n.getPath(), value);
            assertTrue("The property value must be resolved to the correct node.", prop.getParent().isSame(n));

            // relative node path
            prop.getParent().setProperty(propName, ".", PropertyType.PATH);
            n = prop.getNode();
            assertTrue("The property value must be resolved to the correct node.", prop.getParent().getNode(".").isSame(n));

            // non-existing property path
            while (session.nodeExists(nodePath)) {
                nodePath += "x";
            }
            prop.getParent().setProperty(propName, nodePath, PropertyType.PATH);
            try {
                prop.getNode();
                fail("Calling Property.getNode() for a PATH value that doesn't have a corresponding Node, ItemNotFoundException is expected");
            } catch (ItemNotFoundException e) {
                //ok
            }
        } else {
            try {
                prop.getNode();
                fail("Property.getNode() called on a multivalue property " +
                        "should throw a ValueFormatException.");
            } catch (ValueFormatException vfe) {
                //ok
            }
        }
    }

    /**
     * Since JCR 2.0 a path property can be dereferenced if it points to a
     * Property.
     * TODO: create several tests out of this one
     */
    public void testGetProperty() throws RepositoryException {
        if (!multiple) {
            String propPath = prop.getPath();
            String propName = prop.getName();

            // absolute property path
            prop.getParent().setProperty(propName, propPath, PropertyType.PATH);
            String path = prop.getString();
            Property p = prop.getProperty();
            assertEquals("The path of the dereferenced property must be equal to the value", path, p.getPath());
            assertTrue("The property value must be resolved to the correct property.", prop.isSame(p));

            // relative property path
            prop.getParent().setProperty(propName, propName, PropertyType.PATH);
            path = prop.getString();
            p = prop.getProperty();
            assertEquals("The path of the dereferenced property must be equal to the value", path, p.getName());
            assertTrue("The property value must be resolved to the correct property.", prop.getParent().getProperty(path).isSame(p));

            // non-existing property path
            while (session.propertyExists(propPath)) {
                propPath += "x";
            }
            prop.getParent().setProperty(propName, propPath, PropertyType.PATH);
            try {
                prop.getProperty();
                fail("Calling Property.getProperty() for a PATH value that doesn't have a corresponding Property, ItemNotFoundException is expected");
            } catch (ItemNotFoundException e) {
                //ok
            }
        } else {
            try {
                prop.getProperty();
                fail("Property.getNode() called on a multivalue property " +
                        "should throw a ValueFormatException.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        }
    }
}
