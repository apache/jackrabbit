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

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
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
	 * Thread concerning ReentrantWriterPreferenceReadWriteLock
     */
    private final ReadWriteLock rwLock = new ReentrantWriterPreferenceReadWriteLock();
    
    /**
     * The internal Xid aware read-write lock.
     */
    private final ReadWriteLock xidRwLock = new XidRWLock();
    
    /**
     * {@inheritDoc}
     */
    public ReadLock acquireReadLock(ItemId id) throws InterruptedException {
    	if (TransactionContext.getCurrentXid() == null) {
            return new ReadLockImpl(rwLock.readLock());
    	} else {
            return new ReadLockImpl(xidRwLock.readLock());
    	}
    }

    /**
     * {@inheritDoc}
     */
    public WriteLock acquireWriteLock(ChangeLog changeLog) throws InterruptedException {
    	if (TransactionContext.getCurrentXid() == null) {
    		return new WriteLockImpl(rwLock);
    	} else {
    		return new WriteLockImpl(xidRwLock);
    	}
    }

    private static final class WriteLockImpl implements WriteLock {
    	
    	private ReadWriteLock readWriteLock;
    	
    	private WriteLockImpl(ReadWriteLock readWriteLock) throws InterruptedException {
    		this.readWriteLock = readWriteLock;
    		this.readWriteLock.writeLock().acquire();
		}

		/**
		 * {@inheritDoc}
		 */
		public void release() {
		    this.readWriteLock.writeLock().release();
		}

		/**
		 * {@inheritDoc}
		 */
		public ReadLock downgrade() throws InterruptedException {
		    ReadLock rLock = new ReadLockImpl(this.readWriteLock.readLock());
		    release();
		    return rLock;
		}
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

	/**
	 * Xid concerning ReentrantWriterPreferenceReadWriteLock
	 */
    private static final class XidRWLock extends ReentrantWriterPreferenceReadWriteLock {

        private Xid activeXid;

        /**
         * Check if the given Xid comes from the same globalTX
         * @param otherXid
         * @return true if same globalTX otherwise false
         */
        boolean isSameGlobalTx(Xid otherXid) {
    	    return (activeXid == otherXid) || Arrays.equals(activeXid.getGlobalTransactionId(), otherXid.getGlobalTransactionId());
    	}

        /**
         * Allow reader when there is no active Xid, or current Xid owns
         * the write lock (reentrant).
         */
        protected boolean allowReader() {
        	Xid currentXid = TransactionContext.getCurrentXid();
        	return (activeXid == null && waitingWriters_ == 0) || isSameGlobalTx(currentXid);
        }

        /**
         * {@inheritDoc}
         */  
        protected synchronized boolean startWrite() {
        	Xid currentXid = TransactionContext.getCurrentXid();
            if (activeXid != null && isSameGlobalTx(currentXid)) { // already held; re-acquire
            	++writeHolds_;
                return true;
            } else if (writeHolds_ == 0) {
            	if (activeReaders_ == 0 || (readers_.size() == 1 && readers_.get(currentXid) != null)) {
            		activeXid = currentXid;
            		writeHolds_ = 1;
            		return true;
            	} else {
            		return false;
            	}
            } else {
            	return false;
            }
        }

        /**
         * {@inheritDoc}
         */
        protected synchronized Signaller endWrite() {
            --writeHolds_;
            if (writeHolds_ > 0) {  // still being held
            	return null;
            } else {
            	activeXid = null;
                if (waitingReaders_ > 0 && allowReader()) {
                    return readerLock_;
                } else if (waitingWriters_ > 0) {
                    return writerLock_;
                } else {
                    return null;
                }
            }
        }

        /**
         * {@inheritDoc}
         */
		protected synchronized boolean startRead() {
			Xid currentXid = TransactionContext.getCurrentXid();
		    Object c = readers_.get(currentXid);
		    if (c != null) { // already held -- just increment hold count
		    	readers_.put(currentXid, new Integer(((Integer)(c)).intValue()+1));
		    	++activeReaders_;
		    	return true;
		    } else if (allowReader()) {
		    	readers_.put(currentXid, IONE);
		    	++activeReaders_;
		    	return true;
		    } else {
		    	return false;
		    }
		}

        /**
         * {@inheritDoc}
         */
		protected synchronized Signaller endRead() {
			Xid currentXid = TransactionContext.getCurrentXid();
		    Object c = readers_.get(currentXid);
		    if (c == null) {
		    	throw new IllegalStateException();
		    }
		    --activeReaders_;
		    if (c != IONE) { // more than one hold; decrement count
		    	int h = ((Integer)(c)).intValue()-1;
		    	Integer ih = (h == 1)? IONE : new Integer(h);
		    	readers_.put(currentXid, ih);
		    	return null;
		    } else {
		    	readers_.remove(currentXid);
		    
		    	if (writeHolds_ > 0) { // a write lock is still held
		    		return null;
		    	} else if (activeReaders_ == 0 && waitingWriters_ > 0) {
		    		return writerLock_;
		    	} else  {
		    		return null;
		    	}
		    }
		}
    }
}
