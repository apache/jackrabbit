/*
 * $Id$
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.PathNotFoundException;
import javax.jcr.InvalidItemStateException;

/**
 * <code>RemoveNodeTest</code>...
 */
public class RemoveNewNodeTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(RemoveNewNodeTest.class);

    protected Node removeNode;

    protected void setUp() throws Exception {
        super.setUp();

        if (testRootNode.hasNode(nodeName1)) {
            throw new NotExecutableException("Parent node must not yet contain a child node '" + nodeName1 + "'.");
        }
        removeNode = testRootNode.addNode(nodeName1, testNodeType);
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
