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

import java.util.Vector;

import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.nodetype.ChildNodeDef;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.w3c.dom.Element;

import javax.jcr.RepositoryException;

/**
 * Utility class for reading and writing node definition XML elements.
 */
class NodeDefFormat extends ItemDefFormat {

    /** Name of the required primary types element. */
    private static final String REQUIREDPRIMARYTYPES_ELEMENT =
        "requiredPrimaryTypes";

    /** Name of the required primary type element. */
    private static final String REQUIREDPRIMARYTYPE_ELEMENT =
        "requiredPrimaryType";

    /** Name of the default primary type attribute. */
    private static final String DEFAULTPRIMARYTYPE_ATTRIBUTE =
        "defaultPrimaryType";

    /** Name of the <code>sameNameSibs</code> attribute. */
    private static final String SAMENAMESIBS_ATTRIBUTE = "sameNameSibs";

    /** The node definition. */
    private final ChildNodeDef def;

    /**
     * Creates a node definition format object.
     *
     * @param resolver namespace resolver
     * @param element node definition element
     * @param def node definition
     */
    protected NodeDefFormat(
            NamespaceResolver resolver, Element element, ChildNodeDef def) {
        super(resolver, element, def);
        this.def = def;
    }

    /**
     * Reads the node definition from the XML element.
     *
     * @param type name of the declaring node type
     * @throws InvalidNodeTypeDefException if the format of the node
     *                                    definition element is invalid
     */
    protected void read(QName type) throws InvalidNodeTypeDefException {
        def.setDeclaringNodeType(type);
        super.read();
        readRequiredPrimaryTypes();
        readDefaultPrimaryType();
        readSameNameSibs();
    }

    /**
     * Writes the node definition to the XML element.
     */
    protected void write() throws RepositoryException {
        super.write();
        writeRequiredPrimaryTypes();
        writeDefaultPrimaryType();
        writeSameNameSibs();
    }

    /**
     * Reads and sets the required primary types of the node definition.
     *
     * @throws InvalidNodeTypeDefException if the format of the node
     *                                    definition element is invalid
     */
    private void readRequiredPrimaryTypes() throws InvalidNodeTypeDefException {
        String[] types = getGrandChildContents(
                REQUIREDPRIMARYTYPES_ELEMENT, REQUIREDPRIMARYTYPE_ELEMENT);
        if (types != null) {
            Vector vector = new Vector();
            for (int i = 0; i < types.length; i++) {
                vector.add(fromJCRName(types[i]));
            }
            def.setRequiredPrimaryTypes((QName[]) vector.toArray(new QName[0]));
        } else {
            /* Default to nt:base?
            throw new InvalidNodeTypeDefException(
                    "Required primary type(s) not defined for child node "
                    + def.getName() + " of node type "
                    + def.getDeclaringNodeType());
            */
        }
    }

    /**
     * Writes the required primary types of the node definition.
     */
    private void writeRequiredPrimaryTypes() {
        QName[] values = def.getRequiredPrimaryTypes();
        Vector types = new Vector();
        for (int i = 0; i < values.length; i++) {
            types.add(toJCRName(values[i]));
        }
        setGrandChildContents(
                REQUIREDPRIMARYTYPES_ELEMENT, REQUIREDPRIMARYTYPE_ELEMENT,
                types);
    }

    /**
     * Reads and sets the default primary type of the node definition.
     *
     * @throws InvalidNodeTypeDefException if the format of the node
     *                                    definition element is invalid
     */
    private void readDefaultPrimaryType() throws InvalidNodeTypeDefException {
        String value = getAttribute(DEFAULTPRIMARYTYPE_ATTRIBUTE);
        if (value.length() > 0) {
            def.setDefaultPrimaryType(fromJCRName(value));
        }
    }

    /**
     * Writes the default primary type of the node definition.
     */
    private void writeDefaultPrimaryType() {
        QName type = def.getDefaultPrimaryType();
        if (type != null) {
            setAttribute(DEFAULTPRIMARYTYPE_ATTRIBUTE, toJCRName(type));
        } else {
            setAttribute(DEFAULTPRIMARYTYPE_ATTRIBUTE, ""); // Is this legal?
        }
    }

    /**
     * Reads and sets the <code>sameNameSibs</code> attribute of the
     * node definition.
     *
     * @throws InvalidNodeTypeDefException if the format of the node
     *                                    definition element is invalid
     */
    private void readSameNameSibs() throws InvalidNodeTypeDefException {
        String value = getAttribute(SAMENAMESIBS_ATTRIBUTE);
        def.setAllowSameNameSibs(Boolean.valueOf(value).booleanValue());
    }

    /**
     * Writes the <code>sameNameSibs</code> attribute of the node definition.
     */
    private void writeSameNameSibs() {
        String value = Boolean.toString(def.allowSameNameSibs());
        setAttribute(SAMENAMESIBS_ATTRIBUTE, value);
    }

}
