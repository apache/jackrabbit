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
package org.apache.jackrabbit.vfs.ext.ds;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Properties;

import javax.jcr.RepositoryException;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.util.DelegatingFileSystemOptionsBuilder;
import org.apache.jackrabbit.core.data.Backend;
import org.apache.jackrabbit.core.data.CachingDataStore;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Commons VFS based data store.
 */
public class VFSDataStore extends CachingDataStore {

    static final String BASE_FOLDER_URI = "baseFolderUri";

    static final String ASYNC_WRITE_POOL_SIZE = "asyncWritePoolSize";

    static final String FILE_SYSTEM_MANAGER_CLASS_NAME = "fileSystemManagerClassName";

    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(VFSDataStore.class);

    /**
     * Property key name for maximum backend connection. e.g. max total http connections.
     */
    static final String PROP_MAX_TOTAL_CONNECTIONS = "maxTotalConnections";

    /**
     * Property key name for maximum backend connection. e.g. max http connections per host.
     */
    static final String PROP_MAX_CONNECTIONS_PER_HOST = "maxConnectionsPerHost";

    /**
     * Default maximum backend connection. e.g. max http connection.
     */
    static final int DEFAULT_MAX_CONNECTION = 200;

    /**
     * Property key prefix for FielSystemOptions.
     */
    private static final String FILE_SYSTEM_OPTIONS_PROP_PREFIX = "fso.";

    /**
     * The class name of the VFS {@link FileSystemManager} instance used in this VFS data store.
     * If this is not set, then {@link StandardFileSystemManager} is used by default.
     */
    private String fileSystemManagerClassName;

    /**
     * The VFS {@link FileSystemManager} instance used in this VFS data store.
     * If {@link #fileSystemManagerClassName} is not set, then a {@link StandardFileSystemManager} instance is created by default.
     */
    private FileSystemManager fileSystemManager;

    /**
     * {@link FileSystemOptions} used when resolving the {@link #baseFolder}.
     */
    private FileSystemOptions fileSystemOptions;

    /**
     * Properties used when building a {@link FileSystemOptions} using {@link DelegatingFileSystemOptionsBuilder}.
     */
    private Properties fileSystemOptionsProperties;

    /**
     * The VFS base folder URI where all the entry files are maintained.
     */
    private String baseFolderUri;

    /**
     * The VFS base folder object where all the entry files are maintained.
     */
    private FileObject baseFolder;

    /**
     * The pool size of asynchronous write pooling executor.
     */
    private int asyncWritePoolSize = VFSBackend.DEFAULT_ASYNC_WRITE_POOL_SIZE;

    @Override
    public void init(String homeDir) throws RepositoryException {
        overridePropertiesFromConfig();

        if (baseFolderUri == null) {
            throw new RepositoryException("VFS base folder URI must be set.");
        }

        fileSystemManager = createFileSystemManager();

        FileName baseFolderName = null;

        try {
            baseFolderName = fileSystemManager.resolveURI(baseFolderUri);

            FileSystemOptions fso = getFileSystemOptions();

            if (fso != null) {
                baseFolder = fileSystemManager.resolveFile(baseFolderUri, fso);
            } else {
                baseFolder = fileSystemManager.resolveFile(baseFolderUri);
            }

            baseFolder.createFolder();
        } catch (FileSystemException e) {
            throw new RepositoryException("Could not initialize the VFS base folder at '"
                    + (baseFolderName == null ? "" : baseFolderName.getFriendlyURI()) + "'.", e);
        }

        super.init(homeDir);
    }

    @Override
    public void close() throws DataStoreException {
        VFSBackend backend = (VFSBackend) getBackend();

        try {
            // Let's wait for 5 minutes at max if there are still execution jobs in the async writing executor's queue.
            int seconds = 0;
            while (backend.getAsyncWriteExecutorActiveCount() > 0 && seconds++ < 300) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for the async write executor to complete.", e);
        }

        // Commenting out the following because the javadoc of FileSystemManager#closeFileSystem(FileSystem)
        // says it is dangerous when singleton instance is being used, which is the case as VFSDataStore keeps
        // single file system manager instance.
        // Also, VFS seems to remove the related provider component on that invocation.
//        if (fileSystemManager != null) {
//            fileSystemManager.closeFileSystem(baseFolder.getFileSystem());
//        }

        super.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Backend createBackend() {
        VFSBackend backend = new VFSBackend(baseFolder);
        backend.setAsyncWritePoolSize(getAsyncWritePoolSize());
        return backend;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getMarkerFile() {
        return "vfs.init.done";
    }

    /**
     * Returns the class name of the VFS {@link FileSystemManager} instance used in this VFS data store.
     * @return
     */
    public String getFileSystemManagerClassName() {
        return fileSystemManagerClassName;
    }

    /**
     * Sets the class name of the VFS {@link FileSystemManager} instance used in this VFS data store.
     * If this is not set, then {@link StandardFileSystemManager} is used by default.
     * @param fileSystemManagerClassName
     */
    public void setFileSystemManagerClassName(String fileSystemManagerClassName) {
        this.fileSystemManagerClassName = fileSystemManagerClassName;
    }

    /**
     * Returns the VFS {@link FileSystemManager} instance used in this VFS data store.
     * @return the VFS {@link FileSystemManager} instance used in this VFS data store
     */
    public FileSystemManager getFileSystemManager() {
        return fileSystemManager;
    }

    /**
     * Returns {@link FileSystemOptions} instance used when resolving the {@link #baseFolder}.
     * This may return null if {@link FileSystemOptions} instance was not injected or
     * a {@link #fileSystemOptionsProperties} instance cannot be injected or created.
     * Therefore, the caller should check whether or not this returns null.
     * When returning null, the caller may not use a {@link FileSystemOptions} instance.
     * @return {@link FileSystemOptions} instance used when resolving the {@link #baseFolder}
     */
    public FileSystemOptions getFileSystemOptions() throws RepositoryException {
        if (fileSystemOptions == null) {
            fileSystemOptions = createFileSystemOptions();
        }

        return fileSystemOptions;
    }

    /**
     * Sets the {@link FileSystemOptions} instance used when resolving the {@link #baseFolder}.
     * @param fileSystemOptions {@link FileSystemOptions} instance used when resolving the {@link #baseFolder}
     */
    public void setFileSystemOptions(FileSystemOptions fileSystemOptions) {
        this.fileSystemOptions = fileSystemOptions;
    }

    /**
     * Sets the properties used when building a {@link FileSystemOptions}, using {@link DelegatingFileSystemOptionsBuilder}.
     * @param fileSystemOptionsProperties properties used when building a {@link FileSystemOptions}
     */
    public void setFileSystemOptionsProperties(Properties fileSystemOptionsProperties) {
        this.fileSystemOptionsProperties = fileSystemOptionsProperties;
    }

    /**
     * Sets the properties in a semi-colon delimited string used when building a {@link FileSystemOptions},
     * using {@link DelegatingFileSystemOptionsBuilder}.
     * @param fileSystemOptionsPropertiesInString properties in String
     */
    public void setFileSystemOptionsPropertiesInString(String fileSystemOptionsPropertiesInString) {
        if (fileSystemOptionsPropertiesInString != null) {
            try {
                StringReader reader = new StringReader(fileSystemOptionsPropertiesInString);
                Properties props = new Properties();
                props.load(reader);
                fileSystemOptionsProperties = props;
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not load file system options properties.", e);
            }
        }
    }

    /**
     * Sets the base VFS folder URI.
     * @param baseFolderUri base VFS folder URI
     */
    public void setBaseFolderUri(String baseFolderUri) {
        this.baseFolderUri = baseFolderUri;
    }

    /**
     * Returns the pool size of the async write pool executor.
     * @return the pool size of the async write pool executor
     */
    public int getAsyncWritePoolSize() {
        return asyncWritePoolSize;
    }

    /**
     * Sets the pool size of the async write pool executor.
     * @param asyncWritePoolSize pool size of the async write pool executor
     */
    public void setAsyncWritePoolSize(int asyncWritePoolSize) {
        this.asyncWritePoolSize = asyncWritePoolSize;
    }

    /**
     * Creates a {@link FileSystemManager} instance.
     * @return a {@link FileSystemManager} instance.
     * @throws RepositoryException if an error occurs creating the manager.
     */
    protected FileSystemManager createFileSystemManager() throws RepositoryException {
        FileSystemManager fileSystemManager = null;

        try {
            if (getFileSystemManagerClassName() == null) {
                fileSystemManager = new StandardFileSystemManager();
            } else {
                final Class<?> mgrClass = Class.forName(getFileSystemManagerClassName());
                fileSystemManager = (FileSystemManager) mgrClass.newInstance();
            }

            if (fileSystemManager instanceof DefaultFileSystemManager) {
                ((DefaultFileSystemManager) fileSystemManager).init();
            }
        } catch (final FileSystemException e) {
            throw new RepositoryException(
                    "Could not initialize file system manager of class: " + getFileSystemManagerClassName(), e);
        } catch (final Exception e) {
            throw new RepositoryException(
                    "Could not create file system manager of class: " + getFileSystemManagerClassName(), e);
        }

        return fileSystemManager;
    }

    /**
     * Builds and returns {@link FileSystemOptions} instance which is used when resolving the {@link #baseFolder}
     * during the initialization.
     * If {@link #fileSystemOptionsProperties} is available, this scans all the property key names starting with {@link #FILE_SYSTEM_OPTIONS_PROP_PREFIX}
     * and uses the rest of the key name after the {@link #FILE_SYSTEM_OPTIONS_PROP_PREFIX} as the combination of scheme and property name
     * when building a {@link FileSystemOptions} using {@link DelegatingFileSystemOptionsBuilder}.
     * @return {@link FileSystemOptions} instance which is used when resolving the {@link #baseFolder} during the initialization
     * @throws RepositoryException if any file system exception occurs
     */
    protected FileSystemOptions createFileSystemOptions() throws RepositoryException {
        FileSystemOptions fso = null;

        if (fileSystemOptionsProperties != null) {
            try {
                fso = new FileSystemOptions();
                DelegatingFileSystemOptionsBuilder delegate = new DelegatingFileSystemOptionsBuilder(getFileSystemManager());

                String key;
                String schemeDotPropName;
                String scheme;
                String propName;
                String value;
                int offset;

                for (Enumeration<?> e = fileSystemOptionsProperties.propertyNames(); e.hasMoreElements(); ) {
                    key = (String) e.nextElement();

                    if (key.startsWith(FILE_SYSTEM_OPTIONS_PROP_PREFIX)) {
                        value = fileSystemOptionsProperties.getProperty(key);
                        schemeDotPropName = key.substring(FILE_SYSTEM_OPTIONS_PROP_PREFIX.length());
                        offset = schemeDotPropName.indexOf('.');

                        if (offset > 0) {
                            scheme = schemeDotPropName.substring(0, offset);
                            propName = schemeDotPropName.substring(offset + 1);
                            delegate.setConfigString(fso, scheme, propName, value);
                        } else {
                            LOG.warn("Ignoring an FileSystemOptions property in invalid format. Key: {}, Value: {}", key, value);
                        }
                    }
                }
            } catch (FileSystemException e) {
                throw new RepositoryException("Could not create File System Options.", e);
            }
        }

        return fso;
    }

    /**
     * Returns properties used when building a {@link FileSystemOptions} instance by the properties
     * during the initialization.
     * @return properties used when building a {@link FileSystemOptions} instance by the properties during the initialization
     */
    protected Properties getFileSystemOptionsProperties() {
        return fileSystemOptionsProperties;
    }

    private void overridePropertiesFromConfig() throws RepositoryException {
        final String config = getConfig();

        // If config param provided, then override properties from the config file.
        if (config != null && !"".equals(config)) {
            try {
                final Properties props = readConfig(config);

                String propValue = props.getProperty(ASYNC_WRITE_POOL_SIZE);
                if (propValue != null && !"".equals(propValue)) {
                    setAsyncWritePoolSize(Integer.parseInt(propValue));
                }

                propValue = props.getProperty(BASE_FOLDER_URI);
                if (propValue != null && !"".equals(propValue)) {
                    setBaseFolderUri(propValue);
                }

                propValue = props.getProperty(FILE_SYSTEM_MANAGER_CLASS_NAME);
                if (propValue != null && !"".equals(propValue)) {
                    setFileSystemManagerClassName(propValue);
                }

                final Properties fsoProps = new Properties();
                String propName;
                for (Enumeration<?> propNames = props.propertyNames(); propNames.hasMoreElements(); ) {
                    propName = (String) propNames.nextElement();
                    if (propName.startsWith(FILE_SYSTEM_OPTIONS_PROP_PREFIX)) {
                        fsoProps.setProperty(propName, props.getProperty(propName));
                    }
                }
                if (!fsoProps.isEmpty()) {
                    this.setFileSystemOptionsProperties(fsoProps);
                }
            } catch (IOException e) {
                throw new RepositoryException("Configuration file doesn't exist at '" + config + "'.");
            }
        }
    }

    private Properties readConfig(String fileName) throws IOException {
        if (!new File(fileName).exists()) {
            throw new IOException("Config file not found: " + fileName);
        }

        Properties prop = new Properties();
        InputStream in = null;

        try {
            in = new FileInputStream(fileName);
            prop.load(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return prop;
    }

}
