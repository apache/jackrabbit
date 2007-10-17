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

/**
 * Tests if workspace operations trigger the appropriate observation events.
 *
 * <p/>
 * Configuration requirements are:<br/>
 * The {@link #testRoot} must allow child nodes of type {@link #testNodeType}.
 * The child nodes that are created will be named {@link #nodeName1},
 * {@link #nodeName2}, {@link #nodeName3} and {@link #nodeName4}. Furthermore
 * {@link #testNodeType} must allow to add child nodes of the same type
 * ({@link #testNodeType}).
 *
 * @test
 * @sources WorkspaceOperationTest.java
 * @executeClass org.apache.jackrabbit.test.api.observation.WorkspaceOperationTest
 * @keywords observation
 */
public class WorkspaceOperationTest extends AbstractObservationTest {

    /**
     * Tests if {@link javax.jcr.Workspace#copy(String, String)} triggers
     * the correct events.
     */
    public void testCopy() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.addNode(nodeName2, testNodeType);
        testRootNode.save();
        EventResult listener = new EventResult(log);
        addEventListener(listener, Event.NODE_ADDED);
        superuser.getWorkspace().copy(testRoot + "/" + nodeName1, testRoot + "/" + nodeName3);
        Event[] events = listener.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(listener);
        checkNodeAdded(events, new String[]{nodeName3, nodeName3 + "/" + nodeName2}, null);
    }

    /**
     * Tests if {@link javax.jcr.Workspace#move(String, String)} triggers the
     * correct events.
     */
    public void testRename() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        n1.addNode(nodeName2, testNodeType);
        testRootNode.save();
        EventResult addNodeListener = new EventResult(log);
        EventResult removeNodeListener = new EventResult(log);
        addEventListener(addNodeListener, Event.NODE_ADDED);
        addEventListener(removeNodeListener, Event.NODE_REMOVED);
        superuser.getWorkspace().move(n1.getPath(), testRoot + "/" + nodeName3);
        testRootNode.save();
        Event[] added = addNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        Event[] removed = removeNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(addNodeListener);
        removeEventListener(removeNodeListener);
        checkNodeAdded(added, new String[]{nodeName3}, new String[]{nodeName3 + "/" + nodeName2});
        checkNodeRemoved(removed, new String[]{nodeName1}, new String[]{nodeName1 + "/" + nodeName2});
    }

    /**
     * Tests if {@link javax.jcr.Workspace#move(String, String)} triggers the
     * correct events.
     */
    public void testMove() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        Node n3 = testRootNode.addNode(nodeName3, testNodeType);
        n1.addNode(nodeName2, testNodeType);
        testRootNode.save();
        EventResult addNodeListener = new EventResult(log);
        EventResult removeNodeListener = new EventResult(log);
        addEventListener(addNodeListener, Event.NODE_ADDED);
        addEventListener(removeNodeListener, Event.NODE_REMOVED);
        superuser.getWorkspace().move(n1.getPath(), n3.getPath() + "/" + nodeName4);
        testRootNode.save();
        Event[] added = addNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        Event[] removed = removeNodeListener.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(addNodeListener);
        removeEventListener(removeNodeListener);
        checkNodeAdded(added, new String[]{nodeName3 + "/" + nodeName4}, new String[]{nodeName3 + "/" + nodeName4 + "/" + nodeName2});
        checkNodeRemoved(removed, new String[]{nodeName1}, new String[]{nodeName1 + "/" + nodeName2});
    }

}
