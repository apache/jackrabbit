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
 * <code>GrowingLRUMapTest</code>...
 */
public class GrowingLRUMapTest extends TestCase {

    public void testMaxSize() {
        int initialSize = 2;
        int maxSize = 10;
        GrowingLRUMap m = new GrowingLRUMap(initialSize, maxSize);

        for (int i = 0; i < 50; i++) {
            m.put("key" + i, "value" + i);

            if (i <= maxSize) {
                assertSame("i = " + i, i+1, m.size());
            } else {
                assertNotSame("i = " + i, i+1, m.size());
            }
        }
    }
}