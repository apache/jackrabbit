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

import org.apache.lucene.index.IndexReader;

import java.io.IOException;

/**
 * Implements a <code>FieldComparator</code> which decorates a
 * base comparator.
 */
abstract class FieldComparatorDecorator extends FieldComparatorBase {

    /**
     * The base comparator
     */
    private final FieldComparatorBase base;

    /**
     * Create a new instance which delegates to a base comparator.
     * @param base  delegatee
     */
    public FieldComparatorDecorator(FieldComparatorBase base) {
        this.base = base;
    }

    @Override
    public void setNextReader(IndexReader reader, int docBase) throws IOException {
        base.setNextReader(reader, docBase);
    }

    @Override
    protected Comparable sortValue(int doc) {
        return base.sortValue(doc);
    }

    @Override
    protected Comparable getValue(int slot) {
        return base.getValue(slot);
    }

    @Override
    protected void setValue(int slot, Comparable value) {
        base.setValue(slot, value);
    }
}
