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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.config.RepositoryConfig;

public class ConcurrencyTest3 extends TestCase {

    private static final int NUM_ITERATIONS = 1;

    private static final int NUM_THREADS = 5;

    private File repoDescriptor;

    private File repoHome;

    private RepositoryImpl repository;

    public void setUp() throws IOException, RepositoryException {
        File baseDir = new File(System.getProperty("basedir", "."));
        File repoBaseDir = new File(baseDir, "target/corruption-test3");
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

    /**
     * Runs the test.
     */
    public void testConcurrentWritingSessions() throws Exception {

        startRepository();

        // Create test root node
        Session session = login();
        Node testRoot = session.getRootNode().addNode("test");
        Node testRoot2 = session.getRootNode().addNode("test2");
        testRoot.addMixin("mix:referenceable");
        session.save();

        for (int i = 0; i < NUM_ITERATIONS; i++) {
            JcrTestThread[] threads = new JcrTestThread[NUM_THREADS];
            Session[] sessions = new Session[NUM_THREADS];
            for (int j = 0; j < threads.length; j++) {
                // create new session and a new thread
                Session threadSession = login();
                JcrTestThread thread = new JcrTestThread(testRoot.getUUID(), threadSession);
                thread.setName("Iteration " + i + " - Thread " + j);
                thread.start();
                threads[j] = thread;
                sessions[j] = threadSession;
            }
            for (int j = 0; j < threads.length; j++) {
                if (threads[j] != null) {
                    threads[j].join();
                }
            }
            for (int j = 0; j < sessions.length; j++) {
                if (sessions[j] != null) {
                    sessions[j].logout();
                }
            }
        }

        session.logout();
        stopRepository();

        // Restart with an empty index, scan and delete test root node
        deleteIndex();
        startRepository();
        session = login();
        testRoot = session.getRootNode().getNode("test");
        testRoot.addNode("new node");
        session.save();
        scan(testRoot);
        testRoot.remove();
        session.save();
        session.logout();
        stopRepository();
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

    private void deleteIndex() throws IOException {
        FileUtils.deleteDirectory(new File(repoHome, "workspaces/default/index"));
    }

    private void scan(Node node) throws RepositoryException {
        // System.err.println(node.getName() + " - " + node.getPath());
        for (NodeIterator it = node.getNodes(); it.hasNext();) {
            Node child = it.nextNode();
            scan(child);
        }
    }

    class JcrTestThread extends Thread {

        private String testRootUuid;

        private Session jcrSession;

        private ArrayList nodes = new ArrayList();

        private int RUN_SIZE = 100;

        private int ACTION_SIZE = 1000;

        JcrTestThread(String uuid, Session session) throws RepositoryException {
            testRootUuid = uuid;
            jcrSession = session;
        }

        public void run() {
            outer: for (int i = 0; i < RUN_SIZE; i++) {
                for (int j = 0; j < ACTION_SIZE; j++) {
                    int random = (int) Math.floor(2 * Math.random());
                    if (random == 0) {
                        try {
                            // Add a node called "P"
                            Node addedNode = jcrSession.getNodeByUUID(testRootUuid).addNode("P");
                            addedNode.addMixin("mix:referenceable");
                            nodes.add(addedNode.getUUID());
                            
                        } catch (RepositoryException ignore) {
                        }
                    } else {
                        if (nodes.size() > 0) {
                            int randomIndex = (int) Math.floor(nodes.size() * Math.random());
                            try {
                                // Remove a random node we created within this
                                // thread and within this session
                                Node removeNode = jcrSession.getNodeByUUID((String) nodes.get(randomIndex));
                                String path = removeNode.getPath();
                                if (path.indexOf("test2") == -1) {
                                    jcrSession.move(removeNode.getPath(), removeNode.getParent().getPath()
                                            + "2/P");
                                } else {
                                    removeNode.remove();
                                    nodes.remove(randomIndex);
                                }
                            } catch (RepositoryException ignore) {
                                System.err.println(" 1 " + ignore.toString());
                            }
                        }
                    }
                }
                try {
                    jcrSession.save();
                } catch (RepositoryException e) {
                    System.err.println(" 2 " + e.toString());
                    break outer;
                }
            }
        }
    }
}
