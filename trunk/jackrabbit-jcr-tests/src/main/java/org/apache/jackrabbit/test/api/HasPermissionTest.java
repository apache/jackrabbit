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
import javax.jcr.Node;

/**
 * Tests if {@link javax.jcr.Session#hasPermission(String, String)} yields the
 * correct permissions for a read-only, a read-write and an admin session.
 */
public class HasPermissionTest extends AbstractJCRTest {

    private static final String ACTION_ALL = Session.ACTION_READ + "," + Session.ACTION_ADD_NODE + "," + Session.ACTION_REMOVE + "," + Session.ACTION_SET_PROPERTY;

    /**
     * Tests if <code>Session.hasPermission(String, String)</code> returns
     * <ul>
     * <li><code>true</code> for READ.</li>
     * <li><code>false</code> for all other actions.</li>
     * </ul>
     */
    public void testReadOnlyPermission() throws Exception {
        Node n = testRootNode.addNode(nodeName2, testNodeType);
        superuser.save();

        Session readOnly = getHelper().getReadOnlySession();
        try {
            String path = n.getPath();

            assertTrue(readOnly.hasPermission(path, Session.ACTION_READ));
            assertFalse(readOnly.hasPermission(path + "newNode", Session.ACTION_ADD_NODE));
            assertFalse(readOnly.hasPermission(path, Session.ACTION_REMOVE));
            assertFalse(readOnly.hasPermission(path, Session.ACTION_SET_PROPERTY));
            assertFalse(readOnly.hasPermission(path, ACTION_ALL));
            assertFalse(readOnly.hasPermission(path, Session.ACTION_REMOVE + "," + Session.ACTION_SET_PROPERTY));
        } finally {
            readOnly.logout();
        }
    }

    /**
     * Tests if <code>Session.hasPermission(String, String)</code> returns
     * <ul>
     * <li><code>true</code> for all actions.</li>
     * </ul>
     */
    public void testReadWritePermission() throws Exception {
        Node n = testRootNode.addNode(nodeName2, testNodeType);
        superuser.save();

        String path = n.getPath();
        Session rwSession = getHelper().getReadWriteSession();
        try {
            assertTrue(rwSession.hasPermission(path, Session.ACTION_READ));
            assertTrue(rwSession.hasPermission(path + "newNode", Session.ACTION_ADD_NODE));
            assertTrue(rwSession.hasPermission(path, Session.ACTION_REMOVE));
            assertTrue(rwSession.hasPermission(path, Session.ACTION_SET_PROPERTY));
            assertTrue(rwSession.hasPermission(path, ACTION_ALL));
        } finally {
            rwSession.logout();
        }
    }


    /**
     * Tests if <code>Session.hasPermission(String, String)</code> returns
     * <ul>
     * <li><code>true</code> for all actions.</li>
     * </ul>
     */
    public void testAdminPermission() throws Exception {
        Node n = testRootNode.addNode(nodeName2, testNodeType);
        superuser.save();

        String path = n.getPath();

        assertTrue(superuser.hasPermission(path, Session.ACTION_READ));
        assertTrue(superuser.hasPermission(path + "newNode", Session.ACTION_ADD_NODE));
        assertTrue(superuser.hasPermission(path, Session.ACTION_REMOVE));
        assertTrue(superuser.hasPermission(path, Session.ACTION_SET_PROPERTY));
        assertTrue(superuser.hasPermission(path, ACTION_ALL));
    }
}