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
package org.apache.jackrabbit.webdav.jcr;

import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.observation.ObservationResource;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.search.SearchResource;
import org.apache.jackrabbit.webdav.security.Privilege;
import org.apache.jackrabbit.webdav.version.DeltaVResource;
import org.apache.jackrabbit.webdav.xml.Namespace;

import javax.jcr.PropertyType;

/**
 * <code>ItemResourceConstants</code> provides constants for any resources
 * representing repository items.
 */
public interface ItemResourceConstants {

    /**
     * Methods common to all item resources.
     */
    public static final String METHODS = DavResource.METHODS + ", " + ObservationResource.METHODS + ", " + SearchResource.METHODS + ", " +DeltaVResource.METHODS;

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

    /**
     * The namespace for all jcr specific extensions.
     */
    public static final Namespace NAMESPACE = Namespace.getNamespace("dcr", "http://www.day.com/jcr/webdav/1.0");

    public static final String IMPORT_UUID_BEHAVIOR = "ImportUUIDBehavior";

    // xml element names
    public static final String XML_PRIMARYNODETYPE = "primarynodetype";
    public static final String XML_VALUE = "value";
    /**
     * 'type' attribute for the {@link #XML_VALUE value} element, reflecting the
     * {@link PropertyType type} of the value being transported.
     */
    public static final String ATTR_VALUE_TYPE = "type";
    public static final String XML_LENGTH = "length";
    public static final String XML_EXCLUSIVE_SESSION_SCOPED = "exclusive-session-scoped";

    // xml elements used to reflect the workspaces ns-registry
    // TODO: to be reviewed...
    public static final String XML_NAMESPACE = "namespace";
    public static final String XML_PREFIX = "prefix";
    public static final String XML_URI = "uri";

    // xml elements used for repository-descritors report
    public static final String XML_DESCRIPTOR = "descriptor";
    public static final String XML_DESCRIPTORKEY = "descriptorkey";
    public static final String XML_DESCRIPTORVALUE = "descriptorvalue";

    /**
     * Extension to the WebDAV 'exclusive' lock, that allows to distinguish
     * the session-scoped and open-scoped locks on a JCR node.
     *
     * @see javax.jcr.Node#lock(boolean, boolean)
     */
    public static final Scope EXCLUSIVE_SESSION = Scope.create(XML_EXCLUSIVE_SESSION_SCOPED, NAMESPACE);

    /**
     * The 'removeexisting' element is not defined by RFC 3253. If it is present
     * in the UPDATE request body, uuid conflicts should be solved by removing
     * the existing nodes.
     *
     * @see javax.jcr.Node#restore(javax.jcr.version.Version, boolean)
     * @see javax.jcr.Workspace#restore(javax.jcr.version.Version[], boolean)
     * @see org.apache.jackrabbit.webdav.version.UpdateInfo
     */
    public static final String XML_REMOVEEXISTING = "removeexisting";

    /**
     * The 'relpath' element is not defined by RFC 3253. If it is present
     * in the UPDATE request body, the server is forced to used the text contained
     * as 'relPath' argument for the {@link javax.jcr.Node#restore(javax.jcr.version.Version, String, boolean)
     * Node.restore} call.
     *
     * @see javax.jcr.Node#restore(javax.jcr.version.Version, String, boolean)
     * @see org.apache.jackrabbit.webdav.version.UpdateInfo
     */
    public static final String XML_RELPATH = "relpath";

    // name of the workspace for which the repository session has been created.
    public static final DavPropertyName JCR_WORKSPACE_NAME = DavPropertyName.create("workspaceName", NAMESPACE);

    // general property names
    public static final DavPropertyName JCR_NAME = DavPropertyName.create("name", NAMESPACE);
    public static final DavPropertyName JCR_PATH = DavPropertyName.create("path", NAMESPACE);
    public static final DavPropertyName JCR_DEPTH = DavPropertyName.create("depth", NAMESPACE);
    public static final DavPropertyName JCR_PARENT = DavPropertyName.create("parent", NAMESPACE);
    public static final DavPropertyName JCR_ISNEW = DavPropertyName.create("isnew", NAMESPACE);
    public static final DavPropertyName JCR_ISMODIFIED = DavPropertyName.create("ismodified", NAMESPACE);
    public static final DavPropertyName JCR_DEFINITION = DavPropertyName.create("definition", NAMESPACE);
    public static final DavPropertyName JCR_SELECTOR_NAME = DavPropertyName.create("selectorName", NAMESPACE);

    // property names used for resources representing jcr-nodes
    public static final DavPropertyName JCR_PRIMARYNODETYPE = DavPropertyName.create(XML_PRIMARYNODETYPE, NAMESPACE);
    public static final DavPropertyName JCR_MIXINNODETYPES = DavPropertyName.create("mixinnodetypes", NAMESPACE);
    public static final DavPropertyName JCR_INDEX = DavPropertyName.create("index", NAMESPACE);
    public static final DavPropertyName JCR_REFERENCES = DavPropertyName.create("references", NAMESPACE);
    /**
     * @since JCR 2.0
     */
    public static final DavPropertyName JCR_WEAK_REFERENCES = DavPropertyName.create("weakreferences", NAMESPACE);
    public static final DavPropertyName JCR_UUID = DavPropertyName.create("uuid", NAMESPACE);
    public static final DavPropertyName JCR_PRIMARYITEM = DavPropertyName.create("primaryitem", NAMESPACE);

    // property names used for resources representing jcr-properties
    public static final DavPropertyName JCR_TYPE = DavPropertyName.create("type", NAMESPACE);
    public static final DavPropertyName JCR_VALUE = DavPropertyName.create("value", NAMESPACE);
    public static final DavPropertyName JCR_VALUES = DavPropertyName.create("values", NAMESPACE);
    public static final DavPropertyName JCR_LENGTH = DavPropertyName.create("length", NAMESPACE);
    public static final DavPropertyName JCR_LENGTHS = DavPropertyName.create("lengths", NAMESPACE);

    // property names used for resource representing a workspace
    public static final DavPropertyName JCR_NAMESPACES = DavPropertyName.create("namespaces", NAMESPACE);

    // property names used for resource representing a version hisotry
    public static final DavPropertyName JCR_VERSIONABLEUUID = DavPropertyName.create("versionableuuid", NAMESPACE);

    //-----------------------------------------< JSR170 specific privileges >---
    /**
     * Privilege representing the JSR170 'read' action.
     */
    public static final Privilege PRIVILEGE_JCR_READ = Privilege.getPrivilege("read", NAMESPACE);
    /**
     * Privilege representing the JSR170 'add_node' action.
     */
    public static final Privilege PRIVILEGE_JCR_ADD_NODE = Privilege.getPrivilege("add_node", NAMESPACE);
    /**
     * Privilege representing the JSR170 'set_property' action.
     */
    public static final Privilege PRIVILEGE_JCR_SET_PROPERTY = Privilege.getPrivilege("set_property", NAMESPACE);
    /**
     * Privilege representing the JSR170 'remove' action.
     */
    public static final Privilege PRIVILEGE_JCR_REMOVE = Privilege.getPrivilege("remove", NAMESPACE);
}