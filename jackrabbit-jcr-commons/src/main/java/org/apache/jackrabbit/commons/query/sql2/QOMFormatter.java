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
        sb.append("SELECT");
        ws();
        format(qom.getColumns());
        ws();
        sb.append("FROM");
        ws();
        format(qom.getSource());
        Constraint c = qom.getConstraint();
        if (c != null) {
            ws();
            sb.append("WHERE");
            ws();
            format(c);
        }
        Ordering[] orderings = qom.getOrderings();
        if (orderings.length > 0) {
            ws();
            sb.append("ORDER BY");
            ws();
            format(orderings);
        }
        return sb.toString();
    }

    private void format(Ordering[] orderings) {
        String comma = "";
        for (Ordering ordering : orderings) {
            sb.append(comma);
            comma = ", ";
            format(ordering.getOperand());
            if (JCR_ORDER_DESCENDING.equals(ordering.getOrder())) {
                ws();
                sb.append("DESC");
            }
        }
    }

    private void format(Constraint c)
            throws RepositoryException {
        if (c instanceof And) {
            format((And) c);
        } else if (c instanceof ChildNode) {
            format((ChildNode) c);
        } else if (c instanceof Comparison) {
            format((Comparison) c);
        } else if (c instanceof DescendantNode) {
            format((DescendantNode) c);
        } else if (c instanceof FullTextSearch) {
            format((FullTextSearch) c);
        } else if (c instanceof Not) {
            format((Not) c);
        } else if (c instanceof Or) {
            format((Or) c);
        } else if (c instanceof PropertyExistence) {
            format((PropertyExistence) c);
        } else {
            format((SameNode) c);
        }
    }

    private void format(And constraint)
            throws RepositoryException {
        String and = "";
        for (Constraint c : Arrays.asList(
                constraint.getConstraint1(),
                constraint.getConstraint2())) {
            sb.append(and);
            and = " AND ";
            boolean paren = c instanceof Or;
            if (paren) {
                sb.append("(");
            }
            format(c);
            if (paren) {
                sb.append(")");
            }
        }
    }

    private void format(ChildNode constraint) {
        sb.append("ISCHILDNODE(");
        formatName(constraint.getSelectorName());
        sb.append(",");
        ws();
        formatPath(constraint.getParentPath());
        sb.append(")");
    }

    private void format(Comparison constraint)
            throws RepositoryException {
        format(constraint.getOperand1());
        ws();
        formatOperator(constraint.getOperator());
        ws();
        format(constraint.getOperand2());
    }

    private void format(StaticOperand operand)
            throws RepositoryException {
        if (operand instanceof BindVariableValue) {
            format((BindVariableValue) operand);
        } else {
            format((Literal) operand);
        }
    }

    private void format(BindVariableValue value) {
        sb.append("$");
        sb.append(value.getBindVariableName());
    }

    private void format(Literal value)
            throws RepositoryException {
        Value v = value.getLiteralValue();
        switch (v.getType()) {
            case PropertyType.BINARY:
                formatCastLiteral(v.getString(), "BINARY");
                break;
            case PropertyType.BOOLEAN:
                sb.append(v.getString());
                break;
            case PropertyType.DATE:
                formatCastLiteral(v.getString(), "DATE");
                break;
            case PropertyType.DECIMAL:
                sb.append(v.getString());
                break;
            case PropertyType.DOUBLE:
                sb.append(v.getString());
                break;
            case PropertyType.LONG:
                sb.append(v.getString());
                break;
            case PropertyType.NAME:
                formatCastLiteral(v.getString(), "NAME");
                break;
            case PropertyType.PATH:
                formatCastLiteral(v.getString(), "PATH");
                break;
            case PropertyType.REFERENCE:
                formatCastLiteral(v.getString(), "REFERENCE");
                break;
            case PropertyType.STRING:
                formatStringLiteral(v.getString());
                break;
            case PropertyType.URI:
                formatCastLiteral(v.getString(), "URI");
                break;
            case PropertyType.WEAKREFERENCE:
                formatCastLiteral(v.getString(), "WEAKREFERENCE");
                break;
        }
    }

    private void formatCastLiteral(String value, String propertyType) {
        sb.append("CAST(");
        formatStringLiteral(value);
        ws();
        sb.append("AS");
        ws();
        sb.append(propertyType);
        sb.append(")");
    }

    private void formatStringLiteral(String value) {
        sb.append("'");
        sb.append(value.replaceAll("'", "''"));
        sb.append("'");
    }

    private void formatOperator(String operator) {
        if (JCR_OPERATOR_EQUAL_TO.equals(operator)) {
            sb.append("=");
        } else if (JCR_OPERATOR_GREATER_THAN.equals(operator)) {
            sb.append(">");
        } else if (JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO.equals(operator)) {
            sb.append(">=");
        } else if (JCR_OPERATOR_LESS_THAN.equals(operator)) {
            sb.append("<");
        } else if (JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO.equals(operator)) {
            sb.append("<=");
        } else if (JCR_OPERATOR_LIKE.equals(operator)) {
            sb.append("LIKE");
        } else {
            sb.append("<>");
        }
    }

    private void format(DynamicOperand operand) {
        if (operand instanceof FullTextSearchScore) {
            format((FullTextSearchScore) operand);
        } else if (operand instanceof Length) {
            format((Length) operand);
        } else if (operand instanceof LowerCase) {
            format((LowerCase) operand);
        } else if (operand instanceof NodeLocalName) {
            format((NodeLocalName) operand);
        } else if (operand instanceof NodeName) {
            format((NodeName) operand);
        } else if (operand instanceof PropertyValue) {
            format((PropertyValue) operand);
        } else {
            format((UpperCase) operand);
        }
    }

    private void format(FullTextSearchScore operand) {
        sb.append("SCORE(");
        formatName(operand.getSelectorName());
        sb.append(")");
    }

    private void format(Length operand) {
        sb.append("LENGTH(");
        format(operand.getPropertyValue());
        sb.append(")");
    }

    private void format(LowerCase operand) {
        sb.append("LOWER(");
        format(operand.getOperand());
        sb.append(")");
    }

    private void format(NodeLocalName operand) {
        sb.append("LOCALNAME(");
        formatName(operand.getSelectorName());
        sb.append(")");
    }

    private void format(NodeName operand) {
        sb.append("NAME(");
        formatName(operand.getSelectorName());
        sb.append(")");
    }

    private void format(PropertyValue operand) {
        formatName(operand.getSelectorName());
        sb.append(".");
        formatName(operand.getPropertyName());
    }

    private void format(UpperCase operand) {
        sb.append("UPPER(");
        format(operand.getOperand());
        sb.append(")");
    }

    private void format(DescendantNode constraint) {
        sb.append("ISDESCENDANTNODE(");
        formatName(constraint.getSelectorName());
        sb.append(",");
        ws();
        formatPath(constraint.getAncestorPath());
        sb.append(")");
    }

    private void format(FullTextSearch constraint)
            throws RepositoryException {
        sb.append("CONTAINS(");
        formatName(constraint.getSelectorName());
        sb.append(".");
        String propName = constraint.getPropertyName();
        if (propName == null) {
            sb.append("*");
        } else {
            formatName(propName);
        }
        sb.append(",");
        ws();
        format(constraint.getFullTextSearchExpression());
        sb.append(")");
    }

    private void format(Not constraint)
            throws RepositoryException {
        sb.append("NOT");
        ws();
        Constraint c = constraint.getConstraint();
        boolean paren = c instanceof And || c instanceof Or;
        if (paren) {
            sb.append("(");
        }
        format(c);
        if (paren) {
            sb.append(")");
        }
    }

    private void format(Or constraint)
            throws RepositoryException {
        format(constraint.getConstraint1());
        ws();
        sb.append("OR");
        ws();
        format(constraint.getConstraint2());
    }

    private void format(PropertyExistence constraint) {
        formatName(constraint.getSelectorName());
        sb.append(".");
        formatName(constraint.getPropertyName());
        ws();
        sb.append("IS NOT NULL");
    }

    private void format(SameNode constraint) {
        sb.append("ISSAMENODE(");
        formatName(constraint.getSelectorName());
        sb.append(",");
        ws();
        formatPath(constraint.getPath());
        sb.append(")");
    }

    private void format(Column[] columns) {
        if (columns.length == 0) {
            sb.append("*");
        } else {
            String comma = "";
            for (Column c : columns) {
                sb.append(comma);
                comma = ", ";
                formatName(c.getSelectorName());
                sb.append(".");
                String propName = c.getPropertyName();
                if (propName != null) {
                    formatName(propName);
                    ws();
                    sb.append("AS");
                    ws();
                    formatName(c.getColumnName());
                } else {
                    sb.append("*");
                }
            }
        }
    }

    private void format(Source source) {
        if (source instanceof Join) {
            format((Join) source);
        } else {
            format((Selector) source);
        }
    }

    private void format(Join join) {
        format(join.getLeft());
        ws();
        formatJoinType(join.getJoinType());
        ws();
        sb.append("JOIN");
        ws();
        format(join.getRight());
        ws();
        sb.append("ON");
        ws();
        format(join.getJoinCondition());
    }

    private void format(JoinCondition joinCondition) {
        if (joinCondition instanceof EquiJoinCondition) {
            format((EquiJoinCondition) joinCondition);
        } else if (joinCondition instanceof ChildNodeJoinCondition) {
            format((ChildNodeJoinCondition) joinCondition);
        } else if (joinCondition instanceof DescendantNodeJoinCondition) {
            format((DescendantNodeJoinCondition) joinCondition);
        } else {
            format((SameNodeJoinCondition) joinCondition);
        }
    }

    private void format(EquiJoinCondition condition) {
        formatName(condition.getSelector1Name());
        sb.append(".");
        formatName(condition.getProperty1Name());
        ws();
        sb.append("=");
        ws();
        formatName(condition.getSelector2Name());
        sb.append(".");
        formatName(condition.getProperty2Name());
    }

    private void format(ChildNodeJoinCondition condition) {
        sb.append("ISCHILDNODE(");
        formatName(condition.getChildSelectorName());
        sb.append(",");
        ws();
        formatName(condition.getParentSelectorName());
        sb.append(")");
    }

    private void format(DescendantNodeJoinCondition condition) {
        sb.append("ISDESCENDANTNODE(");
        formatName(condition.getDescendantSelectorName());
        sb.append(",");
        ws();
        formatName(condition.getAncestorSelectorName());
        sb.append(")");
    }

    private void format(SameNodeJoinCondition condition) {
        sb.append("ISSAMENODE(");
        formatName(condition.getSelector1Name());
        sb.append(",");
        ws();
        formatName(condition.getSelector2Name());
        if (condition.getSelector2Path() != null) {
            sb.append(",");
            ws();
            formatPath(condition.getSelector2Path());
        }
        sb.append(")");
    }

    private void formatPath(String path) {
        if (isSimpleName(path)) {
            sb.append(path);
        } else {
            sb.append("[");
            sb.append(path);
            sb.append("]");
        }
    }

    private void formatJoinType(String joinType) {
        if (joinType.equals(JCR_JOIN_TYPE_INNER)) {
            sb.append("INNER");
        } else if (joinType.equals(JCR_JOIN_TYPE_LEFT_OUTER)) {
            sb.append("LEFT OUTER");
        } else {
            sb.append("RIGHT OUTER");
        }
    }

    private void format(Selector selector) {
        formatName(selector.getNodeTypeName());
        ws();
        sb.append("AS");
        ws();
        formatName(selector.getSelectorName());
    }

    private void formatName(String name) {
        if (isSimpleName(name)) {
            sb.append(name);
        } else {
            sb.append("[");
            sb.append(name);
            sb.append("]");
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

    private void ws() {
        sb.append(" ");
    }
}
