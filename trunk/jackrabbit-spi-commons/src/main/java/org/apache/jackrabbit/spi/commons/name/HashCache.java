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
package org.apache.jackrabbit.spi.commons.name;

/**
 * Simple utility class that implements a fixed-size and thread-safe
 * (non-blocking) cache of objects. The cache is simply an array
 * of objects, indexed by their hash codes. If more than one objects
 * hash to the same location, only the most recently accessed object is
 * kept in the cache.
 *
 * @see <a href="https://issues.apache.org/jira/browse/JCR-1663">JCR-1663</a>
 */
public class HashCache<T> {

    /**
     * Array of cached objects, indexed by their hash codes
     * (module size of the array).
     */
    private final T[] array;

    /**
     * Creates a hash cache with 1024 slots.
     */
    public HashCache() {
        this(10);
    }

    /**
     * Creates a hash cache with 2^<code>exponent</code> slots.
     *
     * @param exponent the exponent.
     */
    @SuppressWarnings("unchecked")
    public HashCache(int exponent) {
        this.array = (T[]) new Object[2 << exponent];
    }

    /**
     * If a cached copy of the given object already exists, then returns
     * that copy. Otherwise the given object is cached and returned.
     *
     * @param object object to return from the cache
     * @return the given object or a previously cached copy
     */
    public T get(T object) {
        int position = object.hashCode() & (array.length - 1);
        T previous = array[position];
        if (object.equals(previous)) {
            return previous;
        } else {
            array[position] = object;
            return object;
        }
    }

}
