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
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.jdom.Element;

/**
 * This Class implements the configuration object for the versioning.
 */
public class VersioningConfig {

    /** attribute name of home dir */
    private static final String ROOTPATH_ATTRIB = "rootPath";

    /** the homedir for the versioning */
    private final File homeDir;

    /** The <code>FileSystem</code> for the versioing. */
    private final FileSystem fs;

    /** The <code>PersistenceManagerConfig</code> for the versioning */
    private final PersistenceManagerConfig pmConfig;

    /**
     * Creates a new <code>VersioningConfig</code>.
     * @param config the config root element for this <code>VersioningConfig</code>.
     * @param vars map of variable values.
     * @throws RepositoryException if an error occurs while creating the
     *  <code>SearchConfig</code>.
     */
    VersioningConfig(Element config, Map vars) throws RepositoryException {

        // home dir
        homeDir = new File(AbstractConfig.replaceVars(config.getAttributeValue(ROOTPATH_ATTRIB), vars));

        // create FileSystem
        Element fsElement = config.getChild(AbstractConfig.FILE_SYSTEM_ELEMENT);
        this.fs = AbstractConfig.createFileSystem(fsElement, vars);

        // persistence manager config
        Element pmElem = config.getChild(WorkspaceConfig.PERSISTENCE_MANAGER_ELEMENT);
        pmConfig = PersistenceManagerConfig.parse(pmElem, vars);
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
        return pmConfig;
    }

    public File getHomeDir() {
        return homeDir;
    }

}
