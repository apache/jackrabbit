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

import org.apache.jackrabbit.core.query.jsr283.PreparedQuery;
import org.apache.jackrabbit.core.query.qom.QueryObjectModelTree;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.QName;

import javax.jcr.Value;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.query.InvalidQueryException;

/**
 * <code>PreparedQueryImpl</code>...
 */
public class PreparedQueryImpl extends QueryImpl implements PreparedQuery {

    /**
     * The executable prepared query.
     */
    protected ExecutablePreparedQuery query;

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
        this.query = handler.createExecutablePreparedQuery(
                session, itemMgr, createQOMTree(statement, language));
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

        if (!node.isNodeType(session.getJCRName(QName.NT_QUERY))) {
            throw new InvalidQueryException("node is not of type nt:query");
        }
        statement = node.getProperty(session.getJCRName(QName.JCR_STATEMENT)).getString();
        language = node.getProperty(session.getJCRName(QName.JCR_LANGUAGE)).getString();
        query = handler.createExecutablePreparedQuery(
                session, itemMgr, createQOMTree(statement, language));
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
        this.query = handler.createExecutablePreparedQuery(session, itemMgr, qomTree);
        setInitialized();
    }


    /**
     * Binds the given <code>value</code> to the variable named
     * <code>varName</code>.
     *
     * @param varName name of variable in query
     * @param value   value to bind
     * @throws IllegalArgumentException      if <code>varName</code> is not a
     *                                       valid variable in this query.
     * @throws javax.jcr.RepositoryException if an error occurs.
     */
    public void bindValue(String varName, Value value)
            throws IllegalArgumentException, RepositoryException {
        try {
            query.bindValue(session.getQName(varName), value);
        } catch (NameException e) {
            throw new RepositoryException(e.getMessage());
        }
    }

    //----------------------------< internal >----------------------------------

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
