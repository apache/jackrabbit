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
import javax.jcr.Property;
import javax.jcr.observation.Event;

/**
 * <code>IdentifierTest</code> checks if the identifier of an event is correct.
 */
public class GetIdentifierTest extends AbstractObservationTest {

    public void testNodeAdded() throws RepositoryException {
        Event[] events = getEvents(new Callable(){
            public void call() throws RepositoryException {
                testRootNode.addNode(nodeName1, testNodeType);
                testRootNode.getSession().save();
            }
        }, Event.NODE_ADDED);
        Node n = testRootNode.getNode(nodeName1);
        assertEquals(n.getIdentifier(), getEventByPath(events, n.getPath()).getIdentifier());
    }

    public void testNodeMoved() throws RepositoryException {
        final Node n = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.getSession().save();
        Event[] events = getEvents(new Callable(){
            public void call() throws RepositoryException {
                superuser.getWorkspace().move(n.getPath(), testRoot + "/" + nodeName2);
            }
        }, Event.NODE_MOVED);
        String path = testRootNode.getNode(nodeName2).getPath();
        assertEquals(n.getIdentifier(), getEventByPath(events, path).getIdentifier());
    }

    public void testNodeRemoved() throws RepositoryException {
        final Node n = testRootNode.addNode(nodeName1, testNodeType);
        String path = n.getPath();
        testRootNode.getSession().save();
        String identifier = n.getIdentifier();
        Event[] events = getEvents(new Callable(){
            public void call() throws RepositoryException {
                n.remove();
                testRootNode.getSession().save();
            }
        }, Event.NODE_REMOVED);
        assertEquals(identifier, getEventByPath(events, path).getIdentifier());
    }

    public void testPropertyAdded() throws RepositoryException {
        Event[] events = getEvents(new Callable(){
            public void call() throws RepositoryException {
                testRootNode.addNode(nodeName1, testNodeType).setProperty(propertyName1, "test");
                testRootNode.getSession().save();
            }
        }, Event.PROPERTY_ADDED);
        Node n = testRootNode.getNode(nodeName1);
        Property prop = n.getProperty(propertyName1);
        assertEquals(n.getIdentifier(), getEventByPath(events, prop.getPath()).getIdentifier());
    }

    public void testPropertyChanged() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        final Property prop = n.setProperty(propertyName1, "test");
        testRootNode.getSession().save();
        Event[] events = getEvents(new Callable(){
            public void call() throws RepositoryException {
                prop.setValue("modified");
                testRootNode.getSession().save();
            }
        }, Event.PROPERTY_CHANGED);
        assertEquals(n.getIdentifier(), getEventByPath(events, prop.getPath()).getIdentifier());
    }

    public void testPropertyRemoved() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        final Property prop = n.setProperty(propertyName1, "test");
        String propPath = prop.getPath();
        testRootNode.getSession().save();
        Event[] events = getEvents(new Callable(){
            public void call() throws RepositoryException {
                prop.remove();
                testRootNode.getSession().save();
            }
        }, Event.PROPERTY_REMOVED);
        assertEquals(n.getIdentifier(), getEventByPath(events, propPath).getIdentifier());
    }
}
