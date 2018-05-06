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
package org.apache.jackrabbit.spi.commons.query.qom;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.InvalidQueryException;
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
import javax.jcr.query.qom.FullTextSearch;
import javax.jcr.query.qom.FullTextSearchScore;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.JoinCondition;
import javax.jcr.query.qom.Length;
import javax.jcr.query.qom.Literal;
import javax.jcr.query.qom.LowerCase;
import javax.jcr.query.qom.NodeLocalName;
import javax.jcr.query.qom.NodeName;
import javax.jcr.query.qom.Not;
import javax.jcr.query.qom.Or;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.PropertyExistence;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.SameNode;
import javax.jcr.query.qom.SameNodeJoinCondition;
import javax.jcr.query.qom.Selector;
import javax.jcr.query.qom.Source;
import javax.jcr.query.qom.StaticOperand;
import javax.jcr.query.qom.UpperCase;

import org.apache.jackrabbit.commons.query.qom.JoinType;
import org.apache.jackrabbit.commons.query.qom.Operator;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

/**
 * <code>QueryObjectModelFactoryImpl</code> implements the query object model
 * factory from JSR 283.
 */
public abstract class QueryObjectModelFactoryImpl implements QueryObjectModelFactory {

    /**
     * The name and path resolver for this QOM factory.
     */
    private final NamePathResolver resolver;

    public QueryObjectModelFactoryImpl(NamePathResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Creates a query object model from the internal tree representation.
     *
     * @param qomTree the qom tree.
     * @return a query object model that can be executed.
     * @throws InvalidQueryException the the query object model tree is
     *                               considered invalid by the query handler
     *                               implementation.
     * @throws RepositoryException   if any other error occurs.
     */
    protected abstract QueryObjectModel createQuery(QueryObjectModelTree qomTree)
            throws InvalidQueryException, RepositoryException;

    /**
     * Creates a query with one selector.
     * <p>
     * The specified selector will be the <i>default selector</i> of the query.
     *
     * @param selector   the selector; non-null
     * @param constraint the constraint, or null if none
     * @param orderings  zero or more orderings; null is equivalent to a
     *                   zero-length array
     * @param columns    the columns; null is equivalent to a zero-length array
     * @return the query; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public QueryObjectModel createQuery                                           // CM
            (Selector selector,
             Constraint constraint,
             Ordering[] orderings,
             Column[] columns) throws InvalidQueryException, RepositoryException {
        return createQuery((Source) selector, constraint, orderings, columns);
    }

    /**
     * Creates a query with one or more selectors.
     * <p>
     * If <code>source</code> is a selector, that selector is the <i>default
     * selector</i> of the query.  Otherwise the query does not have a default
     * selector.
     *
     * @param source     the node-tuple source; non-null
     * @param constraint the constraint, or null if none
     * @param orderings  zero or more orderings; null is equivalent to a
     *                   zero-length array
     * @param columns    the columns; null is equivalent to a zero-length array
     * @return the query; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public QueryObjectModel createQuery(Source source,
                                        Constraint constraint,
                                        Ordering[] orderings,
                                        Column[] columns)
            throws InvalidQueryException, RepositoryException {
        if (source == null) {
            throw new InvalidQueryException("source must not be null");
        }
        if (!(source instanceof SourceImpl)) {
            throw new RepositoryException("Unknown Source implementation");
        }
        if (constraint != null && !(constraint instanceof ConstraintImpl)) {
            throw new RepositoryException("Unknown Constraint implementation");
        }
        OrderingImpl[] ords;
        if (orderings != null) {
            ords = new OrderingImpl[orderings.length];
            for (int i = 0; i < orderings.length; i++) {
                if (!(orderings[i] instanceof OrderingImpl)) {
                    throw new RepositoryException("Unknown Ordering implementation");
                }
                ords[i] = (OrderingImpl) orderings[i];
            }
        } else {
            ords = OrderingImpl.EMPTY_ARRAY;
        }
        ColumnImpl[] cols;
        if (columns != null) {
            cols = new ColumnImpl[columns.length];
            for (int i = 0; i < columns.length; i++) {
                if (!(columns[i] instanceof ColumnImpl)) {
                    throw new RepositoryException("Unknown Column implementation");
                }
                cols[i] = (ColumnImpl) columns[i];
            }
        } else {
            cols = ColumnImpl.EMPTY_ARRAY;
        }
        QueryObjectModelTree qomTree = new QueryObjectModelTree(
                resolver, (SourceImpl) source,
                (ConstraintImpl) constraint, ords, cols);
        return createQuery(qomTree);
    }

    /**
     * Selects a subset of the nodes in the repository based on node type.
     * <p>
     * The selector name is the node type name.
     *
     * @param nodeTypeName the name of the required node type; non-null
     * @return the selector; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public Selector selector(String nodeTypeName)                                 // CM
            throws InvalidQueryException, RepositoryException {
        Name ntName = checkNodeTypeName(nodeTypeName);
        return new SelectorImpl(resolver, ntName, ntName);
    }

    /**
     * Selects a subset of the nodes in the repository based on node type.
     *
     * @param nodeTypeName the name of the required node type; non-null
     * @param selectorName the selector name; non-null
     * @return the selector; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public Selector selector(String nodeTypeName, String selectorName)
            throws InvalidQueryException, RepositoryException {
        return new SelectorImpl(resolver, checkNodeTypeName(nodeTypeName),
                checkSelectorName(selectorName));
    }

    /**
     * Performs a join between two node-tuple sources.
     *
     * @param left          the left node-tuple source; non-null
     * @param right         the right node-tuple source; non-null
     * @param joinTypeName  either <ul> <li>{@link QueryObjectModelConstants#JCR_JOIN_TYPE_INNER},</li>
     *                      <li>{@link QueryObjectModelConstants#JCR_JOIN_TYPE_LEFT_OUTER},</li>
     *                      <li>{@link QueryObjectModelConstants#JCR_JOIN_TYPE_RIGHT_OUTER}</li>
     *                      </ul>
     * @param joinCondition the join condition; non-null
     * @return the join; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public Join join(Source left,
                     Source right,
                     String joinTypeName,
                     JoinCondition joinCondition)
            throws InvalidQueryException, RepositoryException {
        if (!(left instanceof SourceImpl) || !(right instanceof SourceImpl)) {
            throw new RepositoryException("Unknown Source implementation");
        }
        if (!(joinCondition instanceof JoinConditionImpl)) {
            throw new RepositoryException("Unknown JoinCondition implementation");
        }
        return new JoinImpl(
                resolver,
                (SourceImpl) left,
                (SourceImpl) right,
                JoinType.getJoinTypeByName(joinTypeName),
                (JoinConditionImpl) joinCondition);
    }

    /**
     * Tests whether the value of a property in a first selector is equal to the
     * value of a property in a second selector.
     *
     * @param selector1Name the name of the first selector; non-null
     * @param property1Name the property name in the first selector; non-null
     * @param selector2Name the name of the second selector; non-null
     * @param property2Name the property name in the second selector; non-null
     * @return the constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public EquiJoinCondition equiJoinCondition(String selector1Name,
                                               String property1Name,
                                               String selector2Name,
                                               String property2Name)
            throws InvalidQueryException, RepositoryException {
        return new EquiJoinConditionImpl(resolver,
                checkSelectorName(selector1Name),
                checkPropertyName(property1Name),
                checkSelectorName(selector2Name),
                checkPropertyName(property2Name));
    }

    /**
     * Tests whether a first selector's node is the same as a second selector's
     * node.
     *
     * @param selector1Name the name of the first selector; non-null
     * @param selector2Name the name of the second selector; non-null
     * @return the constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public SameNodeJoinCondition sameNodeJoinCondition(String selector1Name,
                                                       String selector2Name)
            throws InvalidQueryException, RepositoryException                          // CM
    {
        return new SameNodeJoinConditionImpl(resolver,
                checkSelectorName(selector1Name),
                checkSelectorName(selector2Name),
                null);
    }

    /**
     * Tests whether a first selector's node is the same as a node identified by
     * relative path from a second selector's node.
     *
     * @param selector1Name the name of the first selector; non-null
     * @param selector2Name the name of the second selector; non-null
     * @param selector2Path the path relative to the second selector; non-null
     * @return the constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public SameNodeJoinCondition sameNodeJoinCondition(String selector1Name,
                                                       String selector2Name,
                                                       String selector2Path)
            throws InvalidQueryException, RepositoryException {
        return new SameNodeJoinConditionImpl(resolver,
                checkSelectorName(selector1Name),
                checkSelectorName(selector2Name),
                checkPath(selector2Path));
    }

    /**
     * Tests whether a first selector's node is a child of a second selector's
     * node.
     *
     * @param childSelectorName  the name of the child selector; non-null
     * @param parentSelectorName the name of the parent selector; non-null
     * @return the constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public ChildNodeJoinCondition childNodeJoinCondition(
            String childSelectorName, String parentSelectorName)
            throws InvalidQueryException, RepositoryException {
        return new ChildNodeJoinConditionImpl(resolver,
                checkSelectorName(childSelectorName),
                checkSelectorName(parentSelectorName));
    }

    /**
     * Tests whether a first selector's node is a descendant of a second
     * selector's node.
     *
     * @param descendantSelectorName the name of the descendant selector;
     *                               non-null
     * @param ancestorSelectorName   the name of the ancestor selector;
     *                               non-null
     * @return the constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public DescendantNodeJoinCondition descendantNodeJoinCondition(
            String descendantSelectorName, String ancestorSelectorName)
            throws InvalidQueryException, RepositoryException {
        return new DescendantNodeJoinConditionImpl(resolver,
                checkSelectorName(descendantSelectorName),
                checkSelectorName(ancestorSelectorName));
    }

    /**
     * Performs a logical conjunction of two other constraints.
     *
     * @param constraint1 the first constraint; non-null
     * @param constraint2 the second constraint; non-null
     * @return the <code>And</code> constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public And and(Constraint constraint1, Constraint constraint2)
            throws InvalidQueryException, RepositoryException {
        if (constraint1 == null || constraint2 == null) {
            throw new InvalidQueryException("Constraints must not be null");
        }
        if (constraint1 instanceof ConstraintImpl
                && constraint2 instanceof ConstraintImpl) {
            return new AndImpl(resolver,
                    (ConstraintImpl) constraint1,
                    (ConstraintImpl) constraint2);
        } else {
            throw new RepositoryException("Unknown constraint implementation");
        }
    }

    /**
     * Performs a logical disjunction of two other constraints.
     *
     * @param constraint1 the first constraint; non-null
     * @param constraint2 the second constraint; non-null
     * @return the <code>Or</code> constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public Or or(Constraint constraint1, Constraint constraint2)
            throws InvalidQueryException, RepositoryException {
        if (constraint1 == null || constraint2 == null) {
            throw new InvalidQueryException("Constraints must not be null");
        }
        if (constraint1 instanceof ConstraintImpl
                && constraint2 instanceof ConstraintImpl) {
            return new OrImpl(resolver,
                    (ConstraintImpl) constraint1,
                    (ConstraintImpl) constraint2);
        } else {
            throw new RepositoryException("Unknown constraint implementation");
        }
    }

    /**
     * Performs a logical negation of another constraint.
     *
     * @param constraint the constraint to be negated; non-null
     * @return the <code>Not</code> constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public Not not(Constraint constraint) throws InvalidQueryException, RepositoryException {
        if (!(constraint instanceof ConstraintImpl)) {
            throw new RepositoryException("Unknown Constraint implementation");
        }
        return new NotImpl(resolver, (ConstraintImpl) constraint);
    }

    /**
     * Filters node-tuples based on the outcome of a binary operation.
     *
     * @param left the first operand; non-null
     * @param operatorName the operator; either <ul> <li>{@link #JCR_OPERATOR_EQUAL_TO},</li>
     *                 <li>{@link #JCR_OPERATOR_NOT_EQUAL_TO},</li> <li>{@link
     *                 #JCR_OPERATOR_LESS_THAN},</li> <li>{@link #JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO},</li>
     *                 <li>{@link #JCR_OPERATOR_GREATER_THAN},</li> <li>{@link
     *                 #JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO}, or</li> <li>{@link
     *                 #JCR_OPERATOR_LIKE}</li> </ul>
     * @param right the second operand; non-null
     * @return the constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public Comparison comparison(
            DynamicOperand left, String operatorName, StaticOperand right)
            throws InvalidQueryException, RepositoryException {
        if (!(left instanceof DynamicOperandImpl)) {
            throw new RepositoryException("Invalid left operand: " + left);
        }
        if (!(right instanceof StaticOperandImpl)) {
            throw new RepositoryException("Invalid right operand: " + right);
        }

        return new ComparisonImpl(
                resolver,
                (DynamicOperandImpl) left,
                Operator.getOperatorByName(operatorName),
                (StaticOperandImpl) right);
    }

    /**
     * Tests the existence of a property in the default selector.
     *
     * @param propertyName the property name; non-null
     * @return the constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query has no default
     *                                       selector or is otherwise invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public PropertyExistence propertyExistence(String propertyName)               // CM
            throws InvalidQueryException, RepositoryException {
        return new PropertyExistenceImpl(
                resolver, null, checkPropertyName(propertyName));
    }

    /**
     * Tests the existence of a property in the specified selector.
     *
     * @param selectorName the selector name; non-null
     * @param propertyName the property name; non-null
     * @return the constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public PropertyExistence propertyExistence(String selectorName,
                                               String propertyName)
            throws InvalidQueryException, RepositoryException {
        return new PropertyExistenceImpl(resolver,
                checkSelectorName(selectorName),
                checkPropertyName(propertyName));
    }

    /**
     * Performs a full-text search against the default selector.
     *
     * @param propertyName             the property name, or null to search all
     *                                 full-text indexed properties of the node
     *                                 (or node subtree, in some implementations)
     * @param fullTextSearchExpression the full-text search expression;
     *                                 non-null
     * @return the constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query has no default
     *                                       selector or is otherwise invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public FullTextSearch fullTextSearch(String propertyName,
            StaticOperand fullTextSearchExpression)
            throws InvalidQueryException, RepositoryException                          // CM
    {
        Name propName = null;
        if (propertyName != null) {
            propName = checkPropertyName(propertyName);
        }
        return new FullTextSearchImpl(resolver, null, propName,
                checkFullTextSearchExpression(fullTextSearchExpression));
    }

    /**
     * Performs a full-text search against the specified selector.
     *
     * @param selectorName             the selector name; non-null
     * @param propertyName             the property name, or null to search all
     *                                 full-text indexed properties of the node
     *                                 (or node subtree, in some implementations)
     * @param fullTextSearchExpression the full-text search expression;
     *                                 non-null
     * @return the constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public FullTextSearch fullTextSearch(String selectorName,
                                         String propertyName,
                                         StaticOperand fullTextSearchExpression)
            throws InvalidQueryException, RepositoryException {
        if (fullTextSearchExpression == null) {
            throw new IllegalArgumentException(
                    "Full text search expression is null");
        }
        Name propName = null;
        if (propertyName != null) {
            propName = checkPropertyName(propertyName);
        }
        return new FullTextSearchImpl(resolver,
                checkSelectorName(selectorName), propName,
                checkFullTextSearchExpression(fullTextSearchExpression));
    }

    /**
     * Tests whether a node in the default selector is reachable by a specified
     * absolute path.
     *
     * @param path an absolute path; non-null
     * @return the constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query has no default
     *                                       selector or is otherwise invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public SameNode sameNode(String path) throws InvalidQueryException, RepositoryException                          // CM
    {
        return new SameNodeImpl(resolver, null, checkPath(path));
    }

    /**
     * Tests whether a node in the specified selector is reachable by a
     * specified absolute path.
     *
     * @param selectorName the selector name; non-null
     * @param path         an absolute path; non-null
     * @return the constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public SameNode sameNode(String selectorName, String path)
            throws InvalidQueryException, RepositoryException {
        return new SameNodeImpl(
                resolver, checkSelectorName(selectorName), checkPath(path));
    }

    /**
     * Tests whether a node in the default selector is a child of a node
     * reachable by a specified absolute path.
     *
     * @param path an absolute path; non-null
     * @return the constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query has no default
     *                                       selector or is otherwise invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public ChildNode childNode(String path) throws InvalidQueryException, RepositoryException                          // CM
    {
        return new ChildNodeImpl(resolver, null, checkPath(path));
    }

    /**
     * Tests whether a node in the specified selector is a child of a node
     * reachable by a specified absolute path.
     *
     * @param selectorName the selector name; non-null
     * @param path         an absolute path; non-null
     * @return the constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public ChildNode childNode(String selectorName, String path)
            throws InvalidQueryException, RepositoryException {
        return new ChildNodeImpl(
                resolver, checkSelectorName(selectorName), checkPath(path));
    }

    /**
     * Tests whether a node in the default selector is a descendant of a node
     * reachable by a specified absolute path.
     *
     * @param path an absolute path; non-null
     * @return the constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query has no default
     *                                       selector or is otherwise invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public DescendantNode descendantNode(String path)
            throws InvalidQueryException, RepositoryException {
        return new DescendantNodeImpl(resolver, null, checkPath(path));
    }

    /**
     * Tests whether a node in the specified selector is a descendant of a node
     * reachable by a specified absolute path.
     *
     * @param selectorName the selector name; non-null
     * @param path         an absolute path; non-null
     * @return the constraint; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public DescendantNode descendantNode(String selectorName, String path)
            throws InvalidQueryException, RepositoryException {
        return new DescendantNodeImpl(resolver,
                checkSelectorName(selectorName), checkPath(path));
    }

    /**
     * Evaluates to the value (or values, if multi-valued) of a property of the
     * default selector.
     *
     * @param propertyName the property name; non-null
     * @return the operand; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query has no default
     *                                       selector or is otherwise invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public PropertyValue propertyValue(String propertyName)                       // CM
            throws InvalidQueryException, RepositoryException {
        return new PropertyValueImpl(resolver, null, checkPropertyName(propertyName));
    }

    /**
     * Evaluates to the value (or values, if multi-valued) of a property in the
     * specified selector.
     *
     * @param selectorName the selector name; non-null
     * @param propertyName the property name; non-null
     * @return the operand; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public PropertyValue propertyValue(String selectorName,
                                       String propertyName)
            throws InvalidQueryException, RepositoryException {
        return new PropertyValueImpl(resolver,
                checkSelectorName(selectorName),
                checkPropertyName(propertyName));
    }

    /**
     * Evaluates to the length (or lengths, if multi-valued) of a property.
     *
     * @param propertyValue the property value for which to compute the length;
     *                      non-null
     * @return the operand; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public Length length(PropertyValue propertyValue)
            throws InvalidQueryException, RepositoryException {
        if (!(propertyValue instanceof PropertyValueImpl)) {
            throw new RepositoryException("Unknown PropertyValue implementation");
        }
        return new LengthImpl(resolver, (PropertyValueImpl) propertyValue);
    }

    /**
     * Evaluates to a <code>NAME</code> value equal to the prefix-qualified name
     * of a node in the default selector.
     *
     * @return the operand; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query has no default
     *                                       selector or is otherwise invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public NodeName nodeName()                                                    // CM
            throws InvalidQueryException, RepositoryException {
        return new NodeNameImpl(resolver, null);
    }

    /**
     * Evaluates to a <code>NAME</code> value equal to the prefix-qualified name
     * of a node in the specified selector.
     *
     * @param selectorName the selector name; non-null
     * @return the operand; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public NodeName nodeName(String selectorName) throws InvalidQueryException, RepositoryException {
        return new NodeNameImpl(resolver, checkSelectorName(selectorName));
    }

    /**
     * Evaluates to a <code>NAME</code> value equal to the local (unprefixed)
     * name of a node in the default selector.
     *
     * @return the operand; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query has no default
     *                                       selector or is otherwise invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public NodeLocalName nodeLocalName()                                          // CM
            throws InvalidQueryException, RepositoryException {
        return new NodeLocalNameImpl(resolver, null);
    }

    /**
     * Evaluates to a <code>NAME</code> value equal to the local (unprefixed)
     * name of a node in the specified selector.
     *
     * @param selectorName the selector name; non-null
     * @return the operand; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public NodeLocalName nodeLocalName(String selectorName)
            throws InvalidQueryException, RepositoryException {
        return new NodeLocalNameImpl(resolver, checkSelectorName(selectorName));
    }

    /**
     * Evaluates to a <code>DOUBLE</code> value equal to the full-text search
     * score of a node in the default selector.
     *
     * @return the operand; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query has no default
     *                                       selector or is otherwise invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public FullTextSearchScore fullTextSearchScore()                              // CM
            throws InvalidQueryException, RepositoryException {
        return new FullTextSearchScoreImpl(resolver, null);
    }

    /**
     * Evaluates to a <code>DOUBLE</code> value equal to the full-text search
     * score of a node in the specified selector.
     *
     * @param selectorName the selector name; non-null
     * @return the operand; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public FullTextSearchScore fullTextSearchScore(String selectorName)
            throws InvalidQueryException, RepositoryException {
        return new FullTextSearchScoreImpl(
                resolver, checkSelectorName(selectorName));
    }

    /**
     * Evaluates to the lower-case string value (or values, if multi-valued) of
     * an operand.
     *
     * @param operand the operand whose value is converted to a lower-case
     *                string; non-null
     * @return the operand; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public LowerCase lowerCase(DynamicOperand operand)
            throws InvalidQueryException, RepositoryException {
        if (!(operand instanceof DynamicOperandImpl)) {
            throw new RepositoryException("Unknown DynamicOperand implementation");
        }
        return new LowerCaseImpl(resolver, (DynamicOperandImpl) operand);
    }

    /**
     * Evaluates to the upper-case string value (or values, if multi-valued) of
     * an operand.
     *
     * @param operand the operand whose value is converted to a upper-case
     *                string; non-null
     * @return the operand; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public UpperCase upperCase(DynamicOperand operand)
            throws InvalidQueryException, RepositoryException {
        if (!(operand instanceof DynamicOperandImpl)) {
            throw new RepositoryException("Unknown DynamicOperand implementation");
        }
        return new UpperCaseImpl(resolver, (DynamicOperandImpl) operand);
    }

    /**
     * Evaluates to the value of a bind variable.
     *
     * @param bindVariableName the bind variable name; non-null
     * @return the operand; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public BindVariableValue bindVariable(String bindVariableName)
            throws InvalidQueryException, RepositoryException {
        if (bindVariableName == null) {
            throw new InvalidQueryException("bindVariableName must not be null");
        }
        try {
            return new BindVariableValueImpl(
                    resolver, resolver.getQName(bindVariableName));
        } catch (NameException e) {
            throw new InvalidQueryException(e.getMessage());
        }
    }

    /**
     * Evaluates to a literal value.
     *
     * @param value a JCR value; non-null
     * @return the operand; non-null
     * @throws InvalidQueryException if the query is invalid
     * @throws RepositoryException   if the operation otherwise fails
     */
    public Literal literal(Value value)
            throws InvalidQueryException, RepositoryException {
        if (value == null) {
            throw new InvalidQueryException("value must not be null");
        }
        return new LiteralImpl(resolver, value);
    }

    /**
     * Orders by the value of the specified operand, in ascending order.
     *
     * @param operand the operand by which to order; non-null
     * @return the ordering
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public Ordering ascending(DynamicOperand operand)
            throws InvalidQueryException, RepositoryException {
        if (!(operand instanceof DynamicOperandImpl)) {
            throw new RepositoryException("Unknown DynamicOperand implementation");
        }
        return new OrderingImpl(resolver, (DynamicOperandImpl) operand,
                QueryObjectModelConstants.JCR_ORDER_ASCENDING);
    }

    /**
     * Orders by the value of the specified operand, in descending order.
     *
     * @param operand the operand by which to order; non-null
     * @return the ordering
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public Ordering descending(DynamicOperand operand)
            throws InvalidQueryException, RepositoryException {
        if (!(operand instanceof DynamicOperandImpl)) {
            throw new RepositoryException("Unknown DynamicOperand implementation");
        }
        return new OrderingImpl(resolver, (DynamicOperandImpl) operand,
                QueryObjectModelConstants.JCR_ORDER_DESCENDING);
    }

    /**
     * Identifies a property in the default selector to include in the tabular
     * view of query results.
     * <p>
     * The column name is the property name.
     *
     * @param propertyName the property name, or null to include a column for
     *                     each single-value non-residual property of the
     *                     selector's node type
     * @return the column; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query has no default
     *                                       selector or is otherwise invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public Column column(String propertyName)                                     // CM
            throws InvalidQueryException, RepositoryException {
        Name propName = null;
        if (propertyName != null) {
            try {
                propName = resolver.getQName(propertyName);
            } catch (NameException e) {
                throw new InvalidQueryException(e.getMessage());
            }
        }
        return new ColumnImpl(resolver, null, propName, propertyName);
    }

    /**
     * Identifies a property in the default selector to include in the tabular
     * view of query results.
     *
     * @param propertyName the property name, or null to include a column for
     *                     each single-value non-residual property of the
     *                     selector's node type
     * @param columnName   the column name; must be null if <code>propertyName</code>
     *                     is null
     * @return the column; non-null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query has no default
     *                                       selector or is otherwise invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public Column column(String propertyName, String columnName)                  // CM
            throws InvalidQueryException, RepositoryException {
        if (propertyName == null && columnName != null) {
            throw new InvalidQueryException(
                    "columnName must be null if propertyName is null");
        }
        Name propName = null;
        if (propertyName != null) {
            try {
                propName = resolver.getQName(propertyName);
            } catch (NameException e) {
                throw new InvalidQueryException(e.getMessage());
            }
        }
        return new ColumnImpl(resolver, null, propName, columnName);
    }

    /**
     * Identifies a property in the specified selector to include in the tabular
     * view of query results.
     *
     * @param selectorName the selector name; non-null
     * @param propertyName the property name, or null to include a column for
     *                     each single-value non-residual property of the
     *                     selector's node type
     * @param columnName   the column name; if null, defaults to
     *                     <code>propertyName</code>; must be null if
     *                     <code>propertyName</code> is null
     * @throws javax.jcr.query.InvalidQueryException
     *                                       if the query is invalid
     * @throws javax.jcr.RepositoryException if the operation otherwise fails
     */
    public Column column(String selectorName,
                         String propertyName,
                         String columnName) throws InvalidQueryException, RepositoryException {
        if (propertyName == null && columnName != null) {
            throw new InvalidQueryException(
                    "columnName must be null if propertyName is null");
        }
        Name propName = null;
        if (propertyName != null) {
            try {
                propName = resolver.getQName(propertyName);
            } catch (NameException e) {
                throw new InvalidQueryException(e.getMessage());
            }
        }
        return new ColumnImpl(resolver, checkSelectorName(selectorName),
                propName, columnName);
    }

    //------------------------------< internal >--------------------------------

    private Name checkSelectorName(String selectorName)
            throws RepositoryException {
        if (selectorName == null) {
            throw new InvalidQueryException("selectorName must not be null");
        }
        try {
            return resolver.getQName(selectorName);
        } catch (NameException e) {
            throw new InvalidQueryException(e.getMessage());
        }
    }

    private Name checkNodeTypeName(String nodeTypeName)
            throws RepositoryException {
        if (nodeTypeName == null) {
            throw new InvalidQueryException("nodeTypeName must not be null");
        }
        try {
            return resolver.getQName(nodeTypeName);
        } catch (NameException e) {
            throw new InvalidQueryException(e.getMessage());
        }
    }

    private Path checkPath(String path) throws RepositoryException {
        if (path == null) {
            throw new InvalidQueryException("path must not be null");
        }
        try {
            return resolver.getQPath(path);
        } catch (NameException e) {
            throw new InvalidQueryException(e.getMessage());
        }
    }

    private Name checkPropertyName(String propertyName)
            throws RepositoryException {
        if (propertyName == null) {
            throw new InvalidQueryException("propertyName must not be null");
        }
        try {
            return resolver.getQName(propertyName);
        } catch (NameException e) {
            throw new InvalidQueryException(e.getMessage());
        }
    }

    private StaticOperand checkFullTextSearchExpression(StaticOperand fullTextSearchExpression)
            throws RepositoryException {
        if (fullTextSearchExpression == null) {
            throw new InvalidQueryException(
                    "fullTextSearchExpression must not be null");
        }
        return fullTextSearchExpression;
    }
}
