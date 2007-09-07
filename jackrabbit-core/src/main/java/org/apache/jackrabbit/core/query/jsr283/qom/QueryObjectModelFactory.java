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
package org.apache.jackrabbit.core.query.jsr283.qom;

import javax.jcr.query.InvalidQueryException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * A <code>QueryObjectModelFactory</code> creates instances of the JCR query
 * object model.
 * <p/>
 * Refer to {@link org.apache.jackrabbit.core.query.jsr283.qom.QueryObjectModel} for a description of the query object
 * model.
 *
 * @since JCR 2.0
 */
public interface QueryObjectModelFactory
        extends QueryObjectModelConstants {
    ///
    /// QUERY
    ///

    /**
     * Creates a query with one selector.
     * <p/>
     * The specified selector will be the <i>default selector</i> of the query.
     *
     * @param selector   the selector; non-null
     * @param constraint the constraint, or null if none
     * @param orderings  zero or more orderings; null is equivalent to a
     *                   zero-length array
     * @param columns    the columns; null is equivalent to a zero-length
     *                   array
     * @return the query; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public QueryObjectModel createQuery
            (Selector selector,
             Constraint constraint,
             Ordering[] orderings,
             Column[] columns) throws InvalidQueryException, RepositoryException;

    /**
     * Creates a query with one or more selectors.
     * <p/>
     * If <code>source</code> is a selector, that selector is the <i>default
     * selector</i> of the query.  Otherwise the query does not have a default
     * selector.
     *
     * @param source     the node-tuple source; non-null
     * @param constraint the constraint, or null if none
     * @param orderings  zero or more orderings; null is equivalent to a
     *                   zero-length array
     * @param columns    the columns; null is equivalent to a zero-length
     *                   array
     * @return the query; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public QueryObjectModel createQuery
            (Source source,
             Constraint constraint,
             Ordering[] orderings,
             Column[] columns) throws InvalidQueryException, RepositoryException;

    ///
    /// SELECTOR
    ///

    /**
     * Selects a subset of the nodes in the repository based on node type.
     * <p/>
     * The selector name is the node type name.
     *
     * @param nodeTypeName the name of the required node type; non-null
     * @return the selector; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public Selector selector(String nodeTypeName)
            throws InvalidQueryException, RepositoryException;

    /**
     * Selects a subset of the nodes in the repository based on node type.
     *
     * @param nodeTypeName the name of the required node type; non-null
     * @param selectorName the selector name; non-null
     * @return the selector; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public Selector selector(String nodeTypeName, String selectorName)
            throws InvalidQueryException, RepositoryException;

    ///
    /// JOIN
    ///

    /**
     * Performs a join between two node-tuple sources.
     *
     * @param left          the left node-tuple source; non-null
     * @param right         the right node-tuple source; non-null
     * @param joinType      either
     *                      <ul>
     *                      <li>{@link org.apache.jackrabbit.core.query.jsr283.qom.QueryObjectModelConstants#JOIN_TYPE_INNER},</li>
     *                      <li>{@link org.apache.jackrabbit.core.query.jsr283.qom.QueryObjectModelConstants#JOIN_TYPE_LEFT_OUTER},</li>
     *                      <li>{@link org.apache.jackrabbit.core.query.jsr283.qom.QueryObjectModelConstants#JOIN_TYPE_RIGHT_OUTER}</li>
     *                      </ul>
     * @param joinCondition the join condition; non-null
     * @return the join; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public Join join
            (Source left,
             Source right,
             int joinType,
             JoinCondition joinCondition) throws InvalidQueryException, RepositoryException;

    ///
    /// JOINCONDITION
    ///

    /**
     * Tests whether the value of a property in a first selector is equal to the
     * value of a property in a second selector.
     *
     * @param selector1Name the name of the first selector; non-null
     * @param property1Name the property name in the first selector; non-null
     * @param selector2Name the name of the second selector; non-null
     * @param property2Name the property name in the second selector; non-null
     * @return the constraint; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public EquiJoinCondition equiJoinCondition
            (String selector1Name,
             String property1Name,
             String selector2Name,
             String property2Name) throws InvalidQueryException, RepositoryException;

    /**
     * Tests whether a first selector's node is the same as a second selector's
     * node.
     *
     * @param selector1Name the name of the first selector; non-null
     * @param selector2Name the name of the second selector; non-null
     * @return the constraint; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public SameNodeJoinCondition sameNodeJoinCondition
            (String selector1Name,
             String selector2Name) throws InvalidQueryException, RepositoryException;

    /**
     * Tests whether a first selector's node is the same as a node identified
     * by relative path from a second selector's node.
     *
     * @param selector1Name the name of the first selector; non-null
     * @param selector2Name the name of the second selector; non-null
     * @param selector2Path the path relative to the second selector; non-null
     * @return the constraint; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public SameNodeJoinCondition sameNodeJoinCondition
            (String selector1Name,
             String selector2Name,
             String selector2Path) throws InvalidQueryException, RepositoryException;

    /**
     * Tests whether a first selector's node is a child of a second selector's
     * node.
     *
     * @param childSelectorName  the name of the child selector; non-null
     * @param parentSelectorName the name of the parent selector; non-null
     * @return the constraint; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public ChildNodeJoinCondition childNodeJoinCondition
            (String childSelectorName,
             String parentSelectorName) throws InvalidQueryException, RepositoryException;

    /**
     * Tests whether a first selector's node is a descendant of a second
     * selector's node.
     *
     * @param descendantSelectorName the name of the descendant selector; non-null
     * @param ancestorSelectorName   the name of the ancestor selector; non-null
     * @return the constraint; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public DescendantNodeJoinCondition descendantNodeJoinCondition
            (String descendantSelectorName,
             String ancestorSelectorName) throws InvalidQueryException, RepositoryException;

    ///
    /// CONSTRAINT
    ///

    /**
     * Performs a logical conjunction of two other constraints.
     *
     * @param constraint1 the first constraint; non-null
     * @param constraint2 the second constraint; non-null
     * @return the <code>And</code> constraint; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public And and(Constraint constraint1, Constraint constraint2)
            throws InvalidQueryException, RepositoryException;

    /**
     * Performs a logical disjunction of two other constraints.
     *
     * @param constraint1 the first constraint; non-null
     * @param constraint2 the second constraint; non-null
     * @return the <code>Or</code> constraint; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public Or or(Constraint constraint1, Constraint constraint2)
            throws InvalidQueryException, RepositoryException;

    /**
     * Performs a logical negation of another constraint.
     *
     * @param constraint the constraint to be negated; non-null
     * @return the <code>Not</code> constraint; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public Not not(Constraint constraint)
            throws InvalidQueryException, RepositoryException;

    /**
     * Filters node-tuples based on the outcome of a binary operation.
     *
     * @param operand1 the first operand; non-null
     * @param operator the operator; either
     *                 <ul>
     *                 <li>{@link #OPERATOR_EQUAL_TO},</li>
     *                 <li>{@link #OPERATOR_NOT_EQUAL_TO},</li>
     *                 <li>{@link #OPERATOR_LESS_THAN},</li>
     *                 <li>{@link #OPERATOR_LESS_THAN_OR_EQUAL_TO},</li>
     *                 <li>{@link #OPERATOR_GREATER_THAN},</li>
     *                 <li>{@link #OPERATOR_GREATER_THAN_OR_EQUAL_TO}, or</li>
     *                 <li>{@link #OPERATOR_LIKE}</li>
     *                 </ul>
     * @param operand2 the second operand; non-null
     * @return the constraint; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public Comparison comparison
            (DynamicOperand operand1,
             int operator,
             StaticOperand operand2) throws InvalidQueryException, RepositoryException;

    /**
     * Tests the existence of a property in the default selector.
     *
     * @param propertyName the property name; non-null
     * @return the constraint; non-null
     * @throws InvalidQueryException if the query has no default selector
     *                               or is otherwise invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public PropertyExistence propertyExistence(String propertyName)
            throws InvalidQueryException, RepositoryException;

    /**
     * Tests the existence of a property in the specified selector.
     *
     * @param selectorName the selector name; non-null
     * @param propertyName the property name; non-null
     * @return the constraint; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public PropertyExistence propertyExistence
            (String selectorName,
             String propertyName) throws InvalidQueryException, RepositoryException;

    /**
     * Performs a full-text search against the default selector.
     *
     * @param propertyName             the property name, or null to search all
     *                                 full-text indexed properties of the node
     *                                 (or node subtree, in some implementations)
     * @param fullTextSearchExpression the full-text search expression; non-null
     * @return the constraint; non-null
     * @throws InvalidQueryException if the query has no default selector
     *                               or is otherwise invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public FullTextSearch fullTextSearch
            (String propertyName,
             String fullTextSearchExpression) throws InvalidQueryException, RepositoryException;

    /**
     * Performs a full-text search against the specified selector.
     *
     * @param selectorName             the selector name; non-null
     * @param propertyName             the property name, or null to search all
     *                                 full-text indexed properties of the node
     *                                 (or node subtree, in some implementations)
     * @param fullTextSearchExpression the full-text search expression; non-null
     * @return the constraint; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public FullTextSearch fullTextSearch
            (String selectorName,
             String propertyName,
             String fullTextSearchExpression) throws InvalidQueryException, RepositoryException;

    /**
     * Tests whether a node in the default selector is reachable by a specified
     * absolute path.
     *
     * @param path an absolute path; non-null
     * @return the constraint; non-null
     * @throws InvalidQueryException if the query has no default selector
     *                               or is otherwise invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public SameNode sameNode(String path)
            throws InvalidQueryException, RepositoryException;

    /**
     * Tests whether a node in the specified selector is reachable by a specified
     * absolute path.
     *
     * @param selectorName the selector name; non-null
     * @param path         an absolute path; non-null
     * @return the constraint; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public SameNode sameNode(String selectorName, String path)
            throws InvalidQueryException, RepositoryException;

    /**
     * Tests whether a node in the default selector is a child of a node
     * reachable by a specified absolute path.
     *
     * @param path an absolute path; non-null
     * @return the constraint; non-null
     * @throws InvalidQueryException if the query has no default selector
     *                               or is otherwise invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public ChildNode childNode(String path)
            throws InvalidQueryException, RepositoryException;

    /**
     * Tests whether a node in the specified selector is a child of a node
     * reachable by a specified absolute path.
     *
     * @param selectorName the selector name; non-null
     * @param path         an absolute path; non-null
     * @return the constraint; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public ChildNode childNode(String selectorName, String path)
            throws InvalidQueryException, RepositoryException;

    /**
     * Tests whether a node in the default selector is a descendant of a node
     * reachable by a specified absolute path.
     *
     * @param path an absolute path; non-null
     * @return the constraint; non-null
     * @throws InvalidQueryException if the query has no default selector
     *                               or is otherwise invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public DescendantNode descendantNode(String path)
            throws InvalidQueryException, RepositoryException;

    /**
     * Tests whether a node in the specified selector is a descendant of a node
     * reachable by a specified absolute path.
     *
     * @param selectorName the selector name; non-null
     * @param path         an absolute path; non-null
     * @return the constraint; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public DescendantNode descendantNode(String selectorName, String path)
            throws InvalidQueryException, RepositoryException;

    ///
    /// OPERAND
    ///

    /**
     * Evaluates to the value (or values, if multi-valued) of a property of
     * the default selector.
     *
     * @param propertyName the property name; non-null
     * @return the operand; non-null
     * @throws InvalidQueryException if the query has no default selector
     *                               or is otherwise invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public PropertyValue propertyValue(String propertyName)
            throws InvalidQueryException, RepositoryException;

    /**
     * Evaluates to the value (or values, if multi-valued) of a property in the
     * specified selector.
     *
     * @param selectorName the selector name; non-null
     * @param propertyName the property name; non-null
     * @return the operand; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public PropertyValue propertyValue(String selectorName, String propertyName)
            throws InvalidQueryException, RepositoryException;

    /**
     * Evaluates to the length (or lengths, if multi-valued) of a property.
     *
     * @param propertyValue the property value for which to compute the length;
     *                      non-null
     * @return the operand; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public Length length(PropertyValue propertyValue)
            throws InvalidQueryException, RepositoryException;

    /**
     * Evaluates to a <code>NAME</code> value equal to the prefix-qualified name
     * of a node in the default selector.
     *
     * @return the operand; non-null
     * @throws InvalidQueryException if the query has no default selector
     *                               or is otherwise invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public NodeName nodeName()
            throws InvalidQueryException, RepositoryException;

    /**
     * Evaluates to a <code>NAME</code> value equal to the prefix-qualified name
     * of a node in the specified selector.
     *
     * @param selectorName the selector name; non-null
     * @return the operand; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public NodeName nodeName(String selectorName)
            throws InvalidQueryException, RepositoryException;

    /**
     * Evaluates to a <code>NAME</code> value equal to the local (unprefixed)
     * name of a node in the default selector.
     *
     * @return the operand; non-null
     * @throws InvalidQueryException if the query has no default selector
     *                               or is otherwise invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public NodeLocalName nodeLocalName()
            throws InvalidQueryException, RepositoryException;

    /**
     * Evaluates to a <code>NAME</code> value equal to the local (unprefixed)
     * name of a node in the specified selector.
     *
     * @param selectorName the selector name; non-null
     * @return the operand; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public NodeLocalName nodeLocalName(String selectorName)
            throws InvalidQueryException, RepositoryException;

    /**
     * Evaluates to a <code>DOUBLE</code> value equal to the full-text search
     * score of a node in the default selector.
     *
     * @return the operand; non-null
     * @throws InvalidQueryException if the query has no default selector
     *                               or is otherwise invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public FullTextSearchScore fullTextSearchScore()
            throws InvalidQueryException, RepositoryException;

    /**
     * Evaluates to a <code>DOUBLE</code> value equal to the full-text search
     * score of a node in the specified selector.
     *
     * @param selectorName the selector name; non-null
     * @return the operand; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public FullTextSearchScore fullTextSearchScore(String selectorName)
            throws InvalidQueryException, RepositoryException;

    /**
     * Evaluates to the lower-case string value (or values, if multi-valued)
     * of an operand.
     *
     * @param operand the operand whose value is converted to a
     *                lower-case string; non-null
     * @return the operand; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public LowerCase lowerCase(DynamicOperand operand)
            throws InvalidQueryException, RepositoryException;

    /**
     * Evaluates to the upper-case string value (or values, if multi-valued)
     * of an operand.
     *
     * @param operand the operand whose value is converted to a
     *                upper-case string; non-null
     * @return the operand; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public UpperCase upperCase(DynamicOperand operand)
            throws InvalidQueryException, RepositoryException;

    /**
     * Evaluates to the value of a bind variable.
     *
     * @param bindVariableName the bind variable name; non-null
     * @return the operand; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public BindVariableValue bindVariable(String bindVariableName)
            throws InvalidQueryException, RepositoryException;

    /**
     * Evaluates to a literal value.
     *
     * @param value a JCR value; non-null
     * @return the operand; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public Literal literal(Value value)
            throws InvalidQueryException, RepositoryException;

    ///
    /// ORDERING
    ///

    /**
     * Orders by the value of the specified operand, in ascending order.
     *
     * @param operand the operand by which to order; non-null
     * @return the ordering
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public Ordering ascending(DynamicOperand operand)
            throws InvalidQueryException, RepositoryException;

    /**
     * Orders by the value of the specified operand, in descending order.
     *
     * @param operand the operand by which to order; non-null
     * @return the ordering
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public Ordering descending(DynamicOperand operand)
            throws InvalidQueryException, RepositoryException;

    ///
    /// COLUMN
    ///

    /**
     * Identifies a property in the default selector to include in the tabular
     * view of query results.
     * <p/>
     * The column name is the property name.
     *
     * @param propertyName the property name, or null to include a column
     *                     for each single-value non-residual property of
     *                     the selector's node type
     * @return the column; non-null
     * @throws InvalidQueryException if the query has no default selector
     *                               or is otherwise invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public Column column(String propertyName)
            throws InvalidQueryException, RepositoryException;

    /**
     * Identifies a property in the default selector to include in the tabular
     * view of query results.
     *
     * @param propertyName the property name, or null to include a column
     *                     for each single-value non-residual property of
     *                     the selector's node type
     * @param columnName   the column name; must be null if
     *                     <code>propertyName</code> is null
     * @return the column; non-null
     * @throws InvalidQueryException if the query has no default selector
     *                               or is otherwise invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public Column column(String propertyName, String columnName)
            throws InvalidQueryException, RepositoryException;

    /**
     * Identifies a property in the specified selector to include in the tabular
     * view of query results.
     *
     * @param selectorName the selector name; non-null
     * @param propertyName the property name, or null to include a column
     *                     for each single-value non-residual property of
     *                     the selector's node type
     * @param columnName   the column name; if null, defaults to
     *                     <code>propertyName</code>; must be null if
     *                     <code>propertyName</code> is null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public Column column
            (String selectorName,
             String propertyName,
             String columnName) throws InvalidQueryException, RepositoryException;
}
