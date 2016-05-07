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
import java.io.OutputStream;
import java.net.URI;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.http.HttpFileSystemConfigBuilder;
import org.apache.jackrabbit.core.data.AsyncTouchCallback;
import org.apache.jackrabbit.core.data.AsyncTouchResult;
import org.apache.jackrabbit.core.data.AsyncUploadCallback;
import org.apache.jackrabbit.core.data.AsyncUploadResult;
import org.apache.jackrabbit.core.data.Backend;
import org.apache.jackrabbit.core.data.CachingDataStore;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.data.util.NamedThreadFactory;
import org.apache.jackrabbit.vfs.ext.VFSConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A data store backend that stores data on VFS file system.
 */
public class VFSBackend implements Backend {

    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(VFSBackend.class);

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
     * The maximum last modified time resolution of the file system.
     */
    private static final int ACCESS_TIME_RESOLUTION = 2000;

    /**
     * Touch file name suffix.
     * When {@link #isTouchFilePreferred()} returns true, this backend creates a separate file named by
     * the original file base name followed by this touch file name suffix.
     * So, this backend can set the last modified time on the separate touch file instead of trying to do it
     * on the original entry file.
     * For example, WebDAV file system doesn't allow to modify the last modified time on a file.
     */
    private static final String TOUCH_FILE_NAME_SUFFIX = ".touch";

    /**
     * {@link CachingDataStore} instance using this backend.
     */
    private CachingDataStore store;

    /**
     * Path of repository home directory.
     */
    private String homeDir;

    /**
     * Configuration file path for the <code>VFSBackend</code> specific configuration properties.
     */
    private String config;

    /**
     * <code>VFSBackend</code> specific configuration properties.
     */
    private Properties properties;

    /**
     * VFS base folder URI.
     */
    private String vfsBaseFolderUri;

    /**
     * VFS base folder object.
     */
    private FileObject vfsBaseFolder;

    /**
     * Asynchronous write pooling executor.
     */
    private ThreadPoolExecutor asyncWriteExecuter;

    /**
     * Whether or not a touch file is preferred to set/get the last modified timestamp for a file object
     * instead of setting/getting the last modified timestamp directly from the file object.
     */
    private boolean touchFilePreferred = true;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(CachingDataStore store, String homeDir, String config) throws DataStoreException {
        Properties initProps = null;
        // Check is configuration is already provided. That takes precedence
        // over config provided via file based config
        this.config = config;

        if (this.properties != null) {
            initProps = this.properties;
        } else {
            initProps = new Properties();
            InputStream in = null;

            try {
                in = new FileInputStream(config);
                initProps.load(in);
            } catch (IOException e) {
                throw new DataStoreException("Could not initialize VFSBackend from " + config, e);
            } finally {
                IOUtils.closeQuietly(in);
            }

            this.properties = initProps;
        }

        init(store, homeDir, initProps);
    }

    /**
     * This method initialize backend with the configuration as {@link java.util.Properties}.
     * 
     * @param store {@link CachingDataStore}
     * @param homeDir path of repository home dir.
     * @param prop configuration as {@link java.util.Properties}.
     * @throws DataStoreException if any file system exception occurs
     */
    public void init(CachingDataStore store, String homeDir, Properties prop) throws DataStoreException {
        this.store = store;
        this.homeDir = homeDir;

        vfsBaseFolderUri = prop.getProperty(VFSConstants.VFS_BASE_FOLDER_URI);

        if (vfsBaseFolderUri == null || "".equals(vfsBaseFolderUri)) {
            throw new DataStoreException("Could not initialize VFSBackend from " + config + ". ["
                    + VFSConstants.VFS_BASE_FOLDER_URI + "] property not found.");
        }

        FileSystemOptions opts = new FileSystemOptions();
        buildFileSystemOptions(opts, prop);

        try {
            vfsBaseFolder = getFileSystemManager().resolveFile(vfsBaseFolderUri, opts);
            vfsBaseFolder.createFolder();

            if ("file".equals(vfsBaseFolder.getName().getScheme())) {
                touchFilePreferred = false;
            }
        } catch (FileSystemException e) {
            throw new DataStoreException(
                    "Could not resolve or create vfs uri folder: " + vfsBaseFolder.getName().getURI(), e);
        }

        int asyncWritePoolSize = 10;
        String asyncWritePoolSizeStr = prop.getProperty(VFSConstants.ASYNC_WRITE_POOL_SIZE);

        if (asyncWritePoolSizeStr != null && !"".equals(asyncWritePoolSizeStr)) {
            asyncWritePoolSize = Integer.parseInt(asyncWritePoolSizeStr);
        }

        asyncWriteExecuter = createAsyncWriteExecuter(asyncWritePoolSize);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream read(DataIdentifier identifier) throws DataStoreException {
        FileObject fileObject = getExistingFileObject(identifier);

        if (fileObject == null) {
            throw new DataStoreException("Could not find file object for: " + identifier);
        }

        try {
            return new LazyFileContentInputStream(fileObject);
        } catch (FileSystemException e) {
            throw new DataStoreException("Could not get input stream from object: " + identifier, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLength(DataIdentifier identifier) throws DataStoreException {
        FileObject fileObject = getExistingFileObject(identifier);

        if (fileObject == null) {
            throw new DataStoreException("Could not find file object for: " + identifier);
        }

        try {
            return fileObject.getContent().getSize();
        } catch (FileSystemException e) {
            throw new DataStoreException("Could not get length from object: " + identifier, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastModified(DataIdentifier identifier) throws DataStoreException {
        FileObject fileObject = getExistingFileObject(identifier);

        if (fileObject == null) {
            throw new DataStoreException("Could not find file object for: " + identifier);
        }

        return getLastModifiedTime(fileObject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(DataIdentifier identifier, File file) throws DataStoreException {
        write(identifier, file, false, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeAsync(DataIdentifier identifier, File file, AsyncUploadCallback callback)
            throws DataStoreException {
        if (callback == null) {
            throw new IllegalArgumentException("callback parameter cannot be null in asyncUpload");
        }

        getAsyncWriteExecuter().execute(new AsyncUploadJob(identifier, file, callback));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<DataIdentifier> getAllIdentifiers() throws DataStoreException {
        List<DataIdentifier> identifiers = new LinkedList<DataIdentifier>();

        try {
            for (FileObject fileObject : VFSUtils.getChildFolders(getBaseFolderObject())) { // skip top-level files
                pushIdentifiersRecursively(identifiers, fileObject);
            }
        } catch (FileSystemException e) {
            throw new DataStoreException("Object identifiers not resolved.", e);
        }

        LOG.debug("Found " + identifiers.size() + " identifiers.");

        return identifiers.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(DataIdentifier identifier, boolean touch) throws DataStoreException {
        FileObject fileObject = getExistingFileObject(identifier);

        if (fileObject == null) {
            return false;
        }

        if (touch) {
            touch(identifier, System.currentTimeMillis(), false, null);
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(DataIdentifier identifier) throws DataStoreException {
        return exists(identifier, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void touch(DataIdentifier identifier, long minModifiedDate) throws DataStoreException {
        touch(identifier, minModifiedDate, false, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void touchAsync(DataIdentifier identifier, long minModifiedDate, AsyncTouchCallback callback)
            throws DataStoreException {
        if (callback == null) {
            throw new IllegalArgumentException("callback parameter cannot be null in touchAsync");
        }

        getAsyncWriteExecuter().execute(new AsyncTouchJob(identifier, minModifiedDate, callback));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws DataStoreException {
        getAsyncWriteExecuter().shutdownNow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<DataIdentifier> deleteAllOlderThan(long timestamp) throws DataStoreException {
        Set<DataIdentifier> deleteIdSet = new HashSet<DataIdentifier>(30);

        try {
            for (FileObject folderObject : VFSUtils.getChildFolders(getBaseFolderObject())) {
                deleteOlderRecursive(deleteIdSet, folderObject, timestamp);
            }
        } catch (FileSystemException e) {
            throw new DataStoreException("Object deletion aborted.", e);
        }

        return deleteIdSet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteRecord(DataIdentifier identifier) throws DataStoreException {
        FileObject fileObject = getExistingFileObject(identifier);

        if (fileObject != null) {
            deleteRecordFileObject(fileObject);
            deleteEmptyParentFolders(fileObject);
        }
    }

    /**
     * Properties used to configure the backend.
     * If provided explicitly before init is invoked then these take precedence.
     * @param properties to configure Backend
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Returns true if a touch file should be used to save/get the last modified time for a file object.
     * True by default unless the {@link #getBaseFolderObject()} is representing a local file system folder (e.g, file://...).
     * <P>
     * When returns true, this backend creates a separate file named by the original file base name followed
     * by this touch file name suffix. So, this backend can set the last modified time on the separate touch file
     * instead of trying to do it on the original entry file.
     * For example, WebDAV file system doesn't allow to modify the last modified time on a file.
     * </P>
     * @return true if a touch file should be used to save/get the last modified time for a file object
     */
    public boolean isTouchFilePreferred() {
        return touchFilePreferred;
    }

    /**
     * Sets whether or not a touch file should be used to save/get the last modified timestamp for a file object.
     * @param touchFilePreferred whether or not a touch file should be used to save/get the last modified timestamp for a file object
     */
    public void setTouchFilePreferred(boolean touchFilePreferred) {
        this.touchFilePreferred = touchFilePreferred;
    }

    /**
     * Returns {@link FileSystemManager} instance to use in this backend implementation.
     * @return {@link FileSystemManager} instance to use in this backend implementation
     * @throws DataStoreException if any file system exception occurs
     */
    protected FileSystemManager getFileSystemManager() throws DataStoreException {
        try {
            FileSystemManager fileSystemManager = VFS.getManager();

            if (fileSystemManager == null) {
                throw new DataStoreException("Could not get the default VFS manager.");
            }

            return fileSystemManager;
        } catch (FileSystemException e) {
            throw new DataStoreException("Could not get VFS manager.", e);
        }
    }

    /**
     * Builds {@link FileSystemOptions} instance by reading {@code props}
     * to use when resolving the {@link #vfsBaseFolder} during the initialization.
     * @param opts {@link FileSystemOptions} instance
     * @param props VFS backend configuration properties
     */
    protected void buildFileSystemOptions(FileSystemOptions opts, Properties props) {
        String baseUriProp = props.getProperty(VFSConstants.VFS_BASE_FOLDER_URI);
        URI baseUri = URI.create(baseUriProp);
        String scheme = baseUri.getScheme();

        if ("http".equals(scheme) || "https".equals(scheme) || "webdav".equals(scheme)) {
            HttpFileSystemConfigBuilder builder = HttpFileSystemConfigBuilder.getInstance();
            builder.setMaxTotalConnections(opts,
                    getIntProperty(props, PROP_MAX_TOTAL_CONNECTIONS, DEFAULT_MAX_CONNECTION));
            builder.setMaxConnectionsPerHost(opts,
                    getIntProperty(props, PROP_MAX_CONNECTIONS_PER_HOST, DEFAULT_MAX_CONNECTION));
        }
    }

    /**
     * Returns the VFS base folder object.
     * @return the VFS base folder object
     */
    protected FileObject getBaseFolderObject() {
        return vfsBaseFolder;
    }

    /**
     * Returns a resolved identified file object. This method implements the pattern
     * used to avoid problems with too many files in a single folder.
     *
     * @param identifier data identifier
     * @return identified file object
     * @throws FileSystemException if VFS file system exception occurs
     * @throws DataStoreException if any file system exception occurs
     */
    protected FileObject resolveFileObject(DataIdentifier identifier) throws DataStoreException {
        try {
            String relPath = resolveFileObjectRelPath(identifier);
            return getBaseFolderObject().resolveFile(relPath);
        } catch (FileSystemException e) {
            throw new DataStoreException("File object not resolved: " + identifier, e);
        }
    }

    /**
     * Returns a resolved relative file object path by the given entry identifier.
     * @param identifier entry identifier
     * @return a resolved relative file object path by the given entry identifier
     */
    protected String resolveFileObjectRelPath(DataIdentifier identifier) {
        String idString = identifier.toString();
        StringBuilder sb = new StringBuilder(80);
        sb.append(idString.substring(0, 2)).append('/');
        sb.append(idString.substring(2, 4)).append('/');
        sb.append(idString.substring(4, 6)).append('/');
        sb.append(idString.substring(6, 8)).append('/');
        sb.append(idString);
        return sb.toString();
    }

    /**
     * Returns the identified file object. If not existing, returns null.
     *
     * @param identifier data identifier
     * @return identified file object
     * @throws FileSystemException if any file system exception occurs
     * @throws DataStoreException if any file system exception occurs
     */
    protected FileObject getExistingFileObject(DataIdentifier identifier) throws DataStoreException {
        String relPath = resolveFileObjectRelPath(identifier);
        String [] segments = relPath.split("/");

        FileObject tempFileObject = getBaseFolderObject();

        try {
            for (int i = 0; i < segments.length; i++) {
                tempFileObject = tempFileObject.getChild(segments[i]);

                if (tempFileObject == null) {
                    return null;
                }
            }

            return tempFileObject;
        } catch (FileSystemException e) {
            throw new DataStoreException("File object not resolved: " + identifier, e);
        }
    }

    /**
     * Returns true if the fileObject is used for touching purpose.
     *
     * @param fileObject file object
     * @return true if the fileObject is used for touching purpose
     */
    protected boolean isTouchFileObject(FileObject fileObject) {
        if (fileObject.getName().getBaseName().endsWith(TOUCH_FILE_NAME_SUFFIX)) {
            return true;
        }

        return false;
    }

    /**
     * Returns the touch file for the fileObject.
     *
     * @param fileObject file object
     * @param create create a touch file if not existing
     * @return touch file object
     * @throws DataStoreException if any file system exception occurs
     */
    protected FileObject getTouchFileObject(FileObject fileObject, boolean create) throws DataStoreException {
        try {
            FileObject folderObject = fileObject.getParent();
            String touchFileName = fileObject.getName().getBaseName() + TOUCH_FILE_NAME_SUFFIX;
            FileObject touchFileObject = folderObject.getChild(touchFileName);

            if (touchFileObject == null && create) {
                touchFileObject = folderObject.resolveFile(touchFileName);
                touchFileObject.createFile();
                touchFileObject = folderObject.getChild(touchFileName);
            }

            return touchFileObject;
        } catch (FileSystemException e) {
            throw new DataStoreException("Touch file object not resolved: " + fileObject.getName().getURI(), e);
        }
    }

    /**
     * Creates a {@link ThreadPoolExecutor}.
     * This method is invoked during the initialization for asynchronous write/touch job executions.
     * @param workerCount thread pool count
     * @return a {@link ThreadPoolExecutor}
     */
    protected ThreadPoolExecutor createAsyncWriteExecuter(int workerCount) {
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(workerCount,
                new NamedThreadFactory("vfs-write-worker"));
    }

    /**
     * Returns ThreadPoolExecutor used to execute asynchronous write or touch jobs.
     * @return ThreadPoolExecutor used to execute asynchronous write or touch jobs
     */
    protected ThreadPoolExecutor getAsyncWriteExecuter() {
        return asyncWriteExecuter;
    }

    /**
     * Copy the content of the local file ({@code srcFile}) to the record identified by the {@code identifier}.
     * @param srcFile source local file
     * @param identifier record identifier
     * @throws IOException if any IO exception occurs
     * @throws DataStoreException if any file system exception occurs
     */
    private void copyFileContentToRecord(File srcFile, DataIdentifier identifier) throws IOException, DataStoreException {
        String relPath = resolveFileObjectRelPath(identifier);
        String [] segments = relPath.split("/");

        InputStream input = null;
        OutputStream output = null;

        try {
            FileObject baseFolderObject = getBaseFolderObject();
            FileObject folderObject = null;

            for (int i = 0; i < segments.length - 1; i++) {
                folderObject = VFSUtils.createChildFolder(baseFolderObject, segments[i]);
                baseFolderObject = folderObject;
            }

            FileObject destFileObject = VFSUtils.createChildFile(folderObject, segments[segments.length - 1]);
            input = new FileInputStream(srcFile);
            output = destFileObject.getContent().getOutputStream();
            IOUtils.copy(input, output);
        } finally {
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(input);
        }
    }

    /**
     * Set the last modified time of a fileObject, if the fileObject is writable.
     * @param fileObject the file object
     * @param time the new last modified date
     * @throws DataStoreException if the fileObject is writable but modifying the date fails
     */
    private void updateLastModifiedTime(FileObject fileObject) throws DataStoreException {
        try {
            if (isTouchFilePreferred()) {
                getTouchFileObject(fileObject, true);
            } else {
                long time = System.currentTimeMillis() + ACCESS_TIME_RESOLUTION;
                fileObject.getContent().setLastModifiedTime(time);
            }
        } catch (FileSystemException e) {
            throw new DataStoreException("An IO Exception occurred while trying to set the last modified date: "
                    + fileObject.getName().getURI(), e);
        }
    }

    /**
     * Get the last modified time of a file object.
     * @param file the file object
     * @return the last modified date
     * @throws DataStoreException if reading fails
     */
    private long getLastModifiedTime(FileObject fileObject) throws DataStoreException {
        long lastModified = 0;

        try {
            if (isTouchFilePreferred()) {
                FileObject touchFile = getTouchFileObject(fileObject, false);

                if (touchFile != null) {
                    lastModified = touchFile.getContent().getLastModifiedTime();
                } else {
                    lastModified = fileObject.getContent().getLastModifiedTime();
                }
            } else {
                lastModified = fileObject.getContent().getLastModifiedTime();
            }

            if (lastModified == 0) {
                throw new DataStoreException("Failed to read record modified date: " + fileObject.getName().getURI());
            }
        } catch (FileSystemException e) {
            throw new DataStoreException("Failed to read record modified date: " + fileObject.getName().getURI());
        }

        return lastModified;
    }

    /**
     * Scans {@code folderObject} and all the descendant folders to find record entries and push the record entry
     * identifiers to {@code identifiers}.
     * @param identifiers identifier list
     * @param folderObject folder object
     * @throws FileSystemException if any file system exception occurs
     * @throws DataStoreException if any file system exception occurs
     */
    private void pushIdentifiersRecursively(List<DataIdentifier> identifiers, FileObject folderObject)
            throws FileSystemException, DataStoreException {
        FileType type;

        for (FileObject fileObject : VFSUtils.getChildFileOrFolders(folderObject)) {
            type = fileObject.getType();

            if (type == FileType.FOLDER) {
                pushIdentifiersRecursively(identifiers, fileObject);
            } else if (type == FileType.FILE) {
                if (!isTouchFileObject(fileObject)) {
                    identifiers.add(new DataIdentifier(fileObject.getName().getBaseName()));
                }
            }
        }
    }

    /**
     * Writes {@code file}'s content to the record entry identified by {@code identifier}.
     * @param identifier record identifier
     * @param file local file to copy from
     * @param asyncUpload whether or not it should be done asynchronously
     * @param callback asynchronous uploading callback instance
     * @throws DataStoreException if any file system exception occurs
     */
    private void write(DataIdentifier identifier, File file, boolean asyncUpload, AsyncUploadCallback callback)
            throws DataStoreException {
        AsyncUploadResult asyncUpRes = null;

        if (asyncUpload) {
            asyncUpRes = new AsyncUploadResult(identifier, file);
        }

        synchronized (this) {
            FileObject fileObject = getExistingFileObject(identifier);
            FileObject resolvedFileObject = resolveFileObject(identifier);

            try {
                if (fileObject != null) {
                    updateLastModifiedTime(resolvedFileObject);
                } else {
                    copyFileContentToRecord(file, identifier);
                }

                if (asyncUpRes != null && callback != null) {
                    callback.onSuccess(asyncUpRes);
                }
            } catch (IOException e) {
                DataStoreException e2 = new DataStoreException(
                        "Could not get output stream to object: " + resolvedFileObject.getName().getURI(), e);

                if (asyncUpRes != null && callback != null) {
                    asyncUpRes.setException(e2);
                    callback.onFailure(asyncUpRes);
                }

                throw e2;
            }
        }
    }

    /**
     * Touches the object entry file identified by {@code identifier}.
     * @param identifier record identifier
     * @param minModifiedDate minimum modified date time to be used in touching
     * @param asyncTouch whether or not it should be done asynchronously
     * @param callback asynchrounous touching callback instance
     * @throws DataStoreException if any file system exception occurs
     */
    private void touch(DataIdentifier identifier, long minModifiedDate, boolean asyncTouch, AsyncTouchCallback callback)
            throws DataStoreException {
        AsyncTouchResult asyncTouchRes = null;

        if (asyncTouch) {
            asyncTouchRes = new AsyncTouchResult(identifier);
        }

        try {
            FileObject fileObject = getExistingFileObject(identifier);

            if (fileObject != null) {
                if (minModifiedDate > 0 && minModifiedDate > getLastModifiedTime(fileObject)) {
                    updateLastModifiedTime(fileObject);
                }
            } else {
                LOG.warn("File doesn't exist for the identifier: {}.", identifier);
            }
        } catch (DataStoreException e) {
            if (asyncTouchRes != null) {
                asyncTouchRes.setException(e);
            }

            throw e;
        } finally {
            if (asyncTouchRes != null && callback != null) {
                if (asyncTouchRes.getException() != null) {
                    callback.onFailure(asyncTouchRes);
                } else {
                    callback.onSuccess(asyncTouchRes);
                }
            }
        }
    }

    /**
     * Deletes record file object.
     * @param fileObject file object to delete
     * @return true if deleted
     * @throws DataStoreException if any file system exception occurs
     */
    private boolean deleteRecordFileObject(FileObject fileObject) throws DataStoreException {
        if (isTouchFilePreferred()) {
            try {
                FileObject touchFile = getTouchFileObject(fileObject, false);

                if (touchFile != null) {
                    touchFile.delete();
                }
            } catch (FileSystemException e) {
                LOG.warn("Could not delete touch file for " + fileObject.getName().getURI(), e);
            }
        }

        try {
            return fileObject.delete();
        } catch (FileSystemException e) {
            throw new DataStoreException("Could not delete record file at " + fileObject.getName().getURI(), e);
        }
    }

    /**
     * Deletes the parent folders of {@code fileObject} if a parent folder is empty.
     * @param fileObject fileObject to start with
     * @throws DataStoreException if any file system exception occurs
     */
    private void deleteEmptyParentFolders(FileObject fileObject) throws DataStoreException {
        try {
            String baseFolderUri = getBaseFolderObject().getName().getURI() + "/";
            FileObject parentFolder = fileObject.getParent();

            // Only iterate & delete if parent folder of the blob file is
            // child of the base directory and if it is empty
            while (parentFolder.getName().getURI().startsWith(baseFolderUri)) {
                if (VFSUtils.hasAnyChildFileOrFolder(parentFolder)) {
                    break;
                }

                boolean deleted = parentFolder.delete();
                LOG.debug("Deleted parent folder [{}] of file [{}]: {}",
                        new Object[] { parentFolder, fileObject.getName().getURI(), deleted });
                parentFolder = parentFolder.getParent();
            }
        } catch (IOException e) {
            LOG.warn("Error in parents deletion for " + fileObject.getName().getURI(), e);
        }
    }

    /**
     * Deletes any descendant record files under {@code folderObject} if the record files are older than {@code timestamp},
     * and push all the deleted record identifiers into {@code deleteIdSet}.
     * @param deleteIdSet set to store all the deleted record identifiers
     * @param folderObject folder object to start with
     * @param timestamp timestamp
     * @throws FileSystemException if any file system exception occurs
     * @throws DataStoreException if any file system exception occurs
     */
    private void deleteOlderRecursive(Set<DataIdentifier> deleteIdSet, FileObject folderObject, long timestamp)
            throws FileSystemException, DataStoreException {
        FileType type;
        DataIdentifier identifier;

        for (FileObject fileObject : VFSUtils.getChildFileOrFolders(folderObject)) {
            type = fileObject.getType();

            if (type == FileType.FOLDER) {
                deleteOlderRecursive(deleteIdSet, fileObject, timestamp);

                synchronized (this) {
                    if (!VFSUtils.hasAnyChildFileOrFolder(fileObject)) {
                        fileObject.delete();
                    }
                }

            } else if (type == FileType.FILE) {
                long lastModified = getLastModifiedTime(fileObject);

                if (lastModified < timestamp) {
                    identifier = new DataIdentifier(fileObject.getName().getBaseName());

                    if (store.confirmDelete(identifier)) {
                        store.deleteFromCache(identifier);

                        if (LOG.isInfoEnabled()) {
                            LOG.info("Deleting old file " + fileObject.getName().getURI() + " modified: "
                                    + new Timestamp(lastModified).toString() + " length: "
                                    + fileObject.getContent().getSize());
                        }

                        if (deleteRecordFileObject(fileObject)) {
                            deleteIdSet.add(identifier);
                        } else {
                            LOG.warn("Failed to delete old file " + fileObject.getName().getURI());
                        }
                    }
                }
            }
        }
    }

    /**
     * This class implements {@link Runnable} interface to copy {@link File} to VFS file object asynchronously.
     */
    private class AsyncUploadJob implements Runnable {

        private DataIdentifier identifier;

        private File file;

        private AsyncUploadCallback callback;

        public AsyncUploadJob(DataIdentifier identifier, File file, AsyncUploadCallback callback) {
            super();
            this.identifier = identifier;
            this.file = file;
            this.callback = callback;
        }

        public void run() {
            try {
                write(identifier, file, true, callback);
            } catch (DataStoreException e) {
                LOG.error("Could not upload [" + identifier + "], file[" + file + "]", e);
            }
        }
    }

    /**
     * This class implements {@link Runnable} interface to touch a VFS file object asynchronously.
     */
    private class AsyncTouchJob implements Runnable {

        private DataIdentifier identifier;

        private long minModifiedDate;

        private AsyncTouchCallback callback;

        public AsyncTouchJob(DataIdentifier identifier, long minModifiedDate, AsyncTouchCallback callback) {
            super();
            this.identifier = identifier;
            this.minModifiedDate = minModifiedDate;
            this.callback = callback;
        }

        public void run() {
            try {
                touch(identifier, minModifiedDate, true, callback);
            } catch (DataStoreException e) {
                LOG.error("Could not touch [" + identifier + "]", e);
            }
        }
    }

    private int getIntProperty(Properties props, String key, int defaultValue) {
        try {
            String value = props.getProperty(key);

            if (value != null) {
                value = value.trim();

                if (!"".equals(value)) {
                    return Integer.parseInt(value);
                }
            }
        } catch (NumberFormatException ignore) {
        }

        return defaultValue;
    }
}
