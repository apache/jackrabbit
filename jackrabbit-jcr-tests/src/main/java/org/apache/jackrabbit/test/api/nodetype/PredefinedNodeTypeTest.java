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
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

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
 */
public class PredefinedNodeTypeTest extends AbstractJCRTest {

    private static final Map<String, String[]> SUPERTYPES = new HashMap<String, String[]>();

    static {
        SUPERTYPES.put("mix:created", new String[]{});
        SUPERTYPES.put("mix:etag", new String[]{});
        SUPERTYPES.put("mix:language", new String[]{});
        SUPERTYPES.put("mix:lastModified", new String[]{});
        SUPERTYPES.put("mix:lifecycle", new String[]{});
        SUPERTYPES.put("mix:lockable", new String[]{});
        SUPERTYPES.put("mix:mimeType", new String[]{});
        SUPERTYPES.put("mix:referenceable", new String[]{});
        SUPERTYPES.put("mix:shareable", new String[]{"mix:referenceable"});
        SUPERTYPES.put("mix:simpleVersionable", new String[]{});
        SUPERTYPES.put("mix:title", new String[]{});
        SUPERTYPES.put("mix:versionable", new String[]{"mix:referenceable", "mix:simpleVersionable"});
        SUPERTYPES.put("nt:activity", new String[]{"nt:base"});
        SUPERTYPES.put("nt:address", new String[]{"nt:base"});
        SUPERTYPES.put("nt:base", new String[]{});
        SUPERTYPES.put("nt:childNodeDefinition", new String[]{"nt:base"});
        SUPERTYPES.put("nt:configuration", new String[]{"nt:base"});
        SUPERTYPES.put("nt:file", new String[]{"nt:hierarchyNode"});
        SUPERTYPES.put("nt:folder", new String[]{"nt:hierarchyNode"});
        SUPERTYPES.put("nt:frozenNode", new String[]{"nt:base"});
        SUPERTYPES.put("nt:hierarchyNode", new String[]{"nt:base", "mix:created"});
        SUPERTYPES.put("nt:linkedFile", new String[]{"nt:hierarchyNode"});
        SUPERTYPES.put("nt:nodeType", new String[]{"nt:base"});
        SUPERTYPES.put("nt:propertyDefinition", new String[]{"nt:base"});
        SUPERTYPES.put("nt:query", new String[]{"nt:base"});
        SUPERTYPES.put("nt:resource", new String[]{"nt:base", "mix:lastModified", "mix:mimeType"});
        SUPERTYPES.put("nt:unstructured", new String[]{"nt:base"});
        SUPERTYPES.put("nt:version", new String[]{"nt:base", "mix:referenceable"});
        SUPERTYPES.put("nt:versionedChild", new String[]{"nt:base"});
        SUPERTYPES.put("nt:versionHistory", new String[]{"nt:base", "mix:referenceable"});
        SUPERTYPES.put("nt:versionLabels", new String[]{"nt:base"});
    }

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

        session = getHelper().getReadOnlySession();
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

    /** Test for the predefined mix:lifecycle node type. */
    public void testLifecycle() throws NotExecutableException {
        testPredefinedNodeType("mix:lifecycle", false);
    }

    /** Test for the predefined mix:lockable node type. */
    public void testLockable() throws NotExecutableException {
        testPredefinedNodeType("mix:lockable", false);
    }

    /** Test for the predefined mix:referenceable node type. */
    public void testReferenceable() throws NotExecutableException {
        testPredefinedNodeType("mix:referenceable", false);
    }

    /** Test for the predefined mix:referenceable node type. */
    public void testShareable() throws NotExecutableException {
        testPredefinedNodeType("mix:shareable", false);
    }

    /** Test for the predefined mix:versionable node type. */
    public void testVersionable() throws NotExecutableException {
        testPredefinedNodeType("mix:versionable", false);
    }

    /** Test for the predefined mix:simpleVersionable node type. */
    public void testSimpleVersionable() throws NotExecutableException {
        testPredefinedNodeType("mix:simpleVersionable", false);
    }

    /** Test for the predefined mix:created node type. */
    public void testMixCreated() throws NotExecutableException {
        testPredefinedNodeType("mix:created", true);
    }

    /** Test for the predefined mix:lastModified node type. */
    public void testMixLastModified() throws NotExecutableException {
        testPredefinedNodeType("mix:lastModified", true);
    }

    /** Test for the predefined mix:etag node type. */
    public void testMixETag() throws NotExecutableException {
        testPredefinedNodeType("mix:etag", false);
    }

    /** Test for the predefined mix:title node type. */
    public void testMixTitle() throws NotExecutableException {
        testPredefinedNodeType("mix:title", true);
    }

    /** Test for the predefined mix:language node type. */
    public void testMixLanguage() throws NotExecutableException {
        testPredefinedNodeType("mix:language", true);
    }

    /** Test for the predefined mix:language node type. */
    public void testMixMimeType() throws NotExecutableException {
        testPredefinedNodeType("mix:mimeType", true);
    }

    /** Test for the predefined nt:address node type. */
    public void testNtAddress() throws NotExecutableException {
        testPredefinedNodeType("nt:address", false);
    }

    /** Test for the predefined nt:base node type. */
    public void testBase() throws NotExecutableException {
        testPredefinedNodeType("nt:base", false);
    }

    /** Test for the predefined nt:unstructured node type. */
    public void testUnstructured() throws NotExecutableException {
        testPredefinedNodeType("nt:unstructured", false);
    }

    /** Test for the predefined nt:hierarchyNode node type. */
    public void testHierarchyNode() throws NotExecutableException {
        testPredefinedNodeType("nt:hierarchyNode", false);
    }

    /** Test for the predefined nt:file node type. */
    public void testFile() throws NotExecutableException {
        testPredefinedNodeType("nt:file", false);
    }

    /** Test for the predefined nt:linkedFile node type. */
    public void testLinkedFile() throws NotExecutableException {
        testPredefinedNodeType("nt:linkedFile", false);
    }

    /** Test for the predefined nt:folder node type. */
    public void testFolder() throws NotExecutableException {
        testPredefinedNodeType("nt:folder", false);
    }

    /** Test for the predefined nt:nodeType node type. */
    public void testNodeType() throws NotExecutableException {
        testPredefinedNodeType("nt:nodeType", false);
    }

    /** Test for the predefined nt:propertyDef node type. */
    public void testPropertyDef() throws NotExecutableException {
        testPredefinedNodeType("nt:propertyDefinition", false);
    }

    /** Test for the predefined nt:childNodeDef node type. */
    public void testChildNodeDef() throws NotExecutableException {
        testPredefinedNodeType("nt:childNodeDefinition", false);
    }

    /** Test for the predefined nt:versionHistory node type. */
    public void testVersionHistory() throws NotExecutableException {
        testPredefinedNodeType("nt:versionHistory", false);
    }

    /** Test for the predefined nt:versionLabels node type. */
    public void testVersionLabels() throws NotExecutableException {
        testPredefinedNodeType("nt:versionLabels", false);
    }

    /** Test for the predefined nt:version node type. */
    public void testVersion() throws NotExecutableException {
        testPredefinedNodeType("nt:version", false);
    }

    /** Test for the predefined nt:activity node type. */
    public void testActivity() throws NotExecutableException {
        testPredefinedNodeType("nt:activity", false);
    }

    /** Test for the predefined nt:configuration node type. */
    public void testConfiguration() throws NotExecutableException {
        testPredefinedNodeType("nt:configuration", false);
    }

    /** Test for the predefined nt:frozenNode node type. */
    public void testFrozenNode() throws NotExecutableException {
        testPredefinedNodeType("nt:frozenNode", false);
    }

    /** Test for the predefined nt:versionedChild node type. */
    public void testVersionedChild() throws NotExecutableException {
        testPredefinedNodeType("nt:versionedChild", false);
    }

    /** Test for the predefined nt:query node type. */
    public void testQuery() throws NotExecutableException {
        testPredefinedNodeType("nt:query", false);
    }

    /** Test for the predefined nt:resource node type. */
    public void testResource() throws NotExecutableException {
        testPredefinedNodeType("nt:resource", false);
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
     * @param propsVariant whether the properties of this node type may
     *   have implementation variant autocreated and OPV flags.
     * @throws NotExecutableException if the node type is not supported by
     *   this repository implementation.
     */
    private void testPredefinedNodeType(String name, boolean propsVariant)
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
            String current = getNodeTypeSpec(type, propsVariant).trim();
            if (!System.getProperty("line.separator").equals("\n")) {
                current = normalizeLineSeparators(current);
            }
            String expected = normalizeLineSeparators(spec.toString()).trim();

            assertEquals("Predefined node type " + name, expected, current);

            // check minimum declared supertypes
            Set<String> declaredSupertypes = new HashSet<String>();
            for (Iterator<NodeType> it = Arrays.asList(
                    type.getDeclaredSupertypes()).iterator(); it.hasNext(); ) {
                NodeType nt = it.next();
                declaredSupertypes.add(nt.getName());
            }
            for (Iterator<String> it = Arrays.asList(
                    SUPERTYPES.get(name)).iterator(); it.hasNext(); ) {
                String supertype = it.next();
                assertTrue("Predefined node type " + name + " does not " +
                        "declare supertype " + supertype,
                        declaredSupertypes.contains(supertype));
            }
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
     * @param propsVariant whether the properties of this node type may
     *   have implementation variant autocreated and OPV flags.
     * @return spec string
     * @throws RepositoryException on repository errors
     */
    private static String getNodeTypeSpec(NodeType type, boolean propsVariant)
            throws RepositoryException {
        String typeName = type.getName();
        StringWriter buffer = new StringWriter();

        PrintWriter writer = new PrintWriter(buffer);
        writer.println("NodeTypeName");
        writer.println("  " + typeName);
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
            writer.print(getPropertyDefSpec(properties[i], propsVariant));
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
     * @param propsVariant whether the properties of this node type may
     *   have implementation variant autocreated and OPV flags.
     * @return spec string
     * @throws RepositoryException on repository errors
     */
    private static String getPropertyDefSpec(PropertyDefinition property,
                                             boolean propsVariant)
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
        if (!propsVariant) {
            writer.println("  AutoCreated " + property.isAutoCreated());
        }
        writer.println("  Mandatory " + property.isMandatory());
        String action = OnParentVersionAction.nameFromValue(
                property.getOnParentVersion());
        if (!propsVariant) {
            writer.println("  OnParentVersion " + action);
        }
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
    private static final Comparator<NodeDefinition> NODE_DEF_COMPARATOR = new Comparator<NodeDefinition>() {
        public int compare(NodeDefinition nda, NodeDefinition ndb) {
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
    private static final Comparator<PropertyDefinition> PROPERTY_DEF_COMPARATOR = new Comparator<PropertyDefinition>() {
        public int compare(PropertyDefinition pda, PropertyDefinition pdb) {
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
    private static final Comparator<NodeType> NODE_TYPE_COMPARATOR = new Comparator<NodeType>() {
        public int compare(NodeType nta, NodeType ntb) {
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
