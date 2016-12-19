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
package org.apache.jackrabbit.spi;

import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;

/**
 * <code>SessionInfo</code> is created upon
 * {@link RepositoryService#obtain(javax.jcr.Credentials, String)} or
 * {@link RepositoryService#obtain(SessionInfo, String)} and will be used for
 * any call on the RepositoryService that requires user and workspace
 * identification.
 * <p>
 * In addition the SessionInfo acts as primary container for
 * lock tokens. They will assert that a given SessionInfo is able to execute
 * operations on the RepositoryService that are affected by existing locks.
 */
public interface SessionInfo {

    /**
     * Returns the user id.
     *
     * @return The user identification.
     * @see RepositoryService#obtain(javax.jcr.Credentials, String)
     */
    public String getUserID();

    /**
     * Returns the workspace name.
     *
     * @return The name of the {@link javax.jcr.Workspace workspace} this
     * SessionInfo has been built for.
     * @see RepositoryService#obtain(javax.jcr.Credentials, String)
     * @see javax.jcr.Workspace#getName()
     */
    public String getWorkspaceName();

    /**
     * Returns the lock tokens present on this <code>SessionInfo</code>.
     *
     * @return lock tokens present on this <code>SessionInfo</code>.
     * @throws UnsupportedRepositoryOperationException If locking is not supported.
     * @throws RepositoryException If another error occurs.
     */
    public String[] getLockTokens() throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * Add the given lock token to this <code>SessionInfo</code>. The token will
     * enable the SessionInfo to operate on Items that are affected by the
     * lock identified by the given token.
     *
     * @param lockToken to be added.
     * @throws UnsupportedRepositoryOperationException If locking is not supported.
     * @throws LockException If the token cannot be added.
     * @throws RepositoryException If another error occurs.
     */
    public void addLockToken(String lockToken) throws UnsupportedRepositoryOperationException, LockException, RepositoryException;

    /**
     * Removes the given lock token from this <code>SessionInfo</code>.
     * This must happen if the associated Session successfully removes the Lock
     * from a Node or if the token is removed from the Session itself by calling
     * {@link javax.jcr.Session#removeLockToken(String)}. Consequently all
     * <code>RepositoryService</code> operations affected by a lock will fail
     * with LockException provided the lock hasn't been released.
     *
     * @param lockToken to be removed.
     * @throws UnsupportedRepositoryOperationException If locking is not supported.
     * @throws LockException If the token cannot be removed.
     * @throws RepositoryException If another error occurs.
     */
    public void removeLockToken(String lockToken) throws UnsupportedRepositoryOperationException, LockException, RepositoryException;

    /**
     * Sets the user data used for {@link org.apache.jackrabbit.spi.Event#getUserData()}.
     *
     * @param userData
     * @throws RepositoryException
     * @see javax.jcr.observation.ObservationManager#setUserData(String)
     */
    public void setUserData(String userData) throws RepositoryException;
}
