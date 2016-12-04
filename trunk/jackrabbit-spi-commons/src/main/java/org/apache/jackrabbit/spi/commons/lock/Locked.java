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
package org.apache.jackrabbit.spi.commons.lock;

import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameException;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Session;
import javax.jcr.Repository;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.Event;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

/**
 * <code>Locked</code> is a utility to synchronize modifications on a lockable
 * node. The modification is applied while the lock on the node is held, thus
 * ensuring that the modification will never fail with an {@link
 * javax.jcr.InvalidItemStateException}. This utility can be used with any
 * JCR Repository, not just Jackrabbit.
 * <p>
 * The following example shows how this utility can be used to implement
 * a persistent counter:
 * <pre>
 * Node counter = ...;
 * long nextValue = ((Long) new Locked() {
 *     protected Object run(Node counter) throws RepositoryException {
 *         Property seqProp = counter.getProperty("value");
 *         long value = seqProp.getLong();
 *         seqProp.setValue(++value);
 *         seqProp.save();
 *         return new Long(value);
 *     }
 * }.with(counter, false)).longValue();
 * </pre>
 * If you specify a <code>timeout</code> you need to check the return value
 * whether the <code>run</code> method could be executed within the timeout
 * period:
 * <pre>
 * Node counter = ...;
 * Object ret = new Locked() {
 *     protected Object run(Node counter) throws RepositoryException {
 *         Property seqProp = counter.getProperty("value");
 *         long value = seqProp.getLong();
 *         seqProp.setValue(++value);
 *         seqProp.save();
 *         return new Long(value);
 *     }
 * }.with(counter, false);
 * if (ret == Locked.TIMED_OUT) {
 *     // do whatever you think is appropriate in this case
 * } else {
 *     // get the value
 *     long nextValue = ((Long) ret).longValue();
 * }
 * </pre>
 *
 * @deprecated Use org.apache.jackrabbit.util.Locked instead.
 */
public abstract class Locked {

    /**
     * Object returned when timeout is reached without being able to call
     * {@link #run} while holding the lock.
     */
    public static final Object TIMED_OUT = new Object();

    /**
     * Executes {@link #run} while the lock on <code>lockable</code> is held.
     * This method will block until {@link #run} is executed while holding the
     * lock on node <code>lockable</code>.
     *
     * @param lockable a lockable node.
     * @param isDeep   <code>true</code> if <code>lockable</code> will be locked
     *                 deep.
     * @return the object returned by {@link #run}.
     * @throws IllegalArgumentException if <code>lockable</code> is not
     *      <i>mix:lockable</i>.
     * @throws RepositoryException  if {@link #run} throws an exception.
     * @throws InterruptedException if this thread is interrupted while waiting
     *                              for the lock on node <code>lockable</code>.
     */
    public Object with(Node lockable, boolean isDeep)
            throws RepositoryException, InterruptedException {
        return with(lockable, isDeep, Long.MAX_VALUE);
    }

    /**
     * Executes the method {@link #run} within the scope of a lock held on
     * <code>lockable</code>.
     *
     * @param lockable the node where the lock is obtained from.
     * @param isDeep   <code>true</code> if <code>lockable</code> will be locked
     *                 deep.
     * @param timeout  time in milliseconds to wait at most to aquire the lock.
     * @return the object returned by {@link #run} or {@link #TIMED_OUT} if the
     *         lock on <code>lockable</code> could not be aquired within the
     *         specified timeout.
     * @throws IllegalArgumentException if <code>timeout</code> is negative or
     *                                  <code>lockable</code> is not
     *                                  <i>mix:lockable</i>.
     * @throws RepositoryException      if {@link #run} throws an exception.
     * @throws UnsupportedRepositoryOperationException
     *                                  if this repository does not support
     *                                  locking.
     * @throws InterruptedException     if this thread is interrupted while
     *                                  waiting for the lock on node
     *                                  <code>lockable</code>.
     */
    public Object with(Node lockable, boolean isDeep, long timeout)
            throws UnsupportedRepositoryOperationException, RepositoryException, InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must be >= 0");
        }

        Session session = lockable.getSession();
        NamePathResolver resolver = new DefaultNamePathResolver(session);

        Lock lock;
        EventListener listener = null;
        try {
            // check whether the lockable can be locked at all
            if (!lockable.isNodeType(resolver.getJCRName(NameConstants.MIX_LOCKABLE))) {
                throw new IllegalArgumentException("Node is not lockable");
            }

            lock = tryLock(lockable, isDeep);
            if (lock != null) {
                return runAndUnlock(lock);
            }

            if (timeout == 0) {
                return TIMED_OUT;
            }

            long timelimit;
            if (timeout == Long.MAX_VALUE) {
                timelimit = Long.MAX_VALUE;
            } else {
                timelimit = System.currentTimeMillis() + timeout;
            }

            // node is locked by other session -> register event listener if possible
            if (isObservationSupported(session)) {
                ObservationManager om = session.getWorkspace().getObservationManager();
                listener = new EventListener() {
                    public void onEvent(EventIterator events) {
                        synchronized (this) {
                            this.notify();
                        }
                    }
                };
                om.addEventListener(listener, Event.PROPERTY_REMOVED,
                        lockable.getPath(), false, null, null, true);
            }

            // now keep trying to aquire the lock
            // using 'this' as a monitor allows the event listener to notify
            // the current thread when the lockable node is possibly unlocked
            for (; ;) {
                synchronized (this) {
                    lock = tryLock(lockable, isDeep);
                    if (lock != null) {
                        return runAndUnlock(lock);
                    } else {
                        // check timeout
                        if (System.currentTimeMillis() > timelimit) {
                            return TIMED_OUT;
                        }
                        if (listener != null) {
                            // event listener *should* wake us up, however
                            // there is a chance that removal of the lockOwner
                            // property is notified before the node is acutally
                            // unlocked. therefore we use a safety net to wait
                            // at most 1000 millis.
                            this.wait(Math.min(1000, timeout));
                        } else {
                            // repository does not support observation
                            // wait at most 50 millis then retry
                            this.wait(Math.min(50, timeout));
                        }
                    }
                }
            }
        } catch (NameException e) {
            throw new RepositoryException(e);
        } finally {
            if (listener != null) {
                session.getWorkspace().getObservationManager().removeEventListener(listener);
            }
        }
    }

    /**
     * This method is executed while holding the lock.
     * @param node The <code>Node</code> on which the lock is placed.
     * @return an object which is then returned by {@link #with with()}.
     * @throws RepositoryException if an error occurs.
     */
    protected abstract Object run(Node node) throws RepositoryException;

    /**
     * Executes {@link #run} and unlocks the lockable node in any case, even
     * when an exception is thrown.
     *
     * @param lock The <code>Lock</code> to unlock in any case before returning.
     *
     * @return the object returned by {@link #run}.
     * @throws RepositoryException if an error occurs.
     */
    private Object runAndUnlock(Lock lock) throws RepositoryException {
        try {
            return run(lock.getNode());
        } finally {
            lock.getNode().unlock();
        }
    }

    /**
     * Tries to aquire a session scoped lock on <code>lockable</code>.
     *
     * @param lockable the lockable node
     * @param isDeep   <code>true</code> if the lock should be deep
     * @return The <code>Lock</code> or <code>null</code> if the
     *         <code>lockable</code> cannot be locked.
     * @throws UnsupportedRepositoryOperationException
     *                             if this repository does not support locking.
     * @throws RepositoryException if an error occurs
     */
    private static Lock tryLock(Node lockable, boolean isDeep)
            throws UnsupportedRepositoryOperationException, RepositoryException {
        try {
            return lockable.lock(isDeep, true);
        } catch (LockException e) {
            // locked by some other session
        }
        return null;
    }

    /**
     * Returns <code>true</code> if the repository supports observation.
     *
     * @param s a session of the repository.
     * @return <code>true</code> if the repository supports observation.
     */
    private static boolean isObservationSupported(Session s) {
        return "true".equalsIgnoreCase(s.getRepository().getDescriptor(Repository.OPTION_OBSERVATION_SUPPORTED));
    }

}
