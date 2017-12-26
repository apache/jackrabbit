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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;

/**
 * This utility class can dump the contents of a node bundle. This class is
 * based on BundleReader, but is able to dump even if the data is corrupt
 * (unlike the BundleReader). The class does not have any dependencies so it can
 * be run from the command line without problems (without having to add any jar
 * files to the classpath).
 */
public class BundleDumper {

    private static final int VERSION_1 = 1;
    private static final int VERSION_2 = 2;
    private static final int VERSION_3 = 3;

    private static final int BINARY_IN_BLOB_STORE = -1;
    private static final int BINARY_IN_DATA_STORE = -2;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public static final int UNDEFINED = 0, STRING = 1, BINARY = 2, LONG = 3, DOUBLE = 4, DATE = 5, BOOLEAN = 6,
        NAME = 7, PATH = 8, REFERENCE = 9, WEAKREFERENCE = 10, URI = 11, DECIMAL = 12;

    private static final String[] NAMES = { "undefined", "String", "Binary", "Long", "Double", "Date", "Boolean",
        "Name", "Path", "Reference", "WeakReference", "URI", "Decimal" };

    private StringBuilder buffer = new StringBuilder();

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

    /**
     * Wrapper for reading structured data from the input stream.
     */
    private DataInputStream in;

    private int version;

    private final String[] namespaces =
        // NOTE: The length of this array must be seven
        { "", null, null, null, null, null, null };


    static final UUID NULL_PARENT_ID =
        UUID.fromString("bb4e9d10-d857-11df-937b-0800200c9a66");


    public static void main(String... args) throws IOException {
        new BundleDumper().run(args);
    }

    void run(String... args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java " + getClass().getName() + " <fileName>");
            System.out.println("where the file name points to a node bundle.");
            return;
        }
        RandomAccessFile f = new RandomAccessFile(args[0], "r");
        byte[] bundle = new byte[(int) f.length()];
        f.readFully(bundle);
        f.close();
        System.out.println(dump(bundle));
    }

    public String dump(byte[] bundle) throws IOException {
        try {
            ByteArrayInputStream bin = new ByteArrayInputStream(bundle);
            this.in = new DataInputStream(bin);
            version = in.readUnsignedByte();
            buffer.append("version: ").append(version).append("\n");
            if (version >= VERSION_3) {
                readBundleNew();
            } else {
                readBundleOld();
            }
        } catch (Exception e) {
            buffer.append("\n");
            buffer.append("error: ").append(e.toString());
        }
        return buffer.toString();
    }

    private void readBundleNew() throws IOException {
        // node type
        buffer.append("nodeTypeName: ").append(readName()).append("\n");

        // parentUUID
        UUID parentId = readNodeId();
        buffer.append("parentId: ").append(parentId).append("\n");
        if (NULL_PARENT_ID.equals(parentId)) {
            parentId = null;
            buffer.append("parentId is null\n");
        }

        // read modcount
        buffer.append("modCount: ").append((short) readVarInt()).append("\n");

        int b = in.readUnsignedByte();
        buffer.append("referenceable: ").append((b & 1) != 0).append("\n");

        // mixin types
        int mn = readVarInt((b >> 7) & 1, 1);
        if (mn == 1) {
            buffer.append("mixing type:").append(readName()).append("\n");
        } else if (mn > 1) {
            buffer.append("mixing type count:").append(mn).append("\n");
            for (int i = 0; i < mn; i++) {
                buffer.append("mixing type:").append(readName()).append("\n");
            }
        }

        // properties
        int pn = readVarInt((b >> 4) & 7, 7);
        for (int i = 0; i < pn; i++) {
            buffer.append("property: ").append(readName()).append("\n");
            readPropertyEntry();
        }

        // child nodes (list of name/uuid pairs)
        int nn = readVarInt((b >> 2) & 3, 3);
        for (int i = 0; i < nn; i++) {
            buffer.append("child node: ").append(readQName()).
                    append(" id: ").append(readNodeId()).append("\n");
        }

        // read shared set
        int sn = readVarInt((b >> 1) & 1, 1);
        if (sn == 1) {
            buffer.append("shared set:").append(readNodeId()).append("\n");
        } else if (sn > 1) {
            buffer.append("shared set count:").append(sn).append("\n");
            for (int i = 0; i < sn; i++) {
                buffer.append("shared set:").append(readNodeId()).append("\n");
            }
        }
    }

    private void readBundleOld() throws IOException {
        // read primary type...special handling
        int a = in.readUnsignedByte();
        int b = in.readUnsignedByte();
        int c = in.readUnsignedByte();
        String uri = "#" + (a << 16 | b << 8 | c);
        String local = "#" + in.readInt();
        buffer.append("nodeTypeName: ").append(uri).append(":").append(local).append("\n");

        // parentUUID
        buffer.append("parentUUID: ").append(readNodeId()).append("\n");

        // definitionId
        buffer.append("definitionId: ").append(in.readUTF()).append("\n");

        // mixin types
        String name = readIndexedQName();
        if (name != null) {
            do {
                buffer.append("mixin: ").append(name).append("\n");
                name = readIndexedQName();
            } while (name != null);
        } else {
            buffer.append("mixins: -\n");
        }

        // properties
        name = readIndexedQName();
        while (name != null) {
            buffer.append("property: ").append(name).append("\n");
            readPropertyEntry();
            name = readIndexedQName();
        }

        // set referenceable flag
        buffer.append("referenceable: ").append(in.readBoolean()).append("\n");

        // child nodes (list of uuid/name pairs)
        UUID childId = readNodeId();
        while (childId != null) {
            buffer.append("childId: ").append(childId).append(" ").append(readQName()).append("\n");
            childId = readNodeId();
        }

        // read modcount, since version 1.0
        if (version >= VERSION_1) {
            buffer.append("modCount: ").append(in.readShort()).append("\n");
        }

        // read shared set, since version 2.0
        if (version >= VERSION_2) {
            // shared set (list of parent uuids)
            UUID parentId = readNodeId();
            if (parentId != null) {
                do {
                    buffer.append("shared set parentId: ").append(parentId).append("\n");
                    parentId = readNodeId();
                } while (parentId != null);
            }
        }
    }

    private static String getType(int type) {
        try {
            return NAMES[type];
        } catch (Exception e) {
            return "unknown type " + type;
        }
    }

    /**
     * Deserializes a <code>PropertyState</code> from the data input stream.
     *
     * @param id the property id for the new property entry
     * @return the property entry
     * @throws IOException if an I/O error occurs.
     */
    private void readPropertyEntry()
            throws IOException {
        int count = 1;
        int type;
        if (version >= VERSION_3) {
            int b = in.readUnsignedByte();
            type = b & 0x0f;
            buffer.append("  type: ").append(getType(type)).append("\n");
            int len = b >>> 4;
            if (len != 0) {
                buffer.append("  multivalued\n");
                if (len == 0x0f) {
                    count = readVarInt() + 0x0f - 1;
                } else {
                    count = len - 1;
                }
            }
            buffer.append("  modcount: ").append((short) readVarInt()).append("\n");
        } else {
            // type and modcount
            type = in.readInt();
            buffer.append("  modcount: ").append((short) ((type >> 16) & 0x0ffff)).append("\n");
            type &= 0x0ffff;
            buffer.append("  type: ").append(getType(type)).append("\n");

            // multiValued
            boolean mv = in.readBoolean();
            if (mv) {
                buffer.append("  multivalued\n");
            }

            // definitionId
            buffer.append("  definitionId: ").append(in.readUTF()).append("\n");

            // count
            count = in.readInt();
            if (count != 1) {
                buffer.append("  count: ").append(count).append("\n");
            }
        }

        // values
        for (int i = 0; i < count; i++) {
            switch (type) {
                case BINARY:
                    int size = in.readInt();
                    if (size == BINARY_IN_DATA_STORE) {
                        buffer.append("  value: binary in datastore: ").append(readString()).append("\n");
                    } else if (size == BINARY_IN_BLOB_STORE) {
                        buffer.append("  value: binary in blobstore: ").append(readString()).append("\n");
                    } else {
                        // short values into memory
                        byte[] data = new byte[size];
                        in.readFully(data);
                        buffer.append("  value: binary: ").append(convertBytesToHex(data)).append("\n");
                    }
                    break;
                case DOUBLE:
                    buffer.append("  value: double: ").append(in.readDouble()).append("\n");
                    break;
                case DECIMAL:
                    buffer.append("  value: double: ").append(readDecimal()).append("\n");
                    break;
                case LONG:
                    if (version >= VERSION_3) {
                        buffer.append("  value: varLong: ").append(readVarLong()).append("\n");
                    } else {
                        buffer.append("  value: long: ").append(in.readLong()).append("\n");
                    }
                    break;
                case BOOLEAN:
                    buffer.append("  value: boolean: ").append(in.readBoolean()).append("\n");
                    break;
                case NAME:
                    buffer.append("  value: name: ").append(readQName()).append("\n");
                    break;
                case WEAKREFERENCE:
                    buffer.append("  value: weakreference: ").append(readNodeId()).append("\n");
                    break;
                case REFERENCE:
                    buffer.append("  value: reference: ").append(readNodeId()).append("\n");
                    break;
                case DATE:
                    if (version >= VERSION_3) {
                        buffer.append("  value: date: ").append(readDate()).append("\n");
                        break;
                    } // else fall through
                default:
                    if (version >= VERSION_3) {
                        buffer.append("  value: string: ").append(readString()).append("\n");
                    } else {
                        // because writeUTF(String) has a size limit of 64k,
                        // Strings are serialized as <length><byte[]>
                        int len = in.readInt();
                        byte[] bytes = new byte[len];
                        in.readFully(bytes);
                        buffer.append("  value: string: ").append(new String(bytes, StandardCharsets.UTF_8)).append("\n");
                    }
            }
        }
    }

    /**
     * Deserializes a node identifier
     *
     * @return the node id
     * @throws IOException in an I/O error occurs.
     */
    private UUID readNodeId() throws IOException {
        if (version >= VERSION_3 || in.readBoolean()) {
            long msb = in.readLong();
            long lsb = in.readLong();
            return new UUID(msb, lsb);
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
    private String readQName() throws IOException {
        if (version >= VERSION_3) {
            return readName();
        }

        String uri = "#" + in.readInt();
        String local = in.readUTF();
        return uri + ":" + local;
    }

    /**
     * Deserializes an indexed Name
     *
     * @return the qname
     * @throws IOException in an I/O error occurs.
     */
    private String readIndexedQName() throws IOException {
        if (version >= VERSION_3) {
            return readName();
        }

        int index = in.readInt();
        if (index < 0) {
            return null;
        } else {
            String uri = "#" + index;
            String local = "#" + in.readInt();
            return uri + ":" + local;
        }
    }

    /**
     * Deserializes a name written using bundle serialization version 3.
     *
     * @return deserialized name
     * @throws IOException if an I/O error occurs
     */
    private String readName() throws IOException {
        int b = in.readUnsignedByte();
        if ((b & 0x80) == 0) {
            return "indexToName #" + b;
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
            return uri + ":" + local;
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
        if (version >= VERSION_3) {
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

    public static String convertBytesToHex(byte[] value) {
        int len = value.length;
        char[] buff = new char[len + len];
        char[] hex = HEX;
        for (int i = 0; i < len; i++) {
            int c = value[i] & 0xff;
            buff[i + i] = hex[c >> 4];
            buff[i + i + 1] = hex[c & 0xf];
        }
        return new String(buff);
    }

}
