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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.jcr2spi.state.Status;

import javax.jcr.Session;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;

/** <code>ExternalModificationTest</code>... */
public class ExternalModificationTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(ExternalModificationTest.class);

    private Node destParentNode;
    private Node refNode;
    private Session testSession;

    protected void setUp() throws Exception {
        super.setUp();

        // create a referenceable node and destination parent.
        destParentNode = testRootNode.addNode(nodeName1, testNodeType);
        refNode = testRootNode.addNode(nodeName2, getProperty("nodetype2"));
        refNode.addMixin(mixReferenceable);
        testRootNode.save();

        testSession = helper.getReadWriteSession();
    }

    protected void tearDown() throws Exception {
        if (testSession != null) {
            testSession.logout();
        }
        super.tearDown();
    }

    private static boolean isItemStatus(Item item, int status) throws NotExecutableException {
        if (!(item instanceof ItemImpl)) {
            throw new NotExecutableException("org.apache.jackrabbit.jcr2spi.ItemImpl expected");
        }
        int st = ((ItemImpl) item).getItemState().getStatus();
        return st == status;
    }

    private static void assertItemStatus(Item item, int status) throws NotExecutableException {
        if (!(item instanceof ItemImpl)) {
            throw new NotExecutableException("org.apache.jackrabbit.jcr2spi.ItemImpl expected");
        }
        int st = ((ItemImpl) item).getItemState().getStatus();
        assertEquals("Expected status to be " + Status.getName(status) + ", was " + Status.getName(st), status, st);
    }

    public void testMovedReferenceableNode() throws RepositoryException, NotExecutableException {
        Node refNode2 = (Node) testSession.getItem(refNode.getPath());

        superuser.move(refNode.getPath(), destParentNode.getPath() + "/" + nodeName2);
        superuser.save();

        try {
            // modify some prop of the moved node with session 2
            refNode2.setProperty(propertyName1, "test");
            testSession.save();
            // node has been automatically moved to new place
            // -> check if the parent is correct.
            assertTrue(testSession.getItem(destParentNode.getPath()).isSame(refNode.getParent()));
        } catch (InvalidItemStateException e) {
            // no automatic move of the externally moved node. ok.
            log.debug(e.getMessage());
        }
    }

    public void testRefreshMovedReferenceableNode() throws RepositoryException, NotExecutableException {
        Node refNode2 = (Node) testSession.getItem(refNode.getPath());

        superuser.move(refNode.getPath(), destParentNode.getPath() + "/" + nodeName2);
        superuser.save();

        try {
            refNode2.refresh(true);
            Node parent = refNode2.getParent();
            if (parent.isSame(testSession.getItem(destParentNode.getPath()))) {
                // node has been automatically moved to new place
                assertItemStatus(refNode2, Status.EXISTING);
            } else {
                assertItemStatus(refNode2, Status.REMOVED);
            }
        } catch (InvalidItemStateException e) {
            // no automatic move of the externally moved node. ok.
            log.debug(e.getMessage());
            // since node had no pending changes -> status should be changed
            // to REMOVED.
            assertItemStatus(refNode2, Status.REMOVED);
        }
    }

    public void testConflictingAddMixin() throws RepositoryException, NotExecutableException {
        Node refNode2 = (Node) testSession.getItem(refNode.getPath());
        refNode2.addMixin(mixLockable);

        superuser.move(refNode.getPath(), destParentNode.getPath() + "/" + nodeName2);
        superuser.save();

        try {
            refNode2.refresh(true);
            Node parent = refNode2.getParent();
            if (parent.isSame(testSession.getItem(destParentNode.getPath()))) {
                // node has been automatically moved to new place
                assertItemStatus(refNode2, Status.EXISTING_MODIFIED);
            } else if (!isItemStatus(refNode2, Status.EXISTING_MODIFIED)) {
                // external removal was detected either by observation or be
                // batch-reading the parent -> status must be stale.
                assertItemStatus(refNode2, Status.STALE_DESTROYED);
            }
        } catch (InvalidItemStateException e) {
            // no automatic move of the externally moved node. ok.
            log.debug(e.getMessage());
            // since refNode2 has pending modifications its status should be
            // changed to STALE_DESTROYED.
            assertItemStatus(refNode2, Status.STALE_DESTROYED);
            Node refAgain = testSession.getNodeByUUID(refNode.getUUID());
            assertTrue(refAgain.getParent().isSame(testSession.getItem(destParentNode.getPath())));
            assertFalse(refAgain.isNodeType(mixLockable));
        }
    }

    public void testStaleDestroyed() throws RepositoryException, NotExecutableException {
        Node refNode2 = (Node) testSession.getItem(refNode.getPath());
        refNode2.addMixin(mixLockable);

        superuser.move(refNode.getPath(), destParentNode.getPath() + "/" + nodeName2);
        superuser.save();

        try {
            refNode2.refresh(true);
            Node parent = refNode2.getParent();
        } catch (InvalidItemStateException e) {
        }

        if (isItemStatus(refNode2, Status.STALE_DESTROYED)) {
            try {
                refNode2.refresh(false);
                fail();
            } catch (InvalidItemStateException e) {
                // correct behaviour
            }
        }
    }

    public void testStaleDestroyed2() throws RepositoryException, NotExecutableException {
        Node refNode2 = (Node) testSession.getItem(refNode.getPath());
        refNode2.addMixin(mixLockable);

        superuser.move(refNode.getPath(), destParentNode.getPath() + "/" + nodeName2);
        superuser.save();

        try {
            refNode2.refresh(true);
            Node parent = refNode2.getParent();
        } catch (InvalidItemStateException e) {
        }

        if (isItemStatus(refNode2, Status.STALE_DESTROYED)) {
            testSession.refresh(false);
            assertItemStatus(refNode2, Status.REMOVED);
        }
    }
}