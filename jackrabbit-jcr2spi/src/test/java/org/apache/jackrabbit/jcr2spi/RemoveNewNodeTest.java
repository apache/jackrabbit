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

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>RemoveNodeTest</code>...
 */
public class RemoveNewNodeTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(RemoveNewNodeTest.class);

    protected Node removeNode;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (testRootNode.hasNode(nodeName1)) {
            throw new NotExecutableException("Parent node must not yet contain a child node '" + nodeName1 + "'.");
        }
        removeNode = testRootNode.addNode(nodeName1, testNodeType);
    }

    @Override
    protected void tearDown() throws Exception {
        removeNode = null;
        super.tearDown();
    }

    /**
     * Removes a transient node using {@link javax.jcr.Node#remove()}.
     */
    public void testRemoveNode() throws RepositoryException {
        // create the transient node
        removeNode.remove();
        try {
            testRootNode.getNode(nodeName1);
            fail("Removed transient node should no longer be accessible from parent node.");
        } catch (PathNotFoundException e) {
            // ok , works as expected
        }
    }

    /**
     * Test if a node, that has be transiently added and removed is not 'New'.
     */
    public void testNotNewRemovedNode() throws RepositoryException {
        removeNode.remove();
        assertFalse("Removed transient node must not be 'new'.", removeNode.isNew());
    }

    /**
     * Test if a node, that has be transiently added and removed is not 'Modified'.
     */
    public void testNotModifiedRemovedNode() throws RepositoryException {
        removeNode.remove();
        assertFalse("Removed transient node must not be 'modified'.", removeNode.isModified());
    }

    /**
     * A removed transient node must throw InvalidItemStateException upon any call to a
     * node specific method.
     */
    public void testInvalidStateRemovedNode() throws RepositoryException {
        removeNode.remove();
        try {
            removeNode.getName();
            fail("Calling getName() on a removed node must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }

        try {
            removeNode.getPath();
            fail("Calling getPath() on a removed node must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }

        try {
            removeNode.getPrimaryNodeType();
            fail("Calling getPrimaryNodeType() on a removed node must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }

        try {
            removeNode.getProperty(jcrPrimaryType);
            fail("Calling getProperty(String) on a removed node must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }

        try {
            removeNode.save();
            fail("Calling save() on a removed node must throw InvalidItemStateException.");
        } catch (InvalidItemStateException e) {
            //ok
        }
    }
}
