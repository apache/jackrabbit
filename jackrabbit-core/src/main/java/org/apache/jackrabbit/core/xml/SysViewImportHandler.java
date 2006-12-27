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

import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.core.NodeId;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

/**
 * <code>SysViewImportHandler</code>  ...
 */
class SysViewImportHandler extends TargetImportHandler {

    /**
     * stack of ImportState instances; an instance is pushed onto the stack
     * in the startElement method every time a sv:node element is encountered;
     * the same instance is popped from the stack in the endElement method
     * when the corresponding sv:node element is encountered.
     */
    private final Stack stack = new Stack();

    /**
     * fields used temporarily while processing sv:property and sv:value elements
     */
    private QName currentPropName;
    private int currentPropType = PropertyType.UNDEFINED;
    // list of AppendableValue objects
    private ArrayList currentPropValues = new ArrayList();
    private BufferedStringValue currentPropValue;

    /**
     * Constructs a new <code>SysViewImportHandler</code>.
     *
     * @param importer
     * @param nsContext
     */
    SysViewImportHandler(Importer importer) {
        super(importer);
    }

    private void processNode(ImportState state, boolean start, boolean end)
            throws SAXException {
        if (!start && !end) {
            return;
        }
        QName[] mixinNames = null;
        if (state.mixinNames != null) {
            mixinNames = (QName[]) state.mixinNames.toArray(
                    new QName[state.mixinNames.size()]);
        }
        NodeId id = null;
        if (state.uuid != null) {
            id = NodeId.valueOf(state.uuid);
        }
        NodeInfo node =
            new NodeInfo(state.nodeName, state.nodeTypeName, mixinNames, id);
        // call Importer
        try {
            if (start) {
                importer.startNode(node, state.props);
                // dispose temporary property values
                for (Iterator iter = state.props.iterator(); iter.hasNext();) {
                    PropInfo pi = (PropInfo) iter.next();
                    pi.dispose();
                }

            }
            if (end) {
                importer.endNode(node);
            }
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    //-------------------------------------------------------< ContentHandler >

    /**
     * {@inheritDoc}
     */
    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes atts)
            throws SAXException {
        // check namespace
        if (!QName.NS_SV_URI.equals(namespaceURI)) {
            throw new SAXException(new InvalidSerializedDataException("invalid namespace for element in system view xml document: "
                    + namespaceURI));
        }
        // check element name
        if (SysViewSAXEventGenerator.NODE_ELEMENT.equals(localName)) {
            // sv:node element

            // node name (value of sv:name attribute)
            String name = atts.getValue(SysViewSAXEventGenerator.PREFIXED_NAME_ATTRIBUTE);
            if (name == null) {
                throw new SAXException(new InvalidSerializedDataException(
                        "missing mandatory sv:name attribute of element sv:node"));
            }

            if (!stack.isEmpty()) {
                // process current node first
                ImportState current = (ImportState) stack.peek();
                // need to start current node
                if (!current.started) {
                    processNode(current, true, false);
                    current.started = true;
                }
            }

            // push new ImportState instance onto the stack
            ImportState state = new ImportState();
            try {
                state.nodeName = NameFormat.parse(name, nsContext);
            } catch (NameException e) {
                throw new SAXException(new InvalidSerializedDataException("illegal node name: " + name, e));
            }
            stack.push(state);
        } else if (SysViewSAXEventGenerator.PROPERTY_ELEMENT.equals(localName)) {
            // sv:property element

            // reset temp fields
            currentPropValues.clear();

            // property name (value of sv:name attribute)
            String name = atts.getValue(SysViewSAXEventGenerator.PREFIXED_NAME_ATTRIBUTE);
            if (name == null) {
                throw new SAXException(new InvalidSerializedDataException(
                        "missing mandatory sv:name attribute of element sv:property"));
            }
            try {
                currentPropName = NameFormat.parse(name, nsContext);
            } catch (NameException e) {
                throw new SAXException(new InvalidSerializedDataException("illegal property name: " + name, e));
            }
            // property type (sv:type attribute)
            String type = atts.getValue(SysViewSAXEventGenerator.PREFIXED_TYPE_ATTRIBUTE);
            if (type == null) {
                throw new SAXException(new InvalidSerializedDataException(
                        "missing mandatory sv:type attribute of element sv:property"));
            }
            currentPropType = PropertyType.valueFromName(type);
        } else if (SysViewSAXEventGenerator.VALUE_ELEMENT.equals(localName)) {
            // sv:value element

            // reset temp fields
            currentPropValue = new BufferedStringValue(nsContext);
        } else {
            throw new SAXException(new InvalidSerializedDataException("unexpected element found in system view xml document: "
                    + localName));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (currentPropValue != null) {
            // property value (character data of sv:value element)
            try {
                currentPropValue.append(ch, start, length);
            } catch (IOException ioe) {
                throw new SAXException("error while processing property value",
                        ioe);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException {
        if (currentPropValue != null) {
            // property value

            // data reported by the ignorableWhitespace event within
            // sv:value tags is considered part of the value
            try {
                currentPropValue.append(ch, start, length);
            } catch (IOException ioe) {
                throw new SAXException("error while processing property value",
                        ioe);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        // check element name
        ImportState state = (ImportState) stack.peek();
        if (SysViewSAXEventGenerator.NODE_ELEMENT.equals(localName)) {
            // sv:node element
            if (!state.started) {
                // need to start & end current node
                processNode(state, true, true);
                state.started = true;
            } else {
                // need to end current node
                processNode(state, false, true);
            }
            // pop current state from stack
            stack.pop();
        } else if (SysViewSAXEventGenerator.PROPERTY_ELEMENT.equals(localName)) {
            // sv:property element

            // check if all system properties (jcr:primaryType, jcr:uuid etc.)
            // have been collected and create node as necessary
            if (currentPropName.equals(QName.JCR_PRIMARYTYPE)) {
                BufferedStringValue val = (BufferedStringValue) currentPropValues.get(0);
                String s = null;
                try {
                    s = val.retrieve();
                    state.nodeTypeName = NameFormat.parse(s, nsContext);
                } catch (IOException ioe) {
                    throw new SAXException("error while retrieving value", ioe);
                } catch (NameException e) {
                    throw new SAXException(new InvalidSerializedDataException("illegal node type name: " + s, e));
                }
            } else if (currentPropName.equals(QName.JCR_MIXINTYPES)) {
                if (state.mixinNames == null) {
                    state.mixinNames = new ArrayList(currentPropValues.size());
                }
                for (int i = 0; i < currentPropValues.size(); i++) {
                    BufferedStringValue val =
                            (BufferedStringValue) currentPropValues.get(i);
                    String s = null;
                    try {
                        s = val.retrieve();
                        QName mixin = NameFormat.parse(s, nsContext);
                        state.mixinNames.add(mixin);
                    } catch (IOException ioe) {
                        throw new SAXException("error while retrieving value", ioe);
                    } catch (NameException e) {
                        throw new SAXException(new InvalidSerializedDataException("illegal mixin type name: " + s, e));
                    }
                }
            } else if (currentPropName.equals(QName.JCR_UUID)) {
                BufferedStringValue val = (BufferedStringValue) currentPropValues.get(0);
                try {
                    state.uuid = val.retrieve();
                } catch (IOException ioe) {
                    throw new SAXException("error while retrieving value", ioe);
                }
            } else {
                PropInfo prop = new PropInfo(
                        currentPropName, currentPropType,
                        (TextValue[]) currentPropValues.toArray(
                                new TextValue[currentPropValues.size()]));
                state.props.add(prop);
            }
            // reset temp fields
            currentPropValues.clear();
        } else if (SysViewSAXEventGenerator.VALUE_ELEMENT.equals(localName)) {
            // sv:value element
            currentPropValues.add(currentPropValue);
            // reset temp fields
            currentPropValue = null;
        } else {
            throw new SAXException(new InvalidSerializedDataException("invalid element in system view xml document: " + localName));
        }
    }

    //--------------------------------------------------------< inner classes >
    class ImportState {
        /**
         * name of current node
         */
        QName nodeName;
        /**
         * primary type of current node
         */
        QName nodeTypeName;
        /**
         * list of mixin types of current node
         */
        ArrayList mixinNames;
        /**
         * uuid of current node
         */
        String uuid;

        /**
         * list of PropInfo instances representing properties of current node
         */
        ArrayList props = new ArrayList();

        /**
         * flag indicating whether startNode() has been called for current node
         */
        boolean started = false;
    }
}
