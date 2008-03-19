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

import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.spi.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Collections;
import java.util.regex.PatternSyntaxException;

/**
 *
 */
class TraversingNodeResolver extends NodeResolver {

    private static final Logger log = LoggerFactory.getLogger(TraversingNodeResolver.class);

    /**
     * Additonally to the NodeType-Argument the resolvers searched is narrowed
     * by indicating a Path to an {@link javax.jcr.Item} as start for the search
     *
     * @param session      to use for repository access
     */
    TraversingNodeResolver(SessionImpl session) throws RepositoryException {
        super(session);
    }

    //-------------------------------------------------------< NodeResolver >---
    /**
     * @inheritDoc
     */
    public Node findNode(Name propertyName, String value, Name ntName) throws RepositoryException {
        try {
            Node root = (Node) getSession().getItem(getSearchRoot(ntName));
            NodeIterator nodes = collectNodes(value, Collections.singleton(propertyName), ntName,
                    root.getNodes(), true, 1);
            if (nodes.hasNext()) {
                return nodes.nextNode();
            }
        } catch (PathNotFoundException e) {
            log.warn("Error while searching for node having a property " + propertyName + " with value " + value);
        }

        return null;
    }

    /**
     * @inheritDoc
     */
    public NodeIterator findNodes(Set propertyNames, String value, Name ntName,
                                  boolean exact, long maxSize) throws RepositoryException {

        NodeImpl root = (NodeImpl) getSession().getItem(getSearchRoot(ntName));
        return collectNodes(value, propertyNames, ntName, root.getNodes(), exact, maxSize);
    }

    //--------------------------------------------------------------------------
    /**
     * searches the given value in the range of the given NodeIterator.
     * recurses unitll all matching values in all configured props are found.
     *
     * @param value   the value to be found in the nodes
     * @param props   property to be searched, or null if {@link javax.jcr.Item#getName()}
     * @param ntName  to filter search
     * @param nodes   range of nodes and descendants to be searched
     * @param exact   if set to true the value has to match exactly else a
     * substring is searched
     * @param maxSize
     */
    private NodeIterator collectNodes(String value, Set props, Name ntName,
                                      NodeIterator nodes, boolean exact,
                                      long maxSize) {
        Set matches = new HashSet();
        collectNodes(value, props, ntName, nodes, matches, exact, maxSize);
        return new NodeIteratorAdapter(matches);
    }

    /**
     * searches the given value in the range of the given NodeIterator.
     * recurses unitll all matching values in all configured properties are found.
     *
     * @param value         the value to be found in the nodes
     * @param propertyNames property to be searched, or null if {@link javax.jcr.Item#getName()}
     * @param nodeTypeName  name of nodetypes to search
     * @param itr           range of nodes and descendants to be searched
     * @param matches       Set of found matches to append results
     * @param exact         if set to true the value has to match exact
     * @param maxSize
     */
    private void collectNodes(String value, Set propertyNames,
                              Name nodeTypeName, NodeIterator itr,
                              Set matches, boolean exact, long maxSize) {
        while (itr.hasNext()) {
            NodeImpl node = (NodeImpl) itr.nextNode();
            try {
                if (matches(node, nodeTypeName, propertyNames, value, exact)) {
                    matches.add(node);
                    maxSize--;
                }
                if (node.hasNodes() && maxSize > 0) {
                    collectNodes(value, propertyNames, nodeTypeName,
                            node.getNodes(), matches, exact, maxSize);
                }
            } catch (RepositoryException e) {
                log.warn("failed to access Node at " + e);
            }
        }
    }

    /**
     * 
     * @param node
     * @param nodeTypeName
     * @param propertyNames
     * @param value
     * @param exact
     * @return
     * @throws RepositoryException
     */
    private boolean matches(NodeImpl node, Name nodeTypeName,
                            Collection propertyNames, String value,
                            boolean exact) throws RepositoryException {

        boolean match = false;
        if (node.isNodeType(nodeTypeName)) {
            try {
                if (propertyNames.isEmpty()) {
                    match = (exact) ? node.getName().equals(value) :
                            node.getName().matches(".*"+value+".*");
                } else {
                    Iterator pItr = propertyNames.iterator();
                    while (!match && pItr.hasNext()) {
                        Name propertyName = (Name) pItr.next();
                        if (node.hasProperty(propertyName)) {
                            String toMatch = node.getProperty(propertyName).getString();
                            match = (exact) ?
                                    toMatch.equals(value) :
                                    toMatch.matches(".*"+value+".*");
                        }
                    }
                }
            } catch (PatternSyntaxException pe) {
                log.debug("couldn't search for {}, pattern invalid: {}",
                          value, pe.getMessage());
            }
        }
        return match;
    }
}
