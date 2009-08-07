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
 * Test cases for {@link javax.jcr.observation.Event#NODE_ADDED} events.
 * <p/>
 * Configuration requirements are:<br/>
 * The {@link #testRoot} must allow child nodes of type {@link #testNodeType}.
 * The child nodes that are created will be named {@link #nodeName1} and
 * {@link #nodeName2}. Furthermore {@link #testNodeType} must allow to add
 * child nodes of the same type ({@link #testNodeType}).
 *
 * @test
 * @sources NodeAddedTest.java
 * @executeClass org.apache.jackrabbit.test.api.observation.NodeAddedTest
 * @keywords observation
 */
public class NodeAddedTest extends AbstractObservationTest {

    /**
     * Tests if {@link javax.jcr.observation.Event#NODE_ADDED} is triggerd
     * when a single node is added.
     */
    public void testSingleNodeAdded() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, Event.NODE_ADDED);
        testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        checkNodeAdded(events, new String[]{nodeName1}, null);
    }

    /**
     * Tests if {@link javax.jcr.observation.Event#NODE_ADDED} is triggered
     * for multiple nodes on the same level.
     */
    public void testMultipleNodeAdded1() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, Event.NODE_ADDED);
        testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.addNode(nodeName2, testNodeType);
        testRootNode.save();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        checkNodeAdded(events, new String[]{nodeName1, nodeName2}, null);
    }

    /**
     * Tests if {@link javax.jcr.observation.Event#NODE_ADDED} is triggered
     * when nodes are created on multiple levels.
     */
    public void testMultipleNodeAdded2() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, Event.NODE_ADDED);
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.addNode(nodeName2, testNodeType);
        testRootNode.save();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        checkNodeAdded(events, new String[]{nodeName1, nodeName1 + "/" + nodeName2}, null);
    }

    /**
     * Tests if events are only created for changes that are persisted.
     */
    public void testTransientNodeAddedRemoved() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, Event.NODE_ADDED);
        testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        n2.remove();
        testRootNode.save();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        checkNodeAdded(events, new String[]{nodeName1}, null);
    }
}
