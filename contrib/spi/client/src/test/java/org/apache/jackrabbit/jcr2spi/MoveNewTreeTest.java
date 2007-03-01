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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Item;
import javax.jcr.PathNotFoundException;

/**
 * <code>MoveTreeTest</code>...
 */
public class MoveNewTreeTest extends AbstractMoveTreeTest {

    private static Logger log = LoggerFactory.getLogger(MoveNewTreeTest.class);

    protected boolean saveBeforeMove() {
        return false;
    }

    protected boolean isSessionMove() {
        return true;
    }

   public void testTreeAncestors() throws RepositoryException {
        int degree = destParentNode.getDepth();
        Item ancestor = childNode.getAncestor(degree);
        assertTrue("Moving a node must move all child items as well.", ancestor.isSame(destParentNode));
        ancestor = childProperty.getAncestor(degree);
        assertTrue("Moving a node must move all child items as well.", ancestor.isSame(destParentNode));
        ancestor = grandChildNode.getAncestor(degree);
        assertTrue("Moving a node must move all child items as well.", ancestor.isSame(destParentNode));

    }

    public void testTreeEntries() throws RepositoryException {
        Item item = superuser.getItem(destinationPath + "/" + nodeName2);
        assertTrue("Moving a node must move all child items as well.", childNode.isSame(item));
        item = superuser.getItem(destinationPath + "/" + propertyName2);
        assertTrue("Moving a node must move all child items as well.", childProperty.isSame(item));
        item = superuser.getItem(destinationPath + "/" + nodeName2 + "/" + nodeName3);
        assertTrue("Moving a node must move all child items as well.", grandChildNode.isSame(item));
    }

    public void testOldPath() throws RepositoryException {
        try {
            superuser.getItem(srcPath + "/" + nodeName2 + "/" + nodeName3);
            fail("Moving a node must move all child items as well.");
        } catch (PathNotFoundException e) {
            // ok
        }
    }

    /**
     * Reverting the MOVE of a NEW-node must also remove the Node at its
     * original position.
     *
     * @throws RepositoryException
     */
    public void testRevertRemovedFromSrc() throws RepositoryException {
        superuser.refresh(false);
        assertFalse("Reverting move of a new node must remove the node from both positions.", superuser.itemExists(srcPath));
    }

    /**
     * Reverting the MOVE of a NEW-node must remove the Node from the destination.
     *
     * @throws RepositoryException
     */
    public void testRevertRemovedFromDestination() throws RepositoryException {
        superuser.refresh(false);
        assertFalse("Reverting move of a new node must remove the node from both positions.", superuser.itemExists(destinationPath));
    }

    public void testRevertInvalidatedMovedTree() throws RepositoryException {
        superuser.refresh(false);
        try {
            childNode.getAncestor(0);
            fail("Reverting move of a new node must remove the tree completely");
        } catch (RepositoryException e) {
            // OK
        }
        try {
            childProperty.getAncestor(0);
            fail("Reverting move of a new node must remove the tree completely");
        } catch (RepositoryException e) {
            // OK
        }
        try {
            grandChildNode.getAncestor(0);
            fail("Reverting move of a new node must remove the tree completely");
        } catch (RepositoryException e) {
            // OK
        }
    }
}
