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
package org.apache.jackrabbit.test.api;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.Session;

import java.security.AccessControlException;

/**
 * Tests if {@link Session#checkPermission(String, String)} yields the correct
 * permissions for a read-only session and a 'superuser' session.
 *
 * @test
 * @sources CheckPermissionTest.java
 * @executeClass org.apache.jackrabbit.test.api.CheckPermissionTest
 * @keywords level2
 */
public class CheckPermissionTest extends AbstractJCRTest {

    /**
     * Tests if <code>Session.checkPermission(String, String)</code> works
     * properly: <ul> <li>Returns quietly if access is permitted.</li>
     * <li>Throws an {@link java.security.AccessControlException} if access is
     * denied.</li> </ul>
     */
    public void testCheckPermission() throws Exception {
        testRootNode.addNode(nodeName2, testNodeType);
        superuser.save();

        Session readOnly = helper.getReadOnlySession();
        try {
            permissionCheckReadOnly(readOnly);
            permissionCheckReadWrite(superuser);
        } finally {
            readOnly.logout();
        }
    }

    /**
     * Helper function used in testCheckPermission checks if a read-only session
     * has the correct permissions
     */
    private void permissionCheckReadOnly(Session readOnly) throws Exception {
        String pathPrefix = (testRoot.length() == 1) ? testRoot : testRoot + "/";
        readOnly.checkPermission(testRoot, "read");

        try {
            readOnly.checkPermission(pathPrefix + nodeName1, "add_node");
            fail("add_node permission granted to read-only Session");
        } catch (AccessControlException success) {
            // ok
        }

        try {
            readOnly.checkPermission(pathPrefix + nodeName1, "set_property");
            fail("set_property permission granted to read-only Session");
        } catch (AccessControlException success) {
            // ok
        }

        try {
            readOnly.checkPermission(pathPrefix + nodeName2, "remove");
            fail("remove permission granted to read-only Session");
        } catch (AccessControlException success) {
            // ok
        }
    }

    /**
     * Helper function used in testCheckPermission checks if a read-write
     * session has the correct permissions
     */
    private void permissionCheckReadWrite(Session readWrite) throws Exception {
        String pathPrefix = (testRoot.length() == 1) ? testRoot : testRoot + "/";
        readWrite.checkPermission(testRoot, "read");
        readWrite.checkPermission(pathPrefix + nodeName1, "add_node");
        readWrite.checkPermission(pathPrefix + propertyName1, "set_property");
        readWrite.checkPermission(pathPrefix + nodeName2, "remove");
    }
}
