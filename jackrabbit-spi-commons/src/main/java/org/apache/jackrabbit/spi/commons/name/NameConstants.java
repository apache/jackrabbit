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
package org.apache.jackrabbit.spi.commons.name;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;

/**
 * <code>NameConstants</code>...
 */
public class NameConstants {

    private static final NameFactory FACTORY = NameFactoryImpl.getInstance();

    /**
     * Extra Name for the root node
     */
    public static final Name ROOT = FACTORY.create(Name.NS_DEFAULT_URI,"");

    /**
     * jcr:system
     */
    public static final Name JCR_SYSTEM = FACTORY.create(Name.NS_JCR_URI, "system");

    /**
     * jcr:nodeTypes
     */
    public static final Name JCR_NODETYPES = FACTORY.create(Name.NS_JCR_URI, "nodeTypes");

    /**
     * jcr:uuid
     */
    public static final Name JCR_UUID = FACTORY.create(Name.NS_JCR_URI, "uuid");

    /**
     * jcr:primaryType
     */
    public static final Name JCR_PRIMARYTYPE = FACTORY.create(Name.NS_JCR_URI, "primaryType");

    /**
     * jcr:mixinTypes
     */
    public static final Name JCR_MIXINTYPES = FACTORY.create(Name.NS_JCR_URI, "mixinTypes");

    /**
     * jcr:created
     */
    public static final Name JCR_CREATED = FACTORY.create(Name.NS_JCR_URI, "created");

    /**
     * jcr:createdBy
     */
    public static final Name JCR_CREATEDBY = FACTORY.create(Name.NS_JCR_URI, "createdBy");

    /**
     * jcr:lastModified
     */
    public static final Name JCR_LASTMODIFIED = FACTORY.create(Name.NS_JCR_URI, "lastModified");

    /**
     * jcr:lastModifiedBy
     */
    public static final Name JCR_LASTMODIFIEDBY = FACTORY.create(Name.NS_JCR_URI, "lastModifiedBy");

    /**
     * jcr:encoding
     */
    public static final Name JCR_ENCODING = FACTORY.create(Name.NS_JCR_URI, "encoding");

    /**
     * jcr:mimeType
     */
    public static final Name JCR_MIMETYPE = FACTORY.create(Name.NS_JCR_URI, "mimeType");

    /**
     * jcr:data
     */
    public static final Name JCR_DATA = FACTORY.create(Name.NS_JCR_URI, "data");

    /**
     * jcr:content
     */
    public static final Name JCR_CONTENT = FACTORY.create(Name.NS_JCR_URI, "content");

    /**
     * jcr:etag
     */
    public static final Name JCR_ETAG = FACTORY.create(Name.NS_JCR_URI, "etag");

    /**
     * jcr:protocol
     */
    public static final Name JCR_PROTOCOL = FACTORY.create(Name.NS_JCR_URI, "protocol");

    /**
     * jcr:host
     */
    public static final Name JCR_HOST = FACTORY.create(Name.NS_JCR_URI, "host");

    /**
     * jcr:port
     */
    public static final Name JCR_PORT = FACTORY.create(Name.NS_JCR_URI, "port");

    /**
     * jcr:repository
     */
    public static final Name JCR_REPOSITORY = FACTORY.create(Name.NS_JCR_URI, "repository");

    /**
     * jcr:workspace
     */
    public static final Name JCR_WORKSPACE = FACTORY.create(Name.NS_JCR_URI, "workspace");

    /**
     * jcr:id
     */
    public static final Name JCR_ID = FACTORY.create(Name.NS_JCR_URI, "id");

    //--------------------------------------< xml related item name constants >

    /**
     * jcr:root (dummy name for root node used in XML serialization)
     */
    public static final Name JCR_ROOT = FACTORY.create(Name.NS_JCR_URI, "root");

    /**
     * jcr:xmltext
     */
    public static final Name JCR_XMLTEXT = FACTORY.create(Name.NS_JCR_URI, "xmltext");

    /**
     * jcr:xmlcharacters
     */
    public static final Name JCR_XMLCHARACTERS = FACTORY.create(Name.NS_JCR_URI, "xmlcharacters");

    //-----------------------------------------< query related name constants >

    /**
     * jcr:score
     */
    public static final Name JCR_SCORE = FACTORY.create(Name.NS_JCR_URI, "score");

    /**
     * jcr:path
     */
    public static final Name JCR_PATH = FACTORY.create(Name.NS_JCR_URI, "path");

    /**
     * jcr:statement
     */
    public static final Name JCR_STATEMENT = FACTORY.create(Name.NS_JCR_URI, "statement");

    /**
     * jcr:language
     */
    public static final Name JCR_LANGUAGE = FACTORY.create(Name.NS_JCR_URI, "language");

    //----------------------------------< locking related item name constants >

    /**
     * jcr:lockOwner
     */
    public static final Name JCR_LOCKOWNER = FACTORY.create(Name.NS_JCR_URI, "lockOwner");

    /**
     * jcr:lockIsDeep
     */
    public static final Name JCR_LOCKISDEEP = FACTORY.create(Name.NS_JCR_URI, "lockIsDeep");

    //-------------------------------< versioning related item name constants >

    /**
     * jcr:versionStorage
     */
    public static final Name JCR_VERSIONSTORAGE = FACTORY.create(Name.NS_JCR_URI, "versionStorage");

    /**
     * jcr:mergeFailed
     */
    public static final Name JCR_MERGEFAILED = FACTORY.create(Name.NS_JCR_URI, "mergeFailed");

    /**
     * jcr:frozenNode
     */
    public static final Name JCR_FROZENNODE = FACTORY.create(Name.NS_JCR_URI, "frozenNode");

    /**
     * jcr:frozenUuid
     */
    public static final Name JCR_FROZENUUID = FACTORY.create(Name.NS_JCR_URI, "frozenUuid");

    /**
     * jcr:frozenPrimaryType
     */
    public static final Name JCR_FROZENPRIMARYTYPE = FACTORY.create(Name.NS_JCR_URI, "frozenPrimaryType");

    /**
     * jcr:frozenMixinTypes
     */
    public static final Name JCR_FROZENMIXINTYPES = FACTORY.create(Name.NS_JCR_URI, "frozenMixinTypes");

    /**
     * jcr:predecessors
     */
    public static final Name JCR_PREDECESSORS = FACTORY.create(Name.NS_JCR_URI, "predecessors");

    /**
     * jcr:versionLabels
     */
    public static final Name JCR_VERSIONLABELS = FACTORY.create(Name.NS_JCR_URI, "versionLabels");

    /**
     * jcr:successors
     */
    public static final Name JCR_SUCCESSORS = FACTORY.create(Name.NS_JCR_URI, "successors");

    /**
     * jcr:isCheckedOut
     */
    public static final Name JCR_ISCHECKEDOUT = FACTORY.create(Name.NS_JCR_URI, "isCheckedOut");

    /**
     * jcr:versionHistory
     */
    public static final Name JCR_VERSIONHISTORY = FACTORY.create(Name.NS_JCR_URI, "versionHistory");

    /**
     * jcr:baseVersion
     */
    public static final Name JCR_BASEVERSION = FACTORY.create(Name.NS_JCR_URI, "baseVersion");

    /**
     * jcr:childVersionHistory
     */
    public static final Name JCR_CHILDVERSIONHISTORY = FACTORY.create(Name.NS_JCR_URI, "childVersionHistory");

    /**
     * jcr:rootVersion
     */
    public static final Name JCR_ROOTVERSION = FACTORY.create(Name.NS_JCR_URI, "rootVersion");

    /**
     * jcr:versionableUuid
     */
    public static final Name JCR_VERSIONABLEUUID = FACTORY.create(Name.NS_JCR_URI, "versionableUuid");

    /**
     * jcr:copiedFrom
     * @since 2.0
     */
    public static final Name JCR_COPIEDFROM = FACTORY.create(Name.NS_JCR_URI, "copiedFrom");

    /**
     * jcr:activities
     * @since 2.0
     */
    public static final Name JCR_ACTIVITIES = FACTORY.create(Name.NS_JCR_URI, "activities");

    /**
     * jcr:activity
     * @since 2.0
     */
    public static final Name JCR_ACTIVITY = FACTORY.create(Name.NS_JCR_URI, "activity");

    /**
     * jcr:activityTitle
     * @since 2.0
     */
    public static final Name JCR_ACTIVITY_TITLE = FACTORY.create(Name.NS_JCR_URI, "activityTitle");

    /**
     * jcr:configurations
     * @since 2.0
     */
    public static final Name JCR_CONFIGURATIONS = FACTORY.create(Name.NS_JCR_URI, "configurations");

    /**
     * jcr:configuration
     * @since 2.0
     */
    public static final Name JCR_CONFIGURATION = FACTORY.create(Name.NS_JCR_URI, "configuration");


    //--------------------------------< node type related item name constants >

    /**
     * jcr:nodeTypeName
     */
    public static final Name JCR_NODETYPENAME = FACTORY.create(Name.NS_JCR_URI, "nodeTypeName");

    /**
     * jcr:hasOrderableChildNodes
     */
    public static final Name JCR_HASORDERABLECHILDNODES = FACTORY.create(Name.NS_JCR_URI, "hasOrderableChildNodes");

    /**
     * jcr:isMixin
     */
    public static final Name JCR_ISMIXIN = FACTORY.create(Name.NS_JCR_URI, "isMixin");

    /**
     * jcr:supertypes
     */
    public static final Name JCR_SUPERTYPES = FACTORY.create(Name.NS_JCR_URI, "supertypes");

    /**
     * jcr:propertyDefinition
     */
    public static final Name JCR_PROPERTYDEFINITION = FACTORY.create(Name.NS_JCR_URI, "propertyDefinition");

    /**
     * jcr:name
     */
    public static final Name JCR_NAME = FACTORY.create(Name.NS_JCR_URI, "name");

    /**
     * jcr:mandatory
     */
    public static final Name JCR_MANDATORY = FACTORY.create(Name.NS_JCR_URI, "mandatory");

    /**
     * jcr:protected
     */
    public static final Name JCR_PROTECTED = FACTORY.create(Name.NS_JCR_URI, "protected");

    /**
     * jcr:requiredType
     */
    public static final Name JCR_REQUIREDTYPE = FACTORY.create(Name.NS_JCR_URI, "requiredType");

    /**
     * jcr:onParentVersion
     */
    public static final Name JCR_ONPARENTVERSION = FACTORY.create(Name.NS_JCR_URI, "onParentVersion");

    /**
     * jcr:primaryItemName
     */
    public static final Name JCR_PRIMARYITEMNAME = FACTORY.create(Name.NS_JCR_URI, "primaryItemName");

    /**
     * jcr:multiple
     */
    public static final Name JCR_MULTIPLE = FACTORY.create(Name.NS_JCR_URI, "multiple");

    /**
     * jcr:valueConstraints
     */
    public static final Name JCR_VALUECONSTRAINTS = FACTORY.create(Name.NS_JCR_URI, "valueConstraints");

    /**
     * jcr:defaultValues
     */
    public static final Name JCR_DEFAULTVALUES = FACTORY.create(Name.NS_JCR_URI, "defaultValues");

    /**
     * jcr:autoCreated
     */
    public static final Name JCR_AUTOCREATED = FACTORY.create(Name.NS_JCR_URI, "autoCreated");

    /**
     * jcr:childNodeDefinition
     */
    public static final Name JCR_CHILDNODEDEFINITION = FACTORY.create(Name.NS_JCR_URI, "childNodeDefinition");

    /**
     * jcr:sameNameSiblings
     */
    public static final Name JCR_SAMENAMESIBLINGS = FACTORY.create(Name.NS_JCR_URI, "sameNameSiblings");

    /**
     * jcr:defaultPrimaryType
     */
    public static final Name JCR_DEFAULTPRIMARYTYPE = FACTORY.create(Name.NS_JCR_URI, "defaultPrimaryType");

    /**
     * jcr:requiredPrimaryTypes
     */
    public static final Name JCR_REQUIREDPRIMARYTYPES = FACTORY.create(Name.NS_JCR_URI, "requiredPrimaryTypes");


    //-------------------------------< lifecycle related item name constants >

    /**
     * <code>jcr:lifecyclePolicy</code>: This property is a reference to
     * another node that contains lifecycle policy information.
     * @since JCR 2.0
     */
    public static final Name JCR_LIFECYCLE_POLICY =
        FACTORY.create(Name.NS_JCR_URI, "lifecyclePolicy");

    /**
     * <code>jcr:currentLifecycleState</code>: This property is a string
     * identifying the current lifecycle state of this node.
     * @since JCR 2.0
     */
    public static final Name JCR_CURRENT_LIFECYCLE_STATE =
        FACTORY.create(Name.NS_JCR_URI, "currentLifecycleState");

    //-------------------------------------------< node type name constants >---
    /**
     * nt:unstructured
     */
    public static final Name NT_UNSTRUCTURED = FACTORY.create(Name.NS_NT_URI, "unstructured");

    /**
     * nt:base
     */
    public static final Name NT_BASE = FACTORY.create(Name.NS_NT_URI, "base");

    /**
     * nt:hierarchyNode
     */
    public static final Name NT_HIERARCHYNODE = FACTORY.create(Name.NS_NT_URI, "hierarchyNode");

    /**
     * nt:resource
     */
    public static final Name NT_RESOURCE = FACTORY.create(Name.NS_NT_URI, "resource");

    /**
     * nt:file
     */
    public static final Name NT_FILE = FACTORY.create(Name.NS_NT_URI, "file");

    /**
     * nt:folder
     */
    public static final Name NT_FOLDER = FACTORY.create(Name.NS_NT_URI, "folder");

    /**
     * mix:created
     */
    public static final Name MIX_CREATED = FACTORY.create(Name.NS_MIX_URI, "created");

    /**
     * mix:lastModified
     */
    public static final Name MIX_LASTMODIFIED = FACTORY.create(Name.NS_MIX_URI, "lastModified");

    /**
     * mix:title
     */
    public static final Name MIX_TITLE = FACTORY.create(Name.NS_MIX_URI, "title");

    /**
     * mix:language
     */
    public static final Name MIX_LANGUAGE = FACTORY.create(Name.NS_MIX_URI, "language");

    /**
     * mix:mimeType
     */
    public static final Name MIX_MIMETYPE = FACTORY.create(Name.NS_MIX_URI, "mimeType");

    /**
     * mix:etag
     */
    public static final Name MIX_ETAG = FACTORY.create(Name.NS_MIX_URI, "etag");

    /**
     * nt:address
     */
    public static final Name NT_ADDRESS = FACTORY.create(Name.NS_NT_URI, "address");

    /**
     * nt:query
     */
    public static final Name NT_QUERY = FACTORY.create(Name.NS_NT_URI, "query");

    /**
     * nt:share
     */
    public static final Name NT_SHARE = FACTORY.create(Name.NS_NT_URI, "share");

    /**
     * mix:referenceable
     */
    public static final Name MIX_REFERENCEABLE = FACTORY.create(Name.NS_MIX_URI, "referenceable");
    /**
     * mix:referenceable
     */
    public static final Name MIX_LOCKABLE = FACTORY.create(Name.NS_MIX_URI, "lockable");
    /**
     * mix:versionable
     */
    public static final Name MIX_VERSIONABLE = FACTORY.create(Name.NS_MIX_URI, "versionable");
    /**
     * mix:simpleVersionable
     */
    public static final Name MIX_SIMPLE_VERSIONABLE = FACTORY.create(Name.NS_MIX_URI, "simpleVersionable");
    /**
     * mix:shareable
     */
    public static final Name MIX_SHAREABLE = FACTORY.create(Name.NS_MIX_URI, "shareable");
    /**
     * nt:versionHistory
     */
    public static final Name NT_VERSIONHISTORY = FACTORY.create(Name.NS_NT_URI, "versionHistory");
    /**
     * nt:version
     */
    public static final Name NT_VERSION = FACTORY.create(Name.NS_NT_URI, "version");
    /**
     * nt:versionLabels
     */
    public static final Name NT_VERSIONLABELS = FACTORY.create(Name.NS_NT_URI, "versionLabels");
    /**
     * nt:versionedChild
     */
    public static final Name NT_VERSIONEDCHILD = FACTORY.create(Name.NS_NT_URI, "versionedChild");
    /**
     * nt:frozenNode
     */
    public static final Name NT_FROZENNODE = FACTORY.create(Name.NS_NT_URI, "frozenNode");
    /**
     * nt:nodeType
     */
    public static final Name NT_NODETYPE = FACTORY.create(Name.NS_NT_URI, "nodeType");
    /**
     * nt:propertyDefinition
     */
    public static final Name NT_PROPERTYDEFINITION = FACTORY.create(Name.NS_NT_URI, "propertyDefinition");
    /**
     * nt:childNodeDefinition
     */
    public static final Name NT_CHILDNODEDEFINITION = FACTORY.create(Name.NS_NT_URI, "childNodeDefinition");

    /**
     * <code>mix:lifecycle</code>: Only nodes with mixin node type
     * <code>mix:lifecycle</code> may participate in a lifecycle.
     * @since JCR 2.0
     */
    public static final Name MIX_LIFECYCLE =
        FACTORY.create(Name.NS_MIX_URI, "lifecycle");

    /**
     * nt:activity
     * @since 2.0
     */
    public static final Name NT_ACTIVITY = FACTORY.create(Name.NS_NT_URI, "activity");

    /**
     * nt:configuration
     * @since 2.0
     */
    public static final Name NT_CONFIGURATION = FACTORY.create(Name.NS_NT_URI, "configuration");

    //--------------------------------------------------------------------------
    /**
     * rep:root
     */
    public static final Name REP_ROOT = FACTORY.create(Name.NS_REP_URI, "root");

    /**
     * rep:system
     */
    public static final Name REP_SYSTEM = FACTORY.create(Name.NS_REP_URI, "system");

    /**
     * rep:versionStorage
     */
    public static final Name REP_VERSIONSTORAGE = FACTORY.create(Name.NS_REP_URI, "versionStorage");

    /**
     * rep:Activities
     */
    public static final Name REP_ACTIVITIES = FACTORY.create(Name.NS_REP_URI, "Activities");

    /**
     * rep:Configurations
     */
    public static final Name REP_CONFIGURATIONS = FACTORY.create(Name.NS_REP_URI, "Configurations");

    /**
     * rep:baseVersions
     */
    public static final Name REP_BASEVERSIONS = FACTORY.create(Name.NS_REP_URI, "baseVersions");

    /**
     * rep:VersionReference
     */
    public static final Name REP_VERSION_REFERENCE = FACTORY.create(Name.NS_REP_URI, "VersionReference");

    /**
     * rep:versions
     */
    public static final Name REP_VERSIONS = FACTORY.create(Name.NS_REP_URI, "versions");
    
    /**
     * rep:nodeTypes
     */
    public static final Name REP_NODETYPES = FACTORY.create(Name.NS_REP_URI, "nodeTypes");

    /**
     * The special wildcard name used as the name of residual item definitions.
     */
    public static final Name ANY_NAME = FACTORY.create("", "*");

    //------------------------------------------< system view name constants >
    /**
     * sv:node
     */
    public static final Name SV_NODE = FACTORY.create(Name.NS_SV_URI, "node");
    /**
     * sv:property
     */
    public static final Name SV_PROPERTY = FACTORY.create(Name.NS_SV_URI, "property");
    /**
     * sv:value
     */
    public static final Name SV_VALUE = FACTORY.create(Name.NS_SV_URI, "value");
    /**
     * sv:type
     */
    public static final Name SV_TYPE = FACTORY.create(Name.NS_SV_URI, "type");
    /**
     * sv:name
     */
    public static final Name SV_NAME = FACTORY.create(Name.NS_SV_URI, "name");


}