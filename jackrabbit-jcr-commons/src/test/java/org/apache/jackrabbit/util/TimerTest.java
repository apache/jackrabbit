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
package org.apache.jackrabbit.util;

import junit.framework.TestCase;

/**
 * <code>TimerTest</code> checks if the internal thread of a timer is stopped
 * after {@link Timer#IDLE_TIME} elapsed.
 */
public class TimerTest extends TestCase {

    private Timer timer;

    private DummyTask task;

    protected void setUp() throws Exception {
        super.setUp();
        timer = new Timer(true);
        task = new DummyTask();
    }

    protected void tearDown() throws Exception {
        timer.cancel();
        super.tearDown();
    }

    public void testInitiallyNotRunning() {
        assertTrue("Timer must not be running without a scheduled task", !timer.isRunning());
    }

    public void testIsRunning() {
        timer.schedule(task, 0, Integer.MAX_VALUE);
        assertTrue("Timer must be running with a scheduled task", timer.isRunning());
    }

    public void testLongDelay() throws InterruptedException {
        int testDelay = Timer.IDLE_TIME + 1000;
        timer.schedule(task, testDelay, Integer.MAX_VALUE);
        Thread.sleep(testDelay);
        assertTrue("Timer must be running with a scheduled task", timer.isRunning());
    }

    public void testIdle() throws InterruptedException {
        timer.schedule(task, 0, Integer.MAX_VALUE);
        task.waitUntilRun();
        task.cancel();
        assertTrue("Timer must be running while idle", timer.isRunning());
        Thread.sleep(Timer.IDLE_TIME + 2 * Timer.CHECKER_INTERVAL);
        assertTrue("Timer must not be running after idle time elapsed", !timer.isRunning());
    }

    private static class DummyTask extends Timer.Task {

        private boolean run = false;

        public void run() {
            synchronized (this) {
                run = true;
                notifyAll();
            }
        }

        public void waitUntilRun() throws InterruptedException {
            synchronized (this) {
                while (!run) {
                    wait();
                }
            }
        }
    }
}
