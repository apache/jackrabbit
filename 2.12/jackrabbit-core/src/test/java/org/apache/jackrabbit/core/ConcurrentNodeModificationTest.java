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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 * Performs a test with n sessions concurrently performing non-conflicting
 * modifications on the <i>same</i> node.
 * <p>
 * See http://issues.apache.org/jira/browse/JCR-584.
 */
public class ConcurrentNodeModificationTest extends AbstractJCRTest {

    /**
     * Logger instance.
     */
    private static final Logger log =
        LoggerFactory.getLogger(ConcurrentNodeModificationTest.class);

    private static final int NUM_SESSIONS = 100;
    private static final int NUM_ITERATIONS = 10;
    private static final int NUM_NODES = 10;

    private volatile boolean success;

    /**
     * Runs the test.
     */
    public void testConcurrentNodeModificationSessions() throws Exception {
        success = true;

        Thread[] threads = new Thread[NUM_SESSIONS];
        for (int i = 0; i < threads.length; i++) {
            TestSession ts = new TestSession("s" + i);
            threads[i] = new Thread(ts, "CNMT " + i);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        assertTrue("Unexpected exceptions during test, see the log file for details", success);
    }

    //--------------------------------------------------------< inner classes >
    class TestSession implements Runnable {

        private final Session session;
        private final String identity;

        TestSession(String identity) throws RepositoryException {
            this.session = getHelper().getSuperuserSession();
            this.identity = identity;
        }

        public void run() {
            log.debug("started.");
            try {
                for (int i = 0; success && i < NUM_ITERATIONS; i++) {
                    runIteration();
                }
            } catch (Exception e) {
                log.error("Operation failed", e);
                success = false;
            } finally {
                session.logout();
            }

            log.info("ended.");
        }

        private void runIteration() throws RepositoryException {
            Node n = session.getRootNode().getNode(testPath);

            String propName = "prop_" + identity;

            log.info("setting property {}", propName);
            n.setProperty(propName, "Hello World!");
            Thread.yield(); // maximize chances of interference
            session.save();

            log.info("removing property {}", propName);
            n.setProperty(propName, (Value) null);
            Thread.yield(); // maximize chances of interference
            session.save();

            for (int i = 0; i < NUM_NODES; i++) {
                String name = "x_" + identity + "_" + i;
                log.info("adding subnode {}", name);
                //Node n1 = n.addNode("x" + i, "nt:unstructured");
                Node n1 = n.addNode(name, "nt:unstructured");
                n1.setProperty("testprop", "xxx");
                Thread.yield(); // maximize chances of interference
                session.save();
            }

            for (int i = 0; i < NUM_NODES; i++) {
                String name = "x_" + identity + "_" + i;
                log.info("removing subnode {}", name);
                n.getNode(name).remove();
                Thread.yield(); // maximize chances of interference
                session.save();
            }
        }

    }

}
