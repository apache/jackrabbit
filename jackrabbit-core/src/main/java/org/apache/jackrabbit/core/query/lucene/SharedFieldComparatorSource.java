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

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.query.lucene.sort.AbstractFieldComparator;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;

/**
 * Implements a <code>FieldComparatorSource</code> for <code>FieldComparator</code>s which
 * know how to sort on a lucene field that contains values for multiple properties.
 */
public class SharedFieldComparatorSource extends FieldComparatorSource {

    private static final long serialVersionUID = -5803240954874585429L;

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
                indexes[i] = SharedFieldCache.INSTANCE.getValueIndex(r,
                        fieldName, namedValue);
            }
        }

        @Override
        protected Comparable<?> sortValue(int doc) {
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
        protected Comparable<?> sortValue(int doc) {
            try {
                final String uuid = getUUIDForIndex(doc);
                
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
        public Comparable<?> sortValue(int doc) {
            for (FieldComparator fieldComparator : fieldComparators) {
                if (fieldComparator instanceof FieldComparatorBase) {
                    Comparable<?> c = ((FieldComparatorBase) fieldComparator).sortValue(doc);

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
