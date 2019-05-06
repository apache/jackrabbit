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

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

/**
 * The <code>AccessManager</code> can be queried to determines whether privileges
 * are granted on a specific item.
 */
public interface AccessManager {

    /**
     * READ permission constant
     * @deprecated
     */
    int READ = 1;

    /**
     * WRITE permission constant
     * @deprecated
     */
    int WRITE = 2;

    /**
     * REMOVE permission constant
     * @deprecated 
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
     * Initialize this access manager. An <code>AccessDeniedException</code> will
     * be thrown if the subject of the given <code>context</code> is not
     * granted access to the specified workspace.
     *
     * @param context access manager context.
     * @param acProvider The access control provider.
     * @param wspAccessMgr The workspace access manager.
     * @throws AccessDeniedException if the subject is not granted access
     *                               to the specified workspace.
     * @throws Exception             if another error occurs
     */
    void init(AMContext context, AccessControlProvider acProvider,
              WorkspaceAccessManager wspAccessMgr) throws AccessDeniedException, Exception;

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
     * @deprecated 
     */
    void checkPermission(ItemId id, int permissions)
            throws AccessDeniedException, ItemNotFoundException, RepositoryException;

    /**
     * Determines whether the specified <code>permissions</code> are granted
     * on the item with the specified <code>id</code> (i.e. the <i>target</i> item).
     *
     * @param absPath Path to an item.
     * @param permissions A combination of one or more of the
     * {@link org.apache.jackrabbit.core.security.authorization.Permission}
     * constants encoded as a bitmask value.
     * @throws AccessDeniedException if permission is denied
     * @throws RepositoryException   it another error occurs
     */
    void checkPermission(Path absPath, int permissions) throws AccessDeniedException, RepositoryException;

    /**
     * Determines whether the specified <code>permissions</code> are granted
     * on the repository level.
     *
     * @param permissions The permissions to check.
     * @throws AccessDeniedException if permissions are denied.
     * @throws RepositoryException if another error occurs.
     */
    void checkRepositoryPermission(int permissions) throws AccessDeniedException, RepositoryException;

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
     * @deprecated
     */
    boolean isGranted(ItemId id, int permissions)
            throws ItemNotFoundException, RepositoryException;

    /**
     * Determines whether the specified <code>permissions</code> are granted
     * on the item with the specified <code>absPath</code> (i.e. the <i>target</i>
     * item, that may or may not yet exist).
     *
     * @param absPath     the absolute path to test
     * @param permissions A combination of one or more of the
     * {@link org.apache.jackrabbit.core.security.authorization.Permission}
     * constants encoded as a bitmask value.
     * @return <code>true</code> if the specified permissions are granted;
     * otherwise <code>false</code>.
     * @throws RepositoryException if an error occurs.
     */
    boolean isGranted(Path absPath, int permissions) throws RepositoryException;

    /**
     * Determines whether the specified <code>permissions</code> are granted
     * on an item represented by the combination of the given
     * <code>parentPath</code> and <code>childName</code> (i.e. the <i>target</i>
     * item, that may or may not yet exist).
     *
     * @param parentPath  Path to an existing parent node.
     * @param childName   Name of the child item that may or may not exist yet.
     * @param permissions A combination of one or more of the
     * {@link org.apache.jackrabbit.core.security.authorization.Permission}
     * constants encoded as a bitmask value.
     * @return <code>true</code> if the specified permissions are granted;
     * otherwise <code>false</code>.
     * @throws RepositoryException if an error occurs.
     */
    boolean isGranted(Path parentPath, Name childName, int permissions) throws RepositoryException;

    /**
     * Determines whether the item with the specified <code>itemPath</code>
     * or <code>itemId</code> can be read. Either of the two parameters
     * may be <code>null</code>.<br>
     * Note, that this method should only be called for persisted items as NEW
     * items may not be visible to the permission evaluation.
     * For new items {@link #isGranted(Path, int)} should be used instead.
     * <p>
     * If this method is called with both Path and ItemId it is left to the
     * evaluation, which parameter is used.
     *
     * @param itemPath The path to the item or <code>null</code> if itemId
     * should be used to determine the READ permission.
     * @param itemId Id of the item to be tested or <code>null</code> if the
     * itemPath should be used to determine the permission.
     * @return <code>true</code> if the item can be read; otherwise <code>false</code>.
     * @throws RepositoryException if the item is NEW and only an itemId is
     * specified or if another error occurs.
     */
    boolean canRead(Path itemPath, ItemId itemId) throws RepositoryException;

    /**
     * Determines whether the subject of the current context is granted access
     * to the given workspace. Note that an implementation is free to test for
     * the existence of a workspace with the specified name. In this case
     * the expected return value is <code>false</code>, if no such workspace
     * exists.
     *
     * @param workspaceName name of workspace
     * @return <code>true</code> if the subject of the current context is
     *         granted access to the given workspace; otherwise <code>false</code>.
     * @throws RepositoryException if an error occurs.
     */
    boolean canAccess(String workspaceName) throws RepositoryException;
}
