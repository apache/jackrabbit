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
 */
public interface NodeTypeConstants {

    public static final Namespace NAMESPACE = ItemResourceConstants.NAMESPACE;

    public static final String XML_NODETYPE = "nodetype";
    public static final String XML_NODETYPENAME = "nodetypename";

    public static final String XML_NODETYPEDEFINITION = "nodetypedefinition";

    public static final String XML_MIXIN = "mixin";
    public static final String XML_ORDERABLECHILDNODES = "orderablechildnodes";
    public static final String XML_PRIMARYITEMNAME = "primaryitemname";

    public static final String XML_SUPERTYPES = "supertypes";
    public static final String XML_DECLARED_SUPERTYPES = "declaredsupertypes";
    public static final String XML_CHILDNODEDEF = "childnodedef";
    public static final String XML_PROPERTYDEF = "propertydef";

    public static final String ATTR_NAME = "name";
    public static final String ATTR_AUTOCREATE = "autocreate";
    public static final String ATTR_MANDATORY = "mandatory";
    public static final String ATTR_PROTECTED = "protected";
    public static final String ATTR_ONPARENTVERSION = "onparentversion";

    public static final String ATTR_MULTIPLE = "multiple";
    public static final String ATTR_TYPE = "type";

    public static final String ATTR_SAMENAMESIBS = "samenamesibs";

    public static final String XML_DECLARINGNODETYPE = "declaringnodetype";
    
    public static final String XML_VALUECONSTRAINTS = "valueconstraints";
    public static final String XML_VALUECONSTRAINT = "valueconstraint";
    public static final String XML_DEFAULTVALUES = "defaultvalues";
    public static final String XML_DEFAULTVALUE = "defaultvalue";

    public static final String XML_DEFAULTPRIMARYTYPE = "defaultprimarytype";
    public static final String XML_REQUIREDPRIMARYTYPES = "requiredprimarytypes";
}