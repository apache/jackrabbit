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

import java.util.Properties;

import org.apache.jackrabbit.core.fs.FileSystem;

/**
 * Search index configuration. This bean configuration class
 * is used to create configured search index objects.
 * <p>
 * In addition to generic bean configuration information, this
 * class also contains a configured file system implementation
 * used by the search index.
 *
 * @see WorkspaceConfig#getSearchConfig()
 */
public class SearchConfig extends BeanConfig {

    /**
     * The search index file system configuration.
     */
    private final FileSystemConfig fsc;

    /**
     * Creates a search index configuration object.
     *
     * @param className search index implementation class
     * @param properties search index properties
     * @param fsc search index file system configuration
     */
    SearchConfig(
            String className, Properties properties, FileSystemConfig fsc) {
        super(className, properties);
        this.fsc = fsc;
    }

    /**
     * Initializes the search index file system.
     *
     * @throws ConfigurationException on file system configuration errors
     */
    public void init() throws ConfigurationException {
        fsc.init();
    }

    /**
     * Returns the search implementation class name.
     *
     * @return search implementation class name
     */
    public String getHandlerClassName() {
        return getClassName();
    }

    /**
     * Returns the search index file system.
     *
     * @return search index file system
     */
    public FileSystem getFileSystem() {
        return fsc.getFileSystem();
    }

}
