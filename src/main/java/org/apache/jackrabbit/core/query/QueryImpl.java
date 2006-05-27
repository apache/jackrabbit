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
package org.apache.jackrabbit.core.query;

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

/**
 * Provides the default implementation for a JCR query.
 */
public class QueryImpl extends AbstractQueryImpl {

    /**
     * The session of the user executing this query
     */
    protected SessionImpl session;

    /**
     * The query statement
     */
    protected String statement;

    /**
     * The syntax of the query statement
     */
    protected String language;

    /**
     * The actual query implementation that can be executed
     */
    protected ExecutableQuery query;

    /**
     * The node where this query is persisted. Only set when this is a persisted
     * query.
     */
    protected Node node;

    /**
     * The query handler for this query.
     */
    protected QueryHandler handler;

    /**
     * Flag indicating whether this query is initialized.
     */
    private boolean initialized = false;

    /**
     * @inheritDoc
     */
    public void init(SessionImpl session,
                     ItemManager itemMgr,
                     QueryHandler handler,
                     String statement,
                     String language) throws InvalidQueryException {
        checkNotInitialized();
        this.session = session;
        this.statement = statement;
        this.language = language;
        this.handler = handler;
        this.query = handler.createExecutableQuery(session, itemMgr, statement, language);
        initialized = true;
    }

    /**
     * @inheritDoc
     */
    public void init(SessionImpl session,
                     ItemManager itemMgr,
                     QueryHandler handler,
                     Node node)
            throws InvalidQueryException, RepositoryException {
        checkNotInitialized();
        this.session = session;
        this.node = node;
        this.handler = handler;

        try {
            if (!node.isNodeType(QName.NT_QUERY.toJCRName(session.getNamespaceResolver()))) {
                throw new InvalidQueryException("node is not of type nt:query");
            }
            statement = node.getProperty(QName.JCR_STATEMENT.toJCRName(session.getNamespaceResolver())).getString();
            language = node.getProperty(QName.JCR_LANGUAGE.toJCRName(session.getNamespaceResolver())).getString();
            query = handler.createExecutableQuery(session, itemMgr, statement, language);
        } catch (NoPrefixDeclaredException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
        initialized = true;
    }

    /**
     * This method simply forwards the <code>execute</code> call to the
     * {@link ExecutableQuery} object returned by
     * {@link QueryHandler#createExecutableQuery}.
     * {@inheritDoc}
     */
    public QueryResult execute() throws RepositoryException {
        checkInitialized();
        return query.execute();
    }

    /**
     * {@inheritDoc}
     */
    public String getStatement() {
        checkInitialized();
        return statement;
    }

    /**
     * {@inheritDoc}
     */
    public String getLanguage() {
        checkInitialized();
        return language;
    }

    /**
     * {@inheritDoc}
     */
    public String getStoredQueryPath()
            throws ItemNotFoundException, RepositoryException {
        checkInitialized();
        if (node == null) {
            throw new ItemNotFoundException("not a persistent query");
        }
        return node.getPath();
    }

    /**
     * {@inheritDoc}
     */
    public Node storeAsNode(String absPath)
            throws ItemExistsException,
            PathNotFoundException,
            VersionException,
            ConstraintViolationException,
            LockException,
            UnsupportedRepositoryOperationException,
            RepositoryException {

        checkInitialized();
        try {
            NamespaceResolver resolver = session.getNamespaceResolver();
            Path p = Path.create(absPath, resolver, true);
            if (!p.isAbsolute()) {
                throw new RepositoryException(absPath + " is not an absolute path");
            }

            String relPath = p.toJCRPath(resolver).substring(1);
            Node queryNode = session.getRootNode().addNode(relPath,
                    QName.NT_QUERY.toJCRName(resolver));
            // set properties
            queryNode.setProperty(QName.JCR_LANGUAGE.toJCRName(resolver), language);
            queryNode.setProperty(QName.JCR_STATEMENT.toJCRName(resolver), statement);
            node = queryNode;
            return node;
        } catch (MalformedPathException e) {
            throw new RepositoryException(e.getMessage(), e);
        } catch (NoPrefixDeclaredException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
    }

    //-----------------------------< internal >---------------------------------

    /**
     * Checks if this query is not yet initialized and throws an
     * <code>IllegalStateException</code> if it is already initialized.
     */
    protected void checkNotInitialized() {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
    }

    /**
     * Checks if this query is initialized and throws an
     * <code>IllegalStateException</code> if it is not yet initialized.
     */
    protected void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
    }
}

