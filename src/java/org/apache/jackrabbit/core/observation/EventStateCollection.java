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
package org.apache.jackrabbit.core.observation;

import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.state.*;
import org.apache.log4j.Logger;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * The <code>EventStateCollection</code> class implements how {@link EventState}
 * objects are created based on the {@link org.apache.jackrabbit.core.state.ItemState}s
 * passed to the {@link #createEventStates} method.
 */
public final class EventStateCollection {

    /**
     * Logger instance for this class
     */
    private static Logger log = Logger.getLogger(EventStateCollection.class);

    /**
     * List of events
     */
    private final List events = new ArrayList();

    /**
     * The <code>ObservationManagerFactory</code> that notifies the EventListeners.
     */
    private final ObservationManagerFactory dispatcher;

    /**
     * The session that created these events
     */
    private final SessionImpl session;

    /**
     * The ItemStateProvider of the session that creates the events.
     */
    private final ItemStateProvider provider;

    /**
     * The HierarchyManager of the session that creates the events.
     */
    private final HierarchyManager hmgr;

    /**
     * Creates a new empty <code>EventStateCollection</code>.
     *
     * @param session the session that created these events.
     */
    EventStateCollection(ObservationManagerFactory dispatcher,
                         SessionImpl session,
                         ItemStateProvider provider,
                         HierarchyManager hmgr) {
        this.dispatcher = dispatcher;
        this.session = session;
        this.provider = provider;
        this.hmgr = hmgr;
    }

    /**
     * Creates {@link EventState}s for the {@link org.apache.jackrabbit.core.state.ItemState state}
     * instances contained in the specified collection.
     *
     * @param states collection of transient <code>ItemState</code> for whom
     *               to create {@link EventState}s.
     * @see #createEventStates(org.apache.jackrabbit.core.state.ItemState)
     */
    public void createEventStates(Collection states)
            throws RepositoryException {
        Iterator iter = states.iterator();
        while (iter.hasNext()) {
            createEventStates((ItemState) iter.next());
        }
    }

    /**
     * Creates {@link EventState}s for the passed {@link org.apache.jackrabbit.core.state.ItemState state}
     * instance.
     *
     * @param state the transient <code>ItemState</code> for whom
     *              to create {@link EventState}s.
     */
    public void createEventStates(ItemState state)
            throws RepositoryException {
        int status = state.getStatus();

        if (status == ItemState.STATUS_EXISTING_MODIFIED
                || status == ItemState.STATUS_NEW) {

            if (state.isNode()) {
                NodeState currentNode = (NodeState) state;
                QName nodeTypeName = currentNode.getNodeTypeName();
                NodeTypeImpl nodeType = session.getNodeTypeManager().getNodeType(nodeTypeName);
                Path parentPath = hmgr.getPath(currentNode.getId());

                // 1) check added properties
                List addedProperties = currentNode.getAddedPropertyEntries();
                for (Iterator it = addedProperties.iterator(); it.hasNext();) {
                    NodeState.PropertyEntry prop = (NodeState.PropertyEntry) it.next();
                    events.add(EventState.propertyAdded(currentNode.getUUID(),
                            parentPath,
                            Path.create(prop.getName(), 0),
                            nodeType,
                            session));
                }

                // 2) check removed properties
                List removedProperties = currentNode.getRemovedPropertyEntries();
                for (Iterator it = removedProperties.iterator(); it.hasNext();) {
                    NodeState.PropertyEntry prop = (NodeState.PropertyEntry) it.next();
                    events.add(EventState.propertyRemoved(currentNode.getUUID(),
                            parentPath,
                            Path.create(prop.getName(), 0),
                            nodeType,
                            session));
                }

                // 3) check for added nodes
                List addedNodes = currentNode.getAddedChildNodeEntries();
                for (Iterator it = addedNodes.iterator(); it.hasNext();) {
                    NodeState.ChildNodeEntry child = (NodeState.ChildNodeEntry) it.next();
                    events.add(EventState.childNodeAdded(currentNode.getUUID(),
                            parentPath,
                            child.getUUID(),
                            Path.create(child.getName(), child.getIndex()),
                            nodeType,
                            session));
                }

                // 4) check for removed nodes
                List removedNodes = currentNode.getRemovedChildNodeEntries();
                for (Iterator it = removedNodes.iterator(); it.hasNext();) {
                    NodeState.ChildNodeEntry child = (NodeState.ChildNodeEntry) it.next();
                    events.add(EventState.childNodeRemoved(currentNode.getUUID(),
                            parentPath,
                            child.getUUID(),
                            Path.create(child.getName(), child.getIndex()),
                            nodeType,
                            session));
                }
            } else {
                // only add property changed event if property is existing
                if (state.getStatus() == ItemState.STATUS_EXISTING_MODIFIED) {
                    NodeId parentId = new NodeId(state.getParentUUID());
                    try {
                        NodeState parentState = (NodeState) provider.getItemState(parentId);
                        Path parentPath = hmgr.getPath(parentId);
                        events.add(EventState.propertyChanged(state.getParentUUID(),
                                parentPath,
                                Path.create(((PropertyState) state).getName(), 0),
                                session.getNodeTypeManager().getNodeType(parentState.getNodeTypeName()),
                                session));
                    } catch (ItemStateException e) {
                        // should never happen
                        log.error("internal error: item state exception", e);
                    }
                }
            }
        } else if (status == ItemState.STATUS_EXISTING_REMOVED) {
            if (state.isNode()) {
                // zombie nodes
                NodeState currentNode = (NodeState) state;
                QName nodeTypeName = currentNode.getNodeTypeName();
                NodeTypeImpl nodeType = session.getNodeTypeManager().getNodeType(nodeTypeName);

                // FIXME replace by HierarchyManager.getPath(ItemId id, boolean includeZombie)
                // when available.
                Path[] parentPaths = hmgr.getAllPaths(currentNode.getId(), true);   // include zombie
                for (int i = 0; i < parentPaths.length; i++) {
                    List removedNodes = currentNode.getRemovedChildNodeEntries();
                    for (Iterator it = removedNodes.iterator(); it.hasNext();) {
                        NodeState.ChildNodeEntry child = (NodeState.ChildNodeEntry) it.next();
                        events.add(EventState.childNodeRemoved(currentNode.getUUID(),
                                parentPaths[i],
                                child.getUUID(),
                                Path.create(child.getName(), child.getIndex()),
                                nodeType,
                                session));
                    }
                }
            }
        }
    }

    /**
     * Prepares the events for dispatching.
     */
    public void prepare() throws RepositoryException {
        dispatcher.prepareEvents(this);
    }

    /**
     * Dispatches the events to the {@link javax.jcr.observation.EventListener}s.
     */
    public void dispatch() {
        dispatcher.dispatchEvents(this);
    }

    /**
     * Returns an iterator over {@link EventState} instance.
     *
     * @return an iterator over {@link EventState} instance.
     */
    Iterator iterator() {
        return events.iterator();
    }
}
