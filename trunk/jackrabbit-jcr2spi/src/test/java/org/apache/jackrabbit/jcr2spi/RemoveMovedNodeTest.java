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

import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** <code>RemoveMovedNodeTest</code>... */
public class RemoveMovedNodeTest extends AbstractMoveTest {

    private static Logger log = LoggerFactory.getLogger(RemoveMovedNodeTest.class);

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    protected boolean isSessionMove() {
        return true;
    }

    public void testRemove() throws RepositoryException {
        String srcPath = moveNode.getPath();
        doMove(srcPath, destinationPath);

        // now remove the moved node
        testRootNode.getSession().getItem(destinationPath).remove();
        assertFalse(superuser.itemExists(srcPath));
        assertFalse(superuser.itemExists(destinationPath));

        testRootNode.save();

        assertFalse(superuser.itemExists(srcPath));
        assertFalse(superuser.itemExists(destinationPath));
        assertFalse(destParentNode.isModified());
        assertFalse(srcParentNode.isModified());
    }

    public void testRevert() throws RepositoryException {
        String srcPath = moveNode.getPath();
        doMove(srcPath, destinationPath);

        // now remove the moved node
        testRootNode.getSession().getItem(destinationPath).remove();
        testRootNode.refresh(false);

        assertTrue(superuser.itemExists(srcPath));
        assertFalse(superuser.itemExists(destinationPath));

        assertFalse(moveNode.isModified());
        assertFalse(destParentNode.isModified());
        assertFalse(srcParentNode.isModified());
    }

    public void testRefresh() throws RepositoryException {
        String srcPath = moveNode.getPath();
        doMove(srcPath, destinationPath);

        // now remove the moved node
        testRootNode.getSession().getItem(destinationPath).remove();
        testRootNode.refresh(true);

        assertFalse(superuser.itemExists(srcPath));
        assertFalse(superuser.itemExists(destinationPath));

        // after removal the 'modified' flag is removed from the moved node
        assertFalse(moveNode.isModified());
        // however: parent states are still modified
        assertTrue(destParentNode.isModified()); // TODO: check if correct.
        assertTrue(srcParentNode.isModified());
    }

    public void testRemoveSrcParent() throws RepositoryException {
        String srcPath = moveNode.getPath();
        doMove(srcPath, destinationPath);

        // now remove the moved node
        srcParentNode.remove();

        assertFalse(superuser.itemExists(srcPath));
        assertTrue(superuser.itemExists(destinationPath));

        assertTrue(moveNode.isModified());
        assertTrue(destParentNode.isModified());

        // a removed item is not modified (although it was modified by the move above)
        assertFalse(srcParentNode.isModified());

        testRootNode.refresh(false);
        assertTrue(superuser.itemExists(srcPath));
        assertFalse(superuser.itemExists(destinationPath));

        assertFalse(moveNode.isModified());
        assertFalse(destParentNode.isModified());
        assertFalse(srcParentNode.isModified());
    }
}