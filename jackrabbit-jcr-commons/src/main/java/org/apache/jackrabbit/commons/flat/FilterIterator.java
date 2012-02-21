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

import org.apache.jackrabbit.commons.predicate.Predicate;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator filtering out items which do not match a given predicate.
 * @param <T>
 * @deprecated use {@link org.apache.jackrabbit.commons.iterator.FilterIterator}
 */
public class FilterIterator<T> extends org.apache.jackrabbit.commons.iterator.FilterIterator<T> {

    /**
     * Create a new filtered iterator based on the given <code>iterator</code>.
     *
     * @param tIterator  iterator to filter
     * @param predicate only item matching this predicate are included
     */
    public FilterIterator(Iterator<T> tIterator, Predicate predicate) {
        super(tIterator, predicate);
    }

}
