/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.jdom.Document;

/**
 * A <code>RepositoryConfig</code> ...
 */
public class RepositoryConfig {
    
    public static RepositoryConfig create(String file, String home)
            throws RepositoryException {
        ConfigurationParser parser = new ConfigurationParser(new Properties());
        return parser.parseRepositoryConfig(file, home);
    }
    
    private Document config;
    private ConfigurationParser parser;

    /**
     * map of workspace names and workspace configurations
     */
    private final Map wspConfigs;

    /**
     * repository home directory
     */
    private String repHomeDir;

    /**
     * virtual file system where the repository stores global state
     */
    private FileSystem repFS;

    /**
     * the name of the JAAS configuration app-entry for this repository 
     */
    private String appName;

    /**
     * workspaces config root directory (i.e. folder that contains
     * a subfolder with a workspace configuration file for every workspace
     * in the repository)
     */
    private String wspConfigRootDir;

    /**
     * name of default workspace
     */
    private String defaultWspName;

    /**
     * configuration for the access manager
     */
    private BeanConfig amConfig;

    /**
     * the versioning config
     */
    private VersioningConfig vConfig;

    public RepositoryConfig(
            Document config, ConfigurationParser parser,
            String home, String name, Map wspConfigs,
            FileSystem fs, String root, String defaultWspName,
            BeanConfig amc, VersioningConfig vc) {
        this.config = config;
        this.parser = parser;
        this.repHomeDir = home;
        this.appName = name;
        this.wspConfigs = wspConfigs;
        this.repFS = fs;
        this.wspConfigRootDir = root;
        this.defaultWspName = defaultWspName;
        this.amConfig = amc;
        this.vConfig = vc;
    }
    
    /**
     * Creates a new workspace configuration with the specified name.
     *
     * @param name workspace name
     * @return a new <code>WorkspaceConfig</code> object.
     * @throws RepositoryException if the specified name already exists or
     *                             if an error occured during the creation.
     */
    public synchronized WorkspaceConfig createWorkspaceConfig(String name)
            throws RepositoryException {
        if (wspConfigs.containsKey(name)) {
            String msg = "A workspace with the specified name alreay exists";
            throw new RepositoryException(msg);
        }
        WorkspaceConfig wspConfig =
            parser.createWorkspaceConfig(config, wspConfigRootDir, name);
        wspConfigs.put(name, wspConfig);
        return wspConfig;
    }

    /**
     * Returns the home directory of the repository.
     *
     * @return the home directory of the repository
     */
    public String getHomeDir() {
        return repHomeDir;
    }

    /**
     * Returns the virtual file system where the repository stores global state.
     *
     * @return the virtual file system where the repository stores global state
     */
    public FileSystem getFileSystem() {
        return repFS;
    }

    /**
     * Returns the name of the JAAS configuration app-entry for this repository.
     *
     * @return the name of the JAAS configuration app-entry for this repository
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Returns workspaces config root directory (i.e. the folder that contains
     * a subfolder with a workspace configuration file for every workspace
     * in the repository).
     *
     * @return the workspaces config root directory
     */
    public String getWorkspacesConfigRootDir() {
        return wspConfigRootDir;
    }

    /**
     * Returns the name of the default workspace.
     *
     * @return the name of the default workspace
     */
    public String getDefaultWorkspaceName() {
        return defaultWspName;
    }

    /**
     * Returns all workspace configurations.
     *
     * @return a collection of <code>WorkspaceConfig</code> objects.
     */
    public Collection getWorkspaceConfigs() {
        return wspConfigs.values();
    }

    /**
     * Returns the configuration of the specified workspace.
     *
     * @param name workspace name
     * @return a <code>WorkspaceConfig</code> object or <code>null</code>
     *         if no such workspace exists.
     */
    public WorkspaceConfig getWorkspaceConfig(String name) {
        return (WorkspaceConfig) wspConfigs.get(name);
    }

    /**
     * Returns the configuration for the versioning
     *
     * @return a <code>VersioningConfig</code> object
     */
    public VersioningConfig getVersioningConfig() {
        return vConfig;
    }

    /**
     * Returns the access manager configuration
     *
     * @return an <code>AccessManagerConfig</code> object
     */
    public AccessManagerConfig getAccessManagerConfig() {
        return new AccessManagerConfig(amConfig);
    }

}
