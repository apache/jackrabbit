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
 * Implements the search configuration.
 */
public class SearchConfig extends BeanConfig {

    /** The <code>FileSystem</code> for the search index. */
    private final FileSystem fs;

    public SearchConfig(
            String className, Properties properties, FileSystem fs) {
        super(className, properties);
        this.fs = fs;
    }

    /**
     * Returns <code>FileSystem</code> for search index persistence.
     * @return <code>FileSystem</code> for search index persistence.
     */
    public FileSystem getFileSystem() {
        return fs;
    }

    /**
     * Returns the name of the class implementing the <code>QueryHandler</code>
     * interface.
     * @return the name of the class implementing the <code>QueryHandler</code>
     *   interface.
     */
    public String getHandlerClassName() {
        return getClassName();
    }

}
