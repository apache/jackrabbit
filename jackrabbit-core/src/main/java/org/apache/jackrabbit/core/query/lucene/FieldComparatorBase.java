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

package org.apache.jackrabbit.core.query.lucene;

import org.apache.lucene.search.FieldComparator;

import java.io.IOException;

/**
 * Abstract base class for <code>FieldComparator</code> implementations
 * which are based on values in the form of <code>Comparables</code>.
 */
abstract public class FieldComparatorBase extends FieldComparator {

    /**
     * The bottom value.
     */
    private Comparable<?> bottom;

    /**
     * Value for a document
     *
     * @param doc  id of the document
     * @return  the value for the given id
     */
    protected abstract Comparable<?> sortValue(int doc);

    /**
     * Retrieves the value of a given slot
     *
     * @param slot  index of the value to retrieve
     * @return  the value in the given slot
     */
    protected abstract Comparable<?> getValue(int slot);

    /**
     * Puts a value into a given slot
     *
     * @param slot  index where to put the value
     * @param value  the value to put into the given slot
     */
    protected abstract void setValue(int slot, Comparable<?> value);

    @Override
    public int compare(int slot1, int slot2) {
        return compare(getValue(slot1), getValue(slot2));
    }

    @Override
    public int compareBottom(int doc) throws IOException {
        return compare(bottom, sortValue(doc));
    }

    @Override
    public void setBottom(int slot) {
        bottom = getValue(slot);
    }

    /**
     * Compare two values
     *
     * @param val1  first value
     * @param val2  second value
     * @return  A negative integer if <code>val1</code> comes before <code>val2</code>,
     *   a positive integer if <code>val1</code> comes after <code>val2</code> and
     *   <code>0</code> if <code>val1</code> and <code>val2</code> are equal.
     */
    protected int compare(Comparable<?> val1, Comparable<?> val2) {
        if (val1 == null) {
            if (val2 == null) {
                return 0;
            }
            return -1;
        }
        else if (val2 == null) {
            return 1;
        }
        return Util.compare(val1, val2);
    }

    @Override
    public void copy(int slot, int doc) throws IOException {
        setValue(slot, sortValue(doc));
    }

    @Override
    public Comparable<?> value(int slot) {
        return getValue(slot);
    }
}
