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
package org.apache.jackrabbit.core.query.lucene.constraint;

import java.net.URLDecoder;
import java.util.Map;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.query.InvalidQueryException;

import org.apache.jackrabbit.commons.query.qom.Operator;
import org.apache.jackrabbit.core.query.lucene.LuceneQueryFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.query.qom.AndImpl;
import org.apache.jackrabbit.spi.commons.query.qom.BindVariableValueImpl;
import org.apache.jackrabbit.spi.commons.query.qom.ChildNodeImpl;
import org.apache.jackrabbit.spi.commons.query.qom.ComparisonImpl;
import org.apache.jackrabbit.spi.commons.query.qom.ConstraintImpl;
import org.apache.jackrabbit.spi.commons.query.qom.DefaultQOMTreeVisitor;
import org.apache.jackrabbit.spi.commons.query.qom.DescendantNodeImpl;
import org.apache.jackrabbit.spi.commons.query.qom.DynamicOperandImpl;
import org.apache.jackrabbit.spi.commons.query.qom.FullTextSearchImpl;
import org.apache.jackrabbit.spi.commons.query.qom.FullTextSearchScoreImpl;
import org.apache.jackrabbit.spi.commons.query.qom.LengthImpl;
import org.apache.jackrabbit.spi.commons.query.qom.LiteralImpl;
import org.apache.jackrabbit.spi.commons.query.qom.LowerCaseImpl;
import org.apache.jackrabbit.spi.commons.query.qom.NodeLocalNameImpl;
import org.apache.jackrabbit.spi.commons.query.qom.NodeNameImpl;
import org.apache.jackrabbit.spi.commons.query.qom.NotImpl;
import org.apache.jackrabbit.spi.commons.query.qom.OrImpl;
import org.apache.jackrabbit.spi.commons.query.qom.PropertyExistenceImpl;
import org.apache.jackrabbit.spi.commons.query.qom.PropertyValueImpl;
import org.apache.jackrabbit.spi.commons.query.qom.SameNodeImpl;
import org.apache.jackrabbit.spi.commons.query.qom.SelectorImpl;
import org.apache.jackrabbit.spi.commons.query.qom.StaticOperandImpl;
import org.apache.jackrabbit.spi.commons.query.qom.UpperCaseImpl;

/**
 * <code>ConstraintBuilder</code> builds a {@link Constraint} from a tree of
 * QOM constraints.
 */
public class ConstraintBuilder {

    /**
     * Creates a {@link Constraint} from a QOM <code>constraint</code>.
     *
     * @param constraint         the QOM constraint.
     * @param bindVariableValues the map of bind variables and their respective
     *                           value.
     * @param selectors          the selectors of the current query.
     * @param factory            the lucene query factory.
     * @param vf                 the value factory of the current session.
     * @return a {@link Constraint}.
     * @throws RepositoryException if an error occurs while building the
     *                             constraint.
     */
    public static Constraint create(ConstraintImpl constraint,
                                    Map<String, Value> bindVariableValues,
                                    SelectorImpl[] selectors,
                                    LuceneQueryFactory factory,
                                    ValueFactory vf)
            throws RepositoryException {
        try {
            return (Constraint) constraint.accept(new Visitor(
                    bindVariableValues, selectors, factory, vf), null);
        } catch (RepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * A QOM tree visitor that translates the contraints.
     */
    private static final class Visitor extends DefaultQOMTreeVisitor {

        /**
         * The bind variables and their values.
         */
        private final Map<String, Value> bindVariableValues;

        /**
         * The selectors of the query.
         */
        private final SelectorImpl[] selectors;

        /**
         * The lucene query builder.
         */
        private final LuceneQueryFactory factory;

        /**
         * The value factory of the current session.
         */
        private final ValueFactory vf;

        /**
         * Creates a new visitor.
         *
         * @param bindVariableValues the bound values.
         * @param selectors          the selectors of the current query.
         * @param factory            the lucene query factory.
         * @param vf                 the value factory of the current session.
         */
        Visitor(Map<String, Value> bindVariableValues,
                SelectorImpl[] selectors,
                LuceneQueryFactory factory,
                ValueFactory vf) {
            this.bindVariableValues = bindVariableValues;
            this.selectors = selectors;
            this.factory = factory;
            this.vf = vf;
        }

        public Object visit(AndImpl node, Object data) throws Exception {
            ConstraintImpl left = (ConstraintImpl) node.getConstraint1();
            ConstraintImpl right = (ConstraintImpl) node.getConstraint2();
            return new AndConstraint((Constraint) left.accept(this, null),
                    (Constraint) right.accept(this, null));
        }

        public Object visit(BindVariableValueImpl node, Object data)
                throws Exception {
            String name = node.getBindVariableName();
            Value value = bindVariableValues.get(name);
            if (value != null) {
                return value;
            } else {
                throw new RepositoryException(
                        "No value specified for bind variable " + name);
            }
        }

        public Object visit(ChildNodeImpl node, Object data) throws Exception {
            return new ChildNodeConstraint(node,
                    getSelector(node.getSelectorQName()));
        }

        public Object visit(ComparisonImpl node, Object data) throws Exception {
            DynamicOperandImpl op1 = (DynamicOperandImpl) node.getOperand1();
            Operator operator = node.getOperatorInstance();
            StaticOperandImpl op2 = ((StaticOperandImpl) node.getOperand2());
            Value staticValue = (Value) op2.accept(this, null);

            DynamicOperand dynOp = (DynamicOperand) op1.accept(this, staticValue);
            SelectorImpl selector = getSelector(op1.getSelectorQName());
            if (operator == Operator.LIKE) {
                return new LikeConstraint(dynOp, staticValue, selector);
            } else {
                return new ComparisonConstraint(
                        dynOp, operator, staticValue, selector);
            }
        }

        public Object visit(DescendantNodeImpl node, Object data)
                throws Exception {
            return new DescendantNodeConstraint(node,
                    getSelector(node.getSelectorQName()));
        }

        public Object visit(FullTextSearchImpl node, Object data)
                throws Exception {
            return new FullTextConstraint(node,
                    getSelector(node.getSelectorQName()), factory);
        }

        public Object visit(FullTextSearchScoreImpl node, Object data)
                throws Exception {
            return new FullTextSearchScoreOperand();
        }

        public Object visit(LengthImpl node, Object data) throws Exception {
            Value staticValue = (Value) data;
            // make sure it can be converted to Long
            try {
                staticValue.getLong();
            } catch (ValueFormatException e) {
                throw new InvalidQueryException("Static value " +
                        staticValue.getString() + " cannot be converted to a Long");
            }
            PropertyValueImpl propValue = (PropertyValueImpl) node.getPropertyValue();
            return new LengthOperand((PropertyValueOperand) propValue.accept(this, null));
        }

        public Object visit(LiteralImpl node, Object data) throws Exception {
            return node.getLiteralValue();
        }

        public Object visit(LowerCaseImpl node, Object data) throws Exception {
            DynamicOperandImpl operand = (DynamicOperandImpl) node.getOperand();
            return new LowerCaseOperand((DynamicOperand) operand.accept(this, data));
        }

        public Object visit(NodeLocalNameImpl node, Object data) throws Exception {
            return new NodeLocalNameOperand();
        }

        public Object visit(NodeNameImpl node, Object data) throws Exception {
            Value staticValue = (Value) data;
            switch (staticValue.getType()) {
                // STRING, PATH and URI may be convertable to a NAME -> check
                case PropertyType.STRING:
                case PropertyType.PATH:
                case PropertyType.URI:
                    // make sure static value is valid NAME
                    try {
                        String s = staticValue.getString();
                        if (staticValue.getType() == PropertyType.URI) {
                            if (s.startsWith("./")) {
                                s = s.substring(2);
                            }
                            // need to decode
                            s = URLDecoder.decode(s, "UTF-8");
                        }
                        vf.createValue(s, PropertyType.NAME);
                    } catch (ValueFormatException e) {
                            throw new InvalidQueryException("Value " +
                                staticValue.getString() +
                                " cannot be converted into NAME");
                    }
                    break;
                // the following types cannot be converted to NAME
                case PropertyType.DATE:
                case PropertyType.DOUBLE:
                case PropertyType.DECIMAL:
                case PropertyType.LONG:
                case PropertyType.BOOLEAN:
                case PropertyType.REFERENCE:
                case PropertyType.WEAKREFERENCE:
                    throw new InvalidQueryException(staticValue.getString() +
                            " cannot be converted into a NAME value");
            }

            return new NodeNameOperand();
        }

        public Object visit(NotImpl node, Object data) throws Exception {
            ConstraintImpl c = (ConstraintImpl) node.getConstraint();
            return new NotConstraint((Constraint) c.accept(this, null));
        }

        public Object visit(OrImpl node, Object data) throws Exception {
            ConstraintImpl left = (ConstraintImpl) node.getConstraint1();
            ConstraintImpl right = (ConstraintImpl) node.getConstraint2();
            return new OrConstraint((Constraint) left.accept(this, null),
                    (Constraint) right.accept(this, null));
        }

        public Object visit(PropertyExistenceImpl node, Object data)
                throws Exception {
            return new PropertyExistenceConstraint(node,
                    getSelector(node.getSelectorQName()), factory);
        }

        public Object visit(PropertyValueImpl node, Object data) throws Exception {
            return new PropertyValueOperand(node);
        }

        public Object visit(SameNodeImpl node, Object data) throws Exception {
            return new SameNodeConstraint(node,
                    getSelector(node.getSelectorQName()));
        }

        public Object visit(UpperCaseImpl node, Object data) throws Exception {
            DynamicOperandImpl operand = (DynamicOperandImpl) node.getOperand();
            return new UpperCaseOperand((DynamicOperand) operand.accept(this, data));
        }

        private SelectorImpl getSelector(Name name) {
            for (SelectorImpl selector : selectors) {
                if (selector.getSelectorQName().equals(name)) {
                    return selector;
                }
            }
            return null;
        }
    }
}
