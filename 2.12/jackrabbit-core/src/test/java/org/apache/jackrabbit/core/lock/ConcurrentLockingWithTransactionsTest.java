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
package org.apache.jackrabbit.core.lock;

import org.apache.jackrabbit.core.state.StaleItemStateException;
import org.apache.jackrabbit.core.AbstractConcurrencyTest;
import org.apache.jackrabbit.core.UserTransactionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.transaction.UserTransaction;

/** <code>ConcurrentLockingWithTransactionsTest</code>... */
public class ConcurrentLockingWithTransactionsTest extends AbstractConcurrencyTest {

    private static Logger log = LoggerFactory.getLogger(ConcurrentLockingWithTransactionsTest.class);

    /**
     * The number of threads.
     */
    private static final int CONCURRENCY = 100;

    /**
     * The total number of operations to execute. E.g. number of locking operations
     * performed by the threads.
     */
    private static final int NUM_OPERATIONS = 100;

    public void testConcurrentRefreshInTransaction()
            throws RepositoryException {
        runTask(new Task() {
            public void execute(Session session, Node test)
                    throws RepositoryException {
                int i = 0;
                try {
                    Node n = test.addNode("test");
                    n.addMixin(mixLockable);
                    session.save();
                    for (i = 0; i < NUM_OPERATIONS / CONCURRENCY; i++) {
                        Lock lock = n.lock(false, true);

                        final UserTransaction utx = new UserTransactionImpl(test.getSession());
                        utx.begin();
                        lock.refresh();
                        utx.commit();

                        n.unlock();
                    }
                } catch (Exception e) {
                    final String threadName = Thread.currentThread().getName();
                    final Throwable deepCause = getLevel2Cause(e);
                    if (deepCause != null && deepCause instanceof StaleItemStateException) {
                        // ignore
                    } else {
                        throw new RepositoryException(threadName + ", i=" + i + ":" + e.getClass().getName(), e);
                    }
                }
            }
        }, CONCURRENCY);
    }

    public void testConcurrentCreateAndLockUnLockInTransaction()
            throws RepositoryException {
        runTask(new Task() {
            public void execute(Session session, Node test)
                    throws RepositoryException {
                // add versionable nodes
                for (int i = 0; i < NUM_OPERATIONS / CONCURRENCY; i++) {
                    try {
                        final UserTransaction utx = new UserTransactionImpl(test.getSession());
                        utx.begin();
                        Node n = test.addNode("test" + i);
                        n.addMixin(mixLockable);
                        session.save();
                        Lock l = n.lock(false, true);
                        n.unlock();
                        utx.commit();
                    } catch (Exception e) {
                        final String threadName = Thread.currentThread().getName();
                        final Throwable deepCause = getLevel2Cause(e);
                        if (deepCause != null && deepCause instanceof StaleItemStateException) {
                            // ignore
                        } else {
                            throw new RepositoryException(threadName + ", i=" + i + ":" + e.getClass().getName(), e);
                        }
                    }
                }
            }
        }, CONCURRENCY);
    }

    private static Throwable getLevel2Cause(Throwable t) {
        Throwable result = null;
        try {
            result = t.getCause().getCause();
        } catch (NullPointerException npe) {
            // ignore, we have no deep cause
        }
        return result;

    }
}