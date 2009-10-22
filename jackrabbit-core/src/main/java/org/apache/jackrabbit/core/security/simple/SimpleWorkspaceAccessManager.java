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
package org.apache.jackrabbit.core.security.simple;

import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;

import javax.jcr.Session;
import javax.jcr.RepositoryException;
import java.util.Set;
import java.security.Principal;

/**
 * <code>SimpleWorkspaceAccessManager</code> always allows any set of principals
 * to access any workspace.
 */
public class SimpleWorkspaceAccessManager implements WorkspaceAccessManager {

    /**
     * @see WorkspaceAccessManager#init(Session)
     */
    public void init(Session systemSession) {
        // nothing to do
    }

    /**
     * @see WorkspaceAccessManager#close()
     */
    public void close() throws RepositoryException {
        // nothing to do.
    }

    /**
     * Always returns <code>true</code> allowing any set of principals to
     * access all workspaces.
     *
     * @see WorkspaceAccessManager#grants(java.util.Set, String)
     */
    public boolean grants(Set<Principal> principals, String workspaceName)
            throws RepositoryException {
        return true;
    }
}