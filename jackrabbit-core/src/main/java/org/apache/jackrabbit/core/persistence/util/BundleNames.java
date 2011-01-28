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
package org.apache.jackrabbit.core.persistence.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOExceptionWithCause;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

// WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING //
//                                                                         //
// The contents and behaviour of this class are tightly coupled with the    //
// bundle serialization format, so make sure that you know what you're     //
// doing before modifying this class!                                      //
//                                                                         //
// WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING //

/**
 * Static collection of common JCR names. This class is used by the
 * {@link BundleWriter} and {@link BundleReader} classes to optimize the
 * serialization of names used in bundles.
 */
class BundleNames {

    /**
     * Static list of standard names.
     */
    private static final Name[] NAME_ARRAY = {
        // WARNING: Only edit if you really know what you're doing!

        // Most frequently used names
        NameConstants.NT_UNSTRUCTURED,
        NameConstants.NT_RESOURCE,
        NameConstants.NT_FILE,
        NameConstants.NT_FOLDER,
        NameConstants.NT_HIERARCHYNODE,
        NameConstants.MIX_REFERENCEABLE,
        NameConstants.JCR_CREATED,
        NameConstants.JCR_CREATEDBY,
        NameConstants.JCR_LASTMODIFIED,
        NameConstants.JCR_LASTMODIFIEDBY,
        NameConstants.JCR_CONTENT,
        NameConstants.JCR_MIMETYPE,
        NameConstants.JCR_DATA,
        NameConstants.JCR_TITLE,
        NameConstants.JCR_LANGUAGE,
        NameConstants.JCR_ENCODING,
        NameConstants.JCR_SYSTEM,
        NameConstants.REP_ROOT,
        NameConstants.REP_SYSTEM,

        // Access control
        NameConstants.JCR_ADD_CHILD_NODES,
        NameConstants.JCR_LIFECYCLE_MANAGEMENT,
        NameConstants.JCR_LOCK_MANAGEMENT,
        NameConstants.JCR_MODIFY_ACCESS_CONTROL,
        NameConstants.JCR_MODIFY_PROPERTIES,
        NameConstants.JCR_NODE_TYPE_MANAGEMENT,
        NameConstants.JCR_READ,
        NameConstants.JCR_READ_ACCESS_CONTROL,
        NameConstants.JCR_REMOVE_CHILD_NODES,
        NameConstants.JCR_REMOVE_NODE,
        NameConstants.JCR_VERSION_MANAGEMENT,
        NameConstants.REP_ACCESSCONTROL,
        NameConstants.REP_ACCESS_CONTROL,
        NameConstants.REP_ACCESS_CONTROLLABLE,
        NameConstants.REP_ACE,
        NameConstants.REP_ACL,
        NameConstants.REP_DENY_ACE,
        NameConstants.REP_GLOB,
        NameConstants.REP_GRANT_ACE,
        NameConstants.REP_POLICY,
        NameConstants.REP_PRINCIPAL_ACCESS_CONTROL,
        NameConstants.REP_PRINCIPAL_NAME,
        NameConstants.REP_PRIVILEGES,

        // Locking
        NameConstants.MIX_LOCKABLE,
        NameConstants.JCR_LOCKISDEEP,
        NameConstants.JCR_LOCKOWNER,

        // Versioning
        NameConstants.MIX_VERSIONABLE,
        NameConstants.NT_FROZENNODE,
        NameConstants.NT_VERSION,
        NameConstants.NT_VERSIONEDCHILD,
        NameConstants.NT_VERSIONHISTORY,
        NameConstants.NT_VERSIONLABELS,
        NameConstants.JCR_VERSIONSTORAGE,
        NameConstants.JCR_FROZENPRIMARYTYPE,
        NameConstants.JCR_FROZENUUID,
        NameConstants.JCR_FROZENNODE,
        NameConstants.JCR_PREDECESSORS,
        NameConstants.JCR_SUCCESSORS,
        NameConstants.JCR_VERSIONLABELS,
        NameConstants.JCR_VERSIONHISTORY,
        NameConstants.JCR_VERSIONABLEUUID,
        NameConstants.JCR_ROOTVERSION,
        NameConstants.JCR_ISCHECKEDOUT,
        NameConstants.JCR_BASEVERSION,
        NameConstants.JCR_MERGEFAILED,
        NameConstants.REP_NODETYPES,

        // Node types
        NameConstants.NT_NODETYPE,
        NameConstants.NT_PROPERTYDEFINITION,
        NameConstants.NT_CHILDNODEDEFINITION,
        NameConstants.NT_BASE,
        NameConstants.JCR_NODETYPES,
        NameConstants.JCR_PROTECTED,
        NameConstants.JCR_ONPARENTVERSION,
        NameConstants.JCR_MANDATORY,
        NameConstants.JCR_AUTOCREATED,
        NameConstants.JCR_FROZENMIXINTYPES,
        NameConstants.JCR_NAME,
        NameConstants.JCR_VALUECONSTRAINTS,
        NameConstants.JCR_REQUIREDTYPE,
        NameConstants.JCR_PROPERTYDEFINITION,
        NameConstants.JCR_MULTIPLE,
        NameConstants.JCR_DEFAULTVALUES,
        NameConstants.JCR_SUPERTYPES,
        NameConstants.JCR_NODETYPENAME,
        NameConstants.JCR_ISMIXIN,
        NameConstants.JCR_HASORDERABLECHILDNODES,
        NameConstants.JCR_SAMENAMESIBLINGS,
        NameConstants.JCR_REQUIREDPRIMARYTYPES,
        NameConstants.JCR_CHILDNODEDEFINITION,
        NameConstants.JCR_DEFAULTPRIMARYTYPE,
        NameConstants.JCR_PRIMARYITEMNAME,
        NameConstants.JCR_CHILDVERSIONHISTORY,
        NameConstants.REP_VERSIONS,
        NameConstants.REP_VERSIONSTORAGE,
        NameConstants.REP_VERSION_REFERENCE,
        NameConstants.REP_BASEVERSIONS,

        // Miscellaneous node types
        NameConstants.MIX_CREATED,
        NameConstants.MIX_ETAG,
        NameConstants.MIX_LANGUAGE,
        NameConstants.MIX_LASTMODIFIED,
        NameConstants.MIX_LIFECYCLE,
        NameConstants.MIX_MIMETYPE,
        NameConstants.MIX_SHAREABLE,
        NameConstants.MIX_SIMPLE_VERSIONABLE,
        NameConstants.MIX_TITLE,
        NameConstants.NT_ACTIVITY,
        NameConstants.NT_ADDRESS,
        NameConstants.NT_CONFIGURATION,
        NameConstants.NT_QUERY,
        NameConstants.NT_SHARE,

        // Miscellaneous names
        NameConstants.REP_ACTIVITIES,
        NameConstants.JCR_ACTIVITIES,
        NameConstants.JCR_ACTIVITY,
        NameConstants.JCR_ACTIVITY_TITLE,
        NameConstants.JCR_XMLCHARACTERS,
        NameConstants.JCR_XMLTEXT,
        NameConstants.REP_CONFIGURATIONS,
        NameConstants.JCR_CONFIGURATION,
        NameConstants.JCR_CONFIGURATIONS,
        NameConstants.JCR_COPIEDFROM,
        NameConstants.JCR_CURRENT_LIFECYCLE_STATE,
        NameConstants.JCR_ETAG,
        NameConstants.JCR_HOST,
        NameConstants.JCR_ID,
        NameConstants.JCR_LIFECYCLE_POLICY,
        NameConstants.JCR_PATH,
        NameConstants.JCR_STATEMENT,

    };  // WARNING: Only edit if you really know what you're doing!

    private static final Map<Name, Integer> NAME_MAP =
        new HashMap<Name, Integer>();

    static {
        assert NAME_ARRAY.length <= 0x80;
        for (int i = 0; i < NAME_ARRAY.length; i++) {
            NAME_MAP.put(NAME_ARRAY[i], i);
        }
    }

    /**
     * Returns the seven-bit index of a common JCR name, or -1 if the given
     * name is not known.
     *
     * @param name JCR name
     * @return seven-bit index of the name, or -1
     */
    public static int nameToIndex(Name name) {
        Integer index = NAME_MAP.get(name);
        if (index != null) {
            return index;
        } else {
            return -1;
        }
    }

    public static Name indexToName(int index) throws IOException {
        try {
            return NAME_ARRAY[index];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IOExceptionWithCause(
                    "Invalid common JCR name index: " + index, e);
        }
    }

}
