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

import java.util.Random;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BigTreeTest extends AbstractBenchmarkTest {
    private static final Logger log = LoggerFactory.getLogger(BigTreeTest.class);

    protected static final int CHILDCOUNT = 2;
    protected static final int DEPTH = 3;
    protected static final String NODE_NAME = "1";
    protected static final String DEEP_NODE_NAME;

    static {
        String sep = "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < DEPTH; i++) {
            sb.append(sep);
            sb.append("1");
            sep = "/";
        }
        DEEP_NODE_NAME = sb.toString();
    }

    private Node bigTree;

    protected String getCollectionName() {
        return "bigTree";
    }

    protected void setUp() throws Exception {
        super.setUp();
        bigTree = testRootNode.getNode(getCollectionName());
    }

    protected void createContent(Node folder) throws RepositoryException {
        addChildNodes(0, folder);
        folder.getSession().save();
    }

    private void addChildNodes(int depth, Node parent) throws RepositoryException {
        if (depth < DEPTH) {
            for (int i = 0; i < CHILDCOUNT; i++) {
                Node c = parent.addNode("" + i, "nt:folder");
                addChildNodes(depth + 1, c);
            }
        }
    }

    private void performTest(String name, boolean recursive, NodeNameProvier nodeNames) throws RepositoryException {
        Timer getNodeTime = new Timer();
        Timer refreshTime = new Timer();
        int c = 0;

        while (getNodeTime.getElapsedTime() + refreshTime.getElapsedTime() < MINTIME || c < MINCOUNT) {
            String nodeName = nodeNames.getNodeName(c);
            getNodeTime.start();
            Node dir = bigTree.getNode(nodeName);
            getNodeTime.stop();

            refreshTime.start();
            bigTree.refresh(recursive);
            refreshTime.stop();
            c++;
        }

        log.info(name + ": " + (double)getNodeTime.getElapsedTime()/c + "ms per getNode() (" + c + " iterations)");
        log.info(name + ": " + (double)refreshTime.getElapsedTime()/c + "ms per refresh() (" + c + " iterations)");
    }

    public void testRefreshRecursive() throws RepositoryException {
        performTest("testRefreshRecursive", true, new NodeNameProvier() {
            public String getNodeName(int i) {
                return NODE_NAME;
            }
        });
    }

    public void testRefreshNonRecursive() throws RepositoryException {
        performTest("testRefreshNonRecursive", false, new NodeNameProvier() {
            public String getNodeName(int i) {
                return NODE_NAME;
            }
        });
    }

    public void testRefreshRecursiveDeep() throws RepositoryException {
        performTest("testRefreshRecursiveDeep", true, new NodeNameProvier() {
            public String getNodeName(int i) {
                return DEEP_NODE_NAME;
            }
        });
    }

    public void testRefreshNonRecursiveDeep() throws RepositoryException {
        performTest("testRefreshNonRecursiveDeep", false, new NodeNameProvier() {
            public String getNodeName(int i) {
                return DEEP_NODE_NAME;
            }
        });
    }

    public void testRefreshRecursiveRandom() throws RepositoryException {
        performTest("testRefreshRecursiveRandom", true, new RandomNodeNameProvider());
    }

    public void testRefreshNonRecursiveRandom() throws RepositoryException {
        performTest("testRefreshNonRecursiveRandom", false, new RandomNodeNameProvider());
    }

    // -----------------------------------------------------< NodeNameProvier >---

    private static interface NodeNameProvier {
        public String getNodeName(int i);
    }

    private class RandomNodeNameProvider implements NodeNameProvier {
        private final Random rnd = new Random();

        public String getNodeName(int i) {
            StringBuffer sb = new StringBuffer();
            int depth = rnd.nextInt(DEPTH);
            String sep = "";
            for (int k = 0; k <= depth; k++) {
                sb.append(sep).append(rnd.nextInt(CHILDCOUNT));
                sep = "/";
            }
            return sb.toString();
        }
    }

    // -----------------------------------------------------< Timer >---

    private static class Timer {
        private long startTime;
        private long elapsedTime;
        private boolean started;

        public void start() {
            if (started) {
                throw new IllegalStateException("Timer already started");
            }
            started = true;
            startTime = System.currentTimeMillis();
        }

        public long stop() {
            if (!started) {
                throw new IllegalStateException("Timer not started");
            }
            elapsedTime += System.currentTimeMillis() - startTime;
            started = false;
            return elapsedTime;
        }

        public long getElapsedTime() {
            return elapsedTime;
        }
    }

}
