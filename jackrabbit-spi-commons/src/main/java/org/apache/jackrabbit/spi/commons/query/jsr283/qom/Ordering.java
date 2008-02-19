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
 * Determines the relative order of two node-tuples by evaluating
 * {@link #getOperand operand} for each.
 * <p/>
 * For a first node-tuple, <code>nt1</code>, for which {@link #getOperand operand}
 * evaluates to <code>v1</code>, and a second node-tuple, <code>nt2</code>, for
 * which {@link #getOperand operand} evaluates to <code>v2</code>:
 * <ul>
 * <li>If {@link #getOrder order} is <code>Ascending</code>, then:<ul>
 * <li>if either <code>v1</code> is null, <code>v2</code> is null, or both
 * <code>v1</code> and <code>v2</code> are null, the relative order of
 * <code>nt1</code> and <code>nt2</code> is implementation determined,
 * otherwise</li>
 * <li>if <code>v1</code> is a different property type than <code>v2</code>,
 * the relative order of <code>nt1</code> and <code>nt2</code> is
 * implementation determined, otherwise</li>
 * <li>if <code>v1</code> is ordered before <code>v2</code>, then
 * <code>nt1</code> precedes <code>nt2</code>, otherwise</li>
 * <li>if <code>v1</code> is ordered after <code>v2</code>, then
 * <code>nt2</code> precedes <code>nt1</code>, otherwise</li>
 * <li>the relative order of <code>nt1</code> and <code>nt2</code> is
 * implementation determined and may be arbitrary.</li></ul></li>
 * <li>Otherwise, if {@link #getOrder order} is <code>Descending</code>, then:<ul>
 * <li>if either <code>v1</code> is null, <code>v2</code> is null, or both
 * <code>v1</code> and <code>v2</code> are null, the relative order of
 * <code>nt1</code> and <code>nt2</code> is implementation determined,
 * otherwise</li>
 * <li>if <code>v1</code> is a different property type than <code>v2</code>,
 * the relative order of <code>nt1</code> and <code>nt2</code> is
 * implementation determined, otherwise</li>
 * <li>if <code>v1</code> is ordered before <code>v2</code>, then
 * <code>nt2</code> precedes <code>nt1</code>, otherwise</li>
 * <li>if <code>v1</code> is ordered after <code>v2</code>, then
 * <code>nt1</code> precedes <code>nt2</code>, otherwise</li>
 * <li>the relative order of <code>nt1</code> and <code>nt2</code> is
 * implementation determined and may be arbitrary.</li></ul></li>
 * </ul>
 * The query is invalid if {@link #getOperand operand} does not evaluate to a
 * scalar value.
 *
 * @since JCR 2.0
 */
public interface Ordering {

    /**
     * The operand by which to order.
     *
     * @return the operand; non-null
     */
    DynamicOperand getOperand();

    /**
     * Gets the order.
     *
     * @return either
     *         <ul>
     *         <li>{@link QueryObjectModelConstants#ORDER_ASCENDING} or</li>
     *         <li>{@link QueryObjectModelConstants#ORDER_DESCENDING}</li>
     *         </ul>
     */
    int getOrder();

}
