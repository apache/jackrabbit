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
package org.apache.jackrabbit.core.cluster;

import java.io.File;

import org.apache.jackrabbit.core.config.ClusterConfig;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

import javax.jcr.RepositoryException;

/**
 * Initial interface passed to a <code>ClusterNode</code>.
 */
public interface ClusterContext {

    /**
     * Return the cluster configuration.
     *
     * @return cluster configuration
     */
    ClusterConfig getClusterConfig();

    /**
     * Return the repository home directory.
     *
     * @return repository home directory
     */
    File getRepositoryHome();

    /**
     * Return a namespace resolver to map prefixes to URIs and vice-versa
     *
     * @return namespace resolver
     */
    NamespaceResolver getNamespaceResolver();

    /**
     * Notifies the cluster context that some workspace update events are available
     * and that it should start up a listener to receive them.
     *
     * @param workspace workspace name
     * @throws RepositoryException if the context is unable to provide the listener
     */
    void updateEventsReady(String workspace) throws RepositoryException;

    /**
     * Notifies the cluster context that some workspace lock events are available
     * and that it should start up a listener to receive them.
     *
     * @param workspace workspace name
     * @throws RepositoryException if the context is unable to provide the listener
     */
    void lockEventsReady(String workspace) throws RepositoryException;

}
