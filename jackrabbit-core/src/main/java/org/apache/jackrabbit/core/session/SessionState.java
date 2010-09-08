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
import org.slf4j.MDC;

/**
 * Internal session state. This class keeps track of the lifecycle of
 * a session and controls concurrent access to the session internals.
 * <p>
 * The session lifecycle is pretty simple: there are only two lifecycle
 * states, "alive" and "closed", and only one possible state transition,
 * from "alive" to "closed".
 * <p>
 * Concurrent access to session internals is controlled by the
 * {@link #perform(SessionOperation)} method that guarantees that no two
 * {@link SessionOperation operations} are performed concurrently on the
 * same session.
 *
 * @see <a href="https://issues.apache.org/jira/browse/JCR-890">JCR-890</a>
 */
public class SessionState {

    /**
     * Logger instance.
     */
    private static final Logger log =
        LoggerFactory.getLogger(SessionState.class);

    /**
     * Number of nanoseconds in a millisecond.
     */
    private static final int NS_PER_MS = 1000000;

    /**
     * Component context of this session.
     */
    private final SessionContext context;

    /**
     * The lock used to guarantee synchronized execution of repository
     * operations. An explicit lock is used instead of normal Java
     * synchronization in order to be able to log attempts to concurrently
     * use a session.
     */
    private final Lock lock = new ReentrantLock();

    /**
     * Flag to indicate that the current operation is a write operation.
     * Used to detect concurrent writes.
     */
    private volatile boolean isWriteOperation = false;

    /**
     * Flag to indicate a closed session. When <code>null</code>, the session
     * is still alive. And when the session is closed, this reference is set
     * to an exception that contains the stack trace of where the session was
     * closed. The stack trace is used as extra information in possible
     * warning logs caused by clients attempting to access a closed session.
     */
    private volatile Exception closed = null;

    /**
     * Creates a state instance for a session.
     *
     * @param context component context of this session
     */
    public SessionState(SessionContext context) {
        this.context = context;
    }

    /**
     * Checks whether this session is alive. This method should generally
     * only be called from within a performed {@link SessionOperation}, as
     * otherwise there's no guarantee against another thread closing the
     * session right after this method has returned.
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
                    + " for a trace of where the session was closed.", closed);
        }
    }

    /**
     * Performs the given operation within a synchronized block.
     *
     * @param operation session operation
     * @return the return value of the executed operation
     * @throws RepositoryException if the operation fails or
     *                             if the session has already been closed
     */
    public <T> T perform(SessionOperation<T> operation)
            throws RepositoryException {
        // Set MDC variables to reflect the current session and this operation
        LogState logState =
            new LogState("jcr.session", context, "jcr.operation", operation);
        try {
            // Acquire the exclusive lock for accessing session internals.
            // No other session should be holding the lock, so we log a
            // message to let the user know of such cases.
            if (!lock.tryLock()) {
                if (isWriteOperation
                        && operation instanceof SessionWriteOperation) {
                    log.warn("Attempt to perform {} while another thread"
                            + " is concurrently modifying the session."
                            + " Blocking until the other thread is finished"
                            + " using this session.", operation);
                } else {
                    log.debug("Attempt to perform {} while another thread"
                            + " is concurrently accessing the session."
                            + " Blocking until the other thread is finished"
                            + " using this session.", operation);
                }
                lock.lock();
            }

            try {
                // Check that the session is still alive
                checkAlive();

                // Raise the isWriteOperation flag for write operations.
                // The flag is used to set the appropriate log level above.
                boolean wasWriteOperation = isWriteOperation;
                if (!wasWriteOperation
                        && operation instanceof SessionWriteOperation) {
                    isWriteOperation = true;
                }
                try {
                    // Perform the actual operation, optionally with debug logs
                    if (log.isDebugEnabled()) {
                        log.debug("Performing {}", operation);
                        long start = System.nanoTime();
                        try {
                            return operation.perform(context);
                        } finally {
                            log.debug("It took {}ms to perform {}",
                                    (System.nanoTime() - start) / NS_PER_MS,
                                    operation);
                        }
                    } else {
                        return operation.perform(context);
                    }
                } finally {
                    isWriteOperation = wasWriteOperation;
                }
            } finally {
                lock.unlock();
            }
        } finally {
            logState.reset();
        }
    }

    /**
     * Closes this session.
     *
     * @return <code>true</code> if the session was closed, or
     *         <code>false</code> if the session had already been closed
     */
    public boolean close() {
        LogState logState =
            new LogState("jcr.session", context, "jcr.operation", "close()");
        try {
            if (!lock.tryLock()) {
                log.warn("Attempt to close a session while another thread is"
                        + " concurrently accessing the session. Blocking until"
                        + " the other thread is finished using this session.");
                lock.lock();
            }
            try {
                if (isAlive()) {
                    closed = new Exception(
                            "Stack trace of where " + context + " was closed");
                    return true;
                } else {
                    log.warn("This session has already been closed. See the"
                            + " chained exception for a trace of where the "
                            + " session was closed.", closed);
                    return false;
                }
            } finally {
                lock.unlock();
            }
        } finally {
            logState.reset();
        }
    }

    /**
     * Internal utility class for setting MDC variables during the execution
     * of a session operation.
     */
    private static class LogState {

        private final Object[] keyValuePairs;

        public LogState(Object... keyValuePairs) {
            this.keyValuePairs = keyValuePairs;
            for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
                String key = keyValuePairs[i].toString();
                if (MDC.get(key) == null) {
                    MDC.put(key, keyValuePairs[i + 1].toString());
                } else {
                    keyValuePairs[i + 1] = null;
                }
            }
        }

        public void reset() {
            for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
                if (keyValuePairs[i + 1] != null) {
                    MDC.remove(keyValuePairs[i].toString());
                }
            }
        }

    }

}
