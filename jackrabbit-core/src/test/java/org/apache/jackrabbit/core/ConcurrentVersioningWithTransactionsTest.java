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
package org.apache.jackrabbit.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.InvalidItemStateException;
import javax.jcr.version.Version;
import javax.transaction.UserTransaction;

import org.apache.jackrabbit.core.state.StaleItemStateException;

/**
 * <code>ConcurrentVersioningTest</code> contains test cases that run version
 * operations with concurrent threads.
 */
public class ConcurrentVersioningWithTransactionsTest extends AbstractConcurrencyTest {

    /**
     * The number of threads.
     */
    private static final int CONCURRENCY = 100;

    /**
     * The total number of operations to execute. E.g. number of checkins
     * performed by the threads.
     */
    private static final int NUM_OPERATIONS = 100;

    /**
     * Creates the named test node. The node is created within a transaction
     * to avoid mixing transactional and non-transactional writes within a
     * concurrent test run.
     */
    private static synchronized Node createParentNode(Node test, String name)
            throws RepositoryException {
        try {
            UserTransaction utx = new UserTransactionImpl(test.getSession());
            utx.begin();
            Node parent = test.addNode(name);
            test.save();
            utx.commit();
            return parent;
        } catch (RepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new RepositoryException("Failed to add node: " + name, e);
        }
    }

    public void testConcurrentAddVersionableInTransaction()
            throws RepositoryException {
        runTask(new Task() {
            public void execute(Session session, Node test)
                    throws RepositoryException {
                // add versionable nodes
                final String threadName = Thread.currentThread().getName();
                Node parent = createParentNode(test, threadName);
                for (int i = 0; i < NUM_OPERATIONS / CONCURRENCY; i++) {
                    try {
                        final UserTransaction utx = new UserTransactionImpl(test.getSession());
                        utx.begin();
                        final String nodeName = "test" + i;
                        Node n = parent.addNode(nodeName);
                        n.addMixin(mixVersionable);
                        session.save();
                        utx.commit();
                    } catch (InvalidItemStateException e) {
                        // ignore
                    } catch (Exception e) {
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

    public void testConcurrentCheckinInTransaction()
            throws RepositoryException {
        runTask(new Task() {
            public void execute(Session session, Node test)
                    throws RepositoryException {
                int i = 0;
                try {
                    Node n = test.addNode("test");
                    n.addMixin(mixVersionable);
                    session.save();
                    for (i = 0; i < NUM_OPERATIONS / CONCURRENCY; i++) {
                        final UserTransaction utx = new UserTransactionImpl(test.getSession());
                        utx.begin();
                        n.checkout();
                        n.checkin();
                        utx.commit();
                    }
                    n.checkout();
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

    public void testConcurrentCreateAndCheckinCheckoutInTransaction()
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
                        n.addMixin(mixVersionable);
                        session.save();
                        n.checkout();
                        n.checkin();
                        n.checkout();
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

    public void testConcurrentRestoreInTransaction()
            throws RepositoryException {
        runTask(new Task() {
            public void execute(Session session, Node test)
                    throws RepositoryException {
                int i = 0;
                try {
                    Node n = test.addNode("test");
                    n.addMixin(mixVersionable);
                    session.save();
                    // create 3 version
                    List versions = new ArrayList();
                    for (i = 0; i < 3; i++) {
                        n.checkout();
                        versions.add(n.checkin());
                    }
                    // do random restores
                    Random rand = new Random();
                    for (i = 0; i < NUM_OPERATIONS / CONCURRENCY; i++) {
                        Version v = (Version) versions.get(rand.nextInt(versions.size()));
                        n.restore(v, true);
                    }
                    n.checkout();
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
