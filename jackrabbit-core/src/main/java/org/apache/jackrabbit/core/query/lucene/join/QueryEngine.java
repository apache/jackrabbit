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

import static java.util.Locale.ENGLISH;
import static javax.jcr.PropertyType.NAME;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_ORDER_DESCENDING;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
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
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.query.qom.And;
import javax.jcr.query.qom.BindVariableValue;
import javax.jcr.query.qom.ChildNode;
import javax.jcr.query.qom.ChildNodeJoinCondition;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Comparison;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DescendantNode;
import javax.jcr.query.qom.DescendantNodeJoinCondition;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.EquiJoinCondition;
import javax.jcr.query.qom.FullTextSearchScore;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.JoinCondition;
import javax.jcr.query.qom.Length;
import javax.jcr.query.qom.Literal;
import javax.jcr.query.qom.LowerCase;
import javax.jcr.query.qom.NodeLocalName;
import javax.jcr.query.qom.NodeName;
import javax.jcr.query.qom.Not;
import javax.jcr.query.qom.Operand;
import javax.jcr.query.qom.Or;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.PropertyExistence;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.SameNode;
import javax.jcr.query.qom.SameNodeJoinCondition;
import javax.jcr.query.qom.Selector;
import javax.jcr.query.qom.Source;
import javax.jcr.query.qom.UpperCase;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.flat.TreeTraverser;
import org.apache.jackrabbit.commons.iterator.FilteredRangeIterator;
import org.apache.jackrabbit.commons.iterator.RangeIteratorAdapter;
import org.apache.jackrabbit.commons.iterator.RowIteratorAdapter;
import org.apache.jackrabbit.commons.predicate.Predicate;
import org.apache.jackrabbit.commons.predicate.Predicates;
import org.apache.jackrabbit.commons.predicate.RowPredicate;

public class QueryEngine {

    private final Session session;

    private final NodeTypeManager ntManager;

    private final QueryObjectModelFactory qomFactory;

    private final ValueFactory valueFactory;

    private final Map<String, Value> variables;

    public QueryEngine(Session session, Map<String, Value> variables)
            throws RepositoryException {
        this.session = session;

        Workspace workspace = session.getWorkspace();
        this.ntManager = workspace.getNodeTypeManager();
        this.qomFactory = workspace.getQueryManager().getQOMFactory();
        this.valueFactory = session.getValueFactory();

        this.variables = variables;
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
        Source left = join.getLeft();
        Map<String, NodeType> leftSelectors = getSelectorNames(left);

        Source right = join.getRight();
        Map<String, NodeType> rightSelectors = getSelectorNames(right);

        Constraint leftConstraint =
            mapConstraintToSelectors(constraint, leftSelectors.keySet());
        System.out.println("FROM " + left + " WHERE " + leftConstraint);
        QueryResult leftResult = execute(null, left, leftConstraint, null);

        List<Row> leftRows = new ArrayList<Row>();
        for (Row row : JcrUtils.getRows(leftResult)) {
            System.out.println(row);
            leftRows.add(row);
        }

        Constraint joinConstraint = getJoinConstraint(
                join.getJoinCondition(), leftSelectors.keySet(), leftRows);
        Constraint rightConstraint = Constraints.and(
                qomFactory, joinConstraint,
                mapConstraintToSelectors(constraint, rightSelectors.keySet()));
        System.out.println("FROM " + right + " WHERE " + rightConstraint);
        QueryResult rightResult = execute(null, right, rightConstraint, null);

        List<Row> rightRows = new ArrayList<Row>();
        for (Row row : JcrUtils.getRows(rightResult)) {
            System.out.println(row);
            rightRows.add(row);
        }

        Map<String, NodeType> selectors = new LinkedHashMap<String, NodeType>();
        selectors.putAll(leftSelectors);
        selectors.putAll(rightSelectors);
        String[] selectorNames =
            selectors.keySet().toArray(new String[selectors.size()]);

        Map<String, PropertyValue> columnMap = getColumnMap(columns, selectors);
        String[] columnNames =
            columnMap.keySet().toArray(new String[columnMap.size()]);
        PropertyValue[] operands =
            columnMap.values().toArray(new PropertyValue[columnMap.size()]);

        double[] scores = new double[selectorNames.length];
        for (int i = 0; i < scores.length; i++) {
            scores[i] = 1.0;
        }

        Predicate predicate = Predicates.and(
                getPredicate(join.getJoinCondition()),
                getPredicate(constraint));

        List<Row> joinRows = new ArrayList<Row>();
        for (Row leftRow : leftRows) {
            for (Row rightRow : rightRows) {
                Node[] nodes = new Node[selectorNames.length];
                for (int i = 0; i < selectorNames.length; i++) {
                    String selector = selectorNames[i];
                    if (leftSelectors.containsKey(selector)) {
                        nodes[i] = leftRow.getNode(selector);
                    } else {
                        nodes[i] = rightRow.getNode(selector);
                    }
                }
                Value[] values = new Value[operands.length];
                Row row = new SimpleRow(
                        columnNames, values, selectorNames, nodes, scores);
                for (int i = 0; i < operands.length; i++) {
                    values[i] = combine(getPropertyValues(operands[i], row));
                }
                if (predicate.evaluate(row)) {
                    joinRows.add(row);
                }
            }
        }

        return new SimpleQueryResult(
                columnNames, selectorNames, new RowIteratorAdapter(joinRows));
    }

    /**
     * Returns a mapped constraint that only refers to the given set of
     * selectors. The returned constraint is guaranteed to match an as small
     * as possible superset of the node tuples matched by the given original
     * constraints.
     *
     * @param constraint original constraint
     * @param selectors target selectors
     * @return mapped constraint
     * @throws RepositoryException if the constraint mapping fails
     */
    private Constraint mapConstraintToSelectors(
            Constraint constraint, Set<String> selectors)
            throws RepositoryException {
        if (constraint == Constraints.TRUE || constraint == Constraints.FALSE) {
            return constraint;
        } else if (constraint instanceof And) {
            And and = (And) constraint;
            return Constraints.and(
                    qomFactory,
                    mapConstraintToSelectors(and.getConstraint1(), selectors),
                    mapConstraintToSelectors(and.getConstraint2(), selectors));
        } else if (constraint instanceof Or) {
            Or or = (Or) constraint;
            return Constraints.or(
                    qomFactory,
                    mapConstraintToSelectors(or.getConstraint1(), selectors),
                    mapConstraintToSelectors(or.getConstraint2(), selectors));
        } else if (constraint instanceof Not) {
            Not not = (Not) constraint;
            Constraint mapped =
                mapConstraintToSelectors(not.getConstraint(), selectors);
            // Tricky handling of NOT constraints, even NOT TRUE maps to TRUE
            // to guarantee that the mapped result set remains a superset
            if (mapped == Constraints.TRUE || mapped == Constraints.FALSE) {
                return Constraints.TRUE;
            } else {
                return Constraints.not(qomFactory, mapped);
            }
        } else if (selectors.contains(getSelectorName(constraint))) {
            // This constraint refers to one of the target selectors, keep it
            return constraint;
        } else {
            // This constraint refers to some other selector, drop it
            return Constraints.TRUE;
        }
    }

    /**
     * Returns the name of the selector referenced by the given constraint.
     *
     * @param constraint concrete constraint (i.e. not a logical construct)
     * @return selector name
     * @throws UnsupportedRepositoryOperationException
     *         if the constraint type is unknown
     */
    private String getSelectorName(Constraint constraint)
            throws UnsupportedRepositoryOperationException {
        if (constraint instanceof PropertyExistence) {
            PropertyExistence pe = (PropertyExistence) constraint;
            return pe.getSelectorName();
        } else if (constraint instanceof Comparison) {
            Comparison c = (Comparison) constraint;
            return getSelectorName(c.getOperand1());
        } else if (constraint instanceof SameNode) {
            SameNode sn = (SameNode) constraint;
            return sn.getSelectorName();
        } else if (constraint instanceof ChildNode) {
            ChildNode cn = (ChildNode) constraint;
            return cn.getSelectorName();
        } else if (constraint instanceof DescendantNode) {
            DescendantNode dn = (DescendantNode) constraint;
            return dn.getSelectorName();
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Unknown constraint type: " + constraint);
        }
    }

    /**
     * Returns the selector name referenced by the given dynamic operand.
     *
     * @param operand dynamic operand
     * @return selector name
     * @throws UnsupportedRepositoryOperationException
     *         if the operand type is unknown
     */
    private String getSelectorName(DynamicOperand operand)
            throws UnsupportedRepositoryOperationException {
        if (operand instanceof FullTextSearchScore) {
            FullTextSearchScore ftss = (FullTextSearchScore) operand;
            return ftss.getSelectorName();
        } else if (operand instanceof Length) {
            Length length = (Length) operand;
            return getSelectorName(length.getPropertyValue());
        } else if (operand instanceof LowerCase) {
            LowerCase lower = (LowerCase) operand;
            return getSelectorName(lower.getOperand());
        } else if (operand instanceof NodeLocalName) {
            NodeLocalName local = (NodeLocalName) operand;
            return local.getSelectorName();
        } else if (operand instanceof NodeName) {
            NodeName name = (NodeName) operand;
            return name.getSelectorName();
        } else if (operand instanceof PropertyValue) {
            PropertyValue value = (PropertyValue) operand;
            return value.getSelectorName();
        } else if (operand instanceof UpperCase) {
            UpperCase upper = (UpperCase) operand;
            return getSelectorName(upper.getOperand());
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Unknown dynamic operand type: " + operand);
        }
    }

    private Predicate getPredicate(JoinCondition condition)
            throws RepositoryException {
        if (condition instanceof EquiJoinCondition) {
            return getEquiJoinPredicate((EquiJoinCondition) condition);
        } else if (condition instanceof SameNodeJoinCondition) {
            return Predicate.TRUE; // FIXME
        } else if (condition instanceof ChildNodeJoinCondition) {
            return Predicate.TRUE; // FIXME
        } else if (condition instanceof DescendantNodeJoinCondition) {
            return Predicate.TRUE; // FIXME
        } else {
            return Predicate.TRUE; // FIXME
        }
    }

    private Predicate getEquiJoinPredicate(final EquiJoinCondition condition)
            throws RepositoryException {
        final Operand operand1 = qomFactory.propertyValue(
                condition.getSelector1Name(),
                condition.getProperty1Name());
        final Operand operand2 = qomFactory.propertyValue(
                condition.getSelector2Name(),
                condition.getProperty2Name());
        return new RowPredicate() {
            @Override
            protected boolean evaluate(Row row) throws RepositoryException {
                return new ValueComparator().evaluate(
                        QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO,
                        getValues(operand1, row), getValues(operand2, row));
            }
        };
    }

    protected Constraint getJoinConstraint(
            JoinCondition condition, Set<String> selectors, List<Row> rows)
            throws RepositoryException {
        if (condition instanceof EquiJoinCondition) {
            return getJoinConstraint(
                    (EquiJoinCondition) condition, rows, selectors);
        } else if (condition instanceof SameNodeJoinCondition) {
            return getJoinConstraint(
                    (SameNodeJoinCondition) condition, rows, selectors);
        } else if (condition instanceof ChildNodeJoinCondition) {
            return getJoinConstraint(
                    (ChildNodeJoinCondition) condition, rows, selectors);
        } else if (condition instanceof DescendantNodeJoinCondition) {
            return getJoinConstraint(
                    (DescendantNodeJoinCondition) condition, rows, selectors);
        } else {
            return Constraints.TRUE;
        }
    }

    /**
     * Constructs a query constraint for the right side of an equi-join
     * whose left side results are already known.
     *
     * @param condition the equi-join condition
     * @param leftRows results of the left side of the join
     * @return constraint for right side results that can possibly match
     *         one or more of the left side results
     * @throws RepositoryException if the constraint can't be constructed
     */
    private Constraint getJoinConstraint(
            EquiJoinCondition condition, List<Row> leftRows,
            Set<String> rightSelectors) throws RepositoryException {
        String selector1 = condition.getSelector1Name();
        String property1 = condition.getProperty1Name();
        String selector2 = condition.getSelector2Name();
        String property2 = condition.getProperty2Name();
        if (!rightSelectors.contains(selector2)) {
            if (rightSelectors.contains(selector1)) {
                selector1 = condition.getSelector2Name();
                property1 = condition.getProperty2Name();
                selector2 = condition.getSelector1Name();
                property2 = condition.getProperty1Name();
            } else {
                return Constraints.TRUE;
            }
        }

        // Collect all matching values from the left source
        Set<Value> values = new HashSet<Value>();
        PropertyValue left = qomFactory.propertyValue(selector2, property2);
        for (Row row : leftRows) {
            for (Value value : getPropertyValues(left, row)) {
                values.add(value);
            }
        }

        // Convert each distinct value into a comparison constraint
        List<Constraint> constraints = new ArrayList<Constraint>(values.size());
        PropertyValue right = qomFactory.propertyValue(selector1, property1);
        for (Value value : values) {
            Literal literal = qomFactory.literal(value);
            constraints.add(qomFactory.comparison(
                    right, JCR_OPERATOR_EQUAL_TO, literal));
        }

        // Build a big OR constraint over all the collected comparisons
        return Constraints.or(qomFactory, constraints.toArray(
                new Constraint[constraints.size()]));
    }

    private Constraint getJoinConstraint(
            SameNodeJoinCondition condition, List<Row> leftRows,
            Set<String> rightSelectors) throws RepositoryException {
        String selector1 = condition.getSelector1Name();
        String selector2 = condition.getSelector2Name();
        String relativePath = condition.getSelector2Path();
        if (!rightSelectors.contains(selector1)) {
            if (relativePath == null && rightSelectors.contains(selector2)) {
                selector1 = condition.getSelector2Name();
                selector2 = condition.getSelector1Name();
            } else {
                return Constraints.TRUE;
            }
        }

        // Collect all matching paths and convert them into constraints
        List<Constraint> constraints = new ArrayList<Constraint>();
        for (String path : getPaths(selector2, relativePath, leftRows)) {
            constraints.add(qomFactory.descendantNode(selector1, path));
        }

        // Build a big OR constraint over all the collected comparisons
        return Constraints.or(qomFactory, constraints.toArray(
                new Constraint[constraints.size()]));
    }

    private Constraint getJoinConstraint(
            ChildNodeJoinCondition condition, List<Row> leftRows,
            Set<String> rightSelectors) throws RepositoryException {
        String parent = condition.getParentSelectorName();
        String child = condition.getChildSelectorName();
        if (!rightSelectors.contains(child)) {
            return Constraints.TRUE;
        }

        // Collect all matching paths and convert them into constraints
        List<Constraint> constraints = new ArrayList<Constraint>();
        for (String path : getPaths(parent, null, leftRows)) {
            constraints.add(qomFactory.descendantNode(child, path));
        }

        // Build a big OR constraint over all the collected comparisons
        return Constraints.or(qomFactory, constraints.toArray(
                new Constraint[constraints.size()]));
    }

    private Constraint getJoinConstraint(
            DescendantNodeJoinCondition condition, List<Row> leftRows,
            Set<String> rightSelectors) throws RepositoryException {
        String ancestor = condition.getAncestorSelectorName();
        String descendant = condition.getDescendantSelectorName();
        if (!rightSelectors.contains(descendant)) {
            return Constraints.TRUE;
        }

        // Collect all matching paths and convert them into constraints
        List<Constraint> constraints = new ArrayList<Constraint>();
        for (String path : getPaths(ancestor, null, leftRows)) {
            constraints.add(qomFactory.descendantNode(descendant, path));
        }

        // Build a big OR constraint over all the collected comparisons
        return Constraints.or(qomFactory, constraints.toArray(
                new Constraint[constraints.size()]));
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

    protected QueryResult execute(
            Column[] columns, Selector selector,
            Constraint constraint, Ordering[] orderings)
            throws RepositoryException {
        Map<String, NodeType> selectorMap = getSelectorNames(selector);
        final String[] selectorNames =
            selectorMap.keySet().toArray(new String[selectorMap.size()]);

        final Map<String, PropertyValue> columnMap =
            getColumnMap(columns, selectorMap);
        final String[] columnNames =
            columnMap.keySet().toArray(new String[columnMap.size()]);

        final double[] scores = new double[] { 1.0 };

        Iterator<Node> nodes =
            TreeTraverser.nodeIterator(session.getRootNode());
        RangeIterator rows = new RangeIteratorAdapter(nodes) {
            @Override
            public Object next() {
                try {
                    Value[] values = new Value[columnNames.length];
                    Row row = new SimpleRow(
                            columnNames, values, selectorNames,
                            new Node[] { (Node) super.next() }, scores);
                    for (int i = 0; i < values.length; i++) {
                        values[i] = combine(getPropertyValues(
                                columnMap.get(columnNames[i]), row));
                    }
                    return row;
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        RangeIterator filtered = new FilteredRangeIterator(
                rows, getPredicate(selector, constraint));
        QueryResult result = new SimpleQueryResult(
                columnNames, selectorNames, new RowIteratorAdapter(filtered));
        return sort(result, orderings);
    }

    private Value combine(Value[] values) throws RepositoryException {
        if (values.length == 1) {
            return values[0];
        } else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    builder.append('\n');
                }
                builder.append(values[i].getString());
            }
            return valueFactory.createValue(builder.toString());
        }
    }

    private Predicate getPredicate(Selector selector, Constraint constraint)
            throws RepositoryException {
        final String name = selector.getNodeTypeName();
        Predicate predicate = getPredicate(constraint);
        if (name.equals(ntManager.getNodeType(NodeType.NT_BASE).getName())) {
            return predicate;
        } else {
            Predicate typeCheck = new RowPredicate(selector.getSelectorName()) {
                @Override
                protected boolean evaluate(Node node)
                        throws RepositoryException {
                    return node.isNodeType(name);
                }
            };
            return Predicates.and(typeCheck, predicate);
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

            Collections.sort(rows, new Comparator<Row>() {
                public int compare(Row a, Row b) {
                    try {
                        ValueComparator comparator = new ValueComparator();
                        for (Ordering ordering : orderings) {
                            Operand operand = ordering.getOperand();
                            Value[] va = getValues(operand, a);
                            Value[] vb = getValues(operand, b);
                            if (va.length == 1 && vb.length == 1) {
                                int order = comparator.compare(va[0], vb[0]);
                                if (JCR_ORDER_DESCENDING.equals(ordering.getOrder())) {
                                    order = -order;
                                }
                                if (order != 0) {
                                    return order;
                                }
                            }
                        }
                        return 0;
                    } catch (RepositoryException e) {
                        throw new RuntimeException("Unable to compare rows", e);
                    }
                }
            });

            return new SimpleQueryResult(
                    result.getColumnNames(), result.getSelectorNames(),
                    new RowIteratorAdapter(rows));
        } else {
            return result;
        }
    }

    public Predicate getPredicate(Constraint constraint) {
        if (constraint == Constraints.TRUE) {
            return Predicate.TRUE;
        } else if (constraint == Constraints.FALSE) {
            return Predicate.FALSE;
        } else if (constraint instanceof And) {
            And and = (And) constraint;
            return Predicates.and(
                    getPredicate(and.getConstraint1()),
                    getPredicate(and.getConstraint2()));
        } else if (constraint instanceof Or) {
            Or or = (Or) constraint;
            return Predicates.or(
                    getPredicate(or.getConstraint1()),
                    getPredicate(or.getConstraint2()));
        } else if (constraint instanceof Not) {
            Not not = (Not) constraint;
            return Predicates.not(getPredicate(not.getConstraint()));
        } else if (constraint instanceof Not) {
            Not not = (Not) constraint;
            return Predicates.not(getPredicate(not.getConstraint()));
        } else if (constraint instanceof PropertyExistence) {
            final PropertyExistence pe = (PropertyExistence) constraint;
            return new RowPredicate(pe.getSelectorName()) {
                @Override
                protected boolean evaluate(Node node)
                        throws RepositoryException {
                    return node.hasProperty(pe.getPropertyName());
                }
            };
        } else if (constraint instanceof Comparison) {
            final Comparison c = (Comparison) constraint;
            return new RowPredicate() {
                @Override
                protected boolean evaluate(Row row)
                        throws RepositoryException {
                    return new ValueComparator().evaluate(
                            c.getOperator(),
                            getValues(c.getOperand1(), row),
                            getValues(c.getOperand2(), row));
                }
            };
        } else if (constraint instanceof SameNode) {
            final SameNode sn = (SameNode) constraint;
            return new RowPredicate(sn.getSelectorName()) {
                @Override
                protected boolean evaluate(Node node)
                        throws RepositoryException {
                    return node.getPath().equals(sn.getPath());
                }
            };
        } else if (constraint instanceof ChildNode) {
            final ChildNode cn = (ChildNode) constraint;
            return new RowPredicate(cn.getSelectorName()) {
                @Override
                protected boolean evaluate(Node node)
                        throws RepositoryException {
                    if (node.getDepth() > 0) {
                        String path = node.getParent().getPath();
                        return path.equals(cn.getParentPath());
                    } else {
                        return false;
                    }
                }
            };
        } else if (constraint instanceof DescendantNode) {
            final DescendantNode dn = (DescendantNode) constraint;
            return new RowPredicate(dn.getSelectorName()) {
                @Override
                protected boolean evaluate(Node node)
                        throws RepositoryException {
                    if (node.getDepth() > 0) {
                        Node parent = node.getParent();
                        if (parent.getPath().equals(dn.getAncestorPath())) {
                            return true;
                        } else {
                            return evaluate(parent);
                        }
                    } else {
                        return false;
                    }
                }
            };
        } else {
            throw new IllegalArgumentException(
                    "Unknown constraint type: " + constraint);
        }
    }

    /**
     * Evaluates the given operand against the given row. Subclasses can
     * customise the evaluation process by overriding one or more of the
     * protected getValue() methods to which this method dispatches the
     * evaluation process.
     *
     * @param operand operand
     * @param row row
     * @return value of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    public Value[] getValues(Operand operand, Row row)
            throws RepositoryException {
        if (operand instanceof BindVariableValue) {
            return getBindVariableValues((BindVariableValue) operand);
        } else if (operand instanceof FullTextSearchScore) {
            return getFullTextSearchScoreValues(
                    (FullTextSearchScore) operand, row);
        } else if (operand instanceof Length) {
            return getLengthValues((Length) operand, row);
        } else if (operand instanceof Literal) {
            return getLiteralValues((Literal) operand);
        } else if (operand instanceof LowerCase) {
            return getLowerCaseValues((LowerCase) operand, row);
        } else if (operand instanceof NodeLocalName) {
            return getNodeLocalNameValues((NodeLocalName) operand, row);
        } else if (operand instanceof NodeName) {
            return getNodeNameValues((NodeName) operand, row);
        } else if (operand instanceof PropertyValue) {
            return getPropertyValues((PropertyValue) operand, row);
        } else if (operand instanceof UpperCase) {
            return getUpperCaseValues((UpperCase) operand, row);
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Unknown operand type: " + operand);
        }
    }

    /**
     * Returns the value of the given variable value operand at the given row.
     *
     * @param operand variable value operand
     * @return value of the operand at the given row
     */
    private Value[] getBindVariableValues(BindVariableValue operand) {
        Value value = variables.get(operand.getBindVariableName());
        if (value != null) {
            return new Value[] { value };
        } else {
            return new Value[0];
        }
    }

    /**
     * Returns the value of the given search score operand at the given row.
     *
     * @param operand search score operand
     * @param row row
     * @return value of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    private Value[] getFullTextSearchScoreValues(
            FullTextSearchScore operand, Row row) throws RepositoryException {
        double score = row.getScore(operand.getSelectorName());
        return new Value[] { valueFactory.createValue(score) };
    }

    /**
     * Returns the values of the given value length operand at the given row.
     *
     * @see #getProperty(PropertyValue, Row)
     * @param operand value length operand
     * @param row row
     * @return values of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    private Value[] getLengthValues(Length operand, Row row)
            throws RepositoryException {
        Property property = getProperty(operand.getPropertyValue(), row);
        if (property == null) {
            return new Value[0];
        } else if (property.isMultiple()) {
            long[] lengths = property.getLengths();
            Value[] values = new Value[lengths.length];
            for (int i = 0; i < lengths.length; i++) {
                values[i] = valueFactory.createValue(lengths[i]);
            }
            return values;
        } else {
            long length = property.getLength();
            return new Value[] { valueFactory.createValue(length) };
        }
    }

    /**
     * Returns the value of the given literal value operand.
     *
     * @param operand literal value operand
     * @return value of the operand
     */
    protected Value[] getLiteralValues(Literal operand) {
        return new Value[] { operand.getLiteralValue() };
    }

    /**
     * Returns the values of the given lower case operand at the given row.
     *
     * @param operand lower case operand
     * @param row row
     * @return values of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    private Value[] getLowerCaseValues(LowerCase operand, Row row)
            throws RepositoryException {
        Value[] values = getValues(operand.getOperand(), row);
        for (int i = 0; i < values.length; i++) {
            String value = values[i].getString();
            String lower = value.toLowerCase(ENGLISH);
            if (!value.equals(lower)) {
                values[i] = valueFactory.createValue(lower);
            }
        }
        return values;
    }

    /**
     * Returns the value of the given local name operand at the given row.
     *
     * @param operand local name operand
     * @param row row
     * @return value of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    private Value[] getNodeLocalNameValues(NodeLocalName operand, Row row)
            throws RepositoryException {
        String name = row.getNode(operand.getSelectorName()).getName();
        int colon = name.indexOf(':');
        if (colon != -1) {
            name = name.substring(colon + 1);
        }
        return new Value[] { valueFactory.createValue(name, NAME) };
    }

    /**
     * Returns the value of the given node name operand at the given row.
     *
     * @param operand node name operand
     * @param row row
     * @return value of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    private Value[] getNodeNameValues(NodeName operand, Row row)
            throws RepositoryException {
        Node node = row.getNode(operand.getSelectorName());
        return new Value[] { valueFactory.createValue(node.getName(), NAME) };
    }

    /**
     * Returns the values of the given property value operand at the given row.
     *
     * @see #getProperty(PropertyValue, Row)
     * @param operand property value operand
     * @param row row
     * @return values of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    private Value[] getPropertyValues(PropertyValue operand, Row row)
            throws RepositoryException {
        Property property = getProperty(operand, row);
        if (property == null) {
            return new Value[0];
        } else if (property.isMultiple()) {
            return property.getValues();
        } else {
            return new Value[] { property.getValue() };
        }
    }

    /**
     * Returns the values of the given upper case operand at the given row.
     *
     * @param operand upper case operand
     * @param row row
     * @return values of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    private Value[] getUpperCaseValues(UpperCase operand, Row row)
            throws RepositoryException {
        Value[] values = getValues(operand.getOperand(), row);
        for (int i = 0; i < values.length; i++) {
            String value = values[i].getString();
            String upper = value.toLowerCase(ENGLISH);
            if (!value.equals(upper)) {
                values[i] = valueFactory.createValue(upper);
            }
        }
        return values;
    }

    /**
     * Returns the identified property from the given row. This method
     * is used by both the {@link #getValue(Length, Row)} and the
     * {@link #getValue(PropertyValue, Row)} methods to access properties.
     * Subclasses can override this method to customise property access.
     *
     * @param operand property value operand
     * @param row row
     * @return the identified property, or <code>null</code>
     * @throws RepositoryException if the property can't be accessed
     */
    protected Property getProperty(PropertyValue operand, Row row)
            throws RepositoryException {
        try {
            String selector = operand.getSelectorName();
            String property = operand.getPropertyName();
            return row.getNode(selector).getProperty(property);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

}
