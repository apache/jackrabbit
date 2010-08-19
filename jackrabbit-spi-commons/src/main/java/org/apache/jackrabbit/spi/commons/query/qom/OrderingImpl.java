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

import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.QueryObjectModelConstants;

import org.apache.jackrabbit.commons.query.qom.Order;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

/**
 * <code>OrderingImpl</code>...
 */
public class OrderingImpl extends AbstractQOMNode implements Ordering {

    /**
     * Empty <code>OrderingImpl</code> array.
     */
    public static final OrderingImpl[] EMPTY_ARRAY = new OrderingImpl[0];

    /**
     * Operand by which to order.
     */
    private final DynamicOperandImpl operand;

    /**
     * The order.
     */
    private final Order order;

    OrderingImpl(NamePathResolver resolver,
                 DynamicOperandImpl operand,
                 String order) {
        super(resolver);
        this.operand = operand;
        this.order = Order.getOrderByName(order);
    }

    /**
     * The operand by which to order.
     *
     * @return the operand; non-null
     */
    public DynamicOperand getOperand() {
        return operand;
    }

    /**
     * Gets the order.
     *
     * @return either <ul> <li>{@link QueryObjectModelConstants#JCR_ORDER_ASCENDING}
     *         or</li> <li>{@link QueryObjectModelConstants#JCR_ORDER_DESCENDING}</li>
     *         </ul>
     */
    public String getOrder() {
        return order.getName();
    }

    /**
     * @return <code>true</code> if this ordering is ascending. Returns
     *         <code>false</code> if ordering is descending.
     */
    public boolean isAscending() {
        return order == Order.ASCENDING;
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

    //------------------------< Object >----------------------------------------

    public String toString() {
        if (order == Order.ASCENDING) {
            return operand + " ASC";
        } else {
            return operand + " DESC";
        }
    }

}
