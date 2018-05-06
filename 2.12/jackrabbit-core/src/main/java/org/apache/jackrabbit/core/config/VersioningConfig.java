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

import java.io.File;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemFactory;
import org.apache.jackrabbit.core.state.ISMLocking;
import org.apache.jackrabbit.core.state.ISMLockingFactory;

/**
 * Versioning configuration. This configuration class is used to
 * create configured versioning objects.
 * <p>
 * The contained configuration information are: the home directory,
 * the file system implementation, and the persistence manager
 * implementation.
 *
 * @see RepositoryConfig#getVersioningConfig()
 */
public class VersioningConfig implements FileSystemFactory, ISMLockingFactory {

    /**
     * Versioning home directory.
     */
    private final String home;

    /**
     * Versioning file system factory.
     */
    private final FileSystemFactory fsf;

    /**
     * Versioning persistence manager configuration.
     */
    private final PersistenceManagerConfig pmc;

    /**
     * ISM locking factory
     */
    private final ISMLockingFactory ismLockingFactory;

    /**
     * Creates a versioning configuration object.
     *
     * @param home             home directory
     * @param fsf              file system factory
     * @param pmc              persistence manager configuration
     * @param ismLockingFactory the item state manager locking factory
     */
    public VersioningConfig(String home,
                            FileSystemFactory fsf,
                            PersistenceManagerConfig pmc,
                            ISMLockingFactory ismLockingFactory) {
        this.home = home;
        this.fsf = fsf;
        this.pmc = pmc;
        this.ismLockingFactory = ismLockingFactory;
    }

    /**
     * Returns the versioning home directory.
     *
     * @return versioning home directory
     */
    public File getHomeDir() {
        return new File(home);
    }

    /**
     * Creates and returns the configured versioning file system.
     *
     * @return the configured {@link FileSystem}
     * @throws RepositoryException if the file system can not be created
     */
    public FileSystem getFileSystem() throws RepositoryException {
        return fsf.getFileSystem();
    }

    /**
     * Returns the versioning persistence manager configuration.
     *
     * @return persistence manager configuration
     */
    public PersistenceManagerConfig getPersistenceManagerConfig() {
        return pmc;
    }

    /**
     * Creates and returns the configured versioning locking strategy.
     *
     * @return the configured {@link ISMLocking}
     * @throws RepositoryException if the locking strategy can not be created
     */
    public ISMLocking getISMLocking() throws RepositoryException {
        return ismLockingFactory.getISMLocking();
    }

}
