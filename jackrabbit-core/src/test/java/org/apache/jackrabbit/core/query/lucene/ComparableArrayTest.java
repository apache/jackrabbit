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

import static junit.framework.Assert.assertEquals;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.query.lucene.SharedFieldCache.ComparableArray;
import org.junit.Test;

public class ComparableArrayTest {

    /**
     * Test for JCR-2906 to make sure the SharedFieldCache arranges the entries
     * properly and keeps the internal array creation efficient.
     */
    @Test
    public void testInsert() throws RepositoryException {
        ComparableArray ca = new ComparableArray("a", 1);
        assertEquals("a", ca.toString());
        assertEquals(1, ca.getOffset());

        // insert before
        ca.insert("b", 0);
        assertEquals("[b, a]", ca.toString());
        assertEquals(0, ca.getOffset());

        // insert after
        ca.insert("c", 3);
        assertEquals("[b, a, null, c]", ca.toString());
        assertEquals(0, ca.getOffset());

        // insert inside
        ca.insert("d", 2);
        assertEquals("[b, a, d, c]", ca.toString());
        assertEquals(0, ca.getOffset());
    }
}
