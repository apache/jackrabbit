/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
package org.apache.jackrabbit.core.state.obj;

import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.fs.*;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.state.*;
import org.apache.log4j.Logger;

import javax.jcr.PropertyType;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * <code>ObjectPersistenceManager</code> ...
 */
public class ObjectPersistenceManager implements BLOBStore, PersistenceManager {

    private static Logger log = Logger.getLogger(ObjectPersistenceManager.class);

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

    /**
     * Creates a new <code>ObjectPersistenceManager</code> instance.
     */
    public ObjectPersistenceManager() {
        initialized = false;
    }

    private static String buildNodeFolderPath(String uuid) {
        StringBuffer sb = new StringBuffer();
        char[] chars = uuid.toCharArray();
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

    private static String buildPropFilePath(String parentUUID, QName propName) {
        String fileName;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(propName.getNamespaceURI().getBytes());
            md5.update(propName.getLocalName().getBytes());
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
            log.fatal(msg, nsae);
            throw new InternalError(msg + nsae);
        }
        return buildNodeFolderPath(parentUUID) + FileSystem.SEPARATOR + fileName;
    }

    private static String buildBlobFilePath(String parentUUID, QName propName, int i) {
        return buildNodeFolderPath(parentUUID) + FileSystem.SEPARATOR
                + FileSystemPathUtil.escapeName(propName.toString()) + "." + i + ".bin";
    }

    private static String buildNodeFilePath(String uuid) {
        return buildNodeFolderPath(uuid) + FileSystem.SEPARATOR + NODEFILENAME;
    }

    private static String buildNodeReferencesFilePath(String uuid) {
        return buildNodeFolderPath(uuid) + FileSystem.SEPARATOR + NODEREFSFILENAME;
    }

    //------------------------------------------------< static helper methods >
    /**
     * Serializes the specified <code>state</code> object to the given
     * <code>stream</code>.
     *
     * @param state  <code>state</code> to serialize
     * @param stream the stream where the <code>state</code> should be serialized to
     * @throws Exception if an error occurs during the serialization
     * @see #deserialize(PersistentNodeState, InputStream)
     */
    public static void serialize(PersistentNodeState state, OutputStream stream)
            throws Exception {
        DataOutputStream out = new DataOutputStream(stream);
        // uuid
        out.writeUTF(state.getUUID());
        // primaryType
        out.writeUTF(state.getNodeTypeName().toString());
        // parentUUID
        out.writeUTF(state.getParentUUID() == null ? "" : state.getParentUUID());
        // definitionId
        out.writeUTF(state.getDefinitionId().toString());
        // parentUUIDs
        Collection c = state.getParentUUIDs();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            out.writeUTF((String) iter.next()); // uuid
        }
        // mixin types
        c = state.getMixinTypeNames();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            out.writeUTF(iter.next().toString());   // name
        }
        // properties (names)
        c = state.getPropertyEntries();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            NodeState.PropertyEntry entry = (NodeState.PropertyEntry) iter.next();
            out.writeUTF(entry.getName().toString());   // name
        }
        // child nodes (list of name/uuid pairs)
        c = state.getChildNodeEntries();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
            out.writeUTF(entry.getName().toString());   // name
            out.writeUTF(entry.getUUID());  // uuid
        }
    }

    /**
     * Deserializes a <code>state</code> object from the given <code>stream</code>.
     *
     * @param state  <code>state</code> to deserialize
     * @param stream the stream where the <code>state</code> should be deserialized from
     * @throws Exception if an error occurs during the deserialization
     * @see #serialize(PersistentNodeState, OutputStream)
     */
    public static void deserialize(PersistentNodeState state, InputStream stream)
            throws Exception {
        DataInputStream in = new DataInputStream(stream);
        // check uuid
        String s = in.readUTF();
        if (!state.getUUID().equals(s)) {
            String msg = "invalid serialized state: uuid mismatch";
            log.error(msg);
            throw new ItemStateException(msg);
        }

        // deserialize node state

        // primaryType
        s = in.readUTF();
        state.setNodeTypeName(QName.valueOf(s));
        // parentUUID
        s = in.readUTF();
        if (s.length() > 0) {
            state.setParentUUID(s);
        }
        // definitionId
        s = in.readUTF();
        state.setDefinitionId(NodeDefId.valueOf(s));
        // parentUUIDs
        int count = in.readInt();   // count
        List list = new ArrayList(count);
        for (int i = 0; i < count; i++) {
            list.add(in.readUTF()); // uuid
        }
        if (list.size() > 0) {
            state.setParentUUIDs(list);
        }
        // mixin types
        count = in.readInt();   // count
        Set set = new HashSet(count);
        for (int i = 0; i < count; i++) {
            set.add(QName.valueOf(in.readUTF())); // name
        }
        if (set.size() > 0) {
            state.setMixinTypeNames(set);
        }
        // properties (names)
        count = in.readInt();   // count
        for (int i = 0; i < count; i++) {
            state.addPropertyEntry(QName.valueOf(in.readUTF())); // name
        }
        // child nodes (list of name/uuid pairs)
        count = in.readInt();   // count
        for (int i = 0; i < count; i++) {
            QName name = QName.valueOf(in.readUTF());    // name
            String s1 = in.readUTF();   // uuid
            state.addChildNodeEntry(name, s1);
        }
    }

    /**
     * Serializes the specified <code>state</code> object to the given
     * <code>stream</code>.
     *
     * @param state     <code>state</code> to serialize
     * @param stream    the stream where the <code>state</code> should be serialized to
     * @param blobStore handler for blob data
     * @throws Exception if an error occurs during the serialization
     * @see #deserialize(PersistentPropertyState, InputStream, BLOBStore)
     */
    public static void serialize(PersistentPropertyState state,
                                 OutputStream stream,
                                 BLOBStore blobStore)
            throws Exception {
        DataOutputStream out = new DataOutputStream(stream);
        // type
        out.writeInt(state.getType());
        // definitionId
        out.writeUTF(state.getDefinitionId().toString());
        // values
        InternalValue[] values = state.getValues();
        out.writeInt(values.length); // count
        for (int i = 0; i < values.length; i++) {
            InternalValue val = values[i];
            if (state.getType() == PropertyType.BINARY) {
                // special handling required for binary value:
                // spool binary value to file in blob store
                BLOBFileValue blobVal = (BLOBFileValue) val.internalValue();
                String blobId = blobStore.put((PropertyId) state.getId(), i, blobVal.getStream(), blobVal.getLength());
                // store id of blob as property value
                out.writeUTF(blobId);   // value
                // replace value instance with value
                // backed by resource in blob store and delete temp file
                values[i] = InternalValue.create(blobStore.get(blobId));
                if (blobVal.isTempFile()) {
                    blobVal.delete();
                    blobVal = null;
                }
            } else {
                out.writeUTF(val.toString());   // value
            }
        }
    }

    /**
     * Deserializes a <code>state</code> object from the given <code>stream</code>.
     *
     * @param state     <code>state</code> to deserialize
     * @param stream    the stream where the <code>state</code> should be deserialized from
     * @param blobStore handler for blob data
     * @throws Exception if an error occurs during the deserialization
     * @see #serialize(PersistentPropertyState, OutputStream, BLOBStore)
     */
    public static void deserialize(PersistentPropertyState state,
                                   InputStream stream,
                                   BLOBStore blobStore)
            throws Exception {
        DataInputStream in = new DataInputStream(stream);

        // type
        int type = in.readInt();
        state.setType(type);
        // definitionId
        String s = in.readUTF();
        state.setDefinitionId(PropDefId.valueOf(s));
        // values
        int count = in.readInt();   // count
        InternalValue[] values = new InternalValue[count];
        for (int i = 0; i < count; i++) {
            InternalValue val;
            s = in.readUTF();   // value
            if (type == PropertyType.BINARY) {
                // special handling required for binary value:
                // the value stores the id of the blob resource in the blob store
                val = InternalValue.create(blobStore.get(s));
            } else {
                val = InternalValue.valueOf(s, type);
            }
            values[i] = val;
        }
        state.setValues(values);
    }

    /**
     * Serializes the specified <code>NodeReferences</code> object to the given
     * <code>stream</code>.
     *
     * @param refs   object to serialize
     * @param stream the stream where the object should be serialized to
     * @throws IOException if an error occurs during the serialization
     * @see #deserialize(NodeReferences, InputStream)
     */
    public static void serialize(NodeReferences refs, OutputStream stream)
            throws IOException {
        DataOutputStream out = new DataOutputStream(stream);

        // references
        Collection c = refs.getReferences();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            PropertyId propId = (PropertyId) iter.next();
            out.writeUTF(propId.toString());   // propertyId
        }
    }

    /**
     * Deserializes a <code>NodeReferences</code> object from the given
     * <code>stream</code>.
     *
     * @param refs   object to deserialize
     * @param stream the stream where the object should be deserialized from
     * @throws Exception if an error occurs during the deserialization
     * @see #serialize(NodeReferences, OutputStream)
     */
    public static void deserialize(NodeReferences refs, InputStream stream)
            throws Exception {
        DataInputStream in = new DataInputStream(stream);

        refs.clearAllReferences();

        // references
        int count = in.readInt();   // count
        for (int i = 0; i < count; i++) {
            refs.addReference(PropertyId.valueOf(in.readUTF()));    // propertyId
        }
    }

    //------------------------------------------------------------< BLOBStore >
    /**
     * @see BLOBStore#get
     */
    public FileSystemResource get(String blobId) throws Exception {
        return new FileSystemResource(blobFS, blobId);
    }

    /**
     * @see BLOBStore#put
     */
    public String put(PropertyId id, int index, InputStream in, long size) throws Exception {
        String path = buildBlobFilePath(id.getParentUUID(), id.getName(), index);
        OutputStream out = null;
        FileSystemResource internalBlobFile = new FileSystemResource(blobFS, path);
        internalBlobFile.makeParentDirs();
        try {
            out = new BufferedOutputStream(internalBlobFile.getOutputStream());
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            out.close();
        }
        return path;
    }

    /**
     * @see BLOBStore#remove
     */
    public boolean remove(String blobId) throws Exception {
        FileSystemResource res = new FileSystemResource(blobFS, blobId);
        if (!res.exists()) {
            return false;
        }
        // delete resource and prune empty parent folders
        res.delete(true);
        return true;
    }

    //---------------------------------------------------< PersistenceManager >
    /**
     * @see PersistenceManager#init
     */
    public void init(WorkspaceConfig wspConfig) throws Exception {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }

        FileSystem wspFS = wspConfig.getFileSystem();
        itemStateFS = new BasedFileSystem(wspFS, "/data");

        /**
         * store blob's in local file system in a sub directory
         * of the workspace home directory
         */
        LocalFileSystem blobFS = new LocalFileSystem();
        blobFS.setPath(wspConfig.getHomeDir() + "/blobs");
        blobFS.init();
        this.blobFS = blobFS;

        initialized = true;
    }

    /**
     * @see PersistenceManager#close
     */
    public synchronized void close() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            // close blob store
            blobFS.close();
            blobFS = null;
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
     * @see PersistenceManager#load(PersistentNodeState)
     */
    public synchronized void load(PersistentNodeState state)
            throws NoSuchItemStateException, ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String nodeFilePath = buildNodeFilePath(state.getUUID());

        try {
            if (!itemStateFS.isFile(nodeFilePath)) {
                throw new NoSuchItemStateException(state.getId().toString());
            }
        } catch (FileSystemException fse) {
            String msg = "failed to read node state: " + state.getId();
            log.error(msg, fse);
            throw new ItemStateException(msg, fse);
        }

        try {
            BufferedInputStream in =
                    new BufferedInputStream(itemStateFS.getInputStream(nodeFilePath));
            try {
                deserialize(state, in);
                return;
            } catch (Exception e) {
                String msg = "failed to read node state: " + state.getId();
                log.error(msg, e);
                throw new ItemStateException(msg, e);
            } finally {
                in.close();
            }
        } catch (Exception e) {
            String msg = "failed to read node state: " + state.getId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * @see PersistenceManager#load(PersistentPropertyState)
     */
    public synchronized void load(PersistentPropertyState state)
            throws NoSuchItemStateException, ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String propFilePath = buildPropFilePath(state.getParentUUID(), state.getName());

        try {
            if (!itemStateFS.isFile(propFilePath)) {
                throw new NoSuchItemStateException(state.getId().toString());
            }
        } catch (FileSystemException fse) {
            String msg = "failed to read property state: " + state.getId();
            log.error(msg, fse);
            throw new ItemStateException(msg, fse);
        }

        try {
            BufferedInputStream in =
                    new BufferedInputStream(itemStateFS.getInputStream(propFilePath));
            try {
                deserialize(state, in, this);
                return;
            } finally {
                in.close();
            }
        } catch (Exception e) {
            String msg = "failed to read property state: " + state.getId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * @see PersistenceManager#store
     */
    public synchronized void store(PersistentNodeState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String uuid = state.getUUID();
        String nodeFilePath = buildNodeFilePath(uuid);
        FileSystemResource nodeFile = new FileSystemResource(itemStateFS, nodeFilePath);
        try {
            nodeFile.makeParentDirs();
            BufferedOutputStream out = new BufferedOutputStream(nodeFile.getOutputStream());
            try {
                // serialize node state
                serialize(state, out);
                return;
            } finally {
                out.close();
            }
        } catch (Exception e) {
            String msg = "failed to write node state: " + uuid;
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * @see PersistenceManager#store
     */
    public synchronized void store(PersistentPropertyState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String propFilePath = buildPropFilePath(state.getParentUUID(), state.getName());
        FileSystemResource propFile = new FileSystemResource(itemStateFS, propFilePath);
        try {
            propFile.makeParentDirs();
            BufferedOutputStream out = new BufferedOutputStream(propFile.getOutputStream());
            try {
                // serialize property state
                serialize(state, out, this);
                return;
            } finally {
                out.close();
            }
        } catch (Exception e) {
            String msg = "failed to store property state: " + state.getParentUUID() + "/" + state.getName();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * @see PersistenceManager#destroy
     */
    public synchronized void destroy(PersistentNodeState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String uuid = state.getUUID();
        String nodeFilePath = buildNodeFilePath(uuid);
        FileSystemResource nodeFile = new FileSystemResource(itemStateFS, nodeFilePath);
        try {
            if (nodeFile.exists()) {
                // delete resource and prune empty parent folders
                nodeFile.delete(true);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to delete node state: " + uuid;
            log.error(msg, fse);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * @see PersistenceManager#destroy
     */
    public synchronized void destroy(PersistentPropertyState state) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        // delete binary values (stored as files)
        InternalValue[] values = state.getValues();
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                InternalValue val = values[i];
                if (val != null) {
                    if (val.getType() == PropertyType.BINARY) {
                        BLOBFileValue blobVal = (BLOBFileValue) val.internalValue();
                        // delete blob file and prune empty parent folders
                        blobVal.delete(true);
                    }
                }
            }
        }
        // delete property file
        String propFilePath = buildPropFilePath(state.getParentUUID(), state.getName());
        FileSystemResource propFile = new FileSystemResource(itemStateFS, propFilePath);
        try {
            if (propFile.exists()) {
                // delete resource and prune empty parent folders
                propFile.delete(true);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to delete property state: " + state.getParentUUID() + "/" + state.getName();
            log.error(msg, fse);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * @see PersistenceManager#load(NodeReferences)
     */
    public void load(NodeReferences refs)
            throws NoSuchItemStateException, ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String refsFilePath = buildNodeReferencesFilePath(refs.getTargetId().getUUID());

        try {
            if (!itemStateFS.isFile(refsFilePath)) {
                throw new NoSuchItemStateException(refs.getTargetId().toString());
            }
        } catch (FileSystemException fse) {
            String msg = "failed to load references: " + refs.getTargetId();
            log.error(msg, fse);
            throw new ItemStateException(msg, fse);
        }

        try {
            BufferedInputStream in =
                    new BufferedInputStream(itemStateFS.getInputStream(refsFilePath));
            try {
                deserialize(refs, in);
                return;
            } finally {
                in.close();
            }
        } catch (Exception e) {
            String msg = "failed to load references: " + refs.getTargetId();
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * @see PersistenceManager#store(NodeReferences)
     */
    public void store(NodeReferences refs) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String uuid = refs.getTargetId().getUUID();
        String refsFilePath = buildNodeReferencesFilePath(uuid);
        FileSystemResource refsFile = new FileSystemResource(itemStateFS, refsFilePath);
        try {
            refsFile.makeParentDirs();
            OutputStream out = new BufferedOutputStream(refsFile.getOutputStream());
            try {
                serialize(refs, out);
            } finally {
                out.close();
            }
        } catch (Exception e) {
            String msg = "failed to store references: " + uuid;
            log.error(msg, e);
            throw new ItemStateException(msg, e);
        }
    }

    /**
     * @see PersistenceManager#destroy(NodeReferences)
     */
    public void destroy(NodeReferences refs) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        String uuid = refs.getTargetId().getUUID();
        String refsFilePath = buildNodeReferencesFilePath(uuid);
        FileSystemResource refsFile = new FileSystemResource(itemStateFS, refsFilePath);
        try {
            if (refsFile.exists()) {
                // delete resource and prune empty parent folders
                refsFile.delete(true);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to delete references: " + uuid;
            log.error(msg, fse);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * @see PersistenceManager#exists(org.apache.jackrabbit.core.ItemId id)
     */
    public boolean exists(ItemId id) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            if (id.denotesNode()) {
                NodeId nodeId = (NodeId) id;
                String nodeFilePath = buildNodeFilePath(nodeId.getUUID());
                FileSystemResource nodeFile = new FileSystemResource(itemStateFS, nodeFilePath);
                return nodeFile.exists();
            } else {
                PropertyId propId = (PropertyId) id;
                String propFilePath = buildPropFilePath(propId.getParentUUID(), propId.getName());
                FileSystemResource propFile = new FileSystemResource(itemStateFS, propFilePath);
                return propFile.exists();
            }
        } catch (FileSystemException fse) {
            String msg = "failed to check existence of item state: " + id;
            log.error(msg, fse);
            throw new ItemStateException(msg, fse);
        }
    }

    /**
     * @see PersistenceManager#referencesExist(NodeId targetId)
     */
    public boolean referencesExist(NodeId targetId) throws ItemStateException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        try {
            String uuid = targetId.getUUID();
            String refsFilePath = buildNodeReferencesFilePath(uuid);
            FileSystemResource refsFile = new FileSystemResource(itemStateFS, refsFilePath);
            return refsFile.exists();
        } catch (FileSystemException fse) {
            String msg = "failed to check existence of references: " + targetId;
            log.error(msg, fse);
            throw new ItemStateException(msg, fse);
        }
    }
}
