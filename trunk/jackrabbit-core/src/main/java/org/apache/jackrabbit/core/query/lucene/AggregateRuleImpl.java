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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.apache.jackrabbit.util.Text;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
     * The node includes of this indexing aggregate.
     */
    private final NodeInclude[] nodeIncludes;

    /**
     * The property includes of this indexing aggregate.
     */
    private final PropertyInclude[] propertyIncludes;

    /**
     * The item state manager to retrieve additional item states.
     */
    private final ItemStateManager ism;

    /**
     * A hierarchy resolver for the item state manager.
     */
    private final HierarchyManager hmgr;

    /**
     * recursive aggregation (for same type nodes) default value.
     */
    private static final boolean RECURSIVE_AGGREGATION_DEFAULT = false;

    /**
     * flag to enable recursive aggregation (for same type nodes).
     */
    private final boolean recursiveAggregation;

    /**
     * recursive aggregation (for same type nodes) limit default value.
     */

    protected static final long RECURSIVE_AGGREGATION_LIMIT_DEFAULT = 100;

    /**
     * recursive aggregation (for same type nodes) limit. embedded aggregation
     * of nodes that have the same type can go only this levels up.
     * 
     * A value eq to 0 gives unlimited aggregation.
     */
    private final long recursiveAggregationLimit;

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
     * @throws RepositoryException If another error occurs.
     */
    AggregateRuleImpl(Node config,
                      NameResolver resolver,
                      ItemStateManager ism,
                      HierarchyManager hmgr) throws MalformedPathException,
            IllegalNameException, NamespaceException, RepositoryException {
        this.resolver = resolver;
        this.nodeTypeName = getNodeTypeName(config);
        this.nodeIncludes = getNodeIncludes(config);
        this.propertyIncludes = getPropertyIncludes(config);
        this.ism = ism;
        this.hmgr = hmgr;
        this.recursiveAggregation = getRecursiveAggregation(config);
        this.recursiveAggregationLimit = getRecursiveAggregationLimit(config);
    }

    /**
     * Returns root node state for the indexing aggregate where
     * <code>nodeState</code> belongs to.
     *
     * @param nodeState the node state.
     * @return the root node state of the indexing aggregate or
     *         <code>null</code> if <code>nodeState</code> does not belong to an
     *         indexing aggregate.
     * @throws ItemStateException  if an error occurs.
     * @throws RepositoryException if an error occurs.
     */
    public NodeState getAggregateRoot(NodeState nodeState)
            throws ItemStateException, RepositoryException {
        for (NodeInclude nodeInclude : nodeIncludes) {
            NodeState aggregateRoot = nodeInclude.matches(nodeState);
            if (aggregateRoot != null && aggregateRoot.getNodeTypeName().equals(nodeTypeName)) {
                boolean sameNodeTypeAsRoot = nodeState.getNodeTypeName().equals(aggregateRoot.getNodeTypeName());
                if(!sameNodeTypeAsRoot || (sameNodeTypeAsRoot && recursiveAggregation)){
                    return aggregateRoot;
                }
            }
        }
        
        // check property includes
        for (PropertyInclude propertyInclude : propertyIncludes) {
            NodeState aggregateRoot = propertyInclude.matches(nodeState);
            if (aggregateRoot != null && aggregateRoot.getNodeTypeName().equals(nodeTypeName)) {
                boolean sameNodeTypeAsRoot = nodeState.getNodeTypeName().equals(aggregateRoot.getNodeTypeName());
                if(!sameNodeTypeAsRoot || (sameNodeTypeAsRoot && recursiveAggregation)){
                    return aggregateRoot;
                }
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
            List<NodeState> nodeStates = new ArrayList<NodeState>();
            for (NodeInclude nodeInclude : nodeIncludes) {
                for (NodeState childNs : nodeInclude.resolve(nodeState)) {
                    boolean sameNodeTypeAsRoot = nodeState.getNodeTypeName().equals(childNs.getNodeTypeName());
                    if (!sameNodeTypeAsRoot || (sameNodeTypeAsRoot && recursiveAggregation)) {
                        nodeStates.add(childNs);
                    }
                }
            }
            if (nodeStates.size() > 0) {
                return nodeStates.toArray(new NodeState[nodeStates.size()]);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public PropertyState[] getAggregatedPropertyStates(NodeState nodeState)
            throws ItemStateException {
        if (nodeState.getNodeTypeName().equals(nodeTypeName)) {
            List<PropertyState> propStates = new ArrayList<PropertyState>();
            for (PropertyInclude propertyInclude : propertyIncludes) {
                propStates.addAll(Arrays.asList(propertyInclude.resolvePropertyStates(nodeState)));
            }
            if (propStates.size() > 0) {
                return propStates.toArray(new PropertyState[propStates.size()]);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public long getRecursiveAggregationLimit() {
        return recursiveAggregationLimit;
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
     * Creates node includes defined in the <code>config</code>.
     *
     * @param config the indexing aggregate configuration.
     * @return the node includes defined in the <code>config</code>.
     * @throws MalformedPathException if a path in the configuration is
     *                                malformed.
     * @throws IllegalNameException   if the node type name contains illegal
     *                                characters.
     * @throws NamespaceException if the node type contains an unknown
     *                                prefix.
     */
    private NodeInclude[] getNodeIncludes(Node config)
            throws MalformedPathException, IllegalNameException, NamespaceException {
        List<NodeInclude> includes = new ArrayList<NodeInclude>();
        NodeList childNodes = config.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node n = childNodes.item(i);
            if (n.getNodeName().equals("include")) {
                Name ntName = null;
                Node ntAttr = n.getAttributes().getNamedItem("primaryType");
                if (ntAttr != null) {
                    ntName = resolver.getQName(ntAttr.getNodeValue());
                }
                PathBuilder builder = new PathBuilder();
                for (String element : Text.explode(getTextContent(n), '/')) {
                    if (element.equals("*")) {
                        builder.addLast(NameConstants.ANY_NAME);
                    } else {
                        builder.addLast(resolver.getQName(element));
                    }
                }
                includes.add(new NodeInclude(builder.getPath(), ntName));
            }
        }
        return includes.toArray(new NodeInclude[includes.size()]);
    }

    /**
     * Creates property includes defined in the <code>config</code>.
     *
     * @param config the indexing aggregate configuration.
     * @return the property includes defined in the <code>config</code>.
     * @throws MalformedPathException if a path in the configuration is
     *                                malformed.
     * @throws IllegalNameException   if the node type name contains illegal
     *                                characters.
     * @throws NamespaceException if the node type contains an unknown
     *                                prefix.
     * @throws RepositoryException If the PropertyInclude cannot be builded
     * due to unknown ancestor relationship.
     */
    private PropertyInclude[] getPropertyIncludes(Node config) throws
            MalformedPathException, IllegalNameException, NamespaceException,
            RepositoryException {
        List<PropertyInclude> includes = new ArrayList<PropertyInclude>();
        NodeList childNodes = config.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node n = childNodes.item(i);
            if (n.getNodeName().equals("include-property")) {
                PathBuilder builder = new PathBuilder();
                for (String element : Text.explode(getTextContent(n), '/')) {
                    if (element.equals("*")) {
                        throw new IllegalNameException("* not supported in include-property");
                    }
                    builder.addLast(resolver.getQName(element));
                }
                includes.add(new PropertyInclude(builder.getPath()));
            }
        }
        return includes.toArray(new PropertyInclude[includes.size()]);
    }
    
    private boolean getRecursiveAggregation(Node config) {
        Node rAttr = config.getAttributes().getNamedItem("recursive");
        if (rAttr == null) {
            return RECURSIVE_AGGREGATION_DEFAULT;
        }
        return Boolean.valueOf(rAttr.getNodeValue());
    }

    private long getRecursiveAggregationLimit(Node config)
            throws RepositoryException {
        Node rAttr = config.getAttributes().getNamedItem("recursiveLimit");
        if (rAttr == null) {
            return RECURSIVE_AGGREGATION_LIMIT_DEFAULT;
        }
        try {
            return Long.valueOf(rAttr.getNodeValue());
        } catch (NumberFormatException e) {
            throw new RepositoryException(
                    "Unable to read indexing configuration (recursiveLimit).",
                    e);
        }
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

    private abstract class AbstractInclude {

        /**
         * Optional node type name.
         */
        protected final Name nodeTypeName;

        /**
         * A relative path pattern.
         */
        protected final Path pattern;

        /**
         * Creates a new rule with a relative path pattern and an optional node
         * type name.
         *
         * @param nodeTypeName node type name or <code>null</code> if all node
         *                     types are allowed.
         * @param pattern      a relative path pattern.
         */
        AbstractInclude(Path pattern, Name nodeTypeName) {
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
         * @throws ItemStateException if an error occurs while accessing node
         *                            states.
         * @throws RepositoryException if another error occurs.
         */
        NodeState matches(NodeState nodeState)
                throws ItemStateException, RepositoryException {
            // first check node type
            if (nodeTypeName == null
                    || nodeState.getNodeTypeName().equals(nodeTypeName)) {
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
        protected void resolve(NodeState nodeState, List<NodeState> collector, int offset)
                throws ItemStateException {
            Name currentName = pattern.getElements()[offset].getName();
            List<ChildNodeEntry> cne;
            if (currentName.getLocalName().equals("*")) {
                // matches all
                cne = nodeState.getChildNodeEntries();
            } else {
                cne = nodeState.getChildNodeEntries(currentName);
            }
            if (pattern.getLength() - 1 == offset) {
                // last segment -> add to collector if node type matches
                for (ChildNodeEntry entry : cne) {
                    NodeState ns = (NodeState) ism.getItemState(entry.getId());
                    if (nodeTypeName == null || ns.getNodeTypeName().equals(nodeTypeName)) {
                        collector.add(ns);
                    }
                }
            } else {
                // traverse
                offset++;
                for (ChildNodeEntry entry : cne) {
                    NodeId id = entry.getId();
                    resolve((NodeState) ism.getItemState(id), collector, offset);
                }
            }
        }
    }

    private final class NodeInclude extends AbstractInclude {

        /**
         * Creates a new node include with a relative path pattern and an
         * optional node type name.
         *
         * @param nodeTypeName node type name or <code>null</code> if all node
         *                     types are allowed.
         * @param pattern      a relative path pattern.
         */
        NodeInclude(Path pattern, Name nodeTypeName) {
            super(pattern, nodeTypeName);
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
            List<NodeState> nodeStates = new ArrayList<NodeState>();
            resolve(nodeState, nodeStates, 0);
            return nodeStates.toArray(new NodeState[nodeStates.size()]);
        }
    }

    private final class PropertyInclude extends AbstractInclude {

        private final Name propertyName;

        PropertyInclude(Path pattern)
                throws RepositoryException {
            super(pattern.getAncestor(1), null);
            this.propertyName = pattern.getName();
        }

        /**
         * Resolves the <code>nodeState</code> using this rule.
         *
         * @param nodeState the root node of the enclosing indexing aggregate.
         * @return the descendant property states as defined by this rule.
         * @throws ItemStateException if an error occurs while resolving the
         *                            property states.
         */
        PropertyState[] resolvePropertyStates(NodeState nodeState)
                throws ItemStateException {
            List<NodeState> nodeStates = new ArrayList<NodeState>();
            resolve(nodeState, nodeStates, 0);
            List<PropertyState> propStates = new ArrayList<PropertyState>();
            for (NodeState state : nodeStates) {
                if (state.hasPropertyName(propertyName)) {
                    PropertyId propId = new PropertyId(state.getNodeId(), propertyName);
                    propStates.add((PropertyState) ism.getItemState(propId));
                }
            }
            return propStates.toArray(new PropertyState[propStates.size()]);
        }
    }
}
