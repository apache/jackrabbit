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

import org.apache.jackrabbit.spi.Name;

/**
 * <code>EffectiveNodeTypeCache</code> defines the interface for a cache for
 * effective node types. Effective node types are addressed by {@link Key}s.
 */
public interface EffectiveNodeTypeCache extends Cloneable {

    /**
     * Puts an effective node type to the cache. The key is internally generated
     * from the set of merged node types.
     * @param ent the effective node type to put to the cache
     */
    void put(EffectiveNodeType ent);

    /**
     * Puts an effective node type to the cache for the given key.
     * @param key the key for the effective node type
     * @param ent the effective node type to put to the cache
     */
    void put(Key key, EffectiveNodeType ent);

    /**
     * Checks if the effective node type for the given key exists.
     * @param key the key to check
     * @return <code>true</code> if the effective node type is cached;
     *         <code>false</code> otherwise.
     */
    boolean contains(Key key);

    /**
     * Returns the effective node type for the given key or <code>null</code> if
     * the desired node type is not cached.
     * @param key the key for the effective node type.
     * @return the effective node type or <code>null</code>
     */
    EffectiveNodeType get(Key key);

    /**
     * Returns a key for an effective node type that consists of the given
     * node type names.
     * @param ntNames the array of node type names for the effective node type
     * @return the key to an effective node type.
     */
    Key getKey(Name[] ntNames);

    /**
     * Removes all effective node types that are aggregated with the node type
     * of the given name.
     * @param name the name of the node type.
     */
    void invalidate(Name name);

    /**
     * Searches the best key k for which the given <code>key</code> is a super
     * set, i.e. for which {@link Key#contains(Key)}} returns
     * <code>true</code>. If an already cached effective node type matches the
     * key it is returned.
     *
     * @param key the key for which the subkey is to be searched
     * @return the best key or <code>null</code> if no key could be found.
     */
    Key findBest(Key key);

    /**
     * Clears the cache.
     */
    void clear();

    /**
    * An <code>ENTKey</code> uniquely identifies
    * a combination (i.e. an aggregation) of one or more node types.
    */
    interface Key extends Comparable<Key> {

        /**
         * Returns the node type names of this key.
         * @return the node type names of this key.
         */
        Name[] getNames();

        /**
         * Checks if the <code>otherKey</code> is contained in this one. I.e. if
         * this key contains all node type names of the other key.
         * @param otherKey the other key to check
         * @return <code>true</code> if this key contains the other key;
         *         <code>false</code> otherwise.
         */
        boolean contains(Key otherKey);

        /**
         * Creates a new key as a result of a subtract operation. i.e. removes all
         * node type names that from the other key.
         * <p>
         * Please note that no exception is thrown if the other key has node type
         * names that are not contained in this key (i.e. {@link #contains(Key)}
         * returns <code>false</code>).
         *
         * @param otherKey the other key to subtract
         * @return the new key of the subtraction operation.
         */
        Key subtract(Key otherKey);
    }
}
