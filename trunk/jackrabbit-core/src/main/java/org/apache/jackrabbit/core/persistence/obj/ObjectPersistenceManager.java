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
package org.apache.jackrabbit.core.persistence.obj;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.fs.BasedFileSystem;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.core.persistence.AbstractPersistenceManager;
import org.apache.jackrabbit.core.persistence.PMContext;
import org.apache.jackrabbit.core.persistence.util.BLOBStore;
import org.apache.jackrabbit.core.persistence.util.FileSystemBLOBStore;
import org.apache.jackrabbit.core.persistence.util.Serializer;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * <code>ObjectPersistenceManager</code> is a <code>FileSystem</code>-based
 * <code>PersistenceManager</code> that persists <code>ItemState</code>
 * and <code>NodeReferences</code> objects using a simple custom binary
 * serialization format (see {@link Serializer}).
 *
 * @deprecated Please migrate to a bundle persistence manager
 *   (<a href="https://issues.apache.org/jira/browse/JCR-2802">JCR-2802</a>)
 */
@Deprecated
public class ObjectPersistenceManager extends AbstractPersistenceManager {

    private static Logger log = LoggerFactory.getLogger(ObjectPersistenceManager.class);

    /**
     * hexdigits for toString
     */
    private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

    private static final String NODEFILENAME = ".node";

    private static final String NODEREFSFILENAME = ".references";

    private boolean initialized;

    // file system where the item state is stored
    private FileSystem itemStateFS;
    // file system where BLOB data is stored
    private FileSystem blobFS;
    // BLOBStore that manages BLOB data in the file system
    private BLOBStore blobStore;

    /**
     * Creates a new <code>ObjectPersistenceManager</code> instance.
     */
    public ObjectPersistenceManager() {
        initialized = false;
    }

    private static String buildNodeFolderPath(NodeId id) {
        StringBuilder sb = new StringBuilder();
        char[] chars = id.toString().toCharArray();
        int cnt = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '-') {
                continue;
            }
            //if (cnt > 0 && cnt % 4 == 0) {
            if (cnt == 2 || cnt == 4) {
                sb.append(FileSystem.SEPARATOR_CHAR);
            }
            sb.append(chars[i]);
            cnt++;
        }
        return sb.toString();
    }

    private static String buildPropFilePath(PropertyId id) {
        String fileName;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(id.getName().getNamespaceURI().getBytes());
            md5.update(id.getName().getLocalName().getBytes());
            byte[] bytes = md5.digest();
            char[] chars = new char[32];
            for (int i = 0, j = 0; i < 16; i++) {
                chars[j++] = HEXDIGITS[(bytes[i] >> 4) & 0x0f];
                chars[j++] = HEXDIGITS[bytes[i] & 0x0f];
            }
            fileName = new String(chars);
        } catch (NoSuchAlgorithmException nsae) {
            // should never get here as MD5 should always be available in the JRE
            String msg = "MD5 not available: ";
            log.error(msg, nsae);
            throw new InternalError(msg + nsae);
        }
        return buildNodeFolderPath(id.getParentId()) + FileSystem.SEPARATOR + fileName;
    }

    private static String buildNodeFilePath(NodeId id) {
        return buildNodeFolderPath(id) + FileSystem.SEPARATOR + NODEFILENAME;
    }

    private static String buildNodeReferencesFilePath(NodeId id) {
        return buildNodeFolderPath(id) + FileSystem.SEPARATOR + NODEREFSFILENAME;
    }

    //---------------------------------------------------< PersistenceManager >
    /**
     * {@inheritDoc}
     */
    public void init(PMContext context) throws Exception {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }

        FileSystem wspFS = context.getFileSystem();
        itemStateFS = new BasedFileSystem(wspFS, "/data");

        /**
         * store BLOB data in local file system in a sub directory
         * of the workspace home directory
         */
        LocalFileSystem blobFS = new LocalFileSystem();
        blobFS.setRoot(new File(context.getHomeDir(), "blobs"));
        blobFS.init();
        this.blobFS = blobFS;
        blobStore = new FileSystemBLOBStore(blobFS);

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
            // close BLOB file system
            blobFS.close();
            blobFS = null;
            blobStore = null;
            /**
             * there's no need close the item state store because it
             * is based in the workspace's file system which is
             * closed by the repository
             */
        } finally {
            initialized = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized NodeState load(NodeId id)
            throws NoSuchItemStateException, ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String nodeFilePath = buildNodeFilePath(id);

        try {
            if (!itemStateFS.isFile(nodeFilePath)) {
                throw new NoSuchItemStateException(nodeFilePath);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to read node state: " + nodeFilePath;
            log.debug(msg);
            throw new ItemStateException(msg, fse);
        }

        try {
            BufferedInputStream in =
                    new BufferedInputStream(itemStateFS.getInputStream(nodeFilePath));
            try {
                NodeState state = createNew(id);
                Serializer.deserialize(state, in);
                return state;
            } catch (Exception e) {
                String msg = "failed to read node state: " + id;
                log.debug(msg);
                throw new ItemStateException(msg, e);
            } finally {
                in.close();
            }
        } catch (Exception e) {
            String msg = "failed to read node state: " + nodeFilePath;
            log.debug(msg);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized PropertyState load(PropertyId id)
            throws NoSuchItemStateException, ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String propFilePath = buildPropFilePath(id);

        try {
            if (!itemStateFS.isFile(propFilePath)) {
                throw new NoSuchItemStateException(propFilePath);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to read property state: " + propFilePath;
            log.debug(msg);
            throw new ItemStateException(msg, fse);
        }

        try {
            BufferedInputStream in =
                    new BufferedInputStream(itemStateFS.getInputStream(propFilePath));
            try {
                PropertyState state = createNew(id);
                Serializer.deserialize(state, in, blobStore);
                return state;
            } finally {
                in.close();
            }
        } catch (Exception e) {
            String msg = "failed to read property state: " + propFilePath;
            log.debug(msg);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized NodeReferences loadReferencesTo(NodeId id)
            throws NoSuchItemStateException, ItemStateException {

        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String refsFilePath = buildNodeReferencesFilePath(id);

        try {
            if (!itemStateFS.isFile(refsFilePath)) {
                throw new NoSuchItemStateException(id.toString());
            }
        } catch (FileSystemException fse) {
            String msg = "failed to load references: " + id;
            log.debug(msg);
            throw new ItemStateException(msg, fse);
        }

        try {
            BufferedInputStream in =
                    new BufferedInputStream(itemStateFS.getInputStream(refsFilePath));
            try {
                NodeReferences refs = new NodeReferences(id);
                Serializer.deserialize(refs, in);
                return refs;
            } finally {
                in.close();
            }
        } catch (Exception e) {
            String msg = "failed to load references: " + id;
            log.debug(msg);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void store(NodeState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String nodeFilePath = buildNodeFilePath(state.getNodeId());
        FileSystemResource nodeFile = new FileSystemResource(itemStateFS, nodeFilePath);
        try {
            nodeFile.makeParentDirs();
            BufferedOutputStream out = new BufferedOutputStream(nodeFile.getOutputStream());
            try {
                // serialize node state
                Serializer.serialize(state, out);
            } finally {
                out.close();
            }
        } catch (Exception e) {
            String msg = "failed to write node state: " + state.getNodeId();
            log.debug(msg);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void store(PropertyState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String propFilePath = buildPropFilePath(state.getPropertyId());
        FileSystemResource propFile = new FileSystemResource(itemStateFS, propFilePath);
        try {
            propFile.makeParentDirs();
            BufferedOutputStream out = new BufferedOutputStream(propFile.getOutputStream());
            try {
                // serialize property state
                Serializer.serialize(state, out, blobStore);
            } finally {
                out.close();
            }
        } catch (Exception e) {
            String msg = "failed to store property state: " + state.getParentId() + "/" + state.getName();
            log.debug(msg);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void store(NodeReferences refs) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String refsFilePath = buildNodeReferencesFilePath(refs.getTargetId());
        FileSystemResource refsFile = new FileSystemResource(itemStateFS, refsFilePath);
        try {
            refsFile.makeParentDirs();
            OutputStream out = new BufferedOutputStream(refsFile.getOutputStream());
            try {
                Serializer.serialize(refs, out);
            } finally {
                out.close();
            }
        } catch (Exception e) {
            String msg = "failed to store " + refs;
            log.debug(msg);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void destroy(NodeState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String nodeFilePath = buildNodeFilePath(state.getNodeId());
        FileSystemResource nodeFile = new FileSystemResource(itemStateFS, nodeFilePath);
        try {
            if (nodeFile.exists()) {
                // delete resource and prune empty parent folders
                nodeFile.delete(true);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to delete node state: " + state.getNodeId();
            log.debug(msg);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void destroy(PropertyState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        // delete binary values (stored as files)
        InternalValue[] values = state.getValues();
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                InternalValue val = values[i];
                if (val != null) {
                    val.deleteBinaryResource();
                }
            }
        }
        // delete property file
        String propFilePath = buildPropFilePath(state.getPropertyId());
        FileSystemResource propFile = new FileSystemResource(itemStateFS, propFilePath);
        try {
            if (propFile.exists()) {
                // delete resource and prune empty parent folders
                propFile.delete(true);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to delete property state: " + state.getParentId() + "/" + state.getName();
            log.debug(msg);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void destroy(NodeReferences refs) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String refsFilePath = buildNodeReferencesFilePath(refs.getTargetId());
        FileSystemResource refsFile = new FileSystemResource(itemStateFS, refsFilePath);
        try {
            if (refsFile.exists()) {
                // delete resource and prune empty parent folders
                refsFile.delete(true);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to delete " + refs;
            log.debug(msg);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean exists(PropertyId id) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            String propFilePath = buildPropFilePath(id);
            FileSystemResource propFile = new FileSystemResource(itemStateFS, propFilePath);
            return propFile.exists();
        } catch (FileSystemException fse) {
            String msg = "failed to check existence of item state: " + id;
            log.debug(msg);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean exists(NodeId id) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            String nodeFilePath = buildNodeFilePath(id);
            FileSystemResource nodeFile = new FileSystemResource(itemStateFS, nodeFilePath);
            return nodeFile.exists();
        } catch (FileSystemException fse) {
            String msg = "failed to check existence of item state: " + id;
            log.error(msg, fse);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean existsReferencesTo(NodeId id)
            throws ItemStateException {

        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            String refsFilePath = buildNodeReferencesFilePath(id);
            FileSystemResource refsFile = new FileSystemResource(itemStateFS, refsFilePath);
            return refsFile.exists();
        } catch (FileSystemException fse) {
            String msg = "failed to check existence of references: " + id;
            log.debug(msg);
            throw new ItemStateException(msg, fse);
        }
    }
}
