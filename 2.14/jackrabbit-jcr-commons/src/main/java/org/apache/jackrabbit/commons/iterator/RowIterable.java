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

import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.jackrabbit.commons.JcrUtils;

/**
 * Adapter class that adapts a {@link RowIterator} instance to an
 * {@link Iterable} instance that always returns the same underlying
 * iterator.
 *
 * @since Apache Jackrabbit 2.0
 * @deprecated - Use {@link JcrUtils#in(RowIterator)} instead
 */
@Deprecated
public class RowIterable implements Iterable<Row> {

    /**
     * The row iterator being adapted.
     */
    private final RowIterator iterator;

    /**
     * Creates an iterable adapter for the given row iterator.
     *
     * @param iterator the row iterator to be adapted
     */
    public RowIterable(RowIterator iterator) {
        this.iterator = iterator;
    }

    /**
     * Returns the row iterator.
     *
     * @return row iterator
     */
    @SuppressWarnings("unchecked")
    public Iterator<Row> iterator() {
        return iterator;
    }

}
