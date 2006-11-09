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
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.PathFormat;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.EventBundle;
import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.util.IteratorHelper;
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
    private final NamespaceResolver nsResolver;

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
     * @param nsResolver NamespaceResolver to be used by this observation manager
     * is based on.
     * @param ntRegistry The <code>NodeTypeRegistry</code> of the session.
     */
    public ObservationManagerImpl(WorkspaceManager wspManager, NamespaceResolver nsResolver, NodeTypeRegistry ntRegistry) {
        this.wspManager = wspManager;
        this.nsResolver = nsResolver;
        this.ntRegistry = ntRegistry;
        this.wspManager.addEventListener(this);
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
            path = PathFormat.parse(absPath, nsResolver).getCanonicalPath();
        } catch (MalformedPathException e) {
            throw new RepositoryException("Malformed path: " + absPath);
        }

        // create NodeType instances from names
        QName[] nodeTypeQNames;
        if (nodeTypeNames == null) {
            nodeTypeQNames = null;
        } else {
            try {
                nodeTypeQNames = new QName[nodeTypeNames.length];
                for (int i = 0; i < nodeTypeNames.length; i++) {
                    QName ntName = NameFormat.parse(nodeTypeNames[i], nsResolver);
                    if (!ntRegistry.isRegistered(ntName)) {
                        throw new RepositoryException("unknown node type: " + nodeTypeNames[i]);
                    }
                    nodeTypeQNames[i] = ntName;
                }
            } catch (NameException e) {
                throw new RepositoryException(e.getMessage());
            }
        }

        synchronized (subscriptions) {
            EventFilter filter = wspManager.createEventFilter(eventTypes, path, isDeep, uuids, nodeTypeQNames, noLocal);
            subscriptions.put(listener, filter);
            readOnlySubscriptions = null;
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
        return new ListenerIterator(activeListeners.keySet());
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
        for (Iterator it = activeListeners.keySet().iterator(); it.hasNext(); ) {
            EventListener listener = (EventListener) it.next();
            EventFilter filter = (EventFilter) activeListeners.get(listener);
            FilteredEventIterator eventIter = new FilteredEventIterator(eventBundle, filter, nsResolver);
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

    private static final class ListenerIterator extends IteratorHelper
            implements EventListenerIterator {

        public ListenerIterator(Collection c) {
            super(c);
        }

        public EventListener nextEventListener() {
            return (EventListener) next();
        }
    }
}
