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

import org.apache.jackrabbit.core.id.NodeId;

import junit.framework.TestCase;

/**
 * Test cases for the {@link ConcurrentCache} class.
 */
public class ConcurrentCacheTest extends TestCase {

    /**
     * Tests a concurrent cache by adding lots of random items to it
     * and checking that the excess items have automatically been evicted
     * while frequently accessed items are still present.
     */
    public void testConcurrentCache() {
        NodeId[] ids = new NodeId[1000];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = NodeId.randomId();
        }

        ConcurrentCache<NodeId, NodeId> cache =
            new ConcurrentCache<NodeId, NodeId>("test");
        cache.setMaxMemorySize(ids.length / 2);

        for (int i = 0; i < ids.length; i++) {
            for (int j = 0; j < i; j += 3) {
                cache.get(ids[j]);
            }
            cache.put(ids[i], ids[i], 1);
        }

        assertTrue(cache.getMemoryUsed() <= ids.length / 2);

        int n = 0;
        for (int i = 0; i < ids.length; i++) {
            if (cache.containsKey(ids[i])) {
                n++;
            }
        }

        assertTrue(n <= ids.length / 2);

        n = 0;
        for (int i = 0; i < ids.length; i += 3) {
            if (cache.containsKey(ids[i])) {
                n++;
            }
        }

        assertTrue(cache.getMemoryUsed() > ids.length / 4);
    }

}
