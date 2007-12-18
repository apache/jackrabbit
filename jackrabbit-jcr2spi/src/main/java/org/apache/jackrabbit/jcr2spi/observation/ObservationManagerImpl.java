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

import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.jcr2spi.WorkspaceManager;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.commons.iterator.EventListenerIteratorAdapter;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

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
     * The session this observation manager belongs to.
     */
    private final NamePathResolver resolver;

    /**
     * The <code>NodeTypeRegistry</code> of the session.
     */
    private final NodeTypeRegistry ntRegistry;

    /**
     * Live mapping of <code>EventListener</code> to <code>EventFilter</code>.
     */
    private final Map subscriptions = new HashMap();

    /**
     * A read only mapping of <code>EventListener</code> to <code>EventFilter</code>.
     */
    private Map readOnlySubscriptions;

    /**
     * Creates a new observation manager for <code>session</code>.
     * @param wspManager the WorkspaceManager.
     * @param resolver
     * @param ntRegistry The <code>NodeTypeRegistry</code> of the session.
     */
    public ObservationManagerImpl(WorkspaceManager wspManager, NamePathResolver resolver,
                                  NodeTypeRegistry ntRegistry) {
        this.wspManager = wspManager;
        this.resolver = resolver;
        this.ntRegistry = ntRegistry;
    }

    /**
     * @inheritDoc
     */
    public void addEventListener(EventListener listener,
                                 int eventTypes,
                                 String absPath,
                                 boolean isDeep,
                                 String[] uuids,
                                 String[] nodeTypeNames,
                                 boolean noLocal) throws RepositoryException {
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

        EventFilter filter = wspManager.createEventFilter(eventTypes, path, isDeep, uuids, qNodeTypeNames, noLocal);
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

    /**
     * @inheritDoc
     */
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

    /**
     * @inheritDoc
     */
    public EventListenerIterator getRegisteredEventListeners() throws RepositoryException {
        Map activeListeners;
        synchronized (subscriptions) {
            ensureReadOnlyMap();
            activeListeners = readOnlySubscriptions;
        }
        return new EventListenerIteratorAdapter(activeListeners.keySet());
    }

    //-----------------------< InternalEventListener >--------------------------

    public Collection getEventFilters() {
        List filters = new ArrayList();
        synchronized (subscriptions) {
            ensureReadOnlyMap();
            filters.addAll(readOnlySubscriptions.values());
        }
        return filters;
    }

    public void onEvent(EventBundle eventBundle) {
        // get active listeners
        Map activeListeners;
        synchronized (subscriptions) {
            ensureReadOnlyMap();
            activeListeners = readOnlySubscriptions;
        }
        for (Iterator it = activeListeners.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            EventListener listener = (EventListener) entry.getKey();
            EventFilter filter = (EventFilter) entry.getValue();
            FilteredEventIterator eventIter = new FilteredEventIterator(eventBundle, filter, resolver);
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
            readOnlySubscriptions = new HashMap(subscriptions);
        }
    }

}
