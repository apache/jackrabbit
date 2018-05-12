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
package org.apache.jackrabbit.rmi.observation;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;
import org.apache.jackrabbit.rmi.client.RemoteRuntimeException;
import org.apache.jackrabbit.rmi.iterator.ArrayEventIterator;
import org.apache.jackrabbit.rmi.remote.RemoteEventCollection;
import org.apache.jackrabbit.rmi.remote.RemoteObservationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ClientEventPoll</code> class is the registry for client-side
 * event listeners on behalf of the
 * {@link org.apache.jackrabbit.rmi.client.ClientObservationManager} class. In
 * addition this class extends the <code>java.lang.Thread</code> class able
 * to be run in a separate thread to constantly poll the server-side observation
 * manager for new events.
 * <p>
 * Notes:
 * <ol>
 * <li>Only one instance of this class should be instantiated for each instance
 * of a {@link org.apache.jackrabbit.rmi.remote.RemoteObservationManager} class.
 * <li><code>EventListener</code>s registered with this class must properly
 * implement the <code>Object.hashCode()</code> and <code>Object.equals()</code>
 * contracts for them to be handled correctly by this class.
 * </ol>
 *
 * @see #run()
 */
public class ClientEventPoll extends Thread {

    /** logger */
    private static final Logger log =
        LoggerFactory.getLogger(ClientEventPoll.class);

    /**
     * The time in milliseconds the {@link #run()} method should be waiting
     * for remote events.
     * @see #run()
     */
    private static final long POLL_TIMEOUT = 5000;

    /** The thread name */
    private static final String THREAD_NAME = "Client Event Poller";

    /** The primitive unique identifier generator. */
    private static long counter = 0;

    /** The {@link RemoteObservationManager} called for the new events. */
    private final RemoteObservationManager remote;

    /**
     * The <code>Session</code> checked by the {@link #run} method whether it
     * is still alive or the thread should terminate.
     */
    private final Session session;

    /** The map of locally registered listeners indexed by the unique identifier */
    private Map listenerMap = new HashMap();

    /** The map of unique identifieres indexed by the registered listeners */
    private Map idMap = new HashMap();

    /**
     * Flag indicating whether the {@link #run()} method should terminate.
     * @see #run()
     */
    private boolean running = true;

    /**
     * Creates an instance of this class talking to the given
     * {@link RemoteObservationManager}.
     *
     * @param remote The remote observation manager which is asked for new
     *      events. This must not be <code>null</code>.
     * @param session The <code>Session</code> which is asked whether it is
     *      alive by the {@link #run()} method. This must not be <code>null</code>.
     *
     * @throws NullPointerException if <code>remote</code> or <code>session</code>
     *      is <code>null</code>.
     */
    public ClientEventPoll(RemoteObservationManager remote, Session session)
            throws NullPointerException {
        super(THREAD_NAME);

        // check remote and session
        if (remote == null) {
            throw new NullPointerException("remote");
        }
        if (session == null) {
            throw new NullPointerException("session");
        }

        this.remote = remote;
        this.session = session;
    }

    /**
     * Registers the given local listener with this instance and returns the
     * unique identifier assigned to it.
     *
     * @param listener The <code>EventListener</code> to register.
     *
     * @return The unique identifier assigned to the newly registered event
     *      listener.
     */
    public synchronized long addListener(EventListener listener) {
        Long id = new Long(counter++);
        listenerMap.put(id, listener);
        idMap.put(listener, id);
        return id.longValue();
    }

    /**
     * Unregisters the given local listener from this instance and returns the
     * unique identifier assigned to it.
     *
     * @param listener The <code>EventListener</code> to unregister.
     *
     * @return The unique identifier assigned to the unregistered event listener
     *      or <code>-1</code> if the listener was not registered.
     */
    public synchronized long removeListener(EventListener listener) {
        Long key = (Long) idMap.remove(listener);
        if (key != null) {
            listenerMap.remove(key);
            return key.longValue();
        }

        return -1;
    }

    /**
     * Returns an array of the registered event listeners.
     *
     * @return registered event listeners
     */
    public synchronized EventListener[] getListeners() {
        return (EventListener[]) listenerMap.values().toArray(
            new EventListener[(listenerMap.size())]);
    }

    /**
     * Indicates to the {@link #run()} method, that asking for events should
     * be terminated.
     *
     * @see #run()
     */
    public void terminate() {
        this.running = false;
    }

    //---------- Thread overwrite ---------------------------------------------

    /**
     * Checks for remote events and dispatches them to the locally registered
     * event listeners. This is how this method works:
     * <ol>
     * <li>Continue with next step if {@link #terminate()} has not been called
     * yet and the session is still alive.
     * <li>Call the {@link RemoteObservationManager#getNextEvent(long)} method
     * waiting for a specified time (5 seconds).
     * <li>If no event was received in the specified time go back to step #1.
     * <li>Extract the unique listener identifier from the remote event and
     * find it in the list of locally registered event listeners. Go back to
     * step #1 if no such listener exists.
     * <li>Convert the remote event list to an <code>EventIterator</code> and
     * call the <code>EventListener.onEvent()</code> method.
     * <li>Go back to step #1.
     * </ol>
     */
    public void run() {
        while (running && session.isLive()) {
            try {
                // ask for an event waiting at most POLL_TIMEOUT milliseconds
                RemoteEventCollection remoteEvent = remote.getNextEvent(POLL_TIMEOUT);

                // poll time out, check running and ask again
                if (remoteEvent == null) {
                    continue;
                }

                // extract the listener id from the remote event and find
                // the locally registered event listener
                Long id = new Long(remoteEvent.getListenerId());
                EventListener listener = (EventListener) listenerMap.get(id);

                // if the listener is not registered (anymore), the event is
                // silently ignored, running is checked and the server asked again
                if (listener == null) {
                    continue;
                }

                // otherwise convert the remote events into an EventIterator
                // and the listener is called
                RemoteEventCollection.RemoteEvent[] remoteEvents = remoteEvent.getEvents();
                EventIterator events = toEvents(remoteEvents);
                try {
                    listener.onEvent(events);
                } catch (Exception e) {
                    log.error("Unexpected failure of Listener " + listener, e);
                }

            } catch (RemoteException re) {
                log.error("Problem handling event. Looking for next one.", re);
            }
        }
    }

    //---------- internal -----------------------------------------------------

    /**
     * Converts an array of {@link RemoteEventCollection.RemoteEvent} instances to an
     * instance of <code>EventIterator</code> suitable to be sent to the
     * event listener.
     *
     * @param remoteEvents array of remote events
     * @return event iterator
     * @throws RemoteException on RMI errors
     */
    private EventIterator toEvents(RemoteEventCollection.RemoteEvent[] remoteEvents)
            throws RemoteException {
        Event[] events = new Event[remoteEvents.length];
        for (int i = 0; i < events.length; i++) {
            events[i] = new JCREvent(remoteEvents[i]);
        }
        return new ArrayEventIterator(events);
    }

    /**
     * The <code>JCREvent</code> class is a simple implementation of the JCR
     * <code>Event</code> interface to be sent to the locally registered
     * event listeners.
     *
     * @author Felix Meschberger
     */
    private static class JCREvent implements Event {

    	/** The adapted remote event. */
    	private final RemoteEventCollection.RemoteEvent remote;
    	
        /**
         * Creates an instance of this class from the contents of the given
         * <code>remoteEvent</code>.
         *
         * @param remoteEvent The {@link RemoteEventCollection.RemoteEvent} instance
         *      providing the data for this event.
         */
        private JCREvent(RemoteEventCollection.RemoteEvent remoteEvent) {
            remote = remoteEvent;
        }

        /** {@inheritDoc} */
        public int getType() {
            try {
                return remote.getType();
            } catch (RemoteException ex) {
                throw new RemoteRuntimeException(ex);
            }
        }

        /** {@inheritDoc} */
        public String getPath() throws RepositoryException {
            try {
                return remote.getPath();
            } catch (RemoteException ex) {
                throw new RemoteRepositoryException(ex);
            }
        }

        /** {@inheritDoc} */
        public String getUserID() {
            try {
                return remote.getUserID();
            } catch (RemoteException ex) {
                throw new RemoteRuntimeException(ex);
            }
        }

        /** {@inheritDoc} */
        public long getDate() throws RepositoryException {
            try {
                return remote.getDate();
            } catch (RemoteException ex) {
                throw new RemoteRepositoryException(ex);
            }
        }

        /** {@inheritDoc} */
        public String getIdentifier() throws RepositoryException {
            try {
                return remote.getIdentifier();
            } catch (RemoteException ex) {
                throw new RemoteRepositoryException(ex);
            }
        }

        /** {@inheritDoc} */
        public Map getInfo() throws RepositoryException {
            try {
                return remote.getInfo();
            } catch (RemoteException ex) {
                throw new RemoteRepositoryException(ex);
            }
        }

        /** {@inheritDoc} */
        public String getUserData() throws RepositoryException {
            try {
                return remote.getUserData();
            } catch (RemoteException ex) {
                throw new RemoteRepositoryException(ex);
            }
        }
    }
}
