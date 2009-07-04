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
package org.apache.jackrabbit.core.integration;

import java.security.AccessControlException;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Integration tests for the Session implementation in Jackrabbit core.
 */
public class SessionImplTest extends AbstractJCRTest {

    /**
     * <a href="https://issues.apache.org/jira/browse/JCR-1731">JCR-1731</a>:
     * Session.checkPermission("/", "add_node") throws PathNotFoundException
     * instead of AccessControlException
     */
    public void testCheckAddNodePermissionOnRoot() throws RepositoryException {
        Session session = getHelper().getReadOnlySession();
        try {
            session.checkPermission("/", "add_node");
        } catch (PathNotFoundException e) {
            fail("JCR-1731: Session.checkPermission(\"/\", \"add_node\")"
                    + " throws PathNotFoundException instead of"
                    + " AccessControlException");
        } catch (AccessControlException e) {
            // expected
        } finally {
            session.logout();
        }
    }

    /**
     * JCR-1932: Session.getAttributes( ) call always returns an empty array
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1932">JCR-1932</a>
     */
    public void testSessionAttributes() throws RepositoryException {
        SimpleCredentials credentials =
            new SimpleCredentials("admin", "admin".toCharArray());
        credentials.setAttribute("test", "attribute");
        Session session = getHelper().getRepository().login(credentials);
        try {
            String[] names = session.getAttributeNames();
            assertEquals(1, names.length);
            assertEquals("test", names[0]);
            assertEquals("attribute", session.getAttribute("test"));
        } finally {
            session.logout();
        }
    }

}
