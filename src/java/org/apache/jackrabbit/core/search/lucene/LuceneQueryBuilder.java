/*
 * Copyright 2004 The Apache Software Foundation.
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

import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.RangeQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.search.*;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.log4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.NamespaceException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
public class LuceneQueryBuilder implements QueryNodeVisitor {

    private static final Logger log = Logger.getLogger(LuceneQueryBuilder.class);

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
	    for (Iterator it = builder.exceptions.iterator(); it.hasNext(); ) {
		msg.append(it.next().toString()).append('\n');
	    }
	    throw new RepositoryException("Exception parsing query: " + msg.toString());
	}
	return q;
    }

    private Query createLuceneQuery() {
	return (Query)root.accept(this, null);
    }

    public Object visit(QueryRootNode node, Object data) {
	BooleanQuery root = new BooleanQuery();
	Query constraintQuery = (Query)node.getConstraintNode().accept(this, null);
	if (constraintQuery != null) {
	    root.add(constraintQuery, true, false);
	}

	String[] props = node.getSelectProperties();
	for (int i = 0; i < props.length; i++) {
	    String prop = props[i];
	    try {
		prop = nsMappings.translatePropertyName(prop, session.getNamespaceResolver());
	    } catch (MalformedPathException e) {
		exceptions.add(e);
	    }
	    root.add(new MatchAllQuery(prop), true, false);
	}

	TextsearchQueryNode textsearchNode = node.getTextsearchNode();
	if (textsearchNode != null) {
	    Query textsearch = (Query)textsearchNode.accept(this, null);
	    if (textsearch != null) {
		root.add(textsearch, true, false);
	    }
	}

	Query wrapped = root;
	if (node.getLocationNode() != null) {
	    wrapped = (Query)node.getLocationNode().accept(this, root);
	}

	return wrapped;
    }

    public Object visit(OrQueryNode node, Object data) {
	BooleanQuery orQuery = new BooleanQuery();
	Object[] result = node.acceptOperands(this, null);
	for (int i = 0; i < result.length; i++) {
	    Query operand = (Query)result[i];
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
	    Query operand = (Query)result[i];
	    andQuery.add(operand, true, false);
	}
	return andQuery;
    }

    public Object visit(NotQueryNode node, Object data) {
	BooleanQuery notQuery = new BooleanQuery();
	Object[] result = node.acceptOperands(this, null);
	for (int i = 0; i < result.length; i++) {
	    Query operand = (Query)result[i];
	    notQuery.add(operand, false, true);
	}
	return notQuery;
    }

    public Object visit(ExactQueryNode node, Object data) {
	String field = node.getPropertyName();
	try {
	    field = nsMappings.translatePropertyName(node.getPropertyName(),
		    session.getNamespaceResolver());
	} catch (MalformedPathException e) {
            exceptions.add(e);
	}
	String value = node.getValue();
	return new TermQuery(new Term(field, value));
    }

    public Object visit(NodeTypeQueryNode node, Object data) {
	String field = node.getPropertyName();
	String value = node.getValue();
	try {
	    field = nsMappings.getPrefix(NodeTypeRegistry.JCR_PRIMARY_TYPE.getNamespaceURI())
		    + ":" + NodeTypeRegistry.JCR_PRIMARY_TYPE.getLocalName();
	    value = nsMappings.translatePropertyName(node.getValue(),
		    session.getNamespaceResolver());
	} catch (NamespaceException e) {
	    // will never happen
	    log.error(e.toString());
	} catch (MalformedPathException e) {
	    exceptions.add(e);
	}
	return new TermQuery(new Term(field, value));
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
	String path = node.getPath();
	try {
	    path = nsMappings.translatePropertyName(node.getPath(),
		    session.getNamespaceResolver());
	} catch (MalformedPathException e) {
	    exceptions.add(e);
	}
	PathFilter filter = new PathFilter(path, node.getType());
	return new PathFilterQuery((Query)data, new PackageFilter(filter));
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

	String field = node.getProperty();
	try {
	    field = nsMappings.translatePropertyName(node.getProperty(),
		    session.getNamespaceResolver());
	} catch (MalformedPathException e) {
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
		query = new WildcardQuery(new Term(field, stringValue));
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
	return null;
    }

    //---------------------------< internal >-----------------------------------

}
