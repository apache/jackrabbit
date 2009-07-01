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
 * <code>EquiJoinConditionTest</code> contains test cases that cover
 * <code>EquiJoinCondition</code>.
 */
public class EquiJoinConditionTest extends AbstractJoinTest {

    private Node n1;

    private Node n2;

    protected void setUp() throws Exception {
        super.setUp();
        String value = createRandomString(10);
        n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.setProperty(propertyName1, value);

        n2 = n1.addNode(nodeName2, testNodeType);
        n2.setProperty(propertyName1, value);
        n2.setProperty(propertyName2, value);
        ensureMixinType(n2, mixReferenceable);
        superuser.save();
    }

    public void testInnerJoin1() throws RepositoryException {
        JoinCondition c = qf.equiJoinCondition(
                LEFT, propertyName1, RIGHT, propertyName2);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_JOIN_TYPE_INNER, c);
        checkQOM(qom, new Node[][]{{n1, n2}, {n2, n2}});
    }

    public void testInnerJoin2() throws RepositoryException {
        JoinCondition c = qf.equiJoinCondition(
                LEFT, propertyName2, RIGHT, propertyName1);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_JOIN_TYPE_INNER, c);
        checkQOM(qom, new Node[][]{{n2, n1}, {n2, n2}});
    }

    public void testRightOuterJoin1() throws RepositoryException {
        JoinCondition c = qf.equiJoinCondition(
                LEFT, propertyName1, RIGHT, propertyName2);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_JOIN_TYPE_RIGHT_OUTER, c);
        checkQOM(qom, new Node[][]{{null, n1}, {n1, n2}, {n2, n2}});
    }

    public void testRightOuterJoin2() throws RepositoryException {
        JoinCondition c = qf.equiJoinCondition(
                LEFT, propertyName2, RIGHT, propertyName1);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_JOIN_TYPE_RIGHT_OUTER, c);
        checkQOM(qom, new Node[][]{{n2, n1}, {n2, n2}});
    }

    public void testLeftOuterJoin1() throws RepositoryException {
        JoinCondition c = qf.equiJoinCondition(
                LEFT, propertyName1, RIGHT, propertyName2);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_JOIN_TYPE_LEFT_OUTER, c);
        checkQOM(qom, new Node[][]{{n1, n2}, {n2, n2}});
    }


    public void testLeftOuterJoin2() throws RepositoryException {
        JoinCondition c = qf.equiJoinCondition(
                LEFT, propertyName2, RIGHT, propertyName1);
        QueryObjectModel qom = createQuery(QueryObjectModelConstants.JCR_JOIN_TYPE_LEFT_OUTER, c);
        checkQOM(qom, new Node[][]{{n1, null}, {n2, n1}, {n2, n2}});
    }
}
