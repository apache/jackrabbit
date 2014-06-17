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
package org.apache.jackrabbit.core.persistence.mem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.persistence.bundle.AbstractBundlePersistenceManager;
import org.apache.jackrabbit.core.persistence.util.BLOBStore;
import org.apache.jackrabbit.core.persistence.util.BundleBinding;
import org.apache.jackrabbit.core.persistence.util.ErrorHandling;
import org.apache.jackrabbit.core.persistence.util.FileSystemBLOBStore;
import org.apache.jackrabbit.core.persistence.util.NodeInfo;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle;
import org.apache.jackrabbit.core.persistence.util.Serializer;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>BundleInMemPersistenceManager</code> is a <code>HashMap</code>-based
 * <code>PersistenceManager</code> for Jackrabbit that keeps all data in memory
 * and that is capable of storing and loading its contents using a simple custom
 * binary serialization format (see {@link Serializer}).
 * <p>
 * It is configured through the following properties:
 * <ul>
 * <li><code>initialCapacity</code>: initial capacity of the hash map used to store the data</li>
 * <li><code>loadFactor</code>: load factor of the hash map used to store the data</li>
 * <li><code>persistent</code>: if <code>true</code> the contents of the hash map
 * is loaded on startup and stored on shutdown;
 * if <code>false</code> nothing is persisted</li>
 * <li><code>useFileBlobStore</code>: if <code>true</code> the contents of the blobs
 * will be directly stored on the file system instead of in memory.</li>
 * <li><code>minBlobSize</code> use blob store for binaries properties larger 
 * than minBlobSite (bytes). Default is 4096.</li>
 * </ul>
 * <p>
 * <b>Please note that this class should only be used for testing purposes.</b>
 *
 */
public class InMemBundlePersistenceManager extends AbstractBundlePersistenceManager {

    /** the default logger */
    private static Logger log = LoggerFactory.getLogger(InMemBundlePersistenceManager.class);

    /** flag indicating if this manager was initialized */
    protected boolean initialized;

    /**
     * Path where bundles are stored on shutdown.
     * (if <code>persistent==true</code>)
     */
    protected static final String BUNDLE_FILE_PATH = "/data/.bundle.bin";

    /**
     * Path where blobs are stored on shutdown.
     * (if <code>persistent==true</code> and <code>useFileBlobStore==false</code>)
     */
    protected static final String BLOBS_FILE_PATH = "/data/.blobs.bin";

    /**
     * Path where references are stored on shutdown.
     * (if <code>persistent==true</code>)
     */
    protected static final String REFS_FILE_PATH = "/data/.refs.bin";

    /**
     * File system where the content of the hash maps are read from/written to
     * (if <code>persistent==true</code>)
     */
    protected FileSystem wspFS;

    /**
     * File system where BLOB data is stored.
     * (if <code>useFileBlobStore==true</code>)
     */
    protected FileSystem blobFS;

    /** 
     * Initial size of buffer used to serialize objects. 
     */
    protected static final int INITIAL_BUFFER_SIZE = 1024;

    /**
     * the minimum size of a property until it gets written to the blob store
     * @see #setMinBlobSize(String)
     */
    private int minBlobSize = 0x1000;

    /**
     * Flag for error handling.
     */
    protected ErrorHandling errorHandling = new ErrorHandling();

    /**
     * the bundle binding
     */
    protected BundleBinding binding;

    /**
     * Bundle memory store.
     */
    protected Map<NodeId, byte[]> bundleStore;

    /**
     * References memory store.
     */
    protected Map<NodeId, byte[]> refsStore;

    /**
     * Blob store.
     */
    protected BLOBStore blobStore;

    /**
     * Blobs memory store used by the InMemBlobStore.
     * (if <code>useFileBlobStore==false</code>)
     */
    private Map<String, byte[]> blobs;

    /**
     *  initial capacity
     */
    protected int initialCapacity = 32768;

    /**
     *  load factor for the hash map
     */
    protected float loadFactor = 0.75f;

    /**
     *  Should hash map be persisted?
     */
    protected boolean persistent = true;

    /**
     * Store blobs on file system instead of memory.
     */
    protected boolean useFileBlobStore = false;

    /**
     * Creates a new <code>InMemBundlePersistenceManager</code> instance.
     */
    public InMemBundlePersistenceManager() {
        initialized = false;
    }

    public void setInitialCapacity(int initialCapacity) {
        this.initialCapacity = initialCapacity;
    }

    public void setInitialCapacity(String initialCapacity) {
        this.initialCapacity = Integer.parseInt(initialCapacity);
    }

    public String getInitialCapacity() {
        return Integer.toString(initialCapacity);
    }

    public void setLoadFactor(float loadFactor) {
        this.loadFactor = loadFactor;
    }

    public void setLoadFactor(String loadFactor) {
        this.loadFactor = Float.parseFloat(loadFactor);
    }

    public String getLoadFactor() {
        return Float.toString(loadFactor);
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public void setPersistent(String persistent) {
        this.persistent = Boolean.valueOf(persistent).booleanValue();
    }

    public void setUseFileBlobStore(boolean useFileBlobStore) {
        this.useFileBlobStore = useFileBlobStore;
    }

    public void setUseFileBlobStore(String useFileBlobStore) {
        this.useFileBlobStore = Boolean.valueOf(useFileBlobStore).booleanValue();
    }

    public String getMinBlobSize() {
        return String.valueOf(minBlobSize);
    }

    public void setMinBlobSize(String minBlobSize) {
        this.minBlobSize = Integer.decode(minBlobSize).intValue();
    }

    /**
     * Reads the content of the hash maps from the file system
     *
     * @throws Exception if an error occurs
     */
    public synchronized void loadContents() throws Exception {
        // read item states
        FileSystemResource fsRes = new FileSystemResource(wspFS, BUNDLE_FILE_PATH);
        if (!fsRes.exists()) {
            return;
        }
        BufferedInputStream bis = new BufferedInputStream(fsRes.getInputStream());
        DataInputStream in = new DataInputStream(bis);

        try {
            int n = in.readInt(); // number of entries
            while (n-- > 0) {
                String s = in.readUTF(); // id
                NodeId id = NodeId.valueOf(s);
                int length = in.readInt(); // data length
                byte[] data = new byte[length];
                in.readFully(data); // data
                // store in map
                bundleStore.put(id, data);
            }
        } finally {
            in.close();
        }

        // read references
        fsRes = new FileSystemResource(wspFS, REFS_FILE_PATH);
        bis = new BufferedInputStream(fsRes.getInputStream());
        in = new DataInputStream(bis);

        try {
            int n = in.readInt(); // number of entries
            while (n-- > 0) {
                String s = in.readUTF(); // target id
                NodeId id = NodeId.valueOf(s);
                int length = in.readInt(); // data length
                byte[] data = new byte[length];
                in.readFully(data); // data
                // store in map
                refsStore.put(id, data);
            }
        } finally {
            in.close();
        }

        if (!useFileBlobStore) {
            // read blobs
            fsRes = new FileSystemResource(wspFS, BLOBS_FILE_PATH);
            bis = new BufferedInputStream(fsRes.getInputStream());
            in = new DataInputStream(bis);

            try {
                int n = in.readInt(); // number of entries
                while (n-- > 0) {
                    String id = in.readUTF(); // id
                    int length = in.readInt(); // data length
                    byte[] data = new byte[length];
                    in.readFully(data); // data
                    // store in map
                    blobs.put(id, data);
                }
            } finally {
                in.close();
            }
        }
    }

    /**
     * Writes the content of the hash maps stores to the file system.
     *
     * @throws Exception if an error occurs
     */
    public synchronized void storeContents() throws Exception {
        // write bundles
        FileSystemResource fsRes = new FileSystemResource(wspFS, BUNDLE_FILE_PATH);
        fsRes.makeParentDirs();
        BufferedOutputStream bos = new BufferedOutputStream(fsRes.getOutputStream());
        DataOutputStream out = new DataOutputStream(bos);

        try {
            out.writeInt(bundleStore.size()); // number of entries
            // entries
            for (NodeId id : bundleStore.keySet()) {
                out.writeUTF(id.toString()); // id

                byte[] data = bundleStore.get(id);
                out.writeInt(data.length); // data length
                out.write(data); // data
            }
        } finally {
            out.close();
        }

        // write references
        fsRes = new FileSystemResource(wspFS, REFS_FILE_PATH);
        fsRes.makeParentDirs();
        bos = new BufferedOutputStream(fsRes.getOutputStream());
        out = new DataOutputStream(bos);

        try {
            out.writeInt(refsStore.size()); // number of entries
            // entries
            for (NodeId id : refsStore.keySet()) {
                out.writeUTF(id.toString()); // target id

                byte[] data = refsStore.get(id);
                out.writeInt(data.length); // data length
                out.write(data); // data
            }
        } finally {
            out.close();
        }

        if (!useFileBlobStore) {
            // write blobs
            fsRes = new FileSystemResource(wspFS, BLOBS_FILE_PATH);
            fsRes.makeParentDirs();
            bos = new BufferedOutputStream(fsRes.getOutputStream());
            out = new DataOutputStream(bos);

            try {
                out.writeInt(blobs.size()); // number of entries
                // entries
                for (String id : blobs.keySet()) {
                    out.writeUTF(id); // id
                    byte[] data = blobs.get(id);
                    out.writeInt(data.length); // data length
                    out.write(data); // data
                }
            } finally {
                out.close();
            }
        }
    }

    //---------------------------------------------------< PersistenceManager >
    /**
     * {@inheritDoc}
     */
    public void init(PMContext context) throws Exception {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        super.init(context);
        // initialize mem stores
        bundleStore = new LinkedHashMap<NodeId, byte[]>(initialCapacity, loadFactor);
        refsStore = new HashMap<NodeId, byte[]>(initialCapacity, loadFactor);

        // Choose a FileSystem for the BlobStore based on whether data is persistent or not 
        if (useFileBlobStore) {
            blobFS = new LocalFileSystem();
            ((LocalFileSystem) blobFS).setRoot(new File(context.getHomeDir(), "blobs"));
            blobFS.init();
            blobStore = new FileSystemBLOBStore(blobFS);
        } else {
            blobStore = new InMemBLOBStore();
        }

        wspFS = context.getFileSystem();

        // load namespaces
        binding = new BundleBinding(errorHandling, blobStore, getNsIndex(), getNameIndex(), context.getDataStore());
        binding.setMinBlobSize(minBlobSize);

        if (persistent) {
            // deserialize contents of the stores
            loadContents();
        }
        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            if (persistent) {
                // serialize contents of state and refs stores
                storeContents();
            } else if (useFileBlobStore) {
                blobFS.close();
                // not persistent, clear out blobs
                wspFS.deleteFolder("blobs");
            }
        } finally {
            initialized = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeReferences loadReferencesTo(NodeId id) throws NoSuchItemStateException, ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
        if (!refsStore.containsKey(id)) {
            throw new NoSuchItemStateException(id.toString());
        }

        try {
            NodeReferences refs = new NodeReferences(id);
            Serializer.deserialize(refs, new ByteArrayInputStream(refsStore.get(id)));
            return refs;
        } catch (Exception e) {
            String msg = "failed to read references: " + id;
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void store(NodeReferences refs) throws ItemStateException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            // serialize references
            Serializer.serialize(refs, out);
            // store in serialized format in map for better memory efficiency
            refsStore.put(refs.getTargetId(), out.toByteArray());
        } catch (Exception e) {
            String msg = "failed to store " + refs;
            log.debug(msg);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean existsReferencesTo(NodeId targetId) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
        return refsStore.containsKey(targetId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void destroy(NodeReferences refs) throws ItemStateException {
        refsStore.remove(refs.getTargetId());
    }

    /**
     * {@inheritDoc}
     */
    public List<NodeId> getAllNodeIds(NodeId after, int maxCount) throws ItemStateException, RepositoryException {
        final List<NodeId> result = new ArrayList<NodeId>();
        boolean add = after == null;
        int count = 0;
        for (NodeId nodeId : bundleStore.keySet()) {
            if (add) {
                result.add(nodeId);
                if (++count == maxCount) {
                    break;
                }
            } else {
                add = nodeId.equals(after);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodePropBundle loadBundle(NodeId id) throws ItemStateException {
        if (!bundleStore.containsKey(id)) {
            return null;
        }
        try {
            return binding.readBundle(new ByteArrayInputStream(bundleStore.get(id)), id);
        } catch (Exception e) {
            String msg = "failed to read bundle: " + id + ": " + e;
            log.error(msg);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void storeBundle(NodePropBundle bundle) throws ItemStateException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
        try {
            binding.writeBundle(out, bundle);
            bundleStore.put(bundle.getId(), out.toByteArray());
        } catch (IOException e) {
            String msg = "failed to write bundle: " + bundle.getId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void destroyBundle(NodePropBundle bundle) throws ItemStateException {
        bundleStore.remove(bundle.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BLOBStore getBlobStore() {
        return blobStore;
    }

    /**
     * Helper interface for closeable stores
     */
    protected static interface CloseableBLOBStore extends BLOBStore {
        void close();
    }

    /**
     * Trivial {@link BLOBStore} implementation that is backed by a {@link HashMap}.
     */
    protected class InMemBLOBStore implements CloseableBLOBStore {

        public InMemBLOBStore() {
            blobs = new HashMap<String, byte[]>();
        }

        /**
         * {@inheritDoc}
         */
        public String createId(PropertyId id, int index) {
            StringBuilder buf = new StringBuilder();
            buf.append(id.getParentId().toString());
            buf.append('.');
            buf.append(getNsIndex().stringToIndex(id.getName().getNamespaceURI()));
            buf.append('.');
            buf.append(getNameIndex().stringToIndex(id.getName().getLocalName()));
            buf.append('.');
            buf.append(index);
            return buf.toString();
        }

        /**
         * {@inheritDoc}
         */
        public void put(String blobId, InputStream in, long size) throws Exception {
            blobs.put(blobId, IOUtils.toByteArray(in));
        }

        /**
         * {@inheritDoc}
         */
        public InputStream get(String blobId) throws Exception {
            if (blobs.containsKey(blobId)) {
                return new ByteArrayInputStream(blobs.get(blobId));
            } else {
                return null;
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean remove(String blobId) throws Exception {
            return blobs.remove(blobId) != null;
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            blobs.clear();
        }
    }
}
