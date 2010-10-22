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

import static javax.jcr.query.qom.QueryObjectModelConstants.JCR_JOIN_TYPE_INNER;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.EquiJoinCondition;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.query.qom.Source;

import org.apache.jackrabbit.commons.query.QueryObjectModelBuilderRegistry;
import org.apache.jackrabbit.core.query.lucene.join.QueryEngine;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelTree;

/**
 * <code>QueryObjectModelImpl</code> implements the query object model.
 */
public class QueryObjectModelImpl extends QueryImpl implements QueryObjectModel {

    /**
     * The query object model tree.
     */
    protected QueryObjectModelTree qomTree;

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
        this.query = handler.createExecutableQuery(sessionContext, qomTree);
        setInitialized();
    }

    public QueryResult execute() throws RepositoryException {
        Source source = getSource();
        if (source instanceof Join) {
            Join join = (Join) source;
            if (JCR_JOIN_TYPE_INNER.equals(join.getJoinType())
                    && join.getJoinCondition() instanceof EquiJoinCondition) {
                QueryEngine engine =
                    new QueryEngine(sessionContext.getSessionImpl(), query.getBindVariables());
                return engine.execute(
                        getColumns(), getSource(), getConstraint(),
                        getOrderings(), offset, limit);
            }
        }
        return super.execute();
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
