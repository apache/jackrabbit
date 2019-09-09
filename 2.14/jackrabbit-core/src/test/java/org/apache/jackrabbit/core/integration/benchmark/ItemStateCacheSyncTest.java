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
package org.apache.jackrabbit.core.integration.benchmark;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import junit.framework.TestCase;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.core.TransientRepository;

/**
 * Test case for
 * <a href="https://issues.apache.org/jira/browse/JCR-2546">JCR-2546</a>.
 * Note that this test takes a long time to finish and does not contain
 * normal assertions, so it should only be invoked explicitly instead of
 * being included in the normal test suite.
 */
public class ItemStateCacheSyncTest extends TestCase {

    private File directory;

    private Repository repository;

    private Session session;

    private Node root;

    private volatile boolean run;

    private AtomicLong counter = new AtomicLong();

    protected void setUp() throws Exception {
        directory = new File("target", "jackrabbit-sync-test-repo");

        repository = new TransientRepository(directory);

        session = repository.login(
                new SimpleCredentials("admin", "admin".toCharArray()));

        // Add a tree of 100k medium-sized (~10kB) nodes
        String[] data = new String[1000];
        Arrays.fill(data, "something");
        root = session.getRootNode();
        for (int i = 0; i < 1000; i++) {
            Node a = root.addNode("a" + i);
            for (int j = 0; j < 100; j++) {
                Node b = a.addNode("b" + j);
                b.setProperty("data", data);
            }
            session.save();
            System.out.println((i + 1) * 1000 + " nodes created");
        }
    }

    protected void tearDown() throws Exception {
        for (int i = 0; i < 1000; i++) {
            root.getNode("a" + i).remove();
            session.save();
        }
        session.logout();
    }

    public void testCacheSync() throws Exception {
        run = true;
        Thread[] threads = new Thread[30];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Lookup());
            threads[i].start();
        }

        // time for all threads to start and caches to warm up
        Thread.sleep(3000);

        long start, stop, count;

        counter.set(0);
        start = System.currentTimeMillis();
        Thread.sleep(10000);
        count = counter.get();
        stop = System.currentTimeMillis();
        System.out.println(
                count * 1000 / (stop - start) + " lookups per second");

        counter.set(0);
        start = System.currentTimeMillis();
        int i = 0;
        do {
            Node node = root.getNode("a" + (i++) % 1000);
            for (Node ignore : JcrUtils.getChildNodes(node)) {
            }
            count = counter.get();
            stop = System.currentTimeMillis();
        } while (stop < start + 10000);
        System.out.println(
                count * 1000 / (stop - start) + " lookups per second"
                + " while traversing " + (i * 1000 * 1000)  / (stop - start)
                + " nodes per second");

        run = false;
        for (i = 0; i < threads.length; i++) {
            threads[i].join();
        }
    }

    public class Lookup implements Runnable {

        public void run() {
            try {
                Session session = repository.login();
                try {
                    while (run) {
                        for (int i = 0; i < 100; i++) {
                            if (session.nodeExists("/a" + i)) {
                                session.getNode("/a" + 1);
                            }
                            counter.incrementAndGet();
                        }
                    }
                } finally {
                    session.logout();
                }
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public class Traverse implements ItemVisitor {

        public void visit(Property property) {
        }

        public void visit(Node node) throws RepositoryException {
            for (Node child : JcrUtils.getChildNodes(node)) {
                child.accept(this);
            }
        }

    }

}
