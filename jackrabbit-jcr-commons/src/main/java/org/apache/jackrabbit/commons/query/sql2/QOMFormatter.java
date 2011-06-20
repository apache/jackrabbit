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
package org.apache.jackrabbit.commons.query.sql2;

import java.util.BitSet;
import java.util.Arrays;

import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.Source;
import javax.jcr.query.qom.Selector;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.JoinCondition;
import javax.jcr.query.qom.EquiJoinCondition;
import javax.jcr.query.qom.ChildNodeJoinCondition;
import javax.jcr.query.qom.DescendantNodeJoinCondition;
import javax.jcr.query.qom.SameNodeJoinCondition;
import javax.jcr.query.qom.And;
import javax.jcr.query.qom.ChildNode;
import javax.jcr.query.qom.Comparison;
import javax.jcr.query.qom.DescendantNode;
import javax.jcr.query.qom.FullTextSearch;
import javax.jcr.query.qom.Not;
import javax.jcr.query.qom.Or;
import javax.jcr.query.qom.PropertyExistence;
import javax.jcr.query.qom.SameNode;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.StaticOperand;
import javax.jcr.query.qom.BindVariableValue;
import javax.jcr.query.qom.Literal;
import javax.jcr.query.qom.FullTextSearchScore;
import javax.jcr.query.qom.Length;
import javax.jcr.query.qom.LowerCase;
import javax.jcr.query.qom.NodeLocalName;
import javax.jcr.query.qom.NodeName;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.UpperCase;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.Value;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * <code>QOMFormatter</code> implements a formatter that translates a query
 * object model into a JCR_SQL2 string statement.
 */
public class QOMFormatter implements QueryObjectModelConstants {

    /**
     * BitSet of valid SQL identifier start characters.
     */
    private static final BitSet IDENTIFIER_START = new BitSet();

    /**
     * BitSet of valid SQL identifier body characters.
     */
    private static final BitSet IDENTIFIER_PART_OR_UNDERSCORE = new BitSet();

    static {
        for (char c = 'a'; c <= 'z'; c++) {
            IDENTIFIER_START.set(c);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            IDENTIFIER_START.set(c);
        }
        IDENTIFIER_PART_OR_UNDERSCORE.or(IDENTIFIER_START);
        for (char c = '0'; c <= '9'; c++) {
            IDENTIFIER_PART_OR_UNDERSCORE.set(c);
        }
        IDENTIFIER_PART_OR_UNDERSCORE.set('_');
    }

    /**
     * The query object model to format.
     */
    private final QueryObjectModel qom;

    /**
     * The JCR_SQL2 statement.
     */
    private final StringBuilder sb = new StringBuilder();

    /**
     * Private constructor.
     *
     * @param qom the query object model to format.
     */
    private QOMFormatter(QueryObjectModel qom) {
        this.qom = qom;
    }

    /**
     * Formats the given <code>qom</code> as a JCR_SQL2 query statement.
     *
     * @param qom the query object model to translate.
     * @return the JCR_SQL2 statement.
     * @throws RepositoryException if an error occurs while formatting the qom.
     */
    public static String format(QueryObjectModel qom)
            throws RepositoryException {
        return new QOMFormatter(qom).format();
    }

    private String format() throws RepositoryException {
        append("SELECT ");
        append(qom.getColumns());
        append(" FROM ");
        append(qom.getSource());
        Constraint c = qom.getConstraint();
        if (c != null) {
            append(" WHERE ");
            append(c);
        }
        Ordering[] orderings = qom.getOrderings();
        if (orderings.length > 0) {
            append(" ORDER BY ");
            append(orderings);
        }
        return sb.toString();
    }

    private void append(Ordering[] orderings) {
        String comma = "";
        for (Ordering ordering : orderings) {
            append(comma);
            comma = ", ";
            append(ordering.getOperand());
            if (JCR_ORDER_DESCENDING.equals(ordering.getOrder())) {
                append(" DESC");
            }
        }
    }

    private void append(Constraint c)
            throws RepositoryException {
        if (c instanceof And) {
            append((And) c);
        } else if (c instanceof ChildNode) {
            append((ChildNode) c);
        } else if (c instanceof Comparison) {
            append((Comparison) c);
        } else if (c instanceof DescendantNode) {
            append((DescendantNode) c);
        } else if (c instanceof FullTextSearch) {
            append((FullTextSearch) c);
        } else if (c instanceof Not) {
            append((Not) c);
        } else if (c instanceof Or) {
            append((Or) c);
        } else if (c instanceof PropertyExistence) {
            append((PropertyExistence) c);
        } else {
            append((SameNode) c);
        }
    }

    private void append(And constraint)
            throws RepositoryException {
        String and = "";
        for (Constraint c : Arrays.asList(
                constraint.getConstraint1(),
                constraint.getConstraint2())) {
            append(and);
            and = " AND ";
            boolean paren = c instanceof Or || c instanceof Not;
            if (paren) {
                append("(");
            }
            append(c);
            if (paren) {
                append(")");
            }
        }
    }

    private void append(ChildNode constraint) {
        append("ISCHILDNODE(");
        appendName(constraint.getSelectorName());
        append(", ");
        appendPath(constraint.getParentPath());
        append(")");
    }

    private void append(Comparison constraint)
            throws RepositoryException {
        append(constraint.getOperand1());
        append(" ");
        appendOperator(constraint.getOperator());
        append(" ");
        append(constraint.getOperand2());
    }

    private void append(StaticOperand operand)
            throws RepositoryException {
        if (operand instanceof BindVariableValue) {
            append((BindVariableValue) operand);
        } else {
            append((Literal) operand);
        }
    }

    private void append(BindVariableValue value) {
        append("$");
        append(value.getBindVariableName());
    }

    private void append(Literal value)
            throws RepositoryException {
        Value v = value.getLiteralValue();
        switch (v.getType()) {
            case PropertyType.BINARY:
                appendCastLiteral(v.getString(), "BINARY");
                break;
            case PropertyType.BOOLEAN:
                append(v.getString());
                break;
            case PropertyType.DATE:
                appendCastLiteral(v.getString(), "DATE");
                break;
            case PropertyType.DECIMAL:
                appendCastLiteral(v.getString(), "DECIMAL");
                break;
            case PropertyType.DOUBLE:
                appendCastLiteral(v.getString(), "DOUBLE");
                break;
            case PropertyType.LONG:
                appendCastLiteral(v.getString(), "LONG");
                break;
            case PropertyType.NAME:
                appendCastLiteral(v.getString(), "NAME");
                break;
            case PropertyType.PATH:
                appendCastLiteral(v.getString(), "PATH");
                break;
            case PropertyType.REFERENCE:
                appendCastLiteral(v.getString(), "REFERENCE");
                break;
            case PropertyType.STRING:
                appendStringLiteral(v.getString());
                break;
            case PropertyType.URI:
                appendCastLiteral(v.getString(), "URI");
                break;
            case PropertyType.WEAKREFERENCE:
                appendCastLiteral(v.getString(), "WEAKREFERENCE");
                break;
        }
    }

    private void appendCastLiteral(String value, String propertyType) {
        append("CAST(");
        appendStringLiteral(value);
        append(" AS ");
        append(propertyType);
        append(")");
    }

    private void appendStringLiteral(String value) {
        append("'");
        append(value.replaceAll("'", "''"));
        append("'");
    }

    private void appendOperator(String operator) {
        if (JCR_OPERATOR_EQUAL_TO.equals(operator)) {
            append("=");
        } else if (JCR_OPERATOR_GREATER_THAN.equals(operator)) {
            append(">");
        } else if (JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO.equals(operator)) {
            append(">=");
        } else if (JCR_OPERATOR_LESS_THAN.equals(operator)) {
            append("<");
        } else if (JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO.equals(operator)) {
            append("<=");
        } else if (JCR_OPERATOR_LIKE.equals(operator)) {
            append("LIKE");
        } else {
            append("<>");
        }
    }

    private void append(DynamicOperand operand) {
        if (operand instanceof FullTextSearchScore) {
            append((FullTextSearchScore) operand);
        } else if (operand instanceof Length) {
            append((Length) operand);
        } else if (operand instanceof LowerCase) {
            append((LowerCase) operand);
        } else if (operand instanceof NodeLocalName) {
            append((NodeLocalName) operand);
        } else if (operand instanceof NodeName) {
            append((NodeName) operand);
        } else if (operand instanceof PropertyValue) {
            append((PropertyValue) operand);
        } else {
            append((UpperCase) operand);
        }
    }

    private void append(FullTextSearchScore operand) {
        append("SCORE(");
        appendName(operand.getSelectorName());
        append(")");
    }

    private void append(Length operand) {
        append("LENGTH(");
        append(operand.getPropertyValue());
        append(")");
    }

    private void append(LowerCase operand) {
        append("LOWER(");
        append(operand.getOperand());
        append(")");
    }

    private void append(NodeLocalName operand) {
        append("LOCALNAME(");
        appendName(operand.getSelectorName());
        append(")");
    }

    private void append(NodeName operand) {
        append("NAME(");
        appendName(operand.getSelectorName());
        append(")");
    }

    private void append(PropertyValue operand) {
        appendName(operand.getSelectorName());
        append(".");
        appendName(operand.getPropertyName());
    }

    private void append(UpperCase operand) {
        append("UPPER(");
        append(operand.getOperand());
        append(")");
    }

    private void append(DescendantNode constraint) {
        append("ISDESCENDANTNODE(");
        appendName(constraint.getSelectorName());
        append(", ");
        appendPath(constraint.getAncestorPath());
        append(")");
    }

    private void append(FullTextSearch constraint) throws RepositoryException {
        append("CONTAINS(");
        appendName(constraint.getSelectorName());
        append(".");
        String propName = constraint.getPropertyName();
        if (propName == null) {
            append("*");
        } else {
            appendName(propName);
        }
        append(", ");
        append(constraint.getFullTextSearchExpression());
        append(")");
    }

    private void append(Not constraint) throws RepositoryException {
        append("NOT ");
        Constraint c = constraint.getConstraint();
        boolean paren = c instanceof And || c instanceof Or;
        if (paren) {
            append("(");
        }
        append(c);
        if (paren) {
            append(")");
        }
    }

    private void append(Or constraint) throws RepositoryException {
        append(constraint.getConstraint1());
        append(" OR ");
        append(constraint.getConstraint2());
    }

    private void append(PropertyExistence constraint) {
        appendName(constraint.getSelectorName());
        append(".");
        appendName(constraint.getPropertyName());
        append(" IS NOT NULL");
    }

    private void append(SameNode constraint) {
        append("ISSAMENODE(");
        appendName(constraint.getSelectorName());
        append(", ");
        appendPath(constraint.getPath());
        append(")");
    }

    private void append(Column[] columns) {
        if (columns.length == 0) {
            append("*");
        } else {
            String comma = "";
            for (Column c : columns) {
                append(comma);
                comma = ", ";
                appendName(c.getSelectorName());
                append(".");
                String propName = c.getPropertyName();
                if (propName != null) {
                    appendName(propName);
                    if (c.getColumnName() != null) {
                        append(" AS ");
                        appendName(c.getColumnName());
                    }
                } else {
                    append("*");
                }
            }
        }
    }

    private void append(Source source) {
        if (source instanceof Join) {
            append((Join) source);
        } else {
            append((Selector) source);
        }
    }

    private void append(Join join) {
        append(join.getLeft());
        append(" ");
        appendJoinType(join.getJoinType());
        append(" JOIN ");
        append(join.getRight());
        append(" ON ");
        append(join.getJoinCondition());
    }

    private void append(JoinCondition joinCondition) {
        if (joinCondition instanceof EquiJoinCondition) {
            append((EquiJoinCondition) joinCondition);
        } else if (joinCondition instanceof ChildNodeJoinCondition) {
            append((ChildNodeJoinCondition) joinCondition);
        } else if (joinCondition instanceof DescendantNodeJoinCondition) {
            append((DescendantNodeJoinCondition) joinCondition);
        } else {
            append((SameNodeJoinCondition) joinCondition);
        }
    }

    private void append(EquiJoinCondition condition) {
        appendName(condition.getSelector1Name());
        append(".");
        appendName(condition.getProperty1Name());
        append(" = ");
        appendName(condition.getSelector2Name());
        append(".");
        appendName(condition.getProperty2Name());
    }

    private void append(ChildNodeJoinCondition condition) {
        append("ISCHILDNODE(");
        appendName(condition.getChildSelectorName());
        append(", ");
        appendName(condition.getParentSelectorName());
        append(")");
    }

    private void append(DescendantNodeJoinCondition condition) {
        append("ISDESCENDANTNODE(");
        appendName(condition.getDescendantSelectorName());
        append(", ");
        appendName(condition.getAncestorSelectorName());
        append(")");
    }

    private void append(SameNodeJoinCondition condition) {
        append("ISSAMENODE(");
        appendName(condition.getSelector1Name());
        append(", ");
        appendName(condition.getSelector2Name());
        if (condition.getSelector2Path() != null) {
            append(", ");
            appendPath(condition.getSelector2Path());
        }
        append(")");
    }

    private void appendPath(String path) {
        if (isSimpleName(path)) {
            append(path);
        } else {
            boolean needQuotes = path.contains(" ");
            append("[");
            if (needQuotes) {
                append("'");
            }
            append(path);
            if (needQuotes) {
                append("'");
            }
            append("]");
        }
    }

    private void appendJoinType(String joinType) {
        if (joinType.equals(JCR_JOIN_TYPE_INNER)) {
            append("INNER");
        } else if (joinType.equals(JCR_JOIN_TYPE_LEFT_OUTER)) {
            append("LEFT OUTER");
        } else {
            append("RIGHT OUTER");
        }
    }

    private void append(Selector selector) {
        appendName(selector.getNodeTypeName());
        if (!selector.getSelectorName().equals(selector.getNodeTypeName())) {
            append(" AS ");
            appendName(selector.getSelectorName());
        }
    }

    private void appendName(String name) {
        if (isSimpleName(name)) {
            append(name);
        } else {
            append("[");
            append(name);
            append("]");
        }
    }

    private static boolean isSimpleName(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i == 0) {
                if (!IDENTIFIER_START.get(c)) {
                    return false;
                }
            } else {
                if (!IDENTIFIER_PART_OR_UNDERSCORE.get(c)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void append(String s) {
        sb.append(s);
    }

}
