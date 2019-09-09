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
package org.apache.jackrabbit.test.api.query.qom;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
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
import javax.jcr.query.qom.Literal;

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
    private static final Set<String> OPERATORS = new HashSet<String>();

    /**
     * Set of all possible join types.
     */
    private static final Set<String> JOIN_TYPES = new HashSet<String>();

    static {
        OPERATORS.add(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO);
        OPERATORS.add(QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN);
        OPERATORS.add(QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO);
        OPERATORS.add(QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN);
        OPERATORS.add(QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO);
        OPERATORS.add(QueryObjectModelConstants.JCR_OPERATOR_LIKE);
        OPERATORS.add(QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO);

        JOIN_TYPES.add(QueryObjectModelConstants.JCR_JOIN_TYPE_INNER);
        JOIN_TYPES.add(QueryObjectModelConstants.JCR_JOIN_TYPE_LEFT_OUTER);
        JOIN_TYPES.add(QueryObjectModelConstants.JCR_JOIN_TYPE_RIGHT_OUTER);
    }

    /**
     * Test case for {@link QueryObjectModelFactory#and(Constraint, Constraint)}
     */
    public void testAnd() throws RepositoryException {
        PropertyExistence c1 = qf.propertyExistence(SELECTOR_NAME1, propertyName1);
        PropertyExistence c2 = qf.propertyExistence(SELECTOR_NAME1, propertyName2);
        And and = qf.and(c1, c2);
        assertTrue("Not a PropertyExistence constraint",
                and.getConstraint1() instanceof PropertyExistence);
        assertTrue("Not a PropertyExistence constraint",
                and.getConstraint2() instanceof PropertyExistence);
    }

    /**
     * Test case for {@link QueryObjectModelFactory#ascending(DynamicOperand)}
     */
    public void testOrderingAscending() throws RepositoryException {
        PropertyValue op = qf.propertyValue(SELECTOR_NAME1, propertyName1);
        Ordering asc = qf.ascending(op);
        assertEquals("Ordering.getOrder() must return QueryObjectModelConstants.ORDER_ASCENDING",
                QueryObjectModelConstants.JCR_ORDER_ASCENDING, asc.getOrder());
        assertTrue("Not a PropertyValue operand", asc.getOperand() instanceof PropertyValue);
    }

    /**
     * Test case for {@link QueryObjectModelFactory#bindVariable(String)}
     */
    public void testBindVariableValue() throws RepositoryException {
        BindVariableValue bindVar = qf.bindVariable(propertyName1);
        assertEquals("Wrong variable name", propertyName1, bindVar.getBindVariableName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#childNode(String, String)}
     */
    public void testChildNode() throws RepositoryException {
        ChildNode childNode = qf.childNode(SELECTOR_NAME1, testRootNode.getPath());
        assertEquals("Wrong path", testRootNode.getPath(), childNode.getParentPath());
        assertEquals("Wrong selector name", SELECTOR_NAME1, childNode.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#childNode(String, String)}
     */
    public void testChildNodeWithSelector() throws RepositoryException {
        ChildNode childNode = qf.childNode(SELECTOR_NAME1, testRootNode.getPath());
        assertEquals("Wrong path", testRootNode.getPath(), childNode.getParentPath());
        assertEquals("Wrong selector name", SELECTOR_NAME1, childNode.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#childNodeJoinCondition(String, String)}
     */
    public void testChildNodeJoinCondition() throws RepositoryException {
        ChildNodeJoinCondition cond = qf.childNodeJoinCondition(SELECTOR_NAME1, SELECTOR_NAME2);
        assertEquals("Wrong selector name", cond.getChildSelectorName(), SELECTOR_NAME1);
        assertEquals("Wrong selector name", cond.getParentSelectorName(), SELECTOR_NAME2);
    }

    /**
     * Test case for {@link QueryObjectModelFactory#column(String, String, String)}
     */
    public void testColumn() throws RepositoryException {
        Column col = qf.column(SELECTOR_NAME1, propertyName1, propertyName1);
        assertEquals("Wrong selector name", SELECTOR_NAME1, col.getSelectorName());
        assertEquals("Wrong property name", propertyName1, col.getPropertyName());
        assertEquals("Wrong column name", propertyName1, col.getColumnName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#column(String, String, String)}
     */
    public void testColumnAllProperties() throws RepositoryException {
        Column col = qf.column(SELECTOR_NAME1, null, null);
        assertEquals("Wrong selector name", SELECTOR_NAME1, col.getSelectorName());
        assertNull("Property name must be null", col.getPropertyName());
        assertNull("Column name must be null", col.getColumnName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#column(String, String, String)}
     */
    public void testColumnWithColumnName() throws RepositoryException {
        Column col = qf.column(SELECTOR_NAME1, propertyName1, COLUMN_NAME);
        assertEquals("Wrong selector name", SELECTOR_NAME1, col.getSelectorName());
        assertEquals("Wrong property name", propertyName1, col.getPropertyName());
        assertEquals("Wrong column name", COLUMN_NAME, col.getColumnName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#column(String, String, String)}
     */
    public void testColumnWithSelector() throws RepositoryException {
        Column col = qf.column(SELECTOR_NAME1, propertyName1, COLUMN_NAME);
        assertEquals("Wrong selector name", SELECTOR_NAME1, col.getSelectorName());
        assertEquals("Wrong property name", propertyName1, col.getPropertyName());
        assertEquals("Wrong column name", COLUMN_NAME, col.getColumnName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#comparison(DynamicOperand, String, StaticOperand)}
     */
    public void testComparison() throws RepositoryException {
        PropertyValue op1 = qf.propertyValue(SELECTOR_NAME1, propertyName1);
        BindVariableValue op2 = qf.bindVariable(VARIABLE_NAME);
        for (Iterator<String> it = OPERATORS.iterator(); it.hasNext(); ) {
            String operator = it.next();
            Comparison comp = qf.comparison(op1, operator, op2);
            assertTrue("Not a PropertyValue operand", comp.getOperand1() instanceof PropertyValue);
            assertTrue("Not a BindVariableValue operand", comp.getOperand2() instanceof BindVariableValue);
            assertEquals("Wrong operator", operator, comp.getOperator());
        }
    }

    public void testCreateQuery() throws RepositoryException {
        Selector selector = qf.selector(testNodeType, SELECTOR_NAME1);
        QueryObjectModel qom = qf.createQuery(selector, null, null, null);
        assertTrue("Not a selector source", qom.getSource() instanceof Selector);
        assertNull("Constraint must be null", qom.getConstraint());
        assertEquals("Wrong size of orderings", 0, qom.getOrderings().length);
        assertEquals("Wrong size of columns", 0, qom.getColumns().length);
    }

    public void testCreateQueryWithConstraint() throws RepositoryException {
        Selector selector = qf.selector(testNodeType, SELECTOR_NAME1);
        PropertyExistence propExist = qf.propertyExistence(SELECTOR_NAME1, propertyName1);
        QueryObjectModel qom = qf.createQuery(
                selector, propExist, null, null);
        assertTrue("Not a selector source", qom.getSource() instanceof Selector);
        assertTrue("Not a property existence constraint", qom.getConstraint() instanceof PropertyExistence);
        assertEquals("Wrong size of orderings", 0, qom.getOrderings().length);
        assertEquals("Wrong size of columns", 0, qom.getColumns().length);
    }

    public void testCreateQueryWithConstraintAndOrdering() throws RepositoryException {
        Selector selector = qf.selector(testNodeType, SELECTOR_NAME1);
        PropertyExistence propExist = qf.propertyExistence(SELECTOR_NAME1, propertyName1);
        PropertyValue propValue = qf.propertyValue(SELECTOR_NAME1, propertyName1);
        Ordering ordering = qf.ascending(propValue);
        QueryObjectModel qom = qf.createQuery(selector, propExist,
                new Ordering[]{ordering}, null);
        assertTrue("Not a selector source", qom.getSource() instanceof Selector);
        assertTrue("Not a property existence constraint", qom.getConstraint() instanceof PropertyExistence);
        assertEquals("Wrong size of orderings", 1, qom.getOrderings().length);
        assertEquals("Wrong size of columns", 0, qom.getColumns().length);
    }

    public void testCreateQueryWithConstraintOrderingAndColumn() throws RepositoryException {
        Selector selector = qf.selector(testNodeType, SELECTOR_NAME1);
        PropertyExistence propExist = qf.propertyExistence(SELECTOR_NAME1, propertyName1);
        PropertyValue propValue = qf.propertyValue(SELECTOR_NAME1, propertyName1);
        Ordering ordering = qf.ascending(propValue);
        Column column = qf.column(SELECTOR_NAME1, propertyName1, propertyName1);
        QueryObjectModel qom = qf.createQuery(selector, propExist,
                new Ordering[]{ordering}, new Column[]{column});
        assertTrue("Not a selector source", qom.getSource() instanceof Selector);
        assertTrue("Not a property existence constraint", qom.getConstraint() instanceof PropertyExistence);
        assertEquals("Wrong size of orderings", 1, qom.getOrderings().length);
        assertEquals("Wrong size of columns", 1, qom.getColumns().length);
    }

    public void testCreateQueryFromSource() throws RepositoryException {
        Source selector = qf.selector(testNodeType, SELECTOR_NAME1);
        QueryObjectModel qom = qf.createQuery(selector, null, null, null);
        assertTrue("Not a selector source", qom.getSource() instanceof Selector);
        assertNull("Constraint must be null", qom.getConstraint());
        assertEquals("Wrong size of orderings", 0, qom.getOrderings().length);
        assertEquals("Wrong size of columns", 0, qom.getColumns().length);
    }

    public void testCreateQueryFromSourceWithConstraint() throws RepositoryException {
        Source selector = qf.selector(testNodeType, SELECTOR_NAME1);
        PropertyExistence propExist = qf.propertyExistence(SELECTOR_NAME1, propertyName1);
        QueryObjectModel qom = qf.createQuery(
                selector, propExist, null, null);
        assertTrue("Not a selector source", qom.getSource() instanceof Selector);
        assertTrue("Not a property existence constraint", qom.getConstraint() instanceof PropertyExistence);
        assertEquals("Wrong size of orderings", 0, qom.getOrderings().length);
        assertEquals("Wrong size of columns", 0, qom.getColumns().length);
    }

    public void testCreateQueryFromSourceWithConstraintAndOrdering() throws RepositoryException {
        Source selector = qf.selector(testNodeType, SELECTOR_NAME1);
        PropertyExistence propExist = qf.propertyExistence(SELECTOR_NAME1, propertyName1);
        PropertyValue propValue = qf.propertyValue(SELECTOR_NAME1, propertyName1);
        Ordering ordering = qf.ascending(propValue);
        QueryObjectModel qom = qf.createQuery(selector, propExist,
                new Ordering[]{ordering}, null);
        assertTrue("Not a selector source", qom.getSource() instanceof Selector);
        assertTrue("Not a property existence constraint", qom.getConstraint() instanceof PropertyExistence);
        assertEquals("Wrong size of orderings", 1, qom.getOrderings().length);
        assertEquals("Wrong size of columns", 0, qom.getColumns().length);
    }

    public void testCreateQueryFromSourceWithConstraintOrderingAndColumn() throws RepositoryException {
        Source selector = qf.selector(testNodeType, SELECTOR_NAME1);
        PropertyExistence propExist = qf.propertyExistence(SELECTOR_NAME1, propertyName1);
        PropertyValue propValue = qf.propertyValue(SELECTOR_NAME1, propertyName1);
        Ordering ordering = qf.ascending(propValue);
        Column column = qf.column(SELECTOR_NAME1, propertyName1, propertyName1);
        QueryObjectModel qom = qf.createQuery(selector, propExist,
                new Ordering[]{ordering}, new Column[]{column});
        assertTrue("Not a selector source", qom.getSource() instanceof Selector);
        assertTrue("Not a property existence constraint", qom.getConstraint() instanceof PropertyExistence);
        assertEquals("Wrong size of orderings", 1, qom.getOrderings().length);
        assertEquals("Wrong size of columns", 1, qom.getColumns().length);
    }

    /**
     * Test case for {@link QueryObjectModelFactory#descendantNode(String, String)}
     */
    public void testDescendantNode() throws RepositoryException {
        DescendantNode descNode = qf.descendantNode(SELECTOR_NAME1, testRootNode.getPath());
        assertEquals("Wrong selector", SELECTOR_NAME1, descNode.getSelectorName());
        assertEquals("Wrong path", testRootNode.getPath(), descNode.getAncestorPath());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#descendantNode(String, String)}
     */
    public void testDescendantNodeWithSelector() throws RepositoryException {
        DescendantNode descNode = qf.descendantNode(SELECTOR_NAME1, testRootNode.getPath());
        assertEquals("Wrong selector name", SELECTOR_NAME1, descNode.getSelectorName());
        assertEquals("Wrong path", testRootNode.getPath(), descNode.getAncestorPath());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#descendantNodeJoinCondition(String, String)}
     */
    public void testDescendantNodeJoinCondition() throws RepositoryException {
        DescendantNodeJoinCondition cond = qf.descendantNodeJoinCondition(SELECTOR_NAME1, SELECTOR_NAME2);
        assertEquals("Wrong selector name", SELECTOR_NAME1, cond.getDescendantSelectorName());
        assertEquals("Wrong selector name", SELECTOR_NAME2, cond.getAncestorSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#descending(DynamicOperand)}
     */
    public void testOrderingDescending() throws RepositoryException {
        PropertyValue op = qf.propertyValue(SELECTOR_NAME1, propertyName1);
        Ordering desc = qf.descending(op);
        assertEquals("Ordering.getOrder() must return QueryObjectModelConstants.ORDER_DESCENDING",
                QueryObjectModelConstants.JCR_ORDER_DESCENDING, desc.getOrder());
        assertTrue("Not a PropertyValue operand", desc.getOperand() instanceof PropertyValue);
    }

    /**
     * Test case for {@link QueryObjectModelFactory#equiJoinCondition(String, String, String, String)}
     */
    public void testEquiJoinCondition() throws RepositoryException {
        EquiJoinCondition cond = qf.equiJoinCondition(SELECTOR_NAME1, propertyName1, SELECTOR_NAME2, propertyName2);
        assertEquals("Wrong selector name", SELECTOR_NAME1, cond.getSelector1Name());
        assertEquals("Wrong property name", propertyName1, cond.getProperty1Name());
        assertEquals("Wrong selector name", SELECTOR_NAME2, cond.getSelector2Name());
        assertEquals("Wrong property name", propertyName2, cond.getProperty2Name());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#fullTextSearch(String, String, StaticOperand)}
     */
    public void testFullTextSearch() throws RepositoryException {
        FullTextSearch ftSearch = qf.fullTextSearch(
                SELECTOR_NAME1, propertyName1,
                qf.literal(vf.createValue(FULLTEXT_SEARCH_EXPR)));
        assertEquals("Wrong selector name", SELECTOR_NAME1, ftSearch.getSelectorName());
        assertEquals("Wrong propertyName", propertyName1, ftSearch.getPropertyName());

        StaticOperand op = ftSearch.getFullTextSearchExpression();
        assertNotNull(op);
        assertTrue("not a Literal", op instanceof Literal);
        Literal literal = (Literal) op;
        assertEquals(FULLTEXT_SEARCH_EXPR, literal.getLiteralValue().getString());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#fullTextSearch(String, String, StaticOperand)}
     */
    public void testFullTextSearchAllProperties() throws RepositoryException {
        FullTextSearch ftSearch = qf.fullTextSearch(
                SELECTOR_NAME1, null,
                qf.literal(vf.createValue(FULLTEXT_SEARCH_EXPR)));
        assertEquals("Wrong selector name", SELECTOR_NAME1, ftSearch.getSelectorName());
        assertNull("Property name must be null", ftSearch.getPropertyName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#fullTextSearch(String, String, StaticOperand)}
     */
    public void testFullTextSearchWithBindVariableValue() throws RepositoryException {
        FullTextSearch ftSearch = qf.fullTextSearch(
                SELECTOR_NAME1, propertyName1,
                qf.bindVariable(VARIABLE_NAME));
        assertEquals("Wrong selector name", SELECTOR_NAME1, ftSearch.getSelectorName());
        assertEquals("Wrong propertyName", propertyName1, ftSearch.getPropertyName());

        StaticOperand op = ftSearch.getFullTextSearchExpression();
        assertNotNull(op);
        assertTrue("not a BindVariableValue", op instanceof BindVariableValue);
        BindVariableValue value = (BindVariableValue) op;
        assertEquals(VARIABLE_NAME, value.getBindVariableName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#fullTextSearchScore(String)}
     */
    public void testFullTextSearchScore() throws RepositoryException {
        FullTextSearchScore score = qf.fullTextSearchScore(SELECTOR_NAME1);
        assertEquals("Wrong selector name", SELECTOR_NAME1, score.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#fullTextSearchScore(String)}
     */
    public void testFullTextSearchScoreWithSelector() throws RepositoryException {
        FullTextSearchScore score = qf.fullTextSearchScore(SELECTOR_NAME1);
        assertEquals("Wrong selector name", SELECTOR_NAME1, score.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#join(Source, Source, String, JoinCondition)}
     */
    public void testJoin() throws RepositoryException {
        Selector s1 = qf.selector(ntBase, SELECTOR_NAME1);
        Selector s2 = qf.selector(testNodeType, SELECTOR_NAME1);
        JoinCondition cond = qf.equiJoinCondition(ntBase, jcrPrimaryType, testNodeType, jcrPrimaryType);
        for (Iterator<String> it = JOIN_TYPES.iterator(); it.hasNext(); ) {
            String joinType = it.next();
            Join join = qf.join(s1, s2, joinType, cond);
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
        PropertyValue propValue = qf.propertyValue(SELECTOR_NAME1, propertyName1);
        Length len = qf.length(propValue);
        assertNotNull("Property value must not be null", len.getPropertyValue());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#literal(Value)}
     */
    public void testLiteral() throws RepositoryException {
        Value v = superuser.getValueFactory().createValue("test");
        Literal literal = qf.literal(v);
        assertEquals("Wrong literal value", v.getString(),
                literal.getLiteralValue().getString());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#lowerCase(DynamicOperand)}
     */
    public void testLowerCase() throws RepositoryException {
        PropertyValue propValue = qf.propertyValue(SELECTOR_NAME1, propertyName1);
        LowerCase lower = qf.lowerCase(propValue);
        assertTrue("Not a property value operand", lower.getOperand() instanceof PropertyValue);
    }

    /**
     * Test case for {@link QueryObjectModelFactory#nodeLocalName(String)}
     */
    public void testNodeLocalName() throws RepositoryException {
        NodeLocalName localName = qf.nodeLocalName(SELECTOR_NAME1);
        assertEquals("Wrong selector name", SELECTOR_NAME1, localName.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#nodeLocalName(String)}
     */
    public void testNodeLocalNameWithSelector() throws RepositoryException {
        NodeLocalName localName = qf.nodeLocalName(SELECTOR_NAME1);
        assertEquals("Wrong selector name", SELECTOR_NAME1, localName.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#nodeName(String)}
     */
    public void testNodeName() throws RepositoryException {
        NodeName nodeName = qf.nodeName(SELECTOR_NAME1);
        assertEquals("Wrong selector name", SELECTOR_NAME1, nodeName.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#nodeName(String)}
     */
    public void testNodeNameWithSelector() throws RepositoryException {
        NodeName nodeName = qf.nodeName(SELECTOR_NAME1);
        assertEquals("Wrong selector name", SELECTOR_NAME1, nodeName.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#not(Constraint)}
     */
    public void testNot() throws RepositoryException {
        PropertyExistence propExist = qf.propertyExistence(SELECTOR_NAME1, propertyName1);
        Not not = qf.not(propExist);
        assertTrue("Not a property existence constraint", not.getConstraint() instanceof PropertyExistence);
    }

    /**
     * Test case for {@link QueryObjectModelFactory#or(Constraint, Constraint)}
     */
    public void testOr() throws RepositoryException {
        PropertyExistence c1 = qf.propertyExistence(SELECTOR_NAME1, propertyName1);
        PropertyExistence c2 = qf.propertyExistence(SELECTOR_NAME1, propertyName2);
        Or or = qf.or(c1, c2);
        assertTrue("Not a PropertyExistence constraint",
                or.getConstraint1() instanceof PropertyExistence);
        assertTrue("Not a PropertyExistence constraint",
                or.getConstraint2() instanceof PropertyExistence);
    }

    /**
     * Test case for {@link QueryObjectModelFactory#propertyExistence(String, String)}
     */
    public void testPropertyExistence() throws RepositoryException {
        PropertyExistence propExist = qf.propertyExistence(SELECTOR_NAME1, propertyName1);
        assertEquals("Wrong selector", SELECTOR_NAME1, propExist.getSelectorName());
        assertEquals("Wrong property name", propertyName1, propExist.getPropertyName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#propertyExistence(String, String)}
     */
    public void testPropertyExistenceWithSelector() throws RepositoryException {
        PropertyExistence propExist = qf.propertyExistence(SELECTOR_NAME1, propertyName1);
        assertEquals("Wrong selector name", SELECTOR_NAME1, propExist.getSelectorName());
        assertEquals("Wrong property name", propertyName1, propExist.getPropertyName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#propertyValue(String, String)}
     */
    public void testPropertyValue() throws RepositoryException {
        PropertyValue propVal = qf.propertyValue(SELECTOR_NAME1, propertyName1);
        assertEquals("Wrong selector name", SELECTOR_NAME1, propVal.getSelectorName());
        assertEquals("Wrong property name", propertyName1, propVal.getPropertyName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#propertyValue(String, String)}
     */
    public void testPropertyValueWithSelector() throws RepositoryException {
        PropertyValue propVal = qf.propertyValue(SELECTOR_NAME1, propertyName1);
        assertEquals("Wrong selector name", SELECTOR_NAME1, propVal.getSelectorName());
        assertEquals("Wrong property name", propertyName1, propVal.getPropertyName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#sameNode(String, String)}
     */
    public void testSameNode() throws RepositoryException {
        SameNode sameNode = qf.sameNode(SELECTOR_NAME1, testRootNode.getPath());
        assertEquals("Wrong selector name", SELECTOR_NAME1, sameNode.getSelectorName());
        assertEquals("Wrong path", testRootNode.getPath(), sameNode.getPath());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#sameNode(String, String)}
     */
    public void testSameNodeWithSelector() throws RepositoryException {
        SameNode sameNode = qf.sameNode(SELECTOR_NAME1, testRootNode.getPath());
        assertEquals("Wrong selector name", SELECTOR_NAME1, sameNode.getSelectorName());
        assertEquals("Wrong path", testRootNode.getPath(), sameNode.getPath());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#sameNodeJoinCondition(String, String, String)}
     */
    public void testSameNodeJoinCondition() throws RepositoryException {
        SameNodeJoinCondition cond = qf.sameNodeJoinCondition(SELECTOR_NAME1, SELECTOR_NAME2, ".");
        assertEquals("Wrong selector name", SELECTOR_NAME1, cond.getSelector1Name());
        assertEquals("Wrong selector name", SELECTOR_NAME2, cond.getSelector2Name());
        assertEquals("Wrong selector path", ".", cond.getSelector2Path());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#sameNodeJoinCondition(String, String, String)}
     */
    public void testSameNodeJoinConditionWithPath() throws RepositoryException {
        SameNodeJoinCondition cond = qf.sameNodeJoinCondition(SELECTOR_NAME1, SELECTOR_NAME2, nodeName1);
        assertEquals("Wrong selector name", SELECTOR_NAME1, cond.getSelector1Name());
        assertEquals("Wrong selector name", SELECTOR_NAME2, cond.getSelector2Name());
        assertEquals("Wrong path", nodeName1, cond.getSelector2Path());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#selector(String, String)}
     */
    public void testSelector() throws RepositoryException {
        Selector selector = qf.selector(ntBase, SELECTOR_NAME1);
        assertEquals("Wrong node type name", ntBase, selector.getNodeTypeName());
        assertEquals("Wrong selector name", SELECTOR_NAME1, selector.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#selector(String, String)}
     */
    public void testSelectorWithName() throws RepositoryException {
        Selector selector = qf.selector(ntBase, SELECTOR_NAME1);
        assertEquals("Wrong node type name", ntBase, selector.getNodeTypeName());
        assertEquals("Wrong selector name", SELECTOR_NAME1, selector.getSelectorName());
    }

    /**
     * Test case for {@link QueryObjectModelFactory#upperCase(DynamicOperand)}
     */
    public void testUpperCase() throws RepositoryException {
        PropertyValue propValue = qf.propertyValue(SELECTOR_NAME1, propertyName1);
        UpperCase upper = qf.upperCase(propValue);
        assertTrue("Not a property value operand", upper.getOperand() instanceof PropertyValue);
    }
}
