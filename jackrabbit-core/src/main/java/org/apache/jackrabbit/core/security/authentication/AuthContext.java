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
package org.apache.jackrabbit.core.security.authentication;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

/**
 * An authentication context used to authenticate users. It is similar to JAAS' <code>LoginContext</code>
 * but can work in a non-JAAS environment.
 * <p>
 * This class is abstract and has two implementations:
 * <ul>
 *   <li>{@link JAASAuthContext} which delegates to a regular JAAS <code>LoginContext</code></li>
 *   <li>{@link LocalAuthContext} which implements authentication using a locally-defined
 *       JAAS <code>LoginModule</code></li>
 * </ul>
 */
public interface AuthContext {

    /**
     * Perform the authentication and, if successful, associate Principals and Credentials
     * with the authenticated<code>Subject</code>.
     *
     * @see javax.security.auth.login.LoginContext#login()
     * @throws LoginException if the authentication fails.
     */
    void login() throws LoginException;

    /**
     * Return the authenticated Subject.
     *
     * @see javax.security.auth.login.LoginContext#getSubject()
     * @return the authenticated Subject or <code>null</code> if authentication failed.
     */
    Subject getSubject();

    /**
     * Logout the <code>Subject</code>.
     *
     * @see javax.security.auth.login.LoginContext#logout()
     * @throws LoginException if the logout fails.
     */
    void logout() throws LoginException;
}
