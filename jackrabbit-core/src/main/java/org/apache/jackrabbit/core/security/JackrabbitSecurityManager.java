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
package org.apache.jackrabbit.core.security;

import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.security.authentication.AuthContext;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.security.auth.Subject;

/**
 * <code>JackrabbitSecurityManager</code>...
 */
public interface JackrabbitSecurityManager {

    public void init(Repository repository, Session systemSession) throws RepositoryException;

    /**
     * Disposes those parts of this security manager that are related to the
     * workspace indicated by the given <code>workspaceName</code>.
     *
     * @param workspaceName Name of the workspace that is being disposed.
     */
    public void dispose(String workspaceName);

    /**
     * Disposes this security manager instance and cleans all internal caches.
     */
    public void close();

    /**
     * Returns a new <code>AuthContext</code> for the specified credentials and
     * subject.
     *
     * @param creds
     * @param subject
     * @return A new <code>AuthContext</code> for the given <code>creds</code>
     * and <code>subject</code>.
     * @throws RepositoryException
     */
    public AuthContext getAuthContext(Credentials creds, Subject subject) throws RepositoryException;

    /**
     * Retrieve the <code>AccessManager</code> for the given <code>session</code>.
     *
     * @param session
     * @param amContext
     * @return <code>AccessManager</code> for the specified <code>session</code>.
     * @throws RepositoryException
     */
    public AccessManager getAccessManager(Session session, AMContext amContext) throws RepositoryException;

    /**
     * Retrieve the principal manager for the given <code>session</code>.
     *
     * @param session
     * @return PrincipalManager for the given <code>session</code>.
     * @throws UnsupportedRepositoryOperationException If principal management
     * is not supported.
     * @throws RepositoryException if an error occurs
     */
    public PrincipalManager getPrincipalManager(Session session) throws RepositoryException;

    /**
     * Returns the user manager for the specified <code>session</code>.
     *
     * @param session
     * @return UserManager for the given <code>session</code>.
     * @throws UnsupportedRepositoryOperationException If user management is
     * not supported.
     * @throws RepositoryException
     */
    public UserManager getUserManager(Session session) throws RepositoryException;

    /**
     * Retrieve the id to be displayed upon {@link Session#getUserID()} for
     * the specified subject.
     *
     * @param subject
     * @return userID to be displayed upon {@link Session#getUserID()}.
     * @throws RepositoryException
     */
    public String getUserID(Subject subject) throws RepositoryException;
}
