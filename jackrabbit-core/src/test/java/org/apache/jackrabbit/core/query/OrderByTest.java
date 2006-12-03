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
 * Tests queries with order by.
 */
public class OrderByTest extends AbstractQueryTest {

    public void testOrderByScore() throws RepositoryException {
        Node n1 = testRootNode.addNode("node1");
        Node n2 = testRootNode.addNode("node2");
        Node n3 = testRootNode.addNode("node3");

        n1.setProperty("text", "aaa");
        n1.setProperty("value", 3);
        n2.setProperty("text", "bbb");
        n2.setProperty("value", 2);
        n3.setProperty("text", "ccc");
        n3.setProperty("value", 2);

        testRootNode.save();

        String sql = "SELECT value FROM nt:unstructured WHERE " +
                "jcr:path LIKE '" + testRoot + "/%' ORDER BY jcr:score, value";
        Query q = superuser.getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
        QueryResult result = q.execute();
        checkResult(result, 3);

        String xpath = "/" + testRoot + "/*[@jcr:primaryType='nt:unstructured'] order by jcr:score(), @value";
        q = superuser.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        result = q.execute();
        checkResult(result, 3);
    }
}
