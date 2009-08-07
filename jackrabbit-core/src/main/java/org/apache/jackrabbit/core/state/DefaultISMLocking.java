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

import javax.transaction.xa.Xid;

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.TransactionContext;

import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * <code>DefaultISMLocking</code> implements the default locking strategy using
 * coarse grained locking on an ItemStateManager wide read-write lock. E.g.
 * while a write lock is held, no read lock can be acquired.
 */
public class DefaultISMLocking implements ISMLocking {

    /**
     * The internal read-write lock.
     */
    private final RWLock rwLock = new RWLock();

    /**
     * {@inheritDoc}
     */
    public ReadLock acquireReadLock(ItemId id)
            throws InterruptedException {
        return new ReadLockImpl(rwLock.readLock());
    }

    /**
     * {@inheritDoc}
     */
    public WriteLock acquireWriteLock(ChangeLog changeLog)
            throws InterruptedException {
        return new WriteLock() {

            {
                rwLock.writeLock().acquire();
                rwLock.setActiveXid(TransactionContext.getCurrentXid());
            }

            /**
             * {@inheritDoc}
             */
            public void release() {
                rwLock.setActiveXid(null);
                rwLock.writeLock().release();
            }

            /**
             * {@inheritDoc}
             */
            public ReadLock downgrade() throws InterruptedException {
                ReadLock rLock = new ReadLockImpl(rwLock.readLock());
                release();
                return rLock;
            }
        };
    }

    private static final class ReadLockImpl implements ReadLock {

        private final Sync readLock;

        private ReadLockImpl(Sync readLock) throws InterruptedException {
            this.readLock = readLock;
            this.readLock.acquire();
        }

        /**
         * {@inheritDoc}
         */
        public void release() {
            readLock.release();
        }
    }

    private static final class RWLock extends ReentrantWriterPreferenceReadWriteLock {

        private Xid activeXid;

        /**
         * Allow reader when there is no active writer, or current thread owns
         * the write lock (reentrant).
         */
        protected boolean allowReader() {
            return TransactionContext.isCurrentXid(activeXid, (activeWriter_ == null || activeWriter_ == Thread.currentThread()));
        }

        /**
         * Sets the active Xid
         * @param xid
         */
        synchronized void setActiveXid(Xid xid) {
            activeXid = xid;
        }
        
    }
}
