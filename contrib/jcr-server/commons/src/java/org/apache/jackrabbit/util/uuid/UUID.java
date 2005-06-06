/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.util.uuid;

import java.io.DataInput;
import java.io.IOException;
import java.io.Serializable;
import java.util.StringTokenizer;

/** XXX begin modification by stefan@apache.org */
/*import org.apache.commons.id.IdentifierUtils;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;*/
/** XXX end modification by stefan@apache.org */

/**
 * <p><code>UUID</code> represents a Universally Unique Identifier per IETF
 * Draft specification. For more information regarding the IETF Draft UUID
 * specification</p>
 * <p/>
 * <p>See: http://www.ietf.org/internet-drafts/draft-mealling-uuid-urn-01.txt</p>
 * <p/>
 * <p>Copied from the Jakarta Commons-Id project</p>
 * <p/>
 * todo remove and use official commons-id release as soon as it is available
 */

public class UUID implements Constants, Serializable, Comparable {

    /**
     * byte array to store 128-bits composing this UUID
     */
    private byte[] rawBytes = new byte[UUID_BYTE_LENGTH];

    /**
     * Holds node identifier for this UUID
     */
    private Long node = null;

    /**
     * Holds timestamp for this UUID
     */
    private long timestamp = -1;

    /**
     * Holds the clock sequence field
     */
    private Short clockSq = null;

    /**
     * Holds the version field of this UUID
     */
    private int version = -1;

    /**
     * Holds the variant field of this UUID
     */
    private int variant = -1;

    /**
     * Holds the internal string value of the UUID
     */
    private String stringValue = null;

    /**
     * XXX begin modification by stefan@apache.org
     */
    private static VersionFourGenerator versionFourGenereator = new VersionFourGenerator();
    /** XXX end modification by stefan@apache.org */

    /**
     * Constructs a nil UUID
     */
    public UUID() {
        super();
    }

    /**
     * <p>Constructs a UUID from a 128 bit java.math.BigInteger.</p>
     * <p>Method is protected as their is no standard as to the internal representation of a UUID.
     * In this case a BigInteger is used with signum always positive.</p>
     *
     *  @param bigIntValue the 128 bit BigInteger to construct this UUID from.
     *  @throws IllegalArgumentException argument must be 128 bit
     */
    /* protected UUID(BigInteger bigIntValue) throws IllegalArgumentException {
         super();
         if (bigIntValue.bitLength() > UUID.UUID_BIT_LENGTH) {
             throw new IllegalArgumentException("UUID must be contructed using a 128 bit BigInteger");
         }
         numberValue = bigIntValue;
     } */

    /**
     * <p>Copy constructor.</p>
     *
     * @param copyFrom the UUID to copy to create this UUID.
     */
    public UUID(UUID copyFrom) {
        super();
        rawBytes = copyFrom.getRawBytes();
    }

    /**
     * <p>Constructs a UUID from a 16 byte array.</p>
     *
     * @param byteArray the 16 byte array to construct this UUID from.
     * @throws IllegalArgumentException argument must be 16 bytes
     */
    public UUID(byte[] byteArray) throws IllegalArgumentException {
        super();
        if (byteArray.length != UUID_BYTE_LENGTH) {
            throw new IllegalArgumentException("UUID must be contructed using a 16 byte array.");
        }
        // UUID must be immutable so a copy is used.
        System.arraycopy(byteArray, 0, rawBytes, 0, UUID_BYTE_LENGTH);
    }

    /**
     * <p>Constructs a UUID from a DataInput. Note if 16 bytes are not available this method will block.</p>
     *
     * @param input the datainput with 16 bytes to read in from.
     * @throws IOException exception if there is an IO problem also argument must contain 16 bytes.
     */
    public UUID(DataInput input) throws IOException {
        super();
        input.readFully(rawBytes, 0, UUID_BYTE_LENGTH);
    }

    /**
     * <p>Constructs a UUID from two long values in most significant byte, and least significant bytes order.</p>
     *
     * @param mostSignificant  - the most significant 8 bytes of the uuid to be constructed.
     * @param leastSignificant - the least significant 8 bytes of the uuid to be constructed.
     */
    public UUID(long mostSignificant, long leastSignificant) {
        rawBytes = Bytes.append(Bytes.toBytes(mostSignificant), Bytes.toBytes(leastSignificant));
    }

    /** XXX begin modification by stefan@apache.org */
    /**
     * <p>Constructs a UUID from a UUID formatted String.</p>
     *
     * @param uuidString the String representing a UUID to construct this UUID
     * @throws UUIDFormatException String must be a properly formatted UUID string
     */
    //public UUID(String uuidString) throws UUIDFormatException {
    public UUID(String uuidString) {
        //Calls the copy constructor
        this(UUID.fromString(uuidString));
    }
    /** XXX end modification by stefan@apache.org */

    /** XXX begin modification by stefan@apache.org */
    /**
     * <p>Parses a string for a UUID.</p>
     *
     * @param uuidString the UUID formatted String to parse.
     *                   XXX begin modification by stefan@apache.org
     * @return Returns a UUID or null if the formatted string could not be parsed.
     * @throws IllegalArgumentException the String must be a properly formatted UUID String.
     *                             XXX end modification by stefan@apache.org
     */
    //public static UUID fromString(String uuidString) throws UUIDFormatException {
    public static UUID fromString(String uuidString) throws IllegalArgumentException {
        String leanString = uuidString.toLowerCase();
        UUID tmpUUID = null;

        //Handle prefixed UUIDs
        // e.g. urn:uuid:f81d4fae-7dec-11d0-a765-00a0c91e6bf6
        int pos = uuidString.lastIndexOf(":");
        if (pos > 1) {
            leanString = uuidString.substring(++pos, uuidString.length());
        }

        //Check for 36 char length
        if (leanString.length() != UUID_FORMATTED_LENGTH) {
            //throw new UUIDFormatException();
            throw new IllegalArgumentException();
        }

        //Check for 5 fields
        StringTokenizer tok = new StringTokenizer(leanString, "-");
        if (tok.countTokens() != TOKENS_IN_UUID) {
            //throw new UUIDFormatException();
            throw new IllegalArgumentException();
        }

        //Remove the "-" from the formatted string and test token sizes
        StringBuffer buf = new StringBuffer(UUID_UNFORMATTED_LENGTH);
        String token = null;
        int count = 0;
        while (tok.hasMoreTokens()) {
            token = tok.nextToken();
            if (token.length() != TOKEN_LENGTHS[count++]) {
                //throw new UUIDFormatException();
                throw new IllegalArgumentException();
            }
            buf.append(token);
        }

        //Create from the hex value
        /** XXX begin modification by stefan@apache.org */
/*
        try {
            char[] chars = buf.toString().toCharArray();
            tmpUUID = new UUID(Hex.decodeHex(chars));
        } catch (DecoderException de) {
              throw new UUIDFormatException(de.getMessage());
       }
*/
        String s = buf.toString();
        byte[] bytes = new byte[UUID_BYTE_LENGTH];
        for (int i = 0, j = 0; i < (UUID_BYTE_LENGTH * 2); i += 2) {
            bytes[j++] = (byte) Integer.parseInt(s.substring(i, i + 2), 16);
        }
        tmpUUID = new UUID(bytes);
        /** XXX end modification by stefan@apache.org */

        return tmpUUID;
    }
    /** XXX end modification by stefan@apache.org */

    /**
     * <p>Returns a string representation of the UUID.</p>
     *
     * @return a string representation of the UUID formatted according to the specification.
     */
    public String toString() {
        //set string value if not set
        if (stringValue == null) {
            /** XXX begin modification by stefan@apache.org */
/*
            StringBuffer buf = new StringBuffer(new String(Hex.encodeHex(rawBytes)));
            while (buf.length() != UUID_UNFORMATTED_LENGTH) {
                buf.insert(0, "0");
            }
            buf.ensureCapacity(UUID_FORMATTED_LENGTH);
            buf.insert(FORMAT_POSITION1, '-');
            buf.insert(FORMAT_POSITION2, '-');
            buf.insert(FORMAT_POSITION3, '-');
            buf.insert(FORMAT_POSITION4, '-');
            stringValue = buf.toString();
*/
            char[] chars = new char[UUID_FORMATTED_LENGTH];
            for (int i = 0, j = 0; i < 16; i++) {
                chars[j++] = hexDigits[(rawBytes[i] >> 4) & 0x0f];
                chars[j++] = hexDigits[rawBytes[i] & 0x0f];
                if (i == 3 || i == 5 || i == 7 || i == 9) {
                    chars[j++] = '-';
                }
            }
            stringValue = new String(chars);
            /** XXX end modification by stefan@apache.org */
        }
        return stringValue;
    }

    /**
     * <p>Returns a urn representation of the UUID. This is same as the
     * toString() value prefixed with <code>urn:uuid:</code></p>
     *
     * @return Returns the urn string representation of the UUID
     */
    public String toUrn() {
        return URN_PREFIX + this.toString();
    }

    /**
     * <p>Compares two UUID for equality.</p>
     *
     * @see java.lang.Object#equals(Object)
     */

    public boolean equals(Object obj) {
        if (!(obj instanceof UUID)) {
            return false;
        }
        return Bytes.areEqual(((UUID) obj).getRawBytes(), rawBytes);
    }

    /**
     * <p>Returns a hash code value for the object.</p>
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        int iConstant = 37;
        int iTotal = 17;
        for (int i = 0; i < rawBytes.length; i++) {
            iTotal = iTotal * iConstant + rawBytes[i];
        }
        return iTotal;
    }

    /**
     * <p>Compares two UUID's for equality</p>
     *
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(Object compareTo) throws ClassCastException {
        if (!(compareTo instanceof UUID)) {
            throw new ClassCastException();
        }
        return (Bytes.compareTo(rawBytes, ((UUID) compareTo).getRawBytes()));
    }

    /**
     * <p>Returns the clock sequence value in the UUID. The clock sequence is a random assigned to a particular clock instance that
     * generated the time in the timestamp of a time based UUID.</p>
     *
     * @return the clock sequence value in the UUID.
     * @throws UnsupportedOperationException thrown if this is not a IETF variant or not a time-based UUID.
     */
    public int clockSequence() throws UnsupportedOperationException {
        //if variant is not mealling leach salz throw unsupported operation exception
        if (variant() != VARIANT_IETF_DRAFT || version() != VERSION_ONE) {
            throw new UnsupportedOperationException(WRONG_VAR_VER_MSG);
        }
        if (clockSq == null) {
            byte[] b = {((byte) (rawBytes[8] & 0x3F)), rawBytes[9]};
            clockSq = new Short(Bytes.toShort(b));
        }
        return clockSq.intValue();
    }

    /**
     * <p>Returns the version of the UUID.
     * <ul>
     * <li>VERSION_ONE - The time-based version</li>
     * <li>VERSION_TWO - DCE Security version, with embedded POSIX UIDs.</li>
     * <li>VERSION_THREE - Name based UUID.</li>
     * <li>VERSION_FOUR - Random based UUID.</li>
     * </ul>
     * </p>
     *
     * @return the version of the UUID.
     */
    public int version() {
        if (version == -1) {
            version = ((rawBytes[6] >>> 4) & 0x0F);
        }
        return version;
    }

    /**
     * <p>Returns the variant field of the UUID.</p>
     *
     * @return Returns the variant field of the UUID.
     * @see UUID#VARIANT_NCS_COMPAT
     * @see UUID#VARIANT_IETF_DRAFT
     * @see UUID#VARIANT_MS
     * @see UUID#VARIANT_FUTURE
     */
    public int variant() {
        if (variant == -1) {
            if ((rawBytes[8] & 0x80) == 0x0) {
                variant = VARIANT_NCS_COMPAT;
            } else if ((rawBytes[8] & 0x40) == 0x0) {
                variant = VARIANT_IETF_DRAFT;
            } else if ((rawBytes[8] & 0x20) == 0x0) {
                variant = VARIANT_MS;
            } else {
                variant = VARIANT_FUTURE;
            }
        }
        return variant;
    }

    /**
     * <p>Returns the node identifier found in this UUID. The specification was written such that this value holds the IEEE 802 MAC
     * address. The specification permits this value to be calculated from other sources other than the MAC.</p>
     *
     * @return the node identifier found in this UUID.
     * @throws UnsupportedOperationException thrown if this is not a IETF variant or not a time-based UUID.
     */
    public long node() throws UnsupportedOperationException {
        //if variant is not mealling leach salz throw unsupported operation exception
        if (variant() != VARIANT_IETF_DRAFT || version() != VERSION_ONE) {
            throw new UnsupportedOperationException(WRONG_VAR_VER_MSG);
        }
        if (node == null) {
            byte[] b = new byte[8];
            System.arraycopy(rawBytes, 10, b, 2, 6);
            node = new Long((Bytes.toLong(b) & 0xFFFFFFFFFFFFL));
        }
        return node.longValue();
    }

    /**
     * <p>Returns the timestamp value of the UUID as 100-nano second intervals since the Gregorian change offset (00:00:00.00, 15
     * October 1582 ).</p>
     *
     * @return the timestamp value of the UUID as 100-nano second intervals since the Gregorian change offset.
     * @throws UnsupportedOperationException thrown if this is not a IETF variant or not a time-based UUID.
     */
    public long timestamp() throws UnsupportedOperationException {
        //if variant is not mealling leach salz throw unsupported operation exception
        if (variant() != VARIANT_IETF_DRAFT || version() != VERSION_ONE) {
            throw new UnsupportedOperationException(WRONG_VAR_VER_MSG);
        }
        if (timestamp == -1) {
            byte[] longVal = new byte[8];
            System.arraycopy(rawBytes, TIME_HI_START_POS, longVal, TIME_HI_TS_POS, TIME_HI_BYTE_LEN);
            System.arraycopy(rawBytes, TIME_MID_START_POS, longVal, TIME_MID_TS_POS, TIME_MID_BYTE_LEN);
            System.arraycopy(rawBytes, TIME_LOW_START_POS, longVal, TIME_LOW_TS_POS, TIME_LOW_BYTE_LEN);
            longVal[TIME_HI_TS_POS] &= 0x0F;
            timestamp = Bytes.toLong(longVal);
        }
        return timestamp;
    }

    /**
     * <p>Returns the least significant bits stored in the uuid's internal structure.</p>
     *
     * @return the least significant bits stored in the uuid's internal structure.
     */
    long getLeastSignificantBits() {
        byte[] lsb = new byte[8];
        System.arraycopy(rawBytes, 8, lsb, 0, 8);
        return Bytes.toLong(lsb);
    }

    /**
     * <p>Returns the least significant bits stored in the uuid's internal structure.</p>
     *
     * @return the least significant bits stored in the uuid's internal structure.
     */
    long getMostSignificantBits() {
        byte[] msb = new byte[8];
        System.arraycopy(rawBytes, 0, msb, 0, 8);
        return Bytes.toLong(msb);
    }

    /**
     * <p>Returns a copy of the byte values contained in this UUID.
     *
     * @return a copy of the byte values contained in this UUID.
     */
    public byte[] getRawBytes() {
        byte[] ret = new byte[UUID_BYTE_LENGTH];
        System.arraycopy(rawBytes, 0, ret, 0, UUID_BYTE_LENGTH);
        return ret;
    }

    /**
     * <p>Returns a new version 4 UUID, based upon Random bits.</p>
     *
     * @return a new version 4 UUID, based upon Random bits.
     */
    /**
     * XXX begin modification by stefan@apache.org
     */
    //static UUID randomUUID() {
    public static UUID randomUUID() {
        //return (UUID) IdentifierUtils.UUID_VERSION_FOUR_GENERATOR.nextIdentifier();
        return (UUID) versionFourGenereator.nextIdentifier();
    }
    /** XXX end modification by stefan@apache.org */

    /** XXX begin modification by stefan@apache.org */
    /**
     * <p>Returns a new version 1 UUID, based upon node identifier and time stamp.</p>
     *
     * @return a new version 1 UUID, based upon node identifier and time stamp.
     */
/*    static UUID timeUUID() {
        return (UUID) IdentifierUtils.UUID_VERSION_ONE_GENERATOR.nextIdentifier();
    }*/
    /** XXX end modification by stefan@apache.org */

    /** XXX begin modification by stefan@apache.org */
    /**
     * <p>Returns a new version three UUID given a name and the namespace's UUID.</p>
     *
     * @param name String the name to calculate the UUID for.
     * @param namespace UUID assigned to this namespace.
     * @return a new version three UUID given a name and the namespace's UUID.
     */
/*    static UUID nameUUIDFromString(String name, UUID namespace) {
        byte[] nameAsBytes = name.getBytes();
        byte[] concat = new byte[UUID_BYTE_LENGTH + nameAsBytes.length];
        System.arraycopy(namespace.getRawBytes(), 0, concat, 0, UUID_BYTE_LENGTH);
        System.arraycopy(nameAsBytes, 0, concat, UUID_BYTE_LENGTH, nameAsBytes.length);
        byte[] raw = DigestUtils.md5(concat);
        //Set version
        raw[TIME_HI_AND_VERSION_BYTE_6] &= 0x0F;
        raw[TIME_HI_AND_VERSION_BYTE_6] |= (UUID.VERSION_THREE << 4);
        //Set variant
        raw[CLOCK_SEQ_HI_AND_RESERVED_BYTE_8] &= 0x3F; //0011 1111
        raw[CLOCK_SEQ_HI_AND_RESERVED_BYTE_8] |= 0x80; //1000 0000

        return new UUID(raw);
    }*/
    /** XXX end modification by stefan@apache.org */
}
