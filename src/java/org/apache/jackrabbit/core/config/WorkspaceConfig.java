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

import org.apache.jackrabbit.core.fs.FileSystem;

/**
 * A <code>WorkspaceConfig</code> ...
 */
public class WorkspaceConfig {

    /**
     * workspace home directory
     */
    private String wspHomeDir;

    /**
     * virtual file system where the workspace stores meta data etc.
     */
    private FileSystem wspFS;

    /**
     * workspace name
     */
    private String wspName;

    /**
     * configuration for the persistence manager
     */
    private BeanConfig pmc;

    /**
     * configuration for the search manager
     */
    private SearchConfig searchConfig;

    public WorkspaceConfig(String home, String name, FileSystem fs, BeanConfig pmc, SearchConfig sc) {
        this.wspHomeDir = home;
        this.wspName = name;
        this.wspFS = fs;
        this.pmc = pmc;
        this.searchConfig = sc;
    }

    /**
     * Returns the home directory of the workspace.
     *
     * @return the home directory of the workspace
     */
    public String getHomeDir() {
        return wspHomeDir;
    }

    /**
     * Returns the workspace name.
     *
     * @return the workspace name
     */
    public String getName() {
        return wspName;
    }

    /**
     * Returns the virtual file system where the workspace stores global state.
     *
     * @return the virtual file system where the workspace stores global state
     */
    public FileSystem getFileSystem() {
        return wspFS;
    }

    /**
     * Returns the configuration of the persistence manager.
     *
     * @return the <code>PersistenceManagerConfig</code> for this workspace
     */
    public PersistenceManagerConfig getPersistenceManagerConfig() {
        return new PersistenceManagerConfig(pmc);
    }

    /**
     * Returns the configuration of the search manager.
     * Returns <code>null</code> if no search manager is configured.
     *
     * @return the <code>SearchConfig</code> for this workspace
     */
    public SearchConfig getSearchConfig() {
        return searchConfig;
    }

}
