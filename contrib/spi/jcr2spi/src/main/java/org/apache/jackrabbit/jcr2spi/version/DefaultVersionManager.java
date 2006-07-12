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
import org.apache.jackrabbit.spi.NodeId;
import java.util.Collection;

/**
 * <code>DefaultVersionManager</code>...
 */
public class DefaultVersionManager implements VersionManager {

    private static Logger log = LoggerFactory.getLogger(DefaultVersionManager.class);

    public void checkin(NodeId nodeId) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Versioning ist not supported by this repository.");
    }

    public void checkout(NodeId nodeId) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Versioning ist not supported by this repository.");
    }

    public boolean isCheckedOut(NodeId nodeId) throws RepositoryException {
        log.debug("Versioning is not supported by this repository.");
        return true;
    }

    public void restore(NodeId nodeId, NodeId versionId, boolean removeExisting) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Versioning ist not supported by this repository.");
    }

    public void restore(NodeId[] versionIds, boolean removeExisting) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Versioning ist not supported by this repository.");
    }

    public Collection merge(NodeId nodeId, String workspaceName, boolean bestEffort) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Versioning ist not supported by this repository.");
    }

    public void resolveMergeConflict(NodeId nodeId, NodeId versionId, boolean done) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("Versioning ist not supported by this repository.");
    }
}