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

import java.util.Map;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>IOCounters</code> provides a basic mechanism to track I/O during query
 * execution.
 */
public class IOCounters {

    private static final Logger log = LoggerFactory.getLogger(IOCounters.class);

    private static final Map<Thread, Long> counts =
        new WeakHashMap<Thread, Long>();

    /**
     * @return the current read count for caused by the current thread.
     */
    public static synchronized long getReads() {
        Long count = counts.get(Thread.currentThread());
        return count != null ? count : 0;
    }

    /**
     * Increments the read count caused by the current thread.
     */
    public static void incrRead() {
        if (log.isDebugEnabled()) {
            synchronized (IOCounters.class) {
                counts.put(Thread.currentThread(), getReads() + 1);
            }
        }
    }
}
