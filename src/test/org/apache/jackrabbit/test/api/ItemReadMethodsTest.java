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
import javax.jcr.Item;
import javax.jcr.Property;
import javax.jcr.NodeIterator;
import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ItemNotFoundException;
import java.util.NoSuchElementException;

/**
 * Tests the 'read' methods specified in the {@link javax.jcr.Item} interface
 * on a level 1 repository.
 * <p/>
 * The root node of the default workspace must have at least one child node,
 * otherwise a {@link org.apache.jackrabbit.test.NotExecutableException} is
 * thrown.
 *
 * @test
 * @sources ItemReadMethodsTest.java
 * @executeClass org.apache.jackrabbit.test.api.ItemReadMethodsTest
 * @keywords level1
 */
public class ItemReadMethodsTest extends AbstractJCRTest {

    /** Session to access the workspace */
    private Session session;

    /** The primary test item */
    private Item item;

    /** A child item of the primary test item */
    private Item childItem;

    /** A property of the primary test item */
    private Property childProperty;

    /**
     * Sets up the fixture for this test.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();

        session = helper.getReadOnlySession();
        item = session.getRootNode();

        NodeIterator nodes = ((Node) item).getNodes();
        try {
            childItem = nodes.nextNode();
        } catch (NoSuchElementException e) {
            throw new NotExecutableException("Workspace does not have sufficient content to run this test.");
        }

        PropertyIterator properties = ((Node) item).getProperties();
        try {
            childProperty = properties.nextProperty();
        } catch (NoSuchElementException e) {
            fail("Any node must have at least one property set: jcr:primaryType");
        }
    }

    /**
     * Tests if getPath() returns the correct path.
     */
    public void testGetPath() throws RepositoryException {
        String path = childItem.getPath();
        String notation = "";

        try {
            // check for same named sibling of childItem
            ((Node) item).getNode(childItem.getName() + "[2]");
            notation = "[1]";
        } catch (PathNotFoundException e) {
        }

        if (path.indexOf("[") != -1) {
            notation = path.substring(path.indexOf("["));
        }
        assertEquals("getPath returns wrong result",
                "/" + childItem.getName() + notation,
                childItem.getPath());
    }

    /**
     * Tests if getName() returns same as last name returned by getPath()
     */
    public void testGetName() throws RepositoryException {
        assertEquals("getName() of root must be an empty string",
                "",
                item.getName());

        // build name from path
        String path = childItem.getPath();
        String name = path.substring(path.lastIndexOf("/") + 1);
        if (name.indexOf("[") != -1) {
            name = name.substring(0, name.indexOf("["));
        }
        assertEquals("getName() must be the same as the last item in the path",
                name,
                childItem.getName());
    }

    /**
     * Tests if getItem(x).getParent() is item itself
     */
    public void testGetParent() throws RepositoryException {
        assertSame("getParent() of a child item must be the item itself.",
                item, childItem.getParent());
    }

    /**
     * Tests if getParent() of root throws an ItemNotFoundException
     */
    public void testGetParentOfRoot() throws RepositoryException {
        try {
            item.getParent();
            fail("getParent() of root must throw an ItemNotFoundException.");
        } catch (ItemNotFoundException e) {
            // success
        }
    }

    /**
     * Tests if depth of root is 0 and depth of a sub item of root is 1
     */
    public void testGetDepth() throws RepositoryException {
        assertEquals("getDepth() of root must be 0", 0, item.getDepth());
        assertEquals("getDepth() of child item of root must be 1", 1,
                childItem.getDepth());
    }

    /**
     * Tests if getSession() is same as through which the Item was acquired
     */
    public void testGetSession() throws RepositoryException {
        assertSame("getSession must return the Session through which " +
                "the Item was acquired.",
                item.getSession(),
                session);
    }

    /**
     * Tests if isNode() returns true if the Item is a node and false if it is a
     * property
     */
    public void testIsNode() {
        assertTrue("isNode() must return true if Item is a node.",
                childItem.isNode());
        assertFalse("isNode() must return false if Item is a property.",
                childProperty.isNode());
    }

    /**
     * Tests if isSame() returns true when retrieving an item through different
     * sessions
     */
    public void testIsSame() throws RepositoryException {
        assertFalse("isSame(Item item) must return false for different items.",
                item.isSame(childItem));

        // access same item (root) through different session
        Item otherItem = helper.getReadOnlySession().getRootNode();
        assertTrue("isSame(Item item) must return true for the same " +
                "item retrieved through different sessions.",
                item.isSame(otherItem));
    }
}