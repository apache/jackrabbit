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
    String NS_EMPTY_PREFIX = "";
    String NS_DEFAULT_URI = "";

    // reserved namespace for repository internal node types
    String NS_REP_PREFIX = "rep";
    String NS_REP_URI = "internal";

    // reserved namespace for items defined by built-in node types
    String NS_JCR_PREFIX = "jcr";
    String NS_JCR_URI = "http://www.jcp.org/jcr/1.0";

    // reserved namespace for built-in primary node types
    String NS_NT_PREFIX = "nt";
    String NS_NT_URI = "http://www.jcp.org/jcr/nt/1.0";

    // reserved namespace for built-in mixin node types
    String NS_MIX_PREFIX = "mix";
    String NS_MIX_URI = "http://www.jcp.org/jcr/mix/1.0";

    // reserved namespace used in the system view XML serialization format
    String NS_SV_PREFIX = "sv";
    String NS_SV_URI = "http://www.jcp.org/jcr/sv/1.0";

    // reserved namespaces that must not be redefined and should not be used
    String NS_XML_PREFIX = "xml";
    String NS_XML_URI = "http://www.w3.org/XML/1998/namespace";
    String NS_XMLNS_PREFIX = "xmlns";
    String NS_XMLNS_URI = "http://www.w3.org/2000/xmlns/";

    //------------------------------------------< general item name constants >

    /**
     * jcr:system
     */
    QName JCR_SYSTEM = new QName(NS_JCR_URI, "system");

    /**
     * jcr:nodeTypes
     */
    QName JCR_NODETYPES = new QName(NS_JCR_URI, "nodeTypes");

    /**
     * jcr:uuid
     */
    QName JCR_UUID = new QName(NS_JCR_URI, "uuid");

    /**
     * jcr:primaryType
     */
    QName JCR_PRIMARYTYPE = new QName(NS_JCR_URI, "primaryType");

    /**
     * jcr:mixinTypes
     */
    QName JCR_MIXINTYPES = new QName(NS_JCR_URI, "mixinTypes");

    /**
     * jcr:created
     */
    QName JCR_CREATED = new QName(NS_JCR_URI, "created");

    /**
     * jcr:lastModified
     */
    QName JCR_LASTMODIFIED = new QName(NS_JCR_URI, "lastModified");

    /**
     * jcr:content
     */
    QName JCR_CONTENT = new QName(NS_JCR_URI, "content");

    //--------------------------------------< xml related item name constants >

    /**
     * jcr:root (dummy name for root node used in XML serialization)
     */
    QName JCR_ROOT = new QName(NS_JCR_URI, "root");

    /**
     * jcr:xmltext
     */
    QName JCR_XMLTEXT = new QName(NS_JCR_URI, "xmltext");

    /**
     * jcr:xmlcharacters
     */
    QName JCR_XMLCHARACTERS = new QName(NS_JCR_URI, "xmlcharacters");

    //----------------------------------< locking related item name constants >

    /**
     * jcr:lockOwner
     */
    QName JCR_LOCKOWNER = new QName(NS_JCR_URI, "lockOwner");

    /**
     * jcr:lockIsDeep
     */
    QName JCR_LOCKISDEEP = new QName(NS_JCR_URI, "lockIsDeep");

    //-------------------------------< versioning related item name constants >

    /**
     * jcr:versionStorage
     */
    QName JCR_VERSIONSTORAGE = new QName(NS_JCR_URI, "versionStorage");

    /**
     * jcr:mergeFailed
     */
    QName JCR_MERGEFAILED = new QName(NS_JCR_URI, "mergeFailed");

    /**
     * jcr:frozenNode
     */
    QName JCR_FROZENNODE = new QName(NS_JCR_URI, "frozenNode");

    /**
     * jcr:frozenUuid
     */
    QName JCR_FROZENUUID = new QName(NS_JCR_URI, "frozenUuid");

    /**
     * jcr:frozenPrimaryType
     */
    QName JCR_FROZENPRIMARYTYPE = new QName(NS_JCR_URI, "frozenPrimaryType");

    /**
     * jcr:frozenMixinTypes
     */
    QName JCR_FROZENMIXINTYPES = new QName(NS_JCR_URI, "frozenMixinTypes");

    /**
     * jcr:predecessors
     */
    QName JCR_PREDECESSORS = new QName(NS_JCR_URI, "predecessors");

    /**
     * jcr:versionLabels
     */
    QName JCR_VERSIONLABELS = new QName(NS_JCR_URI, "versionLabels");

    /**
     * jcr:successors
     */
    QName JCR_SUCCESSORS = new QName(NS_JCR_URI, "successors");

    /**
     * jcr:isCheckedOut
     */
    QName JCR_ISCHECKEDOUT = new QName(NS_JCR_URI, "isCheckedOut");

    /**
     * jcr:versionHistory
     */
    QName JCR_VERSIONHISTORY = new QName(NS_JCR_URI, "versionHistory");

    /**
     * jcr:baseVersion
     */
    QName JCR_BASEVERSION = new QName(NS_JCR_URI, "baseVersion");

    /**
     * jcr:childVersionHistory
     */
    QName JCR_CHILDVERSIONHISTORY = new QName(NS_JCR_URI, "childVersionHistory");

    /**
     * jcr:rootVersion
     */
    QName JCR_ROOTVERSION = new QName(NS_JCR_URI, "rootVersion");

    /**
     * jcr:versionableUuid
     */
    QName JCR_VERSIONABLEUUID = new QName(NS_JCR_URI, "versionableUuid");

    //--------------------------------< node type related item name constants >

    /**
     * jcr:nodeTypeName
     */
    QName JCR_NODETYPENAME = new QName(NS_JCR_URI, "nodeTypeName");

    /**
     * jcr:hasOrderableChildNodes
     */
    QName JCR_HASORDERABLECHILDNODES = new QName(NS_JCR_URI, "hasOrderableChildNodes");

    /**
     * jcr:isMixin
     */
    QName JCR_ISMIXIN = new QName(NS_JCR_URI, "isMixin");

    /**
     * jcr:supertypes
     */
    QName JCR_SUPERTYPES = new QName(NS_JCR_URI, "supertypes");

    /**
     * jcr:propertyDefinition
     */
    QName JCR_PROPERTYDEFINITION = new QName(NS_JCR_URI, "propertyDefinition");

    /**
     * jcr:name
     */
    QName JCR_NAME = new QName(NS_JCR_URI, "name");

    /**
     * jcr:mandatory
     */
    QName JCR_MANDATORY = new QName(NS_JCR_URI, "mandatory");

    /**
     * jcr:protected
     */
    QName JCR_PROTECTED = new QName(NS_JCR_URI, "protected");

    /**
     * jcr:requiredType
     */
    QName JCR_REQUIREDTYPE = new QName(NS_JCR_URI, "requiredType");

    /**
     * jcr:onParentVersion
     */
    QName JCR_ONPARENTVERSION = new QName(NS_JCR_URI, "onParentVersion");

    /**
     * jcr:primaryItemName
     */
    QName JCR_PRIMARYITEMNAME = new QName(NS_JCR_URI, "primaryItemName");

    /**
     * jcr:multiple
     */
    QName JCR_MULTIPLE = new QName(NS_JCR_URI, "multiple");

    /**
     * jcr:valueConstraints
     */
    QName JCR_VALUECONSTRAINTS = new QName(NS_JCR_URI, "valueConstraints");

    /**
     * jcr:defaultValues
     */
    QName JCR_DEFAULTVALUES = new QName(NS_JCR_URI, "defaultValues");

    /**
     * jcr:autoCreated
     */
    QName JCR_AUTOCREATED = new QName(NS_JCR_URI, "autoCreated");

    /**
     * jcr:childNodeDefinition
     */
    QName JCR_CHILDNODEDEFINITION = new QName(NS_JCR_URI, "childNodeDefinition");

    /**
     * jcr:sameNameSiblings
     */
    QName JCR_SAMENAMESIBLINGS = new QName(NS_JCR_URI, "sameNameSiblings");

    /**
     * jcr:defaultPrimaryType
     */
    QName JCR_DEFAULTPRIMARYTYPE = new QName(NS_JCR_URI, "defaultPrimaryType");

    /**
     * jcr:requiredPrimaryTypes
     */
    QName JCR_REQUIREDPRIMARYTYPES = new QName(NS_JCR_URI, "requiredPrimaryTypes");

    //---------------------------------------------< node type name constants >

    /**
     * rep:root
     */
    QName REP_ROOT = new QName(NS_REP_URI, "root");

    /**
     * rep:system
     */
    QName REP_SYSTEM = new QName(NS_REP_URI, "system");

    /**
     * rep:versionStorage
     */
    QName REP_VERSIONSTORAGE = new QName(NS_REP_URI, "versionStorage");

    /**
     * rep:versionStorage
     */
    QName REP_NODETYPES = new QName(NS_REP_URI, "nodeTypes");

    /**
     * nt:unstructured
     */
    QName NT_UNSTRUCTURED = new QName(NS_NT_URI, "unstructured");

    /**
     * nt:base
     */
    QName NT_BASE = new QName(NS_NT_URI, "base");

    /**
     * nt:hierarchyNode
     */
    QName NT_HIERARCHYNODE = new QName(NS_NT_URI, "hierarchyNode");

    /**
     * nt:resource
     */
    QName NT_RESOURCE = new QName(NS_NT_URI, "resource");

    /**
     * nt:file
     */
    QName NT_FILE = new QName(NS_NT_URI, "file");

    /**
     * nt:folder
     */
    QName NT_FOLDER = new QName(NS_NT_URI, "folder");

    /**
     * nt:query
     */
    QName NT_QUERY = new QName(NS_NT_URI, "query");

    /**
     * mix:referenceable
     */
    QName MIX_REFERENCEABLE = new QName(NS_MIX_URI, "referenceable");

    /**
     * mix:referenceable
     */
    QName MIX_LOCKABLE = new QName(NS_MIX_URI, "lockable");

    /**
     * mix:versionable
     */
    QName MIX_VERSIONABLE = new QName(NS_MIX_URI, "versionable");

    /**
     * nt:versionHistory
     */
    QName NT_VERSIONHISTORY = new QName(NS_NT_URI, "versionHistory");

    /**
     * nt:version
     */
    QName NT_VERSION = new QName(NS_NT_URI, "version");

    /**
     * nt:versionLabels
     */
    QName NT_VERSIONLABELS = new QName(NS_NT_URI, "versionLabels");

    /**
     * nt:versionedChild
     */
    QName NT_VERSIONEDCHILD = new QName(NS_NT_URI, "versionedChild");

    /**
     * nt:frozenNode
     */
    QName NT_FROZENNODE = new QName(NS_NT_URI, "frozenNode");

    /**
     * nt:nodeType
     */
    QName NT_NODETYPE = new QName(NS_NT_URI, "nodeType");

    /**
     * nt:propertyDefinition
     */
    QName NT_PROPERTYDEFINITION = new QName(NS_NT_URI, "propertyDefinition");

    /**
     * nt:childNodeDefinition
     */
    QName NT_CHILDNODEDEFINITION = new QName(NS_NT_URI, "childNodeDefinition");

    //-------------------------------------------< security related constants >
    /**
     * Name of the internal <code>SimpleCredentials</code> attribute where
     * the <code>Subject</code> of the <i>impersonating</i> <code>Session</code>
     * is stored.
     *
     * @see javax.jcr.Session#impersonate(javax.jcr.Credentials)
     */
    String IMPERSONATOR_ATTRIBUTE =
            "org.apache.jackrabbit.core.security.impersonator";
}
