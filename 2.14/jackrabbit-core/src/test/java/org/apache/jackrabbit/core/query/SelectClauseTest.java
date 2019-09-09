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
package org.apache.jackrabbit.core.query;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

/**
 * Performs tests with on the <code>SELECT</code> clause.
 */
public class SelectClauseTest extends AbstractQueryTest {

    public void testSelectSQL() throws RepositoryException {
        Node n = testRootNode.addNode("node1");
        n.setProperty("myvalue", "foo");
        n = testRootNode.addNode("node2");
        n.setProperty("myvalue", "bar");
        n = testRootNode.addNode("node3");
        n.setProperty("yourvalue", "foo");

        testRootNode.save();

        String sql = "SELECT myvalue FROM " + ntBase + " WHERE " +
                "jcr:path LIKE '" + testRoot + "/%' AND myvalue IS NOT NULL";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 2);

        sql = "SELECT myvalue FROM " + ntBase
                + " WHERE jcr:path LIKE '" + testRoot + "/%'"
                + " AND yourvalue = 'foo' AND myvalue IS NOT NULL";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 0);

        sql = "SELECT myvalue FROM " + ntBase + " WHERE myvalue IS NOT NULL";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 2);
    }

    public void testPropertyCountSQL() throws RepositoryException {
        Node n = testRootNode.addNode("node1");
        n.setProperty("myvalue", "foo");
        n = testRootNode.addNode("node2");
        n.setProperty("myvalue", "bar");
        n = testRootNode.addNode("node3");
        n.setProperty("yourvalue", "foo");

        testRootNode.save();

        String sql = "SELECT myvalue FROM " + ntBase + " WHERE " +
                "jcr:path LIKE '" + testRoot + "/%' AND myvalue IS NOT NULL";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 2, 2);

        sql = "SELECT myvalue FROM " + ntBase
                + " WHERE jcr:path LIKE '" + testRoot + "/%'"
                + " AND yourvalue = 'foo' AND myvalue IS NOT NULL";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 0, 0);

        sql = "SELECT myvalue FROM " + ntBase + " WHERE myvalue IS NOT NULL";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 2, 2);

        sql = "SELECT * FROM " + ntBase
                + " WHERE jcr:path LIKE '" + testRoot + "/%'"
                + " AND myvalue LIKE '%'";
        q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        result = q.execute();
        checkResult(result, 2, 2);
    }

    public void testSameNameSiblingSQL() throws RepositoryException {
        Node n = testRootNode.addNode("node");
        n.setProperty("myvalue", "foo");
        n = testRootNode.addNode("node");
        n.setProperty("myvalue", "bar");
        n = testRootNode.addNode("node");
        n.setProperty("yourvalue", "foo");

        testRootNode.save();

        String sql = "SELECT myvalue FROM " + ntBase + " WHERE " +
                "jcr:path LIKE '" + testRoot + "/node[%]' AND myvalue IS NOT NULL";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 2, 2);

    }

}
