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
package org.apache.jackrabbit.jcr.core.observation;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.UnboundedFifoBuffer;
import org.apache.log4j.Logger;
import org.apache.jackrabbit.jcr.core.ItemManager;
import org.apache.jackrabbit.jcr.core.SessionImpl;
import org.apache.jackrabbit.jcr.core.Path;
import org.apache.jackrabbit.jcr.core.MalformedPathException;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * The class <code>ObservationManagerFactory</code> creates new
 * <code>ObservationManager</code> instances for sessions. It also implements the
 * {@link EventDispatcher} interface where {@link EventStateCollection}s can be
 * dispatched to {@link javax.jcr.observation.EventListener}s.
 *
 * @author Marcel Reutegger
 */
public final class ObservationManagerFactory implements EventDispatcher, Runnable {

    /**
     * Logger instance for this class
     */
    private static final Logger log
	    = Logger.getLogger(ObservationManagerFactory.class);

    /**
     * Dummy DispatchAction indicating the notification thread to end
     */
    private static final DispatchAction DISPOSE_MARKER = new DispatchAction(null, null);

    /**
     * Currently active <code>EventConsumer</code>s for notification
     */
    private Set activeConsumers = new HashSet();

    /**
     * Set of <code>EventConsumer</code>s for read only Set access
     */
    private Set readOnlyConsumers;

    /**
     * synchronization monitor for listener changes
     */
    private Object consumerChange = new Object();

    /**
     * Contains the pending events that will be delivered to event listeners
     */
    private Buffer eventQueue
	    = BufferUtils.blockingBuffer(new UnboundedFifoBuffer());

    /**
     * The background notification thread
     */
    private Thread notificationThread;

    /**
     * Creates a new <code>ObservationManagerFactory</code> instance
     * and starts the notification thread deamon.
     */
    public ObservationManagerFactory() {
	notificationThread = new Thread(this, "ObservationManager");
	notificationThread.setDaemon(true);
	notificationThread.start();
    }

    /**
     * Disposes this <code>ObservationManager</code>. This will
     * effectively stop the background notification thread.
     */
    public void dispose() {
	// dispatch dummy event to mark end of notification
	eventQueue.add(DISPOSE_MARKER);
	try {
	    notificationThread.join();
	} catch (InterruptedException e) {
	    // FIXME log exception ?
	}
	log.info("Notification of EventListeners stopped.");
    }

    /**
     * Returns an unmodifieable <code>Set</code> of <code>EventConsumer</code>s.
     *
     * @return <code>Set</code> of <code>EventConsumer</code>s.
     */
    private Set getConsumers() {
	synchronized (consumerChange) {
	    if (readOnlyConsumers == null) {
		readOnlyConsumers = Collections.unmodifiableSet(new HashSet(activeConsumers));
	    }
	    return readOnlyConsumers;
	}
    }

    /**
     * Creates a new <code>session</code> local <code>ObservationManager</code>
     * with an associated <code>NamespaceResolver</code>.
     *
     * @param session the session.
     * @param itemMgr the <code>ItemManager</code> of the <code>session</code>.
     * @return an <code>ObservationManager</code>.
     */
    public ObservationManager createObservationManager(SessionImpl session,
						       ItemManager itemMgr) {
	return new SessionLocalObservationManager(session, itemMgr);
    }


    /**
     * Implements the run method of the background notification
     * thread.
     */
    public void run() {
	DispatchAction action;
	while ((action = (DispatchAction) eventQueue.remove()) != DISPOSE_MARKER) {

	    log.debug("got EventStateCollection");
	    log.debug("event delivery to " + action.eventConsumers.size() + " consumers started...");
	    for (Iterator it = action.eventConsumers.iterator(); it.hasNext();) {
		EventConsumer c = (EventConsumer) it.next();
		try {
		    c.consumeEvents(action.eventStates);
		} catch (Throwable t) {
		    log.error("EventConsumer threw exception.", t);
		    // move on to the next consumer
		}
	    }
	    log.debug("event delivery finished.");

	}
    }

    //-------------------------< EventDispatcher >------------------------------

    /**
     * @see EventDispatcher#dispatchEvents
     */
    public void dispatchEvents(EventStateCollection events) {
	eventQueue.add(new DispatchAction(events, getConsumers()));
    }

    //----------------------------< adapter class >-----------------------------

    /**
     * Each <code>Session</code> instance has its own <code>ObservationManager</code>
     * instance. The class <code>SessionLocalObservationManager</code> implements
     * this behaviour.
     */
    class SessionLocalObservationManager implements ObservationManager {

	/**
	 * The <code>Session</code> this <code>ObservationManager</code>
	 * belongs to.
	 */
	private SessionImpl session;

	/**
	 * The <code>ItemManager</code> for this <code>ObservationManager</code>.
	 */
	private ItemManager itemMgr;

	/**
	 * Creates an <code>ObservationManager</code> instance.
	 *
	 * @param session the <code>Session</code> this ObservationManager
	 *                belongs to.
	 * @param itemMgr {@link org.apache.jackrabbit.jcr.core.ItemManager} of the passed
	 *                <code>Session</code>.
	 * @throws NullPointerException if <code>session</code> or <code>itemMgr</code>
	 *                              is <code>null</code>.
	 */
	SessionLocalObservationManager(SessionImpl session,
				       ItemManager itemMgr) {
	    if (session == null) {
		throw new NullPointerException("session");
	    }
	    if (itemMgr == null) {
		throw new NullPointerException("itemMgr");
	    }

	    this.session = session;
	    this.itemMgr = itemMgr;
	}

	/**
	 * @see ObservationManager#addEventListener
	 */
	public void addEventListener(EventListener listener,
				     long eventTypes,
				     String absPath,
				     boolean isDeep,
				     String[] uuid,
				     String[] nodeTypeName,
				     boolean noLocal)
		throws RepositoryException {

	    // create NodeType instances from names
	    NodeType[] nodeTypes;
	    if (nodeTypeName == null) {
		nodeTypes = null;
	    } else {
		NodeTypeManager ntMgr = session.getNodeTypeManager();
		nodeTypes = new NodeType[nodeTypeName.length];
		for (int i = 0; i < nodeTypes.length; i++) {
		    nodeTypes[i] = ntMgr.getNodeType(nodeTypeName[i]);
		}
	    }

	    Path path;
	    try {
		path = Path.create(absPath, session.getNamespaceResolver(), true);
	    } catch (MalformedPathException mpe) {
		String msg = "invalid path syntax: " + absPath;
		log.error(msg, mpe);
		throw new RepositoryException(msg, mpe);
	    }
	    // create filter
	    EventFilter filter = new EventFilter(itemMgr,
		    session,
		    eventTypes,
		    path,
		    isDeep,
		    uuid,
		    nodeTypes,
		    noLocal);

	    EventConsumer consumer =
		    new EventConsumer(session, listener, filter);

	    synchronized (consumerChange) {
		// remove existing if any
		activeConsumers.remove(consumer);
		// re-add it
		activeConsumers.add(consumer);
		// reset read only consumer set
		readOnlyConsumers = null;
	    }
	}

	/**
	 * @see ObservationManager#removeEventListener(javax.jcr.observation.EventListener)
	 */
	public void removeEventListener(EventListener listener)
		throws RepositoryException {
	    EventConsumer consumer =
		    new EventConsumer(session, listener, EventFilter.BLOCK_ALL);

	    synchronized (consumerChange) {
		activeConsumers.remove(consumer);
		// reset read only listener set
		readOnlyConsumers = null;
	    }
	}

	/**
	 * @see ObservationManager#getRegisteredEventListeners()
	 */
	public EventListenerIterator getRegisteredEventListeners()
		throws RepositoryException {
	    return new EventListenerIteratorImpl(session, getConsumers());
	}
    }
}
