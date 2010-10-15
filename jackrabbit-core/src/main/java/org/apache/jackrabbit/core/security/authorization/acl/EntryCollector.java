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
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.AccessControlModifications;
import org.apache.jackrabbit.core.security.authorization.AccessControlObserver;
import org.apache.jackrabbit.core.security.authorization.PrivilegeRegistry;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;
import javax.jcr.security.AccessControlEntry;
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

    private final PrivilegeRegistry privilegeRegistry;
    
    /**
     * Standard JCR name form of the {@link #N_POLICY} constant.
     */
    private final String repPolicyName;

    /**
     *
     * @param systemSession
     * @param rootID
     * @throws RepositoryException
     */
    protected EntryCollector(SessionImpl systemSession, NodeId rootID) throws RepositoryException {
        this.systemSession = systemSession;
        this.rootID = rootID;

        privilegeRegistry = new PrivilegeRegistry(systemSession);
        repPolicyName = systemSession.getJCRName(N_POLICY);

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
        observationMgr.addEventListener(this, events, systemSession.getRootNode().getPath(), true, null, ntNames, true);        
    }

    /**
     * Release all resources contained by this instance. It will no longer be
     * used. This implementation only stops listening to ac modification events.
     */
    @Override
    protected void close() {
        super.close();
        try {
            systemSession.getWorkspace().getObservationManager().removeEventListener(this);
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
    protected List<AccessControlEntry> collectEntries(NodeImpl node, EntryFilter filter) throws RepositoryException {
        LinkedList<AccessControlEntry> userAces = new LinkedList<AccessControlEntry>();
        LinkedList<AccessControlEntry> groupAces = new LinkedList<AccessControlEntry>();

        NodeId next = node.getNodeId();
        while (next != null) {
            List<AccessControlEntry> entries = getEntries(next);
            if (!entries.isEmpty() && filter != null) {
                filter.filterEntries(entries, userAces, groupAces);
            }
            next = getParentId(next);
        }
        
        List<AccessControlEntry> entries = new ArrayList<AccessControlEntry>(userAces.size() + groupAces.size());
        entries.addAll(userAces);
        entries.addAll(groupAces);

        return entries;
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
    protected List<AccessControlEntry> getEntries(NodeImpl node) throws RepositoryException {
        List<AccessControlEntry> entries;
        if (ACLProvider.isAccessControlled(node)) {
            // collect the aces of that node.
            NodeImpl aclNode = node.getNode(N_POLICY);
            entries = new ACLTemplate(aclNode, privilegeRegistry).getEntries();
        } else {
            // not access controlled
            entries = Collections.emptyList();
        }
        return entries;
    }

    /**
     * 
     * @param nodeId
     * @return
     * @throws RepositoryException
     */
    protected List<AccessControlEntry> getEntries(NodeId nodeId) throws RepositoryException {
        NodeImpl node = getNodeById(nodeId);
        return getEntries(node);
    }

    /**
     * Returns the parentId of the given nodeId.
     *
     * @param nodeId
     * @return
     * @throws RepositoryException
     */
    protected NodeId getParentId(NodeId nodeId) throws RepositoryException {
        NodeId parentId;
        if (rootID.equals(nodeId)) {
            parentId = null; // root node reached.
        } else {
            parentId = getNodeById(nodeId).getParentId();
        }
        return parentId;
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

    //------------------------------------------------------------< private >---

    private static NodeId accessControlledIdFromAclNode(Node aclNode) throws RepositoryException {
        return ((NodeImpl) aclNode.getParent()).getNodeId();
    }

    private static NodeId accessControlledIdFromAceNode(Node aceNode) throws RepositoryException {
        return ((NodeImpl) aceNode.getParent().getParent()).getNodeId();
    }

    private static void addModification(NodeId accessControllNodeId, int modType,
                                        Map<NodeId,Integer> modMap) {
        if (modMap.containsKey(accessControllNodeId)) {
            // update modMap
            modMap.put(accessControllNodeId, modType | modMap.get(accessControllNodeId));
        } else {
            modMap.put(accessControllNodeId, modType);
        }
    }
    
    //------------------------------------------------------< EventListener >---
    /**
     * Collect is of access controlled nodes that are effected by access control
     * modification together with the corresponding modification type and
     * finally inform listeners about the modifications.
     * 
     * @param events
     */
    public void onEvent(EventIterator events) {
        /* map of access-controlled nodeId to type of ac modification */
        Map<NodeId,Integer> modMap = new HashMap<NodeId,Integer>();

        // collect the ids of all access controlled nodes that have been affected
        // by the events and thus need their cache entries updated or cleared.
        while (events.hasNext()) {
            try {
                Event ev = events.nextEvent();
                String identifier = ev.getIdentifier();
                String path = ev.getPath();

                switch (ev.getType()) {
                    case Event.NODE_ADDED:
                        NodeImpl n = (NodeImpl) systemSession.getNodeByIdentifier(identifier);
                        if (n.isNodeType(NT_REP_ACL)) {
                            // a new ACL was added -> use the added node to update
                            // the cache.
                            addModification(accessControlledIdFromAclNode(n), POLICY_ADDED, modMap);
                        } else if (n.isNodeType(NT_REP_ACE)) {
                            // a new ACE was added -> use the parent node (acl)
                            // to update the cache.
                            addModification(accessControlledIdFromAceNode(n), POLICY_MODIFIED, modMap);
                        } /* else: some other node added below an access controlled
                             parent node -> not interested. */
                        break;
                    case Event.NODE_REMOVED:
                        String parentPath = Text.getRelativeParent(path, 1);
                        if (systemSession.nodeExists(parentPath)) {
                            NodeImpl parent = (NodeImpl) systemSession.getNode(parentPath);
                            if (repPolicyName.equals(Text.getName(path))){
                                // the complete acl was removed -> clear cache entry
                                addModification(parent.getNodeId(), POLICY_REMOVED, modMap);
                            } else if (parent.isNodeType(NT_REP_ACL)) {
                                // an ace was removed -> refresh cache for the
                                // containing access control list upon next access
                                addModification(accessControlledIdFromAclNode(parent), POLICY_MODIFIED, modMap);
                            } /* else:
                             a) some other child node of an access controlled
                                node -> not interested.
                             b) a child node of an ACE. not relevant for this
                                implementation -> ignore
                           */
                        } else {
                            log.debug("Cannot process NODE_REMOVED event. Parent " + parentPath + " doesn't exist (anymore).");
                        }
                        break;
                    case Event.PROPERTY_CHANGED:
                        // test if the changed prop belongs to an ACE
                        NodeImpl parent = (NodeImpl) systemSession.getNodeByIdentifier(identifier);
                        if (parent.isNodeType(NT_REP_ACE)) {
                            addModification(accessControlledIdFromAceNode(parent), POLICY_MODIFIED, modMap);
                        } /* some other property below an access controlled node
                             changed -> not interested. (NOTE: rep:ACL doesn't
                             define any properties. */
                        break;
                    default:
                        // illegal event-type: should never occur. ignore
                }
            } catch (RepositoryException e) {
                // should not get here
                log.error("Internal error: ", e);
            }
        }

        if (!modMap.isEmpty()) {
            // notify listeners and eventually clean up internal caches.
            notifyListeners(new AccessControlModifications<NodeId>(modMap));
        }
    }
}