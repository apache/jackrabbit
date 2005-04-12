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

import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.ItemManager;

import javax.jcr.query.InvalidQueryException;
import javax.jcr.RepositoryException;
import java.io.IOException;

/**
 * Defines an interface for the actual node indexing and query execution.
 * The goal is to allow different implementations based on the persistent
 * manager in use. Some persistent model might allow to execute a query
 * in an optimized manner, e.g. database persistence.
 */
public interface QueryHandler {

    /**
     * Initializes this query handler. This method is called after the
     * <code>QueryHandler</code> is instantiated.
     *
     * @param context the context for this query handler.
     * @throws IOException if an error occurs during initialization.
     */
    public void init(QueryHandlerContext context) throws IOException;

    /**
     * Adds a <code>Node</code> to the search index.
     * @param node the NodeState to add.
     * @throws RepositoryException if an error occurs while indexing the node.
     * @throws IOException if an error occurs while adding the node to the index.
     */
    public void addNode(NodeState node) throws RepositoryException, IOException;

    /**
     * Deletes the Node with <code>UUID</code> from the search index.
     * @param uuid the <code>UUID</code> of the node to delete.
     * @throws IOException if an error occurs while deleting the node.
     */
    public void deleteNode(String uuid) throws IOException;

    /**
     * Closes this <code>QueryHandler</code> and frees resources attached
     * to this handler.
     */
    public void close() throws IOException;

    /**
     * Creates a new query by specifying the query statement itself and the
     * language in which the query is stated.  If the query statement is
     * syntactically invalid, given the language specified, an
     * InvalidQueryException is thrown. <code>language</code> must specify a query language
     * string from among those returned by QueryManager.getSupportedQueryLanguages(); if it is not
     * then an <code>InvalidQueryException</code> is thrown.
     *
     * @param session the session of the current user creating the query object.
     * @param itemMgr the item manager of the current user.
     * @param statement the query statement.
     * @param language the syntax of the query statement.
     * @throws InvalidQueryException if statement is invalid or language is unsupported.
     * @return A <code>Query</code> object.
     */
    public ExecutableQuery createExecutableQuery(SessionImpl session,
                             ItemManager itemMgr,
                             String statement,
                             String language) throws InvalidQueryException;
}
