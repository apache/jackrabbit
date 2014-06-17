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
package org.apache.jackrabbit.core.security.authorization;

import org.apache.jackrabbit.core.config.WorkspaceSecurityConfig;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * The <code>AccessControlProviderFactory</code> is used to create
 * {@link AccessControlProvider}s for the various workspaces present in the
 * repository. If a provider is no longer used by the workspace, it is
 * {@link AccessControlProvider#close() closed}.
 * <p>
 * The factory does not need to cache the created {@link AccessControlProvider}s.
 * They are used during the entire lifetime of their workspace, and are cached
 * together with the respective workspace related objects by the repository
 * implementation.
 * <p>
 * The {@link AccessControlProvider}s are requested using a
 * {@link Session system Session}. The system sessions have a distinct access
 * control rules in order to prevent chicken-egg problems when setting up
 * security for a workspace.
 */
public interface AccessControlProviderFactory {

    /**
     * Initialize this factory.
     *
     * @param securitySession Security Session.
     * @throws RepositoryException If an error occurs.
     */
    void init(Session securitySession) throws RepositoryException;

    /**
     * Dispose this <code>AccessControlProviderFactory</code> and its resources.
     *
     * @throws RepositoryException if an error occurs.
     */
    void close() throws RepositoryException;

    /**
     * Creates an AccessControlProvider for the workspace of the given
     * system session. If the passed configuration is <code>null</code> or
     * does not have a provider entry, this factory must create a default
     * provider. In any case the provider must be initialized before it
     * is returned to the caller.
     *
     * @param systemSession the system session for the workspace the
     * <code>AccessControlProvider</code> should be created for.
     * @param config The security configuration for that workspace or
     * <code>null</code> if no config entry is present. In this case the
     * factory must use its default. The configuration is used to determine
     * the implementation of <code>AccessControlProvider</code> to be used
     * and to retrieve eventual configuration parameters.
     * @return a new, initialized AccessControlProvider.
     * @throws RepositoryException if an error occurs
     */
    AccessControlProvider createProvider(Session systemSession, WorkspaceSecurityConfig config) throws RepositoryException;
}
