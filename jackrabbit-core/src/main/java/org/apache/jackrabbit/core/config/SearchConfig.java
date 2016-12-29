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

import java.util.Properties;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemFactory;

/**
 * Search index configuration. This bean configuration class
 * is used to create configured search index objects.
 * <p>
 * In addition to generic bean configuration information, this
 * class also contains an optional file system configuration
 * used by the search index.
 */
public class SearchConfig extends BeanConfig implements FileSystemFactory {

    /**
     * The (optional) factory for creating the configured search file system.
     */
    private final FileSystemFactory fsf;

    /**
     * Creates a search index configuration object.
     *
     * @param className search index implementation class
     * @param properties search index properties
     * @param fsf configured search index file system factory, or <code>null</code>
     */
    public SearchConfig(
            String className, Properties properties, FileSystemFactory fsf) {
        super(className, properties);
        this.fsf = fsf;
    }

    /**
     * Creates and returns the configured search file system, or returns
     * <code>null</code> if a search file system has not been configured.
     *
     * @return the configured {@link FileSystem}, or <code>null</code>
     * @throws RepositoryException if the file system can not be created
     */
    public FileSystem getFileSystem() throws RepositoryException {
        if (fsf != null) {
            return fsf.getFileSystem();
        } else {
            return null;
        }
    }

}
