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
package org.apache.jackrabbit.api.security.user;

import org.apache.jackrabbit.test.NotExecutableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Credentials;

/**
 * <code>UserTest</code>...
 */
public class UserTest extends AbstractUserTest {

    private static Logger log = LoggerFactory.getLogger(UserTest.class);

    public void testNotIsGroup() throws NotExecutableException, RepositoryException {
        User user = getTestUser(superuser);
        assertFalse(user.isGroup());
    }

    public void testSuperuserIsAdmin() throws NotExecutableException, RepositoryException {
        User user = getTestUser(superuser);
        assertTrue(user.isAdmin());
    }

    public void testReadOnlyIsntAdmin() throws NotExecutableException, RepositoryException {
        User user = getTestUser(helper.getReadOnlySession());
        assertFalse(user.isAdmin());
    }

    public void testUserHasCredentials() throws RepositoryException, NotExecutableException {
        User user = getTestUser(superuser);
        Credentials creds = user.getCredentials();
        assertTrue(creds != null);
    }
}
