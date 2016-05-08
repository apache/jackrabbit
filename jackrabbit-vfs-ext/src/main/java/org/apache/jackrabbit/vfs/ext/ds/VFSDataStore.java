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

import java.util.Enumeration;
import java.util.Properties;

import javax.jcr.RepositoryException;

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
        if (baseFolderUri == null) {
            throw new RepositoryException("VFS base folder URI must be set.");
        }

        fileSystemManager = createFileSystemManager();

        try {
            FileSystemOptions fso = createFileSystemOptions(fileSystemManager);

            if (fso != null) {
                baseFolder = fileSystemManager.resolveFile(baseFolderUri, fso);
            } else {
                baseFolder = fileSystemManager.resolveFile(baseFolderUri);
            }

            baseFolder.createFolder();
        } catch (FileSystemException e) {
            throw new RepositoryException("Could not initialize the VFS base folder at '" + baseFolderUri + "'.", e);
        }

        super.init(homeDir);
    }

    @Override
    public void close() throws DataStoreException {
        VFSBackend backend = (VFSBackend) getBackend();

        try {
            // Let's wait for 5 minutes at max if there are still execution jobs in the async writing executor's queue.
            int seconds = 0;
            while (backend.getAsyncWriteExecuter().getActiveCount() > 0 && seconds++ < 300) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for the async write executor to complete.", e);
        }

        // Commenting out the following because the javadoc of FileSystemManager#closeFileSystem(FileSystem)
        // says it is dangerous when singleton instance is being used, which is the case as VFSDataStore keeps
        // single file system manager instance.
        // Alos, VFS seems to remove the related provider component on that invocation.
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
     * Sets the properties used when building a {@link FileSystemOptions}, using {@link DelegatingFileSystemOptionsBuilder}.
     * @param fileSystemOptionsProperties properties used when building a {@link FileSystemOptions}
     */
    public void setFileSystemOptionsProperties(Properties fileSystemOptionsProperties) {
        this.fileSystemOptionsProperties = fileSystemOptionsProperties;
    }

    /**
     * Sets the properties in a semi-colon delimited string used when building a {@link FileSystemOptions},
     * using {@link DelegatingFileSystemOptionsBuilder}.
     * @param fileSystemOptionsProperties properties in a semi-colon delimited string used when building a {@link FileSystemOptions}
     */
    public void setFileSystemOptionsPropertiesInString(String fileSystemOptionsPropertiesInString) {
        fileSystemOptionsProperties = new Properties();

        if (fileSystemOptionsPropertiesInString != null) {
            String [] lines = fileSystemOptionsPropertiesInString.split(";");

            for (String line : lines) {
                String [] pair = line.split("=");

                if (pair.length == 2) {
                    String key = pair[0].trim();
                    String value = pair[1].trim();
                    fileSystemOptionsProperties.setProperty(key, value);
                }
            }
        }
    }

    /**
     * Returns the base VFS folder URI.
     * @return the base VFS folder URI
     */
    public String getBaseFolderUri() {
        return baseFolderUri;
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
     * @throws FileSystemException if an error occurs creating the manager.
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
     * Builds and returns {@link FileSystemOptions} instance which is used when resolving the {@link #vfsBaseFolder}
     * during the initialization.
     * @param fileSystemManager file system manager
     * @return {@link FileSystemOptions} instance which is used when resolving the {@link #vfsBaseFolder} during the initialization
     * @throws RepositoryException if any file system exception occurs
     */
    protected FileSystemOptions createFileSystemOptions(FileSystemManager fileSystemManager) throws RepositoryException {
        FileSystemOptions fso = null;

        if (fileSystemOptionsProperties != null) {
            try {
                fso = new FileSystemOptions();
                DelegatingFileSystemOptionsBuilder delegate = new DelegatingFileSystemOptionsBuilder(fileSystemManager);

                String key;
                String value;
                String scheme;
                String propName;
                int offset;

                for (Enumeration<?> e = fileSystemOptionsProperties.propertyNames(); e.hasMoreElements(); ) {
                    key = (String) e.nextElement();
                    value = fileSystemOptionsProperties.getProperty(key);
                    offset = key.indexOf('.');

                    if (offset > 0) {
                        scheme = key.substring(0, offset);
                        propName = key.substring(offset + 1);
                        delegate.setConfigString(fso, scheme, propName, value);
                    }
                }
            } catch (FileSystemException e) {
                throw new RepositoryException("Could not create File System Options.", e);
            }
        }

        return fso;
    }
}
