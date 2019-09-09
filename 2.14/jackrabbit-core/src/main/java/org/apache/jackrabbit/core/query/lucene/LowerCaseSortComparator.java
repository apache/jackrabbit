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
import org.apache.lucene.search.FieldComparatorSource;

import java.io.IOException;

/**
 * <code>LowerCaseSortComparator</code> implements a <code>FieldComparator</code> which
 * compares the lower-cased string values of a base comparator.
 */
public class LowerCaseSortComparator extends FieldComparatorSource {

    /**
     * The base comparator.
     */
    private final FieldComparatorSource base;

    /**
     * Creates a new upper case sort comparator.
     *
     * @param base the base sort comparator source.
     */
    public LowerCaseSortComparator(FieldComparatorSource base) {
        this.base = base;
    }

    @Override
    public FieldComparator newComparator(String fieldname, int numHits, int sortPos, boolean reversed) throws IOException {
        FieldComparator comparator = base.newComparator(fieldname, numHits, sortPos, reversed);
        assert comparator instanceof FieldComparatorBase;

        return new FieldComparatorDecorator((FieldComparatorBase) comparator) {
            @Override
            protected Comparable sortValue(int doc) {
                Comparable c = super.sortValue(doc);
                return c == null ? null : c.toString().toLowerCase();
            }
        };
    }

}
