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
package org.apache.jackrabbit.core;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.observation.ObservationDispatcher;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.xml.sax.InputSource;

/**
 * Utility class that decouples {@link SessionImpl} from the internal
 * workspace handling details of {@link RepositoryImpl}.
 */
public class WorkspaceManager {

    private final RepositoryImpl repository;

    WorkspaceManager(RepositoryImpl repository) {
        this.repository = repository;
    }

    /**
     * Returns the name of the default workspace.
     *
     * @return default workspace name
     */
    public String getDefaultWorkspaceName() {
        return repository.getConfig().getDefaultWorkspaceName();
    }

    /**
     * Returns the names of all the available workspaces.
     *
     * @return workspace names
     */
    public String[] getWorkspaceNames() {
        return repository.getWorkspaceNames();
    }

    /**
     * Creates a workspace with the given name.
     *
     * @param workspaceName name of the new workspace
     * @throws RepositoryException if a workspace with the given name
     *                             already exists or if another error occurs
     */
    public void createWorkspace(String workspaceName)
            throws RepositoryException {
        repository.createWorkspace(workspaceName);
    }

    /**
     * Creates a workspace with the given name and a workspace configuration
     * template.
     *
     * @param workspaceName  name of the new workspace
     * @param configTemplate the configuration template of the new workspace
     * @throws RepositoryException if a workspace with the given name already
     *                             exists or if another error occurs
     */
    public void createWorkspace(
            String workspaceName, InputSource configTemplate)
            throws RepositoryException {
        repository.createWorkspace(workspaceName, configTemplate);
    }

    // FIXME: This is a too low-level method. Refactor...
    public SharedItemStateManager getWorkspaceStateManager(String workspaceName)
            throws NoSuchWorkspaceException, RepositoryException {
        return repository.getWorkspaceStateManager(workspaceName);
    }

    // FIXME: This is a too low-level method. Refactor...
    public ObservationDispatcher getObservationDispatcher(String workspaceName)
            throws NoSuchWorkspaceException, RepositoryException {
        return repository.getObservationDispatcher(workspaceName);
    }

    // FIXME: There should be a better place for this. Refactor...
    public SessionImpl createSession(Subject subject, String workspaceName)
        throws RepositoryException {
        return repository.createSession(subject, workspaceName);
    }

}
