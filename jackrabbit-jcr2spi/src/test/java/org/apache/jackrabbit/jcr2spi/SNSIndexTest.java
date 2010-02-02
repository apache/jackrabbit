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
package org.apache.jackrabbit.jcr2spi;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>SNSIndexTest</code>...
 */
public class SNSIndexTest extends AbstractJCRTest {

    private static Logger log = LoggerFactory.getLogger(SNSIndexTest.class);

    private String snsName;

    private Node parent;

    private Node sns1;
    private Node sns2;
    private Node sns3;
    private Node sns4;

    private String snsPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        snsName = nodeName2;

        parent = testRootNode.addNode(nodeName1, testNodeType);
        // create sns-siblings
        sns1 = parent.addNode(snsName, testNodeType);
        sns2 = parent.addNode(snsName, testNodeType);
        sns3 = parent.addNode(snsName, testNodeType);
        sns4 = parent.addNode(snsName, testNodeType);

        testRootNode.save();

        snsPath = testRootNode.getPath() + "/" + nodeName1 + "/" + snsName;
    }

    @Override
    protected void tearDown() throws Exception {
        parent = null;
        sns1 = null;
        sns2 = null;
        sns3 = null;
        sns4 = null;
        super.tearDown();
    }

    /**
     * Test if index of the created nodes are as expected.
     */
    public void testIndex() throws RepositoryException {
        checkIndex(sns1, Path.INDEX_DEFAULT);
        checkIndex(sns2, Path.INDEX_DEFAULT + 1);
        checkIndex(sns3, Path.INDEX_DEFAULT + 2);
        checkIndex(sns4, Path.INDEX_DEFAULT + 3);
    }

    /**
     * Test if index of the created nodes are as expected if they are accessed
     * by another session.
     */
    public void testIndexByOtherSession() throws RepositoryException {
        Session otherSession = getHelper().getReadOnlySession();
        try {
            for (int index = Path.INDEX_DEFAULT; index < 4; index++) {
                Node sns = (Node) otherSession.getItem(buildPath(index));
                checkIndex(sns, index);
            }
        } finally {
            otherSession.logout();
        }
    }

    /**
     * Test if passing an bigger index throws exception
     */
    public void testNonExistingIndex() throws RepositoryException {
        try {
            superuser.getItem(buildPath(10));
            fail("Accessing item with non-existing index must throw PathNotFoundException.");
        } catch (PathNotFoundException e) {
            // ok
        }
    }

    /**
     * Test if accessing a child node by sns-Name, the node with the default
     * index is returned.
     */
    public void testDefaultIndex() throws RepositoryException {
        Node sns = parent.getNode(snsName);
        checkIndex(sns, Path.INDEX_DEFAULT);
    }

    /**
     * Test if index of any node is correctly set, if the node is accessed
     * without loading SNSs with lower index before
     */
    public void testNodeEntriesFilledCorrectly() throws RepositoryException {
        Session otherSession = getHelper().getReadOnlySession();
        try {
            Node sns = (Node) otherSession.getItem(buildPath(3));
            checkIndex(sns, 3);

            sns = (Node) otherSession.getItem(buildPath(2));
            checkIndex(sns, 2);

            sns = (Node) otherSession.getItem(buildPath(4));
            checkIndex(sns, 4);

            // check 3 again
            sns = (Node) otherSession.getItem(buildPath(3));
            checkIndex(sns, 3);

            // check default
            sns = (Node) otherSession.getItem(buildPath(1));
            checkIndex(sns, 1);
        } finally {
            otherSession.logout();
        }
    }

    /**
     * Test if accessing the created nodes by name really returns all nodes.
     */
    public void testGetNodesByName() throws RepositoryException {
        NodeIterator it = parent.getNodes(snsName);
        long size = it.getSize();
        if (size != -1) {
            assertTrue("4 SNSs have been added -> but iterator size is " + size + ".", size == 4);
        }
        int expectedIndex = 1;
        while (it.hasNext()) {
            Node sns = it.nextNode();
            checkIndex(sns, expectedIndex);
            expectedIndex++;
        }
        assertTrue("4 SNSs have been added -> but iterator size is " + size + ".", size == 4);
    }

    /**
     * Test if accessing the created nodes by name really returns all nodes.
     */
    public void testGetNodesByNameByOtherSession() throws RepositoryException {
        Session otherSession = getHelper().getReadOnlySession();
        try {
            NodeIterator it = ((Node) otherSession.getItem(parent.getPath())).getNodes(snsName);
            long size = it.getSize();
            if (size != -1) {
                assertTrue("4 SNSs have been added -> but iterator size is " + size + ".", size == 4);
            }
            int expectedIndex = 1;
            while (it.hasNext()) {
                Node sns = it.nextNode();
                checkIndex(sns, expectedIndex);
                expectedIndex++;
            }
            assertTrue("4 SNSs have been added -> but iterator size is " + size + ".", size == 4);
        } finally {
            otherSession.logout();
        }

    }

    private String buildPath(int index) {
        return snsPath + "[" + index + "]";
    }

    private static void checkIndex(Node node, int expectedIndex) throws RepositoryException {
        int index = node.getIndex();
        if (index != expectedIndex) {
            fail("Unexpected index " + index + ". Expected index was " + expectedIndex);
        }
    }
}