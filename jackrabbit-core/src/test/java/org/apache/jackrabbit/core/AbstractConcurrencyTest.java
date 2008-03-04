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

import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * <code>AbstractConcurrencyTest</code> provides utility methods to run tests
 * using concurrent threads.
 */
public abstract class AbstractConcurrencyTest extends AbstractJCRTest {

    /**
     * Execute random queries for this amount of time.
     */
    protected static final int RUN_NUM_SECONDS = getTestScale();

    /**
     * Runs a task with the given concurrency and creates an individual test
     * node for each thread.
     *
     * @param task the task to run.
     * @param concurrency the concurrency.
     * @throws RepositoryException if an error occurs.
     */
    protected void runTask(Task task, int concurrency) throws RepositoryException {
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
            }
        }
    }

    /**
     * Runs a task with the given concurrency on the node identified by path.
     *
     * @param task the task to run.
     * @param concurrency the concurrency.
     * @param path the path to the test node.
     * @throws RepositoryException if an error occurs.
     */
    protected void runTask(Task task, int concurrency, String path)
            throws RepositoryException {
        Thread[] threads = new Thread[concurrency];
        for (int i = 0; i < concurrency; i++) {
            Session s = helper.getSuperuserSession();
            Node test = (Node) s.getItem(path);
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

    protected static class Executor implements Runnable {

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
            } finally {
                session.logout();
            }
        }
    }
}
