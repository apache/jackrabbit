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

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.query.qom.JoinCondition;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.Ordering;

/**
 * <code>OrderingTest</code> contains test cases that check 
 */
public class OrderingTest extends AbstractJoinTest {

    private Node n1;

    private Node n2;

    protected void setUp() throws Exception {
        super.setUp();
        String value = "a";
        n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.setProperty(propertyName1, value);
        n1.setProperty(propertyName2, "b");

        n2 = n1.addNode(nodeName2, testNodeType);
        n2.setProperty(propertyName1, value);
        n2.setProperty(propertyName2, value);
        superuser.save();
    }

    public void testMultipleSelectors() throws RepositoryException {
        // ascending
        Ordering[] orderings = new Ordering[]{
                qf.ascending(qf.propertyValue(LEFT, propertyName2))
        };
        QueryObjectModel qom = createQuery(orderings);
        checkResultOrder(qom, SELECTOR_NAMES, new Node[][]{{n2, n2}, {n1, n2}});

        // descending
        orderings[0] = qf.descending(qf.propertyValue(LEFT, propertyName2));
        qom = createQuery(orderings);
        checkResultOrder(qom, SELECTOR_NAMES, new Node[][]{{n1, n2}, {n2, n2}});
    }

    protected QueryObjectModel createQuery(Ordering[] orderings)
            throws RepositoryException {
        JoinCondition c = qf.equiJoinCondition(
                LEFT, propertyName1, RIGHT, propertyName2);
        QueryObjectModel qom = createQuery(
                QueryObjectModelConstants.JCR_JOIN_TYPE_INNER, c);
        return qf.createQuery(qom.getSource(), qom.getConstraint(),
                orderings, qom.getColumns());
    }
}
