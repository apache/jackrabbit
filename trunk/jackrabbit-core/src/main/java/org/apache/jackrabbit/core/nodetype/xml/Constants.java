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
package org.apache.jackrabbit.core.nodetype.xml;

/**
 * Name constants for the node type XML elements and attributes.
 */
public interface Constants {

    /** Name of the node type definition root element. */
    String NODETYPES_ELEMENT = "nodeTypes";

    /** Name of the node type definition element. */
    String NODETYPE_ELEMENT = "nodeType";

    /** Name of the child node definition element. */
    String CHILDNODEDEFINITION_ELEMENT = "childNodeDefinition";

    /** Name of the property definition element. */
    String PROPERTYDEFINITION_ELEMENT = "propertyDefinition";

    /** Name of the <code>isMixin</code> attribute. */
    String ISMIXIN_ATTRIBUTE = "isMixin";

    /** Name of the <code>isQueryable</code> attribute. */
    String ISQUERYABLE_ATTRIBUTE = "isQueryable";

    /** Name of the <code>isAbstract</code> attribute. */
    String ISABSTRACT_ATTRIBUTE = "isAbstract";

    /** Name of the <code>hasOrderableChildNodes</code> attribute. */
    String HASORDERABLECHILDNODES_ATTRIBUTE = "hasOrderableChildNodes";

    /** Name of the primary item name attribute. */
    String PRIMARYITEMNAME_ATTRIBUTE = "primaryItemName";

    /** Name of the supertypes element. */
    String SUPERTYPES_ELEMENT = "supertypes";

    /** Name of the supertype element. */
    String SUPERTYPE_ELEMENT = "supertype";

    /** Name of the <code>name</code> attribute. */
    String NAME_ATTRIBUTE = "name";

    /** Name of the <code>autoCreated</code> attribute. */
    String AUTOCREATED_ATTRIBUTE = "autoCreated";

    /** Name of the <code>mandatory</code> attribute. */
    String MANDATORY_ATTRIBUTE = "mandatory";

    /** Name of the <code>onParentVersion</code> attribute. */
    String ONPARENTVERSION_ATTRIBUTE = "onParentVersion";

    /** Name of the <code>protected</code> attribute. */
    String PROTECTED_ATTRIBUTE = "protected";

    /** Name of the required type attribute. */
    String REQUIREDTYPE_ATTRIBUTE = "requiredType";

    /** Name of the value constraints element. */
    String VALUECONSTRAINTS_ELEMENT = "valueConstraints";

    /** Name of the value constraint element. */
    String VALUECONSTRAINT_ELEMENT = "valueConstraint";

    /** Name of the default values element. */
    String DEFAULTVALUES_ELEMENT = "defaultValues";

    /** Name of the default value element. */
    String DEFAULTVALUE_ELEMENT = "defaultValue";

    /** Name of the <code>isQueryOrderable</code> attribute. */
    String ISQUERYORDERABLE_ATTRIBUTE = "isQueryOrderable";

    /** Name of the <code>isFullTextSearchable</code> attribute. */
    String ISFULLTEXTSEARCHABLE_ATTRIBUTE = "isFullTextSearchable";

    /** Name of the <code>availableQueryOperators</code> attribute. */
    String AVAILABLEQUERYOPERATORS_ATTRIBUTE = "availableQueryOperators";

    String EQ_ENTITY = "OP_EQ";
    String NE_ENTITY = "OP_NE";
    String LT_ENTITY = "OP_LT";
    String LE_ENTITY = "OP_LE";
    String GT_ENTITY = "OP_GT";
    String GE_ENTITY = "OP_GE";
    String LIKE_ENTITY = "OP_LIKE";

    /** Name of the <code>multiple</code> attribute. */
    String MULTIPLE_ATTRIBUTE = "multiple";

    /** Name of the required primary types element. */
    String REQUIREDPRIMARYTYPES_ELEMENT = "requiredPrimaryTypes";

    /** Name of the required primary type element. */
    String REQUIREDPRIMARYTYPE_ELEMENT = "requiredPrimaryType";

    /** Name of the default primary type attribute. */
    String DEFAULTPRIMARYTYPE_ATTRIBUTE = "defaultPrimaryType";

    /** Name of the <code>sameNameSiblings</code> attribute. */
    String SAMENAMESIBLINGS_ATTRIBUTE = "sameNameSiblings";

}
