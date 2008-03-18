package org.apache.jackrabbit.core;

import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.ConstraintViolationException;

import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;

public class ShareableNodeTest extends AbstractJCRTest {

    private NameFactory factory;
    private Name testShareable;
    
    protected void setUp() throws Exception {
        super.setUp();
        
        factory = NameFactoryImpl.getInstance();
        testShareable = factory.create("http://www.apache.org/jackrabbit/test", "shareable");

        checkNodeTypes();
    }
    
    private void checkNodeTypes() 
            throws RepositoryException, InvalidNodeTypeDefException {
        
        NodeTypeRegistry ntreg = ((NodeTypeManagerImpl) superuser.getWorkspace().
                getNodeTypeManager()).getNodeTypeRegistry(); 
        if (!ntreg.isRegistered(testShareable)) {
            NodeDefImpl nd = new NodeDefImpl();
            nd.setAllowsSameNameSiblings(false);
            nd.setDeclaringNodeType(testShareable);
            nd.setDefaultPrimaryType(null);
            nd.setMandatory(false);
            nd.setName(factory.create("", "*"));
            nd.setProtected(false);
            nd.setRequiredPrimaryTypes(new Name[]{NameConstants.NT_BASE});
            
            NodeTypeDef ntd = new NodeTypeDef();
            ntd.setName(testShareable);
            ntd.setSupertypes(new Name[]{factory.create(Name.NS_NT_URI, "base")});
            ntd.setOrderableChildNodes(false);
            ntd.setChildNodeDefs(new NodeDef[] { nd });
            
            ntreg.registerNodeType(ntd);
        }
    }
    
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
    
    public void testAddMixin() throws Exception {
        // setup parent node and first child 
        Node a = testRootNode.addNode("a");
        Node b = a.addNode("b");
        testRootNode.save();
        
        b.addMixin("mix:shareable");
        b.save();
    }
    
    public void testClone() throws Exception {
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
    }
    
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
}
