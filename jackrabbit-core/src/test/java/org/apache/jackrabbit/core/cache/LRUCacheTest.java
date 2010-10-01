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

import junit.framework.TestCase;

/**
 * Test cases for the {@link LRUCache} class.
 */
public class LRUCacheTest extends TestCase {

    public void testLRUCache() {
        LRUCache<String, String> cache = new LRUCache<String, String>(3);

        cache.put("foo", "1", 1);
        assertTrue(cache.containsKey("foo"));
        assertEquals("1", cache.get("foo"));
        assertEquals(1, cache.getMemoryUsed());

        cache.put("bar", "12", 2);
        assertTrue(cache.containsKey("foo"));
        assertEquals("1", cache.get("foo"));
        assertTrue(cache.containsKey("bar"));
        assertEquals("12", cache.get("bar"));
        assertEquals(3, cache.getMemoryUsed());

        cache.put("baz", "123", 3);
        assertFalse(cache.containsKey("foo"));
        assertFalse(cache.containsKey("bar"));
        assertTrue(cache.containsKey("baz"));
        assertEquals("123", cache.get("baz"));
        assertEquals(3, cache.getMemoryUsed());

        cache.put("foo", "1", 1);
        assertTrue(cache.containsKey("foo"));
        assertFalse(cache.containsKey("bar"));
        assertFalse(cache.containsKey("baz"));
        assertEquals("1", cache.get("foo"));
        assertEquals(1, cache.getMemoryUsed());

        cache.put("bar", "12", 2);
        assertTrue(cache.containsKey("foo"));
        assertEquals("1", cache.get("foo"));
        assertTrue(cache.containsKey("bar"));
        assertEquals("12", cache.get("bar"));
        assertEquals(3, cache.getMemoryUsed());

        cache.remove("foo");
        assertFalse(cache.containsKey("foo"));
        assertTrue(cache.containsKey("bar"));
        assertEquals("12", cache.get("bar"));
        assertEquals(2, cache.getMemoryUsed());

        cache.put("foo", "1", 1);
        assertTrue(cache.containsKey("foo"));
        assertEquals("1", cache.get("foo"));
        assertTrue(cache.containsKey("bar"));
        assertEquals("12", cache.get("bar"));
        assertEquals(3, cache.getMemoryUsed());
    }

}
