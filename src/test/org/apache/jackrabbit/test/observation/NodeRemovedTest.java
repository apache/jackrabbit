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

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.observation.EventType;
import javax.jcr.observation.Event;

/**
 * Test cases for {@link javax.jcr.observation.EventType#CHILD_NODE_REMOVED
 * CHILD_NODE_REMOVED} events.
 *
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
public class NodeRemovedTest extends AbstractObservationTest {

    public void testSingleNodeRemoved() throws RepositoryException {
        EventResult result = new EventResult(log);
	addEventListener(result, EventType.CHILD_NODE_REMOVED);
        testRoot.addNode("foo", NT_UNSTRUCTURED);
        testRoot.save();
	testRoot.remove("foo");
	testRoot.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkNodeRemoved(events, new String[] { "foo" });
    }

    public void testMultiNodesRemoved() throws RepositoryException {
        EventResult result = new EventResult(log);
	addEventListener(result, EventType.CHILD_NODE_REMOVED);
        Node foo = testRoot.addNode("foo", NT_UNSTRUCTURED);
	foo.addNode("bar", NT_UNSTRUCTURED);
        testRoot.save();
	testRoot.remove("foo");
	testRoot.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkNodeRemoved(events, new String[] { "foo", "foo/bar" });
    }

    public void testMultiNodesRemovedWithRemaining() throws RepositoryException {
	EventResult result = new EventResult(log);
	addEventListener(result, EventType.CHILD_NODE_REMOVED);
	Node foo = testRoot.addNode("foo", NT_UNSTRUCTURED);
	testRoot.addNode("foobar", NT_UNSTRUCTURED);
	foo.addNode("bar", NT_UNSTRUCTURED);
	testRoot.save();
	testRoot.remove("foo");
	testRoot.save();
	removeEventListener(result);
	Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
	checkNodeRemoved(events, new String[] { "foo", "foo/bar" });
    }

}
