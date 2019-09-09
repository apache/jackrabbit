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

import org.apache.jackrabbit.core.state.StaleItemStateException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.InvalidItemStateException;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;

/**
 * <code>ConcurrentCheckinMixedTransactionTest</code> performs concurrent
 * version operations with only some threads using XATransactions.
 */
public class ConcurrentCheckinMixedTransactionTest
        extends AbstractConcurrencyTest {

    private static final int NUM_THREADS = 10;

    private static final int RUN_NUM_SECONDS = 20;

    public void testCheckInOut() throws RepositoryException {
        final long end = System.currentTimeMillis() + RUN_NUM_SECONDS * 1000;
        // tasks with even ids run within transactions
        final int[] taskId = new int[1];
        runTask(new Task() {
            public void execute(Session session, Node test)
                    throws RepositoryException {
                int id;
                synchronized (ConcurrentCheckinMixedTransactionTest.this) {
                    id = taskId[0]++;
                }
                int i = 0;
                while (end > System.currentTimeMillis()) {
                    UserTransactionImpl uTx = null;
                    try {
                        if (id % 2 == 0) {
                            uTx = new UserTransactionImpl(session);
                            uTx.begin();
                        }
                        Node n = test.addNode("node" + i++);
                        n.addMixin(mixVersionable);
                        session.save();
                        n.checkout();
                        n.checkin();
                        if (uTx != null) {
                            uTx.commit();
                        }
                    } catch (NotSupportedException e) {
                        throw new RepositoryException(e);
                    } catch (SystemException e) {
                        throw new RepositoryException(e);
                    } catch (HeuristicMixedException e) {
                        throw new RepositoryException(e);
                    } catch (HeuristicRollbackException e) {
                        throw new RepositoryException(e);
                    } catch (RollbackException e) {
                        Throwable t = e;
                        do {
                            t = t.getCause();
                            if (t instanceof StaleItemStateException) {
                                break;
                            }
                        } while (t != null);
                        if (t == null) {
                            throw new RepositoryException(e);
                        }
                    } catch (InvalidItemStateException e) {
                        // try again
                    }
                }
            }
        }, NUM_THREADS);
    }
}
