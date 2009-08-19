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
package org.apache.jackrabbit.core;

import javax.jcr.Node;
import javax.jcr.Workspace;
import javax.jcr.NodeIterator;
import javax.jcr.query.QueryManager;
import javax.jcr.query.Query;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;

import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Tests features available with shareable nodes.
 */
public class ShareableNodeTest extends AbstractJCRTest {
    
    //------------------------------------------------------ specification tests

    /**
     * Verify that observation events are sent only once (6.13.15).
     */
    public void testObservation() throws Exception {
        // setup parent nodes and first child
        Node a1 = testRootNode.addNode("a1");
        Node a2 = testRootNode.addNode("a2");
        Node b1 = a1.addNode("b1");
        testRootNode.save();

        // add mixin
        b1.addMixin("mix:shareable");
        b1.save();

        // clone
        Workspace workspace = b1.getSession().getWorkspace();
        workspace.clone(workspace.getName(), b1.getPath(),
                a2.getPath() + "/b2", false);

        // event listener that counts events received
        class EventCounter implements SynchronousEventListener {

            private int count;

            public void onEvent(EventIterator events) {
                while (events.hasNext()) {
                    events.nextEvent();
                    count++;
                }
            }

            public int getEventCount() {
                return count;
            }

            public void resetCount() {
                count = 0;
            }
        }

        EventCounter el = new EventCounter();
        ObservationManager om = superuser.getWorkspace().getObservationManager();

        // add node underneath shared set: verify it generates one event only
        om.addEventListener(el, Event.NODE_ADDED, testRootNode.getPath(),
                true, null, null, false);
        b1.addNode("c");
        b1.save();
        superuser.getWorkspace().getObservationManager().removeEventListener(el);
        assertEquals(1, el.getEventCount());

        // remove node underneath shared set: verify it generates one event only
        el.resetCount();
        om.addEventListener(el, Event.NODE_REMOVED, testRootNode.getPath(),
                true, null, null, false);
        b1.getNode("c").remove();
        b1.save();
        superuser.getWorkspace().getObservationManager().removeEventListener(el);
        assertEquals(1, el.getEventCount());

        // add property underneath shared set: verify it generates one event only
        el.resetCount();
        om.addEventListener(el, Event.PROPERTY_ADDED, testRootNode.getPath(),
                true, null, null, false);
        b1.setProperty("c", "1");
        b1.save();
        superuser.getWorkspace().getObservationManager().removeEventListener(el);
        assertEquals(1, el.getEventCount());

        // modify property underneath shared set: verify it generates one event only
        el.resetCount();
        om.addEventListener(el, Event.PROPERTY_CHANGED, testRootNode.getPath(),
                true, null, null, false);
        b1.setProperty("c", "2");
        b1.save();
        superuser.getWorkspace().getObservationManager().removeEventListener(el);
        assertEquals(1, el.getEventCount());

        // remove property underneath shared set: verify it generates one event only
        el.resetCount();
        om.addEventListener(el, Event.PROPERTY_REMOVED, testRootNode.getPath(),
                true, null, null, false);
        b1.getProperty("c").remove();
        b1.save();
        superuser.getWorkspace().getObservationManager().removeEventListener(el);
        assertEquals(1, el.getEventCount());
    }

    /**
     * Verify that a shared node is removed when the ancestor is removed.
     * See also JCR-2257.
     */
    public void testRemoveAncestorOfSharedNode() throws Exception {
        Node a1 = testRootNode.addNode("a1");
        Node a2 = a1.addNode("a2");
        Node b1 = a1.addNode("b1");
        ensureMixinType(b1, mixShareable);
        testRootNode.save();
        //now we have a shareable node N with path a1/b1

        Workspace workspace = testRootNode.getSession().getWorkspace();
        String path = a2.getPath() + "/b2";
        workspace.clone(workspace.getName(), b1.getPath(), path, false);
        testRootNode.save();
        //now we have another shareable node N' in the same shared set as N with path a1/a2/b2

        a2.remove();
        testRootNode.save();

        // assert b2 is removed from index
        QueryManager qm = superuser.getWorkspace().getQueryManager();
        String stmt = testPath + "//b2";
        NodeIterator it = qm.createQuery(stmt, Query.XPATH).execute().getNodes();
        assertFalse(it.hasNext());
    }
}
