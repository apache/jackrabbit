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

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.nodetype.ChildNodeDef;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.w3c.dom.Element;

/**
 * Utility class for reading and writing node type definition XML elements.
 */
class NodeTypeFormat extends CommonFormat {

    /** Name of the child node definition element. */
    private static final String CHILDNODEDEF_ELEMENT = "childNodeDef";

    /** Name of the property definition element. */
    private static final String PROPERTYDEF_ELEMENT = "propertyDef";

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
     * Creates a node type definition format object.
     *
     * @param resolver namespace resolver
     * @param element node type definition element
     * @param def node type definition
     */
    protected NodeTypeFormat(
            NamespaceResolver resolver, Element element, NodeTypeDef def) {
        super(resolver, element);
        this.def = def;
    }

    /**
     * Reads the node type definition from the XML element.
     *
     * @throws InvalidNodeTypeDefException if the format of the node type
     *                                    definition element is invalid
     */
    protected void read() throws InvalidNodeTypeDefException {
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
    protected void write() {
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
        Collection types =
            getGrandChildContents(SUPERTYPES_ELEMENT, SUPERTYPE_ELEMENT);
        if (types != null) {
            Vector vector = new Vector();

            Iterator iterator = types.iterator();
            while (iterator.hasNext()) {
                vector.add(fromJCRName((String) iterator.next()));
            }

            def.setSupertypes((QName[]) vector.toArray(new QName[0]));
        }
    }

    /**
     * Writes the supertypes of the node type definition.
     */
    private void writeSupertypes() {
        QName[] values = def.getSupertypes();
        if (values != null && values.length > 0) {
            Vector types = new Vector();
            for (int i = 0; i < values.length; i++) {
                types.add(toJCRName(values[i]));
            }
            setGrandChildContents(
                    SUPERTYPES_ELEMENT, SUPERTYPE_ELEMENT, types);
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

        Iterator iterator = getChildElements(PROPERTYDEF_ELEMENT);
        while (iterator.hasNext()) {
            PropDef property = new PropDef();
            Element element = (Element) iterator.next();
            PropDefFormat format =
                new PropDefFormat(getNamespaceResolver(), element, property);
            format.read(def.getName());
            vector.add(property);
        }

        def.setPropertyDefs((PropDef[]) vector.toArray(new PropDef[0]));
    }

    /**
     * Writes the property definitions of the node type definition.
     */
    private void writePropertyDefinitions() {
        PropDef[] defs = def.getPropertyDefs();
        for (int i = 0; i < defs.length; i++) {
            PropDef property = defs[i];
            Element element = newElement(PROPERTYDEF_ELEMENT);
            PropDefFormat format =
                new PropDefFormat(getNamespaceResolver(), element, property);
            format.write();
            addChild(element);
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

        Iterator iterator = getChildElements(CHILDNODEDEF_ELEMENT);
        while (iterator.hasNext()) {
            ChildNodeDef node = new ChildNodeDef();
            Element element = (Element) iterator.next();
            NodeDefFormat format =
                new NodeDefFormat(getNamespaceResolver(), element, node);
            format.read(def.getName());
            vector.add(node);
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
            ChildNodeDef node = defs[i];
            Element element = newElement(CHILDNODEDEF_ELEMENT);
            NodeDefFormat format =
                new NodeDefFormat(getNamespaceResolver(), element, node);
            format.write();
            addChild(element);
        }
    }

}
