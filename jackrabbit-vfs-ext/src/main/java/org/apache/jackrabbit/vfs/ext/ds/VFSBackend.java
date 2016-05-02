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
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
import org.apache.jackrabbit.core.data.AsyncTouchCallback;
import org.apache.jackrabbit.core.data.AsyncTouchResult;
import org.apache.jackrabbit.core.data.AsyncUploadCallback;
import org.apache.jackrabbit.core.data.AsyncUploadResult;
import org.apache.jackrabbit.core.data.Backend;
import org.apache.jackrabbit.core.data.CachingDataStore;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.data.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A data store backend that stores data on VFS file system.
 */
public class VFSBackend implements Backend {

    private static final Logger LOG = LoggerFactory.getLogger(VFSBackend.class);

    public static final String VFS_BACKEND_URI = "vfsBackendUri";

    public static final String ASYNC_WRITE_POOL_SIZE = "asyncWritePoolSize";

    /**
     * The maximum last modified time resolution of the file system.
     */
    private static final int ACCESS_TIME_RESOLUTION = 2000;

    private CachingDataStore store;

    private String homeDir;

    private Properties properties;

    private String config;

    private String vfsUri;

    private volatile FileObject vfsUriFolder;

    private ThreadPoolExecutor asyncWriteExecuter;

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
                throw new DataStoreException(
                    "Could not initialize VFSBackend from " + config, e);
            } finally {
                IOUtils.closeQuietly(in);
            }
            this.properties = initProps;
        }
        init(store, homeDir, initProps);
    }

    public void init(CachingDataStore store, String homeDir, Properties prop) throws DataStoreException {
        this.store = store;
        this.homeDir = homeDir;
        vfsUri = prop.getProperty(VFS_BACKEND_URI);

        if (vfsUri == null || "".equals(vfsUri)) {
            throw new DataStoreException("Could not initialize VFSBackend from "
                + config + ". [" + VFS_BACKEND_URI + "] property not found.");
        }

        try {
            vfsUriFolder = getFileSystemManager().resolveFile(vfsUri);

            if (vfsUriFolder.exists() && vfsUriFolder.getType() != FileType.FOLDER) {
                throw new DataStoreException("Cannot create a folder "
                    + "because a file exists with the same name: " + vfsUri);
            }

            if (!vfsUriFolder.exists()) {
                vfsUriFolder.createFolder();
            }
        } catch (FileSystemException e) {
            throw new DataStoreException("Could not resolve or create vfs uri folder: "
                    + vfsUriFolder.getName().getPath(), e);
        }

        int asyncWritePoolSize = 10;
        String asyncWritePoolSizeStr = prop.getProperty(ASYNC_WRITE_POOL_SIZE);
        if (asyncWritePoolSizeStr != null) {
            asyncWritePoolSize = Integer.parseInt(asyncWritePoolSizeStr);
        }
        asyncWriteExecuter = (ThreadPoolExecutor) Executors.newFixedThreadPool(
                asyncWritePoolSize, new NamedThreadFactory("vfs-write-worker"));
    }

    @Override
    public InputStream read(DataIdentifier identifier) throws DataStoreException {
        FileObject fileObject = getExistingFileObject(identifier);

        try {
            return new LazyFileContentInputStream(fileObject);
        } catch (FileSystemException e) {
            throw new DataStoreException("Could not get input stream from object: " + identifier, e);
        }
    }

    @Override
    public long getLength(DataIdentifier identifier) throws DataStoreException {
        FileObject fileObject = getExistingFileObject(identifier);

        try {
            return fileObject.getContent().getSize();
        } catch (FileSystemException e) {
            throw new DataStoreException("Could not get length from object: " + identifier, e);
        }
    }

    @Override
    public long getLastModified(DataIdentifier identifier) throws DataStoreException {
        FileObject fileObject = getExistingFileObject(identifier);
        return getLastModified(fileObject);
    }

    @Override
    public void write(DataIdentifier identifier, File file) throws DataStoreException {
        write(identifier, file, false, null);
    }

    @Override
    public void writeAsync(DataIdentifier identifier, File file, AsyncUploadCallback callback)
            throws DataStoreException {
        if (callback == null) {
            throw new IllegalArgumentException("callback parameter cannot be null in asyncUpload");
        }

        asyncWriteExecuter.execute(new AsyncUploadJob(identifier, file, callback));
    }

    @Override
    public Iterator<DataIdentifier> getAllIdentifiers() throws DataStoreException {
        List<DataIdentifier> identifiers = new LinkedList<DataIdentifier>();

        try {
            for (FileObject fileObject : vfsUriFolder.getChildren()) {
                if (!fileObject.exists()) {
                    continue;
                }

                if (fileObject.getType() == FileType.FOLDER) { // skip top-level files
                    pushIdentifiersRecursively(identifiers, fileObject);
                }
            }
        } catch (FileSystemException e) {
            throw new DataStoreException("Object identifiers not resolved.", e);
        }

        LOG.debug("Found " + identifiers.size() + " identifiers.");

        return identifiers.iterator();
    }

    @Override
    public boolean exists(DataIdentifier identifier, boolean touch) throws DataStoreException {
        FileObject fileObject = getFileObject(identifier);

        try {
            if (fileObject.exists() && fileObject.getType() == FileType.FILE) {
                if (touch) {
                    touch(identifier, System.currentTimeMillis(), false, null);
                }
                return true;
            } else {
                return false;
            }
        } catch (FileSystemException e) {
            throw new DataStoreException("Object not resolved: " + identifier, e);
        }
    }

    @Override
    public boolean exists(DataIdentifier identifier) throws DataStoreException {
        return exists(identifier, false);
    }

    @Override
    public void touch(DataIdentifier identifier, long minModifiedDate) throws DataStoreException {
        touch(identifier, minModifiedDate, false, null);
    }

    @Override
    public void touchAsync(DataIdentifier identifier, long minModifiedDate, AsyncTouchCallback callback)
            throws DataStoreException {
        if (callback == null) {
            throw new IllegalArgumentException("callback parameter cannot be null in touchAsync");
        }

        asyncWriteExecuter.execute(new AsyncTouchJob(identifier, minModifiedDate, callback));
    }

    @Override
    public void close() throws DataStoreException {
        asyncWriteExecuter.shutdownNow();
        getFileSystemManager().closeFileSystem(vfsUriFolder.getFileSystem());
    }

    @Override
    public Set<DataIdentifier> deleteAllOlderThan(long timestamp) throws DataStoreException {
        Set<DataIdentifier> deleteIdSet = new HashSet<DataIdentifier>(30);

        try {
            for (FileObject fileObject : vfsUriFolder.getChildren()) {
                if (!fileObject.exists()) {
                    continue;
                }

                if (fileObject.getType() == FileType.FOLDER) { // skip top-level files
                    deleteOlderRecursive(deleteIdSet, fileObject, timestamp);
                }
            }
        } catch (FileSystemException e) {
            throw new DataStoreException("Object deletion aborted.", e);
        }

        return deleteIdSet;
    }

    @Override
    public void deleteRecord(DataIdentifier identifier) throws DataStoreException {
        FileObject fileObject = getFileObject(identifier);

        try {
            if (fileObject.exists() && fileObject.getType() == FileType.FILE) {
                fileObject.delete();
                deleteEmptyParentDirs(fileObject);
            }
        } catch (FileSystemException e) {
            throw new DataStoreException("Object not deleted: " + identifier, e);
        }
    }

    /**
     * Properties used to configure the backend. If provided explicitly before
     * init is invoked then these take precedence
     * @param properties to configure Backend
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

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
     * Returns the identified file object. This method implements the pattern
     * used to avoid problems with too many files in a single folder.
     *
     * @param identifier data identifier
     * @return identified file object
     * @throws FileSystemException if VFS file system exception occurs
     * @throws DataStoreException 
     */
    protected FileObject getFileObject(DataIdentifier identifier) throws DataStoreException {
        String idString = identifier.toString();

        StringBuilder sb = new StringBuilder(80);
        sb.append(idString.substring(0, 2)).append('/');
        sb.append(idString.substring(2, 4)).append('/');
        sb.append(idString.substring(4, 6)).append('/');
        sb.append(idString.substring(6, 8)).append('/');
        sb.append(idString);

        String relPath = sb.toString();

        try {
            return vfsUriFolder.resolveFile(relPath);
        } catch (FileSystemException e) {
            throw new DataStoreException("Object not resolved: " + identifier, e);
        }
    }

    private FileObject getExistingFileObject(DataIdentifier identifier) throws DataStoreException {
        try {
            FileObject file = getFileObject(identifier);

            if (!file.exists()) {
                throw new DataStoreException("Object not found: " + identifier);
            } else if (file.getType() != FileType.FILE) {
                throw new DataStoreException("Object not in file: " + identifier);
            }

            return file;
        } catch (FileSystemException e) {
            throw new DataStoreException("Object not resolved: " + identifier, e);
        }
    }

    /**
     * Set the last modified date of a fileObject, if the fileObject is writable.
     * @param fileObject the file object
     * @param time the new last modified date
     * @throws DataStoreException if the fileObject is writable but modifying the date
     *             fails
     */
    private static void setLastModified(FileObject fileObject, long time)
                    throws DataStoreException {
        try {
            fileObject.getContent().setLastModifiedTime(time);
        } catch (FileSystemException e) {
            throw new DataStoreException(
                    "An IO Exception occurred while trying to set the last modified date: "
                        + fileObject.getName().getPath(), e);
        }
    }

    /**
     * Get the last modified date of a file object.
     * @param file the file object
     * @return the last modified date
     * @throws DataStoreException if reading fails
     */
    private static long getLastModified(FileObject fileObject) throws DataStoreException {
        long lastModified = 0;

        try {
            lastModified = fileObject.getContent().getLastModifiedTime();

            if (lastModified == 0) {
                throw new DataStoreException("Failed to read record modified date: " + fileObject.getName().getPath());
            }
        } catch (FileSystemException e) {
            throw new DataStoreException("Failed to read record modified date: " + fileObject.getName().getPath());
        }

        return lastModified;
    }

    private void pushIdentifiersRecursively(List<DataIdentifier> identifiers, FileObject folderObject)
            throws FileSystemException {
        FileType type;

        for (FileObject fileObject : folderObject.getChildren()) {
            if (!fileObject.exists()) {
                continue;
            }

            type = fileObject.getType();

            if (type == FileType.FOLDER) {
                pushIdentifiersRecursively(identifiers, fileObject);
            } else if (type == FileType.FILE) {
                identifiers.add(new DataIdentifier(fileObject.getName().getBaseName()));
            }
        }
    }

    private void write(DataIdentifier identifier, File file, boolean asyncUpload, AsyncUploadCallback callback)
            throws DataStoreException {
        FileObject fileObject = getFileObject(identifier);

        AsyncUploadResult asyncUpRes = null;

        if (asyncUpload) {
            asyncUpRes = new AsyncUploadResult(identifier, file);
        }

        synchronized (this) {
            try {
                if (fileObject.exists() && fileObject.getType() == FileType.FILE) {
                    long now = System.currentTimeMillis();
                    if (getLastModified(fileObject) < now + ACCESS_TIME_RESOLUTION) {
                        setLastModified(fileObject, now + ACCESS_TIME_RESOLUTION);
                    }
                } else {
                    copy(file, fileObject);
                }
                if (asyncUpRes != null && callback != null) {
                    callback.onSuccess(asyncUpRes);
                }
            } catch (IOException e) {
                DataStoreException e2 = new DataStoreException("Could not get output stream to object: " + fileObject.getName().getPath(), e);
                if (asyncUpRes != null && callback != null) {
                    asyncUpRes.setException(e2);
                    callback.onFailure(asyncUpRes);
                }
                throw e2;
            }
        }
    }

    private void touch(DataIdentifier identifier, long minModifiedDate, boolean asyncTouch, AsyncTouchCallback callback) throws DataStoreException {
        FileObject fileObject = getExistingFileObject(identifier);

        AsyncTouchResult asyncTouchRes = null;

        if (asyncTouch) {
            asyncTouchRes = new AsyncTouchResult(identifier);
        }

        try {
            long now = System.currentTimeMillis();
            if (minModifiedDate > 0 && minModifiedDate > getLastModified(fileObject)) {
                setLastModified(fileObject, now + ACCESS_TIME_RESOLUTION);
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

    private void deleteEmptyParentDirs(FileObject fileObject) {
        try {
            FileObject parent = fileObject.getParent();
            // Only iterate & delete if parent directory of the blob file is
            // child of the base directory and if it is empty
            String vfsUriDir = vfsUri + "/";
            while (parent.getName().getPath().contains(vfsUriDir)) {
                FileObject[] entries = parent.getChildren();
                if (entries.length > 0) {
                    break;
                }
                boolean deleted = parent.delete();
                LOG.debug("Deleted parent [{}] of file [{}]: {}",
                        new Object[] { parent, fileObject.getName().getPath(), deleted });
                parent = parent.getParent();
            }
        } catch (IOException e) {
            LOG.warn("Error in parents deletion for " + fileObject.getName().getPath(), e);
        }
    }

    private void deleteOlderRecursive(Set<DataIdentifier> deleteIdSet, FileObject folderObject, long timestamp)
            throws FileSystemException, DataStoreException {
        FileType type;
        DataIdentifier identifier;

        for (FileObject fileObject : folderObject.getChildren()) {
            if (!fileObject.exists()) {
                continue;
            }

            type = fileObject.getType();

            if (type == FileType.FOLDER) {
                deleteOlderRecursive(deleteIdSet, fileObject, timestamp);

                synchronized (this) {
                    if (fileObject.getChildren().length == 0) {
                        fileObject.delete();
                    }
                }

            } else if (type == FileType.FILE) {
                long lastModified = getLastModified(fileObject);

                if (lastModified < timestamp) {
                    identifier = new DataIdentifier(fileObject.getName().getBaseName());

                    if (store.confirmDelete(identifier)) {
                        store.deleteFromCache(identifier);

                        if (LOG.isInfoEnabled()) {
                            LOG.info("Deleting old file " + fileObject.getName().getPath() + " modified: "
                                    + new Timestamp(lastModified).toString() + " length: "
                                    + fileObject.getContent().getSize());
                        }

                        if (fileObject.delete()) {
                            deleteIdSet.add(identifier);
                        } else {
                            LOG.warn("Failed to delete old file " + fileObject.getName().getPath());
                        }
                    }
                }
            }
        }
    }

    private void copy(File srcFile, FileObject destFileObject) throws IOException {
        InputStream input = null;
        OutputStream output = null;

        try {
            input = new FileInputStream(srcFile);
            output = destFileObject.getContent().getOutputStream();
            IOUtils.copy(input, output);
        } finally {
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(input);
        }
    }

    /**
     * This class implements {@link Runnable} interface to copy {@link File}
     * to VFS file object asynchronously.
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
}
