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

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.FutureResult;
import EDU.oswego.cs.dl.util.concurrent.Callable;

import java.lang.reflect.InvocationTargetException;

/**
 * <code>DynamicPooledExecutor</code> implements an executor, which dynamically
 * adjusts its maximum number of threads according to the number of available
 * processors returned by {@link Runtime#availableProcessors()}.
 */
public class DynamicPooledExecutor {

    /**
     * The underlying pooled executor.
     */
    private final PooledExecutor executor;

    /**
     * Timestamp when the pool size was last checked.
     */
    private volatile long lastCheck;

    /**
     * The number of processors.
     */
    private volatile int numProcessors;

    /**
     * Creates a new DynamicPooledExecutor.
     */
    public DynamicPooledExecutor() {
        executor = new PooledExecutor();
        executor.setKeepAliveTime(500);
        adjustPoolSize();
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
        if (numProcessors == 1) {
            // if there is only one processor execute with current thread
            command.run();
        } else {
            try {
                executor.execute(command);
            } catch (InterruptedException e) {
                // run with current thread instead
                command.run();
            }
        }
    }

    /**
     * Executes a set of commands and waits until all commands have been
     * executed. The results of the commands are returned in the same order as
     * the commands.
     *
     * @param commands the commands to execute.
     * @return the results.
     */
    public Result[] executeAndWait(Command[] commands) {
        Result[] results = new Result[commands.length];
        if (numProcessors == 1) {
            // optimize for one processor
            for (int i = 0; i < commands.length; i++) {
                Object obj = null;
                InvocationTargetException ex = null;
                try {
                    obj = commands[i].call();
                } catch (Exception e) {
                    ex = new InvocationTargetException(e);
                }
                results[i] = new Result(obj, ex);
            }
        } else {
            FutureResult[] futures = new FutureResult[commands.length];
            for (int i = 0; i < commands.length; i++) {
                final Command c = commands[i];
                futures[i] = new FutureResult();
                Runnable r = futures[i].setter(new Callable() {
                    public Object call() throws Exception {
                        return c.call();
                    }
                });
                try {
                    executor.execute(r);
                } catch (InterruptedException e) {
                    // run with current thread instead
                    r.run();
                }
            }
            // wait for all results
            boolean interrupted = false;
            for (int i = 0; i < futures.length; i++) {
                Object obj = null;
                InvocationTargetException ex = null;
                for (;;) {
                    try {
                        obj = futures[i].get();
                    } catch (InterruptedException e) {
                        interrupted = true;
                        // reset interrupted status and try again
                        Thread.interrupted();
                        continue;
                    } catch (InvocationTargetException e) {
                        ex = e;
                    }
                    results[i] = new Result(obj, ex);
                    break;
                }
            }
            if (interrupted) {
                // restore interrupt status again
                Thread.currentThread().interrupt();
            }
        }
        return results;
    }

    /**
     * Adjusts the pool size at most once every second.
     */
    private void adjustPoolSize() {
        if (lastCheck + 1000 < System.currentTimeMillis()) {
            int n = Runtime.getRuntime().availableProcessors();
            if (numProcessors != n) {
                executor.setMaximumPoolSize(n);
                numProcessors = n;
            }
            lastCheck = System.currentTimeMillis();
        }
    }

    public interface Command {

        /**
         * Perform some action that returns a result or throws an exception
         */
        Object call() throws Exception;
    }

    public static class Result {

        /**
         * The result object or <code>null</code> if an exception was thrown.
         */
        private final Object object;

        /**
         * The exception or <code>null</code> if no exception was thrown.
         */
        private final InvocationTargetException exception;

        private Result(Object object, InvocationTargetException exception) {
            this.object = object;
            this.exception = exception;
        }

        /**
         * @return the result object or <code>null</code> if an exception was
         *         thrown.
         */
        public Object get() {
            return object;
        }

        /**
         * @return the exception or <code>null</code> if no exception was
         *         thrown.
         */
        public InvocationTargetException getException() {
            return exception;
        }
    }
}
