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
 * Performs a join between two node-tuple sources.
 * <p/>
 * The query is invalid if {@link #getLeft left} is the same source as
 * {@link #getRight right}.
 *
 * @since JCR 2.0
 */
public interface Join extends Source {

    /**
     * Gets the left node-tuple source.
     *
     * @return the left source; non-null
     */
    Source getLeft();

    /**
     * Gets the right node-tuple source.
     *
     * @return the right source; non-null
     */
    Source getRight();

    /**
     * Gets the join type.
     *
     * @return either
     *         <ul>
     *         <li>{@link QueryObjectModelConstants#JOIN_TYPE_INNER},</li>
     *         <li>{@link QueryObjectModelConstants#JOIN_TYPE_LEFT_OUTER},</li>
     *         <li>{@link QueryObjectModelConstants#JOIN_TYPE_RIGHT_OUTER}</li>
     *         </ul>
     */
    int getJoinType();

    /**
     * Gets the join condition.
     *
     * @return the join condition; non-null
     */
    JoinCondition getJoinCondition();

}
