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
package org.apache.jackrabbit.test.api;

import org.apache.jackrabbit.test.NotExecutableException;
import org.xml.sax.SAXException;

import javax.jcr.RepositoryException;
import javax.jcr.Property;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.PropertyIterator;
import javax.jcr.NamespaceException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemExistsException;
import javax.jcr.nodetype.ConstraintViolationException;
import java.io.IOException;

/**
 * <code>DocumentViewImportTest</code> Tests importXML and
 * getImportContentHandler methods of the Workspace and Session class. Also
 * tests the UuidBehaviour flag.
 *
 * @test
 * @sources DocumentViewImportTest.java
 * @executeClass org.apache.jackrabbit.test.api.DocumentViewImportTest
 * @keywords level2
 */
public class DocumentViewImportTest extends AbstractImportXmlTest {

    private String JCR_XMLTEXT;
    private String JCR_XMLCHAR;

    private boolean withHandler;
    private boolean withWorkspace;

    public void setUp() throws Exception {
        super.setUp();
        JCR_XMLTEXT = superuser.getNamespacePrefix(NS_JCR_URI) + ":xmltext";
        JCR_XMLCHAR = superuser.getNamespacePrefix(NS_JCR_URI) + ":xmlcharacters";
    }

    public void tearDown() throws Exception {
        file.delete();
        super.tearDown();
    }

    public void testWorkspaceImportXml() throws Exception {
        withHandler = false;
        withWorkspace = WORKSPACE;
        doTestImportXML();
    }

    public void testSessionImportXml() throws Exception {
        withHandler = false;
        withWorkspace = SESSION;
        doTestImportXML();
    }

    public void testWorkspaceGetImportContentHandler() throws Exception {
        withHandler = true;
        withWorkspace = SESSION;
        doTestGetImportContentHandler();
    }

    public void testSessionGetImportContentHandler() throws Exception {
        withHandler = true;
        withWorkspace = WORKSPACE;
        doTestGetImportContentHandler();
    }

    /**
     * Tests importXML method with uuidBehaviour IMPORT_UUID_CREATE_NEW. It
     * imports the document created with createSimpleDocument method and checks
     * the imported tree according the rules outlined in chapter 7.3.2 of the
     * specification.
     * <p/>
     * Additionally it checks the uuidBehaviour flag if the jcr:uuid property is
     * respected during import.
     *
     * @throws RepositoryException
     * @throws IOException
     * @throws SAXException
     * @throws NotExecutableException
     */
    public void doTestImportXML() throws Exception {

        importXML(target, createSimpleDocument(), uuidBehaviour, withWorkspace);

        // some implementations may require a refresh to get content
        // added diretly to the workspace
        if (withWorkspace) {
            session.refresh(false);
        }

        performTests();
    }

    /**
     * Tests getImportContentHandler method with uuidBehaviour
     * IMPORT_UUID_CREATE_NEW. It imports the document created with
     * createSimpleDocument method and checks the imported tree according the
     * rules outlined in chapter 7.3.2 of the specification.
     * <p/>
     * Additionally it checks the uuidBehaviour flag if the jcr:uuid property is
     * respected during import.
     *
     * @throws RepositoryException
     * @throws SAXException
     * @throws IOException
     * @throws NotExecutableException
     */
    public void doTestGetImportContentHandler() throws Exception {

        importWithHandler(target, createSimpleDocument(), uuidBehaviour, withWorkspace);

        // some implementations may require a refresh to get content
        // added diretly to the workspace
        if (withWorkspace) {
            session.refresh(false);
        }

        performTests();
    }


    private void performTests() throws Exception {

        checkImportSimpleXMLTree();
        checkNamespaceAdded();
        if (!respectMixRef) {
            throw new NotExecutableException("ImportXML tests with " +
                    "uuidBehaviour flag not executable.");
        } else {
            checkImportDocumentView_IMPORT_UUID_CREATE_NEW();
            checkImportDocumentView_IMPORT_UUID_COLLISION_REMOVE_EXISTING();
            checkImportDocumentView_IMPORT_UUID_COLLISION_REPLACE_EXISTING();
            checkImportDocumentView_IMPORT_UUID_COLLISION_THROW();
        }
    }

    /**
     * Tests if the simple xml document defined in createSimpleDocument() is
     * imported correctly according the specification rules given in  7.3.2
     */
    public void checkImportSimpleXMLTree() throws RepositoryException, IOException {
        Node parent = (Node) session.getItem(target);

        try {
            // check the node names
            String prefix = session.getNamespacePrefix(unusedURI);
            String rootName = prefix + ":" + rootElem;
            //String rootName = rootElem;
            Node rootNode = parent.getNode(rootName);
            Node child = rootNode.getNode(childElem);
            Node xmlTextNode = rootNode.getNode(xmltextElem);
            Node grandChild = xmlTextNode.getNode(grandChildElem);

            // check xmltext
            checkXmlTextNode(xmlTextNode);

            // check the property names and values
            Property prop = grandChild.getProperty(attributeName);
            Property prop2 = xmlTextNode.getProperty(attributeName);
            String value = prop.getString();
            String value2 = prop2.getString();
            assertEquals("Value " + attributeValue + " of attribute " +
                    attributeName + " is imported to different property values.", value, value2);
            assertEquals("Value " + attributeValue + "  of attribute " +
                    attributeName + " is not correctly imported.", value, attributeValue);

            // check the encoded names and values
            Property decodedProp;
            // is decoded
            try {
                child.getNode(decodedElemName);
                decodedProp = child.getProperty(decodedAttributeName);
                String propVal = decodedProp.getString();
                // both possibilities
                if (!propVal.equals(encodedAttributeValue)
                        || !propVal.equals(encodedAttributeValue)) {
                    fail("Value " + encodedAttributeValue + "  of attribute " +
                            decodedAttributeName + " is not correctly imported.");
                }

            } catch (PathNotFoundException pnfe) {
                try {
                    // is not decoded
                    child.getNode(encodedElemName);
                    decodedProp = child.getProperty(encodedAttributeName);
                    String propVal = decodedProp.getString();
                    // both possibilities
                    if (!propVal.equals(encodedAttributeValue)
                            || !propVal.equals(encodedAttributeValue)) {
                        fail("Value " + encodedAttributeValue + "  of attribute " +
                                encodedAttributeName + " is not correctly imported.");
                    }
                } catch (PathNotFoundException pnfe2) {
                    fail("XML Element " + encodedElemName + " or attribute "
                            + encodedAttributeName + " not imported: " + pnfe2);
                }
            }
        } catch (PathNotFoundException pne) {
            fail("Element or attribute is not imported: " + pne);
        }
    }

    /**
     * Tests if xmltext in a body of a xml element is correctly imported to a
     * node with name jcr:xmltext and that the value of the text is stored in
     * the singlevalued jcr:xmlcharacters property of String type.
     *
     * @throws RepositoryException
     */
    public void checkXmlTextNode(Node node) throws RepositoryException, IOException {

        if (node.hasNode(JCR_XMLTEXT)) {
            Node xmlNode = node.getNode(JCR_XMLTEXT);
            if (xmlNode.hasProperty(JCR_XMLCHAR)) {
                Property prop = xmlNode.getProperty(JCR_XMLCHAR);
                // correct type?
                assertTrue("Property " + prop.getPath() + " is not of type String.",
                        prop.getType() == PropertyType.STRING);
                // correct text?
                // todo remove the trim as only the white spaces of the current text should be collected
                assertEquals("Xml text is not correctly stored.",
                        xmltext.trim(), prop.getString().trim());
                // only jcr:xmlcharacters property beneath the jcr:primaryType
                PropertyIterator iter = xmlNode.getProperties();
                assertTrue(JCR_XMLCHAR + " is not the only property beneath " +
                        jcrPrimaryType + " in a " + JCR_XMLTEXT + " node.", getSize(iter) == 2);
            } else {
                fail("Xmltext not stored in property named " + JCR_XMLCHAR);
            }
        } else {
            fail("Xmltext not imported to Node named " + JCR_XMLTEXT);
        }
    }

    /**
     * Checks if a namespace not yet existing in the repository is registered
     * after an according document import.
     *
     * @throws RepositoryException
     * @throws IOException
     */
    public void checkNamespaceAdded() throws RepositoryException, IOException {
        try {
            assertEquals("URI not correctly imported.", nsp.getURI(unusedPrefix), unusedURI);
            assertEquals("Prefix not correctly imported", nsp.getPrefix(unusedURI), unusedPrefix);
        } catch (NamespaceException nse) {
            fail("Namespace " + unusedPrefix + ":" + unusedURI +
                    " not imported during document view import.");
        }
    }

    //------------------< uuid collision behaviour tests >--------------------------
    // we create a node named referenced below the testRootNode, also we create a xml
    // document with an element named rootElem having the uuid of the referenced node
    // as attribute and we import this document below the refTargetNode.

    /**
     * Checks {@link ImportUUIDBehavior#IMPORT_UUID_CREATE_NEW} i.e. that a node
     * receives a new uuid when imported in any case.
     */
    public void checkImportDocumentView_IMPORT_UUID_CREATE_NEW() throws Exception {

        String uuid = createReferenceableNode(referenced);
        // import a document with a element having the same uuid as the node referenced
        importRefNodeDocument(refTarget, uuid, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, withWorkspace, withHandler);

        // different uuid?
        Node node = refTargetNode.getNode(rootElem);
        String rootElemUUID = node.getUUID();
        assertFalse("Imported node " + rootElem + " has a UUID which is " +
                "yet assigned to another node", uuid.equals(rootElemUUID));
    }

    /**
     * Checks ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING i.e that
     * the existing node is removed in case of uuid collision.
     */
    public void checkImportDocumentView_IMPORT_UUID_COLLISION_REMOVE_EXISTING()
            throws Exception {

        String uuid = createReferenceableNode(referenced);
        // import a document with a element having the same uuid as the node referenced
        importRefNodeDocument(refTarget, uuid, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING,
                withWorkspace, withHandler);

        try {
            // should be removed now
            testRootNode.getNode(referenced);
            fail("UUID behavior IMPORT_UUID_COLLISION_REMOVE_EXISTING test is failed: " +
                    "existing node not removed");
        } catch (PathNotFoundException pnfe) {
            // ok
        }
        try {
            // should be there
            refTargetNode.getNode(rootElem);
        } catch (PathNotFoundException pnfe) {
            fail("UUID behavior IMPORT_UUID_COLLISION_REMOVE_EXISTING test is failed: " +
                    "imported node not in its correct place.");
        }
    }

    /**
     * Checks ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING i.e that
     * the existing node is replaced by the imported one node when uuid
     * collision occurs.
     */
    public void checkImportDocumentView_IMPORT_UUID_COLLISION_REPLACE_EXISTING()
            throws Exception {

        String uuid = createReferenceableNode(referenced);
        // import a document with a element having the same uuid as the node referenced
        importRefNodeDocument(refTarget, uuid, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING,
                withWorkspace, withHandler);

        // should be replaced i.e should be modified, in case Workspace method is used
        //  we cannot decide unless we have some additional property of the imported node.
        if (!withWorkspace) {
            Node node = testRootNode.getNode(rootElem);
            assertTrue("Node " + node.getPath() + " not replaced during " +
                    "import with IMPORT_UUID_COLLISION_REPLACE_EXISTING", node.hasProperty(propertyName1));
        }
    }

    /**
     * Checks ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW i.e that a
     * ItemExistsException is thrown in case of importing with an input stream
     * or a SAXException is thrown in case of importing with a ContentHandler.
     *
     * @throws RepositoryException
     * @throws IOException
     */
    public void checkImportDocumentView_IMPORT_UUID_COLLISION_THROW()
            throws Exception {

        String uuid = createReferenceableNode(referenced);
        try {
            importRefNodeDocument(refTarget, uuid, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW,
                    withWorkspace, withHandler);
            fail("UUID behavior IMPORT_UUID_COLLISION_THROW test is failed: " +
                    "should throw an Exception.");
        } catch (ItemExistsException e) {
            if (!withHandler) {
                // ok
            } else {
                throw e;
            }
        } catch (SAXException e) {
            if (withHandler) {
                // ok
            } else {
                throw e;
            }
        }
    }
  //-------------------------------< exception tests >--------------------------------------
    /**
     * Tests correct failure of importing a element wit the same UUID as the target node or
     * an ancestor of it in case of uuidBehavior IMPORT_UUID_COLLISION_REMOVE_EXISTING.
     *
     * The imported document contains a element with jcr:uuid attribute the same as the
     * parent of the import target.
     */
    public void doTestSameUUIDAtAncestor(boolean withWorkspace, boolean withHandler)
            throws Exception {

        String uuid = createReferenceableNode(referenced);
        Node node = testRootNode.getNode(referenced);
        Node node2 = node.addNode("newParent");
        session.save();
        // import a document with a element having the same uuid as the node referenced
        try {
            importRefNodeDocument(node2.getPath(), uuid,
                ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING,
                    withWorkspace, withHandler);
            fail("UUID collision with an ancestor of the target node hould throw a " +
                    "SAXException or a ConstraintViolationException in case of " +
                    "uuidBehavior IMPORT_UUID_COLLISION_REMOVE_EXISTING.");
        } catch(SAXException se) {
            if (!withHandler) {
                throw se;
            }
            // ok
        } catch (ConstraintViolationException cve) {
            if (withHandler) {
                throw cve;
            }
            // ok
        }
    }

    public void testSameUUIDAtAncestorWorkspaceHandler() throws Exception {
        doTestSameUUIDAtAncestor(WORKSPACE, CONTENTHANDLER);
    }

    public void testSameUUIDAtAncestorWorkspace() throws Exception {
        doTestSameUUIDAtAncestor(WORKSPACE, STREAM);
    }

    public void testSameUUIDAtAncestorSessionHandler() throws Exception  {
        doTestSameUUIDAtAncestor(SESSION, CONTENTHANDLER);
    }

    public void testSameUUIDAtAncestorSession() throws Exception {
        doTestSameUUIDAtAncestor(SESSION, STREAM);
    }
}