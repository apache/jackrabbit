/*
 * Copyright 2005 The Apache Software Foundation.
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
package org.apache.jackrabbit.webdav.spi.nodetype;

import org.jdom.Namespace;
import org.apache.jackrabbit.webdav.spi.ItemResourceConstants;

/**
 * <code>NodeTypeConstants</code> used to represent nodetype definitions in
 * Xml.
 *
 * @see javax.jcr.nodetype.NodeType
 * @todo intercaps only for consistency with jackrabbit... webdav rfcs never use intercaps.
 */
public interface NodeTypeConstants {

    public static final Namespace NAMESPACE = ItemResourceConstants.NAMESPACE;

    public static final String XML_NODETYPENAME = "nodetypename";

    public static final String XML_REPORT_ALLNODETYPES = "allnodetypes";
    public static final String XML_REPORT_MIXINNODETYPES = "mixinnodetypes";
    public static final String XML_REPORT_PRIMARYNODETYPES = "primarynodetypes";

    // report response
    /** Name of the node type definition root element. */
    public static final String XML_NODETYPES = "nodeTypes";

    /** Name of the node type definition element. */
    public static final String XML_NODETYPE = "nodeType";

    /** Name of the <code>isMixin</code> attribute. */
    public static final String ATTR_ISMIXIN = "isMixin";

    /** Name of the <code>hasOrderableChildNodes</code> attribute. */
    public static final String ATTR_HASORDERABLECHILDNODES = "hasOrderableChildNodes";

    /** Name of the primary item name attribute. */
    public static final String ATTR_PRIMARYITEMNAME = "primaryItemName";

    /** Name of the supertypes element. */
    public static final String XML_SUPERTYPES = "supertypes";

    /** Name of the supertype element. */
    public static final String XML_SUPERTYPE = "supertype";

    /** Name of the child node definition element. */
    public static final String XML_CHILDNODEDEF = "childNodeDef";

    /** Name of the property definition element. */
    public static final String XML_PROPERTYDEF = "propertyDef";

    /** Name of the <code>name</code> attribute. */
    public static final String ATTR_NAME = "name";

    /** Name of the <code>autoCreate</code> attribute. */
    public static final String ATTR_AUTOCREATE = "autoCreate";

    /** Name of the <code>mandatory</code> attribute. */
    public static final String ATTR_MANDATORY = "mandatory";

    /** Name of the <code>onParentVersion</code> attribute. */
    public static final String ATTR_ONPARENTVERSION = "onParentVersion";

    /** Name of the <code>protected</code> attribute. */
    public static final String ATTR_PROTECTED = "protected";

    /** Name of the required type attribute. */
    public static final String ATTR_REQUIREDTYPE = "requiredType";

    /** Name of the value constraints element. */
    public static final String XML_VALUECONSTRAINTS = "valueConstraints";

    /** Name of the value constraint element. */
    public static final String XML_VALUECONSTRAINT = "valueConstraint";

    /** Name of the default values element. */
    public static final String XML_DEFAULTVALUES = "defaultValues";

    /** Name of the default value element. */
    public static final String XML_DEFAULTVALUE = "defaultValue";

    /** Name of the <code>multiple</code> attribute. */
    public static final String ATTR_MULTIPLE = "multiple";

    /** Name of the required primary types element. */
    public static final String XML_REQUIREDPRIMARYTYPES = "requiredPrimaryTypes";

    /** Name of the required primary type element. */
    public static final String XML_REQUIREDPRIMARYTYPE = "requiredPrimaryType";

    /** Name of the default primary type attribute. */
    public static final String ATTR_DEFAULTPRIMARYTYPE = "defaultPrimaryType";

    /** Name of the <code>sameNameSibs</code> attribute. */
    public static final String ATTR_SAMENAMESIBS = "sameNameSibs";
}