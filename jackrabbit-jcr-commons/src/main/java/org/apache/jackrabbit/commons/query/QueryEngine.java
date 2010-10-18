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
package org.apache.jackrabbit.commons.query;

import static java.util.Locale.ENGLISH;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_ORDER_DESCENDING;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
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
import org.apache.jackrabbit.commons.query.qom.Constraints;

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
        Set<String> leftSelectors = getSelectorNames(left).keySet();

        Source right = join.getRight();
        Set<String> rightSelectors = getSelectorNames(right).keySet();

        List<Row> leftRows = new ArrayList<Row>();
        QueryResult leftResult =
            execute(null, left, mapConstraintToSelectors(constraint, leftSelectors), null);
        for (Row row : JcrUtils.getRows(leftResult)) {
            leftRows.add(row);
        }

        QueryResult rightResult =
            execute(null, right, mapConstraintToSelectors(constraint, rightSelectors), null);

        return null;
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

    protected Constraint narrow(
            Constraint constraint, Set<String> selectors,
            JoinCondition condition, List<Row> rows)
            throws RepositoryException {
        Constraint joinConstraint = Constraints.TRUE;
        if (condition instanceof EquiJoinCondition) {
            joinConstraint = getJoinConstraint(
                    (EquiJoinCondition) condition, rows, selectors);
        } else if (condition instanceof SameNodeJoinCondition) {
            joinConstraint = getJoinConstraint(
                    (SameNodeJoinCondition) condition, rows, selectors);
        } else if (condition instanceof ChildNodeJoinCondition) {
            joinConstraint = getJoinConstraint(
                    (ChildNodeJoinCondition) condition, rows, selectors);
        } else if (condition instanceof DescendantNodeJoinCondition) {
            joinConstraint = getJoinConstraint(
                    (DescendantNodeJoinCondition) condition, rows, selectors);
        }
        return Constraints.and(
                qomFactory, joinConstraint, mapConstraintToSelectors(constraint, selectors));
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
            values.add(getValue(left, row));
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
                        values[i] =
                            getValue(columnMap.get(columnNames[i]), row);
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
            String name = selector.getSelectorName();
            return Collections.singletonMap(name, ntManager.getNodeType(name));
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
                        for (Ordering ordering : orderings) {
                            Operand operand = ordering.getOperand();
                            Value va = getValue(operand, a);
                            Value vb = getValue(operand, b);
                            int order = new ValueComparator().compare(va, vb);
                            if (JCR_ORDER_DESCENDING.equals(ordering.getOrder())) {
                                order = -order;
                            }
                            if (order != 0) {
                                return order;
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
                            getValue(c.getOperand1(), row),
                            getValue(c.getOperand2(), row));
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
    public Value getValue(Operand operand, Row row)
            throws RepositoryException {
        if (operand instanceof BindVariableValue) {
            return getValue((BindVariableValue) operand, row);
        } else if (operand instanceof FullTextSearchScore) {
            return getValue((FullTextSearchScore) operand, row);
        } else if (operand instanceof Length) {
            return getValue((Length) operand, row);
        } else if (operand instanceof Literal) {
            return getValue((Literal) operand, row);
        } else if (operand instanceof LowerCase) {
            return getValue((LowerCase) operand, row);
        } else if (operand instanceof NodeLocalName) {
            return getValue((NodeLocalName) operand, row);
        } else if (operand instanceof NodeName) {
            return getValue((NodeName) operand, row);
        } else if (operand instanceof PropertyValue) {
            return getValue((PropertyValue) operand, row);
        } else if (operand instanceof UpperCase) {
            return getValue((UpperCase) operand, row);
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Unknown operand type: " + operand);
        }
    }

    /**
     * Returns the value of the given variable value operand at the given row.
     * Subclasses can override this method to customise the evaluation.
     *
     * @param operand variable value operand
     * @param row row
     * @return value of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    protected Value getValue(BindVariableValue operand, Row row)
            throws RepositoryException {
        String name = operand.getBindVariableName();
        Value value = variables.get(name);
        if (value != null) {
            return value;
        } else {
            throw new RepositoryException("Unknown bind variable: $" + name);
        }
    }

    /**
     * Returns the value of the given search score operand at the given row.
     * Subclasses can override this method to customise the evaluation.
     *
     * @param operand search score operand
     * @param row row
     * @return value of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    protected Value getValue(FullTextSearchScore operand, Row row)
            throws RepositoryException {
        return valueFactory.createValue(row.getScore(operand.getSelectorName()));
    }

    /**
     * Returns the value of the given value length operand at the given row.
     * Subclasses can override this method to customise the evaluation.
     *
     * @see #getProperty(PropertyValue, Row)
     * @param operand value length operand
     * @param row row
     * @return value of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    protected Value getValue(Length operand, Row row)
            throws RepositoryException {
        Property property = getProperty(operand.getPropertyValue(), row);
        return valueFactory.createValue(property.getLength());
    }

    /**
     * Returns the value of the given literal value operand at the given row.
     * Subclasses can override this method to customise the evaluation.
     *
     * @param operand literal value operand
     * @param row row
     * @return value of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    protected Value getValue(Literal operand, Row row) {
        return operand.getLiteralValue();
    }

    /**
     * Returns the value of the given lower case operand at the given row.
     * Subclasses can override this method to customise the evaluation.
     *
     * @param operand lower case operand
     * @param row row
     * @return value of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    protected Value getValue(LowerCase operand, Row row)
            throws RepositoryException {
        Value value = getValue(operand.getOperand(), row);
        return valueFactory.createValue(value.getString().toLowerCase(ENGLISH));
    }

    /**
     * Returns the value of the given local name operand at the given row.
     * Subclasses can override this method to customise the evaluation.
     *
     * @param operand local name operand
     * @param row row
     * @return value of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    protected Value getValue(NodeLocalName operand, Row row)
            throws RepositoryException {
        String name = row.getNode(operand.getSelectorName()).getName();
        int colon = name.indexOf(':');
        if (colon != -1) {
            name = name.substring(colon + 1);
        }
        return valueFactory.createValue(name, PropertyType.NAME);
    }

    /**
     * Returns the value of the given node name operand at the given row.
     * Subclasses can override this method to customise the evaluation.
     *
     * @param operand node name operand
     * @param row row
     * @return value of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    protected Value getValue(NodeName operand, Row row)
            throws RepositoryException {
        Node node = row.getNode(operand.getSelectorName());
        return valueFactory.createValue(node.getName(), PropertyType.NAME);
    }

    /**
     * Returns the value of the given property value operand at the given row.
     * Subclasses can override this method to customise the evaluation.
     *
     * @see #getProperty(PropertyValue, Row)
     * @param operand property value operand
     * @param row row
     * @return value of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    protected Value getValue(PropertyValue operand, Row row)
            throws RepositoryException {
        return getProperty(operand, row).getValue();
    }

    /**
     * Returns the value of the given upper case operand at the given row.
     * Subclasses can override this method to customise the evaluation.
     *
     * @param operand upper case operand
     * @param row row
     * @return value of the operand at the given row
     * @throws RepositoryException if the operand can't be evaluated
     */
    protected Value getValue(UpperCase operand, Row row)
            throws RepositoryException {
        Value value = getValue(operand.getOperand(), row);
        return valueFactory.createValue(value.getString().toUpperCase(ENGLISH));
    }

    /**
     * Returns the identified property from the given row. This method
     * is used by both the {@link #getValue(Length, Row)} and the
     * {@link #getValue(PropertyValue, Row)} methods to access properties.
     * Subclasses can override this method to customise property access.
     *
     * @param operand property value operand
     * @param row row
     * @return the identified property
     * @throws RepositoryException if the property can't be accessed
     */
    protected Property getProperty(PropertyValue operand, Row row)
            throws RepositoryException {
        String selector = operand.getSelectorName();
        String property = operand.getPropertyName();
        return row.getNode(selector).getProperty(property);
    }

}
