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
package org.apache.jackrabbit;

import javax.jcr.Session;

/**
 * This interface defines some of the item names that are defined in the <a
 * href="https://s.apache.org/jcr-1.0-javadoc/">JCR Specification 1.0</a> in the <i>qualified</i> form,
 * using the default prefixes {@code jcr}, {@code nt} and {@code mix}.
 * <p>
 * Please note that those prefixes can by redefined by an application using the
 * {@link Session#setNamespacePrefix(String, String)} method. As a result, the
 * constants may not refer to the respective items.
 * <p>
 * On the other hand, the constants in {@link javax.jcr.nodetype.NodeType},
 * {@link javax.jcr.Node}, {@link javax.jcr.Property} and {@link javax.jcr.Workspace}
 * are more complete (covering <a href="https://s.apache.org/jcr-2.0-javadoc/">JCR 2.0</a>
 * as well) and also define names using <i>expanded</i> form, which is immune to session local
 * remappings, so it is recommended to use those constants instead whenever possible.
 */
public interface JcrConstants {
    /**
     * Use {@link javax.jcr.Property#JCR_AUTOCREATED} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_AUTOCREATED = "jcr:autoCreated";
    /**
     * Use {@link javax.jcr.Property#JCR_BASE_VERSION} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_BASEVERSION = "jcr:baseVersion";
    
    public static final String JCR_CHILD = "jcr:child";
    /**
     * Use {@link javax.jcr.Node#JCR_CHILD_NODE_DEFINITION} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_CHILDNODEDEFINITION = "jcr:childNodeDefinition";
    /**
     * Use {@link javax.jcr.Node#JCR_CONTENT} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_CONTENT = "jcr:content";
    /**
     * Use {@link javax.jcr.Property#JCR_CREATED} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_CREATED = "jcr:created";
    /**
     * Use {@link javax.jcr.Property#JCR_DATA} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_DATA = "jcr:data";
    /**
     * Use {@link javax.jcr.Property#JCR_DEFAULT_PRIMARY_TYPE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_DEFAULTPRIMARYTYPE = "jcr:defaultPrimaryType";
    /**
     * Use {@link javax.jcr.Property#JCR_DEFAULT_VALUES} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_DEFAULTVALUES = "jcr:defaultValues";
    /**
     * Use {@link javax.jcr.Property#JCR_ENCODING} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_ENCODING = "jcr:encoding";
    /**
     * Use {@link javax.jcr.Property#JCR_FROZEN_MIXIN_TYPES} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_FROZENMIXINTYPES = "jcr:frozenMixinTypes";
    /**
     * Use {@link javax.jcr.Node#JCR_FROZEN_NODE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_FROZENNODE = "jcr:frozenNode";
    /**
     * Use {@link javax.jcr.Property#JCR_FROZEN_PRIMARY_TYPE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_FROZENPRIMARYTYPE = "jcr:frozenPrimaryType";
    /**
     * Use {@link javax.jcr.Property#JCR_FROZEN_UUID} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_FROZENUUID = "jcr:frozenUuid";
    /**
     * Use {@link javax.jcr.Property#JCR_HAS_ORDERABLE_CHILD_NODES} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_HASORDERABLECHILDNODES = "jcr:hasOrderableChildNodes";
    /**
     * Use {@link javax.jcr.Property#JCR_IS_CHECKED_OUT} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_ISCHECKEDOUT = "jcr:isCheckedOut";
    /**
     * Use {@link javax.jcr.Property#JCR_IS_MIXIN} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_ISMIXIN = "jcr:isMixin";
    /**
     * Use {@link javax.jcr.Property#JCR_LANGUAGE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_LANGUAGE = "jcr:language";
    /**
     * Use {@link javax.jcr.Property#JCR_LAST_MODIFIED} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_LASTMODIFIED = "jcr:lastModified";
    /**
     * Use {@link javax.jcr.Property#JCR_LOCK_IS_DEEP} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_LOCKISDEEP = "jcr:lockIsDeep";
    /**
     * Use {@link javax.jcr.Property#JCR_LOCK_OWNER} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_LOCKOWNER = "jcr:lockOwner";
    /**
     * Use {@link javax.jcr.Property#JCR_MANDATORY} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_MANDATORY = "jcr:mandatory";
    /**
     * Use {@link javax.jcr.Property#JCR_MERGE_FAILED} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_MERGEFAILED = "jcr:mergeFailed";
    /**
     * Use {@link javax.jcr.Property#JCR_MIMETYPE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_MIMETYPE = "jcr:mimeType";
    /**
     * Use {@link javax.jcr.Property#JCR_MIXIN_TYPES} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_MIXINTYPES = "jcr:mixinTypes";
    /**
     * Use {@link javax.jcr.Property#JCR_MULTIPLE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_MULTIPLE = "jcr:multiple";
    /**
     * Use {@link javax.jcr.Property#JCR_NAME} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_NAME = "jcr:name";
    /**
     * Use {@link javax.jcr.Property#JCR_NODE_TYPE_NAME} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_NODETYPENAME = "jcr:nodeTypeName";
    /**
     * Use {@link javax.jcr.Property#JCR_ON_PARENT_VERSION} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_ONPARENTVERSION = "jcr:onParentVersion";
    /**
     * Use {@link javax.jcr.Property#JCR_PREDECESSORS} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_PREDECESSORS = "jcr:predecessors";
    /**
     * Use {@link javax.jcr.Property#JCR_PRIMARY_ITEM_NAME} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_PRIMARYITEMNAME = "jcr:primaryItemName";
    /**
     * Use {@link javax.jcr.Property#JCR_PRIMARY_TYPE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_PRIMARYTYPE = "jcr:primaryType";
    /**
     * Use {@link javax.jcr.Node#JCR_PROPERTY_DEFINITION} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_PROPERTYDEFINITION = "jcr:propertyDefinition";
    /**
     * Use {@link javax.jcr.Property#JCR_PROTECTED} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_PROTECTED = "jcr:protected";
    /**
     * Use {@link javax.jcr.Property#JCR_REQUIRED_PRIMARY_TYPES} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_REQUIREDPRIMARYTYPES = "jcr:requiredPrimaryTypes";
    /**
     * Use {@link javax.jcr.Property#JCR_REQUIRED_TYPE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_REQUIREDTYPE = "jcr:requiredType";
    /**
     * Use {@link javax.jcr.Node#JCR_ROOT_VERSION} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_ROOTVERSION = "jcr:rootVersion";
    /**
     * jcr:sameNameSiblings
     * Use {@link javax.jcr.Property#JCR_SAME_NAME_SIBLINGS} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_SAMENAMESIBLINGS = "jcr:sameNameSiblings";
    /**
     * Use {@link javax.jcr.Property#JCR_STATEMENT} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_STATEMENT = "jcr:statement";
    /**
     * Use {@link javax.jcr.Property#JCR_SUCCESSORS} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_SUCCESSORS = "jcr:successors";
    /**
     * Use {@link javax.jcr.Property#JCR_SUPERTYPES} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_SUPERTYPES = "jcr:supertypes";
    /**
     * Use {@link javax.jcr.Workspace#NAME_SYSTEM_NODE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_SYSTEM = "jcr:system";
    /**
     * Use {@link javax.jcr.Property#JCR_TITLE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_TITLE = "jcr:title";
    /**
     * Use {@link javax.jcr.Property#JCR_UUID} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_UUID = "jcr:uuid";
    /**
     * Use {@link javax.jcr.Property#JCR_VALUE_CONSTRAINTS} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_VALUECONSTRAINTS = "jcr:valueConstraints";
    /**
     * Use {@link javax.jcr.Property#JCR_VERSION_HISTORY} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_VERSIONHISTORY = "jcr:versionHistory";
    /**
     * Use {@link javax.jcr.Node#JCR_VERSION_LABELS} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_VERSIONLABELS = "jcr:versionLabels";
    /**
     * Use {@link javax.jcr.Workspace#NAME_VERSION_STORAGE_NODE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_VERSIONSTORAGE = "jcr:versionStorage";
    /**
     * Use {@link javax.jcr.Property#JCR_VERSIONABLE_UUID} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String JCR_VERSIONABLEUUID = "jcr:versionableUuid";

    /**
     * Pseudo property jcr:path used with query results
     */
    public static final String JCR_PATH = "jcr:path";
    /**
     * Pseudo property jcr:score used with query results
     */
    public static final String JCR_SCORE = "jcr:score";

    /**
     * Use {@link javax.jcr.nodetype.NodeType#MIX_CREATED} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String MIX_CREATED = "mix:created";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#MIX_LANGUAGE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String MIX_LANGUAGE = "mix:language";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#MIX_LAST_MODIFIED} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String MIX_LAST_MODIFIED = "mix:lastModified";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#MIX_LIFECYCLE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String MIX_LIFECYCLE = "mix:lifecycle";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#MIX_LOCKABLE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String MIX_LOCKABLE = "mix:lockable";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#MIX_MIMETYPE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String MIX_MIMETYPE = "mix:mimetype";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#MIX_REFERENCEABLE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String MIX_REFERENCEABLE = "mix:referenceable";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#MIX_SIMPLE_VERSIONABLE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String MIX_SIMPLE_VERSIONABLE = "mix:simpleVersionable";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#MIX_VERSIONABLE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String MIX_VERSIONABLE = "mix:versionable";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#MIX_SHAREABLE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String MIX_SHAREABLE = "mix:shareable";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#MIX_TITLE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String MIX_TITLE = "mix:title";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#NT_BASE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String NT_BASE = "nt:base";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#NT_CHILD_NODE_DEFINITION} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String NT_CHILDNODEDEFINITION = "nt:childNodeDefinition";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#NT_FILE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String NT_FILE = "nt:file";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#NT_FOLDER} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String NT_FOLDER = "nt:folder";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#NT_FROZEN_NODE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String NT_FROZENNODE = "nt:frozenNode";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#NT_HIERARCHY_NODE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String NT_HIERARCHYNODE = "nt:hierarchyNode";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#NT_LINKED_FILE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String NT_LINKEDFILE = "nt:linkedFile";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#NT_NODE_TYPE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String NT_NODETYPE = "nt:nodeType";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#NT_PROPERTY_DEFINITION} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String NT_PROPERTYDEFINITION = "nt:propertyDefinition";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#NT_QUERY} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String NT_QUERY = "nt:query";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#NT_RESOURCE} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String NT_RESOURCE = "nt:resource";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#NT_UNSTRUCTURED} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String NT_UNSTRUCTURED = "nt:unstructured";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#NT_VERSION} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String NT_VERSION = "nt:version";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#NT_VERSION_HISTORY} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String NT_VERSIONHISTORY = "nt:versionHistory";
    /**
     * nt:versionLabels
     */
    public static final String NT_VERSIONLABELS = "nt:versionLabels";
    /**
     * Use {@link javax.jcr.nodetype.NodeType#NT_VERSIONED_CHILD} whenever <a href="https://s.apache.org/jcr-2.0-spec/3_Repository_Model.html#3.2.6%20Use%20of%20Qualified%20and%20Expanded%20Names">expanded JCR names are supported (e.g. in JCR API method parameters)</a>.
     */
    public static final String NT_VERSIONEDCHILD = "nt:versionedChild";
}
