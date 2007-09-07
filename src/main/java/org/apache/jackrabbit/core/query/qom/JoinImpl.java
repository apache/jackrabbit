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

import org.apache.jackrabbit.core.query.jsr283.qom.Join;
import org.apache.jackrabbit.core.query.jsr283.qom.Source;
import org.apache.jackrabbit.core.query.jsr283.qom.JoinCondition;

/**
 * <code>JoinImpl</code>...
 */
public class JoinImpl extends SourceImpl implements Join {

    /**
     * The left node-tuple source.
     */
    private final SourceImpl left;

    /**
     * The right node-tuple source.
     */
    private final SourceImpl right;

    /**
     * The join type.
     */
    private final int joinType;

    /**
     * The join condition.
     */
    private final JoinConditionImpl joinCondition;

    JoinImpl(NamePathResolver resolver,
             SourceImpl left,
             SourceImpl right,
             int joinType,
             JoinConditionImpl joinCondition) {
        super(resolver);
        this.left = left;
        this.right = right;
        this.joinType = joinType;
        this.joinCondition = joinCondition;
    }

    /**
     * Gets the left node-tuple source.
     *
     * @return the left source; non-null
     */
    public Source getLeft() {
        return left;
    }

    /**
     * Gets the right node-tuple source.
     *
     * @return the right source; non-null
     */
    public Source getRight() {
        return right;
    }

    /**
     * Gets the join type.
     *
     * @return either <ul> <li>{@link org.apache.jackrabbit.core.query.jsr283.qom.QueryObjectModelConstants#JOIN_TYPE_INNER},</li>
     *         <li>{@link org.apache.jackrabbit.core.query.jsr283.qom.QueryObjectModelConstants#JOIN_TYPE_LEFT_OUTER},</li>
     *         <li>{@link org.apache.jackrabbit.core.query.jsr283.qom.QueryObjectModelConstants#JOIN_TYPE_RIGHT_OUTER}</li>
     *         </ul>
     */
    public int getJoinType() {
        return joinType;
    }

    /**
     * Gets the join condition.
     *
     * @return the join condition; non-null
     */
    public JoinCondition getJoinCondition() {
        return joinCondition;
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
