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
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** <code>ExternalModificationTest</code>... */
public class ExternalModificationTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(ExternalModificationTest.class);

    private Node destParentNode;
    private Node refNode;
    private Session testSession;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create a referenceable node and destination parent.
        destParentNode = testRootNode.addNode(nodeName1, testNodeType);
        refNode = testRootNode.addNode(nodeName2, getProperty("nodetype2"));
        refNode.addMixin(mixReferenceable);
        testRootNode.save();

        testSession = getHelper().getReadWriteSession();
    }

    @Override
    protected void tearDown() throws Exception {
        if (testSession != null) {
            testSession.logout();
            testSession = null;
        }
        destParentNode = null;
        refNode = null;
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

        testSession.getItem(destParentNode.getPath() + "/" + nodeName2);

        assertItemStatus(refNode2, Status.STALE_DESTROYED);
        try {
            refNode2.refresh(false);
            fail();
        } catch (InvalidItemStateException e) {
            // correct behaviour
        }
    }

    public void testStaleDestroyed2() throws RepositoryException, NotExecutableException {
        Node refNode2 = (Node) testSession.getItem(refNode.getPath());
        refNode2.addMixin(mixLockable);

        superuser.move(refNode.getPath(), destParentNode.getPath() + "/" + nodeName2);
        superuser.save();

        testSession.getItem(destParentNode.getPath() + "/" + nodeName2);

        assertItemStatus(refNode2, Status.STALE_DESTROYED);
        testSession.refresh(false);
        assertItemStatus(refNode2, Status.REMOVED);
    }

    public void testStaleDestroyed3() throws RepositoryException, NotExecutableException {
        String uuid = refNode.getUUID();

        Node refNode2 = (Node) testSession.getItem(refNode.getPath());
        assertTrue(refNode2.isSame(testSession.getNodeByUUID(uuid)));
        // add some modification
        refNode2.addMixin(mixLockable);

        String srcPath = refNode.getPath();
        String destPath = destParentNode.getPath() + "/" + nodeName2;
        superuser.move(srcPath, destPath);
        superuser.save();

        testSession.getItem(destPath);

        assertItemStatus(refNode2, Status.STALE_DESTROYED);
        // the uuid must be transferred to the 'moved' node
        Node n = testSession.getNodeByUUID(uuid);
        assertTrue(n.isSame(testSession.getItem(destPath)));
        // assertSame(refNode2, testSession.getItem(srcPath));
        assertTrue(refNode2.isSame(testSession.getItem(srcPath)));
    }

    public void testExternalRemoval() throws RepositoryException, NotExecutableException {
        String uuid = refNode.getUUID();
        Node refNode2 = testSession.getNodeByUUID(uuid);

        String srcPath = refNode.getPath();
        String destPath = destParentNode.getPath() + "/" + nodeName2;
        superuser.move(srcPath, destPath);
        superuser.save();

        try {
            refNode2.refresh(true);
            Node parent = refNode2.getParent();
        } catch (InvalidItemStateException e) {
        }

        assertItemStatus(refNode2, Status.REMOVED);
        // the uuid must be transferred to the 'moved' node
        Node n = testSession.getNodeByUUID(uuid);
        assertTrue(n.isSame(testSession.getItem(destPath)));
    }

    public void testExternalRemoval2() throws RepositoryException, NotExecutableException {
        Node childN = refNode.addNode(nodeName3);
        Property p = childN.setProperty(propertyName1, "anyvalue");
        refNode.save();

        String uuid = refNode.getUUID();
        Node refNode2 = testSession.getNodeByUUID(uuid);
        Node c2 =  (Node) testSession.getItem(childN.getPath());
        Property p2 = (Property) testSession.getItem(p.getPath());
        // transiently remove the property -> test effect of external removal.
        p2.remove();

        String srcPath = refNode.getPath();
        String destPath = destParentNode.getPath() + "/" + nodeName2;
        superuser.move(srcPath, destPath);
        superuser.save();

        try {
            refNode2.refresh(true);
            Node parent = refNode2.getParent();
        } catch (InvalidItemStateException e) {
        }

        assertItemStatus(refNode2, Status.REMOVED);
        assertItemStatus(c2, Status.STALE_DESTROYED);
        assertItemStatus(p2, Status.REMOVED);
    }

    public void testExternalRemoval3() throws RepositoryException, NotExecutableException {
        Node childN = refNode.addNode(nodeName3);
        Property p = childN.setProperty(propertyName1, "anyvalue");
        refNode.save();

        String uuid = refNode.getUUID();
        Node refNode2 = testSession.getNodeByUUID(uuid);
        Node c2 =  (Node) testSession.getItem(childN.getPath());
        Property p2 = (Property) testSession.getItem(p.getPath());
        // transiently modify  -> test effect of external removal.
        p2.setValue("changedValue");

        String srcPath = refNode.getPath();
        String destPath = destParentNode.getPath() + "/" + nodeName2;
        superuser.move(srcPath, destPath);
        superuser.save();

        try {
            refNode2.refresh(true);
            Node parent = refNode2.getParent();
        } catch (InvalidItemStateException e) {
        }

        assertItemStatus(refNode2, Status.REMOVED);
        assertItemStatus(c2, Status.REMOVED);
        assertItemStatus(p2, Status.STALE_DESTROYED);
        assertEquals("changedValue", p2.getString());
    }

    public void testNewItemsUponStaleDestroyed() throws RepositoryException, NotExecutableException {
        String uuid = refNode.getUUID();
        Node refNode2 = (Node) testSession.getItem(refNode.getPath());
        refNode2.addMixin(mixLockable);

        Node childN = refNode2.addNode(nodeName3);
        String childNPath = childN.getPath();

        Property childP = refNode2.setProperty(propertyName2, "someValue");
        String childPPath = childP.getPath();

        String destPath = destParentNode.getPath() + "/" + nodeName2;
        superuser.move(refNode.getPath(), destPath);
        superuser.save();

        testSession.refresh(true);
        testSession.getItem(destPath);

        assertItemStatus(refNode2, Status.STALE_DESTROYED);
        assertItemStatus(refNode2.getProperty(jcrMixinTypes), Status.STALE_DESTROYED);
        assertItemStatus(childN, Status.NEW);
        assertItemStatus(childP, Status.NEW);
        assertItemStatus(childN.getProperty(jcrPrimaryType), Status.NEW);

        assertTrue(testSession.itemExists(childNPath));
        assertTrue(childN.isSame(testSession.getItem(childNPath)));

        assertTrue(testSession.itemExists(childPPath));
        assertTrue(childP.isSame(testSession.getItem(childPPath)));

        testSession.refresh(false);

        assertItemStatus(childN, Status.REMOVED);
        assertItemStatus(childP, Status.REMOVED);
        assertFalse(testSession.itemExists(childNPath));
        assertFalse(testSession.itemExists(childPPath));
    }

    public void testChildItemsUponStaleDestroyed() throws RepositoryException, NotExecutableException {
        Node cNode = refNode.addNode(nodeName3);
        Node cNode2 = cNode.addNode(nodeName4);
        refNode.save();

        String uuid = refNode.getUUID();
        Node refNode2 = (Node) testSession.getItem(refNode.getPath());
        refNode2.addMixin(mixLockable);

        Node child =  (Node) testSession.getItem(cNode.getPath());
        Node child2 = (Node) testSession.getItem(cNode2.getPath());
        Node child3 = child2.addNode(nodeName4);
        String child3Path = child3.getPath();

        String destPath = destParentNode.getPath() + "/" + nodeName2;
        superuser.move(refNode.getPath(), destPath);
        superuser.save();

        testSession.refresh(true);
        testSession.getItem(destPath);

        assertItemStatus(refNode2, Status.STALE_DESTROYED);
        assertItemStatus(refNode2.getProperty(jcrMixinTypes), Status.STALE_DESTROYED);
        assertItemStatus(child, Status.REMOVED);
        assertItemStatus(child2, Status.STALE_DESTROYED);
        assertItemStatus(child3, Status.NEW);
        assertItemStatus(child3.getProperty(jcrPrimaryType), Status.NEW);

        testSession.refresh(false);

        assertItemStatus(child2, Status.REMOVED);
        assertItemStatus(child3, Status.REMOVED);
    }

    public void testUnmodifiedAncestorRemoved() throws RepositoryException, NotExecutableException {
        String uuid = refNode.getUUID();
        Node n3 = refNode.addNode(nodeName3, testNodeType);
        refNode.save();

        Node refNode2 = (Node) testSession.getItem(refNode.getPath());
        // add transient modification to non-referenceable child node
        Node node3 = (Node) testSession.getItem(n3.getPath());
        node3.addMixin(mixLockable);

        // add new child node and child property below
        Node childN = node3.addNode(nodeName3);
        String childNPath = childN.getPath();

        Property childP = node3.setProperty(propertyName2, "someValue");
        String childPPath = childP.getPath();

        // externally move the 'refNode' in order to provoke uuid-conflict
        // in testSession -> refNode2 gets removed, since it doesn't have
        // transient modifications.
        String destPath = destParentNode.getPath() + "/" + nodeName2;
        superuser.move(refNode.getPath(), destPath);
        superuser.save();

        testSession.refresh(true);
        testSession.getItem(destPath);

        assertItemStatus(refNode2, Status.REMOVED);
        assertItemStatus(node3, Status.STALE_DESTROYED);
        assertItemStatus(childN, Status.NEW);
        assertItemStatus(childP, Status.NEW);

        // since 'refNode2' is removed -> child items must not be accessible
        // any more.
        assertFalse(testSession.itemExists(childNPath));
        assertFalse(testSession.itemExists(childPPath));

        // revert all pending changes...
        testSession.refresh(false);
        // must mark all modified/new items as removed.
        assertItemStatus(node3, Status.REMOVED);
        assertItemStatus(childN, Status.REMOVED);
        assertItemStatus(childP, Status.REMOVED);
    }
}