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

import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_ORDER_DESCENDING;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RangeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.query.qom.And;
import javax.jcr.query.qom.ChildNode;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Comparison;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.Literal;
import javax.jcr.query.qom.Not;
import javax.jcr.query.qom.Operand;
import javax.jcr.query.qom.Or;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Selector;
import javax.jcr.query.qom.Source;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.iterator.RangeIteratorAdapter;
import org.apache.jackrabbit.commons.iterator.RowIteratorAdapter;

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

    private final Session session;

    private final NodeTypeManager ntManager;

    private final QueryObjectModelFactory qomFactory;

    private final ValueFactory valueFactory;

    private final OperandEvaluator evaluator;

    public QueryEngine(Session session, Map<String, Value> variables)
            throws RepositoryException {
        this.session = session;

        Workspace workspace = session.getWorkspace();
        this.ntManager = workspace.getNodeTypeManager();
        this.qomFactory = workspace.getQueryManager().getQOMFactory();
        this.valueFactory = session.getValueFactory();

        this.evaluator = new OperandEvaluator(valueFactory, variables);
    }

    public QueryEngine(Session session) throws RepositoryException {
        this(session, new HashMap<String, Value>());
    }

    public QueryResult execute(
            Column[] columns, Source source,
            Constraint constraint, Ordering[] orderings)
            throws RepositoryException {
        if (source instanceof Selector) {
            Selector selector = (Selector) source;
            return execute(columns, selector, constraint, orderings);
        } else if (source instanceof Join) {
            Join join = (Join) source;
            return execute(columns, join, constraint, orderings);
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Unknown source type: " + source);
        }
    }

    protected QueryResult execute(
            Column[] columns, Join join,
            Constraint constraint, Ordering[] orderings)
            throws RepositoryException {
        JoinMerger merger = JoinMerger.getJoinMerger(
                join, getColumnMap(columns, getSelectorNames(join)),
                evaluator, qomFactory);
        ConstraintSplitter splitter = new ConstraintSplitter(
                constraint, qomFactory,
                merger.getLeftSelectors(), merger.getRightSelectors());

        Source left = join.getLeft();
        Constraint leftConstraint = splitter.getLeftConstraint();
        System.out.println("FROM " + left + " WHERE " + leftConstraint);
        QueryResult leftResult = execute(null, left, leftConstraint, null);
        List<Row> leftRows = new ArrayList<Row>();
        for (Row row : JcrUtils.getRows(leftResult)) {
            System.out.println(row);
            leftRows.add(row);
        }

        Source right = join.getRight();
        Constraint rightConstraint = Constraints.and(
                qomFactory,
                merger.getRightJoinConstraint(leftRows),
                splitter.getRightConstraint());
        System.out.println("FROM " + right + " WHERE " + rightConstraint);
        QueryResult rightResult = execute(null, right, rightConstraint, null);

        return merger.merge(
                new RowIteratorAdapter(leftRows), rightResult.getRows());
    }

    private Set<String> getPaths(
            String selectorName, String relativePath, List<Row> rows)
            throws RepositoryException {
        Set<String> paths = new HashSet<String>();
        for (Row row : rows) {
            try {
                Node node = row.getNode(selectorName);
                if (relativePath != null) {
                    node = node.getNode(relativePath);
                }
                paths.add(node.getPath());
            } catch (PathNotFoundException e) {
                // Node at relative path not found, skip
            }
        }
        return paths;
    }

    private String toSqlConstraint(Constraint constraint)
            throws RepositoryException {
        if (constraint instanceof And) {
            And and = (And) constraint;
            String c1 = toSqlConstraint(and.getConstraint1());
            String c2 = toSqlConstraint(and.getConstraint2());
            return "(" + c1 + ") AND (" + c2 + ")";
        } else if (constraint instanceof Or) {
            Or or = (Or) constraint;
            String c1 = toSqlConstraint(or.getConstraint1());
            String c2 = toSqlConstraint(or.getConstraint2());
            return "(" + c1 + ") OR (" + c2 + ")";
        } else if (constraint instanceof Not) {
            Not or = (Not) constraint;
            return "NOT (" + toSqlConstraint(or.getConstraint()) + ")";
        } else if (constraint instanceof Comparison) {
            Comparison c = (Comparison) constraint;
            String left = toSqlOperand(c.getOperand1());
            String right = toSqlOperand(c.getOperand2());
            if (c.getOperator().equals(JCR_OPERATOR_EQUAL_TO)) {
                return left + " = " + right;
            } else if (c.getOperator().equals(JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO)) {
                return left + " >= " + right;
            } else {
                throw new RepositoryException("Unsupported comparison: " + c);
            }
        } else if (constraint instanceof ChildNode) {
            ChildNode cn = (ChildNode) constraint;
            return "jcr:path LIKE '" + cn.getParentPath() + "/%'";
        } else  {
            throw new RepositoryException("Unsupported constraint: " + constraint);
        }
    }

    private String toSqlOperand(Operand operand) throws RepositoryException {
        if (operand instanceof PropertyValue) {
            PropertyValue pv = (PropertyValue) operand;
            return pv.getPropertyName();
        } else if (operand instanceof Literal) {
            Literal literal = (Literal) operand;
            Value value = literal.getLiteralValue();
            int type = value.getType();
            if (type == PropertyType.LONG || type == PropertyType.DOUBLE) {
                return value.getString();
            } else if (type == PropertyType.DATE) {
                return "TIMESTAMP '" + value.getString() + "'";
            } else {
                return "'" + value.getString() + "'";
            }
        } else {
            throw new RepositoryException("Uknown operand type: " + operand);
        }
    }

    protected QueryResult execute(
            Column[] columns, Selector selector,
            Constraint constraint, Ordering[] orderings)
            throws RepositoryException {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT * FROM ");
        builder.append(selector.getNodeTypeName());
        if (constraint != null) {
            builder.append(" WHERE ");
            builder.append(toSqlConstraint(constraint));
        }
        System.out.println(builder.toString());

        QueryManager manager = session.getWorkspace().getQueryManager();
        Query query = manager.createQuery(builder.toString(), Query.SQL);

        Map<String, NodeType> selectorMap = getSelectorNames(selector);
        final String[] selectorNames =
            selectorMap.keySet().toArray(new String[selectorMap.size()]);

        final Map<String, PropertyValue> columnMap =
            getColumnMap(columns, selectorMap);
        final String[] columnNames =
            columnMap.keySet().toArray(new String[columnMap.size()]);

        final String selectorName = selector.getSelectorName();
        RangeIterator rows = new RangeIteratorAdapter(query.execute().getNodes()) {
            @Override
            public Object next() {
                Node node = (Node) super.next();
                return new SelectorRow(
                        columnMap, evaluator, selectorName, node, 1.0);
            }
        };

        QueryResult result = new SimpleQueryResult(
                columnNames, selectorNames, new RowIteratorAdapter(rows));
        return sort(result, orderings);
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
                    selector.getSelectorName(),
                    ntManager.getNodeType(selector.getNodeTypeName()));
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

    /**
     * Sorts the given query results according to the given QOM orderings.
     * If one or more orderings have been specified, this method will iterate
     * through the entire original result set, order the collected rows, and
     * return a new result set based on the sorted collection of rows.
     *
     * @param result original query results
     * @param orderings QOM orderings
     * @return sorted query results
     * @throws RepositoryException if the results can not be sorted
     */
    public QueryResult sort(QueryResult result, final Ordering[] orderings)
            throws RepositoryException {
        if (orderings != null && orderings.length > 0) {
            List<Row> rows = new ArrayList<Row>();

            RowIterator iterator = result.getRows();
            while (iterator.hasNext()) {
                rows.add(iterator.nextRow());
            }

            Collections.sort(rows, new RowComparator(orderings));

            return new SimpleQueryResult(
                    result.getColumnNames(), result.getSelectorNames(),
                    new RowIteratorAdapter(rows));
        } else {
            return result;
        }
    }

}
