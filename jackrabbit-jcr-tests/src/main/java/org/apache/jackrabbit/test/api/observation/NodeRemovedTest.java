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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

/**
 * Test cases for {@link javax.jcr.observation.Event#NODE_REMOVED} events.
 * <p/>
 * Configuration requirements are:<br/>
 * The {@link #testRoot} must allow child nodes of type {@link #testNodeType}.
 * The child nodes that are created will be named {@link #nodeName1} and
 * {@link #nodeName2}. Furthermore {@link #testNodeType} must allow to add
 * child nodes of the same type ({@link #testNodeType}).
 *
 * @test
 * @sources NodeRemovedTest.java
 * @executeClass org.apache.jackrabbit.test.api.observation.NodeRemovedTest
 * @keywords observation
 */
public class NodeRemovedTest extends AbstractObservationTest {

    /**
     * Tests if a {@link javax.jcr.observation.Event#NODE_REMOVED} is triggered
     * when a single node is removed.
     */
    public void testSingleNodeRemoved() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, Event.NODE_REMOVED);
        Node foo = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        foo.remove();
        testRootNode.save();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        checkNodeRemoved(events, new String[]{nodeName1}, null);
    }

    /**
     * Tests if {@link javax.jcr.observation.Event#NODE_REMOVED} events are
     * triggered when multiple nodes are removed.
     */
    public void testMultiNodesRemoved() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, Event.NODE_REMOVED);
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.addNode(nodeName2, testNodeType);
        testRootNode.save();
        n1.remove();
        testRootNode.save();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        checkNodeRemoved(events, new String[]{nodeName1, nodeName1 + "/" + nodeName2}, null);
    }

}
