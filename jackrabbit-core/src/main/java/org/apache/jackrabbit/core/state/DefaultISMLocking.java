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

import java.util.Arrays;

import javax.transaction.xa.Xid;

import org.apache.jackrabbit.core.TransactionContext;
import org.apache.jackrabbit.core.id.ItemId;

/**
 * Default item state locking strategy. The default strategy is simply to use
 * a single coarse-grained read-write lock over the entire workspace.
 */
public class DefaultISMLocking implements ISMLocking {

    /**
     * The read lock instance used by readers to release the acquired lock.
     */
    private final ReadLock readLock = new ReadLock() {
        public void release() {
            releaseReadLock();
        }
    };

    /**
     * The write lock instance used by writers to release or downgrade the
     * acquired lock.
     */
    private final WriteLock writeLock = new WriteLock() {
        public void release() {
            releaseWriteLock(false);
        }
        public ReadLock downgrade() {
            releaseWriteLock(true);
            return readLock;
        }
    };

    /**
     * Number of writer threads waiting. While greater than zero, no new
     * (unrelated) readers are allowed to proceed.
     */
    private int writersWaiting = 0;

    /**
     * The thread identifier of the current writer, or <code>null</code> if
     * no write is in progress. A thread with the same identifier (i.e. the
     * same thread or another thread in the same transaction) can re-acquire
     * read or write locks without limitation, while all other readers and
     * writers remain blocked. Note that a downgraded write lock still retains
     * the writer thread identifier, which allows related threads to reacquire
     * read or write locks even when there are concurrent writers waiting.
     */
    private Object writerId = null;

    /**
     * Number of acquired write locks. All the concurrent write locks are
     * guaranteed to share the same thread identifier (see {@link #writerId}).
     */
    private int writerCount = 0;

    /**
     * Number of acquired read locks.
     */
    private int readerCount = 0;

    /**
     * Increments the reader count and returns the acquired read lock once
     * there are no more writers or the current writer shares the thread id
     * with this reader.
     */
    public synchronized ReadLock acquireReadLock(ItemId id)
            throws InterruptedException {
        while (writerId != null
                ? (writerCount > 0 && !isSameId(writerId, getCurrentId()))
                : writersWaiting > 0) {
            wait();
        }

        readerCount++;
        return readLock;
    }

    /**
     * Decrements the reader count and notifies all pending threads if the
     * lock is now available. Used by the {@link #readLock} instance.
     */
    private synchronized void releaseReadLock() {
        readerCount--;
        if (readerCount == 0 && writerCount == 0) {
            writerId = null;
            notifyAll();
        }
    }

    /**
     * Increments the writer count, sets the writer identifier and returns
     * the acquired read lock once there are no other active readers or
     * writers or the current writer shares the thread id with this writer.
     */
    public synchronized WriteLock acquireWriteLock(ChangeLog changeLog)
            throws InterruptedException {
        Object currentId = getCurrentId();

        writersWaiting++;
        try {
            while (writerId != null
                    ? !isSameId(writerId, currentId) : readerCount > 0) {
                wait();
            }
        } finally {
            writersWaiting--;
        }

        if (writerCount++ == 0) {
            writerId = currentId;
        }
        return writeLock;
    }

    /**
     * Decrements the writer count (and possibly clears the writer identifier)
     * and notifies all pending threads if the lock is now available. If the
     * downgrade argument is true, then the reader count is incremented before
     * notifying any pending threads. Used by the {@link #writeLock} instance.
     */
    private synchronized void releaseWriteLock(boolean downgrade) {
        writerCount--;
        if (downgrade) {
            readerCount++;
        }
        if (writerCount == 0) {
            if (readerCount == 0) {
                writerId = null;
            }
            notifyAll();
        }
    }

    /**
     * Returns the current thread identifier. The identifier is either the
     * current thread instance or the global transaction identifier when
     * running under a transaction.
     *
     * @return current thread identifier
     */
    private Object getCurrentId() {
        Xid xid = TransactionContext.getCurrentXid();
        if (xid != null) {
            return xid.getGlobalTransactionId();
        } else {
            return Thread.currentThread();
        }
    }

    /**
     * Compares the given thread identifiers for equality.
     */
    private boolean isSameId(Object a, Object b) {
        if (a == b) {
            return true;
        } else if (a instanceof byte[] && b instanceof byte[]) {
            return Arrays.equals((byte[]) a, (byte[]) b);
        } else {
            return false;
        }
    }

}
