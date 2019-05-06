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

import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.JoinCondition;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.Source;

import org.apache.jackrabbit.commons.query.qom.JoinType;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

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
    private final JoinType joinType;

    /**
     * The join condition.
     */
    private final JoinConditionImpl joinCondition;

    JoinImpl(NamePathResolver resolver,
             SourceImpl left,
             SourceImpl right,
             JoinType joinType,
             JoinConditionImpl joinCondition) {
        super(resolver);
        this.left = left;
        this.right = right;
        this.joinType = joinType;
        this.joinCondition = joinCondition;
    }

    public JoinType getJoinTypeInstance() {
        return joinType;
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
     * @return either <ul> <li>{@link QueryObjectModelConstants#JCR_JOIN_TYPE_INNER},</li>
     *         <li>{@link QueryObjectModelConstants#JCR_JOIN_TYPE_LEFT_OUTER},</li>
     *         <li>{@link QueryObjectModelConstants#JCR_JOIN_TYPE_RIGHT_OUTER}</li>
     *         </ul>
     */
    public String getJoinType() {
        return joinType.toString();
    }

    /**
     * Gets the join condition.
     *
     * @return the join condition; non-null
     */
    public JoinCondition getJoinCondition() {
        return joinCondition;
    }

    //---------------------------< SourceImpl >---------------------------------

    /**
     * {@inheritDoc}
     */
    public SelectorImpl[] getSelectors() {
        SelectorImpl[] leftSelectors = left.getSelectors();
        SelectorImpl[] rightSelectors = right.getSelectors();
        SelectorImpl[] both =
                new SelectorImpl[leftSelectors.length + rightSelectors.length];
        System.arraycopy(leftSelectors, 0, both, 0, leftSelectors.length);
        System.arraycopy(rightSelectors, 0, both, leftSelectors.length, rightSelectors.length);
        return both;
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
        return joinType.formatSql(left, right, joinCondition);
    }

}
