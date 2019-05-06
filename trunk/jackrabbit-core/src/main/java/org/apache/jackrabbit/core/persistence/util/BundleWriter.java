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
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOExceptionWithCause;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle.ChildNodeEntry;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle.PropertyEntry;
import org.apache.jackrabbit.spi.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle serializer.
 *
 * @see BundleReader
 */
class BundleWriter {

    /** Logger instance */
    private static Logger log = LoggerFactory.getLogger(BundleWriter.class);

    private final BundleBinding binding;

    private final DataOutputStream out;

    /**
     * The default namespace and the first six other namespaces used in this
     * bundle. Used by the {@link #writeName(Name)} method to keep track of
     * already seen namespaces.
     */
    private final String[] namespaces =
        // NOTE: The length of this array must be seven
        { Name.NS_DEFAULT_URI, null, null, null, null, null, null };

    /**
     * Creates a new bundle serializer.
     *
     * @param binding bundle binding
     * @param stream stream to which the bundle will be written
     * @throws IOException if an I/O error occurs.
     */
    public BundleWriter(BundleBinding binding, OutputStream stream)
            throws IOException {
        assert namespaces.length == 7;
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
        NodeId parentId = bundle.getParentId();
        if (parentId == null) {
            parentId = BundleBinding.NULL_PARENT_ID;
        }
        writeNodeId(parentId);

        // write mod count
        writeVarInt(bundle.getModCount());

        Collection<Name> mixins = bundle.getMixinTypeNames();
        Collection<PropertyEntry> properties = bundle.getPropertyEntries();
        Collection<ChildNodeEntry> nodes = bundle.getChildNodeEntries();
        Collection<NodeId> shared = bundle.getSharedSet();

        int mn = mixins.size();
        int pn = properties.size();
        int nn = nodes.size();
        int sn = shared.size();
        int referenceable = 0;
        if (bundle.isReferenceable()) {
            referenceable = 1;
        }
        out.writeByte(
                Math.min(mn, 1) << 7
                | Math.min(pn, 7) << 4
                | Math.min(nn, 3) << 2
                | Math.min(sn, 1) << 1
                | referenceable);

        // mixin types
        writeVarInt(mn, 1);
        for (Name name : mixins) {
            writeName(name);
        }

        // properties
        writeVarInt(pn, 7);
        for (PropertyEntry property : properties) {
            writeState(property);
        }

        // child nodes (list of name/uuid pairs)
        writeVarInt(nn, 3);
        for (ChildNodeEntry child : nodes) {
            writeName(child.getName());   // name
            writeNodeId(child.getId());   // uuid
        }

        // write shared set
        writeVarInt(sn, 1);
        for (NodeId nodeId: shared) {
            writeNodeId(nodeId);
        }

        // set size of bundle
        bundle.setSize(out.size() - size);
    }

    /**
     * Serializes a property entry. The serialization begins with the
     * property name followed by a single byte that encodes the type and
     * multi-valuedness of the property:
     * <pre>
     * +-------------------------------+
     * |   mv count    |     type      |
     * +-------------------------------+
     * </pre>
     * <p>
     * The lower four bits encode the property type (0-12 in JCR 2.0) and
     * higher bits indicate whether this is a multi-valued property and how
     * many property values there are. A value of 0 is reserved for
     * single-valued properties (that are guaranteed to always have just a
     * single value), and all non-zero values indicate a multi-valued property.
     * <p>
     * In multi-valued properties the exact value of the "mv count" field is
     * the number of property values plus one and truncated at 15 (the highest
     * four-bit value). If there are 14 or more (14 + 1 == 15) property values,
     * then the number of additional values is serialized as a variable-length
     * integer (see {@link #writeVarInt(int)}) right after this byte.
     * <p>
     * The modification count of the property state is written next as a
     * variable-length integer, followed by the serializations of all the
     * values of this property.
     *
     * @param state the property entry to store
     * @throws IOException if an I/O error occurs.
     */
    private void writeState(NodePropBundle.PropertyEntry state)
            throws IOException {
        writeName(state.getName());

        InternalValue[] values = state.getValues();

        int type = state.getType();
        if (type < 0 || type > 0xf) {
            throw new IOException("Illegal property type " + type);
        }
        if (state.isMultiValued()) {
            int len = values.length + 1;
            if (len < 0x0f) {
                out.writeByte(len << 4 | type);
            } else {
                out.writeByte(0xf0 | type);
                writeVarInt(len - 0x0f);
            }
        } else {
            if (values.length != 1) {
                throw new IOException(
                        "Single values property with " + values.length + " values: " + 
                        state.getName());
            }
            out.writeByte(type);
        }

        writeVarInt(state.getModCount());

        // values
        for (int i = 0; i < values.length; i++) {
            InternalValue val = values[i];
            switch (type) {
                case PropertyType.BINARY:
                    try {
                        long size = val.getLength();
                        if (val.isInDataStore()) {
                            out.writeInt(BundleBinding.BINARY_IN_DATA_STORE);
                            writeString(val.toString());
                        } else if (binding.dataStore != null) {
                            writeSmallBinary(val, state, i);
                        } else if (size < 0) {
                            log.warn("Blob has negative size. Potential loss of data. "
                                    + "id={} idx={}", state.getId(), String.valueOf(i));
                            out.writeInt(0);
                            values[i] = InternalValue.create(new byte[0]);
                            val.discard();
                        } else if (size > binding.getMinBlobSize()) {
                            // special handling required for binary value:
                            // spool binary value to file in blob store
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
                                    throw new IOExceptionWithCause(msg, e);
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
                            writeString(blobId);   // value
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
                        throw new IOExceptionWithCause(msg, e);
                    }
                    break;
                case PropertyType.DOUBLE:
                    try {
                        out.writeDouble(val.getDouble());
                    } catch (RepositoryException e) {
                        throw convertToIOException(type, e);
                    }
                    break;
                case PropertyType.DECIMAL:
                    try {
                        writeDecimal(val.getDecimal());
                    } catch (RepositoryException e) {
                        throw convertToIOException(type, e);
                    }
                    break;
                case PropertyType.LONG:
                    try {
                        writeVarLong(val.getLong());
                    } catch (RepositoryException e) {
                        throw convertToIOException(type, e);
                    }
                    break;
                case PropertyType.BOOLEAN:
                    try {
                        out.writeBoolean(val.getBoolean());
                    } catch (RepositoryException e) {
                        throw convertToIOException(type, e);
                    }
                    break;
                case PropertyType.NAME:
                    try {
                        writeName(val.getName());
                    } catch (RepositoryException e) {
                        throw convertToIOException(type, e);
                    }
                    break;
                case PropertyType.WEAKREFERENCE:
                case PropertyType.REFERENCE:
                    writeNodeId(val.getNodeId());
                    break;
                case PropertyType.DATE:
                    try {
                        writeDate(val.getCalendar());
                    } catch (RepositoryException e) {
                        throw convertToIOException(type, e);
                    }
                    break;
                case PropertyType.STRING:
                case PropertyType.PATH:
                case PropertyType.URI:
                    writeString(val.toString());
                    break;
                default:
                    throw new IOException("Inknown property type: " + type);
            }
        }
    }
    
    private static IOException convertToIOException(int propertyType, Exception e) {
        String typeName = PropertyType.nameFromValue(propertyType);
        String message = "Unexpected error for property type "+ typeName +" value.";
        log.error(message, e);
        return new IOExceptionWithCause(message, e);
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
            throw new IOExceptionWithCause(msg, e);
        }
    }

    /**
     * Serializes a node identifier
     *
     * @param id the node id
     * @throws IOException in an I/O error occurs.
     */
    private void writeNodeId(NodeId id) throws IOException {
        out.writeLong(id.getMostSignificantBits());
        out.writeLong(id.getLeastSignificantBits());
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
            writeString(decimal.toString());
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
     * The three-bit namespace index identifies the namespace of the name.
     * The serializer keeps track of the default namespace (value 0) and at
     * most six other other namespaces (values 1-6), in the order they appear
     * in the bundle. When one of these six custom namespaces first appears
     * in the bundle, then the namespace URI is written using
     * {@link #writeString(String)} right after this byte.
     * Later uses of such a namespace simply refers back to the already read
     * namespace URI string. Any other namespaces are identified with value 7
     * and always written to the bundle after this byte.
     * <p>
     * The four-bit name length field indicates the length (in UTF-8 bytes)
     * of the local part of the name. Since zero-length local names are not
     * allowed, the length is first decremented by one before storing in this
     * field. The UTF-8 byte sequence is written out after this byte and the
     * possible namespace URI string. If the length of the local name is
     * larger than 15 (i.e. would be stored as 0x0f or more), then the value
     * 0x0f is stored as the name length and the name string is written as
     * UTF-8 using {@link #writeBytes(byte[], int)} with a base length of
     * 0x10 (0x0f + 1).
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
            int ns = 0;
            while (ns < namespaces.length
                    && namespaces[ns] != null
                    && !namespaces[ns].equals(uri)) {
                ns++;
            }

            String local = name.getLocalName();
            if (local.length() == 0) {
                throw new IOException("Attempt to write an empty local name: " + name);
            }
            byte[] bytes = local.getBytes(StandardCharsets.UTF_8);
            int len = Math.min(bytes.length - 1, 0x0f);

            out.writeByte(0x80 | ns << 4 | len);
            if (ns == namespaces.length || namespaces[ns] == null) {
                writeString(uri);
                if (ns < namespaces.length) {
                    namespaces[ns] = uri;
                }
            }
            if (len != 0x0f) {
                out.write(bytes);
            } else {
                writeBytes(bytes, 0x0f + 1);
            }
        }
    }

    /**
     * Serializes an integer using a variable-length encoding that favors
     * small positive numbers. The serialization consists of one to five
     * bytes of the following format:
     * <pre>
     * +-------------------------------+
     * | c | 7 least significant bits  |
     * +-------------------------------+
     * </pre>
     * <p>
     * If the given integer fits in seven bits (i.e. the value between
     * 0 and 127, inclusive), then it is written as-is in a single byte.
     * Otherwise the continuation flag <code>c</code> is set and the least
     * significant seven bits are written together with the flag as a single
     * byte. The integer is then shifed right seven bits and the process
     * continues from the beginning.
     * <p>
     * This format uses a single byte for values 0-127, two bytes for
     * 128-16343, three for 16343-2097151, four for 2097152-268435455
     * and five bytes for all other 32-bit numbers (including negative ones).
     *
     * @param integer integer value
     * @throws IOException if an I/O error occurs
     */
    private void writeVarInt(int value) throws IOException {
        while (true) {
            int b = value & 0x7f;
            if (b != value) {
                out.writeByte(b | 0x80);
                value >>>= 7; // unsigned shift
            } else {
                out.writeByte(b);
                return;
            }
        }
    }

    private void writeVarInt(int value, int base) throws IOException {
        if (value >= base) {
            writeVarInt(value - base);
        }
    }

    /**
     * Serializes a long value using a variable length encoding like the
     * one used by {@link #writeVarInt(int)} for integer values. Before
     * writing out, the value is first normalized to an unsigned value
     * by moving the sign bit to be the end negating the other bits of
     * a negative value. This normalization step maximizes the number of
     * zero high order bits for typical small values (positive or negative),
     * and thus keeps the serialization short.
     *
     * @param value long value
     * @throws IOException if an I/O error occurs
     */
    private void writeVarLong(long value) throws IOException {
        // Normalize to an unsigned value with the sign as the lowest bit
        if (value < 0) {
            value = ~value << 1 | 1;
        } else {
            value <<= 1;
        }
        while (true) {
            long b = value & 0x7f;
            if (b != value) {
                out.writeByte((int) b | 0x80);
                value >>>= 7; // unsigned shift
            } else {
                out.writeByte((int) b);
                return;
            }
        }
    }

    /**
     * Serializes a JCR date value using the {@link #writeVarLong(long)}
     * serialization on a special 64-bit date encoding. This encoding maps
     * the <code>sYYYY-MM-DDThh:mm:ss.sssTZD</code> date format used by
     * JCR to an as small 64 bit integer (positive or negative) as possible,
     * while preserving full accuracy (including time zone offsets) and
     * favouring common levels of accuracy (per minute, hour and day) over
     * full millisecond level detail.
     * <p>
     * Each date value is mapped to separate timestamp and timezone fields,
     * both of whose lenghts are variable: 
     * <pre>
     * +----- ... ------- ... --+
     * |  timestamp  | timezone |
     * +----- ... ------- ... --+
     * </pre>
     * <p>
     * The type and length of the timezone field can be determined by looking
     * at the two least significant bits of the value:
     * <dl>
     *   <dt><code>?0</code></dt>
     *   <dd>
     *     UTC time. The length of the timezone field is just one bit,
     *     i.e. the second bit is already a part of the timestamp field.
     *   </dd>
     *   <dt><code>01</code></dt>
     *   <dd>
     *     The offset is counted as hours from UTC, and stored as the number
     *     of hours (positive or negative) in the next 5 bits (range from
     *     -16 to +15 hours), making the timezone field 7 bits long in total.
     *   </dd>
     *   <dt><code>11</code></dt>
     *   <dd>
     *     The offset is counted as hours and minutes from UTC, and stored
     *     as the total minute offset (positive or negative) in the next
     *     11 bits (range from -17 to +17 hours), making the timezone field
     *     13 bits long in total.
     *   </dd>
     * </dl>
     * <p>
     * The remaining 51-63 bits of the encoded value make up the timestamp
     * field that also uses the two least significant bits to indicate the
     * type and length of the field:
     * <dl>
     *   <dt><code>00</code></dt>
     *   <dd>
     *     <code>sYYYY-MM-DDT00:00:00.000</code>, i.e. midnight of the
     *     specified date. The next 9 bits encode the day within the year
     *     (starting from 1, maximum value 366) and the remaining bits are
     *     used for the year, stored as an offset from year 2010.
     *   </dd>
     *   <dt><code>01</code></dt>
     *   <dd>
     *     <code>sYYYY-MM-DDThh:00:00.000</code>, i.e. at the hour. The
     *     next 5 bits encode the hour within the day (starting from 0,
     *     maximum value 23) and the remaining bits are used as described
     *     above for the date.
     *   </dd>
     *   <dt><code>10</code></dt>
     *   <dd>
     *     <code>sYYYY-MM-DDThh:mm:00.000</code>, i.e. at the minute. The
     *     next 11 bits encode the minute within the day (starting from 0,
     *     maximum value 1439) and the remaining bits are used as described
     *     above for the date.
     *   </dd>
     *   <dt><code>11</code></dt>
     *   <dd>
     *     <code>sYYYY-MM-DDThh:mm:ss.sss</code>, i.e. full millisecond
     *     accuracy. The next 30 bits encode the millisecond within the
     *     day (starting from 0, maximum value 87839999) and the remaining
     *     bits are used as described above for the date.
     *   </dd>
     * </dl>
     * <p>
     * With full timezone and millisecond accuracies, this encoding leaves
     * 10 bits (64 - 9 - 30 - 2 - 11 - 2) for the date offset, which allows
     * for representation of all timestamps between years 1498 and 2521.
     * Timestamps outside this range and with a minute-level timezone offset
     * are automatically truncated to minute-level accuracy to support the
     * full range of years -9999 to 9999 specified in JCR.
     * <p>
     * Note that the year, day of year, and time of day values are stored
     * as separate bit sequences to avoid problems with changing leap second
     * or leap year definitions. Bit fields are used for better encoding and
     * decoding performance than what would be possible with the slightly more
     * space efficient mechanism of using multiplication and modulo divisions
     * to separate the different timestamp fields.
     *
     * @param value date value
     * @throws IOException if an I/O error occurs
     */
    private void writeDate(Calendar value) throws IOException {
        int y = value.get(Calendar.YEAR);
        if (value.isSet(Calendar.ERA)
                && value.get(Calendar.ERA) == GregorianCalendar.BC) {
             y = 1 - y; // convert to an astronomical year
        }
        y -= 2010; // use a recent offset NOTE: do not change this!

        int d = value.get(Calendar.DAY_OF_YEAR);
        int h = value.get(Calendar.HOUR_OF_DAY);
        int m = value.get(Calendar.MINUTE);
        int s = value.get(Calendar.SECOND);
        int u = value.get(Calendar.MILLISECOND);
        int z = value.getTimeZone().getOffset(value.getTimeInMillis()) / (60 * 1000);
        int zh = z / 60;
        int zm = z - zh * 60;

        long ts = y << 9 | d & 0x01ff;

        if ((u != 0 || s != 0) && ((-512 <= y && y < 512) || zm == 0)) {
            ts <<= 30;
            ts |= (((h * 60 + m) * 60 + s) * 1000 + u) & 0x3fffffff; // 30 bits
            ts <<= 2;
            ts |= 3;
        } else if (m != 0) {
            ts <<= 11;
            ts |= (h * 60 + m) & 0x07ff; // 11 bits
            ts <<= 2;
            ts |= 2;
        } else if (h != 0) {
            ts <<= 5;
            ts |= h & 0x1f; // 5 bits
            ts <<= 2;
            ts |= 1;
        } else {
            ts <<= 2;
        }

        if (zm != 0) {
            ts <<= 11;
            ts |= z & 0x07ff; // 11 bits
            writeVarLong(ts << 2 | 3);
        } else if (zh != 0) {
            ts <<= 5;
            ts |= zh & 0x1f; // 5 bits
            writeVarLong(ts << 2 | 1);
        } else {
            writeVarLong(ts << 1);
        }
    }

    /**
     * Serializes a string in UTF-8. The length of the UTF-8 byte sequence
     * is first written as a variable-length string (see
     * {@link #writeVarInt(int)}), and then the sequence itself is written.
     *
     * @param value string value
     * @throws IOException if an I/O error occurs
     */
    private void writeString(String value) throws IOException {
        writeBytes(value.getBytes(StandardCharsets.UTF_8), 0);
    }

    /**
     * Serializes the given array of bytes. The length of the byte array is
     * first written as a {@link #writeVarInt(int) variable length integer},
     * followed by the given bytes.
     *
     * @param bytes the bytes to be serialized
     * @param base optional base length
     * @throws IOException if an I/O error occurs
     */
    private void writeBytes(byte[] bytes, int base) throws IOException {
        assert bytes.length >= base;
        writeVarInt(bytes.length - base);
        out.write(bytes);
    }

}
