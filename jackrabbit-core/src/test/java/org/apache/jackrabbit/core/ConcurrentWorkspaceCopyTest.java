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

import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.RepositoryException;
import java.util.Random;


public class ConcurrentWorkspaceCopyTest extends AbstractJCRTest {

    private static final int NUM_ITERATIONS = 40;
    private static final int NUM_SESSIONS = 2;

    static final String TARGET_NAME = "copy of src";
    String sourcePath;
    String destParentPath;
    String destPath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create a parent node where allowSameNameSiblings is set to false
        destParentPath =
                testRootNode.addNode("destParent",
                        "nt:folder").getPath();
        destPath = destParentPath + "/" + TARGET_NAME;
        // create a source node
        sourcePath = testRootNode.addNode("src", "nt:folder").getPath();

        testRootNode.getSession().save();
    }

    public void testConcurrentCopy() throws Exception {
        for (int n = 0; n < NUM_ITERATIONS; n++) {
            // cleanup
            while (superuser.nodeExists(destPath)) {
                superuser.getNode(destPath).remove();
                superuser.save();
            }

            Thread[] threads = new Thread[NUM_SESSIONS];
            for (int i = 0; i < threads.length; i++) {
                // create new session
                Session session = getHelper().getSuperuserSession();
                String id = "session#" + i;
                TestSession ts = new TestSession(id, session);
                Thread t = new Thread(ts);
                t.setName(id);
                t.start();
                threads[i] = t;
            }
            for (int i = 0; i < threads.length; i++) {
                threads[i].join();
            }

            NodeIterator results = superuser.getNode(destParentPath).getNodes(TARGET_NAME);

            assertEquals(1, results.getSize());
        }
    }


    // -------------------------------------------------------< inner classes >
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

            try {
                session.getWorkspace().copy(sourcePath, destPath);
                session.save();

                randomSleep();
            } catch (RepositoryException e) {
                // expected
            } finally {
                session.logout();
            }

        }
    }

}