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
package org.apache.jackrabbit.commons;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

/**
 * Test cases for the {@link AbstractRepositoryTest} class.
 */
public class AbstractRepositoryTest extends MockCase {

    /**
     * Tests the {@link AbstractRepository#login()} method.
     *
     * @throws RepositoryException if an error occurs
     */
    public void testLogin() throws RepositoryException {
        Repository repository = (Repository) record(AbstractRepository.class);
        repository.login(null, null);

        replay();
        repository.login();

        verify();
    }

    /**
     * Tests the {@link AbstractRepository#login(Credentials)} method.
     *
     * @throws RepositoryException if an error occurs
     */
    public void testLoginWithCredentials() throws RepositoryException {
        Credentials credentials = new SimpleCredentials("", "".toCharArray());

        Repository repository = (Repository) record(AbstractRepository.class);
        repository.login(credentials, null);

        replay();
        repository.login(credentials);

        verify();
    }

    /**
     * Tests the {@link AbstractRepository#login(Credentials)} method with
     * <code>null</code> credentials.
     *
     * @throws RepositoryException if an error occurs
     */
    public void testLoginWithNullCredentials() throws RepositoryException {
        Repository repository = (Repository) record(AbstractRepository.class);
        repository.login(null, null);

        replay();
        repository.login((Credentials) null);

        verify();
    }

    /**
     * Tests the {@link AbstractRepository#login(String)} method.
     *
     * @throws RepositoryException if an error occurs
     */
    public void testLoginWithWorkspace() throws RepositoryException {
        Repository repository = (Repository) record(AbstractRepository.class);
        repository.login(null, "workspace");

        replay();
        repository.login("workspace");

        verify();
    }

    /**
     * Tests the {@link AbstractRepository#login(String)} method with a
     * <code>null</code> workspace name.
     *
     * @throws RepositoryException if an error occurs
     */
    public void testLoginWithNullWorkspace() throws RepositoryException {
        Repository repository = (Repository) record(AbstractRepository.class);
        repository.login(null, null);

        replay();
        repository.login((String) null);

        verify();
    }
}
