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
package org.apache.jackrabbit.iterator;

import java.util.NoSuchElementException;

import javax.jcr.RangeIterator;

/**
 * Array implementation of the JCR
 * {@link javax.jcr.RangeIterator RangeIterator} interface. This class
 * implements the RangeIterator functionality for an underlying array
 * of objects. Used as the base class for the type-specific iterator
 * classes defined in this package.
 */
class ArrayIterator implements RangeIterator {

    /** The current iterator position. */
    private int position;

    /** The underlying array of objects. */
    private final Object[] array;

    /**
     * Creates an iterator for the given array of objects.
     *
     * @param array the objects to iterate
     */
    protected ArrayIterator(Object[] array) {
        this.position = 0;
        this.array = array;
    }

    /**
     * Checks whether there are more elements in the array.
     *
     * @return <code>true</code> if more elements are available,
     *         <code>false</code> otherwise
     * @see Iterator#hasNext()
     */
    public boolean hasNext() {
        return (position < array.length);
    }

    /**
     * Returns the next array element and advances the array position.
     *
     * @return next element
     * @see Iterator#next()
     */
    public Object next() {
        return array[position++];
    }

    /**
     * Element removal is not supported.
     *
     * @throws UnsupportedOperationException always thrown
     * @see Iterator#remove()
     */
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Advances the array position the given number of elements.
     *
     * @param items number of items to skip
     * @throws IllegalArgumentException if the given number of items is negative
     * @throws NoSuchElementException if skipping past the end of the array
     * @see RangeIterator#skip(long)
     */
    public void skip(long items)
            throws IllegalArgumentException, NoSuchElementException {
        if (items < 0) {
            throw new IllegalArgumentException("Negative number of items");
        } else if (position + items < array.length) {
            position += items;
        } else {
            throw new NoSuchElementException("No more elements in the array");
        }
    }

    /**
     * Returns the length of the array.
     *
     * @return array length
     * @see RangeIterator#getSize()
     */
    public long getSize() {
        return array.length;
    }

    /**
     * Returns the current array position
     *
     * @return array position
     * @see RangeIterator#getPosition()
     */
    public long getPosition() {
        return position;
    }

}
