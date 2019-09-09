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
package org.apache.jackrabbit.test;

import junit.framework.TestSuite;
import junit.framework.TestResult;
import junit.framework.Test;
import EDU.oswego.cs.dl.util.concurrent.Executor;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

/**
 * <code>ConcurrentTestSuite</code> implements a test suite that runs tests
 * with a given concurrency level using multiple threads.
 */
public class ConcurrentTestSuite extends TestSuite {

    private final Executor executor;

    private volatile int finishedTestCount;

    public ConcurrentTestSuite(String name) {
        this(name, Runtime.getRuntime().availableProcessors() * 2);
    }

    public ConcurrentTestSuite() {
        this(null);
    }

    public ConcurrentTestSuite(int numThreads) {
        this(null, numThreads);
    }

    public ConcurrentTestSuite(String name, int numThreads) {
        super(name);
        executor = new PooledExecutor(numThreads) {
            {
                waitWhenBlocked();
            }
        };
    }

    public void run(TestResult result) {
        finishedTestCount = 0;
        super.run(result);
        waitUntilFinished();
    }

    public void runTest(final Test test, final TestResult result) {
        try {
            executor.execute(new Runnable() {
                public void run() {
                    try {
                        ConcurrentTestSuite.super.runTest(test, result);
                    } finally {
                        runFinished();
                    }
                }
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e); 
        }
    }

    private synchronized void waitUntilFinished() {
        while (finishedTestCount < testCount()) {
            try {
                wait();
            } catch (InterruptedException e) {
                return; // ignore
            }
        }
    }

    private synchronized void runFinished() {
        finishedTestCount++;
        notifyAll();
    }
}
