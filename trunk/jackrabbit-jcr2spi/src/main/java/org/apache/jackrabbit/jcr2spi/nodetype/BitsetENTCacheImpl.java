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
package org.apache.jackrabbit.jcr2spi.nodetype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.jackrabbit.spi.Name;

/**
 * Implements an effective node type cache that uses a bit set for storing the
 * information about participating node types in a set.
 */
class BitsetENTCacheImpl implements EffectiveNodeTypeCache {

    /**
     * constant for bits-per-word
     */
    private static final int BPW = 64;

    /**
     * OR mask for bit set
     */
    private static final long[] OR_MASK = new long[BPW];
    static {
        for (int i=0; i<BPW; i++) {
            OR_MASK[i] = 1L << i;
        }
    }

    /**
     * An ordered set of the keys. This is used for {@link #findBest(Key)}.
     */
    private final TreeSet<Key> sortedKeys;

    /**
     * cache of pre-built aggregations of node types
     */
    private final HashMap<Key, EffectiveNodeType> aggregates;

    /**
     * A lookup table for bit numbers for a given name.
     *
     * Note: further performance improvements could be made if this index would
     * be stored in the node type registry since only registered node type names
     * are allowed in the keys.
     */
    private final ConcurrentHashMap<Name, Integer> nameIndex = new ConcurrentHashMap<Name, Integer>();

    /**
     * The reverse lookup table for bit numbers to names
     */
    private Name[] names = new Name[1024];

    /**
     * Creates a new bitset effective node type cache
     */
    BitsetENTCacheImpl() {
        sortedKeys = new TreeSet<Key>();
        aggregates = new HashMap<Key, EffectiveNodeType>();
    }

    //---------------------------------------------< EffectiveNodeTypeCache >---
    /**
     * @see EffectiveNodeTypeCache#getKey(Name[])
     */
    public Key getKey(Name[] ntNames) {
        return new BitsetKey(ntNames, nameIndex.size() + ntNames.length);
    }

    /**
     * @see EffectiveNodeTypeCache#put(EffectiveNodeType)
     */
    public void put(EffectiveNodeType ent) {
        put(getKey(ent.getMergedNodeTypes()), ent);
    }

    /**
     * @see EffectiveNodeTypeCache#put(Key, EffectiveNodeType)
     */
    public void put(Key key, EffectiveNodeType ent) {
        aggregates.put(key, ent);
        sortedKeys.add(key);
    }

    /**
     * @see EffectiveNodeTypeCache#findBest(Key)
     */
    public Key findBest(Key key) {
        // quick check for already cached key
        if (contains(key)) {
            return key;
        }
        for (Key k : sortedKeys) {
            if (key.contains(k)) {
                return k;
            }
        }
        return null;
    }

    /**
     * @see EffectiveNodeTypeCache#invalidate(Name)
     */
    public void invalidate(Name name) {
        /**
         * remove all affected effective node types from aggregates cache
         * (copy keys first to prevent ConcurrentModificationException)
         */
        ArrayList<Key> keys = new ArrayList<Key>(aggregates.keySet());
        for (Key k : keys) {
            EffectiveNodeType ent = get(k);
            if (ent.includesNodeType(name)) {
                remove(k);
            }
        }
    }

    /**
     * @see EffectiveNodeTypeCache#contains(Key)
     */
    public boolean contains(Key key) {
        return aggregates.containsKey(key);
    }

    /**
     * @see EffectiveNodeTypeCache#get(Key)
     */
    public EffectiveNodeType get(Key key) {
        return aggregates.get(key);
    }

    /**
     * @see EffectiveNodeTypeCache#clear()
     */
    public void clear() {
        sortedKeys.clear();
        aggregates.clear();
        nameIndex.clear();
    }

    //------------------------------------------------------------< private >---
    /**
     * Returns the bit number for the given name. If the name does not exist
     * a new new bit number for that name is created.
     *
     * @param name the name to lookup
     * @return the bit number for the given name
     */
    private int getBitNumber(Name name) {
        Integer i = nameIndex.get(name);
        if (i == null) {
            synchronized (nameIndex) {
                i = nameIndex.get(name);
                if (i == null) {
                    int idx = nameIndex.size();
                    i = idx;
                    nameIndex.put(name, i);
                    if (idx >= names.length) {
                        Name[] newNames = new Name[names.length*2];
                        System.arraycopy(names, 0, newNames, 0, names.length);
                        names = newNames;
                    }
                    names[idx] = name;
                }
            }
        }
        return i;
    }

    /**
     * Returns the node type name for a given bit number.
     * @param n the bit number to lookup
     * @return the node type name
     */
    private Name getName(int n) {
        return names[n];
    }

    /**
     * Removes the effective node type for the given key from the cache.
     *
     * @param key the key of the effective node type to remove
     * @return the removed effective node type or <code>null</code> if it was
     *         never cached.
     */
    private EffectiveNodeType remove(Key key) {
        EffectiveNodeType removed = aggregates.remove(key);
        if (removed != null) {
            // other than the original implementation, the weights in the
            // treeset are now the same as in the given keys. so we can use
            // the normal remove method
            sortedKeys.remove(key);
        }
        return removed;
    }

    //----------------------------------------------------------< Cloneable >---
    /**
     * @see Cloneable#clone()
     */
    @Override
    public Object clone() {
        BitsetENTCacheImpl clone = new BitsetENTCacheImpl();
        clone.sortedKeys.addAll(sortedKeys);
        clone.aggregates.putAll(aggregates);
        clone.names = new Name[names.length];
        System.arraycopy(names, 0, clone.names, 0, names.length);
        clone.nameIndex.putAll(nameIndex);
        return clone;
    }

    //-------------------------------------------------------------< Object >---

    /**
     * Returns the the state of this instance in a human readable format.
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EffectiveNodeTypeCache (" + super.toString() + ")\n");
        builder.append("EffectiveNodeTypes in cache:\n");
        for (Key k : sortedKeys) {
            builder.append(k);
            builder.append('\n');
        }
        return builder.toString();
    }

    //----------------------------------------------------------------< Key >---
    /**
     * Implements a {@link Key} by storing the node type aggregate information
     * in a bit set. We do not use the {@link java.util.BitSet} because it
     * does not suite all our needs. Every node type is represented by a bit
     * in the set. This key is immutable.
     */
    private class BitsetKey implements Key {

        /**
         * The names of the node types that form this key.
         */
        private final Name[] names;

        /**
         * The array of longs that hold the bit information.
         */
        private final long[] bits;

        /**
         * the hashcode, only calculated once
         */
        private final int hashCode;

        /**
         * Creates a ew bitset key.
         * @param names the node type names
         * @param maxBit the approximate number of the greatest bit
         */
        public BitsetKey(Name[] names, int maxBit) {
            this.names = names;
            bits = new long[maxBit/BPW+1];

            for (int i=0; i<names.length; i++) {
                int n = getBitNumber(names[i]);
                bits[n/BPW] |= OR_MASK[n%BPW];
            }
            hashCode = calcHashCode();
        }

        /**
         * Creates new bitset key.
         * @param bits the array if bits
         * @param numBits the number of bits that are '1' in the given bits
         */
        private BitsetKey(long[] bits, int numBits) {
            this.bits = bits;
            names = new Name[numBits];
            int i = nextSetBit(0);
            int j=0;
            while (i >= 0) {
                names[j++] = BitsetENTCacheImpl.this.getName(i);
                i = nextSetBit(i+1);
            }
            hashCode = calcHashCode();
        }

        /**
         * Returns the bit number of the next bit that is set, starting at
         * <code>fromIndex</code> inclusive.
         *
         * @param fromIndex the bit position to start the search
         * @return the bit position of the bit or -1 if none found.
         */
        private int nextSetBit(int fromIndex) {
            int addr = fromIndex/BPW;
            int off = fromIndex%BPW;
            while (addr < bits.length) {
                if (bits[addr] != 0) {
                    while (off < BPW) {
                        if ((bits[addr] & OR_MASK[off]) != 0) {
                            return addr * BPW + off;
                        }
                        off++;
                    }
                    off=0;
                }
                addr++;
            }
            return -1;
        }

         /**
          * Returns the number of bits set in val.
          * For a derivation of this algorithm, see
          * "Algorithms and data structures with applications to
          *  graphics and geometry", by Jurg Nievergelt and Klaus Hinrichs,
          *  Prentice Hall, 1993.
          *
          * @param val the value to calculate the bit count for
          * @return the number of '1' bits in the value
          */
         private int bitCount(long val) {
             val -= (val & 0xaaaaaaaaaaaaaaaaL) >>> 1;
             val =  (val & 0x3333333333333333L) + ((val >>> 2) & 0x3333333333333333L);
             val =  (val + (val >>> 4)) & 0x0f0f0f0f0f0f0f0fL;
             val += val >>> 8;
             val += val >>> 16;
             return ((int)(val) + (int)(val >>> 32)) & 0xff;
         }

        /**
         * Calculates the hashcode.
         * @return the calculated hashcode
         */
        private int calcHashCode() {
            long h = 1234;
            int addr = bits.length -1;
            while (addr >=0 && bits[addr] == 0) {
                addr--;
            }
            while (addr >=0) {
                h ^= bits[addr] * (addr + 1);
                addr--;
            }
            return (int)((h >> 32) ^ h);
        }

        //------------------------------------------------------------< Key >---
        /**
         * @see Key#getNames()
         */
        public Name[] getNames() {
            return names;
        }

        /**
         * @see Key#contains(Key)
         */
        public boolean contains(Key otherKey) {
            /*
             * 0 - 0 => 0
             * 0 - 1 => 1
             * 1 - 0 => 0
             * 1 - 1 => 0
             * !a and b
             */
            BitsetKey other = (BitsetKey) otherKey;
            int len = Math.max(bits.length, other.bits.length);
            for (int i=0; i<len; i++) {
                long w1 = i < bits.length ? bits[i] : 0;
                long w2 = i < other.bits.length ? other.bits[i] : 0;
                long r = ~w1 & w2;
                if (r != 0) {
                    return false;
                }
            }
            return true;
        }

        /**
         * @see Key#subtract(Key)
         */
        public Key subtract(Key otherKey) {
            /*
             * 0 - 0 => 0
             * 0 - 1 => 0
             * 1 - 0 => 1
             * 1 - 1 => 0
             * a and !b
             */
            BitsetKey other = (BitsetKey) otherKey;
            int len = Math.max(bits.length, other.bits.length);
            long[] newBits = new long[len];
            int numBits = 0;
            for (int i=0; i<len; i++) {
                long w1 = i < bits.length ? bits[i] : 0;
                long w2 = i < other.bits.length ? other.bits[i] : 0;
                newBits[i] = w1 & ~w2;
                numBits += bitCount(newBits[i]);
            }
            return new BitsetKey(newBits, numBits);
        }

        //-----------------------------------------------------< Comparable >---
        /**
         * {@inheritDoc}
         *
         * This compares 1. the cardinality (number of set bits) and 2. the
         * numeric value of the bitsets in descending order.
         *
         * @see Comparable#compareTo(Object)
         */
        public int compareTo(Key other) {
            BitsetKey o = (BitsetKey) other;
            int res = o.names.length - names.length;
            if (res == 0) {
                int adr = Math.max(bits.length, o.bits.length) - 1;
                while (adr >= 0) {
                    long w1 = adr<bits.length ? bits[adr] : 0;
                    long w2 = adr<o.bits.length ? o.bits[adr] : 0;
                    if (w1 != w2) {
                        // some signed arithmetic here
                        long h1 = w1 >>> 32;
                        long h2 = w2 >>> 32;
                        if (h1 == h2) {
                            h1 = w1 & 0x0ffffffffL;
                            h2 = w2 & 0x0ffffffffL;
                        }
                        return Long.signum(h2 - h1);
                    }
                    adr--;
                }
            }
            return res;
        }

        //---------------------------------------------------------< Object >---
        /**
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof BitsetKey) {
                BitsetKey o = (BitsetKey) obj;
                if (names.length != o.names.length) {
                    return false;
                }
                int adr = Math.max(bits.length, o.bits.length) - 1;
                while (adr >= 0) {
                    long w1 = adr<bits.length ? bits[adr] : 0;
                    long w2 = adr<o.bits.length ? o.bits[adr] : 0;
                    if (w1 != w2) {
                        return false;
                    }
                    adr--;
                }
                return true;
            }
            return false;
        }

        /**
         * @see Object#hashCode()
         */
        @Override
        public int hashCode() {
            return hashCode;
        }

        /**
         * @see Object#toString()
         */
        @Override
        public String toString() {
            StringBuffer buf = new StringBuffer("w=");
            buf.append(names.length);
            int i = nextSetBit(0);
            while (i>=0) {
                buf.append(", ").append(i).append("=");
                buf.append(BitsetENTCacheImpl.this.getName(i));
                i = nextSetBit(i+1);
            }
            return buf.toString();
        }
    }
}