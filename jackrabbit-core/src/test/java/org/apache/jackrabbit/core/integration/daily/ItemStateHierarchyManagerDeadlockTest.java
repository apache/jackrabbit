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
package org.apache.jackrabbit.core.integration.daily;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

public class ItemStateHierarchyManagerDeadlockTest extends TestCase {

    public int g_numThreads = 30;

    private int g_numRuns = 30;

    private File repoDescriptor;

    private File repoHome;

    private RepositoryImpl repository;

    public void setUp() throws IOException, RepositoryException {
        File baseDir = new File(System.getProperty("basedir", "."));
        File repoBaseDir = new File(baseDir, "target/ItemStateHierarchyManagerDeadlockTest");
        FileUtils.deleteQuietly(repoBaseDir);

        repoDescriptor = new File(repoBaseDir, "repository.xml");
        repoHome = new File(repoBaseDir, "repository");
        repoHome.mkdirs();

        File repositoryDescriptor = new File(baseDir, "src/test/repository/repository.xml");
        FileUtils.copyFile(repositoryDescriptor, repoDescriptor);
    }

    public void tearDown() throws IOException, InterruptedException {
        FileUtils.deleteQuietly(repoHome.getParentFile());
    }

    private void startRepository() throws RepositoryException {
        repository =
            RepositoryImpl.create(RepositoryConfig.create(repoDescriptor.getAbsolutePath(), repoHome
                .getAbsolutePath()));
    }

    private Session login() throws RepositoryException {
        return repository.login(new SimpleCredentials("admin", "admin".toCharArray()), null);
    }

    private void stopRepository() throws RepositoryException {
        repository.shutdown();
    }

    public void testConcurrentWritingSessions() throws Exception {

        startRepository();

        for (int i = 0; i < g_numRuns; i++) {
            clearInvRootNode();
            createInvRootNode();
            runDeadlockTest();
            try {
                System.out.println("*** DONE FOR RUN " + (i + 1) + "/" + g_numRuns
                        + ". SLEEP 1s before running next one");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        stopRepository();
    }

    public void clearInvRootNode() {
        System.out.println("Clear test repository InventoryTest.");
        Session session = null;
        try {
            session = login();
            Node root = session.getRootNode();
            try {
                Node inv = root.getNode("InventoryTest");
                inv.remove();
                session.save();
            } catch (PathNotFoundException pnfx) {
                System.err.println(" The root node <InventoryTest> is not available.");
            }
        } catch (Exception e) {
            System.err.println("Exception in clear test repository:" + e.getMessage());
            e.printStackTrace();
        } finally {
            if (session != null)
                session.logout();
        }
    }

    public void createInvRootNode() throws RepositoryException {
        Session session = null;
        try {
            session = login();
            getInvRootNode(session);
        } catch (Exception e) {
            System.err.println("Exception in clear test repository:" + e.getMessage());
            e.printStackTrace();
        } finally {
            if (session != null)
                session.logout();
        }
    }

    private Node getInvRootNode(Session session) throws RepositoryException {
        Node root = session.getRootNode();
        Node inventoryRoot;
        try {
            inventoryRoot = root.getNode("InventoryTest");
        } catch (PathNotFoundException pnfx) {
            System.err.println(" The root node <InventoryTest> is not available. So creating new root Node.");
            inventoryRoot = root.addNode("InventoryTest");
            session.save();
        }

        return inventoryRoot;
    }

    public void createNodesUnderInvRootNode() {
        System.out.println("Start createNodesUnderInvRootNode ");
        Session session = null;
        try {
            session = login();
            Node inventoryRoot = getInvRootNode(session);
            for (int num = 0; num < 3; num++) {
                Node current = inventoryRoot.addNode("Test" + num + "_" + System.currentTimeMillis());
                current.setProperty("profondeur", 123);
                current.setProperty("tree", "1");
                current.setProperty("clientid", 1);
                current.setProperty("propId", System.currentTimeMillis());
                current.setProperty("name", "Node " + System.currentTimeMillis());
                current.setProperty("address", "1.22.3.3");
            }
            session.save();
            System.out.println("End createNodesUnderInvRootNode ");
        } catch (Exception e) {
            System.err.println("Exception in createNodesUnderInvRootNode:" + e.getMessage());
        } finally {
            if (session != null)
                session.logout();
        }
    }

    public void retrieveNodesUnderInvRootNode() {
        System.out.println("Start retrieveNodesUnderInvRootNode ");
        Session session = null;
        try {
            session = login();
            // start from the bottom of the tree and move up
            Node inventoryRoot = getInvRootNode(session);
            NodeIterator nodes = inventoryRoot.getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                // System.out.println("   Node: " + node.getName());
                PropertyIterator properties = node.getProperties();
                while (properties.hasNext()) {
                    Property prop = properties.nextProperty();
                    // System.out.println("      Prop: " + prop.getName() + " - " + prop.getString());
                }
            }
            session.save();
            System.out.println("End retrieveNodesUnderInvRootNode");
        } catch (Exception e) {
            System.err.println("Exception in retrieveNodesUnderInvRootNode:" + e.getMessage());
        } finally {
            if (session != null)
                session.logout();
        }
    }

    public void removeNodesUnderInvRootNode() {
        System.out.println("Start removeNodesUnderInvRootNode ");
        Session session = null;
        try {
            session = login();
            // start from the bottom of the tree and move up
            Node inventoryRoot = getInvRootNode(session);
            NodeIterator nodes = inventoryRoot.getNodes();
            int num = 0;
            while (nodes.hasNext() && num < 3) {
                Node node = nodes.nextNode();
                node.remove();
                num++;
            }
            session.save();
            System.out.println("End removeNodesUnderInvRootNode");
        } catch (Exception e) {
            System.err.println("Exception in removeNodesUnderInvRootNode:" + e.getMessage());
        } finally {
            if (session != null)
                session.logout();
        }
    }

    public void runDeadlockTest() {
        long start = System.currentTimeMillis();
        List threads = new ArrayList();
        for (int instanceIndex = 0; instanceIndex < g_numThreads; instanceIndex++) {
            final int index = instanceIndex;
            Thread t = new Thread(new Runnable() {

                public void run() {
                    for (int rounds = 0; rounds < 2; rounds++) {
                        if (index % 2 == 0) {
                            removeNodesUnderInvRootNode();
                        }
                        createNodesUnderInvRootNode();
                        retrieveNodesUnderInvRootNode();
                        if (index % 2 != 0) {
                            removeNodesUnderInvRootNode();
                        }
                    }
                }

            });
            threads.add(t);
            t.start();
        }

        try {
            Iterator it = threads.listIterator();
            while (it.hasNext()) {
                Thread t = (Thread) it.next();
                t.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Duration for run: " + (System.currentTimeMillis() - start) / 1000 + "s");
    }

}
