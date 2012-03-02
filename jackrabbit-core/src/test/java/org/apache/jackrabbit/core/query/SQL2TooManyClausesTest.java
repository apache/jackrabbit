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

import static javax.jcr.query.Query.JCR_SQL2;

import javax.jcr.Node;

/**
 * test for the usecases where there could be more than 1024 boolean clauses on
 * a query and the QueryEngine (usually) has to implement some sort of batching
 * 
 * @see {@link org.apache.lucene.search.BooleanQuery$TooManyClauses
 *      BooleanQuery#TooManyClauses}
 */
public class SQL2TooManyClausesTest extends AbstractQueryTest {

    private Node a;

    private Node boys;

    private Node girls;

    private final int nodes = 3300;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        a = testRootNode.addNode("teacher", "nt:unstructured");
        boys = a.addNode("boys", "nt:unstructured");
        girls = a.addNode("girls", "nt:unstructured");

        for (int i = 0; i < nodes; i++) {
            if (i % 2 == 0) {
                Node n = getOrCreateParent(boys, i).addNode("student" + i,
                        "nt:unstructured");
                n.setProperty("sex", "m");
            } else {
                Node n = getOrCreateParent(girls, i).addNode("student" + i,
                        "nt:unstructured");
                n.setProperty("sex", "f");
            }
            if (i % 100 == 0) {
                superuser.save();
            }
        }
        superuser.save();
    }

    private Node getOrCreateParent(Node node, int index) throws Exception {
        String indexAsString = index + "";
        int len = indexAsString.length();
        Node n = node;
        for (int i = 0; i < 3; i++) {
            String name = "0";
            if (i < len) {
                name = indexAsString.charAt(i) + "";
            }
            n = n.addNode(name);
        }
        return n;
    }

    /**
     * JCR-3108
     */
    public void testISDESCENDANTNODE() throws Exception {
        StringBuilder join = new StringBuilder();
        join.append("SELECT * FROM [nt:unstructured] as students WHERE ");
        join.append(" (ISDESCENDANTNODE([" + boys.getPath()
                + "]) OR ISDESCENDANTNODE([" + girls.getPath() + "])) ");
        join.append(" AND ( CONTAINS(students.sex,'m') OR CONTAINS(students.sex,'f') )");
        checkResult(qm.createQuery(join.toString(), JCR_SQL2).execute(), nodes);

    }
}
