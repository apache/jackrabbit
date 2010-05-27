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
package org.apache.jackrabbit.core.data;

import org.apache.jackrabbit.api.management.DataStoreGarbageCollector;
import org.apache.jackrabbit.api.management.MarkEventListener;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

/**
 * Test case for concurrent garbage collection
 */
public class GCConcurrentTest extends AbstractJCRTest {

    /** logger instance */
    private static final Logger LOG = LoggerFactory.getLogger(GCConcurrentTest.class);

    public void testConcurrentDelete() throws Exception {
        Node root = testRootNode;
        Session session = root.getSession();

        final String testNodeName = "testConcurrentDelete";
        node(root, testNodeName);
        session.save();
        DataStoreGarbageCollector gc = ((SessionImpl) session).createDataStoreGarbageCollector();
        gc.setPersistenceManagerScan(false);
        gc.setMarkEventListener(new MarkEventListener() {
            public void beforeScanning(Node n) throws RepositoryException {
                if (n.getName().equals(testNodeName)) {
                    n.remove();
                    n.getSession().save();
                }
            }

        });
        gc.mark();
        gc.close();
    }

    public void testGC() throws Exception {
        Node root = testRootNode;
        Session session = root.getSession();

        GCThread gc = new GCThread(session);
        Thread gcThread = new Thread(gc, "Datastore Garbage Collector");

        int len = 10 * getTestScale();
        boolean started = false;
        for (int i = 0; i < len; i++) {
            if (!started && i > 5 + len / 100) {
                started = true;
                gcThread.start();
            }
            Node n = node(root, "test" + i);
            ValueFactory vf = session.getValueFactory();
            n.setProperty("data", vf.createBinary(randomInputStream(i)));
            session.save();
            LOG.debug("saved: " + i);
        }
        Thread.sleep(10);
        for (int i = 0; i < len; i++) {
            Node n = root.getNode("test" + i);
            Property p = n.getProperty("data");
            InputStream in = p.getBinary().getStream();
            InputStream expected = randomInputStream(i);
            checkStreams(expected, in);
            n.remove();
            LOG.debug("removed: " + i);
            session.save();
        }
        Thread.sleep(10);
        gc.setStop(true);
        Thread.sleep(10);
        gcThread.join();
        gc.throwException();
    }

    private void checkStreams(InputStream expected, InputStream in) throws IOException {
        while (true) {
            int e = expected.read();
            int i = in.read();
            if (e < 0 || i < 0) {
                if (e >= 0 || i >= 0) {
                    fail("expected: " + e + " got: " + i);
                }
                break;
            } else {
                assertEquals(e, i);
            }
        }
        expected.close();
        in.close();
    }

    static InputStream randomInputStream(long seed) {
        byte[] data = new byte[4096];
        new Random(seed).nextBytes(data);
        return new ByteArrayInputStream(data);
    }

    static Node node(Node n, String x) throws RepositoryException {
        return n.hasNode(x) ? n.getNode(x) : n.addNode(x);
    }

    static int getTestScale() {
        return Integer.parseInt(System.getProperty("jackrabbit.test.scale", "1"));
    }

}
