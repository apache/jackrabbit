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
package org.apache.jackrabbit.spi;

import javax.jcr.RangeIterator;
import javax.jcr.RepositoryException;

import java.util.Map;

/**
 * <code>QueryInfo</code> is the the return value of
 * {@link RepositoryService#executeQuery(SessionInfo, String, String, Map, long, long, Map)} 
 * which is used to run a query on the <code>RepositoryService</code>. It
 * provides access to the rows of the query result as well as to the column
 * names.
 *
 * @see javax.jcr.query.QueryResult#getRows()
 * @see javax.jcr.query.QueryResult#getColumnNames()
 * @see javax.jcr.query.QueryResult#getNodes()
 */
public interface QueryInfo {

    /**
     * @return an iterator over the {@link QueryResultRow}s.
     * @see javax.jcr.query.QueryResult#getRows()
     */
    public RangeIterator getRows();

    /**
     * @return an array of <code>String</code>s representing the column names of
     *         the query result.
     * @see javax.jcr.query.QueryResult#getColumnNames()
     */
    public String[] getColumnNames();

    /**
     * @return an array of <code>String</code>s representing the selector names of
     *         the query result.
     * @see javax.jcr.query.QueryResult#getSelectorNames()
     */
    public String[] getSelectorNames();
}
