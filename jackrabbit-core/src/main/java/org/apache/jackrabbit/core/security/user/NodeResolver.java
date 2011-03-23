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

import java.util.Collections;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.predicate.Predicate;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolver: searches for user and/or groups stored in Nodes of a {@link javax.jcr.Workspace}
 * which match a certain criteria
 */
abstract class NodeResolver {

    private static Logger log = LoggerFactory.getLogger(NodeResolver.class);

    private final Session session;
    private final NamePathResolver resolver;

    private String userSearchRoot = UserConstants.USERS_PATH;
    private String groupSearchRoot = UserConstants.GROUPS_PATH;
    private String authorizableSearchRoot = UserConstants.AUTHORIZABLES_PATH;

    /**
     * Create a new <code>NodeResolver</code>.
     *
     * @param session to use for repository access
     * @param resolver The NamePathResolver used to convert {@link org.apache.jackrabbit.spi.Name}
     * and {@link org.apache.jackrabbit.spi.Path} to JCR names/path.
     */
    NodeResolver(Session session, NamePathResolver resolver) {
        this.session = session;
        this.resolver = resolver;
    }

    void setSearchRoots(String userSearchRoot, String groupSearchRoot) {
        this.userSearchRoot = userSearchRoot;
        this.groupSearchRoot = groupSearchRoot;

        authorizableSearchRoot = userSearchRoot;
        while (!Text.isDescendant(authorizableSearchRoot, groupSearchRoot)) {
            authorizableSearchRoot = Text.getRelativeParent(authorizableSearchRoot, 1);
        }
    }

    /**
     * Get the first node that matches <code>ntName</code> and whose name
     * exactly matches the given <code>nodeName</code>.
     *
     * @param nodeName Name of the node to find.
     * @param ntName Node type name of the node to find.
     * @return A matching node or <code>null</code>.
     * @throws RepositoryException If an error occurs.
     */
    public abstract Node findNode(Name nodeName, Name ntName) throws RepositoryException;

    /**
     * Get the first node that matches <code>ntName</code> and has a
     * property whose value exactly matches the given value. Same as
     * {@link #findNodes(Set,String,Name,boolean,long)} but returning a single node or <code>null</code>.
     *
     * @param propertyName Name of the property to find.
     * @param value Value of the property to find.
     * @param ntName Name of the parent node's node type.
     * @return The first node that matches the specified node type name and has
     * a property with the given propertyName that exactly matches the given
     * value or <code>null</code>.
     * @throws RepositoryException If an error occurs.
     */
    public abstract Node findNode(Name propertyName, String value, Name ntName) throws RepositoryException;

    /**
     * Search for Nodes which contain an exact match for the given value in
     * their property as indicated by the propertyName argument.<br>
     * Same as {@link #findNodes(Set,String,Name,boolean,long)}; where
     * the maxSize parameters is set to {@link Long#MAX_VALUE)}.
     *
     * @param propertyName Name of the property to be searched.
     * @param value Value to be matched.
     * @param ntName  Name of the parent node's node type.
     * @param exact If <code>true</code> value has to match exactly.
     * @return matching nodes (or an empty iterator if no match was found).
     * @throws RepositoryException If an error occurs.
     */
    public NodeIterator findNodes(Name propertyName, String value, Name ntName, boolean exact)
            throws RepositoryException {
        return findNodes(Collections.singleton(propertyName), value, ntName, exact, Long.MAX_VALUE);
    }

    /**
     * Search nodes. Take the arguments as search criteria.
     * The queried value has to be a string fragment of one of the Properties
     * contained in the given set. And the node have to be of a requested nodetype
     *
     * @param propertyNames Names of the property to be searched.
     * @param value The value to find.
     * @param ntName NodeType the hits have to have
     * @param exact  if <code>true</code> match must be exact
     * @param maxSize maximal number of results to search for.
     * @return matching nodes (or an empty iterator if no match was found).
     * @throws RepositoryException If an error occurs.
     */
    public abstract NodeIterator findNodes(Set<Name> propertyNames, String value,
                                           Name ntName, boolean exact, long maxSize)
            throws RepositoryException;

    /**
     * Search all properties underneath an authorizable of the specified type
     * that match the specified value and relative path. If the relative path
     * consists of a single name element the path constraint is omitted.
     * 
     * @param relPath
     * @param value
     * @param authorizableType
     * @param exact
     * @param maxSize
     * @return
     * @throws RepositoryException
     */
    public abstract NodeIterator findNodes(Path relPath, String value,
                                           int authorizableType, boolean exact,
                                           long maxSize) throws RepositoryException;

    /**
     * @return Session this instance has been constructed with.
     */
    Session getSession() {
        return session;
    }

    /**
     * @return The <code>NamePathResolver</code>.
     */
    NamePathResolver getNamePathResolver() {
        return resolver;
    }

    /**
     * @param ntName Any of the following node type names:
     * {@link UserConstants#NT_REP_USER}, {@link UserConstants#NT_REP_GROUP} or
     * {@link UserConstants#NT_REP_AUTHORIZABLE}.
     * @return The path of search root for the specified node type name.
     */
    String getSearchRoot(Name ntName) {
        String searchRoot;
        if (UserConstants.NT_REP_USER.equals(ntName)) {
            searchRoot = userSearchRoot;
        } else if (UserConstants.NT_REP_GROUP.equals(ntName)) {
            searchRoot = groupSearchRoot;
        } else {
            searchRoot = authorizableSearchRoot;
        }
        return searchRoot;
    }

    /**
     * @param authorizableType
     * @return The path of search root for the specified authorizable type.
     */
    String getSearchRoot(int authorizableType) {
        switch (authorizableType) {
            case UserManager.SEARCH_TYPE_USER:
                return userSearchRoot;
            case UserManager.SEARCH_TYPE_GROUP:
                return groupSearchRoot;
            default:
                return authorizableSearchRoot;
        }
    }

    /**
     * 
     * @param authorizableType
     * @param exact If exact is true, the predicate only evaluates to true if the
     * passed node is of the required authorizable node type. Otherwise, all
     * ancestors are taken into account as well.
     * @return a new AuthorizableTypePredicate instance.
     */
    AuthorizableTypePredicate getAuthorizableTypePredicate(int authorizableType, boolean exact) {
        return new AuthorizableTypePredicate(authorizableType, exact);
    }

    //--------------------------------------------------------------------------
    /**
     *
     */
    static class AuthorizableTypePredicate implements Predicate {

        private final int authorizableType;
        private final boolean exact;

        private AuthorizableTypePredicate(int authorizableType, boolean exact) {
            this.authorizableType = authorizableType;
            this.exact = exact;
        }

        /**
         * @see Predicate#evaluate(Object)
         */
        public boolean evaluate(Object object) {
            if (object instanceof NodeImpl) {
                Node n = getAuthorizableNode((NodeImpl) object);
                return n != null;
            }
            return false;
        }

        Node getAuthorizableNode(NodeImpl n) {
            try {
                if (matches(n)) {
                    return n;
                }

                if (!exact) {
                    // walk up the node hierarchy to verify it is a child node
                    // of an authorizable node of the expected type.
                    while (n.getDepth() > 0) {
                        n = (NodeImpl) n.getParent();
                        if (matches(n)) {
                            return n;
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.debug(e.getMessage());
            }
            return null;
        }

        private boolean matches(NodeImpl result) throws RepositoryException {
            Name ntName = ((NodeTypeImpl) result.getPrimaryNodeType()).getQName();
            switch (authorizableType) {
                case UserManager.SEARCH_TYPE_GROUP:
                    return UserConstants.NT_REP_GROUP.equals(ntName);
                case UserManager.SEARCH_TYPE_USER:
                    return UserConstants.NT_REP_USER.equals(ntName);
                default:
                    return UserConstants.NT_REP_USER.equals(ntName) || UserConstants.NT_REP_GROUP.equals(ntName);
            }
        }
    }
}


