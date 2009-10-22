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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Set;
import java.security.Principal;

/**
 * The <code>WorkspaceAccessManager</code> is responsible for workspace access.
 * In contrast to Items that are identified, workspaces are named Objects
 * on different class hierarchy.
 */
public interface WorkspaceAccessManager {

    /**
     * Initialize this <code>WorkspaceAccessManager</code>.
     *
     * @param systemSession Session used to initialize this instance.
     * @throws RepositoryException if an error occurs.
     */
    void init(Session systemSession) throws RepositoryException;

    /**
     * Dispose this <code>WorkspaceAccessManager</code> and its resources.
     *
     * @throws RepositoryException if an error occurs.
     */
    void close() throws RepositoryException;

    /**
     * Returns <code>true</code> if access to the workspace with the given name
     * is granted to the to any of the specified principals.
     *
     * @param principals A set of principals to be tested for being allowed to
     * access workspace identified by <code>workspaceName</code>.
     * @param workspaceName Name of the workspace to be tested.
     * @return true if the given set of principals is allowed to access the
     * workspace with the specified name.
     * @throws RepositoryException If an error occurs. 
     */
    boolean grants(Set<Principal> principals, String workspaceName)
            throws RepositoryException;
}
