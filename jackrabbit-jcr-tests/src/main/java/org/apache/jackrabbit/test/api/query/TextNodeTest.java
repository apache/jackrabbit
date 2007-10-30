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
import javax.jcr.Repository;

/**
 * Tests the text() node test in XPath.
 *
 * @tck.config testroot path to node that allows child nodes of type
 * <code>nodetype</code>. The node at <code>testroot</code> must allow child
 * nodes with name jcr:xmltext. Assignment of node type for that child node must
 * be determined by the child node definition. That is, the test will create the
 * node with {@link javax.jcr.Node#addNode(String)}, without giving an explicit
 * node type.
 * @tck.config nodetype name of a node type for nodes under
 * <code>testroot</code>. This node type must allow child nodes with name
 * jcr:xmltext. Assignment of node type for that child node must be determined
 * by the child node definition. That is, the test will create the node with
 * {@link javax.jcr.Node#addNode(String)}, without giving an explicit node
 * type.
 * @tck.config nodename1 name of a child node under <code>testroot</code>.
 * 
 * @test
 * @sources TextNodeTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.TextNodeTest
 * @keywords textNodeTest
 */
public class TextNodeTest extends AbstractQueryTest {

    /** Resolved Name for jcr:xmltext */
    private String jcrXMLText;

    private String jcrXMLCharacters;

    protected void setUp() throws Exception {
        super.setUp();
        jcrXMLText = superuser.getNamespacePrefix(NS_JCR_URI) + ":xmltext";
        jcrXMLCharacters = superuser.getNamespacePrefix(NS_JCR_URI) + ":xmlcharacters";
    }

    /**
     * Tests if text() node test is equivalent with jcr:xmltext.
     */
    public void testTextNodeTest() throws RepositoryException {
        Node text1 = testRootNode.addNode(jcrXMLText);
        text1.setProperty(jcrXMLCharacters, "foo");
        testRootNode.save();
        String xpath = "/" + jcrRoot + testRoot + "/text()";
        executeXPathQuery(superuser, xpath, new Node[]{text1});
    }

    /**
     * Tests if text() node test is equivalent with jcr:xmltext and will select
     * multiple nodes with name jcr:xmltext.
     */
    public void testTextNodeTestMultiNodes() throws RepositoryException {
        Node text1 = testRootNode.addNode(jcrXMLText);
        text1.setProperty(jcrXMLCharacters, "foo");
        Node text2 = testRootNode.addNode(nodeName1, testNodeType).addNode(jcrXMLText);
        text2.setProperty(jcrXMLCharacters, "foo");
        testRootNode.save();
        String xpath = "/" + jcrRoot + testRoot + "//text()";
        executeXPathQuery(superuser, xpath, new Node[]{text1, text2});
    }

    /**
     * Tests if text() node test is equivalent with jcr:xmltext and jcr:contains
     * matches content in jcr:xmlcharacters property.
     */
    public void testTextNodeTestContains() throws RepositoryException {
        Node text1 = testRootNode.addNode(jcrXMLText);
        text1.setProperty(jcrXMLCharacters, "the quick brown fox jumps over the lazy dog.");
        Node text2 = testRootNode.addNode(nodeName1, testNodeType).addNode(jcrXMLText);
        text2.setProperty(jcrXMLCharacters, "java content repository");
        testRootNode.save();
        String xpath = "/" + jcrRoot + testRoot + "//text()[" + jcrContains + "(., 'fox')]";
        executeXPathQuery(superuser, xpath, new Node[]{text1});
    }

    /**
     * Tests text() node test with various position predicates: position(),
     * first(), last().
     * @throws NotExecutableException if the repository does not support queries
     *  with position inidex.
     */
    public void testTextNodeTestWithPosition()
            throws RepositoryException, NotExecutableException {
        if (!isSupported(Repository.QUERY_XPATH_POS_INDEX)) {
            throw new NotExecutableException("Repository does not support position index");
        }
        Node text1 = testRootNode.addNode(jcrXMLText);
        text1.setProperty(jcrXMLCharacters, "foo");
        if (!text1.getDefinition().allowsSameNameSiblings()) {
            throw new NotExecutableException("Node at path: " + testRoot + " does not allow same name siblings with name: " + jcrXMLText);
        }
        testRootNode.addNode(nodeName1, testNodeType);
        Node text2 = testRootNode.addNode(jcrXMLText);
        text2.setProperty(jcrXMLCharacters, "foo");
        testRootNode.save();
        String xpath = "/" + jcrRoot + testRoot + "/text()[2]";
        executeXPathQuery(superuser, xpath, new Node[]{text2});
        xpath = "/" + jcrRoot + testRoot + "/text()[last()]";
        executeXPathQuery(superuser, xpath, new Node[]{text2});
        xpath = "/" + jcrRoot + testRoot + "/text()[position() = 2]";
        executeXPathQuery(superuser, xpath, new Node[]{text2});
        xpath = "/" + jcrRoot + testRoot + "/text()[first()]";
        executeXPathQuery(superuser, xpath, new Node[]{text1});
    }
}
