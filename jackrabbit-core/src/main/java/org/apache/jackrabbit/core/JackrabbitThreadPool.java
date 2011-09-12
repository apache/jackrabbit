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
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread pool used by the repository.
 */
class JackrabbitThreadPool extends ScheduledThreadPoolExecutor {

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
    private static final RejectedExecutionHandler handler =
            new ThreadPoolExecutor.CallerRunsPolicy();

    /**
     * Creates a new thread pool.
     */
    public JackrabbitThreadPool() {
        super(size, factory, handler);
    }

}
