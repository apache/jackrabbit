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
package org.apache.jackrabbit.core.query.lucene;

/**
 * <code>IOCounters</code> provides a basic mechanism to track I/O during query
 * execution.
 */
public class IOCounters {

    /**
     * The key in the per-query-cache that identifies the read count.
     */
    private static final Object READ_COUNT = new Object();

    /**
     * @return the current read count for caused by the current thread.
     */
    public static long getReads() {
        Long value = (Long) PerQueryCache.getInstance().get(IOCounters.class,  READ_COUNT);
        if (value == null) {
            value = 0L;
        }
        return value;
    }

    /**
     * Increments the read count caused by the current thread.
     */
    public static void incrRead() {
        PerQueryCache cache = PerQueryCache.getInstance();
        Long value = (Long) cache.get(IOCounters.class,  READ_COUNT);
        if (value == null) {
            value = 0L;
        }
        cache.put(IOCounters.class, READ_COUNT, value + 1);
    }
}
