/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 * Array implementation of the JCR
 * {@link javax.jcr.query.RowIterator RowIterator} interface.
 * This class is used by the JCR-RMI client adapters to convert
 * node arrays to iterators.
 *
 * @author Philipp Koch
 */
public class ArrayRowIterator extends ArrayIterator implements RowIterator {

    /**
     * Creates an iterator for the given array of rows.
     *
     * @param rows the rows to iterate
     */
    public ArrayRowIterator(Row[] rows) {
        super(rows);
    }

    /** {@inheritDoc} */
    public Row nextRow() {
        return (Row) next();
    }

}
