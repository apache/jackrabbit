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

import java.util.Calendar;

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
     * Returns information about the version history of the specified node.
     * If the given node does not already have an associated version history,
     * then an empty history is automatically created. This method should
     * only be called by code that already knows that the specified node
     * is versionable.
     *
     * @param session workspace session
     * @param node node whose version history should be returned
     * @return identifiers of the version history and root version nodes
     * @throws RepositoryException if an error occurs
     */
    VersionHistoryInfo getVersionHistory(Session session, NodeState node)
            throws RepositoryException;

    /**
     * invokes the checkin() on the persistent version manager and remaps the
     * newly created version objects.
     *
     * @param node node to checkin
     * @param cal create time of the new version, or <code>null</code>
     * @return the newly created version
     * @throws RepositoryException if an error occurs
     */
    Version checkin(NodeImpl node, Calendar cal) throws RepositoryException;

    /**
     * Removes the specified version from the given version history.
     * @param history version history to remove the version from
     * @param versionName name of the version
     * @throws RepositoryException if an error occurs
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
     * @param history version history
     * @param version name of the version
     * @param label new label
     * @param move if <code>true</code> label will be moved
     * @return the version that had the label or <code>null</code>
     * @throws RepositoryException if an error occurs
     */
    Version setVersionLabel(VersionHistory history, Name version, Name label,
                            boolean move)
            throws RepositoryException;

    /**
     * Returns the version history with the given id
     *
     * @param id id of the version history
     * @return the version history.
     * @throws RepositoryException if an error occurs
     */
    InternalVersionHistory getVersionHistory(NodeId id)
            throws RepositoryException;

    /**
     * Returns the version history for the versionable node with the given id.
     *
     * @param id id of the node to retrieve the version history for
     * @return the version history
     * @throws RepositoryException if an error occurs or the history does not exit
     */
    InternalVersionHistory getVersionHistoryOfNode(NodeId id)
            throws RepositoryException;

    /**
     * Returns the version with the given id
     *
     * @param id id of the version to retrieve
     * @return the version.
     * @throws RepositoryException if an error occurs
     */
    InternalVersion getVersion(NodeId id) throws RepositoryException;

    /**
     * Returns the head version of the node with the given id. this is always
     * the last of all versions. this only works correctly for liner version
     * graphs (i.e. simple versioning)
     *
     * @param id id of the node to retrieve the version for
     * @return the version.
     * @throws RepositoryException if an error occurs
     */
    InternalVersion getHeadVersionOfNode(NodeId id) throws RepositoryException;

    /**
     * Close this version manager. After having closed a persistence
     * manager, further operations on this object are treated as illegal
     * and throw
     *
     * @throws Exception if an error occurs
     */
    void close() throws Exception;
}
