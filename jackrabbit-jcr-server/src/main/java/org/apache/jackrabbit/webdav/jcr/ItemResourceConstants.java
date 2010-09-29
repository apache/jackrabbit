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

import org.apache.jackrabbit.commons.webdav.JcrRemotingConstants;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.observation.ObservationResource;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.search.SearchResource;
import org.apache.jackrabbit.webdav.security.Privilege;
import org.apache.jackrabbit.webdav.version.DeltaVResource;
import org.apache.jackrabbit.webdav.xml.Namespace;

/**
 * <code>ItemResourceConstants</code> provides constants for any resources
 * representing repository items.
 */
public interface ItemResourceConstants extends JcrRemotingConstants {

    /**
     * Methods common to all item resources.
     */
    public static final String METHODS = DavResource.METHODS + ", " + ObservationResource.METHODS + ", " + SearchResource.METHODS + ", " +DeltaVResource.METHODS;

    /**
     * The namespace for all jcr specific extensions.
     */
    public static final Namespace NAMESPACE = Namespace.getNamespace(NS_PREFIX, NS_URI);

    /**
     * Extension to the WebDAV 'exclusive' lock, that allows to distinguish
     * the session-scoped and open-scoped locks on a JCR node.
     *
     * @see javax.jcr.Node#lock(boolean, boolean)
     */
    public static final Scope EXCLUSIVE_SESSION = Scope.create(XML_EXCLUSIVE_SESSION_SCOPED, NAMESPACE);

    // name of the workspace for which the repository session has been created.
    public static final DavPropertyName JCR_WORKSPACE_NAME = DavPropertyName.create(JCR_WORKSPACE_NAME_LN, NAMESPACE);

    // general property names
    public static final DavPropertyName JCR_NAME = DavPropertyName.create(JCR_NAME_LN, NAMESPACE);
    public static final DavPropertyName JCR_PATH = DavPropertyName.create(JCR_PATH_LN, NAMESPACE);
    public static final DavPropertyName JCR_DEPTH = DavPropertyName.create(JCR_DEPTH_LN, NAMESPACE);
    public static final DavPropertyName JCR_PARENT = DavPropertyName.create(JCR_PARENT_LN, NAMESPACE);
    public static final DavPropertyName JCR_ISNEW = DavPropertyName.create(JCR_ISNEW_LN, NAMESPACE);
    public static final DavPropertyName JCR_ISMODIFIED = DavPropertyName.create(JCR_ISMODIFIED_LN, NAMESPACE);
    public static final DavPropertyName JCR_DEFINITION = DavPropertyName.create(JCR_DEFINITION_LN, NAMESPACE);
    public static final DavPropertyName JCR_SELECTOR_NAME = DavPropertyName.create(JCR_SELECTOR_NAME_LN, NAMESPACE);

    // property names used for resources representing jcr-nodes
    public static final DavPropertyName JCR_PRIMARYNODETYPE = DavPropertyName.create(JCR_PRIMARYNODETYPE_LN, NAMESPACE);
    public static final DavPropertyName JCR_MIXINNODETYPES = DavPropertyName.create(JCR_MIXINNODETYPES_LN, NAMESPACE);
    public static final DavPropertyName JCR_INDEX = DavPropertyName.create(JCR_INDEX_LN, NAMESPACE);
    public static final DavPropertyName JCR_REFERENCES = DavPropertyName.create(JCR_REFERENCES_LN, NAMESPACE);
    /**
     * @since JCR 2.0
     */
    public static final DavPropertyName JCR_WEAK_REFERENCES = DavPropertyName.create(JCR_WEAK_REFERENCES_LN, NAMESPACE);
    public static final DavPropertyName JCR_UUID = DavPropertyName.create(JCR_UUID_LN, NAMESPACE);
    public static final DavPropertyName JCR_PRIMARYITEM = DavPropertyName.create(JCR_PRIMARYITEM_LN, NAMESPACE);

    // property names used for resources representing jcr-properties
    public static final DavPropertyName JCR_TYPE = DavPropertyName.create(JCR_TYPE_LN, NAMESPACE);
    public static final DavPropertyName JCR_VALUE = DavPropertyName.create(JCR_VALUE_LN, NAMESPACE);
    public static final DavPropertyName JCR_VALUES = DavPropertyName.create(JCR_VALUES_LN, NAMESPACE);
    public static final DavPropertyName JCR_LENGTH = DavPropertyName.create(JCR_LENGTH_LN, NAMESPACE);
    public static final DavPropertyName JCR_LENGTHS = DavPropertyName.create(JCR_LENGTHS_LN, NAMESPACE);

    // property names used for resource representing a workspace
    public static final DavPropertyName JCR_NAMESPACES = DavPropertyName.create(JCR_NAMESPACES_LN, NAMESPACE);

    // property names used for resource representing a version history
    public static final DavPropertyName JCR_VERSIONABLEUUID = DavPropertyName.create(JCR_VERSIONABLEUUID_LN, NAMESPACE);

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
