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

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;

/**
 * File system configuration. This bean configuration class
 * is used to create a configured file system object.
 */
public class FileSystemConfig extends BeanConfig {

    /**
     * Creates a file system configuration object.
     *
     * @param config file system implementation class configuration
     */
    public FileSystemConfig(BeanConfig config) {
        super(config);
    }

    /**
     * Instantiates and initializes the configured file system
     * implementation class.
     *
     * @return new initialized file system instance.
     * @throws ConfigurationException on file system initialization errors
     */
    public FileSystem createFileSystem() throws ConfigurationException {
        try {
            FileSystem fs = (FileSystem) newInstance();
            fs.init();
            return fs;
        } catch (ClassCastException e) {
            throw new ConfigurationException(
                    "Invalid file system implementation class "
                    + getClassName() + ".", e);
        } catch (FileSystemException e) {
            throw new ConfigurationException(
                    "File system initialization failure.", e);
        }
    }
}
