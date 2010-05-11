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

import org.apache.jackrabbit.core.persistence.util.BLOBStore;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.util.StringIndex;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This Class implements relatively efficient serialization methods for item
 * states.
 */
public class ItemStateBinding {

    /**
     * serialization version 1
     */
    public static final int VERSION_1 = 1;

    /**
     * serialization version 2
     */
    public static final int VERSION_2 = 2;

    /**
     * current version
     */
    public static final int VERSION_CURRENT = VERSION_2;

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
     * @param dataStore the data store
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
     * Returns the blob store that is assosiated with this binding.
     * @return the blob store
     */
    public BLOBStore getBlobStore() {
        return blobStore;
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
        state.setNodeTypeName(NameFactoryImpl.getInstance().create(uri, local));

        // parentUUID
        state.setParentId(readID(in));
        // definitionId
        in.readUTF();

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
            Name name = readQName(in);
            NodeId parentId = readID(in);
            state.addChildNodeEntry(name, parentId);
        }

        if (version >= VERSION_1) {
            state.setModCount(readModCount(in));
        }
        if (version >= VERSION_2) {
            // shared set (list of parent uuids)
            count = in.readInt();   // count
            for (int i = 0; i < count; i++) {
                state.addShare(readID(in));
            }
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
        out.writeUTF("");
        // mixin types
        Collection c = state.getMixinTypeNames();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            writeQName(out, (Name) iter.next());
        }
        // properties (names)
        c = state.getPropertyNames();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            Name pName = (Name) iter.next();
            writeIndexedQName(out, pName);
        }
        // child nodes (list of name/uuid pairs)
        c = state.getChildNodeEntries();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            ChildNodeEntry entry = (ChildNodeEntry) iter.next();
            writeQName(out, entry.getName());   // name
            writeID(out, entry.getId());  // uuid
        }
        writeModCount(out, state.getModCount());
        
        // shared set (list of parent uuids)
        c = state.getSharedSet();
        out.writeInt(c.size()); // count
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            writeID(out, (NodeId) iter.next());
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
            in.readFully(bytes);
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
     * Deserializes a Name
     * @param in the input stream
     * @return the qname
     * @throws IOException in an I/O error occurs.
     */
    public Name readQName(DataInputStream in) throws IOException {
        String uri = nsIndex.indexToString(in.readInt());
        String local = in.readUTF();
        return NameFactoryImpl.getInstance().create(uri, local);
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
     * Serializes a Name
     * @param out the output stream
     * @param name the name
     * @throws IOException in an I/O error occurs.
     */
    public void writeQName(DataOutputStream out, Name name) throws IOException {
        out.writeInt(nsIndex.stringToIndex(name.getNamespaceURI()));
        out.writeUTF(name.getLocalName());
    }

    /**
     * Deserializes an indexed Name
     * @param in the input stream
     * @return the qname
     * @throws IOException in an I/O error occurs.
     */
    public Name readIndexedQName(DataInputStream in) throws IOException {
        int index = in.readInt();
        if (index < 0) {
            return null;
        } else {
            String uri = nsIndex.indexToString(index);
            String local = nameIndex.indexToString(in.readInt());
            return NameFactoryImpl.getInstance().create(uri, local);
        }
    }

    /**
     * Serializes a indexed Name
     * @param out the output stream
     * @param name the name
     * @throws IOException in an I/O error occurs.
     */
    public void writeIndexedQName(DataOutputStream out, Name name) throws IOException {
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
        Name name = readQName(in);
        return new PropertyId(new NodeId(uuid), name);
    }
}
