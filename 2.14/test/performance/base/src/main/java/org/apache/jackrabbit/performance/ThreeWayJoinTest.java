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
package org.apache.jackrabbit.performance;

import java.util.Random;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryManager;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 * Performance test for a three-way join that selects 50 triples from
 * a set of 125k nodes. The query is constructed in a way that should
 * allow a smart implementation to perform the join quite efficiently.
 */
public class ThreeWayJoinTest extends AbstractTest {

    private static final int NODE_COUNT = 30;

    private final Random random = new Random();

    private Session session;

    private Node root;

    public void beforeSuite() throws RepositoryException {
        String joins = getRepository().getDescriptor("query.joins");
        if (joins == null || joins.equals("query.joins.none")) {
            throw new RepositoryException(
                    "Join queries not supported by this repository");
        }

        session = loginWriter();
        root = session.getRootNode().addNode("testroot", "nt:unstructured");

        for (int i = 0; i < NODE_COUNT; i++) {
            Node foo = root.addNode("node" + i, "nt:unstructured");
            foo.setProperty("foo", i);
            for (int j = 0; j < NODE_COUNT; j++) {
                Node bar = foo.addNode("node" + j, "nt:unstructured");
                bar.setProperty("bar", j);
                for (int k = 0; k < NODE_COUNT; k++) {
                    Node baz = bar.addNode("node" + k, "nt:unstructured");
                    baz.setProperty("baz", k);
                }
            }
            session.save();
        }
    }

    public void runTest() throws Exception {
        int x = random.nextInt(NODE_COUNT);
        String query =
            "SELECT a.foo AS a, b.bar AS b, c.baz AS c"
            + " FROM [nt:unstructured] AS a"
            + " INNER JOIN [nt:unstructured] AS b ON a.foo = b.bar"
            + " INNER JOIN [nt:unstructured] AS c ON b.bar = c.baz"
            + " WHERE a.foo = " + x;

        QueryManager manager = session.getWorkspace().getQueryManager();
        RowIterator iterator =
            manager.createQuery(query, "JCR-SQL2").execute().getRows();
        int count = 0;
        while (iterator.hasNext()) {
            Row row = iterator.nextRow();
            long a = row.getValue("a").getLong();
            long b = row.getValue("b").getLong();
            long c = row.getValue("c").getLong();
            if (a != x || b != x || c != x) {
                throw new Exception(
                        "Invalid test result: "
                        + x + " -> " + a + ", " + b + ", " + c);
            }
            count++;
        }
        if (count != NODE_COUNT * NODE_COUNT * NODE_COUNT) {
            throw new Exception(
                    "Invalid test result count: " + count);
        }
    }

    public void afterSuite() throws RepositoryException {
        for (int i = 0; i < NODE_COUNT; i++) {
            root.getNode("node" + i).remove();
            session.save();
        }

        root.remove();
        session.save();
    }

}
