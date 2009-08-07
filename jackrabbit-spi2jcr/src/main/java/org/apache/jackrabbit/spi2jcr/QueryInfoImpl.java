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
package org.apache.jackrabbit.spi2jcr;

import org.apache.jackrabbit.spi.QueryInfo;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.commons.iterator.RangeIteratorAdapter;
import org.apache.jackrabbit.commons.iterator.RangeIteratorDecorator;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.RepositoryException;
import javax.jcr.RangeIterator;
import java.util.NoSuchElementException;

/**
 * <code>QueryInfoImpl</code> implements a <code>QueryInfo</code> based on a
 * JCR {@link javax.jcr.query.QueryResult}.
 */
class QueryInfoImpl implements QueryInfo {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(QueryInfoImpl.class);

    /**
     * The underlying query result.
     */
    private final QueryResult result;

    /**
     * The id factory.
     */
    private final IdFactoryImpl idFactory;

    /**
     * The namespace resolver.
     */
    private final NamePathResolver resolver;

    /**
     * The QValue factory.
     */
    private final QValueFactory qValueFactory;

    /**
     * The names of the columns in the query result.
     */
    private final Name[] columnNames;

    /**
     * The resolved name of the jcr:score column.
     */
    private final String scoreName;

    /**
     * The resolved name of the jcr:path column.
     */
    private final String pathName;

    /**
     * Creates a new query info based on a given <code>result</code>.
     *
     * @param result        the JCR query result.
     * @param idFactory     the id factory.
     * @param resolver
     * @param qValueFactory the QValue factory.
     * @throws RepositoryException if an error occurs while reading from
     *                             <code>result</code>.
     */
    public QueryInfoImpl(QueryResult result,
                         IdFactoryImpl idFactory,
                         NamePathResolver resolver,
                         QValueFactory qValueFactory)
            throws RepositoryException {
        this.result = result;
        this.idFactory = idFactory;
        this.resolver = resolver;
        this.qValueFactory = qValueFactory;
        String[] jcrNames = result.getColumnNames();
        this.columnNames = new Name[jcrNames.length];
        try {
            for (int i = 0; i < jcrNames.length; i++) {
                columnNames[i] = resolver.getQName(jcrNames[i]);
            }
            this.scoreName = resolver.getJCRName(NameConstants.JCR_SCORE);
            this.pathName = resolver.getJCRName(NameConstants.JCR_PATH);
        } catch (NameException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RangeIterator getRows() {
        try {
            final String[] columnJcrNames = result.getColumnNames();
            return new RangeIteratorDecorator(result.getRows()) {
                public Object next() {
                    try {
                        return new QueryResultRowImpl(
                                (Row) super.next(), columnJcrNames, scoreName,
                                pathName, idFactory, resolver, qValueFactory);
                    } catch (RepositoryException e) {
                        log.warn("Exception when creating QueryResultRowImpl: " +
                                e.getMessage(), e);
                        throw new NoSuchElementException();
                    }
                }
            };
        } catch (RepositoryException e) {
            return RangeIteratorAdapter.EMPTY;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Name[] getColumnNames() {
        Name[] names = new Name[columnNames.length];
        System.arraycopy(columnNames, 0, names, 0, columnNames.length);
        return names;
    }
}
