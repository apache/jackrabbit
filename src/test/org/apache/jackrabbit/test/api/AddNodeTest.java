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

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.ItemExistsException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * <code>AddNodeTest</code> contains the test cases for the method
 * <code>Node.addNode(String, String)</code>.
 *
 * @test
 * @sources AddNodeTest.java
 * @executeClass org.apache.jackrabbit.test.api.AddNodeTest
 * @keywords level2
 */
public class AddNodeTest extends AbstractJCRTest {

    /**
     * Tests if the name of the created node is correct.
     */
    public void testName() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        assertEquals("Wrong node name.", n1.getName(), nodeName1);
    }

    /**
     * Tests if the node type of the created node is correct.
     */
    public void testNodeType() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        String ntName = n1.getPrimaryNodeType().getName();
        assertEquals("Wrong node NodeType name.", testNodeType, ntName);
    }

    /**
     * Tests if same name siblings have equal names or if same name
     * siblings are not supported a ItemExistsException is thrown.
     */
    public void testSameNameSiblings() throws RepositoryException {
        if (testRootNode.getDefinition().allowSameNameSibs()) {
            Node n1 = testRootNode.addNode(nodeName1, testNodeType);
            Node n2 = testRootNode.addNode(nodeName1, testNodeType);
            testRootNode.save();
            assertEquals("Names of same name siblings are not equal.",
                    n1.getName(), n2.getName());
        } else {
            testRootNode.addNode(nodeName1, testNodeType);
            try {
                testRootNode.addNode(nodeName1, testNodeType);
                fail("Expected ItemExistsException.");
            } catch (ItemExistsException e) {
                // correct
            }
        }
    }

    /**
     * Tests if addNode() throws a NoSuchNodeTypeException in case
     * of an unknown node type.
     */
    public void testUnknownNodeType() throws RepositoryException {
        try {
            testRootNode.addNode(nodeName1, testNodeType + "unknownSuffix");
            fail("Expected NoSuchNodeTypeException.");
        } catch (NoSuchNodeTypeException e) {
            // correct.
        }
    }

    /**
     * Tests if the path of the created node is correct.
     */
    public void testPath() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        String expected = testRootNode.getPath() + "/" + nodeName1;
        assertEquals("Wrong path for created node.", expected, n1.getPath());
    }

    /**
     * Tests if addNode() throws a PathNotFoundException in case
     * intermediary nodes do not exist.
     */
    public void testPathNotFound() throws RepositoryException {
        try {
            testRootNode.addNode(nodeName1 + "/" + nodeName1, testNodeType);
            fail("Expected PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // correct.
        }
    }

    /**
     * Tests if a ConstraintViolationException is thrown when one attempts
     * to add a node at a path that references a property.
     */
    public void testConstraintViolation() throws RepositoryException {
        try {
            Node rootNode = superuser.getRootNode();
            String propPath = testPath + "/" + jcrPrimaryType;
            rootNode.addNode(propPath + "/" + nodeName1, testNodeType);
            fail("Expected ConstraintViolationException.");
        } catch (ConstraintViolationException e) {
            // correct.
        }
    }

    /**
     * Tests if a RepositoryException is thrown in case the path
     * for the new node contains an index.
     */
    public void testRepositoryException() {
        try {
            testRootNode.addNode(nodeName1 + "[1]");
            fail("Expected RepositoryException.");
        } catch (RepositoryException e) {
            // correct.
        }
        try {
            testRootNode.addNode(nodeName1 + "[1]", testNodeType);
            fail("Expected RepositoryException.");
        } catch (RepositoryException e) {
            // correct.
        }
    }
}