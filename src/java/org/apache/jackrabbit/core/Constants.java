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
    static final String NS_EMPTY_PREFIX = "";
    static final String NS_DEFAULT_URI = "";

    // reserved namespace for repository internal node types
    static final String NS_REP_PREFIX = "rep";
    static final String NS_REP_URI = "internal";

    // reserved namespace for items defined by built-in node types
    static final String NS_JCR_PREFIX = "jcr";
    static final String NS_JCR_URI = "http://www.jcp.org/jcr/1.0";

    // reserved namespace for built-in primary node types
    static final String NS_NT_PREFIX = "nt";
    static final String NS_NT_URI = "http://www.jcp.org/jcr/nt/1.0";

    // reserved namespace for built-in mixin node types
    static final String NS_MIX_PREFIX = "mix";
    static final String NS_MIX_URI = "http://www.jcp.org/jcr/mix/1.0";

    // reserved namespace used in the system view XML serialization format
    static final String NS_SV_PREFIX = "sv";
    static final String NS_SV_URI = "http://www.jcp.org/jcr/sv/1.0";

    // reserved namespaces that must not be redefined and should not be used
    static final String NS_XML_PREFIX = "xml";
    static final String NS_XML_URI = "http://www.w3.org/XML/1998/namespace";
    static final String NS_XMLNS_PREFIX = "xmlns";
    static final String NS_XMLNS_URI = "http://www.w3.org/2000/xmlns/";

    //----------------------------------------< general item name constants >---

    /**
     * jcr:system
     */
    static final QName JCR_SYSTEM = new QName(NS_JCR_URI, "system");

    /**
     * jcr:nodeTypes
     */
    static final QName JCR_NODETYPES = new QName(NS_JCR_URI, "nodeTypes");

    /**
     * jcr:uuid
     */
    static final QName JCR_UUID = new QName(NS_JCR_URI, "uuid");

    /**
     * jcr:primaryType
     */
    static final QName JCR_PRIMARYTYPE = new QName(NS_JCR_URI, "primaryType");

    /**
     * jcr:mixinTypes
     */
    static final QName JCR_MIXINTYPES = new QName(NS_JCR_URI, "mixinTypes");

    /**
     * jcr:created
     */
    static final QName JCR_CREATED = new QName(NS_JCR_URI, "created");

    /**
     * jcr:lastModified
     */
    static final QName JCR_LASTMODIFIED = new QName(NS_JCR_URI, "lastModified");

    //------------------------------------< xml related item name constants >---

    /**
     * jcr:root (dummy name for root node used in XML serialization)
     */
    static final QName JCR_ROOT = new QName(NS_JCR_URI, "root");

    /**
     * jcr:xmltext
     */
    static final QName JCR_XMLTEXT = new QName(NS_JCR_URI, "xmltext");

    /**
     * jcr:xmlcharacters
     */
    static final QName JCR_XMLCHARACTERS = new QName(NS_JCR_URI, "xmlcharacters");

    //----------------------------------------< locking item name constants >---

    /**
     * jcr:lockOwner
     */
    static final QName JCR_LOCKOWNER = new QName(NS_JCR_URI, "lockOwner");

    /**
     * jcr:lockIsDeep
     */
    static final QName JCR_LOCKISDEEP = new QName(NS_JCR_URI, "lockIsDeep");

    //-------------------------------------< versioning item name constants >---

    /**
     * jcr:versionStorage
     */
    static final QName JCR_VERSIONSTORAGE = new QName(NS_JCR_URI, "versionStorage");

    /**
     * jcr:mergeFailed
     */
    static final QName JCR_MERGEFAILED = new QName(NS_JCR_URI, "mergeFailed");

    /**
     * jcr:frozenNode
     */
    static final QName JCR_FROZENNODE = new QName(NS_JCR_URI, "frozenNode");

    /**
     * jcr:frozenUuid
     */
    static final QName JCR_FROZENUUID = new QName(NS_JCR_URI, "frozenUuid");

    /**
     * jcr:frozenPrimaryType
     */
    static final QName JCR_FROZENPRIMARYTYPE = new QName(NS_JCR_URI, "frozenPrimaryType");

    /**
     * jcr:frozenMixinTypes
     */
    static final QName JCR_FROZENMIXINTYPES = new QName(NS_JCR_URI, "frozenMixinTypes");

    /**
     * jcr:predecessors
     */
    static final QName JCR_PREDECESSORS = new QName(NS_JCR_URI, "predecessors");

    /**
     * jcr:versionLabels
     */
    static final QName JCR_VERSIONLABELS = new QName(NS_JCR_URI, "versionLabels");

    /**
     * jcr:successors
     */
    static final QName JCR_SUCCESSORS = new QName(NS_JCR_URI, "successors");

    /**
     * jcr:isCheckedOut
     */
    static final QName JCR_ISCHECKEDOUT = new QName(NS_JCR_URI, "isCheckedOut");

    /**
     * jcr:versionHistory
     */
    static final QName JCR_VERSIONHISTORY = new QName(NS_JCR_URI, "versionHistory");

    /**
     * jcr:baseVersion
     */
    static final QName JCR_BASEVERSION = new QName(NS_JCR_URI, "baseVersion");

    /**
     * jcr:child
     */
    static final QName JCR_CHILD = new QName(NS_JCR_URI, "child");

    /**
     * jcr:rootVersion
     */
    static final QName JCR_ROOTVERSION = new QName(NS_JCR_URI, "rootVersion");

    /**
     * jcr:versionableUuid
     */
    static final QName JCR_VERSIONABLEUUID = new QName(NS_JCR_URI, "versionableUuid");

    //--------------------------------------< node type item name constants >---

    /**
     * jcr:nodeTypeName
     */
    static final QName JCR_NODETYPENAME = new QName(NS_JCR_URI, "nodeTypeName");

    /**
     * jcr:hasOrderableChildNodes
     */
    static final QName JCR_HASORDERABLECHILDNODES = new QName(NS_JCR_URI, "hasOrderableChildNodes");

    /**
     * jcr:isMixin
     */
    static final QName JCR_ISMIXIN = new QName(NS_JCR_URI, "isMixin");

    /**
     * jcr:supertypes
     */
    static final QName JCR_SUPERTYPES = new QName(NS_JCR_URI, "supertypes");

    /**
     * jcr:propertyDefinition
     */
    static final QName JCR_PROPERTYDEFINITION = new QName(NS_JCR_URI, "propertyDefinition");

    /**
     * jcr:name
     */
    static final QName JCR_NAME = new QName(NS_JCR_URI, "name");

    /**
     * jcr:mandatory
     */
    static final QName JCR_MANDATORY = new QName(NS_JCR_URI, "mandatory");

    /**
     * jcr:protected
     */
    static final QName JCR_PROTECTED = new QName(NS_JCR_URI, "protected");

    /**
     * jcr:requiredType
     */
    static final QName JCR_REQUIREDTYPE = new QName(NS_JCR_URI, "requiredType");

    /**
     * jcr:onParentVersion
     */
    static final QName JCR_ONPARENTVERSION = new QName(NS_JCR_URI, "onParentVersion");

    /**
     * jcr:primaryItemName
     */
    static final QName JCR_PRIMARYITEMNAME = new QName(NS_JCR_URI, "primaryItemName");

    /**
     * jcr:multiple
     */
    static final QName JCR_MULTIPLE = new QName(NS_JCR_URI, "multiple");

    /**
     * jcr:valueConstraints
     */
    static final QName JCR_VALUECONSTRAINTS = new QName(NS_JCR_URI, "valueConstraints");

    /**
     * jcr:defaultValues
     */
    static final QName JCR_DEFAULTVALUES = new QName(NS_JCR_URI, "defaultValues");

    /**
     * jcr:autoCreated
     */
    static final QName JCR_AUTOCREATED = new QName(NS_JCR_URI, "autoCreated");

    /**
     * jcr:childNodeDefinition
     */
    static final QName JCR_CHILDNODEDEFINITION = new QName(NS_JCR_URI, "childNodeDefinition");

    /**
     * jcr:sameNameSiblings
     */
    static final QName JCR_SAMENAMESIBLINGS = new QName(NS_JCR_URI, "sameNameSiblings");

    /**
     * jcr:defaultPrimaryType
     */
    static final QName JCR_DEFAULTPRIMARYTYPE = new QName(NS_JCR_URI, "defaultPrimaryType");

    /**
     * jcr:requiredPrimaryTypes
     */
    static final QName JCR_REQUIREDPRIMARYTYPES = new QName(NS_JCR_URI, "requiredPrimaryTypes");

    //-------------------------------------------< node type name constants >---

    /**
     * rep:root
     */
    static final QName REP_ROOT = new QName(NS_REP_URI, "root");

    /**
     * rep:system
     */
    static final QName REP_SYSTEM = new QName(NS_REP_URI, "system");

    /**
     * rep:versionStorage
     */
    static final QName REP_VERSIONSTORAGE = new QName(NS_REP_URI, "versionStorage");

    /**
     * rep:versionStorage
     */
    static final QName REP_NODETYPES = new QName(NS_REP_URI, "nodeTypes");

    /**
     * nt:unstructured
     */
    static final QName NT_UNSTRUCTURED = new QName(NS_NT_URI, "unstructured");

    /**
     * nt:base
     */
    static final QName NT_BASE = new QName(NS_NT_URI, "base");

    /**
     * nt:hierarchyNode
     */
    static final QName NT_HIERARCHYNODE = new QName(NS_NT_URI, "hierarchyNode");

    /**
     * nt:resource
     */
    static final QName NT_RESOURCE = new QName(NS_NT_URI, "resource");

    /**
     * nt:query
     */
    static final QName NT_QUERY = new QName(NS_NT_URI, "query");

    /**
     * mix:referenceable
     */
    static final QName MIX_REFERENCEABLE = new QName(NS_MIX_URI, "referenceable");

    /**
     * mix:referenceable
     */
    static final QName MIX_LOCKABLE = new QName(NS_MIX_URI, "lockable");

    /**
     * mix:versionable
     */
    static final QName MIX_VERSIONABLE = new QName(NS_MIX_URI, "versionable");

    /**
     * nt:versionHistory
     */
    static final QName NT_VERSIONHISTORY = new QName(NS_NT_URI, "versionHistory");

    /**
     * nt:version
     */
    static final QName NT_VERSION = new QName(NS_NT_URI, "version");

    /**
     * nt:versionLabels
     */
    static final QName NT_VERSIONLABELS = new QName(NS_NT_URI, "versionLabels");

    /**
     * nt:versionedChild
     */
    static final QName NT_VERSIONEDCHILD = new QName(NS_NT_URI, "versionedChild");

    /**
     * nt:frozenNode
     */
    static final QName NT_FROZENNODE = new QName(NS_NT_URI, "frozenNode");

    /**
     * nt:nodeType
     */
    static final QName NT_NODETYPE = new QName(NS_NT_URI, "nodeType");

    /**
     * nt:propertyDefinition
     */
    static final QName NT_PROPERTYDEFINITION = new QName(NS_NT_URI, "propertyDefinition");

    /**
     * nt:childNodeDefinition
     */
    static final QName NT_CHILDNODEDEFINITION = new QName(NS_NT_URI, "childNodeDefinition");

    //-------------------------------------------< security related constants >
    /**
     * Name of the internal <code>SimpleCredentials</code> attribute where
     * the <code>Subject</code> of the <i>impersonating</i> <code>Session</code>
     * is stored.
     *
     * @see javax.jcr.Session#impersonate(javax.jcr.Credentials)
     */
    static final String IMPERSONATOR_ATTRIBUTE =
            "org.apache.jackrabbit.core.security.impersonator";
}
