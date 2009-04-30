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

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModel;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.JoinCondition;

/**
 * <code>SameNodeJoinConditionTest</code> contains test cases that cover
 * <code>SameNodeJoinCondition</code>.
 */
public class SameNodeJoinConditionTest extends AbstractJoinTest {

    private Node n1;

    private Node n2;

    protected void setUp() throws Exception {
        super.setUp();
        n1 = testRootNode.addNode(nodeName1, testNodeType);
        n2 = n1.addNode(nodeName2, testNodeType);
        n2.addMixin(mixReferenceable);
        testRootNode.save();
    }

    public void testInnerJoin() throws RepositoryException {
        QueryObjectModel qom = createQuery(JOIN_TYPE_INNER, (String) null);
        QueryResult result = qom.execute();
        checkResult(result, new Node[][]{{n1, n1}, {n2, n2}});
    }

    public void testInnerJoinWithPath() throws RepositoryException {
        QueryObjectModel qom = createQuery(JOIN_TYPE_INNER, nodeName2);
        QueryResult result = qom.execute();
        checkResult(result, new Node[][]{{n2, n1}});
    }

    public void testLeftOuterJoin() throws RepositoryException {
       QueryObjectModel qom = qomFactory.createQuery(
               qomFactory.join(
                        qomFactory.selector(testNodeType, LEFT),
                        qomFactory.selector(mixReferenceable, RIGHT),
                        JOIN_TYPE_LEFT_OUTER,
                        qomFactory.sameNodeJoinCondition(LEFT, RIGHT)
                ),
               qomFactory.descendantNode(LEFT, testRoot),
               null, null);

        QueryResult result = qom.execute();
        checkResult(result, new Node[][]{{n1, null}, {n2, n2}});
    }

    public void testLeftOuterJoinWithPath() throws RepositoryException {
        QueryObjectModel qom = createQuery(JOIN_TYPE_LEFT_OUTER, nodeName2);
        QueryResult result = qom.execute();
        checkResult(result, new Node[][]{{n1, null}, {n2, n1}});
    }

    public void testRightOuterJoin() throws RepositoryException {
        QueryObjectModel qom = qomFactory.createQuery(
                qomFactory.join(
                         qomFactory.selector(mixReferenceable, LEFT),
                         qomFactory.selector(testNodeType, RIGHT),
                         JOIN_TYPE_RIGHT_OUTER,
                         qomFactory.sameNodeJoinCondition(LEFT, RIGHT)
                 ),
                qomFactory.descendantNode(RIGHT, testRoot),
                null, null);

        QueryResult result = qom.execute();
        checkResult(result, new Node[][]{{null, n1}, {n2, n2}});
    }

    public void testRightOuterJoinWithPath() throws RepositoryException {
        QueryObjectModel qom = qomFactory.createQuery(
                qomFactory.join(
                         qomFactory.selector(mixReferenceable, LEFT),
                         qomFactory.selector(testNodeType, RIGHT),
                         JOIN_TYPE_RIGHT_OUTER,
                         qomFactory.sameNodeJoinCondition(LEFT, RIGHT, nodeName2)
                 ),
                qomFactory.descendantNode(RIGHT, testRoot),
                null, null);

        QueryResult result = qom.execute();
        checkResult(result, new Node[][]{{n2, n1}, {null, n2}});
    }

    //-----------------------------< utilities >--------------------------------

    private QueryObjectModel createQuery(int joinType, String relPath)
            throws RepositoryException {
        JoinCondition c;
        if (relPath != null) {
            c = qomFactory.sameNodeJoinCondition(LEFT, RIGHT, relPath);
        } else {
            c = qomFactory.sameNodeJoinCondition(LEFT, RIGHT);
        }
        return createQuery(joinType, c);
    }
}
