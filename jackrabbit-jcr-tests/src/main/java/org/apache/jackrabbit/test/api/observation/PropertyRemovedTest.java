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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Property;
import javax.jcr.observation.Event;

/**
 * Test cases for {@link javax.jcr.observation.Event#PROPERTY_REMOVED} events.
 * <p/>
 * Configuration requirements are:<br/>
 * The {@link #testRoot} must allow child nodes of type {@link #testNodeType}.
 * The child nodes that are created will be named {@link #nodeName1} and
 * {@link #nodeName2}.
 * {@link #testNodeType} must also support String properties with names
 * {@link #propertyName1} and {@link #propertyName2}.
 *
 * @test
 * @sources PropertyRemovedTest.java
 * @executeClass org.apache.jackrabbit.test.api.observation.PropertyRemovedTest
 * @keywords observation
 */
public class PropertyRemovedTest extends AbstractObservationTest {

    /**
     * Tests if a {@link javax.jcr.observation.Event#PROPERTY_REMOVED} is
     * triggered when a property is removed.
     */
    public void testSinglePropertyRemoved() throws RepositoryException {
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        Property prop1 = node.setProperty(propertyName1, "foo");
        node.setProperty(propertyName2, "bar");
        testRootNode.save();
        EventResult result = new EventResult(log);
        addEventListener(result, Event.PROPERTY_REMOVED);
        prop1.remove();
        testRootNode.save();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        checkPropertyRemoved(events, new String[]{nodeName1 + "/" + propertyName1});
    }

    /**
     * Tests if {@link javax.jcr.observation.Event#PROPERTY_REMOVED} are
     * triggered when multiple properties are removed.
     */
    public void testMultiPropertyRemoved() throws RepositoryException {
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        Property prop1 = node.setProperty(propertyName1, "foo");
        Property prop2 = node.setProperty(propertyName2, "bar");
        testRootNode.save();
        EventResult result = new EventResult(log);
        addEventListener(result, Event.PROPERTY_REMOVED);
        prop1.remove();
        prop2.remove();
        testRootNode.save();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        checkPropertyRemoved(events, new String[]{nodeName1 + "/" + propertyName1,
                                                  nodeName1 + "/" + propertyName2});
    }
}
