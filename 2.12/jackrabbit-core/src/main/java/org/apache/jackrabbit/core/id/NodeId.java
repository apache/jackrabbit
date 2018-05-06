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
package org.apache.jackrabbit.core.id;

import java.util.Random;
import java.util.UUID;

/**
 * Node identifier, i.e. an immutable 128 bit UUID.
 */
public class NodeId implements ItemId, Comparable<NodeId> {

    /**
     * The serial version UID.
     */
    private static final long serialVersionUID = 5773949574212570258L;

    /**
     * Chars in a UUID String.
     */
    public static final int UUID_FORMATTED_LENGTH = 36;

    /**
     * Number of bytes in a UUID (16).
     */
    public static final int UUID_BYTE_LENGTH = 16;

    /**
     * Returns a node identifier that is represented by the given UUID string.
     *
     * @param uuid the UUID string
     * @return the node identifier
     * @throws IllegalArgumentException if the given string is <code>null</code>
     *                                  or not a valid UUID
     */
    public static NodeId valueOf(String uuid) throws IllegalArgumentException {
        if (uuid != null) {
            return new NodeId(uuid);
        } else {
            throw new IllegalArgumentException("NodeId.valueOf(null)");
        }
    }

    /**
     * The most significant 64 bits (bytes 0-7) of the UUID.
     */
    private final long msb;

    /**
     * The least significant 64 bits (bytes 8-15) of the UUID.
     */
    private final long lsb;

    /**
     * Creates a node identifier from the given 128 bits.
     *
     * @param msb most significant 64 bits
     * @param lsb least significant 64 bits
     */
    public NodeId(long msb, long lsb) {
        this.msb = msb;
        this.lsb = lsb;
    }

    /**
     * Creates a node identifier from the given 16 bytes.
     *
     * @param bytes array of 16 bytes
     * @throws NullPointerException if the given array is <code>null</code>
     * @throws ArrayIndexOutOfBoundsException
     *             if the given array is less than 16 bytes long
     */
    public NodeId(byte[] bytes)
            throws NullPointerException, ArrayIndexOutOfBoundsException {
        this(   // Most significant 64 bits
                ((((long) bytes[0]) & 0xFF) << 56)
                + ((((long) bytes[1]) & 0xFF) << 48)
                + ((((long) bytes[2]) & 0xFF) << 40)
                + ((((long) bytes[3]) & 0xFF) << 32)
                + ((((long) bytes[4]) & 0xFF) << 24)
                + ((((long) bytes[5]) & 0xFF) << 16)
                + ((((long) bytes[6]) & 0xFF) << 8)
                + ((((long) bytes[7]) & 0xFF)),
                // Least significant 64 bits
                ((((long) bytes[8]) & 0xFF) << 56)
                + ((((long) bytes[9]) & 0xFF) << 48)
                + ((((long) bytes[10]) & 0xFF) << 40)
                + ((((long) bytes[11]) & 0xFF) << 32)
                + ((((long) bytes[12]) & 0xFF) << 24)
                + ((((long) bytes[13]) & 0xFF) << 16)
                + ((((long) bytes[14]) & 0xFF) << 8)
                + ((((long) bytes[15]) & 0xFF)));
    }

    /**
     * Creates a node identifier from the given UUID.
     *
     * @param uuid UUID
     */
    public NodeId(UUID uuid) {
        this(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    /**
     * Creates a node identifier from the given UUID string.
     *
     * @param uuidString UUID string
     * @throws IllegalArgumentException if the UUID string is invalid
     */
    public NodeId(String uuidString) throws IllegalArgumentException {
        // e.g. f81d4fae-7dec-11d0-a765-00a0c91e6bf6
        //      012345678901234567890123456789012345
        if (uuidString.length() != UUID_FORMATTED_LENGTH) {
            throw new IllegalArgumentException(uuidString);
        }
        long m = 0, x = 0;
        for (int i = 0; i < UUID_FORMATTED_LENGTH; i++) {
            int c = uuidString.charAt(i);
            switch (i) {
            case 18:
                m = x;
                x = 0;
                // fall through
            case 8:
            case 13:
            case 23:
                if (c != '-') {
                    throw new IllegalArgumentException(uuidString);
                }
                break;
            default:
                if (c >= '0' && c <= '9') {
                    x = (x << 4) | (c - '0');
                } else if (c >= 'a' && c <= 'f') {
                    x = (x << 4) | (c - 'a' + 0xa);
                } else if (c >= 'A' && c <= 'F') {
                    x = (x << 4) | (c - 'A' + 0xa);
                } else {
                    throw new IllegalArgumentException(uuidString);
                }
            }
        }
        this.msb = m;
        this.lsb = x;
    }

    /**
     * Creates a random node identifier using a secure random number generator.
     */
    public static NodeId randomId() {
        Random random = SeededSecureRandom.getInstance();
        return new NodeId(
                // Most significant 64 bits, with version field set to 4
                random.nextLong() & 0xFfffFfffFfff0fffL | 0x0000000000004000L,
                // Least significant 64 bits, with variant field set to IETF
                random.nextLong() & 0x3fffFfffFfffFfffL | 0x8000000000000000L
            );
    }

    /**
     * Returns the 64 most significant bits of this identifier.
     *
     * @return 64 most significant bits
     */
    public long getMostSignificantBits() {
        return msb;
    }

    /**
     * Returns the 64 least significant bits of this identifier.
     *
     * @return 64 least significant bits
     */
    public long getLeastSignificantBits() {
        return lsb;
    }

    /**
     * Returns the 16 bytes of this identifier.
     *
     * @return newly allocated array of 16 bytes
     */
    public byte[] getRawBytes() {
        return new byte[] {
            (byte) (msb >> 56), (byte) (msb >> 48), (byte) (msb >> 40),
            (byte) (msb >> 32), (byte) (msb >> 24), (byte) (msb >> 16),
            (byte) (msb >> 8), (byte) msb,
            (byte) (lsb >> 56), (byte) (lsb >> 48), (byte) (lsb >> 40),
            (byte) (lsb >> 32), (byte) (lsb >> 24), (byte) (lsb >> 16),
            (byte) (lsb >> 8), (byte) lsb
        };
    }

    //--------------------------------------------------------------< ItemId >

    /**
     * Returns <code>true</code> to indicate that this is a node identifier.
     *
     * @return always <code>true</code>
     */
    public boolean denotesNode() {
        return true;
    }

    //----------------------------------------------------------< Comparable >

    /**
     * Compares this identifier to the given other one.
     *
     * @param that other identifier
     * @return -1, 0 or +1 if this identifier is less than, equal to,
     *         or greater than the given other identifier
     */
    public int compareTo(NodeId that) {
        // This is not a 128 bit integer comparison! See also JCR-687.
        if (msb < that.msb) {
            return -1;
        } else if (msb > that.msb) {
            return 1;
        } else if (lsb < that.lsb) {
            return -1;
        } else if (lsb > that.lsb) {
            return 1;
        } else {
            return 0;
        }
    }

    //--------------------------------------------------------------< Object >

    /**
     * Returns the UUID string representation of this identifier.
     *
     * @see UUID#toString()
     * @return UUID string
     */
    public String toString() {
        char[] retval = new char[36];
        hex4(retval, 0, msb >>> 48);
        hex4(retval, 4, msb >>> 32);
        retval[8]  = '-';
        hex4(retval, 9, msb >>> 16);
        retval[13] = '-';
        hex4(retval, 14, msb);
        retval[18] = '-';
        hex4(retval, 19, lsb >>> 48);
        retval[23] = '-';
        hex4(retval, 24, lsb >>> 32);
        hex4(retval, 28, lsb >>> 16);
        hex4(retval, 32, lsb);
        return new String(retval);
    }

    private static final void hex4(char[] c, int index, long value) {
        for (int i = 0; i < 4; i++) {
            long v = (value >>> (12 - i * 4)) & 0xf;
            if (v < 10) {
                c[index + i] = (char) (v + '0');
            } else {
                c[index + i] = (char) (v - 10 + 'a');
            }
        }
    }

    /**
     * Compares two UUID for equality.
     *
     * @see Object#equals(Object)
     */
    public boolean equals(Object that) {
        return that instanceof NodeId
            && msb == ((NodeId) that).msb
            && lsb == ((NodeId) that).lsb;
    }

    /**
     * Returns a hash code of this identifier.
     *
     * @return hash code
     */
    public int hashCode() {
        return (int) ((msb >>> 32) ^ msb ^ (lsb >>> 32) ^ lsb);
    }

}
