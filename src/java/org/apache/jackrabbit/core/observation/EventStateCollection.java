/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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

import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.Path;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.MalformedPathException;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.log4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.PathNotFoundException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;

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
                         HierarchyManager hmgr) {
        this.dispatcher = dispatcher;
        this.session = session;
        this.hmgr = hmgr;
    }

    /**
     * Creates {@link EventState} instances from <code>ItemState</code>
     * <code>changes</code>.
     * @param changes the changes on <code>ItemState</code>s.
     * @param provider an <code>ItemStateProvider</code> to provide <code>ItemState</code>
     * of items that are not contained in the <code>changes</code> collection.
     * @throws ItemStateException if an error occurs while creating events
     * states for the item state changes.
     */
    public void createEventStates(ChangeLog changes, ItemStateManager provider) throws ItemStateException {
        for (Iterator it = changes.modifiedStates(); it.hasNext();) {
            ItemState state = (ItemState) it.next();
            if (state.isNode()) {
                // node changed
                // covers the following cases:
                // 1) property added
                // 2) property removed
                // 3) child node added
                // 4) child node removed
                // 5) node moved
                // cases 1) and 2) are detected with added and deleted states
                // on the PropertyState itself.
                // cases 3) and 4) are detected with added and deleted states
                // on the NodeState itself.
                // in case 5) two or three nodes change. two nodes are changed
                // when a child node is renamed. three nodes are changed when
                // a node is really moved. In any case we are only interested in
                // the node that actually got moved.
                NodeState n = (NodeState) state;
                if (n.getAddedParentUUIDs().size() > 0 && n.getRemovedParentUUIDs().size() > 0) {
                    // node moved
                    // generate node removed & node added event
                    String oldParentUUID = (String) n.getRemovedParentUUIDs().get(0);
                    NodeTypeImpl nodeType = null;
                    try {
                        nodeType = session.getNodeTypeManager().getNodeType(n.getNodeTypeName());
                    } catch (NoSuchNodeTypeException e) {
                        // should never happen actually
                        String msg = "Item " + state.getId() + " has unknown node type: " + n.getNodeTypeName();
                        log.error(msg);
                        throw new ItemStateException(msg, e);
                    }
                    // FIXME find more efficient way
                    Path newPath = null;
                    Path[] allPaths = null;
                    try {
                        newPath = hmgr.getPath(n.getId());
                        allPaths = hmgr.getAllPaths(n.getId(), true);
                    } catch (RepositoryException e) {
                        // should never happen actually
                        String msg = "Unable to resolve path for item: " + n.getId();
                        log.error(msg);
                        throw new ItemStateException(msg, e);
                    }
                    List paths = new ArrayList(Arrays.asList(allPaths));
                    paths.remove(newPath);
                    if (paths.size() > 0) {
                        Path removedPath = (Path) paths.get(0);
                        Path parentPath = null;
                        try {
                            parentPath = removedPath.getAncestor(1);
                        } catch (PathNotFoundException e) {
                            // should never happen actually, root node cannot
                            // be removed, thus path has always a parent
                            String msg = "Path " + removedPath + " has no parent";
                            log.error(msg);
                            throw new ItemStateException(msg, e);
                        }
                        events.add(EventState.childNodeRemoved(oldParentUUID,
                                parentPath,
                                n.getUUID(),
                                removedPath.getNameElement(),
                                nodeType,
                                session));

                        String newParentUUID = (String) n.getAddedParentUUIDs().get(0);
                        try {
                            parentPath = newPath.getAncestor(1);
                        } catch (PathNotFoundException e) {
                            // should never happen actually, root node cannot
                            // be 'added', thus path has always a parent
                            String msg = "Path " + removedPath + " has no parent";
                            log.error(msg);
                            throw new ItemStateException(msg, e);
                        }
                        events.add(EventState.childNodeAdded(newParentUUID,
                                parentPath,
                                n.getUUID(),
                                newPath.getNameElement(),
                                nodeType,
                                session));
                    } else {
                        log.error("Unable to calculate old path of moved node");
                    }
                } else {
                    // a moved node always has a modified parent node
                    NodeState parent = null;
                    try {
                        // root node does not have a parent UUID
                        if (state.getParentUUID() != null) {
                            parent = (NodeState) changes.get(new NodeId(state.getParentUUID()));
                        }
                    } catch (NoSuchItemStateException e) {
                        // should never happen actually. this would mean
                        // the parent of this modified node is deleted
                        String msg = "Parent of node " + state.getId() + " is deleted.";
                        log.error(msg);
                        throw new ItemStateException(msg, e);
                    }
                    if (parent != null) {
                        // check if node has been renamed
                        NodeState.ChildNodeEntry moved = null;
                        for (Iterator removedNodes = parent.getRemovedChildNodeEntries().iterator(); removedNodes.hasNext();) {
                            NodeState.ChildNodeEntry child = (NodeState.ChildNodeEntry) removedNodes.next();
                            if (child.getUUID().equals(n.getUUID())) {
                                // found node re-added with different name
                                moved = child;
                            }
                        }
                        if (moved != null) {
                            NodeTypeImpl nodeType = null;
                            try {
                                nodeType = session.getNodeTypeManager().getNodeType(n.getNodeTypeName());
                            } catch (NoSuchNodeTypeException e) {
                                // should never happen actually
                                String msg = "Item " + state.getId() + " has unknown node type: " + n.getNodeTypeName();
                                log.error(msg);
                                throw new ItemStateException(msg, e);
                            }
                            Path newPath = null;
                            Path parentPath = null;
                            Path oldPath = null;
                            try {
                                newPath = hmgr.getPath(state.getId());
                                parentPath = newPath.getAncestor(1);
                                oldPath = null;
                                if (moved.getIndex() == 0) {
                                    oldPath = Path.create(parentPath, moved.getName(), false);
                                } else {
                                    oldPath = Path.create(parentPath, moved.getName(), moved.getIndex(), false);
                                }
                            } catch (RepositoryException e) {
                                // should never happen actually
                                String msg = "Unable to resolve path for item: " + state.getId();
                                log.error(msg);
                                throw new ItemStateException(msg, e);
                            } catch (MalformedPathException e) {
                                // should never happen actually
                                String msg = "Malformed path for item: " + state.getId();
                                log.error(msg);
                                throw new ItemStateException(msg, e);
                            }
                            events.add(EventState.childNodeRemoved(parent.getUUID(),
                                    parentPath,
                                    n.getUUID(),
                                    oldPath.getNameElement(),
                                    nodeType,
                                    session));
                            events.add(EventState.childNodeAdded(parent.getUUID(),
                                    parentPath,
                                    n.getUUID(),
                                    newPath.getNameElement(),
                                    nodeType,
                                    session));
                        }
                    }
                }
            } else {
                // property changed
                Path path = null;
                Path parentPath = null;
                try {
                    path = hmgr.getPath(state.getId());
                    parentPath = path.getAncestor(1);
                } catch (RepositoryException e) {
                    // should never happen actually
                    String msg = "Unable to resolve path for item: " + state.getId();
                    log.error(msg);
                    throw new ItemStateException(msg, e);
                }
                NodeState parent = (NodeState) provider.getItemState(new NodeId(state.getParentUUID()));
                NodeTypeImpl nodeType = null;
                try {
                    nodeType = session.getNodeTypeManager().getNodeType(parent.getNodeTypeName());
                } catch (NoSuchNodeTypeException e) {
                    // should never happen actually
                    String msg = "Item " + parent.getId() + " has unknown node type: " + parent.getNodeTypeName();
                    log.error(msg);
                    throw new ItemStateException(msg, e);
                }
                events.add(EventState.propertyChanged(state.getParentUUID(),
                        parentPath,
                        path.getNameElement(),
                        nodeType,
                        session));
            }
        }
        for (Iterator it = changes.addedStates(); it.hasNext();) {
            ItemState state = (ItemState) it.next();
            if (state.isNode()) {
                // node created
                NodeState n = (NodeState) state;
                NodeTypeImpl nodeType = null;
                try {
                    nodeType = session.getNodeTypeManager().getNodeType(n.getNodeTypeName());
                } catch (NoSuchNodeTypeException e) {
                    // should never happen actually
                    String msg = "Item " + state.getId() + " has unknown node type: " + n.getNodeTypeName();
                    log.error(msg);
                    throw new ItemStateException(msg, e);
                }
                Path path = null;
                Path parentPath = null;
                try {
                    path = hmgr.getPath(n.getId());
                    parentPath = path.getAncestor(1);
                } catch (RepositoryException e) {
                    // should never happen actually
                    String msg = "Unable to resolve path for item: " + n.getId();
                    log.error(msg);
                    throw new ItemStateException(msg, e);
                }
                events.add(EventState.childNodeAdded(n.getParentUUID(),
                        parentPath,
                        n.getUUID(),
                        path.getNameElement(),
                        nodeType,
                        session));
            } else {
                // property created / set
                NodeState n = (NodeState) changes.get(new NodeId(state.getParentUUID()));
                NodeTypeImpl nodeType = null;
                try {
                    nodeType = session.getNodeTypeManager().getNodeType(n.getNodeTypeName());
                } catch (NoSuchNodeTypeException e) {
                    // should never happen actually
                    String msg = "Item " + state.getId() + " has unknown node type: " + n.getNodeTypeName();
                    log.error(msg);
                    throw new ItemStateException(msg, e);
                }
                Path path = null;
                Path parentPath = null;
                try {
                    path = hmgr.getPath(state.getId());
                    parentPath = path.getAncestor(1);
                } catch (RepositoryException e) {
                    // should never happen actually
                    String msg = "Unable to resolve path for item: " + n.getId();
                    log.error(msg);
                    throw new ItemStateException(msg, e);
                }
                events.add(EventState.propertyAdded(state.getParentUUID(),
                        parentPath,
                        path.getNameElement(),
                        nodeType,
                        session));
            }
        }
        for (Iterator it = changes.deletedStates(); it.hasNext();) {
            ItemState state = (ItemState) it.next();
            if (state.isNode()) {
                // node deleted
                NodeState n = (NodeState) state;
                NodeTypeImpl nodeType = null;
                try {
                    nodeType = session.getNodeTypeManager().getNodeType(n.getNodeTypeName());
                } catch (NoSuchNodeTypeException e) {
                    // should never happen actually
                    String msg = "Item " + state.getId() + " has unknown node type: " + n.getNodeTypeName();
                    log.error(msg);
                    throw new ItemStateException(msg, e);
                }
                try {
                    Path[] paths = hmgr.getAllPaths(state.getId(), true);
                    for (int i = 0; i < paths.length; i++) {
                        Path parentPath = paths[i].getAncestor(1);
                        events.add(EventState.childNodeRemoved(n.getParentUUID(),
                                parentPath,
                                n.getUUID(),
                                paths[i].getNameElement(),
                                nodeType,
                                session));
                    }
                } catch (RepositoryException e) {
                    // should never happen actually
                    String msg = "Unable to resolve path for item: " + n.getId();
                    log.error(msg);
                    throw new ItemStateException(msg, e);
                }
            } else {
                // property removed
                // only create an event if node still exists
                try {
                    NodeState n = (NodeState) changes.get(new NodeId(state.getParentUUID()));
                    // node state exists -> only property removed
                    NodeTypeImpl nodeType = null;
                    try {
                        nodeType = session.getNodeTypeManager().getNodeType(n.getNodeTypeName());
                    } catch (NoSuchNodeTypeException e) {
                        // should never happen actually
                        String msg = "Item " + state.getId() + " has unknown node type: " + n.getNodeTypeName();
                        log.error(msg);
                        throw new ItemStateException(msg, e);
                    }
                    Path paths[] = null;
                    try {
                        paths = hmgr.getAllPaths(state.getId(), true);
                        for (int i = 0; i < paths.length; i++) {
                            Path parentPath = paths[i].getAncestor(1);
                            events.add(EventState.propertyRemoved(state.getParentUUID(),
                                    parentPath,
                                    paths[i].getNameElement(),
                                    nodeType,
                                    session));
                        }
                    } catch (RepositoryException e) {
                        // should never happen actually
                        String msg = "Unable to resolve path for item: " + n.getId();
                        log.error(msg);
                        throw new ItemStateException(msg, e);
                    }
                } catch (NoSuchItemStateException e) {
                    // also node removed -> do not create an event
                }
            }
        }
    }

    /**
     * Prepares the events for dispatching.
     */
    public void prepare() {
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
