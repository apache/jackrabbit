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
package org.apache.jackrabbit.api.jsr283.observation;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.observation.Event;

import org.apache.jackrabbit.test.NotExecutableException;
import org.apache.jackrabbit.test.api.observation.EventResult;

/**
 * <code>AddEventListenerTest</code> contains tests for JSR 283 specific
 * features.
 */
public class AddEventListenerTest extends AbstractObservationTest {

    /**
     * Tests if events are only generated for specified Identifiers.
     */
    public void testUUID() throws RepositoryException, NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        testRootNode.save();

        EventResult listener = new EventResult(log);
        obsMgr.addEventListener(listener,
                Event.PROPERTY_ADDED,
                testRoot,
                true,
                new String[]{getIdentifier(n1)},
                null,
                false);
        n1.setProperty(propertyName1, "foo");
        n2.setProperty(propertyName1, "foo");
        testRootNode.save();

        Event[] events = listener.getEvents(DEFAULT_WAIT_TIMEOUT);
        obsMgr.removeEventListener(listener);
        checkPropertyAdded(events, new String[]{nodeName1 + "/" + propertyName1});
    }
}
