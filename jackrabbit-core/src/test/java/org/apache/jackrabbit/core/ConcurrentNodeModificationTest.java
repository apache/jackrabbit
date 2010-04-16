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

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;
import java.util.Random;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Performs a test with n sessions concurrently performing non-conflicting
 * modifications on the <i>same</i> node.
 * <p/>
 * See http://issues.apache.org/jira/browse/JCR-584.
 */
public class ConcurrentNodeModificationTest extends AbstractJCRTest {

    private static final int NUM_ITERATIONS = 1;
    private static final int NUM_SESSIONS = 10;
    private static final int NUM_NODES = 100;

    final ArrayList exceptions = new ArrayList();

    /**
     * Runs the test.
     */
    public void testConcurrentNodeModificationSessions() throws Exception {
        int n = NUM_ITERATIONS;
        while (n-- > 0) {
            Thread[] threads = new Thread[NUM_SESSIONS];
            for (int i = 0; i < threads.length; i++) {
                // create new session
                Session session = getHelper().getSuperuserSession();
                TestSession ts = new TestSession("s" + i, session);
                Thread t = new Thread(ts);
                t.setName((NUM_ITERATIONS - n) + "-s" + i);
                t.start();
                log.println("Thread#" + i + " started");
                threads[i] = t;
                Thread.sleep(100);
            }
            for (int i = 0; i < threads.length; i++) {
                threads[i].join();
            }
        }

        if (!exceptions.isEmpty()) {
            Exception e = null;
            for (Iterator it = exceptions.iterator(); it.hasNext();) {
                e = (Exception) it.next();
                e.printStackTrace(log);
            }
            throw e;
            //fail();
        }
    }

    //--------------------------------------------------------< inner classes >
    class TestSession implements Runnable {

        Session session;
        String identity;
        Random r;

        TestSession(String identity, Session s) {
            session = s;
            this.identity = identity;
            r = new Random();
        }

        private void randomSleep() {
            long l = r.nextInt(90) + 20;
            try {
                Thread.sleep(l);
            } catch (InterruptedException ie) {
            }
        }

        public void run() {

            log.println("started.");
            String state = "";
            try {
                Node n = session.getRootNode().getNode(testPath);

                String propName = "testprop" + Math.random();

                state = "setting property " + propName;
                n.setProperty(propName, "Hello World!");
                session.save();
                randomSleep();

                state = "removing property " + propName;
                n.setProperty(propName, (Value) null);
                session.save();
                randomSleep();

                for (int i = 0; i < NUM_NODES; i++) {
                    state = "adding subnode " + i;
                    //Node n1 = n.addNode("x" + i, "nt:unstructured");
                    Node n1 = n.addNode("x" + identity + i, "nt:unstructured");
                    state = "adding property to subnode " + i;
                    n1.setProperty("testprop", "xxx");
                    if (i % 10 == 0) {
                        state = "saving pending (added) subnodes";
                        session.save();
                    }
                    randomSleep();
                }

                for (int i = 0; i < NUM_NODES; i++) {
                    state = "removing subnode " + i;
                    n.getNode("x" + identity + i).remove();
                    if (i % 10 == 0) {
                        state = "saving pending (removed) subnodes";
                        session.save();
                    }
                    randomSleep();
                }
                session.save();
            } catch (Exception e) {
                log.println("Exception while " + state + ": " + e.getMessage());
                //e.printStackTrace();
                exceptions.add(e);
            } finally {
                session.logout();
            }

            log.println("ended.");
        }
    }
}
