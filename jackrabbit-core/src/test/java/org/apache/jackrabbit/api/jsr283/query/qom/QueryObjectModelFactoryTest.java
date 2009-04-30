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
package org.apache.jackrabbit.api.jsr283.query.qom;

import org.apache.jackrabbit.spi.commons.query.jsr283.qom.And;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.PropertyExistence;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.PropertyValue;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.Ordering;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModelConstants;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.BindVariableValue;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.ChildNode;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.ChildNodeJoinCondition;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.Column;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.Comparison;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.DescendantNode;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.DescendantNodeJoinCondition;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.EquiJoinCondition;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.FullTextSearch;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.FullTextSearchScore;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.Join;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.Selector;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.JoinCondition;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.Length;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.LowerCase;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.NodeLocalName;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.NodeName;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.Not;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.Or;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.SameNode;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.SameNodeJoinCondition;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.UpperCase;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModelFactory;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.Constraint;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.DynamicOperand;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.StaticOperand;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.Source;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModel;

import javax.jcr.RepositoryException;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * <code>QueryObjectModelFactoryTest</code> tests all methods on the
 * {@link QueryObjectModelFactory}.
 */
public class QueryObjectModelFactoryTest extends AbstractQOMTest {

    /**
     * A test selector name.
     */
    private static final String SELECTOR_NAME1 = "selector1";

    /**
     * Another test selector name.
     */
    private static final String SELECTOR_NAME2 = "selector2";

    /**
     * A test column name.
     */
    private static final String COLUMN_NAME = "column";

    /**
     * A test variable name.
     */
    private static final String VARIABLE_NAME = "varName";

    /**
     * A test full text search expression
     */
    private static final String FULLTEXT_SEARCH_EXPR = "foo -bar";

    /**
     * Set of all possible operators.
     */
    private static final Set OPERATORS = new HashSet();

    /**
     * Set of all possible join types.
     */
    private static final Set JOIN_TYPES = new HashSet();

    static {
        OPERATORS.add(new Integer(QueryObjectModelConstants.OPERATOR_EQUAL_TO));
        OPERATORS.add(new Integer(QueryObjectModelConstants.OPERATOR_GREATER_THAN));
        OPERATORS.add(new Integer(QueryObjectModelConstants.OPERATOR_GREATER_THAN_OR_EQUAL_TO));
        OPERATORS.add(new Integer(QueryObjectModelConstants.OPERATOR_LESS_THAN));
        OPERATORS.add(new Integer(QueryObjectModelConstants.OPERATOR_LESS_THAN_OR_EQUAL_TO));
        OPERATORS.add(new Integer(QueryObjectModelConstants.OPERATOR_LIKE));
        OPERATORS.add(new Integer(QueryObjectModelConstants.OPERATOR_NOT_EQUAL_TO));

        JOIN_TYPES.add(new Integer(QueryObjectModelConstants.JOIN_TYPE_INNER));
        JOIN_TYPES.add(new Integer(QueryObjectModelConstants.JOIN_TYPE_LEFT_OUTER));
        JOIN_TYPES.add(new Integer(QueryObjectModelConstants.JOIN_TYPE_RIGHT_OUTER));
    }

    /**
     * Test case for {@link QueryObjectModelFactory#and(Constraint, Constraint)}
     */
    public void testAnd() throws RepositoryException {
        PropertyExistence c1 = qomFactory.propertyExistence(propertyName1);
        PropertyExistence c2 = qomFactory.propertyExistence(propertyName2);
        And and = qomFactory.and(c1, c2);
        assertTrue("Not a PropertyExistence constraint",
                and.getConstraint1() instanceof PropertyExistence);
        assertTrue("Not a PropertyExistence constraint",
                and.getConstraint2() instanceof PropertyExistence);
    }

    /**
     * Test case for {@link QueryObjectModelFactory#ascending(DynamicOperand)}
     */
    public void testOrderingAscending() throws RepositoryException {
        PropertyValue op = qomFactory.propertyValue(propertyName1);
        Ordering asc = qomFactory.ascending(op);
        assertEquals("Ordering.getOrder() must return QueryObjectModelConstants.ORDER_ASCENDING",
                QueryObjectModelConstants.ORDER_ASCENDING, asc.getOrder());
        assertTrue("Not a PropertyValue operand", asc.getOperand() instanceof PropertyValue);
    }

    /**
     * Test case for {@link QueryObjectModelFactory#bindVariable(String)}
     */
    public void testBindVariableValue() throws RepositoryException {
        BindVariableValue bindVar = qomFactory.bindVariable(propertyName1);
        assertEquals("Wrong variable name", propertyName1, bindVar.getBindVariableName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#childNode(String)}
     */
    public void testChildNode() throws RepositoryException {
        ChildNode childNode = qomFactory.childNode(testRootNode.getPath());
        assertEquals("Wrong path", testRootNode.getPath(), childNode.getPath());
        assertNull("Selector must be null", childNode.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#childNode(String, String)}
     */
    public void testChildNodeWithSelector() throws RepositoryException {
        ChildNode childNode = qomFactory.childNode(SELECTOR_NAME1, testRootNode.getPath());
        assertEquals("Wrong path", testRootNode.getPath(), childNode.getPath());
        assertEquals("Wrong selector name", SELECTOR_NAME1, childNode.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#childNodeJoinCondition(String, String)}
     */
    public void testChildNodeJoinCondition() throws RepositoryException {
        ChildNodeJoinCondition cond = qomFactory.childNodeJoinCondition(SELECTOR_NAME1, SELECTOR_NAME2);
        assertEquals("Wrong selector name", cond.getChildSelectorName(), SELECTOR_NAME1);
        assertEquals("Wrong selector name", cond.getParentSelectorName(), SELECTOR_NAME2);
    }

    /**
     * Test case for {@link QueryObjectModelFactory#column(String)}
     */
    public void testColumn() throws RepositoryException {
        Column col = qomFactory.column(propertyName1);
        assertNull("Selector must be null", col.getSelectorName());
        assertEquals("Wrong property name", propertyName1, col.getPropertyName());
        assertEquals("Wrong column name", propertyName1, col.getColumnName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#column(String)}
     */
    public void testColumnAllProperties() throws RepositoryException {
        Column col = qomFactory.column(null);
        assertNull("Selector must be null", col.getSelectorName());
        assertNull("Property name must be null", col.getPropertyName());
        assertNull("Column name must be null", col.getColumnName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#column(String, String)}
     */
    public void testColumnWithColumnName() throws RepositoryException {
        Column col = qomFactory.column(propertyName1, COLUMN_NAME);
        assertNull("Selector must be null", col.getSelectorName());
        assertEquals("Wrong property name", propertyName1, col.getPropertyName());
        assertEquals("Wrong column name", COLUMN_NAME, col.getColumnName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#column(String, String, String)}
     */
    public void testColumnWithSelector() throws RepositoryException {
        Column col = qomFactory.column(SELECTOR_NAME1, propertyName1, COLUMN_NAME);
        assertEquals("Wrong selector name", SELECTOR_NAME1, col.getSelectorName());
        assertEquals("Wrong property name", propertyName1, col.getPropertyName());
        assertEquals("Wrong column name", COLUMN_NAME, col.getColumnName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#comparison(DynamicOperand, int, StaticOperand)}
     */
    public void testComparison() throws RepositoryException {
        PropertyValue op1 = qomFactory.propertyValue(propertyName1);
        BindVariableValue op2 = qomFactory.bindVariable(VARIABLE_NAME);
        for (Iterator it = OPERATORS.iterator(); it.hasNext(); ) {
            int operator = ((Integer) it.next()).intValue();
            Comparison comp = qomFactory.comparison(op1, operator, op2);
            assertTrue("Not a PropertyValue operand", comp.getOperand1() instanceof PropertyValue);
            assertTrue("Not a BindVariableValue operand", comp.getOperand2() instanceof BindVariableValue);
            assertEquals("Wrong operator", operator, comp.getOperator());
        }
    }

    public void testCreateQuery() throws RepositoryException {
        Selector selector = qomFactory.selector(testNodeType);
        QueryObjectModel qom = qomFactory.createQuery(selector, null, null, null);
        assertTrue("Not a selector source", qom.getSource() instanceof Selector);
        assertNull("Constraint must be null", qom.getConstraint());
        assertEquals("Wrong size of orderings", 0, qom.getOrderings().length);
        assertEquals("Wrong size of columns", 0, qom.getColumns().length);
    }

    public void testCreateQueryWithConstraint() throws RepositoryException {
        Selector selector = qomFactory.selector(testNodeType);
        PropertyExistence propExist = qomFactory.propertyExistence(propertyName1);
        QueryObjectModel qom = qomFactory.createQuery(
                selector, propExist, null, null);
        assertTrue("Not a selector source", qom.getSource() instanceof Selector);
        assertTrue("Not a property existence constraint", qom.getConstraint() instanceof PropertyExistence);
        assertEquals("Wrong size of orderings", 0, qom.getOrderings().length);
        assertEquals("Wrong size of columns", 0, qom.getColumns().length);
    }

    public void testCreateQueryWithConstraintAndOrdering() throws RepositoryException {
        Selector selector = qomFactory.selector(testNodeType);
        PropertyExistence propExist = qomFactory.propertyExistence(propertyName1);
        PropertyValue propValue = qomFactory.propertyValue(propertyName1);
        Ordering ordering = qomFactory.ascending(propValue);
        QueryObjectModel qom = qomFactory.createQuery(selector, propExist,
                new Ordering[]{ordering}, null);
        assertTrue("Not a selector source", qom.getSource() instanceof Selector);
        assertTrue("Not a property existence constraint", qom.getConstraint() instanceof PropertyExistence);
        assertEquals("Wrong size of orderings", 1, qom.getOrderings().length);
        assertEquals("Wrong size of columns", 0, qom.getColumns().length);
    }

    public void testCreateQueryWithConstraintOrderingAndColumn() throws RepositoryException {
        Selector selector = qomFactory.selector(testNodeType);
        PropertyExistence propExist = qomFactory.propertyExistence(propertyName1);
        PropertyValue propValue = qomFactory.propertyValue(propertyName1);
        Ordering ordering = qomFactory.ascending(propValue);
        Column column = qomFactory.column(propertyName1);
        QueryObjectModel qom = qomFactory.createQuery(selector, propExist,
                new Ordering[]{ordering}, new Column[]{column});
        assertTrue("Not a selector source", qom.getSource() instanceof Selector);
        assertTrue("Not a property existence constraint", qom.getConstraint() instanceof PropertyExistence);
        assertEquals("Wrong size of orderings", 1, qom.getOrderings().length);
        assertEquals("Wrong size of columns", 1, qom.getColumns().length);
    }

    public void testCreateQueryFromSource() throws RepositoryException {
        Source selector = qomFactory.selector(testNodeType);
        QueryObjectModel qom = qomFactory.createQuery(selector, null, null, null);
        assertTrue("Not a selector source", qom.getSource() instanceof Selector);
        assertNull("Constraint must be null", qom.getConstraint());
        assertEquals("Wrong size of orderings", 0, qom.getOrderings().length);
        assertEquals("Wrong size of columns", 0, qom.getColumns().length);
    }

    public void testCreateQueryFromSourceWithConstraint() throws RepositoryException {
        Source selector = qomFactory.selector(testNodeType);
        PropertyExistence propExist = qomFactory.propertyExistence(propertyName1);
        QueryObjectModel qom = qomFactory.createQuery(
                selector, propExist, null, null);
        assertTrue("Not a selector source", qom.getSource() instanceof Selector);
        assertTrue("Not a property existence constraint", qom.getConstraint() instanceof PropertyExistence);
        assertEquals("Wrong size of orderings", 0, qom.getOrderings().length);
        assertEquals("Wrong size of columns", 0, qom.getColumns().length);
    }

    public void testCreateQueryFromSourceWithConstraintAndOrdering() throws RepositoryException {
        Source selector = qomFactory.selector(testNodeType);
        PropertyExistence propExist = qomFactory.propertyExistence(propertyName1);
        PropertyValue propValue = qomFactory.propertyValue(propertyName1);
        Ordering ordering = qomFactory.ascending(propValue);
        QueryObjectModel qom = qomFactory.createQuery(selector, propExist,
                new Ordering[]{ordering}, null);
        assertTrue("Not a selector source", qom.getSource() instanceof Selector);
        assertTrue("Not a property existence constraint", qom.getConstraint() instanceof PropertyExistence);
        assertEquals("Wrong size of orderings", 1, qom.getOrderings().length);
        assertEquals("Wrong size of columns", 0, qom.getColumns().length);
    }

    public void testCreateQueryFromSourceWithConstraintOrderingAndColumn() throws RepositoryException {
        Source selector = qomFactory.selector(testNodeType);
        PropertyExistence propExist = qomFactory.propertyExistence(propertyName1);
        PropertyValue propValue = qomFactory.propertyValue(propertyName1);
        Ordering ordering = qomFactory.ascending(propValue);
        Column column = qomFactory.column(propertyName1);
        QueryObjectModel qom = qomFactory.createQuery(selector, propExist,
                new Ordering[]{ordering}, new Column[]{column});
        assertTrue("Not a selector source", qom.getSource() instanceof Selector);
        assertTrue("Not a property existence constraint", qom.getConstraint() instanceof PropertyExistence);
        assertEquals("Wrong size of orderings", 1, qom.getOrderings().length);
        assertEquals("Wrong size of columns", 1, qom.getColumns().length);
    }

    /**
     * Test case for {@link QueryObjectModelFactory#descendantNode(String)}
     */
    public void testDescendantNode() throws RepositoryException {
        DescendantNode descNode = qomFactory.descendantNode(testRootNode.getPath());
        assertNull("Selector must be null", descNode.getSelectorName());
        assertEquals("Wrong path", testRootNode.getPath(), descNode.getPath());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#descendantNode(String, String)}
     */
    public void testDescendantNodeWithSelector() throws RepositoryException {
        DescendantNode descNode = qomFactory.descendantNode(SELECTOR_NAME1, testRootNode.getPath());
        assertEquals("Wrong selector name", SELECTOR_NAME1, descNode.getSelectorName());
        assertEquals("Wrong path", testRootNode.getPath(), descNode.getPath());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#descendantNodeJoinCondition(String, String)}
     */
    public void testDescendantNodeJoinCondition() throws RepositoryException {
        DescendantNodeJoinCondition cond = qomFactory.descendantNodeJoinCondition(SELECTOR_NAME1, SELECTOR_NAME2);
        assertEquals("Wrong selector name", SELECTOR_NAME1, cond.getDescendantSelectorName());
        assertEquals("Wrong selector name", SELECTOR_NAME2, cond.getAncestorSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#descending(DynamicOperand)}
     */
    public void testOrderingDescending() throws RepositoryException {
        PropertyValue op = qomFactory.propertyValue(propertyName1);
        Ordering desc = qomFactory.descending(op);
        assertEquals("Ordering.getOrder() must return QueryObjectModelConstants.ORDER_DESCENDING",
                QueryObjectModelConstants.ORDER_DESCENDING, desc.getOrder());
        assertTrue("Not a PropertyValue operand", desc.getOperand() instanceof PropertyValue);
    }

    /**
     * Test case for {@link QueryObjectModelFactory#equiJoinCondition(String, String, String, String)}
     */
    public void testEquiJoinCondition() throws RepositoryException {
        EquiJoinCondition cond = qomFactory.equiJoinCondition(SELECTOR_NAME1, propertyName1, SELECTOR_NAME2, propertyName2);
        assertEquals("Wrong selector name", SELECTOR_NAME1, cond.getSelector1Name());
        assertEquals("Wrong property name", propertyName1, cond.getProperty1Name());
        assertEquals("Wrong selector name", SELECTOR_NAME2, cond.getSelector2Name());
        assertEquals("Wrong property name", propertyName2, cond.getProperty2Name());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#fullTextSearch(String, String)}
     */
    public void testFullTextSearch() throws RepositoryException {
        FullTextSearch ftSearch = qomFactory.fullTextSearch(propertyName1, FULLTEXT_SEARCH_EXPR);
        assertNull("Selector must be null", ftSearch.getSelectorName());
        assertEquals("Wrong propertyName", propertyName1, ftSearch.getPropertyName());
        assertEquals("Wrong fulltext search expression", FULLTEXT_SEARCH_EXPR, ftSearch.getFullTextSearchExpression());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#fullTextSearch(String, String)}
     */
    public void testFullTextSearchAllProperties() throws RepositoryException {
        FullTextSearch ftSearch = qomFactory.fullTextSearch(null, FULLTEXT_SEARCH_EXPR);
        assertNull("Selector must be null", ftSearch.getSelectorName());
        assertNull("Property name must be null", ftSearch.getPropertyName());
        assertEquals("Wrong fulltext search expression", FULLTEXT_SEARCH_EXPR, ftSearch.getFullTextSearchExpression());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#fullTextSearch(String, String, String)}
     */
    public void testFullTextSearchWithSelector() throws RepositoryException {
        FullTextSearch ftSearch = qomFactory.fullTextSearch(SELECTOR_NAME1, propertyName1, FULLTEXT_SEARCH_EXPR);
        assertEquals("Wrong selector name", SELECTOR_NAME1, ftSearch.getSelectorName());
        assertEquals("Wrong propertyName", propertyName1, ftSearch.getPropertyName());
        assertEquals("Wrong fulltext search expression", FULLTEXT_SEARCH_EXPR, ftSearch.getFullTextSearchExpression());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#fullTextSearchScore()}
     */
    public void testFullTextSearchScore() throws RepositoryException {
        FullTextSearchScore score = qomFactory.fullTextSearchScore();
        assertNull("Selector must be null", score.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#fullTextSearchScore(String)}
     */
    public void testFullTextSearchScoreWithSelector() throws RepositoryException {
        FullTextSearchScore score = qomFactory.fullTextSearchScore(SELECTOR_NAME1);
        assertEquals("Wrong selector name", SELECTOR_NAME1, score.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#join(Source, Source, int, JoinCondition)}
     */
    public void testJoin() throws RepositoryException {
        Selector s1 = qomFactory.selector(ntBase);
        Selector s2 = qomFactory.selector(testNodeType);
        JoinCondition cond = qomFactory.equiJoinCondition(ntBase, jcrPrimaryType, testNodeType, jcrPrimaryType);
        for (Iterator it = JOIN_TYPES.iterator(); it.hasNext(); ) {
            int joinType = ((Integer) it.next()).intValue();
            Join join = qomFactory.join(s1, s2, joinType, cond);
            assertTrue("Not a selector source", join.getLeft() instanceof Selector);
            assertTrue("Not a selector source", join.getRight() instanceof Selector);
            assertEquals("Wrong join type", joinType, join.getJoinType());
            assertTrue("Not an EquiJoinCondition", join.getJoinCondition() instanceof EquiJoinCondition);
        }
    }

    /**
     * Test case for {@link QueryObjectModelFactory#length(PropertyValue)}
     */
    public void testLength() throws RepositoryException {
        PropertyValue propValue = qomFactory.propertyValue(propertyName1);
        Length len = qomFactory.length(propValue);
        assertNotNull("Property value must not be null", len.getPropertyValue());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#lowerCase(DynamicOperand)}
     */
    public void testLowerCase() throws RepositoryException {
        PropertyValue propValue = qomFactory.propertyValue(propertyName1);
        LowerCase lower = qomFactory.lowerCase(propValue);
        assertTrue("Not a property value operand", lower.getOperand() instanceof PropertyValue);
    }

    /**
     * Test case for {@link QueryObjectModelFactory#nodeLocalName()}
     */
    public void testNodeLocalName() throws RepositoryException {
        NodeLocalName localName = qomFactory.nodeLocalName();
        assertNull("Selector name must be null", localName.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#nodeLocalName(String)}
     */
    public void testNodeLocalNameWithSelector() throws RepositoryException {
        NodeLocalName localName = qomFactory.nodeLocalName(SELECTOR_NAME1);
        assertEquals("Wrong selector name", SELECTOR_NAME1, localName.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#nodeName()}
     */
    public void testNodeName() throws RepositoryException {
        NodeName nodeName = qomFactory.nodeName();
        assertNull("Selector name must be null", nodeName.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#nodeName(String)}
     */
    public void testNodeNameWithSelector() throws RepositoryException {
        NodeName nodeName = qomFactory.nodeName(SELECTOR_NAME1);
        assertEquals("Wrong selector name", SELECTOR_NAME1, nodeName.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#not(Constraint)}
     */
    public void testNot() throws RepositoryException {
        PropertyExistence propExist = qomFactory.propertyExistence(propertyName1);
        Not not = qomFactory.not(propExist);
        assertTrue("Not a property existence constraint", not.getConstraint() instanceof PropertyExistence);
    }

    /**
     * Test case for {@link QueryObjectModelFactory#or(Constraint, Constraint)}
     */
    public void testOr() throws RepositoryException {
        PropertyExistence c1 = qomFactory.propertyExistence(propertyName1);
        PropertyExistence c2 = qomFactory.propertyExistence(propertyName2);
        Or or = qomFactory.or(c1, c2);
        assertTrue("Not a PropertyExistence constraint",
                or.getConstraint1() instanceof PropertyExistence);
        assertTrue("Not a PropertyExistence constraint",
                or.getConstraint2() instanceof PropertyExistence);
    }

    /**
     * Test case for {@link QueryObjectModelFactory#propertyExistence(String)}
     */
    public void testPropertyExistence() throws RepositoryException {
        PropertyExistence propExist = qomFactory.propertyExistence(propertyName1);
        assertNull("Selector name must be null", propExist.getSelectorName());
        assertEquals("Wrong property name", propertyName1, propExist.getPropertyName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#propertyExistence(String, String)}
     */
    public void testPropertyExistenceWithSelector() throws RepositoryException {
        PropertyExistence propExist = qomFactory.propertyExistence(SELECTOR_NAME1, propertyName1);
        assertEquals("Wrong selector name", SELECTOR_NAME1, propExist.getSelectorName());
        assertEquals("Wrong property name", propertyName1, propExist.getPropertyName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#propertyValue(String)}
     */
    public void testPropertyValue() throws RepositoryException {
        PropertyValue propVal = qomFactory.propertyValue(propertyName1);
        assertNull("Selector name must be null", propVal.getSelectorName());
        assertEquals("Wrong property name", propertyName1, propVal.getPropertyName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#propertyValue(String, String)}
     */
    public void testPropertyValueWithSelector() throws RepositoryException {
        PropertyValue propVal = qomFactory.propertyValue(SELECTOR_NAME1, propertyName1);
        assertEquals("Wrong selector name", SELECTOR_NAME1, propVal.getSelectorName());
        assertEquals("Wrong property name", propertyName1, propVal.getPropertyName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#sameNode(String)}
     */
    public void testSameNode() throws RepositoryException {
        SameNode sameNode = qomFactory.sameNode(testRootNode.getPath());
        assertNull("Selector name must be null", sameNode.getSelectorName());
        assertEquals("Wrong path", testRootNode.getPath(), sameNode.getPath());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#sameNode(String, String)}
     */
    public void testSameNodeWithSelector() throws RepositoryException {
        SameNode sameNode = qomFactory.sameNode(SELECTOR_NAME1, testRootNode.getPath());
        assertEquals("Wrong selector name", SELECTOR_NAME1, sameNode.getSelectorName());
        assertEquals("Wrong path", testRootNode.getPath(), sameNode.getPath());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#sameNodeJoinCondition(String, String)}
     */
    public void testSameNodeJoinCondition() throws RepositoryException {
        SameNodeJoinCondition cond = qomFactory.sameNodeJoinCondition(SELECTOR_NAME1, SELECTOR_NAME2);
        assertEquals("Wrong selector name", SELECTOR_NAME1, cond.getSelector1Name());
        assertEquals("Wrong selector name", SELECTOR_NAME2, cond.getSelector2Name());
        assertNull("Path must be null", cond.getSelector2Path());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#sameNodeJoinCondition(String, String, String)}
     */
    public void testSameNodeJoinConditionWithPath() throws RepositoryException {
        SameNodeJoinCondition cond = qomFactory.sameNodeJoinCondition(SELECTOR_NAME1, SELECTOR_NAME2, nodeName1);
        assertEquals("Wrong selector name", SELECTOR_NAME1, cond.getSelector1Name());
        assertEquals("Wrong selector name", SELECTOR_NAME2, cond.getSelector2Name());
        assertEquals("Wrong path", nodeName1, cond.getSelector2Path());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#selector(String)}
     */
    public void testSelector() throws RepositoryException {
        Selector selector = qomFactory.selector(ntBase);
        assertEquals("Wrong node type name", ntBase, selector.getNodeTypeName());
        assertEquals("Wrong selector name", ntBase, selector.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#selector(String, String)}
     */
    public void testSelectorWithName() throws RepositoryException {
        Selector selector = qomFactory.selector(ntBase, SELECTOR_NAME1);
        assertEquals("Wrong node type name", ntBase, selector.getNodeTypeName());
        assertEquals("Wrong selector name", SELECTOR_NAME1, selector.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#upperCase(DynamicOperand)}
     */
    public void testUpperCase() throws RepositoryException {
        PropertyValue propValue = qomFactory.propertyValue(propertyName1);
        UpperCase upper = qomFactory.upperCase(propValue);
        assertTrue("Not a property value operand", upper.getOperand() instanceof PropertyValue);
    }
}
