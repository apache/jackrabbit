/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.search.lucene;

import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.search.*;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RangeQuery;
import org.apache.lucene.search.TermQuery;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.InvalidQueryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Implements a query builder that takes an abstract query tree and creates
 * a lucene {@link org.apache.lucene.search.Query} tree that can be executed
 * on an index.
 * todo introduce a node type hierarchy for efficient translation of NodeTypeQueryNode
 */
class LuceneQueryBuilder implements QueryNodeVisitor {

    /**
     * Logger for this class
     */
    private static final Logger log = Logger.getLogger(LuceneQueryBuilder.class);

    /**
     * QName for jcr:primaryType
     */
    private static QName primaryType = org.apache.jackrabbit.core.Constants.JCR_PRIMARYTYPE;

    /**
     * Root node of the abstract query tree
     */
    private QueryRootNode root;

    /**
     * Session of the user executing this query
     */
    private SessionImpl session;

    /**
     * Namespace mappings to internal prefixes
     */
    private NamespaceMappings nsMappings;

    /**
     * The analyzer instance to use for contains function query parsing
     */
    private Analyzer analyzer;

    /**
     * Exceptions thrown during tree translation
     */
    private List exceptions = new ArrayList();

    /**
     * Creates a new <code>LuceneQueryBuilder</code> instance.
     *
     * @param root       the root node of the abstract query tree.
     * @param session    of the user executing this query.
     * @param nsMappings namespace resolver for internal prefixes.
     * @param analyzer   for parsing the query statement of the contains function.
     */
    private LuceneQueryBuilder(QueryRootNode root,
                               SessionImpl session,
                               NamespaceMappings nsMappings,
                               Analyzer analyzer) {
        this.root = root;
        this.session = session;
        this.nsMappings = nsMappings;
        this.analyzer = analyzer;
    }

    /**
     * Creates a lucene {@link org.apache.lucene.search.Query} tree from an
     * abstract query tree.
     *
     * @param root       the root node of the abstract query tree.
     * @param session    of the user executing the query.
     * @param nsMappings namespace resolver for internal prefixes.
     * @param analyzer   for parsing the query statement of the contains function.
     * @return the lucene query tree.
     * @throws RepositoryException if an error occurs during the translation.
     */
    public static Query createQuery(QueryRootNode root,
                                    SessionImpl session,
                                    NamespaceMappings nsMappings,
                                    Analyzer analyzer)
            throws RepositoryException {

        LuceneQueryBuilder builder = new LuceneQueryBuilder(root,
                session, nsMappings, analyzer);

        Query q = builder.createLuceneQuery();
        if (builder.exceptions.size() > 0) {
            StringBuffer msg = new StringBuffer();
            for (Iterator it = builder.exceptions.iterator(); it.hasNext();) {
                msg.append(it.next().toString()).append('\n');
            }
            throw new RepositoryException("Exception building query: " + msg.toString());
        }
        return q;
    }

    /**
     * Starts the tree traversal and returns the lucene
     * {@link org.apache.lucene.search.Query}.
     *
     * @return the lucene <code>Query</code>.
     */
    private Query createLuceneQuery() {
        return (Query) root.accept(this, null);
    }

    //---------------------< QueryNodeVisitor interface >-----------------------

    public Object visit(QueryRootNode node, Object data) {
        BooleanQuery root = new BooleanQuery();

        Query wrapped = root;
        if (node.getLocationNode() != null) {
            wrapped = (Query) node.getLocationNode().accept(this, root);
        }

        return wrapped;
    }

    public Object visit(OrQueryNode node, Object data) {
        BooleanQuery orQuery = new BooleanQuery();
        Object[] result = node.acceptOperands(this, null);
        for (int i = 0; i < result.length; i++) {
            Query operand = (Query) result[i];
            orQuery.add(operand, false, false);
        }
        return orQuery;
    }

    public Object visit(AndQueryNode node, Object data) {
        Object[] result = node.acceptOperands(this, null);
        if (result.length == 0) {
            return null;
        }
        BooleanQuery andQuery = new BooleanQuery();
        for (int i = 0; i < result.length; i++) {
            Query operand = (Query) result[i];
            andQuery.add(operand, true, false);
        }
        return andQuery;
    }

    public Object visit(NotQueryNode node, Object data) {
        BooleanQuery notQuery = new BooleanQuery();
        try {
            // first select any node
            notQuery.add(new MatchAllQuery(primaryType.toJCRName(nsMappings)),
                    false, false);
        } catch (NoPrefixDeclaredException e) {
            // will never happen, prefixes are created when unknown
        }
        Object[] result = node.acceptOperands(this, null);
        for (int i = 0; i < result.length; i++) {
            Query operand = (Query) result[i];
            // then prohibit the nodes from the not clause
            notQuery.add(operand, false, true);
        }
        return notQuery;
    }

    public Object visit(ExactQueryNode node, Object data) {
        String field = "";
        String value = "";
        try {
            field = node.getPropertyName().toJCRName(nsMappings);
            value = node.getValue().toJCRName(nsMappings);
        } catch (NoPrefixDeclaredException e) {
            // will never happen, prefixes are created when unknown
        }
        return new TermQuery(new Term(field, value));
    }

    public Object visit(NodeTypeQueryNode node, Object data) {
        String field = "";
        List values = new ArrayList();
        try {
            field = Constants.JCR_PRIMARYTYPE.toJCRName(nsMappings);
            values.add(node.getValue().toJCRName(nsMappings));
            NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
            NodeType base = ntMgr.getNodeType(node.getValue().toJCRName(session.getNamespaceResolver()));
            NodeTypeIterator allTypes = ntMgr.getAllNodeTypes();
            while (allTypes.hasNext()) {
                NodeType nt = allTypes.nextNodeType();
                NodeType[] superTypes = nt.getSupertypes();
                if (Arrays.asList(superTypes).contains(base)) {
                    values.add(nsMappings.translatePropertyName(nt.getName(),
                            session.getNamespaceResolver()));
                }
            }
        } catch (IllegalNameException e) {
            exceptions.add(e);
        } catch (UnknownPrefixException e) {
            exceptions.add(e);
        } catch (NoPrefixDeclaredException e) {
            // should never happen
            exceptions.add(e);
        } catch (RepositoryException e) {
            exceptions.add(e);
        }
        if (values.size() == 0) {
            // exception occured
            return new BooleanQuery();
        } else if (values.size() == 1) {
            return new TermQuery(new Term(field, (String) values.get(0)));
        } else {
            BooleanQuery b = new BooleanQuery();
            for (Iterator it = values.iterator(); it.hasNext();) {
                b.add(new TermQuery(new Term(field, (String) it.next())), false, false);
            }
            return b;
        }
    }

    public Object visit(TextsearchQueryNode node, Object data) {
        try {
            org.apache.lucene.queryParser.QueryParser parser
                    = new org.apache.lucene.queryParser.QueryParser(FieldNames.FULLTEXT, analyzer);
            parser.setOperator(org.apache.lucene.queryParser.QueryParser.DEFAULT_OPERATOR_AND);
            // replace unescaped ' with " and escaped ' with just '
            StringBuffer query = new StringBuffer();
            String textsearch = node.getQuery();
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
                        query.append('\'');
                        escaped = false;
                    } else {
                        query.append('\"');
                    }
                } else {
                    if (escaped) {
                        query.append('\\');
                        escaped = false;
                    }
                    query.append(textsearch.charAt(i));
                }
            }
            return parser.parse(query.toString());
        } catch (ParseException e) {
            exceptions.add(e);
        }
        return null;
    }

    public Object visit(PathQueryNode node, Object data) {
        Query context = null;
        LocationStepQueryNode[] steps = node.getPathSteps();
        if (steps.length > 0) {
            if (node.isAbsolute() && !steps[0].getIncludeDescendants()) {
                // eat up first step
                QName nameTest = steps[0].getNameTest();
                if (nameTest == null) {
                    // this is equivalent to the root node
                    context = new TermQuery(new Term(FieldNames.PARENT, ""));
                } else if (nameTest.getLocalName().length() == 0) {
                    // root node
                    context = new TermQuery(new Term(FieldNames.PARENT, ""));
                } else {
                    // then this is a node != the root node
                    // will never match anything!
                    String name = "";
                    try {
                        name = nameTest.toJCRName(nsMappings);
                    } catch (NoPrefixDeclaredException e) {
                        exceptions.add(e);
                    }
                    BooleanQuery and = new BooleanQuery();
                    and.add(new TermQuery(new Term(FieldNames.PARENT, "")), true, false);
                    and.add(new TermQuery(new Term(FieldNames.LABEL, name)), true, false);
                    context = and;
                }
                LocationStepQueryNode[] tmp = new LocationStepQueryNode[steps.length - 1];
                System.arraycopy(steps, 1, tmp, 0, steps.length - 1);
                steps = tmp;
            } else {
                // path is 1) relative or 2) descendant-or-self
                // use root node as context
                context = new TermQuery(new Term(FieldNames.PARENT, ""));
            }
        } else {
            exceptions.add(new InvalidQueryException("Number of location steps must be > 0"));
        }
        // loop over steps
        for (int i = 0; i < steps.length; i++) {
            context = (Query) steps[i].accept(this, context);
        }
        if (data instanceof BooleanQuery) {
            BooleanQuery constraint = (BooleanQuery) data;
            if (constraint.getClauses().length > 0) {
                constraint.add(context, true, false);
                context = constraint;
            }
        }
        return context;
    }

    public Object visit(LocationStepQueryNode node, Object data) {
        Query context = (Query) data;
        BooleanQuery andQuery = new BooleanQuery();

        if (context == null) {
            exceptions.add(new IllegalArgumentException("Unsupported query"));
        }

        // predicate on step?
        Object[] predicates = node.acceptOperands(this, data);
        for (int i = 0; i < predicates.length; i++) {
            andQuery.add((Query) predicates[i], true, false);
        }

        TermQuery nameTest = null;
        if (node.getNameTest() != null) {
            try {
                String internalName = node.getNameTest().toJCRName(nsMappings);
                nameTest = new TermQuery(new Term(FieldNames.LABEL, internalName));
            } catch (NoPrefixDeclaredException e) {
                // should never happen
                exceptions.add(e);
            }
        }

        if (node.getIncludeDescendants()) {
            if (nameTest != null) {
                andQuery.add(new DescendantSelfAxisQuery(context, nameTest), true, false);
            } else {
                // descendant-or-self with nametest=*
                if (predicates.length > 0) {
                    // if we have a predicate attached, the condition acts as
                    // the sub query.
                    Query subQuery = new DescendantSelfAxisQuery(context, andQuery, false);
                    andQuery = new BooleanQuery();
                    andQuery.add(subQuery, true, false);
                } else {
                    // @todo this will traverse the whole index, optimize!
                    Query subQuery = new MatchAllQuery(FieldNames.UUID);
                    context = new DescendantSelfAxisQuery(context, subQuery);
                    andQuery.add(new ChildAxisQuery(context), true, false);
                }
            }
        } else {
            // select child nodes
            andQuery.add(new ChildAxisQuery(context), true, false);

            // name test
            if (nameTest != null) {
                andQuery.add(nameTest, true, false);
            }
        }

        return andQuery;
    }

    public Object visit(RelationQueryNode node, Object data) {
        Query query;
        String stringValue = null;
        switch (node.getValueType()) {
            case 0:
                // not set: either IS NULL or IS NOT NULL
                break;
            case QueryConstants.TYPE_DATE:
                stringValue = DateField.dateToString(node.getDateValue());
                break;
            case QueryConstants.TYPE_DOUBLE:
                stringValue = DoubleField.doubleToString(node.getDoubleValue());
                break;
            case QueryConstants.TYPE_LONG:
                stringValue = LongField.longToString(node.getLongValue());
                break;
            case QueryConstants.TYPE_STRING:
                stringValue = node.getStringValue();
                break;
            default:
                throw new IllegalArgumentException("Unknown relation type: "
                        + node.getValueType());
        }

        String field = "";
        String primaryTypeField = "";
        String mvpField = "";
        try {
            field = node.getProperty().toJCRName(nsMappings);
            primaryTypeField = primaryType.toJCRName(nsMappings);
            StringBuffer tmp = new StringBuffer();
            tmp.append(nsMappings.getPrefix(node.getProperty().getNamespaceURI()));
            tmp.append(':').append(FieldNames.MVP_PREFIX);
            tmp.append(node.getProperty().getLocalName());
            mvpField = tmp.toString();
        } catch (NamespaceException e) {
            // should never happen
            exceptions.add(e);
        } catch (NoPrefixDeclaredException e) {
            // should never happen
            exceptions.add(e);
        }

        switch (node.getOperation()) {
            case QueryConstants.OPERATION_EQ_VALUE:      // =
                query = new TermQuery(new Term(field, stringValue));
                break;
            case QueryConstants.OPERATION_EQ_GENERAL:    // =
                // search in single and multi valued properties
                BooleanQuery or = new BooleanQuery();
                or.add(new TermQuery(new Term(field, stringValue)), false, false);
                or.add(new TermQuery(new Term(mvpField, stringValue)), false, false);
                query = or;
                break;
            case QueryConstants.OPERATION_GE_VALUE:      // >=
                query = new RangeQuery(new Term(field, stringValue), null, true);
                break;
            case QueryConstants.OPERATION_GT_VALUE:      // >
                query = new RangeQuery(new Term(field, stringValue), null, false);
                break;
            case QueryConstants.OPERATION_LE_VALUE:      // <=
                query = new RangeQuery(null, new Term(field, stringValue), true);
                break;
            case QueryConstants.OPERATION_LIKE:          // LIKE
                if (stringValue.equals("%")) {
                    query = new MatchAllQuery(field);
                } else {
                    query = new WildcardQuery(new Term(field, stringValue));
                }
                break;
            case QueryConstants.OPERATION_LT_VALUE:      // <
                query = new RangeQuery(null, new Term(field, stringValue), false);
                break;
            case QueryConstants.OPERATION_NE_VALUE:      // !=
                BooleanQuery notQuery = new BooleanQuery();
                notQuery.add(new MatchAllQuery(field), false, false);
                notQuery.add(new TermQuery(new Term(field, stringValue)), false, true);
                query = notQuery;
                break;
            case QueryConstants.OPERATION_NE_GENERAL:    // !=
                // search in single and multi valued properties
                notQuery = new BooleanQuery();
                notQuery.add(new MatchAllQuery(field), false, false);
                notQuery.add(new MatchAllQuery(mvpField), false, false);
                notQuery.add(new TermQuery(new Term(field, stringValue)), false, true);
                notQuery.add(new TermQuery(new Term(mvpField, stringValue)), false, true);
                query = notQuery;
                break;
            case QueryConstants.OPERATION_NULL:
                notQuery = new BooleanQuery();
                notQuery.add(new MatchAllQuery(primaryTypeField), false, false);
                notQuery.add(new MatchAllQuery(field), false, true);
                query = notQuery;
                break;
            case QueryConstants.OPERATION_NOT_NULL:
                query = new MatchAllQuery(field);
                break;
            default:
                throw new IllegalArgumentException("Unknown relation operation: "
                        + node.getOperation());
        }
        return query;
    }

    public Object visit(OrderQueryNode node, Object data) {
        return data;
    }
}
