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
 * Performance test for a two-way join that selects 300 pairs from
 * a set of 90k nodes. The query is constructed in a way that should
 * allow a smart implementation to perform the join quite efficiently.
 */
public class TwoWayJoinTest extends AbstractTest {

    private static final int NODE_COUNT = 300;

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
            Node node = root.addNode("node" + i, "nt:unstructured");
            node.setProperty("foo", i);
            for (int j = 0; j < NODE_COUNT; j++) {
                Node child = node.addNode("node" + j, "nt:unstructured");
                child.setProperty("bar", j);
            }
            session.save();
        }
    }

    public void runTest() throws Exception {
        int x = random.nextInt(NODE_COUNT);
        String query =
            "SELECT a.foo AS a, b.bar AS b"
            + " FROM [nt:unstructured] AS a"
            + " INNER JOIN [nt:unstructured] AS b ON a.foo = b.bar"
            + " WHERE a.foo = " + x;

        QueryManager manager = session.getWorkspace().getQueryManager();
        RowIterator iterator =
            manager.createQuery(query, "JCR-SQL2").execute().getRows();
        int count = 0;
        while (iterator.hasNext()) {
            Row row = iterator.nextRow();
            long a = row.getValue("a").getLong();
            long b = row.getValue("b").getLong();
            if (a != x || a != b) {
                throw new Exception(
                        "Invalid test result: " + x + " -> " + a + ", " + b);
            }
            count++;
        }
        if (count != NODE_COUNT) {
            throw new Exception(
                    "Invalid test result count: " + count + " !=" + NODE_COUNT);
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
