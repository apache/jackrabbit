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

import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * <code>AbstractConcurrencyTest</code> provides utility methods to run tests
 * using concurrent threads.
 */
public abstract class AbstractConcurrencyTest extends AbstractJCRTest {

    /**
     * Logger instance for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(AbstractConcurrencyTest.class);

    /**
     * Runs a task with the given concurrency and creates an individual test
     * node for each thread.
     *
     * @param task the task to run.
     * @param concurrency the concurrency.
     * @throws RepositoryException if an error occurs.
     */
    protected void runTask(Task task, int concurrency) throws RepositoryException {
        runTasks(new Task[]{task}, concurrency,
                // run for at most one year ;)
                getOneYearAhead());
    }

    /**
     * Runs each of the tasks with the given concurrency and creates an
     * individual test node for each thread.
     *
     * @param tasks       the tasks to run.
     * @param concurrency the concurrency.
     * @param timeout     when System.currentTimeMillis() reaches timeout the
     *                    threads executing the tasks should be interrupted.
     *                    This indicates that a deadlock occured.
     * @throws RepositoryException if an error occurs.
     */
    protected void runTasks(Task[] tasks, int concurrency, long timeout)
            throws RepositoryException {
        Executor[] executors = new Executor[concurrency * tasks.length];
        for (int t = 0; t < tasks.length; t++) {
            for (int i = 0; i < concurrency; i++) {
                int id = t * concurrency + i;
                Session s = getHelper().getSuperuserSession();
                Node test = s.getRootNode().addNode(testPath + "/node" + id);
                s.save();
                executors[id] = new Executor(s, test, tasks[t]);
            }
        }
        executeAll(executors, timeout);
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
        Executor[] executors = new Executor[concurrency];
        for (int i = 0; i < concurrency; i++) {
            Session s = getHelper().getSuperuserSession();
            Node test = (Node) s.getItem(path);
            s.save();
            executors[i] = new Executor(s, test, task);
        }
        executeAll(executors, getOneYearAhead());
    }

    /**
     * Executes all executors using individual threads.
     *
     * @param executors the executors.
     * @param timeout time when running threads should be interrupted.
     * @throws RepositoryException if one of the executors throws an exception.
     */
    private void executeAll(Executor[] executors, long timeout) throws RepositoryException {
        Thread[] threads = new Thread[executors.length];
        for (int i = 0; i < executors.length; i++) {
            threads[i] = new Thread(executors[i], "Executor " + i);
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }

        boolean stacksDumped = false;
        for (int i = 0; i < threads.length; i++) {
            try {
                long wait = Math.max(timeout - System.currentTimeMillis(), 1000);
                threads[i].join(wait);
                if (threads[i].isAlive()) {
                    if (!stacksDumped) {
                        dumpStacks(threads);
                        stacksDumped = true;
                    }
                    threads[i].interrupt();
                    // give the thread a couple of seconds, then call stop
                    Thread.sleep(5 * 1000);
                    if (threads[i].isAlive()) {
                        threads[i].stop();
                    }
                }
            } catch (InterruptedException e) {
                // ignore
            }
        }
        for (int i = 0; i < executors.length; i++) {
            if (executors[i].getException() != null) {
                throw executors[i].getException();
            }
        }
    }

    protected long getOneYearAhead() {
        return System.currentTimeMillis() + 1000L * 60L * 60L * 24L * 30L * 12L;
    }

    /**
     * If tests are run in a 1.5 JVM or higher the stack of the given threads
     * are dumped to the logger with level ERROR.
     *
     * @param threads An array of threads.
     */
    protected static void dumpStacks(Thread[] threads) {
        try {
            Method m = Thread.class.getMethod("getStackTrace", null);
            StringBuffer dumps = new StringBuffer();
            for (int t = 0; t < threads.length; t++) {
                StackTraceElement[] elements = (StackTraceElement[]) m.invoke(
                        threads[t], null);
                dumps.append(threads[t].toString()).append('\n');
                for (int i = 0; i < elements.length; i++) {
                    dumps.append("\tat " + elements[i]).append('\n');
                }
                dumps.append('\n');
            }
            logger.error("Thread dumps:\n{}", dumps);
        } catch (NoSuchMethodException e) {
            // not a 1.5 JVM
        } catch (IllegalAccessException e) {
            // ignore
        } catch (InvocationTargetException e) {
            // ignore
        }
    }

    /**
     * Task implementations must be thread safe! Multiple threads will call
     * {@link #execute(Session, Node)} concurrently.
     */
    public interface Task {

        public abstract void execute(Session session, Node test)
                throws RepositoryException;
    }

    protected static class Executor implements Runnable {

        protected final Session session;

        protected final Node test;

        protected final Task task;

        protected RepositoryException exception;

        public Executor(Session session, Node test, Task task) {
            this.session = session;
            this.test = test;
            this.task = task;
        }

        public RepositoryException getException() {
            return exception;
        }

        public void run() {
            try {
                task.execute(session, test);
            } catch (RepositoryException e) {
                exception = e;
            } finally {
                session.logout();
            }
        }
    }
}
