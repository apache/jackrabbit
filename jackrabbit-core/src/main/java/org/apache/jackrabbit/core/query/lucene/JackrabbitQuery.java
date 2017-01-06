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
package org.apache.jackrabbit.core.query.lucene;

import org.apache.lucene.search.Sort;
import org.apache.jackrabbit.core.SessionImpl;

import java.io.IOException;

/**
 * <code>JackrabbitQuery</code> defines an interface for Jackrabbit query
 * implementations that are at the root of the lucene query tree. It gives the
 * implementation the opportunity to execute in an optimized way returning
 * {@link QueryHits} instead of a result that is tied to Lucene.
 */
public interface JackrabbitQuery {

    /**
     * Executes this query and returns {@link QueryHits} or <code>null</code> if
     * this query should be executed using the regular Lucene API.
     * <p>
     * <b>Important note:</b> an implementation <b>must not</b> call
     * {@link JackrabbitIndexSearcher#execute(Query, Sort, long, org.apache.jackrabbit.spi.Name)}
     * with this query instance as a parameter, otherwise a stack overflow will
     * occur.
     *
     * @param searcher the jackrabbit index searcher.
     * @param session  the session that executes the query.
     * @param sort     the sort criteria that must be reflected in the returned
     *                 {@link QueryHits}.
     * @return the query hits or <code>null</code> if the regular Lucene API
     *         should be used by the caller.
     * @throws IOException if an error occurs while executing the query.
     */
    public QueryHits execute(JackrabbitIndexSearcher searcher,
                             SessionImpl session,
                             Sort sort)
            throws IOException;
}
