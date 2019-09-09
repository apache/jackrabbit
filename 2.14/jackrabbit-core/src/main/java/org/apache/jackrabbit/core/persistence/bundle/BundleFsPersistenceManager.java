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
package org.apache.jackrabbit.core.persistence.bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.BasedFileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.persistence.util.BLOBStore;
import org.apache.jackrabbit.core.persistence.util.BundleBinding;
import org.apache.jackrabbit.core.persistence.util.ErrorHandling;
import org.apache.jackrabbit.core.persistence.util.FileSystemBLOBStore;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle;
import org.apache.jackrabbit.core.persistence.util.Serializer;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is a generic persistence manager that stores the {@link NodePropBundle}s
 * in a filesystem.
 * <p>
 * Configuration:<br>
 * <ul>
 * <li>&lt;param name="{@link #setBlobFSBlockSize(String) blobFSBlockSize}" value="0"/&gt;
 * <li>&lt;param name="{@link #setMinBlobSize(String) minBlobSize}" value="4096"/&gt;
 * <li>&lt;param name="{@link #setErrorHandling(String) errorHandling}" value=""/&gt;
 * </ul>
 */
public class BundleFsPersistenceManager extends AbstractBundlePersistenceManager {

    /** the default logger */
    private static Logger log = LoggerFactory.getLogger(BundleFsPersistenceManager.class);

    /** flag indicating if this manager was initialized */
    protected boolean initialized;

    /** file system where BLOB data is stored */
    protected BundleFsPersistenceManager.CloseableBLOBStore blobStore;

    /**
     * Default blocksize for BLOB filesystem:
     * @see #setBlobFSBlockSize(String)
     */
    private int blobFSBlockSize;

    /**
     * the minimum size of a property until it gets written to the blob store
     * @see #setMinBlobSize(String)
     */
    private int minBlobSize = 0x1000;

    /**
     * the filesystem where the items are stored
     */
    private FileSystem itemFs;

    /**
     * flag for error handling
     */
    protected ErrorHandling errorHandling = new ErrorHandling();

    /**
     * the bundle binding
     */
    protected BundleBinding binding;

    /**
     * the name of this persistence manager
     */
    private String name = super.toString();


    /**
     * Returns the configured block size of the blob cqfs
     * @return the block size.
     */
    public String getBlobFSBlockSize() {
        return String.valueOf(blobFSBlockSize);
    }

    /**
     * Sets the block size of the blob fs and controls how blobs are handled.
     * <br>
     * If the size is 0, the blobs are stored within the workspace's physical filesystem.
     * <br>
     * Otherwise, the blobs are stored within the item filesystem.
     * <br>
     * Please note that not all binary properties are considered as blobs. They
     * are only stored in the respective blob store, if their size exceeds
     * {@link #getMinBlobSize()}.
     *
     * @param size the block size
     */
    public void setBlobFSBlockSize(String size) {
        this.blobFSBlockSize = Integer.decode(size).intValue();
    }

    /**
     * Returns <code>true</code> if the blobs are stored in the local fs.
     * @return <code>true</code> if the blobs are stored in the local fs.
     */
    public boolean useLocalFsBlobStore() {
        return blobFSBlockSize == 0;
    }

    /**
     * Returns the mininum blob size.
     * @return the mininum blob size.
     */
    public String getMinBlobSize() {
        return String.valueOf(minBlobSize);
    }

    /**
     * Sets the minimum blob size. This size defines the threshold of which
     * size a property is included in the bundle or is stored in the blob store.
     * Very high values decrease the performance.
     *
     * @param minBlobSize
     */
    public void setMinBlobSize(String minBlobSize) {
        this.minBlobSize = Integer.decode(minBlobSize).intValue();
    }

    /**
     * Sets the error handling behaviour of this manager. See {@link ErrorHandling}
     * for details about the flags.
     *
     * @param errorHandling
     */
    public void setErrorHandling(String errorHandling) {
        this.errorHandling = new ErrorHandling(errorHandling);
    }

    /**
     * Returns the error handling configuration of this manager
     * @return the error handling configuration of this manager
     */
    public String getErrorHandling() {
        return errorHandling.toString();
    }

    /**
     * {@inheritDoc}
     */
    public void init(PMContext context) throws Exception {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        super.init(context);

        this.name = context.getHomeDir().getName();

        // create item fs
        itemFs = new BasedFileSystem(context.getFileSystem(), "items");

        // create correct blob store
        if (useLocalFsBlobStore()) {
            LocalFileSystem blobFS = new LocalFileSystem();
            blobFS.setRoot(new File(context.getHomeDir(), "blobs"));
            blobFS.init();
            blobStore = new BundleFsPersistenceManager.FSBlobStore(blobFS);
        } else {
            blobStore = new BundleFsPersistenceManager.FSBlobStore(itemFs);
        }

        // load namespaces
        binding = new BundleBinding(errorHandling, blobStore, getNsIndex(), getNameIndex(), context.getDataStore());
        binding.setMinBlobSize(minBlobSize);

        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BLOBStore getBlobStore() {
        return blobStore;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            // close blob store
            blobStore.close();
            blobStore = null;
            itemFs.close();
            itemFs = null;
            super.close();
        } finally {
            initialized = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    protected NodePropBundle loadBundle(NodeId id) throws ItemStateException {
        try {
            String path = buildNodeFilePath(null, id).toString();
            if (!itemFs.exists(path)) {
                return null;
            }
            InputStream in = itemFs.getInputStream(path);
            try {
                return binding.readBundle(in, id);
            } finally {
                IOUtils.closeQuietly(in);
            }
        } catch (Exception e) {
            String msg = "failed to read bundle: " + id + ": " + e;
            log.error(msg);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * Creates the file path for the given node id that is
     * suitable for storing node states in a filesystem.
     *
     * @param buf buffer to append to or <code>null</code>
     * @param id the id of the node
     * @return the buffer with the appended data.
     */
    protected StringBuffer buildNodeFilePath(StringBuffer buf, NodeId id) {
        if (buf == null) {
            buf = new StringBuffer();
        }
        buildNodeFolderPath(buf, id);
        buf.append('.');
        buf.append(NODEFILENAME);
        return buf;
    }

    /**
     * Creates the file path for the given references id that is
     * suitable for storing reference states in a filesystem.
     *
     * @param buf buffer to append to or <code>null</code>
     * @param id the id of the node
     * @return the buffer with the appended data.
     */
    protected StringBuffer buildNodeReferencesFilePath(
            StringBuffer buf, NodeId id) {
        if (buf == null) {
            buf = new StringBuffer();
        }
        buildNodeFolderPath(buf, id);
        buf.append('.');
        buf.append(NODEREFSFILENAME);
        return buf;
    }

    /**
     * {@inheritDoc}
     */
    protected synchronized void storeBundle(NodePropBundle bundle) throws ItemStateException {
        try {
            StringBuffer buf = buildNodeFolderPath(null, bundle.getId());
            buf.append('.');
            buf.append(NODEFILENAME);
            String fileName = buf.toString();
            String dir = fileName.substring(0, fileName.lastIndexOf(FileSystem.SEPARATOR_CHAR));
            if (!itemFs.exists(dir)) {
                itemFs.createFolder(dir);
            }
            OutputStream out = itemFs.getOutputStream(fileName);
            try {
                binding.writeBundle(out, bundle);
            } finally {
                out.close();
            }
        } catch (Exception e) {
            String msg = "failed to write bundle: " + bundle.getId();
            BundleFsPersistenceManager.log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected synchronized void destroyBundle(NodePropBundle bundle) throws ItemStateException {
        try {
            StringBuffer buf = buildNodeFilePath(null, bundle.getId());
            itemFs.deleteFile(buf.toString());
        } catch (Exception e) {
            if (e instanceof NoSuchItemStateException) {
                throw (NoSuchItemStateException) e;
            }
            String msg = "failed to delete bundle: " + bundle.getId();
            BundleFsPersistenceManager.log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized NodeReferences loadReferencesTo(NodeId targetId)
            throws NoSuchItemStateException, ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
        InputStream in = null;
        try {
            String path = buildNodeReferencesFilePath(null, targetId).toString();
            if (!itemFs.exists(path)) {
                // special case
                throw new NoSuchItemStateException(targetId.toString());
            }
            in = itemFs.getInputStream(path);
            NodeReferences refs = new NodeReferences(targetId);
            Serializer.deserialize(refs, in);
            return refs;
        } catch (NoSuchItemStateException e) {
            throw e;
        } catch (Exception e) {
            String msg = "failed to read references: " + targetId;
            BundleFsPersistenceManager.log.error(msg, e);
            throw new ItemStateException(msg, e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void store(NodeReferences refs)
            throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
        try {
            StringBuffer buf = buildNodeFolderPath(null, refs.getTargetId());
            buf.append('.');
            buf.append(NODEREFSFILENAME);
            String fileName = buf.toString();
            String dir = fileName.substring(0, fileName.lastIndexOf(FileSystem.SEPARATOR_CHAR));
            if (!itemFs.exists(dir)) {
                itemFs.createFolder(dir);
            }
            OutputStream out = itemFs.getOutputStream(fileName);
            Serializer.serialize(refs, out);
            out.close();
        } catch (Exception e) {
            String msg = "failed to write " + refs;
            BundleFsPersistenceManager.log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void destroy(NodeReferences refs) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
        try {
            StringBuffer buf = buildNodeReferencesFilePath(null, refs.getTargetId());
            itemFs.deleteFile(buf.toString());
        } catch (Exception e) {
            if (e instanceof NoSuchItemStateException) {
                throw (NoSuchItemStateException) e;
            }
            String msg = "failed to delete " + refs;
            BundleFsPersistenceManager.log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean existsReferencesTo(NodeId targetId) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
        try {
            StringBuffer buf = buildNodeReferencesFilePath(null, targetId);
            return itemFs.exists(buf.toString());
        } catch (Exception e) {
            String msg = "failed to check existence of node references: " + targetId;
            BundleFsPersistenceManager.log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * logs an sql exception
     * @param message
     * @param se
     */
    protected void logException(String message, SQLException se) {
        if (message != null) {
            BundleFsPersistenceManager.log.error(message);
        }
        BundleFsPersistenceManager.log.error("       Reason: " + se.getMessage());
        BundleFsPersistenceManager.log.error(
                "   State/Code: " + se.getSQLState() + "/" + se.getErrorCode());
        BundleFsPersistenceManager.log.debug("   dump:", se);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return name;
    }

    /**
     * Helper interface for closeable stores
     */
    protected static interface CloseableBLOBStore extends BLOBStore {
        void close();
    }

    /**
     * own implementation of the filesystem blob store that uses a different
     * blob-id scheme.
     */
    private class FSBlobStore extends FileSystemBLOBStore implements BundleFsPersistenceManager.CloseableBLOBStore {

        private FileSystem fs;

        public FSBlobStore(FileSystem fs) {
            super(fs);
            this.fs = fs;
        }

        public String createId(PropertyId id, int index) {
            return buildBlobFilePath(null, id, index).toString();
        }

        public void close() {
            try {
                fs.close();
                fs = null;
            } catch (Exception e) {
                // ignore
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<NodeId> getAllNodeIds(NodeId bigger, int maxCount)
            throws ItemStateException {
        ArrayList<NodeId> list = new ArrayList<NodeId>();
        try {
            getListRecursive(list, "", bigger == null ? null : bigger, maxCount);
            return list;
        } catch (FileSystemException e) {
            String msg = "failed to read node list: " + bigger + ": " + e;
            log.error(msg);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected NodeId getIdFromFileName(String fileName) {
        StringBuffer buff = new StringBuffer(35);
        if (!fileName.endsWith("." + NODEFILENAME)) {
            return null;
        }
        for (int i = 0; i < fileName.length(); i++) {
            char c = fileName.charAt(i);
            if (c == '.') {
                break;
            }
            if (c != '/') {
                buff.append(c);
                int len = buff.length();
                if (len == 8 || len == 13 || len == 18 || len == 23) {
                    buff.append('-');
                }
            }
        }
        return new NodeId(buff.toString());
    }

    private void getListRecursive(
            ArrayList<NodeId> list, String path, NodeId bigger, int maxCount)
            throws FileSystemException {
        if (maxCount > 0 && list.size() >= maxCount) {
            return;
        }
        String[] files = itemFs.listFiles(path);
        Arrays.sort(files);
        for (int i = 0; i < files.length; i++) {
            String f = files[i];
            NodeId n = getIdFromFileName(path + FileSystem.SEPARATOR + f);
            if (n == null) {
                continue;
            }
            if (bigger != null && bigger.toString().compareTo(n.toString()) >= 0) {
                continue;
            }
            list.add(n);
            if (maxCount > 0 && list.size() >= maxCount) {
                return;
            }
        }
        String[] dirs = itemFs.listFolders(path);
        Arrays.sort(dirs);
        for (int i = 0; i < dirs.length; i++) {
            getListRecursive(list, path + FileSystem.SEPARATOR + dirs[i],
                    bigger, maxCount);
        }
    }

}
