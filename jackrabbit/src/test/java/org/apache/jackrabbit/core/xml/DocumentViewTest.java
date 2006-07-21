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

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.core.TestRepository;
import org.apache.jackrabbit.test.JCRTestResult;
import org.apache.jackrabbit.test.LogPrintWriter;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import junit.framework.TestCase;
import junit.framework.TestResult;

/**
 * Jackrabbit-specific test cases for the document view XML format.
 *
 * @see org.apache.jackrabbit.test.api.ExportDocViewTest
 * @see org.apache.jackrabbit.test.api.DocumentViewImportTest
 */
public class DocumentViewTest extends TestCase {

    /** Logger instance for this class. */
    private static final Logger log = LoggerFactory.getLogger(DocumentViewTest.class);

    /** Test session. */
    private Session session;

    /**
     * Use a {@link org.apache.jackrabbit.test.JCRTestResult} to suppress test
     * case failures of known issues.
     *
     * @param testResult the test result.
     */
    public void run(TestResult testResult) {
        super.run(new JCRTestResult(testResult, new LogPrintWriter(log)));
    }

    /**
     * Sets up the test fixture.
     *
     * @throws Exception if an unexpected error occurs
     */
    protected void setUp() throws Exception {
        super.setUp();
        session = TestRepository.getInstance().login();
        JackrabbitNodeTypeManager manager = (JackrabbitNodeTypeManager)
            session.getWorkspace().getNodeTypeManager();
        try {
            manager.getNodeType("DocViewMultiValueTest");
        } catch (NoSuchNodeTypeException e) {
            String cnd = "[DocViewMultiValueTest] - test (boolean) multiple";
            manager.registerNodeTypes(
                    new ByteArrayInputStream(cnd.getBytes("UTF-8")),
                    JackrabbitNodeTypeManager.TEXT_X_JCR_CND);
        }
    }

    /**
     * Tears down the test fixture.
     *
     * @throws Exception if an unexpected error occurs
     */
    protected void tearDown() throws Exception {
        // TODO: Unregister the MultiValueTestType node type once Jackrabbit
        // supports node type removal.
        session.logout();
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
            session.importXML(
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

        Node root = session.getRootNode();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        Node node = root.addNode("multi-value-test", "DocViewMultiValueTest");
        node.setProperty("test", new String[] {"true", "false"});
        session.exportDocumentView("/multi-value-test", buffer, true, true);
        session.refresh(false); // Discard the transient multi-value-test node

        session.importXML(
                "/", new ByteArrayInputStream(buffer.toByteArray()),
                ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        try {
            Property property = root.getProperty("multi-value-test/test");
            assertTrue(message, property.getDefinition().isMultiple());
            assertEquals(message, property.getValues().length, 2);
            assertTrue(message, property.getValues()[0].getBoolean());
            assertFalse(message, property.getValues()[1].getBoolean());
        } catch (PathNotFoundException e) {
            fail(message);
        }
    }

}
