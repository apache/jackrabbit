/*
 * Copyright 2004 The Apache Software Foundation.
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;

/**
 * Test cases for the root node.
 *
 * @test
 * @sources RootNodeTest.java
 * @executeClass org.apache.jackrabbit.test.api.RootNodeTest
 * @keywords level1
 */
public class RootNodeTest extends AbstractJCRTest {

    Node rootNode;

    protected void setUp() throws Exception {
        super.setUp();
        rootNode = helper.getReadOnlySession().getRootNode();
    }

    /**
     * Test if name of root node is empty string.
     */
    public void testGetName() throws RepositoryException {
        assertEquals("The name of the root node must be an empty string.", "", rootNode.getName());
    }

    /**
     * Test if depth of root node is 0.
     */
    public void testGetDepth() throws RepositoryException {
        assertEquals("The depth of the root node must be equal to 0.", 0, rootNode.getDepth());
    }

    /**
     * Test if root node has no parent and throws ItemNotFoundException.
     */
    public void testGetParent() throws RepositoryException {
        try {
            rootNode.getParent();
            fail("The root node may not have a parent.");
        } catch (ItemNotFoundException e) {
            // success: ItemNotFoundException as required by the specification.
        }
    }
}