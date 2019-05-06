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
package org.apache.jackrabbit.core.security.authorization;

/**
 * <code>Permission</code>...
 */
public final class Permission {

    public static final int NONE = 0;

    public static final int READ = 1;   

    public static final int SET_PROPERTY = READ << 1;

    public static final int ADD_NODE = SET_PROPERTY << 1;

    public static final int REMOVE_NODE = ADD_NODE << 1;

    public static final int REMOVE_PROPERTY = REMOVE_NODE << 1;

    public static final int READ_AC = REMOVE_PROPERTY << 1;
    
    public static final int MODIFY_AC = READ_AC << 1;

    public static final int NODE_TYPE_MNGMT = MODIFY_AC << 1;

    public static final int VERSION_MNGMT = NODE_TYPE_MNGMT << 1;

    public static final int LOCK_MNGMT = VERSION_MNGMT << 1;

    public static final int LIFECYCLE_MNGMT = LOCK_MNGMT << 1;

    public static final int RETENTION_MNGMT = LIFECYCLE_MNGMT << 1;

    public static final int MODIFY_CHILD_NODE_COLLECTION = RETENTION_MNGMT << 1;

    public static final int NODE_TYPE_DEF_MNGMT = MODIFY_CHILD_NODE_COLLECTION << 1;

    public static final int NAMESPACE_MNGMT = NODE_TYPE_DEF_MNGMT << 1;

    public static final int WORKSPACE_MNGMT = NAMESPACE_MNGMT << 1;

    public static final int PRIVILEGE_MNGMT = WORKSPACE_MNGMT << 1;

    public static final int ALL = (READ | SET_PROPERTY | ADD_NODE | REMOVE_NODE
            | REMOVE_PROPERTY | READ_AC | MODIFY_AC | NODE_TYPE_MNGMT
            | VERSION_MNGMT | LOCK_MNGMT | LIFECYCLE_MNGMT | RETENTION_MNGMT
            | MODIFY_CHILD_NODE_COLLECTION | NODE_TYPE_DEF_MNGMT | NAMESPACE_MNGMT
            | WORKSPACE_MNGMT | PRIVILEGE_MNGMT);

    /**
     * Returns those bits from <code>permissions</code> that are not present in
     * the <code>otherPermissions</code>, i.e. subtracts the other permissions
     * from permissions.<br>
     * If the specified <code>otherPermissions</code> do not intersect with
     * <code>permissions</code>,  <code>permissions</code> are returned.<br>
     * If <code>permissions</code> is included in <code>otherPermissions</code>,
     * {@link #NONE} is returned.
     *
     * @param permissions
     * @param otherPermissions
     * @return the differences of the 2 permissions or <code>{@link #NONE}</code>.
     */
    public static int diff(int permissions, int otherPermissions) {
        return permissions & ~otherPermissions;
    }
}