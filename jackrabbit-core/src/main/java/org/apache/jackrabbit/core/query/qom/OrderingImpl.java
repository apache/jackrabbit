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

import org.apache.jackrabbit.name.NamePathResolver;

import org.apache.jackrabbit.core.query.jsr283.qom.Ordering;
import org.apache.jackrabbit.core.query.jsr283.qom.DynamicOperand;

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
    private final int order;

    OrderingImpl(NamePathResolver resolver,
                 DynamicOperandImpl operand,
                 int order) {
        super(resolver);
        this.operand = operand;
        this.order = order;
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
     * @return either <ul> <li>{@link org.apache.jackrabbit.core.query.jsr283.qom.QueryObjectModelConstants#ORDER_ASCENDING}
     *         or</li> <li>{@link org.apache.jackrabbit.core.query.jsr283.qom.QueryObjectModelConstants#ORDER_DESCENDING}</li>
     *         </ul>
     */
    public int getOrder() {
        return order;
    }


    //------------------------< AbstractQOMNode >-------------------------------

    /**
     * Accepts a <code>visitor</code> and calls the appropriate visit method
     * depending on the type of this QOM node.
     *
     * @param visitor the visitor.
     */
    public void accept(QOMTreeVisitor visitor, Object data) {
        visitor.visit(this, data);
    }
}
