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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.jcr.RangeIterator;

/**
 * Adapter for turning normal {@link Iterator}s into {@link RangeIterator}s.
 * This helper class is used by the adapter classes in this package to
 * implement the JCR iterator interfaces on top of normal Java iterators.
 */
public class RangeIteratorAdapter implements RangeIterator {

    /**
     * Static instance of an empty {@link RangeIterator}.
     */
    public static final RangeIterator EMPTY =
        new RangeIteratorAdapter(Collections.EMPTY_LIST);

    /**
     * The adapted iterator instance.
     */
    private final Iterator iterator;

    /**
     * Number of elements in the adapted iterator, or -1 if unknown.
     */
    private long size;

    /**
     * Current position of the iterator.
     */
    private long position;

    /**
     * Creates an adapter for the given iterator of the given size.
     *
     * @param iterator adapted iterator
     * @param size size of the iterator, or -1 if unknown
     */
    public RangeIteratorAdapter(Iterator iterator, long size) {
        this.iterator = iterator;
        this.size = size;
        this.position = 0;
    }

    /**
     * Creates an adapter for the given iterator of unknown size.
     *
     * @param iterator adapted iterator
     */
    public RangeIteratorAdapter(Iterator iterator) {
        this(iterator, -1);
    }

    /**
     * Creates a {@link RangeIterator} for the given collection.
     *
     * @param collection the collection to iterate
     */
    public RangeIteratorAdapter(Collection collection) {
        this(collection.iterator(), collection.size());
    }

    //-------------------------------------------------------< RangeIterator >

    /**
     * Returns the current position of the iterator.
     *
     * @return iterator position
     */
    public long getPosition() {
        return position;
    }

    /**
     * Returns the size of the iterator.
     *
     * @return iterator size, or -1 if unknown
     */
    public long getSize() {
        return size;
    }

    /**
     * Skips the given number of elements.
     *
     * @param n number of elements to skip
     * @throws IllegalArgumentException if n is negative
     * @throws NoSuchElementException if skipped past the last element
     */
    public void skip(long n)
    throws IllegalArgumentException, NoSuchElementException {
        if (n < 0) {
            throw new IllegalArgumentException("skip(" + n + ")");
        }
        for (long i = 0; i < n; i++) {
            next();
        }
    }

    //------------------------------------------------------------< Iterator >

    /**
     * Checks if this iterator has more elements. If there are no more
     * elements and the size of the iterator is unknown, then the size is
     * set to the current position.
     *
     * @return <code>true</code> if this iterator has more elements,
     *         <code>false</code> otherwise
     */
    public boolean hasNext() {
        if (iterator.hasNext()) {
            return true;
        } else {
            if (size == -1) {
                size = position;
            }
            return false;
        }
    }

    /**
     * Returns the next element in this iterator and advances the iterator
     * position. If there are no more elements and the size of the iterator
     * is unknown, then the size is set to the current position.
     *
     * @return next element
     * @throws NoSuchElementException if there are no more elements
     */
    public Object next() throws NoSuchElementException {
        try {
            Object next = iterator.next();
            position++;
            return next;
        } catch (NoSuchElementException e) {
            if (size == -1) {
                size = position;
            }
            throw e;
        }
    }

    /**
     * Removes the previously retrieved element. Decreases the current
     * position and size of this iterator.
     *
     * @throws UnsupportedOperationException if removes are not permitted
     * @throws IllegalStateException if there is no previous element to remove
     */
    public void remove()
    throws UnsupportedOperationException, IllegalStateException {
        iterator.remove();
        position--;
        if (size != -1) {
            size--;
        }
    }

}
