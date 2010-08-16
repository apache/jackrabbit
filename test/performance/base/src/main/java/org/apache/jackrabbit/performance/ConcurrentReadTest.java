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

import java.util.concurrent.CountDownLatch;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Test case that creates 10k unstructured nodes (100x100) and starts
 * 50 concurrent readers to traverse this content tree. Note that this
 * test measures total throughput of such a concurrent set of readers,
 * not the performance of individual readers nor the overall fairness of
 * the scheduling.
 */
public class ConcurrentReadTest extends AbstractTest {

    private static final int NODE_COUNT = 100;

    private static final int READER_COUNT = 50;

    private Session session;

    protected Node root;

    private Thread[] readers = new Thread[READER_COUNT];

    private volatile boolean running;

    private volatile CountDownLatch latch;

    public void beforeSuite() throws Exception {
        session = getRepository().login(getCredentials());

        root = session.getRootNode().addNode("testroot", "nt:unstructured");
        for (int i = 0; i < NODE_COUNT; i++) {
            Node node = root.addNode("node" + i, "nt:unstructured");
            for (int j = 0; j < NODE_COUNT; j++) {
                node.addNode("node" + j, "nt:unstructured");
            }
            session.save();
        }

        running = true;
        latch = new CountDownLatch(0);
        for (int i = 0; i < READER_COUNT; i++) {
            readers[i] = new Reader();
            readers[i].start();
            // Give the reader some time to get started
            Thread.sleep(100); 
        }
    }

    private class Reader extends Thread implements ItemVisitor {

        @Override
        public void run() {
            try {
                Session session = getRepository().login();
                try {
                    Node node = session.getRootNode().getNode(root.getName());
                    while (running) {
                        node.accept(this);
                        latch.countDown();
                    }
                } finally {
                    session.logout();
                }
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }

        public void visit(Node node) throws RepositoryException {
            NodeIterator iterator = node.getNodes();
            while (iterator.hasNext()) {
                iterator.nextNode().accept(this);
            }
        }

        public void visit(Property property) {
        }

    }

    public void runTest() throws Exception {
        latch = new CountDownLatch(READER_COUNT);
        latch.await();
    }

    public void afterSuite() throws Exception {
        running = false;
        for (int i = 0; i < READER_COUNT; i++) {
            readers[i].join();
        }

        for (int i = 0; i < NODE_COUNT; i++) {
            root.getNode("node" + i).remove();
            session.save();
        }

        root.remove();
        session.save();
        session.logout();
    }

}
