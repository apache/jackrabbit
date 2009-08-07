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
import org.apache.jackrabbit.test.XMLChar;

import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;

import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.PropertyIterator;
import javax.jcr.Value;
import java.util.Stack;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Enumeration;
import java.io.File;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

/**
 * <code>ExportDocViewTest</code> tests the two Session methods :
 * {@link Session#exportDocumentView(String, ContentHandler, boolean, boolean)}
 * and {@link Session#exportDocumentView(String, java.io.OutputStream, boolean, boolean)}
 * against the required behaviours according the document view xml mapping
 * defined in the JSR 170 specification in chapter 6.4.2, 6.4.3 and 6.4.4 .
 *
 * @test
 * @sources ExportDocViewTest.java
 * @executeClass org.apache.jackrabbit.test.api.ExportDocViewTest
 * @keywords level1
 */
public class ExportDocViewTest extends AbstractJCRTest {

    private final boolean CONTENTHANDLER = true, STREAM = false;
    private final boolean SKIPBINARY = true, SAVEBINARY = false;
    private final boolean NORECURSE = true, RECURSE = false;

    /**
     * Resolved Name for jcr:xmltext
     */
    private String JCR_XMLTEXT;
    /**
     * Resolved Name for jcr:xmlcharacters
     */
    private String JCR_XMLDATA;
    /**
     * the stack of the text node values to check
     */
    private Stack textValuesStack;

    private class StackEntry {
        // the list of text node values of the text nodes of an xml element
        ArrayList textValues;
        // the current position in the ArrayList
        int position = 0;
    }

    /**
     * indicates if the tested repository exports multivalued properties.
     */
    private boolean exportMultivalProps = false;

    /**
     * indicates if the tested repository escapes (xml)invalid jcr names.
     */
    private boolean exportInvalidXmlNames = false;

    private boolean skipBinary;
    private boolean noRecurse;
    private boolean withHandler;

    private File file;
    private Session session;
    private Workspace workspace;
    private NamespaceRegistry nsr;
    private String testPath;

    private Document doc;

    protected void setUp() throws Exception {
        isReadOnly = true;
        session = helper.getReadOnlySession();
        workspace = session.getWorkspace();
        nsr = workspace.getNamespaceRegistry();
        file = File.createTempFile("docViewExportTest", ".xml");
        super.setUp();

        JCR_XMLTEXT = session.getNamespacePrefix(NS_JCR_URI) + ":xmltext";
        JCR_XMLDATA = session.getNamespacePrefix(NS_JCR_URI) + ":xmlcharacters";

        testPath = testRoot;
    }

    protected void tearDown() throws Exception {
        file.delete();
        if (session != null) {
            session.logout();
            session = null;
        }
        workspace = null;
        nsr = null;
        super.tearDown();
    }

    public void testExportDocView_handler_session_skipBinary_noRecurse()
            throws IOException, RepositoryException, SAXException, TransformerException {
        doTestExportDocView(CONTENTHANDLER, SKIPBINARY, NORECURSE);
    }

    public void testExportDocView_handler_session_skipBinary_recurse()
            throws IOException, RepositoryException, SAXException, TransformerException {
        doTestExportDocView(CONTENTHANDLER, SKIPBINARY, RECURSE);
    }

    public void testExportDocView_handler_session_saveBinary_noRecurse()
            throws IOException, RepositoryException, SAXException, TransformerException {
        doTestExportDocView(CONTENTHANDLER, SAVEBINARY, NORECURSE);
    }

    public void testExportDocView_handler_session_saveBinary_recurse()
            throws IOException, RepositoryException, SAXException, TransformerException {
        doTestExportDocView(CONTENTHANDLER, SAVEBINARY, RECURSE);
    }

    public void testExportDocView_stream_session_skipBinary_recurse()
            throws IOException, RepositoryException, SAXException, TransformerException {
        doTestExportDocView(STREAM, SKIPBINARY, RECURSE);
    }

    public void testExportDocView_stream_session_skipBinary_noRecurse()
            throws IOException, RepositoryException, SAXException, TransformerException {
        doTestExportDocView(STREAM, SKIPBINARY, NORECURSE);
    }

    public void testExportDocView_stream_session_saveBinary_noRecurse()
            throws IOException, RepositoryException, SAXException, TransformerException {
        doTestExportDocView(STREAM, SAVEBINARY, NORECURSE);
    }

    public void testExportDocView_stream_session_saveBinary_recurse()
            throws IOException, RepositoryException, SAXException, TransformerException {
        doTestExportDocView(STREAM, SAVEBINARY, RECURSE);
    }

    /**
     * Tests session.exportDocView with the different argument possibilities.
     * The flag withHandler decides if the method requiring a ContentHandler as
     * argument is called. The class org.apache.xml.serialize.XMLSerializer is
     * taken as ContentHandler in this case. In both cases ( export with a
     * ContentHandler and export with Stream) the test node is exported to the
     * file defined in the setUp. This exported file is parsed using
     * javax.xml.transform package and the receiving document is compared with
     * the test node and its properties and child nodes in the repository.
     *
     * @param withHandler boolean, decides to call method requiring a
     *                    ContentHandler as argument
     * @param skipBinary
     * @param noRecurse
     */
    public void doTestExportDocView(boolean withHandler, boolean skipBinary, boolean noRecurse)
            throws RepositoryException, IOException, SAXException, TransformerException {

        this.skipBinary = skipBinary;
        this.noRecurse = noRecurse;
        this.withHandler = withHandler;
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
        try {
            if (withHandler) {
                SAXTransformerFactory stf =
                    (SAXTransformerFactory) SAXTransformerFactory.newInstance();
                TransformerHandler th = stf.newTransformerHandler();
                th.setResult(new StreamResult(os));
                session.exportDocumentView(testPath, th, skipBinary, noRecurse);
            } else {
                session.exportDocumentView(testPath, os, skipBinary, noRecurse);
            }
        } finally {
            os.close();
        }

        // build the DOM tree
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        doc = readDocument(in);

        compareTree();
    }

    /**
     * Compares the test node with the document's root element. In case also the
     * child nodes are exported (noRecurse = false) the child nodes of the test
     * node are compared with the child elements of the root element too.
     *
     * @throws RepositoryException
     */
    private void compareTree() throws RepositoryException, IOException {
        Element root = doc.getDocumentElement();
        textValuesStack = new Stack();
        // we assume the path is valid
        Item item = session.getItem(testPath);

        // only an absolute path to a node is allowed
        if (!item.isNode()) {
            fail("Item at the given root path " + testPath + " is not a node.");
        }
        Node node = (Node) item;
        // multival props exported?
        setExportMultivalProps(node, root, false);
        // items with invalid xml names exported?
        setExportInvalidXmlNames(node, root, false);
        // check the test root node
        checkRootElement(node, root);
        // check the namespaces
        compareNamespaces(root);
        // check the exported data against the node which is exported.
        compareNode(node, root);

        // check the whole tree
        if (!noRecurse) {
            checkChildNodes(node, root);
        }
    }

    /**
     * Assures that root element exists and has correct jcr:root name if it is
     * the root node of the repository. (chapter 6.4.2.2 of the JCR
     * specification.) Also checks if multivalued properties are exported
     * (chapter 6.4.2.5 of the JCR specification.) Also tries to find out if
     * items with an invalid xml name are exported or not. (chapter 6.4.2.4 of
     * the JCR specification.)
     *
     * @param node
     * @param root
     * @throws RepositoryException
     */
    private void checkRootElement(Node node, Element root) throws RepositoryException {

        boolean isValidName = XMLChar.isValidName(node.getName());
        if (root != null) {
            // check correct element name if the root node of the repository is exported.
            if (node.getDepth() == 0) {
                assertEquals("Exported root node has not correct name jcr:root.",
                        "jcr:root", root.getTagName());
            }
        } else {
            if (exportInvalidXmlNames || isValidName) {
                fail("Node " + node.getPath() + " is not exported.");
            }
        }
    }

    /**
     * Checks the child nodes of the given node against the child nodes of the
     * given xml element. The found text nodes of the xml element are hold in an
     * ArrayList and put on a stack for further checking if another child
     * element is between them.
     *
     * @param node
     * @param elem
     * @throws RepositoryException
     */
    private void checkChildNodes(Node node, Element elem)
            throws RepositoryException, IOException {

        NodeIterator nodeIter = node.getNodes();
        if (getSize(node.getNodes()) == 0) {
            assertTrue("Exported node " + node.getPath() + " has child elements " +
                    "although it has no child nodes ", 0 == countChildElems(elem));
        } else {
            // create a stack entry for the text child nodes
            // of the current xml element
            StackEntry entry = new StackEntry();
            entry.textValues = getChildTextNodeValues(elem);
            textValuesStack.push(entry);
            // xmltext nodes directly following each other
            // are serialized together as xml text
            ArrayList jcrTextNodes = new ArrayList();

            while (nodeIter.hasNext()) {
                Node childNode = nodeIter.nextNode();
                if (isXMLTextNode(childNode)) {
                    jcrTextNodes.add(childNode);
                } else {
                    if (jcrTextNodes.size() > 0) {
                        compareXmltextNodes(jcrTextNodes, elem);
                        // reset the Array
                        jcrTextNodes.clear();
                    }
                    compareChildTree(childNode, elem);
                }
            }
            // finally we are through the child nodes
            // so we delete the stackEntry
            textValuesStack.pop();
        }
    }

    /**
     * Compares the child tree of a given node against the child elements of a
     * given element. (chapter 6.4.2.1 points 2,3,4 of the JCR specification).
     * <p/>
     * Considered are the export constraints regarding nodes named jcr:xmldata
     * (chapter 6.4.2.3 of the JCR specification).
     * <p/>
     * Also the numbers of exported child elements is compared with the number
     * of child nodes.
     *
     * @param node
     * @param parentElem
     * @throws RepositoryException
     */
    private void compareChildTree(Node node, Element parentElem)
            throws RepositoryException, IOException {

        Element nodeElem;

        // find a childElem belonging to the node and check it.
        nodeElem = findElem(node, parentElem);
        if (nodeElem != null) {
            compareNode(node, nodeElem);
            // go deep
            checkChildNodes(node, nodeElem);
        }
    }

    /**
     * Checks the given Element if it has a child element with the same (or
     * eventually escaped) name as the given node. (chapter 6.4.2.1 point 3 of
     * the JCR specification).
     *
     * @param node
     * @param parentElem
     * @return Child Element of parentElem. Null if no corresponidng element is
     *         found.
     * @throws RepositoryException
     */
    private Element findElem(Node node, Element parentElem) throws RepositoryException {
        String name = node.getName();
        Element nodeElem = null;
        // valid xml name?
        boolean isValidName = XMLChar.isValidName(name);

        name = !isValidName ? escapeNames(name) : name;
        // same name sibs
        ArrayList children = getChildElems(parentElem, name);

        if (children.size() > 0) {
            // xmltext nodes are not exported as elements
            if (isXMLTextNode(node)) {
                fail("Xml text node " + node.getPath() +
                        " is wronlgy exported as element.");
            } else {
                // order of same name siblings is preserved during export
                int index = node.getIndex();
                try {
                    nodeElem = (Element) children.get(index - 1);
                } catch (IndexOutOfBoundsException iobe) {
                    fail("Node " + node.getPath() + " is not exported."
                            + iobe.toString());
                }
            }
        } else {
            // need to be exported
            if (!isXMLTextNode(node) && (isValidName || exportInvalidXmlNames)) {
                fail("Node " + node.getPath() + " is not exported.");
            }
        }
        return nodeElem;
    }

    /**
     * Check if a property of a node is exported. This is true if a
     * corresponding attribute is found in the element the node is exported to.
     * An attribute is corresponding when it has the same name as the given
     * property (or it is equal to its escaped name). (chapter 6.4.2.1 point 5
     * of the JCR specification).
     *
     * @param prop
     * @param elem
     * @return
     * @throws RepositoryException
     */
    private Attr findAttribute(Property prop, Element elem)
            throws RepositoryException {

        String name = prop.getName();

        boolean isValidName = XMLChar.isValidName(name);

        name = !isValidName ? escapeNames(name) : name;
        Attr attribute = elem.getAttributeNode(name);
        return attribute;
    }

    /**
     * Check if a property should be exported according the three choices
     * skipBinary, exportMultivalProps and exportInvalidXmlNames.
     *
     * @param prop
     * @param attribute
     * @throws RepositoryException
     */
    private void checkAttribute(Property prop, Attr attribute) throws RepositoryException {

        boolean isBinary = (prop.getType() == PropertyType.BINARY);
        boolean isMultiple = prop.getDefinition().isMultiple();
        if (skipBinary) {
            if (isBinary && !(isMultiple && !exportMultivalProps)) {
                assertEquals("Value of binary property " + prop.getPath() +
                        " exported although skipBinary is true",
                        attribute.getValue().length(), 0);
            }
            // check the flags
            else {
                checkExportFlags(prop, attribute);
            }
        }
        // saveBinary
        else {
            if (isBinary && !(isMultiple && !exportMultivalProps)) {
                assertTrue("Binary property " + prop.getPath() +
                        " not exported although skipBinary is false", attribute != null);
            }
            // check anyway the flags
            checkExportFlags(prop, attribute);
        }
    }

    /**
     * Checks attribute export regarding the two flags and without considering
     * skipBinary.
     *
     * @param prop
     * @param attribute
     * @throws RepositoryException
     */
    private void checkExportFlags(Property prop, Attr attribute)
            throws RepositoryException {

        String name = prop.getName();
        boolean isMultiple = prop.getDefinition().isMultiple();
        boolean isValidName = XMLChar.isValidName(name);

        if (isMultiple) {
            if (exportMultivalProps) {
                assertTrue("Not all multivalued properties are exported: "
                        + prop.getPath() + " is not exported.", attribute != null);
            } else {
                // skipping multi-valued properties entirely is legal
                // according to "6.4.2.5 Multi-value Properties" of the
                // jsr-170 specification
                return;
            }
        }
        // check anyway the other flag
        if (exportInvalidXmlNames && !isValidName) {
            assertTrue("Not all properties with invalid xml name are exported: " +
                    prop.getPath() + " is not exported.", attribute != null);
        } else {
            assertTrue("Property " + prop.getPath() + " is not exported.",
                    attribute != null);
        }
    }

    /**
     * Compares the given node with the given element. Comparison is succesful
     * if the number of exported child nodes and exported properties match the
     * found child elements and attributes considering the possible exceptions
     * and if the comparison of the properties of the node with the attributes
     * of the element is successful too.
     *
     * @param node
     * @param elem
     * @throws RepositoryException
     */
    private void compareNode(Node node, Element elem)
            throws RepositoryException, IOException {
        // count the child nodes and compare with the exported child elements
        compareChildNumber(node, elem);
        // count the properties and compare with attributes exported
        comparePropNumber(node, elem);

        PropertyIterator propIter = node.getProperties();
        while (propIter.hasNext()) {
            Property prop = propIter.nextProperty();
            Attr attr = findAttribute(prop, elem);
            checkAttribute(prop, attr);
            if (attr != null) {
                compareProperty(prop, attr);
            }
        }
    }

    /**
     * Compare the given property with the given attribute. Comparison is
     * successful if their values can be matched considering binary type,
     * multivalue export. (chapter 6.4.2.1 point 6 of the JCR specification).
     *
     * @param prop
     * @param attr
     * @throws RepositoryException
     */
    private void compareProperty(Property prop, Attr attr)
            throws RepositoryException, IOException {

        boolean isMultiple = prop.getDefinition().isMultiple();
        boolean isBinary = (prop.getType() == PropertyType.BINARY);
        String attrVal = attr.getValue();
        String val = null;
        if (isMultiple) {
            val = exportValues(prop, isBinary);
        } else {
            if (isBinary) {
                try {
                    attrVal = decodeBase64(attrVal);
                    val = prop.getString();
                } catch (IOException ioe) {
                    fail("Could not decode value of binary attribute " +
                            attr.getName() + " of element " +
                            attr.getOwnerElement().getTagName());
                }
            } else {
                val = prop.getString();
            }
        }
        if (isBinary && skipBinary) {
            assertEquals("Value of binary property " + prop.getPath() +
                    " is not exported correctly: ", "", attrVal);
            assertEquals("Value of binary property " + prop.getPath() +
                    " exported although skipBinary is true",
                    "", attrVal);
        } else {
            assertTrue("Value of property " + prop.getPath() +
                    " is not exported correctly: " + attrVal,
                    val.equals(attrVal) || escapeValues(val).equals(attrVal));
        }
    }

    /**
     * Checks if all registered namespaces are exported into the root element.
     * (chapter 6.4.2.1 point 1 of the JCR specification).
     *
     * @param root
     * @throws RepositoryException
     */
    private void compareNamespaces(Element root) throws RepositoryException {

        Properties nameSpaces = new AttributeSeparator(root).getNsAttrs();
        // check if all namespaces exist that were exported
        for (Enumeration e = nameSpaces.keys(); e.hasMoreElements();) {
            String prefix = (String) e.nextElement();
            String URI = nameSpaces.getProperty(prefix);

            assertEquals("Prefix of uri" + URI + "is not exported correctly.",
                    nsr.getPrefix(URI), prefix);
            assertEquals("Uri of prefix " + prefix + "is not exported correctly.",
                    nsr.getURI(prefix), URI);
        }

        String[] registeredNamespaces = nsr.getURIs();
        // check if all required namespaces are exported
        for (int i = 0; i < registeredNamespaces.length; i++) {
            String prefix = nsr.getPrefix(registeredNamespaces[i]);
            // skip default namespace and xml namespaces
            if (prefix.length() == 0 || prefix.startsWith("xml")) {
                continue;
            } else {
                assertTrue("namespace: " + registeredNamespaces[i] + " not exported", nameSpaces.keySet().contains(prefix));
            }
        }
    }

    /**
     * Count the number of child nodes of a node which are exported and compare
     * with the number expected.
     *
     * @param node
     * @param elem
     * @throws RepositoryException
     */
    private void compareChildNumber(Node node, Element elem) throws RepositoryException {
        NodeIterator iter = node.getNodes();
        long size = 0;

        long exported = countChildElems(elem);
        // child tree is exported too
        if (!noRecurse) {
            size = getSize(node.getNodes());
            while (iter.hasNext()) {
                Node n = iter.nextNode();
                String name = n.getName();

                // xmltext node ?
                if (isXMLTextNode(n)) {
                    size--;
                }
                if (!exportInvalidXmlNames && !XMLChar.isValidName(name)) {
                    size--;
                }

            }
        }
        assertEquals("The number of child nodes of node  " + node.getPath() +
                " which are exported is not correct: ", size, exported);
    }

    /**
     * Count the number of exported properties of a given node and compare with
     * the number of the properties expected to be exported.
     *
     * @param node
     * @param elem
     * @throws RepositoryException
     */
    private void comparePropNumber(Node node, Element elem)
            throws RepositoryException {

        PropertyIterator iter = node.getProperties();
        long size = getSize(node.getProperties());
        long exported = new AttributeSeparator(elem).getNonNsAttrs().size();
        while (iter.hasNext()) {
            Property prop = iter.nextProperty();
            String name = prop.getName();
            boolean isMultiple = prop.getDefinition().isMultiple();

            // props not exported so we decrease the expected size.
            if (!exportInvalidXmlNames && !XMLChar.isValidName(name)) {
                size--;
            } else if (!exportMultivalProps && isMultiple) {
                size--;
            }
        }
        assertEquals("The number of properties exported of node " + node.getPath() +
                " is not correct.", size, exported);
    }

    /**
     * Compares the text of a given XML element with the values of the
     * jcr:xmlcharacters properties of the given jcr:xmltext nodes sequel. If
     * the sequel has more than one node the serialized values are concatenated
     * with a space. We only check the case withHandler is true.
     *
     * @param nodes
     * @param parentElem
     * @throws RepositoryException
     */
    private void compareXmltextNodes(ArrayList nodes, Element parentElem)
            throws RepositoryException {
        // only this case
        if (withHandler) {
            String value = "";
            String exportedVal = "";

            StackEntry currentEntry = (StackEntry) textValuesStack.pop();
            try {
                exportedVal = (String) currentEntry.textValues.get(currentEntry.position);
                currentEntry.position++;
                textValuesStack.push(currentEntry);
            } catch (IndexOutOfBoundsException iobe) {
                fail("Xmltext nodes not correctly exported: " + iobe.getMessage());
            }

            int size = nodes.size();
            if (size == 1) {
                Node node = (Node) nodes.get(0);
                Property prop = node.getProperty(JCR_XMLDATA);
                value = prop.getString();
                assertEquals("The " + JCR_XMLTEXT + " node " + node.getPath() +
                        " is not exported correctly.",
                        value, exportedVal);
            } else {
                // check the concatenated values sequenceally
                for (int i = 0; i < nodes.size(); i++) {
                    Node node = (Node) nodes.get(i);
                    Property prop = node.getProperty(JCR_XMLDATA);
                    value = prop.getString();
                    // the first one
                    if (i == 0) {
                        if (exportedVal.regionMatches(0, value, 0, value.length())) {
                            // test ok, remove the checked part of the text
                            exportedVal = exportedVal.substring(0, value.length());
                        } else {
                            fail("The " + JCR_XMLTEXT + " node " + node.getPath() +
                                    " is not exported correctly: expected: " +
                                    value + " found: " + exportedVal);
                        }
                    }
                    // we assume at the moment that any white space char is possible
                    // between  two adjacent xmltext nodesso we try to match as long
                    // as space characters are at the beginning of the
                    // remaining exported string
                    // todo once this will be specified in the spec more exactly
                    else {
                        // the last one
                        if (exportedVal.regionMatches(0, value, 0, value.length())) {
                            // test ok
                            exportedVal = exportedVal.substring(0, value.length());
                        } else {
                            boolean match = false;
                            int j = 0;
                            char c = exportedVal.charAt(j);
                            while (c == ' ' || c == '\n' || c == '\r'
                                    || c == '\t' || c == '\u000B') {
                                if (exportedVal.regionMatches(j, value, 0, value.length())) {
                                    exportedVal = exportedVal.substring(j, value.length() + j);
                                    match = true;
                                    break;
                                } else {
                                    j++;
                                    c = exportedVal.charAt(j);
                                }
                            }
                            assertTrue("The " + JCR_XMLTEXT + " node " + node.getPath() +
                                    " is not exported correctly: expected: "
                                    + value + " found: " + exportedVal, match);
                        }
                    }
                }
            }
        }
    }

    /**
     * Loops through all child items of a given node to test if items with
     * invalid xml name are exported. (chapter 6.4.2.4 of the JCR
     * specification).
     *
     * @param node the root node of the tree to search
     * @param elem the parent element of the element to which the parent node of
     *             the given node is exported.
     * @throws RepositoryException
     */

    private boolean setExportInvalidXmlNames(Node node, Element elem, boolean isSet)
            throws RepositoryException {

        if (!XMLChar.isValidName(node.getName())) {
            if (elem != null) {
                exportInvalidXmlNames = true;
                isSet = true;
            } else {
                exportInvalidXmlNames = false;
                isSet = true;
            }
        }

        // try properties
        if (!isSet) {
            PropertyIterator iter = node.getProperties();
            while (iter.hasNext()) {
                Property prop = iter.nextProperty();
                if (!exportMultivalProps && prop.getDefinition().isMultiple()) {
                    continue;
                }
                if (!XMLChar.isValidName(prop.getName())) {
                    // property exported?
                    exportInvalidXmlNames = isExportedProp(prop, elem);
                    isSet = true;
                }
            }
        }

        // try child nodes
        if (!isSet && !noRecurse) {
            // search again
            NodeIterator iter = node.getNodes();
            while (iter.hasNext()) {
                Node n = iter.nextNode();
                Element el = findElem(n, elem);
                isSet = setExportInvalidXmlNames(n, el, isSet);
            }
        }
        return isSet;
    }

    /**
     * Set the exportMultivalProps flag. Traverses the tree given by the node
     * and searches a multivalue property which is exported to an attribute of a
     * element of an element tree. (chapter 6.4.2.5 of the JCR specification).
     *
     * @param node
     * @param elem
     * @throws RepositoryException
     */
    private boolean setExportMultivalProps(Node node, Element elem, boolean isSet)
            throws RepositoryException {

        Property[] props = searchMultivalProps(node);
        // multivalued property with valid xml name
        if (props[0] != null) {
            exportMultivalProps = isExportedProp(props[0], elem);
            isSet = true;
        } else {
            // invalid xml named multivalue property exported
            if (props[1] != null) {
                exportMultivalProps = isExportedProp(props[1], elem);
                if (!exportMultivalProps && exportInvalidXmlNames) {
                    isSet = true;
                }
            }
        }

        if (!isSet && !noRecurse) {
            // search again
            NodeIterator iter = node.getNodes();
            while (iter.hasNext()) {
                Node n = iter.nextNode();
                Element el = findElem(n, elem);
                if (el != null) {
                    isSet = setExportMultivalProps(n, el, isSet);
                } else {
                    isSet = false;
                }
            }
        }
        return isSet;
    }

    //-----------------------------------< helper methods >-----------------------------
    /**
     * Search a given node if it contains a multivalue property. As invalid xml
     * names may be exported or not we want to find a multivalue property with
     * valid xml name and also one with an invalid xml name. Returned is a pair
     * of multivalued properties, the first has a valid xml name, the second an
     * invalid one. In case one of these is not found it is replaced by null in
     * the pair.
     *
     * @param node the node to start the search.
     * @return A pair of multivalued properties.
     * @throws RepositoryException
     */
    private Property[] searchMultivalProps(Node node) throws RepositoryException {
        Property[] properties = {null, null};
        for (PropertyIterator props = node.getProperties(); props.hasNext();) {
            Property property = props.nextProperty();
            if (property.getDefinition().isMultiple()) {
                if (XMLChar.isValidName(property.getName())) {
                    properties[0] = property;
                    break;
                } else {
                    properties[1] = property;
                }
            }
        }
        return properties;
    }

    /**
     * Tests if a property is exported or not.
     *
     * @param prop
     * @param elem
     * @return
     * @throws RepositoryException
     */
    private boolean isExportedProp(Property prop, Element elem) throws RepositoryException {
        String name = prop.getName();
        name = XMLChar.isValidName(prop.getName()) ? name : escapeNames(name);
        Attr attr = elem.getAttributeNode(name);
        return (attr != null);
    }

    /**
     * Checks if a given  node is a jcr:xmltext named node and fulfills the
     * condition that the property's value is exported as text.
     *
     * @param node The node to check.
     * @return boolean indicating if the given node fulfills the required
     *         conditions.
     * @throws RepositoryException
     */
    private boolean isXMLTextNode(Node node) throws RepositoryException {
        boolean isTrue = node.getName().equals(JCR_XMLTEXT);
        if (node.hasProperty(JCR_XMLDATA)) {
            Property prop = node.getProperty(JCR_XMLDATA);

            isTrue = !prop.getDefinition().isMultiple()
                    && prop.getType() == PropertyType.STRING
                    // only one property beneath the required jcr:primaryType
                    && getSize(node.getProperties()) == 2
                    && getSize(node.getNodes()) == 0;
        } else {
            isTrue = false;
        }
        return isTrue;
    }

//-----------------------------------< static helper methods >-----------------------------

    /**
     * Decodes a given base 64 encoded string.
     *
     * @param str
     * @return
     * @throws IOException
     */
    private static String decodeBase64(String str) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Base64.decode(str, bos);
        String decoded = bos.toString("UTF-8");
        return decoded;
    }

    /**
     * Encodes a given stream to base64.
     *
     * @param in the stream to encode.
     * @return the encoded string in base64.
     * @throws IOException if an error occurs.
     */
    private static String encodeBase64(InputStream in) throws IOException {
        StringWriter writer = new StringWriter();
        Base64.encode(in, writer);
        return writer.getBuffer().toString();
    }

    /**
     * Exports values of a multivalue property and concatenate the values
     * separated by a space. (chapter 6.4.4 of the JCR specification).
     *
     * @param prop
     * @param isBinary
     * @return
     * @throws RepositoryException
     */
    private static String exportValues(Property prop, boolean isBinary)
            throws RepositoryException, IOException {
        Value[] vals = prop.getValues();
        // order of multi values is preserved.
        // multival with empty array is exported as empty string
        StringBuffer exportedVal = new StringBuffer();

        String space = "";
        if (isBinary) {
            for (int i = 0; i < vals.length; i++) {
                exportedVal.append(space);
                InputStream in = vals[i].getStream();
                try {
                    exportedVal.append(encodeBase64(in));
                } finally {
                    in.close();
                }
                space = " ";
            }
        } else {
            for (int i = 0; i < vals.length; i++) {
                exportedVal.append(space);
                exportedVal.append(escapeValues(vals[i].getString()));
                space = " ";
            }
        }
        return exportedVal.toString();
    }

    /**
     * Escapes the characters of a given String representing a  Name of an item.
     * The escaping scheme is according the requirements of the JSR 170
     * Specification chapter 6.4.3 . No check performed if the given string is
     * indeed a Name or not.
     *
     * @param name
     * @return
     */
    private static String escapeNames(String name) {
        return EscapeJCRUtil.escapeJCRNames(name);
    }

    /**
     * Escapes the characters of a given string value according the requirements
     * of chapter 6.4.4 of JSR 170 Specification.
     *
     * @param value The string to escape its characters.
     * @return
     */
    private static String escapeValues(String value) {
        return EscapeJCRUtil.escapeJCRValues(value);
    }

//----------------< helpers to retrieve data from an xml document >-------------------

    /**
     * Returns all child elements of the given xml element which have the given
     * name.
     *
     * @param elem
     * @param name
     * @return
     */
    private ArrayList getChildElems(Element elem, String name) {
        ArrayList children = new ArrayList();
        org.w3c.dom.Node child = elem.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                if (name.equals("*") || name.equals(child.getNodeName())) {
                    children.add(child);
                }
            }
            child = child.getNextSibling();
        }
        return children;
    }

    /**
     * Counts the number of child elements of the given xml element.
     *
     * @param elem
     * @return
     */
    private long countChildElems(Element elem) {
        long length = 0;
        org.w3c.dom.Node child = elem.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                length++;
            }
            child = child.getNextSibling();
        }
        return length;
    }

    /**
     * Collects the characters of successive text nodes of the given xml element
     * into an ArrayList.
     *
     * @param elem
     * @return
     */
    private ArrayList getChildTextNodeValues(Element elem) {
        ArrayList textValues = new ArrayList();
        StringBuffer buf = new StringBuffer();
        org.w3c.dom.Node child = elem.getFirstChild();
        // collect the characters of successive text nodes
        while (child != null) {
            if (child.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
                while (child != null
                        && child.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
                    buf.append(child.getNodeValue());
                    child = child.getNextSibling();
                }
                textValues.add(buf.toString());
                buf = new StringBuffer();
            } else {
                child = child.getNextSibling();
            }
        }
        return textValues;
    }

    /**
     * Reads a DOM document from the given XML stream.
     *
     * @param xml XML stream
     * @return DOM document
     * @throws RepositoryException if the document could not be read
     */
    private Document readDocument(InputStream xml) throws RepositoryException {
        try {
            StreamSource source = new StreamSource(xml);
            DOMResult result = new DOMResult();
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, result);
            return (Document) result.getNode();
        } catch (TransformerException e) {
            throw new RepositoryException("Unable to read xml file", e);
        }
    }

    /**
     * Helper class to separate the attributes with xmlns namespace from the
     * attributes without xmlns namspace. Solely used for the root element of an
     * xml document.
     */
    private class AttributeSeparator {
        private static final String xmlnsURI = "http://www.w3.org/2000/xmlns/";
        private static final String xmlnsPrefix = "xmlns";

        Element elem;
        NamedNodeMap attrs;
        Properties nsAttrs;
        Properties nonNsAttrs;

        AttributeSeparator(Element elem) {
            this.elem = elem;
            nsAttrs = new Properties();
            nonNsAttrs = new Properties();
            attrs = elem.getAttributes();
            separateAttrs();
        }

        public Properties getNsAttrs() {
            return nsAttrs;
        }

        public Properties getNonNsAttrs() {
            return nonNsAttrs;
        }

        private void separateAttrs() {
            for (int i = 0; i < attrs.getLength(); i++) {
                Attr attribute = (Attr) attrs.item(i);

                if (xmlnsURI.equals(attribute.getNamespaceURI())) {
                    String localName = attribute.getLocalName();
                    // ignore setting default namespace
                    if (xmlnsPrefix.equals(localName)) {
                        continue;
                    }
                    nsAttrs.put(localName, attribute.getValue());
                } else {
                    nonNsAttrs.put(attribute.getName(), attribute.getValue());
                }
            }
        }
    }
}