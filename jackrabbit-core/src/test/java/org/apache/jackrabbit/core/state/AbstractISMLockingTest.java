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

import junit.framework.TestCase;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * <code>AbstractISMLockingTest</code> contains test cases for the ISMLocking
 * requirements.
 */
public abstract class AbstractISMLockingTest extends TestCase {

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
    protected List logs;

    protected void setUp() throws Exception {
        super.setUp();
        locking = createISMLocking();
        NodeId id = new NodeId(UUID.randomUUID());
        state = new NodeState(id, NameConstants.NT_BASE, null, ItemState.STATUS_EXISTING, true);
        refs = new NodeReferences(new NodeReferencesId(state.getNodeId()));
        logs = new ArrayList();
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
     * Checks the following requirement:
     * <p/>
     * <i>While a read lock is held for a given item with id <code>I</code> an
     * implementation must ensure that no write lock is issued for a change log
     * that contains a reference to an item with id <code>I</code>. </i>
     */
    public void testReadBlocksWrite() throws InterruptedException {
        ISMLocking.ReadLock rLock = locking.acquireReadLock(state.getId());
        try {
            for (Iterator it = logs.iterator(); it.hasNext(); ) {
                final ChangeLog log = (ChangeLog) it.next();
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        try {
                            checkBlocking(log);
                        } catch (InterruptedException e) {
                            fail(e.toString());
                        }
                    }
                });
                t.start();
                t.join();
            }
        } finally {
            rLock.release();
        }
    }

    /**
     * Checks the following requirement:
     * <p/>
     * <i>While a write lock is held for a given change log <code>C</code> an
     * implementation must ensure that no read lock is issued for an item that
     * is contained in <code>C</code>, unless the current thread is the owner of
     * the write lock!</i>
     */
    public void testWriteBlocksRead() throws InterruptedException {
        for (Iterator it = logs.iterator(); it.hasNext(); ) {
            ISMLocking.WriteLock wLock
                    = locking.acquireWriteLock((ChangeLog) it.next());
            try {
                checkNonBlocking(state.getId());
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        try {
                            checkBlocking(state.getId());
                        } catch (InterruptedException e) {
                            fail(e.toString());
                        }
                    }
                });
                t.start();
                t.join();
            } finally {
                wLock.release();
            }
        }
    }

    /**
     * Checks the following requirement:
     * <p/>
     * While a write lock is held for a given change log <code>C</code> an
     * implementation must ensure that no write lock is issued for a change log
     * <code>C'</code> that intersects with <code>C</code>. That is both change
     * logs contain a reference to the same item. Please note that an
     * implementation is free to block requests entirely for additional write
     * lock while a write lock is active. It is not a requirement to support
     * concurrent write locks.
     */
    public void testIntersectingWrites() throws InterruptedException {
        ChangeLog cl = new ChangeLog();
        cl.added(state);
        ISMLocking.WriteLock wLock = locking.acquireWriteLock(cl);
        try {
            for (Iterator it = logs.iterator(); it.hasNext(); ) {
                final ChangeLog log = (ChangeLog) it.next();
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        try {
                            checkBlocking(log);
                        } catch (InterruptedException e) {
                            fail(e.toString());
                        }
                    }
                });
                t.start();
                t.join();
            }
        } finally {
            wLock.release();
        }
    }

    /**
     * Checks if a downgraded write lock allows other threads to read again.
     */
    public void testDowngrade() throws InterruptedException {
        for (Iterator it = logs.iterator(); it.hasNext(); ) {
            ISMLocking.ReadLock rLock = locking.acquireWriteLock(
                    (ChangeLog) it.next()).downgrade();
            try {
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        try {
                            checkNonBlocking(state.getId());
                        } catch (InterruptedException e) {
                            fail(e.toString());
                        }
                    }
                });
                t.start();
                t.join();
            } finally {
                rLock.release();
            }
        }
    }

    //------------------------------< utilities >-------------------------------

    protected void checkBlocking(ChangeLog log)
            throws InterruptedException {
        final Thread t = Thread.currentThread();
        TimeBomb tb = new TimeBomb(100) {
            public void explode() {
                t.interrupt();
            }
        };
        tb.arm();
        try {
            locking.acquireWriteLock(log).release();
        } catch (InterruptedException e) {
            // success
            return;
        }
        tb.disarm();
        // make sure interrupted status is cleared
        // bomb may blow off right before we disarm it
        Thread.interrupted();
        fail("acquireWriteLock must block");
    }

    protected void checkBlocking(ItemId id)
            throws InterruptedException {
        final Thread t = Thread.currentThread();
        TimeBomb tb = new TimeBomb(100) {
            public void explode() {
                t.interrupt();
            }
        };
        tb.arm();
        try {
            locking.acquireReadLock(id).release();
        } catch (InterruptedException e) {
            // success
            return;
        }
        tb.disarm();
        // make sure interrupted status is cleared
        // bomb may blow off right before we disarm it
        Thread.interrupted();
        fail("acquireReadLock must block");
    }

    protected void checkNonBlocking(ItemId id)
            throws InterruptedException {
        final Thread t = Thread.currentThread();
        TimeBomb tb = new TimeBomb(100) {
            public void explode() {
                t.interrupt();
            }
        };
        tb.arm();
        try {
            locking.acquireReadLock(id).release();
        } catch (InterruptedException e) {
            fail("acquireReadLock must not block");
        }
        tb.disarm();
        // make sure interrupted status is cleared
        // bomb may blow off right before we disarm it
        Thread.interrupted();
    }

    public abstract ISMLocking createISMLocking();

    protected static abstract class TimeBomb {

        private final boolean[] armed = {false};

        private final long millis;

        private Thread timer;

        public TimeBomb(long millis) {
            this.millis = millis;
        }

        public void arm() throws InterruptedException {
            synchronized (armed) {
                if (armed[0]) {
                    return;
                } else {
                    timer = new Thread(new Runnable() {
                        public void run() {
                            synchronized (armed) {
                                armed[0] = true;
                                armed.notify();
                            }
                            try {
                                Thread.sleep(millis);
                                explode();
                            } catch (InterruptedException e) {
                                // disarmed
                            }
                        }
                    });
                    timer.start();
                }
            }
            synchronized (armed) {
                while (!armed[0]) {
                    armed.wait();
                }
            }
        }

        public void disarm() throws InterruptedException {
            synchronized (armed) {
                if (!armed[0]) {
                    return;
                }
            }
            timer.interrupt();
            timer.join();
        }

        public abstract void explode();
    }
}
