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

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Test cases for the {@link WorkspaceImporter} class.
 */
public class WorkspaceImporterTest extends AbstractJCRTest {

    private Node root;

    protected void setUp() throws Exception {
        super.setUp();
        root = superuser.getRootNode().addNode("WorkspaceImporterTest");
        superuser.save();
    }

    protected void tearDown() throws Exception {
        root.remove();
        superuser.save();
        super.tearDown();
    }

    /**
     * Tests that an XML document with an internal reference is correctly
     * imported. This functionality got broken by JCR-569.
     *
     * @throws Exception if an unexpected error occurs
     */
    public void testReferenceImport() throws Exception {
        try {
            NodeId id = NodeId.randomId();
            String xml =
                "<sv:node sv:name=\"a\""
                + " xmlns:jcr=\"http://www.jcp.org/jcr/1.0\""
                + " xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\""
                + " xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\">"
                + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">"
                + "<sv:value>nt:unstructured</sv:value></sv:property>"
                + "<sv:node sv:name=\"b\">"
                + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">"
                + "<sv:value>nt:unstructured</sv:value></sv:property>"
                + "<sv:property sv:name=\"jcr:mixinTypes\" sv:type=\"Name\">"
                + "<sv:value>mix:referenceable</sv:value></sv:property>"
                + "<sv:property sv:name=\"jcr:uuid\" sv:type=\"String\">"
                + "<sv:value>" + id + "</sv:value></sv:property>"
                + "</sv:node>"
                + "<sv:node sv:name=\"c\">"
                + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">"
                + "<sv:value>nt:unstructured</sv:value></sv:property>"
                + "<sv:property sv:name=\"ref\" sv:type=\"Reference\">"
                + "<sv:value>" + id + "</sv:value></sv:property>"
                + "</sv:node>"
                + "</sv:node>";
            superuser.getWorkspace().importXML(
                    root.getPath(),
                    new ByteArrayInputStream(xml.getBytes("UTF-8")),
                    ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);

            Node b = root.getNode("a/b");
            Node c = root.getNode("a/c");
            assertTrue(
                    "Imported reference points to the correct node",
                    b.isSame(c.getProperty("ref").getNode()));
        } catch (PathNotFoundException e) {
            fail("Imported node or property not found");
        } catch (RepositoryException e) {
            fail("Failed to import an XML document with an internal reference");
        }
    }

}
