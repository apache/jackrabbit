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
package org.apache.jackrabbit.core.query.qom;

/**
 * <code>DefaultTraversingQOMTreeVisitor</code>...
 */
public class DefaultTraversingQOMTreeVisitor implements QOMTreeVisitor {

    /**
     * {@inheritDoc}
     * <p/>
     * Calls accept on each of the attached constraints of the AND node.
     */
    public final void visit(AndImpl node, Object data) {
        ((ConstraintImpl) node.getConstraint1()).accept(this, data);
        ((ConstraintImpl) node.getConstraint2()).accept(this, data);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Does nothing.
     */
    public void visit(BindVariableValueImpl node, Object data) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Does nothing.
     */
    public void visit(ChildNodeImpl node, Object data) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Does nothing.
     */
    public void visit(ChildNodeJoinConditionImpl node, Object data) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Does nothing.
     */
    public void visit(ColumnImpl node, Object data) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Calls accept on the two operands in the comparison node.
     */
    public void visit(ComparisonImpl node, Object data) {
        ((DynamicOperandImpl) node.getOperand1()).accept(this, data);
        ((StaticOperandImpl) node.getOperand2()).accept(this, data);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Does nothing.
     */
    public void visit(DescendantNodeImpl node, Object data) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Does nothing.
     */
    public void visit(DescendantNodeJoinConditionImpl node, Object data) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Does nothing.
     */
    public void visit(EquiJoinConditionImpl node, Object data) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Does nothing.
     */
    public void visit(FullTextSearchImpl node, Object data) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Does nothing.
     */
    public void visit(FullTextSearchScoreImpl node, Object data) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Calls accept on the two sources and the join condition in the join node.
     */
    public void visit(JoinImpl node, Object data) {
        ((SourceImpl) node.getRight()).accept(this, data);
        ((SourceImpl) node.getLeft()).accept(this, data);
        ((JoinConditionImpl) node.getJoinCondition()).accept(this, data);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Calls accept on the property value in the length node.
     */
    public void visit(LengthImpl node, Object data) {
        ((PropertyValueImpl) node.getPropertyValue()).accept(this, data);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Calls accept on the dynamic operand in the lower-case node.
     */
    public void visit(LowerCaseImpl node, Object data) {
        ((DynamicOperandImpl) node.getOperand()).accept(this, data);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Does nothing.
     */
    public void visit(NodeLocalNameImpl node, Object data) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Does nothing.
     */
    public void visit(NodeNameImpl node, Object data) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Calls accept on the constraint in the NOT node.
     */
    public void visit(NotImpl node, Object data) {
        ((ConstraintImpl) node.getConstraint()).accept(this, data);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Calls accept on the dynamic operand in the ordering node.
     */
    public void visit(OrderingImpl node, Object data) {
        ((DynamicOperandImpl) node.getOperand()).accept(this, data);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Calls accept on each of the attached constraints of the OR node.
     */
    public final void visit(OrImpl node, Object data) {
        ((ConstraintImpl) node.getConstraint1()).accept(this, data);
        ((ConstraintImpl) node.getConstraint2()).accept(this, data);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Does nothing.
     */
    public void visit(PropertyExistenceImpl node, Object data) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Does nothing.
     */
    public void visit(PropertyValueImpl node, Object data) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Calls accept on the following contained QOM nodes:
     * <ul>
     * <li>Source</li>
     * <li>Constraints</li>
     * <li>Orderings</li>
     * <li>Columns</li>
     * </ul>
     */
    public void visit(QueryObjectModelTree node, Object data) {
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
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Does nothing.
     */
    public void visit(SameNodeImpl node, Object data) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Does nothing.
     */
    public void visit(SameNodeJoinConditionImpl node, Object data) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Does nothing.
     */
    public void visit(SelectorImpl node, Object data) {
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Calls accept on the dynamic operand in the lower-case node.
     */
    public void visit(UpperCaseImpl node, Object data) {
        ((DynamicOperandImpl) node.getOperand()).accept(this, data);
    }
}
