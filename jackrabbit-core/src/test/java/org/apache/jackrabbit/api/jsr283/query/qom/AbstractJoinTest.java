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
package org.apache.jackrabbit.api.jsr283.query.qom;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.JoinCondition;
import javax.jcr.query.qom.QueryObjectModel;

import org.apache.jackrabbit.spi.commons.query.qom.JoinType;

/**
 * <code>AbstractJoinTest</code> provides utility methods for join related
 * tests.
 */
public abstract class AbstractJoinTest extends AbstractQOMTest {

    /**
     * Name of the left selector.
     */
    protected static final String LEFT = "left";

    /**
     * Name of the right selector.
     */
    protected static final String RIGHT = "right";

    /**
     * The selector names for the join.
     */
    protected static final String[] SELECTOR_NAMES = new String[]{LEFT, RIGHT};

    //--------------------------< utilities >-----------------------------------

    protected void checkResult(QueryResult result, Node[][] nodes)
            throws RepositoryException {
        checkResult(result, SELECTOR_NAMES, nodes);
    }

    protected QueryObjectModel createQuery(JoinType joinType,
                                           JoinCondition condition)
            throws RepositoryException {
        return createQuery(joinType, condition, null, null);
    }

    protected QueryObjectModel createQuery(JoinType joinType,
                                           JoinCondition condition,
                                           Constraint left,
                                           Constraint right)
            throws RepositoryException {
        // only consider nodes under test root
        Constraint constraint;
        if (JoinType.LEFT == joinType) {
            constraint = qomFactory.descendantNode(LEFT, testRoot);
        } else {
            constraint = qomFactory.descendantNode(RIGHT, testRoot);
        }

        if (left != null) {
            constraint = qomFactory.and(constraint, left);
        }
        if (right != null) {
            constraint = qomFactory.and(constraint, right);
        }
        Join join = joinType.join(
                qomFactory,
                qomFactory.selector(testNodeType, LEFT),
                qomFactory.selector(testNodeType, RIGHT),
                condition);
        return qomFactory.createQuery(join, constraint, null, null);
    }
}
