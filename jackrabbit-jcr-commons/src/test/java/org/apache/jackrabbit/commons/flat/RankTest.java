/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.jackrabbit.commons.flat;

import org.apache.jackrabbit.commons.flat.Rank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;

public class RankTest extends TestCase {
    private static final Random rnd = new Random();

    public void testEmpty() {
        Rank<Integer> r = Rank.rank(new Integer[0]);
        assertFalse(r.take(0).hasNext());
        assertEquals(0, r.size());
        try {
            r.take(1);
            fail("Excepted " + NoSuchElementException.class.getName());
        }
        catch (NoSuchElementException ignore) { }
    }

    public void testSingleton() {
        Rank<Integer> r = Rank.rank(new Integer[] {42});
        assertFalse(r.take(0).hasNext());
        assertEquals(1, r.size());

        Iterator<Integer> it = r.take(1);
        assertTrue(it.hasNext());
        assertEquals(0, r.size());
        assertEquals(42, it.next().intValue());

        assertFalse(r.take(0).hasNext());

        try {
            r.take(1);
            fail("Excepted " + NoSuchElementException.class.getName());
        }
        catch (NoSuchElementException ignore) { }
    }

    public void testRank() {
        for (int n = 1; n <= 2000; n++) {
            testRank(n);
        }
    }

    public void testGetAll() {
        int n = 100;
        List<Integer> values = createValues(n);
        Rank<Integer> r = Rank.rank(values, Integer.class);

        try {
            r.take(n + 1);
            fail("Expected " + NoSuchElementException.class.getName());
        }
        catch (NoSuchElementException ignore) { }

        assertEquals(n, r.size());

        Iterator<Integer> it = r.take(n);
        while (it.hasNext()) {
            assertTrue(values.remove(it.next()));
        }

        assertTrue(values.isEmpty());
        assertEquals(0, r.size());
    }

    public void testGetSingles() {
        int n = 100;
        List<Integer> values = createValues(n);
        Rank<Integer> r = Rank.rank(values, Integer.class);

        List<Integer> sorted = new ArrayList<Integer>();
        Iterator<Integer> it;
        while (r.size() > 0) {
            it = r.take(1);
            sorted.add(it.next());
            assertFalse(it.hasNext());
        }

        assertTrue(sorted.containsAll(values));
        assertTrue(values.containsAll(sorted));

        Comparator<? super Integer> order = r.getOrder();
        checkOrdered(sorted.iterator(), order);
    }

    public void testOrdered() {
        int n = 1000000;
        Integer[] values = new Integer[n];
        for (int k = 0; k < n; k++) {
            values[k] = k;
        }

        Rank<Integer> r = Rank.rank(values);
        while (r.size() > 0) {
            int k = Math.min(rnd.nextInt(n/10), r.size());
            checkOrdered(r.take(k), r.getOrder());
        }
    }

    // -----------------------------------------------------< internal >---

    private static List<Integer> createValues(int count) {
        Set<Integer> ints = new HashSet<Integer>();
        for (int k = 0; k < count; k++) {
            ints.add(rnd.nextInt());
        }

        List<Integer> intList = new LinkedList<Integer>(ints);
        Collections.shuffle(intList);
        return intList;
    }

    private <T> void checkOrdered(Iterator<T> it, Comparator<? super T> order) {
        T prev = it.next();
        while (it.hasNext()) {
            T next = it.next();
            assertTrue(order.compare(prev, next) < 0);
            prev = next;
        }
    }

    private static void testRank(int count) {
        List<Integer> ints = createValues(count);
        Rank<Integer> r = Rank.rank(ints, Integer.class);
        Set<Integer> all = new HashSet<Integer>(ints);
        Set<Integer> previous = new HashSet<Integer>();
        while (r.size() > 0) {
            int n = Math.min(rnd.nextInt(count + 1), r.size());
            Iterator<Integer> it = r.take(n);
            Set<Integer> actual = new HashSet<Integer>();
            while (it.hasNext()) {
                Integer i = it.next();
                assertTrue(actual.add(i));
                assertTrue(all.remove(i));
            }

            checkOrdering(previous, actual, r.getOrder());
            previous = actual;
        }

        assertTrue(all.isEmpty());
    }

    private static void checkOrdering(Set<Integer> previous, Set<Integer> actual,
            Comparator<? super Integer> order) {

        for (Iterator<Integer> pIt = previous.iterator(); pIt.hasNext(); ) {
            Integer p = pIt.next();
            for(Iterator<Integer> aIt = actual.iterator(); aIt.hasNext(); ) {
                Integer a = aIt.next();
                assertTrue(order.compare(p, a) < 0);
            }
        }
    }

}
