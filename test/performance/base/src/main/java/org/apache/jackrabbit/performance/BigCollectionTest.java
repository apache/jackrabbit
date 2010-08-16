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
package org.apache.jackrabbit.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Several tests for benchmarking the performance when iterating over
 * "big" collections. 
 * <p>
 * Assumes the store supports nt:folder/nt:file/nt:resource below
 * the test root node.
 */
public class BigCollectionTest extends AbstractBenchmarkTest {

    private static final Logger LOG = LoggerFactory.getLogger(BigCollectionTest.class);

    private String bigcollPath;

    protected void setUp() throws Exception {
        super.setUp();
        bigcollPath =  testRootNode.getNode(getCollectionName()).getPath();
    }

    protected String getCollectionName() {
        return "bigcoll";
    }

    private void performTest(String testName, boolean getContentNode, boolean getLength) throws RepositoryException {
        performTest(testName, getContentNode, getLength, false);
    }

    private void performTest(String testName, boolean getContentNode,
                             boolean getLength, boolean refresh) throws RepositoryException {

        long start = System.currentTimeMillis();
        long cnt = 0;

        while (System.currentTimeMillis() - start < MINTIME || cnt < MINCOUNT) {
            Node dir = testRootNode.getNode(getCollectionName());
            int members = 0;
            for (NodeIterator it = dir.getNodes(); it.hasNext(); ) {
                Node child = it.nextNode();
                Node content = getContentNode ? child.getNode("jcr:content") : null;
                String type = getContentNode ? content.getProperty("jcr:mimeType").getString() : null;
                long length = getLength ? content.getProperty("jcr:data").getLength() : -1;
                assertTrue(child.isNode());
                if (getContentNode) {
                    assertEquals(MIMETYPE, type);
                }
                if (getLength) {
                    assertEquals(MEMBERSIZE, length);
                }
                members += 1;
            }
            assertEquals(MEMBERS, members);
            if (refresh) {
                dir.getSession().refresh(true);
            }
            cnt += 1;
        }

        long elapsed = System.currentTimeMillis() - start;

        LOG.info(testName + ": " +  (double)elapsed / cnt + "ms per call (" + cnt + " iterations)");
    }

    private void performTestWithNewSession(String testName, boolean getContentNode, boolean getLength) throws RepositoryException {

        long start = System.currentTimeMillis();
        long cnt = 0;

        while (System.currentTimeMillis() - start < MINTIME || cnt < MINCOUNT) {
            Session s = helper.getReadOnlySession();
            try {
                Node dir = (Node) s.getItem(bigcollPath);
                int members = 0;
                for (NodeIterator it = dir.getNodes(); it.hasNext(); ) {
                    Node child = it.nextNode();
                    Node content = getContentNode ? child.getNode("jcr:content") : null;
                    String type = getContentNode ? content.getProperty("jcr:mimeType").getString() : null;
                    long length = getLength ? content.getProperty("jcr:data").getLength() : -1;
                    assertTrue(child.isNode());
                    if (getContentNode) {
                        assertEquals(MIMETYPE, type);
                    }
                    if (getLength) {
                        assertEquals(MEMBERSIZE, length);
                    }
                    members += 1;
                }
                assertEquals(MEMBERS, members);
            } finally {
                s.logout();
            }
            cnt += 1;
        }

        long elapsed = System.currentTimeMillis() - start;

        LOG.info(testName + ": " +  (double)elapsed / cnt + "ms per call (" + cnt + " iterations)");
    }

    /**
     * Get all children, but do not visit jcr:content child nodes
     */
    public void testGetChildren() throws RepositoryException {
        performTest("testGetChildren", false, false);
    }

    /**
     * Get all children, but do not visit jcr:content child nodes
     */
    public void testGetChildrenAfterRefresh() throws RepositoryException {
        performTest("testGetChildrenAfterRefresh", false, false, true);
    }

    /**
     * Get all children, but do not visit jcr:content child nodes
     */
    public void testGetChildrenAfterLogin() throws RepositoryException {
        performTestWithNewSession("testGetChildrenAfterLogin", false, false);
    }

    /**
     * Get all children and their jcr:content child nodes, but
     * do not visit jcr:data.
     */
    public void testBrowseMinusJcrData() throws RepositoryException {
        performTest("testBrowseMinusJcrData", true, false);
    }

    /**
     * Get all children and their jcr:content child nodes, but
     * do not visit jcr:data.
     */
    public void testBrowseMinusJcrDataAfterRefresh() throws RepositoryException {
        performTest("testBrowseMinusJcrDataAfterRefresh", true, false, true);
    }

    /**
     * Get all children and their jcr:content child nodes, but
     * do not visit jcr:data.
     */
    public void testBrowseMinusJcrDataAfterLogin() throws RepositoryException {
        performTestWithNewSession("testBrowseMinusJcrDataAfterLogin", true, false);
    }

    /**
     * Simulate what a UI usually does on a collection of files:
     * obtain type and length of the files.
     */
    public void testBrowse() throws RepositoryException {
        performTest("testBrowse", true, true);
    }

    /**
     * Simulate what a UI usually does on a collection of files:
     * obtain type and length of the files.
     */
    public void testBrowseAfterRefresh() throws RepositoryException {
        performTest("testBrowseAfterRefresh", true, true, true);
    }

    /**
     * Simulate what a UI usually does on a collection of files:
     * obtain type and length of the files.
     */
    public void testBrowseAfterLogin() throws RepositoryException {
        performTestWithNewSession("testBrowseAfterLogin", true, true);
    }
}
