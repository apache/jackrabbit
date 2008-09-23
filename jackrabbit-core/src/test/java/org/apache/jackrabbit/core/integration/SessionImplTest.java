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

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Integration tests for the Session implementation in Jackrabbit core.
 */
public class SessionImplTest extends AbstractJCRTest {

    /**
     * <a href="https://issues.apache.org/jira/browse/JCR-1389">JCR-1731</a>:
     * Session.checkPermission("/", "add_node") throws PathNotFoundException
     * instead of AccessControlException
     */
    public void testCheckAddNodePermissionOnRoot() throws RepositoryException {
        Session session = helper.getReadOnlySession();
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

}
