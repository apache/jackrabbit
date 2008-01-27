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
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.jcr.RangeIterator;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 * Adapter class for turning {@link RangeIterator}s or {@link Iterator}s
 * into {@link RowIterator}s.
 */
public class RowIteratorAdapter extends RangeIteratorDecorator
        implements RowIterator {

    /**
     * Static instance of an empty {@link RowIterator}.
     */
    public static final RowIterator EMPTY =
        new RowIteratorAdapter(RangeIteratorAdapter.EMPTY);

    /**
     * Creates an adapter for the given {@link RangeIterator}.
     *
     * @param iterator iterator of {@link Row}s
     */
    public RowIteratorAdapter(RangeIterator iterator) {
        super(iterator);
    }

    /**
     * Creates an adapter for the given {@link Iterator}.
     *
     * @param iterator iterator of {@link Row}s
     */
    public RowIteratorAdapter(Iterator iterator) {
        super(new RangeIteratorAdapter(iterator));
    }

    /**
     * Creates an iterator for the given collection.
     *
     * @param collection collection of {@link Row}s
     */
    public RowIteratorAdapter(Collection collection) {
        super(new RangeIteratorAdapter(collection));
    }

    //---------------------------------------------------------< RowIterator >

    /**
     * Returns the next row.
     *
     * @return next row
     * @throws NoSuchElementException if there is no next row
     */
    public Row nextRow() {
        return (Row) next();
    }

}
