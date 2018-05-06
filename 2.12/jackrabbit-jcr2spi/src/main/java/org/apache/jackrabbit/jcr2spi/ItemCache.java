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
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.jcr2spi.state.ItemState;

import javax.jcr.Item;

/**
 * <code>ItemCache</code>...
 */
public interface ItemCache extends ItemLifeCycleListener {

    /**
     * Returns the cached <code>Item</code> that belongs to the given
     * <code>ItemState</code> or <code>null</code> if the cache does not
     * contain that <code>Item</code>.
     *
     * @param state State of the item that should be retrieved.
     * @return The item reference stored in the corresponding cache entry
     * or <code>null</code> if there's no corresponding cache entry.
     */
    Item getItem(ItemState state);

    /**
     * Clear all entries in the ItemCache and free resources.
     */
    void clear();
}