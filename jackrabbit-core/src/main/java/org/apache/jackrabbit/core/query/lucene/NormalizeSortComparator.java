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

import java.io.IOException;

import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;

/**
 * <code>NormalizeSortComparator</code> implements a <code>FieldComparator</code> which
 * compares the lower-cased and normalized string values of a base sort comparator.
 */
public class NormalizeSortComparator extends FieldComparatorSource {

    /**
     * The base sort comparator.
     */
    private final FieldComparatorSource base;

    /**
     * Creates a new upper case sort comparator.
     *
     * @param base the base sort comparator source.
     */
    public NormalizeSortComparator(FieldComparatorSource base)  {
        this.base = base;
    }

    @Override
    public FieldComparator<?> newComparator(
            String fieldname, int numHits, int sortPos, boolean reversed)
            throws IOException {
        FieldComparator<?> comparator =
                base.newComparator(fieldname, numHits, sortPos, reversed);
        assert comparator instanceof FieldComparatorBase;

        return new FieldComparatorDecorator((FieldComparatorBase) comparator) {
            @Override
            protected Comparable<?> sortValue(int doc) {
                Comparable<?> comparable = super.sortValue(doc);
                if (comparable != null) {
                    char[] input = comparable.toString().toCharArray();

                    // Normalize to ASCII using Lucene's ASCIIFoldingFilter
                    char[] output = new char[input.length * 4]; // worst-case
                    int length = ASCIIFoldingFilter.foldToASCII(
                            input, 0, output, 0, input.length);

                    // Convert to lower case, and check if output is different
                    boolean different = length != input.length;
                    for (int i = 0; i < length; i++) {
                        char c = output[i];
                        if ('A'<= c && c <= 'Z') {
                            output[i] = (char) (c + 'a' - 'A');
                        }
                        if (!different && input[i] != output[i]) {
                            different = true;
                        }
                    }

                    if (different) {
                        comparable = new String(output, 0, length);
                    }
                }
                return comparable;
            }
        };
    }

}
