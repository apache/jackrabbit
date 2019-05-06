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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.util.XAReentrantWriterPreferenceReadWriteLock;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * A reentrant read-write lock used by the internal version manager for
 * synchronization. Unlike a normal reentrant lock, this one allows the lock
 * to be re-entered not just by a thread that's already holding the lock but
 * by any thread within the same transaction.
 */
public class VersioningLock {

    /**
     * The internal read-write lock.
     */
    private final XAReentrantWriterPreferenceReadWriteLock rwLock = new XAReentrantWriterPreferenceReadWriteLock();

    public ReadLock acquireReadLock() throws InterruptedException {
    	return new ReadLock(rwLock.readLock());
    }

    public WriteLock acquireWriteLock() throws InterruptedException {
    	return new WriteLock(rwLock);
    }

    public static class WriteLock {

        private ReadWriteLock readWriteLock;

        private WriteLock(ReadWriteLock readWriteLock)
                throws InterruptedException {
            this.readWriteLock = readWriteLock;
            this.readWriteLock.writeLock().acquire();
        }

        public void release() {
            readWriteLock.writeLock().release();
        }

        public ReadLock downgrade() throws InterruptedException {
            ReadLock rLock = new ReadLock(readWriteLock.readLock());
            release();
            return rLock;
        }

    }

    public static class ReadLock {

        private final Sync readLock;

        private ReadLock(Sync readLock) throws InterruptedException {
            this.readLock = readLock;
            this.readLock.acquire();
        }

        public void release() {
            readLock.release();
        }

    }
}