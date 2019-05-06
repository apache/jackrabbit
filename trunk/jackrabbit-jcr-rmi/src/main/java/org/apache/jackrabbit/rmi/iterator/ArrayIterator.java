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
package org.apache.jackrabbit.rmi.iterator;

import java.util.NoSuchElementException;

import javax.jcr.RangeIterator;

/**
 * Array implementation of the JCR
 * {@link javax.jcr.RangeIterator RangeIterator} interface. This class
 * implements the RangeIterator functionality for an underlying array
 * of objects. Used as the base class for the type-specific iterator
 * classes defined in this package.
 */
public class ArrayIterator implements RangeIterator {

    /** The current iterator position. */
    private int position;

    /** The underlying array of objects. */
    private Object[] array;

    /**
     * Creates an iterator for the given array of objects.
     *
     * @param array the objects to iterate
     */
    public ArrayIterator(Object[] array) {
        this.position = 0;
        this.array = array;
    }

    /** {@inheritDoc} */
    public boolean hasNext() {
        return (position < array.length);
    }

    /** {@inheritDoc} */
    public Object next() {
        return array[position++];
    }

    /** {@inheritDoc} */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public void skip(long items) {
        position += items;
        if (position > array.length) {
            position = array.length;
            throw new NoSuchElementException(
                    "Skipped past the last element of the iterator");
        }
    }

    /** {@inheritDoc} */
    public long getSize() {
        return array.length;
    }

    /** {@inheritDoc} */
    public long getPosition() {
        return position;
    }

}
