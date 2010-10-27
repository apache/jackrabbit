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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <code>NodeStateMergerTest</code>...
 */
public class NodeStateMergerTest extends AbstractJCRTest {

    /** Name of the cnd nodetype file for import and namespace registration. */
    private static final String TEST_NODETYPES = "org/apache/jackrabbit/core/nodetype/xml/test_nodestatemerger_nodetypes.cnd";

    private List<String> testMixins = new ArrayList<String>();

    private Node testNode;

    private Session sessionB;
    private Node testNodeB;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Reader cnd = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(TEST_NODETYPES));
        CndImporter.registerNodeTypes(cnd, superuser);
        cnd.close();

        NodeTypeIterator it = superuser.getWorkspace().getNodeTypeManager().getMixinNodeTypes();
        while (it.hasNext()) {
            NodeType nt = it.nextNodeType();
            if (nt.getName().startsWith("test:")) {
                testMixins.add(nt.getName());
            }
        }

        testNode = testRootNode.addNode(nodeName1, "nt:unstructured");
        superuser.save();

        sessionB = getHelper().getSuperuserSession();
        testNodeB = sessionB.getNode(testNode.getPath());
    }

    @Override
    protected void tearDown() throws Exception {
        if (sessionB != null) {
            sessionB.logout();
        }
        super.tearDown();
    }

    /**
     * Add the same property with both sessions but with different values.
     *
     * @throws RepositoryException
     */
    public void testAddSamePropertiesWithDifferentValues() throws RepositoryException {
        assertFalse(testNode.hasProperty(propertyName2));
        assertFalse(testNodeB.hasProperty(propertyName2));

        testNode.setProperty(propertyName2, "value");

        testNodeB.setProperty(propertyName2, "otherValue");
        sessionB.save();

        superuser.save();

        assertEquals("value", testNode.getProperty(propertyName2).getString());
        testNodeB.refresh(false);
        assertEquals("value", testNodeB.getProperty(propertyName2).getString());
    }

    /**
     * Modify the same property with both sessions but with different values.
     *
     * @throws RepositoryException
     */
    public void testModifySamePropertiesWithDifferentValues() throws RepositoryException {
        testNode.setProperty(propertyName2, "test");
        superuser.save();
        testNodeB.refresh(false);

        assertTrue(testNodeB.hasProperty(propertyName2));

        try {
            testNode.setProperty(propertyName2, "value");

            testNodeB.setProperty(propertyName2, "otherValue");
            sessionB.save();

            superuser.save();
            fail();
        } catch (InvalidItemStateException e) {
            // expected
        }
    }

    //------------------ Tests adding jcr:mixinType property in the session1 ---
    /**
     * Both node don't have any mixins assigned yet. Test if adding the same
     * mixin node with both sessions works.
     *
     * @throws RepositoryException
     */
    public void testAddSameMixinToSessionsAB() throws RepositoryException {
        assertFalse(testNode.hasProperty("jcr:mixinTypes"));

        testNode.addMixin(mixReferenceable);

        testNodeB.addMixin(mixReferenceable);
        sessionB.save();

        superuser.save();
    }

    /**
     * Same as {@link #testAddSameMixinToSessionsAB} but in addition
     * adding non-conflicting properties defined by this mixin. The properties
     * must be merged silently.
     *
     * @throws RepositoryException
     */
    public void testAddSameMixinToSessionsAB2() throws RepositoryException {
        assertFalse(testNode.hasProperty("jcr:mixinTypes"));

        testNode.addMixin("test:mixinProp_1");
        testNode.setProperty("test:prop_double", 124);

        testNodeB.addMixin("test:mixinProp_1");
        testNodeB.setProperty("test:prop_string", "abc");
        sessionB.save();

        superuser.save();

        assertEquals("abc", testNode.getProperty("test:prop_string").getString());
        testNodeB.refresh(false);
        assertEquals(124, testNodeB.getProperty("test:prop_double").getLong());
    }

    /**
     * Same as {@link #testAddSameMixinToSessionsAB} but in addition
     * a property defined by this mixin with different value. Value of overlayed
     * property (from sessionB) must be merged into sessionA.
     *
     * @throws RepositoryException
     */
    public void testAddSameMixinToSessionsAB3() throws RepositoryException {
        assertFalse(testNode.hasProperty("jcr:mixinTypes"));

        testNode.addMixin("test:mixinProp_1");
        testNode.setProperty("test:prop_double", 124);

        testNodeB.addMixin("test:mixinProp_1");
        testNodeB.setProperty("test:prop_double", 134);
        sessionB.save();

        superuser.save();

        assertEquals(124, testNode.getProperty("test:prop_double").getLong());
        testNodeB.refresh(false);
        assertEquals(124, testNodeB.getProperty("test:prop_double").getLong());
    }

    /**
     * Same as {@link #testAddSameMixinToSessionsAB3} having additional
     * modifications in the overlayed state (modifications by session B).
     *
     * @throws RepositoryException
     */
    public void testAddSameMixinToSessionsAB4() throws RepositoryException {
        assertFalse(testNode.hasProperty("jcr:mixinTypes"));

        testNode.addMixin("test:mixinProp_1");
        testNode.setProperty("test:prop_double", 124);

        testNodeB.addMixin("test:mixinProp_1");
        testNodeB.setProperty("test:prop_double", 134);
        testNodeB.setProperty("more", "yes");
        sessionB.save();

        superuser.save();

        assertEquals(124, testNode.getProperty("test:prop_double").getLong());
        testNodeB.refresh(false);
        assertEquals(124, testNodeB.getProperty("test:prop_double").getLong());
    }

    /**
     * Test adding mixin(s) to sessionA while the overlayed
     * (attached to testNode2, sessionB) doesn't have any mixins defined.
     * The changes should be merged as there is no conflict.
     *
     * @throws RepositoryException
     */
    public void testMixinAddedInSessionA() throws RepositoryException {
        assertFalse(testNode.hasProperty("jcr:mixinTypes"));

        for (String mixin : testMixins) {
            testNode.addMixin(mixin);
        }

        testNodeB.addNode(nodeName1, "nt:unstructured");
        testNodeB.setProperty(propertyName1, "anyValue");
        sessionB.save();

        superuser.save();

        assertTrue(testNode.hasNode(nodeName1));
        assertTrue(testNode.hasProperty(propertyName1));

        testNodeB.refresh(false);
        for (String mixin : testMixins) {
            assertTrue(testNodeB.isNodeType(mixin));
        }
    }

    /**
     * Test adding mixin(s) to sessionA, while the other sessionB
     * doesn't have any mixins defined. The changes should be merged as there
     * is no conflict.
     * 
     * @throws RepositoryException
     */
    public void testMixinAddedInSessionA2() throws RepositoryException {
        assertFalse(testNode.hasProperty("jcr:mixinTypes"));

        testNode.addMixin("test:mixinProp_1");

        testNodeB.setProperty("test:prop_double", sessionB.getValueFactory().createValue(false));
        sessionB.save();

        superuser.save();
        assertTrue(testNode.isNodeType("test:mixinProp_1"));
        assertEquals(PropertyType.BOOLEAN, testNode.getProperty("test:prop_double").getType());

        testNodeB.refresh(false);
        assertTrue(testNodeB.isNodeType("test:mixinProp_1"));
        assertEquals(PropertyType.BOOLEAN, testNodeB.getProperty("test:prop_double").getType());
        assertEquals(testNode.getProperty("test:prop_double").getString(), testNodeB.getProperty("test:prop_double").getString());
        assertEquals("nt:unstructured", testNode.getProperty("test:prop_double").getDefinition().getDeclaringNodeType().getName());
    }

    /**
     * Test adding mixin(s) to sessionA while sessionB doesn't add new mixins
     * but has new child items that collide with the items defined by the new mixin.
     * The merge should in this case fail.
     *
     * @throws RepositoryException
     */
    public void testAddedInSessionAConflictsWithChildItemsInSessionB() throws RepositoryException {
        assertFalse(testNode.hasProperty("jcr:mixinTypes"));
        
        testNode.addMixin("test:mixinProp_5"); // has an autocreated property

        testNodeB.setProperty("test:prop_long_p", "conflict");
        sessionB.save();

        try {
            superuser.save();
            fail();
        } catch (InvalidItemStateException e) {
            // expected
            assertFalse(testNodeB.isNodeType("test:mixinProp_5"));
            testNodeB.refresh(false);
            assertEquals("conflict", testNodeB.getProperty("test:prop_long_p").getString());
        }
    }

    /**
     * Similar to {@link #testAddedInSessionAConflictsWithChildItemsInSessionB}
     * but adding mix:referenceable in the SessionA while SessionB
     * gets a property jcr:uuid (with different type).
     * 
     * @throws RepositoryException
     */
// TODO: uncomment once JCR-2779 is fixed
//    public void testAddedReferenceableSessionAConflictsWithPropInSessionB() throws RepositoryException {
//        assertFalse(testNode.hasProperty("jcr:mixinTypes"));
//
//        testNode.addMixin(mixReferenceable);
//
//        Property p = testNode2.setProperty("jcr:uuid", session2.getValueFactory().createValue(false));
//        assertTrue(testNode2.hasProperty("jcr:uuid"));
//        assertTrue(p.isNew());
//        session2.save();
//
//        assertTrue(testNode2.hasProperty("jcr:uuid"));
//        assertTrue(p.isNew());
//        try {
//            superuser.save();
//            fail();
//        } catch (InvalidItemStateException e) {
//            assertTrue(testNode.isNodeType(mixReferenceable));
//            assertEquals(PropertyType.STRING, testNode.getProperty("jcr:uuid").getType());
//        }
//    }

    /**
     * Same as {@link #testAddedInSessionAConflictsWithChildItemsInSessionB}
     * but in addition the overlayed state gets an addition child node.
     *
     * @throws RepositoryException
     */
// TODO: uncomment once JCR-2779 is fixed
//    public void testAddedReferenceableSessionAConflictsWithPropInSessionB2() throws RepositoryException {
//        assertFalse(testNode.hasProperty("jcr:mixinTypes"));
//
//        testNode.addMixin(mixReferenceable);
//
//        testNode2.setProperty("jcr:uuid", session2.getValueFactory().createValue(false));
//        testNode2.addNode("test");
//        session2.save();
//
//        assertTrue(testNode2.hasProperty("jcr:uuid"));
//        assertTrue(testNode2.getProperty("jcr:uuid").isNew());
//        try {
//            superuser.save();
//            fail();
//        } catch (InvalidItemStateException e) {
//            assertTrue(testNode.isNodeType(mixReferenceable));
//            assertEquals(PropertyType.STRING, testNode.getProperty("jcr:uuid").getType());
//        }
//    }

    /**
     * Adding different node types in the SessionA and SessionB:
     * Not handled and thus the merge must fail.
     * 
     * @throws RepositoryException
     */
    public void testDifferentMixinAddedInSessionAB() throws Exception {
        assertFalse(testNode.hasProperty("jcr:mixinTypes"));
        
        testNode.addMixin("test:mixinProp_1");

        testNodeB.addMixin("test:mixinProp_3");
        sessionB.save();

        try {
            superuser.save();
            fail("Different mixin types added both in SessionA and SessionB. InvalidItemStateException expected.");
        } catch (InvalidItemStateException e) {
            // expected
            assertFalse(testNode.isNodeType("test:mixinProp_3"));
        }
    }

    //---------------- Tests removing jcr:mixinType property in the SessionA ---
    /**
     * Remove the jcr:mixinType property by removing all mixin types. Merge
     * to changes made in the overlayed state should fail.
     * @throws Exception
     */
    public void testMixinRemovedInSessionA() throws Exception {
        for (int i = 1; i<=5; i++) {
            testNode.addMixin("test:mixinProp_" + i);
        }    
        superuser.save();
        testNodeB.refresh(false);

        // remove all mixin types
        for (NodeType mixin : testNode.getMixinNodeTypes()) {
            testNode.removeMixin(mixin.getName());
        }
        assertFalse(testNode.hasProperty("jcr:mixinTypes"));

        testNodeB.addNode(nodeName1, "nt:unstructured");
        testNodeB.setProperty(propertyName1, "anyValue");
        sessionB.save();

        try {
            superuser.save();
            fail();
        } catch (InvalidItemStateException e) {
            // expected
        }
    }

    /**
     * Same as {@link #testMixinRemovedInSessionA} with different mixin types.
     * @throws Exception
     */
    public void testMixinRemovedInSessionA2() throws Exception {
        for (int i = 1; i<=4; i++) {
            testNode.addMixin("test:mixinNode_" + i);
        }
        superuser.save();
        testNodeB.refresh(false);

        // remove all mixin types
        for (NodeType mixin : testNode.getMixinNodeTypes()) {
            testNode.removeMixin(mixin.getName());
        }
        assertFalse(testNode.hasProperty("jcr:mixinTypes"));

        testNodeB.addNode(nodeName1, "nt:unstructured");
        testNodeB.setProperty(propertyName1, "anyValue");
        sessionB.save();

        try {
            superuser.save();
            fail();
        } catch (InvalidItemStateException e) {
            // expected
        }
    }

    //--------------- Tests modifying jcr:mixinType property in the SessionA ---
    /**
     * Modify the existing mixin property in the SessionA change without adding
     * conflicting modifications to SessionB.
     * 
     * @throws Exception
     */
    public void testMixinModifiedInSessionA() throws Exception {
        testNode.addMixin("test:mixinProp_5");    
        superuser.save();
        testNodeB.refresh(false);
        assertTrue(testNodeB.hasProperty("jcr:mixinTypes"));

        // modify the mixin types
        testNode.addMixin("test:mixinProp_1");

        testNodeB.addNode(nodeName1, "nt:unstructured");
        testNodeB.setProperty(propertyName1, "anyValue");
        sessionB.save();

        superuser.save();

        assertTrue(testNode.hasProperty(propertyName1));
        assertTrue(testNode.hasNode(nodeName1));

        assertTrue(testNodeB.isNodeType("test:mixinProp_1"));
        assertTrue(Arrays.asList(testNodeB.getMixinNodeTypes()).contains(sessionB.getWorkspace().getNodeTypeManager().getNodeType("test:mixinProp_1")));
    }

    public void testMixinModifiedInSessionAB() throws Exception {
        testNode.addMixin("test:mixinProp_5");
        superuser.save();
        testNodeB.refresh(false);
        assertTrue(testNodeB.hasProperty("jcr:mixinTypes"));

        // modify the mixin types
        testNode.addMixin("test:mixinProp_1");

        testNodeB.addMixin("test:mixinProp_1");
        sessionB.save();

        try {
            superuser.save();
            fail();
        } catch (InvalidItemStateException e) {
            // expected
        }
    }

    public void testMixinModifiedInSessionAB2() throws Exception {
        testNode.addMixin("test:mixinProp_5");
        superuser.save();
        testNodeB.refresh(false);
        assertTrue(testNodeB.hasProperty("jcr:mixinTypes"));

        // modify the mixin types
        testNode.addMixin("test:mixinProp_1");

        testNodeB.removeMixin("test:mixinProp_5");
        sessionB.save();

        try {
            superuser.save();
            fail();
        } catch (InvalidItemStateException e) {
            // expected
            assertTrue(testNode.hasProperty("jcr:mixinTypes"));
            assertTrue(testNode.isNodeType("test:mixinProp_1"));
            assertTrue(testNode.isNodeType("test:mixinProp_5"));
            assertEquals(2, testNode.getMixinNodeTypes().length);
        }
    }

    public void testMixinModifiedInSessionAB3() throws Exception {
        testNode.addMixin("test:mixinProp_5");
        superuser.save();
        testNodeB.refresh(false);
        assertTrue(testNodeB.hasProperty("jcr:mixinTypes"));

        // modify the mixin types
        testNode.addMixin("test:mixinProp_1");
        testNode.setProperty(propertyName1, "value");

        testNodeB.removeMixin("test:mixinProp_5");
        sessionB.save();

        try {
            superuser.save();
            fail();
        } catch (InvalidItemStateException e) {
            // expected
            assertTrue(testNode.hasProperty("jcr:mixinTypes"));
            assertTrue(testNode.isNodeType("test:mixinProp_1"));
            assertTrue(testNode.isNodeType("test:mixinProp_5"));
            assertEquals(2, testNode.getMixinNodeTypes().length);
        }
    }

    public void testMixinModifiedInSessionAB4() throws Exception {
        testNode.addMixin("test:mixinProp_5");
        superuser.save();
        testNodeB.refresh(false);
        assertTrue(testNodeB.hasProperty("jcr:mixinTypes"));

        // modify the mixin types
        testNode.addMixin("test:mixinProp_1");

        testNodeB.addMixin("test:mixinProp_2");
        sessionB.save();

        try {
            superuser.save();
            fail();
        } catch (InvalidItemStateException e) {
            // expected
        } 
    }

    //-------------------------- Tests adding jcr:mixinType only in SessionB ---
    /**
     * No jcr:mixinTypes property present in the SessionA but was added
     * to the overlayed state while other changes were made to the SessionA.
     * @throws Exception
     */
    public void testMixinAddedInSessionB() throws Exception {
        assertFalse(testNode.hasProperty("jcr:mixinTypes"));

        testNode.addNode(nodeName2);

        testNodeB.addMixin("test:mixinProp_1");
        sessionB.save();

        superuser.save();

        assertTrue(testNode.isNodeType("test:mixinProp_1"));
        assertTrue(testNode.hasProperty("jcr:mixinTypes"));

        assertTrue(testNodeB.hasNode(nodeName2));
    }

    /**
     * Same as {@link #testMixinAddedInSessionB} but having 2 SessionA modifications.
     * @throws Exception
     */
    public void testMixinAddedInSessionB2() throws Exception {
        assertFalse(testNode.hasProperty("jcr:mixinTypes"));

        testNode.addNode(nodeName2);
        testNode.setProperty(propertyName1, "value");

        testNodeB.addMixin("test:mixinProp_1");
        sessionB.save();

        superuser.save();

        assertTrue(testNode.isNodeType("test:mixinProp_1"));
        assertTrue(testNode.hasProperty("jcr:mixinTypes"));

        assertTrue(testNodeB.hasNode(nodeName2));
    }

    /**
     * Add the mixin property in SessionB by adding a single mixin
     * and create a conflicting item in the SessionA -> merge must fail.
     * @throws Exception
     */
    public void testMixinAddedInSessionBWithConflictingChanges() throws Exception {
        assertFalse(testNode.hasProperty("jcr:mixinTypes"));
        
        testNode.addNode(nodeName2);
        testNode.setProperty("test:prop_long_p", "value");

        testNodeB.addMixin("test:mixinProp_5");
        sessionB.save();

        try {
            superuser.save();
            fail();
        } catch (InvalidItemStateException e) {
            // expected
        }
    }

    //----------------------- Test jcr:mixinTypes removed in SessionB ----------
    /**
     * Test if removing the (existing) jcr:mixinTypes property in the overlayed
     * state (by removing all mixins) is not merged to SessionA changes.
     * @throws Exception
     */
    public void testMixinRemovedInSessionB() throws Exception {
        for (int i = 1; i<=5; i++) {
            testNode.addMixin("test:mixinProp_" + i);
        }
        superuser.save();
        testNodeB.refresh(false);

        testNode.addNode(nodeName1, "nt:unstructured");
        testNode.setProperty(propertyName1, "anyValue");

        // remove all mixin types
        for (NodeType mixin : testNodeB.getMixinNodeTypes()) {
            testNodeB.removeMixin(mixin.getName());
        }
        assertFalse(testNodeB.hasProperty("jcr:mixinTypes"));
        sessionB.save();

        try {
            superuser.save();
            fail();
        } catch (InvalidItemStateException e) {
            // expected
        }
    }

    //----------------------- Test mixin modification (add-only) in SessionB ---
    /**
     * Existing mixins are modified in SessionB but not in the
     * SessionA, where the net effect is 'add-only' in the SessionA. 
     * Changes should be merge if there are no conflicting SessionA
     * modifications.
     * 
     * @throws Exception
     */
    public void testMixinModifiedAddInSessionB() throws Exception {
        for (int i = 1; i<=5; i++) {
            testNode.addMixin("test:mixinProp_" + i);
        }
        superuser.save();
        testNodeB.refresh(false);
        assertTrue(testNodeB.hasProperty("jcr:mixinTypes"));

        // non-conflicting modification in SessionA
        testNode.setProperty(propertyName1, "value");

        testNodeB.addMixin("test:mixinNode_1");
        sessionB.save();

        superuser.save();

        assertTrue(testNode.isNodeType("test:mixinNode_1"));
        List<NodeType> mx = Arrays.asList(testNode.getMixinNodeTypes());
        assertTrue(mx.contains(superuser.getWorkspace().getNodeTypeManager().getNodeType("test:mixinNode_1")));

        testNodeB.refresh(false);
        assertTrue(testNodeB.hasProperty(propertyName1));
    }

    /**
     * Same as {@link #testMixinModifiedAddInSessionB} with different
     * SessionA modifications.
     *
     * @throws Exception
     */
    public void testMixinModifiedAddInSessionB2() throws Exception {
        for (int i = 1; i<=5; i++) {
            testNode.addMixin("test:mixinProp_" + i);
        }
        superuser.save();
        testNodeB.refresh(false);
        assertTrue(testNodeB.hasProperty("jcr:mixinTypes"));

        // non-conflicting modification in SessionA
        testNode.setProperty(propertyName1, "value");
        testNode.addNode(nodeName2);

        testNodeB.addMixin("test:mixinNode_1");
        sessionB.save();

        superuser.save();
        assertTrue(testNode.isNodeType("test:mixinNode_1"));
        List<NodeType> mx = Arrays.asList(testNode.getMixinNodeTypes());
        assertTrue(mx.contains(superuser.getWorkspace().getNodeTypeManager().getNodeType("test:mixinNode_1")));

        testNodeB.refresh(false);
        assertTrue(testNodeB.hasProperty(propertyName1));
        assertTrue(testNodeB.hasNode(nodeName2));
    }

    /**
     * Test if the merge of add-only mixin modification in the overlayed stated
     * is aborted if there are conflicting SessionA changes present.
     * @throws Exception
     */
    public void testMixinModifiedAddInSessionBWithConflictingChanges() throws Exception {
        testNode.addMixin(mixReferenceable);
        superuser.save();
        testNodeB.refresh(false);
        assertTrue(testNodeB.hasProperty("jcr:mixinTypes"));
   
        // conflicting modification in SessionA
        testNode.setProperty(propertyName1, "value");
        testNode.addNode("test:child_1");

        testNodeB.addMixin("test:mixinNode_1");
        sessionB.save();

        try {
            superuser.save();
            fail();
        } catch (InvalidItemStateException e) {
            // expected
        }
    }

    /**
     * Same as {@link #testMixinModifiedAddInSessionBWithConflictingChanges}
     * with different SessionA modifications.
     *
     * @throws Exception
     */
    public void testMixinModifiedAddInSessionBWithConflictingChanges2() throws Exception {
        testNode.addMixin(mixReferenceable);
        superuser.save();
        testNodeB.refresh(false);
        assertTrue(testNodeB.hasProperty("jcr:mixinTypes"));

        // conflicting modification in SessionA
        testNode.addNode("test:child_1");

        testNodeB.addMixin("test:mixinNode_1");
        sessionB.save();

        try {
            superuser.save();
            fail();
        } catch (InvalidItemStateException e) {
            // expected
        }
    }

    /**
     * Same as {@link #testMixinModifiedAddInSessionBWithConflictingChanges}
     * with different mixin and SessionA modifications.
     *
     * @throws Exception
     */
    public void testMixinModifiedAddInSessionBWithConflictingChanges3() throws Exception {
        testNode.addMixin(mixReferenceable);
        superuser.save();
        testNodeB.refresh(false);
        assertTrue(testNodeB.hasProperty("jcr:mixinTypes"));

        // conflicting modification in SessionA
        testNode.setProperty("test:prop_long_p", "non-long-value");

        testNodeB.addMixin("test:mixinProp_5");
        sessionB.save();

        try {
            superuser.save();
            fail();
        } catch (InvalidItemStateException e) {
            // expected
        }
    }

    /**
     * Same as {@link #testMixinModifiedAddInSessionBWithConflictingChanges}
     * with different mixin and other kind of modifications.
     *
     * @throws Exception
     */
    public void testMixinModifiedAddInSessionBWithConflictingChanges4() throws Exception {
        testNode.addMixin(mixReferenceable);
        superuser.save();
        testNodeB.refresh(false);
        assertTrue(testNodeB.hasProperty("jcr:mixinTypes"));

        // conflicting modification in session1
        testNode.setProperty("test:prop_long_p", "non-long-value");        
        testNode.addNode("test:child_1");

        testNodeB.addMixin("test:mixinProp_5");
        sessionB.save();

        try {
            superuser.save();
            fail();
        } catch (InvalidItemStateException e) {
            // expected
        }
    }

    /**
     * Same as {@link #testMixinModifiedAddInSessionBWithConflictingChanges}
     * but using mix:referencable as mixin to force the modification in the
     * overlayed state.
     *
     * @throws Exception
     */
// TODO: uncomment once JCR-2779 is fixed
//    public void testMixinModifiedReferenceableInSessionBConflicting() throws RepositoryException {
//        testNode.addMixin("test:mixinProp_1");
//        superuser.save();
//        assertTrue(testNode2.hasProperty("jcr:mixinTypes"));
//
//        testNode.setProperty("jcr:uuid", superuser.getValueFactory().createValue(false));
//
//        testNode2.addMixin(mixReferenceable);
//        session2.save();
//
//        assertTrue(testNode.hasProperty("jcr:uuid"));
//        assertTrue(testNode.getProperty("jcr:uuid").isNew());
//        try {
//            superuser.save();
//            fail();
//        } catch (InvalidItemStateException e) {
//            assertFalse(testNode.isNodeType(mixReferenceable));
//            assertEquals(PropertyType.BOOLEAN, testNode.getProperty("jcr:uuid").getType());
//
//            assertTrue(testNode2.isNodeType(mixReferenceable));
//            assertEquals(PropertyType.STRING, testNode2.getProperty("jcr:uuid").getType());
//        }
//    }

    /**
     * Same as {@link #testMixinModifiedAddInSessionBWithConflictingChanges}
     * but using mix:referencable as mixin to force the modification in the
     * overlayed state.
     *
     * @throws Exception
     */
// TODO: uncomment once JCR-2779 is fixed
//    public void testMixinModifiedReferenceableInSessionBConflicting2() throws RepositoryException {
//        testNode.addMixin("test:mixinProp_1");
//        superuser.save();
//        assertTrue(testNode2.hasProperty("jcr:mixinTypes"));
//
//        testNode.setProperty("jcr:uuid", superuser.getValueFactory().createValue(false));
//        testNode.addNode(nodeName2);
//
//        testNode2.addMixin(mixReferenceable);
//        session2.save();
//
//        assertTrue(testNode.hasProperty("jcr:uuid"));
//        assertTrue(testNode.getProperty("jcr:uuid").isNew());
//        try {
//            superuser.save();
//            fail();
//        } catch (InvalidItemStateException e) {
//            assertFalse(testNode.isNodeType(mixReferenceable));
//            assertEquals(PropertyType.BOOLEAN, testNode.getProperty("jcr:uuid").getType());
//
//            assertTrue(testNode2.isNodeType(mixReferenceable));
//            assertEquals(PropertyType.STRING, testNode2.getProperty("jcr:uuid").getType());
//        }
//    }

    //-------------------- Test mixin modification (remove-only) in SessionB ---
    /**
     * Test if merge fails if some mixin removal occurred in the SessionB.
     *
     * @throws Exception
     */
    public void testMixinModifiedRemovedInSessionB() throws Exception {
        for (String mixin : testMixins) {
            testNode.addMixin(mixin);
        }
        superuser.save();
        testNodeB.refresh(false);
        assertTrue(testNodeB.hasProperty("jcr:mixinTypes"));


        // modification in session1
        testNode.setProperty(propertyName1, "value");

        // mixin-removal in the session2
        testNodeB.removeMixin("test:mixinProp_1");
        testNodeB.removeMixin("test:mixinProp_2");
        sessionB.save();

        try {
            superuser.save();
            fail();
        } catch (InvalidItemStateException e) {
            // expected
        }
    }

    /**
     * Same as {@link #testMixinModifiedRemovedInSessionB} but with different
     * SessionA modifications.
     * 
     * @throws Exception
     */
    public void testMixinModifiedRemovedInSessionB2() throws Exception {
        for (String mixin : testMixins) {
            testNode.addMixin(mixin);
        }
        superuser.save();
        testNodeB.refresh(false);
        assertTrue(testNodeB.hasProperty("jcr:mixinTypes"));


        // modification in SessionA
        testNode.setProperty(propertyName1, "value");
        testNode.addNode(nodeName2);

        // mixin-removal in SessionB
        testNodeB.removeMixin("test:mixinProp_1");
        testNodeB.removeMixin("test:mixinProp_2");
        sessionB.save();

        try {
            superuser.save();
            fail();
        } catch (InvalidItemStateException e) {
            // expected
        }
    }

    //---------------------------- Test other mixin modification in SessionB ---
    /**
     * Test if merge fails if the mixins of the overlayed state (sessionB) were
     * modified in a combination of add and removal of mixin.
     * 
     * @throws Exception
     */
    public void testMixinModifiedInSessionB() throws Exception {
        for (String mixin : testMixins) {
            testNode.addMixin(mixin);
        }
        superuser.save();
        testNodeB.refresh(false);
        assertTrue(testNodeB.hasProperty("jcr:mixinTypes"));

        // modification in SessionA
        testNode.setProperty(propertyName1, "value");

        // mixin-removal in the SessionB
        testNodeB.addMixin(mixReferenceable);
        testNodeB.removeMixin("test:mixinProp_2");
        sessionB.save();

        try {
            superuser.save();
            fail();
        } catch (InvalidItemStateException e) {
            // expected
        }
    }
}