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
package org.apache.jackrabbit.core.query.lucene;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <code>DynamicPooledExecutor</code> implements an executor, which dynamically
 * adjusts its maximum number of threads according to the number of available
 * processors returned by {@link Runtime#availableProcessors()}.
 */
public class DynamicPooledExecutor implements Executor {

    /**
     * The underlying pooled executor.
     */
    private final ThreadPoolExecutor executor;

    /**
     * The time (in milliseconds) when the pool size was last checked.
     */
    private long lastCheck;

    /**
     * Creates a new DynamicPooledExecutor.
     */
    public DynamicPooledExecutor() {
        ThreadFactory f = new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "DynamicPooledExecutor");
                t.setDaemon(true);
                return t;
            }
        };
        this.executor = new ThreadPoolExecutor(
                1, Runtime.getRuntime().availableProcessors(),
                500, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), f);
        this.lastCheck = System.currentTimeMillis();
    }

    /**
     * Adjusts the pool size at most once every second.
     */
    private synchronized void adjustPoolSize() {
        long now = System.currentTimeMillis();
        if (lastCheck + 1000 < now) {
            int n = Runtime.getRuntime().availableProcessors();
            if (n != executor.getMaximumPoolSize()) {
                executor.setMaximumPoolSize(n);
            }
            lastCheck = now;
        }
    }

    /**
     * Executes the given command. This method will block if all threads in the
     * pool are busy and return only when the command has been accepted. Care
     * must be taken, that no deadlock occurs when multiple commands are
     * scheduled for execution. In general commands should not depend on the
     * execution of other commands!
     *
     * @param command the command to execute.
     */
    public void execute(Runnable command) {
        adjustPoolSize();
        if (executor.getMaximumPoolSize() == 1) {
            // if there is only one processor execute with current thread
            command.run();
        } else {
            executor.execute(command);
        }
    }

}
