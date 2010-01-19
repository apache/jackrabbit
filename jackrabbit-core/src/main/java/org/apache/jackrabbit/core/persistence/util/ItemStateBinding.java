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
package org.apache.jackrabbit.core.persistence.util;

import org.apache.jackrabbit.core.persistence.util.BLOBStore;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.util.StringIndex;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.math.BigDecimal;

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
        Set<Name> set = new HashSet<Name>(count);
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
        Collection<Name> c = state.getMixinTypeNames();
        out.writeInt(c.size()); // count
        for (Iterator<Name> iter = c.iterator(); iter.hasNext();) {
            writeQName(out, iter.next());
        }
        // properties (names)
        c = state.getPropertyNames();
        out.writeInt(c.size()); // count
        for (Iterator<Name> iter = c.iterator(); iter.hasNext();) {
            Name pName = iter.next();
            writeIndexedQName(out, pName);
        }
        // child nodes (list of name/uuid pairs)
        Collection<ChildNodeEntry> collChild = state.getChildNodeEntries();
        out.writeInt(collChild.size()); // count
        for (Iterator<ChildNodeEntry> iter = collChild.iterator(); iter.hasNext();) {
            ChildNodeEntry entry = iter.next();
            writeQName(out, entry.getName());   // name
            writeID(out, entry.getId());  // uuid
        }
        writeModCount(out, state.getModCount());
        
        // shared set (list of parent uuids)
        Collection<NodeId> collShared = state.getSharedSet();
        out.writeInt(collShared.size()); // count
        for (Iterator<NodeId> iter = collShared.iterator(); iter.hasNext();) {
            writeID(out, iter.next());
        }
    }

    /**
     * Deserializes a node identifier
     * @param in the input stream
     * @return the node id
     * @throws IOException in an I/O error occurs.
     */
    public NodeId readNodeId(DataInputStream in) throws IOException {
        if (in.readBoolean()) {
            byte[] bytes = new byte[16];
            in.readFully(bytes);
            return new NodeId(bytes);
        } else {
            return null;
        }
    }

    /**
     * Serializes a node identifier
     * @param out the output stream
     * @param id the node id
     * @throws IOException in an I/O error occurs.
     */
    public void writeNodeId(DataOutputStream out, String id) throws IOException {
        if (id == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.write(new NodeId(id).getRawBytes());
        }
    }

    /**
     * Deserializes a BigDecimal
     * @param in the input stream
     * @return the decimal
     * @throws IOException in an I/O error occurs.
     */
    public BigDecimal readDecimal(DataInputStream in) throws IOException {
        if (in.readBoolean()) {
            // TODO more efficient serialization format
            return new BigDecimal(in.readUTF());
        } else {
            return null;
        }
    }

    /**
     * Serializes a BigDecimal
     * @param out the output stream
     * @param decimal the decimal number
     * @throws IOException in an I/O error occurs.
     */
    public void writeDecimal(DataOutputStream out, BigDecimal decimal) throws IOException {
        if (decimal == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            // TODO more efficient serialization format
            out.writeUTF(decimal.toString());
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
            return new NodeId(bytes);
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
            out.write(id.getRawBytes());
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
        NodeId id = readNodeId(in);
        Name name = readQName(in);
        return new PropertyId(id, name);
    }

}
