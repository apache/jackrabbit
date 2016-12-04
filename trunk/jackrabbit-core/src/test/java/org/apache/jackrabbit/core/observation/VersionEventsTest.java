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
import org.apache.jackrabbit.core.UserTransactionImpl;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.ObservationManager;
import javax.jcr.version.Version;
import javax.transaction.UserTransaction;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>VersionEventsTest</code> checks if the correct events are triggered
 * for version operations.
 */
public class VersionEventsTest extends AbstractObservationTest {

    /**
     * Test if checkin of a node creates two add node events: one for the
     * version and one for the frozen node.
     */
    public void testCheckin() throws RepositoryException {
        // create versionable node
        Node n1 = testRootNode.addNode(nodeName1);
        n1.addMixin(mixVersionable);
        testRootNode.save();

        EventResult listener = new EventResult(log);
        addEventListener(listener, Event.NODE_ADDED);
        Version v = n1.checkin();
        removeEventListener(listener);

        Event[] events = listener.getEvents(1000);
        Set paths = new HashSet();
        for (int i = 0; i < events.length; i++) {
            paths.add(events[i].getPath());
        }

        assertTrue("missing 'node added' event: " + v.getPath(), paths.contains(v.getPath()));
        String frozenPath = v.getPath() + "/" + jcrFrozenNode;
        assertTrue("missing 'node added' event: " + frozenPath, paths.contains(frozenPath));
    }

    /**
     * Test if checkin of a node in an XA transaction creates two add node
     * events: one for the version and one for the frozen node.
     */
    public void testXACheckin() throws Exception {
        // create versionable node
        Node n1 = testRootNode.addNode(nodeName1);
        n1.addMixin(mixVersionable);
        testRootNode.save();

        EventResult listener = new EventResult(log);
        addEventListener(listener, Event.NODE_ADDED);

        // use a transaction
        UserTransaction utx = new UserTransactionImpl(superuser);
        // start transaction
        utx.begin();
        Version v = n1.checkin();
        // commit transaction
        utx.commit();

        removeEventListener(listener);

        Event[] events = listener.getEvents(1000);
        Set paths = new HashSet();
        for (int i = 0; i < events.length; i++) {
            paths.add(events[i].getPath());
        }

        assertTrue("missing 'node added' event: " + v.getPath(), paths.contains(v.getPath()));
        String frozenPath = v.getPath() + "/" + jcrFrozenNode;
        assertTrue("missing 'node added' event: " + frozenPath, paths.contains(frozenPath));
    }

    /**
     * Test if removing a version triggers two node removed events: one for the
     * version and one for the frozen node.
     */
    public void testRemoveVersion() throws RepositoryException {
        // create versionable node
        Node n1 = testRootNode.addNode(nodeName1);
        n1.addMixin(mixVersionable);
        testRootNode.save();

        Version v = n1.checkin();
        String versionPath = v.getPath();

        n1.remove();
        testRootNode.save();

        EventResult listener = new EventResult(log);
        addEventListener(listener, Event.NODE_REMOVED);
        v.getContainingHistory().removeVersion(v.getName());
        removeEventListener(listener);

        Event[] events = listener.getEvents(1000);
        Set paths = new HashSet();
        for (int i = 0; i < events.length; i++) {
            paths.add(events[i].getPath());
        }

        assertTrue("missing 'node removed' event: " + versionPath, paths.contains(versionPath));
        String frozenPath = versionPath + "/" + jcrFrozenNode;
        assertTrue("missing 'node removed' event: " + frozenPath, paths.contains(frozenPath));
    }

    /**
     * Test if removing a version in an XA transaction triggers two node removed
     * events: one for the version and one for the frozen node.
     */
    public void testXARemoveVersion() throws Exception {
        // create versionable node
        Node n1 = testRootNode.addNode(nodeName1);
        n1.addMixin(mixVersionable);
        testRootNode.save();

        Version v = n1.checkin();
        String versionPath = v.getPath();

        n1.remove();
        testRootNode.save();

        EventResult listener = new EventResult(log);
        addEventListener(listener, Event.NODE_REMOVED);

        // use a transaction
        UserTransaction utx = new UserTransactionImpl(superuser);
        // start transaction
        utx.begin();
        v.getContainingHistory().removeVersion(v.getName());
        // commit transaction
        utx.commit();

        removeEventListener(listener);

        Event[] events = listener.getEvents(1000);
        Set paths = new HashSet();
        for (int i = 0; i < events.length; i++) {
            paths.add(events[i].getPath());
        }

        assertTrue("missing 'node removed' event: " + versionPath, paths.contains(versionPath));
        String frozenPath = versionPath + "/" + jcrFrozenNode;
        assertTrue("missing 'node removed' event: " + frozenPath, paths.contains(frozenPath));
    }

    /**
     * Test if checkin creates events also on a different workspace than the one
     * where the checkin was executed.
     */
    public void testCheckinOtherWorkspace() throws RepositoryException {
        // create versionable node
        Node n1 = testRootNode.addNode(nodeName1);
        n1.addMixin(mixVersionable);
        testRootNode.save();

        Session s = getHelper().getReadOnlySession(workspaceName);
        try {
            EventResult listener = new EventResult(log);
            ObservationManager obsMgr = s.getWorkspace().getObservationManager();
            obsMgr.addEventListener(listener, Event.NODE_ADDED, "/", true,
                    null, null, false);
            Version v = n1.checkin();
            obsMgr.removeEventListener(listener);

            Event[] events = listener.getEvents(1000);
            Set paths = new HashSet();
            for (int i = 0; i < events.length; i++) {
                paths.add(events[i].getPath());
            }

            assertTrue("missing 'node removed': " + v.getPath(), paths.contains(v.getPath()));
            String frozenPath = v.getPath() + "/" + jcrFrozenNode;
            assertTrue("missing 'node removed': " + frozenPath, paths.contains(frozenPath));
        } finally {
            s.logout();
        }
    }
}
