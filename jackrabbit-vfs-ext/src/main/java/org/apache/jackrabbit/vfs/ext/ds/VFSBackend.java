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

    /**
     * The maximum last modified time resolution of the file system.
     */
    private static final int ACCESS_TIME_RESOLUTION = 2000;

    private CachingDataStore store;

    /**
     * The name of the folder that contains all the data record files. The structure
     * of content within this directory is controlled by this class.
     */
    private final String basePath;

    private volatile FileObject baseFolder;

    /**
     * This thread pool count for asynchronous uploads. By default it is 10.
     */
    private int asyncUploadPoolSize = 10;

    private ThreadPoolExecutor asyncWriteExecuter;

    public VFSBackend(final String basePath) {
        this.basePath = basePath;
    }

    public int getAsyncUploadPoolSize() {
        return asyncUploadPoolSize;
    }

    public void setAsyncUploadPoolSize(int asyncUploadPoolSize) {
        this.asyncUploadPoolSize = asyncUploadPoolSize;
    }

    @Override
    public void init(CachingDataStore store, String homeDir, String config) throws DataStoreException {
        this.store = store;
        asyncWriteExecuter = (ThreadPoolExecutor) Executors.newFixedThreadPool(
                getAsyncUploadPoolSize(), new NamedThreadFactory("vfs-write-worker"));
    }

    @Override
    public InputStream read(DataIdentifier identifier) throws DataStoreException {
        FileObject fileObject = getFileObject(identifier, true);

        try {
            return fileObject.getContent().getInputStream();
        } catch (FileSystemException e) {
            throw new DataStoreException("Could not get input stream from object: " + identifier, e);
        }
    }

    @Override
    public long getLength(DataIdentifier identifier) throws DataStoreException {
        FileObject fileObject = getFileObject(identifier, true);

        try {
            return fileObject.getContent().getSize();
        } catch (FileSystemException e) {
            throw new DataStoreException("Could not get length from object: " + identifier, e);
        }
    }

    @Override
    public long getLastModified(DataIdentifier identifier) throws DataStoreException {
        FileObject fileObject = getFileObject(identifier, true);

        try {
            return fileObject.getContent().getLastModifiedTime();
        } catch (FileSystemException e) {
            throw new DataStoreException("Could not get last modified timestamp from object: " + identifier, e);
        }
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
            for (FileObject fileObject : baseFolder.getChildren()) {
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
        FileObject fileObject = getFileObject(identifier, false);

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
        getFileSystemManager().closeFileSystem(baseFolder.getFileSystem());
    }

    @Override
    public Set<DataIdentifier> deleteAllOlderThan(long timestamp) throws DataStoreException {
        Set<DataIdentifier> deleteIdSet = new HashSet<DataIdentifier>(30);

        try {
            for (FileObject fileObject : baseFolder.getChildren()) {
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
        FileObject fileObject = getFileObject(identifier, false);

        try {
            fileObject.delete();
        } catch (FileSystemException e) {
            throw new DataStoreException("Object not deleted: " + identifier, e);
        }
    }

    protected FileSystemManager getFileSystemManager() throws DataStoreException {
        try {
            return VFS.getManager();
        } catch (FileSystemException e) {
            throw new DataStoreException("Could not get VFS manager.", e);
        }
    }

    protected FileObject getBaseFolder() throws DataStoreException {
        FileObject localBaseFolder = baseFolder;

        if (localBaseFolder == null) {
            synchronized (this) {
                localBaseFolder = baseFolder;

                if (localBaseFolder == null) {
                    try {
                        localBaseFolder = getFileSystemManager().resolveFile(basePath);

                        if (!localBaseFolder.exists() || localBaseFolder.getType() != FileType.FOLDER) {
                            localBaseFolder.createFolder();
                        }

                        baseFolder = localBaseFolder;
                    } catch (FileSystemException e) {
                        throw new DataStoreException("Could not resolve the base folder at " + basePath, e);
                    }
                }
            }
        }

        return localBaseFolder;
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
    protected FileObject getFileObject(DataIdentifier identifier, boolean checkFileExistence) throws DataStoreException {
        String idString = identifier.toString();

        StringBuilder sb = new StringBuilder(80);
        sb.append(idString.substring(0, 2)).append('/');
        sb.append(idString.substring(2, 4)).append('/');
        sb.append(idString.substring(4, 6)).append('/');
        sb.append(idString.substring(6, 8)).append('/');
        sb.append(idString);

        String relPath = sb.toString();

        try {
            FileObject file = baseFolder.resolveFile(relPath);

            if (checkFileExistence) {
                if (!file.exists()) {
                    throw new DataStoreException("Object not found: " + identifier);
                } else if (file.getType() != FileType.FILE) {
                    throw new DataStoreException("Object not in file: " + identifier);
                }
            }

            return file;
        } catch (FileSystemException e) {
            throw new DataStoreException("Object not resolved: " + identifier, e);
        }
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
        FileObject fileObject = getFileObject(identifier, true);

        AsyncUploadResult asyncUpRes = null;

        if (asyncUpload) {
            asyncUpRes = new AsyncUploadResult(identifier, file);
        }

        InputStream input = null;
        OutputStream output = null;

        try {
            input = new FileInputStream(file);
            output = fileObject.getContent().getOutputStream();
            IOUtils.copy(input, output);
        } catch (FileSystemException e) {
            DataStoreException e2 = new DataStoreException("Could not get output stream to object: " + identifier, e);
            if (asyncUpRes != null) {
                asyncUpRes.setException(e2);
            }
            throw e2;
        } catch (IOException e) {
            DataStoreException e2 = new DataStoreException("Could not copy file to object: " + identifier, e);
            if (asyncUpRes != null) {
                asyncUpRes.setException(e2);
            }
            throw e2;
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    DataStoreException e2 = new DataStoreException("Could not close output stream to object: " + identifier, e); 
                    if (asyncUpRes != null) {
                        asyncUpRes.setException(e2);
                    }
                    throw e2;
                }
            }

            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    DataStoreException e2 = new DataStoreException("Could not close input stream from file: " + identifier, e);
                    if (asyncUpRes != null) {
                        asyncUpRes.setException(e2);
                    }
                    throw e2;
                }
            }

            if (asyncUpRes != null && callback != null) {
                if (asyncUpRes.getException() != null) {
                    callback.onAbort(asyncUpRes);
                } else {
                    callback.onSuccess(asyncUpRes);
                }
            }
        }
    }

    private void touch(DataIdentifier identifier, long minModifiedDate, boolean asyncTouch, AsyncTouchCallback callback) throws DataStoreException {
        FileObject fileObject = getFileObject(identifier, true);

        AsyncTouchResult asyncTouchRes = null;

        if (asyncTouch) {
            asyncTouchRes = new AsyncTouchResult(identifier);
        }

        try {
            FileContent content = fileObject.getContent();

            long now = System.currentTimeMillis();

            if (minModifiedDate > 0 && minModifiedDate > content.getLastModifiedTime()) {
                content.setLastModifiedTime(now + ACCESS_TIME_RESOLUTION);
            }
        } catch (FileSystemException e) {
            DataStoreException e2 = new DataStoreException("Object not resolved: " + identifier, e);
            if (asyncTouchRes != null) {
                asyncTouchRes.setException(e2);
            }
            throw e2;
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
                long lastModified = fileObject.getContent().getLastModifiedTime();

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
