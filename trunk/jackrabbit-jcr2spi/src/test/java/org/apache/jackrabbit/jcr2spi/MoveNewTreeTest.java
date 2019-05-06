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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>MoveTreeTest</code>...
 */
public class MoveNewTreeTest extends AbstractMoveTreeTest {

    private static Logger log = LoggerFactory.getLogger(MoveNewTreeTest.class);

    @Override
    protected boolean saveBeforeMove() {
        return false;
    }

    @Override
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

    public void testRefreshMovedTree() throws RepositoryException {
        testRootNode.refresh(true);
        String msg = "Refresh must not revert a moved tree.";

        assertFalse(msg, superuser.itemExists(srcPath + "/" + nodeName2 + "/" + nodeName3));
        int degree = destParentNode.getDepth();

        List<Item> l = new ArrayList<Item>();
        l.add(childNode);
        l.add(childProperty);
        l.add(grandChildNode);

        for (Iterator<Item> it = l.iterator(); it.hasNext();) {
            Item item = it.next();
            assertTrue(msg, item.isNew());
            assertTrue(msg, childNode.getAncestor(degree).isSame(destParentNode));
        }
    }
}
