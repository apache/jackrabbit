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
package org.apache.jackrabbit.spi.commons.query.jsr283.qom;

/**
 * Filters node-tuples based on the outcome of a binary operation.
 * <p/>
 * For any comparison, {@link #getOperand2 operand2} always evaluates to a
 * scalar value.  In contrast, {@link #getOperand1 operand1} may evaluate to
 * an array of values (for example, the value of a multi-valued property),
 * in which case the comparison is separately performed for each element of
 * the array, and the <code>Comparison</code> constraint is satisfied as a
 * whole if the comparison against <i>any</i> element of the array is satisfied.
 * <p/>
 * If {@link #getOperand1 operand1} and {@link #getOperand2 operand2}
 * evaluate to values of different property types, the value of
 * {@link #getOperand2 operand2} is converted to the property type of
 * the value of {@link #getOperand1 operand1}.  If the type conversion
 * fails, the query is invalid.
 * <p/>
 * Certain operators may only be applied to values of certain property types.
 * The following describes required operator support for each property type:
 * <ul>
 * <li><code>STRING</code><ul>
 * <li><code>EqualTo</code>, <code>NotEqualTo</code>, <code>LessThan</code>,
 * <code>LessThanOrEqualTo</code>, <code>GreaterThan</code>,
 * <code>GreaterThanOrEqualTo</code>, <code>Like</code></li></ul></li>
 * <li><code>BINARY</code><ul>
 * <li>None</li></ul></li>
 * <li><code>DATE</code>, <code>LONG</code>, <code>DOUBLE</code>,
 * <code>DECIMAL</code><ul>
 * <li><code>EqualTo</code>, <code>NotEqualTo</code>, <code>LessThan</code>,
 * <code>LessThanOrEqualTo</code>, <code>GreaterThan</code>,
 * <code>GreaterThanOrEqualTo</code></li></ul></li>
 * <li><code>BOOLEAN</code>, <code>NAME</code>, <code>PATH</code>,
 * <code>REFERENCE</code>, <code>WEAKREFERENCE</code>, <code>URI</code><ul>
 * <li><code>EqualTo</code>, <code>NotEqualTo</code></li></ul></li>
 * </ul>
 * <p/>
 * If {@link #getOperator operator} is not supported for the property type of
 * {@link #getOperand1 operand1}, the query is invalid.
 * <p/>
 * If {@link #getOperand1 operand1} evaluates to null (for example, if the
 * operand evaluates the value of a property which does not exist), the
 * constraint is not satisfied.
 * <p/>
 * The <code>EqualTo</code> operator is satisfied <i>only if</i> the value of
 * {@link #getOperand1 operand1} equals the value of
 * {@link #getOperand2 operand2}.
 * <p/>
 * The <code>NotEqualTo</code> operator is satisfied <i>unless</i> the value of
 * {@link #getOperand1 operand1} equals the value of
 * {@link #getOperand2 operand2}.
 * <p/>
 * The <code>LessThan</code> operator is satisfied <i>only if</i> the value of
 * {@link #getOperand1 operand1} is ordered <i>before</i> the value of
 * {@link #getOperand2 operand2}.
 * <p/>
 * The <code>LessThanOrEqualTo</code> operator is satisfied <i>unless</i> the
 * value of {@link #getOperand1 operand1} is ordered <i>after</i> the value of
 * {@link #getOperand2 operand2}.
 * <p/>
 * The <code>GreaterThan</code> operator is satisfied <i>only if</i> the value
 * of {@link #getOperand1 operand1} is ordered <i>after</i> the value of
 * {@link #getOperand2 operand2}.
 * <p/>
 * The <code>GreaterThanOrEqualTo</code> operator is satisfied <i>unless</i> the
 * value of {@link #getOperand1 operand1} is ordered <i>before</i> the value of
 * {@link #getOperand2 operand2}.
 * <p/>
 * The <code>Like</code> operator is satisfied only if the value of
 * {@link #getOperand1 operand1} <i>matches</i> the pattern specified by the
 * value of {@link #getOperand2 operand2}, where in the pattern:
 * <ul>
 * <li>the character "<code>%</code>" matches zero or more characters, and</li>
 * <li>the character "<code>_</code>" (underscore) matches exactly one
 * character, and</li>
 * <li>the string "<code>\<i>x</i></code>" matches the character
 * "<code><i>x</i></code>", and</li>
 * <li>all other characters match themselves.</li>
 * </ul>
 *
 * @since JCR 2.0
 */
public interface Comparison extends Constraint {

    /**
     * Gets the first operand.
     *
     * @return the operand; non-null
     */
    DynamicOperand getOperand1();

    /**
     * Gets the operator.
     *
     * @return either
     *         <ul>
     *         <li>{@link QueryObjectModelConstants#OPERATOR_EQUAL_TO},</li>
     *         <li>{@link QueryObjectModelConstants#OPERATOR_NOT_EQUAL_TO},</li>
     *         <li>{@link QueryObjectModelConstants#OPERATOR_LESS_THAN},</li>
     *         <li>{@link QueryObjectModelConstants#OPERATOR_LESS_THAN_OR_EQUAL_TO},</li>
     *         <li>{@link QueryObjectModelConstants#OPERATOR_GREATER_THAN},</li>
     *         <li>{@link QueryObjectModelConstants#OPERATOR_GREATER_THAN_OR_EQUAL_TO}, or</li>
     *         <li>{@link QueryObjectModelConstants#OPERATOR_LIKE}</li>
     *         </ul>
     */
    int getOperator();

    /**
     * Gets the second operand.
     *
     * @return the operand; non-null
     */
    StaticOperand getOperand2();

}
