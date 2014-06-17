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
package org.apache.jackrabbit.core.config;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemFactory;
import org.apache.jackrabbit.core.query.QueryHandler;
import org.apache.jackrabbit.core.query.QueryHandlerContext;
import org.apache.jackrabbit.core.query.QueryHandlerFactory;
import org.apache.jackrabbit.core.state.ISMLocking;
import org.apache.jackrabbit.core.state.ISMLockingFactory;

/**
 * Workspace configuration. This configuration class is used to create
 * configured workspace objects.
 * <p>
 * The contained configuration information are: the home directory and name of
 * the workspace, the file system, the persistence manager, the search index and
 * the item state manager locking configuration. The search index and the item
 * state manager locking and the security config are optional parts.
 */
public class WorkspaceConfig
        implements FileSystemFactory, ISMLockingFactory, QueryHandlerFactory {

    /**
     * Workspace home directory.
     */
    private final String home;

    /**
     * Workspace name.
     */
    private final String name;

    /**
     * Flag indicating whether this workspace participates in a cluster.
     */
    private final boolean clustered;

    /**
     * Workspace file system factory.
     */
    private FileSystemFactory fsf;

    /**
     * Workspace persistence manager configuration.
     */
    private PersistenceManagerConfig pmc;

    /**
     * Query handler factory, or <code>null</code> if search is not configured.
     */
    private QueryHandlerFactory qhf;

    /**
     * The item state manager locking factory.
     */
    private ISMLockingFactory ismLockingFactory;

    /**
     * Workspace security configuration. Can be <code>null</code>.
     */
    private final WorkspaceSecurityConfig workspaceSecurityConfig;

    /**
     * Optional configuration for the xml import behavior. Up to now this consists
     * of a single configuration point: the treatment
     * of protected nodes and properties that is defined by a set of classes
     * implementing {@link org.apache.jackrabbit.core.xml.ProtectedNodeImporter}
     * or {@link org.apache.jackrabbit.core.xml.ProtectedPropertyImporter}.
     */
    private final ImportConfig importConfig;

    /**
     * Default lock timeout in seconds.
     */
    private final long defaultLockTimeout;

    /**
     * Creates a workspace configuration object.
     *
     * @param home home directory
     * @param name workspace name
     * @param clustered
     * @param fsf file system factory
     * @param pmc persistence manager configuration
     * @param qhf query handler factory, or <code>null</code> if not configured
     * @param ismLockingFactory the item state manager locking factory
     * @param workspaceSecurityConfig the workspace specific security configuration.
     */
    public WorkspaceConfig(String home, String name, boolean clustered,
                           FileSystemFactory fsf, PersistenceManagerConfig pmc,
                           QueryHandlerFactory qhf,
                           ISMLockingFactory ismLockingFactory,
                           WorkspaceSecurityConfig workspaceSecurityConfig) {
        this(home, name, clustered, fsf, pmc, qhf, ismLockingFactory, workspaceSecurityConfig, null, Long.MAX_VALUE);
    }

    /**
     * Creates a workspace configuration object.
     *
     * @param home home directory
     * @param name workspace name
     * @param clustered
     * @param fsf file system factory
     * @param pmc persistence manager configuration
     * @param qhf query handler factory, or <code>null</code> if not configured
     * @param ismLockingFactory the item state manager locking factory
     * @param workspaceSecurityConfig the workspace specific security configuration.
     */
    public WorkspaceConfig(String home, String name, boolean clustered,
                           FileSystemFactory fsf, PersistenceManagerConfig pmc,
                           QueryHandlerFactory qhf,
                           ISMLockingFactory ismLockingFactory,
                           WorkspaceSecurityConfig workspaceSecurityConfig,
                           ImportConfig importConfig) {
        this(home, name, clustered, fsf, pmc, qhf, ismLockingFactory, workspaceSecurityConfig, importConfig, Long.MAX_VALUE);
    }

    /**
     * Creates a workspace configuration object.
     *
     * @param home home directory
     * @param name workspace name
     * @param clustered
     * @param fsf file system factory
     * @param pmc persistence manager configuration
     * @param qhf query handler factory, or <code>null</code> if not configured
     * @param ismLockingFactory the item state manager locking factory
     * @param workspaceSecurityConfig the workspace specific security configuration.
     * @param defaultLockTimeout default timeout for locks (in seconds)
     */
    public WorkspaceConfig(String home, String name, boolean clustered, FileSystemFactory fsf,
            PersistenceManagerConfig pmc, QueryHandlerFactory qhf, ISMLockingFactory ismLockingFactory,
            WorkspaceSecurityConfig workspaceSecurityConfig, ImportConfig importConfig, long defaultLockTimeout) {
        this.home = home;
        this.name = name;
        this.clustered = clustered;
        this.fsf = fsf;
        this.pmc = pmc;
        this.qhf = qhf;
        this.ismLockingFactory = ismLockingFactory;
        this.workspaceSecurityConfig = workspaceSecurityConfig;
        this.importConfig = importConfig;
        this.defaultLockTimeout = defaultLockTimeout;
    }

    /**
     * Returns the workspace home directory.
     *
     * @return workspace home directory
     */
    public String getHomeDir() {
        return home;
    }

    /**
     * Returns the workspace name.
     *
     * @return the workspace name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a flag indicating whether this workspace participates in a cluster.
     *
     * @return <code>true</code> if this workspace participates in a cluster;
     *         <code>false</code> otherwise
     */
    public boolean isClustered() {
        return clustered;
    }

    /**
     * Returns the default lock timeout in number of seconds or
     * <code>Long.MAX_VALUE</code> when not specified.
     * 
     * @return default lock timeout in number of seconds or
     *         <code>Long.MAX_VALUE</code> when not specified
     */
    public long getDefaultLockTimeout() {
        return defaultLockTimeout;
    }

    /**
     * Creates and returns the configured workspace locking strategy.
     *
     * @return the configured {@link ISMLocking}
     * @throws RepositoryException if the locking strategy can not be created
     */
    public ISMLocking getISMLocking() throws RepositoryException {
        return ismLockingFactory.getISMLocking();
    }

    /**
     * Creates and returns the configured workspace file system.
     *
     * @return the configured {@link FileSystem}
     * @throws RepositoryException if the file system can not be created
     */
    public FileSystem getFileSystem() throws RepositoryException {
        return fsf.getFileSystem();
    }

    /**
     * Returns the workspace persistence manager configuration.
     *
     * @return persistence manager configuration
     */
    public PersistenceManagerConfig getPersistenceManagerConfig() {
        return pmc;
    }

    /**
     * Checks whether search configuration is present.
     *
     * @return <code>true</code> if search is configured,
     *         <code>false</code> otherwise
     */
    public boolean isSearchEnabled() {
        return qhf != null;
    }

    /**
     * Returns an initialized query handler, or <code>null</code> if one
     * was not configured.
     *
     * @return initialized query handler, or <code>null</code>
     */
    public QueryHandler getQueryHandler(QueryHandlerContext context)
            throws RepositoryException {
        if (qhf != null) {
            return qhf.getQueryHandler(context);
        } else {
            return null;
        }
    }
    /**
     * @return workspace-specific security settings.
     * @see WorkspaceSecurityConfig
     */
    public WorkspaceSecurityConfig getSecurityConfig() {
        return workspaceSecurityConfig;
    }

    /**
     * @return xml import settings
     */
    public ImportConfig getImportConfig() {
        return importConfig;
    }
}
