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
package org.apache.jackrabbit.webdav.jcr.property;

import org.apache.jackrabbit.webdav.jcr.ItemResourceConstants;
import org.apache.jackrabbit.webdav.observation.ObservationConstants;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.apache.jackrabbit.webdav.version.VersionControlledResource;
import org.apache.jackrabbit.webdav.version.VersionHistoryResource;
import org.apache.jackrabbit.webdav.version.VersionResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>JcrDavPropertyNameSet</code>...
 */
public final class JcrDavPropertyNameSet implements ItemResourceConstants {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(JcrDavPropertyNameSet.class);


    //------------------------------------------------< property name sets >----
    /**
     * Default property names present with all resources.
     */
    public static final DavPropertyNameSet BASE_SET = new DavPropertyNameSet();
    static {
        BASE_SET.add(DavPropertyName.DISPLAYNAME);
        BASE_SET.add(DavPropertyName.RESOURCETYPE);
        BASE_SET.add(DavPropertyName.ISCOLLECTION);
        BASE_SET.add(DavPropertyName.GETLASTMODIFIED);
        BASE_SET.add(DavPropertyName.CREATIONDATE);
        BASE_SET.add(DavPropertyName.SUPPORTEDLOCK);
        BASE_SET.add(DavPropertyName.LOCKDISCOVERY);
        BASE_SET.add(DeltaVConstants.SUPPORTED_METHOD_SET);
        BASE_SET.add(DeltaVConstants.SUPPORTED_REPORT_SET);
        BASE_SET.add(DeltaVConstants.CREATOR_DISPLAYNAME);
        BASE_SET.add(DeltaVConstants.COMMENT);
        BASE_SET.add(JCR_WORKSPACE_NAME);
    }

    /**
     * Property names defined for JCR workspace resources.
     */
    public static final DavPropertyNameSet WORKSPACE_SET = new DavPropertyNameSet();
    static {
        WORKSPACE_SET.add(DeltaVConstants.WORKSPACE);
        WORKSPACE_SET.add(JCR_NAMESPACES);
        WORKSPACE_SET.add(JCR_NODETYPES_CND);
    }

    /**
     * Additional property names defined for existing and non-existing item resources.
     */
    public static final DavPropertyNameSet ITEM_BASE_SET = new DavPropertyNameSet();
    static {
        ITEM_BASE_SET.add(DavPropertyName.GETCONTENTTYPE);        
        ITEM_BASE_SET.add(DeltaVConstants.WORKSPACE);
        ITEM_BASE_SET.add(ObservationConstants.SUBSCRIPTIONDISCOVERY);
    }

    /**
     * Additional property names defined for existing item resources.
     */
    public static final DavPropertyNameSet EXISTING_ITEM_BASE_SET = new DavPropertyNameSet(ITEM_BASE_SET);
    static {
        EXISTING_ITEM_BASE_SET.add(JCR_NAME);
        EXISTING_ITEM_BASE_SET.add(JCR_PATH);
        EXISTING_ITEM_BASE_SET.add(JCR_DEPTH);
        EXISTING_ITEM_BASE_SET.add(JCR_DEFINITION);
    }

    /**
     * Additional property names defined by single value JCR properties.
     */
    public static final DavPropertyNameSet PROPERTY_SET = new DavPropertyNameSet();
    static {
        PROPERTY_SET.add(JCR_TYPE);
        PROPERTY_SET.add(JCR_VALUE);
        PROPERTY_SET.add(JCR_LENGTH);
    }

    /**
     * Additional property names defined by single value JCR properties.
     */
    public static final DavPropertyNameSet PROPERTY_MV_SET = new DavPropertyNameSet();
    static {
        PROPERTY_MV_SET.add(JCR_TYPE);
        PROPERTY_MV_SET.add(JCR_VALUES);
        PROPERTY_MV_SET.add(JCR_LENGTHS);
    }

    /**
     * Additional property names defined by regular JCR nodes.
     */
    public static final DavPropertyNameSet NODE_SET = new DavPropertyNameSet();
    static {
        NODE_SET.add(JCR_PRIMARYNODETYPE);
        NODE_SET.add(JCR_MIXINNODETYPES);
        NODE_SET.add(JCR_INDEX);
        NODE_SET.add(JCR_REFERENCES);
        NODE_SET.add(JCR_WEAK_REFERENCES);
    }

    /**
     * Additional property names defined by versionable JCR nodes.
     */
    public static final DavPropertyNameSet VERSIONABLE_SET = new DavPropertyNameSet();
    static {
        VERSIONABLE_SET.add(VersionControlledResource.VERSION_HISTORY);
        VERSIONABLE_SET.add(VersionControlledResource.AUTO_VERSION);
    }

    /**
     * Additional property names defined by JCR version nodes.
     */
    public static final DavPropertyNameSet VERSION_SET = new DavPropertyNameSet();
    static {
        VERSION_SET.add(VersionResource.VERSION_NAME);
        VERSION_SET.add(VersionResource.LABEL_NAME_SET);
        VERSION_SET.add(VersionResource.PREDECESSOR_SET);
        VERSION_SET.add(VersionResource.SUCCESSOR_SET);
        VERSION_SET.add(VersionResource.VERSION_HISTORY);
        VERSION_SET.add(VersionResource.CHECKOUT_SET);
    }

    /**
     * Additional property names defined by JCR version history nodes.
     */
    public static final DavPropertyNameSet VERSIONHISTORY_SET = new DavPropertyNameSet();
    static { 
        VERSIONHISTORY_SET.add(VersionHistoryResource.ROOT_VERSION);
        VERSIONHISTORY_SET.add(VersionHistoryResource.VERSION_SET);
        VERSIONHISTORY_SET.add(JCR_VERSIONABLEUUID);
    }
}