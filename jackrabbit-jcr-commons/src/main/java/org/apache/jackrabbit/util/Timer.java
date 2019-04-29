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

import java.util.TimerTask;

/**
 * <code>Timer</code> wraps the standard Java {@link java.util.Timer} class
 * and implements a guaranteed shutdown of the background thread running
 * in the <code>Timer</code> instance after a certain {@link #IDLE_TIME}.
 */
public class Timer {

    /**
     * Idle time in milliseconds. When a timer instance is idle for this amount
     * of time the underlying timer is canceled.
     */
    static final int IDLE_TIME = 3 * 1000;

    /**
     * The interval at which the idle checker task runs.
     */
    static final int CHECKER_INTERVAL = 1000;

    /**
     * The timer implementation we us internally.
     */
    private java.util.Timer delegatee;

    /**
     * Indicates whether the timer thread should run as deamon.
     */
    private final boolean runAsDeamon;

    /**
     * The number of currently scheduled tasks. If this value drops to zero
     * the internal {@link java.util.Timer} instance is canceled. Whenever
     * this value increases from zero to one a new {@link java.util.Timer}
     * instance is created and started.
     */
    private int numScheduledTasks = 0;

    /**
     * The time when the last task was scheduled.
     */
    private long lastTaskScheduled;

    /**
     * Creates a new <code>Timer</code> instance.
     *
     * @param isDeamon if <code>true</code> the background thread wil run as
     *                 deamon.
     */
    public Timer(boolean isDeamon) {
        runAsDeamon = isDeamon;
    }

    /**
     * Schedules the specified task for repeated <i>fixed-delay execution</i>,
     * beginning after the specified delay.  Subsequent executions take place
     * at approximately regular intervals separated by the specified period.
     *
     * @param task   task to be scheduled.
     * @param delay  delay in milliseconds before task is to be executed.
     * @param period time in milliseconds between successive task executions.
     * @throws IllegalArgumentException if {@code delay} is negative, or
     *         {@code delay + System.currentTimeMillis()} is negative.
     * @throws IllegalStateException if task was already scheduled or
     *         cancelled, timer was cancelled, or timer thread terminated.
     * @see java.util.Timer#schedule(java.util.TimerTask, long, long)
     */
    public void schedule(Task task, long delay, long period) {
        if (delay < 0)
            throw new IllegalArgumentException("Negative delay.");
        if (period <= 0)
            throw new IllegalArgumentException("Non-positive period.");
        synchronized (this) {
            if (delegatee == null) {
                delegatee = new java.util.Timer(runAsDeamon);
                // run idle checker every second
                Task idleChecker = new IdleCheckerTask();
                idleChecker.setTimer(this);
                delegatee.schedule(idleChecker, IDLE_TIME, CHECKER_INTERVAL);
            }
            delegatee.schedule(task, delay, period);
            task.setTimer(this);
            numScheduledTasks++;
            lastTaskScheduled = System.currentTimeMillis();
        }
    }

    /**
     * Terminates this timer, discarding any currently scheduled tasks.
     * Does not interfere with a currently executing task (if it exists).
     * Once a timer has been terminated, its execution thread terminates
     * gracefully, and no more tasks may be scheduled on it.
     *
     * <p>Note that calling this method from within the run method of a
     * timer task that was invoked by this timer absolutely guarantees that
     * the ongoing task execution is the last task execution that will ever
     * be performed by this timer.
     *
     * <p>This method may be called repeatedly; the second and subsequent
     * calls have no effect.
     */
    public void cancel() {
        synchronized (this) {
            if (delegatee != null) {
                delegatee.cancel();
                numScheduledTasks = 0;
                delegatee = null;
            }
        }
    }

    /**
     * @return <code>true</code> if this timer has a running backround thread
     *         for scheduled tasks. This method is only for test purposes.
     */
    boolean isRunning() {
        synchronized (this) {
            return delegatee != null;
        }
    }

    /**
     * Notifies this <code>Timer</code> that a task has been canceled.
     */
    private void taskCanceled() {
        synchronized (this) {
            --numScheduledTasks;
        }
    }

    /**
     * Extends the <code>TimerTask</code> with callback hooks to this
     * <code>Timer</code> implementation.
     */
    public static abstract class Task extends TimerTask {

        /**
         * The <code>Timer</code> instance where this <code>Task</code> is
         * scheduled on.
         */
        private Timer timer;

        /**
         * Sets the timer instance where this task is scheduled on.
         * @param timer the timer instance.
         */
        private void setTimer(Timer timer) {
            this.timer = timer;
        }

        /**
         * {@inheritDoc}
         */
        public final boolean cancel() {
            if (timer != null) {
                timer.taskCanceled();
                timer = null;
            }
            return super.cancel();
        }
    }

    /**
     * Checks if the enclosing timer had been idle for at least
     * {@link Timer#IDLE_TIME} and cancels it in that case.
     */
    private class IdleCheckerTask extends Task {

        public void run() {
            synchronized (Timer.this) {
                if (numScheduledTasks == 0 &&
                        System.currentTimeMillis() > lastTaskScheduled + IDLE_TIME) {
                    if (delegatee != null) {
                        delegatee.cancel();
                        delegatee = null;
                    }
                }
            }
        }
    }
}
