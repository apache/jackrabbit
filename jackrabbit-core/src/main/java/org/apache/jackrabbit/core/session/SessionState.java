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

import static org.apache.jackrabbit.api.stats.RepositoryStatistics.Type;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.core.WorkspaceManager;
import org.apache.jackrabbit.core.observation.ObservationDispatcher;
import org.apache.jackrabbit.stats.RepositoryStatisticsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Component context of this session.
     */
    private final SessionContext context;

    /**
     * Counter of read operations.
     */
    private final AtomicLong readCounter;

    /**
     * Counter of write operations.
     */
    private final AtomicLong writeCounter;

    /**
     * Duration of read operations.
     */
    private final AtomicLong readDuration;

    /**
     * Duration of write operations.
     */
    private final AtomicLong writeDuration;
    
    /**
     * Number of open sessions.
     */
    private final AtomicLong sessionCount;

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

        RepositoryStatisticsImpl statistics =
                context.getRepositoryContext().getRepositoryStatistics();
        this.readCounter = statistics.getCounter(Type.SESSION_READ_COUNTER);
        this.writeCounter = statistics.getCounter(Type.SESSION_WRITE_COUNTER);
        this.readDuration = statistics.getCounter(Type.SESSION_READ_DURATION);
        this.writeDuration = statistics.getCounter(Type.SESSION_WRITE_DURATION);
        this.sessionCount = statistics.getCounter(Type.SESSION_COUNT);
        statistics.getCounter(Type.SESSION_LOGIN_COUNTER).incrementAndGet();
        sessionCount.incrementAndGet();
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
     * Performs the given operation within a synchronized block. Special care
     * is made to detect attempts to access the session concurrently and to
     * log detailed warnings in such cases.
     *
     * @param operation session operation
     * @return the return value of the executed operation
     * @throws RepositoryException if the operation fails or
     *                             if the session has already been closed
     */
    public <T> T perform(SessionOperation<T> operation)
            throws RepositoryException {
        String session = context.getSessionImpl().toString();

        // Acquire the exclusive lock for accessing session internals.
        // No other session should be holding the lock, so we log a
        // message to let the user know of such cases.
        if (!lock.tryLock()) {
            if (isWriteOperation
                    && operation instanceof SessionWriteOperation) {
                Exception trace = new Exception(
                        "Stack trace of concurrent access to " + session);
                log.warn("Attempt to perform " + operation
                        + " while another thread is concurrently writing"
                        + " to " + session + ". Blocking until the other"
                        + " thread is finished using this session. Please"
                        + " review your code to avoid concurrent use of"
                        + " a session.", trace);
            } else if (log.isDebugEnabled()) {
                Exception trace = new Exception(
                        "Stack trace of concurrent access to " + session);
                log.debug("Attempt to perform " + operation + " while"
                        + " another thread is concurrently reading from "
                        + session + ". Blocking until the other thread"
                        + " is finished using this session. Please"
                        + " review your code to avoid concurrent use of"
                        + " a session.", trace);
            }
            lock.lock();
        }

        boolean isOutermostWriteOperation = false;
        try {
            // Check that the session is still alive
            checkAlive();

            // Raise the isWriteOperation flag for write operations.
            // The flag is used to set the appropriate log level above.
            boolean wasWriteOperation = isWriteOperation;
            if (!wasWriteOperation
                    && operation instanceof SessionWriteOperation) {
                isWriteOperation = true;
                isOutermostWriteOperation = true;
            }

            try {
                // Perform the actual operation
                log.debug("Performing {}", operation);
                long start = System.nanoTime();
                try {
                    return operation.perform(context);
                } finally {
                    long time = System.nanoTime() - start;

                    // JCR-3040: Increment the operation counters
                    if (isWriteOperation) {
                        writeCounter.incrementAndGet();
                        writeDuration.addAndGet(time);
                    } else {
                        readCounter.incrementAndGet();
                        readDuration.addAndGet(time);
                    }

                    log.debug("Performed {} in {}ns", operation, time);
                }
            } finally {
                isWriteOperation = wasWriteOperation;
            }
        } finally {
            lock.unlock();

            // Delay return from a write operation if the observation queue
            // is being overloaded. This needs to be done after releasing
            // the (outermost) write locks to prevent potential deadlocks.
            // See https://issues.apache.org/jira/browse/JCR-2746
            if (isOutermostWriteOperation) {
                WorkspaceManager manager =
                    context.getRepositoryContext().getWorkspaceManager();
                ObservationDispatcher dispatcher =
                    manager.getObservationDispatcher(context.getWorkspace().getName());
                dispatcher.delayIfEventQueueOverloaded();
            }
        }
    }

    /**
     * Closes this session.  Special care is made to detect attempts to
     * access the session concurrently or to close it more than once, and to
     * log detailed warnings in such cases.
     *
     * @return <code>true</code> if the session was closed, or
     *         <code>false</code> if the session had already been closed
     */
    public boolean close() {
        String session = context.getSessionImpl().toString();

        if (!lock.tryLock()) {
            Exception trace = new Exception(
                    "Stack trace of concurrent access to " + session);
            log.warn("Attempt to close " + session + " while another"
                    + " thread is concurrently accessing this session."
                    + " Blocking until the other thread is finished"
                    + " using this session. Please review your code"
                    + " to avoid concurrent use of a session.", trace);
            lock.lock();
        }
        try {
            if (isAlive()) {
                sessionCount.decrementAndGet();
                closed = new Exception(
                        "Stack trace of  where " + session
                        + " was originally closed");
                return true;
            } else {
                Exception trace = new Exception(
                        "Stack trace of the duplicate attempt to close "
                        + session);
                log.warn("Attempt to close " + session + " after it has"
                        + " already been closed. Please review your code"
                        + " for proper session management.", trace);
                log.warn(session + " has already been closed. See the"
                        + " attached exception for a trace of where this"
                        + " session was closed.", closed);
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

}
