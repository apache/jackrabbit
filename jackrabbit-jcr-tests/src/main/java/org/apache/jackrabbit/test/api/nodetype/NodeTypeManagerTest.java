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
package org.apache.jackrabbit.test.api.nodetype;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * Tests if the {@link NodeTypeManager} properly returns primary types an mixin
 * types.
 *
 * @test
 * @sources NodeTypeManagerTest.java
 * @executeClass org.apache.jackrabbit.test.api.nodetype.NodeTypeManagerTest
 * @keywords level1
 */
public class NodeTypeManagerTest extends AbstractJCRTest {

    /**
     * The node type manager we use for the test cases
     */
    private NodeTypeManager manager;

    /**
     * The session for reading from the default workspace
     */
    private Session session;

    /**
     * Sets up the fixture for the test cases.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();

        session = helper.getReadOnlySession();
        manager = session.getWorkspace().getNodeTypeManager();
    }

    /**
     * Releases the session aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
            session = null;
        }
        manager = null;
        super.tearDown();
    }

    /**
     * Test if getNodeType(String nodeTypeName) returns the expected NodeType and
     * if a NoSuchTypeException is thrown if no according node type is existing
     */
    public void testGetNodeType() throws RepositoryException {
        NodeType type = manager.getAllNodeTypes().nextNodeType();
        assertEquals("getNodeType(String nodeTypeName) does not return correct " +
                "NodeType",
                manager.getNodeType(type.getName()).getName(),
                type.getName());


        StringBuffer notExistingName = new StringBuffer("X");
        NodeTypeIterator types = manager.getAllNodeTypes();
        while (types.hasNext()) {
            // build a name which is for sure not existing
            // (":" of namespace prefix will be replaced later on)
            notExistingName.append(types.nextNodeType().getName());
        }
        try {
            manager.getNodeType(notExistingName.toString().replaceAll(":", ""));
            fail("getNodeType(String nodeTypeName) must throw a " +
                    "NoSuchNodeTypeException if no according NodeType " +
                    "does exist");
        } catch (NoSuchNodeTypeException e) {
            // success
        }
    }


    /**
     * Test if getAllNodeTypes() returns all primary and mixin node types
     */
    public void testGetAllNodeTypes() throws RepositoryException {
        long sizeAll = getSize(manager.getAllNodeTypes());
        long sizePrimary = getSize(manager.getPrimaryNodeTypes());
        long sizeMixin = getSize(manager.getMixinNodeTypes());

        assertEquals("sizeAll() must return all primary and mixin node types:",
                sizePrimary + sizeMixin,
                sizeAll);
    }


    /**
     * Test if getPrimaryNodeTypes does not return any mixin node types
     */
    public void testGetPrimaryNodeTypes() throws RepositoryException {
        NodeTypeIterator types = manager.getPrimaryNodeTypes();
        while (types.hasNext()) {
            assertFalse("getPrimaryNodeTypes() must not return mixin " +
                    "node types",
                    types.nextNodeType().isMixin());
        }
    }


    /**
     * Test if getMixinNodeTypes does return exclusively mixin node types
     */
    public void testGetMixinNodeTypes() throws RepositoryException {
        NodeTypeIterator types = manager.getMixinNodeTypes();
        while (types.hasNext()) {
            assertTrue("getMixinNodeTypes() must return exclusively mixin " +
                    "node types",
                    types.nextNodeType().isMixin());
        }
    }


}