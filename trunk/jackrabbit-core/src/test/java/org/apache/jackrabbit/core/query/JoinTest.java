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

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        node = testRootNode.addNode("jointest", "nt:unstructured");

        Node n1a = node.addNode("n1a", "nt:unstructured");
        n1a.addMixin(NodeType.MIX_REFERENCEABLE);
        n1a.setProperty("type", "parent");

        Node n1b = node.addNode("n1b", "nt:unstructured");
        n1b.addMixin(NodeType.MIX_REFERENCEABLE);
        n1b.setProperty("type", "parent");
        n1b.setProperty("testJoinWithOR4", "testJoinWithOR4");

        Node n1c = node.addNode("n1c", "nt:unstructured");
        n1c.addMixin(NodeType.MIX_REFERENCEABLE);
        n1c.setProperty("type", "parent");

        Node n3 = node.addNode("node3", "nt:unstructured");
        n3.addMixin(NodeType.MIX_REFERENCEABLE);
        n3.setProperty("testref", new String[] { n1a.getIdentifier() },
                PropertyType.REFERENCE);
        n3.setProperty("type", "child");
        n3.setProperty("testJoinWithOR4", "testJoinWithOR4");

        Node n4 = node.addNode("node4", "nt:unstructured");
        n4.addMixin(NodeType.MIX_REFERENCEABLE);
        n4.setProperty("testref", new String[] { n1b.getIdentifier() },
                PropertyType.REFERENCE);
        n4.setProperty("type", "child");

        Node n5 = node.addNode("node5", "nt:unstructured");
        n5.addMixin(NodeType.MIX_REFERENCEABLE);
        n5.setProperty("testref", new String[] { n1c.getIdentifier() },
                PropertyType.REFERENCE);
        n5.setProperty("type", "child");

        Node parent2 = testRootNode
                .addNode("jointest_other", "nt:unstructured");
        parent2.setProperty("p", "abc");

        Node p2n1 = parent2.addNode("p2n1", "nt:unstructured");
        p2n1.setProperty("p", "abc");

        Node p2n2 = parent2.addNode("p2n2", "nt:unstructured");
        p2n2.setProperty("p", "xyz");

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
        checkResult(qm.createQuery(join, Query.JCR_SQL2).execute(), 3);
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
        checkResult(qm.createQuery(join, Query.JCR_SQL2).execute(), 3);
    }

    public void testJoinWithOR2() throws Exception {

        StringBuilder join = new StringBuilder(
                "SELECT a.* FROM [nt:unstructured] AS a");
        join.append("  INNER JOIN [nt:unstructured] AS b ON ISCHILDNODE(b, a) ");
        join.append("  WHERE  ");
        join.append("  a.[p] = 'abc' OR b.[p] = 'abc'  ");
        checkResult(qm.createQuery(join.toString(), Query.JCR_SQL2).execute(),
                3);
    }

    public void testJoinWithOR3() throws Exception {
        StringBuilder join = new StringBuilder(
                "SELECT a.* FROM [nt:unstructured] AS a");
        join.append("  INNER JOIN [nt:unstructured] AS b ON ISCHILDNODE(b, a) ");
        join.append("  WHERE  ");
        join.append("  ( CONTAINS(b.*, 'abc' ) OR CONTAINS(a.*, 'abc') )  ");
        join.append("  AND ");
        join.append("  NAME(b) = 'p2n2' ");
        checkResult(qm.createQuery(join.toString(), Query.JCR_SQL2).execute(),
                1);
    }

    public void testJoinWithOR4() throws Exception {

        StringBuilder join = new StringBuilder(
                "SELECT a.* FROM [nt:unstructured] AS a");
        join.append("  INNER JOIN [nt:unstructured] AS b ON b.[jcr:uuid] = a.testref ");
        join.append("  WHERE  ");
        join.append("  a.type = 'child' ");
        join.append("  AND (");
        join.append("    CONTAINS(a.*, 'testJoinWithOR4')  ");
        join.append("    OR ");
        join.append("    b.type = 'parent' ");
        join.append("    AND ");
        join.append("    CONTAINS(b.*, 'testJoinWithOR4') ");
        join.append("  )");

        Query q = qm.createQuery(join.toString(), Query.JCR_SQL2);
        QueryResult result = q.execute();
        checkResult(result, 2);

    }

    public void testJoinWithOR5() throws Exception {

        StringBuilder join = new StringBuilder(
                "SELECT a.* FROM [nt:unstructured] AS a");
        join.append("  INNER JOIN [nt:unstructured] AS b ON b.[jcr:uuid] = a.testref ");
        join.append("  WHERE  ");
        join.append("  a.type = 'child' AND CONTAINS(a.*, 'testJoinWithOR4') ");
        join.append("  OR ");
        join.append("  b.type = 'parent' AND CONTAINS(b.*, 'testJoinWithOR4') ");

        checkResult(qm.createQuery(join.toString(), Query.JCR_SQL2).execute(),
                2);
    }
}
