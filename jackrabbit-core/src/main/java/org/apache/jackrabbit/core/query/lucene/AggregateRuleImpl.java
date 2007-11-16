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
package org.apache.jackrabbit.core.query.lucene;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.name.NameConstants;
import org.apache.jackrabbit.name.PathBuilder;
import org.apache.jackrabbit.conversion.IllegalNameException;
import org.apache.jackrabbit.conversion.MalformedPathException;
import org.apache.jackrabbit.conversion.NameResolver;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.util.Text;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.CharacterData;

import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;

/**
 * <code>AggregateRule</code> defines a configuration for a node index
 * aggregate. It defines rules for items that should be included in the node
 * scope index of an ancestor. Per default the values of properties are only
 * added to the node scope index of the parent node.
 */
class AggregateRuleImpl implements AggregateRule {

    /**
     * A name resolver for parsing QNames in the configuration.
     */
    private final NameResolver resolver;

    /**
     * The node type of the root node of the indexing aggregate.
     */
    private final Name nodeTypeName;

    /**
     * The rules that define this indexing aggregate.
     */
    private final Rule[] rules;

    /**
     * The item state manager to retrieve additional item states.
     */
    private final ItemStateManager ism;

    /**
     * A hierarchy resolver for the item state manager.
     */
    private final HierarchyManager hmgr;

    /**
     * Creates a new indexing aggregate using the given <code>config</code>.
     *
     * @param config     the configuration for this indexing aggregate.
     * @param resolver   the name resolver for parsing Names within the config.
     * @param ism        the item state manager of the workspace.
     * @param hmgr       a hierarchy manager for the item state manager.
     * @throws MalformedPathException if a path in the configuration is
     *                                malformed.
     * @throws IllegalNameException   if a node type name contains illegal
     *                                characters.
     * @throws NamespaceException if a node type contains an unknown
     *                                prefix.
     */
    AggregateRuleImpl(Node config,
                      NameResolver resolver,
                      ItemStateManager ism,
                      HierarchyManager hmgr)
            throws MalformedPathException, IllegalNameException, NamespaceException {
        this.resolver = resolver;
        this.nodeTypeName = getNodeTypeName(config);
        this.rules = getRules(config);
        this.ism = ism;
        this.hmgr = hmgr;
    }

    /**
     * Returns root node state for the indexing aggregate where
     * <code>nodeState</code> belongs to.
     *
     * @param nodeState
     * @return the root node state of the indexing aggregate or
     *         <code>null</code> if <code>nodeState</code> does not belong to an
     *         indexing aggregate.
     * @throws ItemStateException  if an error occurs.
     * @throws RepositoryException if an error occurs.
     */
    public NodeState getAggregateRoot(NodeState nodeState)
            throws ItemStateException, RepositoryException {
        for (int i = 0; i < rules.length; i++) {
            NodeState aggregateRoot = rules[i].matches(nodeState);
            if (aggregateRoot != null &&
                    aggregateRoot.getNodeTypeName().equals(nodeTypeName)) {
                return aggregateRoot;
            }
        }
        return null;
    }

    /**
     * Returns the node states that are part of the indexing aggregate of the
     * <code>nodeState</code>.
     *
     * @param nodeState a node state
     * @return the node states that are part of the indexing aggregate of
     *         <code>nodeState</code>. Returns <code>null</code> if this
     *         aggregate does not apply to <code>nodeState</code>.
     * @throws ItemStateException  if an error occurs.
     */
    public NodeState[] getAggregatedNodeStates(NodeState nodeState)
            throws ItemStateException {
        if (nodeState.getNodeTypeName().equals(nodeTypeName)) {
            List nodeStates = new ArrayList();
            for (int i = 0; i < rules.length; i++) {
                nodeStates.addAll(Arrays.asList(rules[i].resolve(nodeState)));
            }
            if (nodeStates.size() > 0) {
                return (NodeState[]) nodeStates.toArray(new NodeState[nodeStates.size()]);
            }
        }
        return null;
    }

    //---------------------------< internal >-----------------------------------

    /**
     * Reads the node type of the root node of the indexing aggregate.
     *
     * @param config the configuration.
     * @return the name of the node type.
     * @throws IllegalNameException   if the node type name contains illegal
     *                                characters.
     * @throws NamespaceException if the node type contains an unknown
     *                                prefix.
     */
    private Name getNodeTypeName(Node config)
            throws IllegalNameException, NamespaceException {
        String ntString = config.getAttributes().getNamedItem("primaryType").getNodeValue();
        return resolver.getQName(ntString);
    }

    /**
     * Creates rules defined in the <code>config</code>.
     *
     * @param config the indexing aggregate configuration.
     * @return the rules defined in the <code>config</code>.
     * @throws MalformedPathException if a path in the configuration is
     *                                malformed.
     * @throws IllegalNameException   if the node type name contains illegal
     *                                characters.
     * @throws NamespaceException if the node type contains an unknown
     *                                prefix.
     */
    private Rule[] getRules(Node config)
            throws MalformedPathException, IllegalNameException, NamespaceException {
        List rules = new ArrayList();
        NodeList childNodes = config.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node n = childNodes.item(i);
            if (n.getNodeName().equals("include")) {
                Name ntName = null;
                Node ntAttr = n.getAttributes().getNamedItem("primaryType");
                if (ntAttr != null) {
                    ntName = resolver.getQName(ntAttr.getNodeValue());
                }
                String[] elements = Text.explode(getTextContent(n), '/');
                PathBuilder builder = new PathBuilder();
                for (int j = 0; j < elements.length; j++) {
                    if (elements[j].equals("*")) {
                        builder.addLast(NameConstants.ANY_NAME);
                    } else {
                        builder.addLast(resolver.getQName(elements[j]));
                    }
                }
                rules.add(new Rule(builder.getPath(), ntName));
            }
        }
        return (Rule[]) rules.toArray(new Rule[rules.size()]);
    }

    //---------------------------< internal >-----------------------------------

    /**
     * @param node a node.
     * @return the text content of the <code>node</code>.
     */
    private static String getTextContent(Node node) {
        StringBuffer content = new StringBuffer();
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() == Node.TEXT_NODE) {
                content.append(((CharacterData) n).getData());
            }
        }
        return content.toString();
    }

    private final class Rule {

        /**
         * Optional node type name.
         */
        private final Name nodeTypeName;

        /**
         * A relative path pattern.
         */
        private final Path pattern;

        /**
         * Creates a new rule with a relative path pattern and an optional node
         * type name.
         *
         * @param nodeTypeName node type name or <code>null</code> if all node
         *                     types are allowed.
         * @param pattern      a relative path pattern.
         */
        private Rule(Path pattern, Name nodeTypeName) {
            this.nodeTypeName = nodeTypeName;
            this.pattern = pattern;
        }

        /**
         * If the given <code>nodeState</code> matches this rule the root node
         * state of the indexing aggregate is returned.
         *
         * @param nodeState a node state.
         * @return the root node state of the indexing aggregate or
         *         <code>null</code> if <code>nodeState</code> does not belong
         *         to an indexing aggregate defined by this rule.
         */
        NodeState matches(NodeState nodeState)
                throws ItemStateException, RepositoryException {
            // first check node type
            if (nodeTypeName == null ||
                    nodeState.getNodeTypeName().equals(nodeTypeName)) {
                // check pattern
                Path.Element[] elements = pattern.getElements();
                for (int e = elements.length - 1; e >= 0; e--) {
                    NodeId parentId = nodeState.getParentId();
                    if (parentId == null) {
                        // nodeState is root node
                        return null;
                    }
                    NodeState parent = (NodeState) ism.getItemState(parentId);
                    if (elements[e].getName().getLocalName().equals("*")) {
                        // match any parent
                        nodeState = parent;
                    } else {
                        // check name
                        Name name = hmgr.getName(nodeState.getId());
                        if (elements[e].getName().equals(name)) {
                            nodeState = parent;
                        } else {
                            return null;
                        }
                    }
                }
                // if we get here nodeState became the root
                // of the indexing aggregate and is valid
                return nodeState;
            }
            return null;
        }

        /**
         * Resolves the <code>nodeState</code> using this rule.
         *
         * @param nodeState the root node of the enclosing indexing aggregate.
         * @return the descendant node states as defined by this rule.
         * @throws ItemStateException if an error occurs while resolving the
         *                            node states.
         */
        NodeState[] resolve(NodeState nodeState) throws ItemStateException {
            List nodeStates = new ArrayList();
            resolve(nodeState, nodeStates, 0);
            return (NodeState[]) nodeStates.toArray(new NodeState[nodeStates.size()]);
        }

        //-----------------------------< internal >-----------------------------

        /**
         * Recursively resolves node states along the path {@link #pattern}.
         *
         * @param nodeState the current node state.
         * @param collector resolved node states are collected using the list.
         * @param offset    the current path element offset into the path
         *                  pattern.
         * @throws ItemStateException if an error occurs while accessing node
         *                            states.
         */
        private void resolve(NodeState nodeState, List collector, int offset)
                throws ItemStateException {
            Name currentName = pattern.getElements()[offset].getName();
            List cne;
            if (currentName.getLocalName().equals("*")) {
                // matches all
                cne = nodeState.getChildNodeEntries();
            } else {
                cne = nodeState.getChildNodeEntries(currentName);
            }
            if (pattern.getLength() - 1 == offset) {
                // last segment -> add to collector if node type matches
                for (Iterator it = cne.iterator(); it.hasNext(); ) {
                    NodeId id = ((NodeState.ChildNodeEntry) it.next()).getId();
                    NodeState ns = (NodeState) ism.getItemState(id);
                    if (nodeTypeName != null || ns.getNodeTypeName().equals(nodeTypeName)) {
                        collector.add(ns);
                    }
                }
            } else {
                // traverse
                offset++;
                for (Iterator it = cne.iterator(); it.hasNext(); ) {
                    NodeId id = ((NodeState.ChildNodeEntry) it.next()).getId();
                    resolve((NodeState) ism.getItemState(id), collector, offset);
                }
            }
        }
    }
}
