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
package org.apache.jackrabbit.core.persistence.bundle.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.core.persistence.util.BLOBStore;
import org.apache.jackrabbit.core.persistence.util.ResourceBasedBLOBStore;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.uuid.UUID;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.PropertyType;

/**
 * This Class implements relatively efficient serialization methods for item
 * states.
 */
public class ItemStateBinding {

    /** the cvs/svn id */
    static final String CVS_ID = "$URL$ $Rev$ $Date$";

    /**
     * default logger
     */
    private static Logger log = LoggerFactory.getLogger(ItemStateBinding.class);

    /**
     * serialization version 1
     */
    public static final int VERSION_1 = 1;

    /**
     * current version
     */
    public static final int VERSION_CURRENT = VERSION_1;

    /**
     * the namespace index
     */
    protected final StringIndex nsIndex;

    /**
     * the name index
     */
    protected final StringIndex nameIndex;

    /**
     * the blob store
     */
    protected final BLOBStore blobStore;

    /**
     * minimum size of binaries to store in blob store
     */
    protected long minBlobSize = 0x4000; // 16k

    /**
     * the error handling
     */
    protected final ErrorHandling errorHandling;

    /**
     * Data store for binary properties.
     */
    protected final DataStore dataStore;

    /**
     * Creates a new item state binding
     *
     * @param errorHandling the error handing configuration
     * @param blobStore the blobstore
     * @param nsIndex the namespace index
     * @param nameIndex the name index
     */
    public ItemStateBinding(ErrorHandling errorHandling,
                            BLOBStore blobStore,
                            StringIndex nsIndex, StringIndex nameIndex, DataStore dataStore) {
        this.errorHandling = errorHandling;
        this.nsIndex = nsIndex;
        this.nameIndex = nameIndex;
        this.blobStore = blobStore;
        this.dataStore = dataStore;
    }

    /**
     * Returns the minimum blob size
     * @see #setMinBlobSize(long) for details.
     * @return the minimum blob size
     */
    public long getMinBlobSize() {
        return minBlobSize;
    }

    /**
     * Sets the minimum blob size. Binary values that are shorted than this given
     * size will be inlined in the serialization stream, binary value that are
     * longer, will be stored in the blob store. default is 4k.
     *
     * @param minBlobSize the minimum blob size.
     */
    public void setMinBlobSize(long minBlobSize) {
        this.minBlobSize = minBlobSize;
    }

    /**
     * Deserializes a <code>NodeReferences</code> from the data input stream.
     *
     * @param in the input stream
     * @param id the id of the nodereference to deserialize
     * @param pMgr the persistence manager
     * @return the node references
     * @throws IOException in an I/O error occurs.
     */
    public NodeReferences readState(DataInputStream in, NodeReferencesId id,
                                    PersistenceManager pMgr)
            throws IOException {
        NodeReferences state = new NodeReferences(id);
        int count = in.readInt();   // count & version
        // int version = (count >> 24) | 0x0ff;
        count &= 0x00ffffff;
        for (int i = 0; i < count; i++) {
            state.addReference(readPropertyId(in));    // propertyId
        }
        return state;
    }

    /**
     * Serializes a <code>NodeReferences</code> to the data output stream.
     *
     * @param out the output stream
     * @param state the state to write.
     * @throws IOException in an I/O error occurs.
     */
    public void writeState(DataOutputStream out, NodeReferences state)
            throws IOException {
        // references
        Collection c = state.getReferences();
        out.writeInt(c.size() | (VERSION_CURRENT << 24)); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            PropertyId propId = (PropertyId) iter.next();
            writePropertyId(out, propId);
        }
    }

    /**
     * Deserializes a <code>NodeState</code> from the data input stream.
     *
     * @param in the input streaam
     * @param id the id of the nodestate to read
     * @param pMgr the persistence manager
     * @return the node state
     * @throws IOException in an I/O error occurs.
     */
    public NodeState readState(DataInputStream in, NodeId id,
                               PersistenceManager pMgr)
            throws IOException {
        NodeState state = pMgr.createNew(id);
        // primaryType & version
        int index = in.readInt();
        int version = (index >> 24) & 0x0ff;
        String uri = nsIndex.indexToString(index & 0x0ffffff);
        String local = in.readUTF();
        state.setNodeTypeName(new QName(uri, local));

        // parentUUID
        state.setParentId(readID(in));
        // definitionId
        state.setDefinitionId(NodeDefId.valueOf(in.readUTF()));

        // mixin types
        int count = in.readInt();   // count
        Set set = new HashSet(count);
        for (int i = 0; i < count; i++) {
            set.add(readQName(in)); // name
        }
        if (set.size() > 0) {
            state.setMixinTypeNames(set);
        }
        // properties (names)
        count = in.readInt();   // count
        for (int i = 0; i < count; i++) {
            state.addPropertyName(readIndexedQName(in)); // name
        }
        // child nodes (list of name/uuid pairs)
        count = in.readInt();   // count
        for (int i = 0; i < count; i++) {
            QName name = readQName(in);
            NodeId parentId = readID(in);
            state.addChildNodeEntry(name, parentId);
        }

        if (version >= VERSION_1) {
            state.setModCount(readModCount(in));
        }
        return state;
    }

    /**
     * Serializes a <code>NodeState</code> to the data output stream
     *
     * @param out the output stream
     * @param state the state to write
     * @throws IOException in an I/O error occurs.
     */
    public void writeState(DataOutputStream out, NodeState state)
            throws IOException {
        // primaryType & version
        out.writeInt((VERSION_CURRENT << 24) | nsIndex.stringToIndex(state.getNodeTypeName().getNamespaceURI()));
        out.writeUTF(state.getNodeTypeName().getLocalName());
        // parentUUID
        writeID(out, state.getParentId());
        // definitionId
        out.writeUTF(state.getDefinitionId().toString());
        // mixin types
        Collection c = state.getMixinTypeNames();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            writeQName(out, (QName) iter.next());
        }
        // properties (names)
        c = state.getPropertyNames();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            QName pName = (QName) iter.next();
            writeIndexedQName(out, pName);
        }
        // child nodes (list of name/uuid pairs)
        c = state.getChildNodeEntries();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            NodeState.ChildNodeEntry entry = (NodeState.ChildNodeEntry) iter.next();
            writeQName(out, entry.getName());   // name
            writeID(out, entry.getId());  // uuid
        }
        writeModCount(out, state.getModCount());
    }

    /**
     * Deserializes a <code>PropertyState</code> from the data input stream.
     *
     * @param in the input stream
     * @param id the id for the new property state
     * @param pMgr the persistence manager
     * @return the property state
     * @throws IOException in an I/O error occurs.
     */
    public PropertyState readState(DataInputStream in, PropertyId id,
                                   PersistenceManager pMgr)
            throws IOException {
        PropertyState state = pMgr.createNew(id);
        // type and modcount
        int type = in.readInt();
        state.setModCount((short) ((type >> 16) & 0x0ffff));
        type &= 0x0ffff;
        state.setType(type);
        // multiValued
        state.setMultiValued(in.readBoolean());
        // definitionId
        state.setDefinitionId(PropDefId.valueOf(in.readUTF()));
        // values
        int count = in.readInt();   // count
        InternalValue[] values = new InternalValue[count];
        for (int i = 0; i < count; i++) {
            InternalValue val;
            switch (type) {
                case PropertyType.BINARY:
                    int size = in.readInt();
                    if (InternalValue.USE_DATA_STORE && size == -2) {
                        val = InternalValue.create(dataStore, in.readUTF());
                    } else if (size == -1) {
                        String s = in.readUTF();
                        try {
                            if (blobStore instanceof ResourceBasedBLOBStore) {
                                val = InternalValue.create(
                                        ((ResourceBasedBLOBStore) blobStore).getResource(s));
                            } else {
                                val = InternalValue.create(blobStore.get(s));
                            }
                        } catch (IOException e) {
                            if (errorHandling.ignoreMissingBlobs()) {
                                log.warn("Ignoring error while reading blob-resource: " + e);
                                val = InternalValue.create(new byte[0]);
                            } else {
                                throw e;
                            }
                        } catch (Exception e) {
                            throw new IOException("Unable to create property value: " + e.toString());
                        }
                    } else {
                        // short values into memory
                        byte[] data = new byte[size];
                        in.readFully(data);
                        val = InternalValue.create(data);
                    }
                    break;
                case PropertyType.DOUBLE:
                    val = InternalValue.create(in.readDouble());
                    break;
                case PropertyType.LONG:
                    val = InternalValue.create(in.readLong());
                    break;
                case PropertyType.BOOLEAN:
                    val = InternalValue.create(in.readBoolean());
                    break;
                case PropertyType.NAME:
                    val = InternalValue.create(readQName(in));
                    break;
                case PropertyType.REFERENCE:
                    val = InternalValue.create(readUUID(in));
                    break;
                default:
                    // because writeUTF(String) has a size limit of 64k,
                    // Strings are serialized as <length><byte[]>
                    int len = in.readInt();
                    byte[] bytes = new byte[len];
                    int pos = 0;
                    while (pos < len) {
                        pos += in.read(bytes, pos, len - pos);
                    }
                    val = InternalValue.valueOf(new String(bytes, "UTF-8"), type);
            }
            values[i] = val;
        }
        state.setValues(values);
        return state;
    }

    /**
     * Serializes a <code>PropertyState</code> to the data output stream
     * @param out the output stream
     * @param state the property state to write
     * @throws IOException in an I/O error occurs.
     */
    public void writeState(DataOutputStream out, PropertyState state)
            throws IOException {
        // type & mod count
        out.writeInt(state.getType() | (state.getModCount() << 16));
        // multiValued
        out.writeBoolean(state.isMultiValued());
        // definitionId
        out.writeUTF(state.getDefinitionId().toString());
        // values
        InternalValue[] values = state.getValues();
        out.writeInt(values.length); // count
        for (int i = 0; i < values.length; i++) {
            InternalValue val = values[i];
            switch (state.getType()) {
                case PropertyType.BINARY:
                    try {
                        if (InternalValue.USE_DATA_STORE && dataStore != null) {
                            out.writeInt(-2);
                            out.writeUTF(val.toString());
                            break;
                        }
                        // special handling required for binary value:
                        // spool binary value to file in blob store
                        BLOBFileValue blobVal = val.getBLOBFileValue();
                        long size = blobVal.getLength();
                        if (size > minBlobSize) {
                            out.writeInt(-1);
                            InputStream in = blobVal.getStream();
                            String blobId = blobStore.createId((PropertyId) state.getId(), i);
                            try {
                                blobStore.put(blobId, in, size);
                            } finally {
                                try {
                                    in.close();
                                } catch (IOException e) {
                                    // ignore
                                }
                            }
                            // store id of blob as property value
                            out.writeUTF(blobId);   // value
                            // replace value instance with value
                            // backed by resource in blob store and delete temp file
                            if (blobStore instanceof ResourceBasedBLOBStore) {
                                values[i] = InternalValue.create(((ResourceBasedBLOBStore) blobStore).getResource(blobId));
                            } else {
                                values[i] = InternalValue.create(blobStore.get(blobId));
                            }
                            blobVal.discard();
                        } else {
                            // delete evt. blob
                            out.writeInt((int) size);
                            byte[] data = new byte[(int) size];
                            InputStream in = blobVal.getStream();
                            try {
                                int pos = 0;
                                while (pos < size) {
                                    int n = in.read(data, pos, (int) size - pos);
                                    if (n < 0) {
                                        throw new EOFException();
                                    }
                                    pos += n;
                                }
                            } finally {
                                try {
                                    in.close();
                                } catch (IOException e) {
                                    // ignore
                                }
                            }
                            out.write(data, 0, data.length);
                            // replace value instance with value
                            // backed by resource in blob store and delete temp file
                            values[i] = InternalValue.create(data);
                            blobVal.discard();
                        }
                    } catch (IOException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new IOException("Error converting: " + e.toString());
                    }
                    break;
                case PropertyType.DOUBLE:
                    out.writeDouble(val.getDouble());
                    break;
                case PropertyType.LONG:
                    out.writeLong(val.getLong());
                    break;
                case PropertyType.BOOLEAN:
                    out.writeBoolean(val.getBoolean());
                    break;
                case PropertyType.NAME:
                    writeQName(out, val.getQName());
                    break;
                case PropertyType.REFERENCE:
                    writeUUID(out, val.getUUID());
                    break;
                default:
                    // because writeUTF(String) has a size limit of 64k,
                    // we're using write(byte[]) instead
                    byte[] bytes = val.toString().getBytes("UTF-8");
                    out.writeInt(bytes.length); // lenght of byte[]
                    out.write(bytes);   // byte[]
            }
        }
    }

    /**
     * Deserializes a UUID
     * @param in the input stream
     * @return the uuid
     * @throws IOException in an I/O error occurs.
     */
    public UUID readUUID(DataInputStream in) throws IOException {
        if (in.readBoolean()) {
            byte[] bytes = new byte[16];
            int pos = 0;
            while (pos < 16) {
                pos += in.read(bytes, pos, 16 - pos);
            }
            return new UUID(bytes);
        } else {
            return null;
        }
    }

    /**
     * Serializes a UUID
     * @param out the output stream
     * @param uuid the uuid
     * @throws IOException in an I/O error occurs.
     */
    public void writeUUID(DataOutputStream out, String uuid) throws IOException {
        if (uuid == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.write(UUID.fromString(uuid).getRawBytes());
        }
    }

    /**
     * Deserializes a NodeID
     * @param in the input stream
     * @return the uuid
     * @throws IOException in an I/O error occurs.
     */
    public NodeId readID(DataInputStream in) throws IOException {
        if (in.readBoolean()) {
            byte[] bytes = new byte[16];
            int pos = 0;
            while (pos < 16) {
                pos += in.read(bytes, pos, 16 - pos);
            }
            return new NodeId(new UUID(bytes));
        } else {
            return null;
        }
    }

    /**
     * Serializes a node id
     * @param out the output stream
     * @param id the id
     * @throws IOException in an I/O error occurs.
     */
    public void writeID(DataOutputStream out, NodeId id) throws IOException {
        if (id == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.write(id.getUUID().getRawBytes());
        }
    }

    /**
     * Serializes a UUID
     * @param out the output stream
     * @param uuid the uuid
     * @throws IOException in an I/O error occurs.
     */
    public void writeUUID(DataOutputStream out, UUID uuid) throws IOException {
        if (uuid == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.write(uuid.getRawBytes());
        }
    }

    /**
     * Deserializes a QName
     * @param in the input stream
     * @return the qname
     * @throws IOException in an I/O error occurs.
     */
    public QName readQName(DataInputStream in) throws IOException {
        String uri = nsIndex.indexToString(in.readInt());
        String local = in.readUTF();
        return new QName(uri, local);
    }

    /**
     * Deserializes a mod-count
     * @param in the input stream
     * @return the mod count
     * @throws IOException in an I/O error occurs.
     */
    public short readModCount(DataInputStream in) throws IOException {
        return in.readShort();
    }

    /**
     * Sersializes a mod-count
     * @param out the output stream
     * @param modCount the mod count
     * @throws IOException in an I/O error occurs.
     */
    public void writeModCount(DataOutputStream out, short modCount) throws IOException {
        out.writeShort(modCount);
    }

    /**
     * Serializes a QName
     * @param out the output stream
     * @param name the name
     * @throws IOException in an I/O error occurs.
     */
    public void writeQName(DataOutputStream out, QName name) throws IOException {
        out.writeInt(nsIndex.stringToIndex(name.getNamespaceURI()));
        out.writeUTF(name.getLocalName());
    }

    /**
     * Deserializes an indexed QName
     * @param in the input stream
     * @return the qname
     * @throws IOException in an I/O error occurs.
     */
    public QName readIndexedQName(DataInputStream in) throws IOException {
        int index = in.readInt();
        if (index < 0) {
            return null;
        } else {
            String uri = nsIndex.indexToString(index);
            String local = nameIndex.indexToString(in.readInt());
            return new QName(uri, local);
        }
    }

    /**
     * Serializes a indexed QName
     * @param out the output stream
     * @param name the name
     * @throws IOException in an I/O error occurs.
     */
    public void writeIndexedQName(DataOutputStream out, QName name) throws IOException {
        if (name == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(nsIndex.stringToIndex(name.getNamespaceURI()));
            out.writeInt(nameIndex.stringToIndex(name.getLocalName()));
        }
    }

    /**
     * Serializes a PropertyId
     * @param out the output stream
     * @param id the id
     * @throws IOException in an I/O error occurs.
     */
    public void writePropertyId(DataOutputStream out, PropertyId id) throws IOException {
        writeID(out, id.getParentId());
        writeQName(out, id.getName());
    }

    /**
     * Deserializes a PropertyId
     * @param in the input stream
     * @return the property id
     * @throws IOException in an I/O error occurs.
     */
    public PropertyId readPropertyId(DataInputStream in) throws IOException {
        UUID uuid = readUUID(in);
        QName name = readQName(in);
        return new PropertyId(new NodeId(uuid), name);
    }
}
