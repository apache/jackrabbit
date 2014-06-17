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

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;

/**
 * This interface defines the internal baseline.
 * <p>
 * A baseline is the state of a configuration at some point in time, recorded in
 * version storage. A baseline is similar to a normal version except that
 * instead of representing the state of a single node, it represents the state
 * of an entire partial subgraph.
 * <p>
 * The internal baseline is the version of the internal configuration.
 */
public interface InternalBaseline extends InternalVersion {

    /**
     * Returns the recorded base versions of all versionable nodes in the
     * configuration.
     *
     * @return a map of base versions. the map key is the nodeid of the
     * version history.
     * @throws RepositoryException if an error occurs
     */
    VersionSet getBaseVersions() throws RepositoryException;

    /**
     * Returns the id of the nt:configuration node. this is basically the
     * versionable id of the history.
     *
     * @return the configuration node id
     */
    NodeId getConfigurationId();

    /**
     * Returns the id of the root node of a workspace configuration. this is
     * basically the jcr:root property of the frozen configuration.
     *
     * @return the configuration root node id
     */
    NodeId getConfigurationRootId();
}