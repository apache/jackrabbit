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

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

import org.apache.jackrabbit.spi.commons.query.jsr283.qom.Comparison;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.DynamicOperand;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.StaticOperand;

/**
 * <code>ComparisonImpl</code>...
 */
public class ComparisonImpl extends ConstraintImpl implements Comparison {

    /**
     * The first operand.
     */
    private final DynamicOperandImpl operand1;

    /**
     * The operator.
     */
    private final int operator;

    /**
     * The second operand.
     */
    private final StaticOperandImpl operand2;

    ComparisonImpl(NamePathResolver resolver,
                   DynamicOperandImpl operand1,
                   int operator,
                   StaticOperandImpl operand2) {
        super(resolver);
        this.operand1 = operand1;
        this.operator = operator;
        this.operand2 = operand2;
    }

    /**
     * Gets the first operand.
     *
     * @return the operand; non-null
     */
    public DynamicOperand getOperand1() {
        return operand1;
    }

    /**
     * Gets the operator.
     *
     * @return either <ul> <li>{@link org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModelConstants#OPERATOR_EQUAL_TO},</li>
     *         <li>{@link org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModelConstants#OPERATOR_NOT_EQUAL_TO},</li>
     *         <li>{@link org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModelConstants#OPERATOR_LESS_THAN},</li>
     *         <li>{@link org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModelConstants#OPERATOR_LESS_THAN_OR_EQUAL_TO},</li>
     *         <li>{@link org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModelConstants#OPERATOR_GREATER_THAN},</li>
     *         <li>{@link org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModelConstants#OPERATOR_GREATER_THAN_OR_EQUAL_TO},
     *         or</li> <li>{@link org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModelConstants#OPERATOR_LIKE}</li>
     *         </ul>
     */
    public int getOperator() {
        return operator;
    }

    /**
     * Gets the second operand.
     *
     * @return the operand; non-null
     */
    public StaticOperand getOperand2() {
        return operand2;
    }

    //------------------------< AbstractQOMNode >-------------------------------

    /**
     * Accepts a <code>visitor</code> and calls the appropriate visit method
     * depending on the type of this QOM node.
     *
     * @param visitor the visitor.
     */
    public Object accept(QOMTreeVisitor visitor, Object data) throws Exception {
        return visitor.visit(this, data);
    }
}
