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

/**
 * <code>DefaultTraversingQOMTreeVisitor</code> default implementation of a
 * traversing {@link QOMTreeVisitor}.
 */
public class DefaultTraversingQOMTreeVisitor extends DefaultQOMTreeVisitor {

    /**
     * Calls accept on each of the attached constraints of the AND node.
     */
    public final Object visit(AndImpl node, Object data) throws Exception {
        ((ConstraintImpl) node.getConstraint1()).accept(this, data);
        ((ConstraintImpl) node.getConstraint2()).accept(this, data);
        return data;
    }

    /**
     * Calls accept on the two operands in the comparison node.
     */
    public Object visit(ComparisonImpl node, Object data) throws Exception {
        ((DynamicOperandImpl) node.getOperand1()).accept(this, data);
        ((StaticOperandImpl) node.getOperand2()).accept(this, data);
        return data;
    }

    /**
     * Calls accept on the static operand in the fulltext search constraint.
     */
    public Object visit(FullTextSearchImpl node, Object data) throws Exception {
        ((StaticOperandImpl) node.getFullTextSearchExpression()).accept(this, data);
        return data;
    }

    /**
     * Calls accept on the two sources and the join condition in the join node.
     */
    public Object visit(JoinImpl node, Object data) throws Exception {
        ((SourceImpl) node.getRight()).accept(this, data);
        ((SourceImpl) node.getLeft()).accept(this, data);
        ((JoinConditionImpl) node.getJoinCondition()).accept(this, data);
        return data;
    }

    /**
     * Calls accept on the property value in the length node.
     */
    public Object visit(LengthImpl node, Object data) throws Exception {
        ((PropertyValueImpl) node.getPropertyValue()).accept(this, data);
        return data;
    }

    /**
     * Calls accept on the dynamic operand in the lower-case node.
     */
    public Object visit(LowerCaseImpl node, Object data) throws Exception {
        ((DynamicOperandImpl) node.getOperand()).accept(this, data);
        return data;
    }

    /**
     * Calls accept on the constraint in the NOT node.
     */
    public Object visit(NotImpl node, Object data) throws Exception {
        ((ConstraintImpl) node.getConstraint()).accept(this, data);
        return data;
    }

    /**
     * Calls accept on the dynamic operand in the ordering node.
     */
    public Object visit(OrderingImpl node, Object data) throws Exception {
        ((DynamicOperandImpl) node.getOperand()).accept(this, data);
        return data;
    }

    /**
     * Calls accept on each of the attached constraints of the OR node.
     */
    public Object visit(OrImpl node, Object data) throws Exception {
        ((ConstraintImpl) node.getConstraint1()).accept(this, data);
        ((ConstraintImpl) node.getConstraint2()).accept(this, data);
        return data;
    }

    /**
     * Calls accept on the following contained QOM nodes:
     * <ul>
     * <li>Source</li>
     * <li>Constraints</li>
     * <li>Orderings</li>
     * <li>Columns</li>
     * </ul>
     */
    public Object visit(QueryObjectModelTree node, Object data) throws Exception {
        node.getSource().accept(this, data);
        ConstraintImpl constraint = node.getConstraint();
        if (constraint != null) {
            constraint.accept(this, data);
        }
        OrderingImpl[] orderings = node.getOrderings();
        for (int i = 0; i < orderings.length; i++) {
            orderings[i].accept(this, data);
        }
        ColumnImpl[] columns = node.getColumns();
        for (int i = 0; i < columns.length; i++) {
            columns[i].accept(this, data);
        }
        return data;
    }

    /**
     * Calls accept on the dynamic operand in the lower-case node.
     */
    public Object visit(UpperCaseImpl node, Object data) throws Exception {
        ((DynamicOperandImpl) node.getOperand()).accept(this, data);
        return data;
    }
}
