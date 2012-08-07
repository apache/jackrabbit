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
package org.apache.jackrabbit.core.security.authorization.acl;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlModifications;
import org.apache.jackrabbit.core.security.authorization.AccessControlObserver;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <code>EntryCollector</code> collects ACEs defined and effective for a
 * given <code>Node</code> and listens to access control modifications in order
 * to inform listeners.
 */
public class EntryCollector extends AccessControlObserver implements AccessControlConstants {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(EntryCollector.class);

    /**
     * The system session used to register an event listener and process the
     * events as well as collect AC entries.
     */
    protected final SessionImpl systemSession;

    /**
     * The root id.
     */
    protected final NodeId rootID;

    private final EventListener moveListener;

    /**
     *
     * @param systemSession
     * @param rootID
     * @throws RepositoryException
     */
    protected EntryCollector(SessionImpl systemSession, NodeId rootID) throws RepositoryException {
        this.systemSession = systemSession;
        this.rootID = rootID;

        ObservationManager observationMgr = systemSession.getWorkspace().getObservationManager();
        /*
         Make sure the collector and all subscribed listeners are informed upon
         ACL modifications. Interesting events are:
         - new ACL (NODE_ADDED)
         - new ACE (NODE_ADDED)
         - changing ACE (PROPERTY_CHANGED)
         - removed ACL (NODE_REMOVED)
         - removed ACE (NODE_REMOVED)
        */
        int events = Event.PROPERTY_CHANGED | Event.NODE_ADDED | Event.NODE_REMOVED;
        String[] ntNames = new String[] {
                systemSession.getJCRName(NT_REP_ACCESS_CONTROLLABLE),
                systemSession.getJCRName(NT_REP_ACL),
                systemSession.getJCRName(NT_REP_ACE)
        };
        String rootPath = systemSession.getRootNode().getPath();
        observationMgr.addEventListener(this, events, rootPath, true, null, ntNames, true);
        /*
         In addition both the collector and all subscribed listeners should be
         informed about any kind of move events.
         */
        moveListener = new MoveListener();
        observationMgr.addEventListener(moveListener, Event.NODE_MOVED, rootPath, true, null, null, true);
    }

    /**
     * Release all resources contained by this instance. It will no longer be
     * used. This implementation only stops listening to ac modification events.
     */
    @Override
    protected void close() {
        super.close();
        try {
            ObservationManager observationMgr = systemSession.getWorkspace().getObservationManager();
            observationMgr.removeEventListener(this);
            observationMgr.removeEventListener(moveListener);
        } catch (RepositoryException e) {
            log.error("Unexpected error while closing CachingEntryCollector", e);
        }
    }

    /**
     * Collect the ACEs effective at the given node applying the specified
     * filter.
     * 
     * @param node
     * @param filter
     * @return
     * @throws RepositoryException
     */
    protected List<Entry> collectEntries(NodeImpl node, EntryFilter filter) throws RepositoryException {
        LinkedList<Entry> userAces = new LinkedList<Entry>();
        LinkedList<Entry> groupAces = new LinkedList<Entry>();

        if (node == null) {
            // repository level permissions
            NodeImpl root = (NodeImpl) systemSession.getRootNode();
            if (ACLProvider.isRepoAccessControlled(root)) {
                NodeImpl aclNode = root.getNode(N_REPO_POLICY);
                filterEntries(filter, Entry.readEntries(aclNode, null), userAces, groupAces);
            }
        } else {
            filterEntries(filter, getEntries(node).getACEs(), userAces, groupAces);
            NodeId next = node.getParentId();
            while (next != null) {
                Entries entries = getEntries(next);
                filterEntries(filter, entries.getACEs(), userAces, groupAces);
                next = entries.getNextId();
            }
        }

        List<Entry> entries = new ArrayList<Entry>(userAces.size() + groupAces.size());
        entries.addAll(userAces);
        entries.addAll(groupAces);

        return entries;
    }

    /**
     * Filter the specified access control <code>entries</code>
     *
     * @param filter
     * @param aces
     * @param userAces
     * @param groupAces
     */
    @SuppressWarnings("unchecked")
    private static void filterEntries(EntryFilter filter, List<Entry> aces,
                                      LinkedList<Entry> userAces,
                                      LinkedList<Entry> groupAces) {
        if (!aces.isEmpty() && filter != null) {
            filter.filterEntries(aces, userAces, groupAces);
        }
    }

    /**
     * Retrieve the access control entries defined for the given node. If the
     * node is not access controlled or if the ACL is empty this method returns
     * an empty list.
     * 
     * @param node
     * @return
     * @throws RepositoryException
     */
    protected Entries getEntries(NodeImpl node) throws RepositoryException {
        List<Entry> aces;
        if (ACLProvider.isAccessControlled(node)) {
            // collect the aces of that node.
            NodeImpl aclNode = node.getNode(N_POLICY);
            aces = Entry.readEntries(aclNode, node.getPath());
        } else {
            // not access controlled
            aces = Collections.emptyList();
        }
        return new Entries(aces, node.getParentId());
    }

    /**
     * 
     * @param nodeId
     * @return
     * @throws RepositoryException
     */
    protected Entries getEntries(NodeId nodeId) throws RepositoryException {
        NodeImpl node = getNodeById(nodeId);
        return getEntries(node);
    }

    /**
     *
     * @param nodeId
     * @return
     * @throws javax.jcr.RepositoryException
     */
    NodeImpl getNodeById(NodeId nodeId) throws RepositoryException {
        return ((NodeImpl) systemSession.getItemManager().getItem(nodeId));
    }

    //------------------------------------------------------< EventListener >---
    /**
     * Collects access controlled nodes that are effected by access control
     * changes together with the corresponding modification types, and
     * notifies access control listeners about the modifications.
     * 
     * @param events
     */
    public void onEvent(EventIterator events) {
        try {
            // JCR-2890: We need to use a fresh new session here to avoid
            // deadlocks caused by concurrent threads possibly using the
            // systemSession instance for other purposes.
            String workspaceName = systemSession.getWorkspace().getName();
            Session session = systemSession.createSession(workspaceName);
            try {
                // Sift through the events to find access control modifications
                ACLEventSieve sieve = new ACLEventSieve(session, (NameResolver) session);
                sieve.siftEvents(events);

                // Notify listeners and eventually clean up internal caches
                AccessControlModifications<NodeId> mods = sieve.getModifications();
                if (!mods.getNodeIdentifiers().isEmpty()) {
                    notifyListeners(mods);
                }
            } finally {
                session.logout();
            }
        } catch (RepositoryException e) {
            log.error("Failed to process access control modifications", e);
        }
    }

    //--------------------------------------------------------< inner class >---
    /**
     * Private utility class for sifting through observation events on
     * ACL, ACE and Policy nodes to find out the nodes whose access controls
     * have changed. Used by the {@link EntryCollector#onEvent(EventIterator)}
     * method.
     */
    private static class ACLEventSieve {

        /** Session with system privileges. */
        private final Session session;

        /**
         * Standard JCR name form of the
         * {@link AccessControlConstants#N_POLICY} constant.
         */
        private final String repPolicyName;

        /**
         * Map of access-controlled nodeId to type of access control modification.
         */
        private final Map<NodeId, Integer> modMap = new HashMap<NodeId,Integer>();

        private ACLEventSieve(Session session, NameResolver resolver) throws RepositoryException {
            this.session = session;
            this.repPolicyName = resolver.getJCRName(AccessControlConstants.N_POLICY);
        }

        /**
         * Collects the identifiers of all access controlled nodes that have
         * been affected by the events, and thus need their cache entries
         * updated or cleared.
         *
         * @param events access control modification events
         */
        private void siftEvents(EventIterator events) {
            while (events.hasNext()) {
                Event event = events.nextEvent();
                try {
                    switch (event.getType()) {
                    case Event.NODE_ADDED:
                        siftNodeAdded(event.getIdentifier());
                        break;
                    case Event.NODE_REMOVED:
                        siftNodeRemoved(event.getPath());
                        break;
                    case Event.PROPERTY_CHANGED:
                        siftPropertyChanged(event.getIdentifier());
                        break;
                    default:
                        // illegal event-type: should never occur. ignore
                    }
                } catch (RepositoryException e) {
                    // should not get here
                    log.warn("Failed to process ACL event: " + event, e);
                }
            }
        }

        /**
         * Returns the access control modifications collected from
         * related observation events.
         *
         * @return access control modifications
         */
        private AccessControlModifications<NodeId> getModifications() {
            return new AccessControlModifications<NodeId>(modMap);
        }

        private void siftNodeAdded(String identifier) throws RepositoryException {
            try {
                NodeImpl n = (NodeImpl) session.getNodeByIdentifier(identifier);
                if (n.isNodeType(EntryCollector.NT_REP_ACL)) {
                    // a new ACL was added -> use the added node to update
                    // the cache.
                    addModification(
                            accessControlledIdFromAclNode(n),
                            AccessControlObserver.POLICY_ADDED);
                } else if (n.isNodeType(EntryCollector.NT_REP_ACE)) {
                    // a new ACE was added -> use the parent node (acl)
                    // to update the cache.
                    addModification(
                            accessControlledIdFromAceNode(n),
                            AccessControlObserver.POLICY_MODIFIED);
                } /* else: some other node added below an access controlled
                     parent node -> not interested. */
            } catch (ItemNotFoundException e) {
                log.debug("Cannot process NODE_ADDED event. Node {} doesn't exist (anymore).", identifier);
            }
        }

        private void siftNodeRemoved(String path) throws RepositoryException {
            String parentPath = Text.getRelativeParent(path, 1);
            if (session.nodeExists(parentPath)) {
                NodeImpl parent = (NodeImpl) session.getNode(parentPath);
                if (repPolicyName.equals(Text.getName(path))){
                    // the complete ACL was removed -> clear cache entry
                    addModification(
                            parent.getNodeId(),
                            AccessControlObserver.POLICY_REMOVED);
                } else if (parent.isNodeType(EntryCollector.NT_REP_ACL)) {
                    // an ace was removed -> refresh cache for the
                    // containing access control list upon next access
                    addModification(
                            accessControlledIdFromAclNode(parent),
                            AccessControlObserver.POLICY_MODIFIED);
                } /* else:
                         a) some other child node of an access controlled
                            node -> not interested.
                         b) a child node of an ACE. not relevant for this
                            implementation -> ignore
                 */
            } else {
                log.debug("Cannot process NODE_REMOVED event. Parent {} doesn't exist (anymore).", parentPath);
            }
        }

        private void siftPropertyChanged(String identifier) throws RepositoryException {
            try {
                // test if the changed prop belongs to an ACE
                NodeImpl parent = (NodeImpl) session.getNodeByIdentifier(identifier);
                if (parent.isNodeType(EntryCollector.NT_REP_ACE)) {
                    addModification(
                            accessControlledIdFromAceNode(parent),
                            AccessControlObserver.POLICY_MODIFIED);
                } /* some other property below an access controlled node
                 changed -> not interested. (NOTE: rep:ACL doesn't
                 define any properties. */
            } catch (ItemNotFoundException e) {
                log.debug("Cannot process PROPERTY_CHANGED event. Node {} doesn't exist (anymore).", identifier);
            }
        }

        private NodeId accessControlledIdFromAclNode(Node aclNode) throws RepositoryException {
            return ((NodeImpl) aclNode.getParent()).getNodeId();
        }

        private NodeId accessControlledIdFromAceNode(Node aceNode) throws RepositoryException {
            return accessControlledIdFromAclNode(aceNode.getParent());
        }

        private void addModification(NodeId accessControllNodeId, int modType) {
            if (modMap.containsKey(accessControllNodeId)) {
                // update modMap
                modType |= modMap.get(accessControllNodeId);
            }
            modMap.put(accessControllNodeId, modType);
        }
    }

    /**
     * Listening to any kind of move events in the hierarchy. Since ac content
     * is associated with individual nodes the caches need to be informed about
     * any kind of move as well even if the target node is not access control
     * content s.str.
     */
    private class MoveListener implements SynchronousEventListener {

        public void onEvent(EventIterator events) {
            // NOTE: simplified event handling as all listeners just clear
            // the cache in case of any move event. therefore there is currently
            // no need to process all events and using the rootID as marker.
            while (events.hasNext()) {
                Event event = events.nextEvent();
                if (event.getType() == Event.NODE_MOVED) {
                    Map<NodeId, Integer> m = Collections.singletonMap(rootID, AccessControlObserver.MOVE);
                    AccessControlModifications<NodeId> mods = new AccessControlModifications<NodeId>(m);
                    notifyListeners(mods);
                    break;
                } //else: illegal event-type: should never occur. ignore
            }
        }
    }

    //--------------------------------------------------------------------------
    /**
     * Inner class combining a list of access control entries with the information
     * where to start looking for inherited entries.
     *
     * Thus <code>nextId</code> either points to the parent of the access
     * controlled node associated with <code>aces</code> or to the next
     * access controlled ancestor. It is <code>null</code> if the root node has
     * been reached and there is no additional ancestor to retrieve access control
     * entries from.
     */
    static class Entries {

        private final List<Entry> aces;
        private NodeId nextId;

        Entries(List<Entry> aces, NodeId nextId) {
            this.aces = aces;
            this.nextId = nextId;
        }

        List<Entry> getACEs() {
            return aces;
        }

        NodeId getNextId() {
            return nextId;
        }

        void setNextId(NodeId nextId) {
            this.nextId = nextId;
        }

        boolean isEmpty() {
            return aces.isEmpty();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("size = ").append(aces.size()).append(", ");
            sb.append("nextNodeId = ").append(nextId);
            return sb.toString();
        }
    }
}
