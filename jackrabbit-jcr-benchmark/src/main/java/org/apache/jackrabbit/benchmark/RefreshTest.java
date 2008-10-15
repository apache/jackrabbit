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
package org.apache.jackrabbit.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Several tests for benchmarking the performance when refreshing the complete
 * tree (containing "big" collections).
 * <p>
 * Assumes the store supports nt:folder/nt:file/nt:resource below
 * the test root node.
 */
public class RefreshTest extends AbstractBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(RefreshTest.class);

    protected String getCollectionName() {
        return "folder";
    }

    private void performTest(String testName, boolean refreshFlag) throws RepositoryException {
        Session session = testRootNode.getSession();
        long start = System.currentTimeMillis();
        long cnt = 0;

        while (System.currentTimeMillis() - start < RefreshTest.MINTIME || cnt < RefreshTest.MINCOUNT) {
            Node dir = testRootNode.getNode(getCollectionName());
            NodeIterator it = dir.getNodes();
            testRootNode.refresh(refreshFlag);
            cnt += 1;
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info(testName + ": " +  (double)elapsed / cnt + "ms per call (" + cnt + " iterations)");
    }

    /**
     * Get all children, but do not visit jcr:content child nodes
     */
    public void testRefreshFalse() throws RepositoryException {
        performTest("testRefreshFalse", false);
    }

    /**
     * Get all children, but do not visit jcr:content child nodes
     */
    public void testRefreshTrue() throws RepositoryException {
        performTest("testRefreshTrue", true);
    }
}
