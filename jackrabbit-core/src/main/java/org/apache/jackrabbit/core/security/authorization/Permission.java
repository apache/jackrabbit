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

    public static final int SET_PROPERTY = 2;

    public static final int ADD_NODE = 4;

    public static final int REMOVE_NODE = 8;

    public static final int REMOVE_PROPERTY = 16;

    public static final int READ_AC = 32;
    
    public static final int MODIFY_AC = 64;

    public static final int NODE_TYPE_MNGMT = 128;

    public static final int VERSION_MNGMT = 256;

    public static final int LOCK_MNGMT = 512;

    public static final int LIFECYCLE_MNGMT = 1024;

    public static final int RETENTION_MNGMT = 2048;

    public static final int ALL = (READ | SET_PROPERTY | ADD_NODE | REMOVE_NODE | REMOVE_PROPERTY | READ_AC | MODIFY_AC | NODE_TYPE_MNGMT | VERSION_MNGMT | LOCK_MNGMT | LIFECYCLE_MNGMT | RETENTION_MNGMT);

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