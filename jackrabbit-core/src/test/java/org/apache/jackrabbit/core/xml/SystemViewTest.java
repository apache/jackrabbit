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
package org.apache.jackrabbit.core.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import junit.framework.TestResult;
import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.JCRTestResult;
import org.junit.Before;

/**
 * Jackrabbit-specific test cases for the system view XML format.
 */
public class SystemViewTest extends AbstractJCRTest {

    private static final String TEST_NODETYPES = "org/apache/jackrabbit/core/nodetype/xml/test_nodetypes.xml";

    private static final String EXPANDED_NAMES_IMPORT_TEST_ROOT = "expandedNamesImportTestRoot";
    private static final String NID_REGISTERED = "nsRegistered";
    private static final String NID_UNREGISTERED = "nsUnregistered";
    private static final String PREFIX_REGISTERED = "prefixRegistered";
    private static final String PREFIX_XML_DEFINED = "prefixXmlDefined";
    private static final String LOCAL_NODE_NAME_XML_DEFINED = "nodeXmlDefined";
    private static final String LOCAL_PROP_NAME_XML_DEFINED = "propXmlDefined";

    @Before
    private void before() throws RepositoryException {
        superuser.getWorkspace().getNamespaceRegistry().registerNamespace(PREFIX_REGISTERED, "urn:" + NID_REGISTERED);
        superuser.getWorkspace().getNamespaceRegistry().registerNamespace(PREFIX_XML_DEFINED, "urn:otherRegisteredNS");
    }

    private String createCollidingNodeXml(String id) {
        return "  <sv:node xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" sv:name=\"test:defaultTypeNode\">\n" +
            "    <sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">\n" +
            "      <sv:value>nt:unstructured</sv:value>\n" +
            "    </sv:property>\n" +
            "    <sv:property sv:name=\"jcr:uuid\" sv:type=\"String\">" +
            "      <sv:value>" + id + "</sv:value>" +
            "    </sv:property>" +
            "  </sv:node>\n";
    }

    private String createSimpleImportXml(String id) {
        return 
            "<sv:node xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" sv:name=\"testNameCollision\">\n" + 
            "  <sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">\n" + 
            "    <sv:value>test:childNodeType</sv:value>\n" + 
            "  </sv:property>\n" +
            createCollidingNodeXml(id) +
            "</sv:node>\n";
    }

    public void testSameNameErrorMessage() throws Exception {
        importTestNodeTypes();

        String xml = createSimpleImportXml("0120a4f9-196a-3f9e-b9f5-23f31f914da7");
        InputStream input = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        superuser.importXML(
            "/", input, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

        // now import same-named node with different UUID
        xml = createCollidingNodeXml("b2f5ff47-4366-31b6-a533-d8dc3614845d");
        input = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        boolean exceptionThrown = false;
        try {
            superuser.importXML(
                "/testNameCollision", input, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        } catch (ItemExistsException iee) {
            exceptionThrown = true;
            assertFalse(iee.getMessage().contains("UUID"));
        }
        assertTrue(exceptionThrown);
    }

    private void importTestNodeTypes() throws IOException, InvalidNodeTypeDefException, RepositoryException {
        // import some test node types that contain a node def with sameNameSiblings="false"
        InputStream xml = getClass().getClassLoader().getResourceAsStream(TEST_NODETYPES);
        QNodeTypeDefinition[] ntDefs = NodeTypeReader.read(xml);
        NodeTypeRegistry ntReg = ((SessionImpl) superuser).getNodeTypeManager().getNodeTypeRegistry();
        if (!ntReg.isRegistered(ntDefs[0].getName())) {
            ntReg.registerNodeTypes(Arrays.asList(ntDefs));
        }

        // make sure the node def is there as required for our test
        NodeTypeManager ntm = superuser.getWorkspace().getNodeTypeManager();
        NodeType nodeType = ntm.getNodeType("test:childNodeType");
        NodeDefinition[] childNodeDefinitions = nodeType.getChildNodeDefinitions();
        boolean foundRequiredDef = false;
        for (NodeDefinition nodeDefinition : childNodeDefinitions) {
            String nodeDefName = nodeDefinition.getName();
            if (nodeDefName.equals("test:defaultTypeNode") && !nodeDefinition.allowsSameNameSiblings()) {
                foundRequiredDef = true;
                break;
            }
        }
        assertTrue(TEST_NODETYPES + " has changed? Expected child node def 'test:defaultTypeNode' with sameNameSiblings=false", foundRequiredDef);
    }

    // The parameter testIndex is needed to avoid side effects between test cases,
    // because Jackrabbit doesn't support unregistering namespaces.
    private static String createXmlWithExpandedName(String nid, boolean definePrefix, int testIndex) {
        return "<sv:node xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" " +
                (definePrefix ? "xmlns:" + PREFIX_XML_DEFINED + testIndex + "=\"" + "urn:" + nid + testIndex  + "\"" : "")  +
                " sv:name=\"" + EXPANDED_NAMES_IMPORT_TEST_ROOT + "\">" +
                "<sv:node sv:name=\"{urn:" + nid + testIndex  + "}" + LOCAL_NODE_NAME_XML_DEFINED + "\">" +
                "<sv:property sv:type=\"Boolean\" sv:name=\"{" + "urn:" + nid + testIndex  + "}" + LOCAL_PROP_NAME_XML_DEFINED + "\">" +
                "<sv:value>true</sv:value>" +
                "</sv:property>" +
                "</sv:node>" +
                "</sv:node>";
    }

    // Expanded names in content, prefix defined in XML but not yet registered, namespace not yet registered
    // Expectation: prefix and namespace URI from XML will be used
    // OAK-9586
    public void testExpandedNameImportWithPrefixDefinition() throws Exception {
        final int testindex = 0;
        String prefix = null;
        String xml = createXmlWithExpandedName(NID_UNREGISTERED, true, testindex);
        try (InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            superuser.importXML(
                    "/", input, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
            NodeIterator nodes = superuser.getRootNode().getNode(EXPANDED_NAMES_IMPORT_TEST_ROOT).getNodes();
            assertTrue(nodes.hasNext());
            Node node = nodes.nextNode();

            prefix = superuser.getNamespacePrefix("urn:" + NID_UNREGISTERED + testindex);
            assertEquals(PREFIX_XML_DEFINED + testindex, prefix);
            String name = node.getName();
            assertEquals(PREFIX_XML_DEFINED + testindex + ":" + LOCAL_NODE_NAME_XML_DEFINED, name);
            Property p = node.getProperty("{urn:" + NID_UNREGISTERED + testindex + "}" + LOCAL_PROP_NAME_XML_DEFINED);
            assertNotNull(p);
            Property q = node.getProperty(PREFIX_XML_DEFINED + testindex + ":" + LOCAL_PROP_NAME_XML_DEFINED);
            assertNotNull(q);
            assertEquals(p.getName(), q.getName());
            assertEquals(p.getBoolean(), q.getBoolean());
        }
    }

    // Expanded names in content, prefix defined in XML but not yet registered, namespace already registered
    // Expectation: prefix and namespace URI from registry will be used
    // OAK-9586
    public void testExpandedNameImportWithPrefixDefinitionKnownNS() throws Exception {
        final int testindex = 1;
        String prefix = null;
        String xml = createXmlWithExpandedName(NID_REGISTERED, true, testindex);
        try (InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            superuser.getWorkspace().getNamespaceRegistry().registerNamespace(PREFIX_REGISTERED + testindex, "urn:" + NID_REGISTERED + testindex);
            superuser.importXML(
                    "/", input, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
            NodeIterator nodes = superuser.getRootNode().getNode(EXPANDED_NAMES_IMPORT_TEST_ROOT).getNodes();
            assertTrue(nodes.hasNext());
            Node node = nodes.nextNode();

            prefix = superuser.getNamespacePrefix("urn:" + NID_REGISTERED + testindex);
            assertEquals(PREFIX_REGISTERED + testindex, prefix);
            String name = node.getName();
            assertEquals(PREFIX_REGISTERED + testindex + ":" + LOCAL_NODE_NAME_XML_DEFINED, name);
            Property p = node.getProperty("{urn:" + NID_REGISTERED + testindex + "}" + LOCAL_PROP_NAME_XML_DEFINED);
            assertNotNull(p);
            Property q = node.getProperty(PREFIX_REGISTERED + testindex + ":" + LOCAL_PROP_NAME_XML_DEFINED);
            assertNotNull(q);
            assertEquals(p.getName(), q.getName());
            assertEquals(p.getBoolean(), q.getBoolean());
        }
    }

    // Expanded names in content, prefix defined in XML and already registered for the given namespace
    // Expectation: prefix and namespace URI from XML will be used
    // OAK-9586
    public void testExpandedNameImportWithPrefixDefinitionKnownNSAndPrefix() throws Exception {
        final int testindex = 2;
        String prefix = null;
        String xml = createXmlWithExpandedName(NID_REGISTERED, true, testindex);
        try (InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            superuser.getWorkspace().getNamespaceRegistry().registerNamespace(PREFIX_REGISTERED + testindex, "urn:" + NID_REGISTERED + testindex);
            superuser.setNamespacePrefix(PREFIX_REGISTERED + testindex, "urn:" + NID_REGISTERED + testindex);
            superuser.importXML(
                    "/", input, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
            NodeIterator nodes = superuser.getRootNode().getNode(EXPANDED_NAMES_IMPORT_TEST_ROOT).getNodes();
            assertTrue(nodes.hasNext());
            Node node = nodes.nextNode();

            prefix = superuser.getNamespacePrefix("urn:" + NID_REGISTERED + testindex);
            assertEquals(PREFIX_REGISTERED + testindex, prefix);
            String name = node.getName();
            assertEquals(PREFIX_REGISTERED + testindex + ":" + LOCAL_NODE_NAME_XML_DEFINED, name);
            Property p = node.getProperty("{urn:" + NID_REGISTERED + testindex + "}" + LOCAL_PROP_NAME_XML_DEFINED);
            assertNotNull(p);
            Property q = node.getProperty(PREFIX_REGISTERED + testindex + ":" + LOCAL_PROP_NAME_XML_DEFINED);
            assertNotNull(q);
            assertEquals(p.getName(), q.getName());
            assertEquals(p.getBoolean(), q.getBoolean());
        }
    }

    // Expanded names in content, prefix defined in XML and already registered for a different namespace, namespace not yet registered
    // Expectation: new prefix will be created for the given namespace URI
    // OAK-9586
    public void testExpandedNameImportWithCollidingPrefixDefinitionNsUnreg() throws Exception {
        final int testindex = 3;
        String otherNid = "otherRegisteredNS";
        String prefix = null;
        String xml = createXmlWithExpandedName(NID_UNREGISTERED, true, testindex);
        try (InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            superuser.getWorkspace().getNamespaceRegistry().registerNamespace(PREFIX_XML_DEFINED + testindex, "urn:" + otherNid + testindex);
            superuser.importXML(
                    "/", input, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
            NodeIterator nodes = superuser.getRootNode().getNode(EXPANDED_NAMES_IMPORT_TEST_ROOT).getNodes();
            assertTrue(nodes.hasNext());
            Node node = nodes.nextNode();

            prefix = superuser.getNamespacePrefix("urn:" + NID_UNREGISTERED + testindex);
            assertNotNull(prefix);
            String name = node.getName();
            assertEquals(prefix + ":" + LOCAL_NODE_NAME_XML_DEFINED, name);
            Property p = node.getProperty("{urn:" + NID_UNREGISTERED + testindex + "}" + LOCAL_PROP_NAME_XML_DEFINED);
            assertNotNull(p);
            Property q = node.getProperty(prefix + ":" + LOCAL_PROP_NAME_XML_DEFINED);
            assertNotNull(q);
            assertEquals(p.getName(), q.getName());
            assertEquals(p.getBoolean(), q.getBoolean());
        }
    }

    // Expanded names in content, prefix defined in XML and already registered for a different namespace, namespace already registered
    // Expectation: prefix and namespace URI from registry will be used
    // OAK-9586
    public void testExpandedNameImportWithCollidingPrefixDefinitionNsReg() throws Exception {
        final int testindex = 4;
        String otherNid = "otherRegisteredNS";
        String prefix = null;
        String xml = createXmlWithExpandedName(NID_REGISTERED, true, testindex);
        try (InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            superuser.getWorkspace().getNamespaceRegistry().registerNamespace(PREFIX_REGISTERED + testindex, "urn:" + NID_REGISTERED + testindex);
            superuser.getWorkspace().getNamespaceRegistry().registerNamespace(PREFIX_XML_DEFINED + testindex, "urn:" + otherNid + testindex);
            superuser.importXML(
                    "/", input, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
            NodeIterator nodes = superuser.getRootNode().getNode(EXPANDED_NAMES_IMPORT_TEST_ROOT).getNodes();
            assertTrue(nodes.hasNext());
            Node node = nodes.nextNode();

            prefix = superuser.getNamespacePrefix("urn:" + NID_REGISTERED + testindex);
            assertEquals(PREFIX_REGISTERED + testindex, prefix);
            String name = node.getName();
            assertEquals(PREFIX_REGISTERED + testindex + ":" + LOCAL_NODE_NAME_XML_DEFINED, name);
            Property p = node.getProperty("{urn:" + NID_REGISTERED + testindex + "}" + LOCAL_PROP_NAME_XML_DEFINED);
            assertNotNull(p);
            Property q = node.getProperty(PREFIX_REGISTERED + testindex + ":" + LOCAL_PROP_NAME_XML_DEFINED);
            assertNotNull(q);
            assertEquals(p.getName(), q.getName());
            assertEquals(p.getBoolean(), q.getBoolean());
        }
    }

    // Expanded names in content, prefix not defined in XML, namespace not yet registered
    // Expectation: new prefix will be created for the given namespace URI
    // OAK-9586
    public void testExpandedNameImportWithoutPrefixDefinitionAndUnregisteredNS() throws Exception {
        final int testindex = 5;
        String prefix = null;
        String xml = createXmlWithExpandedName(NID_UNREGISTERED, false, testindex);
        try (InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            superuser.importXML(
                    "/", input, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
            NodeIterator nodes = superuser.getRootNode().getNode(EXPANDED_NAMES_IMPORT_TEST_ROOT).getNodes();
            assertTrue(nodes.hasNext());
            Node node = nodes.nextNode();

            prefix = superuser.getNamespacePrefix("urn:" + NID_UNREGISTERED + testindex);
            assertNotNull(prefix);
            String name = node.getName();
            assertEquals(prefix + ":" + LOCAL_NODE_NAME_XML_DEFINED, name);
            Property p = node.getProperty("{urn:" + NID_UNREGISTERED + testindex + "}" + LOCAL_PROP_NAME_XML_DEFINED);
            assertNotNull(p);
            Property q = node.getProperty(prefix + ":" + LOCAL_PROP_NAME_XML_DEFINED);
            assertNotNull(q);
            assertEquals(p.getName(), q.getName());
            assertEquals(p.getBoolean(), q.getBoolean());
        }
    }

    // Expanded names in content, prefix not defined in XML, namespace already registered
    // Expectation: prefix and namespace URI from registry will be used
    // OAK-9586
    public void testExpandedNameImportWithoutPrefixDefinitionAndRegisteredNS() throws Exception {
        final int testindex = 5;
        String prefix = null;
        String xml = createXmlWithExpandedName(NID_REGISTERED, false, testindex);
        try (InputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            superuser.getWorkspace().getNamespaceRegistry().registerNamespace(PREFIX_REGISTERED + testindex, "urn:" + NID_REGISTERED + testindex);
            superuser.importXML(
                    "/", input, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
            NodeIterator nodes = superuser.getRootNode().getNode(EXPANDED_NAMES_IMPORT_TEST_ROOT).getNodes();
            assertTrue(nodes.hasNext());
            Node node = nodes.nextNode();

            prefix = superuser.getNamespacePrefix("urn:" + NID_REGISTERED + testindex);
            assertEquals(PREFIX_REGISTERED + testindex, prefix);
            String name = node.getName();
            assertEquals(prefix + ":" + LOCAL_NODE_NAME_XML_DEFINED, name);
            Property p = node.getProperty("{urn:" + NID_REGISTERED + testindex + "}" + LOCAL_PROP_NAME_XML_DEFINED);
            assertNotNull(p);
            Property q = node.getProperty(prefix + ":" + LOCAL_PROP_NAME_XML_DEFINED);
            assertNotNull(q);
            assertEquals(p.getName(), q.getName());
            assertEquals(p.getBoolean(), q.getBoolean());
        }
    }
}
