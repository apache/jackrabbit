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
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import java.util.NoSuchElementException;

/**
 * <code>PropertyReadMethodsTest</code>...
 *
 * @test
 * @sources PropertyReadMethodsTest.java
 * @executeClass org.apache.jackrabbit.test.api.PropertyReadMethodsTest
 * @keywords level1
 */
public class PropertyReadMethodsTest extends AbstractJCRTest {

    /**
     * Session to access the workspace
     */
    private Session session;

    /**
     * A property of the root node
     */
    private Property property;

    /**
     * Sets up the fixture for this test.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();

        session = helper.getReadOnlySession();
        testRootNode = session.getRootNode().getNode(testPath);

        PropertyIterator properties = testRootNode.getProperties();
        try {
            property = properties.nextProperty();
        } catch (NoSuchElementException e) {
            fail("Any node must have at least one property set: jcr:primaryType");
        }

    }

    /**
     * Releases the session aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
            session = null;
        }
        property = null;
        super.tearDown();
    }

    // -----------< tests of methods inherited of Item >------------------------

    /**
     * Tests if getPath() returns the correct path.
     */
    public void testGetPath()
            throws NotExecutableException, RepositoryException {

        assertEquals("getPath returns wrong result",
                testRoot + "/" + property.getName(),
                property.getPath());
    }

    /**
     * Tests if getName() returns same as last name returned by getPath()
     */
    public void testGetName() throws RepositoryException {
        String path = property.getPath();
        String name = path.substring(path.lastIndexOf("/") + 1);
        assertEquals("getName() must be the same as the last item in the path",
                name,
                property.getName());
    }

    /**
     * Test if the ancestor at depth = n, where n is the depth of this
     * <code>Item</code>, returns this <code>Property</code> itself.
     *
     * @throws RepositoryException
     */
    public void testGetAncestorOfItemDepth() throws RepositoryException {
        Property propertyAtDepth = (Property) property.getAncestor(property.getDepth());
        assertTrue("The ancestor of depth = n, where n is the depth of this " +
                "Property must be the item itself.", property.isSame(propertyAtDepth));
    }

    /**
     * Test if getting the ancestor of depth = n, where n is greater than depth
     * of this <code>Property</code>, throws an <code>ItemNotFoundException</code>.
     *
     * @throws RepositoryException
     */
    public void testGetAncestorOfGreaterDepth() throws RepositoryException {
        try {
            int greaterDepth = property.getDepth() + 1;
            property.getAncestor(greaterDepth);
            fail("Getting ancestor of depth n, where n is greater than depth of" +
                    "this Property must throw an ItemNotFoundException");
        } catch (ItemNotFoundException e) {
            // success
        }
    }

    /**
     * Test if getting the ancestor of negative depth throws an
     * <code>ItemNotFoundException</code>.
     *
     * @throws RepositoryException
     */
    public void testGetAncestorOfNegativeDepth() throws RepositoryException {
        try {
            property.getAncestor(-1);
            fail("Getting ancestor of depth < 0 must throw an ItemNotFoundException.");
        } catch (ItemNotFoundException e) {
            // success
        }
    }

    /**
     * Tests if getParent() returns parent node
     */
    public void testGetParent() throws RepositoryException {
        assertTrue("getParent() of a property must return the parent node.",
                testRootNode.isSame(property.getParent()));
    }

    /**
     * Tests if depth of a property of depth of node + 1
     */
    public void testGetDepth() throws RepositoryException {
        assertEquals("getDepth() of a property of root must be 1", testRootNode.getDepth() + 1,
                property.getDepth());
    }

    /**
     * Tests if getSession() is same as through which the Property was acquired
     */
    public void testGetSession() throws RepositoryException {
        assertSame("getSession must return the Session through which " +
                "the Property was acquired.",
                property.getSession(),
                session);
    }

    /**
     * Tests if isNode() returns false
     */
    public void testIsNode() {
        assertFalse("isNode() must return false.",
                property.isNode());
    }

    /**
     * Tests if isSame() returns true when retrieving a property through
     * different sessions
     */
    public void testIsSame() throws RepositoryException {
        // access same property through different session
        Session otherSession = helper.getReadOnlySession();
        try {
            Property otherProperty = otherSession.getRootNode().getNode(testPath).getProperty(property.getName());
            assertTrue("isSame must return true for the same " +
                    "property retrieved through different sessions.",
                    property.isSame(otherProperty));
        }
        finally {
            otherSession.logout();
        }
    }

    /**
     * Tests if a Property calls the correct visit method on an {@link
     * ItemVisitor}.
     */
    public void testAccept() throws RepositoryException {
        final Property p = property;

        ItemVisitor itemVisitor = new ItemVisitor() {
            public void visit(Property property)
                    throws RepositoryException {
                assertTrue("Visited Property is not the same as the one returned by visit(Property).",
                        p.isSame(property));
            }

            public void visit(Node node) {
                fail("Wrong accept method executed.");
            }
        };

        p.accept(itemVisitor);
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
        Property multiValProp = PropertyUtil.searchMultivalProp(testRootNode);
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
            throw new NotExecutableException();
        }
    }

    /**
     * Tests failure of Property.getValue() method for a multivalue property.
     */
    public void testGetValue() throws RepositoryException, NotExecutableException {
        Property multiValProp = PropertyUtil.searchMultivalProp(testRootNode);
        if (multiValProp != null) {
            try {
                multiValProp.getValue();
                fail("Property.getValue() called on a multivalue property " +
                        "should throw a ValueFormatException.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        } else {
            throw new NotExecutableException();
        }
    }

    /**
     * Tests failure of Property.getValues() method for a single value
     * property.
     */
    public void testGetValues() throws RepositoryException, NotExecutableException {
        Property singleProp = PropertyUtil.searchSingleValuedProperty(testRootNode);
        if (singleProp == null) {
            throw new NotExecutableException("No single valued property found.");
        }

        try {
            singleProp.getValues();
            fail("Property.getValues() called on a single property " +
                    "should throw a ValueFormatException.");
        } catch (ValueFormatException vfe) {
            // ok
        }
    }

    /**
     * Tests if <code>Property.getValues()</code> returns an array that is a copy
     * of the stored values, so changes to it are not reflected in internal storage.
     */
    public void testGetValueCopyStoredValues()
        throws NotExecutableException, RepositoryException {

        Property prop = PropertyUtil.searchMultivalProp(testRootNode);
        if (prop == null) {
            throw new NotExecutableException("No multivalued property found.");
        }

        // acquire the values of the property and change the zeroth value
        Value[] values = prop.getValues();
        if (values.length == 0) {
            throw new NotExecutableException("No testable property found.");
        }
        values[0] = null;

        // re-acquire the values and check if nulled value still exists
        Value[] values2 = prop.getValues();
        assertNotNull("Changes on the array returned by Property.getValues() must " +
                "not be reflected in the internal storage.",
                values2[0]);
    }

    /**
     * Tests if Property.getNode() fails with ValueFormatException for
     * multivalued properties.
     *
     * @throws RepositoryException
     * @throws NotExecutableException
     */
    public void testGetNode() throws RepositoryException, NotExecutableException {
        Property prop = PropertyUtil.searchMultivalProp(testRootNode);
        if (prop == null) {
            throw new NotExecutableException("Test Property.getNode is throwing a "
                    + "ValueFormaException not executable in case of a multivalued property.");
        }
        else {
            try {
                prop.getNode();
                fail("Property.getNode should throw a ValueFormatException in case of "
                        + "a multivalued property.");
            } catch (ValueFormatException vfe) {
                // ok
            }
        }
    }

}
