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

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.Event;

/**
 * Test cases for {@link javax.jcr.observation.Event#PROPERTY_CHANGED} events.
 * <p/>
 * Configuration requirements are:<br/>
 * The {@link #testRoot} must allow child nodes of type {@link #testNodeType}.
 * The child nodes that are created will be named {@link #nodeName1} and
 * {@link #nodeName2}.
 * {@link #testNodeType} must also support String properties with names
 * {@link #propertyName1} and {@link #propertyName2}.
 *
 * @test
 * @sources PropertyChangedTest.java
 * @executeClass org.apache.jackrabbit.test.api.observation.PropertyChangedTest
 * @keywords observation
 */
public class PropertyChangedTest extends AbstractObservationTest {

    /**
     * Tests if a {@link javax.jcr.observation.Event#PROPERTY_CHANGED} is
     * triggered when a single property is changed.
     */
    public void testSinglePropertyChanged() throws RepositoryException {
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        node.setProperty(propertyName1, "foo");
        testRootNode.save();
        EventResult result = new EventResult(log);
        addEventListener(result, Event.PROPERTY_CHANGED);
        node.getProperty(propertyName1).setValue("foobar");
        testRootNode.save();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        checkPropertyChanged(events, new String[]{nodeName1 + "/" + propertyName1});
    }

    /**
     * Tests if {@link javax.jcr.observation.Event#PROPERTY_CHANGED} are
     * triggered when multiple properties are changed.
     * @throws RepositoryException
     */
    public void testMultiPropertyChanged() throws RepositoryException {
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        node.setProperty(propertyName1, "foo");
        node.setProperty(propertyName2, "bar");
        testRootNode.save();
        EventResult result = new EventResult(log);
        addEventListener(result, Event.PROPERTY_CHANGED);
        node.getProperty(propertyName1).setValue("foobar");
        node.getProperty(propertyName2).setValue("foobar");
        testRootNode.save();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        checkPropertyChanged(events, new String[]{nodeName1 + "/" + propertyName1,
                                                  nodeName1 + "/" + propertyName2});
    }

    /**
     * Tests if a {@link javax.jcr.observation.Event#PROPERTY_CHANGED} is
     * triggered only for changed properties and not for new properties.
     */
    public void testSinglePropertyChangedWithAdded() throws RepositoryException {
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        node.setProperty(propertyName1, "foo");
        testRootNode.save();
        EventResult result = new EventResult(log);
        addEventListener(result, Event.PROPERTY_CHANGED);
        node.getProperty(propertyName1).setValue("foobar");
        node.setProperty(propertyName2, "bar");    // will not fire prop changed event
        testRootNode.save();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        checkPropertyChanged(events, new String[]{nodeName1 + "/" + propertyName1});
    }

    /**
     * Tests if either a
     * <ul>
     * <li>{@link Event#PROPERTY_CHANGED}</li>
     * <li>{@link Event#PROPERTY_REMOVED} and {@link Event#PROPERTY_ADDED}</li>
     * </ul>
     * is triggered if a property is transiently removed and set again with
     * the same name but different type and then saved.
     * <p/>
     * If the node type {@link #testNodeType} does not suppport a property with
     * name {@link #propertyName1} of type {@link PropertyType#UNDEFINED} a
     * {@link NotExecutableException} is thrown.
     */
    public void testPropertyRemoveCreate()
            throws RepositoryException, NotExecutableException {
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        NodeType nt = superuser.getWorkspace().getNodeTypeManager().getNodeType(testNodeType);
        Value v1 = superuser.getValueFactory().createValue("foo");
        Value v2 = superuser.getValueFactory().createValue(System.currentTimeMillis());
        if (!nt.canSetProperty(propertyName1, v1) || !nt.canSetProperty(propertyName1, v2)) {
            throw new NotExecutableException("Property " + propertyName1 + " is not of type UNDEFINED");
        }
        n.setProperty(propertyName1, v1);
        testRootNode.save();
        EventResult result = new EventResult(log);
        addEventListener(result, Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED);
        n.getProperty(propertyName1).remove();
        n.setProperty(propertyName1, v2);
        testRootNode.save();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);

        if (events.length == 1) {
            checkPropertyChanged(events, new String[]{nodeName1 + "/" + propertyName1});
        } else {
            // prop remove and add event
            assertEquals("Expected 2 events for a property type change.", 2, events.length);
            int type = Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED;
            String path = testRoot + "/" + nodeName1 + "/" + propertyName1;
            for (int i = 0; i < events.length; i++) {
                assertTrue("Event is not of type PROPERTY_REMOVED or PROPERTY_ADDED", (events[i].getType() & type) > 0);
                assertEquals("Path for event is wrong.", path, events[i].getPath());
            }
        }
    }
}
