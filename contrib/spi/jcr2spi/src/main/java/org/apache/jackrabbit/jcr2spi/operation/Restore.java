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
package org.apache.jackrabbit.jcr2spi.operation;

import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.name.Path;

import javax.jcr.RepositoryException;
import javax.jcr.ItemExistsException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.PathNotFoundException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.jcr.version.Version;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * <code>Restore</code>...
 */
public class Restore extends AbstractOperation {

    // TODO: since the restore target can point to a non-existing item -> use NodeId
    // TODO: review this.
    private final NodeState nodeState;
    private final Path relQPath;
    private final NodeState[] versionStates;
    private final boolean removeExisting;

    private Restore(NodeState nodeState, Path relQPath, NodeState[] versionStates, boolean removeExisting) {
        this.nodeState = nodeState;
        this.relQPath = relQPath;
        this.versionStates = versionStates;
        this.removeExisting = removeExisting;

        // TODO: affected states... needed?
    }

    //----------------------------------------------------------< Operation >---
    /**
     * @see Operation#accept(OperationVisitor)
     */
    public void accept(OperationVisitor visitor) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        visitor.visit(this);
    }

    /**
     * @see Operation#persisted()
     */
    public void persisted() {
        // TODO
    }
    //----------------------------------------< Access Operation Parameters >---

    /**
     * Returns state or the closest existing state of the restore target or
     * <code>null</code> in case of a {@link javax.jcr.Workspace#restore(Version[], boolean)}
     *
     * @return
     */
    public NodeState getNodeState() {
        return nodeState;
    }

    /**
     * Relative qualified path to the non-existing restore target or <code>null</code>
     * if the state returned by {@link #getNodeState()} is the target.
     *
     * @return
     * @see javax.jcr.Node#restore(Version, String, boolean) 
     */
    public Path getRelativePath() {
        return relQPath;
    }

    public NodeState[] getVersionStates() {
        return versionStates;
    }

    public boolean removeExisting() {
        return removeExisting;
    }

    //------------------------------------------------------------< Factory >---
    /**
     *
     * @param nodeState
     * @param versionState
     * @return
     */
    public static Operation create(NodeState nodeState, Path relQPath, NodeState versionState, boolean removeExisting) {
        if (nodeState == null || versionState == null) {
            throw new IllegalArgumentException("Neither nodeId nor versionState must be null.");
        }
        Restore up = new Restore(nodeState, relQPath, new NodeState[] {versionState}, removeExisting);
        return up;
    }

    /**
     *
     * @param versionStates
     * @return
     */
    public static Operation create(NodeState[] versionStates, boolean removeExisting) {
        if (versionStates == null) {
            throw new IllegalArgumentException("Version states must not be null.");
        }
        Restore up = new Restore(null, null, versionStates, removeExisting);
        return up;
    }
}