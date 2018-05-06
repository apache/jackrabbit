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
package org.apache.jackrabbit.commons.webdav;

/**
 * <code>NodeTypeConstants</code> used to represent nodetype definitions in
 * Xml.
 *
 * @see javax.jcr.nodetype.NodeType
 */
public interface NodeTypeConstants {

    public static final String XML_NODETYPENAME = "nodetypename";

    public static final String XML_REPORT_ALLNODETYPES = "all-nodetypes";
    public static final String XML_REPORT_MIXINNODETYPES = "mixin-nodetypes";
    public static final String XML_REPORT_PRIMARYNODETYPES = "primary-nodetypes";
    public static final String XML_NODETYPES = "nodetypes";
    public static final String XML_NODETYPE = "nodetype";

    //------< copied from org.apache.jackrabbit.core.nodetype.xml.Constants >---

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

    /** Name of the <code>hasOrderableChildNodes</code> attribute. */
    String HASORDERABLECHILDNODES_ATTRIBUTE = "hasOrderableChildNodes";

    /**
     * Name of the <code>isAbstract</code> attribute.
     * @since JCR 2.0
     */
    String ISABSTRACT_ATTRIBUTE = "isAbstract";

    /**
     * Name of the <code>isQueryable</code> attribute.
     * @since JCR 2.0
     */
    String ISQUERYABLE_ATTRIBUTE = "isQueryable";

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

    /**
     * Name of the availableQueryOperators element.
     * @since JCR 2.0
     */
    String AVAILABLE_QUERY_OPERATORS_ELEMENT = "availableQueryOperators";

    /**
     * Name of the availableQueryOperator element.
     * @since JCR 2.0
     */
    String AVAILABLE_QUERY_OPERATOR_ELEMENT = "availableQueryOperator";

    /**
     * Name of the fullTextSearchable attribute.
     * @since JCR 2.0
     */
    String FULL_TEXT_SEARCHABLE_ATTRIBUTE = "fullTextSearchable";

    /**
     * Name of the queryOrderable attribute.
     * @since JCR 2.0
     */
    String QUERY_ORDERABLE_ATTRIBUTE = "queryOrderable";

    //----------< attr. not defined by copied from o.a.j.core.n.x.Constants >---
    /**
     * Name of the declaring nodetype. This value is not needed during
     * discovery of nodetype definitions. However if the definition of an item is
     * retrieved (instead of being calculated on the client), this information is
     * needed
     */
    String DECLARINGNODETYPE_ATTRIBUTE = "declaringNodeType";
}