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
 * Defines constants used in the query object model.
 *
 * @since JCR 2.0
 */
public interface QueryObjectModelConstants {

    /**
     * An inner join.
     */
    int JOIN_TYPE_INNER = 101;

    /**
     * A left-outer join.
     */
    int JOIN_TYPE_LEFT_OUTER = 102;

    /**
     * A right-outer join.
     */
    int JOIN_TYPE_RIGHT_OUTER = 103;

    /**
     * The "<code>=</code>" comparison operator.
     */
    int OPERATOR_EQUAL_TO = 201;

    /**
     * The "<code>!=</code>" comparison operator.
     */
    int OPERATOR_NOT_EQUAL_TO = 202;

    /**
     * The "<code>&lt;</code>" comparison operator.
     */
    int OPERATOR_LESS_THAN = 203;

    /**
     * The "<code>&lt;=</code>" comparison operator.
     */
    int OPERATOR_LESS_THAN_OR_EQUAL_TO = 204;

    /**
     * The "<code>&gt;</code>" comparison operator.
     */
    int OPERATOR_GREATER_THAN = 205;

    /**
     * The "<code>&gt;=</code>" comparison operator.
     */
    int OPERATOR_GREATER_THAN_OR_EQUAL_TO = 206;

    /**
     * The "<code>like</code>" comparison operator.
     */
    int OPERATOR_LIKE = 207;

    /**
     * Ascending order.
     */
    int ORDER_ASCENDING = 301;

    /**
     * Descending order.
     */
    int ORDER_DESCENDING = 302;

}
