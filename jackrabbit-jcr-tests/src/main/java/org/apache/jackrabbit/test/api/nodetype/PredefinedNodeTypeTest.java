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
package org.apache.jackrabbit.test.api.nodetype;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Comparator;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.OnParentVersionAction;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>PredefinedNodeTypeTest</code> tests if the predefined node types are
 * implemented correctly.
 *
 * @test
 * @sources PredefinedNodeTypeTest.java
 * @executeClass org.apache.jackrabbit.test.api.nodetype.PredefinedNodeTypeTest
 * @keywords level1
 */
public class PredefinedNodeTypeTest extends AbstractJCRTest {

    /**
     * The NodeTypeManager of the session
     */
    private NodeTypeManager manager;

    /**
     * The read-only session for the test
     */
    private Session session;

    /**
     * Sets up the fixture for this test.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();

        session = helper.getReadOnlySession();
        manager = session.getWorkspace().getNodeTypeManager();
    }

    /**
     * Releases the session aquired in {@link #setUp()}.
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
            session = null;
        }
        manager = null;
        super.tearDown();
    }

    /**
     * Tests if all primary node types are subtypes of node type <code>nt:base</code>
     */
    public void testIfPrimaryNodeTypesAreSubtypesOfNTBase()
            throws RepositoryException {
        NodeTypeIterator types = manager.getPrimaryNodeTypes();
        while (types.hasNext()) {
            NodeType type = types.nextNodeType();
            assertTrue("Primary node type " + type.getName() +
                    " must inherit nt:base",
                    type.isNodeType("nt:base"));
        }
    }

    /** Test for the predefined mix:lockable node type. */
    public void testLockable() throws NotExecutableException {
        testPredefinedNodeType("mix:lockable");
    }

    /** Test for the predefined mix:referenceable node type. */
    public void testReferenceable() throws NotExecutableException {
        testPredefinedNodeType("mix:referenceable");
    }

    /** Test for the predefined mix:versionable node type. */
    public void testVersionable() throws NotExecutableException {
        testPredefinedNodeType("mix:versionable");
    }

    /** Test for the predefined nt:base node type. */
    public void testBase() throws NotExecutableException {
        testPredefinedNodeType("nt:base");
    }

    /** Test for the predefined nt:unstructured node type. */
    public void testUnstructured() throws NotExecutableException {
        testPredefinedNodeType("nt:unstructured");
    }

    /** Test for the predefined nt:hierarchyNode node type. */
    public void testHierarchyNode() throws NotExecutableException {
        testPredefinedNodeType("nt:hierarchyNode");
    }

    /** Test for the predefined nt:file node type. */
    public void testFile() throws NotExecutableException {
        testPredefinedNodeType("nt:file");
    }

    /** Test for the predefined nt:linkedFile node type. */
    public void testLinkedFile() throws NotExecutableException {
        testPredefinedNodeType("nt:linkedFile");
    }

    /** Test for the predefined nt:folder node type. */
    public void testFolder() throws NotExecutableException {
        testPredefinedNodeType("nt:folder");
    }

    /** Test for the predefined nt:nodeType node type. */
    public void testNodeType() throws NotExecutableException {
        testPredefinedNodeType("nt:nodeType");
    }

    /** Test for the predefined nt:propertyDef node type. */
    public void testPropertyDef() throws NotExecutableException {
        testPredefinedNodeType("nt:propertyDefinition");
    }

    /** Test for the predefined nt:childNodeDef node type. */
    public void testChildNodeDef() throws NotExecutableException {
        testPredefinedNodeType("nt:childNodeDefinition");
    }

    /** Test for the predefined nt:versionHistory node type. */
    public void testVersionHistory() throws NotExecutableException {
        testPredefinedNodeType("nt:versionHistory");
    }

    /** Test for the predefined nt:versionLabels node type. */
    public void testVersionLabels() throws NotExecutableException {
        testPredefinedNodeType("nt:versionLabels");
    }

    /** Test for the predefined nt:version node type. */
    public void testVersion() throws NotExecutableException {
        testPredefinedNodeType("nt:version");
    }

    /** Test for the predefined nt:frozenNode node type. */
    public void testFrozenNode() throws NotExecutableException {
        testPredefinedNodeType("nt:frozenNode");
    }

    /** Test for the predefined nt:versionedChild node type. */
    public void testVersionedChild() throws NotExecutableException {
        testPredefinedNodeType("nt:versionedChild");
    }

    /** Test for the predefined nt:query node type. */
    public void testQuery() throws NotExecutableException {
        testPredefinedNodeType("nt:query");
    }

    /** Test for the predefined nt:resource node type. */
    public void testResource() throws NotExecutableException {
        testPredefinedNodeType("nt:resource");
    }

    /**
     * Tests that the named node type matches the JSR 170 specification.
     * The test is performed by genererating a node type definition spec
     * string in the format used by the JSR 170 specification, and comparing
     * the result with a static spec file extracted from the specification
     * itself.
     * <p>
     * Note that the extracted spec files are not exact copies of the node
     * type specification in the JSR 170 document. Some formatting and
     * ordering changes have been made to simplify the test code, but the
     * semantics remain the same.
     *
     * @param name node type name
     * @throws NotExecutableException if the node type is not supported by
     *   this repository implementation.
     */
    private void testPredefinedNodeType(String name)
            throws NotExecutableException {
        try {
            StringBuffer spec = new StringBuffer();
            String resource =
                "org/apache/jackrabbit/test/api/nodetype/spec/"
                + name.replace(':', '-') + ".txt";
            Reader reader = new InputStreamReader(
                    getClass().getClassLoader().getResourceAsStream(resource));
            for (int ch = reader.read(); ch != -1; ch = reader.read()) {
                spec.append((char) ch);
            }

            NodeType type = manager.getNodeType(name);
            String current = getNodeTypeSpec(type);
            if (!System.getProperty("line.separator").equals("\n")) {
                current = normalizeLineSeparators(current);
            }
            String expected = normalizeLineSeparators(spec.toString());

            assertEquals("Predefined node type " + name, expected, current);
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (NoSuchNodeTypeException e) {
            // only nt:base is strictly required
            if ("nt:base".equals(name)) {
                fail(e.getMessage());
            } else {
                throw new NotExecutableException("NodeType " + name +
                        " not supported by this repository implementation.");
            }
        } catch (RepositoryException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Creates and returns a spec string for the given node type definition.
     * The returned spec string follows the node type definition format
     * used in the JSR 170 specification.
     *
     * @param type node type definition
     * @return spec string
     * @throws RepositoryException on repository errors
     */
    private static String getNodeTypeSpec(NodeType type)
            throws RepositoryException {
        String typeName = type.getName();
        StringWriter buffer = new StringWriter();

        PrintWriter writer = new PrintWriter(buffer);
        writer.println("NodeTypeName");
        writer.println("  " + typeName);
        writer.println("Supertypes");
        NodeType[] supertypes = type.getDeclaredSupertypes();
        Arrays.sort(supertypes, NODE_TYPE_COMPARATOR);
        boolean hasPrinted = false;
        for (int i = 0; i < supertypes.length; i++) {
            String name = supertypes[i].getName();
            if (name.startsWith("nt:") ||
                    (name.equals("mix:referenceable") &&
                        (typeName.equals("mix:versionable") ||
                            typeName.equals("nt:resource") ||
                            typeName.equals("nt:versionHistory") ||
                            typeName.equals("nt:version") ||
                            typeName.equals("nt:frozenNode")))) {
                writer.println("  " + supertypes[i].getName());
                hasPrinted = true;
            }
        }
        if (!hasPrinted) {
            writer.println("  []");
        }
        writer.println("IsMixin");
        writer.println("  " + type.isMixin());
        writer.println("HasOrderableChildNodes");
        writer.println("  " + type.hasOrderableChildNodes());
        writer.println("PrimaryItemName");
        writer.println("  " + type.getPrimaryItemName());
        NodeDefinition[] nodes = type.getDeclaredChildNodeDefinitions();
        Arrays.sort(nodes, NODE_DEF_COMPARATOR);
        for (int i = 0; i < nodes.length; i++) {
            writer.print(getChildNodeDefSpec(nodes[i]));
        }
        PropertyDefinition[] properties = type.getDeclaredPropertyDefinitions();
        Arrays.sort(properties, PROPERTY_DEF_COMPARATOR);
        for (int i = 0; i < properties.length; i++) {
            writer.print(getPropertyDefSpec(properties[i]));
        }

        return buffer.toString();
    }

    /**
     * Creates and returns a spec string for the given node definition.
     * The returned spec string follows the child node definition format
     * used in the JSR 170 specification.
     *
     * @param node child node definition
     * @return spec string
     */
    private static String getChildNodeDefSpec(NodeDefinition node) {
        StringWriter buffer = new StringWriter();

        PrintWriter writer = new PrintWriter(buffer);
        writer.println("ChildNodeDefinition");
        if (node.getName().equals("*")) {
            writer.println("  Name \"*\"");
        } else {
            writer.println("  Name " + node.getName());
        }
        writer.print("  RequiredPrimaryTypes [");
        NodeType[] types = node.getRequiredPrimaryTypes();
        Arrays.sort(types, NODE_TYPE_COMPARATOR);
        for (int j = 0; j < types.length; j++) {
            if (j > 0) {
                writer.print(',');
            }
            writer.print(types[j].getName());
        }
        writer.println("]");
        if (node.getDefaultPrimaryType() != null) {
            writer.println("  DefaultPrimaryType "
                    + node.getDefaultPrimaryType().getName());
        } else {
            writer.println("  DefaultPrimaryType null");
        }
        writer.println("  AutoCreated " + node.isAutoCreated());
        writer.println("  Mandatory " + node.isMandatory());
        writer.println("  OnParentVersion "
                + OnParentVersionAction.nameFromValue(node.getOnParentVersion()));
        writer.println("  Protected " + node.isProtected());
        writer.println("  SameNameSiblings " + node.allowsSameNameSiblings());

        return buffer.toString();
    }

    /**
     * Creates and returns a spec string for the given property definition.
     * The returned spec string follows the property definition format
     * used in the JSR 170 specification.
     *
     * @param property property definition
     * @return spec string
     * @throws RepositoryException on repository errors
     */
    private static String getPropertyDefSpec(PropertyDefinition property)
            throws RepositoryException {
        StringWriter buffer = new StringWriter();

        PrintWriter writer = new PrintWriter(buffer);
        writer.println("PropertyDefinition");
        if (property.getName().equals("*")) {
            writer.println("  Name \"*\"");
        } else {
            writer.println("  Name " + property.getName());
        }
        String type = PropertyType.nameFromValue(property.getRequiredType());
        writer.println("  RequiredType " + type.toUpperCase());
        Value[] values = property.getDefaultValues();
        if (values != null && values.length > 0) {
            writer.print("  DefaultValues [");
            for (int j = 0; j < values.length; j++) {
                if (j > 0) {
                    writer.print(',');
                }
                writer.print(values[j].getString());
            }
            writer.println("]");
        } else {
            writer.println("  DefaultValues null");
        }
        writer.println("  AutoCreated " + property.isAutoCreated());
        writer.println("  Mandatory " + property.isMandatory());
        String action = OnParentVersionAction.nameFromValue(
                property.getOnParentVersion());
        writer.println("  OnParentVersion " + action);
        writer.println("  Protected " + property.isProtected());
        writer.println("  Multiple " + property.isMultiple());

        return buffer.toString();
    }

    /**
     * Replaces platform-dependant line-separators in <code>stringValue</code>
     * with "\n".
     *
     * @param stringValue string to normalize
     * @return the normalized string
     */
    private String normalizeLineSeparators(String stringValue) {
        // Replace "\r\n" (Windows format) with "\n" (Unix format)
        stringValue = stringValue.replaceAll("\r\n", "\n");
        // Replace "\r" (Mac format) with "\n" (Unix format)
        stringValue = stringValue.replaceAll("\r", "\n");

        return stringValue;
    }

    /**
     * Comparator for ordering node definition arrays. Node definitions are
     * ordered by name, with the wildcard item definition ("*") ordered last.
     */
    private static final Comparator NODE_DEF_COMPARATOR = new Comparator() {
        public int compare(Object a, Object b) {
            NodeDefinition nda = (NodeDefinition) a;
            NodeDefinition ndb = (NodeDefinition) b;
            if (nda.getName().equals("*") && !ndb.getName().equals("*")) {
                return 1;
            } else if (!nda.getName().equals("*") && ndb.getName().equals("*")) {
                return -1;
            } else {
                return nda.getName().compareTo(ndb.getName());
            }
        }
    };

    /**
     * Comparator for ordering property definition arrays. Property definitions
     * are ordered by name, with the wildcard item definition ("*") ordered
     * last, and isMultiple flag, with <code>isMultiple==true</code> ordered last.
     */
    private static final Comparator PROPERTY_DEF_COMPARATOR = new Comparator() {
        public int compare(Object a, Object b) {
            PropertyDefinition pda = (PropertyDefinition) a;
            PropertyDefinition pdb = (PropertyDefinition) b;
            if (pda.getName().equals("*") && !pdb.getName().equals("*")) {
                return 1;
            } else if (!pda.getName().equals("*") && pdb.getName().equals("*")) {
                return -1;
            }
            int result = pda.getName().compareTo(pdb.getName());
            if (result != 0) {
                return result;
            }
            if (pda.isMultiple() && !pdb.isMultiple()) {
                return 1;
            } else if (!pda.isMultiple() && pdb.isMultiple()) {
                return -1;
            } else {
                return 0;
            }
        }
    };

    /**
     * Comparator for ordering node type arrays. Node types are ordered by
     * name, with all primary node types ordered before mixin node types.
     */
    private static final Comparator NODE_TYPE_COMPARATOR = new Comparator() {
        public int compare(Object a, Object b) {
            NodeType nta = (NodeType) a;
            NodeType ntb = (NodeType) b;
            if (nta.isMixin() && !ntb.isMixin()) {
                return 1;
            } else if (!nta.isMixin() && ntb.isMixin()) {
                return -1;
            } else {
                return nta.getName().compareTo(ntb.getName());
            }
        }
    };

}
