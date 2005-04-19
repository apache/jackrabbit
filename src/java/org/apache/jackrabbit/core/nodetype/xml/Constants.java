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

/**
 * Name constants for the node type XML elements and attributes.
 */
public interface Constants {

    /** Name of the node type definition root element. */
    static final String NODETYPES_ELEMENT = "nodeTypes";

    /** Name of the node type definition element. */
    static final String NODETYPE_ELEMENT = "nodeType";

    /** Name of the child node definition element. */
    static final String CHILDNODEDEFINITION_ELEMENT = "childNodeDefinition";

    /** Name of the property definition element. */
    static final String PROPERTYDEFINITION_ELEMENT = "propertyDefinition";

    /** Name of the <code>isMixin</code> attribute. */
    static final String ISMIXIN_ATTRIBUTE = "isMixin";

    /** Name of the <code>hasOrderableChildNodes</code> attribute. */
    static final String HASORDERABLECHILDNODES_ATTRIBUTE =
        "hasOrderableChildNodes";

    /** Name of the primary item name attribute. */
    static final String PRIMARYITEMNAME_ATTRIBUTE = "primaryItemName";

    /** Name of the supertypes element. */
    static final String SUPERTYPES_ELEMENT = "supertypes";

    /** Name of the supertype element. */
    static final String SUPERTYPE_ELEMENT = "supertype";

    /** Name of the <code>name</code> attribute. */
    static final String NAME_ATTRIBUTE = "name";

    /** Name of the <code>autoCreated</code> attribute. */
    static final String AUTOCREATED_ATTRIBUTE = "autoCreated";

    /** Name of the <code>mandatory</code> attribute. */
    static final String MANDATORY_ATTRIBUTE = "mandatory";

    /** Name of the <code>onParentVersion</code> attribute. */
    static final String ONPARENTVERSION_ATTRIBUTE = "onParentVersion";

    /** Name of the <code>protected</code> attribute. */
    static final String PROTECTED_ATTRIBUTE = "protected";

    /** Name of the required type attribute. */
    static final String REQUIREDTYPE_ATTRIBUTE = "requiredType";

    /** Name of the value constraints element. */
    static final String VALUECONSTRAINTS_ELEMENT = "valueConstraints";

    /** Name of the value constraint element. */
    static final String VALUECONSTRAINT_ELEMENT = "valueConstraint";

    /** Name of the default values element. */
    static final String DEFAULTVALUES_ELEMENT = "defaultValues";

    /** Name of the default value element. */
    static final String DEFAULTVALUE_ELEMENT = "defaultValue";

    /** Name of the <code>multiple</code> attribute. */
    static final String MULTIPLE_ATTRIBUTE = "multiple";

    /** Name of the required primary types element. */
    static final String REQUIREDPRIMARYTYPES_ELEMENT =
        "requiredPrimaryTypes";

    /** Name of the required primary type element. */
    static final String REQUIREDPRIMARYTYPE_ELEMENT =
        "requiredPrimaryType";

    /** Name of the default primary type attribute. */
    static final String DEFAULTPRIMARYTYPE_ATTRIBUTE =
        "defaultPrimaryType";

    /** Name of the <code>sameNameSiblings</code> attribute. */
    static final String SAMENAMESIBLINGS_ATTRIBUTE = "sameNameSiblings";

}
