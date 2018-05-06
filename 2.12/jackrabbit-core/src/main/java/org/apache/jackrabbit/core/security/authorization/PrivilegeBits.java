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
package org.apache.jackrabbit.core.security.authorization;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>PrivilegeBits</code> 
 */
public class PrivilegeBits {

    public static final PrivilegeBits EMPTY = new PrivilegeBits(UnmodifiableData.EMPTY);

    private static final long READ = 1; // PrivilegeRegistry.READ

    private static final Map<Long, PrivilegeBits> BUILT_IN = new HashMap<Long, PrivilegeBits>();
    static {
        BUILT_IN.put(EMPTY.longValue(), EMPTY);
    }

    private final Data d;

    /**
     * Private constructor.
     *
     * @param d
     */
    private PrivilegeBits(Data d) {
        this.d = d;
    }

    /**
     * Package private method used by <code>PrivilegeRegistry</code> to handle
     * built-in privileges and calculate internal permissions.
     * 
     * @return long representation of this instance.
     * @see PrivilegeRegistry#calculatePermissions(PrivilegeBits, PrivilegeBits, boolean, boolean)
     */
    long longValue() {
        return d.longValue();
    }

    /**
     * Package private method used by <code>PrivilegeRegistry</code> to calculate
     * the privilege bits associated with a given built-in or custom privilege
     * definition.
     *
     * @return an instance of <code>PrivilegeBits</code>
     */
    PrivilegeBits nextBits() {
        if (this == EMPTY) {
            return EMPTY;
        } else {
            PrivilegeBits pb = new PrivilegeBits(d.next());
            if (pb.d.isSimple()) {
                BUILT_IN.put(pb.longValue(), pb);
            }
            return pb;
        }
    }

    /**
     * Package private method used by <code>PrivilegeRegistry</code> to get or
     * create an instance of privilege bits for the specified long value.
     *
     * @param bits
     * @return an instance of <code>PrivilegeBits</code>
     */
    static PrivilegeBits getInstance(long bits) {
        if (bits == PrivilegeRegistry.NO_PRIVILEGE) {
            return EMPTY;
        } else if (bits < PrivilegeRegistry.NO_PRIVILEGE) {
            throw new IllegalArgumentException();
        } else {
            PrivilegeBits pb = BUILT_IN.get(bits);
            if (pb == null) {
                pb = new PrivilegeBits(new UnmodifiableData(bits));
                BUILT_IN.put(bits, pb);
            }
            return pb;
        }
    }

    /**
     * Internal method to create a new instance of <code>PrivilegeBits</code>.
     * 
     * @param bits
     * @return an instance of <code>PrivilegeBits</code>
     */
    private static PrivilegeBits getInstance(long[] bits) {
        long[] bts = new long[bits.length];
        System.arraycopy(bits, 0, bts, 0, bits.length);
        return new PrivilegeBits(new UnmodifiableData(bts));
    }

    /**
     * Creates a mutable instance of privilege bits.
     *
     * @return a new instance of privilege bits.
     */
    public static PrivilegeBits getInstance() {
        return new PrivilegeBits(new ModifiableData());
    }

    /**
     * Creates a mutable instance of privilege bits.
     *
     * @param base
     * @return a new instance of privilege bits.
     */
    public static PrivilegeBits getInstance(PrivilegeBits base) {
        return new PrivilegeBits(new ModifiableData(base.d));
    }

    /**
     * Returns <code>true</code> if this privilege bits includes no privileges
     * at all.
     *
     * @return <code>true</code> if this privilege bits includes no privileges
     * at all; <code>false</code> otherwise.
     * @see PrivilegeRegistry#NO_PRIVILEGE
     */
    public boolean isEmpty() {
        return d.isEmpty();
    }

    /**
     * Returns an unmodifiable instance.
     *
     * @return an unmodifiable <code>PrivilegeBits</code> instance.
     */
    public PrivilegeBits unmodifiable() {
        if (d instanceof ModifiableData) {
            return (d.isSimple()) ? getInstance(d.longValue()) : getInstance(d.longValues());
        } else {
            return this;
        }
    }

    /**
     * Returns <code>true</code> if this privilege bits instance can be altered.
     *
     * @return true if this privilege bits instance can be altered.
     */
    public boolean isModifiable() {
        return (d instanceof ModifiableData);
    }

    /**
     * Returns <code>true</code> if this instance includes the jcr:read
     * privilege. Shortcut for calling {@link PrivilegeBits#includes(PrivilegeBits)}
     * where the other bits represented the jcr:read privilege.
     *
     * @return <code>true</code> if this instance includes the jcr:read
     * privilege; <code>false</code> otherwise.
     */
    public boolean includesRead() {
        if (this == EMPTY) {
            return false;
        } else {
            return d.includesRead();
        }
    }

    /**
     * Returns <code>true</code> if all privileges defined by the specified
     * <code>otherBits</code> are present in this instance.
     *
     * @param otherBits
     * @return <code>true</code> if all privileges defined by the specified
     * <code>otherBits</code> are included in this instance; <code>false</code>
     * otherwise.
     */
    public boolean includes(PrivilegeBits otherBits) {
        return d.includes(otherBits.d);
    }

    /**
     * Adds the other privilege bits to this instance.
     *
     * @param other The other privilege bits to be added.
     * @throws UnsupportedOperationException if this instance is immutable.
     */
    public void add(PrivilegeBits other) {
        if (d instanceof ModifiableData) {
            ((ModifiableData) d).add(other.d);
        } else {
            throw new UnsupportedOperationException("immutable privilege bits");
        }
    }

    /**
     * Subtracts the other PrivilegeBits from the this.<br>
     * If the specified bits do not intersect with this, it isn't modified.<br>
     * If <code>this</code> is included in <code>other</code> {@link #EMPTY empty}
     * privilege bits is returned.
     *
     * @param other The other privilege bits to be substracted from this instance.
     * @throws UnsupportedOperationException if this instance is immutable.
     */
    public void diff(PrivilegeBits other) {
        if (d instanceof ModifiableData) {
            ((ModifiableData) d).diff(other.d);
        } else {
            throw new UnsupportedOperationException("immutable privilege bits");
        }
    }

    /**
     * Subtracts the <code>b</code> from <code>a</code> and adds the result (diff)
     * to this instance.
     *
     * @param a An instance of privilege bits.
     * @param b An instance of privilege bits.
     * @throws UnsupportedOperationException if this instance is immutable.
     */
    public void addDifference(PrivilegeBits a, PrivilegeBits b) {
        if (d instanceof ModifiableData) {
            ((ModifiableData) d).addDifference(a.d, b.d);
        } else {
            throw new UnsupportedOperationException("immutable privilege bits");
        }
    }

    //-------------------------------------------------------------< Object >---
    @Override
    public int hashCode() {
        return d.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof PrivilegeBits) {
            return d.equals(((PrivilegeBits) o).d);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PrivilegeBits: ");
        if (d.isSimple()) {
            sb.append(d.longValue());
        } else {
            sb.append(Arrays.toString(d.longValues()));
        }
        return sb.toString();
    }

    //------------------------------------------------------< inner classes >---
    /**
     * Base class for the internal privilege bits representation and handling.
     */
    private static abstract class Data {

        abstract boolean isEmpty();
        
        abstract long longValue();

        abstract long[] longValues();

        abstract boolean isSimple();

        abstract Data next();

        abstract boolean includes(Data other);

        abstract boolean includesRead();

        boolean equalData(Data d) {
            if (isSimple() != d.isSimple()) {
                return false;
            }
            if (isSimple()) {
                return longValue() == d.longValue();
            } else {
                return Arrays.equals(longValues(), d.longValues());
            }
        }

        static boolean includes(long bits, long otherBits) {
            return (bits | ~otherBits) == -1;
        }

        static boolean includes(long[] bits, long[] otherBits) {
            if (otherBits.length <= bits.length) {
                // test for each long if is included
                for (int i = 0; i < otherBits.length; i++) {
                    if ((bits[i] | ~otherBits[i]) != -1) {
                        return false;
                    }
                }
                return true;
            } else {
                // otherbits array is longer > cannot be included in bits
                return false;
            }
        }
    }

    /**
     * Immutable Data object
     */
    private static class UnmodifiableData extends Data {

        private static final long MAX = Long.MAX_VALUE / 2;

        private static final UnmodifiableData EMPTY = new UnmodifiableData(PrivilegeRegistry.NO_PRIVILEGE);

        private final long bits;
        private final long[] bitsArr;
        private final boolean isSimple;
        private final boolean includesRead;

        private UnmodifiableData(long bits) {
            this.bits = bits;
            bitsArr = new long[] {bits};
            isSimple = true;
            includesRead  = (bits & READ) == READ;
        }

        private UnmodifiableData(long[] bitsArr) {
            bits = PrivilegeRegistry.NO_PRIVILEGE;            
            this.bitsArr = bitsArr;
            isSimple = false;
            includesRead = (bitsArr[0] & READ) == READ;
        }

        @Override
        boolean isEmpty() {
            return this == EMPTY;
        }

        @Override
        long longValue() {
            return bits;
        }

        @Override
        long[] longValues() {
            return bitsArr;
        }

        @Override
        boolean isSimple() {
            return isSimple;
        }

        @Override
        Data next() {
            if (this == EMPTY) {
                return EMPTY;
            } else if (isSimple) {
                if (bits < MAX) {
                    long b = bits << 1;
                    return new UnmodifiableData(b);
                } else {
                    return new UnmodifiableData(new long[] {bits}).next();
                }
            } else {
                long[] bts;
                long last = bitsArr[bitsArr.length-1];
                if (last < MAX) {
                    bts = new long[bitsArr.length];
                    System.arraycopy(bitsArr, 0, bts, 0, bitsArr.length);
                    bts[bts.length-1] = last << 1;
                } else {
                    bts = new long[bitsArr.length + 1];
                    bts[bts.length-1] = 1;
                }
                return new UnmodifiableData(bts);
            }
        }

        @Override
        boolean includes(Data other) {
            if (isSimple) {
                return (other.isSimple()) ? includes(bits, other.longValue()) : false;
            } else {
                return includes(bitsArr, other.longValues());
            }
        }

        @Override
        boolean includesRead() {
            return includesRead;
        }

        //---------------------------------------------------------< Object >---
        @Override
        public int hashCode() {
            return (isSimple) ? new Long(bits).hashCode() : Arrays.hashCode(bitsArr);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof UnmodifiableData) {
                UnmodifiableData d = (UnmodifiableData) o;
                if (isSimple != d.isSimple) {
                    return false;
                }
                if (isSimple) {
                    return bits == d.bits;
                } else {
                    return Arrays.equals(bitsArr, d.bitsArr);
                }
            } else if (o instanceof ModifiableData) {
                return equalData((Data) o);
            } else {
                return false;
            }
        }
    }

    /**
     * Mutable implementation of the Data base class.
     */
    private static class ModifiableData extends Data {

        private long[] bits;

        private ModifiableData() {
            bits = new long[] {PrivilegeRegistry.NO_PRIVILEGE};
        }

        private ModifiableData(Data base) {
            long[] b = base.longValues();
            switch (b.length) {
                case 0:
                    // empty
                    bits = new long[] {PrivilegeRegistry.NO_PRIVILEGE};
                    break;
                case 1:
                    // single long
                    bits = new long[] {b[0]};
                    break;
                default:
                    // copy
                    bits = new long[b.length];                    
                    System.arraycopy(b, 0, bits, 0, b.length);
            }
        }

        @Override
        boolean isEmpty() {
            return bits.length == 1 && bits[0] == PrivilegeRegistry.NO_PRIVILEGE;
        }

        @Override
        long longValue() {
            return (bits.length == 1) ? bits[0] : PrivilegeRegistry.NO_PRIVILEGE;
        }

        @Override
        long[] longValues() {
            return bits;
        }

        @Override
        boolean isSimple() {
            return bits.length == 1;
        }

        @Override
        Data next() {
            throw new UnsupportedOperationException("Not implemented.");
        }

        @Override
        boolean includes(Data other) {
            if (bits.length == 1) {
                return other.isSimple() && includes(bits[0], other.longValue());
            } else {
                return includes(bits, other.longValues());
            }
        }

        @Override
        boolean includesRead() {
            return (bits[0] & READ) == READ;
        }

        /**
         * Add the other Data to this instance.
         *
         * @param other
         */
        private void add(Data other) {
            if (other != this) {
                if (bits.length == 1 && other.isSimple()) {
                    bits[0] |= other.longValue();
                } else {
                    or(other.longValues());
                }
            }
        }

        /**
         * Subtract the other Data from this instance.
         * 
         * @param other
         */
        private void diff(Data other) {
            if (bits.length == 1 && other.isSimple()) {
                bits[0] = bits[0] & ~other.longValue();
            } else {
                bits = diff(bits, other.longValues());
            }
        }

        /**
         * Add the diff between the specified Data a and b.
         * 
         * @param a
         * @param b
         */
        private void addDifference(Data a, Data b) {
            if (a.isSimple() && b.isSimple()) {
                bits[0] |= a.longValue() & ~b.longValue();
            } else {
                long[] diff = diff(a.longValues(), b.longValues());
                or(diff);
            }
        }

        private void or(long[] b) {
            if (b.length > bits.length) {
                // enlarge the array
                long[] res = new long[b.length];
                System.arraycopy(bits, 0, res, 0, bits.length);
                bits = res;
            }
            for (int i = 0; i < b.length; i++) {
                bits[i] |= b[i];
            }
        }

        private static long[] diff(long[] a, long[] b) {
            int index = -1;
            long[] res = new long[((a.length > b.length) ? a.length : b.length)];
            for (int i = 0; i < res.length; i++) {
                if (i < a.length && i < b.length) {
                    res[i] = a[i] & ~b[i];
                } else {
                    res[i] = (i < a.length) ? a[i] : 0;
                }
                // remember start of trailing 0 array entries
                if (res[i] != 0) {
                    index = -1;
                } else if (index == -1) {
                    index = i;
                }
            }
            switch (index) {
                case -1:
                    // no need to remove trailing 0-long from the array                    
                    return res;
                case 0 :
                    // array consisting of one or multiple 0
                    return new long[] {PrivilegeRegistry.NO_PRIVILEGE};
                default:
                    // remove trailing 0-long entries from the array
                    long[] r2 = new long[index];
                    System.arraycopy(res, 0, r2, 0, index);
                    return r2;
            }
        }

        //---------------------------------------------------------< Object >---
        @Override
        public int hashCode() {
            // NOTE: mutable object. hashCode not implemented.
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof ModifiableData) {
                ModifiableData d = (ModifiableData) o;
                return Arrays.equals(bits, d.bits);
            } else if (o instanceof UnmodifiableData) {
                return equalData((Data) o);
            } else {
                return false;
            }
        }
    }
}