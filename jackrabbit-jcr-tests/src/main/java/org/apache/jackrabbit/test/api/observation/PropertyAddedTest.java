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
import javax.jcr.observation.Event;
import java.util.ArrayList;
import java.util.List;

/**
 * Test cases for {@link javax.jcr.observation.Event#PROPERTY_ADDED} events.
 * <p/>
 * Configuration requirements are:<br/>
 * The {@link #testRoot} must allow child nodes of type {@link #testNodeType}.
 * The child nodes that are created will be named {@link #nodeName1} and
 * {@link #nodeName2}.
 * {@link #testNodeType} must also support String properties with names
 * {@link #propertyName1} and {@link #propertyName2}.
 *
 * @test
 * @sources PropertyAddedTest.java
 * @executeClass org.apache.jackrabbit.test.api.observation.PropertyAddedTest
 * @keywords observation
 */
public class PropertyAddedTest extends AbstractObservationTest {

    /**
     * Tests if {@link javax.jcr.observation.Event#PROPERTY_ADDED} is triggered
     * for system generated property jcr:primaryType.
     */
    public void testSystemGenerated() throws RepositoryException {
        EventResult result = new EventResult(log);
        addEventListener(result, Event.PROPERTY_ADDED);
        testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        List paths = new ArrayList();
        for (int i = 0; i < events.length; i++) {
            paths.add(events[i].getPath());
        }
        String jcrPrimaryTypePath = testRoot + "/" + nodeName1 + "/" + jcrPrimaryType;
        assertTrue("No event generated for jcr:primaryType.", paths.contains(jcrPrimaryTypePath));
    }

    /**
     * Tests if {@link javax.jcr.observation.Event#PROPERTY_ADDED} is triggered
     * when a single property is added.
     * @throws RepositoryException
     */
    public void testSinglePropertyAdded() throws RepositoryException {
        Node foo = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        EventResult result = new EventResult(log);
        addEventListener(result, Event.PROPERTY_ADDED);
        foo.setProperty(propertyName1, "test content");
        testRootNode.save();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        checkPropertyAdded(events, new String[]{nodeName1 + "/" + propertyName1});
    }

    /**
     * Tests if {@link javax.jcr.observation.Event#PROPERTY_ADDED} is triggered
     * when multiple properties are added.
     */
    public void testMultiPropertyAdded() throws RepositoryException {
        Node foo = testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        EventResult result = new EventResult(log);
        addEventListener(result, Event.PROPERTY_ADDED);
        foo.setProperty(propertyName1, "foo");
        foo.setProperty(propertyName2, "bar");
        testRootNode.save();
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        removeEventListener(result);
        checkPropertyAdded(events, new String[]{nodeName1 + "/" + propertyName1,
                                                nodeName1 + "/" + propertyName2});
    }


}
