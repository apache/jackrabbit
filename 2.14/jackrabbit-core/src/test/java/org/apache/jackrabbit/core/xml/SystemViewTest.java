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
import java.util.Arrays;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Jackrabbit-specific test cases for the system view XML format.
 */
public class SystemViewTest extends AbstractJCRTest {

    private static final String TEST_NODETYPES = "org/apache/jackrabbit/core/nodetype/xml/test_nodetypes.xml";

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
}
