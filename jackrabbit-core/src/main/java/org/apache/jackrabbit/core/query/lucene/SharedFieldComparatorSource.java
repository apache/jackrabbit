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

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a <code>FieldComparatorSource</code> for <code>FieldComparator</code>s which
 * know how to sort on a lucene field that contains values for multiple properties.
 */
public class SharedFieldComparatorSource extends FieldComparatorSource {

    /**
     * The name of the shared field in the lucene index.
     */
    private final String field;

    /**
     * The item state manager.
     */
    private final ItemStateManager ism;

    /**
     * The hierarchy manager on top of {@link #ism}.
     */
    private final HierarchyManager hmgr;

    /**
     * The index internal namespace mappings.
     */
    private final NamespaceMappings nsMappings;

    /**
     * Create a new <code>SharedFieldComparatorSource</code> for a given shared field.
     *
     * @param fieldname the shared field.
     * @param ism       the item state manager of this workspace.
     * @param hmgr      the hierarchy manager of this workspace.
     * @param nsMappings the index internal namespace mappings.
     */
    public SharedFieldComparatorSource(String fieldname, ItemStateManager ism,
                                       HierarchyManager hmgr, NamespaceMappings nsMappings) {
        this.field = fieldname;
        this.ism = ism;
        this.hmgr = hmgr;
        this.nsMappings = nsMappings;
    }

    /**
     * Create a new <code>FieldComparator</code> for an embedded <code>propertyName</code>
     * and a <code>reader</code>.
     *
     * @param propertyName the relative path to the property to sort on as returned
     *          by {@link org.apache.jackrabbit.spi.Path#getString()}.
     * @return a <code>FieldComparator</code>
     * @throws java.io.IOException if an error occurs
     */
    @Override
    public FieldComparator newComparator(String propertyName, int numHits, int sortPos,
                                         boolean reversed) throws IOException {

        PathFactory factory = PathFactoryImpl.getInstance();
        Path path = factory.create(propertyName);

        try {
            SimpleFieldComparator simple = new SimpleFieldComparator(nsMappings.translatePath(path), field, numHits);

            return path.getLength() == 1
                ? simple
                : new CompoundScoreFieldComparator(
                        new FieldComparator[] { simple, new RelPathFieldComparator(path, numHits) }, numHits);

        }
        catch (IllegalNameException e) {
            throw Util.createIOException(e);
        }
    }

    /**
     * Abstract base class for <code>FieldComparator</code>s which keep their values
     * (<code>Comparable</code>s) in an array.
     */
    private abstract static class AbstractFieldComparator extends FieldComparatorBase {

        /**
         * The values for comparing.
         */
        private final Comparable[] values;

        /**
         * The index readers.
         */

        protected final List<IndexReader> readers = new ArrayList<IndexReader>();
        /**
         * The document number starts for the {@link #readers}.
         */
        protected int[] starts;

        /**
         * Create a new instance with the given number of values.
         *
         * @param numHits  the number of values
         */
        protected AbstractFieldComparator(int numHits) {
            values = new Comparable[numHits];
        }

        /**
         * Returns the reader index for document <code>n</code>.
         *
         * @param n document number.
         * @return the reader index.
         */
        protected final int readerIndex(int n) {
            int lo = 0;
            int hi = readers.size() - 1;

            while (hi >= lo) {
                int mid = (lo + hi) >> 1;
                int midValue = starts[mid];
                if (n < midValue) {
                    hi = mid - 1;
                }
                else if (n > midValue) {
                    lo = mid + 1;
                }
                else {
                    while (mid + 1 < readers.size() && starts[mid + 1] == midValue) {
                        mid++;
                    }
                    return mid;
                }
            }
            return hi;
        }

        /**
         * Add the given value to the values array
         *
         * @param slot   index into values
         * @param value  value for adding
         */
        @Override
        public void setValue(int slot, Comparable value) {
            values[slot] = value;
        }

        /**
         * Return a value from the values array
         *
         * @param slot  index to retrieve
         * @return  the retrieved value
         */
        @Override
        public Comparable getValue(int slot) {
            return values[slot];
        }

        @Override
        public void setNextReader(IndexReader reader, int docBase) throws IOException {
            getIndexReaders(readers, reader);

            int maxDoc = 0;
            starts = new int[readers.size() + 1];

            for (int i = 0; i < readers.size(); i++) {
                IndexReader r = readers.get(i);
                starts[i] = maxDoc;
                maxDoc += r.maxDoc();
            }
            starts[readers.size()] = maxDoc;
        }

        /**
         * Checks if <code>reader</code> is of type {@link MultiIndexReader} and if
         * so calls itself recursively for each reader within the
         * <code>MultiIndexReader</code> or otherwise adds the reader to the list.
         *
         * @param readers  list of index readers.
         * @param reader   reader to decompose
         */
        private static void getIndexReaders(List<IndexReader> readers, IndexReader reader) {
            if (reader instanceof MultiIndexReader) {
                for (IndexReader r : ((MultiIndexReader) reader).getIndexReaders()) {
                    getIndexReaders(readers, r);
                }
            }
            else {
                readers.add(reader);
            }
        }
    }

    /**
     * A <code>FieldComparator</code> which works for order by clauses with properties
     * directly on the result nodes.
     */
    static final class SimpleFieldComparator extends AbstractFieldComparator {

        /**
         * The term look ups of the index segments.
         */
        protected SharedFieldCache.ValueIndex[] indexes;

        /**
         * The name of the property
         */
        private final String propertyName;

        /**
         * The name of the field in the index
         */
        private final String fieldName;

        /**
         * Create a new instance of the <code>FieldComparator</code>.
         *
         * @param propertyName  the name of the property
         * @param fieldName     the name of the field in the index
         * @param numHits       the number of values 
         */
        public SimpleFieldComparator(String propertyName, String fieldName, int numHits) {
            super(numHits);
            this.propertyName = propertyName;
            this.fieldName = fieldName;
        }

        @Override
        public void setNextReader(IndexReader reader, int docBase) throws IOException {
            super.setNextReader(reader, docBase);

            indexes = new SharedFieldCache.ValueIndex[readers.size()];

            String namedValue = FieldNames.createNamedValue(propertyName, "");
            for (int i = 0; i < readers.size(); i++) {
                IndexReader r = readers.get(i);
                indexes[i] = SharedFieldCache.INSTANCE.getValueIndex(r, fieldName, namedValue, this);
            }
        }

        @Override
        protected Comparable sortValue(int doc) {
            int idx = readerIndex(doc);
            return indexes[idx].getValue(doc - starts[idx]);
        }

    }

    /**
     * A <code>FieldComparator</code> which works with order by clauses that use a
     * relative path to a property to sort on.
     */
    private final class RelPathFieldComparator extends AbstractFieldComparator {

        /**
         * Relative path to the property
         */
        private final Path propertyName;

        /**
         * Create a new instance of the <code>FieldComparator</code>.
         *
         * @param propertyName  relative path of the property
         * @param numHits       the number of values
         */
        public RelPathFieldComparator(Path propertyName, int numHits) {
            super(numHits);
            this.propertyName = propertyName;
        }

        @Override
        protected Comparable sortValue(int doc) {
            try {
                int idx = readerIndex(doc);
                IndexReader reader = readers.get(idx);
                Document document = reader.document(doc - starts[idx], FieldSelectors.UUID);
                String uuid = document.get(FieldNames.UUID);
                Path path = hmgr.getPath(new NodeId(uuid));
                PathBuilder builder = new PathBuilder(path);
                builder.addAll(propertyName.getElements());
                PropertyId id = hmgr.resolvePropertyPath(builder.getPath());

                if (id == null) {
                    return null;
                }

                PropertyState state = (PropertyState) ism.getItemState(id);
                if (state == null) {
                    return null;
                }

                InternalValue[] values = state.getValues();
                if (values.length > 0) {
                    return Util.getComparable(values[0]);
                }
            }
            catch (Exception ignore) { }

            return null;
        }

    }

    /**
     * Implements a compound <code>FieldComparator</code> which delegates to several
     * other comparators. The comparators are asked for a sort value in the
     * sequence they are passed to the constructor.
     */
    private static final class CompoundScoreFieldComparator extends AbstractFieldComparator {
        private final FieldComparator[] fieldComparators;

        /**
         * Create a new instance of the <code>FieldComparator</code>.
         *
         * @param fieldComparators  delegatees
         * @param numHits           the number of values
         */
        public CompoundScoreFieldComparator(FieldComparator[] fieldComparators, int numHits) {
            super(numHits);
            this.fieldComparators = fieldComparators;
        }

        @Override
        public Comparable sortValue(int doc) {
            for (FieldComparator fieldComparator : fieldComparators) {
                if (fieldComparator instanceof FieldComparatorBase) {
                    Comparable c = ((FieldComparatorBase) fieldComparator).sortValue(doc);

                    if (c != null) {
                        return c;
                    }
                }
            }
            return null;
        }

        @Override
        public void setNextReader(IndexReader reader, int docBase) throws IOException {
            for (FieldComparator fieldComparator : fieldComparators) {
                fieldComparator.setNextReader(reader, docBase);
            }
        }
    }
    
}
