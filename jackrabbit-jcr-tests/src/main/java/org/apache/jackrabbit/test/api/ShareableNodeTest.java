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
package org.apache.jackrabbit.test.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * Tests features available with shareable nodes.
 */
public class ShareableNodeTest extends AbstractJCRTest {

    protected void setUp() throws Exception {
        super.setUp();
        try {
            checkSupportedOption(Repository.OPTION_SHAREABLE_NODES_SUPPORTED);
            ensureKnowsNodeType(superuser, mixShareable);
        } catch (NotExecutableException e) {
            cleanUp();
            throw e;
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    //------------------------------------------------------ specification tests

    /**
     * Verify that Node.getIndex returns the correct index in a shareable
     * node (6.13).
     */
    public void testGetIndex() throws Exception {
        
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        a2.addNode("b");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
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
     * Verify that Node.getName returns the correct name in a shareable node
     * (6.13).
     */
    public void testGetName() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
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
     * Verify that Node.getPath returns the correct path in a shareable
     * node (6.13).
     */
    public void testGetPath() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
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
        String testRootNodePath = testRootNode.getPath();
        assertEquals(testRootNodePath + "/a1/b1", b1.getPath());
        assertEquals(testRootNodePath + "/a2/b2", b2.getPath());
    }

    /**
     * Verify that the shareable node returned by Node.getNode() has the right
     * name.
     */
    public void testGetNode() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // a1.getNode("b1") should return b1
        b1 = a1.getNode("b1");
        assertEquals("b1", b1.getName());

        // a2.getNode("b2") should return b2
        Node b2 = a2.getNode("b2");
        assertEquals("b2", b2.getName());
    }

    /**
     * Verify that the shareable nodes returned by Node.getNodes() have
     * the right name.
     */
    public void testGetNodes() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // a1.getNodes() should return b1
        Node[] children = toArray(a1.getNodes());
        assertEquals(1, children.length);
        assertEquals("b1", children[0].getName());

        // a2.getNodes() should return b2
        children = toArray(a2.getNodes());
        assertEquals(1, children.length);
        assertEquals("b2", children[0].getName());
    }

    /**
     * Verify that the shareable nodes returned by Node.getNodes(String) have
     * the right name.
     */
    public void testGetNodesByPattern() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // a1.getNodes(*) should return b1
        Node[] children = toArray(a1.getNodes("*"));
        assertEquals(1, children.length);
        assertEquals("b1", children[0].getName());

        // a2.getNodes(*) should return b2
        children = toArray(a2.getNodes("*"));
        assertEquals(1, children.length);
        assertEquals("b2", children[0].getName());
    }

    /**
     * Check new API Node.getSharedSet() (6.13.1)
     */
    public void testGetSharedSet() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
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
     * Add the mix:shareable mixin to a node (6.13.2).
     */
    public void testAddMixin() throws Exception {
        // setup parent node and first child
        Node a = testRootNode.addNode("a");
        Node b = a.addNode("b");
        testRootNode.getSession().save();

        ensureMixinType(b, mixShareable);
        b.save();
    }

    /**
     * Create a shareable node by restoring it (6.13.3).
     */
    public void testRestore() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // make b1 shareable
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // make a2 versionable
        ensureMixinType(a2, mixVersionable);
        a2.save();

        // check in version and check out again
        Version v = a2.checkin();
        a2.checkout();

        // delete b2 and save
        a2.getNode("b2").remove();
        a2.save();

        // verify shared set contains one element only
        Node[] shared = getSharedSet(b1);
        assertEquals(1, shared.length);

        // restore version
        a2.restore(v, false);

        // verify shared set contains again two elements
        shared = getSharedSet(b1);
        assertEquals(2, shared.length);
    }


    /**
     * Check new API Node.removeShare() (6.13.4).
     */
    public void testRemoveShare() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
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
        b1.removeShare();
        a1.save();

        // verify shared set of b2 contains only 1 item, namely b2 itself
        shared = getSharedSet(b2);
        assertEquals(1, shared.length);
        assertTrue(shared[0].isSame(b2));
    }

    /**
     * Check new API Node.removeSharedSet() (6.13.4).
     */
    public void testRemoveSharedSet() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // remove shared set
        b1.removeSharedSet();
        testRootNode.getSession().save();

        // verify neither a1 nor a2 contain any more children
        assertFalse(a1.hasNodes());
        assertFalse(a2.hasNodes());
    }

    /**
     * Invoke Node.removeSharedSet(), but save only one of the parent nodes
     * of the shared set. This doesn't need to be supported according to the
     * specification (6.13.4).
     */
    public void testRemoveSharedSetSaveOneParentOnly() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // remove shared set
        b1.removeSharedSet();

        try {
            // save only one of the parents, should fail
            a1.save();
            fail("Removing a shared set requires saving all parents.");
        } catch (ConstraintViolationException e) {
            // expected
        }
    }

    /**
     * Verify that shareable nodes in the same shared set have the same
     * jcr:uuid (6.13.10).
     */
    public void testSameUUID() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
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
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
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
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // copy source tree to destination
        workspace.copy(s.getPath(), testRootNode.getPath() + "/d");

        // verify source contains shared set with 2 entries
        Node[] shared1 = getSharedSet(b1);
        assertEquals(2, shared1.length);

        // verify destination contains shared set with 2 entries
        Node[] shared2 = getSharedSet(testRootNode.getNode("d/a1/b1"));
        assertEquals(2, shared2.length);

        // verify elements in source shared set and destination shared set
        // don't have the same UUID
        String srcUUID = shared1[0].getUUID();
        String destUUID = shared2[0].getUUID();
        assertFalse(
                "Source and destination of a copy must not have the same UUID",
                srcUUID.equals(destUUID));
    }

    /**
     * Verify that a share cycle is detected (6.13.13) when a shareable node
     * is cloned.
     */
    public void testDetectShareCycleOnClone() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        Workspace workspace = b1.getSession().getWorkspace();

        try {
            // clone underneath b1: this must fail
            workspace.clone(workspace.getName(), b1.getPath(),
                    b1.getPath() + "/c", false);
            fail("Share cycle not detected on clone.");
        } catch (RepositoryException e) {
            // expected
        }
    }

    /**
     * Verify that a share cycle is detected (6.13.13) when a node is moved.
     */
    public void testDetectShareCycleOnMove() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // add child node
        Node c = b1.addNode("c");
        b1.save();

        Node[] shared = getSharedSet(b1);
        assertEquals(2, shared.length);

        // move node
        try {
            workspace.move(testRootNode.getPath() + "/a2", c.getPath() + "/d");
            fail("Share cycle not detected on move.");
        } catch (RepositoryException e) {
            // expected
        }
    }

    /**
     * Verify that a share cycle is detected (6.13.13) when a node is
     * transiently moved.
     */
    public void testDetectShareCycleOnTransientMove() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Session session = b1.getSession();
        Workspace workspace = session.getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // add child node
        Node c = b1.addNode("c");
        b1.save();

        Node[] shared = getSharedSet(b1);
        assertEquals(2, shared.length);

        // move node
        try {
            session.move(testRootNode.getPath() + "/a2", c.getPath());
            fail("Share cycle not detected on transient move.");
        } catch (RepositoryException e) {
            // expected
        }
    }

    /**
     * Verify export and import of a tree containing multiple nodes in the
     * same shared set (6.13.14). The first serialized node in that shared
     * set is serialized in the normal fashion (with all of its properties
     * and children), but any subsequent shared node in that shared set is
     * serialized as a special node of type <code>nt:share</code>, which
     * contains only the <code>jcr:uuid</code> property of the shared node
     * and the <code>jcr:primaryType</code> property indicating the type
     * <code>nt:share</code>.
     */
    public void testImportExportNtShare() throws Exception {
        // setup parent nodes and first child
        Node p = testRootNode.addNode("p");
        Node a1 = p.addNode("a1");
        Node a2 = p.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Session session = b1.getSession();
        Workspace workspace = session.getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // create temp file
        File tmpFile = File.createTempFile("test", null);
        tmpFile.deleteOnExit();

        // export system view of /p
        OutputStream out = new FileOutputStream(tmpFile);
        try {
            session.exportSystemView(p.getPath(), out, false, false);
        } finally {
            out.close();
        }

        // delete p and save
        p.remove();
        testRootNode.getSession().save();

        // and import again underneath test root
        InputStream in = new FileInputStream(tmpFile);
        try {
            workspace.importXML(testRootNode.getPath(), in,
                    ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        } finally {
            try { in.close(); } catch (IOException ignore) {}
        }

        // verify shared set consists of two nodes
        Node[] shared = getSharedSet(testRootNode.getNode("p/a1/b1"));
        assertEquals(2, shared.length);
    }

    /**
     * Verify system view import via workspace (6.13.14). Export a system view
     * containing a shareable node and verify, that reimporting underneath
     * a different parent adds another member to the shared set and does not
     * duplicate children nodes.
     */
    public void testImportSystemViewCollision() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node a3 = testRootNode.addNode("a3");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Session session = b1.getSession();
        Workspace workspace = session.getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // add child c to shareable nodes b1 & b2
        b1.addNode("c");
        b1.save();

        // create temp file
        File tmpFile = File.createTempFile("test", null);
        tmpFile.deleteOnExit();

        // export system view of /a1/b1
        OutputStream out = new FileOutputStream(tmpFile);
        try {
            session.exportSystemView(b1.getPath(), out, false, false);
        } finally {
            out.close();
        }

        // and import again underneath /a3
        InputStream in = new FileInputStream(tmpFile);
        try {
            workspace.importXML(a3.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        } finally {
            try { in.close(); } catch (IOException ignore) {}
        }

        // verify there's another element in the shared set
        Node[] shared = getSharedSet(b1);
        assertEquals(3, shared.length);

        // verify child c has not been duplicated
        Node[] children = toArray(b1.getNodes());
        assertEquals(1, children.length);
    }

    /**
     * Verify document view import via workspace (6.13.14). Export a document
     * view containing a shareable node and verify, that reimporting
     * underneath a different parent adds another member to the shared set and
     * does not duplicate children nodes.
     */
    public void testImportDocumentViewCollision() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node a3 = testRootNode.addNode("a3");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Session session = b1.getSession();
        Workspace workspace = session.getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // add child c to shareable nodes b1 & b2
        b1.addNode("c");
        b1.save();

        // create temp file
        File tmpFile = File.createTempFile("test", null);
        tmpFile.deleteOnExit();

        // export system view of /a1/b1
        OutputStream out = new FileOutputStream(tmpFile);
        try {
            session.exportDocumentView(b1.getPath(), out, false, false);
        } finally {
            out.close();
        }

        // and import again underneath /a3
        InputStream in = new FileInputStream(tmpFile);
        try {
            workspace.importXML(a3.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        } finally {
            try { in.close(); } catch (IOException ignore) {}
        }

        // verify there's another element in the shared set
        Node[] shared = getSharedSet(b1);
        assertEquals(3, shared.length);

        // verify child c has not been duplicated
        Node[] children = toArray(b1.getNodes());
        assertEquals(1, children.length);
    }

    /**
     * Verify system view import via session (6.13.14). Export a system view
     * containing a shareable node and verify, that reimporting underneath
     * a different parent adds another member to the shared set and does not
     * duplicate children nodes.
     */
    public void testSessionImportSystemViewCollision() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node a3 = testRootNode.addNode("a3");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Session session = b1.getSession();
        Workspace workspace = session.getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // add child c to shareable nodes b1 & b2
        b1.addNode("c");
        b1.save();

        // create temp file
        File tmpFile = File.createTempFile("test", null);
        tmpFile.deleteOnExit();

        // export system view of /a1/b1
        OutputStream out = new FileOutputStream(tmpFile);
        try {
            session.exportSystemView(b1.getPath(), out, false, false);
        } finally {
            out.close();
        }

        // and import again underneath /a3
        InputStream in = new FileInputStream(tmpFile);
        try {
            session.importXML(a3.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
            session.save();
        } finally {
            try { in.close(); } catch (IOException ignore) {}
        }

        // verify there's another element in the shared set
        Node[] shared = getSharedSet(b1);
        assertEquals(3, shared.length);

        // verify child c has not been duplicated
        Node[] children = toArray(b1.getNodes());
        assertEquals(1, children.length);
    }

    /**
     * Verify document view import via session (6.13.14). Export a document
     * view containing a shareable node and verify, that reimporting
     * underneath a different parent adds another member to the shared set and
     * does not duplicate children nodes.
     */
    public void testSessionImportDocumentViewCollision() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node a3 = testRootNode.addNode("a3");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Session session = b1.getSession();
        Workspace workspace = session.getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // add child c to shareable nodes b1 & b2
        b1.addNode("c");
        b1.save();

        // create temp file
        File tmpFile = File.createTempFile("test", null);
        tmpFile.deleteOnExit();

        // export system view of /a1/b1
        OutputStream out = new FileOutputStream(tmpFile);
        try {
            session.exportSystemView(b1.getPath(), out, false, false);
        } finally {
            out.close();
        }

        // and import again underneath /a3
        InputStream in = new FileInputStream(tmpFile);
        try {
            session.importXML(a3.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
            session.save();
        } finally {
            try { in.close(); } catch (IOException ignore) {}
        }

        // verify there's another element in the shared set
        Node[] shared = getSharedSet(b1);
        assertEquals(3, shared.length);

        // verify child c has not been duplicated
        Node[] children = toArray(b1.getNodes());
        assertEquals(1, children.length);
    }

    /**
     * Verify that a lock applies to all nodes in a shared set (6.13.16).
     */
    public void testLock() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        ensureMixinType(a1, mixLockable);
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        ensureMixinType(b1, mixLockable);
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
     * Restore a shareable node that automatically removes an existing shareable
     * node (6.13.19). In this case the particular shared node is removed but
     * its descendants continue to exist below the remaining members of the
     * shared set.
     */
    public void testRestoreRemoveExisting() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // make b1 shareable
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // add child c
        b1.addNode("c");
        b1.save();

        // make a2 versionable
        ensureMixinType(a2, mixVersionable);
        a2.save();

        // check in version and check out again
        Version v = a2.checkin();
        a2.checkout();

        // delete b2 and save
        a2.getNode("b2").remove();
        a2.save();

        // verify shareable set contains one elements only
        Node[] shared = getSharedSet(b1);
        assertEquals(1, shared.length);

        // restore version and remove existing (i.e. b1)
        a2.restore(v, true);

        // verify shareable set contains still one element
        shared = getSharedSet(a2.getNode("b2"));
        assertEquals(1, shared.length);

        // verify child c still exists
        Node[] children = toArray(a2.getNode("b2").getNodes());
        assertEquals(1, children.length);
    }

    /**
     * Clone a mix:shareable node to the same workspace (6.13.20). Verify
     * that cloning without mix:shareable fails.
     */
    public void testClone() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

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
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone (2nd attempt, with mix:shareable)
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);
    }

    /**
     * Verify that Node.isSame returns <code>true</code> for shareable nodes
     * in the same shared set (6.13.21)
     */
    public void testIsSame() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
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
     * Remove mix:shareable from a shareable node.
     */
    public void testRemoveMixin() throws Exception {
        // setup parent node and first child
        Node a = testRootNode.addNode("a");
        Node b = a.addNode("b");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b, mixShareable);
        b.getSession().save();

        // Removing the mixin will either succeed or will fail with a
        // ConstraintViolationException
        // (per Section 14.15 of JSR-283 specification)
        try {
            // remove mixin
            b.removeMixin(mixShareable);
            b.getSession().save();
            // If this happens, then b shouldn't be shareable anymore ...
            assertFalse(b.isNodeType(mixShareable));
        } catch (ConstraintViolationException e) {
            // one possible outcome if removing 'mix:shareable' isn't supported
        } catch (UnsupportedRepositoryOperationException e) {
            // also possible if the implementation doesn't support this
            // capability
        }
    }

    /**
     * Remove mix:shareable from a shareable node that has 2 nodes in the shared set. 
     */
    public void testRemoveMixinFromSharedNode() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.getSession().save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(), a2.getPath() + "/b2", false);

        Node[] shared = getSharedSet(b1);
        assertEquals(2, shared.length);
        b1 = shared[0];
        Node b2 = shared[1];
        assertTrue(b2.isSame(b1));

        // Removing the mixin will either succeed or will fail with a
        // ConstraintViolationException
        // (per Section 14.15 of JSR-283 specification)
        try {
            // remove mixin
            b1.removeMixin(mixShareable);
            b1.getSession().save();
            // If this happens, then b1 shouldn't be shareable anymore
            // ...
            assertFalse(b1.isNodeType(mixShareable));
            assertFalse(b2.isSame(b1));
        } catch (ConstraintViolationException e) {
            // one possible outcome if removing 'mix:shareable' isn't supported
        } catch (UnsupportedRepositoryOperationException e) {
            // also possible if the implementation doesn't support this
            // capability
        }
    }

    /**
     * Verify that a descendant of a shareable node appears once in the
     * result set (6.13.23)
     */
    public void testSearch() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // add new referenceable child
        Node c = b1.addNode("c");
        ensureMixinType(c, mixReferenceable);
        b1.save();

        String sql = "SELECT * FROM nt:unstructured WHERE jcr:uuid = '"+c.getUUID()+"'";
        QueryResult res = workspace.getQueryManager().createQuery(sql, Query.SQL).execute();

        List<Node> list = new ArrayList<Node>();

        NodeIterator iter = res.getNodes();
        while (iter.hasNext()) {
            list.add(iter.nextNode());
        }
        assertEquals(1, list.size());
        assertTrue(list.get(0).isSame(c));
    }

    //--------------------------------------------------------- limitation tests

    /**
     * Clone a mix:shareable node to the same workspace, with the same
     * parent. This is unsupported in Jackrabbit.
     */
    public void testCloneToSameParent() throws Exception {
        // setup parent nodes and first child
        Node a = testRootNode.addNode("a");
        Node b1 = a.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
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
     * Move a node in a shared set.
     */
    public void testMoveShareableNode() throws Exception {
        // setup parent nodes and first children
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b = a1.addNode("b");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b, mixShareable);
        b.getSession().save();

        // move
        Workspace workspace = b.getSession().getWorkspace();

        // move shareable node
        String newPath = a2.getPath() + "/b";
        workspace.move(b.getPath(), newPath);
        // move was performed using the workspace, so refresh the session
        b.getSession().refresh(false);
        assertEquals(newPath, b.getPath());
    }

    /**
     * Transiently move a node in a shared set.
     */
    public void testTransientMoveShareableNode() throws Exception {
        // setup parent nodes and first children
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b = a1.addNode("b");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b, mixShareable);
        b.getSession().save();

        // move
        Session session = superuser;

        // move shareable node
        String newPath = a2.getPath() + "/b";
        session.move(b.getPath(), newPath);
        session.save();
        assertEquals(newPath, b.getPath());
    }

    //----------------------------------------------------- implementation tests

    /**
     * Verify that invoking save() on a share-ancestor will save changes in
     * all share-descendants.
     */
    public void testRemoveDescendantAndSave() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Session session = b1.getSession();
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // add child node c to b1
        Node c = b1.addNode("c");
        b1.save();

        // remove child node c
        c.remove();

        // save a2 (having path /testroot/a2): this should save c as well
        // since one of the paths to c is /testroot/a2/b2/c
        a2.save();
        assertFalse("Saving share-ancestor should save share-descendants",
                session.hasPendingChanges());
    }

    /**
     * Verify that invoking save() on a share-ancestor will save changes in
     * all share-descendants.
     */
    public void testRemoveDescendantAndRemoveShareAndSave() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Session session = b1.getSession();
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // add child node c to b1
        Node c = b1.addNode("c");
        b1.save();

        // remove child node c
        c.remove();

        // remove share b2 from a2
        a2.getNode("b2").removeShare();

        // save a2 (having path /testroot/a2): this should save c as well
        // since one of the paths to c was /testroot/a2/b2/c
        a2.save();
        assertFalse("Saving share-ancestor should save share-descendants",
                session.hasPendingChanges());
    }

    /**
     * Verify that invoking save() on a share-ancestor will save changes in
     * all share-descendants.
     */
    public void testModifyDescendantAndSave() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // add child node c to b1
        Node c = b1.addNode("c");
        b1.save();

        // add child d to c, this modifies c
        c.addNode("d");

        // save a2 (having path /testroot/a2): this should save c as well
        // since one of the paths to c is /testroot/a2/b2/c
        a2.save();
        assertFalse("Saving share-ancestor should save share-descendants",
                c.isModified());
    }

    /**
     * Verify that invoking save() on a share-ancestor will save changes in
     * all share-descendants.
     */
    public void testModifyDescendantAndRemoveShareAndSave() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b1, mixShareable);
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // add child node c to b1
        Node c = b1.addNode("c");
        b1.save();

        // add child d to c, this modifies c
        c.addNode("d");

        // remove share b2 from a2
        a2.getNode("b2").removeShare();

        // save a2 (having path /testroot/a2): this should save c as well
        // since one of the paths to c was /testroot/a2/b2/c
        a2.save();
        assertFalse("Saving share-ancestor should save share-descendants",
                c.isModified());
    }

    /**
     * Clone a mix:shareable node to the same workspace multiple times, remove
     * all parents and save. Exposes an error that occurred when having more
     * than two members in a shared set and parents were removed in the same
     * order they were created.
     */
    public void testCloneMultipleTimes() throws Exception {
        final int count = 10;
        Node[] parents = new Node[count];

        // setup parent nodes and first child
        for (int i = 0; i < parents.length; i++) {
            parents[i] = testRootNode.addNode("a" + (i + 1));
        }
        Node b = parents[0].addNode("b");
        testRootNode.getSession().save();

        // add mixin
        ensureMixinType(b, mixShareable);
        b.save();

        Workspace workspace = b.getSession().getWorkspace();

        // clone to all other nodes
        for (int i = 1; i < parents.length; i++) {
            workspace.clone(workspace.getName(), b.getPath(),
                    parents[i].getPath() + "/b", false);
        }

        // remove all parents and save
        for (int i = 0; i < parents.length; i++) {
            parents[i].remove();
        }
        testRootNode.getSession().save();
    }

    /**
     * Verify that shared nodes return correct paths.
     */
    public void testSharedNodePath() throws Exception {
       Node a1 = testRootNode.addNode("a1");
       Node a2 = a1.addNode("a2");
       Node b1 = a1.addNode("b1");
       ensureMixinType(b1, mixShareable);
       testRootNode.getSession().save();

       //now we have a shareable node N with path a1/b1

       Session session = testRootNode.getSession();
       Workspace workspace = session.getWorkspace();
       String path = a2.getPath() + "/b2";
       workspace.clone(workspace.getName(), b1.getPath(), path, false);

       //now we have another shareable node N' in the same shared set as N with path a1/a2/b2

       //using the path a1/a2/b2, we should get the node N' here
       Item item = session.getItem(path);
       assertEquals("unexpectedly got the path from another node from the same shared set", path, item.getPath()); 
    } 
    
    //---------------------------------------------------------- utility methods

    /**
     * Return a shared set as an array of nodes.
     *
     * @param n node
     * @return array of nodes in shared set
     */
    private static Node[] getSharedSet(Node n) throws RepositoryException {
        return toArray(n.getSharedSet());
    }

    /**
     * Return an array of nodes given a <code>NodeIterator</code>.
     *
     * @param iter node iterator
     * @return node array
     */
    private static Node[] toArray(NodeIterator iter) {
        List<Node> list = new ArrayList<Node>();

        while (iter.hasNext()) {
            list.add(iter.nextNode());
        }

        Node[] result = new Node[list.size()];
        list.toArray(result);
        return result;
    }
}
