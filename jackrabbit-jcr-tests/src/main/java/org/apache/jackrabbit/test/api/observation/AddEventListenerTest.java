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

import javax.jcr.observation.Event;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.Session;

/**
 * Tests the options for addEventListener().
 * <p/>
 * Configuration requirements are:<br/>
 * The {@link #testRoot} must allow child nodes of type {@link #testNodeType}.
 * The child nodes that are created will be named {@link #nodeName1} and
 * {@link #nodeName2}. Furthermore {@link #testNodeType} must allow to add
 * child nodes of the same type ({@link #testNodeType}).
 * <p/>
 * Certain test require that {@link #testNodeType} is mix:referenceable or
 * allows to add that mixin. If the repository does not support mix:referenceable
 * a {@link org.apache.jackrabbit.test.NotExecutableException} is thrown
 * in those test cases.
 *
 * @test
 * @sources AddEventListenerTest.java
 * @executeClass org.apache.jackrabbit.test.api.observation.AddEventListenerTest
 * @keywords observation
 */
public class AddEventListenerTest extends AbstractObservationTest {

    /**
     * Tests if events are only created for a sub tree of the workspace.
     */
    public void testPath() throws RepositoryException {
        EventResult listener = new EventResult(log);
        obsMgr.addEventListener(listener, Event.NODE_ADDED, testRoot + "/" + nodeName1, true, null, null, false);
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.addNode(nodeName2, testNodeType);
        testRootNode.save();
        Event[] events = listener.getEvents(DEFAULT_WAIT_TIMEOUT);
        obsMgr.removeEventListener(listener);
        checkNodeAdded(events, new String[]{nodeName1 + "/" + nodeName2}, null);
    }

    /**
     * Tests if {@link javax.jcr.observation.Event#NODE_ADDED} is created only
     * for the specified path if <code>isDeep</code> is <code>false</code>.
     */
    public void testIsDeepFalseNodeAdded() throws RepositoryException {
        EventResult listener = new EventResult(log);
        obsMgr.addEventListener(listener, Event.NODE_ADDED, testRoot + "/" + nodeName1, false, null, null, false);
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        n.addNode(nodeName2, testNodeType);
        testRootNode.save();
        Event[] events = listener.getEvents(DEFAULT_WAIT_TIMEOUT);
        obsMgr.removeEventListener(listener);
        checkNodeAdded(events, new String[]{nodeName1 + "/" + nodeName2}, null);
    }

    /**
     * Tests if {@link javax.jcr.observation.Event#PROPERTY_ADDED} is created only
     * for the specified path if <code>isDeep</code> is <code>false</code>.
     */
    public void testIsDeepFalsePropertyAdded() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = testRootNode.addNode(nodeName2, testNodeType);
        testRootNode.save();
        EventResult listener = new EventResult(log);
        obsMgr.addEventListener(listener, Event.PROPERTY_ADDED, testRoot + "/" + nodeName1, false, null, null, false);
        n1.setProperty(propertyName1, "foo");
        n2.setProperty(propertyName1, "foo");
        testRootNode.save();
        Event[] events = listener.getEvents(DEFAULT_WAIT_TIMEOUT);
        obsMgr.removeEventListener(listener);
        checkPropertyAdded(events, new String[]{nodeName1 + "/" + propertyName1});
    }

    /**
     * Tests if no events are generated for own modifications if
     * <code>noLocal</code> is set to <code>true</code>.
     */
    public void testNoLocalTrue() throws RepositoryException {
        EventResult listener = new EventResult(log);
        obsMgr.addEventListener(listener,
                Event.NODE_ADDED,
                testRoot,
                true,
                null,
                null,
                true); // noLocal

        testRootNode.addNode(nodeName1, testNodeType);
        testRootNode.save();
        Event[] events = listener.getEvents(DEFAULT_WAIT_TIMEOUT);
        obsMgr.removeEventListener(listener);
        assertEquals("EventListener must not receive own modification when noLocal=true", 0, events.length);
    }

    /**
     * Tests if events are only generated for specified UUIDs.
     */
    public void testUUID() throws RepositoryException, NotExecutableException {
        Node n1 = null;
        Node n2 = null;
        try {
            n1 = createReferenceable(nodeName1, testNodeType);
            n2 = createReferenceable(nodeName2, testNodeType);
        } catch (RepositoryException e) {
            throw new NotExecutableException("Repository does not support mix:referenceable");
        }
        testRootNode.save();
        EventResult listener = new EventResult(log);
        obsMgr.addEventListener(listener,
                Event.PROPERTY_ADDED,
                testRoot,
                true,
                new String[]{n1.getUUID()},
                null,
                false);
        n1.setProperty(propertyName1, "foo");
        n2.setProperty(propertyName1, "foo");
        testRootNode.save();
        Event[] events = listener.getEvents(DEFAULT_WAIT_TIMEOUT);
        obsMgr.removeEventListener(listener);
        checkPropertyAdded(events, new String[]{nodeName1 + "/" + propertyName1});
    }

    /**
     * Tests if events are only generated for specified node types.
     */
    public void testNodeType() throws RepositoryException {
        String nodetype2 = getProperty("nodetype2");
        EventResult listener = new EventResult(log);
        Node n1 = testRootNode.addNode(nodeName1, testNodeType);
        Node n2 = testRootNode.addNode(nodeName2, nodetype2);
        testRootNode.save();
        obsMgr.addEventListener(listener,
                Event.NODE_ADDED,
                testRoot,
                true,
                null,
                new String[]{testNodeType},
                false);
        Session s = helper.getSuperuserSession();
        try {
            Node n = (Node) s.getItem(n1.getPath());
            n.addNode(nodeName3, ntBase);
            n = (Node) s.getItem(n2.getPath());
            n.addNode(nodeName3, nodetype2);
            n = (Node) s.getItem(testRoot);
            n.save();
        } finally {
            s.logout();
        }
        Event[] events = listener.getEvents(DEFAULT_WAIT_TIMEOUT);
        obsMgr.removeEventListener(listener);
        checkNodeAdded(events, new String[]{nodeName1 + "/" + nodeName3}, null);
    }

    //-------------------------< internal >-------------------------------------

    /**
     * Creates a node with name <code>nodeName</code> with type
     * <code>nodeType</code> as a child node of {@link #testRootNode}.
     * If the created node is not of type mix:referenceable a mixin is added
     * of that type.
     *
     * @param nodeName the node name.
     * @param nodeType the node type of the node to create.
     * @return the created node
     * @throws RepositoryException if node creation fails.
     */
    private Node createReferenceable(String nodeName, String nodeType)
            throws RepositoryException {
        Node n = testRootNode.addNode(nodeName, nodeType);
        if (needsMixin(n, mixReferenceable)) {
            n.addMixin(mixReferenceable);
            // some implementations may require a save after addMixin()
            testRootNode.save();
        }
        return n;
    }
}
