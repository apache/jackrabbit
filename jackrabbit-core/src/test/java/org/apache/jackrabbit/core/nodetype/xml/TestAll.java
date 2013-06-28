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
package org.apache.jackrabbit.core.nodetype.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.version.OnParentVersionAction;

import junit.framework.AssertionFailedError;

import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.value.InternalValueFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.value.ValueFactoryQImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Test cases for reading and writing node type definition files.
 */
public class TestAll extends AbstractJCRTest {

    /** The dummy test namespace. */
    private static final String TEST_NAMESPACE = "http://www.apache.org/jackrabbit/test";

    /** Name of the include test node type definition file. */
    private static final String TEST_NODETYPES =
        "org/apache/jackrabbit/core/nodetype/xml/test_nodetypes.xml";

    /** Name of the xml nodetype file for import and namespace registration. */
    private static final String TEST_NS_XML_NODETYPES =
        "test_ns_xml_nodetypes.xml";

    /** Name of the cnd nodetype file for import and namespace registration. */
    private static final String TEST_NS_CND_NODETYPES =
        "test_ns_cnd_nodetypes.cnd";

    /** Name of the xml nodetype file with same node type name definitions. */
    private static final String TEST_SAME_NT_NAME_XML_NODETYPES =
        "test_same_nt_name_xml_nodetypes.xml";

    /** Name of the cnd nodetype file with same node type name definitions. */
    private static final String TEST_SAME_NT_NAME_CND_NODETYPES =
        "test_same_nt_name_cnd_nodetypes.cnd";

    private static final NameFactory FACTORY = NameFactoryImpl.getInstance();

    /** Test node types definitions. */
    private QNodeTypeDefinition[] types;

    /** Registry for the test namespaces. */
    private NamespaceRegistry registry;

    /**
     * Initializes the node type formatter tests.
     *
     * @throws Exception on initialization errors
     */
    protected void setUp() throws Exception {
        super.setUp();
        InputStream xml =
            getClass().getClassLoader().getResourceAsStream(TEST_NODETYPES);

        types = NodeTypeReader.read(xml);

        registry = new SimpleNamespaceRegistry();
        registry.registerNamespace("test", TEST_NAMESPACE);
    }

    /**
     * Returns the named node type definition. If the node type does not
     * exist, an assertion failure is generated.
     *
     * @param name node type name
     * @return node type definition
     */
    private QNodeTypeDefinition getNodeType(String name) {
        Name qname = FACTORY.create(TEST_NAMESPACE, name);
        for (int i = 0; i < types.length; i++) {
            if (qname.equals(types[i].getName())) {
                return types[i];
            }
        }
        throw new AssertionFailedError("Node type " + name + " does not exist");
    }

    /**
     * Returns the named property definition from the named node type
     * definition. If either of the definitions do not exist, an assertion
     * failure is generated.
     * <p>
     * If the given property name is <code>null</code>, then the residual
     * property definition (if one exists) is returned.
     *
     * @param typeName node type name
     * @param propertyName property name, or <code>null</code>
     * @return property definition
     */
    private QPropertyDefinition getPropDef(String typeName, String propertyName) {
        Name name;
        if (propertyName != null) {
            name = FACTORY.create(TEST_NAMESPACE, propertyName);
        } else {
            name = NameConstants.ANY_NAME;
        }

        QNodeTypeDefinition def = getNodeType(typeName);
        QPropertyDefinition[] defs = def.getPropertyDefs();
        for (int i = 0; i < defs.length; i++) {
            if (name.equals(defs[i].getName())) {
                return defs[i];
            }
        }

        throw new AssertionFailedError(
                "Property " + propertyName + " does not exist");
    }

    /**
     * Returns the string value of the identified property default value.
     *
     * @param def property definition
     * @param index default value index
     * @return default value
     */
    private String getDefaultValue(QPropertyDefinition def, int index) {
        try {
            QValue[] values = def.getDefaultValues();
            NamespaceResolver nsResolver = new AdditionalNamespaceResolver(registry);
            NamePathResolver resolver = new DefaultNamePathResolver(nsResolver);
            ValueFactoryQImpl factory = new ValueFactoryQImpl(InternalValueFactory.getInstance(), resolver);
            return factory.createValue(values[index]).getString();
        } catch (RepositoryException e) {
            throw new AssertionFailedError(e.getMessage());
        }
    }

    /**
     * Returns the named child node definition from the named node type
     * definition. If either of the definitions do not exist, an assertion
     * failure is generated.
     *
     * @param typeName node type name
     * @param nodeName child node name
     * @return child node definition
     */
    private QNodeDefinition getChildNode(String typeName, String nodeName) {
        Name name = FACTORY.create(TEST_NAMESPACE, nodeName);

        QNodeTypeDefinition def = getNodeType(typeName);
        QNodeDefinition[] defs = def.getChildNodeDefs();
        for (int i = 0; i < defs.length; i++) {
            if (name.equals(defs[i].getName())) {
                return defs[i];
            }
        }

        throw new AssertionFailedError(
                "Child node " + nodeName + " does not exist");
    }

    /**
     * Test for reading a node type definition file. The test file
     * has already been read during the test setup, so this method
     * just verifies the number of node types.
     */
    public void testRead() {
        assertEquals("number of node types", 6, types.length);
    }

    /** Test for the empty node type. */
    public void testEmptyNodeType() {
        QNodeTypeDefinition def = getNodeType("emptyNodeType");
        assertNotNull("emptyNodeType exists", def);
        assertEquals("emptyNodeType mixin",
                false, def.isMixin());
        assertEquals("emptyNodeType hasOrderableChildNodes",
                false, def.hasOrderableChildNodes());
        assertEquals("emptyNodeType primaryItemName",
                null, def.getPrimaryItemName());
        assertEquals("emptyNodeType childNodeDefs",
                0, def.getChildNodeDefs().length);
        assertEquals("emptyNodeType propertyDefs",
                0, def.getPropertyDefs().length);
    }

    /** Test for the <code>mixin</code> node type attribute. */
    public void testMixinNodeType() {
        QNodeTypeDefinition def = getNodeType("mixinNodeType");
        assertEquals("mixinNodeType mixin",
                true, def.isMixin());
    }

    /** Test for the <code>hasOrderableChildNodes</code> node type attribute. */
    public void testOrderedNodeType() {
        QNodeTypeDefinition def = getNodeType("orderedNodeType");
        assertEquals("orderedNodeType hasOrderableChildNodes",
                true, def.hasOrderableChildNodes());
    }

    /** Test for node type item definitions. */
    public void testItemNodeType() {
        QNodeTypeDefinition def = getNodeType("itemNodeType");
        assertEquals("itemNodeType primaryItemName",
                FACTORY.create(TEST_NAMESPACE, "emptyItem"),
                def.getPrimaryItemName());
        assertEquals("itemNodeType propertyDefs",
                10, def.getPropertyDefs().length);
        QPropertyDefinition pdef = getPropDef("itemNodeType", null);
        assertTrue("itemNodeType wildcard property", pdef.definesResidual());
    }

    /** Test for namespace registration on node type import. */
    public void testImportXMLNodeTypes() throws Exception {
        try {
            superuser.getNamespacePrefix("http://ns.example.org/test-namespace2");
            // Ignore test case, node type and namespace already registered
        } catch (NamespaceException e1) {
            // Namespace testns2 not yet registered
            JackrabbitNodeTypeManager ntm = (JackrabbitNodeTypeManager)
                superuser.getWorkspace().getNodeTypeManager();
            ntm.registerNodeTypes(
                    TestAll.class.getResourceAsStream(TEST_NS_XML_NODETYPES),
                    JackrabbitNodeTypeManager.TEXT_XML);
            try {
                superuser.getNamespacePrefix("http://ns.example.org/test-namespace2");
            } catch (NamespaceException e2) {
                fail("xml test2 namespace not registered");
            }
        }
    }

    /** Test for same node type name on node type import. */
    public void testInvalidXMLNodeTypes() throws Exception {
        JackrabbitNodeTypeManager ntm = (JackrabbitNodeTypeManager)
            superuser.getWorkspace().getNodeTypeManager();
        try {
            ntm.registerNodeTypes(
                TestAll.class.getResourceAsStream(TEST_SAME_NT_NAME_XML_NODETYPES),
                JackrabbitNodeTypeManager.TEXT_XML);
            fail("Importing multiple node types with the same name must fail");
        } catch (RepositoryException e) {
            if (e.getCause() instanceof InvalidNodeTypeDefException) {
               // Expected
            } else {
               throw e;
            }
        }
    }

    /** Test for namespace registration on node type import. */
    public void testImportCNDNodeTypes() throws Exception {
        try {
            superuser.getNamespacePrefix("http://ns.example.org/test-namespace3");
            // Ignore test case, node type and namespace already registered
        } catch (NamespaceException e1) {
            Reader cnd = new InputStreamReader(TestAll.class.getResourceAsStream(TEST_NS_CND_NODETYPES));
            CndImporter.registerNodeTypes(cnd, superuser);
            cnd.close();
            try {
                superuser.getNamespacePrefix("http://ns.example.org/test-namespace3");
            } catch (NamespaceException e2) {
                fail("cnd test3 namespace not registered");
            }
        }
    }

    /** Test for same node type name on node type import. */
    public void testInvalidCNDNodeTypes() throws Exception {
        JackrabbitNodeTypeManager ntm = (JackrabbitNodeTypeManager)
            superuser.getWorkspace().getNodeTypeManager();
        try {
            ntm.registerNodeTypes(
                TestAll.class.getResourceAsStream(TEST_SAME_NT_NAME_CND_NODETYPES),
                JackrabbitNodeTypeManager.TEXT_X_JCR_CND);
            fail("Importing multiple node types with the same name must fail");
        } catch (RepositoryException e) {
            if (e.getCause() instanceof InvalidNodeTypeDefException) {
               // Expected
            } else {
               throw e;
            }
        }
    }

    /** Test for the empty item definition. */
    public void testEmptyItem() {
        QPropertyDefinition def = getPropDef("itemNodeType", "emptyItem");
        assertEquals("emptyItem autoCreate",
                false, def.isAutoCreated());
        assertEquals("emptyItem mandatory",
                false, def.isMandatory());
        assertEquals("emptyItem onParentVersion",
                OnParentVersionAction.IGNORE, def.getOnParentVersion());
        assertEquals("emptyItem protected",
                false, def.isProtected());
    }

    /** Test for the <code>autoCreated</code> item definition attribute. */
    public void testAutoCreateItem() {
        QPropertyDefinition def = getPropDef("itemNodeType", "autoCreatedItem");
        assertEquals("autoCreatedItem autoCreated",
                true, def.isAutoCreated());
    }

    /** Test for the <code>mandatory</code> item definition attribute. */
    public void testMandatoryItem() {
        QPropertyDefinition def = getPropDef("itemNodeType", "mandatoryItem");
        assertEquals("mandatoryItem mandatory",
                true, def.isMandatory());
    }

    /** Test for the <code>copy</code> parent version action. */
    public void testCopyItem() {
        QPropertyDefinition def = getPropDef("itemNodeType", "copyItem");
        assertEquals("copyItem onParentVersion",
                OnParentVersionAction.COPY, def.getOnParentVersion());
    }

    /** Test for the <code>version</code> parent version action. */
    public void testVersionItem() {
        QPropertyDefinition def = getPropDef("itemNodeType", "versionItem");
        assertEquals("versionItem onParentVersion",
                OnParentVersionAction.VERSION, def.getOnParentVersion());
    }

    /** Test for the <code>initialize</code> parent version action. */
    public void testInitializeItem() {
        QPropertyDefinition def = getPropDef("itemNodeType", "initializeItem");
        assertEquals("initializeItem onParentVersion",
                OnParentVersionAction.INITIALIZE, def.getOnParentVersion());
    }

    /** Test for the <code>compute</code> parent version action. */
    public void testComputeItem() {
        QPropertyDefinition def = getPropDef("itemNodeType", "computeItem");
        assertEquals("computeItem onParentVersion",
                OnParentVersionAction.COMPUTE, def.getOnParentVersion());
    }

    /** Test for the <code>abort</code> parent version action. */
    public void testAbortItem() {
        QPropertyDefinition def = getPropDef("itemNodeType", "abortItem");
        assertEquals("abortItem onParentVersion",
                OnParentVersionAction.ABORT, def.getOnParentVersion());
    }

    /** Test for the <code>protected</code> item definition attribute. */
    public void testProtectedItem() {
        QPropertyDefinition def = getPropDef("itemNodeType", "protectedItem");
        assertEquals("protectedItem protected",
                true, def.isProtected());
    }

    /** Test for node type property definitions. */
    public void testPropertyNodeType() {
        QNodeTypeDefinition def = getNodeType("propertyNodeType");
        assertEquals("propertyNodeType propertyDefs",
                13, def.getPropertyDefs().length);
    }

    /** Test for the empty property definition. */
    public void testEmptyProperty() {
        QPropertyDefinition def = getPropDef("propertyNodeType", "emptyProperty");
        assertEquals("emptyProperty requiredType",
                PropertyType.UNDEFINED, def.getRequiredType());
        assertEquals("emptyProperty multiple",
                false, def.isMultiple());
        assertNull("emptyProperty defaultValues",
                def.getDefaultValues());
        assertEquals("emptyProperty valueConstraints",
                0, def.getValueConstraints().length);
    }

    /** Test for the <code>binary</code> property definition type. */
    public void testBinaryProperty() {
        QPropertyDefinition def = getPropDef("propertyNodeType", "binaryProperty");
        assertEquals("binaryProperty requiredType",
                PropertyType.BINARY, def.getRequiredType());
        assertEquals("binaryProperty valueConstraints",
                1, def.getValueConstraints().length);
        assertEquals("binaryProperty valueConstraints[0]",
                "[0,)", (def.getValueConstraints())[0].getString());
        assertNull("binaryProperty defaultValues",
                def.getDefaultValues());
    }

    /** Test for the <code>boolean</code> property definition type. */
    public void testBooleanProperty() {
        QPropertyDefinition def = getPropDef("propertyNodeType", "booleanProperty");
        assertEquals("booleanProperty requiredType",
                PropertyType.BOOLEAN, def.getRequiredType());
        assertEquals("booleanProperty valueConstraints",
                2, def.getValueConstraints().length);
        assertEquals("booleanProperty valueConstraints[0]",
                "true", (def.getValueConstraints())[0].getString());
        assertEquals("booleanProperty valueConstraints[1]",
                "false", (def.getValueConstraints())[1].getString());
        assertEquals("booleanProperty defaultValues",
                1, def.getDefaultValues().length);
        assertEquals("booleanProperty defaultValues[0]",
                "true", getDefaultValue(def, 0));
    }

    /** Test for the <code>date</code> property definition type. */
    public void testDateProperty() {
        QPropertyDefinition def = getPropDef("propertyNodeType", "dateProperty");
        assertEquals("dateProperty requiredType",
                PropertyType.DATE, def.getRequiredType());
        assertEquals("dateProperty valueConstraints",
                1, def.getValueConstraints().length);
        assertEquals("dateProperty valueConstraints[0]",
                "[2005-01-01T00:00:00.000Z,2006-01-01T00:00:00.000Z)",
                (def.getValueConstraints())[0].getString());
        assertEquals("dateProperty defaultValues",
                1, def.getDefaultValues().length);
        assertEquals("dateProperty defaultValues[0]",
                "2005-01-01T00:00:00.000Z", getDefaultValue(def, 0));
    }

    /** Test for the <code>double</code> property definition type. */
    public void testDoubleProperty() {
        QPropertyDefinition def = getPropDef("propertyNodeType", "doubleProperty");
        assertEquals("doubleProperty requiredType",
                PropertyType.DOUBLE, def.getRequiredType());
        assertEquals("doubleProperty valueConstraints",
                3, def.getValueConstraints().length);
        assertEquals("doubleProperty valueConstraints[0]",
                "[,0.0)", (def.getValueConstraints())[0].getString());
        assertEquals("doubleProperty valueConstraints[1]",
                "(1.0,2.0)", (def.getValueConstraints())[1].getString());
        assertEquals("doubleProperty valueConstraints[2]",
                "(3.0,]", (def.getValueConstraints())[2].getString());
        assertEquals("doubleProperty defaultValues",
                1, def.getDefaultValues().length);
        assertEquals("doubleProperty defaultValues[0]",
                "1.5", getDefaultValue(def, 0));
    }

    /** Test for the <code>long</code> property definition type. */
    public void testLongProperty() {
        QPropertyDefinition def = getPropDef("propertyNodeType", "longProperty");
        assertEquals("longProperty requiredType",
                PropertyType.LONG, def.getRequiredType());
        assertEquals("longProperty valueConstraints",
                3, def.getValueConstraints().length);
        assertEquals("longProperty valueConstraints[0]",
                "(-10,0]", (def.getValueConstraints())[0].getString());
        assertEquals("longProperty valueConstraints[1]",
                "[1,2]", (def.getValueConstraints())[1].getString());
        assertEquals("longProperty valueConstraints[2]",
                "[10,100)", (def.getValueConstraints())[2].getString());
        assertEquals("longProperty defaultValues",
                1, def.getDefaultValues().length);
        assertEquals("longProperty defaultValues[0]",
                "25", getDefaultValue(def, 0));
    }

    /** Test for the <code>name</code> property definition type. */
    public void testNameProperty() {
        QPropertyDefinition def = getPropDef("propertyNodeType", "nameProperty");
        assertEquals("nameProperty requiredType",
                PropertyType.NAME, def.getRequiredType());
        assertEquals("nameProperty valueConstraints",
                1, def.getValueConstraints().length);
        assertEquals("nameProperty valueConstraints[0]",
                "{http://www.apache.org/jackrabbit/test}testName",
                (def.getValueConstraints())[0].getString());
        assertEquals("nameProperty defaultValues",
                1, def.getDefaultValues().length);
        assertEquals("nameProperty defaultValues[0]",
                "test:testName", getDefaultValue(def, 0));
    }

    /** Test for the <code>path</code> property definition type. */
    public void testPathProperty() {
        QPropertyDefinition def = getPropDef("propertyNodeType", "pathProperty");
        assertEquals("pathProperty requiredType",
                PropertyType.PATH, def.getRequiredType());
        assertEquals("pathProperty valueConstraints",
                1, def.getValueConstraints().length);
        assertEquals("pathProperty valueConstraints[0]",
                "{}\t{http://www.apache.org/jackrabbit/test}testPath",
                (def.getValueConstraints())[0].getString());
        assertNull("pathProperty defaultValues",
                def.getDefaultValues());
    }

    /** Test for the <code>path</code> property definition type. */
    public void testPathProperty1() {
        QPropertyDefinition def = getPropDef("propertyNodeType", "pathProperty1");
        assertEquals("pathProperty requiredType",
                PropertyType.PATH, def.getRequiredType());
        assertEquals("pathProperty valueConstraints",
                1, def.getValueConstraints().length);
        assertEquals("pathProperty valueConstraints[0]",
                "{}\t{http://www.apache.org/jackrabbit/test}testPath\t{}*",
                (def.getValueConstraints())[0].getString());
        assertNull("pathProperty defaultValues",
                def.getDefaultValues());
    }

    /** Test for the <code>path</code> property definition type. */
    public void testPathProperty2() {
        QPropertyDefinition def = getPropDef("propertyNodeType", "pathProperty2");
        assertEquals("pathProperty requiredType",
                PropertyType.PATH, def.getRequiredType());
        assertEquals("pathProperty valueConstraints",
                1, def.getValueConstraints().length);
        assertEquals("pathProperty valueConstraints[0]",
                "{http://www.apache.org/jackrabbit/test}testPath\t{}*",
                (def.getValueConstraints())[0].getString());
        assertNull("pathProperty defaultValues",
                def.getDefaultValues());
    }

    /** Test for the <code>reference</code> property definition type. */
    public void testReferenceProperty() {
        QPropertyDefinition def = getPropDef("propertyNodeType", "referenceProperty");
        assertEquals("referenceProperty requiredType",
                PropertyType.REFERENCE, def.getRequiredType());
        assertEquals("referenceProperty valueConstraints",
                1, def.getValueConstraints().length);
        assertEquals("referenceProperty valueConstraints[0]",
                "{http://www.jcp.org/jcr/nt/1.0}base",
                (def.getValueConstraints())[0].getString());
        assertNull("referenceProperty defaultValues",
                def.getDefaultValues());
    }

    /** Test for the <code>string</code> property definition type. */
    public void testStringProperty() {
        QPropertyDefinition def = getPropDef("propertyNodeType", "stringProperty");
        assertEquals("stringProperty requiredType",
                PropertyType.STRING, def.getRequiredType());
        assertEquals("stringProperty valueConstraints",
                1, def.getValueConstraints().length);
        assertEquals("stringProperty valueConstraints[0]",
                "bananas?",
                (def.getValueConstraints())[0].getString());
        assertEquals("stringProperty defaultValues",
                2, def.getDefaultValues().length);
        assertEquals("stringProperty defaultValues[0]",
                "banana", getDefaultValue(def, 0));
        assertEquals("stringProperty defaultValues[1]",
                "bananas", getDefaultValue(def, 1));
    }

    /** Test for the <code>multiple</code> property definition attribute. */
    public void testMultipleProperty() {
        QPropertyDefinition def = getPropDef("propertyNodeType", "multipleProperty");
        assertEquals("multipleProperty multiple",
                true, def.isMultiple());
    }

    /** Test for node type child node definitions. */
    public void testChildNodeType() {
        QNodeTypeDefinition def = getNodeType("childNodeType");
        assertEquals("childNodeType childNodeDefs",
                4, def.getChildNodeDefs().length);
    }

    /** Test for the empty child node definition. */
    public void testEmptyNode() {
        QNodeDefinition def = getChildNode("childNodeType", "emptyNode");
        assertEquals("emptyNode allowsSameNameSiblings",
                false, def.allowsSameNameSiblings());
        assertEquals("emptyNode defaultPrimaryType",
                null, def.getDefaultPrimaryType());
    }

    /** Test for the <code>allowsSameNameSiblings</code> child node attribute. */
    public void testSiblingNode() {
        QNodeDefinition def = getChildNode("childNodeType", "siblingNode");
        assertEquals("siblingNode allowsSameNameSiblings",
                true, def.allowsSameNameSiblings());
    }

    /** Test for the <code>defaultPrimaryType</code> child node attribute. */
    public void testDefaultTypeNode() {
        QNodeDefinition def = getChildNode("childNodeType", "defaultTypeNode");
        assertEquals("defaultTypeNode defaultPrimaryType",
                FACTORY.create(Name.NS_NT_URI, "base"),
                def.getDefaultPrimaryType());
    }

    /** Test for the <code>requiredPrimaryTypes</code> child node attributes. */
    public void testRequiredTypeNode() {
        QNodeDefinition def = getChildNode("childNodeType", "requiredTypeNode");
        assertEquals("requiredTypeNode requiredPrimaryTypes",
                2, def.getRequiredPrimaryTypes().length);
        Name[] types = def.getRequiredPrimaryTypes();
        Arrays.sort(types);
        assertEquals("requiredTypeNode requiredPrimaryTypes[0]",
                FACTORY.create(Name.NS_NT_URI, "base"), types[0]);
        assertEquals("requiredTypeNode requiredPrimaryTypes[1]",
                FACTORY.create(Name.NS_NT_URI, "unstructured"), types[1]);
    }

    /**
     * Test for writing a node type definition file. Writing is tested
     * by writing and re-reading the test node types using an internal
     * byte array. The resulting node type map is then compared to the
     * original test node types.
     *
     * @throws IOException on IO errors
     * @throws RepositoryException on repository errors
     */
    public void testWrite() throws IOException, RepositoryException {
        try {
            ByteArrayOutputStream xml = new ByteArrayOutputStream();
            NodeTypeWriter.write(xml, types, registry);
            byte[] bytes = xml.toByteArray();
            QNodeTypeDefinition[] output =
                NodeTypeReader.read(new ByteArrayInputStream(bytes));
            assertTrue("write output", Arrays.equals(types, output));
        } catch (InvalidNodeTypeDefException e) {
            fail(e.getMessage());
        }
    }

}
