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
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.PropertyType;

/**
 * Tests a path property. If the workspace does not contain a node with a path
 * property a {@link org.apache.jackrabbit.test.NotExecutableException} is
 * thrown.
 *
 * @test
 * @sources PathPropertyTest.java
 * @executeClass org.apache.jackrabbit.test.api.PathPropertyTest
 * @keywords level1
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
     * Tests failure of conversion from Path type to Reference type.
     */
    public void testAsReference() throws RepositoryException {
        if (!multiple) {
            try {
                prop.getNode();
                fail("Conversion from a Path value to a Reference value " +
                        "should throw a ValueFormatException.");
            } catch (ValueFormatException vfe) {
                //ok
            }
        } else {
            try {
                prop.getNode();
                fail("Property.getNode() called on a multivalue property " +
                        "should throw a ValueFormatException.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        }
    }
}