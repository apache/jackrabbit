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

import org.apache.jackrabbit.conversion.IllegalNameException;
import org.apache.jackrabbit.conversion.MalformedPathException;
import org.apache.jackrabbit.conversion.NameResolver;
import org.apache.jackrabbit.conversion.ParsingNameResolver;
import org.apache.jackrabbit.conversion.PathResolver;
import org.apache.jackrabbit.conversion.ParsingPathResolver;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.HierarchyManagerImpl;
import org.apache.jackrabbit.core.nodetype.xml.AdditionalNamespaceResolver;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.query.QueryHandlerContext;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.name.NameFactoryImpl;
import org.apache.jackrabbit.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.namespace.NamespaceResolver;
import org.apache.lucene.analysis.Analyzer;
import org.apache.commons.collections.iterators.AbstractIteratorDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

/**
 * <code>IndexingConfigurationImpl</code> implements a concrete indexing
 * configuration.
 */
public class IndexingConfigurationImpl implements IndexingConfiguration {

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(IndexingConfigurationImpl.class);
    
    /**
     * A namespace resolver for parsing QNames in the configuration.
     */
    private NameResolver resolver;

    /**
     * The item state manager to retrieve additional item states.
     */
    private ItemStateManager ism;

    /**
     * A hierarchy resolver for the item state manager.
     */
    private HierarchyManager hmgr;

    /**
     * The {@link IndexingRule}s inside this configuration.
     */
    private Map configElements = new HashMap();

    /**
     * The indexing aggregates inside this configuration.
     */
    private AggregateRule[] aggregateRules;

    /**
     * The configured analyzers for indexing properties.
     */
    private Map analyzers = new HashMap();
    
    /**
     * {@inheritDoc}
     */
    public void init(Element config, QueryHandlerContext context, NamespaceMappings nsMappings) throws Exception {
        ism = context.getItemStateManager();
        NameResolver nameResolver = new ParsingNameResolver(NameFactoryImpl.getInstance(),
                context.getNamespaceRegistry());
        PathResolver pathResolver = new ParsingPathResolver(PathFactoryImpl.getInstance(),
                nameResolver);
        hmgr = new HierarchyManagerImpl(context.getRootId(), ism, pathResolver);
        
        NamespaceResolver nsResolver = new AdditionalNamespaceResolver(getNamespaces(config));
        resolver = new ParsingNameResolver(NameFactoryImpl.getInstance(), nsResolver);

        NodeTypeRegistry ntReg = context.getNodeTypeRegistry();
        Name[] ntNames = ntReg.getRegisteredNodeTypes();
        List idxAggregates = new ArrayList();
        NodeList indexingConfigs = config.getChildNodes();
        for (int i = 0; i < indexingConfigs.getLength(); i++) {
            Node configNode = indexingConfigs.item(i);
            if (configNode.getNodeName().equals("index-rule")) {
                IndexingRule element = new IndexingRule(configNode);
                // register under node type and all its sub types
                for (int n = 0; n < ntNames.length; n++) {
                    if (ntReg.getEffectiveNodeType(ntNames[n]).includesNodeType(
                            element.getNodeTypeName())) {
                        List perNtConfig = (List) configElements.get(ntNames[n]);
                        if (perNtConfig == null) {
                            perNtConfig = new ArrayList();
                            configElements.put(ntNames[n], perNtConfig);
                        }
                        perNtConfig.add(element);
                    }
                }
            } else if (configNode.getNodeName().equals("aggregate")) {
                idxAggregates.add(new AggregateRuleImpl(
                        configNode, resolver, ism, hmgr));
            } else if (configNode.getNodeName().equals("analyzers")) {
                NodeList childNodes = configNode.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    Node analyzerNode = childNodes.item(j);
                    if (analyzerNode.getNodeName().equals("analyzer")) {
                        String analyzerClassName = analyzerNode.getAttributes().getNamedItem("class").getNodeValue();
                        try {
                        Class clazz = Class.forName(analyzerClassName);
                            if(clazz == JackrabbitAnalyzer.class) {
                                log.warn("Not allowed to configure " + JackrabbitAnalyzer.class.getName() +  " for a property. " +
                                        "Using default analyzer for that property.");
                            }
                            else if(Analyzer.class.isAssignableFrom(clazz)){
                                Analyzer analyzer = (Analyzer)clazz.newInstance();
                                NodeList propertyChildNodes = analyzerNode.getChildNodes();
                                for (int k = 0; k < propertyChildNodes.getLength(); k++) {
                                    Node propertyNode = propertyChildNodes.item(k);
                                    if (propertyNode.getNodeName().equals("property")) {
                                        // get property name
                                        Name propName = resolver.getQName(getTextContent(propertyNode));
                                        String fieldName = nsMappings.translatePropertyName(propName);
                                        // set analyzer for the fulltext property fieldname
                                        int idx = fieldName.indexOf(':');
                                        fieldName = fieldName.substring(0, idx + 1)
                                                    + FieldNames.FULLTEXT_PREFIX + fieldName.substring(idx + 1);;
                                        Object prevAnalyzer = analyzers.put(fieldName, analyzer);
                                        if(prevAnalyzer!=null){
                                            log.warn("Property " + propName.getLocalName() + " has been configured for multiple analyzers. " +
                                                    " Last configured analyzer is used");
                                        }
                                    }
                                }
                            } else {
                                log.warn("org.apache.lucene.analysis.Analyzer is not a superclass of " 
                                        + analyzerClassName +". Ignoring this configure analyzer" );
                            }
                        } catch (ClassNotFoundException e) {
                            log.warn("Analyzer class not found: " + analyzerClassName, e);
                        }
                    }
                }
            }
            
        }
        aggregateRules = (AggregateRule[]) idxAggregates.toArray(
                new AggregateRule[idxAggregates.size()]);
    }

    /**
     * Returns the configured indexing aggregate rules or <code>null</code> if
     * none exist.
     *
     * @return the configured rules or <code>null</code> if none exist.
     */
    public AggregateRule[] getAggregateRules() {
        return aggregateRules;
    }

    /**
     * Returns <code>true</code> if the property with the given name is fulltext
     * indexed according to this configuration.
     *
     * @param state        the node state.
     * @param propertyName the name of a property.
     * @return <code>true</code> if the property is fulltext indexed;
     *         <code>false</code> otherwise.
     */
    public boolean isIndexed(NodeState state, Name propertyName) {
        IndexingRule rule = getApplicableIndexingRule(state);
        if (rule != null) {
            return rule.isIndexed(propertyName);
        }
        // none of the configs matches -> index property
        return true;
    }

    /**
     * Returns the boost value for the given property name. If there is no
     * configuration entry for the property name the {@link #DEFAULT_BOOST} is
     * returned.
     *
     * @param state        the node state.
     * @param propertyName the name of a property.
     * @return the boost value for the property.
     */
    public float getPropertyBoost(NodeState state, Name propertyName) {
        IndexingRule rule = getApplicableIndexingRule(state);
        if (rule != null) {
            return rule.getBoost(propertyName);
        }
        return DEFAULT_BOOST;
    }

    /**
     * Returns the boost for the node scope fulltext index field.
     *
     * @param state the node state.
     * @return the boost for the node scope fulltext index field.
     */
    public float getNodeBoost(NodeState state) {
        IndexingRule rule = getApplicableIndexingRule(state);
        if (rule != null) {
            return rule.getNodeBoost();
        }
        return DEFAULT_BOOST;
    }

    /**
     * Returns <code>true</code> if the property with the given name should be
     * included in the node scope fulltext index. If there is not configuration
     * entry for that propery <code>false</code> is returned.
     *
     * @param state the node state.
     * @param propertyName the name of a property.
     * @return <code>true</code> if the property should be included in the node
     *         scope fulltext index.
     */
    public boolean isIncludedInNodeScopeIndex(NodeState state,
                                              Name propertyName) {
        IndexingRule rule = getApplicableIndexingRule(state);
        if (rule != null) {
            return rule.isIncludedInNodeScopeIndex(propertyName);
        }
        // none of the config elements matched -> default is to include
        return true;
    }

    
    /**
     * Returns the analyzer configured for the property with this fieldName 
     * (the string representation ,JCR-style name, of the given <code>Name</code>
     * prefixed with <code>FieldNames.FULLTEXT_PREFIX</code>)), 
     * and <code>null</code> if none is configured, or the configured analyzer
     * cannot be found. If <code>null</code> is returned, the default Analyzer
     * is used.
     * 
     * @param fieldName the string representation ,JCR-style name, of the given <code>Name</code>
     * prefixed with <code>FieldNames.FULLTEXT_PREFIX</code>))
     * @return the <code>analyzer</code> to use for indexing this property 
     */
    public Analyzer getPropertyAnalyzer(String fieldName) {
        if(analyzers.containsKey(fieldName)){
            return (Analyzer)analyzers.get(fieldName);
        }
        return null;
    }
    //---------------------------------< internal >-----------------------------

    /**
     * Returns the first indexing rule that applies to the given node
     * <code>state</code>.
     *
     * @param state a node state.
     * @return the indexing rule or <code>null</code> if none applies.
     */
    private IndexingRule getApplicableIndexingRule(NodeState state) {
        List rules = null;
        List r = (List) configElements.get(state.getNodeTypeName());
        if (r != null) {
            rules = new ArrayList();
            rules.addAll(r);
        }

        for (Iterator it = state.getMixinTypeNames().iterator(); it.hasNext(); ) {
            r = (List) configElements.get(it.next());
            if (r != null) {
                if (rules == null) {
                    rules = new ArrayList();
                }
                rules.addAll(r);
            }
        }

        if (rules != null) {
            for (Iterator it = rules.iterator(); it.hasNext(); ) {
                IndexingRule ir = (IndexingRule) it.next();
                if (ir.appliesTo(state)) {
                    return ir;
                }
            }
        }

        // no applicable rule
        return null;
    }

    /**
     * Returns the namespaces declared on the <code>node</code>.
     *
     * @param node a DOM node.
     * @return the namespaces
     */
    private Properties getNamespaces(Node node) {
        Properties namespaces = new Properties();
        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            if (attribute.getName().startsWith("xmlns:")) {
                namespaces.setProperty(
                        attribute.getName().substring(6), attribute.getValue());
            }
        }
        return namespaces;
    }

    /**
     * Creates property configurations defined in the <code>config</code>.
     *
     * @param config the fulltext indexing configuration.
     * @return the property configurations defined in the <code>config</code>.
     * @throws IllegalNameException   if the node type name contains illegal
     *                                characters.
     * @throws NamespaceException if the node type contains an unknown
     *                                prefix.
     */
    private Map getPropertyConfigs(Node config)
            throws IllegalNameException, NamespaceException {
        Map configs = new HashMap();
        NodeList childNodes = config.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node n = childNodes.item(i);
            if (n.getNodeName().equals("property")) {
                NamedNodeMap attributes = n.getAttributes();
                // get boost value
                float boost = 1.0f;
                Node boostAttr = attributes.getNamedItem("boost");
                if (boostAttr != null) {
                    try {
                        boost = Float.parseFloat(boostAttr.getNodeValue());
                    } catch (NumberFormatException e) {
                        // use default
                    }
                }

                // get nodeScopeIndex flag
                boolean nodeScopeIndex = true;
                Node nsIndex = attributes.getNamedItem("nodeScopeIndex");
                if (nsIndex != null) {
                    nodeScopeIndex = Boolean.valueOf(
                            nsIndex.getNodeValue()).booleanValue();
                }

                // get property name
                Name propName = resolver.getQName(getTextContent(n));

                configs.put(propName, new PropertyConfig(boost, nodeScopeIndex));
            }
        }
        return configs;
    }

    /**
     * Gets the condition expression from the configuration.
     *
     * @param config the config node.
     * @return the condition expression or <code>null</code> if there is no
     *         condition set on the <code>config</code>.
     * @throws MalformedPathException if the condition string is malformed.
     * @throws IllegalNameException   if a name contains illegal characters.
     * @throws NamespaceException if a name contains an unknown prefix.
     */
    private PathExpression getCondition(Node config)
            throws MalformedPathException, IllegalNameException, NamespaceException {
        Node conditionAttr = config.getAttributes().getNamedItem("condition");
        if (conditionAttr == null) {
            return null;
        }
        String conditionString = conditionAttr.getNodeValue();
        int idx;
        int axis;
        Name elementTest = null;
        Name nameTest = null;
        Name propertyName;
        String propertyValue;

        // parse axis
        if (conditionString.startsWith("ancestor::")) {
            axis = PathExpression.ANCESTOR;
            idx = "ancestor::".length();
        } else if (conditionString.startsWith("parent::")) {
            axis = PathExpression.PARENT;
            idx = "parent::".length();
        } else if (conditionString.startsWith("@")) {
            axis = PathExpression.SELF;
            idx = "@".length();
        } else {
            axis = PathExpression.CHILD;
            idx = 0;
        }

        try {
            if (conditionString.startsWith("element(", idx)) {
                int colon = conditionString.indexOf(',',
                        idx + "element(".length());
                String name = conditionString.substring(
                        idx + "element(".length(), colon).trim();
                if (!name.equals("*")) {
                    nameTest = resolver.getQName(ISO9075.decode(name));
                }
                idx = conditionString.indexOf(")/@", colon);
                String type = conditionString.substring(colon + 1, idx).trim();
                elementTest = resolver.getQName(ISO9075.decode(type));
                idx += ")/@".length();
            } else {
                if (axis == PathExpression.ANCESTOR ||
                        axis == PathExpression.CHILD ||
                        axis == PathExpression.PARENT) {
                    // simple name test
                    String name = conditionString.substring(idx,
                            conditionString.indexOf('/', idx));
                    if (!name.equals("*")) {
                        nameTest = resolver.getQName(ISO9075.decode(name));
                    }
                    idx += name.length() + "/@".length();
                }
            }

            // parse property name
            int eq = conditionString.indexOf('=', idx);
            String name = conditionString.substring(idx, eq).trim();
            propertyName = resolver.getQName(ISO9075.decode(name));

            // parse string value
            int quote = conditionString.indexOf('\'', eq) + 1;
            propertyValue = conditionString.substring(quote,
                    conditionString.indexOf('\'', quote));
        } catch (IndexOutOfBoundsException e) {
            throw new MalformedPathException(conditionString);
        }

        return new PathExpression(axis, elementTest,
                nameTest, propertyName, propertyValue);
    }

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

    private class IndexingRule {

        /**
         * The node type of this fulltext indexing rule.
         */
        private final Name nodeTypeName;

        /**
         * Map of {@link PropertyConfig}. Key=Name of property.
         */
        private final Map propConfigs;

        /**
         * An expression based on a relative path.
         */
        private final PathExpression condition;

        /**
         * The boost value for this config element.
         */
        private final float boost;

        /**
         *
         * @param config the configuration for this rule.
         * @throws MalformedPathException if the condition expression is malformed.
         * @throws IllegalNameException   if a name contains illegal characters.
         * @throws NamespaceException if a name contains an unknown prefix.
         */
        IndexingRule(Node config)
                throws MalformedPathException, IllegalNameException, NamespaceException {
            this.nodeTypeName = getNodeTypeName(config);
            this.propConfigs = getPropertyConfigs(config);
            this.condition = getCondition(config);
            this.boost = getNodeBoost(config);
        }

        /**
         * Returns the name of the node type where this rule applies to.
         *
         * @return name of the node type.
         */
        public Name getNodeTypeName() {
            return nodeTypeName;
        }

        /**
         * @return the value for the node boost.
         */
        public float getNodeBoost() {
            return boost;
        }

        /**
         * Returns <code>true</code> if the property with the given name is
         * indexed according to this rule.
         *
         * @param propertyName the name of a property.
         * @return <code>true</code> if the property is indexed;
         *         <code>false</code> otherwise.
         */
        public boolean isIndexed(Name propertyName) {
            return propConfigs.containsKey(propertyName);
        }

        /**
         * Returns the boost value for the given property name. If there is no
         * configuration entry for the property name the default boost value is
         * returned.
         *
         * @param propertyName the name of a property.
         * @return the boost value for the property.
         */
        public float getBoost(Name propertyName) {
            PropertyConfig config = (PropertyConfig) propConfigs.get(propertyName);
            if (config != null) {
                return config.boost;
            } else {
                return DEFAULT_BOOST;
            }
        }

        /**
         * Returns <code>true</code> if the property with the given name should
         * be included in the node scope fulltext index. If there is not
         * configuration entry for that propery <code>false</code> is returned.
         *
         * @param propertyName the name of a property.
         * @return <code>true</code> if the property should be included in the
         *         node scope fulltext index.
         */
        public boolean isIncludedInNodeScopeIndex(Name propertyName) {
            PropertyConfig config = (PropertyConfig) propConfigs.get(propertyName);
            if (config != null) {
                return config.nodeScopeIndex;
            } else {
                return false;
            }
        }

        /**
         * Returns <code>true</code> if this rule applies to the given node
         * <code>state</code>.
         *
         * @param state the state to check.
         * @return <code>true</code> the rule applies to the given node;
         *         <code>false</code> otherwise.
         */
        public boolean appliesTo(NodeState state) {
            if (!nodeTypeName.equals(state.getNodeTypeName())) {
                return false;
            }
            if (condition == null) {
                return true;
            } else {
                return condition.evaluate(state);
            }
        }

        //-------------------------< internal >---------------------------------

        /**
         * Reads the node type of the root node of the indexing rule.
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
            String ntString = config.getAttributes().getNamedItem("nodeType").getNodeValue();
            return resolver.getQName(ntString);
        }

        /**
         * Returns the node boost from the <code>config</code>.
         *
         * @param config the configuration.
         * @return the configured node boost or the default boost if none is
         *         configured.
         */
        private float getNodeBoost(Node config) {
            Node boost = config.getAttributes().getNamedItem("boost");
            if (boost != null) {
                try {
                    return Float.parseFloat(boost.getNodeValue());
                } catch (NumberFormatException e) {
                    // return default boost
                }
            }
            return DEFAULT_BOOST;
        }
    }

    /**
     * Simple class that holds boost and nodeScopeIndex flag.
     */
    private class PropertyConfig {

        /**
         * The boost value for a property.
         */
        final float boost;

        /**
         * Flag that indicates whether a property is included in the node
         * scope fulltext index of its parent.
         */
        final boolean nodeScopeIndex;

        PropertyConfig(float boost, boolean nodeScopeIndex) {
            this.boost = boost;
            this.nodeScopeIndex = nodeScopeIndex;
        }
    }

    private class PathExpression {

        static final int SELF = 0;

        static final int CHILD = 1;

        static final int ANCESTOR = 2;

        static final int PARENT = 3;

        private final int axis;

        private final Name elementTest;

        private final Name nameTest;

        private final Name propertyName;

        private final String propertyValue;

        PathExpression(int axis, Name elementTest, Name nameTest,
                       Name propertyName, String propertyValue) {
            this.axis = axis;
            this.elementTest = elementTest;
            this.nameTest = nameTest;
            this.propertyName = propertyName;
            this.propertyValue = propertyValue;
        }

        /**
         * Evaluates this expression and returns <code>true</code> if the
         * condition matches using <code>state</code> as the context node
         * state.
         *
         * @param context the context from where the expression should be
         *                evaluated.
         * @return expression result.
         */
        boolean evaluate(final NodeState context) {
            // get iterator along specified axis
            Iterator nodeStates;
            if (axis == SELF) {
                nodeStates = Collections.singletonList(context).iterator();
            } else if (axis == CHILD) {
                nodeStates = new AbstractIteratorDecorator(
                        context.getChildNodeEntries().iterator()) {
                    public Object next() {
                        NodeState.ChildNodeEntry cne =
                                (NodeState.ChildNodeEntry) super.next();
                        try {
                            return (NodeState) ism.getItemState(cne.getId());
                        } catch (ItemStateException e) {
                            throw new NoSuchElementException();
                        }
                    }
                };
            } else if (axis == ANCESTOR) {
                try {
                    nodeStates = new Iterator() {

                        private NodeState next =
                                (NodeState) ism.getItemState(context.getParentId());

                        public void remove() {
                            throw new UnsupportedOperationException();
                        }

                        public boolean hasNext() {
                            return next != null;
                        }

                        public Object next() {
                            NodeState tmp = next;
                            try {
                                if (next.getParentId() != null) {
                                    next = (NodeState) ism.getItemState(next.getParentId());
                                } else {
                                    next = null;
                                }
                            } catch (ItemStateException e) {
                                next = null;
                            }
                            return tmp;
                        }
                    };
                } catch (ItemStateException e) {
                    nodeStates = Collections.EMPTY_LIST.iterator();
                }
            } else if (axis == PARENT) {
                try {
                    if (context.getParentId() != null) {
                        NodeState state = (NodeState) ism.getItemState(context.getParentId());
                        nodeStates = Collections.singletonList(state).iterator();
                    } else {
                        nodeStates = Collections.EMPTY_LIST.iterator();
                    }
                } catch (ItemStateException e) {
                    nodeStates = Collections.EMPTY_LIST.iterator();
                }
            } else {
                // unsupported axis
                nodeStates = Collections.EMPTY_LIST.iterator();
            }

            // check node type, name and property value for each
            while (nodeStates.hasNext()) {
                try {
                    NodeState current = (NodeState) nodeStates.next();
                    if (elementTest != null &&
                            !current.getNodeTypeName().equals(elementTest)) {
                        continue;
                    }
                    if (nameTest != null &&
                            !hmgr.getName(current.getNodeId()).equals(nameTest)) {
                        continue;
                    }
                    if (!current.hasPropertyName(propertyName)) {
                        continue;
                    }
                    PropertyId propId = new PropertyId(
                            current.getNodeId(), propertyName);
                    PropertyState propState =
                            (PropertyState) ism.getItemState(propId);
                    InternalValue[] values = propState.getValues();
                    for (int i = 0; i < values.length; i++) {
                        if (values[i].toString().equals(propertyValue)) {
                            return true;
                        }
                    }
                } catch (RepositoryException e) {
                    // move on to next one
                } catch (ItemStateException e) {
                    // move on to next one
                }
            }
            return false;
        }
    }
}
