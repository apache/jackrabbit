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

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Node;

/**
 * Tests the element test function in XPath.
 *
 * @tck.config testroot path to node that allows child nodes of type
 * <code>nodetype</code> and <code>nt:base</code>.
 * @tck.config nodetype node type name for nodes to create
 * @tck.config nodename1 node name for a child node of type
 * <code>nodetype</code> or <code>nt:base</code>
 * @tck.config nodename2 node name for a child node of type
 * <code>nodetype</code> or <code>nt:base</code>
 * @tck.config nodename3 node name for a child node of type
 * <code>nodetype</code> or <code>nt:base</code>
 * 
 * @test
 * @sources ElementTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.ElementTest
 * @keywords level2
 */
public class ElementTest extends AbstractQueryTest {

    /**
     * Tests the element test without arguments.
     */
    public void testElementTest() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = testRootNode.addNode(nodeName2, ntBase);
        Node n3 = testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.save();

        String query = "/" + jcrRoot + testRoot + "/element()";
        executeXPathQuery(superuser, query, new Node[]{n1, n2, n3});
    }

    /**
     * Tests the element test with one any node argument.
     */
    public void testElementTestAnyNode() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = testRootNode.addNode(nodeName2, ntBase);
        Node n3 = testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.save();

        String query = "/" + jcrRoot + testRoot + "/element(*)";
        executeXPathQuery(superuser, query, new Node[]{n1, n2, n3});
    }

    /**
     * Tests the element test with an any node argument and a type argument
     * that matches all nodes (nt:base).
     */
    public void testElementTestAnyNodeNtBase() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = testRootNode.addNode(nodeName2, ntBase);
        Node n3 = testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.save();

        String query = "/" + jcrRoot + testRoot + "/element(*, " + ntBase + ")";
        executeXPathQuery(superuser, query, new Node[]{n1, n2, n3});
    }

    /**
     * Tests the element test with an any node argument and a type argument
     * that matches only certain child nodes.
     */
    public void testElementTestAnyNodeSomeNT() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.addNode(nodeName2, ntBase);
        Node n3 = testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.save();

        String query = "/" + jcrRoot + testRoot + "/element(*, " + testNodeType + ")";
        executeXPathQuery(superuser, query, new Node[]{n1, n3});
    }

    /**
     * Tests the element test with one single name test argument.
     */
    public void testElementTestNameTest() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.addNode(nodeName2, ntBase);
        testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.save();

        String query = "/" + jcrRoot + testRoot + "/element(" + nodeName1 + ")";
        executeXPathQuery(superuser, query, new Node[]{n1});
    }

    /**
     * Tests the element test with a name test argument and a type argument that
     * matches all nodes (nt:base).
     */
    public void testElementTestNameTestNtBase() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.addNode(nodeName2, ntBase);
        testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.save();

        String query = "/" + jcrRoot + testRoot + "/element(" + nodeName1 + ", " + ntBase + ")";
        executeXPathQuery(superuser, query, new Node[]{n1});
    }

    /**
     * Tests the element test with a name test argument and a type argument that
     * matches only certain child nodes.
     */
    public void testElementTestNameTestSomeNT() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.addNode(nodeName2, ntBase);
        testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.save();

        String query = "/" + jcrRoot + testRoot + "/element(" + nodeName1 + ", " + testNodeType + ")";
        executeXPathQuery(superuser, query, new Node[]{n1});
    }

    /**
     * Tests the element test with a name test argument and a type argument that
     * matches only certain child nodes. Additonally this test requires that
     * testroot allows same name sibling child nodes.
     */
    public void testElementTestNameTestSomeNTWithSNS() throws RepositoryException, NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        if (!n1.getDefinition().allowsSameNameSiblings()) {
            throw new NotExecutableException("Node at " + testRoot + " does not allow same name siblings with name " + nodeName1);
        }
        testRootNode.addNode(nodeName1, ntBase);
        Node n2 = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.addNode(nodeName2, ntBase);
        testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.save();

        String query = "/" + jcrRoot + testRoot + "/element(" + nodeName1 + ", " + testNodeType + ")";
        executeXPathQuery(superuser, query, new Node[]{n1, n2});
    }
}
