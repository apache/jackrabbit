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
package org.apache.jackrabbit.core.cache;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.collections4.map.AbstractLinkedMap;

/**
 * <code>GrowingLRUMap</code> extends the LRUMap such that it can grow from
 * the specified <code>initialSize</code> to the specified <code>maxSize</code>;
 */
public class GrowingLRUMap<K, V> extends LRUMap<K, V> {

    private final int maxSize;

    public GrowingLRUMap(int initialSize, int maxSize) {
        super(initialSize);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeLRU(AbstractLinkedMap.LinkEntry<K, V> entry) {
        return size() > maxSize;
    }
}