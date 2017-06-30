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
package org.apache.jackrabbit.test.api.observation;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.observation.Event;

/**
 * Tests if {@link javax.jcr.Session#move} operations trigger the appropriate
 * observation events.
 * <p>
 * Configuration requirements:
 * <p>
 * The {@link #testRoot} must allow child nodes of type {@link #testNodeType}.
 * The child nodes that are created will be named {@link #nodeName1},
 * {@link #nodeName2}, {@link #nodeName3} and {@link #nodeName4}. Furthermore
 * {@link #testNodeType} must allow to add child nodes of the same type
 * ({@link #testNodeType}).
 *
 */
public class NodeMovedTest extends AbstractObservationTest {

    /**
     * The key <code>srcAbsPath</code> in the info map.
     */
    private static final String SRC_ABS_PATH = "srcAbsPath";

    /**
     * The key <code>destAbsPath</code> in the info map.
     */
    private static final String DEST_ABS_PATH = "destAbsPath";

    /**
     * Tests if node removed and node added event is triggered when a tree
     * is moved.
     */
    public void testMoveTree() throws RepositoryException {
        /**
         * Initial tree:
         *  + testroot
         *      + nodename1
         *          + nodename2
         *
         * After move:
         *  + testroot
         *      + nodename3
         *          + nodename2
         */

        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.addNode(nodeName2, testNodeType);
        testRootNode.getSession().save();
        EventResult addNodeListener = new EventResult(log);
        EventResult removeNodeListener = new EventResult(log);
        EventResult moveNodeListener = new EventResult(log);
        addEventListener(addNodeListener, Event.NODE_ADDED);
        addEventListener(removeNodeListener, Event.NODE_REMOVED);
        addEventListener(moveNodeListener, Event.NODE_MOVED);
        superuser.move(n1.getPath(), testRoot + "/" + nodeName3);
        testRootNode.getSession().save();
        Event[] added = addNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        Event[] removed = removeNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        Event[] moved = moveNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(addNodeListener);
        removeEventListener(removeNodeListener);
        removeEventListener(moveNodeListener);
        checkNodeAdded(added, new String[]{nodeName3}, new String[]{nodeName3 + "/" + nodeName2});
        checkNodeRemoved(removed, new String[]{nodeName1}, new String[]{nodeName1 + "/" + nodeName2});
        checkNodeMoved(moved, nodeName1, nodeName3);
    }

    /**
     * Tests if node removed and node added event is triggered when a node
     * is moved.
     */
    public void testMoveNode() throws RepositoryException {
        /**
         * Initial tree:
         *  + testroot
         *      + nodename1
         *          + nodename2
         *
         * After move:
         *  + testroot
         *      + nodename1
         *      + nodename2
         */

        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = n1.addNode(nodeName2, testNodeType);
        testRootNode.getSession().save();
        EventResult addNodeListener = new EventResult(log);
        EventResult removeNodeListener = new EventResult(log);
        EventResult moveNodeListener = new EventResult(log);
        addEventListener(addNodeListener, Event.NODE_ADDED);
        addEventListener(removeNodeListener, Event.NODE_REMOVED);
        addEventListener(moveNodeListener, Event.NODE_MOVED);
        superuser.move(n2.getPath(), testRoot + "/" + nodeName2);
        testRootNode.getSession().save();
        Event[] added = addNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        Event[] removed = removeNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        Event[] moved = moveNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(addNodeListener);
        removeEventListener(removeNodeListener);
        removeEventListener(moveNodeListener);
        checkNodeAdded(added, new String[]{nodeName2}, null);
        checkNodeRemoved(removed, new String[]{nodeName1 + "/" + nodeName2}, null);
        checkNodeMoved(moved, nodeName1 + "/" + nodeName2, nodeName2);
    }

    /**
     * Tests if a node moved triggers the correct events when the former parent
     * node is removed at the same time.
     */
    public void testMoveWithRemove() throws RepositoryException {
        /**
         * Initial tree:
         *  + testroot
         *      + nodename1
         *          + nodename2
         *      + nodename3
         *
         * After move and remove:
         *  + testroot
         *      + nodename3
         *          + nodename2
         */
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = n1.addNode(nodeName2, testNodeType);
        Node n3 = testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.getSession().save();
        EventResult addNodeListener = new EventResult(log);
        EventResult removeNodeListener = new EventResult(log);
        EventResult moveNodeListener = new EventResult(log);
        addEventListener(addNodeListener, Event.NODE_ADDED);
        addEventListener(removeNodeListener, Event.NODE_REMOVED);
        addEventListener(moveNodeListener, Event.NODE_MOVED);
        // move n2
        superuser.move(n2.getPath(), n3.getPath() + "/" + nodeName2);
        // remove n1
        n1.remove();
        testRootNode.getSession().save();
        Event[] added = addNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        Event[] removed = removeNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        Event[] moved = moveNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(addNodeListener);
        removeEventListener(removeNodeListener);
        removeEventListener(moveNodeListener);
        checkNodeAdded(added, new String[]{nodeName3 + "/" + nodeName2}, null);
        checkNodeRemoved(removed, new String[]{nodeName1 + "/" + nodeName2, nodeName1}, null);
        checkNodeMoved(moved, nodeName1 + "/" + nodeName2, nodeName3 + "/" + nodeName2);
    }

    /**
     * TODO: move to base class once JSR 283 is final
     * Checks <code>Events</code> for paths. All <code>relPaths</code> are
     * relative to {@link #testRoot}.
     *
     * @param events the <code>Event</code>s.
     * @param from   the source path where the node was moved from.
     * @param to     the destination path where the node was moved to.
     * @throws RepositoryException if an error occurs while retrieving the nodes
     *                             from event instances.
     */
    protected void checkNodeMoved(Event[] events, String from, String to)
            throws RepositoryException {
        checkNodes(events, new String[]{to}, null, Event.NODE_MOVED);
        assertEquals("Wrong number of events", 1, events.length);
        Map<?, ?> info = events[0].getInfo();
        checkInfoEntry(info, SRC_ABS_PATH, testRoot + "/" + from);
        checkInfoEntry(info, DEST_ABS_PATH, testRoot + "/" + to);
    }

    /**
     * TODO: move to base class once JSR 283 is final
     * Checks if the info map contains the given <code>key</code> with the
     * <code>expected</code> value.
     *
     * @param info the event info map.
     * @param key the name of the key.
     * @param expected the expected value.
     */
    protected void checkInfoEntry(Map<?, ?> info, String key, String expected) {
        String value = (String) info.get(key);
        assertNotNull("Missing event info key: " + key, value);
        assertEquals("Wrong event info value for: " + key, expected, value);
    }
}
