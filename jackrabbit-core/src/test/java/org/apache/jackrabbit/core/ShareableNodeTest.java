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
package org.apache.jackrabbit.core;

import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Tests features available with shareable nodes.
 */
public class ShareableNodeTest extends AbstractJCRTest {

    //------------------------------------------------------ specification tests

    /**
     * Verifies that Node.getIndex returns the correct index in a shareable
     * node (6.13).
     */
    public void testGetIndex() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        a2.addNode("b");
        testRootNode.save();

        // add mixin
        b1.addMixin("mix:shareable");
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b", false);

        Node[] shared = getSharedSet(b1);
        assertEquals(2, shared.length);
        b1 = shared[0];
        Node b2 = shared[1];

        // verify indices of nodes b1/b2 in shared set
        assertEquals(1, b1.getIndex());
        assertEquals(2, b2.getIndex());
    }

    /**
     * Verifies that Node.getName returns the correct name in a shareable node
     * (6.13).
     */
    public void testGetName() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.save();

        // add mixin
        b1.addMixin("mix:shareable");
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        Node[] shared = getSharedSet(b1);
        assertEquals(2, shared.length);
        b1 = shared[0];
        Node b2 = shared[1];

        // verify names of nodes b1/b2 in shared set
        assertEquals("b1", b1.getName());
        assertEquals("b2", b2.getName());
    }

    /**
     * Verifies that Node.getPath returns the correct path in a shareable
     * node (6.13).
     */
    public void testGetPath() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.save();

        // add mixin
        b1.addMixin("mix:shareable");
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        Node[] shared = getSharedSet(b1);
        assertEquals(2, shared.length);
        b1 = shared[0];
        Node b2 = shared[1];

        // verify paths of nodes b1/b2 in shared set
        assertEquals("/testroot/a1/b1", b1.getPath());
        assertEquals("/testroot/a2/b2", b2.getPath());
    }

    /**
     * Checks new API Node.getSharedSet() (6.13.1)
     */
    public void testIterateSharedSet() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.save();

        // add mixin
        b1.addMixin("mix:shareable");
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // verify shared set contains 2 items
        Node[] shared = getSharedSet(b1);
        assertEquals(2, shared.length);
    }

    /**
     * Adds the mix:shareable mixin to a node (6.13.2).
     */
    public void testAddMixin() throws Exception {
        // setup parent node and first child
        Node a = testRootNode.addNode("a");
        Node b = a.addNode("b");
        testRootNode.save();

        b.addMixin("mix:shareable");
        b.save();
    }

    /**
     * Checks new API Node.removeShare() (6.13.4).
     */
    public void testRemoveShare() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.save();

        // add mixin
        b1.addMixin("mix:shareable");
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        Node[] shared = getSharedSet(b1);
        assertEquals(2, shared.length);
        b1 = shared[0];
        Node b2 = shared[1];

        // remove b1 from shared set
        ((NodeImpl) b1).removeShare();
        a1.save();

        // verify shared set of b2 contains only 1 item, namely b2 itself
        shared = getSharedSet(b2);
        assertEquals(1, shared.length);
        assertTrue(shared[0].isSame(b2));
    }

    /**
     * Checks new API Node.removeSharedSet() (6.13.4).
     */
    public void testRemoveSharedSet() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.save();

        // add mixin
        b1.addMixin("mix:shareable");
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // remove shared set
        ((NodeImpl) b1).removeSharedSet();
        testRootNode.save();

        // verify neither a1 nor a2 contain any more children
        assertFalse(a1.hasNodes());
        assertFalse(a2.hasNodes());
    }

    /**
     * Invokes Node.removeSharedSet(), but saves only one of the parent nodes
     * of the shared set. This doesn't need to be supported according to the
     * specification (6.13.4).
     */
    public void testRemoveSharedSetSaveOneParentOnly() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.save();

        // add mixin
        b1.addMixin("mix:shareable");
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // remove shared set
        ((NodeImpl) b1).removeSharedSet();

        try {
            // save only one of the parents, should fail
            a1.save();
            fail("Removing a shared set requires saving all parents.");
        } catch (ConstraintViolationException e) {
            // expected
        }
    }

    /**
     * Verifies that shareable nodes in the same shared set have the same
     * jcr:uuid (6.13.10).
     */
    public void testSameUUID() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.save();

        // add mixin
        b1.addMixin("mix:shareable");
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        Node[] shared = getSharedSet(b1);
        assertEquals(2, shared.length);
        b1 = shared[0];
        Node b2 = shared[1];

        // verify nodes in a shared set have the same jcr:uuid
        assertTrue(b1.getUUID().equals(b2.getUUID()));
    }

    /**
     * Add a child to a shareable node and verify that another node in the
     * same shared set has the same child and is modified when the first
     * one is (6.13.11).
     */
    public void testAddChild() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.save();

        // add mixin
        b1.addMixin("mix:shareable");
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        Node[] shared = getSharedSet(b1);
        assertEquals(2, shared.length);
        b1 = shared[0];
        Node b2 = shared[1];

        // add node to b1, verify b2 is modified as well and contains that child
        b1.addNode("c");
        assertTrue(b2.isModified());
        assertTrue(b2.hasNode("c"));
        b1.save();
    }

    /**
     * Copy a subtree that contains shareable nodes. Verify that the nodes
     * newly created are not in the shared set that existed before the copy,
     * but if two nodes in the source of a copy are in the same shared set, then
     * the two corresponding nodes in the destination of the copy must also be
     * in the same shared set (6.13.12).
     */
    public void testCopy() throws Exception {
        // setup parent node and first child
        Node s = testRootNode.addNode("s");
        Node a1 = s.addNode("a1");
        Node a2 = s.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.save();

        // add mixin
        b1.addMixin("mix:shareable");
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // copy source tree to destination
        workspace.copy(s.getPath(), testRootNode.getPath() + "/d");

        // verify source contains shared set with 2 entries
        Node[] shared = getSharedSet(b1);
        assertEquals(2, shared.length);

        // verify destination contains shared set with 2 entries
        shared = getSharedSet(testRootNode.getNode("d/a1/b1"));
        assertEquals(2, shared.length);
    }

    /**
     * Verify that a share cycle is detected (6.13.13).
     */
    public void testShareCycle() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node b1 = a1.addNode("b1");
        testRootNode.save();

        // add mixin
        b1.addMixin("mix:shareable");
        b1.save();

        Workspace workspace = b1.getSession().getWorkspace();

        try {
            // clone underneath b1: this must fail
            workspace.clone(workspace.getName(), b1.getPath(),
                    b1.getPath() + "/c", false);
            fail("Cloning should create a share cycle.");
        } catch (RepositoryException e) {
            // expected
        }
    }

    /**
     * Verifies that observation events are sent only once (6.13.15).
     */
    public void testObservation() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.save();

        // add mixin
        b1.addMixin("mix:shareable");
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // event listener that counts events received
        class EventCounter implements SynchronousEventListener {

            private int count;

            public void onEvent(EventIterator events) {
                while (events.hasNext()) {
                    events.nextEvent();
                    count++;
                }
            }

            public int getEventCount() {
                return count;
            }

            public void resetCount() {
                count = 0;
            }
        }

        EventCounter el = new EventCounter();
        ObservationManager om = superuser.getWorkspace().getObservationManager();

        // add node underneath shared set: verify it generates one event only
        om.addEventListener(el, Event.NODE_ADDED, testRootNode.getPath(),
                true, null, null, false);
        b1.addNode("c");
        b1.save();
        superuser.getWorkspace().getObservationManager().removeEventListener(el);
        assertEquals(1, el.getEventCount());

        // remove node underneath shared set: verify it generates one event only
        el.resetCount();
        om.addEventListener(el, Event.NODE_REMOVED, testRootNode.getPath(),
                true, null, null, false);
        b1.getNode("c").remove();
        b1.save();
        superuser.getWorkspace().getObservationManager().removeEventListener(el);
        assertEquals(1, el.getEventCount());

        // add property underneath shared set: verify it generates one event only
        el.resetCount();
        om.addEventListener(el, Event.PROPERTY_ADDED, testRootNode.getPath(),
                true, null, null, false);
        b1.setProperty("c", "1");
        b1.save();
        superuser.getWorkspace().getObservationManager().removeEventListener(el);
        assertEquals(1, el.getEventCount());

        // modify property underneath shared set: verify it generates one event only
        el.resetCount();
        om.addEventListener(el, Event.PROPERTY_CHANGED, testRootNode.getPath(),
                true, null, null, false);
        b1.setProperty("c", "2");
        b1.save();
        superuser.getWorkspace().getObservationManager().removeEventListener(el);
        assertEquals(1, el.getEventCount());

        // remove property underneath shared set: verify it generates one event only
        el.resetCount();
        om.addEventListener(el, Event.PROPERTY_REMOVED, testRootNode.getPath(),
                true, null, null, false);
        b1.getProperty("c").remove();
        b1.save();
        superuser.getWorkspace().getObservationManager().removeEventListener(el);
        assertEquals(1, el.getEventCount());
    }

    /**
     * Verifies that a lock applies to all nodes in a shared set (6.13.16).
     */
    public void testLock() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        a1.addMixin("mix:lockable");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.save();

        // add mixin
        b1.addMixin("mix:shareable");
        b1.addMixin("mix:lockable");
        b1.save();

        // add child c
        Node c = b1.addNode("c");
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        Node[] shared = getSharedSet(b1);
        assertEquals(2, shared.length);
        b1 = shared[0];
        Node b2 = shared[1];

        // lock shareable node -> all nodes in shared set are locked
        b1.lock(false, true);
        try {
            assertTrue(b2.isLocked());
        } finally {
            b1.unlock();
        }

        // deep-lock parent -> locks (common) child node
        a1.lock(true, true);
        try {
            assertTrue(c.isLocked());
        } finally {
            a1.unlock();
        }
    }

    /**
     * Clones a mix:shareable node to the same workspace (6.13.20). Verifies
     * that cloning without mix:shareable fails.
     */
    public void testClone() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.save();

        Workspace workspace = b1.getSession().getWorkspace();

        try {
            // clone (1st attempt, without mix:shareable, should fail)
            workspace.clone(workspace.getName(), b1.getPath(),
                    a2.getPath() + "/b2", false);
            fail("Cloning a node into the same workspace should fail.");
        } catch (RepositoryException e) {
            // expected
        }

        // add mixin
        b1.addMixin("mix:shareable");
        b1.save();

        // clone (2nd attempt, with mix:shareable)
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);
    }

    /**
     * Verifies that Node.isSame returns <code>true</code> for shareable nodes
     * in the same shared set (6.13.21)
     */
    public void testIsSame() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.save();

        // add mixin
        b1.addMixin("mix:shareable");
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        Node[] shared = getSharedSet(b1);
        assertEquals(2, shared.length);
        b1 = shared[0];
        Node b2 = shared[1];

        // verify b1 is same as b2 (and vice-versa)
        assertTrue(b1.isSame(b2));
        assertTrue(b2.isSame(b1));
    }

    /**
     * Removes mix:shareable from a shareable node. This is unsupported in
     * Jackrabbit (6.13.22).
     */
    public void testRemoveMixin() throws Exception {
        // setup parent node and first child
        Node a = testRootNode.addNode("a");
        Node b = a.addNode("b");
        testRootNode.save();

        // add mixin
        b.addMixin("mix:shareable");
        b.save();

        try {
            // remove mixin
            b.removeMixin("mix:shareable");
            b.save();
            fail("Removing mix:shareable should fail.");
        } catch (UnsupportedRepositoryOperationException e) {
            // expected
        }
    }

    /**
     * Verifies that a descendant of a shareable node appears once in the
     * result set (6.13.23)
     */
    public void testSearch() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.save();

        // add mixin
        b1.addMixin("mix:shareable");
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // add new referenceable child
        Node c = b1.addNode("c");
        c.addMixin(mixReferenceable);
        b1.save();

        String sql = "SELECT * FROM nt:unstructured WHERE jcr:uuid = '"+c.getUUID()+"'";
        QueryResult res = workspace.getQueryManager().createQuery(sql, Query.SQL).execute();

        ArrayList list = new ArrayList();

        NodeIterator iter = res.getNodes();
        while (iter.hasNext()) {
            list.add(iter.nextNode());
        }
        assertEquals(1, list.size());
        assertTrue(((Node) list.get(0)).isSame(c));
    }

    //--------------------------------------------------------- limitation tests

    /**
     * Clones a mix:shareable node to the same workspace, with the same
     * parent. This is unsupported in Jackrabbit.
     */
    public void testCloneToSameParent() throws Exception {
        // setup parent nodes and first child
        Node a = testRootNode.addNode("a");
        Node b1 = a.addNode("b1");
        testRootNode.save();

        // add mixin
        b1.addMixin("mix:shareable");
        b1.save();

        Workspace workspace = b1.getSession().getWorkspace();

        try {
            // clone to same parent
            workspace.clone(workspace.getName(), b1.getPath(),
                    a.getPath() + "/b2", false);
            fail("Cloning inside same parent should fail.");
        } catch (UnsupportedRepositoryOperationException e) {
            // expected
        }
    }

    /**
     * Moves a node in a shared set. This is unsupported in Jackrabbit.
     */
    public void testMoveShareableNode() throws Exception {
        // setup parent nodes and first childs
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b = a1.addNode("b");
        testRootNode.save();

        // add mixin
        b.addMixin("mix:shareable");
        b.save();

        // move
        Workspace workspace = b.getSession().getWorkspace();

        try {
            // move shareable node
            workspace.move(b.getPath(), a2.getPath() + "/b");
            fail("Moving a mix:shareable should fail.");
        } catch (UnsupportedRepositoryOperationException e) {
            // expected
        }
    }

    /**
     * Transiently moves a node in a shared set. This is unsupported in
     * Jackrabbit.
     */
    public void testTransientMoveShareableNode() throws Exception {
        // setup parent nodes and first childs
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b = a1.addNode("b");
        testRootNode.save();

        // add mixin
        b.addMixin("mix:shareable");
        b.save();

        // move
        Session session = superuser;

        try {
            // move shareable node
            session.move(b.getPath(), a2.getPath() + "/b");
            session.save();
            fail("Moving a mix:shareable should fail.");
        } catch (UnsupportedRepositoryOperationException e) {
            // expected
        }
    }

    //---------------------------------------------------------- utility methods

    /**
     * Return a shared set as an array of nodes.
     *
     * @param n node
     * @return array of nodes in shared set
     */
    private Node[] getSharedSet(Node n) throws RepositoryException {
        ArrayList list = new ArrayList();

        NodeIterator iter = ((NodeImpl) n).getSharedSet();
        while (iter.hasNext()) {
            list.add(iter.nextNode());
        }

        Node[] result = new Node[list.size()];
        list.toArray(result);
        return result;
    }
}
