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

import org.apache.lucene.search.SortComparatorSource;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.index.IndexReader;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;

/**
 * <code>LengthSortComparator</code> implements a sort comparator source that
 * sorts on the length of property values.
 */
public class LengthSortComparator implements SortComparatorSource {

    private static final long serialVersionUID = 2513564768671391632L;

    /**
     * The index internal namespace mappings.
     */
    private final NamespaceMappings nsMappings;

    public LengthSortComparator(NamespaceMappings nsMappings) {
        this.nsMappings = nsMappings;
    }

    /**
     * Creates a new comparator.
     *
     * @param reader    the current index reader.
     * @param fieldname the name of the property to sort on. This is the string
     *                  representation of {@link org.apache.jackrabbit.spi.Name
     *                  Name}.
     * @return the score doc comparator.
     * @throws IOException if an error occurs while reading from the index.
     */
    public ScoreDocComparator newComparator(IndexReader reader,
                                            String fieldname)
            throws IOException {
        NameFactory factory = NameFactoryImpl.getInstance();
        try {
            return new Comparator(reader,
                    nsMappings.translateName(factory.create(fieldname)));
        } catch (IllegalNameException e) {
            throw Util.createIOException(e);
        }
    }

    private final class Comparator extends AbstractScoreDocComparator {

        /**
         * The term look ups of the index segments.
         */
        protected final SharedFieldCache.ValueIndex[] indexes;

        public Comparator(IndexReader reader,
                          String propertyName) throws IOException {
            super(reader);
            this.indexes = new SharedFieldCache.ValueIndex[readers.size()];

            String namedLength = FieldNames.createNamedValue(propertyName, "");
            for (int i = 0; i < readers.size(); i++) {
                IndexReader r = readers.get(i);
                indexes[i] = SharedFieldCache.INSTANCE.getValueIndex(
                        r, FieldNames.PROPERTY_LENGTHS,
                        namedLength, LengthSortComparator.this);
            }
        }

        public Comparable sortValue(ScoreDoc i) {
            int idx = readerIndex(i.doc);
            return indexes[idx].getValue(i.doc - starts[idx]);
        }
    }
}
