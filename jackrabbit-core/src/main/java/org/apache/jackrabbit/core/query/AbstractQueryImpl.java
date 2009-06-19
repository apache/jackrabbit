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

import javax.jcr.Node;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.SessionImpl;

/**
 * Defines common initialization methods for all query implementations.
 */
public abstract class AbstractQueryImpl implements Query {

    /**
     * Initializes a query instance from a query string.
     *
     * @param session   the session of the user executing this query.
     * @param itemMgr   the item manager of the session executing this query.
     * @param handler   the query handler of the search index.
     * @param statement the query statement.
     * @param language  the syntax of the query statement.
     * @param node      a nt:query node where the query was read from or
     *                  <code>null</code> if it is not a stored query.
     * @throws InvalidQueryException if the query statement is invalid according
     *                               to the specified <code>language</code>.
     */
    public abstract void init(SessionImpl session,
                              ItemManager itemMgr,
                              QueryHandler handler,
                              String statement,
                              String language,
                              Node node) throws InvalidQueryException;
}
