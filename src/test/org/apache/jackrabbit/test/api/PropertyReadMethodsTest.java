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

import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
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
     * The root node of the default workspace
     */
    private Node rootNode;

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
        rootNode = session.getRootNode();

        PropertyIterator properties = rootNode.getProperties();
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
        }
        super.tearDown();
    }

    // -----------< tests of methods inherited of Item >------------------------

    /**
     * Tests if getPath() returns the correct path.
     */
    public void testGetPath()
            throws NotExecutableException, RepositoryException {

        assertEquals("getPath returns wrong result",
                "/" + property.getName(),
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
                rootNode.isSame(property.getParent()));
    }

    /**
     * Tests if depth of a property of root is 1
     */
    public void testGetDepth() throws RepositoryException {
        assertEquals("getDepth() of a property of root must be 1", 1,
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
        PropertyIterator properties = helper.getReadOnlySession().getRootNode().getProperties();
        Property otherProperty = properties.nextProperty();
        assertTrue("isSame must return true for the same " +
                "property retrieved through different sessions.",
                property.isSame(otherProperty));
    }

    /**
     * Tests if a Property calls the correct visit method on an {@link
     * ItemVisitor}.
     */
    public void testAccept() throws RepositoryException {
        final Property p = property;

        ItemVisitor itemVisitor = new ItemVisitor() {
            public void visit(Property property) {
                assertTrue("Visited Property is not the same as the one returned by visit(Property).",
                        p.isSame(property));
            }

            public void visit(Node node) {
                fail("Wrong accept method executed.");
            }
        };

        p.accept(itemVisitor);
    }

}