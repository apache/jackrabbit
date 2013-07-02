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
package org.apache.jackrabbit.core.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test case for JCR-3617.
 */
public class CachingHierarchyManagerConsistencyTest extends AbstractJCRTest {

    private static final Logger log = LoggerFactory.getLogger(CachingHierarchyManagerConsistencyTest.class);

    private static final int TEST_DURATION = 10; // seconds
    private static final int NUM_LISTENERS = 10;
    private static final int ALL_EVENTS = Event.NODE_ADDED | Event.NODE_MOVED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;
    private static final String TEST_PATH = "/my/test/path";

    public void testObservation() throws Exception {
        final List<Exception> exceptions = new ArrayList<Exception>();
        Thread writer = new Thread(new Runnable() {
            public void run() {
                try {
                    long end = System.currentTimeMillis() + TEST_DURATION * 1000;
                    Session s = getHelper().getSuperuserSession();
                    try {
                        log.info("Starting to replace nodes");
                        int i = 0;
                        while (System.currentTimeMillis() < end) {
                            replaceNodes(s, i++);
                        }
                    } finally {
                        s.logout();
                    }
                } catch (RepositoryException e) {
                    exceptions.add(e);
                }
            }
        });
        List<EventListener> listeners = new ArrayList<EventListener>();
        for (int i = 0; i < NUM_LISTENERS; i++) {
            final Session session = getHelper().getSuperuserSession();
            listeners.add(new EventListener() {
                public void onEvent(EventIterator events) {
                    while (events.hasNext()) {
                        Event event = events.nextEvent();
                        String path = "n/a";
                        try {
                            if (event.getType() == Event.NODE_ADDED
                                    || event.getType() == Event.PROPERTY_ADDED) {
                                path = event.getPath();
                                session.getItem(path);
                            }
                        } catch (PathNotFoundException e) {
                            // ignore
                        } catch (RepositoryException e) {
                            log.error(e.toString() + " Unable to get item with path: " + path);
                            exceptions.add(e);
                        }
                    }
                }
            });
        }
        for (EventListener listener : listeners) {
            superuser.getWorkspace().getObservationManager().addEventListener(
                    listener, ALL_EVENTS, "/", true, null, null, false);
        }

        writer.start();
        writer.join();

        for (EventListener listener : listeners) {
            superuser.getWorkspace().getObservationManager().removeEventListener(listener);
        }

        log.info("" + exceptions.size() + " exception(s) occurred.");
        if (!exceptions.isEmpty()) {
            throw exceptions.get(0);
        }
    }

    private void replaceNodes(Session session, int i) throws RepositoryException {
        String nodeName = "node-" + (i % 100);
        Node root = JcrUtils.getOrCreateByPath(testRoot + TEST_PATH, ntUnstructured, session);
        String uuid = UUID.randomUUID().toString();
        if (root.hasNode(nodeName)) {
            Node n = root.getNode(nodeName);
            uuid = n.getIdentifier();
            n.remove();
        }
        Node n = ((NodeImpl) root).addNodeWithUuid(nodeName, ntUnstructured, uuid);
        n.addMixin("mix:referenceable");
        n.addNode("foo").addNode("bar");
        n.addNode("qux");
        session.save();
    }
}
