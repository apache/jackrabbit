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

import org.apache.jackrabbit.core.xml.PropInfo.MultipleStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

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
    private final Stack<ImportState> stack = new Stack<ImportState>();

    /**
     * fields used temporarily while processing sv:property and sv:value elements
     */
    private Name currentPropName;
    private int currentPropType = PropertyType.UNDEFINED;
    private MultipleStatus currentPropMultipleStatus = MultipleStatus.UNKNOWN;
    // list of appendable value objects
    private ArrayList<BufferedStringValue> currentPropValues =
        new ArrayList<BufferedStringValue>();
    private BufferedStringValue currentPropValue;

    /**
     * Constructs a new <code>SysViewImportHandler</code>.
     *
     * @param importer the underlying importer
     * @param valueFactory the value factory
     */
    SysViewImportHandler(Importer importer, ValueFactory valueFactory) {
        super(importer, valueFactory);
    }

    private void processNode(ImportState state, boolean start, boolean end)
            throws SAXException {
        if (!start && !end) {
            return;
        }
        Name[] mixinNames = null;
        if (state.mixinNames != null) {
            mixinNames = state.mixinNames.toArray(
                    new Name[state.mixinNames.size()]);
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
                for (PropInfo pi : state.props) {
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
    @Override
    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes atts)
            throws SAXException {
        Name name = NameFactoryImpl.getInstance().create(namespaceURI, localName);
        // check element name
        if (name.equals(NameConstants.SV_NODE)) {
            // sv:node element

            // node name (value of sv:name attribute)
            String svName = getAttribute(atts, NameConstants.SV_NAME);
            if (svName == null) {
                throw new SAXException(new InvalidSerializedDataException(
                        "missing mandatory sv:name attribute of element sv:node"));
            }

            if (!stack.isEmpty()) {
                // process current node first
                ImportState current = stack.peek();
                // need to start current node
                if (!current.started) {
                    processNode(current, true, false);
                    current.started = true;
                }
            }

            // push new ImportState instance onto the stack
            ImportState state = new ImportState();
            try {
                state.nodeName = resolver.getQName(svName);
            } catch (NameException e) {
                throw new SAXException(new InvalidSerializedDataException("illegal node name: " + name, e));
            } catch (NamespaceException e) {
                throw new SAXException(new InvalidSerializedDataException("illegal node name: " + name, e));
            }
            stack.push(state);
        } else if (name.equals(NameConstants.SV_PROPERTY)) {
            // sv:property element

            // reset temp fields
            currentPropValues.clear();

            // property name (value of sv:name attribute)
            String svName = getAttribute(atts, NameConstants.SV_NAME);
            if (svName == null) {
                throw new SAXException(new InvalidSerializedDataException(
                        "missing mandatory sv:name attribute of element sv:property"));
            }
            try {
                currentPropName = resolver.getQName(svName);
            } catch (NameException e) {
                throw new SAXException(new InvalidSerializedDataException("illegal property name: " + name, e));
            } catch (NamespaceException e) {
                throw new SAXException(new InvalidSerializedDataException("illegal property name: " + name, e));
            }
            // property type (sv:type attribute)
            String type = getAttribute(atts, NameConstants.SV_TYPE);
            if (type == null) {
                throw new SAXException(new InvalidSerializedDataException(
                        "missing mandatory sv:type attribute of element sv:property"));
            }
            try {
                currentPropType = PropertyType.valueFromName(type);
            } catch (IllegalArgumentException e) {
                throw new SAXException(new InvalidSerializedDataException(
                        "Unknown property type: " + type, e));
            }
            // 'multi-value' hint (sv:multiple attribute)
            String multiple = getAttribute(atts, NameConstants.SV_MULTIPLE);
            if (multiple != null) {
                currentPropMultipleStatus = MultipleStatus.MULTIPLE;
            } else {
                currentPropMultipleStatus = MultipleStatus.UNKNOWN;
            }
        } else if (name.equals(NameConstants.SV_VALUE)) {
            // sv:value element
            currentPropValue = new BufferedStringValue(resolver, valueFactory);
            String xsiType = atts.getValue("xsi:type");
            currentPropValue.setBase64("xs:base64Binary".equals(xsiType));
        } else {
            throw new SAXException(new InvalidSerializedDataException(
                    "Unexpected element in system view xml document: " + name));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
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
    @Override
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        Name name = NameFactoryImpl.getInstance().create(namespaceURI, localName);
        // check element name
        ImportState state = stack.peek();
        if (name.equals(NameConstants.SV_NODE)) {
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
        } else if (name.equals(NameConstants.SV_PROPERTY)) {
            // sv:property element

            // check if all system properties (jcr:primaryType, jcr:uuid etc.)
            // have been collected and create node as necessary
            if (currentPropName.equals(NameConstants.JCR_PRIMARYTYPE)) {
                BufferedStringValue val = currentPropValues.get(0);
                String s = null;
                try {
                    s = val.retrieve();
                    state.nodeTypeName = resolver.getQName(s);
                } catch (IOException ioe) {
                    throw new SAXException("error while retrieving value", ioe);
                } catch (NameException e) {
                    throw new SAXException(new InvalidSerializedDataException("illegal node type name: " + s, e));
                } catch (NamespaceException e) {
                    throw new SAXException(new InvalidSerializedDataException("illegal node type name: " + s, e));
                }
            } else if (currentPropName.equals(NameConstants.JCR_MIXINTYPES)) {
                if (state.mixinNames == null) {
                    state.mixinNames = new ArrayList<Name>(currentPropValues.size());
                }
                for (BufferedStringValue val : currentPropValues) {
                    String s = null;
                    try {
                        s = val.retrieve();
                        Name mixin = resolver.getQName(s);
                        state.mixinNames.add(mixin);
                    } catch (IOException ioe) {
                        throw new SAXException("error while retrieving value", ioe);
                    } catch (NameException e) {
                        throw new SAXException(new InvalidSerializedDataException("illegal mixin type name: " + s, e));
                    } catch (NamespaceException e) {
                        throw new SAXException(new InvalidSerializedDataException("illegal mixin type name: " + s, e));
                    }
                }
            } else if (currentPropName.equals(NameConstants.JCR_UUID)) {
                BufferedStringValue val = currentPropValues.get(0);
                try {
                    state.uuid = val.retrieve();
                } catch (IOException ioe) {
                    throw new SAXException("error while retrieving value", ioe);
                }
            } else {
                if (currentPropMultipleStatus == MultipleStatus.UNKNOWN
                        && currentPropValues.size() != 1) {
                    currentPropMultipleStatus = MultipleStatus.MULTIPLE;
                }
                PropInfo prop = new PropInfo(
                        currentPropName,
                        currentPropType,
                        currentPropValues.toArray(new TextValue[currentPropValues.size()]),
                        currentPropMultipleStatus);
                state.props.add(prop);
            }
            // reset temp fields
            currentPropValues.clear();
        } else if (name.equals(NameConstants.SV_VALUE)) {
            // sv:value element
            currentPropValues.add(currentPropValue);
            // reset temp fields
            currentPropValue = null;
        } else {
            throw new SAXException(new InvalidSerializedDataException("invalid element in system view xml document: " + localName));
        }
    }

    //--------------------------------------------------------< inner classes >
    /**
     * The state of parsing the XML stream.
     */
    static class ImportState {
        /**
         * name of current node
         */
        Name nodeName;
        /**
         * primary type of current node
         */
        Name nodeTypeName;
        /**
         * list of mixin types of current node
         */
        List<Name> mixinNames;
        /**
         * uuid of current node
         */
        String uuid;

        /**
         * list of PropInfo instances representing properties of current node
         */
        List<PropInfo> props = new ArrayList<PropInfo>();

        /**
         * flag indicating whether startNode() has been called for current node
         */
        boolean started;
    }

    //-------------------------------------------------------------< private >

    /**
     * Returns the value of the named XML attribute.
     *
     * @param attributes set of XML attributes
     * @param name attribute name
     * @return attribute value,
     *         or <code>null</code> if the named attribute is not found
     */
    private static String getAttribute(Attributes attributes, Name name) {
        return attributes.getValue(name.getNamespaceURI(), name.getLocalName());
    }

}
