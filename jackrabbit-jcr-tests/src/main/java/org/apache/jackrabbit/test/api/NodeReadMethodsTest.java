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

import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.UnsupportedRepositoryOperationException;
import java.util.ArrayList;
import java.util.NoSuchElementException;

/**
 * Tests the 'read' methods specified in the {@link javax.jcr.Node} interface on
 * a level 1 repository.
 * <p/>
 * Most tests require at least one child node under the root node, otherwise a
 * {@link org.apache.jackrabbit.test.NotExecutableException} is thrown.
 *
 * @test
 * @sources NodeReadMethodsTest.java
 * @executeClass org.apache.jackrabbit.test.api.NodeReadMethodsTest
 * @keywords level1
 */
public class NodeReadMethodsTest extends AbstractJCRTest {

    /**
     * Session to access the workspace
     */
    private Session session;

    /**
     * The first child node of the testRootNode
     */
    private Node childNode;

    /**
     * Sets up the fixture for this test.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();

        session = helper.getReadOnlySession();

        testRootNode = session.getRootNode().getNode(testPath);
        NodeIterator nodes = testRootNode.getNodes();
        try {
            childNode = nodes.nextNode();
        } catch (NoSuchElementException e) {
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
        childNode = null;
        super.tearDown();
    }

    // -----------< tests of methods inherited of Item >------------------------

    /**
     * Tests if getPath() returns the correct path.
     */
    public void testGetPath()
            throws NotExecutableException, RepositoryException {

        if (childNode == null) {
            throw new NotExecutableException("Workspace does not have sufficient content to run this test.");
        }

        String path = testRoot + "/" + childNode.getName();
        if (getSize(testRootNode.getNodes(childNode.getName())) > 1) {
            // is a same-name-sibling, append index
            path += "[" + childNode.getIndex() + "]";
        }

        log.println("path: " + childNode.getPath());
        assertEquals("getPath returns wrong result",
                path,
                childNode.getPath());
    }

    /**
     * Tests if getName() returns same as last name returned by getPath()
     */
    public void testGetName() throws RepositoryException, NotExecutableException {
        if (childNode == null) {
            throw new NotExecutableException("Workspace does not have sufficient content to run this test.");
        }

        // build name from path
        String path = childNode.getPath();
        String name = path.substring(path.lastIndexOf("/") + 1);
        if (name.indexOf("[") != -1) {
            name = name.substring(0, name.indexOf("["));
        }
        assertEquals("getName() must be the same as the last item in the path",
                name,
                childNode.getName());
    }

    /**
     * Test if the ancestor at depth = n, where n is the depth of this
     * <code>Item</code>, returns this <code>Node</code> itself.
     */
    public void testGetAncestorOfNodeDepth() throws RepositoryException {
        Node nodeAtDepth = (Node) testRootNode.getAncestor(testRootNode.getDepth());
        assertTrue("The ancestor of depth = n, where n is the depth of this " +
                "Node must be the item itself.", testRootNode.isSame(nodeAtDepth));
    }

    /**
     * Test if getting the ancestor of depth = n, where n is greater than depth
     * of this <code>Node</code>, throws an <code>ItemNotFoundException</code>.
     */
    public void testGetAncestorOfGreaterDepth() throws RepositoryException {
        try {
            int greaterDepth = testRootNode.getDepth() + 1;
            testRootNode.getAncestor(greaterDepth);
            fail("Getting ancestor of depth n, where n is greater than depth of" +
                    "this Node must throw an ItemNotFoundException");
        } catch (ItemNotFoundException e) {
            // success
        }
    }

    /**
     * Test if getting the ancestor of negative depth throws an
     * <code>ItemNotFoundException</code>.
     */
    public void testGetAncestorOfNegativeDepth() throws RepositoryException {
        try {
            testRootNode.getAncestor(-1);
            fail("Getting ancestor of depth < 0 must throw an ItemNotFoundException.");
        } catch (ItemNotFoundException e) {
            // success
        }
    }

    /**
     * Tests if getParent() returns parent node
     */
    public void testGetParent()
            throws NotExecutableException, RepositoryException {

        if (childNode == null) {
            throw new NotExecutableException("Workspace does not have sufficient content to run this test.");
        }

        assertTrue("getParent() of a child node return the parent node.",
                testRootNode.isSame(childNode.getParent()));
    }

    /**
     * Tests if getParent() of root throws an ItemNotFoundException
     */
    public void testGetParentOfRoot() throws RepositoryException {
        try {
            session.getRootNode().getParent();
            fail("getParent() of root must throw an ItemNotFoundException.");
        } catch (ItemNotFoundException e) {
            // success
        }
    }

    /**
     * Tests if depth of root is 0 and depth of a sub node of root is 1
     */
    public void testGetDepth() throws RepositoryException {
        assertEquals("getDepth() of root must be 0", 0, session.getRootNode().getDepth());
        for (NodeIterator it = session.getRootNode().getNodes(); it.hasNext();) {
            assertEquals("getDepth() of child node of root must be 1", 1,
                    it.nextNode().getDepth());
        }
    }

    /**
     * Tests if getSession() is same as through which the Item was acquired
     */
    public void testGetSession() throws RepositoryException {
        assertSame("getSession must return the Session through which " +
                "the Node was acquired.",
                testRootNode.getSession(),
                session);
    }

    /**
     * Tests if isNode() returns true
     */
    public void testIsNode() {
        assertTrue("isNode() must return true.",
                testRootNode.isNode());
    }

    /**
     * Tests if isSame() returns true when retrieving an item through different
     * sessions
     */
    public void testIsSame() throws RepositoryException {
        // access same node through different session
        Session s = helper.getReadOnlySession();
        try {
            Item otherTestNode = s.getRootNode().getNode(testPath);
            assertTrue("isSame(Item item) must return true for the same " +
                    "item retrieved through different sessions.",
                    testRootNode.isSame(otherTestNode));
        } finally {
            s.logout();
        }
    }

    /**
     *
     */
    public void testAccept() throws RepositoryException {
        final Node n = testRootNode;

        ItemVisitor itemVisitor = new ItemVisitor() {
            public void visit(Property property) {
                fail("Wrong accept method executed.");
            }

            public void visit(Node node) throws RepositoryException {
                assertTrue("Visited node is not the same as the one passed in method visit(Node)",
                        n.isSame(node));
            }
        };

        n.accept(itemVisitor);
    }

    // -----------< tests of Node specific methods >----------------------------

    /**
     * Test if getNode(String relPath) returns the correct node and if a
     * PathNotFoundException is thrown when Node at relPath does not exist
     */
    public void testGetNode()
            throws NotExecutableException, RepositoryException {

        StringBuffer notExistingPath = new StringBuffer("X");
        NodeIterator nodes = testRootNode.getNodes();
        while (nodes.hasNext()) {
            // build a path that for sure is not existing
            // (":" of namespace prefix will be replaced later on)
            notExistingPath.append(nodes.nextNode().getName());
        }

        try {
            testRootNode.getNode(notExistingPath.toString().replaceAll(":", ""));
            fail("getNode(String relPath) must throw a PathNotFoundException" +
                    "if no node exists at relPath");
        } catch (PathNotFoundException e) {
            // success
        }

        try {
            NodeIterator nodes2 = testRootNode.getNodes();
            Node node = nodes2.nextNode();
            assertTrue("Node from Iterator is not the same as the Node from getNode()",
                    testRootNode.getNode(node.getName()).isSame(node));
        } catch (NoSuchElementException e) {
            throw new NotExecutableException("Workspace does not have sufficient content for this test. " +
                    "Root node must have at least one child node.");
        }
    }

    /**
     * Test if all returned items are of type node.
     */
    public void testGetNodes() throws RepositoryException {
        NodeIterator nodes = testRootNode.getNodes();
        while (nodes.hasNext()) {
            Item item = (Item) nodes.next();
            assertTrue("Item is not a node", item.isNode());
        }
    }

    /**
     * Test getNodes(String namePattern) with all possible patterns. Tested
     * node: root - NotExecutableException is thrown when root node has no sub
     * nodes.
     */
    public void testGetNodesNamePattern()
            throws NotExecutableException, RepositoryException {

        // get root node and build an ArrayList of its sub nodes
        Node node = testRootNode;
        if (!node.hasNodes()) {
            throw new NotExecutableException("Workspace does not have sufficient content for this test. " +
                    "Root node must have at least one child node.");
        }
        NodeIterator allNodesIt = node.getNodes();
        ArrayList allNodes = new ArrayList();
        while (allNodesIt.hasNext()) {
            Node n = allNodesIt.nextNode();
            allNodes.add(n);
        }

        // test if an empty NodeIterator is returned
        // when the pattern is not matching any child node
        String pattern0 = "";
        NodeIterator nodes0 = node.getNodes(pattern0);
        try {
            nodes0.nextNode();
            fail("An empty NodeIterator must be returned if pattern does" +
                    "not match any child node.");
        } catch (NoSuchElementException e) {
            // success
        }

        // all further tests are using root's first sub node
        Node firstNode = (Node) allNodes.get(0);

        // test pattern "*"
        String pattern1 = "*";
        String assertString1 = "node.getNodes(\"" + pattern1 + "\"): ";
        NodeIterator nodes1 = node.getNodes(pattern1);
        // test if the number of found nodes is correct
        assertEquals(assertString1 + "number of nodes found: ",
                allNodes.size(),
                getSize(nodes1));

        // test pattern "nodeName"
        String pattern2 = firstNode.getName();
        String assertString2 = "node.getNodes(\"" + pattern2 + "\"): ";
        // test if the names of the found nodes are matching the pattern
        NodeIterator nodes2 = node.getNodes(pattern2);
        while (nodes2.hasNext()) {
            Node n = nodes2.nextNode();
            assertEquals(assertString2 + "name comparison failed: ",
                    firstNode.getName(),
                    n.getName());
        }
        // test if the number of found nodes is correct
        int numExpected2 = 0;
        for (int i = 0; i < allNodes.size(); i++) {
            Node n = (Node) allNodes.get(i);
            if (n.getName().equals(firstNode.getName())) {
                numExpected2++;
            }
        }
        nodes2 = node.getNodes(pattern2);
        assertEquals(assertString2 + "number of nodes found: ",
                numExpected2,
                getSize(nodes2));


        // test pattern "nodeName|nodeName"
        String pattern3 = firstNode.getName() + "|" + firstNode.getName();
        String assertString3 = "node.getNodes(\"" + pattern3 + "\"): ";
        // test if the names of the found nodes are matching the pattern
        NodeIterator nodes3 = node.getNodes(pattern3);
        while (nodes3.hasNext()) {
            Node n = nodes3.nextNode();
            assertEquals(assertString2 + "name comparison failed: ",
                    firstNode.getName(),
                    n.getName());
        }
        // test if the number of found nodes is correct
        int numExpected3 = 0;
        for (int i = 0; i < allNodes.size(); i++) {
            Node n = (Node) allNodes.get(i);
            if (n.getName().equals(firstNode.getName())) {
                numExpected3++;
            }
        }
        nodes3 = node.getNodes(pattern3);
        assertEquals(assertString3 + "number of nodes found: ",
                numExpected3,
                getSize(nodes3));


        // test pattern "*odeNam*"
        if (firstNode.getName().length() > 2) {
            String name = firstNode.getName();
            String shortenName = name.substring(1, name.length() - 1);
            String pattern4 = "*" + shortenName + "*";
            String assertString4 = "node.getNodes(\"" + pattern4 + "\"): ";
            // test if the names of the found nodes are matching the pattern
            NodeIterator nodes4 = node.getNodes(pattern4);
            while (nodes4.hasNext()) {
                Node n = nodes4.nextNode();
                assertTrue(assertString4 + "name comparison failed: *" +
                        shortenName + "* not found in " + n.getName(),
                        n.getName().indexOf(shortenName) != -1);
            }
            // test if the number of found nodes is correct
            int numExpected4 = 0;
            for (int i = 0; i < allNodes.size(); i++) {
                Node n = (Node) allNodes.get(i);
                if (n.getName().indexOf(shortenName) != -1) {
                    numExpected4++;
                }
            }
            nodes4 = node.getNodes(pattern4);
            assertEquals(assertString4 + "number of nodes found: ",
                    numExpected4,
                    getSize(nodes4));
        }
    }

    /**
     * Test if getProperty(String relPath) returns the correct node and if a
     * PathNotFoundException is thrown when property at relPath does not exist
     */
    public void testGetProperty()
            throws NotExecutableException, RepositoryException {
        StringBuffer notExistingPath = new StringBuffer("X");
        PropertyIterator properties = testRootNode.getProperties();
        while (properties.hasNext()) {
            // build a path that for sure is not existing
            // (":" of namespace prefix will be replaced later on)
            notExistingPath.append(properties.nextProperty().getName());
        }

        try {
            testRootNode.getProperty(notExistingPath.toString().replaceAll(":", ""));
            fail("getProperty(String relPath) must throw a " +
                    "PathNotFoundException if no node exists at relPath");
        } catch (PathNotFoundException e) {
            // success
        }

        try {
            PropertyIterator properties2 = testRootNode.getProperties();
            Property property = properties2.nextProperty();
            assertTrue("Property returned by getProperties() is not the same as returned by getProperty(String).",
                    testRootNode.getProperty(property.getName()).isSame(property));
        } catch (NoSuchElementException e) {
            fail("Root node must always have at least one property: jcr:primaryType");
        }
    }

    /**
     * Test if all returned items are of type node.
     */
    public void testGetProperties() throws RepositoryException {
        PropertyIterator properties = testRootNode.getProperties();
        while (properties.hasNext()) {
            Item item = (Item) properties.next();
            assertFalse("Item is not a property", item.isNode());
        }
    }

    /**
     * Test getProperties(String namePattern) with all possible patterns. Tested
     * node: root - a NotExecutableException is thrown when root node has no
     * properties.
     */
    public void testGetPropertiesNamePattern()
            throws NotExecutableException, RepositoryException {

        // get root node and build an ArrayList of its sub nodes
        Node node = testRootNode;
        if (!node.hasProperties()) {
            fail("Root node must always have at least one property: jcr:primaryType");
        }
        PropertyIterator allPropertiesIt = node.getProperties();
        ArrayList allProperties = new ArrayList();
        StringBuffer notExistingPropertyName = new StringBuffer();
        while (allPropertiesIt.hasNext()) {
            Property p = allPropertiesIt.nextProperty();
            allProperties.add(p);
            notExistingPropertyName.append(p.getName() + "X");
        }


        // test that an empty NodeIterator is returned
        // when the pattern is not matching any child node
        String pattern0 = notExistingPropertyName.toString().replaceAll(":", "");
        NodeIterator properties0 = node.getNodes(pattern0);
        try {
            properties0.nextNode();
            fail("An empty NodeIterator must be returned if pattern does" +
                    "not match any child node.");
        } catch (NoSuchElementException e) {
            // success
        }

        // all tests are running using root's first property
        Property firstProperty = (Property) allProperties.get(0);

        // test: getProperties("*")
        String pattern1 = "*";
        String assertString1 = "node.getProperties(\"" + pattern1 + "\"): ";
        PropertyIterator properties1 = node.getProperties(pattern1);
        assertEquals(assertString1 + "number of properties found: ",
                allProperties.size(),
                getSize(properties1));

        // test: getProperties("propertyName")
        String pattern2 = firstProperty.getName();
        String assertString2 = "node.getProperties(\"" + pattern2 + "\"): ";
        // test if the names of the found properties are matching the pattern
        PropertyIterator properties2 = node.getProperties(pattern2);
        while (properties2.hasNext()) {
            Property p = properties2.nextProperty();
            assertEquals(assertString2 + "name comparison failed: ",
                    firstProperty.getName(),
                    p.getName());
        }
        // test if the number of found properties is correct
        int numExpected2 = 0;
        for (int i = 0; i < allProperties.size(); i++) {
            Property p = (Property) allProperties.get(i);
            if (p.getName().equals(firstProperty.getName())) {
                numExpected2++;
            }
        }
        properties2 = node.getProperties(pattern2);
        assertEquals(assertString2 + "number of properties found: ",
                numExpected2,
                getSize(properties2));


        // test: getProperties("propertyName|propertyName")
        String pattern3 = firstProperty.getName() + "|" + firstProperty.getName();
        String assertString3 = "node.getProperties(\"" + pattern3 + "\"): ";
        // test if the names of the found properties are matching the pattern
        PropertyIterator properties3 = node.getProperties(pattern3);
        while (properties3.hasNext()) {
            Property p = properties3.nextProperty();
            assertEquals(assertString2 + "name comparison failed: ",
                    firstProperty.getName(),
                    p.getName());
        }
        // test if the number of found properties is correct
        int numExpected3 = 0;
        for (int i = 0; i < allProperties.size(); i++) {
            Property p = (Property) allProperties.get(i);
            if (p.getName().equals(firstProperty.getName())) {
                numExpected3++;
            }
        }
        properties3 = node.getProperties(pattern3);
        assertEquals(assertString3 + "number of properties found: ",
                numExpected3,
                getSize(properties3));


        // test: getProperties("*opertyNam*")
        if (firstProperty.getName().length() > 2) {
            String name = firstProperty.getName();
            String shortenName = name.substring(1, name.length() - 1);
            String pattern4 = "*" + shortenName + "*";
            String assertString4 = "node.getProperties(\"" + pattern4 + "\"): ";
            // test if the names of the found properties are matching the pattern
            PropertyIterator properties4 = node.getProperties(pattern4);
            while (properties4.hasNext()) {
                Property p = properties4.nextProperty();
                assertTrue(assertString4 + "name comparison failed: *" +
                        shortenName + "* not found in " + p.getName(),
                        p.getName().indexOf(shortenName) != -1);
            }
            // test if the number of found properties is correct
            int numExpected4 = 0;
            for (int i = 0; i < allProperties.size(); i++) {
                Property p = (Property) allProperties.get(i);
                if (p.getName().indexOf(shortenName) != -1) {
                    numExpected4++;
                }
            }
            properties4 = node.getProperties(pattern4);
            assertEquals(assertString4 + "number of properties found: ",
                    numExpected4,
                    getSize(properties4));
        }
    }

    /**
     * Test if getPrimaryItem returns the primary item as defined in the primary
     * node type. Therefor a node with a primary item is located recursively in
     * the entire repository. A NotExecutableException is thrown when no such
     * node is found.
     */
    public void testGetPrimaryItem()
            throws NotExecutableException, RepositoryException {

        Node node = locateNodeWithPrimaryItem(testRootNode);

        if (node == null) {
            throw new NotExecutableException("Workspace does not contain a node with primary item defined");
        }

        String primaryItemName = node.getPrimaryNodeType().getPrimaryItemName();

        Item primaryItem = node.getPrimaryItem();
        if (primaryItem.isNode()) {
            assertTrue("Node returned by getPrimaryItem() is not the same as " +
                    "the one aquired by getNode(String)",
                    node.getNode(primaryItemName).isSame(primaryItem));
        } else {
            assertTrue("Property returned by getPrimaryItem() is not the same as " +
                    "the one aquired by getProperty(String)",
                    node.getProperty(primaryItemName).isSame(primaryItem));
        }
    }

    /**
     * Test if getPrimaryItem does throw an ItemNotFoundException if the primary
     * node type does not define a primary item. Therefor a node without a
     * primary item is located recursively in the entire repository. A
     * NotExecutableException is thrown when no such node is found.
     */
    public void testGetPrimaryItemItemNotFoundException()
            throws NotExecutableException, RepositoryException {

        Node node = locateNodeWithoutPrimaryItem(testRootNode);

        if (node == null) {
            throw new NotExecutableException("Workspace does not contain a node without primary item defined");
        }

        try {
            node.getPrimaryItem();
            fail("getPrimaryItem() must throw a ItemNotFoundException " +
                    "if the primary node type does not define one");
        } catch (ItemNotFoundException e) {
            // success
        }
    }

    /**
     * Test if getIndex() returns the correct index. Therefor a node with same
     * name sibling is located recursively in the entire repository. If no such
     * node is found, the test checks if the testRootNode returns 1
     */
    public void testGetIndex()
            throws RepositoryException {

        Node node = locateNodeWithSameNameSiblings(testRootNode);

        if (node == null) {
            assertEquals("getIndex() of a node without same name siblings " +
                    "must return 1", testRootNode.getIndex(), 1);
        } else {
            NodeIterator nodes = node.getParent().getNodes(node.getName());
            int i = 1;
            while (nodes.hasNext()) {
                assertEquals("getIndex() must return the correct index",
                        nodes.nextNode().getIndex(),
                        i);
                i++;
            }
        }
    }

    public void testGetReferences()
            throws NotExecutableException, RepositoryException {

        Node node = locateNodeWithReference(testRootNode);

        if (node == null) {
            throw new NotExecutableException("Workspace does not contain a node with a reference property set");
        }

        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property p = properties.nextProperty();
            if (p.getType() == PropertyType.REFERENCE && !p.getDefinition().isMultiple()) {
                Node referencedNode = p.getNode();
                PropertyIterator refs = referencedNode.getReferences();
                boolean referenceFound = false;
                while (refs.hasNext()) {
                    Property ref = refs.nextProperty();
                    if (ref.isSame(p)) {
                        referenceFound = true;
                    }
                }
                assertTrue("Correct reference not found", referenceFound);
            }
        }
    }

    /**
     * Test if getUUID() returns the string value of the property "jcr:uuid".
     * Therefor a node of type "mix:referenceable" is located recursively in the
     * entire repository. A NotExecutableException is thrown when no node of
     * this type is found.
     *
     * @throws NotExecutableException
     * @throws RepositoryException
     */
    public void testGetUUID()
            throws NotExecutableException, RepositoryException {

        // find a node of type mix:referenceable
        Node node = locateReferenceableNode(testRootNode);

        if (node == null) {
            throw new NotExecutableException("Workspace does not contain a referencable node");
        }

        try {
            assertEquals("node.getUUID() does not match " +
                    "node.getProperty(\"jcr:uuid\").getString()",
                    node.getProperty(jcrUUID).getString(), node.getUUID());
        } catch (PathNotFoundException e) {
            fail("Property UUID expected for " +
                    "node of type \"" + mixReferenceable + "\"");
        }
    }

    /**
     * Test if getUUID() throws a UnsupportedRepositoryOperationException if
     * Node is not referenceable
     */
    public void testGetUUIDOfNonReferenceableNode()
            throws NotExecutableException, RepositoryException {

        // find a node NOT of type mix:referenceable
        Node node = locateNonReferenceableNode(testRootNode);

        if (node == null) {
            throw new NotExecutableException("Workspace does not contain a non referenceable node");
        }

        try {
            node.getUUID();
            fail("UnsupportedRepositoryOperationException expected");
        } catch (UnsupportedRepositoryOperationException e) {
            // success
        }
    }

    /**
     * Test if hasNode(String relPath) returns true if the required node exists
     * and false if it doesn't. Tested node: root
     */
    public void testHasNode()
            throws NotExecutableException, RepositoryException {

        Node node = testRootNode;

        NodeIterator nodes = node.getNodes();
        StringBuffer notExistingNodeName = new StringBuffer();
        while (nodes.hasNext()) {
            Node n = nodes.nextNode();
            assertTrue("hasNode(String relPath) returns false although " +
                    "node at relPath is existing",
                    node.hasNode(n.getName()));
            notExistingNodeName.append(n.getName() + "X");
        }
        if (notExistingNodeName.toString().equals("")) {
            throw new NotExecutableException("Workspace does not have sufficient content for this test. " +
                    "Root node must have at least one child node.");
        }

        assertFalse("hasNode(String relPath) returns true although " +
                "node at relPath is not existing",
                node.hasNode(notExistingNodeName.toString().replaceAll(":", "")));
    }

    /**
     * Test if hasNodes() returns true if any sub node exists or false if not.
     * Tested node: root
     */
    public void testHasNodes() throws RepositoryException {
        Node node = testRootNode;
        NodeIterator nodes = node.getNodes();

        int i = 0;
        while (nodes.hasNext()) {
            nodes.nextNode();
            i++;
        }

        if (i == 0) {
            assertFalse("node.hasNodes() returns true although " +
                    "no sub nodes existing",
                    node.hasNodes());
        } else {
            assertTrue("node.hasNodes() returns false althuogh " +
                    "sub nodes are existing",
                    node.hasNodes());
        }
    }

    /**
     * Test if hasProperty(String relPath) returns true if a required property
     * exists and false if it doesn't. Tested node: root
     */
    public void testHasProperty()
            throws NotExecutableException, RepositoryException {

        Node node = testRootNode;

        PropertyIterator properties = node.getProperties();
        StringBuffer notExistingPropertyName = new StringBuffer();
        while (properties.hasNext()) {
            Property p = properties.nextProperty();
            assertTrue("node.hasProperty(\"relPath\") returns false " +
                    "although property at relPath is existing",
                    node.hasProperty(p.getName()));
            notExistingPropertyName.append(p.getName() + "X");
        }
        if (notExistingPropertyName.toString().equals("")) {
            fail("Root node must at least have one property: jcr:primaryType");
        }

        assertFalse("node.hasProperty(\"relPath\") returns true " +
                "although property at relPath is not existing",
                node.hasProperty(notExistingPropertyName.toString().replaceAll(":", "")));
    }

    /**
     * Test if hasProperty() returns true if any property exists or false if
     * not. Tested node: root
     *
     * @throws RepositoryException
     */
    public void testHasProperties() throws RepositoryException {
        Node node = testRootNode;
        PropertyIterator properties = node.getProperties();

        int i = 0;
        while (properties.hasNext()) {
            Property p = properties.nextProperty();
            log.println(p.getName());
            i++;
        }

        if (i == 0) {
            assertFalse("Must return false when no properties exist",
                    node.hasProperties());
        } else {
            assertTrue("Must return true when one or more properties exist",
                    node.hasProperties());
        }
    }

    //-----------------------< internal >---------------------------------------

    /**
     * Returns the first descendant of <code>node</code> which is of type
     * mix:referencable.
     *
     * @param node <code>Node</code> to start traversal.
     * @return first node of type mix:referenceable
     */
    private Node locateReferenceableNode(Node node)
            throws RepositoryException {

        if (node.isNodeType(mixReferenceable)) {
            return node;
        }

        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node returnedNode = locateReferenceableNode(nodes.nextNode());
            if (returnedNode != null) {
                return returnedNode;
            }
        }
        return null;
    }

    /**
     * Returns the first descendant of <code>node</code> which is not of type
     * mix:referenceable.
     *
     * @param node <code>Node</code> to start traversal.
     * @return first node which is not of type mix:referenceable
     */
    private Node locateNonReferenceableNode(Node node)
            throws RepositoryException {

        if (!node.isNodeType(mixReferenceable)) {
            return node;
        }

        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node returnedNode = locateNonReferenceableNode(nodes.nextNode());
            if (returnedNode != null) {
                return returnedNode;
            }
        }
        return null;
    }

    /**
     * Returns the first descendant of <code>node</code> which has a property of
     * type {@link javax.jcr.PropertyType#REFERENCE} set and is <b>not</b>
     * multivalued.
     *
     * @param node <code>Node</code> to start traversal.
     * @return first node with a property of PropertType.REFERENCE
     */
    private Node locateNodeWithReference(Node node)
            throws RepositoryException {

        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property p = properties.nextProperty();
            if (p.getType() == PropertyType.REFERENCE && !p.getDefinition().isMultiple()) {
                return node;
            }
        }

        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node returnedNode = locateNodeWithReference(nodes.nextNode());
            if (returnedNode != null) {
                return returnedNode;
            }
        }
        return null;
    }

    /**
     * Returns the first descendant of <code>node</code> which defines a primary
     * item.
     *
     * @param node <code>Node</code> to start traversal.
     * @return first node with a primary item
     */
    private Node locateNodeWithPrimaryItem(Node node)
            throws RepositoryException {

        if (node.getPrimaryNodeType().getPrimaryItemName() != null) {
            return node;
        }

        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node returnedNode = locateNodeWithPrimaryItem(nodes.nextNode());
            if (returnedNode != null) {
                return returnedNode;
            }
        }
        return null;
    }

    /**
     * Returns the first descendant of <code>node</code> which does not define a
     * primary item.
     *
     * @param node <code>Node</code> to start traversal.
     * @return first node without a primary item
     */
    private Node locateNodeWithoutPrimaryItem(Node node)
            throws RepositoryException {

        if (node.getPrimaryNodeType().getPrimaryItemName() == null) {
            return node;
        }

        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node n = locateNodeWithoutPrimaryItem(nodes.nextNode());
            if (n != null) {
                return n;
            }
        }
        return null;
    }

    /**
     * Returns the first descendant of <code>node</code> which has same name
     * siblings.
     *
     * @param node <code>Node</code> to start traversal.
     * @return first node with same name siblings
     */
    private Node locateNodeWithSameNameSiblings(Node node)
            throws RepositoryException {

        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node n = nodes.nextNode();
            NodeIterator nodes2 = node.getNodes(n.getName());
            int i = 0;
            while (nodes2.hasNext()) {
                nodes2.next();
                i++;
            }
            if (i > 1) {
                // node has same name siblings
                return n;
            } else {
                Node returnedNode = locateNodeWithSameNameSiblings(n);
                if (returnedNode != null) {
                    return returnedNode;
                }
            }
        }
        return null;
    }
}
