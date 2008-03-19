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
                a2.getPath() + "/b", true);

        ArrayList list = new ArrayList();

        NodeIterator iter = ((NodeImpl) b1).getSharedSet();
        while (iter.hasNext()) {
            list.add(iter.nextNode());
        }
        
        assertEquals(list.size(), 2);
        b1 = (Node) list.get(0);
        Node b2 = (Node) list.get(1);
        assertEquals(b1.getIndex(), 1);
        assertEquals(b2.getIndex(), 2);
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
                a2.getPath() + "/b2", true);

        ArrayList list = new ArrayList();

        NodeIterator iter = ((NodeImpl) b1).getSharedSet();
        while (iter.hasNext()) {
            list.add(iter.nextNode());
        }
        
        assertEquals(list.size(), 2);
        b1 = (Node) list.get(0);
        Node b2 = (Node) list.get(1);
        assertEquals(b1.getName(), "b1");
        assertEquals(b2.getName(), "b2");
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
                a2.getPath() + "/b2", true);

        ArrayList list = new ArrayList();

        NodeIterator iter = ((NodeImpl) b1).getSharedSet();
        while (iter.hasNext()) {
            list.add(iter.nextNode());
        }
        
        assertEquals(list.size(), 2);
        b1 = (Node) list.get(0);
        Node b2 = (Node) list.get(1);
        assertEquals(b1.getPath(), "/testroot/a1/b1");
        assertEquals(b2.getPath(), "/testroot/a2/b2");
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
                a2.getPath() + "/b2", true);
        
        NodeIterator iter = ((NodeImpl) b1).getSharedSet();
        int items = 0;
        while (iter.hasNext()) {
            iter.nextNode();
            items++;
        }
        assertEquals(items, 2);
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
                a2.getPath() + "/b2", true);

        ArrayList list = new ArrayList();

        NodeIterator iter = ((NodeImpl) b1).getSharedSet();
        while (iter.hasNext()) {
            list.add(iter.nextNode());
        }
        
        assertEquals(list.size(), 2);
        b1 = (Node) list.get(0);
        Node b2 = (Node) list.get(1);
        assertTrue(b1.isSame(b2));
        assertTrue(b2.isSame(b1));
        
        ((NodeImpl) b1).removeShare();
        a1.save();
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
                a2.getPath() + "/b2", true);

        ((NodeImpl) b1).removeSharedSet();
        testRootNode.save();
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
                a2.getPath() + "/b2", true);

        ((NodeImpl) b1).removeSharedSet();
        
        try {
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
                a2.getPath() + "/b2", true);

        ArrayList list = new ArrayList();

        NodeIterator iter = ((NodeImpl) b1).getSharedSet();
        while (iter.hasNext()) {
            list.add(iter.nextNode());
        }
        
        assertEquals(list.size(), 2);
        b1 = (Node) list.get(0);
        Node b2 = (Node) list.get(1);
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
                a2.getPath() + "/b2", true);

        ArrayList list = new ArrayList();

        NodeIterator iter = ((NodeImpl) b1).getSharedSet();
        while (iter.hasNext()) {
            list.add(iter.nextNode());
        }
        assertEquals(list.size(), 2);
        b1 = (Node) list.get(0);
        Node b2 = (Node) list.get(1);
        
        b1.addNode("c");
        assertTrue(b2.isModified());
        assertTrue(b2.hasNode("c"));
        b1.save();
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
        
        // clone underneath b1: this must fail
        Workspace workspace = b1.getSession().getWorkspace();
        
        try {
            workspace.clone(workspace.getName(), b1.getPath(), 
                    b1.getPath() + "/c", true);
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
                a2.getPath() + "/b2", true);

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
        };
        
        EventCounter el = new EventCounter();
        ObservationManager om = superuser.getWorkspace().getObservationManager(); 

        om.addEventListener(el, Event.NODE_ADDED, testRootNode.getPath(), 
                true, null, null, false);
        b1.addNode("c");
        b1.save();
        superuser.getWorkspace().getObservationManager().removeEventListener(el);
        assertEquals(el.getEventCount(), 1);
        
        el.resetCount();
        om.addEventListener(el, Event.NODE_REMOVED, testRootNode.getPath(), 
                true, null, null, false);
        b1.getNode("c").remove();
        b1.save();
        superuser.getWorkspace().getObservationManager().removeEventListener(el);
        assertEquals(el.getEventCount(), 1);

        el.resetCount();
        om.addEventListener(el, Event.PROPERTY_ADDED, testRootNode.getPath(), 
                true, null, null, false);
        b1.setProperty("c", "1");
        b1.save();
        superuser.getWorkspace().getObservationManager().removeEventListener(el);
        assertEquals(el.getEventCount(), 1);

        el.resetCount();
        om.addEventListener(el, Event.PROPERTY_CHANGED, testRootNode.getPath(), 
                true, null, null, false);
        b1.setProperty("c", "2");
        b1.save();
        superuser.getWorkspace().getObservationManager().removeEventListener(el);
        assertEquals(el.getEventCount(), 1);

        el.resetCount();
        om.addEventListener(el, Event.PROPERTY_REMOVED, testRootNode.getPath(), 
                true, null, null, false);
        b1.getProperty("c").remove();
        b1.save();
        superuser.getWorkspace().getObservationManager().removeEventListener(el);
        assertEquals(el.getEventCount(), 1);
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
                a2.getPath() + "/b2", true);
        
        ArrayList list = new ArrayList();

        NodeIterator iter = ((NodeImpl) b1).getSharedSet();
        while (iter.hasNext()) {
            list.add(iter.nextNode());
        }
        
        assertEquals(list.size(), 2);
        b1 = (Node) list.get(0);
        Node b2 = (Node) list.get(1);
        
        // lock shareable node -> all nodes in shared set are locked
        b1.lock(false, true);
        assertTrue(b2.isLocked());
        b1.unlock();
        
        // deep-lock parent -> locks (common) child node
        a1.lock(true, true);
        assertTrue(c.isLocked());
        a1.unlock();
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
        
        // clone (1st attempt, without mix:shareable)
        Workspace workspace = b1.getSession().getWorkspace();
        try {
            workspace.clone(workspace.getName(), b1.getPath(), 
                    a2.getPath() + "/b2", true);
            fail("Cloning a node into the same workspace should fail.");
        } catch (RepositoryException e) {
            // expected
        }
        
        // add mixin
        b1.addMixin("mix:shareable");
        b1.save();
        
        // clone (2nd attempt, with mix:shareable)
        workspace.clone(workspace.getName(), b1.getPath(), 
                a2.getPath() + "/b2", true);
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
                a2.getPath() + "/b2", true);

        ArrayList list = new ArrayList();

        NodeIterator iter = ((NodeImpl) b1).getSharedSet();
        while (iter.hasNext()) {
            list.add(iter.nextNode());
        }
        
        assertEquals(list.size(), 2);
        b1 = (Node) list.get(0);
        Node b2 = (Node) list.get(1);
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
        
        // remove mixin
        try {
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
                a2.getPath() + "/b2", true);
        
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
        assertEquals(list.size(), 1);
        assertTrue(((NodeImpl) list.get(0)).isSame(c));
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
        
        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        
        try {
            workspace.clone(workspace.getName(), b1.getPath(), 
                    a.getPath() + "/b2", true);
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
            session.move(b.getPath(), a2.getPath() + "/b");
            session.save();
            fail("Moving a mix:shareable should fail.");
        } catch (UnsupportedRepositoryOperationException e) {
            // expected
        }
    }
}
