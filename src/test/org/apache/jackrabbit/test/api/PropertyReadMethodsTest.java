/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.*;

/**
 * Tests general aspects of multi valued properties.
 *
 * @test
 * @sources PropertyReadMethodsTest.java
 * @executeClass org.apache.jackrabbit.test.api.PropertyReadMethodsTest
 * @keywords level1
 */
public class PropertyReadMethodsTest extends AbstractJCRTest {

    /** A single value property */
    private Property singleProp;

    /** A multi value property */
    private Property multiValProp;

    /**
     * Sets up the fixture for this test.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
        multiValProp = PropertyUtil.searchMultivalProp(testRootNode);
        singleProp = testRootNode.getProperty(jcrPrimaryType);
    }

    /**
     * Tests that no null value property exists in a given node tree.
     */
    public void testNoNullValue() throws RepositoryException {
        assertFalse("Single property with null value found.",
                PropertyUtil.nullValues(testRootNode));
    }

    /**
     * Tests that all values of a multivalue property have the same property
     * type.
     */
    public void testMultiValueType() throws RepositoryException, NotExecutableException {
        if (multiValProp != null) {
            Value[] vals = multiValProp.getValues();
            if (vals.length > 0) {
                int type = vals[0].getType();
                for (int i = 1; i < vals.length; i++) {
                    assertEquals("Multivalue property has values with different types.",
                            type, vals[i].getType());
                }
            }
        } else {
            throw new NotExecutableException("Workspace does not contain a node with a multi valued property");
        }
    }

    /**
     * Tests failure of Property.getValue() method for a multivalue property.
     */
    public void testGetValue() throws RepositoryException, NotExecutableException {
        if (multiValProp != null) {
            try {
                multiValProp.getValue();
                fail("Property.getValue() called on a multivalue property " +
                        "should throw a ValueFormatException.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        } else {
            throw new NotExecutableException("Workspace does not contain a node with a multi valued property");
        }
    }

    /**
     * Tests failure of Property.getValues() method for a single value
     * property.
     */
    public void testGetValues() throws RepositoryException {
        try {
            singleProp.getValues();
            fail("Property.getValues() called on a single property " +
                    "should throw a ValueFormatException.");
        } catch (ValueFormatException vfe) {
            // ok
        }
    }
}