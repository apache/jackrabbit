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

import java.io.File;

import org.apache.jackrabbit.core.fs.FileSystem;

/**
 * This Class implements the configuration object for the versioning.
 */
public class VersioningConfig {

    /** the homedir for the versioning */
    private final File homeDir;

    /** The <code>FileSystem</code> for the versioing. */
    private final FileSystem fs;

    /** The <code>PersistenceManagerConfig</code> for the versioning */
    private final BeanConfig pmc;

    public VersioningConfig(File homeDir, FileSystem fs, BeanConfig pmc) {
        this.homeDir = homeDir;
        this.fs = fs;
        this.pmc = pmc;
    }

    /**
     * Returns the virtual file system where the workspace stores global state.
     *
     * @return the virtual file system where the workspace stores global state
     */
    public FileSystem getFileSystem() {
        return fs;
    }

    /**
     * Returns the configuration of the persistence manager.
     *
     * @return the <code>PersistenceManagerConfig</code> for this workspace
     */
    public PersistenceManagerConfig getPersistenceManagerConfig() {
        return new PersistenceManagerConfig(pmc);
    }

    public File getHomeDir() {
        return homeDir;
    }

}
