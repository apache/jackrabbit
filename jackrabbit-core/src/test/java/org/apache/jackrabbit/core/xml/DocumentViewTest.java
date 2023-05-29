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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Jackrabbit-specific test cases for the document view XML format.
 *
 * @see org.apache.jackrabbit.test.api.ExportDocViewTest
 * @see org.apache.jackrabbit.test.api.DocumentViewImportTest
 */
public class DocumentViewTest extends AbstractJCRTest {

    /**
     * Sets up the test fixture.
     *
     * @throws Exception if an unexpected error occurs
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        JackrabbitNodeTypeManager manager = (JackrabbitNodeTypeManager)
            superuser.getWorkspace().getNodeTypeManager();
        try {
            manager.getNodeType("DocViewMultiValueTest");
        } catch (NoSuchNodeTypeException e) {
            String cnd = "[DocViewMultiValueTest] - test (boolean) multiple";
            Reader cndReader = new InputStreamReader(new ByteArrayInputStream(cnd.getBytes("UTF-8")));
            CndImporter.registerNodeTypes(cndReader, superuser);
        }
    }

    /**
     * Tears down the test fixture.
     *
     * @throws Exception if an unexpected error occurs
     */
    @Override
    protected void tearDown() throws Exception {
        // TODO: Unregister the MultiValueTestType node type once Jackrabbit
        // supports node type removal.
        super.tearDown();
    }

    /**
     * Test case for
     * <a href="http://issues.apache.org/jira/browse/JCR-369">JCR-369</a>:
     * IllegalNameException when importing document view with two mixins.
     *
     * @throws Exception if an unexpected error occurs
     */
    public void testTwoMixins() throws Exception {
        try {
            String xml = "<two-mixins-test"
                + " jcr:mixinTypes=\"mix:referenceable mix:lockable\""
                + " xmlns:jcr=\"http://www.jcp.org/jcr/1.0\""
                + " xmlns:mix=\"http://www.jcp.org/jcr/mix/1.0\"/>";
            InputStream input = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            superuser.importXML(
                    "/", input, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        } catch (ValueFormatException e) {
            fail("JCR-369: IllegalNameException when importing document view"
                    + " with two mixins");
        }
    }

    /**
     * Test case for
     * <a href="http://issues.apache.org/jira/browse/JCR-325">JCR-325</a>:
     * docview roundtripping does not work with multivalue non-string properties
     *
     * @throws Exception if an unexpected error occurs
     */
    public void testMultiValue() throws Exception {
        String message = "JCR-325: docview roundtripping does not work with"
            + " multivalue non-string properties";

        Node root = superuser.getRootNode();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        Node node = root.addNode("multi-value-test", "DocViewMultiValueTest");
        node.setProperty("test", new String[] {"true", "false"});
        superuser.exportDocumentView("/multi-value-test", buffer, true, true);
        superuser.refresh(false); // Discard the transient multi-value-test node

        superuser.importXML(
                "/", new ByteArrayInputStream(buffer.toByteArray()),
                ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        try {
            Property property = root.getProperty("multi-value-test/test");
            assertTrue(message, property.isMultiple());
            assertEquals(message, property.getValues().length, 2);
            assertTrue(message, property.getValues()[0].getBoolean());
            assertFalse(message, property.getValues()[1].getBoolean());
        } catch (PathNotFoundException e) {
            fail(message);
        }
    }

    /**
     * Test case for
     * <a href="https://issues.apache.org/jira/browse/JCR-4935">JCR-4935</a>:
     * session.exportDocumentView() generates unparsable XML if a JCR Property contains invalid XML character
     */
    public void testInvalidXmlCharacter() throws Exception {

        Node root = superuser.getRootNode();

        Node node = root.addNode("invalid-xml-character-test", "nt:unstructured");
        node.setProperty("0x3", "\u0003");
        node.setProperty("0xB", "\u000B");
        node.setProperty("0xC", "\u000C");
        node.setProperty("0x19", "\u0019");
        node.setProperty("0xD800", "\uD800");
        node.setProperty("0xFFFE", "\uFFFE");
        node.setProperty("0xD800", "\uD800");

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        superuser.exportDocumentView("/invalid-xml-character-test", buffer, true, true);
        superuser.refresh(false);

        superuser.importXML(
                "/", new ByteArrayInputStream(buffer.toByteArray()),
                ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

        node = root.getNode("invalid-xml-character-test");
        assertEquals("", node.getProperty("0x3").getString());
        assertEquals("", node.getProperty("0xB").getString());
        assertEquals("", node.getProperty("0xC").getString());
        assertEquals("", node.getProperty("0x19").getString());
        assertEquals("", node.getProperty("0xD800").getString());
        assertEquals("", node.getProperty("0xFFFE").getString());
        assertEquals("", node.getProperty("0xD800").getString());
    }


}
