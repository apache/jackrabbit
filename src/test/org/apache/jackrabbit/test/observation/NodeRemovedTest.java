/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
 * Test cases for {@link javax.jcr.observation.Event#NODE_REMOVED
 * CHILD_NODE_REMOVED} events.
 */
public class NodeRemovedTest extends AbstractObservationTest {

    public void testSingleNodeRemoved() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, Event.NODE_REMOVED);
        Node foo = testRootNode.addNode("foo");
        testRootNode.save();
        foo.remove();
        testRootNode.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkNodeRemoved(events, new String[]{"foo"});
    }

    public void testMultiNodesRemoved() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, Event.NODE_REMOVED);
        Node foo = testRootNode.addNode("foo");
        foo.addNode("bar");
        testRootNode.save();
        foo.remove();
        testRootNode.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkNodeRemoved(events, new String[]{"foo", "foo/bar"});
    }

    public void testMultiNodesRemovedWithRemaining() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, Event.NODE_REMOVED);
        Node foo = testRootNode.addNode("foo");
        testRootNode.addNode("foobar");
        foo.addNode("bar");
        testRootNode.save();
        foo.remove();
        testRootNode.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkNodeRemoved(events, new String[]{"foo", "foo/bar"});
    }

}
