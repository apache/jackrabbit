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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActiveSessionState implements SessionState {

    /**
     * Logger instance.
     */
    private static final Logger log =
        LoggerFactory.getLogger(ActiveSessionState.class);

    /**
     * The lock used to guarantee synchronized execution of repository
     * operations. An explicit lock is used instead of normal Java
     * synchronization in order to be able to log attempts to concurrently
     * use a session. TODO: Check if this is a performance issue!
     */
    private final Lock lock = new ReentrantLock();

    /**
     * Returns <code>true</code>; the session is alive.
     *
     * @return <code>true</code>
     */
    public boolean isAlive() {
        return true;
    }

    /**
     * Performs the given operation within a synchronized block.
     *
     * @throws RepositoryException if the operation fails
     */
    public void perform(SessionOperation operation) throws RepositoryException {
        if (!lock.tryLock()) {
            log.warn("Attempt to perform a {} operation while another thread"
                    + " is concurrently accessing the session", operation);
            lock.lock();
        }
        try {
            operation.perform();
        } finally {
            lock.unlock();
        }
    }

}
