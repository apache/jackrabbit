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
package org.apache.jackrabbit.core.search;

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.MalformedPathException;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.NoPrefixDeclaredException;
import org.apache.jackrabbit.core.Path;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.Constants;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

/**
 * Implements the {@link Query} interface.
 */
public class QueryImpl implements Query {

    /**
     * The session of the user executing this query
     */
    private final SessionImpl session;

    /**
     * The query statement
     */
    private final String statement;

    /**
     * The syntax of the query statement
     */
    private final String language;

    /**
     * The actual query implementation that can be executed
     */
    private final ExecutableQuery query;

    /**
     * The node where this query is persisted. Only set when this is a persisted
     * query.
     */
    private Node node;

    /**
     * Creates a new query instance from a query string.
     *
     * @param session   the session of the user executing this query.
     * @param itemMgr   the item manager of the session executing this query.
     * @param handler   the query handler of the search index.
     * @param statement the query statement.
     * @param language  the syntax of the query statement.
     * @throws InvalidQueryException if the query statement is invalid according
     *                               to the specified <code>language</code>.
     */
    public QueryImpl(SessionImpl session,
                     ItemManager itemMgr,
                     QueryHandler handler,
                     String statement,
                     String language) throws InvalidQueryException {
        this.session = session;
        this.statement = statement;
        this.language = language;
        this.query = handler.createExecutableQuery(session, itemMgr, statement, language);
    }

    /**
     * Create a new query instance from a nt:query node.
     *
     * @param session the session of the user executing this query.
     * @param itemMgr the item manager of the session executing this query.
     * @param handler the query handler of the search index.
     * @param node    a node of type <code>nt:query</code>.
     * @throws InvalidQueryException If <code>node</code> is not a valid persisted query
     *                               (that is, a node of type <code>nt:query</code>).
     * @throws RepositoryException   if another error occurs
     */
    public QueryImpl(SessionImpl session,
                     ItemManager itemMgr,
                     QueryHandler handler,
                     Node node)
            throws InvalidQueryException, RepositoryException {

        this.session = session;
        this.node = node;

        try {
            if (!node.isNodeType(Constants.NT_QUERY.toJCRName(session.getNamespaceResolver()))) {
                throw new InvalidQueryException("node is not of type nt:query");
            }
            statement = node.getProperty(QueryConstants.JCR_STATEMENT.toJCRName(session.getNamespaceResolver())).getString();
            language = node.getProperty(QueryConstants.JCR_LANGUAGE.toJCRName(session.getNamespaceResolver())).getString();
            query = handler.createExecutableQuery(session, itemMgr, statement, language);
        } catch (NoPrefixDeclaredException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
    }

    /**
     * This method simply forwards the <code>execute</code> call to the
     * {@link ExecutableQuery} object returned by
     * {@link QueryHandler#createExecutableQuery}.
     * {@inheritDoc}
     */
    public QueryResult execute() throws RepositoryException {
        return query.execute();
    }

    /**
     * {@inheritDoc}
     */
    public String getStatement() {
        return statement;
    }

    /**
     * {@inheritDoc}
     */
    public String getLanguage() {
        return language;
    }

    /**
     * {@inheritDoc}
     */
    public String getPersistentQueryPath() throws ItemNotFoundException, RepositoryException {
        if (node == null) {
            throw new ItemNotFoundException("not a persistent query");
        }
        return node.getPath();
    }

    /**
     * {@inheritDoc}
     */
    public void save(String absPath)
            throws ItemExistsException,
            PathNotFoundException,
            VersionException,
            ConstraintViolationException,
            LockException,
            UnsupportedRepositoryOperationException,
            RepositoryException {
        try {
            NamespaceResolver resolver = session.getNamespaceResolver();
            Path p = Path.create(absPath, resolver, true);
            if (!p.isAbsolute()) {
                throw new RepositoryException(absPath + " is not an absolut path");
            }
            if (!session.getRootNode().hasNode(p.getAncestor(1).toJCRPath(resolver).substring(1))) {
                throw new PathNotFoundException(p.getAncestor(1).toJCRPath(resolver));
            }
            String relPath = p.toJCRPath(resolver).substring(1);
            if (session.getRootNode().hasNode(relPath)) {
                throw new ItemExistsException(p.toJCRPath(resolver));
            }
            Node queryNode = session.getRootNode().addNode(relPath,
                    Constants.NT_QUERY.toJCRName(resolver));
            // set properties
            queryNode.setProperty(QueryConstants.JCR_LANGUAGE.toJCRName(resolver), language);
            queryNode.setProperty(QueryConstants.JCR_STATEMENT.toJCRName(resolver), statement);
            // todo this should be changed in the spec some time!
            queryNode.getParent().save();
            node = queryNode;
        } catch (MalformedPathException e) {
            throw new RepositoryException(e.getMessage(), e);
        } catch (NoPrefixDeclaredException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
    }
}

