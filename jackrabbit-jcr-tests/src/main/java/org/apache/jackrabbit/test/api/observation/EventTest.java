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

import javax.jcr.observation.Event;
import javax.jcr.RepositoryException;
import javax.jcr.Node;

/**
 * Tests methods on the {@link javax.jcr.observation.Event} interface.
 * <p/>
 * Configuration requirements are:<br/>
 * The {@link #testRoot} must allow child nodes of type {@link #testNodeType}.
 * The child node that is created will be named {@link #nodeName1}.
 *
 * @test
 * @sources EventTest.java
 * @executeClass org.apache.jackrabbit.test.api.observation.EventTest
 * @keywords observation
 */
public class EventTest extends AbstractObservationTest {

    /**
     * Tests if getPath() returns the correct path.
     */
    public void testGetNodePath() throws RepositoryException{
        EventResult result = new EventResult(log);
        addEventListener(result, Event.NODE_ADDED);
        Node addedNode = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        assertEquals("Wrong number of events returned", 1, events.length);
        String path = events[0].getPath();
        String absPath = addedNode.getPath();
        assertEquals("Path returned by getPath() is wrong", absPath, path);
    }

    /**
     * Tests if {@link javax.jcr.observation.Event#getUserID()} returns the same
     * value as {@link javax.jcr.Session#getUserID()}.
     */
    public void testGetUserId() throws RepositoryException{
        EventResult result = new EventResult(log);
        addEventListener(result, Event.NODE_ADDED);
        testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        assertEquals("Wrong number of events returned", 1, events.length);
        String userId = events[0].getUserID();
        String sessionUId = superuser.getUserID();
        assertEquals("UserId of event is not equal to userId of session", userId, sessionUId);
    }

    /**
     * Tests if getType() returns the correct value.
     */
    public void testGetType() throws RepositoryException{
        EventResult result = new EventResult(log);
        addEventListener(result, Event.NODE_ADDED);
        testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        assertEquals("Wrong number of events returned", 1, events.length);
        int type = events[0].getType();
        assertEquals("Event did not return correct event type", Event.NODE_ADDED, type);
    }
}