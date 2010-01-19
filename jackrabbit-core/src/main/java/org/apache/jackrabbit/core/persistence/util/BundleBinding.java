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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.util.StringIndex;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.math.BigDecimal;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * This Class implements efficient serialization methods for item states.
 */
public class BundleBinding extends ItemStateBinding {

    private static final int BINARY_IN_BLOB_STORE = -1;
    private static final int BINARY_IN_DATA_STORE = -2;

    /**
     * default logger
     */
    private static Logger log = LoggerFactory.getLogger(BundleBinding.class);

    /**
     * Creates a new bundle binding
     *
     * @param errorHandling the error handling
     * @param blobStore the blobstore for retrieving blobs
     * @param nsIndex the namespace index
     * @param nameIndex the name index
     * @param dataStore the data store
     */
    public BundleBinding(ErrorHandling errorHandling, BLOBStore blobStore,
                         StringIndex nsIndex, StringIndex nameIndex, DataStore dataStore) {
        super(errorHandling, blobStore, nsIndex, nameIndex, dataStore);
    }

    /**
     * Deserializes a <code>NodePropBundle</code> from a data input stream.
     *
     * @param in the input stream
     * @param id the node id for the new bundle
     * @return the bundle
     * @throws IOException if an I/O error occurs.
     */
    public NodePropBundle readBundle(DataInputStream in, NodeId id)
            throws IOException {

        NodePropBundle bundle = new NodePropBundle(this, id);

        // read version and primary type...special handling
        int index = in.readInt();

        // get version
        int version = (index >> 24) & 0xff;
        index &= 0x00ffffff;
        String uri = nsIndex.indexToString(index);
        String local = nameIndex.indexToString(in.readInt());
        Name nodeTypeName = NameFactoryImpl.getInstance().create(uri, local);

        // primaryType
        bundle.setNodeTypeName(nodeTypeName);

        // parentUUID
        bundle.setParentId(readID(in));

        // definitionId
        in.readUTF();

        // mixin types
        Set<Name> mixinTypeNames = new HashSet<Name>();
        Name name = readIndexedQName(in);
        while (name != null) {
            mixinTypeNames.add(name);
            name = readIndexedQName(in);
        }
        bundle.setMixinTypeNames(mixinTypeNames);

        // properties
        name = readIndexedQName(in);
        while (name != null) {
            PropertyId pId = new PropertyId(bundle.getId(), name);
            // skip redundant primaryType, mixinTypes and uuid properties
            if (name.equals(NameConstants.JCR_PRIMARYTYPE)
                || name.equals(NameConstants.JCR_MIXINTYPES)
                || name.equals(NameConstants.JCR_UUID)) {
                readPropertyEntry(in, pId);
                name = readIndexedQName(in);
                continue;
            }
            NodePropBundle.PropertyEntry pState = readPropertyEntry(in, pId);
            bundle.addProperty(pState);
            name = readIndexedQName(in);
        }

        // set referenceable flag
        bundle.setReferenceable(in.readBoolean());

        // child nodes (list of uuid/name pairs)
        NodeId childId = readID(in);
        while (childId != null) {
            bundle.addChildNodeEntry(readQName(in), childId);
            childId = readID(in);
        }

        // read modcount, since version 1.0
        if (version >= VERSION_1) {
            bundle.setModCount(readModCount(in));
        }

        // read shared set, since version 2.0
        Set<NodeId> sharedSet = new HashSet<NodeId>();
        if (version >= VERSION_2) {
            // shared set (list of parent uuids)
            NodeId parentId = readID(in);
            while (parentId != null) {
                sharedSet.add(parentId);
                parentId = readID(in);
            }
        }
        bundle.setSharedSet(sharedSet);

        return bundle;
    }

    /**
     * Checks a <code>NodePropBundle</code> from a data input stream.
     *
     * @param in the input stream
     * @return <code>true</code> if the data is valid;
     *         <code>false</code> otherwise.
     */
    public boolean checkBundle(DataInputStream in) {
        int version;
        // primaryType & version
        try {
            // read version and primary type...special handling
            int index = in.readInt();

            // get version
            version = (index >> 24) & 0xff;
            index &= 0x00ffffff;
            String uri = nsIndex.indexToString(index);
            String local = nameIndex.indexToString(in.readInt());
            Name nodeTypeName = NameFactoryImpl.getInstance().create(uri, local);

            log.debug("Serialzation Version: " + version);
            log.debug("NodeTypeName: " + nodeTypeName);
        } catch (IOException e) {
            log.error("Error while reading NodeTypeName: " + e);
            return false;
        }
        try {
            NodeId parentId = readID(in);
            log.debug("ParentUUID: " + parentId);
        } catch (IOException e) {
            log.error("Error while reading ParentUUID: " + e);
            return false;
        }
        try {
            String definitionId = in.readUTF();
            log.debug("DefinitionId: " + definitionId);
        } catch (IOException e) {
            log.error("Error while reading DefinitionId: " + e);
            return false;
        }
        try {
            Name mixinName = readIndexedQName(in);
            while (mixinName != null) {
                log.debug("MixinTypeName: " + mixinName);
                mixinName = readIndexedQName(in);
            }
        } catch (IOException e) {
            log.error("Error while reading MixinTypes: " + e);
            return false;
        }
        try {
            Name propName = readIndexedQName(in);
            while (propName != null) {
                log.debug("PropertyName: " + propName);
                if (!checkPropertyState(in)) {
                    return false;
                }
                propName = readIndexedQName(in);
            }
        } catch (IOException e) {
            log.error("Error while reading property names: " + e);
            return false;
        }
        try {
            boolean hasUUID = in.readBoolean();
            log.debug("hasUUID: " + hasUUID);
        } catch (IOException e) {
            log.error("Error while reading 'hasUUID': " + e);
            return false;
        }
        try {
            NodeId cneId = readID(in);
            while (cneId != null) {
                Name cneName = readQName(in);
                log.debug("ChildNodentry: " + cneId + ":" + cneName);
                cneId = readID(in);
            }
        } catch (IOException e) {
            log.error("Error while reading child node entry: " + e);
            return false;
        }

        if (version >= VERSION_1) {
            try {
                short modCount = readModCount(in);
                log.debug("modCount: " + modCount);
            } catch (IOException e) {
                log.error("Error while reading mod cout: " + e);
                return false;
            }
        }

        return true;
    }

    /**
     * Serializes a <code>NodePropBundle</code> to a data output stream
     *
     * @param out the output stream
     * @param bundle the bundle to serialize
     * @throws IOException if an I/O error occurs.
     */
    public void writeBundle(DataOutputStream out, NodePropBundle bundle)
            throws IOException {
        long size = out.size();

        // primaryType and version
        out.writeInt((VERSION_CURRENT << 24) | nsIndex.stringToIndex(bundle.getNodeTypeName().getNamespaceURI()));
        out.writeInt(nameIndex.stringToIndex(bundle.getNodeTypeName().getLocalName()));

        // parentUUID
        writeID(out, bundle.getParentId());

        // definitionId
        out.writeUTF("");

        // mixin types
        for (Name name : bundle.getMixinTypeNames()) {
            writeIndexedQName(out, name);
        }
        writeIndexedQName(out, null);

        // properties
        for (Name pName : bundle.getPropertyNames()) {
            // skip redundant primaryType, mixinTypes and uuid properties
            if (pName.equals(NameConstants.JCR_PRIMARYTYPE)
                || pName.equals(NameConstants.JCR_MIXINTYPES)
                || pName.equals(NameConstants.JCR_UUID)) {
                continue;
            }
            NodePropBundle.PropertyEntry pState = bundle.getPropertyEntry(pName);
            if (pState == null) {
                log.error("PropertyState missing in bundle: " + pName);
            } else {
                writeIndexedQName(out, pName);
                writeState(out, pState);
            }
        }
        writeIndexedQName(out, null);

        // write uuid flag
        out.writeBoolean(bundle.isReferenceable());

        // child nodes (list of uuid/name pairs)
        for (NodePropBundle.ChildNodeEntry entry : bundle.getChildNodeEntries()) {
            writeID(out, entry.getId());  // uuid
            writeQName(out, entry.getName());   // name
        }
        writeID(out, null);

        // write mod count
        writeModCount(out, bundle.getModCount());

        // write shared set
        for (NodeId nodeId: bundle.getSharedSet()) {
            writeID(out, nodeId);
        }
        writeID(out, null);

        // set size of bundle
        bundle.setSize(out.size() - size);
    }

    /**
     * Deserializes a <code>PropertyState</code> from the data input stream.
     *
     * @param in the input stream
     * @param id the property id for the new property entry
     * @return the property entry
     * @throws IOException if an I/O error occurs.
     */
    public NodePropBundle.PropertyEntry readPropertyEntry(DataInputStream in, PropertyId id)
            throws IOException {
        NodePropBundle.PropertyEntry entry = new NodePropBundle.PropertyEntry(id);
        // type and modcount
        int type = in.readInt();
        entry.setModCount((short) ((type >> 16) & 0x0ffff));
        type &= 0x0ffff;
        entry.setType(type);

        // multiValued
        entry.setMultiValued(in.readBoolean());
        // definitionId
        in.readUTF();
        // values
        int count = in.readInt();   // count
        InternalValue[] values = new InternalValue[count];
        String[] blobIds = new String[count];
        for (int i = 0; i < count; i++) {
            InternalValue val;
            switch (type) {
                case PropertyType.BINARY:
                    int size = in.readInt();
                    if (size == BINARY_IN_DATA_STORE) {
                        val = InternalValue.create(dataStore, in.readUTF());
                    } else if (size == BINARY_IN_BLOB_STORE) {
                        blobIds[i] = in.readUTF();
                        try {
                            if (blobStore instanceof ResourceBasedBLOBStore) {
                                val = InternalValue.create(((ResourceBasedBLOBStore) blobStore).getResource(blobIds[i]));
                            } else {
                                val = InternalValue.create(blobStore.get(blobIds[i]));
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
                case PropertyType.DECIMAL:
                    val = InternalValue.create(readDecimal(in));
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
                case PropertyType.WEAKREFERENCE:
                    val = InternalValue.create(readID(in), true);
                    break;
                case PropertyType.REFERENCE:
                    val = InternalValue.create(readID(in), false);
                    break;
                default:
                    // because writeUTF(String) has a size limit of 64k,
                    // Strings are serialized as <length><byte[]>
                    int len = in.readInt();
                    byte[] bytes = new byte[len];
                    in.readFully(bytes);
                    val = InternalValue.valueOf(new String(bytes, "UTF-8"), type);
            }
            values[i] = val;
        }
        entry.setValues(values);
        entry.setBlobIds(blobIds);

        return entry;
    }

    /**
     * Checks a <code>PropertyState</code> from the data input stream.
     *
     * @param in the input stream
     * @return <code>true</code> if the data is valid;
     *         <code>false</code> otherwise.
     */
    public boolean checkPropertyState(DataInputStream in) {
        int type;
        try {
            type = in.readInt();
            short modCount = (short) ((type >> 16) | 0xffff);
            type &= 0xffff;
            log.debug("  PropertyType: " + PropertyType.nameFromValue(type));
            log.debug("  ModCount: " + modCount);
        } catch (IOException e) {
            log.error("Error while reading property type: " + e);
            return false;
        }
        try {
            boolean isMV = in.readBoolean();
            log.debug("  MultiValued: " + isMV);
        } catch (IOException e) {
            log.error("Error while reading multivalued: " + e);
            return false;
        }
        try {
            String defintionId = in.readUTF();
            log.debug("  DefinitionId: " + defintionId);
        } catch (IOException e) {
            log.error("Error while reading definition id: " + e);
            return false;
        }

        int count;
        try {
            count = in.readInt();
            log.debug("  num values: " + count);
        } catch (IOException e) {
            log.error("Error while reading number of values: " + e);
            return false;
        }
        for (int i = 0; i < count; i++) {
            switch (type) {
                case PropertyType.BINARY:
                    int size;
                    try {
                        size = in.readInt();
                        log.debug("  binary size: " + size);
                    } catch (IOException e) {
                        log.error("Error while reading size of binary: " + e);
                        return false;
                    }
                    if (size == BINARY_IN_DATA_STORE) {
                        try {
                            String s = in.readUTF();
                            // truncate log output
                            if (s.length() > 80) {
                                s = s.substring(80) + "...";
                            }
                            log.debug("  global data store id: " + s);
                        } catch (IOException e) {
                            log.error("Error while reading blob id: " + e);
                            return false;
                        }
                    } else if (size == BINARY_IN_BLOB_STORE) {
                        try {
                            String s = in.readUTF();
                            log.debug("  blobid: " + s);
                        } catch (IOException e) {
                            log.error("Error while reading blob id: " + e);
                            return false;
                        }
                    } else {
                        // short values into memory
                        byte[] data = new byte[size];
                        try {
                            in.readFully(data);
                            log.debug("  binary: " + data.length + " bytes");
                        } catch (IOException e) {
                            log.error("Error while reading inlined binary: " + e);
                            return false;
                        }
                    }
                    break;
                case PropertyType.DOUBLE:
                    try {
                        double d = in.readDouble();
                        log.debug("  double: " + d);
                    } catch (IOException e) {
                        log.error("Error while reading double value: " + e);
                        return false;
                    }
                    break;
                case PropertyType.DECIMAL:
                    try {
                        BigDecimal d = readDecimal(in);
                        log.debug("  decimal: " + d);
                    } catch (IOException e) {
                        log.error("Error while reading decimal value: " + e);
                        return false;
                    }
                    break;
                case PropertyType.LONG:
                    try {
                        double l = in.readLong();
                        log.debug("  long: " + l);
                    } catch (IOException e) {
                        log.error("Error while reading long value: " + e);
                        return false;
                    }
                    break;
                case PropertyType.BOOLEAN:
                    try {
                        boolean b = in.readBoolean();
                        log.debug("  boolean: " + b);
                    } catch (IOException e) {
                        log.error("Error while reading boolean value: " + e);
                        return false;
                    }
                    break;
                case PropertyType.NAME:
                    try {
                        Name name = readQName(in);
                        log.debug("  name: " + name);
                    } catch (IOException e) {
                        log.error("Error while reading name value: " + e);
                        return false;
                    }
                    break;
                case PropertyType.WEAKREFERENCE:
                case PropertyType.REFERENCE:
                    try {
                        NodeId id = readID(in);
                        log.debug("  reference: " + id);
                    } catch (IOException e) {
                        log.error("Error while reading reference value: " + e);
                        return false;
                    }
                    break;
                default:
                    // because writeUTF(String) has a size limit of 64k,
                    // Strings are serialized as <length><byte[]>
                    int len;
                    try {
                        len = in.readInt();
                        log.debug("  size of string value: " + len);
                    } catch (IOException e) {
                        log.error("Error while reading size of string value: " + e);
                        return false;
                    }
                    try {
                        byte[] bytes = new byte[len];
                        in.readFully(bytes);
                        String s = new String(bytes, "UTF-8");
                        // truncate log output
                        if (s.length() > 80) {
                            s = s.substring(80) + "...";
                        }
                        log.debug("  string: " + s);
                    } catch (IOException e) {
                        log.error("Error while reading string value: " + e);
                        return false;
                    }
            }
        }
        return true;
    }


    /**
     * Serializes a <code>PropertyState</code> to the data output stream
     *
     * @param out the output stream
     * @param state the property entry to store
     * @throws IOException if an I/O error occurs.
     */
    public void writeState(DataOutputStream out, NodePropBundle.PropertyEntry state)
            throws IOException {
        // type & mod count
        out.writeInt(state.getType() | (state.getModCount() << 16));
        // multiValued
        out.writeBoolean(state.isMultiValued());
        // definitionId
        out.writeUTF("");
        // values
        InternalValue[] values = state.getValues();
        out.writeInt(values.length); // count
        for (int i = 0; i < values.length; i++) {
            InternalValue val = values[i];
            switch (state.getType()) {
                case PropertyType.BINARY:
                    try {
                        long size = val.getLength();
                        if (dataStore != null) {
                            int maxMemorySize = dataStore.getMinRecordLength() - 1;
                            if (size < maxMemorySize) {
                                writeSmallBinary(out, val, state, i);
                            } else {
                                out.writeInt(BINARY_IN_DATA_STORE);
                                val.store(dataStore);
                                out.writeUTF(val.toString());
                            }
                            break;
                        }
                        // special handling required for binary value:
                        // spool binary value to file in blob store
                        if (size < 0) {
                            log.warn("Blob has negative size. Potential loss of data. "
                                    + "id={} idx={}", state.getId(), String.valueOf(i));
                            out.writeInt(0);
                            values[i] = InternalValue.create(new byte[0]);
                            val.discard();
                        } else if (size > minBlobSize) {
                            out.writeInt(BINARY_IN_BLOB_STORE);
                            String blobId = state.getBlobId(i);
                            if (blobId == null) {
                                try {
                                    InputStream in = val.getStream();
                                    try {
                                        blobId = blobStore.createId(state.getId(), i);
                                        blobStore.put(blobId, in, size);
                                        state.setBlobId(blobId, i);
                                    } finally {
                                        IOUtils.closeQuietly(in);
                                    }
                                } catch (Exception e) {
                                    String msg = "Error while storing blob. id="
                                            + state.getId() + " idx=" + i + " size=" + size;
                                    log.error(msg, e);
                                    throw new IOException(msg);
                                }
                                try {
                                    // replace value instance with value
                                    // backed by resource in blob store and delete temp file
                                    if (blobStore instanceof ResourceBasedBLOBStore) {
                                        values[i] = InternalValue.create(((ResourceBasedBLOBStore) blobStore).getResource(blobId));
                                    } else {
                                        values[i] = InternalValue.create(blobStore.get(blobId));
                                    }
                                } catch (Exception e) {
                                    log.error("Error while reloading blob. truncating. id="
                                            + state.getId() + " idx=" + i + " size=" + size, e);
                                    values[i] = InternalValue.create(new byte[0]);
                                }
                                val.discard();
                            }
                            // store id of blob as property value
                            out.writeUTF(blobId);   // value
                        } else {
                            // delete evt. blob
                            byte[] data = writeSmallBinary(out, val, state, i);
                            // replace value instance with value
                            // backed by resource in blob store and delete temp file
                            values[i] = InternalValue.create(data);
                            val.discard();
                        }
                    } catch (RepositoryException e) {
                        String msg = "Error while storing blob. id="
                            + state.getId() + " idx=" + i + " value=" + val;
                        log.error(msg, e);
                        throw new IOException(msg);
                    }
                    break;
                case PropertyType.DOUBLE:
                    try {
                        out.writeDouble(val.getDouble());
                    } catch (RepositoryException e) {
                        // should never occur
                        throw new IOException("Unexpected error while writing DOUBLE value.");
                    }
                    break;
                case PropertyType.DECIMAL:
                    try {
                        writeDecimal(out, val.getDecimal());
                    } catch (RepositoryException e) {
                        // should never occur
                        throw new IOException("Unexpected error while writing DECIMAL value.");
                    }
                    break;
                case PropertyType.LONG:
                    try {
                        out.writeLong(val.getLong());
                    } catch (RepositoryException e) {
                        // should never occur
                        throw new IOException("Unexpected error while writing LONG value.");
                    }
                    break;
                case PropertyType.BOOLEAN:
                    try {
                        out.writeBoolean(val.getBoolean());
                    } catch (RepositoryException e) {
                        // should never occur
                        throw new IOException("Unexpected error while writing BOOLEAN value.");
                    }
                    break;
                case PropertyType.NAME:
                    try {
                        writeQName(out, val.getName());
                    } catch (RepositoryException e) {
                        // should never occur
                        throw new IOException("Unexpected error while writing NAME value.");
                    }
                    break;
                case PropertyType.WEAKREFERENCE:
                case PropertyType.REFERENCE:
                    writeID(out, val.getNodeId());
                    break;
                default:
                    // because writeUTF(String) has a size limit of 64k,
                    // we're using write(byte[]) instead
                    byte[] bytes = val.toString().getBytes("UTF-8");
                    out.writeInt(bytes.length); // length of byte[]
                    out.write(bytes);   // byte[]
            }
        }
    }

    /**
     * Write a small binary value and return the data.
     *
     * @param out the output stream to write
     * @param value the binary value
     * @param state the property state (for error messages)
     * @param i the index (for error messages)
     * @return the data
     * @throws IOException if the data could not be read
     */
    private byte[] writeSmallBinary(DataOutputStream out, InternalValue value, NodePropBundle.PropertyEntry state, int i) throws IOException {
        try {
            int size = (int) value.getLength();
            out.writeInt(size);
            byte[] data = new byte[size];
            DataInputStream in =
                new DataInputStream(value.getStream());
            try {
                in.readFully(data);
            } finally {
                IOUtils.closeQuietly(in);
            }
            out.write(data, 0, data.length);
            return data;
        } catch (Exception e) {
            String msg = "Error while storing blob. id="
                    + state.getId() + " idx=" + i + " value=" + value;
            log.error(msg, e);
            throw new IOException(msg);
        }
    }
}
