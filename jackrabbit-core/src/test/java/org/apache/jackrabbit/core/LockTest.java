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

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.util.Locked;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.Property;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * <code>LockTest</code> tests the utility
 * {@link org.apache.jackrabbit.util.Locked}.
 */
public class LockTest extends AbstractJCRTest {

    private static final int NUM_THREADS = 100;

    private static final int NUM_CHANGES = 10;

    private static final int NUM_VALUE_GETS = 10;

    /**
     * Tests the utility {@link org.apache.jackrabbit.util.Locked} by
     * implementing running multiple threads concurrently that apply changes to
     * a lockable node.
     */
    public void testLockUtility() throws RepositoryException {
        final Node lockable = testRootNode.addNode(nodeName1);
        lockable.addMixin(mixLockable);
        superuser.save();

        final List<Thread> worker = new ArrayList<Thread>();
        for (int i = 0; i < NUM_THREADS; i++) {
            worker.add(new Thread() {

                private final int threadNumber = worker.size();

                public void run() {
                    final Session s;
                    try {
                        s = getHelper().getSuperuserSession();
                    } catch (RepositoryException e) {
                        fail(e.getMessage());
                        return;
                    }
                    try {
                        for (int i = 0; i < NUM_CHANGES; i++) {
                            Node n = (Node) s.getItem(lockable.getPath());
                            new Locked() {
                                protected Object run(Node n)
                                        throws RepositoryException {
                                    String nodeName = "node" + threadNumber;
                                    if (n.hasNode(nodeName)) {
                                        n.getNode(nodeName).remove();
                                    } else {
                                        n.addNode(nodeName);
                                    }
                                    s.save();
                                    log.println("Thread" + threadNumber
                                            + ": saved modification");

                                    return null;
                                }
                            }.with(n, false);
                            // do a random wait
                            Thread.sleep(new Random().nextInt(100));
                        }
                    } catch (RepositoryException e) {
                        log.println("exception while running code with lock:"
                                + e.getMessage());
                    } catch (InterruptedException e) {
                        log.println(Thread.currentThread()
                                + " interrupted while waiting for lock");
                    } finally {
                        s.logout();
                    }
                }
            });
        }

        for (Iterator<Thread> it = worker.iterator(); it.hasNext();) {
            it.next().start();
        }

        for (Iterator<Thread> it = worker.iterator(); it.hasNext();) {
            try {
                it.next().join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Tests the utility {@link org.apache.jackrabbit.util.Locked} by
     * implementing a persistent counter.
     */
    public void testSequence() throws RepositoryException {
        final Node counter = testRootNode.addNode(nodeName1);
        counter.setProperty("value", 0);
        counter.addMixin(mixLockable);
        superuser.save();

        final List<Thread> worker = new ArrayList<Thread>();
        for (int i = 0; i < NUM_THREADS; i++) {
            worker.add(new Thread() {

                private final int threadNumber = worker.size();

                public void run() {
                    final Session s;
                    try {
                        s = getHelper().getSuperuserSession();
                    } catch (RepositoryException e) {
                        fail(e.getMessage());
                        return;
                    }
                    try {
                        for (int i = 0; i < NUM_VALUE_GETS; i++) {
                            Node n = (Node) s.getItem(counter.getPath());
                            long currentValue = ((Long) new Locked() {
                                protected Object run(Node n)
                                        throws RepositoryException {
                                    Property seqProp = n.getProperty("value");
                                    long value = seqProp.getLong();
                                    seqProp.setValue(++value);
                                    s.save();
                                    return new Long(value);
                                }
                            }.with(n, false)).longValue();
                            log.println("Thread" + threadNumber
                                    + ": got sequence number: " + currentValue);
                            // do a random wait
                            Thread.sleep(new Random().nextInt(100));
                        }
                    } catch (RepositoryException e) {
                        log.println("exception while running code with lock:"
                                + e.getMessage());
                    } catch (InterruptedException e) {
                        log.println(Thread.currentThread()
                                + " interrupted while waiting for lock");
                    } finally {
                        s.logout();
                    }
                }
            });
        }

        for (Iterator<Thread> it = worker.iterator(); it.hasNext();) {
            it.next().start();
        }

        for (Iterator<Thread> it = worker.iterator(); it.hasNext();) {
            try {
                it.next().join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Tests the utility {@link org.apache.jackrabbit.util.Locked} by
     * implementing a persistent counter with a timeout when the next value of
     * the counter is retrieved. The number of values that can be retrieved by
     * this test depends on system performance and the configured persistence
     * manager.
     */
    public void testSequenceWithTimeout() throws RepositoryException {
        final Node counter = testRootNode.addNode(nodeName1);
        counter.setProperty("value", 0);
        counter.addMixin(mixLockable);
        superuser.save();

        final List<Thread> worker = new ArrayList<Thread>();
        for (int i = 0; i < NUM_THREADS; i++) {
            worker.add(new Thread() {

                private final int threadNumber = worker.size();

                public void run() {
                    final Session s;
                    try {
                        s = getHelper().getSuperuserSession();
                    } catch (RepositoryException e) {
                        fail(e.getMessage());
                        return;
                    }
                    try {
                        for (int i = 0; i < NUM_VALUE_GETS; i++) {
                            Node n = (Node) s.getItem(counter.getPath());
                            Object ret = new Locked() {
                                protected Object run(Node n)
                                        throws RepositoryException {
                                    Property seqProp = n.getProperty("value");
                                    long value = seqProp.getLong();
                                    seqProp.setValue(++value);
                                    s.save();
                                    return new Long(value);
                                }
                            }.with(n, false, 10 * 1000); // expect a value after
                                                         // ten seconds
                            if (ret == Locked.TIMED_OUT) {
                                log.println("Thread"
                                        + threadNumber
                                        + ": could not get a sequence number within 10 seconds");
                            } else {
                                long currentValue = ((Long) ret).longValue();
                                log.println("Thread" + threadNumber
                                        + ": got sequence number: "
                                        + currentValue);
                            }
                            // do a random wait
                            Thread.sleep(new Random().nextInt(100));
                        }
                    } catch (RepositoryException e) {
                        log.println("exception while running code with lock:"
                                + e.getMessage());
                    } catch (InterruptedException e) {
                        log.println(Thread.currentThread()
                                + " interrupted while waiting for lock");
                    } finally {
                        s.logout();
                    }
                }
            });
        }

        for (Iterator<Thread> it = worker.iterator(); it.hasNext();) {
            it.next().start();
        }

        for (Iterator<Thread> it = worker.iterator(); it.hasNext();) {
            try {
                it.next().join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
