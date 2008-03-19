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

    public static final int ADD_NODE = 4;

    public static final int REMOVE_NODE = 8;

    public static final int SET_PROPERTY = 2;

    public static final int REMOVE_PROPERTY = 16;

    public static final int ALL = (READ | SET_PROPERTY | ADD_NODE | REMOVE_NODE | REMOVE_PROPERTY);

    /**
     * Build the permissions granted by evaluating the given privileges. If
     * <code>protectesPolicy</code> is <code>true</code> read and write
     * permission are only granted if the corresponding ac-privilege is
     * present.
     *
     * @param privs The privileges granted on the Node itself (for properties
     * the ACL of the direct ancestor).
     * @param parentPrivs The privileges granted on the parent of the Node. Not
     * relevant for properties since it only is used to determine permissions
     * on a Node (add_node, remove).
     * @param protectsPolicy
     * @return the permissions granted evaluating the given privileges.
     */
    public static int calculatePermissions(int privs, int parentPrivs, boolean protectsPolicy) {
        int perm = Permission.NONE;
        if (protectsPolicy) {
            if ((parentPrivs & PrivilegeRegistry.READ_AC) == PrivilegeRegistry.READ_AC) {
                perm |= Permission.READ;
            }
            if ((parentPrivs & PrivilegeRegistry.MODIFY_AC) == PrivilegeRegistry.MODIFY_AC) {
                perm |= Permission.ADD_NODE;
                perm |= Permission.SET_PROPERTY;
                perm |= Permission.REMOVE_NODE;
                perm |= Permission.REMOVE_PROPERTY;
            }
        } else {
            if ((privs & PrivilegeRegistry.READ) == PrivilegeRegistry.READ) {
                perm |= Permission.READ;
            }
            if ((privs & PrivilegeRegistry.MODIFY_PROPERTIES) == PrivilegeRegistry.MODIFY_PROPERTIES) {
                perm |= Permission.SET_PROPERTY;
                perm |= Permission.REMOVE_PROPERTY;
            }
            // the following permissions are granted through privilege on the parent.
            if ((parentPrivs & PrivilegeRegistry.ADD_CHILD_NODES) == PrivilegeRegistry.ADD_CHILD_NODES) {
                perm |= Permission.ADD_NODE;
            }
            if ((parentPrivs & PrivilegeRegistry.REMOVE_CHILD_NODES) == PrivilegeRegistry.REMOVE_CHILD_NODES) {
                perm |= Permission.REMOVE_NODE;
            }
        }
        return perm;
    }

    /**
     * Returns those bits from <code>permissions</code> that are not present in
     * the <code>otherPermissions</code>, i.e. subtracts the other permissions
     * from permissions.<br>
     * If the specified <code>otherBits</code> do not intersect with
     * <code>bits</code>,  <code>bits</code> are returned.<br>
     * If <code>bits</code> is included <code>otherBits</code>,
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