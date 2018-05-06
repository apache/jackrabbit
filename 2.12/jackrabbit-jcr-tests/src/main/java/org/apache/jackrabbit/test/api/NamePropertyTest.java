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
import javax.jcr.Property;
import javax.jcr.Node;

/**
 * Tests a date property. If the workspace does not contain a node with a date
 * property a {@link org.apache.jackrabbit.test.NotExecutableException} is
 * thrown.
 *
 */
public class NamePropertyTest extends AbstractPropertyTest {

    /**
     * Returns {@link javax.jcr.PropertyType#NAME}.
     *
     * @return {@link javax.jcr.PropertyType#NAME}.
     */
    protected int getPropertyType() {
        return PropertyType.NAME;
    }

    /**
     * Returns "does not matter" (<code>null</code>).
     * @return <code>null</code>.
     */
    protected Boolean getPropertyIsMultivalued() {
        return null;
    }

    /**
     * Tests conversion from Name type to String type.
     *
     * @throws RepositoryException
     */
    public void testGetString() throws RepositoryException {
        Value val = PropertyUtil.getValue(prop);
        assertTrue("Not a valid Name property: " + prop.getName(),
                PropertyUtil.checkNameFormat(val.getString(), session));

    }

    /**
     * Tests failure of conversion from Name type to Boolean type.
     *
     * @throws RepositoryException
     */
    public void testGetBoolean() throws RepositoryException {
        try {
            Value val = PropertyUtil.getValue(prop);
            val.getBoolean();
            fail("Conversion from a Name value to a Boolean value " +
                    "should throw a ValueFormatException.");
        } catch (ValueFormatException vfe) {
            //ok
        }
    }

    /**
     * Tests failure of conversion from Name type to Date type.
     *
     * @throws RepositoryException
     */
    public void testGetDate() throws RepositoryException {
        try {
            Value val = PropertyUtil.getValue(prop);
            val.getDate();
            fail("Conversion from a Name value to a Date value " +
                    "should throw a ValueFormatException.");
        } catch (ValueFormatException vfe) {
            //ok
        }
    }

    /**
     * Tests failure from Name type to Double type.
     *
     * @throws RepositoryException
     */
    public void testGetDouble() throws RepositoryException {
        try {
            Value val = PropertyUtil.getValue(prop);
            val.getDouble();
            fail("Conversion from a Name value to a Double value " +
                    "should throw a ValueFormatException.");
        } catch (ValueFormatException vfe) {
            //ok
        }
    }

    /**
     * Tests failure of conversion from Name type to Long type.
     *
     * @throws RepositoryException
     */
    public void testGetLong() throws RepositoryException {
        try {
            Value val = PropertyUtil.getValue(prop);
            val.getLong();
            fail("Conversion from a Name value to a Long value " +
                    "should throw a ValueFormatException.");
        } catch (ValueFormatException vfe) {
            //ok
        }
    }

    /**
     * Tests if Value.getType() returns the same as Property.getType() and also
     * tests that prop.getDefinition().getRequiredType() returns the same type
     * in case it is not of Undefined type.
     *
     * @throws RepositoryException
     */
    public void testGetType() throws RepositoryException {
        assertTrue("Value.getType() returns wrong type.",
                PropertyUtil.checkGetType(prop, PropertyType.NAME));
    }

    /**
     * Since JCR 2.0 a path property can be dereferenced if it points to a
     * Node.
     * TODO: create several tests out of this one
     */
    public void testGetNode() throws RepositoryException {
        if (!multiple) {
            String path = prop.getString();
            if (prop.getParent().hasNode(path)) {
                Node n = prop.getNode();
                assertEquals("The name of the dereferenced property must be equal to the value", path, n.getName());
            } else {
                try {
                    prop.getNode();
                    fail("Calling Property.getNode() for a NAME value that doesn't have a corresponding Node, ItemNotFoundException is expected");
                } catch (ItemNotFoundException e) {
                    // success.
                }
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
            String path = prop.getString();
            if (prop.getParent().hasProperty(path)) {
                Property p = prop.getProperty();
                assertEquals("The name of the dereferenced property must be equal to the value", path, p.getName());
            } else {
                try {
                    prop.getProperty();
                    fail("Calling Property.getProperty() for a NAME value that doesn't have a corresponding Node, ItemNotFoundException is expected");
                } catch (ItemNotFoundException e) {
                    // success.
                }
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
