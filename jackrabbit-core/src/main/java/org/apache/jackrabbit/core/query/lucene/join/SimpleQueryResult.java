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
package org.apache.jackrabbit.core.query.lucene.join;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;

/**
 * Simple query result implementation.
 */
public class SimpleQueryResult implements QueryResult {

    /**
     * The column names of this query.
     */
    private final String[] columnNames;

    /**
     * The selector names of this query.
     */
    private final String[] selectorNames;

    /**
     * Query result rows. Set to <code>null</code> when the iterator has
     * already been accessed.
     */
    private RowIterator rowIterator;

    /**
     * Creates a query result with the given column and selector names and
     * row iterator.
     *
     * @param columnNames column names
     * @param selectorNames selector names
     * @param rowIterator iterator over matching rows
     */
    protected SimpleQueryResult(
            String[] columnNames, String[] selectorNames,
            RowIterator rowIterator) {
        assert columnNames != null;
        assert selectorNames != null && selectorNames.length >= 1;
        assert rowIterator != null;
        this.columnNames = columnNames;
        this.selectorNames = selectorNames;
        this.rowIterator = rowIterator;
    }

    /**
     * Returns the column names of this query. Note that the returned array
     * is not protected against modification by the client application.
     *
     * @return column names
     */
    public String[] getColumnNames() {
        return columnNames;
    }

    /**
     * Returns the selector names of this query. Note that the returned array
     * is not protected against modification by the client application.
     *
     * @return selector names
     */
    public String[] getSelectorNames() {
        return selectorNames;
    }

    /**
     * Returns the query result rows.
     *
     * @return query result rows
     * @throws RepositoryException if the query results have already
     *                             been iterated through
     */
    public synchronized RowIterator getRows() throws RepositoryException {
        if (rowIterator != null) {
            RowIterator iterator = rowIterator;
            rowIterator = null;
            return iterator;
        } else {
            throw new RepositoryException(
                    "This query result has already been iterated through");
        }
    }

    /**
     * Returns the nodes that match this query.
     *
     * @return matching nodes
     * @throws RepositoryException if this query has more than one selector,
     *                             or if the query results have already been
     *                             iterated through
     */
    public NodeIterator getNodes() throws RepositoryException {
        if (selectorNames.length == 1) {
            return new NodeIteratorAdapter(getRows()) {
                @Override
                public Object next() {
                    Row row = (Row) super.next();
                    try {
                        return row.getNode();
                    } catch (RepositoryException e) {
                        throw new RuntimeException(
                                "Unable to access the node in " + row, e);
                    }
                }
            };
        } else {
            throw new RepositoryException(
                    "This query result contains more than one selector");
        }
    }

}
