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
package org.apache.jackrabbit.commons.iterator;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class implements the concatenation of iterators. The implementation
 * is lazy in the sense that advancing of any iterator is deferred as much
 * as possible. Specifically no iterator is fully unwrapped at one single
 * point of time.
 *
 * @param <T>  type of values iterating over
 */
public class LazyIteratorChain<T> implements Iterator<T> {
    private final Iterator<Iterator<T>> iterators;
    private Iterator<T> currentIterator;
    private Boolean hasNext;

    /**
     * Returns the concatenation of all iterators in <code>iterators</code>.
     *
     * @param <T>
     * @param iterators
     * @return
     */
    public static <T> Iterator<T> chain(Iterator<Iterator<T>> iterators) {
        return new LazyIteratorChain<T>(iterators);
    }

    /**
     * Returns the concatenation of all iterators in <code>iterators</code>.
     *
     * @param <T>
     * @param iterators
     * @return
     */
    public static <T> Iterator<T> chain(Iterator<T>... iterators) {
        return new LazyIteratorChain<T>(iterators);
    }

    public LazyIteratorChain(Iterator<Iterator<T>> iterators) {
        super();
        this.iterators = iterators;
    }

    public LazyIteratorChain(Iterator<T>... iterators) {
        super();
        this.iterators = Arrays.asList(iterators).iterator();
    }

    public boolean hasNext() {
        // Memoizing the result of hasNext is crucial to performance when recursively
        // traversing tree structures.
        if (hasNext == null) {
            while ((currentIterator == null || !currentIterator.hasNext()) && iterators.hasNext()) {
                currentIterator = iterators.next();
            }
            hasNext = currentIterator != null && currentIterator.hasNext();
        }
        return hasNext;
    }

    public T next() {
        if (hasNext()) {
            hasNext = null;
            return currentIterator.next();
        }
        else {
            throw new NoSuchElementException();
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}