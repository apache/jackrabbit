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
package org.apache.jackrabbit.core.value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Represents binary data which is backed by a byte[] (in memory).
 */
class BLOBInMemory extends BLOBFileValue {

    /**
     * Logger instance for this class
     */
    private static Logger log = LoggerFactory.getLogger(BLOBInMemory.class);

    /**
     * the prefix of the string representation of this value
     */
    private static final String PREFIX = "0x";

    /**
     * the data
     */
    private byte[] data;

    /**
     * empty array
     */
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /**
     * empty instance
     */
    private static final BLOBInMemory EMPTY = new BLOBInMemory(EMPTY_BYTE_ARRAY);

    /**
     * Creates a new instance from a
     * <code>byte[]</code> array.
     *
     * @param data the byte array
     */
    private BLOBInMemory(byte[] data) {
        this.data = data;
    }

    /**
     * Creates a new instance from a
     * <code>byte[]</code> array.
     *
     * @param data the byte array
     */
    static BLOBInMemory getInstance(byte[] data) {
        if (data.length == 0) {
            return EMPTY;
        } else {
            return new BLOBInMemory(data);
        }
    }

    /**
     * Checks if String can be converted to an instance of this class.
     * @param s
     * @return true if it can be converted
     */
    static boolean isInstance(String s) {
        return s.startsWith(PREFIX);
    }

    /**
     * Convert a String to an instance of this class.
     * @param s
     * @return the instance
     */
    static BLOBInMemory getInstance(String s) throws IllegalArgumentException {
        assert s.startsWith(PREFIX);
        s = s.substring(PREFIX.length());
        int len = s.length();
        if (len % 2 != 0) {
            String msg = "unable to deserialize byte array " + s + " , length=" + s.length();
            log.debug(msg);
            throw new IllegalArgumentException(msg);
        }
        len /= 2;
        byte[] data = new byte[len];
        try {
            for (int i = 0; i < len; i++) {
                data[i] = (byte) ((Character.digit(s.charAt(2 * i), 16) << 4) | (Character.digit(s.charAt(2 * i + 1), 16)));
            }
        } catch (NumberFormatException e) {
            String msg = "unable to deserialize byte array " + s;
            log.debug(msg);
            throw new IllegalArgumentException(msg);
        }
        return BLOBInMemory.getInstance(data);
    }

    void delete(boolean pruneEmptyParentDirs) {
        // do nothing
        // this object could still be referenced
        // the data will be garbage collected
    }

    public void dispose() {
        // do nothing
        // this object could still be referenced
        // the data will be garbage collected
    }

    BLOBFileValue copy() throws RepositoryException {
        return this;
    }

    public long getSize() {
        return data.length;
    }

    public InputStream getStream() {
        return new ByteArrayInputStream(data);
    }

    public String toString() {
        StringBuilder buff = new StringBuilder(PREFIX.length() + 2 * data.length);
        buff.append(PREFIX);
        for (int i = 0; i < data.length; i++) {
            int c = data[i] & 0xff;
            buff.append(Integer.toHexString(c >> 4));
            buff.append(Integer.toHexString(c & 0xf));
        }
        return buff.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BLOBInMemory) {
            BLOBInMemory other = (BLOBInMemory) obj;
            return Arrays.equals(data, other.data);
        }
        return false;
    }

    /**
     * Returns zero to satisfy the Object equals/hashCode contract.
     * This class is mutable and not meant to be used as a hash key.
     *
     * @return always zero
     * @see Object#hashCode()
     */
    public int hashCode() {
        return 0;
    }

}
