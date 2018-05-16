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
package org.apache.jackrabbit.core.observation;

import org.apache.jackrabbit.test.api.observation.AbstractObservationTest;
import org.apache.jackrabbit.test.api.observation.EventResult;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;

/**
 * Tests if observation event filtering works based on mixin type names.
 */
public class MixinTest extends AbstractObservationTest {

    /**
     * Tests event filtering with a single mixin type name.
     */
    public void testSingleMixin() throws RepositoryException {
        testRootNode.addNode(nodeName1, testNodeType).addMixin(mixReferenceable);
        testRootNode.addNode(nodeName2, testNodeType);
        testRootNode.addNode(nodeName3, testNodeType).addMixin(mixReferenceable);
        testRootNode.save();

        EventResult propertyAddedListener = new EventResult(log);
        addEventListener(propertyAddedListener, new String[]{mixReferenceable}, Event.PROPERTY_ADDED);
        try {
            testRootNode.getNode(nodeName1).setProperty(propertyName1, "test");
            testRootNode.getNode(nodeName2).setProperty(propertyName1, "test");
            testRootNode.getNode(nodeName3).setProperty(propertyName1, "test");
            testRootNode.save();

            Event[] added = propertyAddedListener.getEvents(DEFAULT_WAIT_TIMEOUT);
            checkPropertyAdded(added, new String[]{nodeName1 + "/" + propertyName1,
                                                   nodeName3 + "/" + propertyName1});
        } finally {
            removeEventListener(propertyAddedListener);
        }
    }

    /**
     * Tests event filtering with multiple mixin type name.
     */
    public void testMultipleMixin() throws RepositoryException {
        testRootNode.addNode(nodeName1, testNodeType).addMixin(mixReferenceable);
        testRootNode.addNode(nodeName2, testNodeType).addMixin(mixTitle);
        testRootNode.addNode(nodeName3, testNodeType).addMixin(mixReferenceable);
        testRootNode.save();

        EventResult propertyAddedListener = new EventResult(log);
        addEventListener(propertyAddedListener, new String[]{mixReferenceable, mixTitle}, Event.PROPERTY_ADDED);
        try {
            testRootNode.getNode(nodeName1).setProperty(propertyName1, "test");
            testRootNode.getNode(nodeName2).setProperty(propertyName1, "test");
            testRootNode.getNode(nodeName3).setProperty(propertyName1, "test");
            testRootNode.save();

            Event[] added = propertyAddedListener.getEvents(DEFAULT_WAIT_TIMEOUT);
            checkPropertyAdded(added, new String[]{nodeName1 + "/" + propertyName1,
                                                   nodeName2 + "/" + propertyName1,
                                                   nodeName3 + "/" + propertyName1});
        } finally {
            removeEventListener(propertyAddedListener);
        }
    }

    /**
     * Tests event filtering on nodes with multiple mixins applied.
     */
    public void testMultipleMixinOnNode() throws RepositoryException {
        Node node1 = testRootNode.addNode(nodeName1, testNodeType);
        node1.addMixin(mixReferenceable);
        node1.addMixin(mixTitle);
        Node node2 = testRootNode.addNode(nodeName2, testNodeType);
        Node node3 = testRootNode.addNode(nodeName3, testNodeType);
        node3.addMixin(mixTitle);
        node3.addMixin(mixReferenceable);
        testRootNode.save();

        EventResult propertyAddedListener = new EventResult(log);
        addEventListener(propertyAddedListener, new String[]{mixReferenceable}, Event.PROPERTY_ADDED);
        try {
            node1.setProperty(propertyName1, "test");
            node2.setProperty(propertyName1, "test");
            node3.setProperty(propertyName1, "test");
            testRootNode.save();

            Event[] added = propertyAddedListener.getEvents(DEFAULT_WAIT_TIMEOUT);
            checkPropertyAdded(added, new String[]{nodeName1 + "/" + propertyName1,
                                                   nodeName3 + "/" + propertyName1});
        } finally {
            removeEventListener(propertyAddedListener);
        }
    }

    /**
     * Checks if an event listener registered for a mixin type T also gets
     * notifications for an event on a node with a mixin type which is derived
     * from T.
     */
    public void testDerivedMixin() throws RepositoryException {
        Node node1 = testRootNode.addNode(nodeName1, testNodeType);
        node1.addMixin(mixVersionable);

        testRootNode.save();

        EventResult propertyAddedListener = new EventResult(log);
        // mix:versionable is derived from mix:referenceable
        addEventListener(propertyAddedListener, new String[]{mixReferenceable}, Event.PROPERTY_ADDED);
        try {
            node1.setProperty(propertyName1, "test");
            testRootNode.save();

            Event[] added = propertyAddedListener.getEvents(DEFAULT_WAIT_TIMEOUT);
            checkPropertyAdded(added, new String[]{nodeName1 + "/" + propertyName1});
        } finally {
            removeEventListener(propertyAddedListener);
        }
    }

    /**
     * Registers an <code>EventListener</code> for events of the specified
     * type(s).
     *
     * @param listener  the <code>EventListener</code>.
     * @param mixins    the names of mixin types to filter.
     * @param eventType the event types
     * @throws RepositoryException if registration fails.
     */
    protected void addEventListener(EventListener listener, String[] mixins, int eventType)
            throws RepositoryException {
        if (obsMgr != null) {
            obsMgr.addEventListener(listener,
                    eventType,
                    superuser.getRootNode().getPath(),
                    true,
                    null,
                    mixins,
                    false);
        } else {
            throw new IllegalStateException("ObservationManager not available.");
        }
    }
}
