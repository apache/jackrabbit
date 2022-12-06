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
package org.apache.jackrabbit.core.util;

import org.apache.commons.collections4.map.LinkedMap;

import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Collections;

/**
 * <code>EmptyLinkedMap</code> implements an empty unmodifiable {@link LinkedMap}.
 */
public class EmptyLinkedMap<K, V> extends LinkedMap<K, V> {

    private static final long serialVersionUID = -9165910643562370800L;

    /**
     * The only instance of this class.
     */
    @SuppressWarnings("rawtypes")
    public static final LinkedMap INSTANCE = new EmptyLinkedMap();

    private EmptyLinkedMap() {
        super();
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    public V remove(int i) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    public V put(Object o, Object o1) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    @SuppressWarnings("rawtypes")
    public void putAll(Map map) {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    public V remove(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an unmodifiable empty set.
     *
     * @return an unmodifiable empty set.
     */
    public Set<Map.Entry<K, V>> entrySet() {
        return Collections.emptySet();
    }

    /**
     * Returns an unmodifiable empty set.
     *
     * @return an unmodifiable empty set.
     */
    public Set<K> keySet() {
        return Collections.emptySet();
    }

    /**
     * Returns an unmodifiable empty collection.
     *
     * @return an unmodifiable empty collection.
     */
    public Collection<V> values() {
        return Collections.emptyList();
    }

    //----------------------------------------------------< Cloneable support >

    /**
     * Returns the single instance of this class.
     *
     * @return {@link #INSTANCE}.
     */
    @SuppressWarnings("unchecked")
    public LinkedMap<K, V> clone() {
        return INSTANCE;
    }

    //-------------------------------------------------< Serializable support >

    /**
     * Returns the single instance of this class.
     *
     * @return {@link #INSTANCE}.
     */
    private Object readResolve() {
        return INSTANCE;
    }
}
