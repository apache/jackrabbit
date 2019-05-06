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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.spi.Name;

import java.util.Set;
import java.util.Iterator;
import java.util.Collection;
import java.util.HashSet;

/**
 * <code>NameSet</code> implements a collection of unique {@link Name}s. The
 * methods exposed via the {@link Set} interface are for read only access, which
 * means this implementation will throw a {@link UnsupportedOperationException}
 * for all modifying methods specified by the {@link Set} interface.
 */
final class NameSet implements Set<Name>, Cloneable {

    /**
     * The name set cache instance.
     */
    private static final NameSetCache CACHE = new NameSetCache();

    /**
     * The maximum number of names in a set that are cached.
     */
    private static final int NUM_NAMES_THRESHOLD = 5;

    /**
     * The set of property {@link Name}s.
     */
    private HashSet names = CACHE.getEmptySet();

    /**
     * Flag indicating whether the {@link #names} set is shared with another
     * {@link NameSet} instance. The initial value is <code>true</code> because
     * {@link #names} is initialized with an empty set from the cache.
     */
    private boolean shared = true;

    /**
     * Adds a <code>name</code>.
     *
     * @param name the name to add.
     * @return <code>true</code> if the name is already present,
     *         <code>false</code> otherwise.
     */
    public boolean add(Name name) {
        if (names.size() > NUM_NAMES_THRESHOLD) {
            ensureModifiable();
            return names.add(name);
        } else {
            int size = names.size();
            // get a cached set
            names = CACHE.get(names, name, !shared);
            // a set from the cache is always shared
            shared = true;
            return names.size() != size;
        }
    }

    /**
     * Removes a <code>name</code>.
     *
     * @param name the name to remove.
     * @return <code>true</code> if the name was removed, <code>false</code>
     *         if the name was unknown.
     */
    boolean remove(Name name) {
        ensureModifiable();
        return names.remove(name);
    }

    /**
     * Removes all names from this {@link NameSet}.
     */
    void removeAll() {
        ensureModifiable();
        names.clear();
    }

    /**
     * Removes all names currently present and adds all names from
     * <code>c</code>.
     *
     * @param c the {@link Name}s to add.
     */
    void replaceAll(Collection c) {
        if (c instanceof NameSet) {
            NameSet propNames = (NameSet) c;
            names = propNames.names;
            shared = true;
            propNames.shared = true;
        } else if (c instanceof HashSet) {
            names = CACHE.get((HashSet) c);
            shared = true;
        } else {
            ensureModifiable();
            names.clear();
            names.addAll(c);
        }
    }

    //------------------------------------------------< unmodifiable Set view >

    /**
     * {@inheritDoc}
     */
    public int size() {
        return names.size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return names.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Object o) {
        return names.contains(o);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The returned iterator will throw a {@link UnsupportedOperationException}
     * on {@link Iterator#remove()}.
     */
    public Iterator iterator() {
        return new Iterator() {
            Iterator i = names.iterator();

            public boolean hasNext() {
                return i.hasNext();
            }

            public Object next() {
                return i.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    public Object[] toArray() {
        return names.toArray();
    }

    /**
     * {@inheritDoc}
     */
    public Object[] toArray(Object[] a) {
        return names.toArray(a);
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsAll(Collection c) {
        return names.containsAll(c);
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    //--------------------------------------------------< equals and hashCode >

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return names.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (obj instanceof NameSet) {
            NameSet other = (NameSet) obj;
            return this.names.equals(other.names);
        }
        return false;
    }

    //----------------------------------------------------< Cloneable support >

    /**
     * Returns a clone of this <code>PropertyNames</code> instance.
     *
     * @return a clone of this <code>PropertyNames</code> instance.
     */
    public Object clone() {
        try {
            NameSet propNames = (NameSet) super.clone();
            shared = true;
            propNames.shared = true;
            return propNames;
        } catch (CloneNotSupportedException e) {
            // will never happen
            throw new InternalError();
        }
    }

    //-------------------------------------------------------------< internal >

    /**
     * Ensures that {@link #names} can be modified (-> not shared).
     */
    private void ensureModifiable() {
        if (shared) {
            names = (HashSet) names.clone();
            shared = false;
        }
    }

    /**
     * Implements a simple <code>HashSet&lt;Name></code> cache.
     * <p>
     * Please note that this cache does not ensures that the sets are immutable!
     * It is the responsibility of the caller to make sure that sets passed to
     * {@link #get} are not modified by multiple threads. Modifying a cached
     * set is not a problem in general because it will only cause cache misses.
     */
    private static final class NameSetCache {

        /**
         * Size of the cache (must be a power of two). Note that this is the
         * maximum number of objects kept in the cache, but due to hashing it
         * can well be that only a part of the cache array is filled even if
         * many more distinct objects are being accessed.
         */
        private static final int SIZE_POWER_OF_2 = 1024;

        /**
         * Array of cached hash sets, indexed by their hash codes
         * (module size of the array).
         */
        private final HashSet[] array = new HashSet[SIZE_POWER_OF_2];

        /**
         * Returns a set that contains all elements from <code>set</code> and
         * <code>obj</code>. If a cached copy of the set already exists, then
         * this method returns that copy. Otherwise <code>obj</code> is added
         * to the given <code>set</code>, the <code>set</code> is cached and
         * then returned.
         *
         * @param set the initial set.
         * @param obj the object to add to <code>set</code>.
         * @param modifiable <code>true</code> if <code>set</code> may be modified.
         * @return a cached set that contains all elements from <code>set</code>
         *         and <code>obj</code>.
         */
        public HashSet get(HashSet set, Object obj, boolean modifiable) {
            if (set.contains(obj)) {
                return set;
            }
            int position = (set.hashCode() + obj.hashCode()) & (SIZE_POWER_OF_2 - 1);
            HashSet previous = array[position];
            if (previous != null &&
                    previous.size() == set.size() + 1 &&
                    previous.containsAll(set) &&
                    previous.contains(obj)) {
                return previous;
            } else {
                if (modifiable) {
                    set.add(obj);
                } else {
                    set = (HashSet) set.clone();
                    set.add(obj);
                }
                array[position] = set;
                return set;
            }
        }

        /**
         * If a cached copy of the given set already exists, then this method
         * returns that copy. Otherwise the given set is cached and returned.
         *
         * @param set set to return from the cache
         * @return the given set or a previously cached copy
         */
        public HashSet get(HashSet set) {
            int position = set.hashCode() & (SIZE_POWER_OF_2 - 1);
            HashSet previous = array[position];
            if (set.equals(previous)) {
                return previous;
            } else {
                array[position] = set;
                return set;
            }
        }

        /**
         * Returns a cached copy of an empty set.
         *
         * @return a cached copy of an empty set.
         */
        public HashSet getEmptySet() {
            HashSet set = array[0];
            if (set == null || !set.isEmpty()) {
                set = new HashSet();
                array[0] = set;
            }
            return set;
        }
    }
}
