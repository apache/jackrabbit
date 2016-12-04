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

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.MergeException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.jcr.version.Version;
import java.util.Iterator;

/**
 * <code>VersionManager</code>...
 */
public interface VersionManager {

    /**
     * @param nodeState
     * @return <code>NodeEntry</code> of newly created version
     * @throws VersionException
     * @throws UnsupportedRepositoryOperationException
     * @throws InvalidItemStateException
     * @throws LockException
     * @throws RepositoryException
     * @see javax.jcr.Node#checkin()
     */
    public NodeEntry checkin(NodeState nodeState) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException;

    /**
     * @param nodeState
     * @throws UnsupportedRepositoryOperationException
     * @throws LockException
     * @throws RepositoryException
     * @see javax.jcr.Node#checkout()
     */
    public void checkout(NodeState nodeState) throws UnsupportedRepositoryOperationException, LockException, RepositoryException;

    /**
     * 
     * @param nodeState
     * @param activityId
     * @throws RepositoryException
     */
    public void checkout(NodeState nodeState, NodeId activityId) throws RepositoryException;

    /**
     * @param nodeState
     * @throws RepositoryException
     * @see javax.jcr.version.VersionManager#checkpoint(String)
     */
    public NodeEntry checkpoint(NodeState nodeState) throws RepositoryException;

    /**
     * @param nodeState
     * @throws RepositoryException
     * @see javax.jcr.version.VersionManager#checkpoint(String)
     */
    public NodeEntry checkpoint(NodeState nodeState, NodeId activityId) throws RepositoryException;
    /**
     * @param nodeState
     * @return
     * @throws RepositoryException
     * @see javax.jcr.Node#isCheckedOut()
     */
    public boolean isCheckedOut(NodeState nodeState) throws RepositoryException;

    /**
     * @param nodeState
     * @throws VersionException If the <code>Node</code> represented by the given
     * <code>NodeState</code> is checkedin.
     * @throws RepositoryException If another error occurs.
     * @see javax.jcr.Node#isCheckedOut()
     */
    public void checkIsCheckedOut(NodeState nodeState) throws VersionException, RepositoryException;

    /**
     * @param versionHistoryState
     * @param versionState
     * @throws ReferentialIntegrityException
     * @throws AccessDeniedException
     * @throws UnsupportedRepositoryOperationException
     * @throws VersionException
     * @throws RepositoryException
     * @see javax.jcr.version.VersionHistory#removeVersion(String)
     */
    public void removeVersion(NodeState versionHistoryState, NodeState versionState) throws ReferentialIntegrityException, AccessDeniedException, UnsupportedRepositoryOperationException, VersionException, RepositoryException;

    /**
     * @param versionHistoryState
     * @param versionState
     * @param qLabel
     * @param moveLabel
     * @throws VersionException
     * @throws RepositoryException
     * @see javax.jcr.version.VersionHistory#addVersionLabel(String, String, boolean)
     */
    public void addVersionLabel(NodeState versionHistoryState, NodeState versionState, Name qLabel, boolean moveLabel) throws VersionException, RepositoryException;

    /**
     * @param versionHistoryState
     * @param versionState
     * @param qLabel
     * @throws VersionException
     * @throws RepositoryException
     * @see javax.jcr.version.VersionHistory#removeVersionLabel(String)
     */
    public void removeVersionLabel(NodeState versionHistoryState, NodeState versionState, Name qLabel) throws VersionException, RepositoryException;

    /**
     * @param nodeState
     * @param relativePath
     * @param versionState
     * @param removeExisting
     * @throws VersionException
     * @throws ItemExistsException
     * @throws UnsupportedRepositoryOperationException
     * @throws LockException
     * @throws InvalidItemStateException
     * @throws RepositoryException
     * @see javax.jcr.Node#restore(String, boolean)
     * @see javax.jcr.Node#restore(Version, boolean)
     * @see javax.jcr.Node#restore(Version, String, boolean)
     * @see javax.jcr.Node#restoreByLabel(String, boolean)
     */
    public void restore(NodeState nodeState, Path relativePath, NodeState versionState, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * @param versionStates
     * @param removeExisting
     * @throws ItemExistsException
     * @throws UnsupportedRepositoryOperationException
     * @throws VersionException
     * @throws LockException
     * @throws InvalidItemStateException
     * @throws RepositoryException
     * @see javax.jcr.Workspace#restore(Version[], boolean)
     */
    public void restore(NodeState[] versionStates, boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * @param nodeState
     * @param workspaceName
     * @param bestEffort
     * @return An Iterator over <code>NodeId</code>s of all <code>Node</code>s
     * that failed to be merged and need manual resolution by the user of the API.
     * @throws NoSuchWorkspaceException
     * @throws AccessDeniedException
     * @throws MergeException
     * @throws LockException
     * @throws InvalidItemStateException
     * @throws RepositoryException
     * @see #resolveMergeConflict(NodeState,NodeState,boolean)
     * @see javax.jcr.Node#merge(String, boolean)
     */
    public Iterator<NodeId> merge(NodeState nodeState, String workspaceName, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException;

    /**
     * @param nodeState
     * @param workspaceName
     * @param bestEffort
     * @param isShallow
     * @return An Iterator over <code>NodeId</code>s of all <code>Node</code>s
     * that failed to be merged and need manual resolution by the user of the API.
     * @throws NoSuchWorkspaceException
     * @throws AccessDeniedException
     * @throws MergeException
     * @throws LockException
     * @throws InvalidItemStateException
     * @throws RepositoryException
     * @see #resolveMergeConflict(NodeState,NodeState,boolean)
     * @see javax.jcr.Node#merge(String, boolean)
     */
    public Iterator<NodeId> merge(NodeState nodeState, String workspaceName, boolean bestEffort, boolean isShallow) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException;


    /**
     * @param nodeState
     * @param versionState
     * @param done
     * @throws VersionException
     * @throws InvalidItemStateException
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     * @see javax.jcr.Node#cancelMerge(Version)
     * @see javax.jcr.Node#doneMerge(Version)
     */
    public void resolveMergeConflict(NodeState nodeState, NodeState versionState, boolean done) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException;

    /**
     *
     * @param nodeState
     * @return
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     */
    public NodeEntry createConfiguration(NodeState nodeState) throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     *
     * @param title
     * @return
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     */
    public NodeEntry createActivity(String title) throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     *
     * @param activityState
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     */
    public void removeActivity(NodeState activityState) throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     * 
     * @param activityState
     * @return
     * @throws UnsupportedRepositoryOperationException
     * @throws RepositoryException
     */
    public Iterator<NodeId> mergeActivity(NodeState activityState) throws UnsupportedRepositoryOperationException, RepositoryException;

    /**
     *
     * @param versionState
     * @return
     */
    public NodeEntry getVersionableNodeEntry(NodeState versionState) throws RepositoryException;

    /**
     *
     * @param versionableState
     * @return
     */
    public NodeEntry getVersionHistoryEntry(NodeState versionableState) throws RepositoryException;
}