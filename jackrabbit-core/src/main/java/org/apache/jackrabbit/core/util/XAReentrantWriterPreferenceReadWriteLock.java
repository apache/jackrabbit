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
package org.apache.jackrabbit.core.util;

import static org.apache.jackrabbit.data.core.TransactionContext.getCurrentThreadId;
import static org.apache.jackrabbit.data.core.TransactionContext.isSameThreadId;
import EDU.oswego.cs.dl.util.concurrent.ReentrantWriterPreferenceReadWriteLock;

/**
 * A reentrant read-write lock for synchronization. 
 * Unlike a normal reentrant lock, this one allows the lock
 * to be re-entered not just by a thread that's already holding the lock but
 * by any thread within the same transaction.
 */
public class XAReentrantWriterPreferenceReadWriteLock extends ReentrantWriterPreferenceReadWriteLock{
	
	private Object activeWriter;
    
    /**
     * {@inheritDoc}
     */
    protected boolean allowReader() {
        Object currentId = getCurrentThreadId();
        return (activeWriter == null && waitingWriters_ == 0) || isSameThreadId(activeWriter, currentId);
    }

    /**
     * {@inheritDoc}
     */
    protected synchronized boolean startWrite() {
    	Object currentId = getCurrentThreadId();
        if (activeWriter != null && isSameThreadId(activeWriter, currentId)) { // already held; re-acquire
        	++writeHolds_;
            return true;
        } else if (writeHolds_ == 0) {
        	if (activeReaders_ == 0 || (readers_.size() == 1 && readers_.get(currentId) != null)) {
        		activeWriter = currentId;
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
        	activeWriter = null;
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
	@SuppressWarnings("unchecked")
	protected synchronized boolean startRead() {
		Object currentId = getCurrentThreadId();
	    Object c = readers_.get(currentId);
	    if (c != null) { // already held -- just increment hold count
	    	readers_.put(currentId, new Integer(((Integer)(c)).intValue()+1));
	    	++activeReaders_;
	    	return true;
	    } else if (allowReader()) {
	    	readers_.put(currentId, IONE);
	    	++activeReaders_;
	    	return true;
	    } else {
	    	return false;
	    }
	}
	
    /**
     * {@inheritDoc}
     */
	@SuppressWarnings("unchecked")
	protected synchronized Signaller endRead() {
		Object currentId = getCurrentThreadId();
	    Object c = readers_.get(currentId);
	    if (c == null) {
	    	throw new IllegalStateException();
	    }
	    --activeReaders_;
	    if (c != IONE) { // more than one hold; decrement count
	    	int h = ((Integer)(c)).intValue()-1;
	    	Integer ih = (h == 1)? IONE : new Integer(h);
	    	readers_.put(currentId, ih);
	    	return null;
	    } else {
	    	readers_.remove(currentId);
	    
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
