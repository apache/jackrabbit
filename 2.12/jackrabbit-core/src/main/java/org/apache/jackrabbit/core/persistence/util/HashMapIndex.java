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

import java.util.HashMap;

import org.apache.jackrabbit.core.util.StringIndex;

/**
 * Implements a {@link StringIndex} that is based on a hashmap. Subclasses
 * can override the protected {@link #load()} and {@link #save()} methods
 * to implement persistent storage of the string index.
 * <p>
 * This class is thread-safe.
 */
public class HashMapIndex implements StringIndex {

    /**
     * holds the string-to-index lookups.
     */
    protected final HashMap<String, Integer> stringToIndex =
        new HashMap<String, Integer>();

    /**
     * holds the index-to-string lookups.
     */
    protected final HashMap<Integer, String> indexToString =
        new HashMap<Integer, String>();

    /**
     * Loads the lookup table.
     */
    protected void load() {
    }

    /**
     * Saves the lookup table.
     */
    protected void save() {
    }

    /**
     * {@inheritDoc}
     *
     * This implementation reloads the table from the resource if a lookup fails
     * and if the resource was modified since.
     */
    public synchronized int stringToIndex(String nsUri) {
        Integer idx = stringToIndex.get(nsUri);
        if (idx == null) {
            load();
            idx = stringToIndex.get(nsUri);
        }
        if (idx == null) {
            // Need to use only 24 bits, since that's what
            // the BundleBinding class stores in bundles
            idx = nsUri.hashCode() & 0x00ffffff;
            while (indexToString.containsKey(idx)) {
                idx = (idx + 1) & 0x00ffffff;
            }
            stringToIndex.put(nsUri, idx);
            indexToString.put(idx, nsUri);
            save();
        }
        return idx;
    }

    /**
     * {@inheritDoc}
     *
     * This implementation reloads the table from the resource if a lookup fails
     * and if the resource was modified since.
     */
    public synchronized String indexToString(int i) {
        Integer idx = Integer.valueOf(i);
        String s = indexToString.get(idx);
        if (s == null) {
            load();
            s = indexToString.get(idx);
        }
        return s;
    }

}
