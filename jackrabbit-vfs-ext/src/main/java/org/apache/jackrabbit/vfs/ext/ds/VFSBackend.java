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
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.jackrabbit.core.data.AbstractBackend;
import org.apache.jackrabbit.core.data.AsyncTouchCallback;
import org.apache.jackrabbit.core.data.AsyncTouchResult;
import org.apache.jackrabbit.core.data.AsyncUploadCallback;
import org.apache.jackrabbit.core.data.AsyncUploadResult;
import org.apache.jackrabbit.core.data.CachingDataStore;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A data store backend that stores data on VFS file system.
 */
public class VFSBackend extends AbstractBackend {

    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(VFSBackend.class);

    /**
     * The default pool size of asynchronous write pooling executor.
     */
    static final int DEFAULT_ASYNC_WRITE_POOL_SIZE = 10;

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
     * VFS base folder object.
     */
    private FileObject baseFolder;

    /**
     * Whether or not a touch file is preferred to set/get the last modified timestamp for a file object
     * instead of setting/getting the last modified timestamp directly from the file object.
     */
    private boolean touchFilePreferred = true;

    public VFSBackend(FileObject baseFolder) {
        this.baseFolder = baseFolder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(CachingDataStore store, String homeDir, String config) throws DataStoreException {
        super.init(store, homeDir, config);

        // When it's local file system, no need to use a separate touch file.
        if ("file".equals(baseFolder.getName().getScheme())) {
            touchFilePreferred = false;
        }
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

        getAsyncWriteExecutor().execute(new AsyncUploadJob(identifier, file, callback));
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

        getAsyncWriteExecutor().execute(new AsyncTouchJob(identifier, minModifiedDate, callback));
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
     * Returns the VFS base folder object.
     * @return the VFS base folder object
     */
    protected FileObject getBaseFolderObject() {
        return baseFolder;
    }

    /**
     * Returns a resolved identified file object. This method implements the pattern
     * used to avoid problems with too many files in a single folder.
     *
     * @param identifier data identifier
     * @return identified file object
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
        sb.append(idString);
        return sb.toString();
    }

    /**
     * Returns the identified file object. If not existing, returns null.
     *
     * @param identifier data identifier
     * @return identified file object
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
     * If there's no corresponding touch file existing, then returns null when {@code create} is false.
     * When {@code create} is true, it creates a new touch file if no corresponding touch file exists.
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
            throw new DataStoreException("Touch file object not resolved: " + fileObject.getName().getFriendlyURI(), e);
        }
    }

    /**
     * Returns the approximate number of threads that are actively executing asynchronous writing tasks.
     * @return the approximate number of threads that are actively executing asynchronous writing tasks
     */
    protected int getAsyncWriteExecutorActiveCount() {
        Executor asyncExecutor = getAsyncWriteExecutor();

        if (asyncExecutor != null && asyncExecutor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) asyncExecutor).getActiveCount();
        }

        return 0;
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
                    + fileObject.getName().getFriendlyURI(), e);
        }
    }

    /**
     * Get the last modified time of a file object.
     * @param fileObject the file object
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
                throw new DataStoreException("Failed to read record modified date: " + fileObject.getName().getFriendlyURI());
            }
        } catch (FileSystemException e) {
            throw new DataStoreException("Failed to read record modified date: " + fileObject.getName().getFriendlyURI());
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
                        "Could not get output stream to object: " + resolvedFileObject.getName().getFriendlyURI(), e);

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
                LOG.debug("File doesn't exist for the identifier: {}.", identifier);
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
                LOG.warn("Could not delete touch file for " + fileObject.getName().getFriendlyURI(), e);
            }
        }

        try {
            return fileObject.delete();
        } catch (FileSystemException e) {
            throw new DataStoreException("Could not delete record file at " + fileObject.getName().getFriendlyURI(), e);
        }
    }

    /**
     * Deletes the parent folders of {@code fileObject} if a parent folder is empty.
     * @param fileObject fileObject to start with
     * @throws DataStoreException if any file system exception occurs
     */
    private void deleteEmptyParentFolders(FileObject fileObject) throws DataStoreException {
        try {
            String baseFolderUri = getBaseFolderObject().getName().getFriendlyURI() + "/";
            FileObject parentFolder = fileObject.getParent();

            // Only iterate & delete if parent folder of the blob file is
            // child of the base directory and if it is empty
            while (parentFolder.getName().getFriendlyURI().startsWith(baseFolderUri)) {
                if (VFSUtils.hasAnyChildFileOrFolder(parentFolder)) {
                    break;
                }

                boolean deleted = parentFolder.delete();
                LOG.debug("Deleted parent folder [{}] of file [{}]: {}",
                        new Object[] { parentFolder, fileObject.getName().getFriendlyURI(), deleted });
                parentFolder = parentFolder.getParent();
            }
        } catch (IOException e) {
            LOG.warn("Error in parents deletion for " + fileObject.getName().getFriendlyURI(), e);
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

                    if (getDataStore().confirmDelete(identifier)) {
                        getDataStore().deleteFromCache(identifier);

                        if (LOG.isInfoEnabled()) {
                            LOG.info("Deleting old file " + fileObject.getName().getFriendlyURI() + " modified: "
                                    + new Timestamp(lastModified).toString() + " length: "
                                    + fileObject.getContent().getSize());
                        }

                        if (deleteRecordFileObject(fileObject)) {
                            deleteIdSet.add(identifier);
                        } else {
                            LOG.warn("Failed to delete old file " + fileObject.getName().getFriendlyURI());
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

        /**
         * Record data identifier.
         */
        private DataIdentifier identifier;

        /**
         * Source file to upload.
         */
        private File file;

        /**
         * Callback to handle events on completion, failure or abortion.
         */
        private AsyncUploadCallback callback;

        /**
         * Constructs an asynchronous file uploading job.
         * @param identifier record data identifier
         * @param file source file to upload
         * @param callback callback to handle events on completion, failure or abortion.
         */
        public AsyncUploadJob(DataIdentifier identifier, File file, AsyncUploadCallback callback) {
            super();
            this.identifier = identifier;
            this.file = file;
            this.callback = callback;
        }

        /**
         * Executes this job.
         */
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

        /**
         * Record data identifier.
         */
        private DataIdentifier identifier;

        /**
         * Minimum modification time in milliseconds to be used in touching.
         */
        private long minModifiedDate;

        /**
         * Callback to handle events on completion, failure or abortion.
         */
        private AsyncTouchCallback callback;

        /**
         * Constructs an asynchronous record touching job.
         * @param identifier record data identifier
         * @param minModifiedDate minimum modification time in milliseconds to be used in touching
         * @param callback callback to handle events on completion, failure or abortion
         */
        public AsyncTouchJob(DataIdentifier identifier, long minModifiedDate, AsyncTouchCallback callback) {
            super();
            this.identifier = identifier;
            this.minModifiedDate = minModifiedDate;
            this.callback = callback;
        }

        /**
         * Executes this job.
         */
        public void run() {
            try {
                touch(identifier, minModifiedDate, true, callback);
            } catch (DataStoreException e) {
                LOG.error("Could not touch [" + identifier + "]", e);
            }
        }
    }
}
