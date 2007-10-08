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

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.observation.Event;
import java.util.List;
import java.util.ArrayList;

/**
 * Tests if locking a node triggers property added events for jcr:lockOwner
 * and jcr:lockIsDeep.
 * <p/>
 * Configuration requirements are:<br/>
 * The {@link #testRoot} must allow child nodes of type {@link #testNodeType}.
 * The child node that is created will be named {@link #nodeName1}. The node
 * type {@link #testNodeType} must either have mix:lockable as one of its
 * supertypes or must allow to add mix:lockable as mixin.
 *
 * @test
 * @sources LockingTest.java
 * @executeClass org.apache.jackrabbit.test.api.observation.LockingTest
 * @keywords observation locking
 */
public class LockingTest extends AbstractObservationTest {

    /**
     * Tests if locking a node triggers property added events for the properties
     * jcr:lockOwner and jcr:lockIsDeep.
     */
    public void testAddLockToNode() throws RepositoryException {
        Node lockable = createLockable(nodeName1, testNodeType);
        testRootNode.save();
        EventResult result = new EventResult(log);
        addEventListener(result, Event.PROPERTY_ADDED);

        // now lock node. no save needed
        lockable.lock(false,true);

        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);

        assertEquals("Wrong number of events.", 2, events.length);
        for (int i = 0; i < events.length; i++) {
            assertEquals("Wrong type of event.", Event.PROPERTY_ADDED, events[i].getType());
        }
        List paths = new ArrayList();
        for (int i = 0; i < events.length; i++) {
            paths.add(events[i].getPath());
        }
        String lockOwnerPath = testRoot + "/" + nodeName1 + "/" + jcrLockOwner;
        assertTrue("No event created for jcr:lockOwner", paths.contains(lockOwnerPath));
        String lockIsDeepPath = testRoot + "/" + nodeName1 + "/" + jcrlockIsDeep;
        assertTrue("No event created for jcr:lockIsDeep", paths.contains(lockIsDeepPath));

        lockable.unlock();
    }

    /**
     * Tests if unlocking a node triggers property removed events for the
     * properties jcr:lockOwner and jcr:lockIsDeep.
     */
    public void testRemoveLockFromNode() throws RepositoryException {
        Node lockable = createLockable(nodeName1, testNodeType);
        testRootNode.save();
        // lock the node
        lockable.lock(false, true);

        EventResult result = new EventResult(log);
        addEventListener(result, Event.PROPERTY_REMOVED);
        lockable.unlock();

        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        assertEquals("Wrong number of events.", 2, events.length);
        for (int i = 0; i < events.length; i++) {
            assertEquals("Wrong type of event.", Event.PROPERTY_REMOVED, events[i].getType());
        }
        List paths = new ArrayList();
        for (int i = 0; i < events.length; i++) {
            paths.add(events[i].getPath());
        }
        String lockOwnerPath = testRoot + "/" + nodeName1 + "/" + jcrLockOwner;
        assertTrue("No event created for jcr:lockOwner", paths.contains(lockOwnerPath));
        String lockIsDeepPath = testRoot + "/" + nodeName1 + "/" + jcrlockIsDeep;
        assertTrue("No event created for jcr:lockIsDeep", paths.contains(lockIsDeepPath));
    }

    /**
     * Creates a child node under {@link #testRoot} with name <code>nodeName</code>
     * and node type <code>nodeType</code>.
     * If <code>nodeType</code> is not of type mix:lockable the mixin is added.
     * @param nodeName the name of the new node.
     * @param nodeType the node type of the new node.
     * @return the lockable node
     */
    private Node createLockable(String nodeName, String nodeType)
            throws RepositoryException {
        Node n = testRootNode.addNode(nodeName, nodeType);
        if (needsMixin(n, mixLockable)) {
            n.addMixin(mixLockable);
        }
        return n;
    }
}