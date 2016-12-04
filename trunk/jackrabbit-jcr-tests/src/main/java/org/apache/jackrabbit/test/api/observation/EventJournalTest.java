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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventJournal;

import org.apache.jackrabbit.test.NotExecutableException;

/**
 * <code>EventJournalTest</code> performs EventJournal tests.
 */
public class EventJournalTest extends AbstractObservationTest {

    private EventJournal journal;

    protected void setUp() throws Exception {
        checkSupportedOption(Repository.OPTION_JOURNALED_OBSERVATION_SUPPORTED);
        super.setUp();
        journal = obsMgr.getEventJournal();
    }

    public void testSkipToNow() throws RepositoryException {
        // skip everything
        skipToNow();
        assertFalse(journal.hasNext());
    }

    public void testSkipTo() throws Exception {
        long time = System.currentTimeMillis();

        // add some nodes
        Node n1 = testRootNode.addNode(nodeName1);
        Node n2 = testRootNode.addNode(nodeName2);

        // make sure some time passed otherwise we might
        // skip this change as well.
        while (time == System.currentTimeMillis()) {
            Thread.sleep(1);
        }

        // now save
        superuser.save();

        journal.skipTo(time);
        // at least the two added nodes must be returned by the journal
        checkJournal(new String[]{n1.getPath(), n2.getPath()}, new String[0]);
    }

    public void testLiveJournal() throws RepositoryException {
        skipToNow();
        assertFalse(journal.hasNext());

        testRootNode.addNode(nodeName1);
        superuser.save();

        assertTrue(journal.hasNext());
    }

    public void testWorkspaceSeparation() throws RepositoryException {
        skipToNow();
        assertFalse(journal.hasNext());

        Session session = getHelper().getSuperuserSession(workspaceName);
        try {
            Node rootNode = session.getRootNode();
            if (rootNode.hasNode(nodeName1)) {
                rootNode.getNode(nodeName1).remove();
            } else {
                rootNode.addNode(nodeName1);
            }
            session.save();
        } finally {
            session.logout();
        }

        assertFalse(journal.hasNext());
    }

    public void testIsDeepTrue() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1);
        Node n2 = n1.addNode(nodeName2);

        journal = obsMgr.getEventJournal();
        skipToNow();

        superuser.save();

        checkJournal(new String[]{n1.getPath(), n2.getPath()}, new String[0]);
    }

    public void testUUID() throws RepositoryException, NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1);
        ensureMixinType(n1, mixReferenceable);
        superuser.save();

        Node n2 = n1.addNode(nodeName2);

        journal = obsMgr.getEventJournal();
        skipToNow();

        superuser.save();

        checkJournal(new String[]{n2.getPath()}, new String[0]);
    }

    public void testUserData() throws RepositoryException {
        testRootNode.addNode(nodeName1);
        String data = createRandomString(5);
        obsMgr.setUserData(data);

        journal = obsMgr.getEventJournal();
        skipToNow();

        superuser.save();

        assertTrue("no more events", journal.hasNext());
        assertEquals("Wrong user data", data, journal.nextEvent().getUserData());
    }

    public void testEventType() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1);

        journal = getEventJournal(Event.PROPERTY_ADDED, testRoot, true, null, null);
        skipToNow();

        superuser.save();

        checkJournal(new String[]{n1.getPath() + "/" + jcrPrimaryType},
                new String[]{n1.getPath()});
    }

    public void testPath() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1);
        Node n2 = n1.addNode(nodeName2);

        journal = getEventJournal(ALL_TYPES, n1.getPath(), true, null, null);
        skipToNow();
        superuser.save();

        checkJournal(new String[]{n2.getPath()}, new String[]{n1.getPath()});
    }

    public void testIsDeepFalse() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1);
        Node n2 = n1.addNode(nodeName2);

        journal = getEventJournal(ALL_TYPES, testRoot, false, null, null);
        skipToNow();

        superuser.save();

        checkJournal(new String[]{n1.getPath()}, new String[]{n2.getPath()});
    }

    public void testNodeType() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1, "nt:folder");
        Node n2 = n1.addNode(nodeName2, "nt:folder");

        journal = getEventJournal(ALL_TYPES, testRoot, true, null,
                new String[]{"nt:folder"});
        skipToNow();

        superuser.save();

        checkJournal(new String[]{n2.getPath()}, new String[]{n1.getPath()});
    }

    public void testPersist() throws RepositoryException, NotExecutableException {
        Node n1 = testRootNode.addNode(nodeName1);

        journal = getEventJournal(Event.PERSIST, testRoot, true, null, null);
        skipToNow();

        superuser.save();

        boolean hasPersistEvents = journal.hasNext();
        if (! hasPersistEvents) {
        	throw new NotExecutableException("repository does not appear to provide PERSIST events");
        }

        journal = getEventJournal(ALL_TYPES | Event.PERSIST, testRoot, true, null, null);
        skipToNow();

        // add another child node
        Node n3 = testRootNode.addNode(nodeName2);
        String target = n1.getPath();
        n1.remove();
        superuser.save();

        // move it
        superuser.getWorkspace().move(n3.getPath(), target);

        // remove it again
        n3 = superuser.getNode(target);
        n3.remove();
        superuser.save();

        int persistCount = 0;
        Event e = null;
        while (journal.hasNext()) {
        	e = journal.nextEvent();
        	if (e.getType() == Event.PERSIST) {
        		persistCount += 1;
        	}
        }

        assertEquals(3, persistCount);

        // last event should be persist
        assertEquals(Event.PERSIST, e.getType());
    }

    //-------------------------------< internal >-------------------------------

    private void skipToNow() {
        long now = System.currentTimeMillis();
        journal.skipTo(now);
        while (now == System.currentTimeMillis()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private EventJournal getEventJournal(int eventTypes, String absPath, boolean isDeep, String[] uuid, String[] nodeTypeName) throws RepositoryException {
        return superuser.getWorkspace().getObservationManager().getEventJournal(eventTypes, absPath, isDeep, uuid, nodeTypeName);
    }

    /**
     * Checks the journal for events.
     *
     * @param allowed allowed paths for the returned events.
     * @param denied denied paths for the returned events.
     * @throws RepositoryException if an error occurs while reading the event
     *          journal.
     */
    private void checkJournal(String[] allowed, String[] denied) throws RepositoryException {
        Set<String> allowedSet = new HashSet<String>(Arrays.asList(allowed));
        Set<String> deniedSet = new HashSet<String>(Arrays.asList(denied));
        while (journal.hasNext()) {
            String path = journal.nextEvent().getPath();
            allowedSet.remove(path);
            if (deniedSet.contains(path)) {
                fail(path + " must not be present in journal");
            }
        }
        assertTrue("Missing paths in journal: " + allowedSet, allowedSet.isEmpty());
    }
}
