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

import org.apache.jackrabbit.core.search.QueryRootNode;
import org.apache.jackrabbit.core.SearchManager;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.Path;
import org.apache.jackrabbit.core.NoPrefixDeclaredException;
import org.apache.jackrabbit.core.MalformedPathException;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.search.QueryParser;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;

import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;

/**
 * @author Marcel Reutegger
 * @version $Revision:  $, $Date:  $
 */
public class QueryImpl implements Query {

    /** jcr:statement */
    private static final QName PROP_STATEMENT =
	    new QName(NamespaceRegistryImpl.NS_JCR_URI, "statement");

    /** jcr:language */
    private static final QName PROP_LANGUAGE =
	    new QName(NamespaceRegistryImpl.NS_JCR_URI, "language");

    private final QueryRootNode root;

    private final SessionImpl session;

    private final ItemManager itemMgr;

    private final String statement;

    private final String language;

    private final SearchManager searchMgr;

    private Path path;

    public QueryImpl(SessionImpl session,
	      ItemManager itemMgr,
		     SearchManager searchMgr,
		     String statement,
		     String language) throws InvalidQueryException {
	this.session = session;
	this.itemMgr = itemMgr;
	this.searchMgr = searchMgr;
	this.statement = statement;
	this.language = language;

	// parse query according to language
	// build query tree
	this.root = QueryParser.parse(statement, language);
    }

    public QueryImpl(SessionImpl session,
	      ItemManager itemMgr,
	      SearchManager searchMgr,
	      String absPath)
	    throws ItemNotFoundException, InvalidQueryException, RepositoryException {

    	this.session = session;
	this.itemMgr = itemMgr;
	this.searchMgr = searchMgr;

	try {
	    Node query = null;
	    if (session.getRootNode().hasNode(absPath)) {
		query = session.getRootNode().getNode(absPath);
		// assert query has mix:referenceable
		query.getUUID();
		NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
		NodeType ntQuery = ntMgr.getNodeType(NodeTypeRegistry.NT_QUERY.toJCRName(
			session.getNamespaceResolver()));
		if (!query.getPrimaryNodeType().equals(ntQuery)) {
		    throw new InvalidQueryException("node is not of type nt:query");
		}
	    } else {
		throw new ItemNotFoundException(absPath);
	    }

	    path = Path.create(absPath, session.getNamespaceResolver(), true);

	    statement = query.getProperty(PROP_STATEMENT.toJCRName(session.getNamespaceResolver())).getString();
	    language = query.getProperty(PROP_LANGUAGE.toJCRName(session.getNamespaceResolver())).getString();

	    // parse query according to language
	    // build query tree and pass to QueryImpl
	    QueryRootNode root = QueryParser.parse(statement, language);
	    this.root = root;
	} catch (NoPrefixDeclaredException e) {
	    throw new InvalidQueryException(e.getMessage(), e);
	} catch (MalformedPathException e) {
	    throw new ItemNotFoundException(absPath, e);
	}
    }

    public QueryResult execute() throws RepositoryException {
	return searchMgr.execute(itemMgr, root, session);
    }

    public String getStatement() {
	return statement;
    }

    public String getLanguage() {
	return language;
    }

    public String getPersistentQueryPath() throws ItemNotFoundException {
	if (path == null) {
	    throw new ItemNotFoundException("not a persistent query");
	}
	try {
	    return path.toJCRPath(session.getNamespaceResolver());
	} catch (NoPrefixDeclaredException e) {
	    // should not happen actually
	    throw new ItemNotFoundException(path.toString());
	}
    }

    public String getPersistentQueryUUID() throws ItemNotFoundException {
	if (path == null) {
	    throw new ItemNotFoundException("not a persistent query");
	}
	try {
	    // FIXME what if nt:query does not have a mix:referencable?
	    return session.getRootNode().getNode(path.toJCRPath(session.getNamespaceResolver())).getUUID();
	} catch (RepositoryException e) {
	    throw new ItemNotFoundException(e.getMessage(), e);
	} catch (NoPrefixDeclaredException e) {
	    throw new ItemNotFoundException(e.getMessage(), e);
	}
    }

    public void save(String absPath)
	    throws ItemExistsException,
	    PathNotFoundException,
	    ConstraintViolationException,
	    RepositoryException {
	try {
	    NamespaceResolver resolver = session.getNamespaceResolver();
	    Path p = Path.create(absPath, resolver, true);
	    if (!p.isAbsolute()) {
		throw new RepositoryException(absPath + " is not absolut");
	    }
	    if (!session.getRootNode().hasNode(p.getAncestor(1).toJCRPath(resolver))) {
		throw new PathNotFoundException(p.getAncestor(1).toJCRPath(resolver));
	    }
	    if (session.getRootNode().hasNode(p.toJCRPath(resolver))) {
		throw new ItemExistsException(p.toJCRPath(resolver));
	    }
	    Node queryNode = session.getRootNode().addNode(p.toJCRPath(resolver),
		    NodeTypeRegistry.NT_QUERY.toJCRName(resolver));
	    // set properties
	    queryNode.setProperty(PROP_LANGUAGE.toJCRName(resolver), language);
	    queryNode.setProperty(PROP_STATEMENT.toJCRName(resolver), statement);
	    // add mixin referenceable
	    queryNode.addMixin(NodeTypeRegistry.MIX_REFERENCEABLE.toJCRName(resolver));
	    // FIXME do anything else?
	    queryNode.save();

	} catch (MalformedPathException e) {
	    throw new RepositoryException(e.getMessage(), e);
	} catch (NoPrefixDeclaredException e) {
	    throw new RepositoryException(e.getMessage(), e);
	}
	session.getRootNode().getNode(absPath);
    }

}
