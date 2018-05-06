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
package org.apache.jackrabbit.core.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.core.AbstractConcurrencyTest;

/**
 * <code>ConcurrentQueriesWithUpdatesTest</code> runs concurrent queries that
 * stress the hierarchy cache in CachingIndexReader combined with updates that
 * modify the hierarchy cache.
 */
public class ConcurrentQueriesWithUpdatesTest extends AbstractConcurrencyTest {

    private static final int NUM_UPDATES = 50;

    private int numNodes;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        numNodes = createNodes(testRootNode, 2, 12, 0); // ~4k nodes
        superuser.save();
    }

    public void testQueriesWithUpdates() throws Exception {
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());
        final AtomicBoolean running = new AtomicBoolean(true);
        // track executed queries and do updates at most at the given rate
        final BlockingQueue<Object> queryExecuted = new LinkedBlockingQueue<Object>();
        Thread queries = new Thread(new Runnable() {
            public void run() {
                try {
                    runTask(new Task() {
                        public void execute(Session session, Node test)
                                throws RepositoryException {
                            QueryManager qm = session.getWorkspace().getQueryManager();
                            while (running.get()) {
                                Query q = qm.createQuery(testPath + "//element(*, nt:unstructured) order by @jcr:score descending", Query.XPATH);
                                NodeIterator nodes = q.execute().getNodes();
                                assertEquals("wrong result set size", numNodes, nodes.getSize());
                                queryExecuted.offer(new Object());
                            }
                        }
                    }, 5, testRootNode.getPath());
                } catch (RepositoryException e) {
                    exceptions.add(e);
                }
            }
        });
        queries.start();
        Thread update = new Thread(new Runnable() {
            public void run() {
                try {
                    runTask(new Task() {
                        public void execute(Session session, Node test)
                                throws RepositoryException {
                            Random rand = new Random();
                            QueryManager qm = session.getWorkspace().getQueryManager();
                            for (int i = 0; i < NUM_UPDATES; i++) {
                                try {
                                    // wait at most 10 seconds
                                    queryExecuted.poll(10, TimeUnit.SECONDS);
                                } catch (InterruptedException e) {
                                    // ignore
                                }
                                Query q = qm.createQuery(testPath + "//node" + rand.nextInt(numNodes) + " order by @jcr:score descending", Query.XPATH);
                                NodeIterator nodes = q.execute().getNodes();
                                if (nodes.hasNext()) {
                                    Node n = nodes.nextNode();
                                    n.setProperty("foo", "bar");
                                    session.save();
                                }
                            }
                        }
                    }, 1, testRootNode.getPath());
                } catch (RepositoryException e) {
                    exceptions.add(e);
                }
            }
        });
        update.start();
        update.join();
        running.set(false);
        queries.join();
    }

    private int createNodes(Node n, int nodesPerLevel, int levels, int count)
            throws RepositoryException {
        levels--;
        for (int i = 0; i < nodesPerLevel; i++) {
            Node child = n.addNode("node" + count);
            count++;
            if (count % 1000 == 0) {
                superuser.save();
            }
            if (levels > 0) {
                count = createNodes(child, nodesPerLevel, levels, count);
            }
        }
        return count;
    }
}
