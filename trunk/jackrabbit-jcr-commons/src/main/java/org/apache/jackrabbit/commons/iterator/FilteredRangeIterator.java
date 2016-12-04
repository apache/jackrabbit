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

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.jcr.RangeIterator;

import org.apache.jackrabbit.commons.predicate.Predicate;

/**
 * Filtering decorator for iterators.
 */
public class FilteredRangeIterator implements RangeIterator {

    /**
     * The underlying iterator.
     */
    private final Iterator<?> iterator;

    /**
     * Predicate used for filtering.
     */
    private final Predicate predicate;

    /**
     * Buffer of pre-fetched objects from the underlying iterator.
     */
    private final Object[] buffer;

    /**
     * Current position within the buffer. Guaranteed to be within the
     * range [0, {@link #bufferSize}].
     */
    private int bufferPosition = 0;

    /**
     * Number of pre-fetched objects in the buffer.
     */
    private int bufferSize = 0;

    /**
     * The zero-based position of the first element in the buffer.
     */
    private long position = 0;

    /**
     * The number of filtered items up to the end of the buffer.
     */
    private long size = 0;


    /**
     * Creates a new filtered iterator.
     *
     * @param iterator underlying iterator
     * @param predicate 
     * @param bufferSize
     */
    public FilteredRangeIterator(
            Iterator<?> iterator, Predicate predicate, int bufferSize) {
        this.iterator = iterator;
        this.predicate = predicate;
        this.buffer = new Object[bufferSize];
    }

    /**
     * Creates a new filtered iterator with the default pre-fetch buffer size.
     *
     * @param iterator underlying iterator
     * @param predicate predicate used for filtering
     */
    public FilteredRangeIterator(Iterator<?> iterator, Predicate predicate) {
        this(iterator, predicate, 1000);
    }

    /**
     * Creates a pre-fetching decorator for the given iterator. This
     * constructor is mostly useful when it is desirable to use the
     * pre-fetching feature to automatically 
     * 
     *
     * @param iterator underlying iterator
     */
    public FilteredRangeIterator(Iterator<?> iterator) {
        this(iterator, Predicate.TRUE, 1000);
    }

    /**
     * Pre-fetches more items into the buffer.
     */
    private void fetch() {
        if (bufferPosition == bufferSize) {
            position += bufferSize;
            bufferPosition = 0;
            bufferSize = 0;
            while (bufferSize < buffer.length && iterator.hasNext()) {
                Object object = iterator.next();
                if (predicate.evaluate(object)) {
                    buffer[bufferSize++] = object;
                }
            }
            size += bufferSize;
        }
    }

    /**
     * Returns the zero-based position of the next element in this iterator.
     *
     * @return position of the next element
     */
    public long getPosition() {
        return position + bufferPosition;
    }

    /**
     * Returns the total number of elements in this iterator, or -1 if that
     * number is unknown.
     *
     * @return number of elements in this iterator, or -1
     */
    public long getSize() {
        fetch();
        if (iterator.hasNext()) {
            return -1; // still unknown
        } else {
            return size;
        }
    }

    /**
     * Skips the next n elements.
     *
     * @param n number of elements to skip
     * @throws IllegalArgumentException if n is negative
     * @throws NoSuchElementException if there are not enough elements to skip
     */
    public void skip(long n)
            throws IllegalArgumentException, NoSuchElementException {
        if (n < 0) {
            throw new IllegalArgumentException();
        }
        while (n > 0) {
            fetch();
            if (bufferPosition < bufferSize) {
                long m = Math.min(n, bufferSize - bufferPosition);
                bufferPosition += m;
                n -= m;
            } else {
                throw new NoSuchElementException();
            }
        }
    }

    /**
     * Checks whether there are more elements in this iterator.
     *
     * @return <code>true</code> if there are more elements,
     *         <code>false</code> otherwise
     */
    public boolean hasNext() {
        fetch();
        return bufferPosition < bufferSize;
    }

    /**
     * Returns the next element in this iterator.
     *
     * @return next element
     * @throws NoSuchElementException if there are no more elements
     */
    public Object next() throws NoSuchElementException {
        fetch();
        if (bufferPosition < bufferSize) {
            return buffer[bufferPosition++];
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * Not supported.
     */
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

}
