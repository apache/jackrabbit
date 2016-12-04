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

import javax.jcr.Item;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>MoveTreeTest</code>...
 */
public class MoveTreeTest extends AbstractMoveTreeTest {

    private static Logger log = LoggerFactory.getLogger(MoveTreeTest.class);

    @Override
    protected boolean saveBeforeMove() {
        return true;
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
            superuser.getItem(srcPath + "/" + nodeName2);
            fail("Moving a node must move all child items as well.");
        } catch (PathNotFoundException e) {
            // ok
        }
    }

    public void testOldPropertyPath() throws RepositoryException {
        try {
            superuser.getItem(srcPath + "/" + propertyName2);
            fail("Moving a node must move all child items as well.");
        } catch (PathNotFoundException e) {
            // ok
        }
    }

    public void testOldChildPath() throws RepositoryException {
        for (int i = 0; i < childPaths.size(); i++) {
            String path = childPaths.get(i).toString();
            assertFalse(superuser.itemExists(path));
            try {
                superuser.getItem(path);
                fail("Moving a node must move all child items as well.");
            } catch (PathNotFoundException e) {
                // ok
            }
        }
    }

    public void testOldChildPropertyPath() throws RepositoryException {
        for (int i = 0; i < childPaths.size(); i++) {
            String propPath = childPaths.get(i).toString() + "/" + jcrPrimaryType;
            assertFalse(superuser.itemExists(propPath));
            try {
                superuser.getItem(propPath);
                fail("Moving a node must move all child items as well.");
            } catch (PathNotFoundException e) {
                // ok
            }
        }
    }

    public void testAncestorAfterRevert() throws RepositoryException {
        superuser.refresh(false);
        Item ancestor = grandChildNode.getAncestor(srcParentNode.getDepth());
        assertTrue("Reverting a move-operation must move the tree back.", ancestor.isSame(srcParentNode));
    }

    public void testDestinationAfterRevert() throws RepositoryException {
        superuser.refresh(false);
        try {
            superuser.getItem(destinationPath + "/" + propertyName2);
            fail("Reverting a move-operation must move the tree back.");
        } catch (PathNotFoundException e) {
            // ok
        }
    }
}