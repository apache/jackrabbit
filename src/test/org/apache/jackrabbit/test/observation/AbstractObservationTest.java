/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.test.observation;

import org.apache.jackrabbit.test.AbstractTest;

import javax.jcr.RepositoryException;
import javax.jcr.observation.ObservationManager;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventType;
import javax.jcr.observation.Event;
import java.util.Set;
import java.util.HashSet;

/**
 * This class implements the basic {@link #setUp} and {@link #tearDown()}
 * methods for the observation test cases.
 *
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
public abstract class AbstractObservationTest extends AbstractTest {

    /** Default wait timeout for events: 5000 ms */
    protected static final long DEFAULT_WAIT_TIMEOUT = 5000;

    /** The <code>ObservationManager</code> */
    protected ObservationManager obsMgr;

    protected void setUp() throws Exception {
	super.setUp();
        obsMgr = superuser.getWorkspace().getObservationManager();
    }

    /**
     * Registers an <code>EventListener</code> for all events.
     *
     * @param listener the <code>EventListener</code>.
     * @throws RepositoryException if registration fails.
     */
    protected void addEventListener(EventListener listener) throws RepositoryException {
        addEventListener(listener,
                EventType.CHILD_NODE_ADDED | EventType.CHILD_NODE_REMOVED | EventType.PROPERTY_ADDED | EventType.PROPERTY_CHANGED | EventType.PROPERTY_REMOVED);
    }

    /**
     * Registers an <code>EventListener</code> for events of the specified
     * type(s).
     *
     * @param listener the <code>EventListener</code>.
     * @param eventType the {@link javax.jcr.observation.EventType}s
     * @throws RepositoryException if registration fails.
     */
    protected void addEventListener(EventListener listener, long eventType)
            throws RepositoryException {
        if (obsMgr != null) {
            obsMgr.addEventListener(listener,
                    eventType,
                    superuser.getRootNode().getPath(),
                    true,
                    null,
                    null,
                    false);
        } else {
            throw new IllegalStateException("ObservationManager not available.");
        }
    }

    /**
     * Removes the <code>EventListener</code> from the ObservationManager.
     *
     * @param listener the <code>EventListener</code> to unregister.
     * @throws RepositoryException if unregister fails.
     */
    protected void removeEventListener(EventListener listener) throws RepositoryException {
        if (obsMgr != null) {
            obsMgr.removeEventListener(listener);
        } else {
            throw new IllegalStateException("ObservationManager not available.");
        }
    }

    //--------------------< check methods >-------------------------------------

    /**
     * Checks <code>Events</code> for paths. All <code>relPaths</code> are
     * relative to {@link #TEST_ROOT}.
     *
     * @param events   the <code>Event</code>s.
     * @param relPaths paths to child nodes added relative to {@link
     *                 #TEST_ROOT}.
     * @throws RepositoryException if an error occurs while retrieving the nodes
     *                             from event instances.
     */
    protected void checkNodeAdded(Event[] events, String[] relPaths)
	    throws RepositoryException {
	checkNodes(events, relPaths, EventType.CHILD_NODE_ADDED);
    }

    /**
     * Checks <code>Events</code> for paths. All <code>relPaths</code> are
     * relative to {@link #TEST_ROOT}.
     *
     * @param events   the <code>Event</code>s.
     * @param relPaths paths to child nodes added relative to {@link
     *                 #TEST_ROOT}.
     * @throws RepositoryException if an error occurs while retrieving the nodes
     *                             from event instances.
     */
    protected void checkNodeRemoved(Event[] events, String[] relPaths)
	    throws RepositoryException {
	checkNodes(events, relPaths, EventType.CHILD_NODE_REMOVED);
    }

    /**
     * Checks <code>Events</code> for paths. All <code>relPaths</code> are
     * relative to {@link #TEST_ROOT}.
     *
     * @param events   the <code>Event</code>s.
     * @param relPaths paths to added properties relative to {@link
     *                 #TEST_ROOT}.
     * @throws RepositoryException if an error occurs while retrieving the nodes
     *                             from event instances.
     */
    protected void checkPropertyAdded(Event[] events, String[] relPaths)
	    throws RepositoryException {
	checkNodes(events, relPaths, EventType.PROPERTY_ADDED);
    }

    /**
     * Checks <code>Events</code> for paths. All <code>relPaths</code> are
     * relative to {@link #TEST_ROOT}.
     *
     * @param events   the <code>Event</code>s.
     * @param relPaths paths to changed properties relative to {@link
     *                 #TEST_ROOT}.
     * @throws RepositoryException if an error occurs while retrieving the nodes
     *                             from event instances.
     */
    protected void checkPropertyChanged(Event[] events, String[] relPaths)
	    throws RepositoryException {
	checkNodes(events, relPaths, EventType.PROPERTY_CHANGED);
    }

    /**
     * Checks <code>Events</code> for paths. All <code>relPaths</code> are
     * relative to {@link #TEST_ROOT}.
     *
     * @param events   the <code>Event</code>s.
     * @param relPaths paths to removed properties relative to {@link
     *                 #TEST_ROOT}.
     * @throws RepositoryException if an error occurs while retrieving the nodes
     *                             from event instances.
     */
    protected void checkPropertyRemoved(Event[] events, String[] relPaths)
	    throws RepositoryException {
	checkNodes(events, relPaths, EventType.PROPERTY_REMOVED);
    }

    /**
     * Checks <code>Events</code> for paths. All <code>relPaths</code> are
     * relative to {@link #TEST_ROOT}.
     *
     * @param events    the <code>Event</code>s.
     * @param relPaths  paths to item events relative to {@link #TEST_ROOT}.
     * @param eventType the type of event to check.
     * @throws RepositoryException if an error occurs while retrieving the nodes
     *                             from event instances.
     */
    private void checkNodes(Event[] events, String[] relPaths, long eventType)
	    throws RepositoryException {
	assertEquals("Number of events wrong", relPaths.length, events.length);
	Set paths = new HashSet();
	for (int i = 0; i < events.length; i++) {
	    assertEquals("Wrong event type", eventType, events[i].getType());
	    String path = events[i].getNodePath()
		    + "/" + events[i].getChildName();
	    paths.add(path);
	}
	for (int i = 0; i < relPaths.length; i++) {
	    String expected = "/" + TEST_ROOT + "/" + relPaths[i];
	    assertTrue("Path " + expected + " not found in events.",
		    paths.contains(expected));
	}
    }
}
