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
    // TODO enable the test once the native sort is properly handled.

    static {
        // To see JCR-2959 in action, enable the following
        // System.setProperty(QueryEngine.NATIVE_SORT_SYSTEM_PROPERTY, "true");
    }

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

        n1.setProperty("value", 3);
        n2.setProperty("value", 1);
        n3.setProperty("value", 2);

        testRootNode.getSession().save();

        QueryResult qr = executeSQL2Query("SELECT * FROM [nt:base] WHERE ISCHILDNODE(["
                + testRoot + "]) ORDER BY [value]");
        checkSeq(qr, new Node[] { n2, n3, n1 });
    }

    public void testOrderByValDesc() throws RepositoryException {

        Node n1 = testRootNode.addNode("node1");
        Node n2 = testRootNode.addNode("node2");
        Node n3 = testRootNode.addNode("node3");

        n1.setProperty("value", 3);
        n2.setProperty("value", 1);
        n3.setProperty("value", 2);

        testRootNode.getSession().save();

        QueryResult qr = executeSQL2Query("SELECT * FROM [nt:base] WHERE ISCHILDNODE(["
                + testRoot + "]) ORDER BY [value] desc");
        checkSeq(qr, new Node[] { n1, n3, n2 });
    }

    public void testOrderByValMult() throws RepositoryException {

        Node n1 = testRootNode.addNode("node1");
        Node n2 = testRootNode.addNode("node2");
        Node n3 = testRootNode.addNode("node3");

        n1.setProperty("value", 2);
        n1.setProperty("text", "b");

        n2.setProperty("value", 1);
        n2.setProperty("text", "x");

        n3.setProperty("value", 2);
        n3.setProperty("text", "a");

        testRootNode.getSession().save();

        QueryResult qr = executeSQL2Query("SELECT * FROM [nt:base] WHERE ISCHILDNODE(["
                + testRoot + "]) ORDER BY [value], [text]");
        checkSeq(qr, new Node[] { n2, n3, n1 });
    }

    public void testOrderByValMultDesc() throws RepositoryException {

        Node n1 = testRootNode.addNode("node1");
        Node n2 = testRootNode.addNode("node2");
        Node n3 = testRootNode.addNode("node3");

        n1.setProperty("value", 2);
        n1.setProperty("text", "b");

        n2.setProperty("value", 1);
        n2.setProperty("text", "x");

        n3.setProperty("value", 2);
        n3.setProperty("text", "a");

        testRootNode.getSession().save();

        QueryResult qr = executeSQL2Query("SELECT * FROM [nt:base] WHERE ISCHILDNODE(["
                + testRoot + "]) ORDER BY [value] desc, [text] desc");
        checkSeq(qr, new Node[] { n1, n3, n2 });
    }

    public void testOrderByValMultMix() throws RepositoryException {

        Node n1 = testRootNode.addNode("node1");
        Node n2 = testRootNode.addNode("node2");
        Node n3 = testRootNode.addNode("node3");

        n1.setProperty("value", 2);
        n1.setProperty("text", "b");

        n2.setProperty("value", 1);
        n2.setProperty("text", "x");

        n3.setProperty("value", 2);
        n3.setProperty("text", "a");

        testRootNode.getSession().save();

        QueryResult qr = executeSQL2Query("SELECT * FROM [nt:base] WHERE ISCHILDNODE(["
                + testRoot + "]) ORDER BY [value], [text] desc");
        checkSeq(qr, new Node[] { n2, n1, n3 });
    }

    public void testOrderByFnc() throws RepositoryException {

        Node n1 = testRootNode.addNode("node1", "nt:unstructured");
        Node n2 = testRootNode.addNode("node2", "nt:unstructured");
        Node n3 = testRootNode.addNode("node3", "nt:unstructured");

        n1.setProperty("value", "aaa");
        n2.setProperty("value", "a");
        n3.setProperty("value", "aa");

        testRootNode.getSession().save();

        QueryResult qr = executeSQL2Query("SELECT * FROM [nt:base] WHERE ISCHILDNODE(["
                + testRoot + "]) ORDER BY LENGTH([value])");
        checkSeq(qr, new Node[] { n2, n3, n1 });
    }

    public void testOrderByFncDesc() throws RepositoryException {

        Node n1 = testRootNode.addNode("node1", "nt:unstructured");
        Node n2 = testRootNode.addNode("node2", "nt:unstructured");
        Node n3 = testRootNode.addNode("node3", "nt:unstructured");

        n1.setProperty("value", "aaa");
        n2.setProperty("value", "a");
        n3.setProperty("value", "aa");

        testRootNode.getSession().save();

        QueryResult qr = executeSQL2Query("SELECT * FROM [nt:base] WHERE ISCHILDNODE(["
                + testRoot + "]) ORDER BY LENGTH([value]) desc");
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
