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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle serializater.
 *
 * @see BundleReader
 */
class BundleWriter {

    /** Logger instance */
    private static Logger log = LoggerFactory.getLogger(BundleWriter.class);

    private final BundleBinding binding;

    private final DataOutputStream out;

    /**
     * Creates a new bundle serializer.
     *
     * @param binding bundle binding
     * @param stream stream to which the bundle will be written
     * @throws IOException if an I/O error occurs.
     */
    public BundleWriter(BundleBinding binding, OutputStream stream)
            throws IOException {
        this.binding = binding;
        this.out = new DataOutputStream(stream);
        this.out.writeByte(BundleBinding.VERSION_CURRENT);
    }

    /**
     * Serializes a <code>NodePropBundle</code> to a data output stream
     *
     * @param bundle the bundle to serialize
     * @throws IOException if an I/O error occurs.
     */
    public void writeBundle(NodePropBundle bundle)
            throws IOException {
        long size = out.size();

        // primaryType
        writeName(bundle.getNodeTypeName());

        // parentUUID
        writeNodeId(bundle.getParentId());

        // definitionId
        out.writeUTF("");

        // mixin types
        for (Name name : bundle.getMixinTypeNames()) {
            writeName(name);
        }
        writeName(null);

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
                writeName(pName);
                writeState(pState);
            }
        }
        writeName(null);

        // write uuid flag
        out.writeBoolean(bundle.isReferenceable());

        // child nodes (list of uuid/name pairs)
        for (NodePropBundle.ChildNodeEntry entry : bundle.getChildNodeEntries()) {
            writeNodeId(entry.getId());  // uuid
            writeName(entry.getName());   // name
        }
        writeNodeId(null);

        // write mod count
        out.writeShort(bundle.getModCount());

        // write shared set
        for (NodeId nodeId: bundle.getSharedSet()) {
            writeNodeId(nodeId);
        }
        writeNodeId(null);

        // set size of bundle
        bundle.setSize(out.size() - size);
    }

    /**
     * Serializes a <code>PropertyState</code> to the data output stream
     *
     * @param state the property entry to store
     * @throws IOException if an I/O error occurs.
     */
    private void writeState(NodePropBundle.PropertyEntry state)
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
                        DataStore dataStore = binding.dataStore;
                        if (dataStore != null) {
                            int maxMemorySize = dataStore.getMinRecordLength() - 1;
                            if (size < maxMemorySize) {
                                writeSmallBinary(val, state, i);
                            } else {
                                out.writeInt(BundleBinding.BINARY_IN_DATA_STORE);
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
                        } else if (size > binding.getMinBlobSize()) {
                            out.writeInt(BundleBinding.BINARY_IN_BLOB_STORE);
                            String blobId = state.getBlobId(i);
                            if (blobId == null) {
                                BLOBStore blobStore = binding.getBlobStore();
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
                            byte[] data = writeSmallBinary(val, state, i);
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
                        writeDecimal(val.getDecimal());
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
                        writeName(val.getName());
                    } catch (RepositoryException e) {
                        // should never occur
                        throw new IOException("Unexpected error while writing NAME value.");
                    }
                    break;
                case PropertyType.WEAKREFERENCE:
                case PropertyType.REFERENCE:
                    writeNodeId(val.getNodeId());
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
     * @param value the binary value
     * @param state the property state (for error messages)
     * @param i the index (for error messages)
     * @return the data
     * @throws IOException if the data could not be read
     */
    private byte[] writeSmallBinary(
            InternalValue value, NodePropBundle.PropertyEntry state, int i)
            throws IOException {
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

    /**
     * Serializes a node identifier
     *
     * @param id the node id
     * @throws IOException in an I/O error occurs.
     */
    private void writeNodeId(NodeId id) throws IOException {
        if (id == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeLong(id.getMostSignificantBits());
            out.writeLong(id.getLeastSignificantBits());
        }
    }

    /**
     * Serializes a BigDecimal
     *
     * @param decimal the decimal number
     * @throws IOException in an I/O error occurs.
     */
    private void writeDecimal(BigDecimal decimal) throws IOException {
        if (decimal == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            // TODO more efficient serialization format
            out.writeUTF(decimal.toString());
        }
    }

    /**
     * Serializes a name. The name encoding works as follows:
     * <p>
     * First; if the name is known by the {@link BundleNames} class (this
     * includes the <code>null</code> name), then the name is serialized
     * as a single byte using the following format.
     * <pre>
     * +-------------------------------+
     * | 0 |    common name index      |
     * +-------------------------------+
     * </pre>
     * <p>
     * Second; if the name is not known, it gets serialized as a
     * variable-length field whose first byte looks like this:
     * <pre>
     * +-------------------------------+
     * | 1 | ns index  |  name length  |
     * +-------------------------------+
     * </pre>
     * <p>
     * The three-bit namespace index identifies either a known namespace
     * in the {@link BundleNames} class (values 0 - 6) or an explicit
     * namespace URI string that is written using
     * {@link DataOutputStream#writeUTF(String)} right after this byte
     * (value 7).
     * <p>
     * The four-bit name length field indicates the length (in UTF-8 bytes)
     * of the local part of the name. Since zero-length local names are not
     * allowed, the length is first decremented by one before storing in this
     * field. The UTF-8 byte sequence is written out after this byte and the
     * possible namespace URI string. If the length of the local name is
     * larger than 15 (i.e. would be stored as 0x0f or more), then the value
     * 0x0f is stored as the name length and the name string is written
     * using {@link DataOutputStream#writeUTF(String)}.
     *
     * @param name the name
     * @throws IOException in an I/O error occurs.
     */
    private void writeName(Name name) throws IOException {
        int index = BundleNames.nameToIndex(name);
        if (index != -1) {
            assert 0 <= index && index < 0x80;
            out.writeByte(index);
        } else {
            String uri = name.getNamespaceURI();
            int ns = BundleNames.namespaceToIndex(uri) & 0x07;

            String local = name.getLocalName();
            byte[] bytes = local.getBytes("UTF-8");
            int len = Math.min(bytes.length - 1, 0x0f);

            out.writeByte(0x80 | ns << 4 | len);
            if (ns == 0x07) {
                out.writeUTF(uri);
            }
            if (len != 0x0f) {
                out.write(bytes);
            } else {
                out.writeUTF(local);
            }
        }
    }

}
