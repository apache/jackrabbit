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

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;

import org.apache.jackrabbit.test.api.observation.AbstractObservationTest;
import org.apache.jackrabbit.test.api.observation.EventResult;
import org.apache.jackrabbit.core.NodeImpl;

/**
 * <code>ShareableNodesTest</code>...
 */
public class ShareableNodesTest extends AbstractObservationTest {

    public void testAddShareableMixin() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1);
        testRootNode.save();

        EventResult result = new EventResult(log);
        addEventListener(result);

        n1.addMixin(mixShareable);
        testRootNode.save();

        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        for (int i = 0; i < events.length; i++) {
            assertFalse("must not contain node added event", events[i].getType() == Event.NODE_ADDED);
            assertFalse("must not contain node removed event", events[i].getType() == Event.NODE_REMOVED);
        }
    }

    public void testAddShare() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1);
        Node n2 = testRootNode.addNode(nodeName2);
        Node s = n1.addNode(nodeName3);
        s.addMixin(mixShareable);
        testRootNode.save();

        EventResult result = new EventResult(log);
        addEventListener(result);

        Workspace wsp = superuser.getWorkspace();
        wsp.clone(wsp.getName(), s.getPath(), n2.getPath() + "/" + s.getName(), false);

        checkNodeAdded(result.getEvents(DEFAULT_WAIT_TIMEOUT),
                new String[]{nodeName2 + "/" + nodeName3},
                new String[0]);
    }

    public void testRemoveShare() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1);
        Node n2 = testRootNode.addNode(nodeName2);
        Node s = n1.addNode(nodeName3);
        s.addMixin(mixShareable);
        testRootNode.save();

        Workspace wsp = superuser.getWorkspace();
        wsp.clone(wsp.getName(), s.getPath(), n2.getPath() + "/" + s.getName(), false);

        EventResult result = new EventResult(log);
        addEventListener(result);

        removeFromSharedSet(n2.getNode(nodeName3));
        testRootNode.save();

        checkNodeRemoved(result.getEvents(DEFAULT_WAIT_TIMEOUT),
                new String[]{nodeName2 + "/" + nodeName3},
                new String[0]);
    }

    protected void removeFromSharedSet(Node node) throws RepositoryException {
        node.removeShare();
    }
}
