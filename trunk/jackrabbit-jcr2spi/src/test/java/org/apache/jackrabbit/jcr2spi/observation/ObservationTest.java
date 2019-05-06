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
package org.apache.jackrabbit.jcr2spi.observation;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObservationTest extends AbstractJCRTest {
    private static Logger log = LoggerFactory.getLogger(ObservationTest.class);

    private Node testNode;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testNode = testRootNode.addNode(nodeName1);
        testRootNode.save();
    }

    @Override
    protected void tearDown() throws Exception {
        testNode = null;
        super.tearDown();
    }

    interface WaitableEventListener extends EventListener {
        public void waitForEvent(int timeout) throws InterruptedException, RepositoryException;
    }

    /**
     * Check whether an item with the path of an add node event exists.
     * Regression test for JCR-2293.
     * @throws RepositoryException
     * @throws InterruptedException
     */
    public void testJCR_2293() throws RepositoryException, InterruptedException {
        final String parentPath = testNode.getPath();
        final String folderName = "folder_" + System.currentTimeMillis();
        final Session session = getHelper().getReadWriteSession();

        final Session session2 = getHelper().getReadOnlySession();
        session2.getItem(parentPath);  // Don't remove. See JCR-2293.

        WaitableEventListener eventListener = new WaitableEventListener() {
            private RepositoryException failure;
            private boolean done;

            public synchronized void onEvent(final EventIterator events) {
                try {
                    while (events.hasNext()) {
                        Event event = events.nextEvent();
                        Item item2 = session2.getItem(event.getPath());
                        assertEquals(parentPath + "/" + folderName, item2.getPath());
                    }
                }
                catch (RepositoryException e) {
                    failure = e;
                }
                finally {
                    done = true;
                    notifyAll();
                }
            }

            public synchronized void waitForEvent(int timeout) throws InterruptedException, RepositoryException {
                if (!done) {
                    wait(timeout);
                }
                if (!done) {
                    fail("Event listener not called");
                }
                if (failure != null) {
                    throw failure;
                }
            }
        };

        session2.getWorkspace().getObservationManager()
                .addEventListener(eventListener, Event.NODE_ADDED,
                                                 parentPath, true, null, null, false);

        Node parent = (Node) session.getItem(parentPath);
        Node toDelete = parent.addNode(folderName, "nt:folder");
        parent.save();

        try {
            eventListener.waitForEvent(60000);
        }
        finally {
            toDelete.remove();
            parent.save();
            assertFalse(parent.hasNode(folderName));
        }
    }
}