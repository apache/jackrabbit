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
 * <code>JcrRemotingConstants</code> provides string constants for WebDAV
 * resources representing repository items.
 */
public interface JcrRemotingConstants {

    /**
     * Namespace prefix used for Jackrabbit specific WebDAV extensions related
     * to JCR remoting.
     * @see #NS_URI
     */
    public static final String NS_PREFIX = "dcr";
    
    /**
     * Namespace uri used for Jackrabbit specific WebDAV extensions related
     * to JCR remoting.
     * @see #NS_PREFIX
     */
    public static final String NS_URI = "http://www.day.com/jcr/webdav/1.0";

    /**
     * The resource path of the root-item-resource.
     */
    public static final String ROOT_ITEM_PATH = "/";
    /**
     * Placeholder resource path for the JCR root node.
     */
    public static final String ROOT_ITEM_RESOURCEPATH = "/jcr:root";

    /**
     * The version storage item resource path.
     */
    public static final String VERSIONSTORAGE_PATH = "/jcr:system/jcr:versionStorage";

    public static final String IMPORT_UUID_BEHAVIOR = "ImportUUIDBehavior";

    // xml element names
    public static final String XML_PRIMARYNODETYPE = "primarynodetype";
    public static final String XML_VALUE = "value";
    /**
     * 'type' attribute for the {@link #XML_VALUE value} element, reflecting the
     * {@link javax.jcr.PropertyType type} of the value being transported.
     */
    public static final String ATTR_VALUE_TYPE = "type";
    public static final String XML_LENGTH = "length";
    public static final String XML_EXCLUSIVE_SESSION_SCOPED = "exclusive-session-scoped";

    // xml elements used to reflect the workspaces ns-registry
    public static final String XML_NAMESPACE = "namespace";
    public static final String XML_PREFIX = "prefix";
    public static final String XML_URI = "uri";

    // xml elements used for repository-descriptors report
    public static final String XML_DESCRIPTOR = "descriptor";
    public static final String XML_DESCRIPTORKEY = "descriptorkey";
    public static final String XML_DESCRIPTORVALUE = "descriptorvalue";

    // xml elements used for node type registration
    public static final String XML_CND = "cnd";
    public static final String XML_ALLOWUPDATE = "allowupdate";
    public static final String XML_NODETYPENAME = "nodetypename";

    /**
     * The 'removeexisting' element is not defined by RFC 3253. If it is present
     * in the UPDATE request body, uuid conflicts should be solved by removing
     * the existing nodes.
     *
     * @see javax.jcr.Node#restore(javax.jcr.version.Version, boolean)
     * @see javax.jcr.Workspace#restore(javax.jcr.version.Version[], boolean)
     */
    public static final String XML_REMOVEEXISTING = "removeexisting";

    /**
     * The 'relpath' element is not defined by RFC 3253. If it is present
     * in the UPDATE request body, the server is forced to used the text contained
     * as 'relPath' argument for the {@link javax.jcr.Node#restore(javax.jcr.version.Version, String, boolean)
     * Node.restore} call.
     *
     * @see javax.jcr.Node#restore(javax.jcr.version.Version, String, boolean)
     */
    public static final String XML_RELPATH = "relpath";

    // property local name of the workspace for which the repository session has been created.
    public static final String JCR_WORKSPACE_NAME_LN = "workspaceName";

    // general property local names
    public static final String JCR_NAME_LN = "name";
    public static final String JCR_PATH_LN = "path";
    public static final String JCR_DEPTH_LN = "depth";
    public static final String JCR_PARENT_LN = "parent";
    public static final String JCR_ISNEW_LN = "isnew";
    public static final String JCR_ISMODIFIED_LN = "ismodified";
    public static final String JCR_DEFINITION_LN = "definition";
    public static final String JCR_SELECTOR_NAME_LN = "selectorName";

    // property local names used for resources representing jcr-nodes
    public static final String JCR_PRIMARYNODETYPE_LN = XML_PRIMARYNODETYPE;
    public static final String JCR_MIXINNODETYPES_LN = "mixinnodetypes";
    public static final String JCR_INDEX_LN = "index";
    public static final String JCR_REFERENCES_LN = "references";
    /**
     * @since JCR 2.0
     */
    public static final String JCR_WEAK_REFERENCES_LN = "weakreferences";
    public static final String JCR_UUID_LN = "uuid";
    public static final String JCR_PRIMARYITEM_LN = "primaryitem";

    // property local names used for resources representing jcr-properties
    public static final String JCR_TYPE_LN = "type";
    public static final String JCR_VALUE_LN = "value";
    public static final String JCR_VALUES_LN = "values";
    public static final String JCR_LENGTH_LN = "length";
    public static final String JCR_LENGTHS_LN = "lengths";
    public static final String JCR_GET_STRING_LN = "getstring";

    public static final String JCR_NAMESPACES_LN = "namespaces";
    public static final String JCR_NODETYPES_CND_LN = "nodetypes-cnd";

    // property local names used for resource representing a version history
    public static final String JCR_VERSIONABLEUUID_LN = "versionableuuid";

    // property local names related to query
    public static final String JCR_QUERY_RESULT_LN = "search-result-property";

    // name of the xml element containing the result columns.
    public static final String XML_QUERY_RESULT_COLUMN = "column";

    public static final String REPORT_EXPORT_VIEW = "exportview";
    public static final String REPORT_PRIVILEGES = "privileges";
    public static final String REPORT_LOCATE_BY_UUID = "locate-by-uuid";
    public static final String REPORT_LOCATE_CORRESPONDING_NODE = "locate-corresponding-node";
    public static final String REPORT_NODETYPES = "nodetypes";
    public static final String REPORT_REGISTERED_NAMESPACES = "registerednamespaces";
    public static final String REPORT_REPOSITORY_DESCRIPTORS = "repositorydescriptors";

    /**
     * RFC 5988 relation type for user data
     * <p>
     * Used to transport JCR User Data inside an HTTP request.
     * <p>
     * Example:
     * 
     * <pre>
     * Link: &lt;data:,my%20user%data&gt;, rel="<i>RELATION_USER_DATA</i>"
     * </pre>
     */
    public static final String RELATION_USER_DATA = NS_URI + "/user-data";

    /**
     * RFC 5988 relation type for remote session identification
     * <p>
     * Used to transport an identifier for the remote session.
     * <p>
     * Example:
     * 
     * <pre>
     * Link: &lt;urn:uuid:96d3c6fe-1073-11e1-a3c0-00059a3c7a00&gt;, rel="<i>RELATION_REMOTE_SESSION_ID</i>"
     * </pre>
     */
    public static final String RELATION_REMOTE_SESSION_ID = NS_URI + "/session-id";
}