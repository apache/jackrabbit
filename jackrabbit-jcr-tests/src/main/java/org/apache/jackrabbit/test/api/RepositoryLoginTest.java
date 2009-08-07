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

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Session;

/**
 * <code>RepositoryLoginTest</code> tests the login methods of a repository.
 *
 * @test
 * @sources RepositoryLoginTest.java
 * @executeClass org.apache.jackrabbit.test.api.RepositoryLoginTest
 * @keywords level1
 */
public class RepositoryLoginTest extends AbstractJCRTest {

    private Credentials credentials;
    private String workspaceName;
    private Repository repository;

    /**
     * Sets up the fixture for the test cases.
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();

        credentials = helper.getReadOnlyCredentials();
        workspaceName = superuser.getWorkspace().getName();
        repository = helper.getRepository();
    }

    /**
     * Tests if {@link javax.jcr.Repository#login(Credentials credentials, String workspaceName)}
     * throws a {@link javax.jcr.NoSuchWorkspaceException}
     * if no workspace of the requested name is existing.
     */
    public void testNoSuchWorkspaceException()
            throws RepositoryException {

        Session session = helper.getReadOnlySession();
        String name;
        try {
            name = getNonExistingWorkspaceName(session);
        } finally {
            session.logout();
            session = null;
        }

        try {
            session = helper.getRepository().login(credentials, name);
            fail("login with a not available workspace name must throw a " +
                    "NoSuchWorkspaceException");
        } catch (NoSuchWorkspaceException e) {
            // success
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * Tests if {@link javax.jcr.Repository#login(Credentials credentials, String workspaceName)}
     * does return a session, i. e. not null.
     */
    public void testSignatureCredentialsAndWorkspaceName()
            throws RepositoryException {

        Session s = repository.login(credentials, workspaceName);
        try {
            assertNotNull("Repository.login(Credentials credentials, " +
                    "String workspaceName) must not return null",
                    s);
        } finally {
            s.logout();
        }
    }

    /**
     * Tests if {@link javax.jcr.Repository#login(Credentials credentials)} does
     * return a session, i. e. not null.
     */
    public void testSignatureCredentials()
            throws RepositoryException {

        Session s = repository.login(credentials);
        try {
            assertNotNull("Repository.login(Credentials credentials) " +
                    "must not return null",
                    s);
        } finally {
            s.logout();
        }
    }
}
