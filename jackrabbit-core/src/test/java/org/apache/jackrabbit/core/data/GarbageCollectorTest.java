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
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

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
    
    public void testGC() throws Exception {
        Node root = testRootNode;
        Session session = root.getSession();
        
        RepositoryImpl rep = (RepositoryImpl) session.getRepository();
        if (rep.getDataStore() == null) {
            LOG.info("testGC skipped. Data store is not used.");
            return;
        }
        
        deleteMyNodes();
        runGC(session);
        runGC(session);
        
        root.addNode("node1");
        Node node2 = root.addNode("node2");
        Node n = node2.addNode("nodeWithBlob");
        n.setProperty("test", new RandomInputStream(10, 10000));
        n = node2.addNode("nodeWithTemporaryBlob");
        n.setProperty("test", new RandomInputStream(11, 10000));
        session.save();
        
        n.remove();
        session.save();
        
        GarbageCollector gc = new GarbageCollector(this, 0);
        gc.setTestDelay(100);
        
        LOG.debug("scanning...");
        gc.scan(session);
        int count = listIdentifiers(gc);
        LOG.debug("stop scanning...");
        gc.stopScan();
        LOG.debug("deleting...");
        assertTrue(gc.deleteUnused() > 0);
        int count2 = listIdentifiers(gc);
        assertEquals(count - 1, count2);
        
        deleteMyNodes();
    }
    
    private void runGC(Session session) throws RepositoryException, IOException {
        GarbageCollector gc = new GarbageCollector(this, 0);
        gc.setTestDelay(100);
        gc.scan(session);
        gc.stopScan();
        gc.deleteUnused();
    }
    
    private int listIdentifiers(GarbageCollector gc) {
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

    public void afterScanning(Node n) throws RepositoryException {
        if (n.getPath().startsWith("/testroot/node")) {
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
        if (n.getPath().equals("/testroot/node2")) {
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
