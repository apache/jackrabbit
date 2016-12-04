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

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>PermissionTest</code>...
 */
public class PermissionTest extends TestCase {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(PermissionTest.class);

    public void testPermissions() {
        assertEquals(0, Permission.NONE);
        assertEquals(1, Permission.READ);
        assertEquals(2, Permission.SET_PROPERTY);
        assertEquals(4, Permission.ADD_NODE);
        assertEquals(8, Permission.REMOVE_NODE);
        assertEquals(16, Permission.REMOVE_PROPERTY);
        assertEquals(32, Permission.READ_AC);
        assertEquals(64, Permission.MODIFY_AC);
        assertEquals(128, Permission.NODE_TYPE_MNGMT);
        assertEquals(256, Permission.VERSION_MNGMT);
        assertEquals(512, Permission.LOCK_MNGMT);
        assertEquals(1024, Permission.LIFECYCLE_MNGMT);
        assertEquals(2048, Permission.RETENTION_MNGMT);
        assertEquals(4096, Permission.MODIFY_CHILD_NODE_COLLECTION);        
        assertEquals(8192, Permission.NODE_TYPE_DEF_MNGMT);        
        assertEquals(16384, Permission.NAMESPACE_MNGMT);
        assertEquals(32768, Permission.WORKSPACE_MNGMT);
        assertEquals(65536, Permission.PRIVILEGE_MNGMT);
    }
}