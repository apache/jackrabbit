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

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;

/**
 * Each <code>Session</code> instance has its own <code>ObservationManager</code>
 * instance. The class <code>SessionLocalObservationManager</code> implements
 * this behaviour.
 */
public class ObservationManagerImpl implements ObservationManager, EventStateCollectionFactory {

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
     * The <code>ItemManager</code> for this <code>ObservationManager</code>.
     */
    private final ItemManager itemMgr;

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
     * @param itemMgr {@link org.apache.jackrabbit.core.ItemManager} of the passed
     *                <code>Session</code>.
     * @throws NullPointerException if <code>session</code> or <code>itemMgr</code>
     *                              is <code>null</code>.
     */
    public ObservationManagerImpl(ObservationDispatcher dispatcher,
                           SessionImpl session,
                           ItemManager itemMgr) throws NullPointerException {
        if (session == null) {
            throw new NullPointerException("session");
        }
        if (itemMgr == null) {
            throw new NullPointerException("itemMgr");
        }

        this.dispatcher = dispatcher;
        this.session = session;
        this.itemMgr = itemMgr;
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
        EventFilter filter = createEventFilter(eventTypes, absPath,
                isDeep, uuid, nodeTypeName, noLocal);

        dispatcher.addConsumer(new EventConsumer(session, listener, filter));
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
     * @param absPath an absolute path.
     * @param isDeep a <code>boolean</code>.
     * @param uuid array of UUIDs.
     * @param nodeTypeName array of node type names.
     * @param noLocal a <code>boolean</code>.
     * @return the event filter with the given restrictions.
     * @throws RepositoryException if an error occurs.
     */
    public EventFilter createEventFilter(int eventTypes,
                                         String absPath,
                                         boolean isDeep,
                                         String[] uuid,
                                         String[] nodeTypeName,
                                         boolean noLocal)
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

        Path path;
        try {
            path = session.getQPath(absPath).getNormalizedPath();
        } catch (NameException e) {
            String msg = "invalid path syntax: " + absPath;
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
        if (!path.isAbsolute()) {
            throw new RepositoryException("absPath must be absolute");
        }
        NodeId[] ids = null;
        if (uuid != null) {
            ids = new NodeId[uuid.length];
            for (int i = 0; i < uuid.length; i++) {
                ids[i] = NodeId.valueOf(uuid[i]);
            }
        }
        // create filter
        return new EventFilter(itemMgr, session, eventTypes, path,
                isDeep, ids, nodeTypes, noLocal);
    }

    //------------------------------------------< EventStateCollectionFactory >

    /**
     * {@inheritDoc}
     * <p/>
     * Creates an <code>EventStateCollection</code> tied to the session
     * which is attached to this <code>ObservationManager</code> instance.
     */
    public EventStateCollection createEventStateCollection() {
        EventStateCollection esc = new EventStateCollection(dispatcher, session, null);
        esc.setUserData(userData);
        return esc;
    }
}
