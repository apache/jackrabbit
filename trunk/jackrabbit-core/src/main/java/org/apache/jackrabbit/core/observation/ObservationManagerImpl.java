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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.EventJournal;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;

import org.apache.jackrabbit.api.observation.JackrabbitEventFilter;
import org.apache.jackrabbit.api.observation.JackrabbitObservationManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.cluster.ClusterNode;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Each <code>Session</code> instance has its own <code>ObservationManager</code>
 * instance. The class <code>SessionLocalObservationManager</code> implements
 * this behaviour.
 */
public class ObservationManagerImpl implements EventStateCollectionFactory,
        JackrabbitObservationManager {

    /**
     * The logger instance of this class
     */
    private static final Logger log = LoggerFactory.getLogger(ObservationManagerImpl.class);

    /**
     * The <code>Session</code> this <code>ObservationManager</code>
     * belongs to.
     */
    private final SessionImpl session;

    /**
     * The cluster node where this session is running.
     */
    private final ClusterNode clusterNode;

    /**
     * The <code>ObservationDispatcher</code>
     */
    private final ObservationDispatcher dispatcher;

    /**
     * The currently set user data.
     */
    private String userData;

    static {
        // preload EventListenerIteratorImpl to prevent classloader issues during shutdown
        EventListenerIteratorImpl.class.hashCode();
    }

    /**
     * Creates an <code>ObservationManager</code> instance.
     *
     * @param dispatcher observation dispatcher
     * @param session the <code>Session</code> this ObservationManager
     *                belongs to.
     * @param clusterNode
     * @throws NullPointerException if <code>dispatcher</code>, <code>session</code>
     *                              or <code>clusterNode</code> is <code>null</code>.
     */
    public ObservationManagerImpl(
            ObservationDispatcher dispatcher, SessionImpl session,
            ClusterNode clusterNode) {
        if (dispatcher == null) {
            throw new NullPointerException("dispatcher");
        }
        if (session == null) {
            throw new NullPointerException("session");
        }

        this.dispatcher = dispatcher;
        this.session = session;
        this.clusterNode = clusterNode;
    }

    /**
     * {@inheritDoc}
     */
    public void addEventListener(EventListener listener,
                                 int eventTypes,
                                 String absPath,
                                 boolean isDeep,
                                 String[] uuid,
                                 String[] nodeTypeName,
                                 boolean noLocal)
            throws RepositoryException {

        // create filter
        EventFilter filter = createEventFilter(eventTypes, Collections.singletonList(absPath),
                isDeep, uuid, nodeTypeName, noLocal, false, false);

        dispatcher.addConsumer(new EventConsumer(session, listener, filter));
    }

    @Override
    public void addEventListener(EventListener listener, JackrabbitEventFilter filter)
            throws RepositoryException {

        String[] excludedPaths = filter.getExcludedPaths();
        if (excludedPaths.length > 0) {
            log.warn("JackrabbitEventFilter excludedPaths is not implemented and will be ignored: {}",
                    Arrays.toString(excludedPaths));
        }

        List<String> absPaths = new ArrayList<String>(Arrays.asList(filter.getAdditionalPaths()));
        if (filter.getAbsPath() != null) {
            absPaths.add(filter.getAbsPath());
        }

        EventFilter f = createEventFilter(filter.getEventTypes(), absPaths,
                filter.getIsDeep(), filter.getIdentifiers(), filter.getNodeTypes(),
                filter.getNoLocal(), filter.getNoExternal(), filter.getNoInternal());

        dispatcher.addConsumer(new EventConsumer(session, listener, f));
    }

    /**
     * {@inheritDoc}
     */
    public void removeEventListener(EventListener listener)
            throws RepositoryException {
        dispatcher.removeConsumer(
                new EventConsumer(session, listener, EventFilter.BLOCK_ALL));

    }

    /**
     * {@inheritDoc}
     */
    public EventListenerIterator getRegisteredEventListeners()
            throws RepositoryException {
        return new EventListenerIteratorImpl(
                session,
                dispatcher.getSynchronousConsumers(),
                dispatcher.getAsynchronousConsumers());
    }

    /**
     * {@inheritDoc}
     */
    public void setUserData(String userData) throws RepositoryException {
        this.userData = userData;
    }

    /**
     * @return the currently set user data.
     */
    String getUserData() {
        return userData;
    }

    /**
     * Unregisters all EventListeners.
     */
    public void dispose() {
        try {
            EventListenerIterator it = getRegisteredEventListeners();
            while (it.hasNext()) {
                EventListener l = it.nextEventListener();
                log.debug("removing EventListener: " + l);
                removeEventListener(l);
            }
        } catch (RepositoryException e) {
            log.error("Internal error: Unable to dispose ObservationManager.", e);
        }

    }

    /**
     * Creates a new event filter with the given restrictions.
     *
     * @param eventTypes A combination of one or more event type constants encoded as a bitmask.
     * @param absPaths absolute paths.
     * @param isDeep a <code>boolean</code>.
     * @param uuid array of UUIDs.
     * @param nodeTypeName array of node type names.
     * @param noLocal a <code>boolean</code>.
     * @param noExternal a <code>boolean</code>.
     * @param noInternal a <code>boolean</code>.
     * @return the event filter with the given restrictions.
     * @throws RepositoryException if an error occurs.
     */
    public EventFilter createEventFilter(int eventTypes,
                                         List<String> absPaths,
                                         boolean isDeep,
                                         String[] uuid,
                                         String[] nodeTypeName,
                                         boolean noLocal,
                                         boolean noExternal,
                                         boolean noInternal)
            throws RepositoryException {
        // create NodeType instances from names
        NodeTypeImpl[] nodeTypes;
        if (nodeTypeName == null) {
            nodeTypes = null;
        } else {
            NodeTypeManagerImpl ntMgr = session.getNodeTypeManager();
            nodeTypes = new NodeTypeImpl[nodeTypeName.length];
            for (int i = 0; i < nodeTypes.length; i++) {
                nodeTypes[i] = (NodeTypeImpl) ntMgr.getNodeType(nodeTypeName[i]);
            }
        }

        List<Path> paths = new ArrayList<Path>();
        for (String absPath : absPaths) {
            try {
                Path normalizedPath = session.getQPath(absPath).getNormalizedPath();
                if (!normalizedPath.isAbsolute()) {
                    throw new RepositoryException("absPath must be absolute");
                }
                paths.add(normalizedPath);
            } catch (NameException e) {
                String msg = "invalid path syntax: " + absPath;
                log.debug(msg);
                throw new RepositoryException(msg, e);

        }            }
        NodeId[] ids = null;
        if (uuid != null) {
            ids = new NodeId[uuid.length];
            for (int i = 0; i < uuid.length; i++) {
                ids[i] = NodeId.valueOf(uuid[i]);
            }
        }
        // create filter
        return new EventFilter(
                session, eventTypes, paths, isDeep, ids, nodeTypes, noLocal, noExternal, noInternal);
    }

    /**
     * Returns the event journal for this workspace. The events are filtered
     * according to the passed criteria.
     *
     * @param eventTypes A combination of one or more event type constants encoded as a bitmask.
     * @param absPath an absolute path.
     * @param isDeep a <code>boolean</code>.
     * @param uuid array of UUIDs.
     * @param nodeTypeName array of node type names.
     * @return the event journal for this repository.
     * @throws UnsupportedRepositoryOperationException if this repository does
     *          not support an event journal (cluster journal disabled).
     * @throws RepositoryException if another error occurs.
     * @see ObservationManager#getEventJournal(int, String, boolean, String[], String[])
     */
    public EventJournal getEventJournal(
            int eventTypes, String absPath, boolean isDeep,
            String[] uuid, String[] nodeTypeName)
            throws RepositoryException {
        if (clusterNode == null) {
            throw new UnsupportedRepositoryOperationException(
                    "Event journal is only available in cluster deployments");
        }

        if (!session.isAdmin()) {
            throw new AccessDeniedException("Only administrator session may access EventJournal");
        }

        EventFilter filter = createEventFilter(
                eventTypes, Collections.singletonList(absPath), isDeep, uuid, nodeTypeName, false, false, false);
        return new EventJournalImpl(
                filter, clusterNode.getJournal(), clusterNode.getId(), session);
    }

    /**
     * Returns an unfiltered event journal for this workspace.
     *
     * @return the event journal for this repository.
     * @throws UnsupportedRepositoryOperationException if this repository does
     *          not support an event journal (cluster journal disabled).
     * @throws RepositoryException if another error occurs.
     */
    public EventJournal getEventJournal() throws RepositoryException {
        return getEventJournal(-1, "/", true, null, null);
    }

    //------------------------------------------< EventStateCollectionFactory >

    /**
     * {@inheritDoc}
     * <p>
     * Creates an <code>EventStateCollection</code> tied to the session
     * which is attached to this <code>ObservationManager</code> instance.
     */
    public EventStateCollection createEventStateCollection() {
        EventStateCollection esc = new EventStateCollection(dispatcher, session, null);
        esc.setUserData(userData);
        return esc;
    }

}
