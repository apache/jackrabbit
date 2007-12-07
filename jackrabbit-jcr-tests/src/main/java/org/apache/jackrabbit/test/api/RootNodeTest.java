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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Session;

/**
 * Test cases for the root node.
 *
 * @test
 * @sources RootNodeTest.java
 * @executeClass org.apache.jackrabbit.test.api.RootNodeTest
 * @keywords level1
 */
public class RootNodeTest extends AbstractJCRTest {

    /** A readonly session for the default workspace */
    private Session session;

    /** The root node of the default workspace */
    private Node rootNode;

    /**
     * Sets up the fixture for the test cases.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
        session = helper.getReadOnlySession();
        rootNode = session.getRootNode();
    }

    /**
     * Releases the session aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
            session = null;
        }
        rootNode = null;
        super.tearDown();
    }

    /**
     * Test if name of root node is empty string.
     */
    public void testGetName() throws RepositoryException {
        assertEquals("The name of the root node must be an empty string.", "", rootNode.getName());
    }

    /**
     * Test if the path of the root node is '/' (slash)
     */
    public void testGetPath() throws RepositoryException {
        assertEquals("The path of the root node must be a single slash.", "/", rootNode.getPath());
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