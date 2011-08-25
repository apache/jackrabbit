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
package org.apache.jackrabbit.core.query.lucene.sort;

import javax.jcr.Value;

import org.apache.jackrabbit.core.query.lucene.join.ValueComparator;

class ValueComparableWrapper implements Comparable<ValueComparableWrapper> {
    private final ValueComparator comparator = new ValueComparator();

    private final Value[] v;
    private final boolean reversed;

    public ValueComparableWrapper(final Value[] v, final boolean reversed) {
        this.v = v;
        this.reversed = reversed;
    }

    public Value[] getValue() {
        return v;
    }

    public int compareTo(ValueComparableWrapper o) {
        final int d = compare(v, o.getValue());
        if (d != 0) {
            if (reversed) {
                return -d;
            }
            return -d;
        }
        return 0;
    }

    private int compare(Value[] a, Value[] b) {
        for (int i = 0; i < a.length && i < b.length; i++) {
            int d = comparator.compare(a[i], b[i]);
            if (d != 0) {
                return d;
            }
        }
        return a.length - b.length;
    }
}