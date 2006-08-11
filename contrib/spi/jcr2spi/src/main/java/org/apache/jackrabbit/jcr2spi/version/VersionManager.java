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
package org.apache.jackrabbit.jcr2spi.version;

import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.jcr2spi.state.NodeState;

import javax.jcr.RepositoryException;
import java.util.Collection;

/**
 * <code>VersionManager</code>...
 */
public interface VersionManager {

    // TODO fix method definitions (throw clauses)
    // TODO review usage of NodeId. for Restore it is requried, since the target must not point to an existing node.

    public void checkin(NodeState nodeState) throws RepositoryException;

    public void checkout(NodeState nodeState) throws RepositoryException;

    public boolean isCheckedOut(NodeState nodeState) throws RepositoryException;

    public void removeVersion(NodeId versionHistoryId, NodeId versionId) throws RepositoryException;

    public void addVersionLabel(NodeId versionHistoryId, NodeId versionId, QName qLabel, boolean moveLabel) throws RepositoryException;

    public void removeVersionLabel(NodeId versionHistoryId, NodeId versionId, QName qLabel) throws RepositoryException;

    public void restore(NodeId nodeId, NodeId versionId, boolean removeExisting) throws RepositoryException;

    public void restore(NodeId[] versionIds, boolean removeExisting) throws RepositoryException;

    /**
     *
     * @param nodeId
     * @param workspaceName
     * @param bestEffort
     * @return A Collection of <code>ItemId</code> containing the ids of those
     * <code>Node</code>s that failed to be merged and need manual resolution
     * by the user of the API.
     * @see #resolveMergeConflict(NodeId,NodeId,boolean)
     */
    public Collection merge(NodeId nodeId, String workspaceName, boolean bestEffort) throws RepositoryException;

    /**
     * 
     * @param nodeId
     * @param versionId
     * @param done
     * @throws RepositoryException
     */
    public void resolveMergeConflict(NodeId nodeId, NodeId versionId, boolean done) throws RepositoryException;
}