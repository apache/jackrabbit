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
package org.apache.jackrabbit.test.api.query;

import javax.jcr.Node;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

/**
 * Test cases for order by queries on long properties.
 *
 * @tck.config testroot path to node that accepts child nodes of type
 * <code>nodetype</code>
 * @tck.config nodetype name of a node type
 * @tck.config nodename1 name of a child node of type <code>nodetype</code>
 * @tck.config nodename2 name of a child node of type <code>nodetype</code>
 * @tck.config nodename3 name of a child node of type <code>nodetype</code>
 * @tck.config nodename4 name of a child node of type <code>nodetype</code>
 * @tck.config propertyname1 name of a single value String property.
 * @tck.config propertyname2 name of a single value long property.
 * @test
 * @sources OrderByMultiTypeTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.OrderByMultiTypeTest
 * @keywords level2
 */
public class OrderByMultiTypeTest extends AbstractOrderByTest {

    /**
     * Tests order by queries with a String property and a long property.
     */
    public void testMultipleOrder() throws Exception {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        Node n3 = testRootNode.addNode(nodeName3, testNodeType);

        n1.setProperty(propertyName1, "aaa");
        n1.setProperty(propertyName2, 3);
        n2.setProperty(propertyName1, "bbb");
        n2.setProperty(propertyName2, 2);
        n3.setProperty(propertyName1, "ccc");
        n3.setProperty(propertyName2, 2);

        testRootNode.save();

        // both ascending
        String sql = "SELECT " + propertyName2 + " FROM " + testNodeType + " WHERE " +
                jcrPath + " LIKE '" + testRoot + "/%' ORDER BY " + propertyName2 + ", " + propertyName1;
        Query q;
        QueryResult result;
        if (checkSQL) {
            q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
            result = q.execute();
            checkResultOrder(result, new String[]{nodeName2, nodeName3, nodeName1});
        }

        String xpath = "/" + testRoot + "/*[@" + jcrPrimaryType + "='" + testNodeType +
                "'] order by @" + propertyName2 + ", @" + propertyName1;
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResultOrder(result, new String[]{nodeName2, nodeName3, nodeName1});

        // both descending
        sql = "SELECT " + propertyName2 + " FROM " + testNodeType + " WHERE " +
                jcrPath + " LIKE '" + testRoot + "/%' ORDER BY " +
                propertyName2 + " DESC, " +
                propertyName1 + " DESC";
        if (checkSQL) {
            q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
            result = q.execute();
            checkResultOrder(result, new String[]{nodeName1, nodeName3, nodeName2});
        }

        xpath = "/" + jcrRoot + testRoot + "/*[@" + jcrPrimaryType + "='" +
                testNodeType + "'] order by @" +
                propertyName2 + " descending, @" +
                propertyName1 + " descending";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResultOrder(result, new String[]{nodeName1, nodeName3, nodeName2});

        // mixed ascending and descending
        sql = "SELECT " + propertyName2 + " FROM " + testNodeType + " WHERE " +
                jcrPath + " LIKE '" + testRoot + "/%' ORDER BY " +
                propertyName2 + " DESC, " + propertyName1;
        if (checkSQL) {
            q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
            result = q.execute();
            checkResultOrder(result, new String[]{nodeName1, nodeName2, nodeName3});
        }

        xpath = "/" + jcrRoot + testRoot + "/*[@" + jcrPrimaryType + "='" +
                testNodeType + "'] order by @" + propertyName2 +
                " descending, @" + propertyName1;
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResultOrder(result, new String[]{nodeName1, nodeName2, nodeName3});
    }
}
