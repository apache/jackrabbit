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

import org.apache.jackrabbit.core.ItemId;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;

/**
 * The <code>AccessManager</code> can be queried to determines whether permission
 * is granted to perform a specific action on a specific item.
 */
public interface AccessManager {

    /**
     * READ permission constant
     */
    int READ = 1;

    /**
     * WRITE permission constant
     */
    int WRITE = 2;

    /**
     * REMOVE permission constant
     */
    int REMOVE = 4;

    /**
     * Initialize this access manager. An <code>AccessDeniedException</code> will
     * be thrown if the subject of the given <code>context</code> is not
     * granted access to the specified workspace.
     *
     * @param context access manager context
     * @throws AccessDeniedException if the subject is not granted access
     *                               to the specified workspace.
     * @throws Exception             if another error occurs
     */
    void init(AMContext context) throws AccessDeniedException, Exception;

    /**
     * Close this access manager. After having closed an access manager,
     * further operations on this object are treated as illegal and throw
     *
     * @throws Exception if an error occurs
     */
    void close() throws Exception;

    /**
     * Determines whether the specified <code>permissions</code> are granted
     * on the item with the specified <code>id</code> (i.e. the <i>target</i> item).
     *
     * @param id          the id of the target item
     * @param permissions A combination of one or more of the following constants
     *                    encoded as a bitmask value:
     *                    <ul>
     *                    <li><code>READ</code></li>
     *                    <li><code>WRITE</code></li>
     *                    <li><code>REMOVE</code></li>
     *                    </ul>
     * @throws AccessDeniedException if permission is denied
     * @throws ItemNotFoundException if the target item does not exist
     * @throws RepositoryException   it an error occurs
     */
    void checkPermission(ItemId id, int permissions)
            throws AccessDeniedException, ItemNotFoundException, RepositoryException;

    /**
     * Determines whether the specified <code>permissions</code> are granted
     * on the item with the specified <code>id</code> (i.e. the <i>target</i> item).
     *
     * @param id          the id of the target item
     * @param permissions A combination of one or more of the following constants
     *                    encoded as a bitmask value:
     *                    <ul>
     *                    <li><code>READ</code></li>
     *                    <li><code>WRITE</code></li>
     *                    <li><code>REMOVE</code></li>
     *                    </ul>
     * @return <code>true</code> if permission is granted; otherwise <code>false</code>
     * @throws ItemNotFoundException if the target item does not exist
     * @throws RepositoryException   if another error occurs
     */
    boolean isGranted(ItemId id, int permissions)
            throws ItemNotFoundException, RepositoryException;

    /**
     * Determines whether the subject of the current context is granted access
     * to the given workspace.
     *
     * @param workspaceName name of workspace
     * @return <code>true</code> if the subject of the current context is
     *         granted access to the given workspace; otherwise <code>false</code>.
     * @throws NoSuchWorkspaceException if a workspace with the given name
     *                                  does not exist.
     * @throws RepositoryException      if another error occurs
     */
    boolean canAccess(String workspaceName)
            throws NoSuchWorkspaceException, RepositoryException;
}
