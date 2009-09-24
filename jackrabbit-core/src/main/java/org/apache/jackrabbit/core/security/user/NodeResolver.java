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

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.util.Text;

/**
 * Resolver: searches for Principals stored in Nodes of a {@link javax.jcr.Workspace}
 * which match a certain criteria<p>
 * The principalNames are assumed to be stored in properties.
 */
abstract class NodeResolver {

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
     * Search nodes. Take the arguments as search cirteria.
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
}


