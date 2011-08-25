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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.commons.JcrUtils;

/**
 * Tests queries with order by.
 */
public class SQL2OrderByTest extends AbstractQueryTest {

    // TODO order by aggregate test?

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        for (Node c : JcrUtils.getChildNodes(testRootNode)) {
            testRootNode.getSession().removeItem(c.getPath());
        }
        testRootNode.getSession().save();
        super.tearDown();
    }

    public void testOrderByScore() throws RepositoryException {
        Node n1 = testRootNode.addNode("node1");
        Node n2 = testRootNode.addNode("node2");
        Node n3 = testRootNode.addNode("node3");

        n1.setProperty("text", "aaa");
        n1.setProperty("value", 3);
        testRootNode.getSession().save();

        n2.setProperty("text", "bbb");
        n2.setProperty("value", 2);
        testRootNode.getSession().save();

        n3.setProperty("text", "ccc");
        n3.setProperty("value", 2);
        testRootNode.getSession().save();

        QueryResult qr = executeSQL2Query("SELECT * FROM [nt:base] WHERE ISCHILDNODE(["
                + testRoot + "]) ORDER BY [jcr:score]");
        checkSeq(qr, new Node[] { n1, n2, n3 });

    }

    public void testOrderByVal() throws RepositoryException {

        Node n1 = testRootNode.addNode("node1");
        Node n2 = testRootNode.addNode("node2");
        Node n3 = testRootNode.addNode("node3");

        n1.setProperty("text", "aaa");
        n1.setProperty("value", 3);
        n2.setProperty("text", "bbb");
        n2.setProperty("value", 2);
        n3.setProperty("text", "ccc");
        n3.setProperty("value", 1);

        testRootNode.getSession().save();

        QueryResult qr = executeSQL2Query("SELECT * FROM [nt:base] WHERE ISCHILDNODE(["
                + testRoot + "]) ORDER BY [value] desc");
        checkSeq(qr, new Node[] { n1, n2, n3 });
    }

    public void testOrderByVal2() throws RepositoryException {

        Node n1 = testRootNode.addNode("node1");
        Node n2 = testRootNode.addNode("node2");
        Node n3 = testRootNode.addNode("node3");

        n1.setProperty("text", "aaa");
        n1.setProperty("value", 3);
        n2.setProperty("text", "bbb");
        n2.setProperty("value", 2);
        n3.setProperty("text", "ccc");
        n3.setProperty("value", 1);

        testRootNode.getSession().save();

        QueryResult qr = executeSQL2Query("SELECT * FROM [nt:base] WHERE ISCHILDNODE(["
                + testRoot + "]) ORDER BY [value]");
        checkSeq(qr, new Node[] { n3, n2, n1 });
    }

    public void testOrderByFunction() throws RepositoryException {

        Node n1 = testRootNode.addNode("node1", "nt:unstructured");
        Node n2 = testRootNode.addNode("node2", "nt:unstructured");
        Node n3 = testRootNode.addNode("node3", "nt:unstructured");

        n1.setProperty("t", "aa");
        n1.setProperty("value", 3);
        n2.setProperty("t", "bbb");
        n2.setProperty("value", 2);
        n3.setProperty("t", "ccc");
        n3.setProperty("value", 1);

        testRootNode.getSession().save();

        QueryResult qr = executeSQL2Query("SELECT * FROM [nt:base] as sissy WHERE ISCHILDNODE(["
                + testRoot + "]) ORDER BY LENGTH([t]), t");
        checkSeq(qr, new Node[] { n1, n2, n3 });
    }

    public void testOrderByFunction2() throws RepositoryException {

        Node n1 = testRootNode.addNode("node1", "nt:unstructured");
        Node n2 = testRootNode.addNode("node2", "nt:unstructured");
        Node n3 = testRootNode.addNode("node3", "nt:unstructured");

        n1.setProperty("t", "aa");
        n1.setProperty("value", 3);
        n2.setProperty("t", "bbb");
        n2.setProperty("value", 2);
        n3.setProperty("t", "ccc");
        n3.setProperty("value", 1);

        testRootNode.getSession().save();

        QueryResult qr = executeSQL2Query("SELECT * FROM [nt:base] as sissy WHERE ISCHILDNODE(["
                + testRoot + "]) ORDER BY LENGTH([t]), t desc");
        checkSeq(qr, new Node[] { n1, n3, n2 });
    }

    private void checkSeq(QueryResult qr, Node[] nodes)
            throws RepositoryException {
        NodeIterator ni = qr.getNodes();
        for (Node n : nodes) {
            assertTrue(ni.hasNext());
            assertEquals(n.getPath(), ni.nextNode().getPath());
        }
    }

}
