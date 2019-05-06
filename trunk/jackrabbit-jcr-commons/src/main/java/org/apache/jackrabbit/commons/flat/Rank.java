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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * <p>
 * This class does efficient ranking of values of type <code>T</code> wrt. to a
 * {@link Comparator} for <code>T</code>. After creating an instance of
 * <code>Rank</code>, the {@link #take(int)} method returns the next
 * <code>k</code> smallest values. That is, each of these values is smaller than
 * every value not yet retrieved. The order of the values returned by
 * <code>take</code> is not specified in general. However if the values are in
 * increasing order, the values returned by <code>take</code> will also be in
 * increasing order.
 * </p>
 * <p>
 * <em>Note</em>: The values <em>may not contain duplicates</em> or the behavior
 * of <code>take</code> is not defined.
 * </p>
 *
 * @param <T> Type of values in this <code>Rank</code>.
 */
public class Rank<T> {
    private final T[] values;
    private final Comparator<? super T> order;
    private int first;

    /**
     * Create a new instance of <code>Rank</code> for a given array of
     * <code>values</code> and a given <code>order</code>. The
     * <code>values</code> are manipulated in place, no copying is performed.
     *
     * @param values values for ranking. Duplicates are <em>not allowed</em>.
     * @param order Ordering for ranking
     */
    public Rank(T[] values, Comparator<? super T> order) {
        super();
        this.values = values;
        this.order = order;
    }

    /**
     * Create a new instance of <code>Rank</code> for a given collection of
     * <code>values</code> and a given <code>order</code>. The
     * <code>values</code> are copied into an internal array before they are
     * manipulated.
     *
     * @param values values for ranking. Duplicates are <em>not allowed</em>.
     * @param componentType type evidence for the values
     * @param order Ordering for ranking
     */
    public Rank(Collection<T> values, Class<T> componentType, Comparator<? super T> order) {
        super();
        this.values = toArray(values, componentType);
        this.order = order;
    }

    /**
     * Create a new instance of <code>Rank</code> for the first
     * <code>count</code> values in a a given iterator of <code>values</code>
     * and a given <code>order</code>. The <code>values</code> are copied into
     * an internal array before they are manipulated.
     *
     * @param values values for ranking. Duplicates are <em>not allowed</em>.
     * @param componentType type evidence for the values
     * @param count Number of items to include. -1 for all.
     * @param order Ordering for ranking
     */
    public Rank(Iterator<T> values, Class<T> componentType, int count, Comparator<? super T> order) {
        super();
        this.order = order;

        if (count >= 0) {
            this.values = createArray(count, componentType);
            for (int k = 0; k < count; k++) {
                this.values[k] = values.next();
            }
        }
        else {
            List<T> l = new LinkedList<T>();
            while (values.hasNext()) {
                l.add(values.next());
            }
            this.values = toArray(l, componentType);
        }
    }

    /**
     * Create a new instance of <code>Rank</code> for a given array of
     * <code>values</code>. The order is determined by the natural ordering of
     * the values (i.e. through {@link Comparable}). The <code>values</code> are
     * manipulated in place, no copying is performed.
     *
     * @param <S> extends Comparable&lt;S&gt;
     * @param values values for ranking. Duplicates are <em>not allowed</em>.
     * @return A new instance of <code>Rank</code>.
     */
    public static <S extends Comparable<S>> Rank<S> rank(S[] values) {
        return new Rank<S>(values, Rank.<S>comparableComparator());
    }

    /**
     * Create a new instance of <code>Rank</code> for a given collection of
     * <code>values</code>. The order is determined by the natural ordering of
     * the values (i.e. through {@link Comparable}). The <code>values</code> are
     * copied into an internal array before they are manipulated.
     *
     * @param <S> extends Comparable&lt;S&gt;
     * @param values values for ranking. Duplicates are <em>not allowed</em>.
     * @param componentType type evidence for the values
     * @return A new instance of <code>Rank</code>.
     */
    public static <S extends Comparable<S>> Rank<S> rank(Collection<S> values, Class<S> componentType) {
        return new Rank<S>(values, componentType, Rank.<S>comparableComparator());
    }

    /**
     * Create a new instance of <code>Rank</code> for the first
     * <code>count</code> values in a a given iterator of <code>values</code>.
     * The order is determined by the natural ordering of the values (i.e.
     * through {@link Comparable}). The <code>values</code> are copied into an
     * internal array before they are manipulated.
     *
     * @param <S> extends Comparable&lt;S&gt;
     * @param values values for ranking. Duplicates are <em>not allowed</em>.
     * @param componentType type evidence for the values
     * @param count Number of items to include. -1 for all.
     * @return A new instance of <code>Rank</code>.
     */
    public static <S extends Comparable<S>> Rank<S> rank(Iterator<S> values, Class<S> componentType, int count) {
        return new Rank<S>(values, componentType, count, Rank.<S>comparableComparator());
    }

    /**
     * Utility method for creating a {@link Comparator} of <code>T</code> from a
     * {@link Comparable} of type <code>T</code>.
     *
     * @param <T> extends Comparable&lt;T&gt;
     * @return Comparator whose order is defined by <code>T</code>.
     */
    public static <T extends Comparable<T>> Comparator<T> comparableComparator() {
        return new Comparator<T>() {
            public int compare(T c1, T c2) {
                return c1.compareTo(c2);
            }
        };
    }

    public Comparator<? super T> getOrder() {
        return order;
    }

    /**
     * Returns the <code>n</code>-th smallest values remaining in this
     * <code>Rank</code>.
     *
     * @param n Number of values to return
     * @return An iterator containing the next <code>n</code> smallest values.
     * @throws NoSuchElementException if this <code>Rank</code> has not enough
     *             remaining elements or when <code>n</code> is negative.
     */
    public Iterator<T> take(int n) {
        if (n < 0 || n + first > values.length) {
            throw new NoSuchElementException();
        }

        if (n > 0) {
            take(n, first, values.length - 1);
            first += n;
            return Arrays.asList(values).subList(first - n, first).iterator();
        } else {
            return Collections.<T>emptySet().iterator();
        }
    }

    /**
     * Returns the number of remaining items in the <code>Rank</code>.
     *
     * @return number of remaining items.
     */
    public int size() {
        return values.length - first;
    }

    // -----------------------------------------------------< internal >---

    /**
     * Rearrange {@link #values} such that each of the <code>n</code> first
     * values starting at <code>from</code> is smaller that all the remaining
     * items up to <code>to</code>.
     */
    private void take(int n, int from, int to) {
        // Shortcut for all values
        if (n >= to - from + 1) {
            return;
        }

        // Choosing the n-th value as pivot results in correct partitioning after one pass
        // for already ordered values.
        int pivot = from + n - 1;
        int lo = from;
        int hi = to;

        // Partition values around pivot
        while (lo < hi) {
            // Find values to swap around the pivot
            while (order.compare(values[lo], values[pivot]) < 0) {
                lo++;
            }
            while (order.compare(values[hi], values[pivot]) > 0) {
                hi--;
            }
            if (lo < hi) {
                // Swap values and keep track of pivot position in case the pivot itself is swapped
                if (lo == pivot) {
                    pivot = hi;
                } else if (hi == pivot) {
                    pivot = lo;
                }
                swap(lo, hi);
                lo++;
                hi--;
            }
        }

        // Actual number of values taken
        int nn = pivot + 1 - from;
        if (nn > n) {      // Recurse: take first n elements from first partition
            take(n, from, pivot);
        }
        else if (nn < n) { // Recurse: take first n - nn elements from second partition
            take(n - nn, pivot + 1, to);
        }
        // else done
    }

    private void swap(int lo, int hi) {
        T t1 = values[lo];
        T t2 = values[hi];
        if (order.compare(t1, t2) == 0) {
            throw new IllegalStateException("Detected duplicates " + t1);
        }
        values[lo] = t2;
        values[hi] = t1;
    }

    // -----------------------------------------------------< utility >---

    private static <S> S[] toArray(Collection<S> collection, Class<S> componentType) {
        return collection.toArray(createArray(collection.size(), componentType));
    }

    @SuppressWarnings("unchecked")
    private static <S> S[] createArray(int size, Class<S> componentType) {
        return (S[]) Array.newInstance(componentType, size);
    }

}
