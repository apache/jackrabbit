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
import javax.jcr.Session;

/**
 * Abstract base class for implementing the JCR {@link Repository} interface.
 * <p>
 * This class implements the three utility login methods by calling the
 * {@link Repository#login(Credentials, String)} method with <code>null</code>
 * arguments as specified in the JCR API.
 */
public abstract class AbstractRepository implements Repository {

    /**
     * Calls {@link Repository#login(Credentials, String)} with
     * <code>null</code> arguments.
     *
     * @return logged in session
     * @throws RepositoryException if an error occurs
     */
    public Session login() throws RepositoryException {
        return login(null, null);
    }

    /**
     * Calls {@link Repository#login(Credentials, String)} with
     * the given credentials and a <code>null</code> workspace name.
     *
     * @param credentials login credentials
     * @return logged in session
     * @throws RepositoryException if an error occurs
     */
    public Session login(Credentials credentials) throws RepositoryException {
        return login(credentials, null);
    }

    /**
     * Calls {@link Repository#login(Credentials, String)} with
     * <code>null</code> credentials and the given workspace name.
     *
     * @param workspace workspace name
     * @return logged in session
     * @throws RepositoryException if an error occurs
     */
    public Session login(String workspace) throws RepositoryException {
        return login(null, workspace);
    }

}
