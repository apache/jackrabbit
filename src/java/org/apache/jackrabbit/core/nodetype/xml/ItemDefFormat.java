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

import javax.jcr.version.OnParentVersionAction;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.nodetype.ChildItemDef;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.w3c.dom.Element;

/**
 * Common functionality shared by the property and node definition format
 * classes.
 */
class ItemDefFormat extends CommonFormat {

    /** Name of the <code>autoCreate</code> attribute. */
    private static final String AUTOCREATE_ATTRIBUTE = "autoCreate";

    /** Name of the <code>mandatory</code> attribute. */
    private static final String MANDATORY_ATTRIBUTE = "mandatory";

    /** Name of the <code>onParentVersion</code> attribute. */
    private static final String ONPARENTVERSION_ATTRIBUTE = "onParentVersion";

    /** Name of the <code>protected</code> attribute. */
    private static final String PROTECTED_ATTRIBUTE = "protected";

    /** The child item definition. */
    private final ChildItemDef def;

    /**
     * Creates a child item definition format object.
     *
     * @param resolver namespace resolver
     * @param element child item definition element
     * @param def child item definition
     */
    protected ItemDefFormat(
            NamespaceResolver resolver, Element element, ChildItemDef def) {
        super(resolver, element);
        this.def = def;
    }

    /**
     * Reads the item definition from the XML element.
     *
     * @throws InvalidNodeTypeDefException if the format of the item
     *                                    definition element is invalid
     */
    protected void read() throws InvalidNodeTypeDefException {
        readName();
        readAutoCreate();
        readMandatory();
        readOnParentVersion();
        readProtected();
    }

    /**
     * Writes the item definition to the XML element.
     */
    protected void write() throws RepositoryException{
        writeName();
        writeAutoCreate();
        writeMandatory();
        writeOnParentVersion();
        writeProtected();
    }

    /**
     * Reads and sets the name of the item definition.
     *
     * @throws InvalidNodeTypeDefException if the format of the item
     *                                    definition element is invalid
     */
    private void readName() throws InvalidNodeTypeDefException {
        def.setName(getName());
    }

    /**
     * Writes the name of the item definition.
     */
    private void writeName() {
        setName(def.getName());
    }

    /**
     * Reads and sets the <code>autoCreate</code> attribute of the
     * item definition.
     *
     * @throws InvalidNodeTypeDefException if the format of the item
     *                                    definition element is invalid
     */
    private void readAutoCreate() throws InvalidNodeTypeDefException {
        String value = getAttribute(AUTOCREATE_ATTRIBUTE);
        def.setAutoCreate(Boolean.valueOf(value).booleanValue());
    }

    /**
     * Writes the <code>autoCreate</code> attribute of the item
     * definition.
     */
    private void writeAutoCreate() {
        String value = Boolean.toString(def.isAutoCreate());
        setAttribute(AUTOCREATE_ATTRIBUTE, value);
    }

    /**
     * Reads and sets the <code>mandatory</code> attribute of the
     * item definition.
     *
     * @throws InvalidNodeTypeDefException if the format of the item
     *                                    definition element is invalid
     */
    private void readMandatory() throws InvalidNodeTypeDefException {
        String value = getAttribute(MANDATORY_ATTRIBUTE);
        def.setMandatory(Boolean.valueOf(value).booleanValue());
    }

    /**
     * Writes the <code>mandatory</code> attribute of the item definition.
     */
    private void writeMandatory() {
        String value = Boolean.toString(def.isMandatory());
        setAttribute(MANDATORY_ATTRIBUTE, value);
    }

    /**
     * Reads and sets the <code>onParentVersion</code> attribute of the
     * item definition.
     *
     * @throws InvalidNodeTypeDefException if the format of the item
     *                                    definition element is invalid
     */
    private void readOnParentVersion() throws InvalidNodeTypeDefException {
        String value = getAttribute(ONPARENTVERSION_ATTRIBUTE);
        def.setOnParentVersion(OnParentVersionAction.valueFromName(value));
    }

    /**
     * Writes the <code>onParentVersion</code> attribute of the item
     * definition.
     */
    private void writeOnParentVersion() {
        String value =
            OnParentVersionAction.nameFromValue(def.getOnParentVersion());
        setAttribute(ONPARENTVERSION_ATTRIBUTE, value);
    }

    /**
     * Reads and sets the <code>protected</code> attribute of the
     * item definition.
     *
     * @throws InvalidNodeTypeDefException if the format of the item
     *                                    definition element is invalid
     */
    private void readProtected() throws InvalidNodeTypeDefException {
        String value = getAttribute(PROTECTED_ATTRIBUTE);
        def.setProtected(Boolean.valueOf(value).booleanValue());
    }

    /**
     * Writes the <code>protected</code> attribute of the item definition.
     */
    private void writeProtected() {
        String value = Boolean.toString(def.isProtected());
        setAttribute(PROTECTED_ATTRIBUTE, value);
    }

}
