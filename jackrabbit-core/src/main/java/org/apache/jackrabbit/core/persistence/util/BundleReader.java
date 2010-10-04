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
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.math.BigDecimal;

import javax.jcr.PropertyType;

/**
 * Bundle deserializater.
 *
 * @see BundleWriter
 */
class BundleReader {

    /** Logger instance */
    private static Logger log = LoggerFactory.getLogger(BundleReader.class);

    private final BundleBinding binding;

    private final DataInputStream in;

    /**
     * Creates a new bundle deserializer.
     *
     * @param binding bundle binding
     * @param stream stream from which the bundle is read
     */
    public BundleReader(BundleBinding binding, InputStream stream) {
        this.binding = binding;
        this.in = new DataInputStream(stream);
    }

    /**
     * Deserializes a <code>NodePropBundle</code> from a data input stream.
     *
     * @param id the node id for the new bundle
     * @return the bundle
     * @throws IOException if an I/O error occurs.
     */
    public NodePropBundle readBundle(NodeId id) throws IOException {
        NodePropBundle bundle = new NodePropBundle(id);

        // read version and primary type...special handling
        int index = in.readInt();

        // get version
        int version = (index >> 24) & 0xff;
        index &= 0x00ffffff;
        String uri = binding.nsIndex.indexToString(index);
        String local = binding.nameIndex.indexToString(in.readInt());
        Name nodeTypeName = NameFactoryImpl.getInstance().create(uri, local);

        // primaryType
        bundle.setNodeTypeName(nodeTypeName);

        // parentUUID
        bundle.setParentId(readNodeId());

        // definitionId
        in.readUTF();

        // mixin types
        Set<Name> mixinTypeNames = new HashSet<Name>();
        Name name = readIndexedQName();
        while (name != null) {
            mixinTypeNames.add(name);
            name = readIndexedQName();
        }
        bundle.setMixinTypeNames(mixinTypeNames);

        // properties
        name = readIndexedQName();
        while (name != null) {
            PropertyId pId = new PropertyId(bundle.getId(), name);
            // skip redundant primaryType, mixinTypes and uuid properties
            if (name.equals(NameConstants.JCR_PRIMARYTYPE)
                || name.equals(NameConstants.JCR_MIXINTYPES)
                || name.equals(NameConstants.JCR_UUID)) {
                readPropertyEntry(pId);
                name = readIndexedQName();
                continue;
            }
            NodePropBundle.PropertyEntry pState = readPropertyEntry(pId);
            bundle.addProperty(pState);
            name = readIndexedQName();
        }

        // set referenceable flag
        bundle.setReferenceable(in.readBoolean());

        // child nodes (list of uuid/name pairs)
        NodeId childId = readNodeId();
        while (childId != null) {
            bundle.addChildNodeEntry(readQName(), childId);
            childId = readNodeId();
        }

        // read modcount, since version 1.0
        if (version >= BundleBinding.VERSION_1) {
            bundle.setModCount(in.readShort());
        }

        // read shared set, since version 2.0
        Set<NodeId> sharedSet = new HashSet<NodeId>();
        if (version >= BundleBinding.VERSION_2) {
            // shared set (list of parent uuids)
            NodeId parentId = readNodeId();
            while (parentId != null) {
                sharedSet.add(parentId);
                parentId = readNodeId();
            }
        }
        bundle.setSharedSet(sharedSet);

        return bundle;
    }

    /**
     * Deserializes a <code>PropertyState</code> from the data input stream.
     *
     * @param id the property id for the new property entry
     * @return the property entry
     * @throws IOException if an I/O error occurs.
     */
    private NodePropBundle.PropertyEntry readPropertyEntry(PropertyId id)
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
                    if (size == BundleBinding.BINARY_IN_DATA_STORE) {
                        val = InternalValue.create(binding.dataStore, in.readUTF());
                    } else if (size == BundleBinding.BINARY_IN_BLOB_STORE) {
                        blobIds[i] = in.readUTF();
                        try {
                            BLOBStore blobStore = binding.getBlobStore();
                            if (blobStore instanceof ResourceBasedBLOBStore) {
                                val = InternalValue.create(((ResourceBasedBLOBStore) blobStore).getResource(blobIds[i]));
                            } else {
                                val = InternalValue.create(blobStore.get(blobIds[i]));
                            }
                        } catch (IOException e) {
                            if (binding.errorHandling.ignoreMissingBlobs()) {
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
                    val = InternalValue.create(readDecimal());
                    break;
                case PropertyType.LONG:
                    val = InternalValue.create(in.readLong());
                    break;
                case PropertyType.BOOLEAN:
                    val = InternalValue.create(in.readBoolean());
                    break;
                case PropertyType.NAME:
                    val = InternalValue.create(readQName());
                    break;
                case PropertyType.WEAKREFERENCE:
                    val = InternalValue.create(readNodeId(), true);
                    break;
                case PropertyType.REFERENCE:
                    val = InternalValue.create(readNodeId(), false);
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
     * Deserializes a node identifier
     *
     * @return the node id
     * @throws IOException in an I/O error occurs.
     */
    private NodeId readNodeId() throws IOException {
        if (in.readBoolean()) {
            long msb = in.readLong();
            long lsb = in.readLong();
            return new NodeId(msb, lsb);
        } else {
            return null;
        }
    }

    /**
     * Deserializes a BigDecimal
     *
     * @return the decimal
     * @throws IOException in an I/O error occurs.
     */
    private BigDecimal readDecimal() throws IOException {
        if (in.readBoolean()) {
            // TODO more efficient serialization format
            return new BigDecimal(in.readUTF());
        } else {
            return null;
        }
    }

    /**
     * Deserializes a Name
     *
     * @return the qname
     * @throws IOException in an I/O error occurs.
     */
    private Name readQName() throws IOException {
        String uri = binding.nsIndex.indexToString(in.readInt());
        String local = in.readUTF();
        return NameFactoryImpl.getInstance().create(uri, local);
    }

    /**
     * Deserializes an indexed Name
     *
     * @return the qname
     * @throws IOException in an I/O error occurs.
     */
    private Name readIndexedQName() throws IOException {
        int index = in.readInt();
        if (index < 0) {
            return null;
        } else {
            String uri = binding.nsIndex.indexToString(index);
            String local = binding.nameIndex.indexToString(in.readInt());
            return NameFactoryImpl.getInstance().create(uri, local);
        }
    }

}
