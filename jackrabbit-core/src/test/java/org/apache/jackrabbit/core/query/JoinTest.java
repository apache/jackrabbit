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
import javax.jcr.PropertyType;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

/**
 * Test case for JOIN queries with JCR_SQL2
 */
public class JoinTest extends AbstractQueryTest {

    private Node node;

    private Node n1;

    private Node n2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        node = testRootNode.addNode("jointest", "nt:unstructured");

        n1 = node.addNode("node1", "nt:unstructured");
        n1.addMixin(NodeType.MIX_REFERENCEABLE);

        n2 = node.addNode("node2", "nt:unstructured");
        n2.addMixin(NodeType.MIX_REFERENCEABLE);

        Node n3 = node.addNode("node3", "nt:unstructured");
        n3.addMixin(NodeType.MIX_REFERENCEABLE);
        n3.setProperty("testref",
                new String[] { n1.getIdentifier(), n2.getIdentifier() },
                PropertyType.REFERENCE);

        Node parent2 = testRootNode
                .addNode("jointest_other", "nt:unstructured");
        parent2.setProperty("p", "abc");

        Node p2n1 = parent2.addNode("node4", "nt:unstructured");
        p2n1.setProperty("p", "abc");

        parent2.addNode("node5", "nt:unstructured");

        testRootNode.getSession().save();
    }

    @Override
    protected void tearDown() throws Exception {
        node.remove();
        testRootNode.getSession().save();
        super.tearDown();
    }

    /**
     * Test case for <a
     * href="https://issues.apache.org/jira/browse/JCR-2718">JCR-2718</a>
     */
    public void testMultiValuedReferenceJoin() throws Exception {
        String join = "SELECT a.*, b.*"
                + " FROM [nt:unstructured] AS a"
                + " INNER JOIN [nt:unstructured] AS b ON a.[jcr:uuid] = b.testref";
        checkResult(qm.createQuery(join, Query.JCR_SQL2).execute(), 2);
    }

    /**
     * Test case for <a
     * href="https://issues.apache.org/jira/browse/JCR-2852">JCR-2852</a>
     */
    public void testJoinWithOR() throws Exception {

        String join = "SELECT a.*, b.*"
                + " FROM [nt:unstructured] AS a"
                + " INNER JOIN [nt:unstructured] AS b ON a.[jcr:uuid] = b.testref WHERE "
                + "a.[jcr:primaryType] IS NOT NULL OR b.[jcr:primaryType] IS NOT NULL";

        Query q = qm.createQuery(join, Query.JCR_SQL2);
        QueryResult result = q.execute();
        checkResult(result, 2);
    }

    public void testJoinWithOR2() throws Exception {

        String join = "SELECT a.*, b.*"
                + " FROM [nt:unstructured] AS a"
                + " INNER JOIN [nt:unstructured] AS b ON ISCHILDNODE(b, a) WHERE "
                + "a.[p] = 'abc' or b.[p] = 'abc' ";

        Query q = qm.createQuery(join, Query.JCR_SQL2);
        QueryResult result = q.execute();
        checkResult(result, 2);
    }

    public void testJoinWithOR3() throws Exception {
        StringBuilder join = new StringBuilder(
                "SELECT a.* FROM [nt:unstructured] AS a");
        join.append("  INNER JOIN [nt:unstructured] AS b ON ISCHILDNODE(b, a) ");
        join.append("  WHERE  ");
        join.append("  ( CONTAINS(b.*, 'abc' ) OR CONTAINS(a.*, 'abc') )  ");
        join.append("  AND ");
        join.append("  NAME(b) = 'node4' ");

        Query q = qm.createQuery(join.toString(), Query.JCR_SQL2);
        QueryResult result = q.execute();
        checkResult(result, 1);
    }

    /**
     * Test case for <a
     * href="https://issues.apache.org/jira/browse/JCR-2852">JCR-2852</a> <br>
     * <p>
     * Test inspired by <a
     * href="http://markmail.org/message/gee5yyygozestsml">this discussion</a>
     */
    public void testMegaJoin() throws Exception {

        // WHERE
        // ( (ISSAMENODE(projects,
        // '/repository/projects/U970f5509-54de-46d8-88bd-bc1a94ab85eb')))
        // AND
        // ( ( ISDESCENDANTNODE( projects, '/repository/projects') AND
        // eventclassassociations.active = true )
        // or
        // ( ISDESCENDANTNODE( projects, '/repository/template') )
        // )
        // AND ((NAME(parentRelationshipStatus) = 'parentRelationshipStatus'))

        StringBuilder join = new StringBuilder(
                "SELECT a.*, b.* FROM [nt:unstructured] AS a");
        join.append("  INNER JOIN [nt:unstructured] AS b ON a.[jcr:uuid] = b.testref ");
        join.append("  WHERE  ");
        join.append("  ISSAMENODE(b, '/testroot/jointest/node3') ");
        join.append("  AND ");
        join.append("  ( ");
        join.append("    ( ");
        join.append("    ISDESCENDANTNODE(b, '/testroot/jointest') ");
        join.append("    AND ");
        join.append("    b.testref IS NOT NULL ");
        join.append("    ) ");
        join.append("    OR ");
        join.append("    ISDESCENDANTNODE(a, '/testroot/jointest') ");
        join.append("  ) ");
        join.append("  AND ");
        join.append(" (NAME(b) = 'node3') ");

        Query q = qm.createQuery(join.toString(), Query.JCR_SQL2);
        QueryResult result = q.execute();
        checkResult(result, 2);
    }
}
