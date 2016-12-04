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

import java.io.ByteArrayInputStream;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

/**
 * Test the DataStore garbage collector.
 * This tests that the EventListener is called while scanning the repository.
 *
 * @author Thomas Mueller
 */
public class GCEventListenerTest extends AbstractJCRTest implements MarkEventListener {

    /** logger instance */
    private static final Logger LOG = LoggerFactory.getLogger(GCEventListenerTest.class);

    private static final String TEST_NODE_NAME = "testGCEventListener";

    private boolean gotNullNode;
    private boolean gotNode;
    private int count;

    public void testEventListener() throws Exception {
        doTestEventListener(true);
        doTestEventListener(false);
    }

    private void doTestEventListener(boolean allowPmScan) throws Exception {
        Node root = testRootNode;
        Session session = root.getSession();
        if (root.hasNode(TEST_NODE_NAME)) {
            root.getNode(TEST_NODE_NAME).remove();
            session.save();
        }
        Node test = root.addNode(TEST_NODE_NAME);
        Random random = new Random();
        byte[] data = new byte[10000];
        for (int i = 0; i < 10; i++) {
            Node n = test.addNode("x" + i);
            random.nextBytes(data);
            ValueFactory vf = session.getValueFactory();
            n.setProperty("data", vf.createBinary(new ByteArrayInputStream(data)));
            session.save();
            if (i % 2 == 0) {
                n.remove();
                session.save();
            }
        }
        session.save();
        SessionImpl si = (SessionImpl) session;
        DataStoreGarbageCollector gc = si.createDataStoreGarbageCollector();
        DataStore ds = ((GarbageCollector) gc).getDataStore();
        if (ds != null) {
            ds.clearInUse();
            boolean pmScan = gc.isPersistenceManagerScan();
            gc.setPersistenceManagerScan(allowPmScan);
            gotNullNode = false;
            gotNode = false;
            gc.setMarkEventListener(this);
            gc.mark();
            if (pmScan && allowPmScan) {
                assertTrue("PM scan without null Node", gotNullNode);
                assertFalse("PM scan, but got a real node", gotNode);
            } else {
                assertFalse("Not a PM scan - but got a null Node", gotNullNode);
                assertTrue("Not a PM scan - without a real node", gotNode);
            }
            int deleted = gc.sweep();
            LOG.debug("Deleted " + deleted);
            assertTrue("Should delete at least one item", deleted >= 0);
            gc.close();
        }
    }

    public String getNodeName(Node n) throws RepositoryException {
        if (n == null) {
            gotNullNode = true;
            return String.valueOf(count++);
        } else {
            gotNode = true;
            return n.getPath();
        }
    }

    public void beforeScanning(Node n) throws RepositoryException {
        String s = getNodeName(n);
        if (s != null) {
            LOG.debug("scanning " + s);
        }
    }

    public void done() {
    }

}
