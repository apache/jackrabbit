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
package org.apache.jackrabbit.commons.iterator;

import java.util.Arrays;
import java.util.List;

import javax.jcr.RangeIterator;

import org.apache.jackrabbit.commons.predicate.Predicate;

import junit.framework.TestCase;

/**
 * Test cases for the {@link FilteredRangeIterator} class.
 *
 * @see <a href="https://issues.apache.org/jira/browse/JCR-2722">JCR-2722</a>
 */
public class FilteredRangeIteratorTest extends TestCase {

    private static final List<String> LIST = Arrays.asList("x", "y", "z");

    public void testMatchAll() {
        RangeIterator iterator = new FilteredRangeIterator(LIST.iterator());
        assertEquals(3, iterator.getSize());

        assertEquals(0, iterator.getPosition());
        assertTrue(iterator.hasNext());
        assertEquals("x", iterator.next());

        assertEquals(1, iterator.getPosition());
        assertTrue(iterator.hasNext());
        assertEquals("y", iterator.next());

        assertEquals(2, iterator.getPosition());
        assertTrue(iterator.hasNext());
        assertEquals("z", iterator.next());

        assertEquals(3, iterator.getPosition());
        assertFalse(iterator.hasNext());
    }

    public void testMatchNone() {
        RangeIterator iterator =
            new FilteredRangeIterator(LIST.iterator(), Predicate.FALSE);
        assertEquals(0, iterator.getSize());
        assertEquals(0, iterator.getPosition());
        assertFalse(iterator.hasNext());
    }

    public void testSkip() {
        RangeIterator iterator = new FilteredRangeIterator(LIST.iterator());
        assertEquals(3, iterator.getSize());

        assertEquals(0, iterator.getPosition());
        assertTrue(iterator.hasNext());
        assertEquals("x", iterator.next());

        iterator.skip(1);

        assertEquals(2, iterator.getPosition());
        assertTrue(iterator.hasNext());
        assertEquals("z", iterator.next());

        assertEquals(3, iterator.getPosition());
        assertFalse(iterator.hasNext());
    }

}
