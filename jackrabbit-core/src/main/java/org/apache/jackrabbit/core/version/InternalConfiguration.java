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

import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;

/**
 * This interface defines the internal represenation of a configuration.
 *
 * A configuration is a partial subgraph of a workspace. It consists of the tree
 * rooted at a particular node (called the configuration root node) minus any
 * subgraphs that are part of another configuration.
 * <p/>
 * An internal configuration soley stores the root id of the actual
 * configuration in the workspace. The configuration is stored at the same
 * relative path as it's baseline history.
 */
public interface InternalConfiguration extends InternalVersionItem {

    /**
     * Returns the id of the root node of the configuration
     * @return the id of the root node.
     * @throws RepositoryException if an error occurs
     */
    NodeId getRootId() throws RepositoryException;

    /**
     * Returns the current baseline of this configuration, i.e. the version of
     * this "versionable"
     *
     * @return the baseline
     * @throws RepositoryException if an error occurs
     */
    InternalBaseline getBaseline() throws RepositoryException;

}