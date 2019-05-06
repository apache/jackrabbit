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
import static org.apache.jackrabbit.core.query.lucene.FieldNames.LOCAL_NAME;
import static org.apache.jackrabbit.core.query.lucene.FieldNames.MVP;
import static org.apache.jackrabbit.core.query.lucene.FieldNames.NAMESPACE_URI;
import static org.apache.jackrabbit.core.query.lucene.FieldNames.PARENT;
import static org.apache.jackrabbit.core.query.lucene.FieldNames.PROPERTIES;
import static org.apache.jackrabbit.core.query.lucene.FieldNames.UUID;
import static org.apache.jackrabbit.core.query.lucene.TransformConstants.TRANSFORM_LOWER_CASE;
import static org.apache.jackrabbit.core.query.lucene.TransformConstants.TRANSFORM_NONE;
import static org.apache.jackrabbit.core.query.lucene.TransformConstants.TRANSFORM_UPPER_CASE;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_PRIMARYTYPE;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.MUST_NOT;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Row;
import javax.jcr.query.qom.And;
import javax.jcr.query.qom.ChildNode;
import javax.jcr.query.qom.Comparison;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DescendantNode;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.FullTextSearch;
import javax.jcr.query.qom.FullTextSearchScore;
import javax.jcr.query.qom.Length;
import javax.jcr.query.qom.LowerCase;
import javax.jcr.query.qom.NodeLocalName;
import javax.jcr.query.qom.NodeName;
import javax.jcr.query.qom.Not;
import javax.jcr.query.qom.Or;
import javax.jcr.query.qom.PropertyExistence;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.SameNode;
import javax.jcr.query.qom.Selector;
import javax.jcr.query.qom.StaticOperand;
import javax.jcr.query.qom.UpperCase;

import org.apache.jackrabbit.commons.predicate.Predicate;
import org.apache.jackrabbit.commons.predicate.Predicates;
import org.apache.jackrabbit.commons.predicate.RowPredicate;
import org.apache.jackrabbit.commons.query.qom.OperandEvaluator;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.query.lucene.join.SelectorRow;
import org.apache.jackrabbit.core.query.lucene.join.ValueComparator;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
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
import org.apache.lucene.search.Sort;

/**
 * Factory that creates Lucene queries from QOM elements.
 */
public class LuceneQueryFactory {

    /**
     * Session of the user executing this query
     */
    protected final SessionImpl session;

    /**
     * Node type manager
     */
    protected final NodeTypeManager ntManager;

    /** Lucene search index */
    protected final SearchIndex index;

    /**
     * Namespace mappings to internal prefixes
     */
    protected final NamespaceMappings nsMappings;

    /**
     * NamePathResolver to map namespace mappings to internal prefixes
     */
    protected final NamePathResolver npResolver;

    /** Operand evaluator */
    protected final OperandEvaluator evaluator;

    protected final String mixinTypesField;

    protected final String primaryTypeField;

    private final PerQueryCache cache = new PerQueryCache();

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

    /**
     * @param columns
     * @param selector
     * @param constraint
     * @param externalSort
     *            if <code>true</code> it means that the lqf should just let the
     *            QueryEngine take care of sorting and applying applying offset
     *            and limit constraints
     * @param offsetIn
     *            used in pagination
     * @param limitIn
     *            used in pagination
     * @return a list of rows
     * @throws RepositoryException
     * @throws IOException
     */
    public List<Row> execute(Map<String, PropertyValue> columns,
            Selector selector, Constraint constraint, Sort sort,
            boolean externalSort, long offsetIn, long limitIn)
            throws RepositoryException, IOException {
        final IndexReader reader = index.getIndexReader(true);
        final int offset = offsetIn < 0 ? 0 : (int) offsetIn;
        final int limit = limitIn < 0 ? Integer.MAX_VALUE : (int) limitIn;

        QueryHits hits = null;
        try {
            JackrabbitIndexSearcher searcher = new JackrabbitIndexSearcher(
                    session, reader, index.getContext().getItemStateManager());
            searcher.setSimilarity(index.getSimilarity());

            Predicate filter = Predicate.TRUE;
            BooleanQuery query = new BooleanQuery();

            QueryPair qp = new QueryPair(query);

            query.add(create(selector), MUST);
            if (constraint != null) {
                String name = selector.getSelectorName();
                NodeType type =
                    ntManager.getNodeType(selector.getNodeTypeName());
                filter = mapConstraintToQueryAndFilter(qp,
                        constraint, Collections.singletonMap(name, type),
                        searcher, reader);
            }

            List<Row> rows = new ArrayList<Row>();

            // TODO depending on the filters, we could push the offset info
            // into the searcher
            hits = searcher.evaluate(qp.mainQuery, sort, offset + limit);
            int currentNode = 0;
            int addedNodes = 0;

            ScoreNode node = hits.nextScoreNode();
            while (node != null) {
                Row row = null;
                try {
                    row = new SelectorRow(columns, evaluator,
                            selector.getSelectorName(),
                            session.getNodeById(node.getNodeId()),
                            node.getScore());
                } catch (ItemNotFoundException e) {
                    // skip the node
                }
                if (row != null && filter.evaluate(row)) {
                    if (externalSort) {
                        // return everything and not worry about sort
                        rows.add(row);
                    } else {
                        // apply limit and offset rules locally
                        if (currentNode >= offset
                                && currentNode - offset < limit) {
                            rows.add(row);
                            addedNodes++;
                        }
                        currentNode++;
                        // end the loop when going over the limit
                        if (addedNodes == limit) {
                            break;
                        }
                    }
                }
                node = hits.nextScoreNode();
            }
            return rows;
        } finally {
            if (hits != null) {
                hits.close();
            }
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

    protected Term createNodeTypeTerm(NodeType type) throws RepositoryException {
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
     * @param fts the full text search constraint.
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
                fieldname, index.getTextAnalyzer(),
                index.getSynonymProvider(), cache);
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
     * @param prop the QOM constraint.
     * @return the lucene query for the given constraint.
     * @throws RepositoryException if an error occurs while creating the query.
     */
    public Query create(PropertyExistenceImpl prop) throws RepositoryException {
        String propName = npResolver.getJCRName(prop.getPropertyQName());
        return Util.createMatchAllQuery(
                propName, index.getIndexFormatVersion(), cache);
    }

    protected Predicate mapConstraintToQueryAndFilter(
            QueryPair query, Constraint constraint,
            Map<String, NodeType> selectorMap,
            JackrabbitIndexSearcher searcher, IndexReader reader)
            throws RepositoryException, IOException {
        Predicate filter = Predicate.TRUE;
        if (constraint instanceof And) {
            And and = (And) constraint;
            filter = mapConstraintToQueryAndFilter(
                    query, and.getConstraint1(), selectorMap, searcher, reader);
            Predicate other = mapConstraintToQueryAndFilter(
                    query, and.getConstraint2(), selectorMap, searcher, reader);
            if (filter == Predicate.TRUE) {
                filter = other;
            } else if (other != Predicate.TRUE) {
                filter = Predicates.and(filter, other);
            }
        } else if (constraint instanceof Comparison) {
            Comparison c = (Comparison) constraint;
            Transform transform = new Transform(c.getOperand1());
            DynamicOperand left = transform.operand;
            final String operator = c.getOperator();
            StaticOperand right = c.getOperand2();
            if (left instanceof Length
                    || left instanceof FullTextSearchScore
                    || (((!JCR_OPERATOR_EQUAL_TO.equals(operator) && !JCR_OPERATOR_LIKE
                            .equals(operator)) || transform.transform != TRANSFORM_NONE) && (left instanceof NodeName || left instanceof NodeLocalName))) {
                try {
                    int type = PropertyType.UNDEFINED;
                    if (left instanceof Length) {
                        type = PropertyType.LONG;
                    } else if (left instanceof FullTextSearchScore) {
                        type = PropertyType.DOUBLE;
                    }
                    final DynamicOperand operand = c.getOperand1();
                    final Value value = evaluator.getValue(right, type);
                    filter = new RowPredicate() {
                        @Override
                        protected boolean evaluate(Row row)
                                throws RepositoryException {
                            return new ValueComparator().evaluate(
                                    operator,
                                    evaluator.getValue(operand, row), value);
                        }
                    };
                } catch (ValueFormatException e) {
                    throw new InvalidQueryException(e);
                }
            } else {
                Query cq = getComparisonQuery(
                        left, transform.transform, operator, right, selectorMap);
                query.subQuery.add(cq, MUST);
            }
        } else if (constraint instanceof DescendantNode) {
            final DescendantNode descendantNode = (DescendantNode) constraint;
            Query context = getNodeIdQuery(UUID, descendantNode.getAncestorPath());
            query.mainQuery = new DescendantSelfAxisQuery(context, query.subQuery, false);
        } else {
            query.subQuery.add(create(constraint, selectorMap, searcher), MUST);
        }
        return filter;
    }


    protected Query create(
            Constraint constraint, Map<String, NodeType> selectorMap,
            JackrabbitIndexSearcher searcher)
            throws RepositoryException, IOException {
        if (constraint instanceof And) {
            return getAndQuery((And) constraint, selectorMap, searcher);
        } else if (constraint instanceof Or) {
            return getOrQuery((Or) constraint, selectorMap, searcher);
        } else if (constraint instanceof Not) {
            return getNotQuery((Not) constraint, selectorMap, searcher);
        } else if (constraint instanceof PropertyExistence) {
            return getPropertyExistenceQuery((PropertyExistence) constraint);
        } else if (constraint instanceof Comparison) {
            Comparison c = (Comparison) constraint;
            Transform left = new Transform(c.getOperand1());
            return getComparisonQuery(
                    left.operand, left.transform, c.getOperator(),
                    c.getOperand2(), selectorMap);
        } else if (constraint instanceof FullTextSearch) {
            return getFullTextSearchQuery((FullTextSearch) constraint);
        } else if (constraint instanceof SameNode) {
            SameNode sn = (SameNode) constraint;
            return getNodeIdQuery(UUID, sn.getPath());
        } else if (constraint instanceof ChildNode) {
            ChildNode cn = (ChildNode) constraint;
            return getNodeIdQuery(PARENT, cn.getParentPath());
        } else if (constraint instanceof DescendantNode) {
            DescendantNode dn = (DescendantNode) constraint;
            return getDescendantNodeQuery(dn, searcher);
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Unknown constraint type: " + constraint);
        }
    }

    protected Query getDescendantNodeQuery(
            DescendantNode dn, JackrabbitIndexSearcher searcher)
            throws RepositoryException, IOException {
        BooleanQuery query = new BooleanQuery();
        int clauses = 0;

        try {
            LinkedList<String> ids = new LinkedList<String>();
            Node ancestor = session.getNode(dn.getAncestorPath());
            ids.add(ancestor.getIdentifier());
            while (!ids.isEmpty()) {
                String id = ids.removeFirst();
                Query q = new JackrabbitTermQuery(new Term(FieldNames.PARENT, id));
                QueryHits hits = searcher.evaluate(q);
                ScoreNode sn = hits.nextScoreNode();
                if (sn != null) {
                    // reset query so it does not overflow because of the max
                    // clause count condition,
                    // see JCR-3108
                    clauses++;
                    if (clauses == BooleanQuery.getMaxClauseCount()) {
                        BooleanQuery wrapQ = new BooleanQuery();
                        wrapQ.add(query, SHOULD);
                        query = wrapQ;
                        clauses = 1;
                    }
                    query.add(q, SHOULD);
                    do {
                        ids.add(sn.getNodeId().toString());
                        sn = hits.nextScoreNode();
                    } while (sn != null);
                }
            }
        } catch (PathNotFoundException e) {
            query.add(new JackrabbitTermQuery(new Term(
                    FieldNames.UUID, "invalid-node-id")), // never matches
                    SHOULD);
        }

        return query;
    }

    protected Query getFullTextSearchQuery(FullTextSearch fts)
            throws RepositoryException {
        String field = FieldNames.FULLTEXT;
        String property = fts.getPropertyName();
        if (property != null) {
            Name name = session.getQName(property);
            field = nsMappings.getPrefix(name.getNamespaceURI()) + ":"
                + FieldNames.FULLTEXT_PREFIX + name.getLocalName();
        }

        StaticOperand expression = fts.getFullTextSearchExpression();
        String query = evaluator.getValue(expression).getString();
        try {
            QueryParser parser = new JackrabbitQueryParser(
                    field, index.getTextAnalyzer(),
                    index.getSynonymProvider(), cache);
            return parser.parse(query);
        } catch (ParseException e) {
            throw new RepositoryException(
                    "Invalid full text search expression: " + query, e);
        }
    }

    protected BooleanQuery getAndQuery(
            And and, Map<String, NodeType> selectorMap,
            JackrabbitIndexSearcher searcher)
            throws RepositoryException, IOException {
        BooleanQuery query = new BooleanQuery();
        addBooleanConstraint(
                query, and.getConstraint1(), MUST, selectorMap, searcher);
        addBooleanConstraint(
                query, and.getConstraint2(), MUST, selectorMap, searcher);
        return query;
    }

    protected BooleanQuery getOrQuery(
            Or or, Map<String, NodeType> selectorMap,
            JackrabbitIndexSearcher searcher)
            throws RepositoryException, IOException {
        BooleanQuery query = new BooleanQuery();
        addBooleanConstraint(
                query, or.getConstraint1(), SHOULD, selectorMap, searcher);
        addBooleanConstraint(
                query, or.getConstraint2(), SHOULD, selectorMap, searcher);
        return query;
    }

    protected void addBooleanConstraint(
            BooleanQuery query, Constraint constraint, Occur occur,
            Map<String, NodeType> selectorMap, JackrabbitIndexSearcher searcher)
            throws RepositoryException, IOException {
        if (occur == MUST && constraint instanceof And) {
            And and = (And) constraint;
            addBooleanConstraint(
                    query, and.getConstraint1(), occur, selectorMap, searcher);
            addBooleanConstraint(
                    query, and.getConstraint2(), occur, selectorMap, searcher);
        } else if (occur == SHOULD && constraint instanceof Or) {
            Or or = (Or) constraint;
            addBooleanConstraint(
                    query, or.getConstraint1(), occur, selectorMap, searcher);
            addBooleanConstraint(
                    query, or.getConstraint2(), occur, selectorMap, searcher);
        } else {
            query.add(create(constraint, selectorMap, searcher), occur);
        }
    }

    protected NotQuery getNotQuery(
            Not not, Map<String, NodeType> selectorMap,
            JackrabbitIndexSearcher searcher)
            throws RepositoryException, IOException {
        return new NotQuery(create(not.getConstraint(), selectorMap, searcher));
    }

    protected Query getPropertyExistenceQuery(PropertyExistence property)
            throws RepositoryException {
        String name = npResolver.getJCRName(session.getQName(
                property.getPropertyName()));
        return Util.createMatchAllQuery(
                name, index.getIndexFormatVersion(), cache);
    }

    protected static class Transform {

        private final DynamicOperand operand;

        private final int transform;

        public Transform(DynamicOperand operand) {
            // Check the transformation type
            if (operand instanceof UpperCase) {
                this.transform = TRANSFORM_UPPER_CASE;
            } else if (operand instanceof LowerCase) {
                this.transform = TRANSFORM_LOWER_CASE;
            } else {
                this.transform = TRANSFORM_NONE;
            }

            // Unwrap any nested transformations
            while (true) {
                if (operand instanceof UpperCase) {
                    operand = ((UpperCase) operand).getOperand();
                } else if (operand instanceof LowerCase) {
                    operand = ((LowerCase) operand).getOperand();
                } else {
                    break;
                }
            }
            this.operand = operand;
        }
    }

    protected Query getComparisonQuery(
            DynamicOperand left, int transform, String operator,
            StaticOperand rigth, Map<String, NodeType> selectorMap)
            throws RepositoryException {
        if (left instanceof PropertyValue) {
            PropertyValue pv = (PropertyValue) left;
            String field = npResolver.getJCRName(session.getQName(
                    pv.getPropertyName()));
            int type = PropertyType.UNDEFINED;
            NodeType nt = selectorMap.get(pv.getSelectorName());
            if (nt != null) {
                for (PropertyDefinition pd : nt.getPropertyDefinitions()) {
                    if (pd.getName().equals(pv.getPropertyName())) {
                        type = pd.getRequiredType();
                        break;
                    }
                }
            }
            return getPropertyValueQuery(
                    field, operator, evaluator.getValue(rigth), type, transform);
        } else if (left instanceof NodeName) {
            return getNodeNameQuery(transform, operator, rigth);
        } else if (left instanceof NodeLocalName) {
            return getNodeLocalNameQuery(transform, operator, rigth);
        } else {
            throw new UnsupportedRepositoryOperationException(
                    "Unknown operand type: " + left); // FIXME
        }
    }

    protected Query getNodeNameQuery(
            int transform, String operator, StaticOperand right)
            throws RepositoryException {
        if (transform != TRANSFORM_NONE
                || !JCR_OPERATOR_EQUAL_TO.equals(operator)) {
            throw new UnsupportedRepositoryOperationException();
        }

        Value value = evaluator.getValue(right);
        int type = value.getType();
        String string = value.getString();
        if (type == PropertyType.URI && string.startsWith("./")) {
            string = string.substring("./".length());
        } else if (type == PropertyType.DOUBLE
                || type == PropertyType.DECIMAL
                || type == PropertyType.LONG
                || type == PropertyType.BOOLEAN
                || type == PropertyType.REFERENCE
                || type == PropertyType.WEAKREFERENCE) {
            throw new InvalidQueryException("Invalid name value: " + string);
        }

        try {
            Name name = session.getQName(string);
            Term uri = new Term(NAMESPACE_URI, name.getNamespaceURI());
            Term local = new Term(LOCAL_NAME, name.getLocalName());

            BooleanQuery query = new BooleanQuery();
            query.add(new JackrabbitTermQuery(uri), MUST);
            query.add(new JackrabbitTermQuery(local), MUST);
            return query;
        } catch (IllegalNameException e) {
            throw new InvalidQueryException("Illegal name: " + string, e);
        }
    }

    protected Query getNodeLocalNameQuery(int transform, String operator,
            StaticOperand right) throws RepositoryException {
        if (!JCR_OPERATOR_EQUAL_TO.equals(operator) && !JCR_OPERATOR_LIKE.equals(operator)) {
            throw new UnsupportedRepositoryOperationException();
        }
        String name = evaluator.getValue(right).getString();

        if (JCR_OPERATOR_LIKE.equals(operator)) {
            return new WildcardQuery(LOCAL_NAME, null, name, transform, cache);
        }
        return new JackrabbitTermQuery(new Term(LOCAL_NAME, name));
    }

    protected Query getNodeIdQuery(String field, String path)
            throws RepositoryException {
        String value;
        try {
            value = session.getNode(path).getIdentifier();
        } catch (PathNotFoundException e) {
            value = "invalid-node-id"; // can never match a node
        }
        return new JackrabbitTermQuery(new Term(field, value));
    }

    protected Query getPropertyValueQuery(
            String field, String operator, Value value,
            int type, int transform) throws RepositoryException {
        String string = getValueString(value, type);
        if (JCR_OPERATOR_LIKE.equals(operator)) {
            return new WildcardQuery(PROPERTIES, field, string, transform, cache);
        }

        Term term = getTerm(field, string);
        if (JCR_OPERATOR_EQUAL_TO.equals(operator)) {
            switch (transform) {
            case TRANSFORM_UPPER_CASE:
                return new CaseTermQuery.Upper(term);
            case TRANSFORM_LOWER_CASE:
                return new CaseTermQuery.Lower(term);
            default:
                return new JackrabbitTermQuery(term);
            }
        } else if (JCR_OPERATOR_GREATER_THAN.equals(operator)) {
            return new RangeQuery(term, getTerm(field, "\uFFFF"), false, transform, cache);
        } else if (JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO.equals(operator)) {
            return new RangeQuery(term, getTerm(field, "\uFFFF"), true, transform, cache);
        } else if (JCR_OPERATOR_LESS_THAN.equals(operator)) {
            return new RangeQuery(getTerm(field, ""), term, false, transform, cache);
        } else if (JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO.equals(operator)) {
            return new RangeQuery(getTerm(field, ""), term, true, transform, cache);
        } else if (JCR_OPERATOR_NOT_EQUAL_TO.equals(operator)) {
            BooleanQuery query = new BooleanQuery();
            query.add(Util.createMatchAllQuery(
                    field, index.getIndexFormatVersion(), cache), SHOULD);
            if (transform == TRANSFORM_UPPER_CASE) {
                query.add(new CaseTermQuery.Upper(term), MUST_NOT);
            } else if (transform == TRANSFORM_LOWER_CASE) {
                query.add(new CaseTermQuery.Lower(term), MUST_NOT);
            } else {
                query.add(new JackrabbitTermQuery(term), MUST_NOT);
            }
            // and exclude all nodes where 'field' is multi valued
            query.add(new JackrabbitTermQuery(new Term(MVP, field)), MUST_NOT);
            return query;
        } else {
            throw new UnsupportedRepositoryOperationException(); // FIXME
        }
    }

    protected Term getTerm(String field, String value) {
        return new Term(PROPERTIES, FieldNames.createNamedValue(field, value));
    }

    protected String getValueString(Value value, int type)
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

    protected static class QueryPair {
        Query mainQuery;
        BooleanQuery subQuery;

        QueryPair(BooleanQuery mainQuery) {
            this.mainQuery = mainQuery;
            this.subQuery = mainQuery;
        }
    }

}
