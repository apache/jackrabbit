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

import javax.jcr.RepositoryException;
import javax.jcr.Node;

/**
 * Tests the various XPath axis.
 */
public class XPathAxisTest extends AbstractQueryTest {

    private Node n1;
    private Node n2;
    private Node n11;
    private Node n12;
    private Node n21;
    private Node n22;

    protected void setUp() throws Exception {
        super.setUp();
        n1 = testRootNode.addNode("node1");
        n2 = testRootNode.addNode("node2");
        n11 = n1.addNode("node11");
        n12 = n1.addNode("node12");
        n21 = n2.addNode("node21");
        n22 = n2.addNode("node22");
        testRootNode.save();
    }

    protected void tearDown() throws Exception {
        n1 = null;
        n2 = null;
        n11 = null;
        n12 = null;
        n21 = null;
        n22 = null;
        super.tearDown();
    }

    public void testChildAxisRoot() throws RepositoryException {
        String xpath = "/*";
        executeXPathQuery(xpath, new Node[]{superuser.getRootNode()});
    }

    public void testChildAxisJCRRoot() throws RepositoryException {
        String xpath = "/jcr:root/*";
        Node[] nodes = toArray(superuser.getRootNode().getNodes());
        executeXPathQuery(xpath, nodes);
    }

    public void testChildAxisTestRoot() throws RepositoryException {
        String xpath = "/jcr:root" + testRoot + "/*";
        executeXPathQuery(xpath, new Node[]{n1, n2});
    }

    public void testChildAxisTestRootRelative() throws RepositoryException {
        String xpath = testPath + "/*";
        executeXPathQuery(xpath, new Node[]{n1, n2});
    }

    public void testChildAxisLeaf() throws RepositoryException {
        String xpath = "/jcr:root" + testRoot + "/node1/node11/*";
        executeXPathQuery(xpath, new Node[0]);
    }

    public void testChildAxisLeafRelative() throws RepositoryException {
        String xpath = testPath + "/node1/node11/*";
        executeXPathQuery(xpath, new Node[0]);
    }

    public void testDescendantAxisTestRoot() throws RepositoryException {
        String xpath = "/jcr:root" + testRoot + "//*";
        executeXPathQuery(xpath, new Node[]{n1, n11, n12, n2, n21, n22});
    }

    public void testDescendantAxisTestRootRelative() throws RepositoryException {
        String xpath = testPath + "//*";
        executeXPathQuery(xpath, new Node[]{n1, n11, n12, n2, n21, n22});
    }

    public void testDescendantAxisLeaf() throws RepositoryException {
        String xpath = "/jcr:root" + testRoot + "node1/node11//*";
        executeXPathQuery(xpath, new Node[0]);
    }

    public void testDescendantAxisLeafRelative() throws RepositoryException {
        String xpath = testPath + "node1/node11//*";
        executeXPathQuery(xpath, new Node[0]);
    }

    public void testDescendantSelfAxisTestRoot1() throws RepositoryException {
        String xpath = "/jcr:root" + testRoot + "//node1";
        executeXPathQuery(xpath, new Node[]{n1});
    }

    public void testDescendantSelfAxisTestRoot1Relative() throws RepositoryException {
        String xpath = testPath + "//node1";
        executeXPathQuery(xpath, new Node[]{n1});
    }

    public void testDescendantSelfAxisAndChild() throws RepositoryException {
        String xpath = "/jcr:root" + testRoot + "//node1/*";
        executeXPathQuery(xpath, new Node[]{n11, n12});
    }

    public void testDescendantSelfAxisAndChildRelative() throws RepositoryException {
        String xpath = testPath + "//node1/*";
        executeXPathQuery(xpath, new Node[]{n11, n12});
    }

    public void testChildAndDescendantSelfAxis() throws RepositoryException {
        String xpath = "/jcr:root" + testRoot + "/*//*";
        executeXPathQuery(xpath, new Node[]{n11, n12, n21, n22});
    }

    public void testChildAndDescendantSelfAxisRelative() throws RepositoryException {
        String xpath = testPath + "/*//*";
        executeXPathQuery(xpath, new Node[]{n11, n12, n21, n22});
    }

    public void testChildChildAxis() throws RepositoryException {
        String xpath = "/jcr:root" + testRoot + "/*/*";
        executeXPathQuery(xpath, new Node[]{n11, n12, n21, n22});
    }

    public void testChildChildAxisRelative() throws RepositoryException {
        String xpath = testPath + "/*/*";
        executeXPathQuery(xpath, new Node[]{n11, n12, n21, n22});
    }

    public void testChildAndNodeTestAxis() throws RepositoryException {
        String xpath = "/jcr:root" + testRoot + "/*/node11";
        executeXPathQuery(xpath, new Node[]{n11});
    }

    public void testChildAndNodeTestAxisRelative() throws RepositoryException {
        String xpath = testPath + "/*/node11";
        executeXPathQuery(xpath, new Node[]{n11});
    }

    public void testDescendantSelfAxisTestRoot2() throws RepositoryException {
        String xpath = "/jcr:root" + testRoot + "//node11";
        executeXPathQuery(xpath, new Node[]{n11});
    }

    public void testDescendantSelfAxisTestRoot2Relative() throws RepositoryException {
        String xpath = testPath + "//node11";
        executeXPathQuery(xpath, new Node[]{n11});
    }

    public void testDescendantSelfAxisNonDesc() throws RepositoryException {
        String xpath = "/jcr:root" + testRoot + "/node1//node22";
        executeXPathQuery(xpath, new Node[0]);
    }

    public void testDescendantSelfAxisRelativeTestPath() throws RepositoryException {
        String xpath = testPath;
        executeXPathQuery(xpath, new Node[]{testRootNode});
    }

    public void testExactRelative() throws RepositoryException {
        String xpath = testPath + "/node1";
        executeXPathQuery(xpath, new Node[]{n1});
    }

    public void testIndex0Descendant() throws RepositoryException {
        String xpath = "/jcr:root" + testRoot + "//*[0]";
        executeXPathQuery(xpath, new Node[0]);
    }

    public void testIndex1Descendant() throws RepositoryException {
        String xpath = "/jcr:root" + testRoot + "//*[1]";
        executeXPathQuery(xpath, new Node[]{n1, n11, n21});
    }

    public void testIndex2Descendant() throws RepositoryException {
        String xpath = "/jcr:root" + testRoot + "//*[2]";
        executeXPathQuery(xpath, new Node[]{n2, n12, n22});
    }

    public void testIndex3Descendant() throws RepositoryException {
        String xpath = "/jcr:root" + testRoot + "//*[3]";
        executeXPathQuery(xpath, new Node[0]);
    }

    public void testRootQuery() throws RepositoryException {
        // JCR-1987
        executeXPathQuery("/jcr:root[@foo = 'does-not-exist']", new Node[0]);
        Node rootNode = superuser.getRootNode();
        executeXPathQuery("/jcr:root", new Node[]{rootNode});
        executeXPathQuery("/jcr:root[@" + jcrPrimaryType + "='" +
                rootNode.getPrimaryNodeType().getName() + "']", new Node[]{rootNode});
    }
}
