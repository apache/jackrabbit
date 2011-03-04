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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.util.LockedWrapper;

/**
 * tests for the utility class {@link org.apache.jackrabbit.util.LockedWrapper}.
 */
public class LockedWrapperTest extends AbstractJCRTest {

    private static final int NUM_THREADS = 100;

    private static final int NUM_CHANGES = 10;

    private static final int NUM_VALUE_GETS = 10;

    private final Random random = new Random();

    private AtomicInteger counter = new AtomicInteger(0);

    /**
     * Tests the utility {@link org.apache.jackrabbit.util.Locked} by
     * implementing running multiple threads concurrently that apply changes to
     * a lockable node.
     */
    public void testConcurrentUpdates() throws RepositoryException,
            InterruptedException {

        final Node lockable = testRootNode.addNode(nodeName1);
        lockable.addMixin(mixLockable);
        superuser.save();

        List<Future<?>> futures = new ArrayList<Future<?>>();
        ExecutorService e = Executors.newFixedThreadPool(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            futures.add(e.submit(buildNewConcurrentUpdateCallable(i,
                    lockable.getPath(), false)));
        }
        e.shutdown();
        assertTrue(e.awaitTermination(10 * 60, TimeUnit.SECONDS));

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException ex) {
                fail(ex.getMessage());
            }
        }
    }

    /**
     * Tests the utility {@link org.apache.jackrabbit.util.Locked} by
     * implementing running multiple threads concurrently that apply changes to
     * a lockable node, also refreshing the session on each operation
     */
    public void testConcurrentUpdatesWithSessionRefresh()
            throws RepositoryException, InterruptedException {

        final Node lockable = testRootNode.addNode(nodeName1);
        lockable.addMixin(mixLockable);
        superuser.save();

        List<Future<?>> futures = new ArrayList<Future<?>>();
        ExecutorService e = Executors.newFixedThreadPool(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            futures.add(e.submit(buildNewConcurrentUpdateCallable(i,
                    lockable.getPath(), true)));
        }
        e.shutdown();
        assertTrue(e.awaitTermination(10 * 60, TimeUnit.SECONDS));

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException ex) {
                fail(ex.getMessage());
            }
        }
    }

    public Callable<Void> buildNewConcurrentUpdateCallable(
            final int threadNumber, final String lockablePath,
            final boolean withSessionRefresh) {

        return new Callable<Void>() {

            public Void call() throws RepositoryException, InterruptedException {

                final Session s = getHelper().getSuperuserSession();

                try {

                    for (int i = 0; i < NUM_CHANGES; i++) {

                        if (withSessionRefresh) {
                            s.refresh(false);
                        }
                        Node n = s.getNode(lockablePath);

                        new LockedWrapper<Void>() {
                            @Override
                            protected Void run(Node node)
                                    throws RepositoryException {
                                String nodeName = "node" + threadNumber;
                                if (node.hasNode(nodeName)) {
                                    node.getNode(nodeName).remove();
                                } else {
                                    node.addNode(nodeName);
                                }
                                s.save();
                                log.println("Thread" + threadNumber
                                        + ": saved modification");
                                return null;
                            }
                        }.with(n, false);

                        // do a random wait
                        Thread.sleep(random.nextInt(100));
                    }
                } finally {
                    s.logout();
                }
                return null;
            }

        };
    }

    public void testTimeout() throws RepositoryException, InterruptedException {

        final Node lockable = testRootNode.addNode("testTimeout"
                + System.currentTimeMillis());
        lockable.addMixin(mixLockable);
        superuser.save();

        ExecutorService e = Executors.newFixedThreadPool(2);
        Future<?> lockMaster = e.submit(buildNewTimeoutCallable(
                lockable.getPath(), true));
        Future<?> lockSlave = e.submit(buildNewTimeoutCallable(
                lockable.getPath(), false));
        e.shutdown();

        try {
            lockMaster.get();
            lockSlave.get();
        } catch (ExecutionException ex) {
            if (ex.getCause().getClass().isAssignableFrom(LockException.class)) {
                return;
            }
            fail(ex.getMessage());
        }
        fail("was expecting a LockException");
    }

    public Callable<Void> buildNewTimeoutCallable(final String lockablePath,
            final boolean isLockMaster) {

        return new Callable<Void>() {

            public Void call() throws RepositoryException, InterruptedException {

                // allow master to be first to obtain the lock on the node
                if (!isLockMaster) {
                    TimeUnit.SECONDS.sleep(2);
                }

                final Session s = getHelper().getSuperuserSession();

                try {
                    Node n = s.getNode(lockablePath);

                    new LockedWrapper<Void>() {

                        @Override
                        protected Void run(Node node)
                                throws RepositoryException {

                            if (isLockMaster) {
                                try {
                                    TimeUnit.SECONDS.sleep(15);
                                } catch (InterruptedException e) {
                                    //
                                }
                            }
                            return null;
                        }
                    }.with(n, false, 2000);

                } finally {
                    s.logout();
                }
                return null;
            }
        };
    }

    /**
     * Tests concurrent updates on a persistent counter
     */
    public void testSequence() throws RepositoryException, InterruptedException {

        counter = new AtomicInteger(0);

        final Node lockable = testRootNode.addNode(nodeName1);
        lockable.setProperty("value", counter.getAndIncrement());
        lockable.addMixin(mixLockable);
        superuser.save();

        List<Future<Long>> futures = new ArrayList<Future<Long>>();
        ExecutorService e = Executors.newFixedThreadPool(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS * NUM_VALUE_GETS; i++) {
            futures.add(e.submit(buildNewSequenceUpdateCallable(
                    lockable.getPath(), false)));
        }
        e.shutdown();
        assertTrue(e.awaitTermination(10 * 60, TimeUnit.SECONDS));

        for (Future<Long> future : futures) {
            try {
                Long v = future.get();
                log.println("Got sequence number: " + v);
            } catch (ExecutionException ex) {
                fail(ex.getMessage());
            }
        }
    }

    /**
     * Tests concurrent updates on a persistent counter, with session refresh
     */
    public void testSequenceWithSessionRefresh() throws RepositoryException,
            InterruptedException {

        counter = new AtomicInteger(0);

        final Node lockable = testRootNode.addNode(nodeName1);
        lockable.setProperty("value", counter.getAndIncrement());
        lockable.addMixin(mixLockable);
        superuser.save();

        List<Future<Long>> futures = new ArrayList<Future<Long>>();
        ExecutorService e = Executors.newFixedThreadPool(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS * NUM_VALUE_GETS; i++) {
            futures.add(e.submit(buildNewSequenceUpdateCallable(
                    lockable.getPath(), true)));
        }
        e.shutdown();
        assertTrue(e.awaitTermination(10 * 60, TimeUnit.SECONDS));

        for (Future<Long> future : futures) {
            try {
                Long v = future.get();
                log.println("Got sequence number: " + v);
            } catch (ExecutionException ex) {
                fail(ex.getMessage());
            }
        }
    }

    public Callable<Long> buildNewSequenceUpdateCallable(
            final String counterPath, final boolean withSessionRefresh) {

        return new Callable<Long>() {

            public Long call() throws RepositoryException, InterruptedException {

                final Session s = getHelper().getSuperuserSession();

                try {
                    if (withSessionRefresh) {
                        s.refresh(false);
                    }
                    Node n = s.getNode(counterPath);

                    long value = new LockedWrapper<Long>() {

                        @Override
                        protected Long run(Node node)
                                throws RepositoryException {

                            Property seqProp = node.getProperty("value");
                            long value = seqProp.getLong();
                            seqProp.setValue(++value);
                            s.save();
                            return value;
                        }
                    }.with(n, false);

                    // check that the sequence is ok
                    assertEquals(counter.getAndIncrement(), value);

                    // do a random wait
                    Thread.sleep(random.nextInt(100));

                    return value;
                } finally {
                    s.logout();
                }
            }
        };
    }
}
