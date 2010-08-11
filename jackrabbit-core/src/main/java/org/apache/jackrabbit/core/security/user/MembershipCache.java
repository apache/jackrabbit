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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
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
    private final Map<String, Collection<String>> cache;

    MembershipCache(SessionImpl systemSession, String groupsPath) {
        this.systemSession = systemSession;
        this.groupsPath = (groupsPath == null) ? UserConstants.GROUPS_PATH : groupsPath;
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
     *
     * @param authorizableNodeIdentifier
     * @return
     * @throws RepositoryException
     */
    private Collection<String> declaredMemberOf(String authorizableNodeIdentifier) throws RepositoryException {
        Collection<String> groupNodeIds = cache.get(authorizableNodeIdentifier);
        if (groupNodeIds == null) {
            groupNodeIds = collectDeclaredMembership(authorizableNodeIdentifier);
            cache.put(authorizableNodeIdentifier, Collections.unmodifiableCollection(groupNodeIds));
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

    private Collection<String> collectDeclaredMembership(final String authorizableNodeIdentifier) throws RepositoryException {
        // retrieve a new session with system-subject in order to avoid
        // concurrent read operations using the system session of this workspace.
        final SessionImpl session = getSession();
        try {
            final Set<String> groupNodeIdentifiers = new HashSet<String>();

            // try to retrieve the membership references using JCR API.
            PropertyIterator refs = getMembershipReferences(authorizableNodeIdentifier, session);
            if (refs != null) {
                while (refs.hasNext()) {
                    try {
                        NodeImpl n = (NodeImpl) refs.nextProperty().getParent();
                        if (n.isNodeType(NT_REP_GROUP)) {
                            String identifier = n.getIdentifier();
                            if (!groupNodeIdentifiers.contains(identifier)) {
                                groupNodeIdentifiers.add(identifier);
                            }
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
            } else {
                // workaround for failure of Node#getWeakReferences
                // traverse the tree below groups-path and collect membership manually.
                log.info("Traversing groups tree to collect membership.");
                ItemVisitor visitor = new TraversingItemVisitor.Default() {
                    @Override
                    protected void entering(Property property, int level) throws RepositoryException {
                        PropertyImpl pImpl = (PropertyImpl) property;
                        NodeImpl n = (NodeImpl) pImpl.getParent();
                        if (P_MEMBERS.equals(pImpl.getQName()) && n.isNodeType(NT_REP_GROUP)) {
                            for (Value value : property.getValues()) {
                                String v = value.getString();
                                if (v.equals(authorizableNodeIdentifier)) {
                                    groupNodeIdentifiers.add(n.getIdentifier());
                                }
                            }
                        }
                    }
                };

                if (session.nodeExists(groupsPath)) {
                    Node groupsNode = session.getNode(groupsPath);
                    visitor.visit(groupsNode);
                } // else: no groups exist -> nothing to do.
            }
            return groupNodeIdentifiers;

        } finally {
            // release session if it isn't the original system session but has
            // been created for this method call only.
            if (session != systemSession) {
                session.logout();
            }
        }
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

    private static PropertyIterator getMembershipReferences(String authorizableNodeIdentifier, SessionImpl session) {
        PropertyIterator refs = null;
        try {
            refs = session.getNodeByIdentifier(authorizableNodeIdentifier).getWeakReferences(session.getJCRName(P_MEMBERS));
        } catch (RepositoryException e) {
            log.error("Failed to retrieve membership references of " + authorizableNodeIdentifier + ".", e);
        }
        return refs;
    }
}