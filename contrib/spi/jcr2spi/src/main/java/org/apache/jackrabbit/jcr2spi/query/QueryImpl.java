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
package org.apache.jackrabbit.jcr2spi.query;

import org.apache.jackrabbit.jcr2spi.ItemManager;
import org.apache.jackrabbit.jcr2spi.SessionImpl;
import org.apache.jackrabbit.jcr2spi.WorkspaceManager;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.PathFormat;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameFormat;

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
 * Provides the default implementation for a JCR query.
 */
public class QueryImpl implements Query {

    /**
     * The session of the user executing this query
     */
    protected SessionImpl session;

    /**
     * The namespace resolver of the session that executes this query.
     */
    // DIFF JR: added
    protected NamespaceResolver resolver;

    /**
     * The item manager of the session that executes this query.
     */
    protected ItemManager itemManager;

    /**
     * The query statement
     */
    protected String statement;

    /**
     * The syntax of the query statement
     */
    protected String language;

    /**
     * The node where this query is persisted. Only set when this is a persisted
     * query.
     */
    protected Node node;

    /**
     * The query handler for this query.
     */
    // DIFF JR: use WorkspaceManager (-> RepositoryService) instead
    //protected QueryHandler handler;
    protected WorkspaceManager wspManager;

    /**
     * Flag indicating whether this query is initialized.
     */
    private boolean initialized = false;

    /**
     * Initializes this query.
     *
     * @param session    the session that created this query.
     * @param itemMgr    the item manager of that session.
     * @param wspManager the workspace manager that belongs to the session.
     * @param statement  the query statement.
     * @param language   the language of the query statement.
     * @throws InvalidQueryException if the query is invalid.
     */
    // DIFF JR: uses WorkspaceManager instead of QueryHandler
    public void init(SessionImpl session,
                     NamespaceResolver resolver,
                     ItemManager itemMgr,
                     WorkspaceManager wspManager,
                     String statement,
                     String language) throws InvalidQueryException {
        checkNotInitialized();
        this.session = session;
        this.resolver = resolver;
        this.itemManager = itemMgr;
        this.statement = statement;
        this.language = language;
        this.wspManager = wspManager;
        // DIFF JR: todo validate statement
        //this.query = handler.createExecutableQuery(session, itemMgr, statement, language);
        initialized = true;
    }

    /**
     * Initializes this query from a node.
     *
     * @param session    the session that created this query.
     * @param itemMgr    the item manager of that session.
     * @param wspManager the workspace manager that belongs to the session.
     * @param node       the node from where to read the query.
     * @throws InvalidQueryException if the query is invalid.
     * @throws RepositoryException   if another error occurs while reading from
     *                               the node.
     */
    // DIFF JR: uses WorkspaceManager instead of QueryHandler
    public void init(SessionImpl session,
                     NamespaceResolver resolver,
                     ItemManager itemMgr,
                     WorkspaceManager wspManager,
                     Node node)
            throws InvalidQueryException, RepositoryException {
        checkNotInitialized();
        this.session = session;
        this.resolver = resolver;
        this.itemManager = itemMgr;
        this.node = node;
        this.wspManager = wspManager;

        try {
            if (!node.isNodeType(NameFormat.format(QName.NT_QUERY, resolver))) {
                throw new InvalidQueryException("node is not of type nt:query");
            }
            statement = node.getProperty(NameFormat.format(QName.JCR_STATEMENT, resolver)).getString();
            language = node.getProperty(NameFormat.format(QName.JCR_LANGUAGE, resolver)).getString();
            // DIFF JR: todo validate statement
            //query = handler.createExecutableQuery(session, itemMgr, statement, language);
        } catch (NoPrefixDeclaredException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    public QueryResult execute() throws RepositoryException {
        checkInitialized();
        return new QueryResultImpl(itemManager,
                wspManager.executeQuery(statement, language), resolver);
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
            Path p = PathFormat.parse(absPath, resolver).getNormalizedPath();
            if (!p.isAbsolute()) {
                throw new RepositoryException(absPath + " is not an absolute path");
            }
            if (session.itemExists(absPath)) {
                throw new ItemExistsException(absPath);
            }
            String jcrParent = PathFormat.format(p.getAncestor(1), resolver);
            if (!session.itemExists(jcrParent)) {
                throw new PathNotFoundException(jcrParent);
            }
            String relPath = PathFormat.format(p, resolver).substring(1);
            String ntName = NameFormat.format(QName.NT_QUERY, resolver);
            Node queryNode = session.getRootNode().addNode(relPath, ntName);
            // set properties
            queryNode.setProperty(NameFormat.format(QName.JCR_LANGUAGE, resolver), language);
            queryNode.setProperty(NameFormat.format(QName.JCR_STATEMENT, resolver), statement);
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

