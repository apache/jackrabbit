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
package org.apache.jackrabbit.core.integration.daily;

import org.apache.jackrabbit.core.AbstractConcurrencyTest;
import org.apache.jackrabbit.core.integration.random.task.VersionOperationsTask;
import org.apache.jackrabbit.core.integration.random.task.ContentOperationsTask;

import javax.jcr.RepositoryException;

/**
 * <code>RandomOperationTest</code> executes randomly chosen operations using
 * multiple threads. Each thread operates on its own subtree to avoid
 * conflicting changes.
 */
public class RandomOperationTest extends AbstractConcurrencyTest {

    /**
     * Each task is executed with this number of threads.
     */
    private static final int NUM_THREADS = 1;

    /**
     * Tasks are advised to run for this amount of time.
     */
    private static final int RUN_NUM_SECONDS = 60;

    /**
     * Number of seconds to wait at most for the tasks to finish their work.
     */
    private static final int MAX_WAIT_SECONDS = 60;

    /**
     * Number of levels of test data to create per thread
     */
    private static final int NUM_LEVELS = 4;

    /**
     * Number of nodes per level
     */
    private static final int NODES_PER_LEVEL = 3;

    /**
     * While creating nodes, save whenever 1000 nodes have been created.
     */
    private static final int SAVE_INTERVAL = 1000;

    private long end;

    protected void setUp() throws Exception {
        super.setUp();
        end = System.currentTimeMillis() + RUN_NUM_SECONDS * 1000;
    }

    public void testRandomContentOperations() throws RepositoryException {
        runTask(new ContentOperationsTask(NUM_LEVELS, NODES_PER_LEVEL, SAVE_INTERVAL, end), NUM_THREADS);
    }

    public void testRandomContentOperationsXA() throws RepositoryException {
        ContentOperationsTask task = new ContentOperationsTask(NUM_LEVELS, NODES_PER_LEVEL, SAVE_INTERVAL, end);
        task.setUseXA(true);
        runTask(task, NUM_THREADS);
    }

    public void testRandomVersionOperations() throws RepositoryException {
        runTask(new VersionOperationsTask(NUM_LEVELS, NODES_PER_LEVEL, SAVE_INTERVAL, end), NUM_THREADS);
    }

    public void testRandomVersionOperationsXA() throws RepositoryException {
        VersionOperationsTask task = new VersionOperationsTask(NUM_LEVELS, NODES_PER_LEVEL, SAVE_INTERVAL, end);
        task.setUseXA(true);
        runTask(task, NUM_THREADS);
    }

    public void testContentAndVersionOperations() throws RepositoryException {
        runTasks(new Task[]{
            new ContentOperationsTask(NUM_LEVELS, NODES_PER_LEVEL, SAVE_INTERVAL, end),
            new VersionOperationsTask(NUM_LEVELS, NODES_PER_LEVEL, SAVE_INTERVAL, end)
        }, NUM_THREADS, end + MAX_WAIT_SECONDS * 1000);
    }

    public void testContentAndVersionOperationsXA() throws RepositoryException {
        ContentOperationsTask task1 = new ContentOperationsTask(NUM_LEVELS, NODES_PER_LEVEL, SAVE_INTERVAL, end);
        task1.setUseXA(true);
        VersionOperationsTask task2 = new VersionOperationsTask(NUM_LEVELS, NODES_PER_LEVEL, SAVE_INTERVAL, end);
        task2.setUseXA(true);
        runTasks(new Task[]{task1, task2}, NUM_THREADS, end + MAX_WAIT_SECONDS * 1000);
    }

    /**
     * Test disabled since it violates the "Don't mix concurrent transactional
     * and non-transactional writes to a single workspace" guideline formed
     * during the concurrency review.
     *
     * @see <a href="http://jackrabbit.apache.org/concurrency-control.html">Concurrency control</a>
     * @see <a href="https://issues.apache.org/jira/browse/JCR-2000">JCR-2000</a>
     */
    public void disabledTestContentAndVersionOperationsXAMixed()
            throws RepositoryException {
        ContentOperationsTask task1 = new ContentOperationsTask(NUM_LEVELS, NODES_PER_LEVEL, SAVE_INTERVAL, end);
        ContentOperationsTask task2 = new ContentOperationsTask(NUM_LEVELS, NODES_PER_LEVEL, SAVE_INTERVAL, end);
        task2.setUseXA(true);
        VersionOperationsTask task3 = new VersionOperationsTask(NUM_LEVELS, NODES_PER_LEVEL, SAVE_INTERVAL, end);
        VersionOperationsTask task4 = new VersionOperationsTask(NUM_LEVELS, NODES_PER_LEVEL, SAVE_INTERVAL, end);
        task4.setUseXA(true);
        runTasks(new Task[]{task1, task2, task3, task4}, NUM_THREADS, end + MAX_WAIT_SECONDS * 1000);
    }

    /**
     * Test disabled since it violates the "Don't mix concurrent transactional
     * and non-transactional writes to a single workspace" guideline formed
     * during the concurrency review.
     *
     * @see <a href="http://jackrabbit.apache.org/concurrency-control.html">Concurrency control</a>
     * @see <a href="https://issues.apache.org/jira/browse/JCR-2000">JCR-2000</a>
     */
    public void disabledTestContentAndVersionOperationsXAMixedShortSaveInterval()
            throws RepositoryException {
        ContentOperationsTask task1 = new ContentOperationsTask(NUM_LEVELS, NODES_PER_LEVEL, 1, end);
        ContentOperationsTask task2 = new ContentOperationsTask(NUM_LEVELS, NODES_PER_LEVEL, 1, end);
        task2.setUseXA(true);
        VersionOperationsTask task3 = new VersionOperationsTask(NUM_LEVELS, NODES_PER_LEVEL, 1, end);
        VersionOperationsTask task4 = new VersionOperationsTask(NUM_LEVELS, NODES_PER_LEVEL, 1, end);
        task4.setUseXA(true);
        runTasks(new Task[]{task1, task2, task3, task4}, NUM_THREADS, end + MAX_WAIT_SECONDS * 1000);
    }

}
