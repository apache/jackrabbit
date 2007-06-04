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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.version.Version;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * <code>ConcurrentVersioningTest</code> contains test cases that run version
 * operations with concurrent threads.
 */
public class ConcurrentVersioningTest extends AbstractJCRTest {

    /**
     * The number of threads.
     */
    private static final int CONCURRENCY = 10;

    /**
     * The total number of operations to execute. E.g. number of checkins
     * performed by the threads.
     */
    private static final int NUM_OPERATIONS = 200;

    public void testConcurrentAddVersionable() throws RepositoryException {
        runTask(new Task() {
            public void execute(Session session, Node test) throws RepositoryException {
                try {
                    // add versionable nodes
                    for (int i = 0; i < NUM_OPERATIONS / CONCURRENCY; i++) {
                        Node n = test.addNode("test" + i);
                        n.addMixin(mixVersionable);
                        session.save();
                    }
                } finally {
                    session.logout();
                }
            }
        }, CONCURRENCY);
    }

    public void testConcurrentCheckin() throws RepositoryException {
        runTask(new Task() {
            public void execute(Session session, Node test) throws RepositoryException {
                try {
                    Node n = test.addNode("test");
                    n.addMixin(mixVersionable);
                    session.save();
                    for (int i = 0; i < NUM_OPERATIONS / CONCURRENCY; i++) {
                        n.checkout();
                        n.checkin();
                    }
                    n.checkout();
                } finally {
                    session.logout();
                }
            }
        }, CONCURRENCY);
    }

    public void testConcurrentCreateAndCheckinCheckout() throws RepositoryException {
        runTask(new Task() {
            public void execute(Session session, Node test) throws RepositoryException {
                try {
                    // add versionable nodes
                    for (int i = 0; i < NUM_OPERATIONS / CONCURRENCY; i++) {
                        Node n = test.addNode("test" + i);
                        n.addMixin(mixVersionable);
                        session.save();
                        n.checkout();
                        n.checkin();
                        n.checkout();
                    }
                } finally {
                    session.logout();
                }
            }
        }, CONCURRENCY);
    }

    public void testConcurrentRestore() throws RepositoryException {
        runTask(new Task() {
            public void execute(Session session, Node test) throws RepositoryException {
                try {
                    Node n = test.addNode("test");
                    n.addMixin(mixVersionable);
                    session.save();
                    // create 3 version
                    List versions = new ArrayList();
                    for (int i = 0; i < 3; i++) {
                        n.checkout();
                        versions.add(n.checkin());
                    }
                    // do random restores
                    Random rand = new Random();
                    for (int i = 0; i < NUM_OPERATIONS / CONCURRENCY; i++) {
                        Version v = (Version) versions.get(rand.nextInt(versions.size()));
                        n.restore(v, true);
                    }
                    n.checkout();
                } finally {
                    session.logout();
                }
            }
        }, CONCURRENCY);
    }

    //----------------------------< internal >----------------------------------

    private void runTask(Task task, int concurrency) throws RepositoryException {
        Thread[] threads = new Thread[concurrency];
        for (int i = 0; i < concurrency; i++) {
            Session s = helper.getSuperuserSession();
            Node test = s.getRootNode().addNode(testPath + "/node" + i);
            s.save();
            threads[i] = new Thread(new Executor(s, test, task));
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public interface Task {

        public abstract void execute(Session session, Node test)
                throws RepositoryException;
    }

    private static class Executor implements Runnable {

        protected final Session session;

        protected final Node test;

        protected final Task task;

        public Executor(Session session, Node test, Task task) {
            this.session = session;
            this.test = test;
            this.task = task;
        }

        public void run() {
            try {
                task.execute(session, test);
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }
    }
}
