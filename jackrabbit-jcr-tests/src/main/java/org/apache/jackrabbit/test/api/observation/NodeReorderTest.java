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

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.observation.Event;

/**
 * Tests if {@link javax.jcr.Node#orderBefore(String, String)} operations trigger
 * the appropriate observation events.
 * <p/>
 * @tck.config testroot must allow orderable child nodes of type
 * <code>nodetype</code>, otherwise the test cases throw a
 * {@link NotExecutableException}. Some tests are only executed if the node
 * at <code>testroot</code> support same name sibling child nodes.
 * @tck.config nodetype node type that allows child nodes of the same type.
 * @tck.config nodename1 child node name of type <code>nodetype</code>
 * @tck.config nodename2 child node name of type <code>nodetype</code>
 * @tck.config nodename3 child node name of type <code>nodetype</code>
 *
 * @test
 * @sources NodeReorderTest.java
 * @executeClass org.apache.jackrabbit.test.api.observation.NodeReorderTest
 * @keywords observation
 */
public class NodeReorderTest extends AbstractObservationTest {

    /**
     * Tests if reordering a child node triggers a {@link Event#NODE_REMOVED}
     * and a {@link Event#NODE_ADDED} event.
     */
    public void testNodeReorder()
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
        testRootNode.save();
        EventResult addNodeListener = new EventResult(log);
        EventResult removeNodeListener = new EventResult(log);
        addEventListener(addNodeListener, Event.NODE_ADDED);
        addEventListener(removeNodeListener, Event.NODE_REMOVED);
        testRootNode.orderBefore(nodeName3, nodeName2);
        testRootNode.save();
        Event[] added = addNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        Event[] removed = removeNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(addNodeListener);
        removeEventListener(removeNodeListener);
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
        for (int i = 0; i < added.length; i++) {
            if (added[i].getPath().endsWith(nodeName2)) {
                reorderEnd = true;
                break;
            }
        }
        if (reorderEnd) {
            checkNodeAdded(added, new String[]{nodeName2}, null);
            checkNodeRemoved(removed, new String[]{nodeName2}, null);
        } else {
            checkNodeAdded(added, new String[]{nodeName3}, null);
            checkNodeRemoved(removed, new String[]{nodeName3}, null);
        }
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
        testRootNode.save();
        EventResult addNodeListener = new EventResult(log);
        EventResult removeNodeListener = new EventResult(log);
        addEventListener(addNodeListener, Event.NODE_ADDED);
        addEventListener(removeNodeListener, Event.NODE_REMOVED);
        testRootNode.orderBefore(nodeName1 + "[3]", nodeName1 + "[2]");
        //testRootNode.orderBefore(nodeName1 + "[2]", null);
        testRootNode.save();
        Event[] added = addNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        Event[] removed = removeNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(addNodeListener);
        removeEventListener(removeNodeListener);
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
        } else {
            checkNodeAdded(added, new String[]{nodeName1 + "[2]"}, null);
            checkNodeRemoved(removed, new String[]{nodeName1 + "[3]"}, null);
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
        testRootNode.save();
        EventResult addNodeListener = new EventResult(log);
        EventResult removeNodeListener = new EventResult(log);
        addEventListener(addNodeListener, Event.NODE_ADDED);
        addEventListener(removeNodeListener, Event.NODE_REMOVED);
        testRootNode.orderBefore(nodeName1 + "[2]", null);
        testRootNode.getNode(nodeName3).remove();
        testRootNode.save();
        Event[] added = addNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        Event[] removed = removeNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(addNodeListener);
        removeEventListener(removeNodeListener);
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
        } else {
            checkNodeAdded(added, new String[]{nodeName1 + "[2]"}, null);
            checkNodeRemoved(removed, new String[]{nodeName1 + "[3]", nodeName3}, null);
        }
    }
}
