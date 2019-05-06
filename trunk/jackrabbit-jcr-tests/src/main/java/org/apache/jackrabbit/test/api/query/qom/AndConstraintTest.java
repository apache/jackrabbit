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
import javax.jcr.query.QueryResult;
import javax.jcr.query.Query;

/**
 * <code>AndConstraintTest</code> contains tests that check AND constraints.
 */
public class AndConstraintTest extends AbstractQOMTest {

    public void testAnd() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.setProperty(propertyName1, "foo");
        n1.setProperty(propertyName2, "bar");
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        n2.setProperty(propertyName2, "bar");
        superuser.save();

        QueryResult result = qf.createQuery(
                qf.selector(testNodeType, "s"),
                qf.and(
                        qf.descendantNode("s", testRootNode.getPath()),
                        qf.and(
                                qf.propertyExistence("s", propertyName1),
                                qf.propertyExistence("s", propertyName2)
                        )
                ),
                null,
                null
        ).execute();
        checkResult(result, new Node[]{n1});

        String stmt = "SELECT * FROM [" + testNodeType + "] AS s WHERE " +
                "ISDESCENDANTNODE(s, [" + testRootNode.getPath() + "]) " +
                "AND s.[" + propertyName1 + "] IS NOT NULL " +
                "AND s.[" + propertyName2 + "] IS NOT NULL";
        result = qm.createQuery(stmt, Query.JCR_SQL2).execute();
        checkResult(result, new Node[]{n1});
    }
}
