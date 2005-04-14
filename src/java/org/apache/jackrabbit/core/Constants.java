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
package org.apache.jackrabbit.core;

/**
 * This interface defines miscellaneous constants used frequently throughout the
 * implementation.
 */
public interface Constants {

    //------------------------------------------< namespace related constants >
    // default namespace (empty uri)
    public static final String NS_EMPTY_PREFIX = "";
    public static final String NS_DEFAULT_URI = "";

    // reserved namespace for repository internal node types
    public static final String NS_REP_PREFIX = "rep";
    public static final String NS_REP_URI = "internal";

    // reserved namespace for items defined by built-in node types
    public static final String NS_JCR_PREFIX = "jcr";
    public static final String NS_JCR_URI = "http://www.jcp.org/jcr/1.0";

    // reserved namespace for built-in primary node types
    public static final String NS_NT_PREFIX = "nt";
    public static final String NS_NT_URI = "http://www.jcp.org/jcr/nt/1.0";

    // reserved namespace for built-in mixin node types
    public static final String NS_MIX_PREFIX = "mix";
    public static final String NS_MIX_URI = "http://www.jcp.org/jcr/mix/1.0";

    // reserved namespace used in the system view XML serialization format
    public static final String NS_SV_PREFIX = "sv";
    public static final String NS_SV_URI = "http://www.jcp.org/jcr/sv/1.0";

    // reserved namespaces that must not be redefined and should not be used
    public static final String NS_XML_PREFIX = "xml";
    public static final String NS_XML_URI = "http://www.w3.org/XML/1998/namespace";
    public static final String NS_XMLNS_PREFIX = "xmlns";
    public static final String NS_XMLNS_URI = "http://www.w3.org/2000/xmlns/";

    //----------------------------------------< general item name constants >---

    /**
     * jcr:system
     */
    public static final QName JCR_SYSTEM = new QName(NS_JCR_URI, "system");

    /**
     * jcr:nodeTypes
     */
    public static final QName JCR_NODETYPES = new QName(NS_JCR_URI, "nodeTypes");

    /**
     * jcr:uuid
     */
    public static final QName JCR_UUID = new QName(NS_JCR_URI, "uuid");

    /**
     * jcr:primaryType
     */
    public static final QName JCR_PRIMARYTYPE = new QName(NS_JCR_URI, "primaryType");

    /**
     * jcr:mixinTypes
     */
    public static final QName JCR_MIXINTYPES = new QName(NS_JCR_URI, "mixinTypes");

    /**
     * jcr:created
     */
    public static final QName JCR_CREATED = new QName(NS_JCR_URI, "created");

    /**
     * jcr:lastModified
     */
    public static final QName JCR_LASTMODIFIED = new QName(NS_JCR_URI, "lastModified");

    //------------------------------------< xml related item name constants >---

    /**
     * jcr:root (dummy name for root node used in XML serialization)
     */
    public static final QName JCR_ROOT = new QName(NS_JCR_URI, "root");

    /**
     * jcr:xmltext
     */
    public static final QName JCR_XMLTEXT = new QName(NS_JCR_URI, "xmltext");

    /**
     * jcr:xmlcharacters
     */
    public static final QName JCR_XMLCHARACTERS = new QName(NS_JCR_URI, "xmlcharacters");

    //----------------------------------------< locking item name constants >---

    /**
     * jcr:lockOwner
     */
    public static final QName JCR_LOCKOWNER = new QName(NS_JCR_URI, "lockOwner");

    /**
     * jcr:lockIsDeep
     */
    public static final QName JCR_LOCKISDEEP = new QName(NS_JCR_URI, "lockIsDeep");

    //-------------------------------------< versioning item name constants >---

    /**
     * jcr:versionStorage
     */
    public static final QName JCR_VERSIONSTORAGE = new QName(NS_JCR_URI, "versionStorage");

    /**
     * jcr:mergeFailed
     */
    public static final QName JCR_MERGEFAILED = new QName(NS_JCR_URI, "mergeFailed");

    /**
     * jcr:frozenNode
     */
    public static final QName JCR_FROZENNODE = new QName(NS_JCR_URI, "frozenNode");

    /**
     * jcr:frozenUuid
     */
    public static final QName JCR_FROZENUUID = new QName(NS_JCR_URI, "frozenUuid");

    /**
     * jcr:frozenPrimaryType
     */
    public static final QName JCR_FROZENPRIMARYTYPE = new QName(NS_JCR_URI, "frozenPrimaryType");

    /**
     * jcr:frozenMixinTypes
     */
    public static final QName JCR_FROZENMIXINTYPES = new QName(NS_JCR_URI, "frozenMixinTypes");

    /**
     * jcr:predecessors
     */
    public static final QName JCR_PREDECESSORS = new QName(NS_JCR_URI, "predecessors");

    /**
     * jcr:versionLabels
     */
    public static final QName JCR_VERSIONLABELS = new QName(NS_JCR_URI, "versionLabels");

    /**
     * jcr:successors
     */
    public static final QName JCR_SUCCESSORS = new QName(NS_JCR_URI, "successors");

    /**
     * jcr:isCheckedOut
     */
    public static final QName JCR_ISCHECKEDOUT = new QName(NS_JCR_URI, "isCheckedOut");

    /**
     * jcr:versionHistory
     */
    public static final QName JCR_VERSIONHISTORY = new QName(NS_JCR_URI, "versionHistory");

    /**
     * jcr:baseVersion
     */
    public static final QName JCR_BASEVERSION = new QName(NS_JCR_URI, "baseVersion");

    /**
     * jcr:child
     */
    public static final QName JCR_CHILD = new QName(NS_JCR_URI, "child");

    /**
     * jcr:rootVersion
     */
    public static final QName JCR_ROOTVERSION = new QName(NS_JCR_URI, "rootVersion");

    //--------------------------------------< node type item name constants >---

    /**
     * jcr:nodeTypeName
     */
    public static final QName JCR_NODETYPENAME = new QName(NS_JCR_URI, "nodeTypeName");

    /**
     * jcr:hasOrderableChildNodes
     */
    public static final QName JCR_HASORDERABLECHILDNODES = new QName(NS_JCR_URI, "hasOrderableChildNodes");

    /**
     * jcr:isMixin
     */
    public static final QName JCR_ISMIXIN = new QName(NS_JCR_URI, "isMixin");

    /**
     * jcr:supertypes
     */
    public static final QName JCR_SUPERTYPES = new QName(NS_JCR_URI, "supertypes");

    /**
     * jcr:propertyDefinition
     */
    public static final QName JCR_PROPERTYDEFINITION = new QName(NS_JCR_URI, "propertyDefinition");

    /**
     * jcr:name
     */
    public static final QName JCR_NAME = new QName(NS_JCR_URI, "name");

    /**
     * jcr:mandatory
     */
    public static final QName JCR_MANDATORY = new QName(NS_JCR_URI, "mandatory");

    /**
     * jcr:protected
     */
    public static final QName JCR_PROTECTED = new QName(NS_JCR_URI, "protected");

    /**
     * jcr:requiredType
     */
    public static final QName JCR_REQUIREDTYPE = new QName(NS_JCR_URI, "requiredType");

    /**
     * jcr:onParentVersion
     */
    public static final QName JCR_ONPARENTVERSION = new QName(NS_JCR_URI, "onParentVersion");

    /**
     * jcr:primaryItemName
     */
    public static final QName JCR_PRIMARYITEMNAME = new QName(NS_JCR_URI, "primaryItemName");

    /**
     * jcr:multiple
     */
    public static final QName JCR_MULTIPLE = new QName(NS_JCR_URI, "multiple");

    /**
     * jcr:valueConstraints
     */
    public static final QName JCR_VALUECONSTRAINTS = new QName(NS_JCR_URI, "valueConstraints");

    /**
     * jcr:defaultValues
     */
    public static final QName JCR_DEFAULTVALUES = new QName(NS_JCR_URI, "defaultValues");

    /**
     * jcr:autoCreated
     */
    public static final QName JCR_AUTOCREATED = new QName(NS_JCR_URI, "autoCreated");

    /**
     * jcr:childNodeDefinition
     */
    public static final QName JCR_CHILDNODEDEFINITION = new QName(NS_JCR_URI, "childNodeDefinition");

    /**
     * jcr:sameNameSiblings
     */
    public static final QName JCR_SAMENAMESIBLINGS = new QName(NS_JCR_URI, "sameNameSiblings");

    /**
     * jcr:defaultPrimaryType
     */
    public static final QName JCR_DEFAULTPRIMARYTYPE = new QName(NS_JCR_URI, "defaultPrimaryType");

    /**
     * jcr:requiredPrimaryTypes
     */
    public static final QName JCR_REQUIREDPRIMARYTYPES = new QName(NS_JCR_URI, "requiredPrimaryTypes");

    //-------------------------------------------< node type name constants >---

    /**
     * rep:root
     */
    public static final QName REP_ROOT = new QName(NS_REP_URI, "root");

    /**
     * rep:system
     */
    public static final QName REP_SYSTEM = new QName(NS_REP_URI, "system");

    /**
     * rep:versionStorage
     */
    public static final QName REP_VERSIONSTORAGE = new QName(NS_REP_URI, "versionStorage");

    /**
     * rep:versionStorage
     */
    public static final QName REP_NODETYPES = new QName(NS_REP_URI, "nodeTypes");

    /**
     * nt:unstructured
     */
    public static final QName NT_UNSTRUCTURED = new QName(NS_NT_URI, "unstructured");

    /**
     * nt:base
     */
    public static final QName NT_BASE = new QName(NS_NT_URI, "base");

    /**
     * nt:hierarchyNode
     */
    public static final QName NT_HIERARCHYNODE = new QName(NS_NT_URI, "hierarchyNode");

    /**
     * nt:resource
     */
    public static final QName NT_RESOURCE = new QName(NS_NT_URI, "resource");

    /**
     * nt:query
     */
    public static final QName NT_QUERY = new QName(NS_NT_URI, "query");

    /**
     * mix:referenceable
     */
    public static final QName MIX_REFERENCEABLE = new QName(NS_MIX_URI, "referenceable");

    /**
     * mix:referenceable
     */
    public static final QName MIX_LOCKABLE = new QName(NS_MIX_URI, "lockable");

    /**
     * mix:versionable
     */
    public static final QName MIX_VERSIONABLE = new QName(NS_MIX_URI, "versionable");

    /**
     * nt:versionHistory
     */
    public static final QName NT_VERSIONHISTORY = new QName(NS_NT_URI, "versionHistory");

    /**
     * nt:version
     */
    public static final QName NT_VERSION = new QName(NS_NT_URI, "version");

    /**
     * nt:versionLabels
     */
    public static final QName NT_VERSIONLABELS = new QName(NS_NT_URI, "versionLabels");

    /**
     * nt:versionedChild
     */
    public static final QName NT_VERSIONEDCHILD = new QName(NS_NT_URI, "versionedChild");

    /**
     * nt:frozenNode
     */
    public static final QName NT_FROZENNODE = new QName(NS_NT_URI, "frozenNode");

    /**
     * nt:nodeType
     */
    public static final QName NT_NODETYPE = new QName(NS_NT_URI, "nodeType");

    /**
     * nt:propertyDefinition
     */
    public static final QName NT_PROPERTYDEFINITION = new QName(NS_NT_URI, "propertyDefinition");

    /**
     * nt:childNodeDefinition
     */
    public static final QName NT_CHILDNODEDEFINITION = new QName(NS_NT_URI, "childNodeDefinition");

    //-------------------------------------------< security related constants >
    /**
     * Name of the internal <code>SimpleCredentials</code> attribute where
     * the <code>Subject</code> of the <i>impersonating</i> <code>Session</code>
     * is stored.
     *
     * @see javax.jcr.Session#impersonate(javax.jcr.Credentials)
     */
    public static final String IMPERSONATOR_ATTRIBUTE =
            "org.apache.jackrabbit.core.security.impersonator";
}
