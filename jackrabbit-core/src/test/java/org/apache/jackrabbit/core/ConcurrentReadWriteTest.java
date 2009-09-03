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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.PropertyIterator;
import javax.jcr.Property;
import javax.jcr.InvalidItemStateException;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Collections;

/**
 * <code>ConcurrentReadWriteTest</code> performs a test with a number of
 * concurrent readers and one writer.
 */
public class ConcurrentReadWriteTest extends AbstractConcurrencyTest {

    private static final int NUM_NODES = 5;

    private static final int NUM_THREADS = 5;

    private static final int RUN_NUM_SECONDS = 20;

    public void testReadWrite() throws RepositoryException {
        final List uuids = new ArrayList();
        for (int i = 0; i < NUM_NODES; i++) {
            Node n = testRootNode.addNode("node" + i);
            n.addMixin(mixReferenceable);
            uuids.add(n.getUUID());
        }
        final List exceptions = Collections.synchronizedList(new ArrayList());
        final long[] numReads = new long[]{0};
        testRootNode.save();
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    runTask(new Task() {
                        public void execute(Session session, Node test)
                                throws RepositoryException {
                            Random rand = new Random();
                            long start = System.currentTimeMillis();
                            long reads = 0;
                            while (System.currentTimeMillis() < start + RUN_NUM_SECONDS * 1000) {
                                String uuid = (String) uuids.get(rand.nextInt(uuids.size()));
                                Node n = session.getNodeByUUID(uuid);
                                try {
                                    for (PropertyIterator it = n.getProperties(); it.hasNext(); ) {
                                        Property p = it.nextProperty();
                                        if (p.isMultiple()) {
                                            p.getValues();
                                        } else {
                                            p.getValue();
                                        }
                                    }
                                } catch (InvalidItemStateException e) {
                                    // ignore
                                }
                                reads++;
                            }
                            synchronized (numReads) {
                                numReads[0] += reads;
                            }
                        }
                    }, NUM_THREADS, testRoot);
                } catch (RepositoryException e) {
                    exceptions.add(e);
                }
            }
        });
        t.start();
        long numWrites = 0;
        while (t.isAlive()) {
            Random rand = new Random();
            String uuid = (String) uuids.get(rand.nextInt(uuids.size()));
            Node n = superuser.getNodeByUUID(uuid);
            if (n.hasProperty("test")) {
                n.getProperty("test").remove();
            } else {
                n.setProperty("test", "hello world");
            }
            n.save();
            numWrites++;
        }
        log.println("#writes performed: " + numWrites);
        log.println("#reads performed: " + numReads[0]);
        if (!exceptions.isEmpty()) {
            fail(((RepositoryException) exceptions.get(0)).getMessage());
        }
    }
}
