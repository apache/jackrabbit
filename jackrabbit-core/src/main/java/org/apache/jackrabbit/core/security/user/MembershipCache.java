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
import org.apache.jackrabbit.spi.commons.name.NameConstants;
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
import javax.jcr.util.TraversingItemVisitor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <code>MembershipCache</code>...
 */
public class MembershipCache implements UserConstants {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(MembershipCache.class);

    private final SessionImpl systemSession;
    private final String groupsPath;
    private final boolean useMembersNode;
    private final Map<String, Collection<String>> cache;

    @SuppressWarnings("unchecked")
    MembershipCache(SessionImpl systemSession, String groupsPath, boolean useMembersNode) {
        this.systemSession = systemSession;
        this.groupsPath = (groupsPath == null) ? UserConstants.GROUPS_PATH : groupsPath;
        this.useMembersNode = useMembersNode;
        cache = new LRUMap();
    }

    synchronized void clear() {
        cache.clear();
    }

    synchronized Collection<String> getDeclaredMemberOf(String authorizableNodeIdentifier) throws RepositoryException {
        return declaredMemberOf(authorizableNodeIdentifier);
    }

    synchronized Collection<String> getMemberOf(String authorizableNodeIdentifier) throws RepositoryException {
        Set<String> groupNodeIds = new HashSet<String>();
        memberOf(authorizableNodeIdentifier, groupNodeIds);
        return Collections.unmodifiableCollection(groupNodeIds);
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
                groupNodeIds = collectDeclaredMembershipFromReferences(authorizableNodeIdentifier, session);
                if (groupNodeIds == null) {
                    groupNodeIds = collectDeclaredMembershipFromTraversal(authorizableNodeIdentifier, session);
                }
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

    private void memberOf(String authorizableNodeIdentifier, Collection<String> groupNodeIds) throws RepositoryException {
        Collection<String> declared = declaredMemberOf(authorizableNodeIdentifier);
        for (String identifier : declared) {
            if (groupNodeIds.add(identifier)) {
                memberOf(identifier, groupNodeIds);
            }
        }
    }

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