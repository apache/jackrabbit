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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * <code>SysViewImportHandler</code>  ...
 */
class SysViewImportHandler extends TargetImportHandler {

    /** Local part of <code>sv:node</code>. */
    private static final String NODE = "node";

    /** Local part of <code>sv:property</code>. */
    private static final String PROPERTY = "property";

    /** Local part of <code>sv:value</code>. */
    private static final String VALUE = "value";

    /** Local part of <code>sv:name</code>. */
    private static final String NAME = "name";

    /** Local part of <code>sv:type</code>. */
    private static final String TYPE = "type";

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
    // list of AppendableValue objects
    private final List<Importer.TextValue> currentPropValues = new ArrayList<Importer.TextValue>();
    private AppendableValue currentPropValue;

    /**
     * Constructs a new <code>SysViewImportHandler</code>.
     *
     * @param importer
     * @param resolver
     */
    SysViewImportHandler(Importer importer, NamePathResolver resolver) {
        super(importer, resolver);
    }

    private void processNode(ImportState state, boolean start, boolean end)
            throws SAXException {
        if (!start && !end) {
            return;
        }
        Name[] mixins = null;
        if (state.mixinNames != null) {
            mixins = state.mixinNames.toArray(new Name[state.mixinNames.size()]);
        }
        Importer.NodeInfo nodeInfo = new Importer.NodeInfo(state.nodeName, state.nodeTypeName, mixins, state.uuid);

        if (state.uuid != null) {
            nodeInfo.setUUID(state.uuid);
        }
        // call Importer
        try {
            if (start) {
                importer.startNode(nodeInfo, state.props, resolver);
                // dispose temporary property values
                for (Importer.PropInfo pi : state.props) {
                    disposePropertyValues(pi);
                }
            }
            if (end) {
                importer.endNode(nodeInfo);
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
        // check namespace
        if (!Name.NS_SV_URI.equals(namespaceURI)) {
            throw new SAXException(new InvalidSerializedDataException("invalid namespace for element in system view xml document: "
                    + namespaceURI));
        }
        // check element name
        if (NODE.equals(localName)) {
            // sv:node element

            // node name (value of sv:name attribute)
            String name = atts.getValue(Name.NS_SV_URI, NAME);
            if (name == null) {
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
                state.nodeName = resolver.getQName(name);
            } catch (NameException e) {
                throw new SAXException(new InvalidSerializedDataException("illegal node name: " + name, e));
            } catch (NamespaceException e) {
                throw new SAXException(new InvalidSerializedDataException("illegal node name: " + name, e));
            }
            stack.push(state);
        } else if (PROPERTY.equals(localName)) {
            // sv:property element

            // reset temp fields
            currentPropValues.clear();

            // property name (value of sv:name attribute)
            String name = atts.getValue(Name.NS_SV_URI, NAME);
            if (name == null) {
                throw new SAXException(new InvalidSerializedDataException(
                        "missing mandatory sv:name attribute of element sv:property"));
            }
            try {
                currentPropName = resolver.getQName(name);
            } catch (NameException e) {
                throw new SAXException(new InvalidSerializedDataException("illegal property name: " + name, e));
            } catch (NamespaceException e) {
                throw new SAXException(new InvalidSerializedDataException("illegal property name: " + name, e));
            }
            // property type (sv:type attribute)
            String type = atts.getValue(Name.NS_SV_URI, TYPE);
            if (type == null) {
                throw new SAXException(new InvalidSerializedDataException(
                        "missing mandatory sv:type attribute of element sv:property"));
            }
            currentPropType = PropertyType.valueFromName(type);
        } else if (VALUE.equals(localName)) {
            // sv:value element

            // reset temp fields
            currentPropValue = new BufferedStringValue();
        } else {
            throw new SAXException(new InvalidSerializedDataException("unexpected element found in system view xml document: "
                    + localName));
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
        // check element name
        ImportState state = stack.peek();
        if (NODE.equals(localName)) {
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
        } else if (PROPERTY.equals(localName)) {
            // sv:property element

            // check if all system properties (jcr:primaryType, jcr:uuid etc.)
            // have been collected and create node as necessary
            if (currentPropName.equals(NameConstants.JCR_PRIMARYTYPE)) {
                AppendableValue val = (AppendableValue) currentPropValues.get(0);
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
                for (int i = 0; i < currentPropValues.size(); i++) {
                    AppendableValue val =
                            (AppendableValue) currentPropValues.get(i);
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
                AppendableValue val = (AppendableValue) currentPropValues.get(0);
                try {
                    state.uuid = val.retrieve();
                } catch (IOException ioe) {
                    throw new SAXException("error while retrieving value", ioe);
                }
            } else {
                Importer.TextValue[] values = currentPropValues.toArray(new Importer.TextValue[currentPropValues.size()]);
                Importer.PropInfo prop = new Importer.PropInfo(currentPropName, currentPropType, values);
                state.props.add(prop);
            }
            // reset temp fields
            currentPropValues.clear();
        } else if (VALUE.equals(localName)) {
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
        List<Importer.PropInfo> props = new ArrayList<Importer.PropInfo>();

        /**
         * flag indicating whether startNode() has been called for current node
         */
        boolean started = false;
    }
}
