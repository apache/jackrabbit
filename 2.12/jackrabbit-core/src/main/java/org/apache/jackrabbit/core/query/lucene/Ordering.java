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

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.query.qom.DefaultTraversingQOMTreeVisitor;
import org.apache.jackrabbit.spi.commons.query.qom.DynamicOperandImpl;
import org.apache.jackrabbit.spi.commons.query.qom.FullTextSearchScoreImpl;
import org.apache.jackrabbit.spi.commons.query.qom.LengthImpl;
import org.apache.jackrabbit.spi.commons.query.qom.LowerCaseImpl;
import org.apache.jackrabbit.spi.commons.query.qom.NodeLocalNameImpl;
import org.apache.jackrabbit.spi.commons.query.qom.NodeNameImpl;
import org.apache.jackrabbit.spi.commons.query.qom.OrderingImpl;
import org.apache.jackrabbit.spi.commons.query.qom.PropertyValueImpl;
import org.apache.jackrabbit.spi.commons.query.qom.QOMTreeVisitor;
import org.apache.jackrabbit.spi.commons.query.qom.UpperCaseImpl;
import org.apache.lucene.search.SortField;

import javax.jcr.RepositoryException;

/**
 * <code>Ordering</code> implements a single ordering specification.
 */
public class Ordering {

    /**
     * The selector name where this ordering applies to.
     */
    private final Name selectorName;

    /**
     * The lucene sort field for this ordering.
     */
    private final SortField sort;

    /**
     * Private constructor.
     *
     * @param selectorName the selector name for this ordering.
     * @param sort         the lucene sort field for this ordering.
     */
    private Ordering(Name selectorName, SortField sort) {
        this.selectorName = selectorName;
        this.sort = sort;
    }

    /**
     * @return the selector name where this ordering applies to.
     */
    public Name getSelectorName() {
        return selectorName;
    }

    /**
     * @return the lucene sort field for this ordering.
     */
    public SortField getSortField() {
        return sort;
    }

    /**
     * Creates an ordering from a JCR QOM ordering.
     *
     * @param ordering   the JCR QOM ordering specification.
     * @param scs        the sort comparator source from the search index.
     * @param nsMappings the index internal namespace mappings.
     * @return an ordering.
     * @throws RepositoryException if an error occurs while translating the JCR
     *                             QOM ordering.
     */
    public static Ordering fromQOM(final OrderingImpl ordering,
                                    final SharedFieldComparatorSource scs,
                                    final NamespaceMappings nsMappings)
            throws RepositoryException {
        final Name[] selectorName = new Name[1];
        QOMTreeVisitor visitor = new DefaultTraversingQOMTreeVisitor() {

            public Object visit(LengthImpl node, Object data) throws Exception {
                PropertyValueImpl propValue = (PropertyValueImpl) node.getPropertyValue();
                selectorName[0] = propValue.getSelectorQName();
                return new SortField(propValue.getPropertyQName().toString(),
                        new LengthSortComparator(nsMappings),
                        !ordering.isAscending());
            }

            public Object visit(LowerCaseImpl node, Object data)
                    throws Exception {
                SortField sf = (SortField) ((DynamicOperandImpl) node.getOperand()).accept(this, data);
                selectorName[0] = node.getSelectorQName();
                return new SortField(sf.getField(),
                        new LowerCaseSortComparator(sf.getComparatorSource()),
                        !ordering.isAscending());
            }

            public Object visit(UpperCaseImpl node, Object data)
                    throws Exception {
                SortField sf = (SortField) ((DynamicOperandImpl) node.getOperand()).accept(this, data);
                selectorName[0] = node.getSelectorQName();
                return new SortField(sf.getField(),
                        new UpperCaseSortComparator(sf.getComparatorSource()),
                        !ordering.isAscending());
            }

            public Object visit(FullTextSearchScoreImpl node, Object data)
                    throws Exception {
                selectorName[0] = node.getSelectorQName();
                return new SortField(null, SortField.SCORE,
                        !ordering.isAscending());
            }

            public Object visit(NodeLocalNameImpl node, Object data) throws Exception {
                selectorName[0] = node.getSelectorQName();
                return new SortField(FieldNames.LOCAL_NAME,
                       SortField.STRING, !ordering.isAscending());
            }

            public Object visit(NodeNameImpl node, Object data) throws Exception {
                selectorName[0] = node.getSelectorQName();
                return new SortField(FieldNames.LABEL,
                       SortField.STRING, !ordering.isAscending());
            }

            public Object visit(PropertyValueImpl node, Object data)
                    throws Exception {
                selectorName[0] = node.getSelectorQName();
                return new SortField(node.getPropertyQName().toString(),
                        scs, !ordering.isAscending());
            }

            public Object visit(OrderingImpl node, Object data)
                    throws Exception {
                return ((DynamicOperandImpl) node.getOperand()).accept(this, data);
            }
        };
        try {
            SortField field = (SortField) ordering.accept(visitor, null);
            return new Ordering(selectorName[0], field);
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }
}
