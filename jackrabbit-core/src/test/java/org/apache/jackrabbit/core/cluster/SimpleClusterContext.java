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

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.config.ClusterConfig;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

/**
 * Simple cluster context, providing only limited functionality.
 */
public class SimpleClusterContext implements ClusterContext {

    /** Cluster config. */
    private ClusterConfig cc;

    /** Repository home. */
    private File repositoryHome;

    /**
     * Create a new instance of this class.
     *
     * @param cc cluster config
     * @param repHome repository home
     */
    public SimpleClusterContext(ClusterConfig cc, File repositoryHome) {
        this.cc = cc;
        this.repositoryHome = repositoryHome;
    }

    /**
     * {@inheritDoc}
     */
    public ClusterConfig getClusterConfig() {
        return cc;
    }

    /**
     * {@inheritDoc}
     */
    public NamespaceResolver getNamespaceResolver() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public File getRepositoryHome() {
        return repositoryHome;
    }

    /**
     * {@inheritDoc}
     */
    public void lockEventsReady(String workspace) throws RepositoryException {}

    /**
     * {@inheritDoc}
     */
    public void updateEventsReady(String workspace) throws RepositoryException {}
}
