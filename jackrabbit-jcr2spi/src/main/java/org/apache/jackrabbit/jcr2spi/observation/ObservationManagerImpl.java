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
package org.apache.jackrabbit.jcr2spi.observation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.observation.EventJournal;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;

import org.apache.jackrabbit.commons.iterator.EventListenerIteratorAdapter;
import org.apache.jackrabbit.jcr2spi.WorkspaceManager;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ObservationManagerImpl</code>...
 */
public class ObservationManagerImpl implements ObservationManager, InternalEventListener {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(ObservationManagerImpl.class);

    /**
     * The workspace manager.
     */
    private final WorkspaceManager wspManager;

    /**
     * The name and path resolver associated with the session this observation
     * manager belongs to.
     */
    private final NamePathResolver resolver;

    /**
     * The <code>NodeTypeRegistry</code> of the session.
     */
    private final NodeTypeRegistry ntRegistry;

    /**
     * Live mapping of <code>EventListener</code> to <code>EventFilter</code>.
     */
    private final Map<EventListener, EventFilter> subscriptions = new HashMap<EventListener, EventFilter>();

    /**
     * A read only mapping of <code>EventListener</code> to <code>EventFilter</code>.
     */
    private Map<EventListener, EventFilter> readOnlySubscriptions;

    /**
     * Creates a new observation manager for <code>session</code>.
     *
     * @param wspManager the WorkspaceManager.
     * @param resolver   the name path resolver for this session.
     * @param ntRegistry The <code>NodeTypeRegistry</code> of the session.
     */
    public ObservationManagerImpl(WorkspaceManager wspManager,
                                  NamePathResolver resolver,
                                  NodeTypeRegistry ntRegistry) {
        this.wspManager = wspManager;
        this.resolver = resolver;
        this.ntRegistry = ntRegistry;
    }

    public void addEventListener(EventListener listener,
                                 int eventTypes,
                                 String absPath,
                                 boolean isDeep,
                                 String[] uuids,
                                 String[] nodeTypeNames,
                                 boolean noLocal) throws RepositoryException {
        EventFilter filter = createEventFilter(eventTypes, absPath,
                isDeep, uuids, nodeTypeNames, noLocal);
        synchronized (subscriptions) {
            subscriptions.put(listener, filter);
            readOnlySubscriptions = null;
        }

        if (subscriptions.size() == 1) {
            wspManager.addEventListener(this);
        } else {
            wspManager.updateEventFilters();
        }
    }

    public void removeEventListener(EventListener listener) throws RepositoryException {
        synchronized (subscriptions) {
            if (subscriptions.remove(listener) != null) {
                readOnlySubscriptions = null;
            }
        }
        if (subscriptions.size() == 0) {
            wspManager.removeEventListener(this);
        } else {
            wspManager.updateEventFilters();
        }
    }

    public EventListenerIterator getRegisteredEventListeners() throws RepositoryException {
        Map<EventListener, EventFilter> activeListeners;
        synchronized (subscriptions) {
            ensureReadOnlyMap();
            activeListeners = readOnlySubscriptions;
        }
        return new EventListenerIteratorAdapter(activeListeners.keySet());
    }

    /**
     * @see javax.jcr.observation.ObservationManager#getEventJournal()
     */
    public EventJournal getEventJournal() throws RepositoryException {
        return getEventJournal(Event.ALL_TYPES, "/", true, null, null);
    }

    /**
     * @see javax.jcr.observation.ObservationManager#getEventJournal(int, String, boolean, String[], String[])
     */
    public EventJournal getEventJournal(
            int eventTypes, String absPath, boolean isDeep,
            String[] uuid, String[] nodeTypeName)
            throws RepositoryException {
        EventFilter filter = createEventFilter(eventTypes, absPath, isDeep, uuid, nodeTypeName, false);
        return new EventJournalImpl(wspManager, filter, resolver);
    }

    /**
     * @see javax.jcr.observation.ObservationManager#setUserData(String)
     */
    public void setUserData(String userData) throws RepositoryException {
        wspManager.setUserData(userData);
    }

    //-----------------------< InternalEventListener >--------------------------

    public Collection<EventFilter> getEventFilters() {
        List<EventFilter> filters = new ArrayList<EventFilter>();
        synchronized (subscriptions) {
            ensureReadOnlyMap();
            filters.addAll(readOnlySubscriptions.values());
        }
        return filters;
    }

    public void onEvent(EventBundle eventBundle) {
        // get active listeners
        Map<EventListener, EventFilter> activeListeners;
        synchronized (subscriptions) {
            ensureReadOnlyMap();
            activeListeners = readOnlySubscriptions;
        }
        for (Map.Entry<EventListener, EventFilter> entry : activeListeners.entrySet()) {
            EventListener listener = entry.getKey();
            EventFilter filter = entry.getValue();
            FilteredEventIterator eventIter = new FilteredEventIterator(
                    eventBundle.getEvents(), eventBundle.isLocal(), filter,
                    resolver, wspManager.getIdFactory());
            if (eventIter.hasNext()) {
                try {
                    listener.onEvent(eventIter);
                } catch (Throwable t) {
                    log.warn("EventConsumer threw exception: " + t.toString());
                    log.debug("Stacktrace: ", t);
                    // move on to the next listener
                }
            }
        }
    }

    //-------------------------< internal >-------------------------------------

    /**
     * Ensures that {@link #readOnlySubscriptions} is set. Callers of this
     * method must own {@link #subscriptions} as a monitor to avoid concurrent
     * access to {@link #subscriptions}.
     */
    private void ensureReadOnlyMap() {
        if (readOnlySubscriptions == null) {
            readOnlySubscriptions = new HashMap<EventListener, EventFilter>(subscriptions);
        }
    }

    /**
     * Creates an SPI event filter from the given list of constraints.
     *
     * @param eventTypes    the event types.
     * @param absPath       an absolute path.
     * @param isDeep        whether to include events for descendant items of
     *                      the node at absPath.
     * @param uuids         uuid filters.
     * @param nodeTypeNames node type filters.
     * @param noLocal       whether to exclude changes from the local session.
     * @return the SPI event filter instance.
     * @throws RepositoryException if an error occurs while creating the event
     *                             filter.
     */
    private EventFilter createEventFilter(int eventTypes,
                                          String absPath,
                                          boolean isDeep,
                                          String[] uuids,
                                          String[] nodeTypeNames,
                                          boolean noLocal)
            throws RepositoryException {
        Path path;
        try {
            path = resolver.getQPath(absPath).getCanonicalPath();
        } catch (NameException e) {
            throw new RepositoryException("Malformed path: " + absPath);
        }

        // create NodeType instances from names
        Name[] qNodeTypeNames;
        if (nodeTypeNames == null) {
            qNodeTypeNames = null;
        } else {
            try {
                qNodeTypeNames = new Name[nodeTypeNames.length];
                for (int i = 0; i < nodeTypeNames.length; i++) {
                    Name ntName = resolver.getQName(nodeTypeNames[i]);
                    if (!ntRegistry.isRegistered(ntName)) {
                        throw new RepositoryException("unknown node type: " + nodeTypeNames[i]);
                    }
                    qNodeTypeNames[i] = ntName;
                }
            } catch (NameException e) {
                throw new RepositoryException(e.getMessage());
            }
        }

        return wspManager.createEventFilter(eventTypes, path, isDeep,
                uuids, qNodeTypeNames, noLocal);
    }
}
