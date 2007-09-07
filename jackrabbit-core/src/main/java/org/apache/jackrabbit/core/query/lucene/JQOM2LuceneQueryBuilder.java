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

import org.apache.jackrabbit.core.query.qom.QOMTreeVisitor;
import org.apache.jackrabbit.core.query.qom.AndImpl;
import org.apache.jackrabbit.core.query.qom.BindVariableValueImpl;
import org.apache.jackrabbit.core.query.qom.ChildNodeImpl;
import org.apache.jackrabbit.core.query.qom.ChildNodeJoinConditionImpl;
import org.apache.jackrabbit.core.query.qom.ColumnImpl;
import org.apache.jackrabbit.core.query.qom.ComparisonImpl;
import org.apache.jackrabbit.core.query.qom.DescendantNodeImpl;
import org.apache.jackrabbit.core.query.qom.DescendantNodeJoinConditionImpl;
import org.apache.jackrabbit.core.query.qom.EquiJoinConditionImpl;
import org.apache.jackrabbit.core.query.qom.FullTextSearchImpl;
import org.apache.jackrabbit.core.query.qom.FullTextSearchScoreImpl;
import org.apache.jackrabbit.core.query.qom.JoinImpl;
import org.apache.jackrabbit.core.query.qom.LengthImpl;
import org.apache.jackrabbit.core.query.qom.LowerCaseImpl;
import org.apache.jackrabbit.core.query.qom.NodeLocalNameImpl;
import org.apache.jackrabbit.core.query.qom.NodeNameImpl;
import org.apache.jackrabbit.core.query.qom.NotImpl;
import org.apache.jackrabbit.core.query.qom.OrderingImpl;
import org.apache.jackrabbit.core.query.qom.OrImpl;
import org.apache.jackrabbit.core.query.qom.PropertyExistenceImpl;
import org.apache.jackrabbit.core.query.qom.PropertyValueImpl;
import org.apache.jackrabbit.core.query.qom.QueryObjectModelTree;
import org.apache.jackrabbit.core.query.qom.SameNodeImpl;
import org.apache.jackrabbit.core.query.qom.SameNodeJoinConditionImpl;
import org.apache.jackrabbit.core.query.qom.SelectorImpl;
import org.apache.jackrabbit.core.query.qom.UpperCaseImpl;
import org.apache.jackrabbit.core.query.qom.ConstraintImpl;
import org.apache.jackrabbit.core.query.qom.LiteralImpl;
import org.apache.jackrabbit.core.query.qom.StaticOperandImpl;
import org.apache.jackrabbit.core.query.qom.DynamicOperandImpl;
import org.apache.jackrabbit.core.query.qom.DefaultTraversingQOMTreeVisitor;
import org.apache.jackrabbit.core.query.PropertyTypeRegistry;
import org.apache.jackrabbit.core.query.jsr283.qom.QueryObjectModelConstants;
import org.apache.jackrabbit.core.query.lucene.fulltext.QueryParser;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.HierarchyManagerImpl;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.PathFormat;
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

/**
 * Implements a query builder that takes an JQOM and creates a lucene {@link
 * org.apache.lucene.search.Query} tree that can be executed on an index.
 */
public class JQOM2LuceneQueryBuilder implements QOMTreeVisitor {

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
     * The shared item state manager of the workspace.
     */
    private final ItemStateManager sharedItemMgr;

    /**
     * A hierarchy manager based on {@link #sharedItemMgr} to resolve paths.
     */
    private final HierarchyManager hmgr;

    /**
     * Namespace mappings to internal prefixes
     */
    private final NamespaceMappings nsMappings;

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
     * queries. Key=QName (selectorName).
     */
    private final Map selectors = new HashMap();

    /**
     * Creates a new <code>LuceneQueryBuilder</code> instance.
     *
     * @param qomTree            the root of the query object model.
     * @param session            of the user executing this query.
     * @param sharedItemMgr      the shared item state manager of the
     *                           workspace.
     * @param hmgr               a hierarchy manager based on sharedItemMgr.
     * @param nsMappings         namespace resolver for internal prefixes.
     * @param analyzer           for parsing the query statement of the contains
     *                           function.
     * @param propReg            the property type registry.
     * @param synonymProvider    the synonym provider or <code>null</code> if
     *                           node is configured.
     * @param bindVariableValues the bind variable values.
     */
    private JQOM2LuceneQueryBuilder(QueryObjectModelTree qomTree,
                                    SessionImpl session,
                                    ItemStateManager sharedItemMgr,
                                    HierarchyManager hmgr,
                                    NamespaceMappings nsMappings,
                                    Analyzer analyzer,
                                    PropertyTypeRegistry propReg,
                                    SynonymProvider synonymProvider,
                                    Map bindVariableValues) {
        this.qomTree = qomTree;
        this.session = session;
        this.sharedItemMgr = sharedItemMgr;
        this.hmgr = hmgr;
        this.nsMappings = nsMappings;
        this.analyzer = analyzer;
        this.propRegistry = propReg;
        this.synonymProvider = synonymProvider;
        this.bindVariableValues = bindVariableValues;
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
                                    Map bindVariableValues)
            throws RepositoryException {

        NodeId id = ((NodeImpl) session.getRootNode()).getNodeId();
        HierarchyManager hmgr = new HierarchyManagerImpl(
                id, sharedItemMgr, session);
        JQOM2LuceneQueryBuilder builder = new JQOM2LuceneQueryBuilder(
                qomTree, session, sharedItemMgr, hmgr, nsMappings,
                analyzer, propReg, synonymProvider, bindVariableValues);

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
            throw new InvalidQueryException("No value bound for variable " +
                    node.getBindVariableName());
        } else {
            return v;
        }
    }

    public Object visit(ChildNodeImpl node, Object data) {
        // TODO: implement
        throw new UnsupportedOperationException("not yet implemented");
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
        Value v = (Value) ((StaticOperandImpl) node.getOperand2()).accept(this, data);
        final String stringValue;
        switch (v.getType()) {
            case PropertyType.BINARY:
                throw new InvalidQueryException("Binary value not supported in comparison");
            case PropertyType.BOOLEAN:
                stringValue = v.getString();
                break;
            case PropertyType.DATE:
                stringValue = DateField.dateToString(v.getDate().getTime());
                break;
            case PropertyType.DOUBLE:
                stringValue = DoubleField.doubleToString(v.getDouble());
                break;
            case PropertyType.LONG:
                stringValue = LongField.longToString(v.getLong());
                break;
            case PropertyType.NAME:
                stringValue = nsMappings.translatePropertyName(
                        v.getString(), session.getNamespaceResolver());
                break;
            case PropertyType.PATH:
                Path p = PathFormat.parse(v.getString(), session.getNamespaceResolver());
                stringValue = PathFormat.format(p, nsMappings);
                break;
            case PropertyType.REFERENCE:
                stringValue = v.getString();
                break;
            case PropertyType.STRING:
                stringValue = v.getString();
                break;
            default:
                // TODO: support for new types defined in JSR 283
                throw new InvalidQueryException("Unsupported property type " +
                        PropertyType.nameFromValue(v.getType()));
        }

        final int operator = node.getOperator();

        return ((DynamicOperandImpl) node.getOperand1()).accept(
                new DefaultTraversingQOMTreeVisitor() {
            public Object visit(PropertyValueImpl node, Object data) throws Exception {
                String propName = NameFormat.format(node.getPropertyQName(), nsMappings);
                String text = FieldNames.createNamedValue(propName, stringValue);
                switch (operator) {
                    case QueryObjectModelConstants.OPERATOR_EQUAL_TO:
                        return new TermQuery(new Term(FieldNames.PROPERTIES, text));
                    case QueryObjectModelConstants.OPERATOR_GREATER_THAN:
                        Term lower = new Term(FieldNames.PROPERTIES, text);
                        Term upper = new Term(FieldNames.PROPERTIES,
                                FieldNames.createNamedValue(propName, "\uFFFF"));
                        return new RangeQuery(lower, upper, false);
                    case QueryObjectModelConstants.OPERATOR_GREATER_THAN_OR_EQUAL_TO:
                        lower = new Term(FieldNames.PROPERTIES, text);
                        upper = new Term(FieldNames.PROPERTIES,
                                FieldNames.createNamedValue(propName, "\uFFFF"));
                        return new RangeQuery(lower, upper, true);
                    case QueryObjectModelConstants.OPERATOR_LESS_THAN:
                        lower = new Term(FieldNames.PROPERTIES,
                                FieldNames.createNamedValue(propName, ""));
                        upper = new Term(FieldNames.PROPERTIES, text);
                        return new RangeQuery(lower, upper, false);
                    case QueryObjectModelConstants.OPERATOR_LESS_THAN_OR_EQUAL_TO:
                        lower = new Term(FieldNames.PROPERTIES,
                                FieldNames.createNamedValue(propName, ""));
                        upper = new Term(FieldNames.PROPERTIES, text);
                        return new RangeQuery(lower, upper, true);
                    case QueryObjectModelConstants.OPERATOR_LIKE:
                        if (stringValue.equals("%")) {
                            return new MatchAllQuery(propName);
                        } else {
                            return new WildcardQuery(FieldNames.PROPERTIES,
                                    propName, stringValue);
                        }
                    case QueryObjectModelConstants.OPERATOR_NOT_EQUAL_TO:
                        MatchAllQuery all = new MatchAllQuery(propName);
                        BooleanQuery b = new BooleanQuery();
                        b.add(all, BooleanClause.Occur.SHOULD);
                        b.add(new TermQuery(new Term(FieldNames.PROPERTIES, text)),
                                BooleanClause.Occur.MUST_NOT);
                        return b;
                    default:
                        throw new InvalidQueryException("Unknown operator " +
                                operator);
                }
            }

            public Object visit(LengthImpl node, Object data) throws Exception {
                // TODO: implement
                return super.visit(node, data);
            }

            public Object visit(NodeLocalNameImpl node, Object data) throws Exception {
                // TODO: implement
                throw new UnsupportedOperationException("Not yet implemented");
            }

            public Object visit(NodeNameImpl node, Object data) throws Exception {
                // TODO: implement
                throw new UnsupportedOperationException("Not yet implemented");
            }

            public Object visit(FullTextSearchScoreImpl node, Object data)
                    throws Exception {
                // TODO: implement
                throw new UnsupportedOperationException("Not yet implemented");
            }

            public Object visit(UpperCaseImpl node, Object data) throws Exception {
                Object obj = super.visit(node, data);
                if (obj instanceof Transformable) {
                    ((Transformable) obj).setTransformation(TransformConstants.TRANSFORM_UPPER_CASE);
                    return obj;
                } else if (obj instanceof TermQuery) {
                    return transformTermQuery((TermQuery) obj, true);
                } else {
                    throw new InvalidQueryException("upper-case not supported " +
                            "on operand " + node.getOperand().getClass().getName());
                }
            }

            public Object visit(LowerCaseImpl node, Object data) throws Exception {
                Object obj = super.visit(node, data);
                if (obj instanceof Transformable) {
                    ((Transformable) obj).setTransformation(TransformConstants.TRANSFORM_LOWER_CASE);
                    return obj;
                } else if (obj instanceof TermQuery) {
                    return transformTermQuery((TermQuery) obj, false);
                } else {
                    throw new InvalidQueryException("lower-case not supported " +
                            "on operand " + node.getOperand().getClass().getName());
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
                } else {
                    throw new InvalidQueryException("Upper/LowerCase not " +
                            "supported on field " + query.getTerm().field());
                }
            }
        }, data);
    }

    public Object visit(DescendantNodeImpl node, Object data) {
        // TODO: implement
        throw new UnsupportedOperationException("not yet implemented");
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
            QName propName = node.getPropertyQName();
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

    public Object visit(LengthImpl node, Object data) {
        // TODO: implement
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * @return the {@link Value} of the literal <code>node</code>.
     */
    public Object visit(LiteralImpl node, Object data) {
        return node.getValue();
    }

    public Object visit(LowerCaseImpl node, Object data) {
        // query builder should not use this method
        throw new IllegalStateException();
    }

    public Object visit(NodeLocalNameImpl node, Object data) {
        // TODO: implement
        throw new UnsupportedOperationException("not yet implemented");
    }

    public Object visit(NodeNameImpl node, Object data) {
        // TODO: implement
        throw new UnsupportedOperationException("not yet implemented");
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
        String propName = NameFormat.format(node.getPropertyQName(), nsMappings);
        return new MatchAllQuery(propName);
    }

    public Object visit(PropertyValueImpl node, Object data) {
        // query builder should not use this method
        throw new IllegalStateException();
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
        // TODO: implement
        throw new UnsupportedOperationException("not yet implemented");
    }

    public Object visit(SameNodeJoinConditionImpl node, Object data) {
        // TODO: implement
        throw new UnsupportedOperationException("not yet implemented");
    }

    public Object visit(SelectorImpl node, Object data) throws Exception {
        List terms = new ArrayList();
        String mixinTypesField = NameFormat.format(QName.JCR_MIXINTYPES, nsMappings);
        String primaryTypeField = NameFormat.format(QName.JCR_PRIMARYTYPE, nsMappings);

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
                            NameFormat.format(node.getNodeTypeQName(), nsMappings)));
            terms.add(t);
        } else {
            // search for nodes where jcr:primaryType is set to this type
            Term t = new Term(FieldNames.PROPERTIES,
                    FieldNames.createNamedValue(primaryTypeField,
                            NameFormat.format(node.getNodeTypeQName(), nsMappings)));
            terms.add(t);
        }

        // now search for all node types that are derived from base
        if (base != null) {
            NodeTypeIterator allTypes = ntMgr.getAllNodeTypes();
            while (allTypes.hasNext()) {
                NodeType nt = allTypes.nextNodeType();
                NodeType[] superTypes = nt.getSupertypes();
                if (Arrays.asList(superTypes).contains(base)) {
                    String ntName = nsMappings.translatePropertyName(nt.getName(),
                            session.getNamespaceResolver());
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
            q= new TermQuery((Term) terms.get(0));
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
        // query builder should not use this method
        throw new IllegalStateException();
    }
}
