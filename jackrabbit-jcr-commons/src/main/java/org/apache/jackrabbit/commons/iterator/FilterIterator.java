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

import org.apache.jackrabbit.commons.predicate.Predicate;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator filtering out items which do not match a given predicate.
 * @param <T>
 */
public class FilterIterator<T> implements Iterator<T> {
    private final Iterator<T> iterator;
    private final Predicate predicate;

    private T next = null;

    /**
     * Create a new filtered iterator based on the given <code>iterator</code>.
     *
     * @param iterator  iterator to filter
     * @param predicate  only item matching this predicate are included
     */
    public FilterIterator(Iterator<T> iterator, Predicate predicate) {
        super();
        this.iterator = iterator;
        this.predicate = predicate;
    }

    public boolean hasNext() {
        while (next == null && iterator.hasNext()) {
            T e = iterator.next();
            if (predicate.evaluate(e)) {
                next = e;
            }
        }

        return next != null;
    }

    public T next() {
        if (hasNext()) {
            T e = next;
            next = null;
            return e;
        }
        else {
            throw new NoSuchElementException();
        }
    }

    /**
     * @throws  UnsupportedOperationException always
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
