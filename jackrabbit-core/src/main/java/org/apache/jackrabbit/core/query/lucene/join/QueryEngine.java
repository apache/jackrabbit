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
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_ORDER_DESCENDING;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.Operand;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Selector;
import javax.jcr.query.qom.Source;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.iterator.RowIteratorAdapter;
import org.apache.jackrabbit.core.query.lucene.LuceneQueryFactory;

public class QueryEngine {

    /**
     * Row comparator.
     */
    private class RowComparator implements Comparator<Row> {

        private final ValueComparator comparator = new ValueComparator();

        private final Ordering[] orderings;

        private RowComparator(Ordering[] orderings) {
            this.orderings = orderings;
        }

        public int compare(Row a, Row b) {
            try {
                for (Ordering ordering : orderings) {
                    Operand operand = ordering.getOperand();
                    Value[] va = evaluator.getValues(operand, a);
                    Value[] vb = evaluator.getValues(operand, b);
                    int d = compare(va, vb);
                    if (d != 0) {
                        if (JCR_ORDER_DESCENDING.equals(ordering.getOrder())) {
                            return -d;
                        } else {
                            return d;
                        }
                    }
                }
                return 0;
            } catch (RepositoryException e) {
                throw new RuntimeException(
                        "Unable to compare rows " + a + " and " + b, e);
            }
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

    private final LuceneQueryFactory lqf;

    private final NodeTypeManager ntManager;

    private final QueryObjectModelFactory qomFactory;

    private final ValueFactory valueFactory;

    private final OperandEvaluator evaluator;

    public QueryEngine(
            Session session, LuceneQueryFactory lqf,
            Map<String, Value> variables) throws RepositoryException {
        this.lqf = lqf;

        Workspace workspace = session.getWorkspace();
        this.ntManager = workspace.getNodeTypeManager();
        this.qomFactory = workspace.getQueryManager().getQOMFactory();
        this.valueFactory = session.getValueFactory();

        this.evaluator = new OperandEvaluator(valueFactory, variables);
    }

    public QueryResult execute(
            Column[] columns, Source source, Constraint constraint,
            Ordering[] orderings, long offset, long limit)
            throws RepositoryException {
        if (source instanceof Selector) {
            Selector selector = (Selector) source;
            return execute(
                    columns, selector, constraint, orderings, offset, limit);
        } else if (source instanceof Join) {
            Join join = (Join) source;
            if (join.getJoinType() == JCR_JOIN_TYPE_RIGHT_OUTER) {
                // Swap the join sources to normalize all outer joins to left
                join = qomFactory.join(
                        join.getRight(), join.getLeft(),
                        JCR_JOIN_TYPE_LEFT_OUTER, join.getJoinCondition());
            }
            return execute(
                    columns, join, constraint, orderings, offset, limit);
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Unknown source type: " + source);
        }
    }

    protected QueryResult execute(
            Column[] columns, Join join, Constraint constraint,
            Ordering[] orderings, long offset, long limit)
            throws RepositoryException {
        JoinMerger merger = JoinMerger.getJoinMerger(
                join, getColumnMap(columns, getSelectorNames(join)),
                evaluator, qomFactory);
        ConstraintSplitter splitter = new ConstraintSplitter(
                constraint, qomFactory,
                merger.getLeftSelectors(), merger.getRightSelectors());

        Source left = join.getLeft();
        Constraint leftConstraint = splitter.getLeftConstraint();
        QueryResult leftResult =
            execute(null, left, leftConstraint, null, 0, -1);
        List<Row> leftRows = new ArrayList<Row>();
        for (Row row : JcrUtils.getRows(leftResult)) {
            leftRows.add(row);
        }

        RowIterator rightRows;
        Source right = join.getRight();
        List<Constraint> rightConstraints =
            merger.getRightJoinConstraints(leftRows);
        if (rightConstraints.size() < 500) {
            Constraint rightConstraint = Constraints.and(
                    qomFactory,
                    Constraints.or(qomFactory, rightConstraints),
                    splitter.getRightConstraint());
            rightRows =
                execute(null, right, rightConstraint, null, 0, -1).getRows();
        } else {
            List<Row> list = new ArrayList<Row>();
            for (int i = 0; i < rightConstraints.size(); i += 500) {
                Constraint rightConstraint = Constraints.and(
                        qomFactory,
                        Constraints.or(qomFactory, rightConstraints.subList(
                                i, Math.min(i + 500, rightConstraints.size()))),
                        splitter.getRightConstraint());
                QueryResult rigthResult =
                    execute(null, right, rightConstraint, null, 0, -1);
                for (Row row : JcrUtils.getRows(rigthResult)) {
                    list.add(row);
                }
            }
            rightRows = new RowIteratorAdapter(list);
        }

        QueryResult result =
            merger.merge(new RowIteratorAdapter(leftRows), rightRows);
        return sort(result, orderings, offset, limit);
    }

    protected QueryResult execute(
            Column[] columns, Selector selector, Constraint constraint,
            Ordering[] orderings, long offset, long limit)
            throws RepositoryException {
        Map<String, NodeType> selectorMap = getSelectorNames(selector);
        String[] selectorNames =
            selectorMap.keySet().toArray(new String[selectorMap.size()]);

        Map<String, PropertyValue> columnMap =
            getColumnMap(columns, selectorMap);
        String[] columnNames =
            columnMap.keySet().toArray(new String[columnMap.size()]);

        try {
            RowIterator rows = new RowIteratorAdapter(lqf.execute(
                    columnMap, selector, constraint));
            QueryResult result =
                new SimpleQueryResult(columnNames, selectorNames, rows);
            return sort(result, orderings, offset, limit);
        } catch (IOException e) {
            throw new RepositoryException(
                    "Failed to access the query index", e);
        }
    }

    private Map<String, PropertyValue> getColumnMap(
            Column[] columns, Map<String, NodeType> selectors)
            throws RepositoryException {
        Map<String, PropertyValue> map =
            new LinkedHashMap<String, PropertyValue>();
        if (columns != null && columns.length > 0) {
            for (int i = 0; i < columns.length; i++) {
                String name = columns[i].getColumnName();
                if (name != null) {
                    map.put(name, qomFactory.propertyValue(
                            columns[i].getSelectorName(),
                            columns[i].getPropertyName()));
                } else {
                    String selector = columns[i].getSelectorName();
                    map.putAll(getColumnMap(selector, selectors.get(selector)));
                }
            }
        } else {
            for (Map.Entry<String, NodeType> selector : selectors.entrySet()) {
                map.putAll(getColumnMap(
                        selector.getKey(), selector.getValue()));
            }
        }
        return map;
    }

    private Map<String, PropertyValue> getColumnMap(
            String selector, NodeType type) throws RepositoryException {
        Map<String, PropertyValue> map =
            new LinkedHashMap<String, PropertyValue>();
        for (PropertyDefinition definition : type.getPropertyDefinitions()) {
            String name = definition.getName();
            if (!definition.isMultiple() && !"*".equals(name)) {
                // TODO: Add proper quoting
                map.put(selector + "." + name,
                        qomFactory.propertyValue(selector, name));
            }
        }
        return map;
    }

    private Map<String, NodeType> getSelectorNames(Source source)
            throws RepositoryException {
        if (source instanceof Selector) {
            Selector selector = (Selector) source;
            return Collections.singletonMap(
                    selector.getSelectorName(), getNodeType(selector));
        } else if (source instanceof Join) {
            Join join = (Join) source;
            Map<String, NodeType> map = new LinkedHashMap<String, NodeType>();
            map.putAll(getSelectorNames(join.getLeft()));
            map.putAll(getSelectorNames(join.getRight()));
            return map;
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Unknown source type: " + source);
        }
    }

    private NodeType getNodeType(Selector selector) throws RepositoryException {
        try {
            return ntManager.getNodeType(selector.getNodeTypeName());
        } catch (NoSuchNodeTypeException e) {
            throw new InvalidQueryException(
                    "Selected node type does not exist: " + selector, e);
        }
    }

    /**
     * Sorts the given query results according to the given QOM orderings.
     * If one or more orderings have been specified, this method will iterate
     * through the entire original result set, order the collected rows, and
     * return a new result set based on the sorted collection of rows.
     *
     * @param result original query results
     * @param orderings QOM orderings
     * @param offset result offset
     * @param limit result limit
     * @return sorted query results
     * @throws RepositoryException if the results can not be sorted
     */
    public QueryResult sort(
            QueryResult result, final Ordering[] orderings,
            long offset, long limit) throws RepositoryException {
        if ((orderings != null && orderings.length > 0)
                || offset != 0 || limit >= 0) {
            List<Row> rows = new ArrayList<Row>();

            RowIterator iterator = result.getRows();
            while (iterator.hasNext()) {
                rows.add(iterator.nextRow());
            }

            if (orderings != null && orderings.length > 0) {
                Collections.sort(rows, new RowComparator(orderings));
            }

            if (offset > 0) {
                int size = rows.size();
                rows = rows.subList((int) Math.min(offset, size), size);
            }
            if (limit >= 0) {
                int size = rows.size();
                rows = rows.subList(0, (int) Math.min(limit, size));
            }

            return new SimpleQueryResult(
                    result.getColumnNames(), result.getSelectorNames(),
                    new RowIteratorAdapter(rows));
        } else {
            return result;
        }
    }

}
