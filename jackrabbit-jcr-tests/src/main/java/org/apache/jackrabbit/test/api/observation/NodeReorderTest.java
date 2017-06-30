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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.observation.Event;

/**
 * Tests if {@link javax.jcr.Node#orderBefore(String, String)} operations trigger
 * the appropriate observation events.
 * <ul>
 * <li>{@code testroot} must allow orderable child nodes of type
 * <code>nodetype</code>, otherwise the test cases throw a
 * {@link NotExecutableException}. Some tests are only executed if the node
 * at <code>testroot</code> support same name sibling child nodes.
 * <li>{@code nodetype} node type that allows child nodes of the same type.
 * <li>{@code nodename1} child node name of type <code>nodetype</code>
 * <li>{@code nodename2} child node name of type <code>nodetype</code>
 * <li>{@code nodename3} child node name of type <code>nodetype</code>
 * </ul>
 */
public class NodeReorderTest extends AbstractObservationTest {

    /**
     * The key <code>srcChildRelPath</code> in the info map.
     */
    private static final String SRC_CHILD_REL_PATH = "srcChildRelPath";

    /**
     * The key <code>destChildRelPath</code> in the info map.
     */
    private static final String DEST_CHILD_REL_PATH = "destChildRelPath";

    private void doTestNodeReorder(List<Event> added, List<Event> removed, List<Event> moved)
            throws RepositoryException, NotExecutableException {
        if (!testRootNode.getDefinition().getDeclaringNodeType().hasOrderableChildNodes()) {
            throw new NotExecutableException("Node at '" + testRoot + "' does not support orderable child nodes.");
        }

        /**
         * Initial tree:
         *  + testroot
         *      + nodename1
         *      + nodename2
         *      + nodename3
         *
         * After reorder:
         *  + testroot
         *      + nodename1
         *      + nodename3
         *      + nodename2
         */
        testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.addNode(nodeName2, testNodeType);
        testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.getSession().save();
        EventResult listener = new EventResult(log);
        addEventListener(listener, Event.NODE_ADDED | Event.NODE_REMOVED | Event.NODE_MOVED);
        testRootNode.orderBefore(nodeName3, nodeName2);
        testRootNode.getSession().save();
        List<Event> events = Arrays.asList(listener.getEvents(DEFAULT_WAIT_TIMEOUT));
        for (Event e : events) {
            if (e.getType() == Event.NODE_ADDED) {
                added.add(e);
            } else if (e.getType() == Event.NODE_REMOVED) {
                removed.add(e);
            } else if (e.getType() == Event.NODE_MOVED) {
                moved.add(e);
            } else {
                fail("unexpected event type: " + e.getType());
            }
        }
        removeEventListener(listener);
    }

    /**
     * Tests if reordering a child node triggers a {@link Event#NODE_REMOVED}
     * and a {@link Event#NODE_ADDED} event.
     */
    public void testNodeReorderAddRemove() throws RepositoryException, NotExecutableException {

        List<Event> added = new ArrayList<Event>();
        List<Event> removed = new ArrayList<Event>();
        List<Event> moved = new ArrayList<Event>();

        doTestNodeReorder(added, removed, moved);

        // either
        // 1) nodename2 has been reordered to the end
        // or:
        // 2) nodename3 has been reordered before nodename2
        // that is, the following event sets are correct:
        // 1) nodename2:remove, nodename2:add
        // or:
        // 2) nodename3:remove, nodename3:add

        // if true, check for option 1)
        boolean reorderEnd = false;
        for (Event e : added) {
            if (e.getPath().endsWith(nodeName2)) {
                reorderEnd = true;
                break;
            }
        }

        if (reorderEnd) {
            checkNodeAdded(added, new String[] { nodeName2 }, null);
            checkNodeRemoved(removed, new String[] { nodeName2 }, null);
        } else {
            checkNodeAdded(added, new String[] { nodeName3 }, null);
            checkNodeRemoved(removed, new String[] { nodeName3 }, null);
        }
    }

    /**
     * Tests if reordering a child node triggers a {@link Event#NODE_MOVED}
     * event.
     */
    public void testNodeReorderMove() throws RepositoryException, NotExecutableException {

        List<Event> added = new ArrayList<Event>();
        List<Event> removed = new ArrayList<Event>();
        List<Event> moved = new ArrayList<Event>();

        doTestNodeReorder(added, removed, moved);

        checkNodeReordered(moved, nodeName3, nodeName3, nodeName2);
    }

    /**
     * Tests if reordering a child node triggers a {@link Event#NODE_REMOVED}
     * and a {@link Event#NODE_ADDED} event with same name siblings.
     */
    public void testNodeReorderSameName()
            throws RepositoryException, NotExecutableException {
        if (!testRootNode.getDefinition().getDeclaringNodeType().hasOrderableChildNodes()) {
            throw new NotExecutableException("Node at '" + testRoot + "' does not support orderable child nodes.");
        }

        /**
         * Initial tree:
         *  + testroot
         *      + nodename1[1]
         *      + nodename1[2]
         *      + nodename1[3]
         *
         * After reorder:
         *  + testroot
         *      + nodename1[1]
         *      + nodename1[2] (was 3)
         *      + nodename1[3] (was 2)
         */
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        if (!n.getDefinition().allowsSameNameSiblings()) {
            throw new NotExecutableException("Node at " + testRoot + " does not allow same name siblings with name " + nodeName1);
        }
        testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.getSession().save();
        EventResult addNodeListener = new EventResult(log);
        EventResult removeNodeListener = new EventResult(log);
        EventResult moveNodeListener = new EventResult(log);
        addEventListener(addNodeListener, Event.NODE_ADDED);
        addEventListener(removeNodeListener, Event.NODE_REMOVED);
        addEventListener(moveNodeListener, Event.NODE_MOVED);
        testRootNode.orderBefore(nodeName1 + "[3]", nodeName1 + "[2]");
        //testRootNode.orderBefore(nodeName1 + "[2]", null);
        testRootNode.getSession().save();
        Event[] added = addNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        Event[] removed = removeNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        Event[] moved = moveNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(addNodeListener);
        removeEventListener(removeNodeListener);
        removeEventListener(moveNodeListener);
        // either
        // 1) nodename1[2] has been reordered to the end
        // or:
        // 2) nodename1[3] has been reordered before nodename1[2]
        // that is, the following event sets are correct:
        // 1) nodename1[2]:remove, nodename1[3]:add
        // or:
        // 2) nodename1[3]:remove, nodename1[2]:add

        // if true, check for option 1)
        boolean reorderEnd = false;
        for (int i = 0; i < added.length; i++) {
            if (added[i].getPath().endsWith(nodeName1 + "[3]")) {
                reorderEnd = true;
                break;
            }
        }
        if (reorderEnd) {
            checkNodeAdded(added, new String[]{nodeName1 + "[3]"}, null);
            checkNodeRemoved(removed, new String[]{nodeName1 + "[2]"}, null);
            checkNodeReordered(moved, nodeName1 + "[2]", nodeName1 + "[3]", null);
        } else {
            checkNodeAdded(added, new String[]{nodeName1 + "[2]"}, null);
            checkNodeRemoved(removed, new String[]{nodeName1 + "[3]"}, null);
            checkNodeReordered(moved, nodeName1 + "[3]", nodeName1 + "[2]", nodeName1 + "[2]");
        }
    }

    /**
     * Tests if reordering a child node triggers a {@link Event#NODE_REMOVED}
     * and a {@link Event#NODE_ADDED} event with same name siblings. Furthermore
     * a node is removed in the same save scope.
     */
    public void testNodeReorderSameNameWithRemove()
            throws RepositoryException, NotExecutableException {
        if (!testRootNode.getDefinition().getDeclaringNodeType().hasOrderableChildNodes()) {
            throw new NotExecutableException("Node at '" + testRoot + "' does not support orderable child nodes.");
        }

        /**
         * Initial tree:
         *  + testroot
         *      + nodename1[1]
         *      + nodename2
         *      + nodename1[2]
         *      + nodename1[3]
         *      + nodename3
         *
         * After reorder:
         *  + testroot
         *      + nodename1[1]
         *      + nodename2
         *      + nodename1[2] (was 3)
         *      + nodename1[3] (was 2)
         */
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        if (!n.getDefinition().allowsSameNameSiblings()) {
            throw new NotExecutableException("Node at " + testRoot + " does not allow same name siblings with name " + nodeName1);
        }
        testRootNode.addNode(nodeName2, testNodeType);
        testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.addNode(nodeName3, testNodeType);
        testRootNode.getSession().save();
        EventResult addNodeListener = new EventResult(log);
        EventResult removeNodeListener = new EventResult(log);
        EventResult moveNodeListener = new EventResult(log);
        addEventListener(addNodeListener, Event.NODE_ADDED);
        addEventListener(removeNodeListener, Event.NODE_REMOVED);
        addEventListener(moveNodeListener, Event.NODE_MOVED);
        testRootNode.orderBefore(nodeName1 + "[2]", null);
        testRootNode.getNode(nodeName3).remove();
        testRootNode.getSession().save();
        Event[] added = addNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        Event[] removed = removeNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        Event[] moved = moveNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(addNodeListener);
        removeEventListener(removeNodeListener);
        removeEventListener(moveNodeListener);
        // either
        // 1) nodename1[2] has been reordered to the end
        // or:
        // 2) nodename1[3] has been reordered before nodename1[2]
        //
        // that is, the following event sets are correct:
        // 1) nodename1[2]:remove, nodename1[3]:add, nodename3:remove
        // or:
        // 2) nodename1[3]:remove, nodename1[2]:add, nodename3:remove

        // if true, check for option 1)
        boolean reorderEnd = false;
        for (int i = 0; i < added.length; i++) {
            if (added[i].getPath().endsWith(nodeName1 + "[3]")) {
                reorderEnd = true;
                break;
            }
        }
        if (reorderEnd) {
            checkNodeAdded(added, new String[]{nodeName1 + "[3]"}, null);
            checkNodeRemoved(removed, new String[]{nodeName1 + "[2]", nodeName3}, null);
            checkNodeReordered(moved, nodeName1 + "[2]", nodeName1 + "[3]", null);
        } else {
            checkNodeAdded(added, new String[]{nodeName1 + "[2]"}, null);
            checkNodeRemoved(removed, new String[]{nodeName1 + "[3]", nodeName3}, null);
            checkNodeReordered(moved, nodeName1 + "[3]", nodeName1 + "[2]", nodeName1 + "[2]");
        }
    }

    /**
     * Checks <code>Events</code> for paths. All <code>relPaths</code> are
     * relative to {@link #testRoot}.
     *
     * @param events the <code>Event</code>s.
     * @param src    the source child path where the node was reordered from.
     * @param dest   the destination child path where the node was reordered to.
     * @param before the destination child path where the node was reordered before.
     * @throws RepositoryException if an error occurs while retrieving the nodes
     *                             from event instances.
     */
    protected void checkNodeReordered(Event[] events, String src,
                                      String dest, String before)
            throws RepositoryException {
        checkNodes(events, new String[]{dest}, null, Event.NODE_MOVED);
        assertEquals("Wrong number of events", 1, events.length);
        Map<?, ?> info = events[0].getInfo();
        checkInfoEntry(info, SRC_CHILD_REL_PATH, src);
        checkInfoEntry(info, DEST_CHILD_REL_PATH, before);
    }

    protected void checkNodeReordered(List<Event> events, String src, String dest, String before)
            throws RepositoryException {
        checkNodes(events.toArray(new Event[0]), new String[] { dest }, null, Event.NODE_MOVED);
        assertEquals("Wrong number of events", 1, events.size());
        Map<?, ?> info = events.get(0).getInfo();
        checkInfoEntry(info, SRC_CHILD_REL_PATH, src);
        checkInfoEntry(info, DEST_CHILD_REL_PATH, before);
    }

    /**
     * Checks if the info map contains the given <code>key</code> with the
     * <code>expected</code> value.
     *
     * @param info the event info map.
     * @param key the name of the key.
     * @param expected the expected value.
     */
    protected void checkInfoEntry(Map<?, ?> info, String key, String expected) {
        assertTrue("Missing event info key: " + key, info.containsKey(key));
        assertEquals("Wrong event info value for: " + key,
                expected, (String) info.get(key));
    }

    protected void checkNodeAdded(List<Event> events, String[] requiredRelPaths, String[] optionalRelPaths)
            throws RepositoryException {
        checkNodes(events.toArray(new Event[0]), requiredRelPaths, optionalRelPaths, Event.NODE_ADDED);
    }

    protected void checkNodeRemoved(List<Event> events, String[] requiredRelPaths, String[] optionalRelPaths)
            throws RepositoryException {
        checkNodes(events.toArray(new Event[0]), requiredRelPaths, optionalRelPaths, Event.NODE_REMOVED);
    }

}
