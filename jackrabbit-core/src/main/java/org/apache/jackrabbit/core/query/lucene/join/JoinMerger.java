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
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_JOIN_TYPE_RIGHT_OUTER;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
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

abstract class JoinMerger {

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
                    join, null, evaluator, factory,
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

    private final String[] columnNames;

    private final PropertyValue[] operands;

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

        this.columnNames =
            columns.keySet().toArray(new String[columns.size()]);
        this.operands =
            columns.values().toArray(new PropertyValue[columns.size()]);

        this.evaluator = evaluator;
        this.factory = factory;
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

    public QueryResult merge(
            RowIterator leftRows, RowIterator rightRows)
            throws RepositoryException {
        if (JCR_JOIN_TYPE_RIGHT_OUTER.equals(type)) {
            Map<String, List<Row>> map = new HashMap<String, List<Row>>();
            for (Row row : new RowIterable(leftRows)) {
                for (String value : getLeftValues(row)) {
                    List<Row> rows = map.get(value);
                    if (rows == null) {
                        rows = new ArrayList<Row>();
                        map.put(value, rows);
                    }
                    rows.add(row);
                }
            }
            return mergeRight(map, rightRows);
        } else {
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
            boolean outer = JCR_JOIN_TYPE_LEFT_OUTER.equals(type);
            return mergeLeft(leftRows, map, outer);
        }
    }

    private QueryResult mergeLeft(
            RowIterator leftRows, Map<String, List<Row>> rightRowMap,
            boolean outer) throws RepositoryException {
        System.out.println("Available right matches " + rightRowMap.keySet());
        List<Row> rows = new ArrayList<Row>();
        for (Row leftRow : new RowIterable(leftRows)) {
            System.out.println("Finding matchers for left row " + leftRow);
            for (String value : getLeftValues(leftRow)) {
                System.out.println(" - checking for matches for " + value);
                List<Row> rightRows = rightRowMap.get(value);
                if (rightRows != null) {
                    for (Row rightRow : rightRows) {
                        System.out.println(" -> found matching row " + rightRow);
                        rows.add(mergeRow(leftRow, rightRow));
                    }
                } else if (outer) {
                    rows.add(mergeRow(leftRow, null));
                }
            }
        }
        return new SimpleQueryResult(
                columnNames, selectorNames, new RowIteratorAdapter(rows));
    }

    private QueryResult mergeRight(
            Map<String, List<Row>> leftRowMap, RowIterator rightRows)
            throws RepositoryException {
        List<Row> rows = new ArrayList<Row>();
        for (Row rightRow : new RowIterable(rightRows)) {
            for (String value : getRightValues(rightRow)) {
                List<Row> leftRows = leftRowMap.get(value);
                if (leftRows != null) {
                    for (Row leftRow : leftRows) {
                        rows.add(mergeRow(leftRow, rightRow));
                    }
                } else {
                    rows.add(mergeRow(null, rightRow));
                }
            }
        }
        return new SimpleQueryResult(
                columnNames, selectorNames, new RowIteratorAdapter(rows));
    }

    /**
     * Merges the given left and right rows to a single joined row.
     *
     * @param left left row, possibly <code>null</code> in a right outer join
     * @param right right row, possibly <code>null</code> in a left outer join
     * @return joined row
     * @throws RepositoryException if the rows can't be joined
     */
    private Row mergeRow(Row left, Row right) throws RepositoryException {
        Node[] nodes = new Node[selectorNames.length];
        double[] scores = new double[selectorNames.length];
        for (int i = 0; i < selectorNames.length; i++) {
            String selector = selectorNames[i];
            if (left != null && leftSelectors.contains(selector)) {
                nodes[i] = left.getNode(selector);
                scores[i] = left.getScore(selector);
            } else if (right != null && rightSelectors.contains(selector)) {
                nodes[i] = right.getNode(selector);
                scores[i] = right.getScore(selector);
            } else {
                nodes[i] = null;
                scores[i] = 0.0;
            }
        }

        Value[] values = new Value[operands.length];
        Row row = new SimpleRow(
                columnNames, values, selectorNames, nodes, scores);
        for (int i = 0; i < operands.length; i++) {
            values[i] = evaluator.getValue(operands[i], row);
        }

        return row;
    }

    public abstract Set<String> getLeftValues(Row row)
            throws RepositoryException;

    public abstract Set<String> getRightValues(Row row)
            throws RepositoryException;

    public abstract Constraint getRightJoinConstraint(List<Row> leftRows)
            throws RepositoryException;

}
