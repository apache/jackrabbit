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

import junit.framework.TestCase;

import org.apache.jackrabbit.core.util.StringIndex;

public class HashMapIndexTest extends TestCase {

    private StringIndex index;

    private int load;
    private int save;

    protected void setUp() throws Exception {
        index = new HashMapIndex() {
            @Override
            protected void load() {
                load++;
            }
            @Override
            protected void save() {
                save++;
            }
        };
        load = 0;
        save = 0;
    }

    public void testIndex() {
        assertEquals(0, load);
        assertEquals(0, save);

        int test = index.stringToIndex("test");
        assertEquals(1, load);
        assertEquals(1, save);

        assertEquals(test, index.stringToIndex("test"));
        assertEquals(1, load);
        assertEquals(1, save);

        assertEquals("test", index.indexToString(test));
        assertEquals(1, load);
        assertEquals(1, save);

        assertNull(index.indexToString(test + 1));
        assertEquals(2, load);
        assertEquals(1, save);

        int foo = index.stringToIndex("foo");
        assertTrue(test != foo);
        assertEquals(3, load);
        assertEquals(2, save);

        assertEquals(foo, index.stringToIndex("foo"));
        assertEquals(3, load);
        assertEquals(2, save);

        assertEquals("foo", index.indexToString(foo));
        assertEquals(3, load);
        assertEquals(2, save);
    }

}
