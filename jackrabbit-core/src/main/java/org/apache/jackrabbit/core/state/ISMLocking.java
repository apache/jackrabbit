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

import org.apache.jackrabbit.core.id.ItemId;

/**
 * <code>ISMLocking</code> defines an interface for a locking strategy of an
 * {@link ItemStateManager}.
 * <p>
 * An implementation of <code>ISMLocking</code> must meet the following
 * requirements:
 * <ul>
 * <li>While a read lock is held for a given item with id <code>I</code> an
 * implementation must ensure that no write lock is issued for a change log
 * that contains a reference to an item with id <code>I</code>.</li>
 * <li>While a write lock is held for a given change log <code>C</code> an
 * implementation must ensure that no read lock is issued for an item that is
 * contained in <code>C</code>, unless the current thread is the owner of the
 * write lock!</li>
 * <li>While a write lock is held for a given change log <code>C</code> an
 * implementation must ensure that no write lock is issued for a change log
 * <code>C'</code> that intersects with <code>C</code>. That is both change
 * logs contain a reference to the same item. Please note that an implementation
 * is free to block requests entirely for additional write lock while a write
 * lock is active. It is not a requirement to support concurrent write locks.
 * </li>
 * <li>While a write lock is held for a change log <code>C</code>, the holder
 * of the write lock (and any related threads) needs to be able to acquire
 * a read lock even if other writers are waiting for the lock. This behaviour
 * must continue also when the write lock has been downgraded. Note that it
 * is not necessary for a holder of a read lock to be able to upgrade to a
 * write lock.</li>
 * </ul>
 */
public interface ISMLocking {

    /**
     * Acquire a read lock for the given item <code>id</code>.
     * @param id an item id.
     */
    ReadLock acquireReadLock(ItemId id) throws InterruptedException;

    /**
     * Acquires a write lock for the given <code>changeLog</code>.
     *
     * @param changeLog the change log
     * @return the write lock for the given <code>changeLog</code>.
     * @throws InterruptedException if the thread is interrupted while creating
     *                              the write lock.
     */
    WriteLock acquireWriteLock(ChangeLog changeLog) throws InterruptedException;

    public interface ReadLock {

        /**
         * Releases this lock.
         */
        void release();

    }

    public interface WriteLock {

        /**
         * Releases this lock.
         */
        void release();

        /**
         * Downgrades this lock into a read lock. When this method returns this
         * write lock is effectively released and the returned read lock must be
         * used to further release the read lock.
         *
         * @return the read lock downgraded from this write lock.
         */
        ReadLock downgrade();

    }

}
