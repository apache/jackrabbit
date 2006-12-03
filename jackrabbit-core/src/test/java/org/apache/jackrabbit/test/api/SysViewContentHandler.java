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
 
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.nodetype.NodeType;
import javax.jcr.Session;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.PropertyIterator;
import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.RangeIterator;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Assert;

/**
 * ContentHandler implementation which checks if the system view export of
 * a node tree is compliant to the specification.
 */
class SysViewContentHandler extends DefaultHandler {

    // the session used
    protected Session session;
    // the path to start from
    protected String path;
    // the choices
    boolean skipBinary;
    boolean noRecurse;
    // the nodeElem in process
    NodeElemData currentNodeElem;
    // the parenNodetElem of the currentNodeElem
    NodeElemData parentNodeElem;
    // the propElem in process
    PropElemData currentPropElem;
    // the valueElem in process
    protected StringBuffer currentValue;
    // the value(s) of the current propElem e.g if multiple values
    protected ArrayList currentValues;
    // prefix mapping data
    protected HashMap prefixes;
    // if the first node is yet treated
    protected boolean testRootDone;
    // The stack holding the opened nodeElems
    Stack nodeElemStack;

    // resolved QNames for well known node and property names
    private String jcrRoot;
    private String jcrPrimaryType;
    private String jcrMixinTypes;
    private String jcrUuid;
    private String svNode;
    private String svProperty;
    private String svName;
    private String svType;
    private String svValue;
    private String mixReferenceable;

    /**
     * Constructor
     * @param path Thepath to the  root node of the tree to be exported.
     * @param session The session used.
     * @param skipBinary Boolean if the binary properties are not exported.
     * @param noRecurse Boolean if only the root node of the tree should be exported.
     */
    public  SysViewContentHandler(String path, Session session,
                                  boolean skipBinary, boolean noRecurse) throws RepositoryException {
        this.session =  session;
        this.path = path;
        this.skipBinary = skipBinary;
        this.noRecurse = noRecurse;

        jcrRoot = session.getNamespacePrefix(AbstractJCRTest.NS_JCR_URI) + ":root";
        jcrPrimaryType = session.getNamespacePrefix(AbstractJCRTest.NS_JCR_URI) + ":primaryType";
        jcrMixinTypes = session.getNamespacePrefix(AbstractJCRTest.NS_JCR_URI) + ":mixinTypes";
        jcrUuid = session.getNamespacePrefix(AbstractJCRTest.NS_JCR_URI) + ":uuid";
        svNode = session.getNamespacePrefix(AbstractJCRTest.NS_SV_URI) + ":node";
        svProperty = session.getNamespacePrefix(AbstractJCRTest.NS_SV_URI) + ":property";
        svName = session.getNamespacePrefix(AbstractJCRTest.NS_SV_URI) + ":name";
        svType = session.getNamespacePrefix(AbstractJCRTest.NS_SV_URI) + ":type";
        svValue = session.getNamespacePrefix(AbstractJCRTest.NS_SV_URI) + ":value";
        mixReferenceable = session.getNamespacePrefix(AbstractJCRTest.NS_MIX_URI) + ":referenceable";
    }

    /**
     * Check if the given path is valid.
     * Init the neccessary data.
     * @throws SAXException
     */
    public void startDocument() throws SAXException {
        try {
            // Check the given path, init the treeState stack
            Item item = session.getItem(path);
            checkCondition("TestPath "+path+" is not a path to a node.", item.isNode());
            nodeElemStack = new Stack();
            currentNodeElem = new NodeElemData();
            currentNodeElem.name = item.getName();
            currentNodeElem.node = (Node) item;
            currentNodeElem.path = path;
            prefixes = new HashMap();
            testRootDone = false;
        } catch (PathNotFoundException pe) {
            checkCondition("TestPath " + path + " is not a valid path."
                    + pe.toString(), false);
        } catch (RepositoryException re) {
            checkCondition("Could not determine test node: "
                    + re.toString(), false);
        }
    }

    // Collect all prefix mappings.
    public void startPrefixMapping(String prefix, String uri) {
        prefixes.put(prefix,uri);
    }


    public void startElement(String uri, String localName,
            String qName, Attributes attributes) throws SAXException {

        try {
            if (qName.equals(svNode)) {
                //attribute sv:name
                String nodeName = attributes.getValue(svName);
                if (noRecurse) {
                    if (!testRootDone) {
                        nodeElemStack.push(currentNodeElem);
                        testRootDone = true;
                        // correct root name?
                        if (currentNodeElem.node.getDepth()==0) {
                            checkCondition("Exported Root node has not the required " +
                                "element name 'jcr:root'.", nodeName.equals(jcrRoot));
                        }
                        // nothing else to do here
                    }
                    // rootNode yet done
                    else {
                        // only the testRootNode should be exported.
                        checkCondition("Child Node Element of testRoot Node " +
                            "element found although noRecursive is true.", !testRootDone);
                    }

                }
                else {
                    if (!testRootDone) {
                        nodeElemStack.push(currentNodeElem);
                        testRootDone = true;
                        // correct root name?
                        if (currentNodeElem.node.getDepth()==0) {
                            checkCondition("Exported Root node has not the required " +
                                "element name 'jcr:root'.", nodeName.equals(jcrRoot));
                        }
                        // nothing else to do here
                    }
                    // Collect the exported data in a NodeElemData object.
                    // every occurrence of an opened sv:node element
                    // creates such an object which will be put on the nodeElemStack.
                    // As this element will be popped from the stack when the node element
                    // is closed, the latest element on the stack represents the parent
                    // node element of the current node element.
                    else {
                        parentNodeElem = (NodeElemData) nodeElemStack.pop();

                        // get the node(s) with the found nodeName
                        NodeIterator nodeIter = parentNodeElem.node.getNodes(nodeName);

                        // create a new nodeElemData for this new node elem in process
                        currentNodeElem = new NodeElemData();
                        currentNodeElem.name = nodeName;

                        long size = getSize(nodeIter);
                        if (size >= 1) {
                            // Find the index of the child node,
                            // collect the childElems data of the parent
                            // ie for every name we count the number of child nodes with that name.
                            // Also get the child node with the correct index given by the
                            // position the node elem is found in the exported tree.
                            if (parentNodeElem.childNodeElemNames.containsKey(nodeName)) {
                                ChildNodeElem child =
                                    (ChildNodeElem) parentNodeElem.childNodeElemNames.get(nodeName);
                                child.number++;
                                currentNodeElem.index = child.number;
                                // get the node
                                String relPath = currentNodeElem.name + "[" + child.number + "]";
                                currentNodeElem.node = parentNodeElem.node.getNode(relPath);
                                currentNodeElem.path = currentNodeElem.node.getPath();
                                parentNodeElem.childNodeElemNames.put(nodeName,child);
                            }
                            else {
                                ChildNodeElem child = new ChildNodeElem();
                                child.name = nodeName;
                                child.number = 1;
                                currentNodeElem.index = child.number;
                                // get the node
                                String relPath = currentNodeElem.name + "[" + child.number + "]";
                                currentNodeElem.node = parentNodeElem.node.getNode(relPath);
                                currentNodeElem.path = currentNodeElem.node.getPath();
                                parentNodeElem.childNodeElemNames.put(nodeName,child);
                            }
                        }
                        else {
                            // no node found, this is an error.
                            checkCondition("No child node of node "+ parentNodeElem.path
                                    + " found with name:  "  + nodeName,false);
                        }
                        // push the parent data and the current node element data on the stack
                        nodeElemStack.push(parentNodeElem);
                        nodeElemStack.push(currentNodeElem);
                    }
                }
            }

            // Collect the property data found in a PropElemData object.
            // Collect the value(s) found in an ArrayList.
            else if (qName.equals(svProperty)) {
                currentPropElem = new PropElemData();
                currentPropElem.name = attributes.getValue(svName);
                currentPropElem.typeName = attributes.getValue(svType);
                currentPropElem.type = PropertyType.valueFromName(currentPropElem.typeName);
                currentPropElem.values = new ArrayList();
            }

            else if (qName.equals(svValue)) {
                // init
                currentValue = new StringBuffer();
            }
            else {
                // invalid element name is used
                checkCondition("Invalid element name " + qName
                        + " in SysView export found",false);
            }
        } catch (PathNotFoundException pne) {
                checkCondition("Item not found during exportSysViewTest: "
                        + pne.toString(), false);
        } catch (RepositoryException re) {
            // what here?
        }
    }

    // Collect the value data
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (currentValue != null)
            currentValue.append(ch, start, length);
    }


    public void endElement (String uri, String localName, String qName)
            throws SAXException {

        try {
            // The value is held in a ArrayList
            // ignoring if it is a  multivalue property or not.
            if (qName.equals(svValue)) {
                if (currentValue != null) {
                    String val = currentValue.toString();
                    if (currentPropElem.type != PropertyType.BINARY ||
                            (currentPropElem.type == PropertyType.BINARY && !skipBinary))
                        currentPropElem.values.add(val);
                }
            }

            else if (qName.equals(svProperty)) {
                // we know all props are exported before the first node
                currentNodeElem = (NodeElemData) nodeElemStack.pop();
                currentNodeElem.propElems.add(currentPropElem);
                nodeElemStack.push(currentNodeElem);
            }

            else if (qName.equals(svNode)) {
                currentNodeElem = (NodeElemData) nodeElemStack.peek();
                // now check all the stuff
                if (currentNodeElem.node == null) {
                    checkCondition("Tree structure of exported node does " +
                        "not match the tree structure of the repository.", false);
                }
                else {
                    // position of jcr:primaryType, jcr:mixinTypes and jcr:uuid if present
                    checkPropOrder(currentNodeElem);
                    // number of child nodes ok?
                    checkChildren(currentNodeElem, noRecurse);
                    // props and their values ok?
                    try {
                        checkAllProps(currentNodeElem, skipBinary);
                        // remove from the stack
                        nodeElemStack.pop();
                    }
                    catch (IOException ioe) {
                        checkCondition("Error in Base64 encoding " +
                                "of a binary property value: " + ioe.toString(), false);
                    }
                }
            }
            else {
                // invalid element name is used
                checkCondition("Invalid element name " + qName +
                        " in SysView export found",false);
            }
        } catch (PathNotFoundException pne) {
            checkCondition("Item not found during exportSysViewTest: " +
                    pne.toString(), false);
        }
        catch (RepositoryException re) {
            // what here?
        }
    }

    public void endDocument() throws SAXException {
        // check exported namespaces
        try {
            Map sessionNamespaces = new HashMap();
            String[] sessionPrefixes = session.getNamespacePrefixes();
            for (int i = 0; i < sessionPrefixes.length; i++) {
                sessionNamespaces.put(sessionPrefixes[i], session.getNamespaceURI(sessionPrefixes[i]));
            }

            // check prefixes against namespace mapping in session
            for (Iterator it = prefixes.keySet().iterator(); it.hasNext(); ) {
                String prefix = (String) it.next();
                if ("xml".equals(prefix)) {
                    Assert.fail("Prefix mapping for 'xml' must not be exported.");
                }

                String uri = (String) prefixes.get(prefix);
                checkCondition("Exported uri " + uri + " is not a registered namespace.",
                        sessionNamespaces.containsValue(uri));
                checkCondition("Exported prefix " + prefix + " does not match " +
                        "current namespacce mapping in Session",
                        sessionNamespaces.containsKey(prefix));
            }
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    // helpers for test result forward
    private void checkCondition(String str, boolean bool) {
        Assert.assertTrue(str, bool);
    }

    public class ConditionException extends SAXException {
        public ConditionException(String message) {
            super(message);
        }
    }

    //--------------helper methods to check the data ---------------------------------
    /**
     * Checks the correct position of the jcr:primarType, jcr:mixinTypes
     * and jcr:uuid in the property elements of the given nodeElem.
     * @param nodeElem
     * @throws RepositoryException
     */
    private void checkPropOrder(NodeElemData nodeElem)
            throws RepositoryException, SAXException {

        boolean jcrPrimaryTypeFound = false;
        boolean jcrMixinTypesFound = true;
        boolean uuidFound = true;
        PropElemData propElem = (PropElemData) nodeElem.propElems.get(0);
        jcrPrimaryTypeFound = (jcrPrimaryType.equals(propElem.name));
        checkCondition("Exported property jcr:primaryType of node " + nodeElem.path +
                " is not at the first position.", jcrPrimaryTypeFound);

        if (nodeElem.node.hasProperty(jcrMixinTypes)) {
            PropElemData propElem2 = (PropElemData) nodeElem.propElems.get(1);
            jcrMixinTypesFound = (jcrMixinTypes.equals(propElem2.name));
            checkCondition("Exported property jcr:jcrMixinTypes of node " + nodeElem.path +
                " is not at the second position.", jcrMixinTypesFound);

            NodeType[] mixins = nodeElem.node.getMixinNodeTypes();
            for (int j=0; j<mixins.length; j++) {
                if (mixins[j].getName().equals(mixReferenceable)) {
                uuidFound = (jcrUuid.equals(((PropElemData)nodeElem.propElems.get(2)).name));
                checkCondition("Exported property jcr:uuid of node " + nodeElem.path +
                        " is not at the third position.", uuidFound);
                }
            }
        }
    }


    /**
     * Checks the values of all exported properties of a given node.
     *
     * @param nodeElem The nodeElem of the given node.
     * @param skipBinary Boolean if the binary properties should be exported.
     * @throws RepositoryException
     * @throws SAXException
     * @throws IOException
     */
    private void checkAllProps(NodeElemData nodeElem, boolean skipBinary)
            throws RepositoryException, SAXException, IOException {

        boolean allFound = false;
        boolean correctVal = false;
        Node node = nodeElem.node;
        ArrayList propElems = nodeElem.propElems;

        // no props exported
        if (propElems.size() == 0) {
            // if node has properties they should be of Binary type and skipBinary should be true
            if (node.hasProperties()) {
                if (skipBinary) {
                    PropertyIterator iter = node.getProperties();
                    while (iter.hasNext()) {
                        Property prop = iter.nextProperty();
                        checkCondition("Property " + prop.getName() + " of node "
                            + node.getPath() + " is not exported.", prop.getType()== PropertyType.BINARY);
                    }
                }
                else {
                    checkCondition("One or more properties of node "
                        + node.getPath() + " are not exported.", false);
                }
            }
            else {
                // ok
            }
        }
        else {
            // compare the propElems with the properties of the given node
            for (int i = 0; i < propElems.size(); i++) {
                correctVal = false;
                PropElemData propElem = (PropElemData) propElems.get(i);
                int propType = propElem.type;
                if (node.hasProperty(propElem.name)) {
                    Property prop = node.getProperty(propElem.name);
                    // compare the propTypes
                    correctVal = (propType == prop.getType());
                    checkCondition("Property type of property " + propElem.name
                                + " of node " + nodeElem.path + " is not exported correctly."
                            + "expected: "+prop.getType()+" received: "+propType, correctVal);

                    // property which should be exported
                    if (propType == PropertyType.BINARY && !skipBinary ||
                            propType != PropertyType.BINARY) {
                        try {
                            int size = propElem.values.size();

                            // multivalue property with empty value array
                            if (size == 0) {
                                if (prop.getDefinition().isMultiple()) {
                                    long length = prop.getValues().length;
                                    checkCondition("Value of property " + prop.getPath() +
                                             " is not exported.", length == 0);
                                }
                                else {
                                    checkCondition("Singler value property " + prop.getPath() +
                                             " with no value is exported.", false);
                                }
                            }

                            // single value property or multivalue property with one value
                            if (size == 1) {
                                String str = "";
                                if (prop.getDefinition().isMultiple()) {
                                    str = (prop.getValues()[0]).getString();
                                }
                                else {
                                    str = prop.getString();
                                }
                                String val = (String) propElem.values.get(0);

                               if (prop.getType() == PropertyType.BINARY) {
                                    // decode value
                                    val = decodeBase64(val);
                                }
                                correctVal = (str.equals(val));
                                checkCondition("Property value of property " + propElem.name
                                    + " of node " + nodeElem.path + " is not exported correctly:" +
                                        " expected value: "+str+" found value: "+val, correctVal);
                            }

                            // multivalue property with several values
                            else {
                                Value[] vals = prop.getValues();
                                checkCondition("Number of exported values of property " +
                                        prop.getPath() + " does not match the number " +
                                        "its values", vals.length == size);
                                for (int j = 0; j < size; j++) {
                                    // we know that the order of the values
                                    // of a mulitval prop is preserved during export
                                    String val = (String)propElem.values.get(j);

                                    if (prop.getType() == PropertyType.BINARY) {
                                        // decode value
                                        val = decodeBase64(val);
                                    }
                                    String str = vals[j].getString();
                                    correctVal = (val.equals(str));
                                    checkCondition("Property value of property " + propElem.name
                                            + " of node " + nodeElem.path +
                                            " is not exported correctly.", correctVal);
                                }
                            }
                        } catch (ValueFormatException vfe ) {
                                checkCondition("Error during retreiviing the value(s)" +
                                        " of property " + prop.getPath() + vfe.toString()
                                        + " .", false);
                        }
                    }
                    // skipBinary true and propType is Binary, should be skipped
                    else {
                        checkCondition("Value of binary property "+ prop.getPath()
                                + " exported although skipBinary flag is true.",
                                propElem.values.isEmpty());
                    }
                }
                // given node has no property with the name given by the prop element
                else {
                    checkCondition("Property element " + propElem.name
                            + " found but node " + nodeElem.node.getPath() +
                            " does not have a property with that name", false);
                }
            }
            // compare the sizes here
            long otherSize = getSize(node.getProperties());
            allFound = propElems.size() == otherSize;
            checkCondition("Not all properties of node " +
                    nodeElem.path + " are exported.", allFound);
        }
    }

    /**
     * Counts the number of child nodes exported and compare with the number
     * of child nodes of a given node.
     * @param nodeElem The node to check.
     * @param noRecurse Boolean if child nodes should be exported at all.
     * @throws RepositoryException
     * @throws SAXException
     */
    private void checkChildren(NodeElemData nodeElem, boolean noRecurse)
            throws RepositoryException, SAXException {

        Hashtable childElemsFound = nodeElem.childNodeElemNames;
        boolean totalSumOk = false;
        boolean partialSumOk = true;
        if (noRecurse) {
            totalSumOk = (childElemsFound.size() == 0);
        }
        else {
            // all children found if number of node.getNodes(name) is the same as found
            // in childElemsFound and if sum(number of nodeGetNodes(names))
            // == number of node.getNodes()
            long childrenFound = 0;
            NodeIterator nodeIter = nodeElem.node.getNodes();

            long children = getSize(nodeIter);
            for (Enumeration e = childElemsFound.elements();  e.hasMoreElements();) {
                ChildNodeElem child = (ChildNodeElem) e.nextElement();
                String name = child.name;
                long number = child.number;

                NodeIterator iter = nodeElem.node.getNodes(name);
                long size = 0;

                size = getSize(iter);
                if (size != number) {
                    partialSumOk = false;
                    break;
                }
                else {
                    childrenFound += number;
                }
            }
            totalSumOk = (children == childrenFound);
            checkCondition("The number of child nodes of node" + nodeElem.path +
                    " which are exported does not match the number of its child nodes.",
                    totalSumOk && partialSumOk);
        }
    }

    // helper methods
    /**
     * Decodes Base64 encoded binary values.
     * @param str the string to decode
     * @return
     * @throws IOException
     */
    private String decodeBase64(String str) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Base64.decode(str, bos);
        String decoded = bos.toString("UTF-8");
        return decoded;
    }

    /**
     *
     * @param it
     * @return
     */
    private long getSize(RangeIterator it) {
        long size = it.getSize();
        if (size != -1) {
            return size;
        }
        size = 0;
        while (it.hasNext()) {
            it.next();
            size++;
        }
        return size;
    }

   //---------------- helper classes for collecting the xml data found ----------------

    /**
     * Node data class holding the collected data found during event processing.
     */
    private class NodeElemData {
        // Name of the node
        String name;
        // the number of the occurence of this name as child element name
        // this is then the same as the index of this node.
        long index;
        // the path of the node
        String path;
        // List of PropElemData
        ArrayList  propElems = new ArrayList();
        // the node itself
        Node node;
        // the current position of the child node in process among its same name siblings.
        // /the index of the child node in proces is therefore position+1)
        int position = 0;
        // the childNodeElems (stored are key: name and
        // value: number of the same name siblings)
        Hashtable childNodeElemNames = new Hashtable();
    }

    /**
     * Property data of the current property element.
     */
    private class PropElemData {
        String name;
        String typeName;
        int type;
        ArrayList values;
    }

    /**
     * Child node data.
     */
    private class ChildNodeElem {
        String name;
        long number;
    }
}
