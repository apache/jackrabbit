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

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import EDU.oswego.cs.dl.util.concurrent.SynchronousChannel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Test cases for data store garbage collection.
 */
public class GarbageCollectorTest extends AbstractJCRTest implements ScanEventListener {

    /** logger instance */
    private static final Logger LOG = LoggerFactory.getLogger(GarbageCollectorTest.class);

    public void testConcurrentGC() throws Exception {
        Node root = testRootNode;
        Session session = root.getSession();
        RepositoryImpl rep = (RepositoryImpl) session.getRepository();
        if (rep.getDataStore() == null) {
            LOG.info("testConcurrentGC skipped. Data store is not used.");
            return;
        }
        final SynchronousChannel sync = new SynchronousChannel();
        final Node node = root.addNode("slowBlob");
        new Thread() {
            public void run() {
                try {
                    node.setProperty("slowBlob", new InputStream() {
                        int pos;
                        public int read() throws IOException {
                            pos++;
                            if (pos < 10000) {
                                return pos % 80 == 0 ? '\n' : '.';
                            } else if (pos == 10000) {
                                try {
                                    sync.put("x");
                                    // deleted
                                    sync.take();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                return 'x';
                            }
                            return -1;
                        }
                    });
                    node.getSession().save();
                    sync.put("saved");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
        assertEquals("x", sync.take());
        GarbageCollector gc = ((SessionImpl) session).createDataStoreGarbageCollector();
        gc.scan();
        gc.stopScan();
        gc.deleteUnused();
        sync.put("deleted");
        assertEquals("saved", sync.take());
        InputStream in = node.getProperty("slowBlob").getStream();
        for (int pos = 1; pos < 10000; pos++) {
            int expected = pos % 80 == 0 ? '\n' : '.';
            assertEquals(expected, in.read());
        }
        assertEquals('x', in.read());
        in.close();
    }

    public void testGC() throws Exception {
        Node root = testRootNode;
        Session session = root.getSession();

        RepositoryImpl rep = (RepositoryImpl) session.getRepository();
        if (rep.getDataStore() == null) {
            LOG.info("testGC skipped. Data store is not used.");
            return;
        }

        deleteMyNodes();
        runGC(session, true);
        runGC(session, true);

        root.addNode("node1");
        Node node2 = root.addNode("node2");
        Node n = node2.addNode("nodeWithBlob");
        n.setProperty("test", new RandomInputStream(10, 10000));
        n = node2.addNode("nodeWithTemporaryBlob");
        n.setProperty("test", new RandomInputStream(11, 10000));
        session.save();

        n.remove();
        session.save();
        Thread.sleep(1000);

        GarbageCollector gc = ((SessionImpl)session).createDataStoreGarbageCollector();
        gc.setScanEventListener(this);
        gc.setTestDelay(1000);

        LOG.debug("scanning...");
        gc.scan();
        int count = listIdentifiers(gc);
        LOG.debug("stop scanning; currently " + count + " identifiers");
        gc.stopScan();
        LOG.debug("deleting...");
        gc.getDataStore().clearInUse();
        assertTrue(gc.deleteUnused() > 0);
        int count2 = listIdentifiers(gc);
        assertEquals(count - 1, count2);

        deleteMyNodes();
    }

    private void runGC(Session session, boolean all) throws RepositoryException, IOException, ItemStateException {
        GarbageCollector gc = ((SessionImpl)session).createDataStoreGarbageCollector();
        gc.setScanEventListener(this);
        gc.setTestDelay(1000);
        gc.scan();
        gc.stopScan();
        if (all) {
            gc.getDataStore().clearInUse();
        }
        gc.deleteUnused();
    }

    private int listIdentifiers(GarbageCollector gc) throws DataStoreException {
        LOG.debug("identifiers:");
        Iterator it = gc.getDataStore().getAllIdentifiers();
        int count = 0;
        while (it.hasNext()) {
            DataIdentifier id = (DataIdentifier) it.next();
            LOG.debug("  " + id);
            count++;
        }
        return count;
    }

    public void testTransientObjects() throws Exception {

        Node root = testRootNode;
        Session session = root.getSession();

        RepositoryImpl rep = (RepositoryImpl) session.getRepository();
        if (rep.getDataStore() == null) {
            LOG.info("testTransientObjects skipped. Data store is not used.");
            return;
        }

        deleteMyNodes();
        runGC(session, true);
        runGC(session, true);

        Credentials cred = helper.getSuperuserCredentials();
        Session s2 = helper.getRepository().login(cred);
        root = s2.getRootNode();
        Node node2 = root.addNode("node3");
        Node n = node2.addNode("nodeWithBlob");
        n.setProperty("test", new RandomInputStream(10, 10000));
        Thread.sleep(1000);

        runGC(session, false);

        s2.save();

        InputStream in = n.getProperty("test").getStream();
        InputStream in2 = new RandomInputStream(10, 10000);
        while (true) {
            int a = in.read();
            int b = in2.read();
            assertEquals(a, b);
            if (a < 0) {
                break;
            }
        }

        deleteMyNodes();
    }

    public void afterScanning(Node n) throws RepositoryException {
        if (n != null && n.getPath().startsWith("/testroot/node")) {
            String path = n.getPath();
            LOG.debug("scanned: " + path);
        }
    }

    private void list(Node n) throws RepositoryException {
        if (!n.getName().startsWith("jcr:")) {
            for (NodeIterator it = n.getNodes(); it.hasNext();) {
                list(it.nextNode());
            }
        }
    }

    public void beforeScanning(Node n) throws RepositoryException {
        if (n != null && n.getPath().equals("/testroot/node2")) {
            Session session = n.getSession();
            list(session.getRootNode());
            session.move("/testroot/node2/nodeWithBlob", "/testroot/node1/nodeWithBlob");
            session.save();
            LOG.debug("moved /testroot/node2/nodeWithBlob to /testroot/node1");
        }
    }

    public void done() {
    }

    private void deleteMyNodes() throws RepositoryException {
        Node root = testRootNode;
        while (root.hasNode("testroot")) {
            root.getNode("testroot").remove();
        }
        root.getSession().save();
    }

}
