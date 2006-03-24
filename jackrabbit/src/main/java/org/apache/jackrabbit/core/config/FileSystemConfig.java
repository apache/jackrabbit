/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
import org.apache.jackrabbit.core.fs.FileSystemException;

/**
 * File system configuration. This bean configuration class
 * is used to create a configured file system object. The file system
 * is instantiated by the {@link #init() init()} method, and accessible
 * using the {@link #getFileSystem() getFileSystem()} method. Calling
 * {@link #dispose() dispose()} will close and dispose a file system instance
 * previously created by the {@link #init() init()} method.
 *
 */
public class FileSystemConfig extends BeanConfig {

    /** The initialized file system implementation. */
    private FileSystem fs;

    /**
     * Creates a file system configuration object.
     *
     * @param config file system implementation class configuration
     */
    public FileSystemConfig(BeanConfig config) {
        super(config);
        fs = null;
    }

    /**
     * Instantiates and initializes the configured file system
     * implementation class.
     *
     * @throws ConfigurationException on file system initialization errors
     * @throws IllegalStateException if the file system has already been
     *                               initialized
     */
    public void init() throws ConfigurationException, IllegalStateException {
        if (fs == null) {
            try {
                fs = (FileSystem) newInstance();
                fs.init();
            } catch (ClassCastException e) {
                throw new ConfigurationException(
                        "Invalid file system implementation class "
                        + getClassName() + ".", e);
            } catch (FileSystemException e) {
                throw new ConfigurationException(
                        "File system initialization failure.", e);
            }
        } else {
            throw new IllegalStateException(
            "File system has already been initialized.");
        }
    }

    /**
     * Closes and disposes a file system instance previously created by the
     * {@link #init() init()} method, i.e. resets this instance to the
     * <i>uninitialized</i> state.
     */
    public void dispose() {
        if (fs != null) {
            try {
                fs.close();
            } catch (FileSystemException fse) {
                // ignore...
            }
            fs = null;
        } else {
            throw new IllegalStateException("File system has not been initialized.");
        }
    }

    /**
     * Returns the configured file system. The {@link #init() init()} method
     * must have been called before this method can be invoked.
     *
     * @return configured file system
     * @throws IllegalStateException if the file system has not been initialized
     */
    public FileSystem getFileSystem() throws IllegalStateException {
        if (fs != null) {
            return fs;
        } else {
            throw new IllegalStateException(
                    "File system has not been initialized.");
        }
    }
}
