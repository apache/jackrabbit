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

import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_LANGUAGE;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_STATEMENT;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.NT_QUERY;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.api.stats.RepositoryStatistics.Type;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.session.SessionOperation;
import org.apache.jackrabbit.stats.RepositoryStatisticsImpl;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the default implementation for a JCR query.
 */
public class QueryImpl extends AbstractQueryImpl {

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(QueryImpl.class);

    /**
     * Component context of the current session
     */
    protected SessionContext sessionContext;

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
     * The maximum result size
     */
    protected long limit = -1;

    /**
     * The offset in the total result set
     */
    protected long offset = 0;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(
            SessionContext sessionContext, QueryHandler handler,
            String statement, String language, Node node)
            throws InvalidQueryException {
        checkNotInitialized();
        this.sessionContext = sessionContext;
        this.statement = statement;
        this.language = language;
        this.handler = handler;
        this.node = node;
        this.query = handler.createExecutableQuery(sessionContext, statement, language);
        setInitialized();
    }

    /**
     * This method simply forwards the <code>execute</code> call to the
     * {@link ExecutableQuery} object returned by
     * {@link QueryHandler#createExecutableQuery}.
     * {@inheritDoc}
     */
    public QueryResult execute() throws RepositoryException {
        checkInitialized();
        long time = System.nanoTime();
        QueryResult result = sessionContext.getSessionState().perform(
                new SessionOperation<QueryResult>() {
                    public QueryResult perform(SessionContext context)
                            throws RepositoryException {
                        return query.execute(offset, limit);
                    }

                    public String toString() {
                        return "query.execute(" + statement + ")";
                    }
                });
        time = System.nanoTime() - time;
        final long timeMs = time / 1000000;
        log.debug("executed in {} ms. ({})", timeMs, statement);
        RepositoryStatisticsImpl statistics = sessionContext
                .getRepositoryContext().getRepositoryStatistics();
        statistics.getCounter(Type.QUERY_COUNT).incrementAndGet();
        statistics.getCounter(Type.QUERY_DURATION).addAndGet(timeMs);
        sessionContext.getRepositoryContext().getStatManager().getQueryStat()
                .logQuery(language, statement, timeMs);
        return result;
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
            Path p = sessionContext.getQPath(absPath).getNormalizedPath();
            if (!p.isAbsolute()) {
                throw new RepositoryException(absPath + " is not an absolute path");
            }

            String relPath = sessionContext.getJCRPath(p).substring(1);
            Node queryNode =
                sessionContext.getSessionImpl().getRootNode().addNode(
                        relPath, sessionContext.getJCRName(NT_QUERY));
            // set properties
            queryNode.setProperty(sessionContext.getJCRName(JCR_LANGUAGE), language);
            queryNode.setProperty(sessionContext.getJCRName(JCR_STATEMENT), statement);
            node = queryNode;
            return node;
        } catch (NameException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getBindVariableNames() {
        return new String[0];
    }

    /**
     * Throws an {@link IllegalArgumentException} as XPath and SQL1 queries
     * have no bind variables.
     *
     * @throws IllegalArgumentException always thrown
     */
    public void bindValue(String varName, Value value)
            throws IllegalArgumentException {
        throw new IllegalArgumentException("No such bind variable: " + varName);
    }

    /**
     * Sets the maximum size of the result set.
     *
     * @param limit new maximum size of the result set
     */
    public void setLimit(long limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative");
        }
        this.limit = limit;
    }

    /**
     * Sets the start offset of the result set.
     *
     * @param offset new start offset of the result set
     */
    public void setOffset(long offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        this.offset = offset;
    }

    //-----------------------------< internal >---------------------------------

    /**
     * Sets the initialized flag.
     */
    protected void setInitialized() {
        initialized = true;
    }

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
