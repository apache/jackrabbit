/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.nodetype.xml;

import java.util.Iterator;
import java.util.Vector;

import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.nodetype.ChildNodeDef;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.jdom.Element;

/**
 * Utility class for reading and writing node type definition XML elements.
 */
public class NodeTypeFormat extends CommonFormat {

    /** Name of the node type definition element. */
    public static final String NODETYPE_ELEMENT = "nodeType";

    /** Name of the <code>isMixin</code> attribute. */
    private static final String ISMIXIN_ATTRIBUTE = "isMixin";

    /** Name of the <code>hasOrderableChildNodes</code> attribute. */
    private static final String HASORDERABLECHILDNODES_ATTRIBUTE =
        "hasOrderableChildNodes";

    /** Name of the primary item name attribute. */
    private static final String PRIMARYITEMNAME_ATTRIBUTE = "primaryItemName";

    /** Name of the supertypes element. */
    private static final String SUPERTYPES_ELEMENT = "supertypes";

    /** Name of the supertype element. */
    private static final String SUPERTYPE_ELEMENT = "supertype";


    /** The node type definition. */
    private final NodeTypeDef def;

    /**
     * Creates a node type definition format object. This constructor
     * is used internally by the public reader and writer constructors.
     *
     * @param resolver namespace resolver
     * @param element node type definition element
     * @param def node type definition
     */
    private NodeTypeFormat(
            NamespaceResolver resolver, Element element, NodeTypeDef def) {
        super(resolver, element);
        this.def = def;
    }

    /**
     * Creates a node type definition reader. An empty node type definition
     * instance is created. The instance properties are filled in by the
     * {@link #read() read} method.
     *
     * @param resolver namespace resolver
     * @param element node type definition element
     */
    public NodeTypeFormat(NamespaceResolver resolver, Element element) {
        this(resolver, element, new NodeTypeDef());
    }

    /**
     * Creates a node type definition writer. The node type definition
     * element is instantiated as an empty <code>nodeType</code> element.
     * The element is filled in by the {@link #write() write} method.
     *
     * @param resolver namespace resolver
     * @param def node type definition
     */
    public NodeTypeFormat(NamespaceResolver resolver, NodeTypeDef def) {
        this(resolver, new Element(NODETYPE_ELEMENT), def);
    }

    /**
     * Returns the node type definition object.
     *
     * @return node type definition
     */
    public NodeTypeDef getNodeType() {
        return def;
    }

    /**
     * Reads the node type definition from the XML element.
     *
     * @throws InvalidNodeTypeDefException if the format of the node type
     *                                    definition element is invalid
     */
    public void read() throws InvalidNodeTypeDefException {
        readName();
        readSupertypes();
        readIsMixin();
        readOrderableChildNodes();
        readPrimaryItemName();
        readPropertyDefinitions();
        readChildNodeDefinitions();
    }

    /**
     * Writes the node type definition to the XML element.
     */
    public void write() {
        writeName();
        writeSupertypes();
        writeIsMixin();
        writeOrderableChildNodes();
        writePrimaryItemName();
        writePropertyDefinitions();
        writeChildNodeDefinitions();
    }

    /**
     * Reads and sets the name of the node type definition.
     *
     * @throws InvalidNodeTypeDefException if the format of the node type
     *                                    definition element is invalid
     */
    private void readName() throws InvalidNodeTypeDefException {
        def.setName(getName());
    }

    /**
     * Writes the name of the node type definition.
     */
    private void writeName() {
        setName(def.getName());
    }

    /**
     * Reads and sets the supertypes of the node type definition.
     *
     * @throws InvalidNodeTypeDefException if the format of the node type
     *                                    definition element is invalid
     */
    private void readSupertypes() throws InvalidNodeTypeDefException {
        Vector vector = new Vector();

        Element types = getChild(SUPERTYPES_ELEMENT);
        if (types != null) {
            Iterator iterator = types.getChildren(SUPERTYPE_ELEMENT).iterator();
            while (iterator.hasNext()) {
                Element type = (Element) iterator.next();
                vector.add(fromJCRName(type.getTextTrim()));
            }
        }

        def.setSupertypes((QName[]) vector.toArray(new QName[0]));
    }

    /**
     * Writes the supertypes of the node type definition.
     */
    private void writeSupertypes() {
        QName[] values = def.getSupertypes();
        if (values.length > 0) {
            Element types = new Element(SUPERTYPES_ELEMENT);
            for (int i = 0; i < values.length; i++) {
                Element type = new Element(SUPERTYPE_ELEMENT);
                type.setText(toJCRName(values[i]));
                types.addContent(type);
            }
            addChild(types);
        }
    }

    /**
     * Reads and sets the <code>isMixin</code> attribute of the
     * node type definition.
     *
     * @throws InvalidNodeTypeDefException if the format of the node type
     *                                    definition element is invalid
     */
    private void readIsMixin() throws InvalidNodeTypeDefException {
        String value = getAttribute(ISMIXIN_ATTRIBUTE);
        def.setMixin(Boolean.valueOf(value).booleanValue());
    }

    /**
     * Writes the <code>isMixin</code> attribute of the node type definition.
     */
    private void writeIsMixin() {
        String value = Boolean.toString(def.isMixin());
        setAttribute(ISMIXIN_ATTRIBUTE, value);
    }

    /**
     * Reads and sets the <code>hasOrderableChildNodes</code> attribute
     * of the node type definition.
     *
     * @throws InvalidNodeTypeDefException if the format of the node type
     *                                    definition element is invalid
     */
    private void readOrderableChildNodes() throws InvalidNodeTypeDefException {
        String value = getAttribute(HASORDERABLECHILDNODES_ATTRIBUTE);
        def.setOrderableChildNodes(Boolean.valueOf(value).booleanValue());
    }

    /**
     * Writes the <code>hasOrderableChildNodes</code> attribute of
     * the node type definition.
     */
    private void writeOrderableChildNodes() {
        String value = Boolean.toString(def.hasOrderableChildNodes());
        setAttribute(HASORDERABLECHILDNODES_ATTRIBUTE, value);
    }

    /**
     * Reads and sets the primary item name of the node type definition.
     *
     * @throws InvalidNodeTypeDefException if the format of the node type
     *                                    definition element is invalid
     */
    private void readPrimaryItemName() throws InvalidNodeTypeDefException {
        String value = getAttribute(PRIMARYITEMNAME_ATTRIBUTE);
        if (value.length() > 0) {
            def.setPrimaryItemName(fromJCRName(value));
        }
    }

    /**
     * Writes the primary item name of the node type definition.
     */
    private void writePrimaryItemName() {
        QName name = def.getPrimaryItemName();
        if (name == null) {
            setAttribute(PRIMARYITEMNAME_ATTRIBUTE, "");
        } else {
            setAttribute(PRIMARYITEMNAME_ATTRIBUTE, toJCRName(name));
        }
    }

    /**
     * Reads and sets the property definitions of the node type definition.
     * <p>
     * Note that the {@link #readName() readName} method must have been
     * called before this method.
     *
     * @throws InvalidNodeTypeDefException if the format of the node type
     *                                    definition element is invalid
     */
    private void readPropertyDefinitions() throws InvalidNodeTypeDefException {
        Vector vector = new Vector();

        Iterator iterator = getChildIterator(PropDefFormat.PROPERTYDEF_ELEMENT);
        while (iterator.hasNext()) {
            PropDefFormat format = new PropDefFormat(
                    getNamespaceResolver(), (Element) iterator.next());
            format.read(def.getName());
            vector.add(format.getPropDef());
        }

        def.setPropertyDefs((PropDef[]) vector.toArray(new PropDef[0]));
    }

    /**
     * Writes the property definitions of the node type definition.
     */
    private void writePropertyDefinitions() {
        PropDef[] defs = def.getPropertyDefs();
        for (int i = 0; i < defs.length; i++) {
            PropDefFormat format =
                new PropDefFormat(getNamespaceResolver(), defs[i]);
            format.write();
            addChild(format.getElement());
        }
    }

    /**
     * Reads and sets the child node definitions of the node type definition.
     * <p>
     * Note that the {@link #readName() readName} method must have been
     * called before this method.
     *
     * @throws InvalidNodeTypeDefException if the format of the node type
     *                                    definition element is invalid
     */
    private void readChildNodeDefinitions() throws InvalidNodeTypeDefException {
        Vector vector = new Vector();

        Iterator iterator =
            getChildIterator(NodeDefFormat.CHILDNODEDEF_ELEMENT);
        while (iterator.hasNext()) {
            NodeDefFormat format = new NodeDefFormat(
                    getNamespaceResolver(), (Element) iterator.next());
            format.read(def.getName());
            vector.add(format.getNodeDef());
        }

        def.setChildNodeDefs(
                (ChildNodeDef[]) vector.toArray(new ChildNodeDef[0]));
    }

    /**
     * Writes the child node definitions of the node type definition.
     */
    private void writeChildNodeDefinitions() {
        ChildNodeDef[] defs = def.getChildNodeDefs();
        for (int i = 0; i < defs.length; i++) {
            NodeDefFormat format =
                new NodeDefFormat(getNamespaceResolver(), defs[i]);
            format.write();
            addChild(format.getElement());
        }
    }

}
