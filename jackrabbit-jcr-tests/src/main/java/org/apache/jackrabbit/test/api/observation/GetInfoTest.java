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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

/**
 * <code>GetInfoTest</code> checks that the info map is empty for event types:
 * {@link Event#NODE_ADDED}, {@link Event#NODE_REMOVED},
 * {@link Event#PROPERTY_ADDED}, {@link Event#PROPERTY_CHANGED} and
 * {@link Event#PROPERTY_REMOVED}.
 */
public class GetInfoTest extends AbstractObservationTest {

    public void testNodeAdded() throws RepositoryException {
        Event[] events = getEvents(new Callable(){
            public void call() throws RepositoryException {
                testRootNode.addNode(nodeName1, testNodeType);
                testRootNode.getSession().save();
            }
        }, Event.NODE_ADDED);
        for (int i = 0; i < events.length; i++) {
            Set<?> unexpectedKeys = getUnexpectedKeys(events[i].getInfo());
            assertEquals("info map contains invalid keys: " + unexpectedKeys, 0, unexpectedKeys.size());
        }
    }

    public void testNodeRemoved() throws RepositoryException {
        final Node n = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.getSession().save();
        Event[] events = getEvents(new Callable(){
            public void call() throws RepositoryException {
                n.remove();
                testRootNode.getSession().save();
            }
        }, Event.NODE_REMOVED);
        for (int i = 0; i < events.length; i++) {
            Set<?> unexpectedKeys = getUnexpectedKeys(events[i].getInfo());
            assertEquals("info map must be empty", 0, unexpectedKeys.size());
        }
    }

    public void testPropertyAdded() throws RepositoryException {
        Event[] events = getEvents(new Callable(){
            public void call() throws RepositoryException {
                testRootNode.addNode(nodeName1, testNodeType).setProperty(propertyName1, "test");
                testRootNode.getSession().save();
            }
        }, Event.PROPERTY_ADDED);
        for (int i = 0; i < events.length; i++) {
            Set<?> unexpectedKeys = getUnexpectedKeys(events[i].getInfo());
            assertEquals("info map must be empty", 0, unexpectedKeys.size());
        }
    }

    public void testPropertyChanged() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        final Property prop = n.setProperty(propertyName1, "test");
        testRootNode.getSession().save();
        Event[] events = getEvents(new Callable(){
            public void call() throws RepositoryException {
                prop.setValue("modified");
                prop.getSession().save();
            }
        }, Event.PROPERTY_CHANGED);
        for (int i = 0; i < events.length; i++) {
            Set<?> unexpectedKeys = getUnexpectedKeys(events[i].getInfo());
            assertEquals("info map must be empty: " + unexpectedKeys, 0, unexpectedKeys.size());
        }
    }

    public void testPropertyRemoved() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        final Property prop = n.setProperty(propertyName1, "test");
        testRootNode.getSession().save();
        Event[] events = getEvents(new Callable(){
            public void call() throws RepositoryException {
                Session s = prop.getSession();
                prop.remove();
                s.save();
            }
        }, Event.PROPERTY_REMOVED);
        for (int i = 0; i < events.length; i++) {
            Set<?> unexpectedKeys = getUnexpectedKeys(events[i].getInfo());
            assertEquals("info map must be empty: " + unexpectedKeys, 0, unexpectedKeys.size());
        }
    }

    private static Set<?> getUnexpectedKeys(Map<?, ?> info) {
        Set<Object> result = new HashSet<Object>();
        result.addAll(info.keySet());
        result.remove("jcr:primaryType");
        result.remove("jcr:mixinTypes");
        return result;
    }
}
