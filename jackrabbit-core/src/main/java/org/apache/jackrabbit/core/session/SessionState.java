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
package org.apache.jackrabbit.core.session;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The internal state of a session.
 */
public class SessionState {

    /**
     * Logger instance.
     */
    private static final Logger log =
        LoggerFactory.getLogger(SessionState.class);

    /**
     * The lock used to guarantee synchronized execution of repository
     * operations. An explicit lock is used instead of normal Java
     * synchronization in order to be able to log attempts to concurrently
     * use a session. TODO: Check if this is a performance issue!
     */
    private final Lock lock = new ReentrantLock();

    private volatile Exception closed = null;

    /**
     * Checks whether this session is alive.
     *
     * @see Session#isLive()
     * @return <code>true</code> if the session is alive,
     *         <code>false</code> otherwise
     */
    public boolean isAlive() {
        return closed == null;
    }

    /**
     * Throws an exception if this session is not alive.
     *
     * @throws RepositoryException throw if this session is not alive
     */
    public void checkAlive() throws RepositoryException {
        if (!isAlive()) {
            throw new RepositoryException(
                    "This session has been closed. See the chained exception"
                    + " for a trace of where the session was closed", closed);
        }
    }

    /**
     * Performs the given operation within a synchronized block.
     *
     * @throws RepositoryException if the operation fails
     */
    public void perform(SessionOperation operation) throws RepositoryException {
        if (!lock.tryLock()) {
            log.warn("Attempt to perform {} while another thread is"
                    + " concurrently accessing the session. Blocking until"
                    + " the other thread is finished using this session.",
                    operation);
            lock.lock();
        }
        try {
            checkAlive();
            log.debug("Performing {}", operation);
            operation.perform();
        } finally {
            lock.unlock();
        }
    }

    public boolean close() {
        if (!lock.tryLock()) {
            log.warn("Attempt to close a session while another thread is"
                    + " concurrently accessing the session. Blocking until"
                    + " the other thread is finished using this session.");
            lock.lock();
        }
        try {
            if (isAlive()) {
                closed = new Exception();
                return true;
            } else {
                log.warn("This session has already been closed", closed);
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

}
