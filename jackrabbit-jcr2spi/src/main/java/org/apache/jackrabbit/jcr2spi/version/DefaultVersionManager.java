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

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;

import java.util.Iterator;

/**
 * <code>DefaultVersionManager</code>...
 */
public class DefaultVersionManager implements VersionManager {

    private static Logger log = LoggerFactory.getLogger(DefaultVersionManager.class);

    public NodeEntry checkin(NodeState nodeState) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Versioning ist not supported by this repository.");
    }

    public void checkout(NodeState nodeState) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Versioning ist not supported by this repository.");
    }

    public boolean isCheckedOut(NodeState nodeState) throws RepositoryException {
        log.debug("Versioning is not supported by this repository.");
        return true;
    }

    public void checkIsCheckedOut(NodeState nodeState) throws VersionException, RepositoryException {
        // ignore
    }

    public void removeVersion(NodeState versionHistoryState, NodeState versionState) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Versioning ist not supported by this repository.");
    }

    public void addVersionLabel(NodeState versionHistoryState, NodeState versionState, Name qLabel, boolean moveLabel) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Versioning ist not supported by this repository.");
    }

    public void removeVersionLabel(NodeState versionHistoryState, NodeState versionState, Name qLabel) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Versioning ist not supported by this repository.");
    }

    public void restore(NodeState nodeState, Path relativePath, NodeState versionState, boolean removeExisting) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Versioning ist not supported by this repository.");
    }

    public void restore(NodeState[] versionStates, boolean removeExisting) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Versioning ist not supported by this repository.");
    }

    public Iterator merge(NodeState nodeState, String workspaceName, boolean bestEffort) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Versioning ist not supported by this repository.");
    }

    public void resolveMergeConflict(NodeState nodeState, NodeState versionState, boolean done) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Versioning ist not supported by this repository.");
    }

    public NodeEntry getVersionableNodeEntry(NodeState versionState) {
        throw new UnsupportedOperationException();
    }

    public NodeEntry getVersionHistoryEntry(NodeState versionableState) {
        throw new UnsupportedOperationException();
    }
}