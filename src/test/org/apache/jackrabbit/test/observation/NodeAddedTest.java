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
import javax.jcr.observation.Event;
import javax.jcr.observation.EventType;

/**
 * Test cases for {@link javax.jcr.observation.EventType#CHILD_NODE_ADDED
 * CHILD_NODE_ADDED} events.
 *
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
public class NodeAddedTest extends AbstractObservationTest {

    public void testSingleNodeAdded() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, EventType.CHILD_NODE_ADDED);
        testRoot.addNode("foo", NT_UNSTRUCTURED);
        testRoot.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkNodeAdded(events, new String[] { "foo" });
    }

    public void testMultipleNodeAdded1() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, EventType.CHILD_NODE_ADDED);
        testRoot.addNode("foo", NT_UNSTRUCTURED);
        testRoot.addNode("bar", NT_UNSTRUCTURED);
        testRoot.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkNodeAdded(events, new String[] { "foo", "bar" });
    }

    public void testMultipleNodeAdded2() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, EventType.CHILD_NODE_ADDED);
        Node foo = testRoot.addNode("foo", NT_UNSTRUCTURED);
        foo.addNode("bar", NT_UNSTRUCTURED);
        testRoot.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkNodeAdded(events, new String[] { "foo", "foo/bar" });
    }

    public void testTransientNodeAddedRemoved() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, EventType.CHILD_NODE_ADDED);
        Node foo = testRoot.addNode("foo", NT_UNSTRUCTURED);
        foo.addNode("bar", NT_UNSTRUCTURED);
        foo.remove("bar");
        testRoot.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkNodeAdded(events, new String[] { "foo" });
    }

    /*
    NOT POSSIBLE YET. WAIT FOR MIXIN IMPLEMENTATION.

    public void testMultiPathSameNameAdded() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, EventType.CHILD_NODE_ADDED);
        Node foo = testRoot.addNode("foo", NT_UNSTRUCTURED);
        Node bar = testRoot.addNode("bar", NT_UNSTRUCTURED);
        Node bla = foo.addNode("bla", NT_UNSTRUCTURED);
        bla.addMixin(MIX_REFERENCABLE);
        bar.addNode(bla, "bla");
        testRoot.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkNodeAdded(events, new String[] { "foo", "bar", "foo/bla", "bar/bla"});
    }
    */
}
