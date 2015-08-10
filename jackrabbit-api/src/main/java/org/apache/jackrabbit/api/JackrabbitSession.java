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
package org.apache.jackrabbit.api;

import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;

import javax.jcr.Session;
import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * <code>JackrabbitSession</code>...
 */
public interface JackrabbitSession extends Session {

    /**
     * Returns the <code>PrincipalManager</code> for the current <code>Session</code>.
     *
     * @return the <code>PrincipalManager</code> associated with this <code>Session</code>.
     * @throws AccessDeniedException If the session lacks privileges to access
     * the principal manager or principals in general.
     * @throws UnsupportedRepositoryOperationException If principal management
     * is not supported.
     * @throws RepositoryException If another error occurs.
     * @see PrincipalManager
     */
    PrincipalManager getPrincipalManager() throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Returns the <code>UserManager</code> for the current <code>Session</code>.
     *
     * @return the <code>UserManager</code> associated with this <code>Session</code>.
     * @throws javax.jcr.AccessDeniedException If this session is not allowed to
     * to access user data.
     * @throws UnsupportedRepositoryOperationException If user management is
     * not supported.
     * @throws javax.jcr.RepositoryException If another error occurs.
     * @see UserManager
     */
    UserManager getUserManager() throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException;

}