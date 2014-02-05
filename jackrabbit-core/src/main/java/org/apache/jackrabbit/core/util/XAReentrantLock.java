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

import static org.apache.jackrabbit.data.core.TransactionContext.isSameThreadId;

import org.apache.jackrabbit.data.core.TransactionContext;

import EDU.oswego.cs.dl.util.concurrent.ReentrantLock;

/**
 * A reentrant lock for synchronization. 
 * Unlike a normal reentrant lock, this one allows the lock
 * to be re-entered not just by a thread that's already holding the lock but
 * by any thread within the same transaction.
 */
public class XAReentrantLock extends ReentrantLock {
	
	/**
	 * The active lock holder of this {@link ReentrantLock}
	 */
    private Object activeId;

    /**
     * {@inheritDoc}
     */
    @Override
    public void acquire() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        Object currentId = TransactionContext.getCurrentThreadId();
        synchronized(this) {
            if (currentId == activeId || (activeId != null && isSameThreadId(activeId, currentId))) { 
                ++holds_;
            } else {
                try {  
                    while (activeId != null) {
                        wait(); 
                    }
                    activeId = currentId;
                    holds_ = 1;
                } catch (InterruptedException ex) {
                    notify();
                    throw ex;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void release()  {
        Object currentId = TransactionContext.getCurrentThreadId();
        if (activeId != null && !isSameThreadId(activeId, currentId)) {
            throw new Error("Illegal Lock usage"); 
        }

        if (--holds_ == 0) {
            activeId = null;
            notify(); 
        }
    }
}
