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
package org.apache.jackrabbit.core.query.lucene.join;

import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_JOIN_TYPE_LEFT_OUTER;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_JOIN_TYPE_INNER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.query.qom.ChildNodeJoinCondition;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DescendantNodeJoinCondition;
import javax.jcr.query.qom.EquiJoinCondition;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.JoinCondition;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.SameNodeJoinCondition;
import javax.jcr.query.qom.Selector;
import javax.jcr.query.qom.Source;

import org.apache.jackrabbit.commons.iterator.RowIterable;
import org.apache.jackrabbit.commons.iterator.RowIteratorAdapter;
import org.apache.jackrabbit.commons.query.qom.OperandEvaluator;

/**
 * A join merger is used by the {@link QueryEngine} class to efficiently
 * merge together two parts of a join query.
 * <p>
 * Each join condition type ({@link EquiJoinCondition equi-} and
 * {@link SameNodeJoinCondition same}, {@link ChildNodeJoinCondition child}
 * or {@link DescendantJoinCondition descendant} node joins) has it's own
 * merger class that extends the functionality of this abstract base class
 * with functionality specific to that join condition.
 */
abstract class JoinMerger {

    /**
     * Static factory method for creating a merger for the given join.
     *
     * @param join join
     * @param columns columns of the query
     * @param evaluator operand evaluator
     * @param factory QOM factory
     * @return join merger
     * @throws RepositoryException if the merger can not be created
     */
    public static JoinMerger getJoinMerger(
            Join join, Map<String, PropertyValue> columns,
            OperandEvaluator evaluator, QueryObjectModelFactory factory)
            throws RepositoryException {
        JoinCondition condition = join.getJoinCondition();
        if (condition instanceof EquiJoinCondition) {
            return new EquiJoinMerger(
                    join, columns, evaluator, factory,
                    (EquiJoinCondition) condition);
        } else if (condition instanceof SameNodeJoinCondition) {
            return new SameNodeJoinMerger(
                    join, columns, evaluator, factory,
                    (SameNodeJoinCondition) condition);
        } else if (condition instanceof ChildNodeJoinCondition) {
            return new ChildNodeJoinMerger(
                    join, columns, evaluator, factory,
                    (ChildNodeJoinCondition) condition);
        } else if (condition instanceof DescendantNodeJoinCondition) {
            return new DescendantNodeJoinMerger(
                    join, columns, evaluator, factory,
                    (DescendantNodeJoinCondition) condition);
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Unsupported join condition type: " + condition);
        }
    }

    private final String type;

    protected final Set<String> leftSelectors;

    protected final Set<String> rightSelectors;

    private final String[] selectorNames;

    private final Map<String, PropertyValue> columns;

    private final String[] columnNames;

    protected final OperandEvaluator evaluator;

    protected final QueryObjectModelFactory factory;

    protected JoinMerger(
            Join join, Map<String, PropertyValue> columns,
            OperandEvaluator evaluator, QueryObjectModelFactory factory)
            throws RepositoryException {
        this.type = join.getJoinType();

        this.leftSelectors = getSelectorNames(join.getLeft());
        this.rightSelectors = getSelectorNames(join.getRight());

        Set<String> selectors = new LinkedHashSet<String>();
        selectors.addAll(leftSelectors);
        selectors.addAll(rightSelectors);
        this.selectorNames =
            selectors.toArray(new String[selectors.size()]);

        this.columns = columns;
        this.columnNames =
            columns.keySet().toArray(new String[columns.size()]);

        this.evaluator = evaluator;
        this.factory = factory;
    }
    
    public String[] getColumnNames(){
        return columnNames;
    }
    
    public String[] getSelectorNames(){
        return selectorNames;
    }

    public Set<String> getLeftSelectors() {
        return leftSelectors;
    }

    public Set<String> getRightSelectors() {
        return rightSelectors;
    }

    private Set<String> getSelectorNames(Source source)
            throws RepositoryException {
        if (source instanceof Selector) {
            Selector selector = (Selector) source;
            return Collections.singleton(selector.getSelectorName());
        } else if (source instanceof Join) {
            Join join = (Join) source;
            Set<String> set = new LinkedHashSet<String>();
            set.addAll(getSelectorNames(join.getLeft()));
            set.addAll(getSelectorNames(join.getRight()));
            return set;
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Unknown source type: " + source);
        }
    }

    /**
     * Merges the left and right dataset of a join query. Take special
     * precaution for outer joins, as extra checks are needed to distinguish
     * 'null' nodes vs 'not to be included' nodes
     * 
     * 
     * @param leftRows
     *            the left dataset of the join
     * @param rightRows
     *            the right dataset of the join
     * @param excludingOuterJoinRowsSet
     *            if not <code>null</code> must be taken into consideration when
     *            merging OUTER JOINs
     * @param rowComparator
     *            a comparator implementation that has to handle the 'is row
     *            equal to' problem, in the case of outer joins with
     *            excludingOuterJoinRowsSet
     * @return a QueryResult that has the final JOIN resultset
     * @throws RepositoryException
     */
    public QueryResult merge(RowIterator leftRows, RowIterator rightRows,
            Set<Row> excludingOuterJoinRowsSet, Comparator<Row> rowComparator)
            throws RepositoryException {
        Map<String, List<Row>> map = buildRightRowValues(rightRows);

        if (JCR_JOIN_TYPE_INNER.equals(type) && !map.isEmpty()) {
            List<Row> rows = new ArrayList<Row>();
            for (Row leftRow : new RowIterable(leftRows)) {
                for (String value : getLeftValues(leftRow)) {
                    List<Row> matchingRows = map.get(value);
                    if (matchingRows != null) {
                        for (Row rightRow : matchingRows) {
                            rows.add(mergeRow(leftRow, rightRow));
                        }
                    }
                }
            }
            return asQueryResult(new RowIteratorAdapter(rows));
        }

        if (JCR_JOIN_TYPE_LEFT_OUTER.equals(type)) {
            // there are no RIGHT dataset values
            if (map.isEmpty()) {
                // if there are no WHERE conditions, just return everything
                // else return an empty set
                if (excludingOuterJoinRowsSet == null) {
                    return asQueryResult(new RowIteratorAdapter(leftRows) {
                        @Override
                        public Object next() {
                            return mergeRow((Row) super.next(), null);
                        }
                    });
                }
                return asQueryResult(new RowIteratorAdapter(
                        Collections.emptySet()));
            }

            List<Row> rows = new ArrayList<Row>();
            for (Row leftRow : new RowIterable(leftRows)) {
                Set<String> leftValues = getLeftValues(leftRow);
                if(leftValues.isEmpty()){
                    leftValues.add(null);
                }
                for (String value : leftValues) {
                    List<Row> matchingRows = map.get(value);
                    if (matchingRows != null) {
                        for (Row rightRow : matchingRows) {
                            // I have possible WHERE clauses on the join that I
                            // need to look at for each rightRow
                            if (excludingOuterJoinRowsSet == null) {
                                rows.add(mergeRow(leftRow, rightRow));
                            } else {
                                boolean isIncluded = false;
                                // apparently
                                // 'excludingOuterJoinRowsSet.contains' fails to
                                // match rows

                                // TODO can 'rightRow.getNode()' break because
                                // of joins that are bigger than 2 way?
                                // how does this perform for 3 way joins ?
                                for (Row r : excludingOuterJoinRowsSet) {
                                    if(rowComparator.compare(rightRow, r) == 0){
                                        isIncluded = true;
                                        break;
                                    }
                                }
                                if (isIncluded) {
                                    rows.add(mergeRow(leftRow, rightRow));
                                }
                            }
                        }
                    } else {
                        // No matches in an outer join -> add a null row, if
                        // there are no 'WHERE' conditions
                        if (excludingOuterJoinRowsSet == null) {
                            rows.add(mergeRow(leftRow, null));
                        }
                    }
                }
            }
            return asQueryResult(new RowIteratorAdapter(rows));
        }
        return asQueryResult(new RowIteratorAdapter(Collections.emptySet()));
    }

    private QueryResult asQueryResult(RowIterator rowIterator) {
        return new SimpleQueryResult(columnNames, selectorNames, rowIterator);
    }

    private Map<String, List<Row>> buildRightRowValues(RowIterator rightRows)
            throws RepositoryException {
        Map<String, List<Row>> map = new HashMap<String, List<Row>>();
        for (Row row : new RowIterable(rightRows)) {
            for (String value : getRightValues(row)) {
                List<Row> rows = map.get(value);
                if (rows == null) {
                    rows = new ArrayList<Row>();
                    map.put(value, rows);
                }
                rows.add(row);
            }
        }
        return map;
    }

    /**
     * Merges the given left and right rows to a single joined row.
     *
     * @param left left row, possibly <code>null</code> in a right outer join
     * @param right right row, possibly <code>null</code> in a left outer join
     * @return joined row
     */
    private Row mergeRow(Row left, Row right) {
        return new JoinRow(
                columns, evaluator,
                left, leftSelectors, right, rightSelectors);
    }

    public abstract Set<String> getLeftValues(Row row)
            throws RepositoryException;

    public abstract Set<String> getRightValues(Row row)
            throws RepositoryException;

    public abstract List<Constraint> getRightJoinConstraints(Collection<Row> leftRows)
            throws RepositoryException;

}
