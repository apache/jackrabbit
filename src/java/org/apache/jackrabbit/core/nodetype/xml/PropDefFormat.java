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

import javax.jcr.PropertyType;

import org.apache.jackrabbit.core.InternalValue;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.nodetype.InvalidConstraintException;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.ValueConstraint;
import org.jdom.Element;

/**
 * Utility class for reading and writing property definition XML elements.
 */
class PropDefFormat extends ItemDefFormat {

    /** Name of the property definition element. */
    public static final String PROPERTYDEF_ELEMENT = "propertyDef";

    /** Name of the required type attribute. */
    private static final String REQUIREDTYPE_ATTRIBUTE = "requiredType";

    /** Name of the value constraints element. */
    private static final String VALUECONSTRAINTS_ELEMENT = "valueConstraints";

    /** Name of the value constraint element. */
    private static final String VALUECONSTRAINT_ELEMENT = "valueConstraint";

    /** Name of the default values element. */
    private static final String DEFAULTVALUES_ELEMENT = "defaultValues";

    /** Name of the default value element. */
    private static final String DEFAULTVALUE_ELEMENT = "defaultValue";

    /** Name of the <code>multiple</code> attribute. */
    private static final String MULTIPLE_ATTRIBUTE = "multiple";

    /** The property definition. */
    private final PropDef def;

    /**
     * Creates a property definition format object. This constructor
     * is used internally by the public reader and writer constructors.
     *
     * @param resolver namespace resolver
     * @param element property definition element
     * @param def property definition
     */
    private PropDefFormat(
            NamespaceResolver resolver, Element element, PropDef def) {
        super(resolver, element, def);
        this.def = def;
    }

    /**
     * Creates a property definition reader. The internal property
     * definition instance is created using the given node type name
     * as the name of the declaring node type.
     *
     * @param resolver namespace resolver
     * @param element property definition element
     */
    public PropDefFormat(NamespaceResolver resolver, Element element) {
        this(resolver, element, new PropDef());
    }

    /**
     * Creates a property definition writer. The internal property
     * definition element is instantiated as an empty <code>propertyDef</code>
     * element.
     *
     * @param resolver namespace resolver
     * @param def property definition
     */
    public PropDefFormat(NamespaceResolver resolver, PropDef def) {
        this(resolver, new Element(PROPERTYDEF_ELEMENT), def);
    }

    /**
     * Returns the property definition instance.
     *
     * @return property definition
     */
    public PropDef getPropDef() {
        return def;
    }

    /**
     * Reads the property definition from the XML element.
     *
     * @param type name of the declaring node type
     * @throws InvalidNodeTypeDefException if the format of the property
     *                                    definition element is invalid
     */
    public void read(QName type) throws InvalidNodeTypeDefException {
        def.setDeclaringNodeType(type);
        super.read();
        readRequiredType();
        readValueConstraints();
        readDefaultValues();
        readMultiple();
    }

    /**
     * Writes the property definition to the XML element.
     */
    public void write() {
        super.write();
        writeRequiredType();
        writeValueConstraints();
        writeDefaultValues();
        writeMultiple();
    }

    /**
     * Reads and sets the required type of the property definition.
     *
     * @throws InvalidNodeTypeDefException if the format of the property
     *                                    definition element is invalid
     */
    private void readRequiredType() throws InvalidNodeTypeDefException {
        String value = getAttribute(REQUIREDTYPE_ATTRIBUTE);
        def.setRequiredType(PropertyType.valueFromName(value));
    }

    /**
     * Writes the required type of the property definition.
     */
    private void writeRequiredType() {
        String value = PropertyType.nameFromValue(def.getRequiredType());
        setAttribute(REQUIREDTYPE_ATTRIBUTE, value);
    }

    /**
     * Reads and sets the value constraints of the property definition.
     *
     * @throws InvalidNodeTypeDefException if the format of the property
     *                                    definition element is invalid
     */
    private void readValueConstraints() throws InvalidNodeTypeDefException {
        Vector vector = new Vector();

        Element constraints = getChild(VALUECONSTRAINTS_ELEMENT);
        if (constraints != null) {
            int type = def.getRequiredType();

            Iterator iterator =
                constraints.getChildren(VALUECONSTRAINT_ELEMENT).iterator();
            while (iterator.hasNext()) {
                Element constraint = (Element) iterator.next();
                String value = constraint.getTextTrim();
                try {
                    vector.add(ValueConstraint.create(
                            type, value, getNamespaceResolver()));
                } catch (InvalidConstraintException e) {
                    throw new InvalidNodeTypeDefException(
                            "Invalid property value constraint " + value, e);
                }
            }
        }

        def.setValueConstraints(
                (ValueConstraint[]) vector.toArray(new ValueConstraint[0]));
    }

    /**
     * Writes the value constraints of the property definition.
     */
    private void writeValueConstraints() {
        Element values = new Element(VALUECONSTRAINTS_ELEMENT);

        ValueConstraint[] constraints = def.getValueConstraints();
        for (int i = 0; i < constraints.length; i++) {
            Element value = new Element(VALUECONSTRAINT_ELEMENT);
            value.setText(constraints[i].getDefinition());
            values.addContent(value);
        }

        addChild(values);
    }

    /**
     * Reads and sets the default values of the property definition.
     */
    private void readDefaultValues() {
        Vector vector = new Vector();

        Element values = getChild(DEFAULTVALUES_ELEMENT);
        if (values != null) {
            int type = def.getRequiredType();
            if (type == PropertyType.UNDEFINED) {
                type = PropertyType.STRING;
            }

            Iterator iterator =
                values.getChildren(DEFAULTVALUE_ELEMENT).iterator();
            while (iterator.hasNext()) {
                Element value = (Element) iterator.next();
                vector.add(InternalValue.valueOf(value.getTextTrim(), type));
            }
        }

        def.setDefaultValues(
                (InternalValue[]) vector.toArray(new InternalValue[0]));
    }

    /**
     * Writes the default values of the property definition.
     */
    private void writeDefaultValues() {
        Element values = new Element(DEFAULTVALUES_ELEMENT);

        InternalValue[] defaults = def.getDefaultValues();
        for (int i = 0; i < defaults.length; i++) {
            Element value = new Element(DEFAULTVALUE_ELEMENT);
            value.setText(defaults[i].toString());
            values.addContent(value);
        }

        addChild(values);
    }

    /**
     * Reads and sets the <code>multiple</code> attribute of the
     * property definition.
     *
     * @throws InvalidNodeTypeDefException if the format of the property
     *                                    definition element is invalid
     */
    private void readMultiple() throws InvalidNodeTypeDefException {
        String value = getAttribute(MULTIPLE_ATTRIBUTE);
        def.setMultiple(Boolean.valueOf(value).booleanValue());
    }

    /**
     * Writes the <code>multiple</code> attribute of the property definition.
     */
    private void writeMultiple() {
        String value = Boolean.toString(def.isMultiple());
        setAttribute(MULTIPLE_ATTRIBUTE, value);
    }

}
