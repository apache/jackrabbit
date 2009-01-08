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

import org.apache.jackrabbit.core.AbstractConcurrencyTest;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.lock.Lock;

/**
 * <code>ConcurrentLockingTest</code> contains test cases that run lock
 * operations with concurrent threads.
 */
public class ConcurrentLockingTest extends AbstractConcurrencyTest {

    /**
     * The number of threads.
     */
    private static final int CONCURRENCY = 10;

    /**
     * The total number of operations to execute. E.g. number of lock
     * performed by the threads.
     */
    private static final int NUM_OPERATIONS = 200;

    public void testConcurrentLockUnlock() throws RepositoryException {
        runTask(new Task() {
            public void execute(Session session, Node test) throws RepositoryException {
                Node n = test.addNode("test");
                n.addMixin(mixLockable);
                session.save();

                for (int i = 0; i < NUM_OPERATIONS / CONCURRENCY; i++) {
                    n.lock(false, true);
                    n.unlock();
                }
            }
        }, CONCURRENCY);
    }

    public void testConcurrentCreateAndLockUnlock() throws RepositoryException {
        runTask(new Task() {
            public void execute(Session session, Node test) throws RepositoryException {
                // add versionable nodes
                for (int i = 0; i < NUM_OPERATIONS / CONCURRENCY; i++) {
                    Node n = test.addNode("test" + i);
                    n.addMixin(mixLockable);
                    session.save();
                    Lock l = n.lock(false, true);
                    l.refresh();
                    n.unlock();
                }
            }
        }, CONCURRENCY);
    }

    public void testConcurrentLock() throws RepositoryException {
        runTask(new Task() {
            public void execute(Session session, Node test) throws RepositoryException {
                Node n = test.addNode("test");
                n.addMixin(mixLockable);
                session.save();

                for (int i = 0; i < NUM_OPERATIONS / CONCURRENCY; i++) {
                    if (n.isLocked()) {
                        n.unlock();
                    }
                    n.lock(false, true);
                }
            }
        }, CONCURRENCY);
    }
}
