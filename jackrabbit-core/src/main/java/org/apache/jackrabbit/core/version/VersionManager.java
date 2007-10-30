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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
import org.apache.jackrabbit.spi.Name;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

/**
 * This interface defines the version manager. It gives access to the underlying
 * persistence layer of the versioning.
 */
public interface VersionManager {

    /**
     * returns the virtual item state provider that exposes the internal versions
     * as items.
     *
     * @return the virtual item state provider.
     */
    VirtualItemStateProvider getVirtualItemStateProvider();

    /**
     * Creates a new version history. This action is needed either when creating
     * a new 'mix:versionable' node or when adding the 'mix:versionable' mixin
     * to a node.
     *
     * @param node
     * @return
     * @throws RepositoryException
     * @see #getVersionHistory(Session, NodeState)
     */
    VersionHistory createVersionHistory(Session session, NodeState node)
            throws RepositoryException;

    /**
     * Returns the version history of the specified <code>node</code> or
     * <code>null</code> if the given node doesn't (yet) have an associated
     * version history.
     *
     * @param session
     * @param node node whose version history should be returned
     * @return the version history of the specified <code>node</code> or
     *         <code>null</code> if the given node doesn't (yet) have an
     *        associated version history.
     * @throws RepositoryException if an error occurs
     * @see #createVersionHistory(Session, NodeState)
     */
    VersionHistory getVersionHistory(Session session, NodeState node)
            throws RepositoryException;

    /**
     * invokes the checkin() on the persistent version manager and remaps the
     * newly created version objects.
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    Version checkin(NodeImpl node) throws RepositoryException;

    /**
     * Removes the specified version from the given version history.
     * @param history
     * @param versionName
     * @throws RepositoryException
     */
    void removeVersion(VersionHistory history, Name versionName)
            throws RepositoryException;

    /**
     * Sets the version <code>label</code> to the given <code>version</code>.
     * If the label is already assigned to another version, a VersionException is
     * thrown unless <code>move</code> is <code>true</code>. If <code>version</code>
     * is <code>null</code>, the label is removed from the respective version.
     * In either case, the version the label was previously assigned is returned,
     * or <code>null</code> of the label was not moved.
     *
     * @param history
     * @param version
     * @param label
     * @param move
     * @return
     * @throws RepositoryException
     */
    Version setVersionLabel(VersionHistory history, Name version, Name label,
                            boolean move)
            throws RepositoryException;

    /**
     * Checks if the version history with the given id exists
     *
     * @param id
     * @return <code>true</code> if the version history exists.
     */
    boolean hasVersionHistory(NodeId id);

    /**
     * Returns the version history with the given id
     *
     * @param id
     * @return the version history.
     * @throws RepositoryException
     */
    InternalVersionHistory getVersionHistory(NodeId id)
            throws RepositoryException;

    /**
     * Checks if the version with the given id exists
     *
     * @param id
     * @return <code>true</code> if the version exists.
     */
    boolean hasVersion(NodeId id);

    /**
     * Returns the version with the given id
     *
     * @param id
     * @return the version.
     * @throws RepositoryException
     */
    InternalVersion getVersion(NodeId id) throws RepositoryException;

    /**
     * Close this version manager. After having closed a persistence
     * manager, further operations on this object are treated as illegal
     * and throw
     *
     * @throws Exception if an error occurs
     */
    void close() throws Exception;
}
