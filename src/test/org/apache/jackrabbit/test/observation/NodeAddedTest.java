/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.test.observation;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

/**
 * Test cases for {@link javax.jcr.observation.Event.NODE_ADDED
 * CHILD_NODE_ADDED} events.
 */
public class NodeAddedTest extends AbstractObservationTest {

    public void testSingleNodeAdded() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, Event.NODE_ADDED);
        testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkNodeAdded(events, new String[]{nodeName1});
    }

    public void testMultipleNodeAdded1() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, Event.NODE_ADDED);
        testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.addNode(nodeName2, testNodeType);
        testRootNode.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkNodeAdded(events, new String[]{nodeName1, nodeName2});
    }

    public void testMultipleNodeAdded2() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, Event.NODE_ADDED);
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.addNode(nodeName2, testNodeType);
        testRootNode.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkNodeAdded(events, new String[]{nodeName1, nodeName1 + "/" + nodeName2});
    }

    public void testTransientNodeAddedRemoved() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, Event.NODE_ADDED);
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = n1.addNode(nodeName2, testNodeType);
        n2.remove();
        testRootNode.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkNodeAdded(events, new String[]{nodeName1});
    }

    /*
    NOT POSSIBLE YET. WAIT FOR MIXIN IMPLEMENTATION.

    public void testMultiPathSameNameAdded() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, EventType.CHILD_NODE_ADDED);
        Node foo = testRootNode.addNode("foo", NT_UNSTRUCTURED);
        Node bar = testRootNode.addNode("bar", NT_UNSTRUCTURED);
        Node bla = foo.addNode("bla", NT_UNSTRUCTURED);
        bla.addMixin(MIX_REFERENCABLE);
        bar.addNode(bla, "bla");
        testRootNode.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkNodeAdded(events, new String[] { "foo", "bar", "foo/bla", "bar/bla"});
    }
    */
}
