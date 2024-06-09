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

import junit.framework.TestCase;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.mockito.Mockito;
import org.mockito.internal.verification.Times;

/**
 * Test cases for the {@link AbstractRepositoryTest} class.
 */
public class AbstractRepositoryTest extends TestCase {

    /**
     * Tests the {@link AbstractRepository#login()} method.
     *
     * @throws RepositoryException if an error occurs
     */
    public void testLogin() throws RepositoryException {
        Repository repository = Mockito.spy(AbstractRepository.class);
        repository.login(null, null);
        repository.login();
        Mockito.verify(repository, new Times(2)).login(null, null);
    }

    /**
     * Tests the {@link AbstractRepository#login(Credentials)} method.
     *
     * @throws RepositoryException if an error occurs
     */
    public void testLoginWithCredentials() throws RepositoryException {
        Credentials credentials = new SimpleCredentials("", "".toCharArray());
        Repository repository = Mockito.spy(AbstractRepository.class);
        repository.login(credentials, null);
        repository.login(credentials);
        Mockito.verify(repository, new Times(2)).login(credentials, null);
    }

    /**
     * Tests the {@link AbstractRepository#login(Credentials)} method with
     * <code>null</code> credentials.
     *
     * @throws RepositoryException if an error occurs
     */
    public void testLoginWithNullCredentials() throws RepositoryException {
        Repository repository = Mockito.spy(AbstractRepository.class);
        repository.login(null, null);
        repository.login((Credentials) null);
        Mockito.verify(repository, new Times(2)).login(null, null);
    }

    /**
     * Tests the {@link AbstractRepository#login(String)} method.
     *
     * @throws RepositoryException if an error occurs
     */
    public void testLoginWithWorkspace() throws RepositoryException {
        Repository repository = Mockito.spy(AbstractRepository.class);
        repository.login(null, "workspace");
        repository.login("workspace");
        Mockito.verify(repository, new Times(2)).login(null, "workspace");
    }

    /**
     * Tests the {@link AbstractRepository#login(String)} method with a
     * <code>null</code> workspace name.
     *
     * @throws RepositoryException if an error occurs
     */
    public void testLoginWithNullWorkspace() throws RepositoryException {
        Repository repository = Mockito.spy(AbstractRepository.class);
        repository.login(null, null);
        repository.login((String) null);
        Mockito.verify(repository, new Times(2)).login(null, null);
    }
}
