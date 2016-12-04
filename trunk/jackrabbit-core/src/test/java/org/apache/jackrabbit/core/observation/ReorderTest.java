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
package org.apache.jackrabbit.core.observation;

import org.apache.jackrabbit.test.api.observation.AbstractObservationTest;
import org.apache.jackrabbit.test.api.observation.EventResult;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.observation.Event;

/**
 * Tests implementation specific aspects of the observation manager.
 */
public class ReorderTest extends AbstractObservationTest {

    /**
     * Tests if reordering a child node triggers a {@link javax.jcr.observation.Event#NODE_REMOVED}
     * and a {@link javax.jcr.observation.Event#NODE_ADDED} event with same name siblings. Furthermore
     * a node is removed in the same save scope.
     * <p>
     * Because of the one reorder operation, three nodes change their index. And
     * actually four nodes change their context position. The minimal events
     * that may be triggered only includes one pair of remove/add node events
     * (plus the additional event for the removed node).
     */
    public void testNodeReorderComplex()
            throws RepositoryException, NotExecutableException {
        if (!testRootNode.getDefinition().getDeclaringNodeType().hasOrderableChildNodes()) {
            throw new NotExecutableException("Node at '" + testRoot + "' does not support orderable child nodes.");
        }

        /**
         * Initial tree:
         *  + testroot
         *      + nodename1[1]
         *      + nodename1[2]
         *      + nodename2
         *      + nodename1[3]
         *      + nodename1[4]
         *      + nodename3
         *
         * After reorder:
         *  + testroot
         *      + nodename1[1]
         *      + nodename2
         *      + nodename1[2] (was 3)
         *      + nodename1[3] (was 4)
         *      + nodename1[4] (was 2)
         */
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        if (!n.getDefinition().allowsSameNameSiblings()) {
            throw new NotExecutableException("Node at " + testRoot + " does not allow same name siblings with name " + nodeName1);
        }
        testRootNode.addNode(nodeName1, testNodeType);
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
        removeEventListener(addNodeListener);
        removeEventListener(removeNodeListener);
        Event[] added = addNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        Event[] removed = removeNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        // not deterministic, there exist various re-order seqences. the minimal
        // is:
        // nodename1[2] has been reordered to the end + nodeName3 has been removed
        checkNodeAdded(added, new String[]{nodeName1 + "[4]"}, null);
        checkNodeRemoved(removed, new String[]{nodeName1 + "[2]", nodeName3}, null);
    }
}
