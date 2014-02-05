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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.ValueFactory;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.api.JackrabbitRepositoryFactory;
import org.apache.jackrabbit.api.management.MarkEventListener;
import org.apache.jackrabbit.core.RepositoryFactoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.gc.GarbageCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test case for the scenario where the GC thread traverses the workspace and at
 * some point, a subtree that the GC thread did not see yet is moved to a location
 * that the thread has already traversed. The GC thread should not ignore binaries 
 * references by this subtree and eventually delete them.
 */
public class GCSubtreeMoveTest extends TestCase {

    private static final Logger logger = LoggerFactory.getLogger(GCSubtreeMoveTest.class);

    private String testDirectory;
    private JackrabbitRepository repository;
    private Session sessionGarbageCollector;
    private Session sessionMover;

    public void setUp() throws IOException {
        testDirectory = "target/" + getClass().getSimpleName()  + "/" + getName();
        FileUtils.deleteDirectory(new File(testDirectory));
    }

    public void tearDown() throws IOException {
        sessionGarbageCollector.logout();
        sessionMover.logout();
        repository.shutdown();

        repository = null;
        sessionGarbageCollector = null;
        sessionMover = null;

        FileUtils.deleteDirectory(new File(testDirectory));
        testDirectory = null;
    }

    public void test() {
        setupRepository();

        GarbageCollector garbageCollector = setupGarbageCollector();
        // To make sure even listener for NODE_ADDED is registered in GC.
        garbageCollector.setPersistenceManagerScan(false);

        assertEquals(0, getBinaryCount(garbageCollector));
        setupNodes();
        assertEquals(1, getBinaryCount(garbageCollector));
        garbageCollector.getDataStore().clearInUse();

        garbageCollector.setMarkEventListener(new MarkEventListener() {

            public void beforeScanning(Node node) throws RepositoryException {
                String path = node.getPath();
                if (path.startsWith("/node")) {
                    log("Traversing: " + node.getPath());
                }

                if ("/node1".equals(node.getPath())) {
                    String from = "/node2/node3";
                    String to = "/node0/node3";
                    log("Moving " + from + " -> " + to);
                    sessionMover.move(from, to);
                    sessionMover.save();
                    sleepForFile();
                }
            }
        });

        try {
            garbageCollector.getDataStore().clearInUse();
            garbageCollector.mark();
            garbageCollector.stopScan();
            sleepForFile();
            int numberOfDeleted = garbageCollector.sweep();
            log("Number of deleted: " + numberOfDeleted);
            // Binary data should still be there.
            assertEquals(1, getBinaryCount(garbageCollector));
        } catch (RepositoryException e) {
            e.printStackTrace();
            failWithException(e);
        } finally {
            garbageCollector.close();
        }
    }

    private void setupNodes() {
        try {
            Node rootNode = sessionMover.getRootNode();
            rootNode.addNode("node0");
            rootNode.addNode("node1");
            Node node2 = rootNode.addNode("node2");
            Node node3 = node2.addNode("node3");
            Node nodeWithBinary = node3.addNode("node-with-binary");
            ValueFactory vf = sessionGarbageCollector.getValueFactory();
            nodeWithBinary.setProperty("prop", vf.createBinary(new RandomInputStream(10, 1000)));
            sessionMover.save();
            sleepForFile();
        } catch (RepositoryException e) {
            failWithException(e);
        }
    }

    private void sleepForFile() {
        // Make sure the file is old (access time resolution is 2 seconds)
        try {
            Thread.sleep(2200);
        } catch (InterruptedException ignore) {
        }
    }

    private void setupRepository() {
        JackrabbitRepositoryFactory repositoryFactory = new RepositoryFactoryImpl();
        createRepository(repositoryFactory);
        login();
    }

    private void createRepository(JackrabbitRepositoryFactory repositoryFactory) {
        Properties prop = new Properties();
        prop.setProperty("org.apache.jackrabbit.repository.home", testDirectory);
        prop.setProperty("org.apache.jackrabbit.repository.conf", testDirectory + "/repository.xml");
        try {
            repository = (JackrabbitRepository)repositoryFactory.getRepository(prop);
        } catch (RepositoryException e) {
            failWithException(e);
        };
    }

    private void login() {
        try {
            sessionGarbageCollector = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            sessionMover = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        } catch (Exception e) {
            failWithException(e);
        }
    }

    private GarbageCollector setupGarbageCollector() {
        try {
            return ((SessionImpl) sessionGarbageCollector).createDataStoreGarbageCollector();
        } catch (RepositoryException e) {
            failWithException(e);
        }
        return null;
    }

    private void failWithException(Exception e) {
        fail("Not expected: " + e.getMessage());
    }

    private int getBinaryCount(GarbageCollector garbageCollector) {
        int count = 0;
        Iterator<DataIdentifier> it;
        try {
            it = garbageCollector.getDataStore().getAllIdentifiers();
            while (it.hasNext()) {
                it.next();
                count++;
            }
        } catch (DataStoreException e) {
            failWithException(e);
        }
        log("Binary count: " + count);
        return count;
    }

    private void log(String message) {
        logger.debug(message);
        //System.out.println(message);
    }
}
