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

import javax.jcr.RepositoryException;
import javax.jcr.Credentials;

/**
 * User is a special {@link Authorizable} that can be authenticated and
 * impersonated.
 *
 * @see #getCredentials()
 * @see #getImpersonation()
 */
public interface User extends Authorizable {

    /**
     * @return true if the current user represents the administrator.
     */
    boolean isAdmin();

    /**
     * Returns <code>Credentials</code> for this user.
     *
     * @return <code>Credentials</code> for this user.
     * @throws javax.jcr.RepositoryException If an error occurs.
     */
    Credentials getCredentials() throws RepositoryException;

    /**
     * @return <code>Impersonation</code> for this <code>User</code>.
     * @throws javax.jcr.RepositoryException If an error occurs.
     */
    Impersonation getImpersonation() throws RepositoryException;

    /**
     * Change the password of this user.
     *
     * @param password The new password.
     * @throws RepositoryException If an error occurs.
     */
    void changePassword(String password) throws RepositoryException;
}
