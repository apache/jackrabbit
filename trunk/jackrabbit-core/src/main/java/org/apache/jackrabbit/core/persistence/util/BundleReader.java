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
import org.apache.commons.io.IOExceptionWithCause;
import org.apache.commons.io.input.CountingInputStream;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.math.BigDecimal;

import javax.jcr.PropertyType;

/**
 * Bundle deserializer. See the {@link BundleWriter} class for details of
 * the serialization format.
 *
 * @see BundleWriter
 */
class BundleReader {

    /*
     * Implementation note: if you change this class, also change BundleDumper
     * accordingly.
     */

    /** Logger instance */
    private static Logger log = LoggerFactory.getLogger(BundleReader.class);

    /**
     * Pre-calculated {@link TimeZone} objects for common timezone offsets.
     */
    private static final TimeZone[] COMMON_TIMEZONES = {
        TimeZone.getTimeZone("GMT+00:00"), // 0b00000
        TimeZone.getTimeZone("GMT+01:00"), // 0b00001
        TimeZone.getTimeZone("GMT+02:00"), // 0b00010
        TimeZone.getTimeZone("GMT+03:00"), // 0b00011
        TimeZone.getTimeZone("GMT+04:00"), // 0b00100
        TimeZone.getTimeZone("GMT+05:00"), // 0b00101
        TimeZone.getTimeZone("GMT+06:00"), // 0b00110
        TimeZone.getTimeZone("GMT+07:00"), // 0b00111
        TimeZone.getTimeZone("GMT+08:00"), // 0b01000
        TimeZone.getTimeZone("GMT+09:00"), // 0b01001
        TimeZone.getTimeZone("GMT+10:00"), // 0b01010
        TimeZone.getTimeZone("GMT+11:00"), // 0b01011
        TimeZone.getTimeZone("GMT+12:00"), // 0b01100
        TimeZone.getTimeZone("GMT+13:00"), // 0b01101
        TimeZone.getTimeZone("GMT+14:00"), // 0b01110
        TimeZone.getTimeZone("GMT+15:00"), // 0b01111
        TimeZone.getTimeZone("GMT-16:00"), // 0b10000
        TimeZone.getTimeZone("GMT-15:00"), // 0b10001
        TimeZone.getTimeZone("GMT-14:00"), // 0b10010
        TimeZone.getTimeZone("GMT-13:00"), // 0b10011
        TimeZone.getTimeZone("GMT-12:00"), // 0b10100
        TimeZone.getTimeZone("GMT-11:00"), // 0b10101
        TimeZone.getTimeZone("GMT-10:00"), // 0b10110
        TimeZone.getTimeZone("GMT-09:00"), // 0b10111
        TimeZone.getTimeZone("GMT-08:00"), // 0b11000
        TimeZone.getTimeZone("GMT-07:00"), // 0b11001
        TimeZone.getTimeZone("GMT-06:00"), // 0b11010
        TimeZone.getTimeZone("GMT-05:00"), // 0b11011
        TimeZone.getTimeZone("GMT-04:00"), // 0b11100
        TimeZone.getTimeZone("GMT-03:00"), // 0b11101
        TimeZone.getTimeZone("GMT-02:00"), // 0b11110
        TimeZone.getTimeZone("GMT-01:00"), // 0b11111
    };

    private final BundleBinding binding;

    /**
     * Counter for the number of bytes read from the input stream.
     */
    private final CountingInputStream cin;

    /**
     * Wrapper for reading structured data from the input stream.
     */
    private final DataInputStream in;

    private final int version;

    /**
     * The default namespace and the first six other namespaces used in this
     * bundle. Used by the {@link #readName()} method to keep track of
     * already seen namespaces.
     */
    private final String[] namespaces =
        // NOTE: The length of this array must be seven
        { Name.NS_DEFAULT_URI, null, null, null, null, null, null };

    /**
     * Creates a new bundle deserializer.
     *
     * @param binding bundle binding
     * @param stream stream from which the bundle is read
     * @throws IOException if an I/O error occurs.
     */
    public BundleReader(BundleBinding binding, InputStream stream)
            throws IOException {
        this.binding = binding;
        this.cin = new CountingInputStream(stream);
        this.in = new DataInputStream(cin);
        this.version = in.readUnsignedByte();
    }

    /**
     * Deserializes a <code>NodePropBundle</code> from a data input stream.
     *
     * @param id the node id for the new bundle
     * @return the bundle
     * @throws IOException if an I/O error occurs.
     */
    public NodePropBundle readBundle(NodeId id) throws IOException {
        long start = cin.getByteCount();
        NodePropBundle bundle = new NodePropBundle(id);
        if (version >= BundleBinding.VERSION_3) {
            readBundleNew(bundle);
        } else {
            readBundleOld(bundle);
        }
        bundle.setSize(cin.getByteCount() - start);
        return bundle;
    }

    private void readBundleNew(NodePropBundle bundle) throws IOException {
        // node type
        bundle.setNodeTypeName(readName());

        // parentUUID
        NodeId parentId = readNodeId();
        if (BundleBinding.NULL_PARENT_ID.equals(parentId)) {
            parentId = null;
        }
        bundle.setParentId(parentId);

        // read modcount
        bundle.setModCount((short) readVarInt());

        int b = in.readUnsignedByte();
        bundle.setReferenceable((b & 1) != 0);

        // mixin types
        int mn = readVarInt((b >> 7) & 1, 1);
        if (mn == 0) {
            bundle.setMixinTypeNames(Collections.<Name>emptySet());
        } else if (mn == 1) {
            bundle.setMixinTypeNames(Collections.singleton(readName()));
        } else {
            Set<Name> mixins = new HashSet<Name>(mn * 2);
            for (int i = 0; i < mn; i++) {
                mixins.add(readName());
            }
            bundle.setMixinTypeNames(mixins);
        }

        // properties
        int pn = readVarInt((b >> 4) & 7, 7);
        for (int i = 0; i < pn; i++) {
            PropertyId id = new PropertyId(bundle.getId(), readName());
            bundle.addProperty(readPropertyEntry(id));
        }

        // child nodes (list of name/uuid pairs)
        int nn = readVarInt((b >> 2) & 3, 3);
        for (int i = 0; i < nn; i++) {
            Name name = readQName();
            NodeId id = readNodeId();
            bundle.addChildNodeEntry(name, id);
        }

        // read shared set
        int sn = readVarInt((b >> 1) & 1, 1);
        if (sn == 0) {
            bundle.setSharedSet(Collections.<NodeId>emptySet());
        } else if (sn == 1) {
            bundle.setSharedSet(Collections.singleton(readNodeId()));
        } else {
            Set<NodeId> shared = new HashSet<NodeId>();
            for (int i = 0; i < sn; i++) {
                shared.add(readNodeId());
            }
            bundle.setSharedSet(shared);
        }
    }

    private void readBundleOld(NodePropBundle bundle) throws IOException {
        // read primary type...special handling
        int a = in.readUnsignedByte();
        int b = in.readUnsignedByte();
        int c = in.readUnsignedByte();
        String uri = binding.nsIndex.indexToString(a << 16 | b << 8 | c);
        String local = binding.nameIndex.indexToString(in.readInt());
        bundle.setNodeTypeName(
                NameFactoryImpl.getInstance().create(uri, local));

        // parentUUID
        bundle.setParentId(readNodeId());

        // definitionId
        in.readUTF();

        // mixin types
        Name name = readIndexedQName();
        if (name != null) {
            Set<Name> mixinTypeNames = new HashSet<Name>();
            do {
                mixinTypeNames.add(name);
                name = readIndexedQName();
            } while (name != null);
            bundle.setMixinTypeNames(mixinTypeNames);
        } else {
            bundle.setMixinTypeNames(Collections.<Name>emptySet());
        }

        // properties
        name = readIndexedQName();
        while (name != null) {
            PropertyId pId = new PropertyId(bundle.getId(), name);
            NodePropBundle.PropertyEntry pState = readPropertyEntry(pId);
            // skip redundant primaryType, mixinTypes and uuid properties
            if (!name.equals(NameConstants.JCR_PRIMARYTYPE)
                    && !name.equals(NameConstants.JCR_UUID)) {
                bundle.addProperty(pState);
            }
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
        if (version >= BundleBinding.VERSION_2) {
            // shared set (list of parent uuids)
            NodeId parentId = readNodeId();
            if (parentId != null) {
                Set<NodeId> shared = new HashSet<NodeId>();
                do {
                    shared.add(parentId);
                    parentId = readNodeId();
                } while (parentId != null);
                bundle.setSharedSet(shared);
            } else {
                bundle.setSharedSet(Collections.<NodeId>emptySet());
            }
        } else {
            bundle.setSharedSet(Collections.<NodeId>emptySet());
        }
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

        int count = 1;
        if (version >= BundleBinding.VERSION_3) {
            int b = in.readUnsignedByte();

            entry.setType(b & 0x0f);

            int len = b >>> 4;
            if (len != 0) {
                entry.setMultiValued(true);
                if (len == 0x0f) {
                    count = readVarInt() + 0x0f - 1;
                } else {
                    count = len - 1;
                }
            }

            entry.setModCount((short) readVarInt());
        } else {
            // type and modcount
            int type = in.readInt();
            entry.setModCount((short) ((type >> 16) & 0x0ffff));
            type &= 0x0ffff;
            entry.setType(type);

            // multiValued
            entry.setMultiValued(in.readBoolean());

            // definitionId
            in.readUTF();

            // count
            count = in.readInt();
        }

        // values
        InternalValue[] values = new InternalValue[count];
        String[] blobIds = new String[count];
        for (int i = 0; i < count; i++) {
            InternalValue val;
            int type = entry.getType();
            switch (type) {
                case PropertyType.BINARY:
                    int size = in.readInt();
                    if (size == BundleBinding.BINARY_IN_DATA_STORE) {
                        val = InternalValue.create(binding.dataStore, readString());
                    } else if (size == BundleBinding.BINARY_IN_BLOB_STORE) {
                        blobIds[i] = readString();
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
                            throw new IOExceptionWithCause("Unable to create property value: " + e.toString(), e);
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
                    if (version >= BundleBinding.VERSION_3) {
                        val = InternalValue.create(readVarLong());
                    } else {
                        val = InternalValue.create(in.readLong());
                    }
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
                case PropertyType.DATE:
                    if (version >= BundleBinding.VERSION_3) {
                        val = InternalValue.create(readDate());
                        break;
                    } // else fall through
                default:
                    if (version >= BundleBinding.VERSION_3) {
                        val = InternalValue.valueOf(
                                readString(), entry.getType());
                } else {
                    // because writeUTF(String) has a size limit of 64k,
                    // Strings are serialized as <length><byte[]>
                    int len = in.readInt();
                    byte[] bytes = new byte[len];
                    in.readFully(bytes);
                    String stringVal = new String(bytes, StandardCharsets.UTF_8);

                    // https://issues.apache.org/jira/browse/JCR-3083
                    if (PropertyType.DATE == entry.getType()) {
                        val = InternalValue.createDate(stringVal);
                    } else {
                        val = InternalValue.valueOf(stringVal, entry.getType());
                    }
                }
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
        if (version >= BundleBinding.VERSION_3 || in.readBoolean()) {
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
            return new BigDecimal(readString());
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
        if (version >= BundleBinding.VERSION_3) {
            return readName();
        }

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
        if (version >= BundleBinding.VERSION_3) {
            return readName();
        }

        int index = in.readInt();
        if (index < 0) {
            return null;
        } else {
            String uri = binding.nsIndex.indexToString(index);
            String local = binding.nameIndex.indexToString(in.readInt());
            return NameFactoryImpl.getInstance().create(uri, local);
        }
    }

    /**
     * Deserializes a name written using bundle serialization version 3.
     *
     * @return deserialized name
     * @throws IOException if an I/O error occurs
     */
    private Name readName() throws IOException {
        int b = in.readUnsignedByte();
        if ((b & 0x80) == 0) {
            return BundleNames.indexToName(b);
        } else {
            String uri;
            int ns = (b >> 4) & 0x07;
            if (ns < namespaces.length && namespaces[ns] != null) {
                uri = namespaces[ns];
            } else {
                uri = readString();
                if (ns < namespaces.length) {
                    namespaces[ns] = uri;
                }
            }

            String local = new String(readBytes((b & 0x0f) + 1, 0x10), StandardCharsets.UTF_8);

            return NameFactoryImpl.getInstance().create(uri, local);
        }
    }

    /**
     * Deserializes a variable-length integer written using bundle
     * serialization version 3.
     *
     * @return deserialized integer
     * @throws IOException if an I/O error occurs
     */
    private int readVarInt() throws IOException {
        int b = in.readUnsignedByte();
        if ((b & 0x80) == 0) {
            return b;
        } else {
            return readVarInt() << 7 | b & 0x7f;
        }
    }

    private int readVarInt(int value, int base) throws IOException {
        if (value < base) {
            return value;
        } else {
            return readVarInt() + base;
        }
    }

    /**
     * Deserializes a variable-length long written using bundle
     * serialization version 3.
     *
     * @return deserialized long
     * @throws IOException if an I/O error occurs
     */
    private long readVarLong() throws IOException {
        long value = 0;
        int bits = 0;
        long b;
        do {
            b = in.readUnsignedByte();
            if (bits < 57) {
                value = (b & 0x7f) << 57 | value >>> 7;
                bits += 7;
            } else {
                value = (b & 0x01) << 63 | value >>> 1;
                bits = 64;
            }
        } while ((b & 0x80) != 0);
        value = value >>> (64 - bits);
        if ((value & 1) != 0) {
            return ~(value >>> 1);
        } else {
            return value >>> 1;
        }
    }

    /**
     * Deserializes a specially encoded date written using bundle
     * serialization version 3.
     *
     * @return deserialized date
     * @throws IOException if an I/O error occurs
     */
    private Calendar readDate() throws IOException {
        long ts = readVarLong();

        TimeZone tz;
        if ((ts & 1) == 0) {
            tz = COMMON_TIMEZONES[0];
            ts >>= 1;
        } else if ((ts & 2) == 0) {
            tz = COMMON_TIMEZONES[((int) ts >> 2) & 0x1f]; // 5 bits;
            ts >>= 7;
        } else {
            int m = ((int) ts << 19) >> 21; // 11 bits, sign-extended
            int h = m / 60;
            String s;
            if (m < 0) {
                s = String.format("GMT-%02d:%02d", -h, h * 60 - m);
            } else {
                s = String.format("GMT+%02d:%02d", h, m - h * 60);
            }
            tz = TimeZone.getTimeZone(s);
            ts >>= 13;
        }

        int u = 0;
        int s = 0;
        int m = 0;
        int h = 0;
        int type = (int) ts & 3;
        ts >>= 2;
        switch (type) {
        case 3:
            u = (int) ts & 0x3fffffff; // 30 bits
            s = u / 1000;
            m = s / 60;
            h = m / 60;
            m -= h * 60;
            s -= (h * 60 + m) * 60;
            u -= ((h * 60 + m) * 60 + s) * 1000;
            ts >>= 30;
            break;
        case 2:
            m = (int) ts & 0x07ff; // 11 bits
            h = m / 60;
            m -= h * 60;
            ts >>= 11;
            break;
        case 1:
            h = (int) ts & 0x1f; // 5 bits
            ts >>= 5;
            break;
        }

        int d = (int) ts & 0x01ff; // 9 bits;
        ts >>= 9;
        int y = (int) (ts + 2010);

        Calendar value = Calendar.getInstance(tz);
        if (y <= 0) {
            value.set(Calendar.YEAR, 1 - y);
            value.set(Calendar.ERA, GregorianCalendar.BC);
        } else {
            value.set(Calendar.YEAR, y);
            value.set(Calendar.ERA, GregorianCalendar.AD);
        }
        value.set(Calendar.DAY_OF_YEAR, d);
        value.set(Calendar.HOUR_OF_DAY, h);
        value.set(Calendar.MINUTE, m);
        value.set(Calendar.SECOND, s);
        value.set(Calendar.MILLISECOND, u);

        return value;
    }

    private String readString() throws IOException {
        if (version >= BundleBinding.VERSION_3) {
            return new String(readBytes(0, 0), StandardCharsets.UTF_8);
        } else {
            return in.readUTF();
        }
    }

    private byte[] readBytes(int len, int base) throws IOException {
        byte[] bytes = new byte[readVarInt(len, base)];
        in.readFully(bytes);
        return bytes;
    }

}
