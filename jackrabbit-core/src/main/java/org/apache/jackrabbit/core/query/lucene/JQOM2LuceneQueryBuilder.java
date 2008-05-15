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

import org.apache.jackrabbit.core.query.PropertyTypeRegistry;
import org.apache.jackrabbit.core.query.lucene.fulltext.QueryParser;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.HierarchyManagerImpl;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.query.qom.QOMTreeVisitor;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelTree;
import org.apache.jackrabbit.spi.commons.query.qom.AndImpl;
import org.apache.jackrabbit.spi.commons.query.qom.ConstraintImpl;
import org.apache.jackrabbit.spi.commons.query.qom.BindVariableValueImpl;
import org.apache.jackrabbit.spi.commons.query.qom.ChildNodeImpl;
import org.apache.jackrabbit.spi.commons.query.qom.ChildNodeJoinConditionImpl;
import org.apache.jackrabbit.spi.commons.query.qom.ColumnImpl;
import org.apache.jackrabbit.spi.commons.query.qom.ComparisonImpl;
import org.apache.jackrabbit.spi.commons.query.qom.StaticOperandImpl;
import org.apache.jackrabbit.spi.commons.query.qom.DynamicOperandImpl;
import org.apache.jackrabbit.spi.commons.query.qom.PropertyValueImpl;
import org.apache.jackrabbit.spi.commons.query.qom.LengthImpl;
import org.apache.jackrabbit.spi.commons.query.qom.NodeLocalNameImpl;
import org.apache.jackrabbit.spi.commons.query.qom.NodeNameImpl;
import org.apache.jackrabbit.spi.commons.query.qom.FullTextSearchScoreImpl;
import org.apache.jackrabbit.spi.commons.query.qom.UpperCaseImpl;
import org.apache.jackrabbit.spi.commons.query.qom.LowerCaseImpl;
import org.apache.jackrabbit.spi.commons.query.qom.DescendantNodeImpl;
import org.apache.jackrabbit.spi.commons.query.qom.DescendantNodeJoinConditionImpl;
import org.apache.jackrabbit.spi.commons.query.qom.EquiJoinConditionImpl;
import org.apache.jackrabbit.spi.commons.query.qom.FullTextSearchImpl;
import org.apache.jackrabbit.spi.commons.query.qom.JoinImpl;
import org.apache.jackrabbit.spi.commons.query.qom.LiteralImpl;
import org.apache.jackrabbit.spi.commons.query.qom.NotImpl;
import org.apache.jackrabbit.spi.commons.query.qom.OrderingImpl;
import org.apache.jackrabbit.spi.commons.query.qom.OrImpl;
import org.apache.jackrabbit.spi.commons.query.qom.PropertyExistenceImpl;
import org.apache.jackrabbit.spi.commons.query.qom.SameNodeImpl;
import org.apache.jackrabbit.spi.commons.query.qom.SameNodeJoinConditionImpl;
import org.apache.jackrabbit.spi.commons.query.qom.SelectorImpl;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModelConstants;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.index.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.PropertyType;
import javax.jcr.PathNotFoundException;
import javax.jcr.NodeIterator;
import javax.jcr.Node;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Implements a query builder that takes an JQOM and creates a lucene {@link
 * org.apache.lucene.search.Query} tree that can be executed on an index.
 */
public class JQOM2LuceneQueryBuilder implements QOMTreeVisitor, QueryObjectModelConstants {

    /**
     * Logger for this class
     */
    private static final Logger log = LoggerFactory.getLogger(JQOM2LuceneQueryBuilder.class);

    /**
     * The root of the query object model tree.
     */
    private final QueryObjectModelTree qomTree;

    /**
     * Session of the user executing this query
     */
    private final SessionImpl session;

    /**
     * The item state manager of the workspace.
     */
    private final ItemStateManager ism;

    /**
     * A hierarchy manager based on {@link #ism} to resolve paths.
     */
    private final HierarchyManager hmgr;

    /**
     * Namespace mappings to internal prefixes
     */
    private final NamespaceMappings nsMappings;

    /**
     * NamePathResolver to map namespace mappings to internal prefixes
     */
    private final NamePathResolver npResolver;

    /**
     * The analyzer instance to use for contains function query parsing
     */
    private final Analyzer analyzer;

    /**
     * The property type registry.
     */
    private final PropertyTypeRegistry propRegistry;

    /**
     * The synonym provider or <code>null</code> if none is configured.
     */
    private final SynonymProvider synonymProvider;

    /**
     * Maps variable names to values.
     */
    private final Map bindVariableValues;

    /**
     * The selector queries that have already been translated into lucene
     * queries. Key=Name (selectorName).
     */
    private final Map selectors = new HashMap();

    /**
     * The index format version.
     */
    private final IndexFormatVersion version;

    /**
     * Creates a new <code>LuceneQueryBuilder</code> instance.
     *
     * @param qomTree            the root of the query object model.
     * @param session            of the user executing this query.
     * @param ism                the item state manager of the workspace.
     * @param hmgr               a hierarchy manager based on ism.
     * @param nsMappings         namespace resolver for internal prefixes.
     * @param analyzer           for parsing the query statement of the contains
     *                           function.
     * @param propReg            the property type registry.
     * @param synonymProvider    the synonym provider or <code>null</code> if
     *                           node is configured.
     * @param bindVariableValues the bind variable values.
     * @param version            the index format version.
     */
    private JQOM2LuceneQueryBuilder(QueryObjectModelTree qomTree,
                                    SessionImpl session,
                                    ItemStateManager ism,
                                    HierarchyManager hmgr,
                                    NamespaceMappings nsMappings,
                                    Analyzer analyzer,
                                    PropertyTypeRegistry propReg,
                                    SynonymProvider synonymProvider,
                                    Map bindVariableValues,
                                    IndexFormatVersion version) {
        this.qomTree = qomTree;
        this.session = session;
        this.ism = ism;
        this.hmgr = hmgr;
        this.nsMappings = nsMappings;
        this.npResolver = NamePathResolverImpl.create(nsMappings);
        this.analyzer = analyzer;
        this.propRegistry = propReg;
        this.synonymProvider = synonymProvider;
        this.bindVariableValues = bindVariableValues;
        this.version = version;
    }

    /**
     * Creates a lucene {@link org.apache.lucene.search.Query} tree from an
     * abstract query tree.
     *
     * @param qomTree            the root of the query object model.
     * @param session            of the user executing the query.
     * @param sharedItemMgr      the shared item state manager of the
     *                           workspace.
     * @param nsMappings         namespace resolver for internal prefixes.
     * @param analyzer           for parsing the query statement of the contains
     *                           function.
     * @param propReg            the property type registry to lookup type
     *                           information.
     * @param synonymProvider    the synonym provider or <code>null</code> if
     *                           node is configured.
     * @param bindVariableValues the bind variable values.
     * @param version            the index format version.
     * @return the lucene query tree.
     * @throws RepositoryException if an error occurs during the translation.
     */
    public static Query createQuery(QueryObjectModelTree qomTree,
                                    SessionImpl session,
                                    ItemStateManager sharedItemMgr,
                                    NamespaceMappings nsMappings,
                                    Analyzer analyzer,
                                    PropertyTypeRegistry propReg,
                                    SynonymProvider synonymProvider,
                                    Map bindVariableValues,
                                    IndexFormatVersion version)
            throws RepositoryException {

        HierarchyManager hmgr = new HierarchyManagerImpl(
                RepositoryImpl.ROOT_NODE_ID, sharedItemMgr);
        JQOM2LuceneQueryBuilder builder = new JQOM2LuceneQueryBuilder(
                qomTree, session, sharedItemMgr, hmgr, nsMappings, analyzer,
                propReg, synonymProvider, bindVariableValues, version);

        return builder.createLuceneQuery();
    }

    private Query createLuceneQuery() throws InvalidQueryException {
        try {
            return (Query) qomTree.accept(this, null);
        } catch (InvalidQueryException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidQueryException(e.getMessage(), e);
        }
    }

    //----------------------------< QOMTreeVisitor >----------------------------

    public Object visit(AndImpl node, Object data) throws Exception {
        BooleanQuery b = new BooleanQuery();
        b.add((Query) ((ConstraintImpl) node.getConstraint1()).accept(this, data),
                BooleanClause.Occur.MUST);
        b.add((Query) ((ConstraintImpl) node.getConstraint2()).accept(this, data),
                BooleanClause.Occur.MUST);
        return b;
    }

    /**
     * @return the {@link Value} for the passed bind variable value node.
     * @throws InvalidQueryException if there is no value bound for the passed
     *                               bind variable.
     */
    public Object visit(BindVariableValueImpl node, Object data)
            throws InvalidQueryException {
        Value v = (Value) bindVariableValues.get(node.getBindVariableQName());
        if (v == null) {
            throw new InvalidQueryException("No value bound for variable "
                    + node.getBindVariableName());
        } else {
            return v;
        }
    }

    public Object visit(ChildNodeImpl node, Object data) {
        Name ntName = qomTree.getSelector(node.getSelectorQName()).getNodeTypeQName();
        List scoreNodes = new ArrayList();
        try {
            Node parent = session.getNode(node.getPath());
            for (NodeIterator it = parent.getNodes(); it.hasNext(); ) {
                NodeImpl n = (NodeImpl) it.nextNode();
                if (n.isNodeType(ntName)) {
                    scoreNodes.add(new ScoreNode(n.getNodeId(), 1.0f));
                }
            }
            return new QueryHitsQuery(new DefaultQueryHits(scoreNodes));
        } catch (PathNotFoundException e) {
            // node does not exist
        } catch (RepositoryException e) {
            log.warn("Exception while constructing query: " + e);
            log.debug("Stacktrace: ", e);
        }
        // return a dummy query, which does not match any nodes
        return new BooleanQuery();
    }

    public Object visit(ChildNodeJoinConditionImpl node, Object data) {
        // TODO: implement
        throw new UnsupportedOperationException("not yet implemented");
    }

    public Object visit(ColumnImpl node, Object data) {
        // query builder should not use this method
        throw new IllegalStateException();
    }

    public Object visit(ComparisonImpl node, Object data) throws Exception {
        return ((DynamicOperandImpl) node.getOperand1()).accept(this, node);
    }

    public Object visit(DescendantNodeImpl node, Object data) throws Exception {
        // TODO simplify, is there a way to aggregate constraints for the same selector?
        Query selectorQuery = (Query) qomTree.getSelector(node.getSelectorQName()).accept(this, null);
        try {
            NodeImpl n = (NodeImpl) session.getNode(node.getPath());
            ScoreNode sn = new ScoreNode(n.getNodeId(), 1.0f);
            Query context = new QueryHitsQuery(new DefaultQueryHits(
                    Collections.singletonList(sn)));
            return new DescendantSelfAxisQuery(context, selectorQuery, false);
        } catch (PathNotFoundException e) {
            // node does not exist
        } catch (RepositoryException e) {
            log.warn("Exception while constructing query: " + e);
            log.debug("Stacktrace: ", e);
        }
        // return a dummy query, which does not match any nodes
        return new BooleanQuery();
    }

    public Object visit(DescendantNodeJoinConditionImpl node, Object data) {
        // TODO: implement
        throw new UnsupportedOperationException("not yet implemented");
    }

    public Object visit(EquiJoinConditionImpl node, Object data) {
        // TODO: implement
        throw new UnsupportedOperationException("not yet implemented");
    }

    public Object visit(FullTextSearchImpl node, Object data) throws Exception {
        String fieldname;
        if (node.getPropertyName() == null) {
            // fulltext on node
            fieldname = FieldNames.FULLTEXT;
        } else {
            // final path element is a property name
            Name propName = node.getPropertyQName();
            StringBuffer tmp = new StringBuffer();
            tmp.append(nsMappings.getPrefix(propName.getNamespaceURI()));
            tmp.append(":").append(FieldNames.FULLTEXT_PREFIX);
            tmp.append(propName.getLocalName());
            fieldname = tmp.toString();
        }
        QueryParser parser = new QueryParser(
                fieldname, analyzer, synonymProvider);
        parser.setOperator(QueryParser.DEFAULT_OPERATOR_AND);
        // replace escaped ' with just '
        StringBuffer query = new StringBuffer();
        String textsearch = node.getFullTextSearchExpression();
        // the default lucene query parser recognizes 'AND' and 'NOT' as
        // keywords.
        textsearch = textsearch.replaceAll("AND", "and");
        textsearch = textsearch.replaceAll("NOT", "not");
        boolean escaped = false;
        for (int i = 0; i < textsearch.length(); i++) {
            if (textsearch.charAt(i) == '\\') {
                if (escaped) {
                    query.append("\\\\");
                    escaped = false;
                } else {
                    escaped = true;
                }
            } else if (textsearch.charAt(i) == '\'') {
                if (escaped) {
                    escaped = false;
                }
                query.append(textsearch.charAt(i));
            } else {
                if (escaped) {
                    query.append('\\');
                    escaped = false;
                }
                query.append(textsearch.charAt(i));
            }
        }
        return parser.parse(query.toString());
    }

    public Object visit(FullTextSearchScoreImpl node, Object data) {
        // TODO: implement
        throw new UnsupportedOperationException("not yet implemented");
    }

    public Object visit(JoinImpl node, Object data) {
        // TODO: implement
        throw new UnsupportedOperationException("not yet implemented");
    }

    public Object visit(LengthImpl node, Object data) throws Exception {
        if (version.getVersion() < IndexFormatVersion.V3.getVersion()) {
            throw new InvalidQueryException("Length operator is only " +
                    "available with index version >= 3. Please re-index " +
                    "repository and execute query again.");
        }
        PropertyValueImpl pv = (PropertyValueImpl) node.getPropertyValue();
        String propName = npResolver.getJCRName(pv.getPropertyQName());
        if (data instanceof ComparisonImpl) {
            ComparisonImpl comp = (ComparisonImpl) data;
            int operator = comp.getOperator();
            Value v = (Value) ((StaticOperandImpl) comp.getOperand2()).accept(this, data);
            String namedLength = FieldNames.createNamedLength(propName, v.getLong());

            switch (operator) {
                case OPERATOR_EQUAL_TO:
                    return new TermQuery(new Term(FieldNames.PROPERTY_LENGTHS, namedLength));
                case OPERATOR_GREATER_THAN:
                    Term lower = new Term(FieldNames.PROPERTY_LENGTHS, namedLength);
                    Term upper = new Term(FieldNames.PROPERTY_LENGTHS,
                            FieldNames.createNamedLength(propName, Long.MAX_VALUE));
                    return new RangeQuery(lower, upper, false);
                case OPERATOR_GREATER_THAN_OR_EQUAL_TO:
                    lower = new Term(FieldNames.PROPERTY_LENGTHS, namedLength);
                    upper = new Term(FieldNames.PROPERTY_LENGTHS,
                            FieldNames.createNamedLength(propName, Long.MAX_VALUE));
                    return new RangeQuery(lower, upper, true);
                case OPERATOR_LESS_THAN:
                    lower = new Term(FieldNames.PROPERTY_LENGTHS,
                            FieldNames.createNamedLength(propName, -1));
                    upper = new Term(FieldNames.PROPERTY_LENGTHS, namedLength);
                    return new RangeQuery(lower, upper, false);
                case OPERATOR_LESS_THAN_OR_EQUAL_TO:
                    lower = new Term(FieldNames.PROPERTY_LENGTHS,
                            FieldNames.createNamedLength(propName, -1));
                    upper = new Term(FieldNames.PROPERTY_LENGTHS, namedLength);
                    return new RangeQuery(lower, upper, true);
                case OPERATOR_LIKE:
                    throw new InvalidQueryException("Like operator cannot be used with length operand");
                case OPERATOR_NOT_EQUAL_TO:
                    Query all = Util.createMatchAllQuery(propName, version);
                    BooleanQuery b = new BooleanQuery();
                    b.add(all, BooleanClause.Occur.SHOULD);
                    b.add(new TermQuery(new Term(FieldNames.PROPERTY_LENGTHS, namedLength)),
                            BooleanClause.Occur.MUST_NOT);
                    return b;
                default:
                    throw new InvalidQueryException(
                            "Unknown operator " + operator);
            }
        } else {
            throw new UnsupportedOperationException("not yet implemented");
        }
    }

    /**
     * @return the {@link Value} of the literal <code>node</code>.
     */
    public Object visit(LiteralImpl node, Object data) {
        return node.getValue();
    }

    public Object visit(LowerCaseImpl node, Object data) throws Exception {
        Object obj = ((DynamicOperandImpl) node.getOperand()).accept(this, data);
        return transformCase(obj, data, false);
    }

    public Object visit(NodeLocalNameImpl node, Object data) throws Exception {
        if (version.getVersion() < IndexFormatVersion.V3.getVersion()) {
            throw new InvalidQueryException("NodeLocalName operand is only " +
                    "available with index version >= 3. Please re-index " +
                    "repository and execute query again.");
        }
        if (data instanceof ComparisonImpl) {
            ComparisonImpl comp = ((ComparisonImpl) data);
            int operator = comp.getOperator();
            Value v = (Value) ((StaticOperandImpl) comp.getOperand2()).accept(this, data);
            String value = v.getString();

            switch (operator) {
                case OPERATOR_EQUAL_TO:
                    return new TermQuery(new Term(FieldNames.LOCAL_NAME, value));
                case OPERATOR_GREATER_THAN:
                    return new LocalNameRangeQuery(value, null, false);
                case OPERATOR_GREATER_THAN_OR_EQUAL_TO:
                    return new LocalNameRangeQuery(value, null, true);
                case OPERATOR_LESS_THAN:
                    return new LocalNameRangeQuery(null, value, false);
                case OPERATOR_LESS_THAN_OR_EQUAL_TO:
                    return new LocalNameRangeQuery(null, value, true);
                case OPERATOR_LIKE:
                    if (value.equals("%")) {
                        return new MatchAllDocsQuery();
                    } else {
                        return new WildcardQuery(FieldNames.LOCAL_NAME, null, value);
                    }
                case OPERATOR_NOT_EQUAL_TO:
                    MatchAllDocsQuery all = new MatchAllDocsQuery();
                    BooleanQuery b = new BooleanQuery();
                    b.add(all, BooleanClause.Occur.SHOULD);
                    b.add(new TermQuery(new Term(FieldNames.LOCAL_NAME, value)),
                            BooleanClause.Occur.MUST_NOT);
                    return b;
                default:
                    throw new InvalidQueryException(
                            "Unknown operator " + operator);
            }
        } else {
            // TODO
            throw new InvalidQueryException("not yet implemented");
        }
    }

    public Object visit(NodeNameImpl node, Object data) throws Exception {
        if (data instanceof ComparisonImpl) {
            ComparisonImpl comp = ((ComparisonImpl) data);
            int operator = comp.getOperator();
            Value v = (Value) ((StaticOperandImpl) comp.getOperand2()).accept(this, data);
            switch (v.getType()) {
                case PropertyType.DATE:
                case PropertyType.DOUBLE:
                // TODO case PropertyType.DECIMAL:
                case PropertyType.LONG:
                case PropertyType.BOOLEAN:
                case PropertyType.REFERENCE:
                // TODO case PropertyType.WEAKREFERENCE:
                // TODO case PropertyType.URI
                    throw new InvalidQueryException(v.getString() +
                            " cannot be converted into a NAME value");
            }

            Name value;
            try {
                value = JQOM2LuceneQueryBuilder.this.session.getQName(v.getString());
            } catch (RepositoryException e) {
                throw new InvalidQueryException(v.getString() +
                        " cannot be converted into a NAME value");
            }

            switch (operator) {
                case OPERATOR_EQUAL_TO:
                    return new NameQuery(value, version, nsMappings);
                case OPERATOR_GREATER_THAN:
                    return new NameRangeQuery(value, null, false, version, nsMappings);
                case OPERATOR_GREATER_THAN_OR_EQUAL_TO:
                    return new NameRangeQuery(value, null, true, version, nsMappings);
                case OPERATOR_LESS_THAN:
                    return new NameRangeQuery(null, value, false, version, nsMappings);
                case OPERATOR_LESS_THAN_OR_EQUAL_TO:
                    return new NameRangeQuery(null, value, true, version, nsMappings);
                case OPERATOR_LIKE:
                    throw new InvalidQueryException("Operator LIKE is not supported with NAME operands");
                case OPERATOR_NOT_EQUAL_TO:
                    MatchAllDocsQuery all = new MatchAllDocsQuery();
                    BooleanQuery b = new BooleanQuery();
                    b.add(all, BooleanClause.Occur.SHOULD);
                    b.add(new NameQuery(value, version, nsMappings),
                            BooleanClause.Occur.MUST_NOT);
                    return b;
                default:
                    throw new InvalidQueryException(
                            "Unknown operator " + operator);
            }
        } else {
            // TODO
            throw new InvalidQueryException("not yet implemented");
        }
    }

    public Object visit(NotImpl node, Object data) throws Exception {
        Query c = (Query) ((ConstraintImpl) node.getConstraint()).accept(this, data);
        return new NotQuery(c);
    }

    public Object visit(OrderingImpl node, Object data) {
        // query builder should not use this method
        throw new IllegalStateException();
    }

    public Object visit(OrImpl node, Object data) throws Exception {
        BooleanQuery b = new BooleanQuery();
        b.add((Query) ((ConstraintImpl) node.getConstraint1()).accept(this, data),
                BooleanClause.Occur.SHOULD);
        b.add((Query) ((ConstraintImpl) node.getConstraint2()).accept(this, data),
                BooleanClause.Occur.SHOULD);
        return b;
    }

    public Object visit(PropertyExistenceImpl node, Object data) throws Exception {
        String propName = npResolver.getJCRName(node.getPropertyQName());
        return Util.createMatchAllQuery(propName, version);
    }

    public Object visit(PropertyValueImpl node, Object data) throws Exception {
        if (data instanceof ComparisonImpl) {
            ComparisonImpl comp = ((ComparisonImpl) data);
            int operator = comp.getOperator();
            Value v = (Value) ((StaticOperandImpl) comp.getOperand2()).accept(this, data);
            String stringValue = stringValueOf(v);
            String propName = npResolver.getJCRName(node.getPropertyQName());
            String text = FieldNames.createNamedValue(propName, stringValue);
            switch (operator) {
                case OPERATOR_EQUAL_TO:
                    return new TermQuery(new Term(FieldNames.PROPERTIES, text));
                case OPERATOR_GREATER_THAN:
                    Term lower = new Term(FieldNames.PROPERTIES, text);
                    Term upper = new Term(FieldNames.PROPERTIES,
                            FieldNames.createNamedValue(propName, "\uFFFF"));
                    return new RangeQuery(lower, upper, false);
                case OPERATOR_GREATER_THAN_OR_EQUAL_TO:
                    lower = new Term(FieldNames.PROPERTIES, text);
                    upper = new Term(FieldNames.PROPERTIES,
                            FieldNames.createNamedValue(propName, "\uFFFF"));
                    return new RangeQuery(lower, upper, true);
                case OPERATOR_LESS_THAN:
                    lower = new Term(FieldNames.PROPERTIES,
                            FieldNames.createNamedValue(propName, ""));
                    upper = new Term(FieldNames.PROPERTIES, text);
                    return new RangeQuery(lower, upper, false);
                case OPERATOR_LESS_THAN_OR_EQUAL_TO:
                    lower = new Term(FieldNames.PROPERTIES,
                            FieldNames.createNamedValue(propName, ""));
                    upper = new Term(FieldNames.PROPERTIES, text);
                    return new RangeQuery(lower, upper, true);
                case OPERATOR_LIKE:
                    if (stringValue.equals("%")) {
                        return Util.createMatchAllQuery(propName, version);
                    } else {
                        return new WildcardQuery(FieldNames.PROPERTIES,
                                propName, stringValue);
                    }
                case OPERATOR_NOT_EQUAL_TO:
                    Query all = Util.createMatchAllQuery(propName, version);
                    BooleanQuery b = new BooleanQuery();
                    b.add(all, BooleanClause.Occur.SHOULD);
                    b.add(new TermQuery(new Term(FieldNames.PROPERTIES, text)),
                            BooleanClause.Occur.MUST_NOT);
                    return b;
                default:
                    throw new InvalidQueryException(
                            "Unknown operator " + operator);
            }
        } else {
            // TODO
            throw new InvalidQueryException("not yet implemented");
        }
    }

    public Object visit(QueryObjectModelTree node, Object data)
            throws Exception {
        Query source = (Query) node.getSource().accept(this, data);
        if (node.getConstraint() == null) {
            return source;
        } else {
            Query constraint = (Query) node.getConstraint().accept(this, data);
            BooleanQuery b = new BooleanQuery();
            b.add(source, BooleanClause.Occur.MUST);
            b.add(constraint, BooleanClause.Occur.MUST);
            return b;
        }
    }

    public Object visit(SameNodeImpl node, Object data) {
        Name ntName = qomTree.getSelector(node.getSelectorQName()).getNodeTypeQName();
        try {
            NodeImpl n = (NodeImpl) session.getNode(node.getPath());
            if (n.isNodeType(ntName)) {
                ScoreNode sn = new ScoreNode(n.getNodeId(), 1.0f);
                return new QueryHitsQuery(new DefaultQueryHits(
                        Collections.singletonList(sn)));
            }
        } catch (PathNotFoundException e) {
            // node does not exist
        } catch (RepositoryException e) {
            log.warn("Exception while constructing query: " + e);
            log.debug("Stacktrace: ", e);
        }
        // return a dummy query, which does not match any nodes
        return new BooleanQuery();
    }

    public Object visit(SameNodeJoinConditionImpl node, Object data) {
        // TODO: implement
        throw new UnsupportedOperationException("not yet implemented");
    }

    public Object visit(SelectorImpl node, Object data) throws Exception {
        List terms = new ArrayList();
        String mixinTypesField = npResolver.getJCRName(NameConstants.JCR_MIXINTYPES);
        String primaryTypeField = npResolver.getJCRName(NameConstants.JCR_PRIMARYTYPE);

        NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
        NodeType base = null;
        try {
            base = ntMgr.getNodeType(session.getJCRName(node.getNodeTypeQName()));
        } catch (RepositoryException e) {
            // node type does not exist
        }

        if (base != null && base.isMixin()) {
            // search for nodes where jcr:mixinTypes is set to this mixin
            Term t = new Term(FieldNames.PROPERTIES,
                    FieldNames.createNamedValue(mixinTypesField,
                            npResolver.getJCRName(node.getNodeTypeQName())));
            terms.add(t);
        } else {
            // search for nodes where jcr:primaryType is set to this type
            Term t = new Term(FieldNames.PROPERTIES,
                    FieldNames.createNamedValue(primaryTypeField,
                            npResolver.getJCRName(node.getNodeTypeQName())));
            terms.add(t);
        }

        // now search for all node types that are derived from base
        if (base != null) {
            NodeTypeIterator allTypes = ntMgr.getAllNodeTypes();
            while (allTypes.hasNext()) {
                NodeType nt = allTypes.nextNodeType();
                NodeType[] superTypes = nt.getSupertypes();
                if (Arrays.asList(superTypes).contains(base)) {
                    Name n = session.getQName(nt.getName());
                    String ntName = nsMappings.translatePropertyName(n);
                    Term t;
                    if (nt.isMixin()) {
                        // search on jcr:mixinTypes
                        t = new Term(FieldNames.PROPERTIES,
                                FieldNames.createNamedValue(mixinTypesField, ntName));
                    } else {
                        // search on jcr:primaryType
                        t = new Term(FieldNames.PROPERTIES,
                                FieldNames.createNamedValue(primaryTypeField, ntName));
                    }
                    terms.add(t);
                }
            }
        }
        Query q;
        if (terms.size() == 1) {
            q = new TermQuery((Term) terms.get(0));
        } else {
            BooleanQuery b = new BooleanQuery();
            for (Iterator it = terms.iterator(); it.hasNext();) {
                b.add(new TermQuery((Term) it.next()), BooleanClause.Occur.SHOULD);
            }
            q = b;
        }
        selectors.put(node.getSelectorQName(), q);
        return q;
    }

    public Object visit(UpperCaseImpl node, Object data) throws Exception {
        Object obj = ((DynamicOperandImpl) node.getOperand()).accept(this, data);
        return transformCase(obj, data, true);
    }

    //------------------------------< internal >--------------------------------

    private String stringValueOf(Value value) throws RepositoryException {
        switch (value.getType()) {
            case PropertyType.BINARY:
                return value.getString();
            case PropertyType.BOOLEAN:
                return value.getString();
            case PropertyType.DATE:
                return DateField.dateToString(value.getDate().getTime());
            case PropertyType.DOUBLE:
                return DoubleField.doubleToString(value.getDouble());
            case PropertyType.LONG:
                return LongField.longToString(value.getLong());
            case PropertyType.NAME:
                Name n = session.getQName(value.getString());
                return nsMappings.translatePropertyName(n);
            case PropertyType.PATH:
                Path p = session.getQPath(value.getString());
                return npResolver.getJCRPath(p);
            case PropertyType.REFERENCE:
                return value.getString();
            case PropertyType.STRING:
                return value.getString();
            default:
                // TODO: support for new types defined in JSR 283
                throw new InvalidQueryException("Unsupported property type "
                        + PropertyType.nameFromValue(value.getType()));
        }
    }

    private Query transformTermQuery(TermQuery query, boolean toUpper)
            throws InvalidQueryException {
        if (query.getTerm().field() == FieldNames.PROPERTIES) {
            if (toUpper) {
                return new CaseTermQuery.Upper(query.getTerm());
            } else {
                return new CaseTermQuery.Lower(query.getTerm());
            }
        } else if (query.getTerm().field() == FieldNames.PROPERTIES_SET) {
            return query;
        } else {
            throw new InvalidQueryException(
                    "Upper/LowerCase not supported on field "
                    + query.getTerm().field());
        }
    }

    private Object transformCase(Object operand, Object data, boolean toUpperCase)
            throws InvalidQueryException, Exception {
        if (operand instanceof Transformable) {
            ((Transformable) operand).setTransformation(toUpperCase ?
                    TransformConstants.TRANSFORM_UPPER_CASE :
                    TransformConstants.TRANSFORM_LOWER_CASE);
            return operand;
        } else if (operand instanceof TermQuery) {
            return transformTermQuery((TermQuery) operand, toUpperCase);
        } else if (operand instanceof LowerCaseImpl) {
            LowerCaseImpl lc = (LowerCaseImpl) operand;
            if (toUpperCase) {
                // upper-case operand, ignore lower-case
                return transformCase(lc.getOperand(), data, true);
            } else {
                // lower-cased twice
                return ((DynamicOperandImpl) lc.getOperand()).accept(this, data);
            }
        } else if (operand instanceof UpperCaseImpl) {
            UpperCaseImpl oc = (UpperCaseImpl) operand;
            if (toUpperCase) {
                // upper-cased twice
                return ((DynamicOperandImpl) oc.getOperand()).accept(this, data);
            } else {
                // lower-case operand, ignore upper-case
                return transformCase(oc.getOperand(), data, false);
            }
        } else if (operand instanceof CaseTermQuery) {
            CaseTermQuery ctq = (CaseTermQuery) operand;
            return transformTermQuery(new TermQuery(ctq.getTerm()), toUpperCase);
        } else if (operand instanceof MatchAllQuery) {
            return operand;
        } else if (operand instanceof BooleanQuery) {
            BooleanQuery original = (BooleanQuery) operand;
            BooleanQuery transformed = new BooleanQuery();
            BooleanClause[] clauses = original.getClauses();
            for (int i = 0; i < clauses.length; i++) {
                Query q = (Query) transformCase(clauses[i].getQuery(),
                        data, toUpperCase);
                transformed.add(q, clauses[i].getOccur());
            }
            return transformed;
        } else {
            throw new InvalidQueryException(
                    "lower/upper-case not supported on operand "
                    + operand.getClass().getName());
        }
    }
}
