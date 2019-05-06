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

import java.util.NoSuchElementException;

import javax.jcr.RangeIterator;

/**
 * Base class for decorating {@link RangeIterator}s.
 */
public class RangeIteratorDecorator implements RangeIterator {

    /**
     * The decorated iterator.
     */
    private final RangeIterator iterator;

    /**
     * Creates a decorated iterator. Protected since this class is only
     * useful when subclassed.
     *
     * @param iterator the iterator to be decorated
     */
    protected RangeIteratorDecorator(RangeIterator iterator) {
        this.iterator = iterator;
    }

    //-------------------------------------------------------< RangeIterator >

    /**
     * Delegated to the underlying iterator.
     *
     * @return iterator position
     */
    public long getPosition() {
        return iterator.getPosition();
    }

    /**
     * Delegated to the underlying iterator.
     *
     * @return iterator size
     */
    public long getSize() {
        return iterator.getSize();
    }

    /**
     * Delegated to the underlying iterator.
     *
     * @param n number of elements to skip
     * @throws NoSuchElementException if skipped past the last element
     */
    public void skip(long n) throws NoSuchElementException {
        iterator.skip(n);
    }

    //------------------------------------------------------------< Iterator >

    /**
     * Delegated to the underlying iterator.
     *
     * @return <code>true</code> if the iterator has more elements,
     *         <code>false</code> otherwise
     */
    public boolean hasNext() {
        return iterator.hasNext();
    }

    /**
     * Delegated to the underlying iterator.
     *
     * @return next element
     * @throws NoSuchElementException if there are no more elements
     */
    public Object next() throws NoSuchElementException {
        return iterator.next();
    }

    /**
     * Delegated to the underlying iterator.
     *
     * @throws UnsupportedOperationException if the operation is not supported
     * @throws IllegalStateException if there is no element to be removed
     */
    public void remove()
    throws UnsupportedOperationException, IllegalStateException {
        iterator.remove();
    }
}
