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

/**
 * Search index configuration. This bean configuration class
 * is used to create configured search index objects.
 * <p>
 * In addition to generic bean configuration information, this
 * class also contains an optional file system configuration
 * used by the search index.
 *
 * @see WorkspaceConfig#getSearchConfig()
 */
public class SearchConfig extends BeanConfig {

    /**
     * The search index file system configuration, or <code>null</code> if
     * none is provided.
     */
    private final FileSystemConfig fsc;

    /**
     * Creates a search index configuration object.
     *
     * @param className search index implementation class
     * @param properties search index properties
     * @param fsc search index file system configuration, or <code>null</code>
     *   if none is configured.
     */
    public SearchConfig(
            String className, Properties properties, FileSystemConfig fsc) {
        super(className, properties);
        this.fsc = fsc;
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
     * Returns the configuration for the <code>FileSystem</code> or
     * <code>null</code> if none is configured in this <code>SearchConfig</code>.
     *
     * @return the <code>FileSystemConfig</code> for this <code>SearchConfig</code>.
     */
    public FileSystemConfig getFileSystemConfig() {
        return fsc;
    }
}
