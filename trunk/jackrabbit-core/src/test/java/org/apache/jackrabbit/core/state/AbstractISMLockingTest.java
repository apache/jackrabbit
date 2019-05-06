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
package org.apache.jackrabbit.core.state;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.ISMLocking.ReadLock;
import org.apache.jackrabbit.core.state.ISMLocking.WriteLock;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

/**
 * <code>AbstractISMLockingTest</code> contains test cases for the ISMLocking requirements.
 */
public abstract class AbstractISMLockingTest extends TestCase {

    /**
     * The {@link ISMLocking} instance under test.
     */
    protected ISMLocking locking;

    /**
     * Test node state.
     */
    protected NodeState state;

    /**
     * Node references instance targeting {@link #state}.
     */
    protected NodeReferences refs;

    /**
     * List of change logs, each with a different modification for {@link #state}.
     */
    protected List<ChangeLog> logs;

    protected void setUp() throws Exception {
        super.setUp();
        locking = createISMLocking();
        NodeId id = NodeId.randomId();
        state = new NodeState(id, NameConstants.NT_BASE, null, ItemState.STATUS_EXISTING, true);
        refs = new NodeReferences(state.getNodeId());
        logs = new ArrayList<ChangeLog>();
        ChangeLog log = new ChangeLog();
        log.added(state);
        logs.add(log);
        log = new ChangeLog();
        log.deleted(state);
        logs.add(log);
        log = new ChangeLog();
        log.modified(state);
        logs.add(log);
        log = new ChangeLog();
        log.modified(refs);
        logs.add(log);
    }

    /**
     * @return an {@link ISMLocking} instance which is exercised by the tests
     */
    public abstract ISMLocking createISMLocking();

    /**
     * Checks the following requirement: <p> <i>While a read lock is held for a given item with id
     * <code>I</code> an implementation must ensure that no write lock is issued for a change log that
     * contains a reference to an item with id <code>I</code>. </i>
     *
     * @throws InterruptedException on interruption; this will err the test
     */
    public void testReadBlocksWrite() throws InterruptedException {
        ReadLock rLock = locking.acquireReadLock(state.getId());
        for (ChangeLog changeLog : logs) {
            verifyBlocked(startWriterThread(locking, changeLog));
        }
        rLock.release();
    }

    /**
     * Checks the following requirement: <p> <i>While a write lock is held for a given change log
     * <code>C</code> an implementation must ensure that no read lock is issued for an item that is contained
     * in <code>C</code>, unless the current thread is the owner of the write lock!</i> <p> The "unless"
     * clause is tested by {@link #testWriteBlocksRead_notIfSameThread()} test.
     *
     * @throws InterruptedException on interruption; this will err the test
     */
    public void testWriteBlocksRead() throws InterruptedException {
        for (ChangeLog changeLog : logs) {
            WriteLock wLock = locking.acquireWriteLock(changeLog);
            verifyBlocked(startReaderThread(locking, state.getId()));
            wLock.release();
        }
    }

    public void testWriteBlocksRead_notIfSameThread() throws InterruptedException {
        for (final ChangeLog changeLog : logs) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        WriteLock wLock = locking.acquireWriteLock(changeLog);
                        locking.acquireReadLock(state.getId()).release();
                        wLock.release();
                    } catch (InterruptedException e) {
                    }
                }
            });
            t.start();
            verifyNotBlocked(t);
        }
    }

    /**
     * Checks the following requirement: <p> While a write lock is held for a given change log <code>C</code>
     * an implementation must ensure that no write lock is issued for a change log <code>C'</code> that intersects
     * with <code>C</code>. That is both change logs contain a reference to the same item. Please note that an
     * implementation is free to block requests entirely for additional write lock while a write lock is
     * active. It is not a requirement to support concurrent write locks.
     *
     * @throws InterruptedException on interruption; this will err the test
     */
    public void testIntersectingWrites() throws InterruptedException {
        ChangeLog cl = new ChangeLog();
        cl.added(state);
        WriteLock wLock = locking.acquireWriteLock(cl);
        for (ChangeLog changeLog : logs) {
            verifyBlocked(startWriterThread(locking, changeLog));
        }
        wLock.release();
    }

    /**
     * Checks if a downgraded write lock allows other threads to read again.
     *
     * @throws InterruptedException on interruption; this will err the test
     */
    public void testDowngrade() throws InterruptedException {
        for (ChangeLog changeLog : logs) {
            WriteLock wLock = locking.acquireWriteLock(changeLog);
            verifyBlocked(startReaderThread(locking, state.getId()));
            ReadLock rLock = wLock.downgrade();
            verifyNotBlocked(startReaderThread(locking, state.getId()));
            rLock.release();
        }
    }

    // ------------------------------< utilities >-------------------------------

    /**
     * Creates and starts a thread that acquires and releases the write lock of the given
     * <code>ISMLocking</code> for the given changelog. The thread's interrupted status is set if it was
     * interrupted during the acquire-release sequence and could therefore not finish it.
     *
     * @param lock the <code>ISMLocking</code> to use
     * @param changeLog the <code>ChangeLog</code> to use
     * @return a thread that has been started
     */
    protected final Thread startWriterThread(final ISMLocking lock, final ChangeLog changeLog) {
        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    lock.acquireWriteLock(changeLog).release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        t.start();
        return t;
    }

    /**
     * Creates and starts an thread that acquires and releases the read lock of the given
     * <code>ISMLocking</code> for the given id. The thread's interrupted status is set if it was interrupted
     * during the acquire-release sequence and could therefore not finish it.
     *
     * @param lock the <code>ISMLocking</code> to use
     * @param id the id to use
     * @return a thread that has been started
     */
    protected final Thread startReaderThread(final ISMLocking lock, final ItemId id) {
        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    lock.acquireReadLock(id).release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        t.start();
        return t;
    }

    /**
     * Verifies that the given thread is blocked. Then it interrupts the thread and waits a certain amount of
     * time for it to complete. (If it doesn't complete within that time then the test that calls this method
     * fails).
     *
     * @param other a started thread
     * @throws InterruptedException on interruption
     */
    protected final void verifyBlocked(final Thread other) throws InterruptedException {
        Thread.sleep(100);
        assertTrue(other.isAlive());
        other.interrupt();
        other.join(100);
        assertFalse(other.isAlive());
    }

    /**
     * Verifies that the given thread is not blocked and runs to completion within a certain amount of time
     * and without interruption. (If it doesn't complete within that time without interruption then the test
     * that calls this method fails).
     *
     * @param other a started thread
     * @throws InterruptedException on interruption
     */
    protected final void verifyNotBlocked(final Thread other) throws InterruptedException {
        other.join(1000);
        assertFalse(other.isInterrupted());
        assertFalse(other.isAlive());
    }
}
