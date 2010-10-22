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

import static javax.jcr.PropertyType.DATE;
import static javax.jcr.PropertyType.DECIMAL;
import static javax.jcr.PropertyType.DOUBLE;
import static javax.jcr.PropertyType.LONG;
import static javax.jcr.PropertyType.NAME;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.STRING;
import static javax.jcr.PropertyType.UNDEFINED;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_LIKE;
import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO;
import static org.apache.jackrabbit.core.query.lucene.FieldNames.PROPERTIES;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_PRIMARYTYPE;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.Row;
import javax.jcr.query.qom.And;
import javax.jcr.query.qom.ChildNode;
import javax.jcr.query.qom.Comparison;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DescendantNode;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.FullTextSearch;
import javax.jcr.query.qom.Not;
import javax.jcr.query.qom.Or;
import javax.jcr.query.qom.PropertyExistence;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.SameNode;
import javax.jcr.query.qom.Selector;
import javax.jcr.query.qom.StaticOperand;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.query.lucene.join.OperandEvaluator;
import org.apache.jackrabbit.core.query.lucene.join.SelectorRow;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.query.qom.FullTextSearchImpl;
import org.apache.jackrabbit.spi.commons.query.qom.PropertyExistenceImpl;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

/**
 * Factory that creates Lucene queries from QOM elements.
 */
public class LuceneQueryFactory {

    /**
     * Session of the user executing this query
     */
    private final SessionImpl session;

    /**
     * Node type manager
     */
    private final NodeTypeManager ntManager;

    /** Lucene search index */
    private final SearchIndex index;

    /**
     * Namespace mappings to internal prefixes
     */
    private final NamespaceMappings nsMappings;

    /**
     * NamePathResolver to map namespace mappings to internal prefixes
     */
    private final NamePathResolver npResolver;

    /** Operand evaluator */
    private final OperandEvaluator evaluator;

    private final String mixinTypesField;

    private final String primaryTypeField;

    /**
     * Creates a new lucene query factory.
     *
     * @param session         the session that executes the query.
     * @param index           the search index
     * @param bindVariables   the bind variable values of the query
     */
    public LuceneQueryFactory(
            SessionImpl session, SearchIndex index,
            Map<String, Value> bindVariables) throws RepositoryException {
        this.session = session;
        this.ntManager = session.getWorkspace().getNodeTypeManager();
        this.index = index;
        this.nsMappings = index.getNamespaceMappings();
        this.npResolver = NamePathResolverImpl.create(nsMappings);
        this.evaluator =
            new OperandEvaluator(session.getValueFactory(), bindVariables);
        this.mixinTypesField = nsMappings.translateName(JCR_MIXINTYPES);
        this.primaryTypeField = nsMappings.translateName(JCR_PRIMARYTYPE);
    }

    public List<Row> execute(
            Map<String, PropertyValue> columns, Selector selector,
            Constraint constraint) throws RepositoryException, IOException {
        final IndexReader reader = index.getIndexReader(true);
        try {
            JackrabbitIndexSearcher searcher = new JackrabbitIndexSearcher(
                    session, reader, index.getContext().getItemStateManager());
            searcher.setSimilarity(index.getSimilarity());

            Query query = create(selector);
            if (constraint != null) {
                Query cq = create(constraint, Collections.singletonMap(
                        selector.getSelectorName(),
                        ntManager.getNodeType(selector.getNodeTypeName())));

                BooleanQuery bq;
                if (query instanceof BooleanQuery) {
                    bq = (BooleanQuery) query;
                } else {
                    bq = new BooleanQuery();
                    bq.add(query, MUST);
                    query = bq;
                }
                bq.add(cq, MUST);
            }

            List<Row> rows = new ArrayList<Row>();
            System.out.println(query);
            QueryHits hits = searcher.evaluate(query);
            ScoreNode node = hits.nextScoreNode();
            while (node != null) {
                try {
                    rows.add(new SelectorRow(
                            columns, evaluator, selector.getSelectorName(),
                            session.getNodeById(node.getNodeId()),
                            node.getScore()));
                } catch (ItemNotFoundException e) {
                    // skip the node
                }
                node = hits.nextScoreNode();
            }
            return rows;
        } finally {
            PerQueryCache.getInstance().dispose();
            Util.closeOrRelease(reader);
        }
    }

    /**
     * Creates a lucene query for the given QOM selector.
     *
     * @param selector the selector.
     * @return a lucene query for the given selector.
     * @throws RepositoryException if an error occurs while creating the query.
     */
    public Query create(Selector selector) throws RepositoryException {
        List<Term> terms = new ArrayList<Term>();

        String name = selector.getNodeTypeName();
        NodeTypeIterator allTypes = ntManager.getAllNodeTypes();
        while (allTypes.hasNext()) {
            NodeType nt = allTypes.nextNodeType();
            if (nt.isNodeType(name)) {
                terms.add(createNodeTypeTerm(nt));
            }
        }

        if (terms.size() == 1) {
            return new JackrabbitTermQuery(terms.get(0));
        } else {
            BooleanQuery b = new BooleanQuery();
            for (Term term : terms) {
                b.add(new JackrabbitTermQuery(term), SHOULD);
            }
            return b;
        }
    }

    private Term createNodeTypeTerm(NodeType type) throws RepositoryException {
        String field;
        if (type.isMixin()) {
            // search for nodes where jcr:mixinTypes is set to this mixin
            field = mixinTypesField;
        } else {
            // search for nodes where jcr:primaryType is set to this type
            field = primaryTypeField;
        }
        String name = nsMappings.translateName(session.getQName(type.getName()));
        return new Term(PROPERTIES, FieldNames.createNamedValue(field, name));
    }

    /**
     * Creates a lucene query for the given QOM full text search.
     *
     * @param constraint the full text search constraint.
     * @return the lucene query for the given constraint.
     * @throws RepositoryException if an error occurs while creating the query.
     */
    public Query create(FullTextSearchImpl fts) throws RepositoryException {
        String fieldname;
        if (fts.getPropertyName() == null) {
            // fulltext on node
            fieldname = FieldNames.FULLTEXT;
        } else {
            // final path element is a property name
            Name propName = fts.getPropertyQName();
            StringBuffer tmp = new StringBuffer();
            tmp.append(nsMappings.getPrefix(propName.getNamespaceURI()));
            tmp.append(":").append(FieldNames.FULLTEXT_PREFIX);
            tmp.append(propName.getLocalName());
            fieldname = tmp.toString();
        }
        QueryParser parser = new JackrabbitQueryParser(
                fieldname, index.getTextAnalyzer(), index.getSynonymProvider());
        try {
            StaticOperand expr = fts.getFullTextSearchExpression();
            return parser.parse(evaluator.getValue(expr).getString());
        } catch (ParseException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Creates a lucene query for the given QOM property existence constraint.
     *
     * @param constraint the QOM constraint.
     * @return the lucene query for the given constraint.
     * @throws RepositoryException if an error occurs while creating the query.
     */
    public Query create(PropertyExistenceImpl prop) throws RepositoryException {
        String propName = npResolver.getJCRName(prop.getPropertyQName());
        return Util.createMatchAllQuery(propName, index.getIndexFormatVersion());
    }

    public Query create(
            Constraint constraint, Map<String, NodeType> selectorMap)
            throws RepositoryException {
        if (constraint instanceof And) {
            return getAndQuery((And) constraint, selectorMap);
        } else if (constraint instanceof Or) {
            return getOrQuery((Or) constraint, selectorMap);
        } else if (constraint instanceof Not) {
            return getNotQuery((Not) constraint, selectorMap);
        } else if (constraint instanceof PropertyExistence) {
            return getPropertyExistenceQuery((PropertyExistence) constraint);
        } else if (constraint instanceof Comparison) {
            return getComparisonQuery((Comparison) constraint, selectorMap);
        } else if (constraint instanceof FullTextSearch) {
            FullTextSearch fts = (FullTextSearch) constraint;
            return getFullTextSearchQuery(fts);
        } else if (constraint instanceof SameNode) {
            SameNode sn = (SameNode) constraint;
            return getNodeIdQuery(FieldNames.UUID, sn.getPath());
        } else if (constraint instanceof ChildNode) {
            ChildNode cn = (ChildNode) constraint;
            return getNodeIdQuery(FieldNames.PARENT, cn.getParentPath());
        } else if (constraint instanceof DescendantNode) {
            DescendantNode dn = (DescendantNode) constraint;
            // FIXME
            return getNodeIdQuery(FieldNames.PARENT, dn.getAncestorPath());
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Unknown constraint type: " + constraint);
        }
    }

    private Query getFullTextSearchQuery(FullTextSearch fts)
            throws RepositoryException {
        String field = FieldNames.FULLTEXT;
        String property = fts.getPropertyName();
        if (property != null) {
            Name name = npResolver.getQName(property);
            field = nsMappings.getPrefix(name.getNamespaceURI()) + ":"
                + FieldNames.FULLTEXT_PREFIX + name.getLocalName();
        }

        StaticOperand expression = fts.getFullTextSearchExpression();
        String query = evaluator.getValue(expression).getString();
        try {
            QueryParser parser = new JackrabbitQueryParser(
                    field, index.getTextAnalyzer(), index.getSynonymProvider());
            return parser.parse(query);
        } catch (ParseException e) {
            throw new RepositoryException(
                    "Invalid full text search expression: " + query, e);
        }
    }

    private BooleanQuery getAndQuery(
            And and, Map<String, NodeType> selectorMap)
            throws RepositoryException {
        BooleanQuery query = new BooleanQuery();
        addBooleanConstraint(query, and.getConstraint1(), MUST, selectorMap);
        addBooleanConstraint(query, and.getConstraint2(), MUST, selectorMap);
        return query;
    }

    private BooleanQuery getOrQuery(Or or, Map<String, NodeType> selectorMap)
            throws RepositoryException {
        BooleanQuery query = new BooleanQuery();
        addBooleanConstraint(query, or.getConstraint1(), SHOULD, selectorMap);
        addBooleanConstraint(query, or.getConstraint2(), SHOULD, selectorMap);
        return query;
    }

    private void addBooleanConstraint(
            BooleanQuery query, Constraint constraint, Occur occur,
            Map<String, NodeType> selectorMap) throws RepositoryException {
        if (occur == MUST && constraint instanceof And) {
            And and = (And) constraint;
            addBooleanConstraint(query, and.getConstraint1(), occur, selectorMap);
            addBooleanConstraint(query, and.getConstraint2(), occur, selectorMap);
        } else if (occur == SHOULD && constraint instanceof Or) {
            Or or = (Or) constraint;
            addBooleanConstraint(query, or.getConstraint1(), occur, selectorMap);
            addBooleanConstraint(query, or.getConstraint2(), occur, selectorMap);
        } else {
            query.add(create(constraint, selectorMap), occur);
        }
    }

    private NotQuery getNotQuery(Not not, Map<String, NodeType> selectorMap)
            throws RepositoryException {
        return new NotQuery(create(not.getConstraint(), selectorMap));
    }

    private Query getPropertyExistenceQuery(PropertyExistence property)
            throws RepositoryException {
        String name = npResolver.getJCRName(session.getQName(
                property.getPropertyName()));
        return Util.createMatchAllQuery(name, index.getIndexFormatVersion());
    }

    private Query getComparisonQuery(
            Comparison comparison, Map<String, NodeType> selectorMap)
            throws RepositoryException {
        DynamicOperand operand = comparison.getOperand1();
        if (operand instanceof PropertyValue) {
            PropertyValue property = (PropertyValue) operand;
            String field = npResolver.getJCRName(session.getQName(
                    property.getPropertyName()));
            int type = PropertyType.UNDEFINED;
            NodeType nt = selectorMap.get(property.getSelectorName());
            if (nt != null) {
                for (PropertyDefinition pd : nt.getPropertyDefinitions()) {
                    if (pd.getName().equals(property.getPropertyName())) {
                        type = pd.getRequiredType();
                    }
                }
            }
            return getPropertyValueQuery(
                    field, comparison.getOperator(),
                    evaluator.getValue(comparison.getOperand2()), type);
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Unknown operand type: " + operand); // FIXME
        }
    }

    private Query getNodeIdQuery(String field, String path)
            throws RepositoryException {
        String value;
        try {
            NodeImpl node = (NodeImpl) session.getNode(path);
            value = node.getNodeId().toString();
        } catch (PathNotFoundException e) {
            value = "invalid-node-id"; // can never match a node
        }
        return new JackrabbitTermQuery(new Term(field, value));
    }

    private Query getPropertyValueQuery(
            String field, String operator, Value value, int type)
            throws RepositoryException {
        Term term = getTerm(field, getValueString(value, type));
        if (JCR_OPERATOR_EQUAL_TO.equals(operator)) {
            return new JackrabbitTermQuery(term);
        } else if (JCR_OPERATOR_GREATER_THAN.equals(operator)) {
            return new RangeQuery(term, getTerm(field, "\uFFFF"), false);
        } else if (JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO.equals(operator)) {
            return new RangeQuery(term, getTerm(field, "\uFFFF"), true);
        } else if (JCR_OPERATOR_LESS_THAN.equals(operator)) {
            return new RangeQuery(getTerm(field, ""), term, false);
        } else if (JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO.equals(operator)) {
            return new RangeQuery(getTerm(field, ""), term, true);
        } else if (JCR_OPERATOR_NOT_EQUAL_TO.equals(operator)) {
            BooleanQuery or = new BooleanQuery();
            or.add(new RangeQuery(getTerm(field, ""), term, false), SHOULD);
            or.add(new RangeQuery(term, getTerm(field, "\uFFFF"), false), SHOULD);
            return or;
        } else if (JCR_OPERATOR_LIKE.equals(operator)) {
            throw new UnsupportedRepositoryOperationException(); // FIXME
        } else {
            throw new UnsupportedRepositoryOperationException(); // FIXME
        }
    }

    private Term getTerm(String field, String value) {
        return new Term(PROPERTIES, FieldNames.createNamedValue(field, value));
    }

    private String getValueString(Value value, int type)
            throws RepositoryException {
        switch (value.getType()) {
        case DATE:
            return DateField.dateToString(value.getDate().getTime());
        case DOUBLE:
            return DoubleField.doubleToString(value.getDouble());
        case LONG:
            return LongField.longToString(value.getLong());
        case DECIMAL:
            return DecimalField.decimalToString(value.getDecimal());
        case NAME:
            return npResolver.getJCRName(session.getQName(value.getString()));
        case PATH:
            return npResolver.getJCRPath(session.getQPath(value.getString()));
        default:
            String string = value.getString();
            if (type != UNDEFINED && type != STRING) {
                return getValueString(
                        session.getValueFactory().createValue(string, type),
                        UNDEFINED);
            } else {
                return string;
            }
        }
    }

}
