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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.Source;

import org.apache.jackrabbit.api.stats.RepositoryStatistics.Type;
import org.apache.jackrabbit.commons.query.QueryObjectModelBuilderRegistry;
import org.apache.jackrabbit.core.query.lucene.LuceneQueryFactory;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.apache.jackrabbit.core.query.lucene.join.QueryEngine;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.session.SessionOperation;
import org.apache.jackrabbit.stats.RepositoryStatisticsImpl;
import org.apache.jackrabbit.spi.commons.query.qom.BindVariableValueImpl;
import org.apache.jackrabbit.spi.commons.query.qom.DefaultTraversingQOMTreeVisitor;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>QueryObjectModelImpl</code> implements the query object model.
 */
public class QueryObjectModelImpl extends QueryImpl implements QueryObjectModel {

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(QueryObjectModelImpl.class);

    /**
     * The query object model tree.
     */
    protected QueryObjectModelTree qomTree;

    /** Bind variables */
    private final Map<String, Value> variables = new HashMap<String, Value>();

    private LuceneQueryFactory lqf;

    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException always.
     */
    @Override
    public void init(
            SessionContext sessionContext, QueryHandler handler,
            String statement, String language, Node node)
            throws InvalidQueryException {
        throw new UnsupportedOperationException();
    }

    /**
     * Initializes a query instance from a query object model.
     *
     * @param sessionContext component context of the current session
     * @param handler  the query handler of the search index.
     * @param qomTree  the query object model tree.
     * @param language the original query syntax from where the JQOM was
     *                 created.
     * @param node     a nt:query node where the query was read from or
     *                 <code>null</code> if it is not a stored query.
     * @throws InvalidQueryException if the qom tree cannot be serialized
     *                               according to the given language.
     * @throws RepositoryException   if another error occurs
     */
    public void init(
            SessionContext sessionContext, QueryHandler handler,
            QueryObjectModelTree qomTree, String language, Node node)
            throws InvalidQueryException, RepositoryException {
        checkNotInitialized();
        this.sessionContext = sessionContext;
        this.language = language;
        this.handler = handler;
        this.qomTree = qomTree;
        this.node = node;
        this.statement = QueryObjectModelBuilderRegistry.getQueryObjectModelBuilder(language).toString(this);

        try {
            qomTree.accept(new DefaultTraversingQOMTreeVisitor() {
                @Override
                public Object visit(BindVariableValueImpl node, Object data) {
                    variables.put(node.getBindVariableName(), null);
                    return data;
                }
            }, null);
        } catch (Exception ignore) {
        }
        this.lqf = new LuceneQueryFactory(
                sessionContext.getSessionImpl(), (SearchIndex) handler,
                variables);
        setInitialized();
    }

    public QueryResult execute() throws RepositoryException {
        long time = System.nanoTime();
        final QueryResult result = sessionContext.getSessionState().perform(
                new SessionOperation<QueryResult>() {
                    public QueryResult perform(SessionContext context)
                            throws RepositoryException {
                        final QueryEngine engine = new QueryEngine(
                                sessionContext.getSessionImpl(), lqf, variables);
                        return engine.execute(getColumns(), getSource(),
                                getConstraint(), getOrderings(), offset, limit);
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

    @Override
    public String[] getBindVariableNames() {
        return variables.keySet().toArray(new String[variables.size()]);
    }

    @Override
    public void bindValue(String varName, Value value)
            throws IllegalArgumentException {
        if (variables.containsKey(varName)) {
            variables.put(varName, value);
        } else {
            throw new IllegalArgumentException(
                    "No such bind variable: " + varName);
        }
    }

    //-------------------------< QueryObjectModel >-----------------------------

    /**
     * Gets the node-tuple source for this query.
     *
     * @return the node-tuple source; non-null
     */
    public Source getSource() {
        return qomTree.getSource();
    }

    /**
     * Gets the constraint for this query.
     *
     * @return the constraint, or null if none
     */
    public Constraint getConstraint() {
        return qomTree.getConstraint();
    }

    /**
     * Gets the orderings for this query.
     *
     * @return an array of zero or more orderings; non-null
     */
    public Ordering[] getOrderings() {
        return qomTree.getOrderings();
    }

    /**
     * Gets the columns for this query.
     *
     * @return an array of zero or more columns; non-null
     */
    public Column[] getColumns() {
        return qomTree.getColumns();
    }
}
