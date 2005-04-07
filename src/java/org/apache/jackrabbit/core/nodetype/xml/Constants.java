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
final class Constants {

    /** Name of the node type definition root element. */
    public static final String NODETYPES_ELEMENT = "nodeTypes";

    /** Name of the node type definition element. */
    public static final String NODETYPE_ELEMENT = "nodeType";

    /** Name of the child node definition element. */
    public static final String CHILDNODEDEF_ELEMENT = "childNodeDef";

    /** Name of the property definition element. */
    public static final String PROPERTYDEF_ELEMENT = "propertyDef";

    /** Name of the <code>isMixin</code> attribute. */
    public static final String ISMIXIN_ATTRIBUTE = "isMixin";

    /** Name of the <code>hasOrderableChildNodes</code> attribute. */
    public static final String HASORDERABLECHILDNODES_ATTRIBUTE =
        "hasOrderableChildNodes";

    /** Name of the primary item name attribute. */
    public static final String PRIMARYITEMNAME_ATTRIBUTE = "primaryItemName";

    /** Name of the supertypes element. */
    public static final String SUPERTYPES_ELEMENT = "supertypes";

    /** Name of the supertype element. */
    public static final String SUPERTYPE_ELEMENT = "supertype";

    /** Name of the <code>name</code> attribute. */
    public static final String NAME_ATTRIBUTE = "name";

    /** Name of the <code>autoCreate</code> attribute. */
    public static final String AUTOCREATE_ATTRIBUTE = "autoCreate";

    /** Name of the <code>mandatory</code> attribute. */
    public static final String MANDATORY_ATTRIBUTE = "mandatory";

    /** Name of the <code>onParentVersion</code> attribute. */
    public static final String ONPARENTVERSION_ATTRIBUTE = "onParentVersion";

    /** Name of the <code>protected</code> attribute. */
    public static final String PROTECTED_ATTRIBUTE = "protected";

    /** Name of the required type attribute. */
    public static final String REQUIREDTYPE_ATTRIBUTE = "requiredType";

    /** Name of the value constraints element. */
    public static final String VALUECONSTRAINTS_ELEMENT = "valueConstraints";

    /** Name of the value constraint element. */
    public static final String VALUECONSTRAINT_ELEMENT = "valueConstraint";

    /** Name of the default values element. */
    public static final String DEFAULTVALUES_ELEMENT = "defaultValues";

    /** Name of the default value element. */
    public static final String DEFAULTVALUE_ELEMENT = "defaultValue";

    /** Name of the <code>multiple</code> attribute. */
    public static final String MULTIPLE_ATTRIBUTE = "multiple";

    /** Name of the required primary types element. */
    public static final String REQUIREDPRIMARYTYPES_ELEMENT =
        "requiredPrimaryTypes";

    /** Name of the required primary type element. */
    public static final String REQUIREDPRIMARYTYPE_ELEMENT =
        "requiredPrimaryType";

    /** Name of the default primary type attribute. */
    public static final String DEFAULTPRIMARYTYPE_ATTRIBUTE =
        "defaultPrimaryType";

    /** Name of the <code>sameNameSibs</code> attribute. */
    public static final String SAMENAMESIBS_ATTRIBUTE = "sameNameSibs";

}
