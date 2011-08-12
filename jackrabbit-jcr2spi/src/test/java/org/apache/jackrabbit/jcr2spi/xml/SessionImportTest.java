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
package org.apache.jackrabbit.jcr2spi.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * <code>SessionImportTest</code>...
 */
public class SessionImportTest extends AbstractJCRTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testImportNameValueWithUnregisteredNamespace() throws RepositoryException, SAXException {
        String prefix = getUniquePrefix(superuser);
        String uri = getUnknownURI(superuser, "anyURI");
        String testValue = prefix + ":someLocalName";

        String svuri = Name.NS_SV_URI;
        String svprefix = Name.NS_SV_PREFIX + ":";

        ContentHandler ch = superuser.getImportContentHandler(testRootNode.getPath(), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        ch.startDocument();
        ch.startPrefixMapping(prefix, uri);

        String nN = "node";
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute(svuri, "name", svprefix + "name", "CDATA", nodeName1);
        ch.startElement(svuri, nN, svprefix + nN, attrs);

        // primary node type
        String pN = "property";
        attrs = new AttributesImpl();
        attrs.addAttribute(svuri, "name", svprefix + "name", "CDATA", JcrConstants.JCR_PRIMARYTYPE);
        attrs.addAttribute(svuri, "type", svprefix + "type", "CDATA", PropertyType.nameFromValue(PropertyType.NAME));
        ch.startElement(svuri, pN, svprefix + pN, attrs);
            ch.startElement(svuri, "value", svprefix + "value", new AttributesImpl());
            char[] val = testNodeType.toCharArray();
            ch.characters(val, 0, val.length);
            ch.endElement(svuri, "value", svprefix + "value");
        ch.endElement(svuri, pN, prefix + pN);

        // another name value
        attrs = new AttributesImpl();
        attrs.addAttribute(svuri, "name", svprefix + "name", "CDATA", propertyName1);
        attrs.addAttribute(svuri, "type", svprefix + "type", "CDATA", PropertyType.nameFromValue(PropertyType.NAME));
        ch.startElement(svuri, pN, svprefix + pN, attrs);
            ch.startElement(svuri, "value", svprefix + "value", new AttributesImpl());
            val = testValue.toCharArray();
            ch.characters(val, 0, val.length);
            ch.endElement(svuri, "value", svprefix + "value");
        ch.endElement(svuri, pN, svprefix + pN);

        ch.endElement(svuri, nN, svprefix + nN);
        ch.endDocument();

        // test if property has been imported with correct namespace
        String assignedPrefix = superuser.getNamespacePrefix(uri);
        assertTrue(superuser.getNamespaceURI(assignedPrefix).equals(uri));
        String path = testRootNode.getPath() + "/" + nodeName1 + "/" + propertyName1;

        assertTrue(superuser.itemExists(path));
        Item item = superuser.getItem(path);
        if (item.isNode()) {
            fail("Item with path " + path + " must be a property.");
        } else {
            Property prop = (Property) item;
            assertTrue(prop.getValue().getType() == PropertyType.NAME);
            String expectedValue = assignedPrefix + ":someLocalName";
            assertTrue(prop.getValue().getString().equals(expectedValue));
        }

        // try to save
        superuser.save();
    }

    /**
     * Test case for issue <a href="https://issues.apache.org/jira/browse/JCR-1857">JCR-1857</href>
     *
     * @throws IOException
     * @throws RepositoryException
     */
    public void testEmptyMixins() throws IOException, RepositoryException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<sv:node xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\"\n" +
                "         xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\"\n" +
                "         xmlns:mix=\"http://www.jcp.org/jcr/mix/1.0\"\n" +
                "         xmlns:jcr=\"http://www.jcp.org/jcr/1.0\"\n" +
                "         sv:name=\"testnode1\">\n" +
                "    <sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">\n" +
                "        <sv:value>nt:unstructured</sv:value>\n" +
                "    </sv:property>\n" +
                "    <sv:property sv:name=\"jcr:title\" sv:type=\"String\">\n" +
                "        <sv:value>Test Node</sv:value>\n" +
                "    </sv:property>\n" +
                "    <sv:property sv:name=\"jcr:uuid\" sv:type=\"String\">\n" +
                "        <sv:value>1234</sv:value>\n" +
                "    </sv:property>\n" +
                "</sv:node>";

        InputStream in = new ByteArrayInputStream(xml.getBytes());
        try {
            superuser.importXML(testRootNode.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
            fail("jcr:uuid cannot be created if mix:referenceable is not part of the effective nodetype.");
        } catch (ConstraintViolationException e) {
            // ok.
        }
    }

    /**
     * Test case for issue <a href="https://issues.apache.org/jira/browse/JCR-1857">JCR-1857</href>
     *
     * @throws IOException
     * @throws RepositoryException
     */
    public void testEmptyMixins2() throws IOException, RepositoryException, NotExecutableException {
        /*
        look for a a node type that includes mix:referenceable but isn't any
        of the known internal nodetypes that ev. cannot be created through a
        session-import
        */
        String referenceableNt = null;
        NodeTypeIterator it = superuser.getWorkspace().getNodeTypeManager().getPrimaryNodeTypes();
        while (it.hasNext() && referenceableNt == null) {
            NodeType nt = it.nextNodeType();
            String ntName = nt.getName();
            if (nt.isNodeType(mixReferenceable) &&
                    !nt.isAbstract() &&
                    // TODO: improve....
                    // ignore are built-in nodetypes (mostly version related)
                    !ntName.startsWith("nt:") &&
                    // also skip all internal node types...
                    !ntName.startsWith("rep:")) {
                referenceableNt = ntName;
            }
        }
        if (referenceableNt == null) {
            throw new NotExecutableException("No primary type found that extends from mix:referenceable.");
        }
        /*
        TODO: retrieve valid jcr:uuid value from test-properties.
        */
        String uuid = UUID.randomUUID().toString();
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<sv:node xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\"\n" +
                "         xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\"\n" +
                "         xmlns:mix=\"http://www.jcp.org/jcr/mix/1.0\"\n" +
                "         xmlns:jcr=\"http://www.jcp.org/jcr/1.0\"\n" +
                "         sv:name=\"testnode1\">\n" +
                "    <sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">\n" +
                "        <sv:value>" + referenceableNt + "</sv:value>\n" +
                "    </sv:property>\n" +
                "    <sv:property sv:name=\"jcr:uuid\" sv:type=\"String\">\n" +
                "        <sv:value>" + uuid + "</sv:value>\n" +
                "    </sv:property>\n" +
                "</sv:node>";

        InputStream in = new ByteArrayInputStream(xml.getBytes());
        superuser.importXML(testRootNode.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
    }

    /**
     *
     * @throws IOException
     * @throws RepositoryException
     */
    public void testMixVersionable() throws IOException, RepositoryException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<sv:node sv:name=\"test\" xmlns:mix=\"http://www.jcp.org/jcr/mix/1.0\" " +
                "xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\" " +
                "xmlns:fn_old=\"http://www.w3.org/2004/10/xpath-functions\" " +
                "xmlns:fn=\"http://www.w3.org/2005/xpath-functions\" " +
                "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                "xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" " +
                "xmlns:rep=\"internal\" " +
                "xmlns:jcr=\"http://www.jcp.org/jcr/1.0\">" +
                "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">" +
                "   <sv:value>nt:unstructured</sv:value>" +
                "</sv:property>" +
                "<sv:property sv:name=\"jcr:mixinTypes\" sv:type=\"Name\" sv:multiple=\"true\">" +
                "   <sv:value>mix:versionable</sv:value>" +
                "</sv:property>" +
                "<sv:property sv:name=\"jcr:uuid\" sv:type=\"String\">" +
                "   <sv:value>75806b92-317f-4cb3-bc3d-ee87a95cf21f</sv:value>" +
                "</sv:property>" +
                "<sv:property sv:name=\"jcr:baseVersion\" sv:type=\"Reference\">" +
                "   <sv:value>6b91c6e5-1b83-4921-94a1-5d92ca389b3f</sv:value>" +
                "</sv:property>" +
                "<sv:property sv:name=\"jcr:isCheckedOut\" sv:type=\"Boolean\">" +
                "   <sv:value>true</sv:value>" +
                "</sv:property>" +
                "<sv:property sv:name=\"jcr:predecessors\" sv:type=\"Reference\" sv:multiple=\"true\">" +
                "   <sv:value>6b91c6e5-1b83-4921-94a1-5d92ca389b3f</sv:value>" +
                "</sv:property>" +
                "<sv:property sv:name=\"jcr:versionHistory\" sv:type=\"Reference\">" +
                "   <sv:value>99b5ec0f-49cb-4ccf-b9fd-9fba82349420</sv:value>" +
                "</sv:property>" +
                "</sv:node>";

        InputStream in = new ByteArrayInputStream(xml.getBytes());
        superuser.importXML(testRootNode.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        superuser.save();
        
        assertTrue("test node must be present", testRootNode.hasNode("test"));
        Node n = testRootNode.getNode("test");
        assertTrue("node must be mix:versionable", n.isNodeType(mixVersionable));
        assertTrue("node must be mix:referenceable", n.isNodeType(mixReferenceable));
        assertEquals("75806b92-317f-4cb3-bc3d-ee87a95cf21f", n.getUUID());
    }

    private static String getUnknownURI(Session session, String uriHint) throws RepositoryException {
        String uri = uriHint;
        int index = 0;
        List<String> uris = Arrays.asList(session.getWorkspace().getNamespaceRegistry().getURIs());
        while (uris.contains(uri)) {
            uri = uriHint + index;
            index++;
        }
        return uri;
    }

    /**
     * Returns a prefix that is unique among the already registered prefixes.
     *
     * @param session
     * @return a unique prefix
     */
    public static String getUniquePrefix(Session session) throws RepositoryException {
        return "_pre" + (session.getNamespacePrefixes().length + 1);
    }
}