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
import org.apache.jackrabbit.core.gc.GarbageCollector;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import EDU.oswego.cs.dl.util.concurrent.SynchronousChannel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.jcr.Binary;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

/**
 * Test cases for data store garbage collection.
 */
public class GarbageCollectorTest extends AbstractJCRTest implements ScanEventListener {

    /** logger instance */
    private static final Logger LOG = LoggerFactory.getLogger(GarbageCollectorTest.class);

    public void testCloseSessionWhileRunningGc() throws Exception {
        final Session session = getHelper().getReadWriteSession();

        final DataStoreGarbageCollector gc = ((SessionImpl) session).createDataStoreGarbageCollector();
        gc.setPersistenceManagerScan(false);
        final Exception[] ex = new Exception[1];
        gc.setMarkEventListener(new MarkEventListener() {
            boolean closed;

            public void beforeScanning(Node n) throws RepositoryException {
                closeTest();
            }

            private void closeTest() {
                if (closed) {
                    ex[0] = new Exception("Scanning after the session is closed");
                }
                closed = true;
                session.logout();
            }

        });
        try {
            gc.mark();
            fail("Exception 'session has been closed' expected");
        } catch (RepositoryException e) {
            LOG.debug("Expected exception caught: " + e.getMessage());
        }
        if (ex[0] != null) {
            throw ex[0];
        }
        gc.close();
    }

    public void testConcurrentGC() throws Exception {
        Node root = testRootNode;
        Session session = root.getSession();

        final SynchronousChannel sync = new SynchronousChannel();
        final Node node = root.addNode("slowBlob");
        final int blobLength = 1000;
        final ValueFactory vf = session.getValueFactory();
        new Thread() {
            public void run() {
                try {
                    node.setProperty("slowBlob", vf.createBinary(new InputStream() {
                        int pos;
                        public int read() throws IOException {
                            pos++;
                            if (pos < blobLength) {
                                return pos % 80 == 0 ? '\n' : '.';
                            } else if (pos == blobLength) {
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
                    }));
                    node.getSession().save();
                    sync.put("saved");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
        assertEquals("x", sync.take());
        DataStoreGarbageCollector gc = ((SessionImpl) session).createDataStoreGarbageCollector();
        gc.setPersistenceManagerScan(false);
        gc.mark();
        gc.sweep();
        sync.put("deleted");
        assertEquals("saved", sync.take());
        InputStream in = node.getProperty("slowBlob").getBinary().getStream();
        for (int pos = 1; pos < blobLength; pos++) {
            int expected = pos % 80 == 0 ? '\n' : '.';
            assertEquals(expected, in.read());
        }
        assertEquals('x', in.read());
        in.close();
        gc.close();
    }

    public void testGC() throws Exception {
        Node root = testRootNode;
        Session session = root.getSession();

        deleteMyNodes();
        runGC(session, true);

        root.addNode("node1");
        Node node2 = root.addNode("node2");
        Node n = node2.addNode("nodeWithBlob").addNode("sub");
        ValueFactory vf = session.getValueFactory();
        Binary b = vf.createBinary(new RandomInputStream(20, 1000));
        n.setProperty("test", b);
        session.save();
        n = node2.addNode("nodeWithTemporaryBlob");
        n.setProperty("test", vf.createBinary(new RandomInputStream(11, 1000)));
        session.save();

        n.remove();
        session.save();

        GarbageCollector gc = ((SessionImpl)session).createDataStoreGarbageCollector();
        gc.getDataStore().clearInUse();
        gc.setPersistenceManagerScan(false);
        gc.setMarkEventListener(this);

        if (gc.getDataStore() instanceof FileDataStore) {
            // make sure the file is old (access time resolution is 2 seconds)
            Thread.sleep(2000);
        }

        LOG.debug("scanning...");
        gc.mark();
        int count = listIdentifiers(gc);
        LOG.debug("stop scanning; currently " + count + " identifiers");
        LOG.debug("deleting...");
        gc.getDataStore().clearInUse();
        assertTrue(gc.sweep() > 0);
        int count2 = listIdentifiers(gc);
        assertEquals(count - 1, count2);

        // verify the node was moved, and that the binary is still there
        n = root.getNode("node1").getNode("nodeWithBlob").getNode("sub");
        b = n.getProperty("test").getValue().getBinary();
        InputStream in = b.getStream();
        InputStream in2 = new RandomInputStream(20, 1000);
        verifyInputStream(in, in2);

        deleteMyNodes();

        gc.close();
    }

    /**
     *  Test to validate that two  GC cannot run simultaneously. One 
     *  exits throwing exception.
     */
    public void testSimultaneousRunGC() throws Exception {
        Node root = testRootNode;
        Session session = root.getSession();

        GCThread gct1 = new GCThread(session);
        GCThread gct2 = new GCThread(session);
        Thread gcThread1 = new Thread(gct1, "Datastore Garbage Collector 1");
        Thread gcThread2 = new Thread(gct2, "Datastore Garbage Collector 2");
        // run simultaneous GC
        gcThread1.start();
        gcThread2.start();
        Thread.sleep(100);

        gct1.setStop(true);
        gct2.setStop(true);

        // allow them to complete
        gcThread1.join();
        gcThread2.join();

        // only one should throw error
        int count = (gct1.getException() == null ? 0 : 1) + (gct2.getException() == null ? 0 : 1);
        if (count == 0) {
            fail("None of the GCs threw an exception");
        }
        else {
            assertEquals("Only one gc should throw an exception ", 1, count);
        }
    }

    private void runGC(Session session, boolean all) throws Exception {
        GarbageCollector gc = ((SessionImpl)session).createDataStoreGarbageCollector();
        gc.setMarkEventListener(this);
        gc.setPersistenceManagerScan(false);

        if (gc.getDataStore() instanceof FileDataStore) {
            // make sure the file is old (access time resolution is 2 seconds)
            Thread.sleep(2000);
        }
        gc.mark();
        gc.stopScan();
        if (all) {
            gc.getDataStore().clearInUse();
        }
        gc.sweep();
        gc.close();
    }

    private static int listIdentifiers(GarbageCollector gc) throws DataStoreException {
        LOG.debug("identifiers:");
        int count = 0;
        Iterator<DataIdentifier> it = gc.getDataStore().getAllIdentifiers();
        while (it.hasNext()) {
            DataIdentifier id = it.next();
            LOG.debug("  " + id);
            count++;
        }
        return count;
    }

    public void testTransientObjects() throws Exception {

        Node root = testRootNode;
        Session session = root.getSession();

        deleteMyNodes();

        Credentials cred = getHelper().getSuperuserCredentials();
        Session s2 = getHelper().getRepository().login(cred);
        root = s2.getRootNode();
        Node node2 = root.addNode("node3");
        Node n = node2.addNode("nodeWithBlob");
        ValueFactory vf = session.getValueFactory();
        n.setProperty("test", vf.createBinary(new RandomInputStream(10, 1000)));

        runGC(session, false);

        s2.save();

        InputStream in = n.getProperty("test").getBinary().getStream();
        InputStream in2 = new RandomInputStream(10, 1000);
        verifyInputStream(in, in2);

        deleteMyNodes();

        s2.logout();
    }

    private static void verifyInputStream(InputStream in, InputStream in2) throws IOException {
        while (true) {
            int a = in.read();
            int b = in2.read();
            assertEquals(a, b);
            if (a < 0) {
                break;
            }
        }

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

    private void deleteMyNodes() throws RepositoryException {
        Node root = testRootNode;
        while (root.hasNode("testroot")) {
            root.getNode("testroot").remove();
        }
        root.getSession().save();
    }

}
