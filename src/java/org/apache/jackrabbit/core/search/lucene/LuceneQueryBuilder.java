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

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.IllegalNameException;
import org.apache.jackrabbit.core.UnknownPrefixException;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.NoPrefixDeclaredException;
import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.search.*;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RangeQuery;
import org.apache.lucene.search.TermQuery;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;

/**
 */
class LuceneQueryBuilder implements QueryNodeVisitor {

    private static final Logger log = Logger.getLogger(LuceneQueryBuilder.class);

    private static QName primaryType = new QName(NamespaceRegistryImpl.NS_JCR_URI, "primaryType");

    private QueryRootNode root;

    private SessionImpl session;

    private NamespaceMappings nsMappings;

    private Analyzer analyzer;

    private List exceptions = new ArrayList();

    private LuceneQueryBuilder(QueryRootNode root,
                               SessionImpl session,
                               NamespaceMappings nsMappings,
                               Analyzer analyzer) {
        this.root = root;
        this.session = session;
        this.nsMappings = nsMappings;
        this.analyzer = analyzer;
    }

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
            throw new RepositoryException("Exception parsing query: " + msg.toString());
        }
        return q;
    }

    private Query createLuceneQuery() {
        return (Query) root.accept(this, null);
    }

    public Object visit(QueryRootNode node, Object data) {
        BooleanQuery root = new BooleanQuery();

        QName[] props = node.getSelectProperties();
        for (int i = 0; i < props.length; i++) {
            try {
                String prop = props[i].toJCRName(nsMappings);
                // @todo really search nodes that have non null values?
                root.add(new MatchAllQuery(prop), true, false);
            } catch (NoPrefixDeclaredException e) {
                // should never happen actually. prefixes are dynamically created
                exceptions.add(e);
            }
        }

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
            field = NodeTypeRegistry.JCR_PRIMARY_TYPE.toJCRName(nsMappings);
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
                b.add(new TermQuery(new Term(field, (String)it.next())), false, false);
            }
            return b;
        }
    }

    public Object visit(RangeQueryNode node, Object data) {
        return null;
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
        // loop over steps
        QueryNode[] steps = node.getPathSteps();
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
        if (node.getNameTest() != null && node.getNameTest().getLocalName().length() == 0) {
            // select root node
            return new TermQuery(new Term(FieldNames.PARENT, ""));
        }

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
                    Query subQuery = new DescendantSelfAxisQuery(context, andQuery);
                    andQuery = new BooleanQuery();
                    andQuery.add(subQuery, true, false);
                } else {
                    // @todo this will traverse the whole index, optimize!
                    Query subQuery = new MatchAllQuery(FieldNames.UUID);
                    andQuery.add(new DescendantSelfAxisQuery(context, subQuery), true, false);
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
        String stringValue;
        switch (node.getType()) {
            case Constants.TYPE_DATE:
                stringValue = DateField.dateToString(node.getDateValue());
                break;
            case Constants.TYPE_DOUBLE:
                stringValue = DoubleField.doubleToString(node.getDoubleValue());
                break;
            case Constants.TYPE_LONG:
                stringValue = LongField.longToString(node.getLongValue());
                break;
            case Constants.TYPE_STRING:
                stringValue = node.getStringValue();
                break;
            default:
                throw new IllegalArgumentException("Unknown relation type: "
                        + node.getType());
        }

        String field = "";
        try {
            field = node.getProperty().toJCRName(nsMappings);
        } catch (NoPrefixDeclaredException e) {
            // should never happen
            exceptions.add(e);
        }

        switch (node.getOperation()) {
            case Constants.OPERATION_EQ:	// =
                query = new TermQuery(new Term(field, stringValue));
                break;
            case Constants.OPERATION_GE:	// >=
                query = new RangeQuery(new Term(field, stringValue), null, true);
                break;
            case Constants.OPERATION_GT:	// >
                query = new RangeQuery(new Term(field, stringValue), null, false);
                break;
            case Constants.OPERATION_LE:	// <=
                query = new RangeQuery(null, new Term(field, stringValue), true);
                break;
            case Constants.OPERATION_LIKE:	// LIKE
                if (stringValue.equals("%")) {
                    query = new MatchAllQuery(field);
                } else {
                    query = new WildcardQuery(new Term(field, stringValue));
                }
                break;
            case Constants.OPERATION_LT:	// <
                query = new RangeQuery(null, new Term(field, stringValue), false);
                break;
            case Constants.OPERATION_NE:	// !=
                BooleanQuery notQuery = new BooleanQuery();
                notQuery.add(new MatchAllQuery(field), false, false);
                notQuery.add(new TermQuery(new Term(field, stringValue)), false, true);
                query = notQuery;
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
