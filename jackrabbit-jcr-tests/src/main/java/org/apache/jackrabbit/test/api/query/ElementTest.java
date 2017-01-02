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
 * <ul>
 * <li>{@code testroot} path to node that allows child nodes of type
 * <code>nodetype</code> and <code>nt:base</code>.
 * <li>{@code nodetype} node type name for nodes to create
 * <li>{@code nodename1} node name for a child node of type
 * <code>nodetype</code> or <code>nt:base</code>
 * <li>{@code nodename2} node name for a child node of type
 * <code>nodetype</code> or <code>nt:base</code>
 * <li>{@code nodename3} node name for a child node of type
 * <code>nodetype</code> or <code>nt:base</code>
 * </ul>
 */
public class ElementTest extends AbstractQueryTest {

    private String simpleNodeType;

    protected void setUp() throws Exception {
        super.setUp();
        simpleNodeType = testNodeTypeNoChildren == null ? ntBase : testNodeTypeNoChildren;
    }

    /**
     * Tests the element test without arguments.
     * @throws NotExecutableException 
     */
    public void testElementTest() throws RepositoryException, NotExecutableException {

        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = testRootNode.addNode(nodeName2, simpleNodeType);
        Node n3 = testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.getSession().save();

        String query = "/" + jcrRoot + testRoot + "/element()";
        executeXPathQuery(superuser, query, new Node[]{n1, n2, n3});
    }

    /**
     * Tests the element test with one any node argument.
     * @throws NotExecutableException 
     */
    public void testElementTestAnyNode() throws RepositoryException, NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = testRootNode.addNode(nodeName2, simpleNodeType);
        Node n3 = testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.getSession().save();

        String query = "/" + jcrRoot + testRoot + "/element(*)";
        executeXPathQuery(superuser, query, new Node[]{n1, n2, n3});
    }

    /**
     * Tests the element test with an any node argument and a type argument
     * that matches all nodes (nt:base).
     * @throws NotExecutableException 
     */
    public void testElementTestAnyNodeNtBase() throws RepositoryException, NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = testRootNode.addNode(nodeName2, simpleNodeType);
        Node n3 = testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.getSession().save();

        String query = "/" + jcrRoot + testRoot + "/element(*, " + ntBase + ")";
        executeXPathQuery(superuser, query, new Node[]{n1, n2, n3});
    }

    /**
     * Tests the element test with an any node argument and a type argument
     * that matches only certain child nodes.
     * @throws NotExecutableException 
     */
    public void testElementTestAnyNodeSomeNT() throws RepositoryException, NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.addNode(nodeName2, simpleNodeType);
        Node n3 = testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.getSession().save();

        String query = "/" + jcrRoot + testRoot + "/element(*, " + testNodeType + ")";
        executeXPathQuery(superuser, query, new Node[]{n1, n3});
    }

    /**
     * Tests the element test with one single name test argument.
     * @throws NotExecutableException 
     */
    public void testElementTestNameTest() throws RepositoryException, NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.addNode(nodeName2, simpleNodeType);
        testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.getSession().save();

        String query = "/" + jcrRoot + testRoot + "/element(" + nodeName1 + ")";
        executeXPathQuery(superuser, query, new Node[]{n1});
    }

    /**
     * Tests the element test with a name test argument and a type argument that
     * matches all nodes (nt:base).
     * @throws NotExecutableException 
     */
    public void testElementTestNameTestNtBase() throws RepositoryException, NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.addNode(nodeName2, simpleNodeType);
        testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.getSession().save();

        String query = "/" + jcrRoot + testRoot + "/element(" + nodeName1 + ", " + ntBase + ")";
        executeXPathQuery(superuser, query, new Node[]{n1});
    }

    /**
     * Tests the element test with a name test argument and a type argument that
     * matches only certain child nodes.
     * @throws NotExecutableException 
     */
    public void testElementTestNameTestSomeNT() throws RepositoryException, NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.addNode(nodeName2, simpleNodeType);
        testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.getSession().save();

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
        testRootNode.addNode(nodeName1, simpleNodeType);
        Node n2 = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.addNode(nodeName2, simpleNodeType);
        testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.getSession().save();

        String query = "/" + jcrRoot + testRoot + "/element(" + nodeName1 + ", " + testNodeType + ")";
        executeXPathQuery(superuser, query, new Node[]{n1, n2});
    }
}
