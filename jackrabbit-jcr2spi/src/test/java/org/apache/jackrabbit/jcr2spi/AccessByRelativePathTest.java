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
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.NodeIterator;

/**
 * <code>AccessByRelativePathTest</code>...
 */
public class AccessByRelativePathTest extends AbstractJCRTest {

    private static String DOT = ".";
    private static String DOTDOT = "..";

    /**
     * <code>Node.hasNode(".") </code> applied to the root node must return
     * <code>true</code>.
     *
     * @throws RepositoryException
     */
    public void testRootHasNodeDot() throws RepositoryException {
        Node root = superuser.getRootNode();
        assertTrue("Node.hasNode(\".\") must return true.", root.hasNode(DOT));
    }

    /**
     * <code>Node.getNode(".") </code> applied to the root node must return
     * the same <code>Node</code> again.
     *
     * @throws RepositoryException
     */
    public void testRootGetNodeDot() throws RepositoryException {
        Node root = superuser.getRootNode();
        assertTrue("Node.getNode(\".\") must return the same node", root.getNode(DOT).isSame(root));
    }

    /**
     * <code>Node.getNode("..") </code> applied to the root node must throw
     * <code>PathNotFoundException</code>.
     *
     * @throws RepositoryException
     */
     public void testRootGetNodeDotDot() throws RepositoryException {
         Node root = superuser.getRootNode();
         try {
             root.getNode(DOTDOT);
             fail("Root does not have a parent node. <root>.getNode(\"..\") must fail.");
         } catch (RepositoryException e) {
             // ok.
         }
    }

    /**
     * <code>Node.hasNode(".") </code> applied to any test node must return
     * <code>true</code>.
     *
     * @throws RepositoryException
     */
    public void testHasNodeDot() throws RepositoryException {
        assertTrue("Node.hasNode(\".\") must return true.", testRootNode.hasNode(DOT));
    }

    /**
     * <code>Node.getNode(".") </code> applied to any test node must return
     * the same <code>Node</code> again.
     *
     * @throws RepositoryException
     */
    public void GetNodeDot() throws RepositoryException {
        assertTrue("Node.getNode(\".\") must return the same node.", testRootNode.getNode(DOT).isSame(testRootNode));
    }

    /**
     * <code>Node.getNode("..") </code> applied to any test node must the same
     * node as {@link Node#getParent()}.
     *
     * @throws RepositoryException
     * @throws NotExecutableException if the parent node cannot be retrieved
     * with {@link Node#getParent()}.
     */
    public void testGetNodeDotDot() throws RepositoryException, NotExecutableException {
        Node parent;
        try {
            parent = testRootNode.getParent();
        } catch (Exception e) {
            throw new NotExecutableException();
        }
        assertTrue("Node.getNode(\"..\") must return the parent.", testRootNode.getNode(DOTDOT).isSame(parent));
    }

    /**
     * <code>Node.hasProperty(".") </code> applied to any test node must return
     * <code>false</code>.
     *
     * @throws RepositoryException
     */
    public void testHasPropertyDot() throws RepositoryException {
        assertFalse("Node.hasProperty(\".\") must return false.", testRootNode.hasProperty(DOT));
    }

    /**
     * <code>Node.getProperty(".") </code> applied to any test node must throw
     * <code>PathNotFoundException</code>.
     *
     * @throws RepositoryException
     */
    public void testGetPropertyDot() throws RepositoryException {
         try {
             testRootNode.getProperty(DOT);
             fail("A node must never have a property \".\".");
         } catch (PathNotFoundException e) {
             // ok.
         }
    }

    /**
     * <code>Node.hasProperty("..") </code> applied to any test node must return
     * <code>false</code>.
     *
     * @throws RepositoryException
     */
    public void testHasPropertyDotDot() throws RepositoryException {
        assertFalse("Node.hasProperty(\"..\") must return false.", testRootNode.hasProperty(DOTDOT));
    }

    /**
     * <code>Node.getProperty("..") </code> applied to any test node must throw
     * <code>PathNotFoundException</code>.
     *
     * @throws RepositoryException
     */
    public void testGetPropertyDotDot() throws RepositoryException {
        try {
             testRootNode.getProperty(DOTDOT);
             fail("A node must never have a property \"..\".");
         } catch (PathNotFoundException e) {
             // ok.
         }
    }

    /**
     * <code>Node.getNode("./testNodeName") </code> applied to the parent
     * of any node with name 'testNodeName' must return the same node.
     *
     * @throws RepositoryException
     * @throws NotExecutableException if the parent cannot be retrieved or if
     * the parent has more than 1 node with the given name.
     */
    public void testGetNodeDotSlashName() throws RepositoryException, NotExecutableException {
        Node parent;
        try {
            parent = testRootNode.getParent();
            NodeIterator it = parent.getNodes(testRootNode.getName());
            int cnt = 0;
            while (it.hasNext() && cnt <= 1) {
                it.nextNode();
                cnt++;
            }
            if (cnt > 1) {
               throw new NotExecutableException();
            }
        } catch (Exception e) {
            throw new NotExecutableException();
        }
        String otherRelPath = DOT + "/" + testRootNode.getName();
        assertTrue(testRootNode.isSame(parent.getNode(otherRelPath)));
    }

    /**
     * <code>Node.getNode("../" + Node.getName()) </code> applied to any test
     * node must return the test node.
     *
     * @throws RepositoryException
     */
    public void testGetNodeDotDotSlashName() throws RepositoryException, NotExecutableException {
        String otherRelPath = DOTDOT + "/" + testRootNode.getName();
        if (testRootNode.getIndex() > 1) {
            otherRelPath = otherRelPath + "[" + testRootNode.getIndex() + "]";
        }
        assertTrue(testRootNode.isSame(testRootNode.getNode(otherRelPath)));
    }

    /**
     * <code>Node.getProperty("./jcr:primaryType") </code> applied to any
     * test node must return the same Property as
     * {@link Node#getProperty(String) Node.getProperty("jcr:primaryType")}.
     *
     * @throws RepositoryException
     */
    public void testGetPropertyDotSlashName() throws RepositoryException {
        Property pt = testRootNode.getProperty(jcrPrimaryType);
        String otherRelPath = DOT + "/" + jcrPrimaryType;
        assertTrue(pt.isSame(testRootNode.getProperty(otherRelPath)));
    }

    /**
     * <code>Node.getProperty("../jcr:primaryType") </code> applied to any
     * test node must return the same Property as
     * {@link Node#getProperty(String) Node.getParent().getProperty("jcr:primaryType")}.
     *
     * @throws RepositoryException
     * @throws NotExecutableException if the parent cannot be retrieved.
     */
    public void testGetPropertyDotDotSlashName() throws RepositoryException, NotExecutableException {
        Node parent;
        try {
            parent = testRootNode.getParent();
        } catch (Exception e) {
            throw new NotExecutableException();
        }

        Property pt = parent.getProperty(jcrPrimaryType);
        String otherRelPath = DOTDOT + "/" + jcrPrimaryType;
        assertTrue(pt.isSame(testRootNode.getProperty(otherRelPath)));
    }
}