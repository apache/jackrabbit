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

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Attr;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.RepositoryException;
import javax.jcr.PathNotFoundException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

/**
 * <code>AbstractImportXmlTest</code> Provides names, data and methods to create
 * xml documents and referenceable nodes for the tests of document view import
 * methods of Workspace and Session.
 */
abstract class AbstractImportXmlTest extends AbstractJCRTest {

    protected final boolean WORKSPACE = true;
    protected final boolean SESSION = false;
    protected boolean CONTENTHANDLER = true;
    protected boolean STREAM = false;

    // the absolute path to the target nodes for the imports
    protected String target;
    protected String refTarget;

    protected Session session;
    protected Workspace workspace;
    protected NodeTypeManager ntManager;
    protected NamespaceRegistry nsp;

    protected String ntUnstructured;

    protected File file;

    // some node names
    protected String referenced;
    protected String referencing;

    // the target nodes
    protected Node targetNode;
    protected Node refTargetNode;

    protected String unusedPrefix;
    protected String unusedURI;

    // names for namespace import
    protected final String TEST_PREFIX = "docview";
    protected final String TEST_URI = "www.apache.org/jackrabbit/test/namespaceImportTest";
    protected final String XML_NS = "xmlns";

    // xml document related names
    protected static final String rootElem = "docRoot";
    protected static final String refNodeElem = "refNodeElem";
    protected static final String xmltextElem = "xmltextElem";
    protected static final String childElem = "childElem";
    protected static final String grandChildElem = "grandChildElem";

    protected static final String encodedElemName = "Element_x003C__x003E_Name";
    protected static final String decodedElemName = "Element<>Name";

    protected static final String attributeName = "attribute";
    protected static final String attributeValue = "attrVal";

    protected static final String encodedAttributeName = "Prop_x0020_Name";
    protected static final String decodedAttributeName = "Prop Name";
    protected static final String encodedAttributeValue = "Hello_x0009_&_x0009_GoodBye";
    protected static final String decodedAttributeValue = "Hello\\t&\\tGoodBye";

    //String value for the test with leading and trailing spaces and entity reference charachters
    protected String xmltext = "\\t Text for docView Export test_x0009_with escaped _x003C_ characters.  ";

    // is semantic of mix:referenceable respected?
    protected boolean respectMixRef = false;

    // default uuidBehaviour for the tests
    protected int uuidBehaviour = ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;

    protected DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    protected DocumentBuilder dom;

    /**
     * Sets up the fixture for the test cases.
     */
    public void setUp() throws Exception {
        super.setUp();

        try {
            dom = factory.newDocumentBuilder();
            file = File.createTempFile("docViewImportTest", ".xml");
            log.print("Tempfile: " + file.getAbsolutePath());
            session = superuser;
            workspace = session.getWorkspace();
            // create the target nodes for the imports
            target = testRoot + "/target";
            targetNode = createAncestors(target);
            refTarget = testRoot + "/refTarget";
            refTargetNode = createAncestors(refTarget);
  
            nsp = workspace.getNamespaceRegistry();
            ntManager = workspace.getNodeTypeManager();
  
            // construct a namespace not existing in the repository
            unusedPrefix = getUnusedPrefix();
            unusedURI = getUnusedURI();
            referenced = nodeName1;
            referencing = nodeName2;
            // test if jcr:uuid of mix:referenceable node type is respected
            respectMixRef = isMixRefRespected();
        }
        catch (Exception ex) {
            if (file != null) {
                file.delete();
                file = null;
            }
            throw (ex);
        }
    }

    public void tearDown() throws Exception {
        if (file != null) {
            file.delete();
            file = null;
        }
        session = null;
        workspace = null;
        ntManager = null;
        nsp = null;
        targetNode = null;
        refTargetNode = null;
        super.tearDown();
    }

    /**
     * Creates a document with some nodes and props for Namespace adding test
     * and for correct tree structure tests after having imported.
     *
     * @return
     */
    public Document createSimpleDocument() {
        Document doc = dom.newDocument();
        Element root = doc.createElementNS(unusedURI, unusedPrefix + ":" + rootElem);
        root.setAttribute(XML_NS + ":" + unusedPrefix, unusedURI);
        Element child = doc.createElement(childElem);
        Element xmlElem = doc.createElement(xmltextElem);
        Element encoded = doc.createElement(encodedElemName);
        Element grandchild = doc.createElement(grandChildElem);

        Attr attr = doc.createAttribute(attributeName);
        attr.setValue(attributeValue);
        Attr encodedAttr = doc.createAttribute(encodedAttributeName);
        encodedAttr.setValue(encodedAttributeValue);

        child.appendChild(encoded);
        child.setAttributeNode(encodedAttr);

        grandchild.setAttributeNode(attr);

        xmlElem.appendChild(doc.createTextNode(xmltext));
        xmlElem.appendChild(grandchild);
        xmlElem.setAttribute(attributeName, attributeValue);

        root.appendChild(child);
        root.appendChild(xmlElem);

        doc.appendChild(root);
        return doc;
    }

    /**
     * Imports a given document using either Workspace.importXML or
     * Session.importXML method.
     *
     * @param absPath       the absPath to the parent node where to import the
     *                      document
     * @param document      the document to import
     * @param uuidBehaviour how the uuid collisions should be handled
     * @param withWorkspace if workspace or session interface should be used
     * @throws RepositoryException
     * @throws IOException
     */
    protected void importXML(String absPath, Document document,
                             int uuidBehaviour, boolean withWorkspace)
            throws RepositoryException, IOException {
        serialize(document);
        BufferedInputStream bin = new BufferedInputStream(new FileInputStream(file));
        try {
            if (withWorkspace) {
                workspace.importXML(absPath, bin, uuidBehaviour);
            } else {
                session.importXML(absPath, bin, uuidBehaviour);
                session.save();
            }
        } finally {
            bin.close();
        }
    }

    /**
     * Imports a given document using the ContentHandler received either with
     * Workspace.getImportContentHandler or Session.getImportContentHandler.
     * This handler is then passed to a XML parser which parses the given
     * document.
     *
     * @param absPath       the absPath to the parent node where to import the
     *                      document
     * @param document      the document to import
     * @param uuidBehaviour how the uuid collisions should be handled
     * @param withWorkspace if workspace or session interface should be used
     * @throws RepositoryException
     * @throws SAXException
     * @throws IOException
     */
    public void importWithHandler(String absPath, Document document,
                                  int uuidBehaviour, boolean withWorkspace)
            throws Exception {

        serialize(document);
        BufferedInputStream bin = new BufferedInputStream(new FileInputStream(file));

        ContentHandler handler;
        if (withWorkspace) {
            handler = workspace.getImportContentHandler(absPath, uuidBehaviour);
        } else {
            handler = session.getImportContentHandler(absPath, uuidBehaviour);
        }

        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setFeature("http://xml.org/sax/features/namespaces", true);
        reader.setFeature("http://xml.org/sax/features/namespace-prefixes", false);

        reader.setContentHandler(handler);
        reader.parse(new InputSource(bin));

        if (!withWorkspace) {
            session.save();
        }
    }

//--------------------------------< helpers >-----------------------------------
    /**
     * Tests if jcr:uuid property of mix:referenceable nodetype is respected.
     * This is believed as true when during import with uuidBehaviour
     * IMPORT_UUID_COLLISION_REMOVE_EXISTING a node with the same uuid as a node
     * to be imported will be deleted.
     *
     * @return
     * @throws RepositoryException
     * @throws IOException
     */
    public boolean isMixRefRespected() throws RepositoryException, IOException {
        boolean respected = false;
        if (supportsNodeType(mixReferenceable)) {
            String uuid;
            try {
                uuid = createReferenceableNode(referenced);
            } catch (NotExecutableException e) {
                return false;
            }
            Document document = dom.newDocument();
            Element root = document.createElement(rootElem);
            root.setAttribute(XML_NS + ":jcr", NS_JCR_URI);
            root.setAttributeNS(NS_JCR_URI, jcrUUID, uuid);
            root.setAttributeNS(NS_JCR_URI, jcrMixinTypes, mixReferenceable);
            document.appendChild(root);

            importXML(refTargetNode.getPath(), document,
                    ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING, SESSION);
            session.save();

            // existing node with same uuid should now be deleted
            respected = !testRootNode.hasNode(referenced);

            // check if imported document node is referenceable
            Node rootNode = refTargetNode.getNode(rootElem);
            respected &= rootNode.isNodeType(mixReferenceable);
        }
        return respected;
    }

    /**
     * Creates a node with given name below the testRootNode which will be
     * referenced by the node nodeName2 and returns the UUID assigned to the
     * created node.
     *
     * @param name
     * @return
     * @throws RepositoryException
     * @throws NotExecutableException if the created node is not referenceable
     * and cannot be made referenceable by adding mix:referenceable.
     */
    public String createReferenceableNode(String name) throws RepositoryException, NotExecutableException {
        // remove a yet existing node at the target
        try {
            Node node = testRootNode.getNode(name);
            node.remove();
            session.save();
        } catch (PathNotFoundException pnfe) {
            // ok
        }
        // a referenceable node
        Node n1 = testRootNode.addNode(name, testNodeType);
        if (!n1.isNodeType(mixReferenceable) && !n1.canAddMixin(mixReferenceable)) {
            n1.remove();
            session.save();
            throw new NotExecutableException("node type " + testNodeType +
                    " does not support mix:referenceable");
        }
        n1.addMixin(mixReferenceable);
        // make sure jcr:uuid is available
        testRootNode.save();
        return n1.getUUID();
    }

    /**
     * Creates a document with a element rootElem containing a jcr:uuid
     * attribute with the given uuid as value. This document is imported below
     * the node with path absPath. If nod node at absPth it is created.
     * If there is yet a node rootElem below the then this node is
     * romoved in advance.
     *
     * @param uuid
     * @param uuidBehaviour
     * @param withWorkspace
     * @param withHandler
     * @throws RepositoryException
     * @throws IOException
     */
    public void importRefNodeDocument(
            String absPath, String uuid, int uuidBehaviour,
            boolean withWorkspace, boolean withHandler)
            throws Exception {

        Document document = dom.newDocument();
        Element root = document.createElement(rootElem);
        root.setAttribute(XML_NS + ":jcr", NS_JCR_URI);
        root.setAttributeNS(NS_JCR_URI, jcrUUID, uuid);
        root.setAttributeNS(NS_JCR_URI, jcrMixinTypes, mixReferenceable);
        root.setAttribute(propertyName1, "some text");
        document.appendChild(root);

        Node targetNode = null;
        try {
            targetNode = (Node) session.getItem(absPath);
            // remove a yet existing node at the target
            try {
                Node node = targetNode.getNode(rootElem);
                node.remove();
                session.save();
            } catch (PathNotFoundException pnfe) {
                // ok
            }
        } catch (PathNotFoundException pnfe) {
            // create the node
            targetNode = createAncestors(absPath);
        }

        if (withHandler) {
            importWithHandler(targetNode.getPath(), document, uuidBehaviour, withWorkspace);
        } else {
            importXML(targetNode.getPath(), document, uuidBehaviour, withWorkspace);
        }
        session.save();
    }

    protected Node createAncestors(String absPath) throws RepositoryException {
        // create nodes to name of absPath
        Node root = session.getRootNode();
        StringTokenizer names = new StringTokenizer(absPath, "/");
        Node currentNode = root;
        while (names.hasMoreTokens()) {
            String name = names.nextToken();
            if (currentNode.hasNode(name)) {
                currentNode = currentNode.getNode(name);
            } else {
                currentNode = currentNode.addNode(name);
            }
        }
        root.save();
        return currentNode;
    }

    public void serialize(Document document) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        try {
            // disable pretty printing/default line wrapping!
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            t.setOutputProperty(OutputKeys.INDENT, "no");
            Source s = new DOMSource(document);
            Result r = new StreamResult(bos);
            t.transform(s, r);
        } catch (TransformerException te) {
            throw (IOException) new IOException(te.getMessage()).initCause(te);
        } finally {
            bos.close();
        }
    }

    public boolean supportsNodeType(String ntName) throws RepositoryException {
        boolean support = false;
        try {
            ntManager.getNodeType(ntName);
            support = true;
        } catch (NoSuchNodeTypeException nste) {
            //
        }
        return support;
    }

    /**
     * Returns a namespace prefix that currently not used in the namespace
     * registry.
     *
     * @return an unused namespace prefix.
     */
    protected String getUnusedPrefix() throws RepositoryException {
        Set prefixes = new HashSet(Arrays.asList(nsp.getPrefixes()));
        String prefix = TEST_PREFIX;
        int i = 0;
        while (prefixes.contains(prefix)) {
            prefix = TEST_PREFIX + i++;
        }
        return prefix;
    }

    /**
     * Returns a namespace URI that currently not used in the namespace
     * registry.
     *
     * @return an unused namespace URI.
     */
    protected String getUnusedURI() throws RepositoryException {
        Set uris = new HashSet(Arrays.asList(nsp.getURIs()));
        String uri = TEST_URI;
        int i = 0;
        while (uris.contains(uri)) {
            uri = TEST_URI + i++;
        }
        return uri;
    }
}
