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

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.List;

/**
 * Performs a test with two threads. One thread adds nodes to the testRootNode
 * and sets a property. The second thread removes the property as soon as it
 * sees a newly created node.
 */
public class ConcurrentSaveTest extends AbstractJCRTest {

    /** logger instance */
    private static final Logger log = LoggerFactory.getLogger(ConcurrentSaveTest.class);

    private final int NUM_NODES = 1000;
    private Session addNodeSession;
    private Session removePropertySession;

    protected void setUp() throws Exception {
        super.setUp();
        addNodeSession = getHelper().getSuperuserSession();
        removePropertySession = getHelper().getSuperuserSession();
    }

    protected void tearDown() throws Exception {
        try {
            if (addNodeSession != null) {
                addNodeSession.logout();
                addNodeSession = null;
            }
            if (removePropertySession != null) {
                removePropertySession.logout();
                removePropertySession = null;
            }
        } finally {
            super.tearDown();
        }
    }

    /**
     * Runs the test.
     */
    public void testConcurrentSave() throws Exception {

        final String path = testPath;
        final List exceptions = new ArrayList();

        Thread addNodeWorker = new Thread() {
            public void run() {
                try {
                    Node testNode = addNodeSession.getRootNode().getNode(path);
                    for (int i = 0; i < NUM_NODES; i++) {
                        Node n = testNode.addNode("node" + i);
                        n.setProperty("foo", "some text");
                        log.info("creating node: node" + i);
                        testNode.save();
                        log.info("created node: node" + i);
                        // give other thread a chance to catch up
                        yield();
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
        };

        Thread removePropertyWorker = new Thread() {
            public void run() {
                try {
                    Node rootNode = removePropertySession.getRootNode().getNode(path);
                    for (int i = 0; i < NUM_NODES; i++) {
                        // wait for node to be created
                        while (!rootNode.hasNode("node" + i)) {
                            Thread.sleep(0, 50);
                        }
                        Node n = rootNode.getNode("node" + i);
                        // remove property
                        n.setProperty("foo", (Value) null);
                        log.info("removing property from node: node" + i);
                        n.save();
                        log.info("property removed from node: node" + i);
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
        };

        addNodeWorker.start();
        removePropertyWorker.start();
        addNodeWorker.join();
        removePropertyWorker.join();

        if (exceptions.size() > 0) {
            throw (Exception) exceptions.get(0);
        }
    }
}
