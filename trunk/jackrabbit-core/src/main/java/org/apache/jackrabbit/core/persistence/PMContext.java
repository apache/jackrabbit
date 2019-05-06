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
package org.apache.jackrabbit.core.persistence;

import java.io.File;

import javax.jcr.NamespaceRegistry;

import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.stats.RepositoryStatisticsImpl;

/**
 * A <code>PMContext</code> is used to provide context information for a
 * <code>PersistenceManager</code>.
 *
 * @see PersistenceManager#init(PMContext)
 */
public class PMContext {

    /**
     * the physical home dir
     */
    private final File physicalHomeDir;

    /**
     * the virtual jackrabbit filesystem
     */
    private final FileSystem fs;

    /**
     * namespace registry
     */
    private final NamespaceRegistry nsReg;

    /**
     * node type registry
     */
    private final NodeTypeRegistry ntReg;

    /**
     * uuid of the root node
     */
    private final NodeId rootNodeId;

    /**
     * Data store for binary properties.
     */
    private final DataStore dataStore;

    /** Repository statistics collector. */
    private final RepositoryStatisticsImpl stats;

    /**
     * Creates a new <code>PMContext</code>.
     *
     * @param homeDir the physical home directory
     * @param fs the virtual jackrabbit filesystem
     * @param rootNodeId id of the root node
     * @param nsReg        namespace registry
     * @param ntReg        node type registry
     */
    public PMContext(File homeDir,
            FileSystem fs,
            NodeId rootNodeId,
            NamespaceRegistry nsReg,
            NodeTypeRegistry ntReg,
            DataStore dataStore,
            RepositoryStatisticsImpl stats) {
        this.physicalHomeDir = homeDir;
        this.fs = fs;
        this.rootNodeId = rootNodeId;
        this.nsReg = nsReg;
        this.ntReg = ntReg;
        this.dataStore = dataStore;
        this.stats = stats;
    }


    /**
     * Returns the physical home directory for this persistence manager
     * @return the physical home directory for this persistence manager
     */
    public File getHomeDir() {
        return physicalHomeDir;
    }

    /**
     * Returns the virtual filesystem for this persistence manager
     * @return the virtual filesystem for this persistence manager
     */
    public FileSystem getFileSystem() {
        return fs;
    }

    /**
     * Returns the id of the root node
     * @return the id of the root node
     */
    public NodeId getRootNodeId() {
        return rootNodeId;
    }

    /**
     * Returns the namespace registry
     *
     * @return the namespace registry
     */
    public NamespaceRegistry getNamespaceRegistry() {
        return nsReg;
    }

    /**
     * Returns the node type registry
     *
     * @return the node type registry
     */
    public NodeTypeRegistry getNodeTypeRegistry() {
        return ntReg;
    }

    /**
     * Returns the data store
     *
     * @return the data store
     */
    public DataStore getDataStore() {
        return dataStore;
    }


    /**
     * Returns the repository statistics collector.
     *
     * @return repository statistics
     */
    public RepositoryStatisticsImpl getRepositoryStatistics() {
        return stats;
    }

}
