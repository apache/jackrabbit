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
package org.apache.jackrabbit.test.api.observation;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

/**
 * Test cases for {@link javax.jcr.observation.Event.PROPERTY_CHANGED} events.
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
        Node node = testRootNode.addNode(nodeName1);
        node.setProperty(propertyName1, "foo");
        testRootNode.save();
        EventResult result = new EventResult(log);
        addEventListener(result, Event.PROPERTY_CHANGED);
        node.getProperty(propertyName1).setValue("foobar");
        testRootNode.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkPropertyChanged(events, new String[]{nodeName1 + "/" + propertyName1});
    }

    /**
     * Tests if {@link javax.jcr.observation.Event#PROPERTY_CHANGED} are
     * triggered when multiple properties are changed.
     * @throws RepositoryException
     */
    public void testMultiPropertyChanged() throws RepositoryException {
        Node node = testRootNode.addNode(nodeName1);
        node.setProperty(propertyName1, "foo");
        node.setProperty(propertyName2, "bar");
        testRootNode.save();
        EventResult result = new EventResult(log);
        addEventListener(result, Event.PROPERTY_CHANGED);
        node.getProperty(propertyName1).setValue("foobar");
        node.getProperty(propertyName2).setValue("foobar");
        testRootNode.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkPropertyChanged(events, new String[]{nodeName1 + "/" + propertyName1,
                                                  nodeName1 + "/" + propertyName2});
    }

    /**
     * Tests if a {@link javax.jcr.observation.Event#PROPERTY_CHANGED} is
     * triggered only for changed properties and not for new properties.
     */
    public void testSinglePropertyChangedWithAdded() throws RepositoryException {
        Node node = testRootNode.addNode(nodeName1);
        node.setProperty(propertyName1, "foo");
        testRootNode.save();
        EventResult result = new EventResult(log);
        addEventListener(result, Event.PROPERTY_CHANGED);
        node.getProperty(propertyName1).setValue("foobar");
        node.setProperty(propertyName2, "bar");    // will not fire prop changed event
        testRootNode.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkPropertyChanged(events, new String[]{nodeName1 + "/" + propertyName1});
    }

}
