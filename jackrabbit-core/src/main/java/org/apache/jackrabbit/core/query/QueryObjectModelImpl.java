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

import org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModel;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.Source;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.Ordering;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.Constraint;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.Column;
import org.apache.jackrabbit.spi.commons.query.qom.QueryObjectModelTree;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.ItemManager;

import javax.jcr.query.InvalidQueryException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * <code>QueryObjectModelImpl</code>...
 */
public class QueryObjectModelImpl extends QueryImpl implements QueryObjectModel {

    /**
     * The query object model tree.
     */
    protected QueryObjectModelTree qomTree;

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
        this.language = language;
        this.handler = handler;
        this.statement = statement;
        this.query = handler.createExecutableQuery(session, itemMgr,
                createQOMTree(statement, language));
        setInitialized();
    }

    /**
     * @inheritDoc
     */
    public void init(SessionImpl session,
                     ItemManager itemMgr,
                     QueryHandler handler,
                     Node node) throws InvalidQueryException, RepositoryException {
        checkNotInitialized();
        this.session = session;
        this.node = node;
        this.handler = handler;

        if (!node.isNodeType(session.getJCRName(NameConstants.NT_QUERY))) {
            throw new InvalidQueryException("node is not of type nt:query");
        }
        this.statement = node.getProperty(session.getJCRName(NameConstants.JCR_STATEMENT)).getString();
        this.language = node.getProperty(session.getJCRName(NameConstants.JCR_LANGUAGE)).getString();
        this.query = handler.createExecutableQuery(session, itemMgr,
                createQOMTree(statement, language));
        setInitialized();
    }

    /**
     * @inheritDoc
     */
    public void init(SessionImpl session,
                     ItemManager itemMgr,
                     QueryHandler handler,
                     QueryObjectModelTree qomTree,
                     String language)
            throws InvalidQueryException, RepositoryException {
        checkNotInitialized();
        this.session = session;
        this.language = language;
        this.handler = handler;
        this.qomTree = qomTree;
        this.statement = null; // TODO: format qomTree into a SQL2 statement
        this.query = handler.createExecutableQuery(session, itemMgr, qomTree);
        setInitialized();
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

    //------------------------------< internal >--------------------------------

    /**
     * Creates a {@link QueryObjectModelTree} representation for the query
     * <code>statement</code>.
     *
     * @param statement the query statement.
     * @param language  the language of the query statement.
     * @return the {@link QueryObjectModelTree} representation.
     * @throws InvalidQueryException if the query statement is malformed.
     */
    private QueryObjectModelTree createQOMTree(String statement,
                                               String language)
            throws InvalidQueryException {
        // TODO: implement
        return null;
    }
}
