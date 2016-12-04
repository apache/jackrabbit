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

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread pool used by the repository.
 */
class JackrabbitThreadPool extends ScheduledThreadPoolExecutor {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory
            .getLogger(JackrabbitThreadPool.class);

    /**
     * Size of the per-repository thread pool.
     */
    private static final int size =
            Runtime.getRuntime().availableProcessors() * 2;

    /**
     * The classloader used as the context classloader of threads in the pool.
     */
    private static final ClassLoader loader =
            JackrabbitThreadPool.class.getClassLoader();

    /**
     * Thread counter for generating unique names for the threads in the pool.
     */
    private static final AtomicInteger counter = new AtomicInteger(1);

    /**
     * Thread factory for creating the threads in the pool
     */
    private static final ThreadFactory factory = new ThreadFactory() {
        public Thread newThread(Runnable runnable) {
            int count = counter.getAndIncrement();
            String name = "jackrabbit-pool-" + count;
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
            thread.setContextClassLoader(loader);
            return thread;
        }
    };

    /**
     * Handler for tasks for which no free thread is found within the pool.
     */
    private static final RejectedExecutionHandler handler = new CallerRunsPolicy();

    /**
     * Property to control the value at which the thread pool starts to schedule
     * the {@link LowPriorityTask} tasks for later execution.
     * 
     * Set to <code>0</code> to disable the check
     * 
     * Default value is 0 (check is disabled).
     * 
     */
    public static final String MAX_LOAD_FOR_LOW_PRIORITY_TASKS_PROPERTY = "org.apache.jackrabbit.core.JackrabbitThreadPool.maxLoadForLowPriorityTasks";

    /**
     * @see #MAX_LOAD_FOR_LOW_PRIORITY_TASKS_PROPERTY
     */
    private final static Integer maxLoadForLowPriorityTasks = getMaxLoadForLowPriorityTasks();

    private static int getMaxLoadForLowPriorityTasks() {
        final int defaultMaxLoad = 75;
        int max = Integer.getInteger(MAX_LOAD_FOR_LOW_PRIORITY_TASKS_PROPERTY,
                defaultMaxLoad);
        if (max < 0 || max > 100) {
            return defaultMaxLoad;
        }
        return max;
    }

    /**
     * Queue where all the {@link LowPriorityTask} tasks go for later execution
     */
    private final BlockingQueue<Runnable> lowPriorityTasksQueue = new LinkedBlockingQueue<Runnable>();

    /**
     * Tasks that handles the scheduling and the execution of
     * {@link LowPriorityTask} tasks
     */
    private final RetryLowPriorityTask retryTask;

    /**
     * Creates a new thread pool.
     */
    public JackrabbitThreadPool() {
        super(size, factory, handler);
        retryTask = new RetryLowPriorityTask(this, lowPriorityTasksQueue);
    }

    @Override
    public void execute(Runnable command) {
        if (command instanceof LowPriorityTask) {
            scheduleLowPriority(command);
            return;
        }
        super.execute(command);
    }

    private void scheduleLowPriority(Runnable command) {
        if (isOverDefinedMaxLoad()) {
            lowPriorityTasksQueue.add(command);
            retryTask.retryLater();
            return;
        }
        super.execute(command);
    }

    /**
     * compares the current load of the executor with the defined
     * <code>{@link #maxLoadForLowPriorityTasks}</code> parameter.
     * 
     * Used to determine if the executor can handle additional
     * {@link LowPriorityTask} tasks.
     * 
     * @return true if the load is under the
     *         <code>{@link #maxLoadForLowPriorityTasks}</code> parameter
     */
    private boolean isOverDefinedMaxLoad() {
        if (maxLoadForLowPriorityTasks == 0) {
            return false;
        }
        double currentLoad = ((double) getActiveCount()) / getPoolSize() * 100;
        return currentLoad > maxLoadForLowPriorityTasks;
    }

    /**
     * TEST ONLY
     * 
     * @return the number of low priority tasks that are waiting in the queue
     */
    int getPendingLowPriorityTaskCount() {
        return lowPriorityTasksQueue.size();
    }

    private static final class RetryLowPriorityTask implements Runnable {

        /**
         * schedule interval in ms for delayed tasks
         */
        private static final int LATER_MS = 50;

        private final JackrabbitThreadPool executor;
        private final BlockingQueue<Runnable> lowPriorityTasksQueue;

        /**
         * flag to indicate that another execute has been scheduled or is
         * currently running.
         */
        private final AtomicBoolean retryPending;

        public RetryLowPriorityTask(JackrabbitThreadPool executor,
                BlockingQueue<Runnable> lowPriorityTasksQueue) {
            this.executor = executor;
            this.lowPriorityTasksQueue = lowPriorityTasksQueue;
            this.retryPending = new AtomicBoolean(false);
        }

        public void retryLater() {
            if (!retryPending.getAndSet(true)) {
                executor.schedule(this, LATER_MS, TimeUnit.MILLISECONDS);
            }
        }

        public void run() {
            int count = 0;
            while (!executor.isOverDefinedMaxLoad()) {
                Runnable r = lowPriorityTasksQueue.poll();
                if (r == null) {
                    log.debug("Executed {} low priority tasks.", count);
                    break;
                }
                count++;
                executor.execute(r);
            }
            retryPending.set(false);
            if (!lowPriorityTasksQueue.isEmpty()) {
                log.debug(
                        "Executor is under load, will schedule {} remaining tasks for {} ms later",
                        lowPriorityTasksQueue.size(), LATER_MS);
                retryLater();
            }
        }
    }
}
