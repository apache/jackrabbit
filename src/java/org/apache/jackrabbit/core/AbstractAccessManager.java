/*
 * Copyright 2004 The Apache Software Foundation.
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

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.access.AccessManager;
import javax.jcr.access.Permission;

/**
 * <code>AbstractAccessManager</code> provides a convenient base class
 * for specific <code>AccessManager</code> implementations.
 * <p/>
 * A derived class has only to implement the abstract <code>getPermissions(String)</code>
 * as all other methods except <code>getSupportedPermissions()</code> directly or
 * indirectly call this method.
 */
abstract class AbstractAccessManager implements AccessManager {

    /**
     * Package private constructor
     */
    AbstractAccessManager() {
    }

    //--------------------------------------------------------< AccessManager >
    /**
     * @see AccessManager#getPermissions(String)
     */
    public abstract long getPermissions(String absPath)
            throws PathNotFoundException, RepositoryException;

    /**
     * @see AccessManager#getSupportedPermissions
     */
    public Permission[] getSupportedPermissions() {
        return PermissionImpl.ALL_PERMISSIONS;
    }

    /**
     * @see AccessManager#isGranted(String, long)
     */
    public boolean isGranted(String absPath, long permissions)
            throws PathNotFoundException, RepositoryException {
        return (getPermissions(absPath) & permissions) == permissions;
    }

    //--------------------------------------------------------< inner classes >
    static final class PermissionImpl implements Permission {

        private final String name;
        private final long value;

        static final Permission ADD_NODE_PERMISSION =
                new PermissionImpl("add node", Permission.ADD_NODE);
        static final Permission SET_PROPERTY_PERMISSION =
                new PermissionImpl("set property", Permission.SET_PROPERTY);
        static final Permission REMOVE_ITEM_PERMISSION =
                new PermissionImpl("remove item", Permission.REMOVE_ITEM);
        static final Permission READ_ITEM_PERMISSION =
                new PermissionImpl("read item", Permission.READ_ITEM);

        static final long ALL_VALUES =
                ADD_NODE_PERMISSION.getValue() | SET_PROPERTY_PERMISSION.getValue()
                | READ_ITEM_PERMISSION.getValue() | REMOVE_ITEM_PERMISSION.getValue();

        static final Permission[] ALL_PERMISSIONS =
                new Permission[]{ADD_NODE_PERMISSION, SET_PROPERTY_PERMISSION,
                                 READ_ITEM_PERMISSION, REMOVE_ITEM_PERMISSION};

        private PermissionImpl(String name, long value) {
            this.name = name;
            this.value = value;
        }

        //-------------------------------------------------------< Permission >
        public String getName() {
            return name;
        }

        public long getValue() {
            return value;
        }
    }
}
