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

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.NodeIterator;
import javax.jcr.Node;
import javax.jcr.query.QueryManager;
import javax.jcr.query.Query;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.PrintWriter;

/**
 * Runs queries in one thread while another thread is modifying the workspace.
 */
public class ConcurrentQueryTest extends AbstractJCRTest {

    /**
     * Number of threads executing queries.
     */
    private static final int NUM_READERS = 1;

    /**
     * The read sessions executing the queries.
     */
    private List<Session> readSessions = new ArrayList<Session>();

    /**
     * Gets the read sessions for the test cases.
     */
    protected void setUp() throws Exception {
        super.setUp();
        for (int i = 0; i < NUM_READERS; i++) {
            readSessions.add(getHelper().getReadOnlySession());
        }
    }

    /**
     * Logs out the sessions acquired in setUp().
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        for (Session s : readSessions) {
            s.logout();
        }
        readSessions.clear();
    }

    /**
     * Writes 1000 nodes in transactions of 5 nodes to the workspace while
     * other threads query the workspace. Query results must always return
     * a consistent view of the workspace, that is:<br/>
     * <code>result.numNodes % 5 == 0</code>
     */
    public void testConcurrentQueryWithWrite() throws Exception {

        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());
        List<QueryWorker> readers = new ArrayList<QueryWorker>();
        String query = "/jcr:root" + testRoot + "//*[@testprop = 'foo']";
        for (Session s : readSessions) {
            readers.add(new QueryWorker(s, query, exceptions, log));
        }

        Thread writer = new Thread() {
            public void run() {
                try {
                    for (int i = 0; i < 20; i++) {
                        Node n = testRootNode.addNode("node" + i);
                        for (int j = 0; j < 10; j++) {
                            Node n1 = n.addNode("node" + j);
                            for (int k = 0; k < 5; k++) {
                                n1.addNode("node" + k).setProperty("testprop", "foo");
                            }
                            testRootNode.save();
                        }
                    }
                } catch (RepositoryException e) {
                    exceptions.add(e);
                }
            }
        };

        // start the threads
        writer.start();
        for (Thread t : readers ) {
            t.start();
        }

        // wait for writer thread to finish its work
        writer.join();

        // request readers to finish
        for (QueryWorker t : readers) {
            t.finish();
            t.join();
        }

        // fail in case of exceptions
        if (exceptions.size() > 0) {
            fail(exceptions.get(0).toString());
        }
    }

    /**
     * Deletes 1000 nodes in transactions of 5 nodes while
     * other threads query the workspace. Query results must always return
     * a consistent view of the workspace, that is:<br/>
     * <code>result.numNodes % 5 == 0</code>
     */
    public void testConcurrentQueryWithDeletes() throws Exception {

        // create 1000 nodes
        for (int i = 0; i < 20; i++) {
            Node n = testRootNode.addNode("node" + i);
            for (int j = 0; j < 10; j++) {
                Node n1 = n.addNode("node" + j);
                for (int k = 0; k < 5; k++) {
                    n1.addNode("node" + k).setProperty("testprop", "foo");
                }
            }
            testRootNode.save();
        }

        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());
        List<QueryWorker> readers = new ArrayList<QueryWorker>();
        String query = "/jcr:root" + testRoot + "//*[@testprop = 'foo']";
        for (Session s : readSessions) {
            readers.add(new QueryWorker(s, query, exceptions, log));
        }

        Thread writer = new Thread() {
            public void run() {
                try {
                    for (int i = 0; i < 20; i++) {
                        Node n = testRootNode.getNode("node" + i);
                        for (int j = 0; j < 10; j++) {
                            Node n1 = n.getNode("node" + j);
                            for (int k = 0; k < 5; k++) {
                                n1.getNode("node" + k).remove();
                            }
                            testRootNode.save();
                        }
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
        };

        // start the threads
        writer.start();
        for (Thread t : readers) {
            t.start();
        }

        // wait for writer thread to finish its work
        writer.join();

        // request readers to finish
        for (QueryWorker t : readers) {
            t.finish();
            t.join();
        }

        // fail in case of exceptions
        if (!exceptions.isEmpty()) {
            fail(exceptions.get(0).toString());
        }
    }

    /**
     * Executes queries in a separate thread.
     */
    private static final class QueryWorker extends Thread {

        private Session s;
        private String query;
        private final List<Exception> exceptions;
        private final PrintWriter log;
        private boolean finish = false;
        private int count;

        QueryWorker(Session s, String query, List<Exception> exceptions, PrintWriter log) {
            this.s = s;
            this.query = query;
            this.exceptions = exceptions;
            this.log = log;
        }

        public void run() {
            try {
                // run the queries
                QueryManager qm = s.getWorkspace().getQueryManager();
                Query q = qm.createQuery(query, Query.XPATH);
                for (;;) {
                    long time = System.currentTimeMillis();
                    NodeIterator nodes = q.execute().getNodes();
                    long size = nodes.getSize();
                    if (size == -1) {
                        while (nodes.hasNext()) {
                            size++;
                            nodes.nextNode();
                        }
                    }
                    time = System.currentTimeMillis() - time;
                    log.println(getName() + ": num nodes:" + size +
                            " executed in: " + time + " ms.");

                    count++;
                    if (size % 5 != 0) {
                        exceptions.add(new Exception("number of result nodes must be divisible by 5, but is: " + size));
                    }
                    // do not consume all cpu power
                    Thread.sleep(10);
                    synchronized (this) {
                        if (finish) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                exceptions.add(e);
            }
            log.println("Executed " + count + " queries");
        }

        public synchronized void finish() {
            finish = true;
        }
    }
}
