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
package org.apache.jackrabbit.core.security.user;

import org.apache.commons.collections.map.LRUMap;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.SessionListener;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.util.TraversingItemVisitor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <code>MembershipCache</code>...
 */
public class MembershipCache implements UserConstants, SynchronousEventListener, SessionListener {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(MembershipCache.class);

    private final SessionImpl systemSession;
    private final String groupsPath;
    private final boolean useMembersNode;
    private final String pMembers;
    private final Map<String, Collection<String>> cache;

    @SuppressWarnings("unchecked")
    MembershipCache(SessionImpl systemSession, String groupsPath, boolean useMembersNode) throws RepositoryException {
        this.systemSession = systemSession;
        this.groupsPath = (groupsPath == null) ? UserConstants.GROUPS_PATH : groupsPath;
        this.useMembersNode = useMembersNode;

        pMembers = systemSession.getJCRName(UserManagerImpl.P_MEMBERS);
        cache = new LRUMap();
                
        String[] ntNames = new String[] {
                systemSession.getJCRName(UserConstants.NT_REP_GROUP),
                systemSession.getJCRName(UserConstants.NT_REP_MEMBERS)
        };
        // register event listener to be informed about membership changes.
        systemSession.getWorkspace().getObservationManager().addEventListener(this,
                Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED,
                groupsPath,
                true,
                null,
                ntNames,
                false);
        // make sure the membership cache is informed if the system session is
        // logged out in order to stop listening to events.
        systemSession.addListener(this);
    }


    //------------------------------------------------------< EventListener >---
    /**
     * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
     */
    public void onEvent(EventIterator eventIterator) {
        // evaluate if the membership cache needs to be cleared;
        boolean clear = false;
        while (eventIterator.hasNext() && !clear) {
            Event ev = eventIterator.nextEvent();
            try {
                if (pMembers.equals(Text.getName(ev.getPath()))) {
                    // simple case: a rep:members property that is affected
                    clear = true;
                } else if (useMembersNode) {
                    // test if it affects a property defined by rep:Members node type.
                    int type = ev.getType();
                    if (type == Event.PROPERTY_ADDED || type == Event.PROPERTY_CHANGED) {
                        Property p = systemSession.getProperty(ev.getPath());
                        Name declNtName = ((NodeTypeImpl) p.getDefinition().getDeclaringNodeType()).getQName();
                        clear = NT_REP_MEMBERS.equals(declNtName);
                    } else {
                        // PROPERTY_REMOVED
                        // test if the primary node type of the parent node is rep:Members
                        // this could potentially by some other property as well as the
                        // rep:Members node are not protected and could changed by
                        // adding a mixin type.
                        // ignoring this and simply clear the cache
                        String parentId = ev.getIdentifier();
                        Node n = systemSession.getNodeByIdentifier(parentId);
                        Name ntName = ((NodeTypeImpl) n.getPrimaryNodeType()).getQName();
                        clear = (UserConstants.NT_REP_MEMBERS.equals(ntName));
                    }
                }
            } catch (RepositoryException e) {
                log.warn(e.getMessage());
                // exception while processing the event -> clear the cache to
                // be sure it isn't outdated.
                clear = true;
            }
        }

        if (clear) {
            synchronized (cache) {
                cache.clear();
            }
        }
    }

    //----------------------------------------------------< SessionListener >---
    /**
     * @see SessionListener#loggingOut(org.apache.jackrabbit.core.SessionImpl)
     */
    public void loggingOut(SessionImpl session) {
        try {
            systemSession.getWorkspace().getObservationManager().removeEventListener(this);
        } catch (RepositoryException e) {
            log.error("Unexpected error: Failed to stop event listening of MembershipCache.", e);
        }

    }

    /**
     * @see SessionListener#loggedOut(org.apache.jackrabbit.core.SessionImpl)
     */
    public void loggedOut(SessionImpl session) {
        // nothing to do
    }

    //--------------------------------------------------------------------------
    /**
     * @param authorizableNodeIdentifier The identifier of the node representing
     * the authorizable to retrieve the declared membership for.
     * @return A collection of node identifiers of those group nodes the
     * authorizable in question is declared member of.
     * @throws RepositoryException If an error occurs.
     */
    synchronized Collection<String> getDeclaredMemberOf(String authorizableNodeIdentifier) throws RepositoryException {
        return declaredMemberOf(authorizableNodeIdentifier);
    }

    /**
     * @param authorizableNodeIdentifier The identifier of the node representing
     * the authorizable to retrieve the membership for.
     * @return A collection of node identifiers of those group nodes the
     * authorizable in question is a direct or indirect member of.
     * @throws RepositoryException If an error occurs.
     */
    synchronized Collection<String> getMemberOf(String authorizableNodeIdentifier) throws RepositoryException {
        Set<String> groupNodeIds = new HashSet<String>();
        memberOf(authorizableNodeIdentifier, groupNodeIds);
        return Collections.unmodifiableCollection(groupNodeIds);
    }

    /**
     * Collects the declared memberships for the specified identifier of an
     * authorizable using the specified session.
     * 
     * @param authorizableNodeIdentifier The identifier of the node representing
     * the authorizable to retrieve the membership for.
     * @param session The session to be used to read the membership information.
     * @return @return A collection of node identifiers of those group nodes the
     * authorizable in question is a direct member of.
     * @throws RepositoryException If an error occurs.
     */
    Collection<String> collectDeclaredMembership(String authorizableNodeIdentifier, Session session) throws RepositoryException {
        Collection<String> groupNodeIds = collectDeclaredMembershipFromReferences(authorizableNodeIdentifier, session);
        if (groupNodeIds == null) {
            groupNodeIds = collectDeclaredMembershipFromTraversal(authorizableNodeIdentifier, session);
        }
        return groupNodeIds;
    }

    /**
     * Collects the complete memberships for the specified identifier of an
     * authorizable using the specified session.
     *
     * @param authorizableNodeIdentifier The identifier of the node representing
     * the authorizable to retrieve the membership for.
     * @param session The session to be used to read the membership information.
     * @return A collection of node identifiers of those group nodes the
     * authorizable in question is a direct or indirect member of.
     * @throws RepositoryException If an error occurs.
     */
    Collection<String> collectMembership(String authorizableNodeIdentifier, Session session) throws RepositoryException {
        Set<String> groupNodeIds = new HashSet<String>();
        memberOf(authorizableNodeIdentifier, groupNodeIds, session);
        return groupNodeIds;
    }

    //------------------------------------------------------------< private >---
    /**
     * @param authorizableNodeIdentifier
     * @return
     * @throws RepositoryException
     */
    private Collection<String> declaredMemberOf(String authorizableNodeIdentifier) throws RepositoryException {
        Collection<String> groupNodeIds = cache.get(authorizableNodeIdentifier);
        if (groupNodeIds == null) {
            // retrieve a new session with system-subject in order to avoid
            // concurrent read operations using the system session of this workspace.
            Session session = getSession();
            try {
                groupNodeIds = collectDeclaredMembership(authorizableNodeIdentifier, session);
                cache.put(authorizableNodeIdentifier, Collections.unmodifiableCollection(groupNodeIds));
            }
            finally {
                // release session if it isn't the original system session
                if (session != systemSession) {
                    session.logout();
                }
            }
        }
        return groupNodeIds;
    }

    /**
     * 
     * @param authorizableNodeIdentifier
     * @param groupNodeIds
     * @throws RepositoryException
     */
    private void memberOf(String authorizableNodeIdentifier, Collection<String> groupNodeIds) throws RepositoryException {
        Collection<String> declared = declaredMemberOf(authorizableNodeIdentifier);
        for (String identifier : declared) {
            if (groupNodeIds.add(identifier)) {
                memberOf(identifier, groupNodeIds);
            }
        }
    }

    /**
     * 
     * @param authorizableNodeIdentifier
     * @param groupNodeIds
     * @param session
     * @throws RepositoryException
     */
    private void memberOf(String authorizableNodeIdentifier, Collection<String> groupNodeIds, Session session) throws RepositoryException {
        Collection<String> declared = collectDeclaredMembership(authorizableNodeIdentifier, session);
        for (String identifier : declared) {
            if (groupNodeIds.add(identifier)) {
                memberOf(identifier, groupNodeIds, session);
            }
        }
    }

    /**
     * 
     * @param authorizableNodeIdentifier
     * @param session
     * @return
     * @throws RepositoryException
     */
    private Collection<String> collectDeclaredMembershipFromReferences(String authorizableNodeIdentifier,
                                                                       Session session) throws RepositoryException {
        Set<String> pIds = new HashSet<String>();
        Set<String> nIds = new HashSet<String>();

        // Try to get membership information from references
        PropertyIterator refs = getMembershipReferences(authorizableNodeIdentifier, session);
        if (refs == null) {
            return null;
        }

        while (refs.hasNext()) {
            try {
                PropertyImpl pMember = (PropertyImpl) refs.nextProperty();
                NodeImpl nGroup = (NodeImpl) pMember.getParent();

                Set<String> groupNodeIdentifiers;
                if (P_MEMBERS.equals(pMember.getQName())) {
                    // Found membership information in members property
                    groupNodeIdentifiers = pIds;
                } else {
                    // Found membership information in members node
                    groupNodeIdentifiers = nIds;
                    while (nGroup.isNodeType(NT_REP_MEMBERS)) {
                        nGroup = (NodeImpl) nGroup.getParent();
                    }
                }

                if (nGroup.isNodeType(NT_REP_GROUP)) {
                    groupNodeIdentifiers.add(nGroup.getIdentifier());
                } else {
                    // weak-ref property 'rep:members' that doesn't reside under an
                    // group node -> doesn't represent a valid group member.
                    log.debug("Invalid member reference to '" + this + "' -> Not included in membership set.");
                }
            } catch (ItemNotFoundException e) {
                // group node doesn't exist  -> -> ignore exception
                // and skip this reference from membership list.
            } catch (AccessDeniedException e) {
                // not allowed to see the group node -> ignore exception
                // and skip this reference from membership list.
            }
        }

        // Based on the user's setting return either of the found membership informations
        return select(pIds, nIds);
    }

    private Collection<String> collectDeclaredMembershipFromTraversal(
            final String authorizableNodeIdentifier, Session session) throws RepositoryException {

        final Set<String> pIds = new HashSet<String>();
        final Set<String> nIds = new HashSet<String>();

        // workaround for failure of Node#getWeakReferences
        // traverse the tree below groups-path and collect membership manually.
        log.info("Traversing groups tree to collect membership.");
        ItemVisitor visitor = new TraversingItemVisitor.Default() {
            @Override
            protected void entering(Property property, int level) throws RepositoryException {
                PropertyImpl pMember = (PropertyImpl) property;
                NodeImpl nGroup = (NodeImpl) pMember.getParent();
                if (P_MEMBERS.equals(pMember.getQName()) && nGroup.isNodeType(NT_REP_GROUP)) {
                    // Found membership information in members property
                    for (Value value : property.getValues()) {
                        String v = value.getString();
                        if (v.equals(authorizableNodeIdentifier)) {
                            pIds.add(nGroup.getIdentifier());
                        }
                    }
                } else {
                    // Found membership information in members node
                    while (nGroup.isNodeType(NT_REP_MEMBERS)) {
                        nGroup = (NodeImpl) nGroup.getParent();
                    }

                    if (nGroup.isNodeType(NT_REP_GROUP) && !NameConstants.JCR_UUID.equals(pMember.getQName())) {
                        String v = pMember.getString();
                        if (v.equals(authorizableNodeIdentifier)) {
                            nIds.add(nGroup.getIdentifier());
                        }
                    }
                }
            }
        };

        if (session.nodeExists(groupsPath)) {
            Node groupsNode = session.getNode(groupsPath);
            visitor.visit(groupsNode);
        } // else: no groups exist -> nothing to do.

        // Based on the user's setting return either of the found membership informations
        return select(pIds, nIds);
    }

    /**
     * Return either of both sets depending on the users setting whether
     * to use the members property or the members node to record membership
     * information. If both sets are non empty, the one configured in the
     * settings will take precedence and an warning is logged.
     *
     * @return
     */
    private Set<String> select(Set<String> pIds, Set<String> nIds) {
        Set<String> result;
        if (useMembersNode) {
            if (!nIds.isEmpty() || pIds.isEmpty()) {
                result = nIds;
            } else {
                result = pIds;
            }
        } else {
            if (!pIds.isEmpty() || nIds.isEmpty()) {
                result = pIds;
            } else {
                result = nIds;
            }
        }

        if (!pIds.isEmpty() && !nIds.isEmpty()) {
            log.warn("Found members node and members property. Ignoring {} members",
                    useMembersNode ? "property" : "node");
        }

        return result;
    }


    /**
     * @return a new Session that needs to be properly released after usage.
     * @throws RepositoryException
     * @throws AccessDeniedException
     */
    private SessionImpl getSession() {
        try {
            return (SessionImpl) systemSession.createSession(systemSession.getWorkspace().getName());
        } catch (RepositoryException e) {
            // fallback
            return systemSession;
        }
    }

    private static PropertyIterator getMembershipReferences(String authorizableNodeIdentifier,
                                                            Session session) {

        PropertyIterator refs = null;
        try {
            refs = session.getNodeByIdentifier(authorizableNodeIdentifier).getWeakReferences(null);
        } catch (RepositoryException e) {
            log.error("Failed to retrieve membership references of " + authorizableNodeIdentifier + ".", e);
        }
        return refs;
    }
}