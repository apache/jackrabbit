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
package org.apache.jackrabbit.test.api.query.qom;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.qom.JoinCondition;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelConstants;

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
        ensureMixinType(n2, mixReferenceable);
        superuser.save();
    }

    public void testInnerJoin() throws RepositoryException {
        QueryObjectModel qom = createQomQuery(QueryObjectModelConstants.JCR_JOIN_TYPE_INNER, null);
        checkQOM(qom, new Node[][]{{n1, n1}, {n2, n2}});
    }

    public void testInnerJoinWithPath() throws RepositoryException {
        QueryObjectModel qom = createQomQuery(QueryObjectModelConstants.JCR_JOIN_TYPE_INNER, nodeName2);
        checkQOM(qom, new Node[][]{{n2, n1}});
    }

    public void testLeftOuterJoin() throws RepositoryException {
       QueryObjectModel qom = qf.createQuery(
               qf.join(
                       qf.selector(testNodeType, LEFT),
                       qf.selector(mixReferenceable, RIGHT),
                       QueryObjectModelConstants.JCR_JOIN_TYPE_LEFT_OUTER,
                       qf.sameNodeJoinCondition(LEFT, RIGHT, ".")),
               qf.descendantNode(LEFT, testRoot),
               null, null);

        checkQOM(qom, new Node[][]{{n1, null}, {n2, n2}});
    }

    public void testLeftOuterJoinWithPath() throws RepositoryException {
        QueryObjectModel qom = createQomQuery(QueryObjectModelConstants.JCR_JOIN_TYPE_LEFT_OUTER, nodeName2);
        checkQOM(qom, new Node[][]{{n1, null}, {n2, n1}});
    }

    public void testRightOuterJoin() throws RepositoryException {
        QueryObjectModel qom = qf.createQuery(
                qf.join(
                        qf.selector(mixReferenceable, LEFT),
                        qf.selector(testNodeType, RIGHT),
                        QueryObjectModelConstants.JCR_JOIN_TYPE_RIGHT_OUTER,
                        qf.sameNodeJoinCondition(LEFT, RIGHT, ".")),
                qf.descendantNode(RIGHT, testRoot),
                null, null);

        checkQOM(qom, new Node[][]{{null, n1}, {n2, n2}});
    }

    public void testRightOuterJoinWithPath() throws RepositoryException {
        QueryObjectModel qom = qf.createQuery(
                qf.join(
                        qf.selector(mixReferenceable, LEFT),
                        qf.selector(testNodeType, RIGHT),
                        QueryObjectModelConstants.JCR_JOIN_TYPE_RIGHT_OUTER,
                        qf.sameNodeJoinCondition(LEFT, RIGHT, nodeName2)),
                qf.descendantNode(RIGHT, testRoot),
                null, null);

        checkQOM(qom, new Node[][]{{n2, n1}, {null, n2}});
    }

    //-----------------------------< utilities >--------------------------------

    private QueryObjectModel createQomQuery(String joinType, String relPath)
            throws RepositoryException {
        JoinCondition c;
        if (relPath != null) {
            c = qf.sameNodeJoinCondition(LEFT, RIGHT, relPath);
        } else {
            c = qf.sameNodeJoinCondition(LEFT, RIGHT, ".");
        }
        return createQuery(joinType, c);
    }
}
