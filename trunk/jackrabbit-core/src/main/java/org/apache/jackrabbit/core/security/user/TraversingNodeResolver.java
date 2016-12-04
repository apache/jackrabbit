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
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

/**
 *
 */
class TraversingNodeResolver extends NodeResolver {

    private static final Logger log = LoggerFactory.getLogger(TraversingNodeResolver.class);

    /**
     * Additionally to the NodeType-Argument the resolvers searched is narrowed
     * by indicating a Path to an {@link javax.jcr.Item} as start for the search
     *
     * @param session to use for repository access
     * @param resolver The NamePathResolver used to convert {@link org.apache.jackrabbit.spi.Name}
     * and {@link org.apache.jackrabbit.spi.Path} to JCR names/path.
     */
    TraversingNodeResolver(Session session, NamePathResolver resolver) {
        super(session, resolver);
    }

    //-------------------------------------------------------< NodeResolver >---
    /**
     * @inheritDoc
     */
    @Override
    public Node findNode(Name nodeName, Name ntName) throws RepositoryException {
        String sr = getSearchRoot(ntName);
        if (getSession().nodeExists(sr)) {
            try {
                Node root = getSession().getNode(sr);
                return collectNode(nodeName, ntName, root.getNodes());
            } catch (PathNotFoundException e) {
                // should not get here
                log.warn("Error while retrieving node " + sr);
            }
        } // else: searchRoot does not exist yet -> omit the search
        return null;
    }

    /**
     * @inheritDoc
     */
    @Override
    public Node findNode(Name propertyName, String value, Name ntName) throws RepositoryException {
        String sr = getSearchRoot(ntName);
        if (getSession().nodeExists(sr)) {
            try {
                Node root = getSession().getNode(sr);
                Set<Node> matchSet = new HashSet<Node>();
                collectNodes(value, Collections.singleton(propertyName), ntName, root.getNodes(), matchSet, true, 1);

                NodeIterator it = new NodeIteratorAdapter(matchSet);
                if (it.hasNext()) {
                    return it.nextNode();
                }
            } catch (PathNotFoundException e) {
                // should not get here
                log.warn("Error while retrieving node " + sr);
            }
        } // else: searchRoot does not exist yet -> omit the search
        return null;
    }

    /**
     * @inheritDoc
     */
    @Override
    public NodeIterator findNodes(Set<Name> propertyNames, String value, Name ntName,
                                  boolean exact, long maxSize) throws RepositoryException {
        String sr = getSearchRoot(ntName);
        if (getSession().nodeExists(sr)) {
            try {
                Node root = getSession().getNode(sr);
                Set<Node> matchSet = new HashSet<Node>();
                collectNodes(value, propertyNames, ntName, root.getNodes(), matchSet, exact, maxSize);
                return new NodeIteratorAdapter(matchSet);
            } catch (PathNotFoundException e) {
                // should not get here
                log.warn("Error while retrieving node " + sr);
            }
        } // else: searchRoot does not exist yet -> omit the search
        return NodeIteratorAdapter.EMPTY;
    }

    /**
     * @inheritDoc
     */
    @Override
    public NodeIterator findNodes(Path relPath, String value, int authorizableType, boolean exact, long maxSize) throws RepositoryException {
        String sr = getSearchRoot(authorizableType);
        if (getSession().nodeExists(sr)) {
            try {
                String path = getNamePathResolver().getJCRPath(relPath);
                AuthorizableTypePredicate pred = getAuthorizableTypePredicate(authorizableType, relPath.getLength() > 1);

                Node root = getSession().getNode(sr);
                Map<String, Node> matchingNodes = new HashMap<String, Node>();
                collectNodes(value, path, pred, root.getNodes(), matchingNodes, exact, maxSize);

                return new NodeIteratorAdapter(matchingNodes.values());
            } catch (PathNotFoundException e) {
                // should not get here
                log.warn("Error while retrieving node " + sr);
            }
        } // else: searchRoot does not exist yet -> omit the search
        return NodeIteratorAdapter.EMPTY;
    }

    //--------------------------------------------------------------------------
    /**
     *
     * @param nodeName
     * @param ntName
     * @param nodes
     * @return The first matching node or <code>null</code>.
     */
    private Node collectNode(Name nodeName, Name ntName, NodeIterator nodes) {
        Node match = null;
        while (match == null && nodes.hasNext()) {
            NodeImpl node = (NodeImpl) nodes.nextNode();
            try {
                if (node.isNodeType(ntName) && nodeName.equals(node.getQName())) {
                    match = node;
                } else if (node.hasNodes()) {
                    match = collectNode(nodeName, ntName, node.getNodes());
                }
            } catch (RepositoryException e) {
                log.warn("Internal error while accessing node", e);
            }
        }
        return match;
    }

    /**
     * Searches the given value in the range of the given NodeIterator.
     * This method is called recursively to look within the complete tree
     * of authorizable nodes.
     *
     * @param value         the value to be found in the nodes
     * @param propertyNames property to be searched, or null if {@link javax.jcr.Item#getName()}
     * @param nodeTypeName  name of node types to search
     * @param itr           range of nodes and descendants to be searched
     * @param matchSet      Set of found matches to append results
     * @param exact         if set to true the value has to match exact
     * @param maxSize
     */
    private void collectNodes(String value, Set<Name> propertyNames,
                              Name nodeTypeName, NodeIterator itr,
                              Set<Node> matchSet, boolean exact, long maxSize) {
        while (itr.hasNext()) {
            NodeImpl node = (NodeImpl) itr.nextNode();
            try {
                if (matches(node, nodeTypeName, propertyNames, value, exact)) {
                    matchSet.add(node);
                    maxSize--;
                }
                if (node.hasNodes() && maxSize > 0) {
                    collectNodes(value, propertyNames, nodeTypeName,
                            node.getNodes(), matchSet, exact, maxSize);
                }
            } catch (RepositoryException e) {
                log.warn("Internal error while accessing node", e);
            }
        }
    }

    private void collectNodes(String value, String relPath,
                              AuthorizableTypePredicate predicate, NodeIterator itr,
                              Map<String, Node> matchingNodes, boolean exact, long maxSize) {
        while (itr.hasNext()) {
            NodeImpl node = (NodeImpl) itr.nextNode();
            try {
                Node authNode = getMatchingNode(node, predicate, relPath, value, exact);
                if (authNode != null) {
                    matchingNodes.put(authNode.getIdentifier(), authNode);
                    maxSize--;
                } else if (node.hasNodes() && maxSize > 0) {
                    collectNodes(value, relPath, predicate, node.getNodes(), matchingNodes, exact, maxSize);
                }
            } catch (RepositoryException e) {
                log.warn("Internal error while accessing node", e);
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
    private static boolean matches(NodeImpl node, Name nodeTypeName,
                            Collection<Name> propertyNames, String value,
                            boolean exact) throws RepositoryException {

        boolean match = false;
        if (node.isNodeType(nodeTypeName)) {
            if (value == null) {
                match = true;
            } else {
                try {
                    if (propertyNames.isEmpty()) {
                        match = (exact) ? node.getName().equals(value) :
                                node.getName().matches(".*"+value+".*");
                    } else {
                        Iterator<Name> pItr = propertyNames.iterator();
                        while (!match && pItr.hasNext()) {
                            Name propertyName = pItr.next();
                            if (node.hasProperty(propertyName)) {
                                Property prop = node.getProperty(propertyName);
                                if (prop.isMultiple()) {
                                    Value[] values = prop.getValues();
                                    for (int i = 0; i < values.length && !match; i++) {
                                        match = matches(value, values[i].getString(), exact);
                                    }
                                } else {
                                    match = matches(value, prop.getString(), exact);
                                }
                            }
                        }
                    }
                } catch (PatternSyntaxException pe) {
                    log.debug("couldn't search for {}, pattern invalid: {}",
                            value, pe.getMessage());
                }
            }
        }
        return match;
    }

    /**
     *
     * @param node
     * @param predicate
     * @param relPath
     * @param value
     * @param exact
     * @return
     * @throws RepositoryException
     */
    private static Node getMatchingNode(NodeImpl node, AuthorizableTypePredicate predicate,
                                        String relPath, String value,
                                        boolean exact) throws RepositoryException {
        boolean match = false;
        Node authNode = predicate.getAuthorizableNode(node);
        if (authNode != null && node.hasProperty(relPath)) {
            try {
                Property prop = node.getProperty(relPath);
                if (prop.isMultiple()) {
                    Value[] values = prop.getValues();
                    for (int i = 0; i < values.length && !match; i++) {
                        match = matches(value, values[i].getString(), exact);
                    }
                } else {
                    match = matches(value, prop.getString(), exact);
                }
            } catch (PatternSyntaxException pe) {
                log.debug("couldn't search for {}, pattern invalid: {}", value, pe.getMessage());
            }
        }
        return (match) ? authNode : null;
    }

    private static boolean matches(String value, String toMatch, boolean exact) {
        return (exact) ? toMatch.equals(value) : toMatch.matches(".*"+value+".*");
    }
}
